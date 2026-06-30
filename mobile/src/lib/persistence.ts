import * as FileSystem from "expo-file-system/legacy";
import { Platform } from "react-native";

export interface PersistedConfig {
  javaPath: string;
  serverName: string;
  serverDir: string | null;
  jarPath: string | null;
  ramMB: number;
  eula: boolean;
  serverInstalled: boolean;
  tunnelAddress: string | null;
  maxPlayers: number;
  onlineMode: boolean;
  seed: string;
  gamemode: string;
  difficulty: string;
  localPort: number;
}

const CONFIG_PATH =
  Platform.OS === "web"
    ? null
    : `${FileSystem.documentDirectory ?? ""}portalhost_config.json`;

const defaultConfig: PersistedConfig = {
  javaPath: "java",
  serverName: "",
  serverDir: null,
  jarPath: null,
  ramMB: 1024,
  eula: false,
  serverInstalled: false,
  tunnelAddress: null,
  maxPlayers: 20,
  onlineMode: true,
  seed: "",
  gamemode: "survival",
  difficulty: "normal",
  localPort: 25565,
};

export const persistence = {
  async save(config: Partial<PersistedConfig>): Promise<void> {
    if (!CONFIG_PATH) return;
    try {
      const existing = await this.load();
      const merged = { ...defaultConfig, ...existing, ...config };
      await FileSystem.writeAsStringAsync(CONFIG_PATH, JSON.stringify(merged, null, 2));
    } catch {}
  },

  async load(): Promise<PersistedConfig> {
    if (!CONFIG_PATH) return defaultConfig;
    try {
      const raw = await FileSystem.readAsStringAsync(CONFIG_PATH);
      return { ...defaultConfig, ...JSON.parse(raw) };
    } catch {
      return defaultConfig;
    }
  },

  async clear(): Promise<void> {
    if (!CONFIG_PATH) return;
    try {
      await FileSystem.deleteAsync(CONFIG_PATH, { idempotent: true });
    } catch {}
  },
};
