#!/bin/sh
# Maven Wrapper simplificado de APOLO (Linux / macOS).
# Descarga Maven la primera vez (necesita internet solo ese día) y lo reutiliza después.
# Si prefieres el wrapper oficial: "mvn wrapper:wrapper -Dmaven=3.9.9" lo reemplaza.
set -eu

MAVEN_VERSION="3.9.9"
BASE_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}"
DIST_DIR="${HOME}/.m2/wrapper/dists/apache-maven-${MAVEN_VERSION}"
MVN_HOME="${DIST_DIR}/apache-maven-${MAVEN_VERSION}"

if [ ! -x "${MVN_HOME}/bin/mvn" ]; then
    echo "Descargando Maven ${MAVEN_VERSION} (solo la primera vez)..."
    mkdir -p "${DIST_DIR}"
    ARCHIVO="${DIST_DIR}/maven.tar.gz"
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL "${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz" -o "${ARCHIVO}"
    elif command -v wget >/dev/null 2>&1; then
        wget -q "${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz" -O "${ARCHIVO}"
    else
        echo "Se necesita curl o wget para descargar Maven." >&2
        exit 1
    fi
    tar -xzf "${ARCHIVO}" -C "${DIST_DIR}"
    rm -f "${ARCHIVO}"
fi

exec "${MVN_HOME}/bin/mvn" "$@"
