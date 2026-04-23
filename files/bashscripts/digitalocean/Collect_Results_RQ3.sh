#!/bin/bash

# ============================================================
# RQ3 DATA COLLECTION AUTOMATION (v2)
#
# Downloads fault detection CSV files from 10 DigitalOcean nodes.
#
# Remote structure (verified via `find` on node 188.166.89.99):
#   <CASE>/faultdetection/perProduct/
#       <PREFIX>_EdgeOmission_shard##.csv
#       <PREFIX>_EdgeOmission_MultiSeedRW_shard##.csv
#       <PREFIX>_EventOmission_shard##.csv
#       <PREFIX>_EventOmission_MultiSeedRW_shard##.csv
#   <CASE>/faultdetection/sensitivity/
#       <PREFIX>_DampingSensitivity_EdgeOmission_shard##.csv
#       <PREFIX>_DampingSensitivity_EventOmission_shard##.csv
#       <PREFIX>_DampingSensitivity_TestGen_shard##.csv
#
# Each node runs 8 shards -> 56 CSVs per SPL per node.
# 10 nodes x 8 SPLs -> 560 CSVs per SPL expected total.
# ============================================================

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IPS_FILE="$SCRIPT_DIR/ips.txt"

if [ ! -f "$IPS_FILE" ]; then
    echo "ERROR: ips.txt not found at $IPS_FILE"
    exit 1
fi

# Normalize ips.txt line endings
tr -d '\r' < "$IPS_FILE" > "${IPS_FILE}_clean" && mv "${IPS_FILE}_clean" "$IPS_FILE"
IFS=$'\n' read -d '' -r -a IPS < "$IPS_FILE" || true

# Parallel indexed arrays (bash 3.2 compatible — macOS default)
# CASES[i] and PREFIXES[i] must stay aligned.
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
PREFIXES=(
    "SVM"
    "eM"
    "El"
    "BAv2"
    "SAS"
    "Te"
    "Svia"
    "HS"
)

# Look up prefix by case name
get_prefix() {
    local target="$1"
    local i
    for i in "${!CASES[@]}"; do
        if [ "${CASES[$i]}" = "$target" ]; then
            echo "${PREFIXES[$i]}"
            return 0
        fi
    done
    echo ""
    return 1
}

LOCAL_CASES_ROOT="/Users/dilekozturk/git/esg-with-feature-expressions/files/Cases"

PER_PRODUCT_TYPES=(
    "EdgeOmission"
    "EdgeOmission_MultiSeedRW"
    "EventOmission"
    "EventOmission_MultiSeedRW"
)
SENSITIVITY_TYPES=(
    "DampingSensitivity_EdgeOmission"
    "DampingSensitivity_EventOmission"
    "DampingSensitivity_TestGen"
)

SHARDS_PER_NODE=8
TOTAL_SHARDS=80
FILES_PER_SPL_PER_NODE=$((${#PER_PRODUCT_TYPES[@]} * SHARDS_PER_NODE + ${#SENSITIVITY_TYPES[@]} * SHARDS_PER_NODE))
EXP_PP=$((${#PER_PRODUCT_TYPES[@]} * TOTAL_SHARDS))
EXP_SENS=$((${#SENSITIVITY_TYPES[@]} * TOTAL_SHARDS))
EXPECTED_PER_SPL=$((EXP_PP + EXP_SENS))

echo "===================================================="
echo "RQ3 DATA COLLECTION"
echo "===================================================="
echo "Nodes        : ${#IPS[@]}"
echo "SPLs         : ${#CASES[@]}"
echo "Per node/SPL : $FILES_PER_SPL_PER_NODE CSVs expected"
echo "Total/SPL    : $EXPECTED_PER_SPL CSVs expected across all nodes"
echo "===================================================="

for IP in "${IPS[@]}"; do
    if [ -z "$IP" ]; then continue; fi

    echo ""
    echo "------------------------------------------------------"
    echo "Node: $IP"
    echo "------------------------------------------------------"

    for CASE in "${CASES[@]}"; do
        PFX="$(get_prefix "$CASE")"

        # --- perProduct ---
        REMOTE_PP="/root/esg-with-feature-expressions/files/Cases/$CASE/faultdetection/perProduct/*.csv"
        LOCAL_PP="$LOCAL_CASES_ROOT/$CASE/faultdetection/perProduct"
        mkdir -p "$LOCAL_PP"

        scp -q -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
            "root@$IP:$REMOTE_PP" "$LOCAL_PP/" 2>/dev/null || true

        pp_total=$(ls -1 "$LOCAL_PP"/${PFX}_*.csv 2>/dev/null | wc -l | xargs)

        # --- sensitivity ---
        REMOTE_SENS="/root/esg-with-feature-expressions/files/Cases/$CASE/faultdetection/sensitivity/*.csv"
        LOCAL_SENS="$LOCAL_CASES_ROOT/$CASE/faultdetection/sensitivity"
        mkdir -p "$LOCAL_SENS"

        scp -q -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
            "root@$IP:$REMOTE_SENS" "$LOCAL_SENS/" 2>/dev/null || true

        sens_total=$(ls -1 "$LOCAL_SENS"/${PFX}_*.csv 2>/dev/null | wc -l | xargs)

        echo "  [$CASE] perProduct=$pp_total sensitivity=$sens_total (cumulative)"
    done
done


echo ""
echo "===================================================="
echo "GLOBAL SANITY CHECK"
echo "===================================================="
printf "%-24s | %-8s %-22s %-22s %-6s\n" \
    "CASE" "STATUS" "perProduct (got/exp)" "sensitivity (got/exp)" "total"
printf "%.0s-" {1..90}; echo ""

OVERALL_OK=1

for CASE in "${CASES[@]}"; do
    PFX="$(get_prefix "$CASE")"
    LOCAL_PP="$LOCAL_CASES_ROOT/$CASE/faultdetection/perProduct"
    LOCAL_SENS="$LOCAL_CASES_ROOT/$CASE/faultdetection/sensitivity"

    pp_count=0
    for TYPE in "${PER_PRODUCT_TYPES[@]}"; do
        c=$(ls -1 "$LOCAL_PP"/${PFX}_${TYPE}_shard*.csv 2>/dev/null | wc -l | xargs)
        pp_count=$((pp_count + c))
    done

    sens_count=0
    for TYPE in "${SENSITIVITY_TYPES[@]}"; do
        c=$(ls -1 "$LOCAL_SENS"/${PFX}_${TYPE}_shard*.csv 2>/dev/null | wc -l | xargs)
        sens_count=$((sens_count + c))
    done

    total=$((pp_count + sens_count))

    if [ "$total" -eq "$EXPECTED_PER_SPL" ]; then
        flag="[OK]"
    else
        flag="[MISS]"
        OVERALL_OK=0
    fi

    printf "%-24s | %-8s %-22s %-22s %-6s\n" \
        "$CASE" "$flag" \
        "$pp_count/$EXP_PP" \
        "$sens_count/$EXP_SENS" \
        "$total"
done

echo ""
if [ "$OVERALL_OK" -eq 1 ]; then
    echo "All SPLs have the expected number of CSV files."
else
    echo "Some SPLs have missing files. Per-type breakdown:"
    echo ""
    for CASE in "${CASES[@]}"; do
        PFX="$(get_prefix "$CASE")"
        LOCAL_PP="$LOCAL_CASES_ROOT/$CASE/faultdetection/perProduct"
        LOCAL_SENS="$LOCAL_CASES_ROOT/$CASE/faultdetection/sensitivity"

        case_total=0
        for TYPE in "${PER_PRODUCT_TYPES[@]}"; do
            c=$(ls -1 "$LOCAL_PP"/${PFX}_${TYPE}_shard*.csv 2>/dev/null | wc -l | xargs)
            case_total=$((case_total + c))
        done
        for TYPE in "${SENSITIVITY_TYPES[@]}"; do
            c=$(ls -1 "$LOCAL_SENS"/${PFX}_${TYPE}_shard*.csv 2>/dev/null | wc -l | xargs)
            case_total=$((case_total + c))
        done

        if [ "$case_total" -ne "$EXPECTED_PER_SPL" ]; then
            echo "  [$CASE]"
            for TYPE in "${PER_PRODUCT_TYPES[@]}"; do
                c=$(ls -1 "$LOCAL_PP"/${PFX}_${TYPE}_shard*.csv 2>/dev/null | wc -l | xargs)
                missing=$((TOTAL_SHARDS - c))
                if [ "$missing" -gt 0 ]; then
                    existing_shards=$(ls -1 "$LOCAL_PP"/${PFX}_${TYPE}_shard*.csv 2>/dev/null \
                        | sed -E "s/.*shard([0-9]+)\.csv/\1/" | sort -u)
                    miss_list=""
                    for i in $(seq -w 00 79); do
                        if ! grep -q "^$i$" <<< "$existing_shards"; then
                            miss_list="$miss_list $i"
                        fi
                    done
                    echo "      perProduct/$TYPE : got=$c/$TOTAL_SHARDS, missing:$miss_list"
                fi
            done
            for TYPE in "${SENSITIVITY_TYPES[@]}"; do
                c=$(ls -1 "$LOCAL_SENS"/${PFX}_${TYPE}_shard*.csv 2>/dev/null | wc -l | xargs)
                missing=$((TOTAL_SHARDS - c))
                if [ "$missing" -gt 0 ]; then
                    existing_shards=$(ls -1 "$LOCAL_SENS"/${PFX}_${TYPE}_shard*.csv 2>/dev/null \
                        | sed -E "s/.*shard([0-9]+)\.csv/\1/" | sort -u)
                    miss_list=""
                    for i in $(seq -w 00 79); do
                        if ! grep -q "^$i$" <<< "$existing_shards"; then
                            miss_list="$miss_list $i"
                        fi
                    done
                    echo "      sensitivity/$TYPE : got=$c/$TOTAL_SHARDS, missing:$miss_list"
                fi
            done
        fi
    done
fi

echo ""
echo "===================================================="
echo "RQ3 DATA COLLECTION COMPLETE"
echo "===================================================="