import { View, Text, StyleSheet, TouchableOpacity, TextInput, ScrollView, Alert, Platform } from "react-native";
import { useState } from "react";
import { router } from "expo-router";
import * as DocumentPicker from "expo-document-picker";
import { colors, spacing, fontSize, borderRadius } from "../../constants/theme";
import { useServerStore } from "../../stores/serverStore";
import { fileManager } from "../../lib/fileManager";

const RAM_PRESETS = [512, 1024, 1536, 2048, 3072, 4096];
const GAMEMODES = ["survival", "creative", "adventure", "spectator"];
const DIFFICULTIES = ["peaceful", "easy", "normal", "hard"];

const steps = ["Pick JAR", "Name", "RAM", "Config", "EULA"];

export default function CreateServerScreen() {
  const [step, setStep] = useState(0);
  const [jarPath, setJarPath] = useState<string | null>(null);
  const [jarName, setJarName] = useState("");
  const [serverName, setServerName] = useState("");
  const [ramMB, setRamMB] = useState(1024);
  const [maxPlayers, setMaxPlayers] = useState("20");
  const [onlineMode, setOnlineMode] = useState(true);
  const [seed, setSeed] = useState("");
  const [gamemode, setGamemode] = useState("survival");
  const [difficulty, setDifficulty] = useState("normal");
  const [eula, setEula] = useState(false);
  const [loading, setLoading] = useState(false);

  const handlePickJar = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: "application/java-archive",
        copyToCacheDirectory: true,
      });
      if (!result.canceled && result.assets?.[0]) {
        setJarPath(result.assets[0].uri);
        const name = result.assets[0].name ?? "server.jar";
        setJarName(name);
      }
    } catch {
      Alert.alert("Error", "Failed to pick file");
    }
  };

  const handleCreate = async () => {
    if (!jarPath || !serverName.trim() || !eula) return;
    setLoading(true);
    try {
      const props = {
        maxPlayers: parseInt(maxPlayers, 10) || 20,
        onlineMode,
        seed: seed.trim(),
        gamemode,
        difficulty,
      };
      const dest = await fileManager.copyJarToServer(jarPath, serverName.trim());
      const dir = dest.replace("/server.jar", "");
      await fileManager.writeEula(dir);
      await fileManager.writeServerProperties(dir, props);
      useServerStore.getState().configureServer(serverName.trim(), dest, ramMB, eula, props);
      useServerStore.getState().setServerDir(dir);
      Alert.alert("Server Created", `"${serverName}" is ready to start.`, [
        { text: "Go to Dashboard", onPress: () => router.replace("/(tabs)/dashboard") },
      ]);
    } catch {
      Alert.alert("Error", "Failed to create server. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const next = () => {
    if (step === 0 && !jarPath) { Alert.alert("Select a JAR", "Pick a server .jar file first."); return; }
    if (step === 1 && !serverName.trim()) { Alert.alert("Name it", "Enter a name for your server."); return; }
    if (step < steps.length - 1) setStep((s) => s + 1);
  };

  const prev = () => setStep((s) => Math.max(0, s - 1));

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Text style={{ fontSize: 24, color: colors.text, padding: spacing.sm }}>✕</Text>
        </TouchableOpacity>
        <Text style={styles.title}>Create Server</Text>
        <View style={{ width: 40 }} />
      </View>

      <View style={styles.stepIndicator}>
        {steps.map((s, i) => (
          <View key={s} style={styles.stepWrapper}>
            <View style={[styles.stepDot, { backgroundColor: i <= step ? colors.primary : colors.surfaceLight }]}>
              <Text style={[styles.stepNum, { color: i <= step ? colors.text : colors.textMuted }]}>{i + 1}</Text>
            </View>
            {i < steps.length - 1 && <View style={[styles.stepLine, { backgroundColor: i < step ? colors.primary : colors.surfaceLight }]} />}
          </View>
        ))}
      </View>

      {Platform.OS === "web" && (
        <View style={styles.webBanner}>
          <Text style={styles.webBannerText}>
            🌐 Web Demo — Server creation requires Android. The form works for UI testing.
          </Text>
        </View>
      )}

      <ScrollView style={styles.form} contentContainerStyle={styles.formContent}>
        {step === 0 && (
          <View style={styles.stepContent}>
            <Text style={styles.stepTitle}>Select a Server JAR</Text>
            <Text style={styles.stepHint}>
              Download a server .jar (Paper, Fabric, Vanilla) and pick it from your device.
            </Text>
            <TouchableOpacity style={styles.pickerArea} onPress={handlePickJar}>
              {jarPath ? (
                <>
                  <Text style={{ fontSize: 32 }}>📦</Text>
                  <Text style={styles.pickedFile}>{jarName}</Text>
                  <Text style={styles.pickedHint}>Tap to change</Text>
                </>
              ) : (
                <>
                  <Text style={{ fontSize: 40 }}>📁</Text>
                  <Text style={styles.pickerText}>Tap to select .jar file</Text>
                </>
              )}
            </TouchableOpacity>
          </View>
        )}

        {step === 1 && (
          <View style={styles.stepContent}>
            <Text style={styles.stepTitle}>Name Your Server</Text>
            <Text style={styles.stepHint}>This name will appear on the dashboard.</Text>
            <TextInput
              style={styles.nameInput}
              value={serverName}
              onChangeText={setServerName}
              placeholder="e.g. Survival World"
              placeholderTextColor={colors.textMuted}
              autoFocus
            />
          </View>
        )}

        {step === 2 && (
          <View style={styles.stepContent}>
            <Text style={styles.stepTitle}>Allocate RAM</Text>
            <Text style={styles.stepHint}>
              More RAM = better performance, but uses more device memory.
            </Text>
            <View style={styles.ramGrid}>
              {RAM_PRESETS.map((mb) => (
                <TouchableOpacity
                  key={mb}
                  style={[styles.ramChip, ramMB === mb && styles.ramChipActive]}
                  onPress={() => setRamMB(mb)}
                >
                  <Text style={[styles.ramChipText, ramMB === mb && styles.ramChipTextActive]}>
                    {mb < 1024 ? `${mb}MB` : `${mb / 1024}GB`}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
            <View style={styles.ramInfo}>
              <Text style={styles.ramInfoText}>
                {ramMB < 1024 ? `${ramMB}MB` : `${ramMB / 1024}GB`} — {ramMB >= 2048 ? "Recommended" : ramMB === 512 ? "Minimal" : "Good"}
              </Text>
            </View>
          </View>
        )}

        {step === 3 && (
          <View style={styles.stepContent}>
            <Text style={styles.stepTitle}>Server Properties</Text>
            <Text style={styles.stepHint}>
              These settings are written to server.properties before first start.
            </Text>

            <Text style={styles.fieldLabel}>Max Players</Text>
            <TextInput
              style={styles.nameInput}
              value={maxPlayers}
              onChangeText={setMaxPlayers}
              keyboardType="number-pad"
              placeholder="20"
              placeholderTextColor={colors.textMuted}
            />

            <Text style={styles.fieldLabel}>Online Mode</Text>
            <TouchableOpacity
              style={[styles.toggleRow, { borderColor: onlineMode ? colors.primary : colors.surfaceLight }]}
              onPress={() => setOnlineMode(!onlineMode)}
            >
              <View style={[styles.toggleDot, { backgroundColor: onlineMode ? colors.primary : colors.textMuted }]} />
              <Text style={styles.toggleText}>{onlineMode ? "ON (premium only)" : "OFF (cracked allowed)"}</Text>
            </TouchableOpacity>

            <Text style={styles.fieldLabel}>World Seed</Text>
            <TextInput
              style={styles.nameInput}
              value={seed}
              onChangeText={setSeed}
              placeholder="Leave blank for random"
              placeholderTextColor={colors.textMuted}
            />

            <Text style={styles.fieldLabel}>Gamemode</Text>
            <View style={styles.chipRow}>
              {GAMEMODES.map((g) => (
                <TouchableOpacity
                  key={g}
                  style={[styles.chip, gamemode === g && styles.chipActive]}
                  onPress={() => setGamemode(g)}
                >
                  <Text style={[styles.chipText, gamemode === g && styles.chipTextActive]}>{g}</Text>
                </TouchableOpacity>
              ))}
            </View>

            <Text style={styles.fieldLabel}>Difficulty</Text>
            <View style={styles.chipRow}>
              {DIFFICULTIES.map((d) => (
                <TouchableOpacity
                  key={d}
                  style={[styles.chip, difficulty === d && styles.chipActive]}
                  onPress={() => setDifficulty(d)}
                >
                  <Text style={[styles.chipText, difficulty === d && styles.chipTextActive]}>{d}</Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>
        )}

        {step === 4 && (
          <View style={styles.stepContent}>
            <Text style={styles.stepTitle}>Review & Agree</Text>

            <View style={styles.summaryCard}>
              <SummaryRow label="Name" value={serverName} />
              <SummaryRow label="JAR" value={jarName} />
              <SummaryRow label="RAM" value={ramMB < 1024 ? `${ramMB}MB` : `${ramMB / 1024}GB`} />
              <SummaryRow label="Max Players" value={maxPlayers} />
              <SummaryRow label="Online Mode" value={onlineMode ? "Premium" : "Cracked"} />
              <SummaryRow label="Seed" value={seed.trim() || "Random"} />
              <SummaryRow label="Gamemode" value={gamemode} />
              <SummaryRow label="Difficulty" value={difficulty} />
            </View>

            <TouchableOpacity style={styles.eulaRow} onPress={() => setEula(!eula)}>
              <View style={[styles.checkbox, eula && styles.checkboxActive]}>
                {eula && <Text style={{ color: colors.text, fontSize: 14, fontWeight: "700" }}>✓</Text>}
              </View>
              <Text style={styles.eulaText}>
                I agree to the{" "}
                <Text style={{ color: colors.accent }}>Minecraft EULA</Text>
              </Text>
            </TouchableOpacity>

            {eula && (
              <TouchableOpacity
                style={[styles.createBtn, loading && { opacity: 0.5 }]}
                onPress={handleCreate}
                disabled={loading}
              >
                <Text style={styles.createBtnText}>
                  {loading ? "Creating..." : "Create Server"}
                </Text>
              </TouchableOpacity>
            )}
          </View>
        )}
      </ScrollView>

      <View style={styles.footer}>
        {step > 0 && (
          <TouchableOpacity style={styles.footerBtn} onPress={prev}>
            <Text style={styles.footerBtnText}>Back</Text>
          </TouchableOpacity>
        )}
        {step < steps.length - 1 && (
          <TouchableOpacity style={[styles.footerBtn, styles.footerBtnPrimary]} onPress={next}>
            <Text style={[styles.footerBtnText, { color: colors.text }]}>Next</Text>
          </TouchableOpacity>
        )}
      </View>
    </View>
  );
}

function SummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.summaryRow}>
      <Text style={styles.summaryLabel}>{label}</Text>
      <Text style={styles.summaryValue}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: spacing.sm,
    paddingTop: spacing.xl,
  },
  title: {
    fontSize: fontSize.xl,
    fontWeight: "700",
    color: colors.text,
  },
  stepIndicator: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    paddingVertical: spacing.lg,
    paddingHorizontal: spacing.xl,
  },
  stepWrapper: {
    flexDirection: "row",
    alignItems: "center",
  },
  stepDot: {
    width: 32,
    height: 32,
    borderRadius: 16,
    alignItems: "center",
    justifyContent: "center",
  },
  stepNum: {
    fontSize: fontSize.sm,
    fontWeight: "700",
  },
  stepLine: {
    width: 40,
    height: 3,
    borderRadius: 2,
    marginHorizontal: spacing.xs,
  },
  form: {
    flex: 1,
  },
  formContent: {
    padding: spacing.md,
  },
  stepContent: {
    gap: spacing.md,
  },
  stepTitle: {
    fontSize: fontSize.lg,
    fontWeight: "700",
    color: colors.text,
  },
  stepHint: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    lineHeight: 20,
  },
  pickerArea: {
    borderWidth: 2,
    borderColor: colors.surfaceLight,
    borderStyle: "dashed",
    borderRadius: borderRadius.lg,
    padding: spacing.xl,
    alignItems: "center",
    gap: spacing.sm,
  },
  pickerText: {
    fontSize: fontSize.md,
    color: colors.textMuted,
  },
  pickedFile: {
    fontSize: fontSize.md,
    color: colors.text,
    fontWeight: "600",
  },
  pickedHint: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
  },
  nameInput: {
    backgroundColor: colors.surface,
    color: colors.text,
    fontSize: fontSize.md,
    padding: spacing.md,
    borderRadius: borderRadius.md,
    borderWidth: 1,
    borderColor: colors.surfaceLight,
  },
  ramGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: spacing.sm,
  },
  ramChip: {
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.md,
    borderRadius: borderRadius.sm,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceLight,
  },
  ramChipActive: {
    borderColor: colors.primary,
    backgroundColor: colors.primary + "20",
  },
  ramChipText: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    fontWeight: "600",
  },
  ramChipTextActive: {
    color: colors.primary,
  },
  ramInfo: {
    alignItems: "center",
    paddingVertical: spacing.sm,
  },
  ramInfoText: {
    fontSize: fontSize.sm,
    color: colors.textMuted,
  },
  fieldLabel: {
    fontSize: fontSize.sm,
    fontWeight: "600",
    color: colors.textSecondary,
    textTransform: "uppercase",
    letterSpacing: 1,
    marginTop: spacing.xs,
  },
  toggleRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.sm,
    backgroundColor: colors.surface,
    padding: spacing.md,
    borderRadius: borderRadius.md,
    borderWidth: 1,
  },
  toggleDot: {
    width: 12,
    height: 12,
    borderRadius: 6,
  },
  toggleText: {
    fontSize: fontSize.sm,
    color: colors.text,
    fontWeight: "500",
  },
  chipRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: spacing.sm,
  },
  chip: {
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.md,
    borderRadius: borderRadius.sm,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceLight,
  },
  chipActive: {
    borderColor: colors.accent,
    backgroundColor: colors.accent + "18",
  },
  chipText: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    fontWeight: "600",
    textTransform: "capitalize",
  },
  chipTextActive: {
    color: colors.accent,
  },
  summaryCard: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.md,
    overflow: "hidden",
    borderWidth: 1,
    borderColor: colors.surfaceLight,
  },
  summaryRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: colors.surfaceLight,
  },
  summaryLabel: {
    fontSize: fontSize.sm,
    color: colors.textMuted,
  },
  summaryValue: {
    fontSize: fontSize.sm,
    color: colors.text,
    fontWeight: "600",
  },
  eulaRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.md,
    paddingVertical: spacing.md,
  },
  checkbox: {
    width: 28,
    height: 28,
    borderRadius: borderRadius.sm,
    borderWidth: 2,
    borderColor: colors.surfaceLight,
    alignItems: "center",
    justifyContent: "center",
  },
  checkboxActive: {
    borderColor: colors.primary,
    backgroundColor: colors.primary,
  },
  eulaText: {
    flex: 1,
    fontSize: fontSize.sm,
    color: colors.text,
    lineHeight: 20,
  },
  createBtn: {
    backgroundColor: colors.primary,
    paddingVertical: spacing.lg,
    borderRadius: borderRadius.md,
    alignItems: "center",
    marginTop: spacing.md,
  },
  createBtnText: {
    color: colors.text,
    fontSize: fontSize.lg,
    fontWeight: "700",
  },
  footer: {
    flexDirection: "row",
    gap: spacing.sm,
    padding: spacing.md,
    borderTopWidth: 1,
    borderTopColor: colors.surfaceLight,
  },
  footerBtn: {
    flex: 1,
    paddingVertical: spacing.md,
    borderRadius: borderRadius.md,
    alignItems: "center",
    backgroundColor: colors.surfaceLight,
  },
  footerBtnPrimary: {
    backgroundColor: colors.primary,
  },
  footerBtnText: {
    fontSize: fontSize.md,
    fontWeight: "700",
    color: colors.textSecondary,
  },
  webBanner: {
    backgroundColor: colors.warning + "20",
    borderWidth: 1,
    borderColor: colors.warning + "60",
    marginHorizontal: spacing.md,
    padding: spacing.sm,
    borderRadius: borderRadius.sm,
    marginBottom: spacing.sm,
  },
  webBannerText: {
    fontSize: fontSize.xs,
    color: colors.warning,
    textAlign: "center",
    lineHeight: 18,
  },
});
