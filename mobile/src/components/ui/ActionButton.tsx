import { TouchableOpacity, Text, StyleSheet, ActivityIndicator } from "react-native";
import { colors, borderRadius, spacing, fontSize } from "../../constants/theme";

interface ActionButtonProps {
  label: string;
  onPress: () => void;
  variant?: "primary" | "danger" | "warning" | "ghost";
  loading?: boolean;
  disabled?: boolean;
}

const variantStyles = {
  primary: { bg: colors.primary, text: colors.text },
  danger: { bg: colors.error, text: colors.text },
  warning: { bg: colors.warning, text: "#000" },
  ghost: { bg: colors.surfaceLight, text: colors.text },
} as const;

export function ActionButton({ label, onPress, variant = "primary", loading, disabled }: ActionButtonProps) {
  const v = variantStyles[variant];
  return (
    <TouchableOpacity
      style={[styles.btn, { backgroundColor: v.bg, opacity: disabled ? 0.5 : 1 }]}
      onPress={onPress}
      disabled={disabled || loading}
      activeOpacity={0.8}
    >
      {loading ? (
        <ActivityIndicator color={v.text} size="small" />
      ) : (
        <Text style={[styles.label, { color: v.text }]}>{label}</Text>
      )}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  btn: {
    flex: 1,
    paddingVertical: spacing.lg,
    borderRadius: borderRadius.md,
    alignItems: "center",
    justifyContent: "center",
    minHeight: 52,
  },
  label: {
    fontSize: fontSize.md,
    fontWeight: "700",
  },
});
