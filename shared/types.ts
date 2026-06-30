export interface ServerStatus {
  online: boolean;
  players: PlayerList;
  performance: PerformanceMetrics;
  uptime: number;
  address: string | null;
}

export interface PlayerList {
  online: Player[];
  whitelisted: Player[];
  banned: Player[];
}

export interface Player {
  name: string;
  uuid: string;
  online: boolean;
  op: boolean;
}

export interface PerformanceMetrics {
  cpu: number;
  ram: { used: number; total: number };
  tps: number;
  mspt: number;
  disk: { used: number; total: number };
}

export interface ConsoleLog {
  id: string;
  timestamp: number;
  message: string;
  level: "info" | "warn" | "error" | "debug";
}

export interface ServerAction {
  type: "start" | "stop" | "restart" | "force-stop";
}

export interface ServerConfig {
  autoStart: boolean;
  scheduledRestart: string | null;
  host: string;
  port: number;
}

export interface EventPayloads {
  "server:status": ServerStatus;
  "server:config": ServerConfig;
  "console:log": ConsoleLog;
  "console:clear": void;
  "player:join": Player;
  "player:leave": { name: string };
  "player:list": PlayerList;
  "metrics:update": PerformanceMetrics;
}
