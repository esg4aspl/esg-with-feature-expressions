#!/bin/bash
if [ -z "$1" ]; then echo "❌ Usage: ./Script.sh <Case> <Short> [ShardCount]"; exit 1; fi

CASE_NAME=$1
SHORT_NAME=$2
SHARD_PARAM=$3

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

if [ -n "$SHARD_PARAM" ]; then N=$SHARD_PARAM; else if [[ "$OSTYPE" == "darwin"* ]]; then N=4; else N=30; fi; fi
if [[ "$OSTYPE" == "darwin"* ]]; then XMS=512m; XMX=1200m; else XMS=1g; XMX=2g; fi

cd "$PROJECT_DIR" || exit 1

LOG_DIR="logs/${CASE_NAME}"
mkdir -p "$LOG_DIR"

mvn clean package -DskipTests > "$LOG_DIR/build.log" 2>&1
if [ $? -ne 0 ]; then echo "❌ Compile Error"; exit 1; fi

export CP=$(mvn dependency:build-classpath | grep -v 'INFO')
MAIN="tr.edu.iyte.esgfx.cases.${CASE_NAME}.MutationTesting_EventInserter_${SHORT_NAME}"
JAVA_OPTS="-Xms$XMS -Xmx$XMX -XX:+UseG1GC"

echo "=== 🚀 STARTING: $N SHARDS ($CASE_NAME) ==="

# Clean old logs
rm -f "${LOG_DIR}/run_mutanteventinserter_s*.log"

# Start the loop
for i in $(seq 0 $((N-1))); do
  LOG="${LOG_DIR}/run_mutanteventinserter_s$(printf "%02d" $i).log"
  nohup java $JAVA_OPTS -cp "target/classes:$CP" "$MAIN" > "$LOG" 2>&1 &
  echo "✅ Shard $i started -> $LOG"
  if [[ "$OSTYPE" == "darwin"* ]]; then sleep 1; else sleep 0.2; fi
done

echo "--------------------------------------------------"
echo "🎉 PROCESS STARTED FOR $CASE_NAME!"
echo "To monitor logs: tail -f ${LOG_DIR}/run_mutanteventinserter_s00.log"