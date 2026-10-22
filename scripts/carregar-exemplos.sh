#!/usr/bin/env bash
# Entra como administrador e envia os três documentos de exemplo.
# Uso: ./scripts/carregar-exemplos.sh   (lê ADMIN_EMAIL e ADMIN_SENHA do arquivo .env)
set -euo pipefail

cd "$(dirname "$0")/.."
URL="${URL:-http://localhost:8080}"

if [ -f .env ]; then
  set -a; source .env; set +a
fi
: "${ADMIN_EMAIL:?defina ADMIN_EMAIL no .env}"
: "${ADMIN_SENHA:?defina ADMIN_SENHA no .env}"

TOKEN=$(curl -sf -X POST "$URL/api/auth/entrar" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"senha\":\"$ADMIN_SENHA\"}" | sed -E 's/.*"token":"([^"]+)".*/\1/')

enviar() {
  echo "Enviando $2..."
  curl -sf -X POST "$URL/api/documentos" \
    -H "Authorization: Bearer $TOKEN" \
    -F "arquivo=@exemplos/$1" \
    -F "titulo=$2" > /dev/null || echo "  (falhou: talvez já tenha sido enviado)"
}

enviar manual-rh.md "Manual de RH"
enviar politica-ti.md "Política de TI"
enviar guia-onboarding.md "Guia de Onboarding"

echo "Pronto. Acompanhe o status em $URL ou com: curl -H \"Authorization: Bearer \$TOKEN\" $URL/api/documentos"
