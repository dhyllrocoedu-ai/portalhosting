$msi = New-Object -ComObject WindowsInstaller.Installer
$db = $msi.OpenDatabase("D:\mydevprojects\portalhosting\composeApp\build\compose\binaries\main-release\msi\PortalHost-5.0.66.msi", 0)

# Get version
$q = $db.OpenView("SELECT `Version` FROM `ProductVersion`")
$q.Execute()
$r = $q.Fetch()
if ($r -ne $null) {
    $r.StringData(1)
} else {
    "no row"
}
$q.Close()

# Check for WixRemoveFoldersEx in InstallExecuteSequence
$q2 = $db.OpenView("SELECT `Action` FROM `InstallExecuteSequence` WHERE `Action` LIKE '%WixRemoveFoldersEx%'")
$q2.Execute()
$r2 = $q2.Fetch()
while ($r2 -ne $null) {
    $r2.StringData(1)
    $r2 = $q2.Fetch()
}
$q2.Close()

# INSTALLDIR property
$q3 = $db.OpenView("SELECT `Value` FROM `Property` WHERE `Property` = 'INSTALLDIR'")
$q3.Execute()
$r3 = $q3.Fetch()
if ($r3 -ne $null) {
    $r3.StringData(1)
} else {
    "no INSTALLDIR"
}
$q3.Close()

# CustomAction for WixRemoveFoldersEx
$q4 = $db.OpenView("SELECT `Action`, `Type`, `Source`, `Target` FROM `CustomAction` WHERE `Action` LIKE '%WixRemoveFoldersEx%'")
$q4.Execute()
$r4 = $q4.Fetch()
while ($r4 -ne $null) {
    "Action: " + $r4.StringData(1) + ", Type: " + $r4.StringData(2) + ", Source: " + $r4.StringData(3) + ", Target: " + $r4.StringData(4)
    $r4 = $q4.Fetch()
}
$q4.Close()