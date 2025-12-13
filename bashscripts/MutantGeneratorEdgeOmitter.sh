#!/bin/bash
# USAGE: ./Script.sh <CaseName> <ShortName> [TotalShards] [StartShard] [EndShard]
# Example: ./MutantGeneratorEdgeOmitter.sh SVM SVM 40 0 3

# Validate arguments
if [ -z "$1" ]; then 
    echo "❌ Error: Missing arguments. Usage: ./Script.sh <Case> <Short> [ShardCount] [StartNode] [EndNode]"
    exit 1
fi

CASE_NAME=$1
SHORT_NAME=$2
SHARD_PARAM=$3
START_PARAM=$4
END_PARAM=$5

# Resolve Paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# 1. CONFIGURE TOTAL LOGICAL SHARDS (N)
if [ -n "$SHARD_PARAM" ]; then 
    N=$SHARD_PARAM
else 
    # Fallback defaults
    if [[ "$OSTYPE" == "darwin"* ]]; then N=4; else N=40; fi
fi

# 2. CONFIGURE EXECUTION RANGE FOR THIS NODE
# If no specific range is provided, run all shards (0 to N-1)
if [ -n "$START_PARAM" ]; then S_NODE=$START_PARAM; else S_NODE=0; fi
if [ -n "$END_PARAM" ]; then E_NODE=$END_PARAM; else E_NODE=$((N-1)); fi

# 3. JAVA MEMORY CONFIGURATION
# Critical: Adjusted for 8GB nodes running 4 concurrent processes.
# Xmx is set to 1500m to prevent OutOfMemory errors on the host.
if [[ "$OSTYPE" == "darwin"* ]]; then 
    XMS=512m; XMX=1200m 
else 
    XMS=512m; XMX=1500m 
fi

# Navigate to project root
cd "$PROJECT_DIR" || { echo "❌ Directory not found: $PROJECT_DIR"; exit 1; }

# Create Log Directory
LOG_DIR="logs/${CASE_NAME}"
mkdir -p "$LOG_DIR"

# Build Project (Maven)
# Only strictly necessary for the first run, but kept for consistency.
mvn clean package -DskipTests > "$LOG_DIR/build.log" 2>&1
if [ $? -ne 0 ]; then echo "❌ Build Failed: See $LOG_DIR/build.log"; exit 1; fi

# Set Classpath
export CP=$(mvn dependency:build-classpath | grep -v 'INFO')

# =======================================================
# TODO: CHANGE THIS VARIABLE FOR EACH SCRIPT FILE!
# =======================================================
MAIN="tr.edu.iyte.esgfx.cases.${CASE_NAME}.MutationTesting_EdgeOmitter_${SHORT_NAME}"
# =======================================================

JAVA_OPTS="-Xms$XMS -Xmx$XMX -XX:+UseG1GC"

echo "=== 🚀 NODE EXECUTION STARTED: SHARDS $S_NODE to $E_NODE (Total Context: $N) ==="

# Execute Shards in Parallel (Background Processes)
for i in $(seq $S_NODE $E_NODE); do
  # Define Log File
  # TODO: CHANGE LOG NAME PREFIX FOR EACH SCRIPT (e.g., run_mutantedgeomitter, run_testsequence...)
  LOG="${LOG_DIR}/run_mutantedgeomitter_s$(printf "%02d" $i).log"
  
  # Launch Java Process
  # We pass 'N' as the total count, but 'i' as the specific shard ID.
  N_SHARDS=$N SHARD=$i \
  nohup java $JAVA_OPTS -cp "target/classes:$CP" "$MAIN" > "$LOG" 2>&1 &
  
  echo "✅ Shard $i dispatched -> $LOG"
  
  # Stagger startup to reduce CPU spike
  if [[ "$OSTYPE" == "darwin"* ]]; then sleep 1; else sleep 0.2; fi
done

echo "--------------------------------------------------"
echo "🎉 ALL PROCESSES DISPATCHED FOR $CASE_NAME ($S_NODE - $E_NODE)"
echo "   Monitor logs using: tail -f ${LOG_DIR}/*.log"