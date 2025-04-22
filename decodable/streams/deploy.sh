#!/bin/bash

# Exit on any error
set -e

# Get the absolute path to the streams directory
STREAMS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "🚀 Starting deployment of stream resources..."

# Check if decodable CLI is installed
if ! command -v decodable &> /dev/null; then
    echo "❌ Decodable CLI is not installed. Please install it first."
    exit 1
fi

# Function to wait for a resource to be deployed
wait_for_resource() {
    local resource_name=$1
    local resource_type=$2
    
    echo "⏳ Waiting for $resource_type '$resource_name' to be deployed..."
    
    # # Use decodable query to wait for the resource to be deployed
    # decodable query $resource_name.yaml --name "$resource_name" --operation create --stabilize --timeout=5m
    
    sleep 10
    
    if [ $? -eq 0 ]; then
        echo "✅ $resource_type '$resource_name' deployed successfully!"
        return 0
    else
        echo "❌ Failed to deploy $resource_type '$resource_name'!"
        return 1
    fi
}

# Find all YAML files in the current directory
YAML_FILES=$(find "$STREAMS_DIR" -maxdepth 1 -name "*.yaml" -o -name "*.yml")

if [ -z "$YAML_FILES" ]; then
    echo "ℹ️ No YAML files found in $STREAMS_DIR"
    exit 0
fi

# Apply each YAML file
for yaml_file in $YAML_FILES; do
    echo "📦 Applying resource definition: $(basename "$yaml_file")"
    
    # Change to the streams directory before running decodable apply
    cd "$STREAMS_DIR"
    
    # Apply the YAML file
    decodable apply "$(basename "$yaml_file")"
    
    if [ $? -eq 0 ]; then
        echo "✅ Successfully applied $(basename "$yaml_file")"
        
        # Extract the resource name from the YAML file
        # This assumes the YAML file has a metadata.name field
        RESOURCE_NAME=$(grep -A 5 "metadata:" "$yaml_file" | grep "name:" | head -1 | awk '{print $2}')
        
        if [ -n "$RESOURCE_NAME" ]; then
            # Wait for the resource to be deployed
            wait_for_resource "$RESOURCE_NAME" "Stream"
        else
            echo "⚠️ Could not determine resource name from $(basename "$yaml_file"), skipping wait..."
        fi
    else
        echo "❌ Failed to apply $(basename "$yaml_file")"
        exit 1
    fi
done

echo "✅ All stream resources deployed successfully!" 