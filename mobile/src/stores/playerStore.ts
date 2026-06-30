import { create } from "zustand";
import { serverManager } from "../lib/serverManager";

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
  online: [],
  whitelisted: [],
  banned: [],

  setOnline: (online) => set({ online }),
  setWhitelisted: (whitelisted) => set({ whitelisted }),
  setBanned: (banned) => set({ banned }),

  kickPlayer: (name) => {
    serverManager.sendCommand(`kick ${name}`).catch(console.warn);
  },
  banPlayer: (name) => {
    serverManager.sendCommand(`ban ${name}`).catch(console.warn);
  },
  unbanPlayer: (name) => {
    serverManager.sendCommand(`pardon ${name}`).catch(console.warn);
  },
  opPlayer: (name) => {
    serverManager.sendCommand(`op ${name}`).catch(console.warn);
  },
  deopPlayer: (name) => {
    serverManager.sendCommand(`deop ${name}`).catch(console.warn);
  },
  addToWhitelist: (name) => {
    serverManager.sendCommand(`whitelist add ${name}`).catch(console.warn);
  },
}));
