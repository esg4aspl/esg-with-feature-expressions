#!/bin/bash

# ============================================================
# RQ3 DATA COLLECTION AUTOMATION
# Description: Downloads fault detection CSV files (perProduct 
#              and summaries) from remote servers.
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
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

LOCAL_CASES_ROOT="/Users/dilekozturk/git/esg-with-feature-expressions/files/Cases"

echo "=== STARTING RQ3 DATA COLLECTION ==="

for IP in "${IPS[@]}"; do
    if [ -z "$IP" ]; then continue; fi

    echo "--------------------------------------------------"
    echo "Processing Server: $IP"
    echo "--------------------------------------------------"

    for CASE in "${CASES[@]}"; do
        echo "   Checking Case: $CASE"