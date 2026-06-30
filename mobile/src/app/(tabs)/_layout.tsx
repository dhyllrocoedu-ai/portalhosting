import { Tabs } from "expo-router";
import { SafeAreaView } from "react-native-safe-area-context";
import { StyleSheet, Text, Platform } from "react-native";
import { colors } from "../../constants/theme";

export default function TabLayout() {
  return (
    <SafeAreaView style={styles.safeArea} edges={Platform.OS === "android" ? ["top", "bottom"] : ["top"]}>
      <Tabs
        screenOptions={{
          headerShown: false,
          tabBarStyle: {
            backgroundColor: colors.surface,
            borderTopColor: colors.surfaceLight,
            borderTopWidth: 1,
            height: 60,
            paddingBottom: 8,
            paddingTop: 8,
          },
          tabBarActiveTintColor: colors.primary,
          tabBarInactiveTintColor: colors.textMuted,
          tabBarLabelStyle: {
            fontSize: 11,
            fontWeight: "600",
          },
        }}
      >
        <Tabs.Screen
          name="dashboard"
          options={{
            title: "Dashboard",
            tabBarIcon: ({ color }) => <Text style={{ fontSize: 22, color }}>◈</Text>,
          }}
        />
        <Tabs.Screen
          name="console"
          options={{
            title: "Console",
            tabBarIcon: ({ color }) => <Text style={{ fontSize: 22, color }}>⌨</Text>,
          }}
        />
        <Tabs.Screen
          name="players"
          options={{
            title: "Players",
            tabBarIcon: ({ color }) => <Text style={{ fontSize: 22, color }}>👤</Text>,
          }}
        />
        <Tabs.Screen
          name="settings"
          options={{
            title: "Settings",
            tabBarIcon: ({ color }) => <Text style={{ fontSize: 22, color }}>⚙</Text>,
          }}
        />
      </Tabs>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: colors.background,
  },
});
