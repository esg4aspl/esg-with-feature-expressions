#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IPS_FILE="$SCRIPT_DIR/ips.txt"

tr -d '\r' < "$IPS_FILE" > "${IPS_FILE}_clean" && mv "${IPS_FILE}_clean" "$IPS_FILE"
IFS=$'\n' read -d '' -r -a IP_LIST < "$IPS_FILE"

if [ ${#IP_LIST[@]} -eq 0 ]; then
    echo "ERROR: ips.txt is empty or not found."
    exit 1
fi

TAGS=(
    "s0-3" 
    "s4-7" 
    "s8-11" 
    "s12-15" 
    "s16-19" 
    "s20-23" 
    "s24-27" 
    "s28-31"
    "s32-35"
    "s36-39"
)

FIRST_IP="${IP_LIST[0]}"
WORKLOAD_CMD=$(ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 root@$FIRST_IP 'ps -eo cmd | grep java | grep -v grep | head -n 1' 2>/dev/null)

if [[ -z "$WORKLOAD_CMD" ]]; then
    WORKLOAD_NAME="Idle / Finished"
else
    WORKLOAD_NAME=$(echo "$WORKLOAD_CMD" | grep -o 'tr\.edu\.iyte[^ ]*' | awk -F'.' '{print $NF}')
    if [[ -z "$WORKLOAD_NAME" ]]; then WORKLOAD_NAME="Unknown Java Process"; fi
fi

clear
echo "===================================================================================================="
echo "📊 CLUSTER MONITORING - $(date '+%d/%m/%Y %H:%M:%S')"
echo "🛠️  CURRENT TASK: $WORKLOAD_NAME"
echo "===================================================================================================="

printf "%-3s | %-8s | %-15s | %-14s | %-11s | %-8s | %-10s\n" "#" "SHARDS" "IP ADDRESS" "JAVA PROCESSES" "RAM" "CPU LOAD" "STATUS"
echo "----------------------------------------------------------------------------------------------------"

for i in "${!IP_LIST[@]}"; do
    index=$((i + 1))
    ip="${IP_LIST[$i]}"
    tag="${TAGS[$i]}" 
    
    if [[ -z "$tag" ]]; then tag="???"; fi

    DATA=$(ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 root@$ip 'echo $(pgrep -c java || echo 0) "|" $(free -h | grep Mem | awk "{print \$3 \"/\" \$2}") "|" $(uptime | awk -F"load average:" "{ print \$2 }" | awk -F, "{print \$1}")' 2>/dev/null)
    
    JAVA_COUNT_RAW=$(echo "$DATA" | cut -d'|' -f1 | xargs)
    JAVA_COUNT=$(echo "$JAVA_COUNT_RAW" | awk '{print $1}') 
    
    RAM_USAGE=$(echo "$DATA" | cut -d'|' -f2 | xargs)
    CPU_LOAD=$(echo "$DATA" | cut -d'|' -f3 | xargs)
    
    if [[ -z "$JAVA_COUNT" ]]; then JAVA_COUNT=0; fi

    if [ "$JAVA_COUNT" -gt 0 ]; then
        STATUS="🟢 RUNNING" 
    else
        STATUS="🔴 STOPPED" 
    fi
    
    printf "%-3s | %-8s | %-15s | %-14s | %-11s | %-8s | %-10s\n" "$index" "$tag" "$ip" "$JAVA_COUNT" "$RAM_USAGE" "$CPU_LOAD" "$STATUS"
done
echo "===================================================================================================="