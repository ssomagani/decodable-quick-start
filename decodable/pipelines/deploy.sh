#!/bin/bash

# Exit on any error
set -e

# Get the absolute path to the project root
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Configuration
PIPELINE_DEF_TEMPLATE="$PROJECT_ROOT/decodable/pipelines/pipelines.yaml.template"
PIPELINE_DEF="$PROJECT_ROOT/decodable/pipelines/pipelines.yaml"
CUSTOM_DIR="$PROJECT_ROOT/decodable/custom"
TARGET_DIR="$CUSTOM_DIR/target"
ENV_FILE="$PROJECT_ROOT/.env"

echo "🚀 Starting deployment process..."
echo "📂 Using project root: $PROJECT_ROOT"
echo "📂 Custom directory: $CUSTOM_DIR"

# Check if decodable CLI is installed
if ! command -v decodable &> /dev/null; then
    echo "❌ Decodable CLI is not installed. Please install it first."
    exit 1
fi

# Clear the environment file
echo "# Environment variables generated on $(date)" > "$ENV_FILE"
echo "💾 Cleared environment file"

# Function to save environment variables to the temporary file
save_env() {
    local key=$1
    local value=$2
    echo "export $key=\"$value\"" >> "$ENV_FILE"
    echo "💾 Saved $key to environment"
}

# Function to process a template file and create a copy with environment variables substituted
process_template_file() {
    local template_file=$1
    local output_file=$2
    
    echo "🔄 Processing template file: $(basename "$template_file")"
    
    # Check if the template file exists
    if [ ! -f "$template_file" ]; then
        echo "❌ Template file not found: $template_file"
        return 1
    fi
    
    # Create a copy of the template file
    cp "$template_file" "$output_file"
    echo "📝 Created file: $output_file"
    
    # Get all environment variables
    local env_vars=$(env | cut -d= -f1)
    
    # Replace each environment variable in the output file
    for var in $env_vars; do
        # Skip internal variables and functions
        if [[ "$var" == "_" || "$var" == "BASH_"* || "$var" == "FUNCNAME" || "$var" == "PROJECT_ROOT" || "$var" == "CUSTOM_DIR" || "$var" == "TARGET_DIR" || "$var" == "ENV_FILE" ]]; then
            continue
        fi
        
        # Get the value of the environment variable
        local value="${!var}"
        
        # Skip empty values
        if [ -z "$value" ]; then
            continue
        fi
        
        # Replace the variable in the output file using awk
        # Format: ${VAR_NAME}
        awk -v var="$var" -v val="$value" '{gsub("\\$\\{" var "\\}", val); print}' "$output_file" > "${output_file}.new"
        mv "${output_file}.new" "$output_file"
        
        # Format: $VAR_NAME
        awk -v var="$var" -v val="$value" '{gsub("\\$" var, val); print}' "$output_file" > "${output_file}.new"
        mv "${output_file}.new" "$output_file"
    done
    
    echo "✅ Processed template file: $(basename "$template_file")"
}

# Build the JAR
echo "📦 Building JAR file..."
cd "$CUSTOM_DIR"
mvn clean package
cd "$(dirname "${BASH_SOURCE[0]}")"

# Find the JAR file (excluding original-*.jar)
JAR_FILE=$(find "$TARGET_DIR" -name "*.jar" ! -name "original-*.jar" -type f)

# Check if exactly one JAR was found
JAR_COUNT=$(echo "$JAR_FILE" | wc -l)
if [ "$JAR_COUNT" -ne 1 ]; then
    echo "❌ Expected exactly one JAR file in $TARGET_DIR, but found $JAR_COUNT:"
    echo "$JAR_FILE"
    save_env "JAR_FILE" "$JAR_FILE"
    exit 1
fi

echo "📦 Using JAR file: $JAR_FILE"
save_env "JAR_FILE" "$JAR_FILE"

# Process the pipeline template file
if [ -f "$PIPELINE_DEF_TEMPLATE" ]; then
    echo "📝 Processing pipeline template..."
    
    # Create a temporary file for the template
    TEMP_TEMPLATE=$(mktemp)
    cp "$PIPELINE_DEF_TEMPLATE" "$TEMP_TEMPLATE"
    
    # Replace JAR_FILE directly in the template
    sed -i.bak "s|\${JAR_FILE}|$JAR_FILE|g" "$TEMP_TEMPLATE"
    sed -i.bak "s|\$JAR_FILE|$JAR_FILE|g" "$TEMP_TEMPLATE"
    rm -f "${TEMP_TEMPLATE}.bak"
    
    # Process the template with the JAR_FILE already replaced
    process_template_file "$TEMP_TEMPLATE" "$PIPELINE_DEF"
    rm -f "$TEMP_TEMPLATE"
    
    if [ $? -ne 0 ]; then
        echo "❌ Failed to process pipeline template"
        exit 1
    fi
else
    echo "ℹ️ No template file found at $PIPELINE_DEF_TEMPLATE, using $PIPELINE_DEF directly"
fi

# Deploy to Decodable using declarative syntax
echo "📤 Deploying to Decodable..."
decodable apply "$PIPELINE_DEF"

echo "✅ Deployment completed successfully!"
echo "You can now activate the pipeline using: decodable pipeline activate flink-example" 