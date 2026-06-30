import type { Player } from "../../../shared/types";

export type TabName = "dashboard" | "console" | "players" | "settings";

export interface ServerInfo {
  name: string;
  jarPath: string;
  ramMB: number;
  eula: boolean;
  createdAt: number;
}

export interface FileEntry {
  name: string;
  path: string;
  isDirectory: boolean;
  size: number;
  modificationTime: number;
}

export interface ProcessEvent {
  type: "stdout" | "stderr" | "exit";
  data?: string;
  code?: number;
}

export type { Player };
