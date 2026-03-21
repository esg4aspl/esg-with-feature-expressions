#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IPS_FILE="$SCRIPT_DIR/ips.txt"

tr -d '\r' < "$IPS_FILE" > "${IPS_FILE}_clean" && mv "${IPS_FILE}_clean" "$IPS_FILE"
IFS=$'\n' read -d '' -r -a IP_LIST < "$IPS_FILE"

echo "=================================================="
echo "SCANNING CLUSTER LOGS FOR ERRORS / EXCEPTIONS"
echo "=================================================="

for i in "${!IP_LIST[@]}"; do
    IP="${IP_LIST[$i]}"
    NODE_ID=$((i + 1))
    
    echo "--------------------------------------------------"
    echo "Node $NODE_ID: $IP"
    echo "--------------------------------------------------"
    
    ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 root@$IP "
        LOG_DIR='/root/esg-with-feature-expressions/files/logs'
        if [ -d \"\$LOG_DIR\" ]; then
            ERROR_FILES=\$(find \"\$LOG_DIR\" -type f -name \"*.log\" -exec grep -lE \"Exception|CRITICAL|OutOfMemoryError\" {} +)
            if [ -n \"\$ERROR_FILES\" ]; then
                for f in \$ERROR_FILES; do
                    echo \"[ERROR IN] \$f\"
                    tail -n 5 \"\$f\"
                    echo \"\"
                done
            else
                echo \"No critical errors found in logs.\"
            fi
        else
            echo \"Log directory not found yet.\"
        fi
    " || echo "Connection Failed"
done
echo "=================================================="