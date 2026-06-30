import { View, Text, StyleSheet } from "react-native";
import { colors, borderRadius, spacing, fontSize } from "../../constants/theme";

interface ServerStatusIndicatorProps {
  status: "online" | "offline" | "starting" | "stopping";
  large?: boolean;
}

const config = {
  online: { color: colors.success, label: "Server is online" },
  offline: { color: colors.error, label: "Server is offline" },
  starting: { color: colors.warning, label: "Server is starting..." },
  stopping: { color: colors.warning, label: "Server is stopping..." },
} as const;

export function ServerStatusIndicator({ status, large }: ServerStatusIndicatorProps) {
  const c = config[status];
  return (
    <View style={[styles.banner, { borderColor: c.color, backgroundColor: c.color + "18" }]}>
      <View style={[styles.dot, { backgroundColor: c.color, width: large ? 14 : 10, height: large ? 14 : 10 }]} />
      <Text style={[styles.text, { color: c.color, fontSize: large ? fontSize.lg : fontSize.md }]}>{c.label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  banner: {
    flexDirection: "row",
    alignItems: "center",
    padding: spacing.md,
    borderRadius: borderRadius.md,
    borderWidth: 1,
    marginBottom: spacing.sm,
    gap: spacing.sm,
  },
  dot: {
    borderRadius: 7,
  },
  text: {
    fontWeight: "600",
  },
});
