using System;
using System.Diagnostics;
using System.IO;
using System.Threading;

class PortalHostUninstaller
{
    static void Main(string[] args)
    {
        Console.WriteLine("============================================");
        Console.WriteLine("  PortalHost Uninstaller");
        Console.WriteLine("============================================");
        Console.WriteLine();

        // Confirmation
        Console.WriteLine("This will uninstall PortalHost and remove ALL data:");
        Console.WriteLine("  - Server files");
        Console.WriteLine("  - Playit configuration");
        Console.WriteLine("  - All settings and preferences");
        Console.WriteLine();
        Console.Write("Type 'yes' to confirm: ");
        string confirm = Console.ReadLine();

        if (confirm.ToLower() != "yes")
        {
            Console.WriteLine("Cancelled.");
            return;
        }

        Console.WriteLine();
        Console.WriteLine("[1/4] Closing PortalHost...");
        KillProcess("PortalHost");

        Console.WriteLine("[2/4] Finding installation...");
        string uninstallString = FindUninstaller();

        if (!string.IsNullOrEmpty(uninstallString))
        {
            Console.WriteLine("Found: " + uninstallString);
            Console.WriteLine("Running uninstaller...");
            RunUninstaller(uninstallString);
            Thread.Sleep(3000);
        }
        else
        {
            Console.WriteLine("Installation not found in registry.");
        }

        Console.WriteLine("[3/4] Removing app data...");
        string appDataPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
            ".portalhost");

        if (Directory.Exists(appDataPath))
        {
            try
            {
                Directory.Delete(appDataPath, true);
                Console.WriteLine("App data removed: " + appDataPath);
            }
            catch (Exception ex)
            {
                Console.WriteLine("Could not remove app data: " + ex.Message);
            }
        }
        else
        {
            Console.WriteLine("No app data found.");
        }

        Console.WriteLine("[4/4] Cleaning installation folder...");
        string[] installPaths = new string[]
        {
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "PortalHost"),
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86), "PortalHost")
        };

        foreach (string path in installPaths)
        {
            if (Directory.Exists(path))
            {
                try
                {
                    Directory.Delete(path, true);
                    Console.WriteLine("Removed: " + path);
                }
                catch (Exception ex)
                {
                    Console.WriteLine("Could not remove " + path + ": " + ex.Message);
                }
            }
        }

        Console.WriteLine();
        Console.WriteLine("============================================");
        Console.WriteLine("Uninstall complete!");
        Console.WriteLine("Press Enter to exit...");
        Console.ReadLine();
    }

    static void KillProcess(string name)
    {
        try
        {
            foreach (var proc in Process.GetProcessesByName(name))
            {
                proc.Kill();
                Console.WriteLine("Killed: " + proc.ProcessName);
            }
        }
        catch { }
    }

    static string FindUninstaller()
    {
        // Search registry for PortalHost uninstaller
        string[] regPaths = new string[]
        {
            @"SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall",
            @"SOFTWARE\Wow6432Node\Microsoft\Windows\CurrentVersion\Uninstall"
        };

        foreach (string basePath in regPaths)
        {
            try
            {
                using (var key = Microsoft.Win32.Registry.LocalMachine.OpenSubKey(basePath))
                {
                    if (key != null)
                    {
                        foreach (string subKeyName in key.GetSubKeyNames())
                        {
                            using (var subKey = key.OpenSubKey(subKeyName))
                            {
                                var displayName = subKey?.GetValue("DisplayName");
                                if (displayName != null && displayName.ToString().Contains("PortalHost"))
                                {
                                    var uninstallString = subKey?.GetValue("UninstallString");
                                    if (uninstallString != null)
                                        return uninstallString.ToString();
                                }
                            }
                        }
                    }
                }
            }
            catch { }

            try
            {
                using (var key = Microsoft.Win32.Registry.CurrentUser.OpenSubKey(basePath))
                {
                    if (key != null)
                    {
                        foreach (string subKeyName in key.GetSubKeyNames())
                        {
                            using (var subKey = key.OpenSubKey(subKeyName))
                            {
                                var displayName = subKey?.GetValue("DisplayName");
                                if (displayName != null && displayName.ToString().Contains("PortalHost"))
                                {
                                    var uninstallString = subKey?.GetValue("UninstallString");
                                    if (uninstallString != null)
                                        return uninstallString.ToString();
                                }
                            }
                        }
                    }
                }
            }
            catch { }
        }

        return null;
    }

    static void RunUninstaller(string uninstallString)
    {
        try
        {
            // Handle both MSI and EXE uninstallers
            if (uninstallString.Contains("msiexec"))
            {
                // Extract MSI product code from command
                var parts = uninstallString.Split(' ');
                foreach (var part in parts)
                {
                    if (part.StartsWith("{") && part.EndsWith("}"))
                    {
                        Process.Start(new ProcessStartInfo
                        {
                            FileName = "msiexec",
                            Arguments = "/x " + part + " /qn",
                            UseShellExecute = true
                        }).WaitForExit();
                        return;
                    }
                }
            }
            else
            {
                Process.Start(new ProcessStartInfo
                {
                    FileName = uninstallString.Trim('"').Split(' ')[0],
                    Arguments = uninstallString.Contains(' ') ? uninstallString.Substring(uninstallString.IndexOf(' ') + 1) : "",
                    UseShellExecute = true
                }).WaitForExit();
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine("Uninstall error: " + ex.Message);
        }
    }
}