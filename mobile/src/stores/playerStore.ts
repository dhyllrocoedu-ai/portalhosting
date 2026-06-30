import { create } from "zustand";

interface Player {
  name: string;
  uuid: string;
  online: boolean;
  op: boolean;
}

interface PlayerState {
  online: Player[];
  whitelisted: Player[];
  banned: Player[];
  setOnline: (players: Player[]) => void;
  setWhitelisted: (players: Player[]) => void;
  setBanned: (players: Player[]) => void;
  kickPlayer: (name: string) => void;
  banPlayer: (name: string) => void;
  unbanPlayer: (name: string) => void;
  opPlayer: (name: string) => void;
  deopPlayer: (name: string) => void;
  addToWhitelist: (name: string) => void;
}

export const usePlayerStore = create<PlayerState>((set) => ({
  online: [
    { name: "Steve", uuid: "uuid-1", online: true, op: true },
    { name: "Alex", uuid: "uuid-2", online: true, op: false },
    { name: "Notch", uuid: "uuid-3", online: false, op: false },
  ].filter((p) => p.online),
  whitelisted: [
    { name: "Steve", uuid: "uuid-1", online: true, op: true },
    { name: "Alex", uuid: "uuid-2", online: true, op: false },
    { name: "Notch", uuid: "uuid-3", online: false, op: false },
  ],
  banned: [],

  setOnline: (online) => set({ online }),
  setWhitelisted: (whitelisted) => set({ whitelisted }),
  setBanned: (banned) => set({ banned }),

  kickPlayer: (_name) => {},
  banPlayer: (_name) => {},
  unbanPlayer: (_name) => {},
  opPlayer: (_name) => {},
  deopPlayer: (_name) => {},
  addToWhitelist: (_name) => {},
}));
