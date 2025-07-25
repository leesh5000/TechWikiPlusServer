#!/bin/bash

# 크로스 플랫폼 진입점 스크립트 (Unix/Linux/Mac)

# OS 감지
OS=$(uname -s)
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "🐳 TechWikiPlus Docker Manager"
echo "OS detected: $OS"
echo ""

# Unix 스크립트 디렉토리로 이동
cd "$SCRIPT_DIR/../unix"

# Docker Manager 실행
if [ -f "docker-manager.sh" ]; then
    ./docker-manager.sh
else
    echo "Error: docker-manager.sh not found!"
    echo "Please ensure the script exists at: $SCRIPT_DIR/../unix/docker-manager.sh"
    exit 1
fi