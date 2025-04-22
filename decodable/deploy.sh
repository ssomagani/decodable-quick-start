#!/bin/bash

# Exit on any error
set -e

# Get the absolute path to the decodable directory
DECODABLE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Function to display usage
show_usage() {
    echo "Usage: $0 [options]"
    echo "Options:"
    echo "  -c, --connections    Deploy only connections"
    echo "  -p, --pipelines      Deploy only pipelines"
    echo "  -s, --streams        Deploy only streams"
    echo "  -a, --all            Deploy all components (default)"
    echo "  -h, --help           Show this help message"
    exit 1
}

# Function to run deploy script if it exists
run_deploy_script() {
    local dir=$1
    local script="$dir/deploy.sh"
    
    if [ -f "$script" ]; then
        echo "📦 Running deployment script in $dir..."
        # Export all environment variables to the subscript
        env bash "$script"
        if [ $? -eq 0 ]; then
            echo "✅ Deployment in $dir completed successfully!"
        else
            echo "❌ Deployment in $dir failed!"
            exit 1
        fi
    else
        echo "ℹ️ No deployment script found in $dir"
    fi
}

# Parse command line arguments
DEPLOY_CONNECTIONS=false
DEPLOY_PIPELINES=false
DEPLOY_STREAMS=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -c|--connections)
            DEPLOY_CONNECTIONS=true
            shift
            ;;
        -p|--pipelines)
            DEPLOY_PIPELINES=true
            shift
            ;;
        -s|--streams)
            DEPLOY_STREAMS=true
            shift
            ;;
        -a|--all)
            DEPLOY_CONNECTIONS=true
            DEPLOY_PIPELINES=true
            DEPLOY_STREAMS=true
            shift
            ;;
        -h|--help)
            show_usage
            ;;
        *)
            echo "❌ Unknown option: $1"
            show_usage
            ;;
    esac
done

# If no specific components are selected, deploy all
if [ "$DEPLOY_CONNECTIONS" = false ] && [ "$DEPLOY_PIPELINES" = false ] && [ "$DEPLOY_STREAMS" = false ]; then
    DEPLOY_CONNECTIONS=true
    DEPLOY_PIPELINES=true
    DEPLOY_STREAMS=true
fi

echo "🚀 Starting deployment process..."

# Deploy streams first
if [ "$DEPLOY_STREAMS" = true ]; then
    if [ -d "$DECODABLE_DIR/streams" ]; then
        echo "🔄 Deploying streams first..."
        run_deploy_script "$DECODABLE_DIR/streams"
        echo "⏳ Waiting for streams deployment to complete..."
        sleep 5  # Give some time for the streams to be fully created
    else
        echo "ℹ️ Directory streams not found, skipping..."
    fi
fi

# Then deploy connections
if [ "$DEPLOY_CONNECTIONS" = true ]; then
    if [ -d "$DECODABLE_DIR/connections" ]; then
        echo "🔄 Deploying connections..."
        run_deploy_script "$DECODABLE_DIR/connections"
    else
        echo "ℹ️ Directory connections not found, skipping..."
    fi
fi

# Finally deploy pipelines
if [ "$DEPLOY_PIPELINES" = true ]; then
    if [ -d "$DECODABLE_DIR/pipelines" ]; then
        echo "🔄 Deploying pipelines..."
        run_deploy_script "$DECODABLE_DIR/pipelines"
    else
        echo "ℹ️ Directory pipelines not found, skipping..."
    fi
fi

echo "✅ All selected deployments completed successfully!" 