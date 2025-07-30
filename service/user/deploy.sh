#!/bin/bash

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
HEALTH_CHECK_URL="${HEALTH_CHECK_URL:-http://localhost:9000/actuator/health}"
HEALTH_CHECK_TIMEOUT=30
DOCKER_COMPOSE_CMD="docker-compose --env-file .env --env-file .env.prod -f docker-compose.base.yml -f docker-compose.prod.yml"

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

# Start deployment
echo -e "${PURPLE}"
echo "╔═══════════════════════════════════════════════════╗"
echo "║          User Service Deployment Script           ║"
echo "║                                                   ║"
echo "║  This script will deploy the User Service with   ║"
echo "║  comprehensive checks and validations.            ║"
echo "╚═══════════════════════════════════════════════════╝"
echo -e "${NC}"

# Step 1: Check Docker and Docker Compose installation
print_step "1" "Checking Docker and Docker Compose installation"

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
    if [ -n "$AWS_REGION" ] && [ -n "$ECR_REGISTRY" ]; then
        print_info "Attempting to authenticate with ECR..."

        if aws ecr get-login-password --region $AWS_REGION 2>/dev/null | docker login --username AWS --password-stdin $ECR_REGISTRY >/dev/null 2>&1; then
            print_success "Successfully authenticated with AWS ECR"
        else
            print_warning "Failed to authenticate with AWS ECR"
            echo "This might be due to:"
            echo "  - Missing AWS credentials (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)"
            echo "  - Incorrect AWS_REGION or ECR_REGISTRY"
            echo "  - IAM permissions issue"
            echo ""
            echo "Continuing with deployment (images might need to be pulled from ECR)..."
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
    "docker-compose.base.yml"
    "docker-compose.prod.yml"
    ".env"
    ".env.prod"
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

if [ "$ALL_FILES_EXIST" = false ]; then
    print_error "Some required files are missing!"
    echo "Please ensure all required files are present in the current directory:"
    echo "  - docker-compose.base.yml: Base Docker Compose configuration"
    echo "  - docker-compose.prod.yml: Production-specific overrides"
    echo "  - .env: Base environment variables"
    echo "  - .env.prod: Production environment variables"
    exit 1
fi

# Step 4: Deploy with Docker Compose
print_step "4" "Deploying services with Docker Compose"

print_info "Pulling latest images and starting services..."

# Capture current images before deployment
BEFORE_IMAGES=$(docker images --format "table {{.Repository}}:{{.Tag}}\t{{.ID}}" | grep -v "<none>")

# Run docker-compose up
if $DOCKER_COMPOSE_CMD up -d --build 2>&1 | tee deploy.log; then
    print_success "Docker Compose deployment completed"

    # Check for updated services
    print_info "Checking for updated services..."

    # Get list of services that were recreated
    RECREATED_SERVICES=$(grep -E "(Recreating|Creating)" deploy.log | awk '{print $2}' | sort | uniq)

    if [ -n "$RECREATED_SERVICES" ]; then
        echo -e "\n${GREEN}Updated/Created services:${NC}"
        for service in $RECREATED_SERVICES; do
            echo "  - $service"

            # Try to get image info for the service
            CONTAINER_ID=$(docker-compose ps -q $service 2>/dev/null)
            if [ -n "$CONTAINER_ID" ]; then
                IMAGE_INFO=$(docker inspect $CONTAINER_ID --format='{{.Config.Image}}' 2>/dev/null)
                if [ -n "$IMAGE_INFO" ]; then
                    echo "    Image: $IMAGE_INFO"
                fi
            fi
        done
    else
        print_info "No services were updated (all services are up-to-date)"
    fi

    # Clean up deployment log
    rm -f deploy.log
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

print_info "Checking container health..."

# Get all containers from the compose project
CONTAINERS=$($DOCKER_COMPOSE_CMD ps --format json 2>/dev/null || $DOCKER_COMPOSE_CMD ps)

# Check each container
ALL_HEALTHY=true
UNHEALTHY_CONTAINERS=()

# Get list of services
SERVICES=$($DOCKER_COMPOSE_CMD ps --services 2>/dev/null)

if [ -n "$SERVICES" ]; then
    for service in $SERVICES; do
        CONTAINER_STATUS=$($DOCKER_COMPOSE_CMD ps $service 2>/dev/null | tail -n +2)

        if [ -n "$CONTAINER_STATUS" ]; then
            if echo "$CONTAINER_STATUS" | grep -qE "(Up|running)"; then
                print_success "Service '$service' is running"

                # Check if container has health check
                CONTAINER_NAME=$($DOCKER_COMPOSE_CMD ps -q $service 2>/dev/null)
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
else
    print_warning "Deployment completed with warnings"
    echo -e "\n${YELLOW}Please check the issues mentioned above.${NC}"
fi

echo -e "\n${BLUE}Useful commands:${NC}"
echo "  - View logs: $DOCKER_COMPOSE_CMD logs -f"
echo "  - Stop services: $DOCKER_COMPOSE_CMD down"
echo "  - Restart services: $DOCKER_COMPOSE_CMD restart"
echo "  - Check status: $DOCKER_COMPOSE_CMD ps"

exit 0
