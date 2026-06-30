import { create } from "zustand";
import { serverManager } from "../lib/serverManager";

interface ServerState {
  status: "online" | "offline" | "starting" | "stopping";
  playerCount: number;
  maxPlayers: number;
  onlineMode: boolean;
  gamemode: string;
  difficulty: string;
  seed: string;
  uptime: number;
  serverName: string;
  serverInstalled: boolean;
  localIp: string | null;
  localPort: number;
  serverDir: string | null;
  jarPath: string | null;
  ramMB: number;
  eula: boolean;
  tunnelAddress: string | null;

  setStatus: (status: ServerState["status"]) => void;
  setPlayerCount: (count: number, max: number) => void;
  setUptime: (uptime: number) => void;
  setServerName: (name: string) => void;
  setServerInstalled: (installed: boolean) => void;
  setLocalIp: (ip: string | null) => void;
  setServerDir: (dir: string | null) => void;
  setJarPath: (path: string | null) => void;
  setRamMB: (mb: number) => void;
  setEula: (accepted: boolean) => void;
  setTunnelAddress: (address: string | null) => void;
  configureServer: (name: string, jarPath: string, ramMB: number, eula: boolean, props: { maxPlayers: number; onlineMode: boolean; seed: string; gamemode: string; difficulty: string }) => void;
  startServer: () => Promise<void>;
  stopServer: () => Promise<void>;
  restartServer: () => Promise<void>;
}

export const useServerStore = create<ServerState>((set, get) => ({
  status: "offline",
  playerCount: 0,
  maxPlayers: 20,
  onlineMode: true,
  gamemode: "survival",
  difficulty: "normal",
  seed: "",
  uptime: 0,
  serverName: "",
  serverInstalled: false,
  localIp: null,
  localPort: 25565,
  serverDir: null,
  jarPath: null,
  ramMB: 1024,
  eula: false,
  tunnelAddress: null,

  setStatus: (status) => set({ status }),
  setPlayerCount: (playerCount, maxPlayers) => set({ playerCount, maxPlayers }),
  setUptime: (uptime) => set({ uptime }),
  setServerName: (serverName) => set({ serverName }),
  setServerInstalled: (serverInstalled) => set({ serverInstalled }),
  setLocalIp: (localIp) => set({ localIp }),
  setServerDir: (serverDir) => set({ serverDir }),
  setJarPath: (jarPath) => set({ jarPath }),
  setRamMB: (ramMB) => set({ ramMB }),
  setEula: (eula) => set({ eula }),
  setTunnelAddress: (tunnelAddress) => set({ tunnelAddress }),

  configureServer: (name, jarPath, ramMB, eula, props) =>
    set({
      serverName: name,
      jarPath,
      ramMB,
      eula,
      maxPlayers: props.maxPlayers,
      onlineMode: props.onlineMode,
      seed: props.seed,
      gamemode: props.gamemode,
      difficulty: props.difficulty,
      serverInstalled: true,
      status: "offline",
    }),

  startServer: async () => {
    const { jarPath, ramMB } = get();
    if (!jarPath) return;
    set({ status: "starting" });
    try {
      await serverManager.startServer(jarPath, [
        "-Xms512M",
        `-Xmx${ramMB}M`,
        "-jar",
        jarPath,
        "nogui",
      ]);
      set({ status: "online" });
    } catch {
      set({ status: "offline" });
    }
  },

  stopServer: async () => {
    set({ status: "stopping" });
    try {
      await serverManager.stopServer();
      set({ status: "offline" });
    } catch {
      set({ status: "online" });
    }
  },

  restartServer: async () => {
    await get().stopServer();
    await new Promise((r) => setTimeout(r, 2000));
    await get().startServer();
  },
}));
