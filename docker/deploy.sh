#!/bin/bash

# Exit on any error
set -e

# Get the absolute path to the docker directory
DOCKER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Configuration
COMPOSE_FILE="$DOCKER_DIR/kafka-ngrok-compose.yaml"

echo "🚀 Starting Docker deployment process..."

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install it first."
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install it first."
    exit 1
fi

# Check if the compose file exists
if [ ! -f "$COMPOSE_FILE" ]; then
    echo "❌ Compose file not found: $COMPOSE_FILE"
    exit 1
fi

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo "❌ jq is not installed. Please install it first."
    exit 1
fi

# Stop any existing containers
echo "🛑 Stopping existing containers..."
docker compose -f "$COMPOSE_FILE" down

# Start the containers
echo "📦 Starting containers..."
docker compose -f "$COMPOSE_FILE" up -d --remove-orphans

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 10

# Check if containers are running
echo "🔍 Checking container health..."
FAILED_CONTAINERS=0

# Get container status using a more reliable approach
CONTAINER_LIST=$(docker compose -f "$COMPOSE_FILE" ps --services)

for CONTAINER_NAME in $CONTAINER_LIST; do
    CONTAINER_STATE=$(docker compose -f "$COMPOSE_FILE" ps $CONTAINER_NAME --format json | jq -r '.State')
    
    if [[ "$CONTAINER_STATE" != "running" ]]; then
        echo "❌ Container $CONTAINER_NAME failed to start (State: $CONTAINER_STATE)"
        FAILED_CONTAINERS=$((FAILED_CONTAINERS + 1))
        
        # Show container logs for failed containers
        echo "📋 Logs for $CONTAINER_NAME:"
        docker compose -f "$COMPOSE_FILE" logs $CONTAINER_NAME | tail -n 20
        echo "----------------------------------------"
    else
        echo "✅ Container $CONTAINER_NAME is running"
    fi
done

if [ $FAILED_CONTAINERS -gt 0 ]; then
    echo "❌ Deployment failed: $FAILED_CONTAINERS container(s) failed to start"
    echo "Check the logs above for details"
    exit 1
fi

# Get ngrok endpoint from API
echo "🔍 Retrieving ngrok endpoint..."
NGROK_CONTAINER=$(docker compose -f "$COMPOSE_FILE" ps ngrok --format json | jq -r '.Name')
if [ -n "$NGROK_CONTAINER" ]; then
    # Get the ngrok URL from the API
    NGROK_URL=$(curl -s http://localhost:4040/api/tunnels/command_line | jq -r '.public_url')
    
    if [ -n "$NGROK_URL" ] && [ "$NGROK_URL" != "null" ]; then
        echo "🌐 Ngrok endpoint: $NGROK_URL"
        
        # Extract hostname:port from the URL - handle various formats
        if [[ "$NGROK_URL" =~ ^(https?|tcp)://([^/]+) ]]; then
            NGROK_HOST="${BASH_REMATCH[2]}"
            echo "🔗 Ngrok host: $NGROK_HOST"
            
            # If this script is being sourced, export the host
            if [[ "${BASH_SOURCE[0]}" != "${0}" ]]; then
                export NGROK_HOST
            fi
        else
            # Try a simpler approach - just take everything after the protocol
            NGROK_HOST=$(echo "$NGROK_URL" | sed -E 's|^[^://]+://||')
            echo "🔗 Ngrok host (using fallback): $NGROK_HOST"
            
            # If this script is being sourced, export the host
            if [[ "${BASH_SOURCE[0]}" != "${0}" ]]; then
                export NGROK_HOST
            fi
        fi
    else
        echo "⚠️ Could not find ngrok URL from API. Trying alternative method..."
        # Fallback to logs if API fails
        NGROK_URL=$(docker logs $NGROK_CONTAINER 2>&1 | grep -o 'https://.*\.ngrok\.io' | tail -n 1)
        if [ -n "$NGROK_URL" ]; then
            echo "🌐 Ngrok endpoint (from logs): $NGROK_URL"
            
            # Extract hostname:port from the URL - handle various formats
            if [[ "$NGROK_URL" =~ ^(https?|tcp)://([^/]+) ]]; then
                NGROK_HOST="${BASH_REMATCH[2]}"
                echo "🔗 Ngrok host: $NGROK_HOST"
                
                # If this script is being sourced, export the host
                if [[ "${BASH_SOURCE[0]}" != "${0}" ]]; then
                    export NGROK_HOST
                fi
            else
                # Try a simpler approach - just take everything after the protocol
                NGROK_HOST=$(echo "$NGROK_URL" | sed -E 's|^[^://]+://||')
                echo "🔗 Ngrok host (using fallback): $NGROK_HOST"
                
                # If this script is being sourced, export the host
                if [[ "${BASH_SOURCE[0]}" != "${0}" ]]; then
                    export NGROK_HOST
                fi
            fi
        else
            echo "⚠️ Could not find ngrok URL. Check the logs manually."
            NGROK_HOST=""
        fi
    fi
else
    echo "⚠️ Ngrok container not found. Check your compose file."
    NGROK_HOST=""
fi

# Create a test topic using the ngrok endpoint
TOPIC_CREATED=false

if [ -n "$NGROK_HOST" ]; then
    echo "📝 Creating test topic using ngrok endpoint..."
    
    # Extract host and port from NGROK_HOST
    if [[ "$NGROK_HOST" =~ ([^:]+):([0-9]+) ]]; then
        NGROK_HOSTNAME="${BASH_REMATCH[1]}"
        NGROK_PORT="${BASH_REMATCH[2]}"
        
        echo "🔍 Creating topic 'test_topic' on $NGROK_HOSTNAME:$NGROK_PORT"
        
        # Create the topic using the ngrok endpoint
        if docker exec -it broker /opt/kafka/bin/kafka-topics.sh --create --if-not-exists \
            --bootstrap-server $NGROK_HOSTNAME:$NGROK_PORT \
            --topic test_topic \
            --partitions 3 \
            --replication-factor 1; then
            
            echo "✅ Successfully created topic: test_topic"
            
            # List topics to verify
            echo "📋 Listing topics:"
            docker exec -it broker /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server $NGROK_HOSTNAME:$NGROK_PORT
            
            TOPIC_CREATED=true
        else
            echo "❌ Failed to create topic using ngrok endpoint."
            echo "❌ Topic creation failed. Deployment incomplete."
            exit 1
        fi
    else
        echo "⚠️ Could not parse ngrok host and port."
        echo "❌ Topic creation failed. Deployment incomplete."
        exit 1
    fi
else
    echo "⚠️ No ngrok host available."
    echo "❌ Topic creation failed. Deployment incomplete."
    exit 1
fi

echo "✅ Deployment completed successfully!"
echo "You can view the logs using: docker compose -f $COMPOSE_FILE logs -f"
echo "To stop the services, run: docker compose -f $COMPOSE_FILE down"

# Return the hostname if this script is being executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    echo "$NGROK_HOST"
fi 