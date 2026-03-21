#!/bin/bash

# Scriptin bulunduğu konumu (digitalocean klasörünü) bul ve ips.txt'yi oradan oku
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IPS_FILE="$SCRIPT_DIR/ips.txt"

tr -d '\r' < "$IPS_FILE" > "${IPS_FILE}_clean" && mv "${IPS_FILE}_clean" "$IPS_FILE"
IFS=$'\n' read -d '' -r -a IP_LIST < "$IPS_FILE"

if [ ${#IP_LIST[@]} -lt 10 ]; then
    echo "ERROR: ips.txt must contain at least 10 IP addresses."
    exit 1
fi

echo "INITIALIZING DIGITAL OCEAN CLUSTER (10 NODES)..."

GIT_REPO="https://github.com/esg4aspl/esg-with-feature-expressions.git"
SHARDS_PER_NODE=4

launch_node() {
    local node_index=$1
    local ip=${IP_LIST[$node_index]}
    
    local start_shard=$((node_index * SHARDS_PER_NODE))
    local end_shard=$((start_shard + SHARDS_PER_NODE - 1))
    
    echo "Configuring Node $node_index -> IP: $ip (Shards: $start_shard to $end_shard)"
    
    ssh -o StrictHostKeyChecking=no -f -n root@$ip "
        export DEBIAN_FRONTEND=noninteractive;
        
        echo '1. UPDATING PACKAGES...'
        apt-get update -y > /dev/null 2>&1;
        
        echo '2. INSTALLING JAVA 11, JAVA 8 AND MAVEN...'
        apt-get install -y openjdk-11-jdk openjdk-8-jdk maven git > /dev/null 2>&1;
        
        echo '3. CONFIGURING JAVA PATHS...'
        update-alternatives --set java /usr/lib/jvm/java-11-openjdk-amd64/bin/java > /dev/null 2>&1;
        update-alternatives --set javac /usr/lib/jvm/java-11-openjdk-amd64/bin/javac > /dev/null 2>&1;
        
        export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64;
        export JAVA_8_EXE=/usr/lib/jvm/java-8-openjdk-amd64/bin/java;
        
        echo 'export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64' >> ~/.bashrc;
        echo 'export JAVA_8_EXE=/usr/lib/jvm/java-8-openjdk-amd64/bin/java' >> ~/.bashrc;
        
        echo '4. CLONING REPOSITORY...'
        rm -rf /root/esg-with-feature-expressions;
        cd /root;
        git clone $GIT_REPO > /dev/null 2>&1;
        
        echo '5. BUILDING PROJECT...'
        cd esg-with-feature-expressions;
        mvn clean package dependency:copy-dependencies -DskipTests > build_do.log 2>&1;
        
        if grep -q 'BUILD SUCCESS' build_do.log; then
            echo '6. PROJECT BUILT SUCCESSFULLY. STARTING ORCHESTRATOR...'
            
            # NOT: Github'da Orchestrator hala bashscripts klasöründe olduğu için burası aynı kalır!
            cd files/bashscripts;
            chmod +x *.sh;
            nohup ./Global_Orchestrator.sh $start_shard $end_shard > main_run.log 2>&1 &
        else
            echo 'CRITICAL ERROR: MAVEN BUILD FAILED ON NODE $node_index';
        fi
    " &
}

for i in {0..9}; do
    launch_node $i
    sleep 2
done

wait
echo "CLUSTER INITIALIZATION AND DISPATCH COMPLETE."
echo "You can SSH into any node and check /root/esg-with-feature-expressions/files/bashscripts/main_run.log"