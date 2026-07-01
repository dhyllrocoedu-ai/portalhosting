import { View, Text, StyleSheet, ScrollView, TouchableOpacity, ActivityIndicator, Image } from "react-native";
import * as Clipboard from "expo-clipboard";
import { useCallback } from "react";
import { router } from "expo-router";
import { colors, spacing, fontSize, borderRadius } from "../../constants/theme";
import { useServerStore } from "../../stores/serverStore";
import { useMetricsStore } from "../../stores/metricsStore";
import { ServerStatusIndicator } from "../../components/ui/ServerStatusIndicator";
import { ActionButton } from "../../components/ui/ActionButton";
import { Gauge } from "../../components/ui/Gauge";

function AddressCard({ ip, port }: { ip: string; port: number }) {
  const handleCopy = useCallback(async () => {
    await Clipboard.setStringAsync(`${ip}:${port}`);
  }, [ip, port]);

  return (
    <TouchableOpacity style={styles.addressCard} onPress={handleCopy} activeOpacity={0.8}>
      <Text style={styles.addressLabel}>LOCAL ADDRESS (tap to copy)</Text>
      <Text style={styles.addressValue}>{ip}:{port}</Text>
    </TouchableOpacity>
  );
}

function TunnelCard({ address }: { address: string }) {
  const handleCopy = useCallback(async () => {
    await Clipboard.setStringAsync(address);
  }, [address]);

  return (
    <TouchableOpacity style={styles.tunnelCard} onPress={handleCopy} activeOpacity={0.8}>
      <Text style={styles.addressLabel}>TUNNEL ADDRESS (tap to copy)</Text>
      <Text style={styles.addressValue}>{address}</Text>
    </TouchableOpacity>
  );
}

export default function DashboardScreen() {
  const {
    status, playerCount, maxPlayers, uptime,
    serverInstalled, serverName, localIp, localPort, tunnelAddress,
    startServer, stopServer, restartServer,
  } = useServerStore();
  const { cpu, ramUsed, ramTotal, tps } = useMetricsStore();

  const formatUptime = (s: number) => {
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    return `${h}h ${m}m`;
  };

  if (!serverInstalled) {
    return (
      <View style={styles.container}>
        <View style={styles.emptyState}>
          <Image
            source={require("../../../assets/images/logo-glow.png")}
            style={styles.emptyLogo}
            resizeMode="contain"
          />
          <Text style={styles.emptyTitle}>PortalHost</Text>
          <Text style={styles.emptyText}>
            Run a Minecraft server directly on your Android device. No PC required.
          </Text>
          <TouchableOpacity style={styles.createBtn} onPress={() => router.push("/server/create")}>
            <Text style={styles.createBtnText}>Create Server</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  const running = status === "online" || status === "starting";

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={styles.topRow}>
        <Text style={styles.screenTitle}>{serverName}</Text>
      </View>

      <ServerStatusIndicator status={status} />

      <View style={styles.infoBar}>
        <View style={styles.infoBlock}>
          <Text style={styles.infoValue}>{playerCount}/{maxPlayers}</Text>
          <Text style={styles.infoLabel}>Players</Text>
        </View>
        <View style={styles.infoDivider} />
        <View style={styles.infoBlock}>
          <Text style={styles.infoValue}>{uptime > 0 ? formatUptime(uptime) : "—"}</Text>
          <Text style={styles.infoLabel}>Uptime</Text>
        </View>
        <View style={styles.infoDivider} />
        <View style={styles.infoBlock}>
          <Text style={styles.infoValue}>{tps.toFixed(1)}</Text>
          <Text style={styles.infoLabel}>TPS</Text>
        </View>
      </View>

      {running && localIp && <AddressCard ip={localIp} port={localPort} />}
      {running && tunnelAddress && <TunnelCard address={tunnelAddress} />}

      <View style={styles.actions}>
        {status === "offline" ? (
          <ActionButton label="Start Server" onPress={startServer} variant="primary" />
        ) : status === "online" ? (
          <>
            <ActionButton label="Stop" onPress={stopServer} variant="danger" />
            <ActionButton label="Restart" onPress={restartServer} variant="warning" />
          </>
        ) : (
          <View style={styles.startingBox}>
            <ActivityIndicator color={colors.warning} size="small" />
            <Text style={styles.startingText}>
              {status === "starting" ? "Starting..." : "Stopping..."}
            </Text>
          </View>
        )}
      </View>

      <TouchableOpacity style={styles.fileBtn} onPress={() => router.push("/files")}>
        <Text style={styles.fileBtnIcon}>📁</Text>
        <Text style={styles.fileBtnText}>File Manager</Text>
        <Text style={styles.fileBtnArrow}>›</Text>
      </TouchableOpacity>

      <View style={styles.metricsCard}>
        <Text style={styles.metricsTitle}>Performance</Text>
        <Gauge label="RAM" value={ramUsed} max={ramTotal} unit="GB" color={colors.accent} />
        <Gauge label="CPU" value={cpu} max={100} unit="%" color={colors.primary} />
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  content: {
    padding: spacing.md,
    paddingBottom: spacing.xl * 2,
  },
  topRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: spacing.md,
  },
  screenTitle: {
    fontSize: fontSize.xxl,
    fontWeight: "700",
    color: colors.text,
  },
  infoBar: {
    flexDirection: "row",
    backgroundColor: colors.surface,
    borderRadius: borderRadius.md,
    padding: spacing.md,
    marginBottom: spacing.sm,
  },
  infoBlock: {
    flex: 1,
    alignItems: "center",
  },
  infoDivider: {
    width: 1,
    backgroundColor: colors.surfaceLight,
    marginVertical: spacing.xs,
  },
  infoValue: {
    fontSize: fontSize.lg,
    fontWeight: "700",
    color: colors.text,
  },
  infoLabel: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
    marginTop: 2,
  },
  addressCard: {
    backgroundColor: "#0D3B0E",
    borderRadius: borderRadius.md,
    padding: spacing.lg,
    marginBottom: spacing.sm,
    borderWidth: 1,
    borderColor: colors.primary + "40",
  },
  tunnelCard: {
    backgroundColor: "#002244",
    borderRadius: borderRadius.md,
    padding: spacing.lg,
    marginBottom: spacing.sm,
    borderWidth: 1,
    borderColor: colors.accent + "40",
  },
  addressLabel: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
    letterSpacing: 1,
    marginBottom: spacing.xs,
  },
  addressValue: {
    fontSize: fontSize.xl,
    fontWeight: "700",
    color: colors.text,
    fontFamily: "monospace",
  },
  actions: {
    flexDirection: "row",
    gap: spacing.sm,
    marginBottom: spacing.sm,
  },
  startingBox: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: spacing.sm,
    backgroundColor: colors.surface,
    paddingVertical: spacing.lg,
    borderRadius: borderRadius.md,
  },
  startingText: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
  },
  fileBtn: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.surface,
    padding: spacing.md,
    borderRadius: borderRadius.md,
    marginBottom: spacing.sm,
    gap: spacing.sm,
  },
  fileBtnIcon: {
    fontSize: 20,
  },
  fileBtnText: {
    flex: 1,
    fontSize: fontSize.sm,
    fontWeight: "600",
    color: colors.text,
  },
  fileBtnArrow: {
    fontSize: 24,
    color: colors.textMuted,
  },
  metricsCard: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.md,
    padding: spacing.md,
  },
  metricsTitle: {
    fontSize: fontSize.sm,
    fontWeight: "600",
    color: colors.textSecondary,
    textTransform: "uppercase",
    letterSpacing: 1,
    marginBottom: spacing.md,
  },
  emptyState: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    padding: spacing.xl,
    gap: spacing.md,
  },
  emptyLogo: {
    width: 100,
    height: 100,
    marginBottom: spacing.sm,
  },
  emptyTitle: {
    fontSize: fontSize.xl,
    fontWeight: "700",
    color: colors.text,
  },
  emptyText: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    textAlign: "center",
    lineHeight: 20,
  },
  createBtn: {
    backgroundColor: colors.primary,
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.xl,
    borderRadius: borderRadius.md,
  },
  createBtnText: {
    color: colors.text,
    fontSize: fontSize.md,
    fontWeight: "700",
  },
});
