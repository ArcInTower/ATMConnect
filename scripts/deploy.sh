#!/bin/bash

# ATMConnect Deployment Script
# Deploys the application using Docker Compose

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ENVIRONMENT=${1:-development}
ACTION=${2:-up}

echo -e "${BLUE}🚀 ATMConnect Deployment${NC}"
echo -e "${BLUE}================================================${NC}"
echo -e "Environment: ${ENVIRONMENT}"
echo -e "Action: ${ACTION}"
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

# Check if Docker and Docker Compose are available
if ! command -v docker &> /dev/null; then
    error "Docker is not installed or not in PATH"
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    error "Docker Compose is not installed or not in PATH"
fi

# Determine Docker Compose command
if command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker-compose"
else
    COMPOSE_CMD="docker compose"
fi

# Determine compose file based on environment
case ${ENVIRONMENT} in
    "development"|"dev")
        COMPOSE_FILE="docker-compose.yml"
        PROFILES="--profile full"
        ;;
    "production"|"prod")
        COMPOSE_FILE="docker-compose.prod.yml"
        PROFILES=""
        ;;
    *)
        error "Unknown environment: ${ENVIRONMENT}. Use 'development' or 'production'"
        ;;
esac

# Check if compose file exists
if [ ! -f "${COMPOSE_FILE}" ]; then
    error "Compose file not found: ${COMPOSE_FILE}"
fi

# Load environment variables if .env file exists
if [ -f ".env" ]; then
    log "Loading environment variables from .env file"
    export $(cat .env | grep -v '^#' | xargs)
fi

# Validate required environment variables for production
if [ "${ENVIRONMENT}" = "production" ] || [ "${ENVIRONMENT}" = "prod" ]; then
    log "Validating production environment variables..."
    
    required_vars=("DB_NAME" "DB_USERNAME" "DB_PASSWORD" "JWT_SECRET_KEY" "REDIS_PASSWORD")
    missing_vars=()
    
    for var in "${required_vars[@]}"; do
        if [ -z "${!var}" ]; then
            missing_vars+=("$var")
        fi
    done
    
    if [ ${#missing_vars[@]} -ne 0 ]; then
        error "Missing required environment variables: ${missing_vars[*]}"
    fi
    
    # Validate JWT secret key length
    if [ ${#JWT_SECRET_KEY} -lt 32 ]; then
        error "JWT_SECRET_KEY must be at least 32 characters long"
    fi
fi

# Execute action
case ${ACTION} in
    "up"|"start")
        log "Starting ATMConnect services..."
        ${COMPOSE_CMD} -f ${COMPOSE_FILE} up -d ${PROFILES}
        
        log "Waiting for services to be healthy..."
        sleep 10
        
        # Check service health
        if ${COMPOSE_CMD} -f ${COMPOSE_FILE} ps | grep -q "unhealthy"; then
            warn "Some services are unhealthy. Check logs with: ${COMPOSE_CMD} -f ${COMPOSE_FILE} logs"
        else
            log "All services are running"
        fi
        
        # Display service status
        ${COMPOSE_CMD} -f ${COMPOSE_FILE} ps
        ;;
        
    "down"|"stop")
        log "Stopping ATMConnect services..."
        ${COMPOSE_CMD} -f ${COMPOSE_FILE} down
        ;;
        
    "restart")
        log "Restarting ATMConnect services..."
        ${COMPOSE_CMD} -f ${COMPOSE_FILE} restart
        ;;
        
    "logs")
        log "Showing service logs..."
        ${COMPOSE_CMD} -f ${COMPOSE_FILE} logs -f
        ;;
        
    "ps"|"status")
        log "Showing service status..."
        ${COMPOSE_CMD} -f ${COMPOSE_FILE} ps
        ;;
        
    "build")
        log "Building services..."
        ${COMPOSE_CMD} -f ${COMPOSE_FILE} build --no-cache
        ;;
        
    "pull")
        log "Pulling latest images..."
        ${COMPOSE_CMD} -f ${COMPOSE_FILE} pull
        ;;
        
    "clean")
        log "Cleaning up services and volumes..."
        ${COMPOSE_CMD} -f ${COMPOSE_FILE} down -v --remove-orphans
        docker system prune -f
        ;;
        
    *)
        error "Unknown action: ${ACTION}. Use: up, down, restart, logs, ps, build, pull, or clean"
        ;;
esac

echo ""
echo -e "${GREEN}✅ Deployment action '${ACTION}' completed successfully!${NC}"
echo -e "${BLUE}================================================${NC}"

# Show useful commands
if [ "${ACTION}" = "up" ] || [ "${ACTION}" = "start" ]; then
    echo -e "Application URLs:"
    if [ "${ENVIRONMENT}" = "production" ] || [ "${ENVIRONMENT}" = "prod" ]; then
        echo -e "  API: ${YELLOW}https://localhost:443/api/v1/health${NC}"
        echo -e "  Health: ${YELLOW}https://localhost:443/api/v1/health${NC}"
    else
        echo -e "  API: ${YELLOW}http://localhost:8080/api/v1/health${NC}"
        echo -e "  Health: ${YELLOW}http://localhost:8080/api/v1/health${NC}"
    fi
    echo ""
    echo -e "Useful commands:"
    echo -e "  View logs: ${YELLOW}${COMPOSE_CMD} -f ${COMPOSE_FILE} logs -f${NC}"
    echo -e "  Check status: ${YELLOW}${COMPOSE_CMD} -f ${COMPOSE_FILE} ps${NC}"
    echo -e "  Stop services: ${YELLOW}./scripts/deploy.sh ${ENVIRONMENT} down${NC}"
fi

echo ""