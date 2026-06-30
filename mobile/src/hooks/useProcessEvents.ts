import { useEffect, useRef } from "react";
import { serverManager } from "../lib/serverManager";
import { useConsoleStore } from "../stores/consoleStore";
import { useServerStore } from "../stores/serverStore";

export function useProcessEvents() {
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;

    serverManager.onStdout((data) => {
      if (!mounted.current) return;
      useConsoleStore.getState().addLog({
        id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        timestamp: Date.now(),
        message: data,
        level: "info",
      });
    });

    serverManager.onStderr((data) => {
      if (!mounted.current) return;
      useConsoleStore.getState().addLog({
        id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        timestamp: Date.now(),
        message: data,
        level: "error",
      });
    });

    serverManager.onExit((_code) => {
      if (!mounted.current) return;
      useServerStore.getState().setStatus("offline");
    });

    return () => {
      mounted.current = false;
      serverManager.removeListeners();
    };
  }, []);
}
