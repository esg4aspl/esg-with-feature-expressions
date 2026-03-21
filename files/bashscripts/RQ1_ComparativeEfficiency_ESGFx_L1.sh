#!/bin/bash
# USAGE: ./RQ1_ComparativeEfficiency_ESGFx_L1.sh <CaseName> <ShortName> <RunID> [TotalShards] [StartShard] [EndShard]
# EXAMPLE: ./RQ1_ComparativeEfficiency_ESGFx_L1.sh SodaVendingMachine SVM 5 40 0 39

# === ARGUMENT VALIDATION ===
if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]; then
  echo "❌ ERROR: Missing required arguments."
  echo "Usage: ./RQ1_ComparativeEfficiency_ESGFx_L1.sh <CaseName> <ShortName> <RunID> [TotalShards] [StartShard] [EndShard]"
  echo "Example: ./RQ1_ComparativeEfficiency_ESGFx_L1.sh SodaVendingMachine SVM 1 4 0 3"
  exit 1
fi

CASE_NAME=$1
SHORT_NAME=$2
RUN_PARAM=$3
SHARD_PARAM=$4
START_PARAM=$5
END_PARAM=$6

# --- 1. DYNAMIC PATH CONFIGURATION ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILES_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_DIR="$(dirname "$FILES_DIR")"

# --- 2. CLUSTER SHARD LOGIC ---
# Configure Total Logical Shards (N)
if [ -n "$SHARD_PARAM" ]; then
  N=$SHARD_PARAM
else
  if [[ "$OSTYPE" == "darwin"* ]]; then N=4; else N=40; fi
fi

# Configure Execution Range for this Node
if [ -n "$START_PARAM" ]; then S_NODE=$START_PARAM; else S_NODE=0; fi
if [ -n "$END_PARAM" ]; then E_NODE=$END_PARAM; else E_NODE=$((N-1)); fi

# --- 3. MEMORY OPTIMIZATION ---
# Adjusted for 8GB nodes. Xmx=1500m leaves room for OS overhead.
if [[ "$OSTYPE" == "darwin"* ]]; then
  XMS=512m; XMX=1200m
else
  XMS=512m; XMX=1500m
fi

# Navigate to project directory
cd "$PROJECT_DIR" || { echo "❌ ERROR: Directory not found: $PROJECT_DIR"; exit 1; }

# Create Log Directory for L1
LOG_DIR="${FILES_DIR}/logs/${CASE_NAME}/RQ1/ESGFx_L1"
mkdir -p "$LOG_DIR"

# --- 4. COMPILE & DEPENDENCIES ---
echo "🔨 Compiling project and gathering dependencies..."
mvn clean package dependency:copy-dependencies -DskipTests > "$LOG_DIR/RQ1_ESGFx_L1_build.log" 2>&1
if [ $? -ne 0 ]; then
    echo "❌ COMPILATION ERROR! Check '$LOG_DIR/RQ1_ESGFx_L1_build.log' for details."
    exit 1
fi

# Set Classpath securely
export CP="target/classes:target/dependency/*"

# Target Java Class
MAIN="tr.edu.iyte.esgfx.cases.${CASE_NAME}.RQ1_ComparativeEfficiency_ESGFx_L1_${SHORT_NAME}"
# =======================================================

JAVA_OPTS="-Xms$XMS -Xmx$XMX -XX:+UseG1GC"

echo "=== 🚀 BENCHMARK START: SHARDS $S_NODE to $E_NODE (Total Context: $N) | RunID: $RUN_PARAM ==="

# Execute Shards
for i in $(seq $S_NODE $E_NODE); do

  # Clear log naming structure specific to L1
  LOG="${LOG_DIR}/run_RQ1_ESGFx_L1_runID${RUN_PARAM}_shard$(printf "%02d" $i).log"
  # =======================================================
  
  # Environmental variables injected here: N_SHARDS, SHARD, runID
  N_SHARDS=$N SHARD=$i runID=$RUN_PARAM \
  nohup java $JAVA_OPTS -cp "$CP" "$MAIN" > "$LOG" 2>&1 &
  
  echo "✅ Shard $i dispatched -> $LOG"
  
  # Stagger start to prevent OS-level thread spawning spikes
  if [[ "$OSTYPE" == "darwin"* ]]; then sleep 1; else sleep 0.2; fi
done

echo "--------------------------------------------------"
echo "🎉 PROCESS DISPATCHED FOR $CASE_NAME (ESGFx L1 | Range: $S_NODE-$E_NODE | RunID: $RUN_PARAM)"