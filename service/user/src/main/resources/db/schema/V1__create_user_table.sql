-- User 테이블 생성
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL COMMENT '사용자 고유 식별자',
    `email` VARCHAR(255) NOT NULL COMMENT '사용자 이메일',
    `email_verified` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '이메일 인증 여부',
    `password` VARCHAR(255) NOT NULL COMMENT '암호화된 비밀번호',
    `nickname` VARCHAR(20) NOT NULL COMMENT '사용자 닉네임',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_email` (`email`),
    UNIQUE KEY `uk_user_nickname` (`nickname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 정보';
