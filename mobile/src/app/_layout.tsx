import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";
import * as SplashScreen from "expo-splash-screen";
import { useEffect } from "react";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { colors } from "../constants/theme";
import { useProcessEvents } from "../hooks/useProcessEvents";
import { useServerStore } from "../stores/serverStore";

SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  useProcessEvents();

  useEffect(() => {
    useServerStore.getState().loadPersistedState();
    SplashScreen.hideAsync();
  }, []);

  return (
    <SafeAreaProvider>
      <StatusBar style="light" />
      <Stack
        screenOptions={{
          headerShown: false,
          contentStyle: { backgroundColor: colors.background },
          animation: "fade",
        }}
      >
        <Stack.Screen name="index" />
        <Stack.Screen name="(tabs)" />
        <Stack.Screen name="server/create" options={{ animation: "slide_from_bottom" }} />
        <Stack.Screen name="files" options={{ animation: "slide_from_right" }} />
      </Stack>
    </SafeAreaProvider>
  );
}
