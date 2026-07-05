param(
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$repoDir = "$env:TEMP\playit-agent"
$assetDir = "app\src\main\assets"

# Clone if not exists
if (-not (Test-Path $repoDir)) {
    git clone --depth 1 https://github.com/playit-cloud/playit-agent.git $repoDir
} elseif ($Force) {
    Set-Location $repoDir
    git pull --ff-only
}

# Patch playit-cli's build.rs for cross-compilation
$buildRs = "$repoDir\packages\playit-cli\build.rs"
$content = Get-Content $buildRs -Raw
if ($content -notmatch "CARGO_CFG_TARGET_OS") {
    Set-Content $buildRs @"
fn main() {
    let target_os = std::env::var("CARGO_CFG_TARGET_OS").unwrap_or_default();
    if cfg!(windows) && target_os == "windows" {
        let mut res = winres::WindowsResource::new();
        res.set_icon("wix/Product.ico");
        res.compile().unwrap();
    }
}
"@
}

# Set NDK paths (use API 34 for broader compatibility)
$ndkBin = "$env:LOCALAPPDATA\Android\Sdk\ndk\27.1.12297006\toolchains\llvm\prebuilt\windows-x86_64\bin"
$env:Path += ";$env:USERPROFILE\.cargo\bin;$ndkBin"
$env:CC_aarch64_linux_android = "$ndkBin\aarch64-linux-android34-clang.cmd"
$env:CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER = "$ndkBin\aarch64-linux-android34-clang.cmd"
$env:AR_aarch64_linux_android = "$ndkBin\llvm-ar.exe"

# Build for Android
Set-Location $repoDir
cargo build --target aarch64-linux-android --release --package playitd --package playit-cli

# Copy to project
Copy-Item "target\aarch64-linux-android\release\playitd" "$assetDir\playitd-android" -Force
Copy-Item "target\aarch64-linux-android\release\playit-cli" "$assetDir\playit-cli-android" -Force
Write-Host "playitd: $((Get-Item "$assetDir\playitd-android").Length) bytes"
Write-Host "playit-cli: $((Get-Item "$assetDir\playit-cli-android").Length) bytes"
Set-Location $PSScriptRoot
