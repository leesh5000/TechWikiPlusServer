# TechWiki Plus

![CodeRabbit Pull Request Reviews](https://img.shields.io/coderabbit/prs/github/leesh5000/TechWikiPlusServer?utm_source=oss&utm_medium=github&utm_campaign=leesh5000%2FTechWikiPlusServer&labelColor=171717&color=FF570A&link=https%3A%2F%2Fcoderabbit.ai&label=CodeRabbit+Reviews)

> AI와 인간이 협업하여 만드는 신뢰할 수 있는 기술 지식 플랫폼

## 🚀 프로젝트 개요

TechWiki Plus는 AI가 생성한 기술 콘텐츠를 크라우드소싱을 통해 검증하고 개선하는 혁신적인 플랫폼입니다. 기여자들에게 경제적 보상을 제공하여 지속가능한 생태계를 구축하고, 고품질의 기술 문서를 무료로 제공합니다.

### 🎯 핵심 가치

- **정확성**: 커뮤니티 검증을 통한 신뢰할 수 있는 콘텐츠
- **보상**: 기여에 대한 공정한 경제적 보상
- **접근성**: 누구나 무료로 양질의 기술 문서 열람 가능
- **투명성**: 모든 편집 이력과 기여도 공개

## 📚 문서 구조

### 📋 [기획서](./docs/기획서.md)

프로젝트의 전체적인 비전과 방향성을 담은 문서입니다.

- 프로젝트 개요 및 비전
- 시장 분석 및 타겟 사용자
- 핵심 기능 정의
- 경쟁사 분석 및 차별화 포인트

### 📝 [PRD (Product Requirements Document)](./docs/PRD.md)

제품의 구체적인 요구사항과 기능을 정의한 문서입니다.

- 사용자 스토리
- 기능 요구사항 (MVP 및 Phase 2)
- 비기능 요구사항 (성능, 보안, 사용성)

### 🔧 [TRD (Technical Requirements Document)](./docs/TRD.md)

기술적 구현 방향과 아키텍처를 다룬 문서입니다.

- 시스템 아키텍처
- 기술 스택 선정
- 데이터베이스 설계
- API 설계
- 보안 요구사항
- 개발 로드맵

## 🏗️ 기술 스택

### Frontend

- [해당 저장소](https://github.com/leesh5000/TechWikiPlus)를 확인 바랍니다.

### Backend

- Spring Boot 3.x (LTS)
- Kotlin (JVM21) with kapt
- JPA/Hibernate
- Spring Boot Configuration Processor

### Database

- MySQL 8.0+
- Redis (캐시)
- Elasticsearch (검색)

## 🚀 빠른 시작

### Docker를 이용한 실행

```bash
# 환경 변수 설정
cp .env.example .env
# .env 파일을 편집하여 필수 값 설정:
# - JWT_SECRET: 보안 키 (32자 이상)
# - MAIL_USERNAME: 이메일 주소
# - MAIL_PASSWORD: 이메일 앱 비밀번호

# 개발 환경에서는 콘솔 메일 전송 사용 가능
# - SPRING_MAIL_TYPE=console

# 서비스 빌드 및 실행
./docker-build.sh
```

### 테스트 실행

```bash
# 로컬에서 테스트 실행
./gradlew test

# CI/CD에서 자동 실행 (GitHub Actions)
```

자세한 설정 방법은 [Docker 설정 가이드](docs/docker-setup.md)를 참조하세요.

## 🚦 개발 현황

현재 프로젝트는 **기획 단계**에 있습니다.

### Phase 1: MVP (예정 - 3개월)

- [x] 프로젝트 셋업 및 인프라 구축
- [x] 인증 시스템 및 사용자 관리
- [x] CI/CD 파이프라인 구축 (GitHub Actions)
- [x] 메일 전송 시스템 (환경별 설정 가능)
- [ ] AI 콘텐츠 생성 파이프라인
- [ ] 편집 요청 시스템
- [ ] 검증 및 투표 시스템
- [ ] 포인트 시스템 및 광고 통합

## 🤝 기여하기

프로젝트에 기여하고 싶으시다면 다음 문서들을 먼저 읽어보시기 바랍니다:

1. [기획서](./docs/기획서.md) - 프로젝트의 전체 방향성 이해
2. [PRD](./docs/PRD.md) - 제품 요구사항 파악
3. [TRD](./docs/TRD.md) - 기술적 구현 방향 확인

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 공개됩니다.
