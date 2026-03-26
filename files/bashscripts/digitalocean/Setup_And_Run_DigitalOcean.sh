#!/bin/bash

# ============================================================
# DIGITAL OCEAN CLUSTER PROVISIONING & ORCHESTRATION
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# SCRIPT_DIR = .../files/bashscripts/digitalocean
# İki üst dizine çıkarak "files" klasörünü buluyoruz
FILES_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"

IPS_FILE="$SCRIPT_DIR/ips.txt"
LOGS_DIR="$FILES_DIR/logs/Setup_Logs"

mkdir -p "$LOGS_DIR"

if [ ! -f "$IPS_FILE" ]; then
    echo "❌ ERROR: ips.txt not found at $IPS_FILE"
    exit 1
fi

tr -d '\r' < "$IPS_FILE" > "${IPS_FILE}_clean" && mv "${IPS_FILE}_clean" "$IPS_FILE"
IFS=$'\n' read -d '' -r -a IP_LIST < "$IPS_FILE"

if [ ${#IP_LIST[@]} -eq 0 ]; then
    echo "❌ ERROR: ips.txt is empty."
    exit 1
fi

echo "=================================================="
echo "🚀 INITIALIZING DIGITAL OCEAN CLUSTER (${#IP_LIST[@]} NODES)"
echo "=================================================="

GIT_REPO="https://github.com/esg4aspl/esg-with-feature-expressions.git"
SHARDS_PER_NODE=8

launch_node() {
    local node_index=$1
    local ip=${IP_LIST[$node_index]}
    
    local start_shard=$((node_index * SHARDS_PER_NODE))
    local end_shard=$((start_shard + SHARDS_PER_NODE - 1))
    local log_file="$LOGS_DIR/setup_node_${node_index}_${ip}.log"
    
    echo "   ⚙️  Configuring Node $node_index -> IP: $ip (Shards: $start_shard to $end_shard)"
    echo "      📝 Log: $log_file"
    
    ssh -o StrictHostKeyChecking=no root@$ip bash -s "$start_shard" "$end_shard" "$GIT_REPO" > "$log_file" 2>&1 << 'EOF' &
        START_SHARD=$1
        END_SHARD=$2
        REPO_URL=$3

        set -e

        export DEBIAN_FRONTEND=noninteractive
        export NEEDRESTART_MODE=a
        export NEEDRESTART_SUSPEND=1

        echo "1. WAITING FOR CLOUD-INIT TO FINISH (Preventing dpkg locks)..."
        cloud-init status --wait || true
        while fuser /var/lib/dpkg/lock-frontend >/dev/null 2>&1; do sleep 5; done;

        echo "2. UPDATING PACKAGES & INSTALLING DEPENDENCIES..."
        apt-get update -y
        apt-get install -y openjdk-11-jdk openjdk-8-jdk maven git dos2unix curl

        echo "3. CONFIGURING JAVA ENVIRONMENTS..."
        update-alternatives --set java /usr/lib/jvm/java-11-openjdk-amd64/bin/java
        update-alternatives --set javac /usr/lib/jvm/java-11-openjdk-amd64/bin/javac
        
        echo 'export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64' >> ~/.bashrc
        echo 'export JAVA_8_EXE=/usr/lib/jvm/java-8-openjdk-amd64/bin/java' >> ~/.bashrc
        
        echo "4. CLONING REPOSITORY..."
        rm -rf /root/esg-with-feature-expressions
        cd /root
        git clone "$REPO_URL"

        echo "5. SETTING UP PROJECT..."
        cd esg-with-feature-expressions/files/bashscripts
        
        dos2unix *.sh
        chmod +x *.sh

        echo "6. STARTING GLOBAL ORCHESTRATOR..."
        nohup ./Global_Orchestrator.sh "$START_SHARD" "$END_SHARD" > /root/main_run.log 2>&1 &
        
        echo "✅ NODE PROVISIONING COMPLETE!"
EOF
}

for i in "${!IP_LIST[@]}"; do
    launch_node "$i"
    sleep 1 
done

echo "=================================================="
echo "🎯 ALL PROVISIONING COMMANDS DISPATCHED!"
echo "Logs saved to: $LOGS_DIR"
echo "=================================================="