import * as FileSystem from "expo-file-system/legacy";

const TUNNEL_FILE = `${FileSystem.documentDirectory}tunnel-address.json`;

export const tunnelStorage = {
  async getAddress(): Promise<string | null> {
    try {
      const data = await FileSystem.readAsStringAsync(TUNNEL_FILE);
      const parsed = JSON.parse(data);
      return parsed.address ?? null;
    } catch {
      return null;
    }
  },

  async setAddress(address: string | null): Promise<void> {
    if (address) {
      await FileSystem.writeAsStringAsync(
        TUNNEL_FILE,
        JSON.stringify({ address })
      );
    } else {
      try {
        await FileSystem.deleteAsync(TUNNEL_FILE, { idempotent: true });
      } catch {}
    }
  },
};
