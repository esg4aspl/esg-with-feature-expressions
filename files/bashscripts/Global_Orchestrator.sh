#!/bin/bash
# USAGE: ./Global_Orchestrator.sh [START_SHARD] [END_SHARD]
# EXAMPLES:
#   MacBook (4 shards): ./Global_Orchestrator.sh 0 3
#   DO Node 1: ./Global_Orchestrator.sh 0 3

START_SHARD=${1:-0}
END_SHARD=${2:-3}

echo "=================================================="
echo "🌍 GLOBAL ORCHESTRATOR STARTED (Assigned Shards: $START_SHARD to $END_SHARD)"
echo "=================================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

# --- PHASE 1: RQ0 (MODEL GENERATION) ---
echo ">>> PHASE 1: EXECUTING RQ0 (MODEL GENERATION) <<<"
# Does not take shard parameters. It generates all files natively.
bash ./RQ0_ProductGeneration_Master.sh

echo "⏳ Phase 1 Completed. Waiting 10 seconds for OS file indexing..."
sleep 10

# --- PHASE 2: RQ1 (COMPARATIVE EFFICIENCY) ---
echo ">>> PHASE 2: EXECUTING RQ1 (COMPARATIVE EFFICIENCY) <<<"
bash ./RQ1_ComparativeEfficiency_Master.sh "$START_SHARD" "$END_SHARD"
sleep 10

# --- PHASE 3: RQ3 (FAULT DETECTION) ---
echo ">>> PHASE 3: EXECUTING RQ3 (FAULT DETECTION) <<<"
bash ./RQ3_FaultDetection_Master.sh "$START_SHARD" "$END_SHARD"
sleep 10

# --- PHASE 3: RQ3 (FAULT DETECTION - DAMPING SENSITIVITY) ---
echo ">>> PHASE 3: EXECUTING RQ3 (FAULT DETECTION - DAMPING SENSITIVITY) <<<"
bash ./RQ3_RandomWalk_DampingSensivityAnalysis.sh "$START_SHARD" "$END_SHARD"
sleep 10

# --- PHASE 4: RQ2 (EXTREME SCALABILITY) ---
# NOTE: Commented out for local MacBook testing. 
# Uncomment the line below when deploying to the actual cluster.

echo ">>> PHASE 4: EXECUTING RQ2 (EXTREME SCALABILITY) <<<"
bash ./RQ2_ExtremeScalability_Master.sh "$START_SHARD" "$END_SHARD"

echo "=================================================="
echo "🏆 ALL SCHEDULED RESEARCH QUESTIONS COMPLETED ON THIS MACHINE!"
echo "=================================================="