import { View, Text, StyleSheet } from "react-native";
import { colors, borderRadius, spacing, fontSize } from "../../constants/theme";

interface StatusBadgeProps {
  status: "online" | "offline" | "starting" | "stopping";
}

const statusConfig = {
  online: { color: colors.success, label: "Online" },
  offline: { color: colors.error, label: "Offline" },
  starting: { color: colors.warning, label: "Starting..." },
  stopping: { color: colors.warning, label: "Stopping..." },
} as const;

export function StatusBadge({ status }: StatusBadgeProps) {
  const config = statusConfig[status];
  return (
    <View style={[styles.badge, { backgroundColor: config.color + "20", borderColor: config.color }]}>
      <View style={[styles.dot, { backgroundColor: config.color }]} />
      <Text style={[styles.label, { color: config.color }]}>{config.label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
    borderRadius: borderRadius.sm,
    borderWidth: 1,
    gap: spacing.xs,
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  label: {
    fontSize: fontSize.xs,
    fontWeight: "700",
  },
});
