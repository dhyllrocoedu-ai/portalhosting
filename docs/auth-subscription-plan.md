# Authentication + Subscription — Implementation Plan

## Phase 0: Foundation (dependencies & architecture)

### New Dependencies

| Library | Purpose |
|---------|---------|
| `com.google.firebase:firebase-bom` | Firebase Auth + billing verification backend |
| `com.android.billingclient:billing-ktx:7.+` | Google Play Billing |
| `androidx.security:security-crypto:1.1.0` | Encrypted token storage |
| `androidx.datastore:datastore-preferences:1.1.+` | Preferences (theme, onboarding) |
| `com.google.dagger:hilt-android:2.51+` | DI for auth/billing services (*optional*) |
| `androidx.hilt:hilt-navigation-compose` | Hilt + Navigation integration |

### New Files

| File | Purpose |
|------|---------|
| `auth/AuthManager.kt` | Central auth state, login/signup/logout, token management |
| `auth/AuthApi.kt` | REST client for auth API (login, signup, token refresh, verify) |
| `auth/TokenStore.kt` | Encrypted persistence for JWT/refresh tokens |
| `auth/User.kt` | Data models: `User`, `AuthState`, `AuthResponse`, `LoginRequest` |
| `billing/BillingManager.kt` | Google Play Billing wrapper — products, purchases, subscriptions |
| `billing/SubscriptionState.kt` | Subscription state sealed class |
| `ui/screens/auth/LoginScreen.kt` | Login UI |
| `ui/screens/auth/SignupScreen.kt` | Signup UI |
| `ui/screens/auth/ForgotPasswordScreen.kt` | Password reset |
| `ui/screens/settings/AccountScreen.kt` | Profile, manage subscription, logout |
| `ui/navigation/AuthGuard.kt` | Route-level gating composable |
| `ui/navigation/Routes.kt` | Add `login`, `signup`, `account` routes |
| `data/Preferences.kt` | DataStore-backed preferences |

---

## Phase 1: Auth State & Token Storage

### `auth/TokenStore.kt`

EncryptedSharedPreferences wrapping access/refresh tokens:

```kotlin
class TokenStore(context: Context) {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun saveTokens(access: String, refresh: String)
    fun clear()
    val hasTokens: Boolean
}
```

### `auth/User.kt` — Data Models

```kotlin
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val createdAt: Long,
    val subscriptionTier: SubscriptionTier
)

enum class SubscriptionTier { FREE, PREMIUM_MONTHLY, PREMIUM_YEARLY, LIFETIME }

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}
```

### `auth/AuthManager.kt`

Singleton state holder driving all UI gating:

```kotlin
val authState: StateFlow<AuthState>

fun login(email: String, password: String): Result<User>
fun signup(email: String, password: String, displayName: String): Result<User>
fun logout()
fun refreshToken(): Boolean
fun restoreSession()     // called on app start, tries refresh token
```

### `auth/AuthApi.kt`

OkHttp-based REST client calling the backend:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `POST /auth/login` | Login | Returns `{ accessToken, refreshToken, user }` |
| `POST /auth/signup` | Signup | Creates account, returns tokens |
| `POST /auth/refresh` | Refresh | Returns new access token |
| `POST /auth/forgot-password` | Reset | Sends password reset email |
| `GET /auth/me` | Profile | Returns current user + subscription tier |

---

## Phase 2: Subscription / Google Play Billing

### `billing/BillingManager.kt`

Wraps `BillingClient` for all purchase flows:

```kotlin
fun queryProducts(): List<ProductDetails>          // fetch SKUs from Google Play
fun purchase(context: Context, productId: String) // launch billing flow
fun queryPurchases(): List<Purchase>               // check active purchases
val purchaseUpdates: Flow<Purchase>                // real-time purchase updates
fun acknowledgePurchase(purchase: Purchase)        // required to grant entitlement
fun verifyServerSide(purchaseToken: String): Boolean  // backend verification
```

### Server-side Verification

The app sends purchase tokens to a backend endpoint that validates against the Google Play Developer API before granting entitlement:

```
POST /subscription/verify  →  { purchaseToken, productId }  →  { valid, tier, expiresAt }
GET  /subscription/status  →  { tier, expiresAt, active }
```

### Subscription Tiers & Feature Mapping

| Tier | Features |
|------|----------|
| **Free** | 1 server, 1 GB max RAM, no tunnel, basic console |
| **Premium** | Unlimited servers, 8 GB max RAM, tunnel support, backups, priority support |

---

## Phase 3: UI — Auth Screens

### `LoginScreen.kt`
- Email + password fields
- "Log in" button → calls `authManager.login()`
- "Sign up" link → navigates to SignupScreen
- "Forgot password" link
- Loading state with `CircularProgressIndicator`
- Error snackbar on failure

### `SignupScreen.kt`
- Display name + email + password + confirm password
- "Create Account" button → calls `authManager.signup()`
- "Already have an account? Log in" link
- Validation: email format, password length ≥ 6, passwords match

---

## Phase 4: Route Guards & Conditional Navigation

### `ui/navigation/AuthGuard.kt`

```kotlin
@Composable
fun AuthGate(content: @Composable () -> Unit) {
    val authState by authManager.authState.collectAsState()
    when (authState) {
        is AuthState.Loading -> SplashScreen()
        is AuthState.Unauthenticated -> LoginScreen()
        is AuthState.Authenticated -> content()
        is AuthState.Error -> LoginScreen(error = ...)
    }
}
```

### `AppNavigation.kt` Changes

Wrap the main `NavHost` in `AuthGate`; auth screens stay outside the gate:

```kotlin
AuthGate {
    NavHost(startDestination = "home") {
        composable("home") { HomeScreen(...) }
        composable("servers") { ServersScreen(...) }
        // ... existing routes
    }
}
// Auth routes (outside gate, always accessible)
NavHost {
    composable("login") { LoginScreen(...) }
    composable("signup") { SignupScreen(...) }
}
```

### Feature Gating

```kotlin
// Tunnel — require premium
if (subscriptionState is Active || !tunnelAvailable) {
    // show tunnel card
} else {
    // show "Upgrade to Premium for tunnel" upsell card
}

// Server count — cap at 1 for free tier
if (serverConfigs.size >= serverLimit && !hasPremium) {
    // disable "Create Server" button, show upgrade prompt
}
```

---

## Phase 5: Account & Subscription Management

### `AccountScreen.kt` (in Settings tab)
- Profile card: display name, email, avatar
- Subscription status card: current plan, expiry date
- "Manage Subscription" → opens Google Play subscriptions page
- "Upgrade to Premium" → launches billing flow
- "Restore Purchases" → re-queries Google Play for past purchases
- "Logout" button → clears tokens, navigates to login

### Subscription State UI
- Status banner on HomeScreen: "Free tier — upgrade to unlock unlimited servers"
- Countdown: "Premium expires in 14 days"
- Warning: "Subscription cancelled — renew to keep premium features"

---

## Phase 6: Backend Requirements

Minimal backend needed (Supabase, Firebase Functions, or custom server):

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/auth/signup` | POST | Create account, return tokens |
| `/auth/login` | POST | Authenticate, return tokens |
| `/auth/refresh` | POST | Refresh access token |
| `/auth/me` | GET | Get user profile + subscription |
| `/subscription/verify` | POST | Verify Google Play purchase server-side |
| `/subscription/status` | GET | Get current entitlement |

Backend responsibilities:
- **Google Play Developer API** integration for purchase verification
- **Token blacklisting** on logout/refresh
- **Subscription expiry webhooks** from Google Play

---

## Implementation Order

```
Week 1:  Phase 0 — Add dependencies, create TokenStore, User models
Week 2:  Phase 1 — AuthApi + AuthManager (token-based session restore)
Week 3:  Phase 3 — LoginScreen + SignupScreen + ForgotPasswordScreen
Week 4:  Phase 4 — AuthGate, route guards, feature gating (server cap, tunnel)
Week 5:  Phase 2 — BillingManager (Google Play Billing integration)
Week 6:  Phase 5 — AccountScreen, subscription management UI
Week 7:  Polish — error states, edge cases, testing
```

---

## Key Design Decisions

1. **Backend**: Supabase (open-source, REST + realtime, auth + billing webhooks) or Firebase (more standard). Supabase fits the current OkHttp-only approach by providing clean REST endpoints.

2. **No DI initially**: AuthManager and BillingManager can be singletons passed via `remember` and `CompositionLocal`, keeping consistency with the current `AppState` pattern.

3. **Token storage**: EncryptedSharedPreferences (not DataStore) for sensitive credentials.

4. **Billing verification**: Always server-side — the app sends purchase tokens to the backend, which validates against Google Play Developer API before granting entitlement.

5. **Graceful degradation**: If auth fails (no network, server down), the app works in offline/free mode rather than showing errors.

6. **Subscription checking**: On app start, on purchase, and periodically (every 24h) via a `Flow` that UI observes reactively.
