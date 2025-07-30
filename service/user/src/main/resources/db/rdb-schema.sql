-- MySQL RDB Schema for User Service
CREATE TABLE users (
                    id CHAR(36) PRIMARY KEY NOT NULL COMMENT 'ID(PK)',
                    email VARCHAR(255) UNIQUE NOT NULL COMMENT '이메일',
                    nickname VARCHAR(50) UNIQUE NOT NULL COMMENT '닉네임',
                    password VARCHAR(255) NOT NULL COMMENT '암호화 된 비밀번호',
                    status VARCHAR(20) DEFAULT 'active' COMMENT '사용자 상태',
                    role VARCHAR(20) DEFAULT 'user' COMMENT '사용자 권한',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일'
);

-- 로그인 성능 최적화: WHERE email = ? AND status = 'active' 쿼리 패턴 지원
-- email은 이미 UNIQUE 인덱스이지만, status를 포함한 복합 인덱스로 커버링 인덱스 효과 달성
-- 비활성화된 계정 로그인 차단 로직에서 단일 인덱스 스캔으로 처리 가능 (성능 50% 향상 예상)
/**
  * 사용 예시
  * CREATE INDEX idx_email_status ON users (email, status);
    -- 방법 1 (대부분의 경우 더 빠름)
    -- Step 1: 계정 활성화 상태 확인 (커버링 인덱스 사용)
    SELECT status FROM users WHERE email = 'user@example.com';
    -- Step 2: 활성 계정인 경우에만 전체 정보 조회
    SELECT * FROM users WHERE email = 'user@example.com';
 */
CREATE INDEX idx_email_status ON users (email, status);

# -- 시간 기반 조회 최적화: 최근 가입자 조회, 페이징 처리에 필수
# -- ORDER BY created_at DESC LIMIT ? 패턴에서 전체 테이블 스캔 방지
# -- 대시보드의 "최근 가입 회원" 위젯 성능 70% 향상 예상
# CREATE INDEX idx_created_at ON users (created_at DESC);
#
# -- 상태/권한별 복합 조회 최적화: 관리자 페이지의 사용자 필터링 지원
# -- WHERE status = ? AND role = ? ORDER BY created_at DESC 패턴 최적화
# -- 활성 관리자 조회, 권한별 사용자 통계 등에서 활용 (성능 60% 향상 예상)
# CREATE INDEX idx_status_role_created_at ON users (status, role, created_at DESC);
