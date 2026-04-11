#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILES_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"
IPS_FILE="$SCRIPT_DIR/ips.txt"

# Logları bashscripts klasörü yerine ana logs klasörü içine indiriyoruz
LOCAL_DEST="$FILES_DIR/logs/Cluster_Logs"

if [ ! -f "$IPS_FILE" ]; then
    echo "ERROR: ips.txt not found at $IPS_FILE"
    exit 1
fi

tr -d '\r' < "$IPS_FILE" > "${IPS_FILE}_clean" && mv "${IPS_FILE}_clean" "$IPS_FILE"
IFS=$'\n' read -d '' -r -a IP_LIST < "$IPS_FILE"

mkdir -p "$LOCAL_DEST"

echo "=================================================="
echo "DOWNLOADING ALL LOGS FROM CLUSTER NODES"
echo "=================================================="

for i in "${!IP_LIST[@]}"; do
    IP="${IP_LIST[$i]}"
    if [ -z "$IP" ]; then continue; fi

    NODE_ID=$((i + 1))
    NODE_DIR="$LOCAL_DEST/Node_${NODE_ID}_${IP}"
    
    mkdir -p "$NODE_DIR/logs"
    mkdir -p "$NODE_DIR/Cases"
    
    echo "--------------------------------------------------"
    echo "Downloading from Node $NODE_ID ($IP)..."
    
    scp -q -r -o StrictHostKeyChecking=no -o ConnectTimeout=10 root@$IP:/root/esg-with-feature-expressions/files/logs/* "$NODE_DIR/logs/" 2>/dev/null
    
    if [ $? -eq 0 ]; then
        echo "   [✓] Logs downloaded successfully."
    else
        echo "   [!] Failed to download Logs."
    fi
    
done

echo "=================================================="
echo "LOG COLLECTION COMPLETE! Saved to $LOCAL_DEST"
echo "=================================================="