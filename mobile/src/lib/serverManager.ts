import {
  startServerProcess,
  stopServerProcess,
  writeStdin,
  isServerRunning,
  addStdoutListener,
  addExitListener,
} from "../../modules/server-process-module";

import type { EventSubscription } from "expo-modules-core";

let stdoutSub: EventSubscription | null = null;
let exitSub: EventSubscription | null = null;

function stripFileScheme(path: string): string {
  return path.replace(/^file:\/\//, "");
}

export const serverManager = {
  async startServer(jarPath: string, javaPath: string, javaArgs: string[]): Promise<void> {
    await startServerProcess(stripFileScheme(jarPath), [javaPath, ...javaArgs]);
  },

  async stopServer(): Promise<void> {
    await stopServerProcess();
  },

  async sendCommand(command: string): Promise<void> {
    await writeStdin(command + "\n");
  },

  async isRunning(): Promise<boolean> {
    return isServerRunning();
  },

  onStdout(callback: (data: string) => void): void {
    stdoutSub?.remove();
    stdoutSub = addStdoutListener(callback);
  },

  onExit(callback: (code: number) => void): void {
    exitSub?.remove();
    exitSub = addExitListener(callback);
  },

  removeListeners(): void {
    stdoutSub?.remove();
    exitSub?.remove();
    stdoutSub = null;
    exitSub = null;
  },
};
