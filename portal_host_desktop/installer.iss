; Inno Setup script for Portal Host Desktop
; Compile with: iscc installer.iss
; Requires: Inno Setup 6+

#define MyAppName "Portal Host"
#define MyAppVersion "4.2.5"
#define MyAppPublisher "dhyllrocoedu-ai"
#define MyAppURL "https://github.com/dhyllrocoedu-ai/portalhosting"
#define MyAppExeName "portal_host_desktop.exe"
#define BuildDir "build\windows\x64\runner\Release"

[Setup]
AppId={{A3F8B2C1-9D4E-4F6A-8B7C-3E5F1D2A4B8C}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={autopf}\{#MyAppName}
DisableDirPage=yes
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
OutputDir=.
OutputBaseFilename=PortalHost-{#MyAppVersion}-Setup
SetupIconFile={#BuildDir}\data\flutter_assets\assets\icons\portal_host_icon.ico
Compression=lzma
SolidCompression=yes
WizardStyle=modern
ArchitecturesInstallIn64BitMode=x64
ArchitecturesAllowed=x64
PrivilegesRequired=lowest
UninstallDisplayIcon={app}\{#MyAppExeName}

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked
Name: "startup"; Description: "Start Portal Host on Windows startup"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "{#BuildDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{group}\Uninstall {#MyAppName}"; Filename: "{uninstallexe}"
Name: "{commondesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Registry]
; Add to Windows startup if user selected the task
Root: HKCU; Subkey: "Software\Microsoft\Windows\CurrentVersion\Run"; ValueType: string; ValueName: "PortalHost"; ValueData: """{app}\{#MyAppExeName}"""; Flags: uninsdeletevalue; Tasks: startup

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: filesandordirs; Name: "{app}"

[Code]
; Check if running on Windows 10+
function InitializeSetup(): Boolean;
begin
  Result := (GetWindowsVersion >= $0A000000); // Windows 10+
  if not Result then
    MsgBox('Portal Host requires Windows 10 or later.', mbError, MB_OK);
end;