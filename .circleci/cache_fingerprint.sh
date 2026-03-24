#!/bin/sh

set -eu

if [ "$#" -ne 2 ]; then
	echo "Usage: $0 <engine|studio|webadmin> <output-file>" >&2
	exit 1
fi

mode="$1"
output="$2"

cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT

add_path() {
	if [ -f "$1" ]; then
		printf '%s\n' "$1" >> "$tmp"
	fi
}

add_glob() {
	for path in $1; do
		if [ -f "$path" ]; then
			printf '%s\n' "$path" >> "$tmp"
		fi
	done
}

add_common_gradle_inputs() {
	add_path build.gradle
	add_path settings.gradle
	add_path gradle.properties
	add_path gradle/wrapper/gradle-wrapper.properties
}

case "$mode" in
	engine)
		add_common_gradle_inputs
		add_path headers.gradle
		add_path changelog.gradle
		add_path engine/build.gradle
		add_path gradle-plugin/build.gradle
		add_path patches/build.gradle
		;;
	studio)
		add_common_gradle_inputs
		add_path eclipse-base/base.target
		add_glob 'eclipse-base/pom.xml'
		add_glob 'eclipse-feature/pom.xml'
		add_glob 'eclipse-plugin-*/pom.xml'
		add_glob 'eclipse-repository/pom.xml'
		;;
	webadmin)
		add_path convertigo-studio-web/package.json
		add_path convertigo-studio-web/package-lock.json
		;;
	*)
		echo "Unknown fingerprint mode: $mode" >&2
		exit 1
		;;
esac

mkdir -p "$(dirname "$output")"

sort -u "$tmp" | while IFS= read -r path; do
	checksum="$(shasum -a 256 "$path" | awk '{print $1}')"
	printf '%s %s\n' "$path" "$checksum"
done > "$output"
