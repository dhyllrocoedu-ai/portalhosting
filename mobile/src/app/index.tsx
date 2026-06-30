import { View, Text, StyleSheet, TouchableOpacity, Image } from "react-native";
import { useEffect } from "react";
import { router } from "expo-router";
import { colors, spacing, fontSize, borderRadius } from "../constants/theme";
import { useServerStore } from "../stores/serverStore";

export default function WelcomeScreen() {
  const serverInstalled = useServerStore((s) => s.serverInstalled);

  useEffect(() => {
    if (serverInstalled) {
      router.replace("/(tabs)/dashboard");
    }
  }, [serverInstalled]);

  if (serverInstalled) return null;

  return (
    <View style={styles.container}>
      <View style={styles.content}>
        <Image
          source={require("../../assets/images/logo-glow.png")}
          style={styles.logo}
          resizeMode="contain"
        />
        <Text style={styles.title}>PortalHost</Text>
        <Text style={styles.subtitle}>Minecraft Server Manager</Text>

        <View style={styles.featureList}>
          <Text style={styles.feature}>● Run a server directly on Android</Text>
          <Text style={styles.feature}>● Full console with command input</Text>
          <Text style={styles.feature}>● Player management (OP, ban, whitelist)</Text>
          <Text style={styles.feature}>● Real-time performance metrics</Text>
        </View>

        <TouchableOpacity style={styles.createBtn} onPress={() => router.push("/server/create")}>
          <Text style={styles.createBtnText}>Create Server</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
    alignItems: "center",
    justifyContent: "center",
  },
  content: {
    alignItems: "center",
    padding: spacing.xl,
    gap: spacing.md,
  },
  logo: {
    width: 120,
    height: 120,
  },
  title: {
    fontSize: fontSize.title,
    fontWeight: "700",
    color: colors.text,
    letterSpacing: 1,
  },
  subtitle: {
    fontSize: fontSize.md,
    color: colors.textSecondary,
  },
  featureList: {
    marginTop: spacing.lg,
    gap: spacing.sm,
    alignSelf: "stretch",
  },
  feature: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    lineHeight: 22,
  },
  createBtn: {
    marginTop: spacing.xl,
    backgroundColor: colors.primary,
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.xl * 2,
    borderRadius: borderRadius.md,
  },
  createBtnText: {
    color: colors.text,
    fontSize: fontSize.lg,
    fontWeight: "700",
  },
});
