import { create } from "zustand";
import { serverManager } from "../lib/serverManager";

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
  logs: [],
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
    serverManager.sendCommand(command).catch(console.warn);
  },
}));
