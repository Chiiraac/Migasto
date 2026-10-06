#!/usr/bin/env bash
# Publica en Firebase las reglas de seguridad y la Cloud Function de avisos de MiGasto.
# Pensado para Cloud Shell (consola de Firebase → icono >_), con el proyecto ya en el plan Blaze:
#   cd ~ && rm -rf Migasto && git clone -q -b claude/android-app-apk-playstore-ol8yi9 \
#     https://github.com/Chiiraac/Migasto.git && bash Migasto/tools/desplegar-firebase.sh
set -euo pipefail

PROJECT="${1:-migasto-chiiraac}"
cd "$(dirname "$0")/.."
firebase() { npx --yes firebase-tools@15 "$@"; }

echo "▶ Instalando las dependencias de la función…"
(cd functions && npm ci --silent --no-audit --no-fund)

echo "▶ Comprobando el acceso a Firebase…"
if ! firebase projects:list >/dev/null 2>&1; then
  echo
  echo "  Hace falta iniciar sesión una vez: abre el enlace que aparece, entra con tu cuenta de"
  echo "  Google y pega aquí el código del final (no se lo pases a nadie)."
  echo
  firebase login --no-localhost
fi

# La primera vez Google tarda unos minutos en preparar los permisos internos (Eventarc,
# Cloud Build…): si falla, se espera y se reintenta.
for attempt in 1 2 3 4; do
  echo "▶ Publicando reglas y función (intento $attempt de 4)…"
  if firebase deploy --only firestore:rules,functions --project "$PROJECT" --force; then
    echo
    echo "✅ Listo: las reglas y los avisos de MiGasto están publicados en $PROJECT."
    exit 0
  fi
  if [ "$attempt" -lt 4 ]; then
    echo "  Ha fallado (es normal la primera vez). Reintento en 90 segundos…"
    sleep 90
  fi
done

echo
echo "❌ No se pudo publicar. Revisa que el proyecto está en el plan Blaze"
echo "   (consola de Firebase → abajo a la izquierda) y manda una captura del error."
exit 1
