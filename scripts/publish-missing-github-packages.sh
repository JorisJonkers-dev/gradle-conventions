#!/usr/bin/env bash
set -euo pipefail

version="$(jq --raw-output '."." // empty' .release-please-manifest.json)"
if [[ -z "${version}" ]]; then
  echo "::error::.release-please-manifest.json does not contain a root package version"
  exit 1
fi

package_owner="${PACKAGE_OWNER:-${GITHUB_REPOSITORY_OWNER:?GITHUB_REPOSITORY_OWNER is required}}"

package_has_version() {
  local package_name="$1"
  local api_error
  api_error="$(mktemp)"

  local versions
  if versions="$(gh api --paginate "/users/${package_owner}/packages/maven/${package_name}/versions?per_page=100" --jq '.[].name' 2>"${api_error}")"; then
    rm -f "${api_error}"
    grep --fixed-strings --line-regexp --quiet -- "${version}" <<<"${versions}"
    return $?
  fi

  if grep --quiet "HTTP 404" "${api_error}"; then
    rm -f "${api_error}"
    return 1
  fi

  cat "${api_error}" >&2
  rm -f "${api_error}"
  return 2
}

declare -a all_packages=()
declare -a missing_packages=()
declare -a missing_tasks=()

while IFS="|" read -r package_name publish_task; do
  all_packages+=("${package_name}")

  set +e
  package_has_version "${package_name}"
  result=$?
  set -e

  case "${result}" in
    0)
      echo "GitHub Packages already contains ${package_name}:${version}; skipping ${publish_task}."
      ;;
    1)
      missing_packages+=("${package_name}")
      missing_tasks+=("${publish_task}")
      ;;
    *)
      exit 1
      ;;
  esac
done <<'PACKAGES'
dev.jorisjonkers.gradle-conventions|:aggregate:publishMavenPublicationToGitHubPackagesRepository
dev.jorisjonkers.gradle-conventions-detekt|:plugins:detekt:publishPluginMavenPublicationToGitHubPackagesRepository
dev.jorisjonkers.detekt.dev.jorisjonkers.detekt.gradle.plugin|:plugins:detekt:publishDev.jorisjonkers.detektPluginMarkerMavenPublicationToGitHubPackagesRepository
dev.jorisjonkers.gradle-conventions-jooq-codegen|:plugins:jooq-codegen:publishPluginMavenPublicationToGitHubPackagesRepository
dev.jorisjonkers.jooq-codegen.dev.jorisjonkers.jooq-codegen.gradle.plugin|:plugins:jooq-codegen:publishDev.jorisjonkers.jooq-codegenPluginMarkerMavenPublicationToGitHubPackagesRepository
dev.jorisjonkers.gradle-conventions-kotlin|:plugins:kotlin:publishPluginMavenPublicationToGitHubPackagesRepository
dev.jorisjonkers.kotlin.dev.jorisjonkers.kotlin.gradle.plugin|:plugins:kotlin:publishDev.jorisjonkers.kotlinPluginMarkerMavenPublicationToGitHubPackagesRepository
dev.jorisjonkers.gradle-conventions-ktlint|:plugins:ktlint:publishPluginMavenPublicationToGitHubPackagesRepository
dev.jorisjonkers.ktlint.dev.jorisjonkers.ktlint.gradle.plugin|:plugins:ktlint:publishDev.jorisjonkers.ktlintPluginMarkerMavenPublicationToGitHubPackagesRepository
dev.jorisjonkers.gradle-conventions-spring|:plugins:spring:publishPluginMavenPublicationToGitHubPackagesRepository
dev.jorisjonkers.spring.dev.jorisjonkers.spring.gradle.plugin|:plugins:spring:publishDev.jorisjonkers.springPluginMarkerMavenPublicationToGitHubPackagesRepository
dev.jorisjonkers.gradle-conventions-test-logging|:plugins:test-logging:publishPluginMavenPublicationToGitHubPackagesRepository
dev.jorisjonkers.test-logging.dev.jorisjonkers.test-logging.gradle.plugin|:plugins:test-logging:publishDev.jorisjonkers.test-loggingPluginMarkerMavenPublicationToGitHubPackagesRepository
dev.jorisjonkers.gradle-conventions-testing|:plugins:testing:publishPluginMavenPublicationToGitHubPackagesRepository
dev.jorisjonkers.testing.dev.jorisjonkers.testing.gradle.plugin|:plugins:testing:publishDev.jorisjonkers.testingPluginMarkerMavenPublicationToGitHubPackagesRepository
PACKAGES

if [[ "${#missing_tasks[@]}" -eq 0 ]]; then
  echo "All GitHub Packages coordinates for ${version} already exist; nothing to publish."
  exit 0
fi

echo "Publishing missing GitHub Packages coordinates for ${version}:"
printf ' - %s\n' "${missing_packages[@]}"

set +e
./gradlew "${missing_tasks[@]}" --no-daemon --no-parallel --max-workers=1
publish_status=$?
set -e

if [[ "${publish_status}" -eq 0 ]]; then
  exit 0
fi

echo "::warning::Gradle publish failed; rechecking package versions before failing."

declare -a still_missing=()
for package_name in "${all_packages[@]}"; do
  set +e
  package_has_version "${package_name}"
  result=$?
  set -e

  case "${result}" in
    0) ;;
    1) still_missing+=("${package_name}") ;;
    *) exit 1 ;;
  esac
done

if [[ "${#still_missing[@]}" -eq 0 ]]; then
  echo "All GitHub Packages coordinates for ${version} exist after the failed publish; treating publish as successful."
  exit 0
fi

echo "::error::Gradle publish failed and these coordinates are still missing:"
printf ' - %s\n' "${still_missing[@]}" >&2
exit "${publish_status}"
