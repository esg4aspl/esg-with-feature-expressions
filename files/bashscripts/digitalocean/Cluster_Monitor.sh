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
    "s0-7" 
    "s8-15" 
    "s16-23" 
    "s24-31" 
    "s32-39" 
    "s40-47" 
    "s48-55" 
    "s56-63"
    "s64-71"
    "s72-79"
)

clear
echo "========================================================================================================================="
echo "  CLUSTER MONITORING - $(date '+%d/%m/%Y %H:%M:%S')"
echo "========================================================================================================================="

printf "%-3s | %-6s | %-15s | %-4s | %-10s | %-6s | %-42s | %-10s\n" "#" "SHARDS" "IP" "JAVA" "RAM" "CPU" "CURRENT TASK" "RUN"
echo "------------------------------------------------------------------------------------------------------------------------------"

for i in "${!IP_LIST[@]}"; do
    index=$((i + 1))
    ip="${IP_LIST[$i]}"
    tag="${TAGS[$i]:-???}"

    DATA=$(ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 root@$ip bash << 'REMOTE_EOF' 2>/dev/null
        # Java count
        JCOUNT=$(pgrep -c java 2>/dev/null || echo 0)
        
        # RAM
        RAM=$(free -h | grep Mem | awk '{print $3"/"$2}')
        
        # CPU load
        CPU=$(uptime | awk -F'load average:' '{print $2}' | awk -F, '{print $1}' | xargs)
        
        # Current task from java process
        TASK=$(ps -eo cmd | grep java | grep -v grep | head -1 | grep -o 'RQ[0-9]_[^ ]*' | head -1)
        if [ -z "$TASK" ]; then
            TASK="Idle"
        fi
        
        # Current run number from log file
        RUNINFO=""
        for LOGFILE in /root/main_run.log /root/rerun.log; do
            if [ -f "$LOGFILE" ]; then
                RUNINFO=$(grep -oE 'Run [0-9]+/[0-9]+' "$LOGFILE" | tail -1)
                if [ -n "$RUNINFO" ]; then break; fi
            fi
        done
        if [ -n "$RUNINFO" ]; then
            echo "${JCOUNT}|${RAM}|${CPU}|${TASK}|${RUNINFO}"
        else
            echo "${JCOUNT}|${RAM}|${CPU}|${TASK}|"
        fi
REMOTE_EOF
    )

    if [ -z "$DATA" ]; then
        printf "%-3s | %-6s | %-15s | %-4s | %-10s | %-6s | %-42s | %-10s\n" "$index" "$tag" "$ip" "?" "?" "?" "UNREACHABLE" ""
        continue
    fi

    JAVA_COUNT=$(echo "$DATA" | cut -d'|' -f1)
    RAM_USAGE=$(echo "$DATA" | cut -d'|' -f2)
    CPU_LOAD=$(echo "$DATA" | cut -d'|' -f3)
    TASK=$(echo "$DATA" | cut -d'|' -f4)
    RUNINFO=$(echo "$DATA" | cut -d'|' -f5)

    if [ "$JAVA_COUNT" -gt 0 ]; then
        STATUS="🟢"
    else
        STATUS="🔴"
    fi

    printf "%-3s | %-6s | %-15s | %s%-3s | %-10s | %-6s | %-42s | %-10s\n" "$index" "$tag" "$ip" "$STATUS" "$JAVA_COUNT" "$RAM_USAGE" "$CPU_LOAD" "$TASK" "$RUNINFO"
done
echo "========================================================================================================================="