import { View, Text, StyleSheet, FlatList, TextInput, TouchableOpacity } from "react-native";
import { useState } from "react";
import { colors, spacing, fontSize, borderRadius } from "../../constants/theme";
import { useConsoleStore } from "../../stores/consoleStore";

const levelColors: Record<string, string> = {
  info: colors.textSecondary,
  warn: colors.warning,
  error: colors.error,
  debug: colors.textMuted,
};

export default function ConsoleScreen() {
  const { logs, isPaused, filter, clearLogs, setPaused, setFilter } = useConsoleStore();
  const [command, setCommand] = useState("");

  const filteredLogs = filter
    ? logs.filter((l) => l.message.toLowerCase().includes(filter.toLowerCase()))
    : logs;

  const handleSend = () => {
    if (!command.trim()) return;
    useConsoleStore.getState().sendCommand(command);
    setCommand("");
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Console</Text>
        <View style={styles.toolbar}>
          <TouchableOpacity onPress={() => setPaused(!isPaused)}>
            <Text style={{ fontSize: 20, color: colors.textSecondary }}>{isPaused ? "▶" : "⏸"}</Text>
          </TouchableOpacity>
          <TouchableOpacity onPress={clearLogs}>
            <Text style={{ fontSize: 20, color: colors.textSecondary }}>🗑</Text>
          </TouchableOpacity>
        </View>
      </View>

      <View style={styles.searchBar}>
        <Text style={{ fontSize: 14, color: colors.textMuted }}>🔍</Text>
        <TextInput
          style={styles.searchInput}
          placeholder="Filter logs..."
          placeholderTextColor={colors.textMuted}
          value={filter}
          onChangeText={setFilter}
        />
      </View>

      <FlatList
        data={filteredLogs}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => {
          const time = new Date(item.timestamp).toLocaleTimeString();
          const stripped = item.message.replace(/[§&][0-9a-fklmnor]/g, "");
          return (
            <View style={styles.logEntry}>
              <Text style={[styles.logTime, { color: levelColors[item.level] ?? colors.textSecondary }]}>{time}</Text>
              <Text style={[styles.logMessage, { color: levelColors[item.level] ?? colors.textSecondary }]} selectable>
                {stripped}
              </Text>
            </View>
          );
        }}
        style={styles.logList}
        contentContainerStyle={styles.logContent}
        windowSize={10}
      />

      <View style={styles.inputBar}>
        <TextInput
          style={styles.input}
          placeholder="Enter command..."
          placeholderTextColor={colors.textMuted}
          value={command}
          onChangeText={setCommand}
          onSubmitEditing={handleSend}
          returnKeyType="send"
        />
        <TouchableOpacity style={styles.sendBtn} onPress={handleSend}>
          <Text style={{ fontSize: 28, color: colors.primary }}>↵</Text>
        </TouchableOpacity>
      </View>
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
    justifyContent: "space-between",
    alignItems: "center",
    paddingHorizontal: spacing.md,
    paddingTop: spacing.xl,
    paddingBottom: spacing.sm,
  },
  title: {
    fontSize: fontSize.xxl,
    fontWeight: "700",
    color: colors.text,
  },
  toolbar: {
    flexDirection: "row",
    gap: spacing.md,
  },
  searchBar: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.surface,
    marginHorizontal: spacing.md,
    marginBottom: spacing.sm,
    paddingHorizontal: spacing.sm,
    borderRadius: borderRadius.sm,
    height: 36,
  },
  searchInput: {
    flex: 1,
    color: colors.text,
    fontSize: fontSize.sm,
    marginLeft: spacing.xs,
  },
  logList: {
    flex: 1,
  },
  logContent: {
    paddingHorizontal: spacing.md,
    paddingBottom: spacing.sm,
  },
  logEntry: {
    flexDirection: "row",
    paddingVertical: 2,
  },
  logTime: {
    fontSize: fontSize.xs,
    fontFamily: "monospace",
    marginRight: spacing.sm,
    minWidth: 70,
  },
  logMessage: {
    fontSize: fontSize.xs,
    fontFamily: "monospace",
    flexShrink: 1,
  },
  inputBar: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderTopWidth: 1,
    borderTopColor: colors.surfaceLight,
    backgroundColor: colors.background,
  },
  input: {
    flex: 1,
    backgroundColor: colors.surface,
    color: colors.text,
    fontSize: fontSize.sm,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: borderRadius.sm,
    marginRight: spacing.sm,
    fontFamily: "monospace",
  },
  sendBtn: {
    justifyContent: "center",
    alignItems: "center",
  },
});
