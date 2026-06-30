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
  cpu: 0,
  ramUsed: 0,
  ramTotal: 0,
  tps: 0,
  mspt: 0,
  diskUsed: 0,
  diskTotal: 0,

  setCpu: (cpu) => set({ cpu }),
  setRam: (ramUsed, ramTotal) => set({ ramUsed, ramTotal }),
  setTps: (tps) => set({ tps }),
  setMspt: (mspt) => set({ mspt }),
  setDisk: (diskUsed, diskTotal) => set({ diskUsed, diskTotal }),
}));
