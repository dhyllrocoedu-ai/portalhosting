Set msi = CreateObject("WindowsInstaller.Installer")
Set db = msi.OpenDatabase("D:\mydevprojects\portalhosting\composeApp\build\compose\binaries\main-release\msi\PortalHost-5.0.66.msi", 0)

' ProductVersion from Property table
Set view = db.OpenView("SELECT `Value` FROM `Property` WHERE `Property` = 'ProductVersion'")
view.Execute
Set rec = view.Fetch
If Not rec Is Nothing Then
    WScript.Echo "ProductVersion: " & rec.StringData(1)
Else
    WScript.Echo "ProductVersion: not found"
End If
view.Close

' Also check ProductCode
Set view2 = db.OpenView("SELECT `Value` FROM `Property` WHERE `Property` = 'ProductCode'")
view2.Execute
Set rec2 = view2.Fetch
If Not rec2 Is Nothing Then
    WScript.Echo "ProductCode: " & rec2.StringData(1)
Else
    WScript.Echo "ProductCode: not found"
End If
view2.Close

' Check WixRemoveFolderEx table
Set view3 = db.OpenView("SELECT * FROM `WixRemoveFolderEx`")
view3.Execute
Set rec3 = view3.Fetch
Do While Not rec3 Is Nothing
    WScript.Echo "WixRemoveFolderEx: " & rec3.StringData(1) & ", " & rec3.StringData(2) & ", " & rec3.StringData(3)
    Set rec3 = view3.Fetch
Loop
view3.Close

' Check InstallExecuteSequence for WixRemoveFolderEx action
Set view4 = db.OpenView("SELECT `Action` FROM `InstallExecuteSequence` WHERE `Action` LIKE '%WixRemoveFolderEx%'")
view4.Execute
Set rec4 = view4.Fetch
Do While Not rec4 Is Nothing
    WScript.Echo "IES WixRemoveFolderEx: " & rec4.StringData(1)
    Set rec4 = view4.Fetch
Loop
view4.Close

' INSTALLDIR property
Set view5 = db.OpenView("SELECT `Value` FROM `Property` WHERE `Property` = 'INSTALLDIR'")
view5.Execute
Set rec5 = view5.Fetch
If Not rec5 Is Nothing Then
    WScript.Echo "INSTALLDIR: " & rec5.StringData(1)
Else
    WScript.Echo "INSTALLDIR: not found"
End If
view5.Close

' CustomAction for WixRemoveFolderEx
Set view6 = db.OpenView("SELECT `Action`, `Type`, `Source`, `Target` FROM `CustomAction` WHERE `Action` LIKE '%WixRemoveFolderEx%'")
view6.Execute
Set rec6 = view6.Fetch
Do While Not rec6 Is Nothing
    WScript.Echo "CA: " & rec6.StringData(1) & ", " & rec6.StringData(2) & ", " & rec6.StringData(3) & ", " & rec6.StringData(4)
    Set rec6 = view6.Fetch
Loop
view6.Close