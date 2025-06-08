#!/bin/bash

# ATMConnect Build Script
# Builds the application and Docker images

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
IMAGE_NAME="atmconnect"
VERSION=${1:-latest}
BUILD_TARGET=${2:-production}

echo -e "${BLUE}🏗️  Building ATMConnect Application${NC}"
echo -e "${BLUE}================================================${NC}"
echo -e "Image Name: ${IMAGE_NAME}"
echo -e "Version: ${VERSION}"
echo -e "Target: ${BUILD_TARGET}"
echo ""

# Function to log messages
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
    exit 1
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    error "Docker is not running. Please start Docker and try again."
fi

# Check if required files exist
if [ ! -f "Dockerfile" ]; then
    error "Dockerfile not found. Please run this script from the project root."
fi

if [ ! -f "pom.xml" ]; then
    error "pom.xml not found. Please run this script from the project root."
fi

# Clean previous builds
log "Cleaning previous builds..."
if [ -d "target" ]; then
    rm -rf target/
fi

# Build with Maven
log "Building application with Maven..."
if command -v ./mvnw &> /dev/null; then
    ./mvnw clean package -DskipTests -B
elif command -v mvn &> /dev/null; then
    mvn clean package -DskipTests -B
else
    error "Maven not found. Please install Maven or use the Maven wrapper."
fi

# Verify JAR was created
if [ ! -f target/*.jar ]; then
    error "JAR file not found after build. Build may have failed."
fi

log "Application build completed successfully"

# Build Docker image
log "Building Docker image..."
docker build \
    --target ${BUILD_TARGET} \
    --tag ${IMAGE_NAME}:${VERSION} \
    --tag ${IMAGE_NAME}:latest \
    .

# Verify image was created
if docker images ${IMAGE_NAME}:${VERSION} | grep -q ${IMAGE_NAME}; then
    log "Docker image built successfully: ${IMAGE_NAME}:${VERSION}"
else
    error "Failed to build Docker image"
fi

# Display image information
log "Image information:"
docker images ${IMAGE_NAME}:${VERSION}

# Security scan (if available)
if command -v docker scan &> /dev/null; then
    log "Running security scan..."
    docker scan ${IMAGE_NAME}:${VERSION} || warn "Security scan completed with warnings"
else
    warn "Docker scan not available. Consider running security scans manually."
fi

echo ""
echo -e "${GREEN}✅ Build completed successfully!${NC}"
echo -e "${BLUE}================================================${NC}"
echo -e "Docker image: ${IMAGE_NAME}:${VERSION}"
echo -e "Target: ${BUILD_TARGET}"
echo ""
echo -e "To run the application:"
echo -e "  ${YELLOW}docker run -p 8080:8080 ${IMAGE_NAME}:${VERSION}${NC}"
echo ""
echo -e "To run with Docker Compose:"
echo -e "  ${YELLOW}docker-compose up${NC}"
echo ""