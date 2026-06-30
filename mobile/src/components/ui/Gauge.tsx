import { View, Text, StyleSheet } from "react-native";
import { colors, borderRadius, fontSize, spacing } from "../../constants/theme";

interface GaugeProps {
  label: string;
  value: number;
  max: number;
  unit?: string;
  color?: string;
}

export function Gauge({ label, value, max, unit, color = colors.primary }: GaugeProps) {
  const pct = max > 0 ? Math.min((value / max) * 100, 100) : 0;
  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.label}>{label}</Text>
        <Text style={styles.value}>
          {value.toFixed(1)}{max > 0 ? ` / ${max.toFixed(1)}` : ""}{unit ? ` ${unit}` : ""}
        </Text>
      </View>
      <View style={styles.barBg}>
        <View style={[styles.barFill, { width: `${pct}%`, backgroundColor: color }]} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { marginBottom: spacing.sm },
  header: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: spacing.xs,
  },
  label: { fontSize: fontSize.sm, color: colors.textSecondary },
  value: { fontSize: fontSize.sm, color: colors.text, fontWeight: "600" },
  barBg: {
    height: 8,
    backgroundColor: colors.surfaceLight,
    borderRadius: 4,
    overflow: "hidden",
  },
  barFill: { height: "100%", borderRadius: 4 },
});
