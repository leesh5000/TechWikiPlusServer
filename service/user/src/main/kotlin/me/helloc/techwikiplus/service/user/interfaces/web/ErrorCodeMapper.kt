package me.helloc.techwikiplus.service.user.interfaces.web

import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class ErrorCodeMapper {
    fun mapToHttpStatus(errorCode: ErrorCode): HttpStatus {
        return when (errorCode) {
            // User Status
            ErrorCode.USER_DORMANT,
            ErrorCode.USER_BANNED,
            ErrorCode.USER_PENDING -> HttpStatus.FORBIDDEN
            
            ErrorCode.USER_DELETED -> HttpStatus.GONE
            
            // User Management
            ErrorCode.USER_ALREADY_EXISTS -> HttpStatus.CONFLICT
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.PENDING_USER_NOT_FOUND -> HttpStatus.NOT_FOUND
            
            // Authentication
            ErrorCode.INVALID_CREDENTIALS,
            ErrorCode.INVALID_TOKEN,
            ErrorCode.TOKEN_EXPIRED,
            ErrorCode.INVALID_TOKEN_TYPE -> HttpStatus.UNAUTHORIZED
            
            ErrorCode.PASSWORDS_MISMATCH -> HttpStatus.BAD_REQUEST
            
            // Verification
            ErrorCode.INVALID_VERIFICATION_CODE,
            ErrorCode.CODE_MISMATCH -> HttpStatus.BAD_REQUEST
            
            ErrorCode.REGISTRATION_NOT_FOUND -> HttpStatus.NOT_FOUND
            
            // Notification
            ErrorCode.NOTIFICATION_FAILED -> HttpStatus.SERVICE_UNAVAILABLE
            
            // Application Level
            ErrorCode.SIGNUP_FAILED,
            ErrorCode.LOGIN_FAILED,
            ErrorCode.VERIFICATION_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR
            
            // Email Validation
            ErrorCode.BLANK_EMAIL,
            ErrorCode.INVALID_EMAIL_FORMAT,
            
            // Nickname Validation
            ErrorCode.BLANK_NICKNAME,
            ErrorCode.NICKNAME_TOO_SHORT,
            ErrorCode.NICKNAME_TOO_LONG,
            ErrorCode.NICKNAME_CONTAINS_SPACE,
            ErrorCode.NICKNAME_CONTAINS_SPECIAL_CHAR,
            
            // Password Validation
            ErrorCode.BLANK_PASSWORD,
            ErrorCode.PASSWORD_TOO_SHORT,
            ErrorCode.PASSWORD_TOO_LONG,
            ErrorCode.PASSWORD_NO_UPPERCASE,
            ErrorCode.PASSWORD_NO_LOWERCASE,
            ErrorCode.PASSWORD_NO_SPECIAL_CHAR,
            
            // UserId Validation
            ErrorCode.BLANK_USER_ID,
            ErrorCode.USER_ID_TOO_LONG,
            
            // Generic
            ErrorCode.VALIDATION_ERROR -> HttpStatus.BAD_REQUEST
            ErrorCode.DOMAIN_ERROR,
            ErrorCode.INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR
        }
    }
    
    fun mapToMessage(errorCode: ErrorCode, params: Array<out Any?>): String {
        val baseMessage = when (errorCode) {
            ErrorCode.USER_DORMANT -> "휴면 계정입니다"
            ErrorCode.USER_BANNED -> "차단된 계정입니다"
            ErrorCode.USER_PENDING -> "인증 대기중인 계정입니다"
            ErrorCode.USER_DELETED -> "삭제된 계정입니다"
            ErrorCode.USER_ALREADY_EXISTS -> if (params.isNotEmpty()) "이미 존재하는 사용자입니다: ${params[0]}" else "이미 존재하는 사용자입니다"
            ErrorCode.USER_NOT_FOUND -> if (params.isNotEmpty()) "사용자를 찾을 수 없습니다: ${params[0]}" else "사용자를 찾을 수 없습니다"
            ErrorCode.PENDING_USER_NOT_FOUND -> if (params.isNotEmpty()) "대기중인 사용자를 찾을 수 없습니다: ${params[0]}" else "대기중인 사용자를 찾을 수 없습니다"
            ErrorCode.INVALID_CREDENTIALS -> "인증 정보가 올바르지 않습니다"
            ErrorCode.PASSWORDS_MISMATCH -> "비밀번호가 일치하지 않습니다"
            ErrorCode.INVALID_TOKEN -> "유효하지 않은 토큰입니다"
            ErrorCode.TOKEN_EXPIRED -> "만료된 토큰입니다"
            ErrorCode.INVALID_TOKEN_TYPE -> if (params.isNotEmpty()) "잘못된 토큰 타입입니다: ${params[0]}" else "잘못된 토큰 타입입니다"
            ErrorCode.INVALID_VERIFICATION_CODE -> if (params.isNotEmpty()) "유효하지 않은 인증 코드입니다: ${params[0]}" else "유효하지 않은 인증 코드입니다"
            ErrorCode.REGISTRATION_NOT_FOUND -> if (params.isNotEmpty()) "회원가입 정보를 찾을 수 없습니다: ${params[0]}" else "회원가입 정보를 찾을 수 없습니다"
            ErrorCode.CODE_MISMATCH -> "인증 코드가 일치하지 않습니다"
            ErrorCode.NOTIFICATION_FAILED -> "알림 전송에 실패했습니다"
            ErrorCode.SIGNUP_FAILED -> "회원가입 처리 중 오류가 발생했습니다"
            ErrorCode.LOGIN_FAILED -> "로그인 처리 중 오류가 발생했습니다"
            ErrorCode.VERIFICATION_FAILED -> "인증 처리 중 오류가 발생했습니다"
            
            // Email Validation
            ErrorCode.BLANK_EMAIL -> "이메일은 필수 입력 항목입니다"
            ErrorCode.INVALID_EMAIL_FORMAT -> "올바른 이메일 형식이 아닙니다"
            
            // Nickname Validation
            ErrorCode.BLANK_NICKNAME -> "닉네임은 필수 입력 항목입니다"
            ErrorCode.NICKNAME_TOO_SHORT -> if (params.size > 1) "닉네임은 최소 ${params[1]}자 이상이어야 합니다" else "닉네임이 너무 짧습니다"
            ErrorCode.NICKNAME_TOO_LONG -> if (params.size > 1) "닉네임은 최대 ${params[1]}자 이하여야 합니다" else "닉네임이 너무 깁니다"
            ErrorCode.NICKNAME_CONTAINS_SPACE -> "닉네임에는 공백을 포함할 수 없습니다"
            ErrorCode.NICKNAME_CONTAINS_SPECIAL_CHAR -> "닉네임은 한글, 영문, 숫자, 언더스코어(_), 하이픈(-)만 사용할 수 있습니다"
            
            // Password Validation
            ErrorCode.BLANK_PASSWORD -> "비밀번호는 필수 입력 항목입니다"
            ErrorCode.PASSWORD_TOO_SHORT -> if (params.size > 1) "비밀번호는 최소 ${params[1]}자 이상이어야 합니다" else "비밀번호가 너무 짧습니다"
            ErrorCode.PASSWORD_TOO_LONG -> if (params.size > 1) "비밀번호는 최대 ${params[1]}자 이하여야 합니다" else "비밀번호가 너무 깁니다"
            ErrorCode.PASSWORD_NO_UPPERCASE -> "비밀번호는 대문자를 포함해야 합니다"
            ErrorCode.PASSWORD_NO_LOWERCASE -> "비밀번호는 소문자를 포함해야 합니다"
            ErrorCode.PASSWORD_NO_SPECIAL_CHAR -> "비밀번호는 특수문자를 포함해야 합니다"
            
            // UserId Validation
            ErrorCode.BLANK_USER_ID -> "사용자 ID는 필수 입력 항목입니다"
            ErrorCode.USER_ID_TOO_LONG -> if (params.size > 1) "사용자 ID는 최대 ${params[1]}자 이하여야 합니다" else "사용자 ID가 너무 깁니다"
            
            ErrorCode.VALIDATION_ERROR -> if (params.isNotEmpty()) "검증 실패: ${params[0]}" else "검증 실패"
            ErrorCode.DOMAIN_ERROR -> "도메인 처리 중 오류가 발생했습니다"
            ErrorCode.INTERNAL_ERROR -> "시스템 오류가 발생했습니다"
        }
        
        return baseMessage
    }
}