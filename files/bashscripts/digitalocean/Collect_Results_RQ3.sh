#!/bin/bash

# ============================================================
# RQ3 DATA COLLECTION AUTOMATION
# Description: Downloads fault detection CSV files (perProduct, 
#              sensitivity, and summaries) from remote servers.
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILES_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"
IPS_FILE="$SCRIPT_DIR/ips.txt"

if [ ! -f "$IPS_FILE" ]; then
    echo "ERROR: ips.txt not found at $IPS_FILE"
    exit 1
fi

tr -d '\r' < "$IPS_FILE" > "${IPS_FILE}_clean" && mv "${IPS_FILE}_clean" "$IPS_FILE"
IFS=$'\n' read -d '' -r -a IPS < "$IPS_FILE"

CASES=(
    "SodaVendingMachine"
    "eMail"
    "Elevator"
    "BankAccountv2"
    "StudentAttendanceSystem"
    "Tesla"
    "syngovia"
    "HockertyShirts"
)

# Dinamik yol
LOCAL_CASES_ROOT="$FILES_DIR/Cases"

echo "=== STARTING RQ3 DATA COLLECTION ==="

for IP in "${IPS[@]}"; do
    if [ -z "$IP" ]; then continue; fi

    echo "--------------------------------------------------"
    echo "Processing Server: $IP"
    echo "--------------------------------------------------"

    for CASE in "${CASES[@]}"; do
        echo "   📂 Checking Case: $CASE"

        # 1. Ana Fault Detection özet CSV'lerini topla
        REMOTE_BASE="/root/esg-with-feature-expressions/files/Cases/$CASE/faultdetection/*.csv"
        LOCAL_BASE="$LOCAL_CASES_ROOT/$CASE/faultdetection"
        mkdir -p "$LOCAL_BASE"
        scp -o StrictHostKeyChecking=no -o ConnectTimeout=10 root@$IP:"$REMOTE_BASE" "$LOCAL_BASE/" 2>/dev/null

        # 2. perProduct verilerini topla
        REMOTE_PER_PRODUCT="/root/esg-with-feature-expressions/files/Cases/$CASE/faultdetection/perProduct/*"
        LOCAL_PER_PRODUCT="$LOCAL_CASES_ROOT/$CASE/faultdetection/perProduct"
        mkdir -p "$LOCAL_PER_PRODUCT"
        scp -o StrictHostKeyChecking=no -o ConnectTimeout=10 root@$IP:"$REMOTE_PER_PRODUCT" "$LOCAL_PER_PRODUCT/" 2>/dev/null
        if [ $? -eq 0 ]; then
            count=$(ls -1 "$LOCAL_PER_PRODUCT" 2>/dev/null | wc -l | xargs)
            echo "      [✓] perProduct -> Local total: $count files"
        fi

        # 3. Damping Sensitivity verilerini topla
        REMOTE_SENSITIVITY="/root/esg-with-feature-expressions/files/Cases/$CASE/faultdetection/sensitivity/*"
        LOCAL_SENSITIVITY="$LOCAL_CASES_ROOT/$CASE/faultdetection/sensitivity"
        mkdir -p "$LOCAL_SENSITIVITY"
        scp -o StrictHostKeyChecking=no -o ConnectTimeout=10 root@$IP:"$REMOTE_SENSITIVITY" "$LOCAL_SENSITIVITY/" 2>/dev/null
        if [ $? -eq 0 ] && [ "$(ls -A "$LOCAL_SENSITIVITY" 2>/dev/null)" ]; then
            count=$(ls -1 "$LOCAL_SENSITIVITY" 2>/dev/null | wc -l | xargs)
            echo "      [✓] sensitivity -> Local total: $count files"
        fi
    done
done

echo "=================================================="
echo "RQ3 DATA COLLECTION COMPLETE!"
echo "=================================================="