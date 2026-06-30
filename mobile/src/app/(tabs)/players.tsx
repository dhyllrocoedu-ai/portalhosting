import { View, Text, StyleSheet, FlatList, TouchableOpacity, TextInput } from "react-native";
import { useState } from "react";
import { colors, spacing, fontSize, borderRadius } from "../../constants/theme";
import { usePlayerStore } from "../../stores/playerStore";

function PlayerBadge({ label, color }: { label: string; color: string }) {
  return (
    <View style={[styles.playerBadge, { backgroundColor: color + "20", borderColor: color }]}>
      <View style={[styles.playerDot, { backgroundColor: color }]} />
      <Text style={[styles.playerBadgeText, { color }]}>{label}</Text>
    </View>
  );
}

export default function PlayersScreen() {
  const { online, whitelisted, banned, kickPlayer, banPlayer, opPlayer, deopPlayer, addToWhitelist } = usePlayerStore();
  const [selectedPlayer, setSelectedPlayer] = useState<string | null>(null);
  const [whitelistInput, setWhitelistInput] = useState("");

  const sections = [
    { title: "Online", data: online },
    { title: "Whitelisted", data: whitelisted },
    { title: "Banned", data: banned },
  ];

  const handleWhitelistAdd = () => {
    const name = whitelistInput.trim();
    if (!name) return;
    addToWhitelist(name);
    setWhitelistInput("");
  };

  const renderPlayer = ({ item }: { item: { name: string; online: boolean; op: boolean } }) => (
    <TouchableOpacity
      style={styles.playerRow}
      onPress={() => setSelectedPlayer(selectedPlayer === item.name ? null : item.name)}
    >
      <View style={styles.playerInfo}>
        <View style={[styles.statusDot, { backgroundColor: item.online ? colors.success : colors.textMuted }]} />
        <Text style={styles.playerName}>{item.name}</Text>
        {item.op && <PlayerBadge label="OP" color={colors.gold} />}
      </View>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Players</Text>

      <FlatList
        data={sections}
        keyExtractor={(item) => item.title}
        renderItem={({ item: section }) => (
          <View style={styles.section}>
            <View style={styles.sectionHeader}>
              <Text style={styles.sectionTitle}>
                {section.title} ({section.data.length})
              </Text>
            </View>
            {section.data.length === 0 ? (
              <Text style={styles.emptyText}>No players</Text>
            ) : (
              section.data.map((player) => (
                <View key={player.uuid}>
                  {renderPlayer({ item: player })}
                  {selectedPlayer === player.name && (
                    <View style={styles.actions}>
                      <TouchableOpacity style={styles.actionBtn} onPress={() => kickPlayer(player.name)}>
                        <Text style={styles.actionBtnText}>Kick</Text>
                      </TouchableOpacity>
                      <TouchableOpacity style={styles.actionBtn} onPress={() => banPlayer(player.name)}>
                        <Text style={styles.actionBtnText}>Ban</Text>
                      </TouchableOpacity>
                      <TouchableOpacity style={styles.actionBtn} onPress={() => player.op ? deopPlayer(player.name) : opPlayer(player.name)}>
                        <Text style={styles.actionBtnText}>
                          {player.op ? "De-OP" : "OP"}
                        </Text>
                      </TouchableOpacity>
                    </View>
                  )}
                </View>
              ))
            )}
          </View>
        )}
        contentContainerStyle={styles.listContent}
      />

      <View style={styles.whitelistBar}>
        <TextInput
          style={styles.whitelistInput}
          placeholder="Add to whitelist..."
          placeholderTextColor={colors.textMuted}
          value={whitelistInput}
          onChangeText={setWhitelistInput}
        />
        <TouchableOpacity style={styles.whitelistBtn} onPress={handleWhitelistAdd}>
          <Text style={{ fontSize: 28, color: colors.primary }}>+</Text>
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
  title: {
    fontSize: fontSize.xxl,
    fontWeight: "700",
    color: colors.text,
    paddingHorizontal: spacing.md,
    paddingTop: spacing.xl,
    paddingBottom: spacing.sm,
  },
  listContent: {
    paddingBottom: spacing.xl,
  },
  section: {
    marginBottom: spacing.sm,
  },
  sectionHeader: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: colors.surfaceLight,
  },
  sectionTitle: {
    fontSize: fontSize.sm,
    fontWeight: "600",
    color: colors.textSecondary,
    textTransform: "uppercase",
    letterSpacing: 1,
  },
  playerRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.md,
  },
  playerInfo: {
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.sm,
    flex: 1,
  },
  playerName: {
    fontSize: fontSize.md,
    color: colors.text,
    fontWeight: "500",
  },
  statusDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
  },
  playerBadge: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: spacing.xs,
    paddingVertical: 1,
    borderRadius: borderRadius.sm,
    borderWidth: 1,
  },
  playerDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    marginRight: 3,
  },
  playerBadgeText: {
    fontSize: 10,
    fontWeight: "700",
  },
  actions: {
    flexDirection: "row",
    gap: spacing.sm,
    paddingHorizontal: spacing.md + 20,
    paddingBottom: spacing.sm,
  },
  actionBtn: {
    backgroundColor: colors.surfaceLight,
    paddingVertical: spacing.xs,
    paddingHorizontal: spacing.md,
    borderRadius: borderRadius.sm,
  },
  actionBtnText: {
    fontSize: fontSize.sm,
    color: colors.text,
    fontWeight: "600",
  },
  emptyText: {
    fontSize: fontSize.sm,
    color: colors.textMuted,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    fontStyle: "italic",
  },
  whitelistBar: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderTopWidth: 1,
    borderTopColor: colors.surfaceLight,
    backgroundColor: colors.background,
  },
  whitelistInput: {
    flex: 1,
    backgroundColor: colors.surface,
    color: colors.text,
    fontSize: fontSize.sm,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: borderRadius.sm,
    marginRight: spacing.sm,
  },
  whitelistBtn: {
    justifyContent: "center",
    alignItems: "center",
  },
});
