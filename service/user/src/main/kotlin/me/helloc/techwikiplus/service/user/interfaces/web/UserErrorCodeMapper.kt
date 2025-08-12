package me.helloc.techwikiplus.service.common.interfaces

import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class UserErrorCodeMapper {
    fun mapToHttpStatus(userErrorCode: UserErrorCode): HttpStatus {
        return when (userErrorCode) {
            // User Status
            UserErrorCode.USER_DORMANT,
            UserErrorCode.USER_BANNED,
            UserErrorCode.USER_PENDING,
            -> HttpStatus.FORBIDDEN

            UserErrorCode.USER_DELETED -> HttpStatus.GONE

            // User Management
            UserErrorCode.DUPLICATE_EMAIL,
            UserErrorCode.DUPLICATE_NICKNAME,
            -> HttpStatus.CONFLICT
            UserErrorCode.USER_NOT_FOUND,
            UserErrorCode.NO_STATUS_USER,
            UserErrorCode.NOT_FOUND_PENDING_USER,
            -> HttpStatus.NOT_FOUND

            // Authentication
            UserErrorCode.INVALID_CREDENTIALS,
            UserErrorCode.INVALID_TOKEN,
            UserErrorCode.TOKEN_EXPIRED,
            UserErrorCode.INVALID_TOKEN_TYPE,
            UserErrorCode.UNAUTHORIZED,
            -> HttpStatus.UNAUTHORIZED

            UserErrorCode.FORBIDDEN -> HttpStatus.FORBIDDEN

            UserErrorCode.PASSWORD_MISMATCH -> HttpStatus.BAD_REQUEST

            // Verification
            UserErrorCode.INVALID_VERIFICATION_CODE,
            UserErrorCode.CODE_MISMATCH,
            -> HttpStatus.BAD_REQUEST

            UserErrorCode.REGISTRATION_EXPIRED -> HttpStatus.NOT_FOUND

            // Notification
            UserErrorCode.NOTIFICATION_FAILED -> HttpStatus.SERVICE_UNAVAILABLE

            // Application Level
            UserErrorCode.SIGNUP_FAILED,
            UserErrorCode.LOGIN_FAILED,
            UserErrorCode.VERIFICATION_FAILED,
            -> HttpStatus.INTERNAL_SERVER_ERROR

            // Email Validation
            UserErrorCode.BLANK_EMAIL,
            UserErrorCode.INVALID_EMAIL_FORMAT,

            // Nickname Validation
            UserErrorCode.BLANK_NICKNAME,
            UserErrorCode.NICKNAME_TOO_SHORT,
            UserErrorCode.NICKNAME_TOO_LONG,
            UserErrorCode.NICKNAME_CONTAINS_SPACE,
            UserErrorCode.NICKNAME_CONTAINS_SPECIAL_CHAR,

            // Password Validation
            UserErrorCode.BLANK_PASSWORD,
            UserErrorCode.PASSWORD_TOO_SHORT,
            UserErrorCode.PASSWORD_TOO_LONG,
            UserErrorCode.PASSWORD_NO_UPPERCASE,
            UserErrorCode.PASSWORD_NO_LOWERCASE,
            UserErrorCode.PASSWORD_NO_SPECIAL_CHAR,

            // UserId Validation
            UserErrorCode.BLANK_USER_ID,
            UserErrorCode.USER_ID_TOO_LONG,
            UserErrorCode.INVALID_USER_ID_FORMAT,

            // Generic
            UserErrorCode.VALIDATION_ERROR,
            -> HttpStatus.BAD_REQUEST
            UserErrorCode.DOMAIN_ERROR,
            UserErrorCode.INTERNAL_ERROR,
            -> HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    fun mapToMessage(
        userErrorCode: UserErrorCode,
        params: Array<out Any?>,
    ): String {
        val baseMessage =
            when (userErrorCode) {
                UserErrorCode.USER_DORMANT -> "휴면 계정입니다. 관리자에게 문의해주세요"
                UserErrorCode.USER_BANNED -> "차단된 계정입니다. 관리자에게 문의해주세요"
                UserErrorCode.USER_PENDING -> "인증 대기중인 계정입니다. 이메일 인증을 완료 후 다시 시도해주세요."
                UserErrorCode.USER_DELETED -> "이미 삭제된 계정입니다."
                UserErrorCode.NOT_FOUND_PENDING_USER ->
                    if (params.isNotEmpty()) {
                        "인증 대기중인 사용자(${params[0]})를 찾을 수 없습니다"
                    } else {
                        "인증 대기중인 사용자를 찾을 수 없습니다"
                    }
                UserErrorCode.DUPLICATE_EMAIL ->
                    if (params.isNotEmpty()) {
                        "이미 사용중인 이메일(${params[0]})입니다"
                    } else {
                        "이미 사용중인 이메일입니다"
                    }
                UserErrorCode.DUPLICATE_NICKNAME ->
                    if (params.isNotEmpty()) {
                        "이미 사용중인 닉네임(${params[0]})입니다"
                    } else {
                        "이미 사용중인 닉네임입니다"
                    }
                UserErrorCode.USER_NOT_FOUND ->
                    if (params.isNotEmpty()) {
                        "사용자(${params[0]})를 찾을 수 없습니다"
                    } else {
                        "사용자를 찾을 수 없습니다"
                    }
                UserErrorCode.NO_STATUS_USER ->
                    if (params.isNotEmpty()) {
                        "${params[0]} 상태의 사용자를 찾을 수 없습니다"
                    } else {
                        "사용자를 찾을 수 없습니다"
                    }
                UserErrorCode.INVALID_CREDENTIALS -> "인증 정보가 올바르지 않습니다"
                UserErrorCode.PASSWORD_MISMATCH -> "비밀번호가 일치하지 않습니다"
                UserErrorCode.UNAUTHORIZED -> "인증이 필요합니다"
                UserErrorCode.FORBIDDEN -> "접근 권한이 없습니다"
                UserErrorCode.INVALID_TOKEN -> "유효하지 않은 토큰입니다"
                UserErrorCode.TOKEN_EXPIRED -> "만료된 토큰입니다"
                UserErrorCode.INVALID_TOKEN_TYPE ->
                    if (params.isNotEmpty()) {
                        "잘못된 토큰 타입(${params[0]})입니다"
                    } else {
                        "잘못된 토큰 타입입니다"
                    }
                UserErrorCode.INVALID_VERIFICATION_CODE ->
                    if (params.isNotEmpty()) {
                        "유효하지 않은 인증 코드(${params[0]})입니다"
                    } else {
                        "유효하지 않은 인증 코드입니다"
                    }
                UserErrorCode.REGISTRATION_EXPIRED ->
                    "회원 가입 요청이 만료되었습니다. 인증 코드 다시 발송 후 재인증 해주세요."
                UserErrorCode.CODE_MISMATCH -> "인증 코드가 일치하지 않습니다"
                UserErrorCode.NOTIFICATION_FAILED -> "알림 전송에 실패했습니다"
                UserErrorCode.SIGNUP_FAILED -> "회원가입 처리 중 오류가 발생했습니다"
                UserErrorCode.LOGIN_FAILED -> "로그인 처리 중 오류가 발생했습니다"
                UserErrorCode.VERIFICATION_FAILED -> "인증 처리 중 오류가 발생했습니다"

                // Email Validation
                UserErrorCode.BLANK_EMAIL -> "이메일은 필수 입력 항목입니다"
                UserErrorCode.INVALID_EMAIL_FORMAT -> "올바른 이메일 형식이 아닙니다"

                // Nickname Validation
                UserErrorCode.BLANK_NICKNAME -> "닉네임은 필수 입력 항목입니다"
                UserErrorCode.NICKNAME_TOO_SHORT ->
                    if (params.size > 1) {
                        "닉네임은 최소 ${params[1]}자 이상이어야 합니다"
                    } else {
                        "닉네임이 너무 짧습니다"
                    }
                UserErrorCode.NICKNAME_TOO_LONG ->
                    if (params.size > 1) {
                        "닉네임은 최대 ${params[1]}자 이하여야 합니다"
                    } else {
                        "닉네임이 너무 깁니다"
                    }
                UserErrorCode.NICKNAME_CONTAINS_SPACE -> "닉네임에는 공백을 포함할 수 없습니다"
                UserErrorCode.NICKNAME_CONTAINS_SPECIAL_CHAR ->
                    "닉네임은 한글, 영문, 숫자, 언더스코어(_), 하이픈(-)만 사용할 수 있습니다"

                // Password Validation
                UserErrorCode.BLANK_PASSWORD -> "비밀번호는 필수 입력 항목입니다"
                UserErrorCode.PASSWORD_TOO_SHORT ->
                    if (params.size > 1) {
                        "비밀번호는 최소 ${params[1]}자 이상이어야 합니다"
                    } else {
                        "비밀번호가 너무 짧습니다"
                    }
                UserErrorCode.PASSWORD_TOO_LONG ->
                    if (params.size > 1) {
                        "비밀번호는 최대 ${params[1]}자 이하여야 합니다"
                    } else {
                        "비밀번호가 너무 깁니다"
                    }
                UserErrorCode.PASSWORD_NO_UPPERCASE -> "비밀번호는 대문자를 포함해야 합니다"
                UserErrorCode.PASSWORD_NO_LOWERCASE -> "비밀번호는 소문자를 포함해야 합니다"
                UserErrorCode.PASSWORD_NO_SPECIAL_CHAR -> "비밀번호는 특수문자를 포함해야 합니다"

                // UserId Validation
                UserErrorCode.BLANK_USER_ID -> "사용자 ID는 필수 입력 항목입니다"
                UserErrorCode.USER_ID_TOO_LONG ->
                    if (params.size > 1) {
                        "사용자 ID는 최대 ${params[1]}자 이하여야 합니다"
                    } else {
                        "사용자 ID가 너무 깁니다"
                    }
                UserErrorCode.INVALID_USER_ID_FORMAT ->
                    if (params.isNotEmpty()) {
                        "유효하지 않은 사용자 ID 형식입니다: ${params[0]}"
                    } else {
                        "유효하지 않은 사용자 ID 형식입니다"
                    }

                UserErrorCode.VALIDATION_ERROR ->
                    if (params.isNotEmpty()) {
                        "검증 실패: ${params[0]}"
                    } else {
                        "검증 실패"
                    }
                UserErrorCode.DOMAIN_ERROR -> "도메인 처리 중 오류가 발생했습니다"
                UserErrorCode.INTERNAL_ERROR -> "시스템 오류가 발생했습니다"
            }

        return baseMessage
    }
}
