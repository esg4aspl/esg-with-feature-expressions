#!/bin/bash
# ============================================================
# RERUN ORCHESTRATOR — runs RQ2 targeted reruns using v4 master
# ============================================================
# USAGE: ./Rerun_Orchestrator.sh <spec_file_name> [max_concurrent]
#
# Arguments:
#   spec_file_name     name of spec file in files/bashscripts/
#                      (e.g. rerun_hockerty_runID1.txt)
#   max_concurrent     optional (default 8). Matches original 8-shard-per-node.
#
# Called from Provision_And_Deploy_Rerun.sh after node setup.
# Runs ONLY the v4 master with the given spec file. Does NOT run
# RQ1/RQ3/damping/orig RQ2 — orig Global_Orchestrator does those.
# ============================================================

SPEC_FILE_NAME="${1:-}"
MAX_CONCURRENT="${2:-8}"

if [ -z "$SPEC_FILE_NAME" ]; then
    echo "USAGE: $0 <spec_file_name> [max_concurrent]"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

SPEC_FILE_PATH="$SCRIPT_DIR/$SPEC_FILE_NAME"

if [ ! -f "$SPEC_FILE_PATH" ]; then
    echo "ERROR: Spec file not found: $SPEC_FILE_PATH"
    exit 1
fi

echo "=================================================="
echo "🎯 RERUN ORCHESTRATOR STARTED"
echo "   Spec: $SPEC_FILE_NAME"
echo "   Max concurrent: $MAX_CONCURRENT"
echo "=================================================="

# Run v5 master (SPL + runID batched) with the spec
bash ./RQ2_ExtremeScalability_Master_Runner_v5.sh "$SPEC_FILE_PATH" "$MAX_CONCURRENT"

echo "=================================================="
echo "🏆 RERUN ORCHESTRATOR COMPLETE ON THIS NODE"
echo "=================================================="