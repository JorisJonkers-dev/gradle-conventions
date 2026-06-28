#!/usr/bin/env bash
set -euo pipefail
json=false; [[ "${1:-}" == "--json" ]] && { json=true; shift; }
desc="${*:-feature}"
short="$(echo "$desc" | tr '[:upper:]' '[:lower:]' | grep -oE '[a-z0-9]+' | head -4 | paste -sd- -)"
num=$(printf "%03d" "$(( $(find specs -mindepth 1 -maxdepth 1 -type d 2>/dev/null | wc -l) + 1 ))")
dir="specs/${num}-${short}"; mkdir -p "$dir/checklists"
[[ -f "$dir/spec.md" ]] || cp .specify/templates/spec-template.md "$dir/spec.md"
echo "{\"name\":\"$num-$short\"}" > .specify/feature.json
$json && printf '{"FEATURE_DIR":"%s","SPEC_FILE":"%s/spec.md"}\n' "$dir" "$dir" || echo "$dir"
