param(
  [Parameter(Mandatory=$true)][string]$RepoPath,
  [string]$Branch = "kahanifilm-development"
)
$ErrorActionPreference = "Stop"
$bundleRoot = $PSScriptRoot
$repo = (Resolve-Path $RepoPath).Path
if (-not (Test-Path (Join-Path $repo ".git"))) { throw "RepoPath must point to an existing git clone." }
if ((git -C $repo status --porcelain).Length -gt 0) { throw "Repository has uncommitted changes. Commit or stash them first; no local work will be overwritten." }
$origin = git -C $repo remote get-url origin
if ($origin -notmatch "KahaniFilm-AI-Studio-v45") { throw "Unexpected origin: $origin" }
git -C $repo fetch origin $Branch
git -C $repo checkout $Branch
git -C $repo pull --ff-only origin $Branch
$copyMap = @(
  @{ Source="assets/babu_ke_liye/stills"; Destination="tools/testdata/babu_ke_liye/assets/stills" },
  @{ Source="assets/babu_ke_liye/reference_video"; Destination="tools/testdata/babu_ke_liye/reference_video" },
  @{ Source="assets/babu_ke_liye/reference_video_variants"; Destination="tools/testdata/babu_ke_liye/reference_video_variants" },
  @{ Source="assets/aakhiri_chai/reference_pack"; Destination="tools/testdata/aakhiri_chai/reference_pack" },
  @{ Source="assets/aakhiri_chai/reference_video"; Destination="tools/testdata/aakhiri_chai/reference_video" },
  @{ Source="assets/aakhiri_chai/reference_video_variants"; Destination="tools/testdata/aakhiri_chai/reference_video_variants" }
)
foreach ($m in $copyMap) {
  $src = Join-Path $bundleRoot $m.Source
  $dst = Join-Path $repo $m.Destination
  if (-not (Test-Path $src)) { throw "Missing bundle folder: $src" }
  New-Item -ItemType Directory -Force -Path $dst | Out-Null
  Copy-Item -Path (Join-Path $src "*") -Destination $dst -Force
}
Copy-Item (Join-Path $bundleRoot "asset_inventory.json") (Join-Path $repo "tools/testdata/reference_asset_inventory.json") -Force
git -C $repo add tools/testdata/babu_ke_liye/assets tools/testdata/babu_ke_liye/reference_video tools/testdata/babu_ke_liye/reference_video_variants tools/testdata/aakhiri_chai/reference_pack tools/testdata/aakhiri_chai/reference_video tools/testdata/aakhiri_chai/reference_video_variants tools/testdata/reference_asset_inventory.json
git -C $repo diff --cached --stat
if ((git -C $repo diff --cached --name-only).Length -eq 0) { Write-Host "No asset changes to commit; import already present."; exit 0 }
git -C $repo commit -m "Add source assets and reference videos for both films"
git -C $repo push origin $Branch
Write-Host "Assets pushed to $origin ($Branch)."
