# PortalHost Native App Patches

Apply these 3 remaining patches in Android Studio after the CPU rounding fix in `HomeLiveStatsGrid.kt` is already confirmed working.

---

## Patch 1: IP Address Display

**File:** `native/app/src/main/java/com/portalhost/app/ui/screens/home/HomeServerCard.kt`

### Step 1a: Add NetworkInfo import

Add this line among the imports at the top:
```kotlin
import com.portalhost.app.network.NetworkInfo
```

### Step 1b: Add `networkInfo` parameter

In the `ServerCard(` function signature (around line 40-52), add a new parameter after `statusColor: Color`:

```kotlin
fun ServerCard(
    activeServer: ServerConfig?,
    serverConfigs: List<ServerConfig>,
    serverState: ServerState,
    statusColor: Color,
    networkInfo: NetworkInfo,       // <-- ADD THIS LINE
    repository: ServerRepository,
    ...
)
```

### Step 1c: Add IP address state variables

After the line `var expanded by remember { mutableStateOf(false) }` (line 53), add:

```kotlin
val isRunning = serverState.status == ServerStatus.ONLINE || serverState.status == ServerStatus.STARTING
val connectionAddress = if (isRunning && networkInfo.localIp != "Unknown")
    "${networkInfo.localIp}:${activeServer?.port ?: 25565}"
else "Server not running"
```

### Step 1d: Add the IP address row to the UI

Find the chips `LazyRow` block (the one starting around line 131 with `androidx.compose.foundation.lazy.LazyRow(`). After its closing brace `}` and the `Spacer(Modifier.height(6.dp))` that follows it, insert this block BEFORE the `if (serverConfigs.isNotEmpty())` line:

```kotlin
Spacer(Modifier.height(6.dp))
Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
        Icons.Default.Lan,
        contentDescription = null,
        modifier = Modifier.size(14.dp),
        tint = MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.width(4.dp))
    Text(
        text = if (isRunning) connectionAddress else "Server not running",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        color = if (isRunning) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant
    )
}
```

### Step 1e: Update the call site in HomeScreen.kt

In `HomeScreen.kt`, find the `ServerCard(` call (around line 137-149). Add `networkInfo = networkInfo` as a named parameter:

```kotlin
ServerCard(
    activeServer = activeServer,
    serverConfigs = serverConfigs,
    serverState = serverState,
    statusColor = statusColor,
    networkInfo = networkInfo,    // <-- ADD THIS LINE
    repository = repository,
    ...
)
```

---

## Patch 2: Fix Upload/Download 0 B/s

**File:** `native/app/src/main/java/com/portalhost/app/server/ProcessMonitor.kt`

### Step 2a: Add TrafficStats imports

Add these imports at the top of the file (in the existing import block):
```kotlin
import android.net.TrafficStats
import android.os.Process
```

### Step 2b: Replace measureNetworkRate()

Replace the ENTIRE `measureNetworkRate()` function (lines 116-160) with:

```kotlin
private fun measureNetworkRate(): Pair<Long, Long> {
    return try {
        val now = System.nanoTime()
        val elapsedNs = now - lastNetTime
        if (lastNetTime == 0L || elapsedNs <= 0) {
            lastNetRx = 0L
            lastNetTx = 0L
            lastNetTime = now
            return 0L to 0L
        }
        val pid = runCatching {
            val f = process!!.javaClass.getDeclaredField("pid").apply { isAccessible = true }
            f.getInt(process)
        }.getOrNull()
        val serverUid = pid?.let {
            File("/proc/$it/status").readLines()
                .firstOrNull { l -> l.startsWith("Uid:") }
                ?.split("\\s+".toRegex())?.getOrNull(1)?.toIntOrNull()
        }
        val rx = serverUid?.let { TrafficStats.getUidRxBytes(it) } ?: 0L
        val tx = serverUid?.let { TrafficStats.getUidTxBytes(it) } ?: 0L
        val elapsedSec = elapsedNs / 1_000_000_000.0
        val rxRate = if (elapsedSec > 0) ((rx - lastNetRx) / elapsedSec).toLong().coerceIn(0L, Long.MAX_VALUE) else 0L
        val txRate = if (elapsedSec > 0) ((tx - lastNetTx) / elapsedSec).toLong().coerceIn(0L, Long.MAX_VALUE) else 0L
        lastNetRx = rx
        lastNetTx = tx
        lastNetTime = now
        rxRate to txRate
    } catch (_: Exception) {
        0L to 0L
    }
}
```

**No AndroidManifest.xml permission changes needed** — `TrafficStats.getUidRxBytes/TxBytes` works without any permission for your own app's child processes.

---

## Patch 3: Storage Card

**File:** `native/app/src/main/java/com/portalhost/app/ui/screens/HomeScreen.kt`

Find where `PerformanceCard(` is called. It's currently around line 219. Add this line RIGHT AFTER the `PerformanceCard(...)` block closes (look for the `)` and `,` that ends the `PerformanceCard` call):

```kotlin
StorageCard(storageStats = storageStats)
```

The surrounding code should look like:
```kotlin
PerformanceCard(
    processStats = processStats,
    serverState = serverState,
    maxPlayers = maxPlayers,
    onOpenPerformance = onOpenPerformance
)
StorageCard(storageStats = storageStats)   // <-- ADD THIS

ConsoleCard(
    consoleLines = consoleLines,
    ...
)
```

---

## Patch 4: Recent Activity shows transitional states

**File:** `native/app/src/main/java/com/portalhost/app/ui/screens/home/HomeServerCard.kt` (or wherever `ActivityLog` is updated)

Find where the server state change events are added to the activity log. Add explicit events for `STARTING` and `STOPPING` so the Recent Activity card shows them instead of jumping straight to ONLINE/STOPPED:

```kotlin
serverState.status = ServerStatus.STARTING
activityLog.add(ActivityLog.ServerStarting(serverState.status))

serverState.status = ServerStatus.STOPPING
activityLog.add(ActivityLog.ServerStopping(serverState.status))
```

---

## Build & Install

After all patches:

```powershell
cd D:\mydevprojects\portalhosting\native
.\gradlew.bat assembleRelease
C:\Users\dreeb\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\release\PortalHost.apk
```