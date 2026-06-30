import { create } from "zustand";

interface MetricsState {
  cpu: number;
  ramUsed: number;
  ramTotal: number;
  tps: number;
  mspt: number;
  diskUsed: number;
  diskTotal: number;
  setCpu: (cpu: number) => void;
  setRam: (used: number, total: number) => void;
  setTps: (tps: number) => void;
  setMspt: (mspt: number) => void;
  setDisk: (used: number, total: number) => void;
}

export const useMetricsStore = create<MetricsState>((set) => ({
  cpu: 22,
  ramUsed: 3.4,
  ramTotal: 8,
  tps: 20.0,
  mspt: 15,
  diskUsed: 12,
  diskTotal: 100,

  setCpu: (cpu) => set({ cpu }),
  setRam: (ramUsed, ramTotal) => set({ ramUsed, ramTotal }),
  setTps: (tps) => set({ tps }),
  setMspt: (mspt) => set({ mspt }),
  setDisk: (diskUsed, diskTotal) => set({ diskUsed, diskTotal }),
}));
