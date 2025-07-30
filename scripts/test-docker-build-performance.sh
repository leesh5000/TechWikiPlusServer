#!/bin/bash

# Docker Build Performance Testing Script
# This script helps identify bottlenecks in Docker build times

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
DOCKERFILE_ORIGINAL="service/user/docker/Dockerfile"
DOCKERFILE_OPTIMIZED="service/user/docker/Dockerfile.ci"
BUILD_CONTEXT="."
PLATFORM="linux/amd64"
IMAGE_PREFIX="techwikiplus-user-service"

# Helper functions
print_header() {
    echo -e "\n${PURPLE}===================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${PURPLE}===================================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

format_time() {
    local seconds=$1
    local minutes=$((seconds / 60))
    local remaining_seconds=$((seconds % 60))
    echo "${minutes}m ${remaining_seconds}s"
}

# Check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed"
        exit 1
    fi
    print_success "Docker is installed: $(docker --version)"
    
    if ! docker buildx version &> /dev/null; then
        print_error "Docker Buildx is not available"
        exit 1
    fi
    print_success "Docker Buildx is available: $(docker buildx version)"
    
    # Check if BuildKit is enabled
    if [ "$DOCKER_BUILDKIT" != "1" ]; then
        export DOCKER_BUILDKIT=1
        print_warning "Enabled Docker BuildKit"
    fi
    
    # Create buildx builder if not exists
    if ! docker buildx ls | grep -q "buildkit-perf-test"; then
        print_info "Creating buildx builder..."
        docker buildx create --name buildkit-perf-test --driver docker-container --use
    else
        docker buildx use buildkit-perf-test
    fi
}

# Clean build cache
clean_cache() {
    print_header "Cleaning Build Cache"
    
    print_info "Removing Docker build cache..."
    docker buildx prune -af --builder buildkit-perf-test || true
    
    print_info "Removing local cache directories..."
    rm -rf /tmp/buildkit-cache /tmp/docker-cache
    mkdir -p /tmp/buildkit-cache /tmp/docker-cache
    
    print_success "Cache cleaned"
}

# Test build performance
test_build() {
    local dockerfile=$1
    local test_name=$2
    local cache_type=$3
    local cache_args=$4
    
    print_header "Testing: $test_name"
    print_info "Dockerfile: $dockerfile"
    print_info "Cache Type: $cache_type"
    
    local start_time=$(date +%s)
    
    # Build command
    local build_cmd="docker buildx build \
        --platform $PLATFORM \
        --file $dockerfile \
        --tag ${IMAGE_PREFIX}:${test_name}-test \
        $cache_args \
        --progress=plain \
        --build-arg VERSION=test-$(date +%Y%m%d%H%M%S) \
        $BUILD_CONTEXT"
    
    echo -e "${BLUE}Build Command:${NC}"
    echo "$build_cmd"
    echo
    
    # Execute build
    if eval "$build_cmd" > "/tmp/build-${test_name}.log" 2>&1; then
        local end_time=$(date +%s)
        local build_time=$((end_time - start_time))
        
        print_success "Build completed in $(format_time $build_time)"
        
        # Extract stage timings if available
        echo -e "\n${BLUE}Stage Timings:${NC}"
        grep -E "(STEP|CACHED|DONE)" "/tmp/build-${test_name}.log" | tail -10 || true
        
        echo "$test_name,$build_time,$cache_type" >> build-performance-results.csv
    else
        print_error "Build failed! Check /tmp/build-${test_name}.log"
        tail -20 "/tmp/build-${test_name}.log"
    fi
}

# Run performance tests
run_performance_tests() {
    print_header "Running Performance Tests"
    
    # Initialize results file
    echo "Test Name,Build Time (seconds),Cache Type" > build-performance-results.csv
    
    # Test 1: Original Dockerfile without cache
    clean_cache
    test_build "$DOCKERFILE_ORIGINAL" "original-no-cache" "none" "--no-cache"
    
    # Test 2: Original Dockerfile with local cache
    test_build "$DOCKERFILE_ORIGINAL" "original-local-cache" "local" \
        "--cache-from type=local,src=/tmp/buildkit-cache --cache-to type=local,dest=/tmp/buildkit-cache,mode=max"
    
    # Test 3: Optimized Dockerfile without cache
    clean_cache
    test_build "$DOCKERFILE_OPTIMIZED" "optimized-no-cache" "none" "--no-cache"
    
    # Test 4: Optimized Dockerfile with local cache
    test_build "$DOCKERFILE_OPTIMIZED" "optimized-local-cache" "local" \
        "--cache-from type=local,src=/tmp/docker-cache --cache-to type=local,dest=/tmp/docker-cache,mode=max"
    
    # Test 5: Optimized Dockerfile with inline cache
    test_build "$DOCKERFILE_OPTIMIZED" "optimized-inline-cache" "inline" \
        "--cache-from ${IMAGE_PREFIX}:optimized-inline-cache-test --cache-to type=inline --build-arg BUILDKIT_INLINE_CACHE=1"
}

# Generate performance report
generate_report() {
    print_header "Performance Test Report"
    
    if [ -f build-performance-results.csv ]; then
        echo -e "${BLUE}Test Results:${NC}"
        column -t -s, build-performance-results.csv
        echo
        
        # Calculate improvements
        if [ -f build-performance-results.csv ]; then
            local original_no_cache=$(grep "original-no-cache" build-performance-results.csv | cut -d, -f2)
            local optimized_no_cache=$(grep "optimized-no-cache" build-performance-results.csv | cut -d, -f2)
            local original_cached=$(grep "original-local-cache" build-performance-results.csv | cut -d, -f2 | tail -1)
            local optimized_cached=$(grep "optimized-local-cache" build-performance-results.csv | cut -d, -f2 | tail -1)
            
            if [ -n "$original_no_cache" ] && [ -n "$optimized_no_cache" ]; then
                local improvement=$(( (original_no_cache - optimized_no_cache) * 100 / original_no_cache ))
                echo -e "${GREEN}Performance Improvement (No Cache): ${improvement}%${NC}"
            fi
            
            if [ -n "$original_cached" ] && [ -n "$optimized_cached" ]; then
                local improvement=$(( (original_cached - optimized_cached) * 100 / original_cached ))
                echo -e "${GREEN}Performance Improvement (With Cache): ${improvement}%${NC}"
            fi
        fi
        
        echo -e "\n${BLUE}Recommendations:${NC}"
        echo "1. Use the optimized Dockerfile for CI/CD builds"
        echo "2. Enable BuildKit cache mounts for Gradle dependencies"
        echo "3. Use GitHub Actions cache or registry-based caching"
        echo "4. Consider using distributed BuildKit for large-scale builds"
        echo "5. Monitor cache hit rates and adjust cache strategies accordingly"
    else
        print_error "No test results found"
    fi
}

# Main execution
main() {
    print_header "Docker Build Performance Testing"
    
    check_prerequisites
    
    # Ask user what to test
    echo -e "${BLUE}Select test mode:${NC}"
    echo "1. Run all performance tests (recommended)"
    echo "2. Test original Dockerfile only"
    echo "3. Test optimized Dockerfile only"
    echo "4. Quick cache test"
    echo "5. Clean cache and exit"
    
    read -p "Enter your choice (1-5): " choice
    
    case $choice in
        1)
            run_performance_tests
            generate_report
            ;;
        2)
            clean_cache
            test_build "$DOCKERFILE_ORIGINAL" "original-test" "local" \
                "--cache-from type=local,src=/tmp/buildkit-cache --cache-to type=local,dest=/tmp/buildkit-cache,mode=max"
            ;;
        3)
            clean_cache
            test_build "$DOCKERFILE_OPTIMIZED" "optimized-test" "local" \
                "--cache-from type=local,src=/tmp/docker-cache --cache-to type=local,dest=/tmp/docker-cache,mode=max"
            ;;
        4)
            # Quick test with existing cache
            test_build "$DOCKERFILE_OPTIMIZED" "quick-cache-test" "local" \
                "--cache-from type=local,src=/tmp/docker-cache --cache-to type=local,dest=/tmp/docker-cache,mode=max"
            ;;
        5)
            clean_cache
            ;;
        *)
            print_error "Invalid choice"
            exit 1
            ;;
    esac
    
    print_success "Testing completed!"
}

# Run main function
main "$@"