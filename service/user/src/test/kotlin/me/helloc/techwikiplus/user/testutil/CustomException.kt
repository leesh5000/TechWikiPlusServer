package me.helloc.techwikiplus.user.testutil

/**
 * 테스트에서 사용하는 사용자 정의 예외
 * 예상치 못한 예외 상황을 시뮬레이션하기 위해 사용
 */
class CustomException(message: String) : RuntimeException(message)
