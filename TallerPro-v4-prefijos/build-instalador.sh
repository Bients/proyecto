#!/usr/bin/env bash
# Genera el instalador en macOS (.dmg) o Linux (.deb). Requiere JDK 17+.
set -e
mvn clean verify
VERSION="$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout -Dstyle.color=never)"
JAR="taller-pro-$VERSION.jar"
mkdir -p target/jpackage-input
cp "target/$JAR" target/jpackage-input/
case "$(uname)" in
  Darwin) TIPO=dmg ;;
  *)      TIPO=deb ;;
esac
jpackage --type "$TIPO" --name "TallerPro" --app-version "$VERSION" \
  --input target/jpackage-input --main-jar "$JAR" --dest target/instalador
echo "Instalador generado en la carpeta target/instalador/"
