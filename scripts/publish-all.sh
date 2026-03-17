#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
MODE="central"
SKIP_PLUGIN_PORTAL=0
TARGET_VERSION=""

usage() {
  cat <<'USAGE'
Usage: ./scripts/publish-all.sh [options]

Options:
  --local                 Publish all artifacts to Maven Local instead of remote registries.
  --version <version>     Update the root project and build-logic version before publishing.
  --skip-plugin-portal    Skip publishing libs/resource/gradle-plugin to the Gradle Plugin Portal.
  -h, --help              Show this help.

Remote publish requirements:
  Maven Central:
    ORG_GRADLE_PROJECT_mavenCentralUsername
    ORG_GRADLE_PROJECT_mavenCentralPassword
    ORG_GRADLE_PROJECT_signingInMemoryKey
    ORG_GRADLE_PROJECT_signingInMemoryKeyPassword

  Gradle Plugin Portal:
    GRADLE_PUBLISH_KEY
    GRADLE_PUBLISH_SECRET
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --local)
      MODE="local"
      shift
      ;;
    --version)
      TARGET_VERSION="${2:-}"
      if [[ -z "$TARGET_VERSION" ]]; then
        echo "--version requires a value" >&2
        exit 1
      fi
      shift 2
      ;;
    --skip-plugin-portal)
      SKIP_PLUGIN_PORTAL=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

set_property() {
  local file="$1"
  local key="$2"
  local value="$3"
  sed -i.bak -E "s/^${key}=.*/${key}=${value}/" "$file"
  rm -f "$file.bak"
}

current_property() {
  local file="$1"
  local key="$2"
  sed -n "s/^${key}=//p" "$file"
}

require_env() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: $name" >&2
    exit 1
  fi
}

run_gradle() {
  echo
  echo "==> ./gradlew $*"
  (
    cd "$ROOT_DIR"
    ./gradlew "$@"
  )
}

run_plugins_gradle() {
  echo
  echo "==> ./gradlew -p plugins $*"
  (
    cd "$ROOT_DIR"
    ./gradlew -p plugins "$@"
  )
}

if [[ -n "$TARGET_VERSION" ]]; then
  set_property "$ROOT_DIR/gradle.properties" "version" "$TARGET_VERSION"
  set_property "$ROOT_DIR/gradle.properties" "VERSION_NAME" "$TARGET_VERSION"
  set_property "$ROOT_DIR/plugins/gradle.properties" "version" "$TARGET_VERSION"
fi

ROOT_VERSION=$(current_property "$ROOT_DIR/gradle.properties" "version")
ROOT_VERSION_NAME=$(current_property "$ROOT_DIR/gradle.properties" "VERSION_NAME")
PLUGINS_VERSION=$(current_property "$ROOT_DIR/plugins/gradle.properties" "version")

if [[ "$ROOT_VERSION" != "$ROOT_VERSION_NAME" || "$ROOT_VERSION" != "$PLUGINS_VERSION" ]]; then
  echo "Version mismatch detected:" >&2
  echo "  gradle.properties version      = $ROOT_VERSION" >&2
  echo "  gradle.properties VERSION_NAME = $ROOT_VERSION_NAME" >&2
  echo "  plugins/gradle.properties      = $PLUGINS_VERSION" >&2
  exit 1
fi

VERSION="$ROOT_VERSION"
IS_SNAPSHOT=0
if [[ "$VERSION" == *-SNAPSHOT ]]; then
  IS_SNAPSHOT=1
fi

echo "Publishing version: $VERSION"
echo "Mode: $MODE"

if [[ "$MODE" == "local" ]]; then
  run_gradle \
    :libs:graph:publishToMavenLocal \
    :libs:resource:runtime:publishToMavenLocal \
    :libs:image:image:publishToMavenLocal \
    :libs:image:engine-coil:publishToMavenLocal \
    :libs:image:engine-glide:publishToMavenLocal
  run_plugins_gradle :resource-gradle-plugin:publishToMavenLocal
  exit 0
fi

require_env ORG_GRADLE_PROJECT_mavenCentralUsername
require_env ORG_GRADLE_PROJECT_mavenCentralPassword
require_env ORG_GRADLE_PROJECT_signingInMemoryKey
require_env ORG_GRADLE_PROJECT_signingInMemoryKeyPassword

CENTRAL_TASK="publishAndReleaseToMavenCentral"
if [[ "$IS_SNAPSHOT" -eq 1 ]]; then
  CENTRAL_TASK="publishToMavenCentral"
fi

run_gradle :libs:graph:"$CENTRAL_TASK"
run_gradle :libs:resource:runtime:"$CENTRAL_TASK"
run_gradle \
  :libs:image:image:"$CENTRAL_TASK" \
  :libs:image:engine-coil:"$CENTRAL_TASK" \
  :libs:image:engine-glide:"$CENTRAL_TASK"

if [[ "$SKIP_PLUGIN_PORTAL" -eq 1 ]]; then
  exit 0
fi

if [[ "$IS_SNAPSHOT" -eq 1 ]]; then
  echo "Gradle Plugin Portal does not support snapshot versions. Re-run with --skip-plugin-portal or publish a release version." >&2
  exit 1
fi

require_env GRADLE_PUBLISH_KEY
require_env GRADLE_PUBLISH_SECRET
run_plugins_gradle :resource-gradle-plugin:publishPlugins
