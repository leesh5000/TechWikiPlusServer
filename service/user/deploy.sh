#!/bin/bash
set -euo pipefail  # Exit on error, undefined vars, and pipe failures

# User Service Deployment Script
# This script handles the deployment of the User Service with comprehensive checks

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
HEALTH_CHECK_URL="${HEALTH_CHECK_URL:-http://localhost:9000/health}"
DOCKER_COMPOSE_CMD="docker-compose --env-file .env.tag --env-file .env.base --env-file .env.user-service -f docker-compose.base.yml -f docker-compose.user-service.yml"
DEPLOYMENT_HISTORY_FILE="deployments.json"
MAX_HISTORY_ENTRIES=10

# Source GitHub Actions configuration if available
if [ -f ".env.github-actions" ]; then
    set -a  # automatically export all variables
    source .env.github-actions
    set +a  # turn off automatic export
    echo -e "${GREEN}✅ GitHub Actions configuration loaded from .env.github-actions${NC}"
fi

# Parse command line arguments
ROLLBACK_MODE=false
ROLLBACK_VERSION=""
while [[ $# -gt 0 ]]; do
    case $1 in
        --rollback)
            ROLLBACK_MODE=true
            shift
            if [[ -n "$1" ]] && [[ ! "$1" =~ ^-- ]]; then
                ROLLBACK_VERSION="$1"
                shift
            fi
            ;;
        *)
            shift
            ;;
    esac
done

# Helper functions
print_step() {
    echo -e "\n${BLUE}===================================================${NC}"
    echo -e "${PURPLE}STEP $1: $2${NC}"
    echo -e "${BLUE}===================================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✅ SUCCESS: $1${NC}"
}

print_error() {
    echo -e "${RED}❌ ERROR: $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  WARNING: $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  INFO: $1${NC}"
}

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to save deployment history
save_deployment_history() {
    local version=$1
    local status=$2
    local commit_sha=${COMMIT_SHA:-"unknown"}
    local timestamp
    timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

    # Initialize history file if it doesn't exist
    if [ ! -f "$DEPLOYMENT_HISTORY_FILE" ]; then
        echo "[]" > "$DEPLOYMENT_HISTORY_FILE"
    fi

    # Create new entry
    local new_entry
    new_entry=$(jq -n \
        --arg v "$version" \
        --arg s "$status" \
        --arg c "$commit_sha" \
        --arg t "$timestamp" \
        '{version: $v, status: $s, commit: $c, timestamp: $t}')

    # Add to history and keep only last MAX_HISTORY_ENTRIES
    if jq ". += [$new_entry] | .[-$MAX_HISTORY_ENTRIES:]" "$DEPLOYMENT_HISTORY_FILE" > "${DEPLOYMENT_HISTORY_FILE}.tmp"; then
        mv "${DEPLOYMENT_HISTORY_FILE}.tmp" "$DEPLOYMENT_HISTORY_FILE"
    else
        print_error "Failed to save deployment history"
    fi
}

# Function to get last successful deployment
get_last_successful_version() {
    if [ -f "$DEPLOYMENT_HISTORY_FILE" ]; then
        jq -r '.[] | select(.status == "success") | .version' "$DEPLOYMENT_HISTORY_FILE" | tail -1
    else
        echo ""
    fi
}

# Function to display deployment history
show_deployment_history() {
    if [ -f "$DEPLOYMENT_HISTORY_FILE" ]; then
        echo -e "\n${BLUE}Recent Deployment History:${NC}"
        jq -r '.[] | "\(.timestamp) | \(.version) | \(.status) | \(.commit)"' "$DEPLOYMENT_HISTORY_FILE" | \
            column -t -s "|" | tail -10
    else
        print_warning "No deployment history found"
    fi
}

# Start deployment
echo -e "${PURPLE}"
echo "╔═══════════════════════════════════════════════════╗"
echo "║          User Service Deployment Script           ║"
echo "║                                                   ║"
echo "║  This script will deploy the User Service with    ║"
echo "║  comprehensive checks and validations.            ║"
echo "╚═══════════════════════════════════════════════════╝"
echo -e "${NC}"

# Display deployment information
echo -e "\n${BLUE}╔═══════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║           DEPLOYMENT INFORMATION                  ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════╝${NC}"

# Handle rollback mode
if [ "$ROLLBACK_MODE" = true ]; then
    print_step "ROLLBACK" "Initiating rollback procedure"

    if [ -z "$ROLLBACK_VERSION" ]; then
        # Get last successful version
        ROLLBACK_VERSION=$(get_last_successful_version)
        if [ -z "$ROLLBACK_VERSION" ]; then
            print_error "No successful deployment history found for rollback"
            exit 1
        fi
        print_info "Rolling back to last successful version: $ROLLBACK_VERSION"
    else
        print_info "Rolling back to specified version: $ROLLBACK_VERSION"
    fi

    # Update .env.tag with rollback version
    echo "IMAGE_TAG=$ROLLBACK_VERSION" > .env.tag
    print_success "Updated .env.tag with rollback version: $ROLLBACK_VERSION"
    show_deployment_history
else
    # Normal deployment - read IMAGE_TAG from .env.tag
    echo -e "\n${PURPLE}Image Tag Configuration:${NC}"
    echo -e "${BLUE}───────────────────────────────────────────────────${NC}"

    # Read IMAGE_TAG from .env.tag file
    if [ -f ".env.tag" ] && [ -n "$(grep '^IMAGE_TAG=' .env.tag)" ]; then
        IMAGE_TAG=$(grep '^IMAGE_TAG=' .env.tag | cut -d'=' -f2)
        print_success "IMAGE_TAG loaded from .env.tag"
        echo -e "${GREEN}  → Current IMAGE_TAG: ${PURPLE}${IMAGE_TAG}${NC}"
    else
        print_error "IMAGE_TAG not found in .env.tag file!"
        exit 1
    fi

    # Display deployment type based on IMAGE_TAG pattern
    if [[ "$IMAGE_TAG" =~ ^[a-f0-9]{7}$ ]]; then
        echo -e "${BLUE}  → Deployment Type: ${GREEN}Automated CI/CD Deployment${NC}"
        echo -e "${BLUE}  → Version Format: Git Commit SHA (Short)${NC}"
    elif [[ "$IMAGE_TAG" =~ ^[a-f0-9]{40}$ ]]; then
        echo -e "${BLUE}  → Deployment Type: ${GREEN}Automated CI/CD Deployment${NC}"
        echo -e "${BLUE}  → Version Format: Git Commit SHA (Full)${NC}"
    elif [ "$IMAGE_TAG" = "latest" ]; then
        echo -e "${BLUE}  → Deployment Type: ${YELLOW}Manual/Development Deployment${NC}"
        echo -e "${BLUE}  → Version Format: Latest available image${NC}"
    else
        echo -e "${BLUE}  → Deployment Type: ${PURPLE}Custom Tag Deployment${NC}"
        echo -e "${BLUE}  → Version Format: User-defined tag${NC}"
    fi
fi

# Display Docker image information
echo -e "\n${PURPLE}Docker Image Information:${NC}"
echo -e "${BLUE}───────────────────────────────────────────────────${NC}"

# Check if ECR_REGISTRY is set and construct full image path
if [ -n "${ECR_REGISTRY:-}" ]; then
    # ECR_REGISTRY should already include the full repository path
    FULL_IMAGE_PATH="${ECR_REGISTRY}:${IMAGE_TAG}"
    echo -e "${BLUE}  → Registry: ${NC}${ECR_REGISTRY}"
    echo -e "${BLUE}  → Tag: ${NC}${IMAGE_TAG}"
    echo -e "${GREEN}  → Full Image Path: ${NC}${FULL_IMAGE_PATH}"
else
    print_warning "ECR_REGISTRY not set - using local/default registry"
    FULL_IMAGE_PATH="techwikiplus-user-service:${IMAGE_TAG}"
    echo -e "${BLUE}  → Repository: ${NC}techwikiplus-user-service"
    echo -e "${BLUE}  → Tag: ${NC}${IMAGE_TAG}"
    echo -e "${GREEN}  → Full Image Path: ${NC}${FULL_IMAGE_PATH}"
fi

# Display additional deployment context if available
echo -e "\n${PURPLE}Deployment Context:${NC}"
echo -e "${BLUE}───────────────────────────────────────────────────${NC}"

if [ -n "${COMMIT_SHA:-}" ]; then
    echo -e "${BLUE}  → Git Commit SHA: ${NC}${COMMIT_SHA}"
else
    echo -e "${YELLOW}  → Git Commit SHA: ${NC}Not available"
fi

if [ -n "${GITHUB_RUN_NUMBER:-}" ]; then
    echo -e "${BLUE}  → GitHub Actions Run #: ${NC}${GITHUB_RUN_NUMBER}"
fi

if [ -n "${GITHUB_ACTOR:-}" ]; then
    echo -e "${BLUE}  → Triggered by: ${NC}${GITHUB_ACTOR}"
fi

echo -e "${BLUE}  → Deployment Time: ${NC}$(date '+%Y-%m-%d %H:%M:%S %Z')"
echo -e "${BLUE}───────────────────────────────────────────────────${NC}\n"

# Step 1: Check Docker and Docker Compose installation
print_step "1" "Checking Docker and Docker Compose installation"

# Check for timeout command (required for preventing hangs)
if ! command_exists timeout; then
    print_error "timeout command is not available!"
    echo "The timeout command is required to prevent the script from hanging."
    echo "Please install it using:"
    echo "  - Ubuntu/Debian: sudo apt-get install coreutils"
    echo "  - CentOS/RHEL: sudo yum install coreutils"
    echo "  - macOS: brew install coreutils"
    exit 1
fi

# Check for jq command (required for JSON parsing)
if ! command_exists jq; then
    print_error "jq command is not available!"
    echo "The jq command is required for parsing JSON output."
    echo "Please install it using:"
    echo "  - Ubuntu/Debian: sudo apt-get install jq"
    echo "  - CentOS/RHEL: sudo yum install jq"
    echo "  - macOS: brew install jq"
    exit 1
fi

if command_exists docker; then
    DOCKER_VERSION=$(docker --version)
    print_success "Docker is installed: $DOCKER_VERSION"
else
    print_error "Docker is not installed!"
    echo "Please install Docker using one of the following methods:"
    echo "  - Ubuntu/Debian: sudo apt-get install docker.io"
    echo "  - CentOS/RHEL: sudo yum install docker"
    echo "  - Or visit: https://docs.docker.com/get-docker/"
    exit 1
fi

if command_exists docker-compose; then
    COMPOSE_VERSION=$(docker-compose --version)
    print_success "Docker Compose is installed: $COMPOSE_VERSION"
else
    print_error "Docker Compose is not installed!"
    echo "Please install Docker Compose:"
    echo "  - Visit: https://docs.docker.com/compose/install/"
    echo "  - Or run: sudo apt-get install docker-compose (Ubuntu/Debian)"
    exit 1
fi

# Check if Docker daemon is running
if docker info >/dev/null 2>&1; then
    print_success "Docker daemon is running"
else
    print_error "Docker daemon is not running!"
    echo "Please start Docker daemon:"
    echo "  - sudo systemctl start docker"
    echo "  - Or: sudo service docker start"
    exit 1
fi

# Step 2: Check AWS ECR connectivity
print_step "2" "Checking AWS ECR connectivity"

# Check if AWS CLI is installed
if command_exists aws; then
    print_info "AWS CLI is installed"

    # Check ECR login capability
    if [ -n "${AWS_REGION:-}" ] && [ -n "${ECR_REGISTRY:-}" ]; then
        print_info "Attempting to authenticate with ECR..."

        if aws ecr get-login-password --region $AWS_REGION 2>/dev/null | docker login --username AWS --password-stdin $ECR_REGISTRY >/dev/null 2>&1; then
            print_success "Successfully authenticated with AWS ECR"
        else
            print_error "Failed to authenticate with AWS ECR"
            echo "This might be due to:"
            echo "  - Missing AWS credentials (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)"
            echo "  - Incorrect AWS_REGION or ECR_REGISTRY"
            echo "  - IAM permissions issue"
            echo ""
            echo "Cannot proceed without ECR access when using ECR registry."
            exit 1
        fi
    else
        print_warning "AWS_REGION or ECR_REGISTRY not set"
        echo "ECR authentication skipped. Make sure Docker images are available locally or in a public registry."
    fi
else
    print_warning "AWS CLI not installed. Skipping ECR authentication check."
    echo "If you need to pull images from ECR, please install AWS CLI:"
    echo "  - Visit: https://aws.amazon.com/cli/"
fi

# Step 3: Check required files
print_step "3" "Checking required configuration files"

REQUIRED_FILES=(
    ".env.tag"
    ".env.base"
    ".env.user-service"
    "docker-compose.base.yml"
    "docker-compose.user-service.yml"
)

# .env.github-actions is optional (only exists for automated deployments)
OPTIONAL_FILES=(
    ".env.github-actions"
)

ALL_FILES_EXIST=true

for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        print_success "Found: $file"
    else
        print_error "Missing: $file"
        ALL_FILES_EXIST=false
    fi
done

# Check optional files
for file in "${OPTIONAL_FILES[@]}"; do
    if [ -f "$file" ]; then
        print_success "Found: $file (optional)"
    else
        print_info "Not found: $file (optional - this is OK for manual deployments)"
    fi
done

if [ "$ALL_FILES_EXIST" = false ]; then
    print_error "Some required files are missing!"
    echo "Please ensure all required files are present in the current directory:"
    echo "  - docker-compose.base.yml: Base Docker Compose configuration"
    echo "  - docker-compose.user-service.yml: User service specific configuration"
    echo "  - .env.base: Base environment variables"
    echo "  - .env.user-service: User service specific environment variables"
    echo "  - .env.tag: Docker image tag (created by CD pipeline or manually)"
    echo ""
    echo "Optional files:"
    echo "  - .env.github-actions: AWS configuration and GitHub Actions metadata (created by CD pipeline)"
    exit 1
fi

# Step 4: Deploy with Docker Compose
print_step "4" "Deploying services with Docker Compose"

print_info "Pulling latest images and starting services..."

# IMAGE_TAG is already available from .env.tag via --env-file flag

# Run docker-compose up
if $DOCKER_COMPOSE_CMD up -d --build 2>&1 | tee deploy.log; then
    print_success "Docker Compose deployment completed"

    # Check for updated services
    print_info "Checking for updated services..."

    # Get list of all services from docker-compose
    set +e  # Temporarily disable exit on error for this section
    ALL_SERVICES=$(timeout 10s $DOCKER_COMPOSE_CMD ps --services 2>&1)
    SERVICE_LIST_EXIT_CODE=$?
    set -e  # Re-enable exit on error
    
    if [ $SERVICE_LIST_EXIT_CODE -eq 124 ]; then
        print_warning "Timeout while retrieving service list"
    elif [ $SERVICE_LIST_EXIT_CODE -ne 0 ]; then
        print_warning "Failed to retrieve service list (exit code: $SERVICE_LIST_EXIT_CODE)"
        if [ -n "$ALL_SERVICES" ]; then
            echo "Error output: $ALL_SERVICES"
        fi
    elif [ -z "$ALL_SERVICES" ]; then
        print_warning "Service list is empty"
    else
        echo -e "\n${GREEN}Service status:${NC}"
        
        # Check each service for recent updates
        for service in $ALL_SERVICES; do
            echo -e "\n  ${BLUE}Service: $service${NC}"
            
            # Get container ID for the service
            set +e  # Temporarily disable exit on error
            CONTAINER_ID=$(timeout 10s $DOCKER_COMPOSE_CMD ps -q "$service" 2>&1)
            CONTAINER_ID_EXIT_CODE=$?
            set -e  # Re-enable exit on error
            
            if [ $CONTAINER_ID_EXIT_CODE -eq 124 ]; then
                print_warning "    Timeout while getting container ID for $service"
                continue
            elif [ $CONTAINER_ID_EXIT_CODE -ne 0 ]; then
                print_warning "    Failed to get container ID for $service (exit code: $CONTAINER_ID_EXIT_CODE)"
                continue
            elif [ -z "$CONTAINER_ID" ]; then
                print_warning "    Container ID is empty for $service"
                continue
            elif [[ ! "$CONTAINER_ID" =~ ^[a-f0-9]+$ ]]; then
                print_warning "    Invalid container ID format for $service: $CONTAINER_ID"
                continue
            fi
            
            # Get container details
            if CONTAINER_INFO=$(timeout 5s docker inspect "$CONTAINER_ID" 2>/dev/null); then
                # Extract relevant information
                IMAGE_NAME=$(echo "$CONTAINER_INFO" | jq -r '.[0].Config.Image' 2>/dev/null || echo "unknown")
                CREATED_AT=$(echo "$CONTAINER_INFO" | jq -r '.[0].Created' 2>/dev/null || echo "unknown")
                STATUS=$(echo "$CONTAINER_INFO" | jq -r '.[0].State.Status' 2>/dev/null || echo "unknown")
                
                echo "    Image: $IMAGE_NAME"
                echo "    Status: $STATUS"
                
                # Check if container was recently created (within last 2 minutes)
                if [ "$CREATED_AT" != "unknown" ] && command_exists date; then
                    CREATED_TIMESTAMP=$(date -d "$CREATED_AT" +%s 2>/dev/null || echo "0")
                    CURRENT_TIMESTAMP=$(date +%s)
                    AGE_SECONDS=$((CURRENT_TIMESTAMP - CREATED_TIMESTAMP))
                    
                    if [ $AGE_SECONDS -lt 120 ]; then
                        echo "    ${GREEN}Recently updated (${AGE_SECONDS}s ago)${NC}"
                    else
                        AGE_MINUTES=$((AGE_SECONDS / 60))
                        echo "    Last updated: ${AGE_MINUTES} minutes ago"
                    fi
                fi
            else
                print_warning "    Could not inspect container"
            fi
        done
    fi

    # Clean up deployment log if it exists
    [ -f deploy.log ] && rm -f deploy.log
else
    print_error "Docker Compose deployment failed!"
    echo "Check the error messages above and the deploy.log file for details."
    echo "Common issues:"
    echo "  - Port conflicts (check if ports are already in use)"
    echo "  - Invalid environment variables"
    echo "  - Docker daemon issues"
    echo "  - Insufficient disk space"
    exit 1
fi

# Step 5: Clean up unused images
print_step "5" "Cleaning up unused Docker images"

# Get disk usage before cleanup
BEFORE_DISK=$(df -h /var/lib/docker 2>/dev/null | awk 'NR==2 {print $3}' || echo "N/A")

print_info "Current Docker disk usage: $BEFORE_DISK"
print_info "Removing unused images..."

# Remove dangling images
DANGLING_IMAGES=$(docker images -f "dangling=true" -q)
if [ -n "$DANGLING_IMAGES" ]; then
    docker rmi $DANGLING_IMAGES 2>/dev/null && print_success "Removed dangling images"
else
    print_info "No dangling images to remove"
fi

# Prune unused images (keeping images used by running containers)
if docker image prune -f --filter "until=24h" 2>&1 | grep -q "Total reclaimed space"; then
    RECLAIMED=$(docker image prune -f --filter "until=24h" 2>&1 | grep "Total reclaimed space" | awk '{print $4, $5}')
    print_success "Cleaned up unused images. Reclaimed: $RECLAIMED"
else
    print_info "No unused images to clean up"
fi

# Get disk usage after cleanup
AFTER_DISK=$(df -h /var/lib/docker 2>/dev/null | awk 'NR==2 {print $3}' || echo "N/A")
print_info "Docker disk usage after cleanup: $AFTER_DISK"

# Step 6: Verify container status
print_step "6" "Verifying container status"

print_info "Waiting 30 seconds for containers to initialize..."

# Show countdown
for i in {30..1}; do
    echo -ne "\r${BLUE}Waiting: $i seconds remaining...${NC}"
    sleep 1
done
echo -e "\r${GREEN}Wait complete!                       ${NC}"

print_info "Checking container health..."

# Check each container
ALL_HEALTHY=true
UNHEALTHY_CONTAINERS=()

# Get list of services with timeout
SERVICES=$(timeout 10s $DOCKER_COMPOSE_CMD ps --services 2>/dev/null)

if [ -n "$SERVICES" ]; then
    for service in $SERVICES; do
        CONTAINER_STATUS=$(timeout 10s $DOCKER_COMPOSE_CMD ps "$service" 2>/dev/null | tail -n +2)

        if [ -n "$CONTAINER_STATUS" ]; then
            if echo "$CONTAINER_STATUS" | grep -qE "(Up|running)"; then
                print_success "Service '$service' is running"

                # Check if container has health check
                CONTAINER_NAME=$(timeout 10s $DOCKER_COMPOSE_CMD ps -q "$service" 2>/dev/null)
                if [ -n "$CONTAINER_NAME" ]; then
                    HEALTH_STATUS=$(docker inspect $CONTAINER_NAME --format='{{.State.Health.Status}}' 2>/dev/null)
                    if [ -n "$HEALTH_STATUS" ] && [ "$HEALTH_STATUS" != "<no value>" ]; then
                        if [ "$HEALTH_STATUS" = "healthy" ]; then
                            print_success "  Health check: $HEALTH_STATUS"
                        else
                            print_warning "  Health check: $HEALTH_STATUS"
                            if [ "$HEALTH_STATUS" = "unhealthy" ]; then
                                ALL_HEALTHY=false
                                UNHEALTHY_CONTAINERS+=("$service")
                            fi
                        fi
                    fi
                fi
            else
                print_error "Service '$service' is not running properly"
                ALL_HEALTHY=false
                UNHEALTHY_CONTAINERS+=("$service")
            fi
        fi
    done
else
    print_error "Could not retrieve service list"
    ALL_HEALTHY=false
fi

if [ "$ALL_HEALTHY" = false ]; then
    print_warning "Some containers are not healthy!"
    echo "Unhealthy services: ${UNHEALTHY_CONTAINERS[*]}"
    echo "Check logs with: docker-compose logs <service-name>"
fi

# Step 7: Health check
print_step "7" "Performing application health check"

print_info "Waiting 30 seconds for services to stabilize..."

# Show countdown
for i in {30..1}; do
    echo -ne "\r${BLUE}Waiting: $i seconds remaining...${NC}"
    sleep 1
done
echo -e "\r${GREEN}Wait complete!                       ${NC}"

print_info "Checking application health at: $HEALTH_CHECK_URL"

# Perform health check
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 10 --max-time 30 "$HEALTH_CHECK_URL" 2>/dev/null)

if [ "$HTTP_STATUS" = "200" ]; then
    print_success "Application is healthy! (HTTP $HTTP_STATUS)"

    # Try to get more health info if endpoint returns JSON
    HEALTH_RESPONSE=$(curl -s --connect-timeout 10 --max-time 30 "$HEALTH_CHECK_URL" 2>/dev/null)
    if [ -n "$HEALTH_RESPONSE" ]; then
        echo -e "\n${BLUE}Health check response:${NC}"
        echo "$HEALTH_RESPONSE" | jq '.' 2>/dev/null || echo "$HEALTH_RESPONSE"
    fi
elif [ -n "$HTTP_STATUS" ] && [ "$HTTP_STATUS" != "000" ]; then
    print_warning "Application returned HTTP $HTTP_STATUS"
    echo "The application is responding but may not be fully healthy."
    echo "Check application logs for more details."
else
    print_error "Application health check failed!"
    echo "Could not connect to $HEALTH_CHECK_URL"
    echo "Possible issues:"
    echo "  - Application is still starting up (try waiting longer)"
    echo "  - Wrong port or URL configuration"
    echo "  - Network connectivity issues"
    echo "  - Application crashed during startup"
    echo ""
    echo "Debug commands:"
    echo "  - Check logs: $DOCKER_COMPOSE_CMD logs"
    echo "  - Check specific service: $DOCKER_COMPOSE_CMD logs <service-name>"
    echo "  - Check container status: $DOCKER_COMPOSE_CMD ps"
fi

# Final summary
echo -e "\n${PURPLE}"
echo "╔═══════════════════════════════════════════════════╗"
echo "║              Deployment Summary                   ║"
echo "╚═══════════════════════════════════════════════════╝"
echo -e "${NC}"

if [ "$ALL_HEALTHY" = true ] && [ "$HTTP_STATUS" = "200" ]; then
    print_success "Deployment completed successfully! 🎉"
    echo -e "\n${GREEN}All systems are operational.${NC}"
    save_deployment_history "$IMAGE_TAG" "success"
    print_info "Deployment history saved (version: $IMAGE_TAG)"

    echo -e "\n${BLUE}Useful commands:${NC}"
    echo "  - View logs: $DOCKER_COMPOSE_CMD logs -f"
    echo "  - Stop services: $DOCKER_COMPOSE_CMD down"
    echo "  - Restart services: $DOCKER_COMPOSE_CMD restart"
    echo "  - Check status: $DOCKER_COMPOSE_CMD ps"

    exit 0
else
    print_error "Deployment failed with errors"
    echo -e "\n${RED}Deployment did not complete successfully.${NC}"
    save_deployment_history "$IMAGE_TAG" "failed"

    if [ "$ALL_HEALTHY" = false ]; then
        echo -e "${RED}Container health check failed for: ${UNHEALTHY_CONTAINERS[*]}${NC}"
    fi

    if [ "$HTTP_STATUS" != "200" ]; then
        echo -e "${RED}Application health check failed (HTTP status: ${HTTP_STATUS:-N/A})${NC}"
    fi

    echo -e "\n${BLUE}Debug commands:${NC}"
    echo "  - View logs: $DOCKER_COMPOSE_CMD logs -f"
    echo "  - Check specific service: $DOCKER_COMPOSE_CMD logs <service-name>"
    echo "  - Check container status: $DOCKER_COMPOSE_CMD ps"
    echo "  - Rollback to previous version: ./deploy.sh --rollback"

    exit 1
fi