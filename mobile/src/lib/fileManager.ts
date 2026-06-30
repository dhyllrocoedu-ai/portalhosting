import * as FileSystem from "expo-file-system/legacy";
import { Platform } from "react-native";
import type { FileEntry } from "../types";

const WEB_DEMO_DIR = "/portalhost-servers/";
const BASE_DIR = Platform.OS === "web" ? WEB_DEMO_DIR : `${FileSystem.documentDirectory ?? ""}servers/`;

export const fileManager = {
  async ensureServerDir(serverName: string): Promise<string> {
    const dir = `${BASE_DIR}${serverName}/`;
    if (Platform.OS === "web") return dir;
    const info = await FileSystem.getInfoAsync(dir);
    if (!info.exists) {
      await FileSystem.makeDirectoryAsync(dir, { intermediates: true });
    }
    return dir;
  },

  async copyJarToServer(jarUri: string, serverName: string): Promise<string> {
    const dir = await this.ensureServerDir(serverName);
    const dest = `${dir}server.jar`;
    if (Platform.OS !== "web") {
      await FileSystem.copyAsync({ from: jarUri, to: dest });
    }
    return dest;
  },

  async listDir(dir: string): Promise<FileEntry[]> {
    if (Platform.OS === "web") return [];
    const info = await FileSystem.getInfoAsync(dir);
    if (!info.exists || !info.isDirectory) {
      return [];
    }
    const names = await FileSystem.readDirectoryAsync(dir);
    const entries: FileEntry[] = [];
    for (const name of names) {
      const path = `${dir}${name}`;
      const stat = await FileSystem.getInfoAsync(path);
      entries.push({
        name,
        path,
        isDirectory: stat.isDirectory ?? false,
        size: (stat as any).size ?? 0,
        modificationTime: (stat as any).modificationTime ?? 0,
      });
    }
    return entries.sort((a, b) => {
      if (a.isDirectory !== b.isDirectory) return a.isDirectory ? -1 : 1;
      return a.name.localeCompare(b.name);
    });
  },

  async readFile(path: string): Promise<string> {
    if (Platform.OS === "web") return "(demo — file contents not available on web)";
    return await FileSystem.readAsStringAsync(path);
  },

  async deleteFile(path: string): Promise<void> {
    if (Platform.OS !== "web") {
      await FileSystem.deleteAsync(path, { idempotent: true });
    }
  },

  async deleteDir(path: string): Promise<void> {
    if (Platform.OS !== "web") {
      await FileSystem.deleteAsync(path, { idempotent: true });
    }
  },

  async serverExists(serverName: string): Promise<boolean> {
    if (Platform.OS === "web") return false;
    const dir = `${BASE_DIR}${serverName}/`;
    const info = await FileSystem.getInfoAsync(dir);
    return info.exists;
  },

  async writeEula(serverDir: string): Promise<void> {
    if (Platform.OS !== "web") {
      await FileSystem.writeAsStringAsync(
        `${serverDir}eula.txt`,
        "eula=true\n"
      );
    }
  },

  async writeServerProperties(
    serverDir: string,
    props: {
      maxPlayers: number;
      onlineMode: boolean;
      seed: string;
      gamemode: string;
      difficulty: string;
    }
  ): Promise<void> {
    const lines = [
      `max-players=${props.maxPlayers}`,
      `online-mode=${props.onlineMode}`,
      `level-seed=${props.seed}`,
      `gamemode=${props.gamemode}`,
      `difficulty=${props.difficulty}`,
      `spawn-protection=16`,
      `pvp=true`,
      `allow-nether=true`,
      `enable-command-block=false`,
      `motd=A PortalHost Server`,
    ];
    if (Platform.OS !== "web") {
      await FileSystem.writeAsStringAsync(
        `${serverDir}server.properties`,
        lines.join("\n") + "\n"
      );
    }
  },
};
