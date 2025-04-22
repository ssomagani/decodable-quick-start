#!/bin/bash

# Exit on any error
set -e

# Get the absolute path to the project root
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "🚀 Starting project deployment..."

# Function to display usage
show_usage() {
    echo "Usage: $0 [options]"
    echo "Options:"
    echo "  -f, --force       Force deployment even if resources exist"
    echo "  -h, --help        Show this help message"
    exit 1
}

# Parse command line arguments
FORCE_DEPLOY=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -f|--force)
            FORCE_DEPLOY=true
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

# Create a temporary file to store environment variables
ENV_FILE=$(mktemp)
echo "📝 Created temporary environment file: $ENV_FILE"

cleanup() {
    echo "🧹 Cleaning up temporary files..."
    rm -f "$ENV_FILE"
}

# Register the cleanup function to run on script exit
trap cleanup EXIT

# Function to source environment variables from the temporary file
load_env() {
    if [ -f "$ENV_FILE" ]; then
        echo "📥 Loading environment variables..."
        source "$ENV_FILE"
    fi
}

# Function to save environment variables to the temporary file
save_env() {
    local key=$1
    local value=$2
    echo "export $key=\"$value\"" >> "$ENV_FILE"
    echo "💾 Saved $key to environment"
}

# Function to check if required environment variables are set
check_required_env() {
    local missing_vars=()
    for var in "$@"; do
        if [ -z "${!var}" ]; then
            missing_vars+=("$var")
        fi
    done
    
    if [ ${#missing_vars[@]} -ne 0 ]; then
        echo "❌ Missing required environment variables: ${missing_vars[*]}"
        exit 1
    fi
}

echo "🔍 Starting infrastructure deployment..."

# Step 1: Deploy Docker infrastructure
echo "📦 Deploying Docker infrastructure..."
if [ -f "$PROJECT_ROOT/docker/deploy.sh" ]; then
    # Source the Docker deploy script to get access to its variables
    source "$PROJECT_ROOT/docker/deploy.sh"
    
    # Save the ngrok host for use by other components
    if [ -n "$NGROK_HOST" ]; then
        save_env "NGROK_HOST" "$NGROK_HOST"
        echo "✅ Successfully captured Kafka broker endpoint: $NGROK_HOST"
    else
        echo "❌ Failed to get Kafka broker endpoint"
        exit 1
    fi
else
    echo "❌ Docker deployment script not found"
    exit 1
fi

# Load environment variables before proceeding
load_env

# Step 2: Verify infrastructure
echo "🔍 Verifying infrastructure..."
check_required_env "NGROK_HOST"

# Step 3: Deploy Decodable resources
echo "📦 Deploying Decodable resources..."

# Deploy Decodable resources in the correct order
if [ -f "$PROJECT_ROOT/decodable/deploy.sh" ]; then
    echo "📦 Deploying Decodable resources in order..."
    env bash "$PROJECT_ROOT/decodable/deploy.sh"
    if [ $? -ne 0 ]; then
        echo "❌ Failed to deploy Decodable resources"
        exit 1
    fi
else
    echo "❌ Decodable deployment script not found, deploying components directly..."
    
    # Deploy streams first
    if [ -d "$PROJECT_ROOT/decodable/streams" ]; then
        echo "📦 Deploying streams first..."
        env bash "$PROJECT_ROOT/decodable/streams/deploy.sh"
        if [ $? -ne 0 ]; then
            echo "❌ Failed to deploy streams"
            exit 1
        fi
    else
        echo "ℹ️ Directory streams not found, skipping..."
    fi
    
    # Wait for streams to be ready
    echo "⏳ Waiting for streams to be ready..."
    sleep 10
    
    # Then deploy connections
    if [ -d "$PROJECT_ROOT/decodable/connections" ]; then
        echo "📦 Deploying connections..."
        env bash "$PROJECT_ROOT/decodable/connections/deploy.sh"
        if [ $? -ne 0 ]; then
            echo "❌ Failed to deploy connections"
            exit 1
        fi
    else
        echo "ℹ️ Directory connections not found, skipping..."
    fi
    
    # Wait for connections to be ready
    echo "⏳ Waiting for connections to be ready..."
    sleep 10
    
    # Deploy pipelines last
    if [ -d "$PROJECT_ROOT/decodable/pipelines" ]; then
        echo "📦 Deploying pipelines..."
        env bash "$PROJECT_ROOT/decodable/pipelines/deploy.sh"
        if [ $? -ne 0 ]; then
            echo "❌ Failed to deploy pipelines"
            exit 1
        fi
    else
        echo "ℹ️ Directory pipelines not found, skipping..."
    fi
fi

echo "✅ Project deployment completed successfully!"
echo "🔍 Infrastructure endpoint: $NGROK_HOST"
echo "📋 Next steps:"
echo "  1. Verify all components are running correctly"
echo "  2. Check Decodable dashboard for pipeline status"
echo "  3. Start sending data to the Kafka topic: test-topic"