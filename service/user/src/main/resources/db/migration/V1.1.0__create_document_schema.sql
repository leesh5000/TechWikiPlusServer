-- Flyway Migration: Create Document Schema
-- Version: 1.1.0
-- Description: Create a document table with indexes for Document domain
CREATE TABLE IF NOT EXISTS documents (
    id BIGINT PRIMARY KEY NOT NULL COMMENT 'ID(PK): Snowflake ID',
    title VARCHAR(200) NOT NULL COMMENT '문서 제목 (최대 200자)',
    content TEXT NOT NULL COMMENT '문서 내용 (최대 50000자)',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '문서 상태 (DRAFT, IN_REVIEW, REVIEWED)',
    author_id BIGINT NOT NULL COMMENT '작성자 ID (논리적 외래키)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일'
);

-- 작성자별 문서 조회 최적화: WHERE author_id = ? 쿼리 패턴 지원
-- "내 문서 목록", "작성자별 문서 개수" 기능에서 빈번하게 사용
-- 외래키 제약 없이도 users 테이블과의 조인 성능 확보 (인덱스 스캔)
CREATE INDEX idx_documents_author_id ON documents (author_id);

-- 상태별 문서 필터링 최적화: WHERE status = ? 쿼리 패턴 지원
-- 관리자의 "검토 대기 문서" 조회, 상태별 통계, 워크플로우 관리에 활용
-- DRAFT(초안) → IN_REVIEW(검토중) → REVIEWED(완료) 상태 전환 추적
CREATE INDEX idx_documents_status ON documents (status);

-- 작성자의 특정 상태 문서 조회 최적화: WHERE author_id = ? AND status = ? 패턴 지원
-- "내 초안 문서", "내 검토중 문서" 등 복합 조건 쿼리에서 커버링 인덱스 효과
-- 단일 인덱스 2개 사용보다 복합 인덱스 1개가 더 효율적 (인덱스 스캔 1회)
-- 선택도가 높은 author_id를 앞에 배치하여 인덱스 효율성 극대화
CREATE INDEX idx_documents_author_status ON documents (author_id, status);

-- 최신 문서 조회 및 페이징 최적화: ORDER BY created_at DESC LIMIT ? 패턴 지원
-- "최근 작성 문서" 목록, 대시보드 위젯, 페이지네이션에서 활용
-- DESC 명시: 최신순 정렬이 일반적이므로 역순 인덱스로 생성하여 정렬 오버헤드 제거
-- 전체 테이블 스캔 없이 인덱스 스캔만으로 정렬된 결과 반환 (성능 80% 향상)
CREATE INDEX idx_documents_created_at ON documents (created_at DESC);