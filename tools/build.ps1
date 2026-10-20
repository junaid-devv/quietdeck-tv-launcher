param(
    [string]$SdkRoot = "$env:LOCALAPPDATA\Android\sdk",
    [string]$JavaHome = $env:JAVA_HOME,
    [switch]$Release,
    [string]$SigningKey = $env:QUIETDECK_KEYSTORE,
    [string]$KeyAlias = 'quietdeck'
)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
$main = Join-Path $project 'app\src\main'
$work = Join-Path $project '.local'
$build = Join-Path $work (Get-Date -Format 'yyyyMMdd-HHmmss-fff')
$dist = Join-Path $project 'dist'
$tools = Join-Path $SdkRoot 'build-tools\35.0.0'
$android = Join-Path $SdkRoot 'platforms\android-35\android.jar'
$java = Join-Path $JavaHome 'bin\java.exe'
foreach ($required in @($android,$java,(Join-Path $JavaHome 'bin\javac.exe'),(Join-Path $tools 'aapt.exe'))) {
    if (!(Test-Path -LiteralPath $required)) { throw "Missing build tool: $required" }
}
New-Item -ItemType Directory -Force -Path $build,$dist,(Join-Path $build 'classes'),(Join-Path $build 'dex') | Out-Null
function Check-Exit([string]$stage) { if ($LASTEXITCODE -ne 0) { throw "$stage failed ($LASTEXITCODE)" } }
$sources = @(Get-ChildItem (Join-Path $main 'java') -Recurse -Filter '*.java' | ForEach-Object FullName)
$boot = "$android;$(Join-Path $tools 'core-lambda-stubs.jar')"
& (Join-Path $JavaHome 'bin\javac.exe') -source 8 -target 8 -encoding UTF-8 -bootclasspath $boot -d (Join-Path $build 'classes') @sources
Check-Exit 'Java compilation'
# AAPT needs fields that Gradle normally supplies during manifest merging.
[xml]$manifest = Get-Content (Join-Path $main 'AndroidManifest.xml') -Raw
$root = $manifest.DocumentElement
$root.SetAttribute('package','com.still.launcher')
$ns = 'http://schemas.android.com/apk/res/android'
function Android-Attribute($element,[string]$name,[string]$value) {
    $attr = $manifest.CreateAttribute('android',$name,$ns)
    $attr.Value = $value
    $element.Attributes.SetNamedItem($attr) | Out-Null
}
Android-Attribute $root 'versionCode' '3'
Android-Attribute $root 'versionName' '1.1.0'
$sdk = $manifest.CreateElement('uses-sdk')
Android-Attribute $sdk 'minSdkVersion' '23'
Android-Attribute $sdk 'targetSdkVersion' '35'
$root.PrependChild($sdk) | Out-Null
$manifest.Save((Join-Path $build 'AndroidManifest.xml'))
& (Join-Path $tools 'aapt.exe') package -f -M (Join-Path $build 'AndroidManifest.xml') -S (Join-Path $main 'res') -I $android -F (Join-Path $build 'unsigned.apk')
Check-Exit 'Resource packaging'
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory((Join-Path $build 'classes'),(Join-Path $build 'classes.jar'))
& $java -cp (Join-Path $tools 'lib\d8.jar') com.android.tools.r8.D8 --min-api 23 --lib $android --output (Join-Path $build 'dex') (Join-Path $build 'classes.jar')
Check-Exit 'DEX compilation'
$apk = [System.IO.Compression.ZipFile]::Open((Join-Path $build 'unsigned.apk'),[System.IO.Compression.ZipArchiveMode]::Update)
try { [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($apk,(Join-Path $build 'dex\classes.dex'),'classes.dex',[System.IO.Compression.CompressionLevel]::Optimal) | Out-Null }
finally { $apk.Dispose() }
& (Join-Path $tools 'zipalign.exe') -f -p 4 (Join-Path $build 'unsigned.apk') (Join-Path $build 'aligned.apk')
Check-Exit 'Alignment'
if ($Release) {
    if (!$SigningKey -or !(Test-Path -LiteralPath $SigningKey) -or !$env:QUIETDECK_SIGNING_PASSWORD) {
        throw 'Release builds require QUIETDECK_KEYSTORE, QUIETDECK_SIGNING_PASSWORD and the matching -KeyAlias. Keep them outside Git.'
    }
    $password = 'env:QUIETDECK_SIGNING_PASSWORD'
    $output = Join-Path $dist 'QuietDeck-TV-v1.1.0.apk'
} else {
    $SigningKey = Join-Path $work 'development.p12'
    $KeyAlias = 'development'
    $password = 'pass:android'
    if (!(Test-Path -LiteralPath $SigningKey)) {
        & (Join-Path $JavaHome 'bin\keytool.exe') -genkeypair -noprompt -keystore $SigningKey -storetype PKCS12 -storepass android -keypass android -alias $KeyAlias -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=QuietDeck Local Development'
        Check-Exit 'Development key generation'
    }
    $output = Join-Path $dist 'QuietDeck-TV-local.apk'
}
& $java -jar (Join-Path $tools 'lib\apksigner.jar') sign --ks $SigningKey --ks-key-alias $KeyAlias --ks-pass $password --key-pass $password --out $output (Join-Path $build 'aligned.apk')
Check-Exit 'Signing'
& $java -jar (Join-Path $tools 'lib\apksigner.jar') verify --verbose $output
Check-Exit 'Signature verification'
Write-Output "Built: $output"
