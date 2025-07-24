#!/bin/bash

# Docker BuildKit 활성화
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}🐳 TechWikiPlus Docker 빌드 시작${NC}"
echo -e "${BLUE}ℹ️  로컬 개발 환경용 빌드 (테스트 제외)${NC}"

# .env 파일 확인
if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠️  .env 파일이 없습니다. .env.example을 복사합니다.${NC}"
    cp .env.example .env
    echo -e "${RED}❗ .env 파일에서 JWT_SECRET, MAIL_USERNAME, MAIL_PASSWORD를 설정하세요.${NC}"
    exit 1
fi

# 빌드 및 실행
echo -e "${GREEN}📦 이미지 빌드 중...${NC}"
docker-compose build

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ 빌드 성공!${NC}"
    echo -e "${GREEN}🚀 서비스 시작 중...${NC}"
    docker-compose up -d
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✨ 모든 서비스가 시작되었습니다!${NC}"
        echo ""
        echo -e "${GREEN}서비스 접속 정보:${NC}"
        echo -e "  - User Service API: http://localhost:9000"
        echo -e "  - MySQL: localhost:13306"
        echo -e "  - Redis: localhost:16379"
        echo ""
        echo -e "${BLUE}테스트 실행 방법:${NC}"
        echo -e "  - 로컬: ./gradlew test"
        echo -e "  - CI/CD: GitHub Actions에서 자동 실행"
        echo ""
        echo -e "${YELLOW}로그 확인: docker-compose logs -f${NC}"
    else
        echo -e "${RED}❌ 서비스 시작 실패${NC}"
        exit 1
    fi
else
    echo -e "${RED}❌ 빌드 실패${NC}"
    echo -e "${YELLOW}상세 로그를 확인하려면: docker-compose logs${NC}"
    exit 1
fi