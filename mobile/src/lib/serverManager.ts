import { NativeModules } from "react-native";

const { ServerProcessModule } = NativeModules;

let _stdoutCallback: ((data: string) => void) | null = null;
let _stderrCallback: ((data: string) => void) | null = null;
let _exitCallback: ((code: number) => void) | null = null;

export const serverManager = {
  async startServer(jarPath: string, javaArgs: string[]): Promise<void> {
    if (!ServerProcessModule) {
      console.warn("ServerProcessModule not available (platform not supported)");
      return;
    }
    await ServerProcessModule.startProcess(jarPath, javaArgs);
  },

  async stopServer(): Promise<void> {
    if (!ServerProcessModule) return;
    await ServerProcessModule.stopProcess();
  },

  async sendCommand(command: string): Promise<void> {
    if (!ServerProcessModule) return;
    await ServerProcessModule.writeStdin(command + "\n");
  },

  async isRunning(): Promise<boolean> {
    if (!ServerProcessModule) return false;
    return await ServerProcessModule.isRunning();
  },

  onStdout(callback: (data: string) => void): void {
    _stdoutCallback = callback;
  },

  onStderr(callback: (data: string) => void): void {
    _stderrCallback = callback;
  },

  onExit(callback: (code: number) => void): void {
    _exitCallback = callback;
  },

  removeListeners(): void {
    _stdoutCallback = null;
    _stderrCallback = null;
    _exitCallback = null;
  },
};

export function handleProcessEvent(type: string, data?: string, code?: number): void {
  switch (type) {
    case "stdout":
      _stdoutCallback?.(data ?? "");
      break;
    case "stderr":
      _stderrCallback?.(data ?? "");
      break;
    case "exit":
      _exitCallback?.(code ?? 0);
      break;
  }
}
