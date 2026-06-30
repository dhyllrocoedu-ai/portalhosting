import { requireNativeModule } from "expo-modules-core";
import type { EventSubscription } from "expo-modules-core";

const NativeModule = requireNativeModule("ServerProcessModule") as any;

export function startServerProcess(jarPath: string, args: string[]): Promise<void> {
  return NativeModule.startProcess(jarPath, args);
}

export function stopServerProcess(): Promise<void> {
  return NativeModule.stopProcess();
}

export function writeStdin(command: string): Promise<void> {
  return NativeModule.writeStdin(command);
}

export function isServerRunning(): Promise<boolean> {
  return NativeModule.isRunning();
}

export function addStdoutListener(cb: (data: string) => void): EventSubscription {
  return NativeModule.addListener("onStdout", cb);
}

export function addExitListener(cb: (code: number) => void): EventSubscription {
  return NativeModule.addListener("onExit", cb);
}
