import { create } from "zustand";

interface ConsoleLog {
  id: string;
  timestamp: number;
  message: string;
  level: "info" | "warn" | "error" | "debug";
}

interface ConsoleState {
  logs: ConsoleLog[];
  isPaused: boolean;
  filter: string;
  addLog: (log: ConsoleLog) => void;
  clearLogs: () => void;
  setPaused: (paused: boolean) => void;
  setFilter: (filter: string) => void;
  sendCommand: (command: string) => void;
}

export const useConsoleStore = create<ConsoleState>((set) => ({
  logs: [
    { id: "1", timestamp: Date.now() - 60000, message: "[INFO] Server starting...", level: "info" },
    { id: "2", timestamp: Date.now() - 55000, message: "[INFO] Loading properties", level: "info" },
    { id: "3", timestamp: Date.now() - 50000, message: "[INFO] Default game type: SURVIVAL", level: "info" },
    { id: "4", timestamp: Date.now() - 40000, message: "[INFO] Preparing spawn area: 100%", level: "info" },
    { id: "5", timestamp: Date.now() - 30000, message: "[INFO] Done! For help, type 'help'", level: "info" },
  ],
  isPaused: false,
  filter: "",

  addLog: (log) =>
    set((state) => ({
      logs: [...state.logs.slice(-999), log],
    })),

  clearLogs: () => set({ logs: [] }),

  setPaused: (isPaused) => set({ isPaused }),

  setFilter: (filter) => set({ filter }),

  sendCommand: (command: string) => {
    const { sendCommand } = require("../lib/serverManager").serverManager;
    sendCommand(command).catch(console.warn);
  },
}));
