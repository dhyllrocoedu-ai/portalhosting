# Builds the stub libraries for the Termux OpenJDK runtime.
# Requires the Android NDK (aarch64 toolchain for API 35).
$ErrorActionPreference = "Stop"

$ndk = "C:\Users\dreeb\AppData\Local\Android\Sdk\ndk\27.1.12297006"
$bin = Join-Path $ndk "toolchains\llvm\prebuilt\windows-x86_64\bin"
$cc = Join-Path $bin "aarch64-linux-android35-clang.cmd"
$root = $PSScriptRoot

if (-not (Test-Path $cc)) {
    Write-Error "NDK compiler not found: $cc"
}

$targets = @(
    @{ src = "$root\libandroid-shmem\shmem.c";   out = "$root\libandroid-shmem\libandroid-shmem.so" },
    @{ src = "$root\libandroid-spawn\stub.c";    out = "$root\libandroid-spawn\libandroid-spawn.so" }
)

foreach ($t in $targets) {
    $args = @('-shared', '-fPIC', '-O2', '-Wl,-z,noexecstack', '-o', $t.out, $t.src)
    $argLine = ($args | ForEach-Object { if ($_ -match '\s') { "`"$_`"" } else { $_ } }) -join ' '
    & cmd /c "`"$cc`" $argLine"
    if ($LASTEXITCODE -ne 0) { Write-Error "Build failed for $($t.src)" }
    $fi = Get-Item $t.out
    Write-Host "Built $($fi.FullName) ($($fi.Length) bytes)"
}
