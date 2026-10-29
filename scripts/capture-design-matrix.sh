#!/usr/bin/env bash
set -euo pipefail

: "${ANDROID_SERIAL:?set ANDROID_SERIAL to one adb device}"
package="${PACKAGE_NAME:-me.yuki.folk}"
out_dir="${1:-/tmp/folk-design-matrix}"
temporary="$(mktemp -d)"
prefs_backup="$temporary/config.xml"
font_scale="$(adb -s "$ANDROID_SERIAL" shell settings get system font_scale | tr -d '\r')"

restore_device() {
    adb -s "$ANDROID_SERIAL" shell wm size reset >/dev/null 2>&1 || true
    adb -s "$ANDROID_SERIAL" shell wm density reset >/dev/null 2>&1 || true
    adb -s "$ANDROID_SERIAL" shell settings put system font_scale "${font_scale:-1.0}" >/dev/null 2>&1 || true
    if [[ -s "$prefs_backup" ]]; then
        adb -s "$ANDROID_SERIAL" push "$prefs_backup" /data/local/tmp/folk-config.xml >/dev/null
        adb -s "$ANDROID_SERIAL" shell run-as "$package" cp /data/local/tmp/folk-config.xml shared_prefs/config.xml
    fi
    adb -s "$ANDROID_SERIAL" shell am force-stop "$package" >/dev/null 2>&1 || true
    rm -rf "$temporary"
}
trap restore_device EXIT

adb -s "$ANDROID_SERIAL" exec-out run-as "$package" cat shared_prefs/config.xml > "$prefs_backup"
mkdir -p "$out_dir"

capture() {
    local name="$1"
    local raw="$temporary/screen.png"
    adb -s "$ANDROID_SERIAL" shell am force-stop "$package"
    adb -s "$ANDROID_SERIAL" shell am start -W \
        -n "$package/me.bmax.apatch.ui.MainActivityAliasAnime" >/dev/null
    sleep 5
    adb -s "$ANDROID_SERIAL" shell uiautomator dump /sdcard/folk-matrix.xml >/dev/null
    adb -s "$ANDROID_SERIAL" pull /sdcard/folk-matrix.xml "$temporary/ui.xml" >/dev/null
    if rg -q 'text="Skip"' "$temporary/ui.xml"; then
        read -r x y < <(python3 - "$temporary/ui.xml" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
for node in root.iter("node"):
    if node.attrib.get("text") != "Skip":
        continue
    left, top, right, bottom = map(int, re.findall(r"\d+", node.attrib["bounds"]))
    print((left + right) // 2, (top + bottom) // 2)
    break
PY
        )
        adb -s "$ANDROID_SERIAL" shell input tap "$x" "$y"
        sleep 2
        adb -s "$ANDROID_SERIAL" shell uiautomator dump /sdcard/folk-matrix.xml >/dev/null
        adb -s "$ANDROID_SERIAL" pull /sdcard/folk-matrix.xml "$temporary/ui.xml" >/dev/null
    fi
    rg -q 'Manager version|设置' "$temporary/ui.xml"
    adb -s "$ANDROID_SERIAL" exec-out screencap -p > "$raw"
    magick "$raw" -resize 420x -quality 82 "$out_dir/$name.webp"
}

write_prefs() {
    local dark="$1"
    sed -e 's/name="night_mode_follow_sys" value="true"/name="night_mode_follow_sys" value="false"/' \
        -e "s/name=\"night_mode_enabled\" value=\"[^\"]*\"/name=\"night_mode_enabled\" value=\"$dark\"/" \
        "$prefs_backup" > "$temporary/config.xml"
    adb -s "$ANDROID_SERIAL" push "$temporary/config.xml" /data/local/tmp/folk-config.xml >/dev/null
    adb -s "$ANDROID_SERIAL" shell run-as "$package" cp /data/local/tmp/folk-config.xml shared_prefs/config.xml
}

profiles=(
    "compact:720x1280:320"
    "phone:1080x2340:440"
    "tablet:1600x2560:320"
)
themes=("light:false" "dark:true")
fonts=("normal:1.0" "large:1.3")

for profile in "${profiles[@]}"; do
    IFS=: read -r profile_name size density <<< "$profile"
    adb -s "$ANDROID_SERIAL" shell wm size "$size"
    adb -s "$ANDROID_SERIAL" shell wm density "$density"
    for theme in "${themes[@]}"; do
        IFS=: read -r theme_name dark <<< "$theme"
        write_prefs "$dark"
        for font in "${fonts[@]}"; do
            IFS=: read -r font_name scale <<< "$font"
            adb -s "$ANDROID_SERIAL" shell settings put system font_scale "$scale"
            capture "$profile_name-$theme_name-$font_name"
        done
    done
done

echo "captured 12 screenshots in $out_dir"
