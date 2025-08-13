-- MySQL RDB Schema for User Service
CREATE TABLE users (
                       id BIGINT PRIMARY KEY NOT NULL COMMENT 'ID(PK): Snowflake ID)',
                       email VARCHAR(255) UNIQUE NOT NULL COMMENT '이메일 (이메일을 만족하는 형식의 문자열)',
                       nickname VARCHAR(50) COLLATE utf8mb4_0900_ai_ci UNIQUE NOT NULL COMMENT '닉네임 (영문, 숫자, 특수문자 포함 20자 이내, 대소문자 구분 없음)',
                       password VARCHAR(255) NOT NULL COMMENT '암호화 된 비밀번호 (최대 255자)',
                       status VARCHAR(20) DEFAULT 'active' COMMENT '사용자 상태 (ACTIVE, DORMANT, BANNED, PENDING, DELETED)',
                       role VARCHAR(20) DEFAULT 'user' COMMENT '사용자 권한 (ADMIN, USER)',
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '가입일',
                       modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일'
);