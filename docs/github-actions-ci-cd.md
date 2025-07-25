# GitHub Actions CI/CD 파이프라인

이 문서는 TechWikiPlus 프로젝트의 GitHub Actions CI/CD 파이프라인을 설명합니다.

## 파이프라인 구조

### Job 구성

#### 1. **lint** - 코드 스타일 검사
- ktlint를 사용한 코드 스타일 검사
- test job과 병렬 실행
- **Job Summary**: ktlint 검사 통과/실패 여부 표시

#### 2. **test** - 테스트 실행
- 단위 및 통합 테스트 실행
- 테스트 리포트 업로드
- lint job과 병렬 실행
- **Job Summary**: 
  - 테스트 성공/실패 상태
  - 모듈별 테스트 리포트 생성 여부
  - Artifacts 링크 제공

#### 3. **build** - 애플리케이션 빌드
- Gradle로 JAR 파일 빌드
- lint와 test가 모두 성공해야 실행
- 빌드된 JAR을 artifact로 업로드
- **Job Summary**: 
  - 빌드 성공/실패 상태
  - JAR 파일명 및 크기 정보

#### 4. **docker-build** - Docker 이미지 빌드
- Docker 이미지 빌드 및 ECR 푸시
- build job의 JAR 파일 사용
- main/develop 브랜치에서만 실행
- **Job Summary**: 
  - ECR Registry 정보
  - 생성된 이미지 태그 목록
  - 푸시 성공 확인

#### 5. **deploy-staging** - 스테이징 배포
- develop 브랜치에서 자동 배포
- staging environment 사용
- **Job Summary**: 배포 환경 및 이미지 정보

#### 6. **deploy-production** - 프로덕션 배포
- main 브랜치에서만 실행
- AWS environment 사용
- EC2로 docker-compose 파일들 (base.yml, prod.yml) 전송 및 실행
- 다중 compose 파일 사용: `-f docker-compose.base.yml -f docker-compose.prod.yml`
- **Job Summary**: 
  - 배포 성공/실패 상태
  - 배포 시간 및 배포자 정보
  - 컨테이너 상태
  - 다음 단계 가이드

## 트리거 조건

### 전체 파이프라인
- **푸시**: main, develop 브랜치
- **PR**: main 브랜치로의 PR (opened, synchronize, reopened)

### Job별 조건
- **lint, test**: 모든 트리거에서 실행
- **build**: lint와 test가 성공해야 실행
- **docker-build**: push 이벤트 또는 머지된 PR
- **deploy-staging**: develop 브랜치로의 push 또는 머지된 PR
- **deploy-production**: main 브랜치로의 push 또는 머지된 PR

## 주요 기능

1. **병렬 처리**
   - lint와 test job이 동시 실행
   - 빠른 피드백 제공

2. **브랜치별 배포**
   - main 브랜치: 프로덕션 환경
   - develop 브랜치: 스테이징 환경

3. **자동 테스트**
   - 코드 스타일 검사
   - 단위/통합 테스트

4. **Docker 이미지 관리**
   - ECR로 자동 푸시
   - 태그: 브랜치명, 날짜-커밋해시, latest
   - 배포 시에는 날짜-커밋해시 태그 사용 (버전 추적 가능)

5. **Artifact 공유**
   - 빌드된 JAR 파일을 job 간 공유
   - Docker 빌드 시간 단축

6. **Job Summary 제공**
   - 각 Job의 실행 결과를 시각적으로 표시
   - 빠른 상태 파악과 디버깅 지원
   - 테스트 결과, 빌드 정보, 배포 상태 요약

## 환경 변수

### Workflow 레벨
```yaml
env:
  AWS_REGION: ap-northeast-2
  ECR_REPOSITORY_NAME: techwikiplus/server/user-service
  PROJECT_DIRECTORY: techwikiplus-server
  PROJECT_NAME: techwikiplus-server-user-service  # Docker Compose 프로젝트 이름
  # 헬스체크 설정 - 서비스별로 변경 가능
  HEALTH_CHECK_URL: http://localhost:9000/actuator/health
  HEALTH_CHECK_MAX_RETRIES: 10
  HEALTH_CHECK_RETRY_DELAY: 5
```

#### 환경변수 설명
- `AWS_REGION`: AWS 리전 설정
- `ECR_REPOSITORY_NAME`: ECR 리포지토리 이름
- `PROJECT_DIRECTORY`: EC2에서 사용할 프로젝트 디렉토리
- `PROJECT_NAME`: Docker Compose 프로젝트 이름 (컨테이너 구분용)
- `HEALTH_CHECK_URL`: 서비스 헬스체크 URL (기본값: http://localhost:9000/actuator/health)
- `HEALTH_CHECK_MAX_RETRIES`: 헬스체크 최대 재시도 횟수
- `HEALTH_CHECK_RETRY_DELAY`: 헬스체크 재시도 간격(초)

## 필요한 GitHub Secrets

### AWS Environment
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `EC2_HOST`
- `EC2_USERNAME`
- `EC2_SSH_KEY`

### Staging Environment
- 스테이징 배포용 시크릿 (필요시 추가)

## 배포 프로세스

### 프로덕션 배포 (main 브랜치)
1. Docker 이미지가 ECR에 여러 태그로 푸시됨 (브랜치명, 날짜-커밋해시, latest)
2. SSH로 EC2 접속
3. docker-compose 파일들 및 deploy.sh 파일 전송:
   - `docker/compose/docker-compose.base.yml`
   - `docker/compose/docker-compose.prod.yml`
   - `scripts/deploy.sh`
4. deploy.sh 스크립트 실행:
   - 실제 태그(날짜-커밋해시) 사용
   - ECR에서 해당 태그의 이미지 pull
   - .env.prod 파일의 이미지 태그 업데이트
   - Docker Compose로 새 버전 배포 (다중 파일 사용):
     ```bash
     docker compose -p techwikiplus-server-user-service \
       -f docker/compose/docker-compose.base.yml \
       -f docker/compose/docker-compose.prod.yml \
       --env-file .env.prod up -d
     ```
   - 헬스체크 및 로그 확인

### 스테이징 배포 (develop 브랜치)
- TODO: 스테이징 환경 구성 필요

## 장점

1. **빠른 피드백**: 병렬 실행으로 빠른 결과 확인
2. **모듈화**: 각 job이 독립적으로 실행/재실행 가능
3. **효율성**: 필요한 경우에만 Docker 빌드 수행
4. **유연성**: 환경별로 다른 배포 전략 적용 가능
5. **가시성**: 각 단계의 성공/실패를 명확히 확인

## 중요 사항

### Docker 이미지 태그 정책
- **빌드**: 모든 태그 (브랜치명, 날짜-커밋해시, latest) 생성
- **배포**: 날짜-커밋해시 형식의 실제 태그 사용 (예: 20250124-abc1234)
- **Pull Policy**: `pull_policy: always`로 설정하여 항상 최신 이미지 확인

### 배포 실패 시 디버깅
배포 실패 시 다음 정보가 자동으로 수집됩니다:

1. **시스템 상태**
   - Docker 및 Docker Compose 버전
   - 디스크 공간 및 메모리 사용량

2. **Docker 상태**
   - 실행/중지된 컨테이너 목록
   - 각 서비스별 로그 (MySQL, Redis, User Service)
   - Docker 이벤트 로그

3. **환경 검증**
   - 필수 환경변수 확인
   - 네트워크 및 볼륨 상태

4. **오류 진단**
   - 가능한 오류 원인 제시
   - 해결 방법 가이드

### Job Summary
각 Job이 완료되면 GitHub Actions Summary 페이지에서 다음 정보를 확인할 수 있습니다:

- **코드 스타일 검사**: ktlint 실행 결과 및 수정 가이드
- **테스트 결과**: 모듈별 테스트 상태 및 리포트 링크
- **빌드 정보**: JAR 파일 정보 및 빌드 시간
- **Docker 이미지**: ECR 푸시 결과 및 태그 목록
- **배포 상태**: 
  - 배포 성공/실패 여부
  - 배포된 이미지 태그
  - 서비스 상태
  - 후속 조치 가이드

## 개선 가능한 사항

1. **매트릭스 빌드**: 여러 JDK 버전에서 테스트
2. **보안 스캔**: 의존성 취약점 검사 job 추가
3. **성능 테스트**: 부하 테스트 job 추가
4. **수동 승인**: 프로덕션 배포 전 승인 단계 추가
5. **알림**: Slack/이메일 알림 추가