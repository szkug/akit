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
  --version <version>     Update VERSION_NAME in all library submodules before publishing.
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

set_version() {
  local repo_dir="$1"
  local version="$2"
  sed -i.bak -E "s/^VERSION_NAME=.*/VERSION_NAME=${version}/" "$repo_dir/gradle.properties"
  rm -f "$repo_dir/gradle.properties.bak"
}

current_version() {
  local repo_dir="$1"
  sed -n 's/^VERSION_NAME=//p' "$repo_dir/gradle.properties"
}

require_env() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: $name" >&2
    exit 1
  fi
}

run_gradle() {
  local repo_path="$1"
  shift
  echo
  echo "==> $repo_path :: ./gradlew $*"
  (
    cd "$ROOT_DIR/$repo_path"
    ./gradlew "$@"
  )
}

PUBLISHED_FLAG=(-Pmunchkin.usePublishedDependencies=true)

if [[ -n "$TARGET_VERSION" ]]; then
  for repo in libs/graph libs/image libs/resource; do
    set_version "$ROOT_DIR/$repo" "$TARGET_VERSION"
  done
fi

GRAPH_VERSION=$(current_version "$ROOT_DIR/libs/graph")
IMAGE_VERSION=$(current_version "$ROOT_DIR/libs/image")
RESOURCE_VERSION=$(current_version "$ROOT_DIR/libs/resource")

if [[ "$GRAPH_VERSION" != "$IMAGE_VERSION" || "$GRAPH_VERSION" != "$RESOURCE_VERSION" ]]; then
  echo "VERSION_NAME mismatch detected:" >&2
  echo "  libs/graph    = $GRAPH_VERSION" >&2
  echo "  libs/image    = $IMAGE_VERSION" >&2
  echo "  libs/resource = $RESOURCE_VERSION" >&2
  exit 1
fi

VERSION="$GRAPH_VERSION"
IS_SNAPSHOT=0
if [[ "$VERSION" == *-SNAPSHOT ]]; then
  IS_SNAPSHOT=1
fi

echo "Publishing version: $VERSION"
echo "Mode: $MODE"

if [[ "$MODE" == "local" ]]; then
  run_gradle libs/graph publishToMavenLocal
  run_gradle libs/resource "${PUBLISHED_FLAG[@]}" :runtime:publishToMavenLocal :gradle-plugin:publishToMavenLocal
  run_gradle libs/image "${PUBLISHED_FLAG[@]}" :image:publishToMavenLocal :engine-coil:publishToMavenLocal :engine-glide:publishToMavenLocal
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

run_gradle libs/graph publishToMavenLocal
run_gradle libs/graph "$CENTRAL_TASK"
run_gradle libs/resource "${PUBLISHED_FLAG[@]}" :runtime:publishToMavenLocal
run_gradle libs/resource "${PUBLISHED_FLAG[@]}" :runtime:"$CENTRAL_TASK"
run_gradle libs/image "${PUBLISHED_FLAG[@]}" :image:"$CENTRAL_TASK" :engine-coil:"$CENTRAL_TASK" :engine-glide:"$CENTRAL_TASK"

if [[ "$SKIP_PLUGIN_PORTAL" -eq 1 ]]; then
  exit 0
fi

if [[ "$IS_SNAPSHOT" -eq 1 ]]; then
  echo "Gradle Plugin Portal does not support snapshot versions. Re-run with --skip-plugin-portal or publish a release version." >&2
  exit 1
fi

require_env GRADLE_PUBLISH_KEY
require_env GRADLE_PUBLISH_SECRET
run_gradle libs/resource :gradle-plugin:publishPlugins
