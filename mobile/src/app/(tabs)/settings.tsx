import { View, Text, StyleSheet, ScrollView, TouchableOpacity, TextInput, Switch, Alert, Platform } from "react-native";
import { useState, useEffect } from "react";
import { router } from "expo-router";
import { colors, spacing, fontSize, borderRadius } from "../../constants/theme";
import { useServerStore } from "../../stores/serverStore";

const COMMON_JAVA_PATHS = [
  "/data/data/com.termux/files/usr/bin/java",
  "/system/bin/java",
  "/system/xbin/java",
];

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>{title}</Text>
      <View style={styles.sectionContent}>{children}</View>
    </View>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <View style={styles.row}>
      <Text style={styles.rowLabel}>{label}</Text>
      {children}
    </View>
  );
}

export default function SettingsScreen() {
  const [autoStart, setAutoStart] = useState(false);
  const [scheduledRestart, setScheduledRestart] = useState("");
  const [tunnelInput, setTunnelInput] = useState("");
  const [javaInput, setJavaInput] = useState("");

  const { serverName, serverInstalled, tunnelAddress, javaPath, saveJavaPath, saveTunnelAddress, deleteServer } = useServerStore();

  useEffect(() => {
    setJavaInput(javaPath);
    setTunnelInput(tunnelAddress ?? "");
  }, [javaPath, tunnelAddress]);

  const handleSaveJava = () => {
    if (javaInput.trim()) {
      saveJavaPath(javaInput.trim());
      Alert.alert("Saved", "Java path saved.");
    }
  };

  const handleAutoDetectJava = () => {
    if (Platform.OS !== "android") {
      Alert.alert("Not Available", "Auto-detect only works on Android.");
      return;
    }
    Alert.alert(
      "Auto-detect Java",
      "Common Java paths:\n\n" + COMMON_JAVA_PATHS.join("\n") + "\n\nTermux is the most common way to install Java on Android.",
      [
        { text: "Cancel", style: "cancel" },
        ...COMMON_JAVA_PATHS.map((path) => ({
          text: path,
          onPress: () => {
            setJavaInput(path);
            saveJavaPath(path);
          },
        })),
      ],
    );
  };

  const handleSaveTunnel = () => {
    saveTunnelAddress(tunnelInput.trim() || null);
    Alert.alert("Saved", "Tunnel address saved. It will appear on the dashboard when the server is running.");
  };

  const handleCreateNewServer = () => {
    router.push("/server/create");
  };

  const handleDeleteServer = () => {
    Alert.alert(
      "Delete Server",
      "This will permanently delete all server files and configuration.",
      [
        { text: "Cancel", style: "cancel" },
        {
          text: "Delete",
          style: "destructive",
          onPress: () => deleteServer(),
        },
      ],
    );
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.title}>Settings</Text>

      <Section title="Server Management">
        {serverInstalled ? (
          <>
            <Row label="Server">
              <View style={styles.serverInfo}>
                <Text style={styles.valueText}>{serverName}</Text>
              </View>
            </Row>
            <TouchableOpacity style={styles.linkRow} onPress={handleCreateNewServer}>
              <Text style={styles.linkText}>Create New Server</Text>
              <Text style={{ fontSize: 18, color: colors.textMuted }}>›</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.linkRow} onPress={() => router.push("/files")}>
              <Text style={styles.linkText}>File Manager</Text>
              <Text style={{ fontSize: 18, color: colors.textMuted }}>›</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.linkRow} onPress={handleDeleteServer}>
              <Text style={[styles.linkText, { color: colors.error }]}>Delete Server</Text>
              <Text style={{ fontSize: 18, color: colors.error }}>✕</Text>
            </TouchableOpacity>
          </>
        ) : (
          <TouchableOpacity style={styles.button} onPress={handleCreateNewServer}>
            <Text style={styles.buttonText}>Create New Server</Text>
          </TouchableOpacity>
        )}
      </Section>

      <Section title="Server Config">
        <View style={styles.javaPathSection}>
          <Text style={styles.javaPathLabel}>Java Path</Text>
          <TextInput
            style={styles.javaPathInput}
            value={javaInput}
            onChangeText={setJavaInput}
            placeholder='/data/data/com.termux/files/usr/bin/java'
            placeholderTextColor={colors.textMuted}
            autoCapitalize="none"
            autoCorrect={false}
          />
          {Platform.OS === "android" && (
            <TouchableOpacity style={styles.javaDetectBtn} onPress={handleAutoDetectJava}>
              <Text style={styles.javaDetectBtnText}>Auto-detect</Text>
            </TouchableOpacity>
          )}
          <TouchableOpacity style={styles.javaSaveBtn} onPress={handleSaveJava}>
            <Text style={styles.javaSaveBtnText}>Save Java Path</Text>
          </TouchableOpacity>
        </View>
        <Row label="Auto-start">
          <Switch
            value={autoStart}
            onValueChange={setAutoStart}
            trackColor={{ false: colors.surfaceLight, true: colors.primary + "80" }}
            thumbColor={autoStart ? colors.primary : colors.textMuted}
          />
        </Row>
        <Row label="Scheduled Restart">
          <TextInput
            style={styles.input}
            value={scheduledRestart}
            onChangeText={setScheduledRestart}
            placeholder="e.g. 04:00"
            placeholderTextColor={colors.textMuted}
          />
        </Row>
      </Section>

      <Section title="Tunnel">
        <Text style={styles.hintText}>
          If you use a tunnel service (like playit.gg or ngrok), enter your public address here. It will appear on the dashboard when the server is online.
        </Text>
        <Row label="Public Address">
          <TextInput
            style={styles.input}
            value={tunnelInput}
            onChangeText={setTunnelInput}
            placeholder={tunnelAddress ?? "e.g. example.playit.gg:25565"}
            placeholderTextColor={colors.textMuted}
          />
        </Row>
        <TouchableOpacity style={styles.button} onPress={handleSaveTunnel}>
          <Text style={styles.buttonText}>Save Address</Text>
        </TouchableOpacity>
      </Section>

      <Section title="About">
        <Row label="Version">
          <Text style={styles.valueText}>1.1.0</Text>
        </Row>
        <Row label="Platform">
          <Text style={styles.valueText}>On-Device Server</Text>
        </Row>
      </Section>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  content: {
    paddingBottom: spacing.xl * 2,
  },
  title: {
    fontSize: fontSize.xxl,
    fontWeight: "700",
    color: colors.text,
    paddingHorizontal: spacing.md,
    paddingTop: spacing.xl,
    paddingBottom: spacing.lg,
  },
  section: {
    marginBottom: spacing.md,
  },
  sectionTitle: {
    fontSize: fontSize.sm,
    fontWeight: "600",
    color: colors.textSecondary,
    textTransform: "uppercase",
    letterSpacing: 1,
    paddingHorizontal: spacing.md,
    marginBottom: spacing.sm,
  },
  sectionContent: {
    backgroundColor: colors.surface,
    marginHorizontal: spacing.md,
    borderRadius: borderRadius.md,
    overflow: "hidden",
  },
  row: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.surfaceLight,
  },
  rowLabel: {
    fontSize: fontSize.md,
    color: colors.text,
  },
  input: {
    backgroundColor: colors.surfaceLight,
    color: colors.text,
    fontSize: fontSize.sm,
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
    borderRadius: borderRadius.sm,
    minWidth: 160,
    textAlign: "right",
  },
  button: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: spacing.sm,
    paddingVertical: spacing.md,
    marginHorizontal: spacing.md,
    marginVertical: spacing.sm,
    borderRadius: borderRadius.sm,
    backgroundColor: colors.surfaceLight,
  },
  buttonText: {
    fontSize: fontSize.sm,
    color: colors.text,
    fontWeight: "600",
  },
  linkRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.surfaceLight,
  },
  linkText: {
    fontSize: fontSize.md,
    color: colors.accent,
    fontWeight: "500",
  },
  valueText: {
    fontSize: fontSize.md,
    color: colors.textSecondary,
  },
  serverInfo: {
    alignItems: "flex-end",
  },
  hintText: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
    lineHeight: 18,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  javaPathSection: {
    paddingHorizontal: spacing.md,
    paddingTop: spacing.md,
    paddingBottom: spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: colors.surfaceLight,
    gap: spacing.sm,
  },
  javaPathLabel: {
    fontSize: fontSize.sm,
    fontWeight: "600",
    color: colors.textSecondary,
    textTransform: "uppercase",
    letterSpacing: 1,
    marginBottom: spacing.xs,
  },
  javaPathInput: {
    backgroundColor: colors.surfaceLight,
    color: colors.text,
    fontSize: fontSize.sm,
    fontFamily: "monospace",
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: borderRadius.sm,
  },
  javaDetectBtn: {
    alignSelf: "flex-start",
    paddingVertical: spacing.xs,
    paddingHorizontal: spacing.sm,
    borderRadius: borderRadius.sm,
    borderWidth: 1,
    borderColor: colors.accent,
  },
  javaDetectBtnText: {
    fontSize: fontSize.xs,
    color: colors.accent,
    fontWeight: "600",
  },
  javaSaveBtn: {
    backgroundColor: colors.primary,
    paddingVertical: spacing.sm,
    borderRadius: borderRadius.sm,
    alignItems: "center",
  },
  javaSaveBtnText: {
    fontSize: fontSize.sm,
    color: colors.text,
    fontWeight: "600",
  },
});
