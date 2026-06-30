import { View, Text, StyleSheet, FlatList, TouchableOpacity, Alert, ActivityIndicator } from "react-native";
import { useState, useEffect, useCallback } from "react";
import { router } from "expo-router";
import { colors, spacing, fontSize, borderRadius } from "../../constants/theme";
import { fileManager } from "../../lib/fileManager";
import { useServerStore } from "../../stores/serverStore";
import type { FileEntry } from "../../types";

export default function FileManagerScreen() {
  const serverDir = useServerStore((s) => s.serverDir);
  const [entries, setEntries] = useState<FileEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [previewPath, setPreviewPath] = useState<string | null>(null);
  const [previewContent, setPreviewContent] = useState<string | null>(null);

  const loadFiles = useCallback(async () => {
    if (!serverDir) return;
    setLoading(true);
    try {
      const list = await fileManager.listDir(serverDir);
      setEntries(list);
    } catch {
      setEntries([]);
    } finally {
      setLoading(false);
    }
  }, [serverDir]);

  useEffect(() => {
    loadFiles();
  }, [loadFiles]);

  const handlePress = async (entry: FileEntry) => {
    if (entry.isDirectory) {
      return;
    }
    try {
      const content = await fileManager.readFile(entry.path);
      setPreviewPath(entry.path);
      setPreviewContent(content);
    } catch {
      Alert.alert("Error", "Cannot read this file.");
    }
  };

  const handleDelete = (entry: FileEntry) => {
    Alert.alert("Delete", `Delete "${entry.name}"?`, [
      { text: "Cancel", style: "cancel" },
      {
        text: "Delete",
        style: "destructive",
        onPress: async () => {
          try {
            if (entry.isDirectory) {
              await fileManager.deleteDir(entry.path);
            } else {
              await fileManager.deleteFile(entry.path);
            }
            await loadFiles();
          } catch {
            Alert.alert("Error", "Failed to delete file.");
          }
        },
      },
    ]);
  };

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  if (previewContent !== null && previewPath) {
    return (
      <View style={styles.container}>
        <View style={styles.header}>
          <TouchableOpacity onPress={() => { setPreviewPath(null); setPreviewContent(null); }}>
            <Text style={{ fontSize: 24, color: colors.text, padding: spacing.sm }}>←</Text>
          </TouchableOpacity>
          <Text style={styles.title} numberOfLines={1}>
            {previewPath.split("/").pop()}
          </Text>
          <View style={{ width: 40 }} />
        </View>
        <FlatList
          data={[{ key: previewContent }]}
          keyExtractor={(item) => item.key}
          renderItem={({ item }) => (
            <Text style={styles.previewText} selectable>{item.key}</Text>
          )}
          contentContainerStyle={styles.previewContent}
        />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Text style={{ fontSize: 24, color: colors.text, padding: spacing.sm }}>←</Text>
        </TouchableOpacity>
        <Text style={styles.title}>File Manager</Text>
        <TouchableOpacity onPress={loadFiles}>
          <Text style={{ fontSize: 20, color: colors.accent, padding: spacing.sm }}>↻</Text>
        </TouchableOpacity>
      </View>

      {serverDir && (
        <Text style={styles.pathText} numberOfLines={1}>{serverDir}</Text>
      )}

      {loading ? (
        <ActivityIndicator color={colors.primary} style={{ marginTop: spacing.xl }} />
      ) : entries.length === 0 ? (
        <View style={styles.emptyState}>
          <Text style={{ fontSize: 40 }}>📂</Text>
          <Text style={styles.emptyText}>No files yet</Text>
          <Text style={styles.emptyHint}>Start the server to generate server files.</Text>
        </View>
      ) : (
        <FlatList
          data={entries}
          keyExtractor={(item) => item.path}
          renderItem={({ item }) => (
            <View style={styles.fileRow}>
              <TouchableOpacity style={styles.fileInfo} onPress={() => handlePress(item)}>
                <Text style={{ fontSize: 18, marginRight: spacing.sm }}>
                  {item.isDirectory ? "📁" : "📄"}
                </Text>
                <View style={{ flex: 1 }}>
                  <Text style={styles.fileName} numberOfLines={1}>{item.name}</Text>
                  {!item.isDirectory && (
                    <Text style={styles.fileSize}>{formatSize(item.size)}</Text>
                  )}
                </View>
              </TouchableOpacity>
              <TouchableOpacity style={styles.deleteBtn} onPress={() => handleDelete(item)}>
                <Text style={{ fontSize: 18, color: colors.error }}>🗑</Text>
              </TouchableOpacity>
            </View>
          )}
          contentContainerStyle={styles.listContent}
        />
      )}
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
    flex: 1,
  },
  pathText: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
    fontFamily: "monospace",
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  listContent: {
    padding: spacing.md,
  },
  fileRow: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.surface,
    borderRadius: borderRadius.sm,
    marginBottom: spacing.xs,
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.md,
  },
  fileInfo: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
  },
  fileName: {
    fontSize: fontSize.sm,
    color: colors.text,
    fontFamily: "monospace",
  },
  fileSize: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
    marginTop: 1,
  },
  deleteBtn: {
    padding: spacing.xs,
    marginLeft: spacing.sm,
  },
  emptyState: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    gap: spacing.sm,
    padding: spacing.xl,
  },
  emptyText: {
    fontSize: fontSize.lg,
    color: colors.text,
    fontWeight: "600",
  },
  emptyHint: {
    fontSize: fontSize.sm,
    color: colors.textMuted,
    textAlign: "center",
  },
  previewContent: {
    padding: spacing.md,
  },
  previewText: {
    fontSize: fontSize.xs,
    color: colors.textSecondary,
    fontFamily: "monospace",
    lineHeight: 18,
  },
});
