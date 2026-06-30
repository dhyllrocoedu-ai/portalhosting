import { create } from "zustand";
import { serverManager } from "../lib/serverManager";
import { fileManager } from "../lib/fileManager";
import { persistence } from "../lib/persistence";

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
  javaPath: string;
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
  setJavaPath: (path: string) => void;
  setTunnelAddress: (address: string | null) => void;
  configureServer: (name: string, jarPath: string, serverDir: string, ramMB: number, eula: boolean, props: { maxPlayers: number; onlineMode: boolean; seed: string; gamemode: string; difficulty: string }) => void;
  saveJavaPath: (path: string) => Promise<void>;
  saveTunnelAddress: (address: string | null) => Promise<void>;
  startServer: () => Promise<void>;
  stopServer: () => Promise<void>;
  restartServer: () => Promise<void>;
  loadPersistedState: () => Promise<void>;
  deleteServer: () => Promise<void>;
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
  javaPath: "java",
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
  setJavaPath: (javaPath) => set({ javaPath }),
  setTunnelAddress: (tunnelAddress) => set({ tunnelAddress }),

  saveJavaPath: async (path) => {
    set({ javaPath: path });
    await persistence.save({ javaPath: path });
  },

  saveTunnelAddress: async (address) => {
    set({ tunnelAddress: address });
    await persistence.save({ tunnelAddress: address });
  },

  loadPersistedState: async () => {
    const config = await persistence.load();
    set({
      javaPath: config.javaPath,
      serverName: config.serverName,
      serverDir: config.serverDir,
      jarPath: config.jarPath,
      ramMB: config.ramMB,
      eula: config.eula,
      serverInstalled: config.serverInstalled,
      tunnelAddress: config.tunnelAddress,
      maxPlayers: config.maxPlayers,
      onlineMode: config.onlineMode,
      seed: config.seed,
      gamemode: config.gamemode,
      difficulty: config.difficulty,
      localPort: config.localPort,
    });
  },

  deleteServer: async () => {
    const { serverDir } = get();
    set({
      serverInstalled: false,
      status: "offline",
      serverName: "",
      serverDir: null,
      jarPath: null,
      ramMB: 1024,
      eula: false,
      uptime: 0,
      maxPlayers: 20,
      onlineMode: true,
      seed: "",
      gamemode: "survival",
      difficulty: "normal",
    });
    await persistence.clear();
    if (serverDir) {
      await fileManager.deleteDir(serverDir);
    }
  },

  configureServer: (name, jarPath, serverDir, ramMB, eula, props) => {
    const newState = {
      serverName: name,
      jarPath,
      serverDir,
      ramMB,
      eula,
      maxPlayers: props.maxPlayers,
      onlineMode: props.onlineMode,
      seed: props.seed,
      gamemode: props.gamemode,
      difficulty: props.difficulty,
      serverInstalled: true,
      status: "offline",
    };
    set(newState);
    persistence.save(newState);
  },

  startServer: async () => {
    const { jarPath: rawJarPath, ramMB, serverDir, javaPath } = get();
    if (!rawJarPath || !serverDir) return;
    set({ status: "starting" });
    try {
      await fileManager.writeEula(serverDir);
      const jarPath = rawJarPath.replace(/^file:\/\//, "");
      await serverManager.startServer(jarPath, javaPath, [
        "-Xms512M",
        `-Xmx${ramMB}M`,
        "-jar",
        jarPath,
        "nogui",
      ]);
      set({ status: "online", uptime: 0, localIp: "127.0.0.1" });
    } catch {
      set({ status: "offline" });
    }
  },

  stopServer: async () => {
    set({ status: "stopping" });
    try {
      await serverManager.stopServer();
      set({ status: "offline", uptime: 0 });
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
