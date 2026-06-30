import { NativeModules, NativeEventEmitter } from "react-native";

const { ServerProcessModule } = NativeModules as {
  ServerProcessModule?: {
    startProcess: (jarPath: string, args: string[]) => Promise<void>;
    stopProcess: () => Promise<void>;
    writeStdin: (command: string) => Promise<void>;
    isRunning: () => Promise<boolean>;
  };
};

const hasModule = !!ServerProcessModule;

let eventEmitter: NativeEventEmitter | null = null;
if (hasModule) {
  eventEmitter = new NativeEventEmitter(ServerProcessModule as any);
}

let stdoutSub: any = null;
let stderrSub: any = null;
let exitSub: any = null;

function stripFileScheme(path: string): string {
  return path.replace(/^file:\/\//, "");
}

export const serverManager = {
  async startServer(jarPath: string, javaPath: string, javaArgs: string[]): Promise<void> {
    if (!hasModule || !ServerProcessModule) {
      throw new Error("ServerProcessModule not available (platform not supported)");
    }
    await ServerProcessModule.startProcess(stripFileScheme(jarPath), [javaPath, ...javaArgs]);
  },

  async stopServer(): Promise<void> {
    if (!hasModule || !ServerProcessModule) return;
    await ServerProcessModule.stopProcess();
  },

  async sendCommand(command: string): Promise<void> {
    if (!hasModule || !ServerProcessModule) return;
    await ServerProcessModule.writeStdin(command + "\n");
  },

  async isRunning(): Promise<boolean> {
    if (!hasModule || !ServerProcessModule) return false;
    return ServerProcessModule.isRunning();
  },

  onStdout(callback: (data: string) => void): void {
    stdoutSub?.remove();
    if (eventEmitter) stdoutSub = eventEmitter.addListener("onStdout", callback);
  },

  onStderr(callback: (data: string) => void): void {
    stderrSub?.remove();
    if (eventEmitter) stderrSub = eventEmitter.addListener("onStderr", callback);
  },

  onExit(callback: (code: number) => void): void {
    exitSub?.remove();
    if (eventEmitter) exitSub = eventEmitter.addListener("onExit", callback);
  },

  removeListeners(): void {
    stdoutSub?.remove();
    stderrSub?.remove();
    exitSub?.remove();
    stdoutSub = null;
    stderrSub = null;
    exitSub = null;
  },
};
