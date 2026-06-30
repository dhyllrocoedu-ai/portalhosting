import { useEffect, useRef } from "react";
import { serverManager } from "../lib/serverManager";
import { useConsoleStore } from "../stores/consoleStore";
import { useServerStore } from "../stores/serverStore";
import { usePlayerStore } from "../stores/playerStore";
import { useMetricsStore } from "../stores/metricsStore";

const playerJoinRe = /\[.*\]: (\w+) joined the game/;
const playerLeaveRe = /\[.*\]: (\w+) left the game/;

export function useProcessEvents() {
  const mounted = useRef(true);
  const uptimeInterval = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    mounted.current = true;

    const startUptimeCounter = () => {
      uptimeInterval.current = setInterval(() => {
        const store = useServerStore.getState();
        if (store.status === "online") {
          store.setUptime(store.uptime + 1);
        }
      }, 1000);
    };

    const stopUptimeCounter = () => {
      if (uptimeInterval.current) {
        clearInterval(uptimeInterval.current);
        uptimeInterval.current = null;
      }
    };

    serverManager.onStdout((data) => {
      if (!mounted.current) return;

      const level = data.startsWith("[ERROR]") || data.startsWith("[FATAL]")
        ? "error" as const
        : data.startsWith("[WARN]")
          ? "warn" as const
          : "info" as const;

      useConsoleStore.getState().addLog({
        id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        timestamp: Date.now(),
        message: data,
        level,
      });

      const joinMatch = data.match(playerJoinRe);
      if (joinMatch) {
        const name = joinMatch[1];
        const players = usePlayerStore.getState();
        players.setOnline([...players.online, { name, uuid: `${name}-${Date.now()}`, online: true, op: false }]);
        useServerStore.getState().setPlayerCount(players.online.length + 1, useServerStore.getState().maxPlayers);
        return;
      }

      const leaveMatch = data.match(playerLeaveRe);
      if (leaveMatch) {
        const name = leaveMatch[1];
        const players = usePlayerStore.getState();
        players.setOnline(players.online.filter((p) => p.name !== name));
        useServerStore.getState().setPlayerCount(players.online.length - 1, useServerStore.getState().maxPlayers);
        return;
      }
    });

    serverManager.onExit((_code) => {
      if (!mounted.current) return;
      stopUptimeCounter();
      useServerStore.getState().setStatus("offline");
      useServerStore.getState().setUptime(0);
      usePlayerStore.getState().setOnline([]);
      useMetricsStore.getState().setCpu(0);
      useMetricsStore.getState().setRam(0, 0);
      useMetricsStore.getState().setTps(0);
    });

    const unsubServer = useServerStore.subscribe((state, prev) => {
      if (state.status === "online" && prev.status !== "online") {
        startUptimeCounter();
      } else if (state.status !== "online" && prev.status === "online") {
        stopUptimeCounter();
      }
    });

    return () => {
      mounted.current = false;
      stopUptimeCounter();
      unsubServer();
      serverManager.removeListeners();
    };
  }, []);
}
