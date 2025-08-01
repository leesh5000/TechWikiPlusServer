package me.helloc.techwikiplus.common.snowflake

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import me.helloc.common.snowflake.Snowflake
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class SnowflakeTest : FunSpec({

    // 테스트 대상 Snowflake 인스턴스 생성
    val snowflake = Snowflake()

    test("nextId는 각 쓰레드에서 오름차순으로 생성되고 전체적으로 유니크해야 한다") {
        // 고정된 크기의 쓰레드 풀 생성 (10개)
        val threadPool = Executors.newFixedThreadPool(10)
        // 반복 횟수(쓰레드 개수), 각 쓰레드당 생성할 ID 개수 설정
        val repeatCount = 1000
        val idCount = 1000

        // ID 생성 작업을 Callable로 제출하여 Future 리스트 얻기
        val futures =
            (1..repeatCount).map {
                threadPool.submit<List<Long>> {
                    // idCount 만큼 nextId 호출하여 리스트 생성
                    List(idCount) { snowflake.nextId() }
                }
            }

        // 결과를 모두 모아 검증하기 위한 리스트
        val allIds = mutableListOf<Long>()
        futures.forEach { future ->
            // Future에서 실제 생성된 ID 리스트 가져오기 (블로킹)
            val ids = future.get()
            // 같은 쓰레드 내에서 ID들이 오름차순으로 생성됐는지 확인
            ids.zipWithNext().forEach { (prev, next) ->
                next shouldBeGreaterThan prev
            }
            // 전체 결과 리스트에 합치기
            allIds += ids
        }

        // 총 테스트된 데이터 건수 계산 후 출력
        val totalCount = allIds.size
        println("총 테스트 데이터: ${totalCount}건")
        // 전체 생성된 ID가 중복 없이 unique한지 확인
        allIds.distinct().size shouldBe totalCount

        // 테스트 후 쓰레드 풀 종료
        threadPool.shutdown()
    }

    test("nextId 성능 측정 출력") {
        // 고정된 크기의 쓰레드 풀 생성 (10개)
        val threadPool = Executors.newFixedThreadPool(10)
        // 반복 횟수(쓰레드 개수), 각 쓰레드당 생성할 ID 개수 설정
        val repeatCount = 1000
        val idCount = 1000
        // 모든 작업이 완료됐는지 확인하기 위한 CountDownLatch
        val latch = CountDownLatch(repeatCount)

        // 성능 측정을 위한 시작 시간 기록 (나노초)
        val start = System.nanoTime()
        repeat(repeatCount) {
            threadPool.submit {
                // 각 쓰레드에서 idCount 만큼 ID 생성
                repeat(idCount) { snowflake.nextId() }
                // 작업 완료 시 카운트 다운
                latch.countDown()
            }
        }
        // 모든 쓰레드가 완료될 때까지 대기
        latch.await()
        // 완료 후 종료 시간 기록
        val end = System.nanoTime()
        // 실행 시간(ms)으로 변환하여 출력
        println("times = ${(end - start) / 1_000_000} ms")

        // 테스트 후 쓰레드 풀 종료
        threadPool.shutdown()
    }
})
