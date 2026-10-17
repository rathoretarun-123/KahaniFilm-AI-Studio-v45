#!/usr/bin/env bash
# Prepare both committed story asset packs for the APK and deterministic CI fixtures.
set -euo pipefail
cd "$(dirname "$0")/../.."

archives=(
  "Aakhiri_Chai_Assets_Part1.zip"
  "Aakhiri_Chai_Assets_Part2.zip"
  "Babu_Ke_Liye_Assets_Part1.zip"
  "Babu_Ke_Liye_Assets_Part2.zip"
)
for archive in "${archives[@]}"; do
  test -s "$archive" || { echo "::error::Missing committed asset archive: $archive"; exit 1; }
done

ROOT="$(mktemp -d "${TMPDIR:-/tmp}/kahani-film-assets.XXXXXX")"
trap 'rm -rf "$ROOT"' EXIT
for archive in "${archives[@]}"; do
  echo "Validating archive: $archive"
  unzip -tq "$archive"
  unzip -oq "$archive" -d "$ROOT"
done

# Restore legacy test-only fixtures if a committed packet actually contains them.
# Do not synthesize or substitute character art: absent real references must remain
# a visible fixture gap, not be mistaken for user-approved film assets.
for fixture in sheets upload sheets38 sheets40 sample voices; do
  target="tools/testdata/$fixture"
  candidate=""
  while IFS= read -r -d '' found; do
    candidate="$found"
    break
  done < <(find "$ROOT" -type d -name "$fixture" -print0)
  if [ -n "$candidate" ]; then
    mkdir -p "$target"
    cp -an "$candidate/." "$target/"
    echo "Restored fixture directory '$fixture' from committed asset packet: $candidate"
  else
    echo "Fixture directory '$fixture' not found inside the four story packets."
  fi
done

# Reassemble split MP4 payloads only after both ZIP parts are extracted.
while IFS= read -r -d '' first; do
  second="${first%.chunk1}.chunk2"
  output="${first%.chunk1}"
  test -s "$second" || { echo "::error::Missing paired video chunk: $second"; exit 1; }
  cat "$first" "$second" > "$output"
  magic="$(dd if="$output" bs=1 skip=4 count=4 2>/dev/null)"
  test "$magic" = "ftyp" || { echo "::error::Invalid MP4 header for $output ($magic)"; exit 1; }
  echo "Reassembled $(basename "$output"): $(wc -c < "$output") bytes"
done < <(find "$ROOT" -type f -name '*.mp4.chunk1' -print0)

for film in aakhiri_chai babu_ke_liye; do
  src="$ROOT/$film"
  test -d "$src" || { echo "::error::Missing story asset folder: $src"; exit 1; }
  mkdir -p "tools/testdata/$film"
  cp -a "$src/." "tools/testdata/$film/"

  dest="app/src/main/assets/film_library/$film"
  # Merge generated assets without deleting pre-existing film-library additions.
  # Stable packet filenames are updated only in their corresponding managed paths;
  # unrelated/user-added files remain untouched.
  mkdir -p "$dest/stills" "$dest/videos" "$dest/review"

  # Preserve unique original stills as app-bundled, built-in library references.
  count=0
  while IFS= read -r -d '' img; do
    rel="${img#"$src"/}"
    low="/${rel,,}/"
    [[ "$low" == *"/images/"* || "$low" == *"/stills/"* ]] || continue
    base="$(basename "$img")"
    case "$base" in *contact_sheet*|*contact-sheet*|*kahani_contact_sheet*|*montage*) continue ;; esac
    safe="${rel//\//__}"
    cp -p "$img" "$dest/stills/$safe"
    count=$((count + 1))
  done < <(find "$src" -type f \( -iname '*.webp' -o -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.png' \) -print0)
  echo "$film bundled stills: $count"
  if [ "$film" = "aakhiri_chai" ]; then
    test "$count" -ge 60 || { echo "::error::Too few Aakhiri Chai stills imported ($count)"; exit 1; }
  else
    test "$count" -ge 40 || { echo "::error::Too few Babu Ke Liye stills imported ($count)"; exit 1; }
  fi

  # Keep visual contact sheets for review, but never present them as individual cast/set references.
  while IFS= read -r -d '' sheet; do
    cp -p "$sheet" "$dest/review/$(basename "$sheet")"
  done < <(find "$src" -type f \( -iname '*contact*sheet*.jpg' -o -iname '*contact*sheet*.png' -o -iname '*montage*.jpg' \) -print0)

  # Bundle all original/variant MP4s for review, plus the exact phone-uploaded reference at a stable path.
  while IFS= read -r -d '' video; do
    rel="${video#"$src"/}"
    safe="${rel//\//__}"
    cp -p "$video" "$dest/videos/$safe"
  done < <(find "$src" -type f -iname '*.mp4' -print0)

  if [ "$film" = "aakhiri_chai" ]; then
    uploaded="tools/testdata/$film/reference_video/gemini_generated_video_2D53A537.mp4"
  else
    uploaded="tools/testdata/$film/reference_video/gemini_generated_video_7A313517.mp4"
  fi
  test -s "$uploaded" || { echo "::error::Missing complete uploaded reference video: $uploaded"; exit 1; }
  magic="$(dd if="$uploaded" bs=1 skip=4 count=4 2>/dev/null)"
  test "$magic" = "ftyp" || { echo "::error::Uploaded reference is not an MP4: $uploaded"; exit 1; }
  cp -p "$uploaded" "$dest/videos/user_uploaded_reference.mp4"
  cp "tools/testdata/$film/reference_manifest.json" "$dest/manifest.json"

  video_count="$(find "$dest/videos" -maxdepth 1 -type f -iname '*.mp4' | wc -l)"
  echo "$film bundled MP4s: $video_count"
  test "$video_count" -ge 2 || { echo "::error::At least two MP4 references must be bundled for $film"; exit 1; }
done

# Bundle the filmmaker's additional Aakhiri Chitthi reference library. These are
# individual uploads (not a generated contact sheet), kept in source control so
# both the APK and the browsable in-app library receive all committed originals.
chitthi_src="tools/testdata/aakhiri_chitthi/stills"
chitthi_dest="app/src/main/assets/film_library/aakhiri_chitthi/stills"
test -d "$chitthi_src" || { echo "::error::Missing Aakhiri Chitthi reference folder: $chitthi_src"; exit 1; }
mkdir -p "$chitthi_dest"
while IFS= read -r -d '' img; do cp -p "$img" "$chitthi_dest/$(basename "$img")"; done < <(find "$chitthi_src" -type f \( -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.png' -o -iname '*.webp' \) -print0)
chitthi_count="$(find "$chitthi_dest" -maxdepth 1 -type f \( -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.png' -o -iname '*.webp' \) | wc -l)"
test "$chitthi_count" -ge 100 || { echo "::error::Too few Aakhiri Chitthi references bundled ($chitthi_count)"; exit 1; }
echo "aakhiri_chitthi bundled stills: $chitthi_count"

echo "Bundled story asset inventory:"
find app/src/main/assets/film_library -type f -printf '%p %s bytes\n' | sort
