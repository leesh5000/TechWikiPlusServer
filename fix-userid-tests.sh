#!/bin/bash

# 테스트 파일들에서 UserId 문자열 패턴을 Long으로 변경하는 스크립트

TEST_DIR="/mnt/c/Users/leesh/Desktop/Study/TechWikiPlusServer/service/user/src/test"

# 공통 패턴들 변경
find $TEST_DIR -name "*.kt" -type f -exec sed -i \
    -e 's/UserId("test-user-1")/UserId(1000001L)/g' \
    -e 's/UserId("test-user-2")/UserId(1000002L)/g' \
    -e 's/UserId("user-1")/UserId(1000001L)/g' \
    -e 's/UserId("user-2")/UserId(1000002L)/g' \
    -e 's/UserId("new-user")/UserId(3000001L)/g' \
    -e 's/UserId("existing-user")/UserId(2000001L)/g' \
    -e 's/UserId("a")/UserId(1L)/g' \
    {} \;

echo "UserId 문자열 패턴을 Long 타입으로 변경 완료"