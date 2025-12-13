#!/bin/bash
# USAGE: ./Script.sh <Case> <Short> [TotalShards] [StartShard] [EndShard]

# === ARGUMENT VALIDATION ===
if [ -z "$1" ]; then
  echo "❌ ERROR: Missing arguments. Usage: ./Script.sh <Case> <Short> [ShardCount] [StartNode] [EndNode]"
  exit 1
fi

CASE_NAME=$1
SHORT_NAME=$2
SHARD_PARAM=$3
START_PARAM=$4
END_PARAM=$5

# --- 1. DYNAMIC PATH CONFIGURATION ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

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

# Create Log Directory
LOG_DIR="logs/${CASE_NAME}"
mkdir -p "$LOG_DIR"

# --- 4. COMPILE ---
# Only compiles if necessary.
mvn clean package -DskipTests > "$LOG_DIR/build.log" 2>&1
if [ $? -ne 0 ]; then
    echo "❌ COMPILATION ERROR! Check '$LOG_DIR/build.log' for details."
    exit 1
fi

# Set Classpath
export CP=$(mvn dependency:build-classpath | grep -v 'INFO')


MAIN="tr.edu.iyte.esgfx.cases.${CASE_NAME}.TotalTimeMeasurementApp_L4_${SHORT_NAME}"
# =======================================================

JAVA_OPTS="-Xms$XMS -Xmx$XMX -XX:+UseG1GC"

echo "=== 🚀 BENCHMARK START: SHARDS $S_NODE to $E_NODE (Total Context: $N) ==="

# Execute Shards
for i in $(seq $S_NODE $E_NODE); do

  LOG="${LOG_DIR}/run_timeL4_s$(printf "%02d" $i).log"
  # =======================================================

  # N_SHARDS is total (40), SHARD is specific ID (i)
  N_SHARDS=$N SHARD=$i \
  nohup java $JAVA_OPTS -cp "target/classes:$CP" "$MAIN" > "$LOG" 2>&1 &
  
  echo "✅ Shard $i dispatched -> $LOG"
  
  # Stagger start to prevent CPU spike
  if [[ "$OSTYPE" == "darwin"* ]]; then sleep 1; else sleep 0.2; fi
done

echo "--------------------------------------------------"
echo "🎉 PROCESS DISPATCHED FOR $CASE_NAME (Range: $S_NODE-$E_NODE)"