#!/bin/bash
set -e

# --- Sécurité locale nombres décimaux ---
export LC_ALL=C

# --- Config par défaut (surchargeables en variables d'env) ---
BASENAME="${BASENAME:-convertigo}"
IMG_DIR="${IMG_DIR:-.}"           # ex: "." si le script est dans images/
ICONSET="${BASENAME}.iconset"

RADIUS_PCT="${RADIUS_PCT:-0.22}"  # 22% style macOS ; ex: 0.20
CROP_PCT="${CROP_PCT:-0.02}"      # recadre (zoom) pour enlever anciens coins ; ex: 0.06
ADD_SHADOW="${ADD_SHADOW:-1}"     # 0 = pas d’ombre ; 1 = ombre
SHADOW_SPEC="${SHADOW_SPEC:-25x2+0+2}"  # opacité% x blur + dx + dy

# --- Source PNG (on prend la meilleure) ---
for CAND in "${IMG_DIR}/${BASENAME}_1024x1024_32.png" \
            "${IMG_DIR}/${BASENAME}_512x512_32.png" \
            "${IMG_DIR}/${BASENAME}_256x256_32.png" ; do
  [ -f "$CAND" ] && SRC="$CAND" && break
done
[ -z "$SRC" ] && echo "❌ Source introuvable (${IMG_DIR}/${BASENAME}_1024x1024_32.png | 512 | 256)" && exit 1

command -v magick >/dev/null || { echo "❌ ImageMagick v7 requis (commande 'magick')"; exit 1; }
command -v iconutil >/dev/null || { echo "❌ 'iconutil' (macOS) requis"; exit 1; }

rm -rf "$ICONSET"; mkdir "$ICONSET"
TMP="$(mktemp -d -t macicon.XXXXXX)"

round_and_style () {
  local size="$1" out="$2"

  # Rayon d’arrondi en px
  local r ; r=$(awk -v s="$size" -v p="$RADIUS_PCT" 'BEGIN{printf("%d", s*p)}')
  # Taille intérieure recadrée en px (on coupe CROP_PCT de chaque côté puis on ZOOM au plein format)
  local inner ; inner=$(awk -v s="$size" -v c="$CROP_PCT" 'BEGIN{printf("%d", s*(1-2*c))}')

  # 1) Mise à l’échelle + colorimétrie figée (évite shift)
  magick "$SRC" -filter Lanczos -resize "${size}x${size}" \
    -colorspace sRGB -set colorspace sRGB -strip -define png:color-type=6 \
    PNG32:"$TMP/base_full.png"

  # 2) CROP centré puis ZOOM pour remplir à 100% la zone (élimine anciens coins)
  magick "$TMP/base_full.png" \
    -gravity center -crop "${inner}x${inner}+0+0" +repage \
    -resize "${size}x${size}!" \
    PNG32:"$TMP/base.png"

  # 3) Masque arrondi
  magick -size "${size}x${size}" xc:none \
    -fill white -draw "roundrectangle 0,0 $((size-1)),$((size-1)) $r,$r" \
    PNG32:"$TMP/mask.png"

  # 4) Application alpha
  magick "$TMP/base.png" "$TMP/mask.png" -alpha off \
    -compose CopyOpacity -composite PNG32:"$TMP/rounded.png"

	# 5) Ombre optionnelle — propre (sous l’icône, pas de voile)
	if [ "$ADD_SHADOW" -eq 1 ]; then
	  magick "$TMP/rounded.png" \
	    \( +clone -alpha extract -background black -shadow "$SHADOW_SPEC" \) \
	    -compose DstOver -composite -background none +repage PNG32:"$out"
	else
	  cp "$TMP/rounded.png" "$out"
	fi
}

gen_size () {
  local s="$1" n="$2" n2="$3"
  round_and_style "$s" "$ICONSET/$n"
  [ -n "$n2" ] && magick "$ICONSET/$n" -resize $((s*2))x$((s*2)) "$ICONSET/$n2"
}

# Tailles Apple .iconset
gen_size 16  "icon_16x16.png"       "icon_16x16@2x.png"
gen_size 32  "icon_32x32.png"       "icon_32x32@2x.png"
gen_size 128 "icon_128x128.png"     "icon_128x128@2x.png"
gen_size 256 "icon_256x256.png"     "icon_256x256@2x.png"
gen_size 512 "icon_512x512.png"     "icon_512x512@2x.png"
# 1024 est équivalent à 512@2x (déjà généré)

iconutil -c icns "$ICONSET"
mv "${BASENAME}.icns" "${IMG_DIR}/${BASENAME}.icns"

# Log lisible (sans dépendre de la locale)
RAD_PCT_PCT=$(awk -v p="$RADIUS_PCT" 'BEGIN{printf("%d", p*100+0.5)}')
CROP_PCT_PCT=$(awk -v c="$CROP_PCT" 'BEGIN{printf("%d", c*100+0.5)}')
echo "✅ ${IMG_DIR}/${BASENAME}.icns créé (rayon=${RAD_PCT_PCT}% ; crop=${CROP_PCT_PCT}%)"

rm -rf "$TMP" "$ICONSET"
