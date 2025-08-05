package me.helloc.techwikiplus.service.user.adapter.outbound.init

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.PreparedStatement
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

/**
 * 독립적으로 실행 가능한 데이터 초기화 프로그램
 *
 * 실행 방법:
 * 1. gradle build로 jar 파일 생성
 * 2. java -cp user-service.jar me.helloc.techwikiplus.service.user.infrastructure.init.DataInitializerMain
 *
 * 또는 IntelliJ에서 직접 Run 실행
 */
object DataInitializerMain {
    private val logger = LoggerFactory.getLogger(javaClass)

    private const val TOTAL_USERS = 20_000_000
    private const val CHUNK_SIZE = 100_000
    private const val BATCH_SIZE = 5_000
    private const val PARALLEL_THREADS = 8
    private const val LOG_INTERVAL = 1_000_000

    // BCrypt로 암호화된 "password123!" 문자열
    private const val ENCODED_PASSWORD = "\$2a\$10\$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG"

    private val processedCount = AtomicInteger(0)

    @JvmStatic
    fun main(args: Array<String>) {
        logger.info("=== User Data Initializer ===")
        logger.info("Total users to insert: $TOTAL_USERS")

        val startTime = System.currentTimeMillis()

        var dataSource: HikariDataSource? = null

        try {
            // 데이터베이스 연결 설정
            dataSource = createDataSource()

            // 기존 데이터 삭제
            clearExistingData(dataSource)

            // 대량 데이터 삽입
            val insertTime =
                measureTimeMillis {
                    insertUsersInParallel(dataSource)
                }

            val totalTime = System.currentTimeMillis() - startTime

            logger.info("=== Data initialization completed! ===")
            logger.info("Total users inserted: ${processedCount.get()}")
            logger.info("Insert time: ${insertTime / 1000}s")
            logger.info("Total time: ${totalTime / 1000}s")
            logger.info("Average speed: ${TOTAL_USERS / (insertTime / 1000)} users/second")
        } catch (e: Exception) {
            logger.error("Data initialization failed", e)
            System.exit(1)
        } finally {
            try {
                dataSource?.close()
                logger.info("DataSource closed successfully")
            } catch (e: Exception) {
                logger.error("Failed to close DataSource", e)
            }

            // 모든 리소스 정리 후 정상 종료
            System.exit(0)
        }
    }

    private fun createDataSource(): HikariDataSource {
        val config =
            HikariConfig().apply {
                // 환경 변수에서 읽거나 기본값 사용
                jdbcUrl = System.getenv("DB_URL")
                    ?: (
                        "jdbc:mysql://localhost:13306/techwikiplus-user?useSSL=false&serverTimezone=Asia/Seoul&" +
                            "characterEncoding=UTF-8&" +
                            "allowPublicKeyRetrieval=true&rewriteBatchedStatements=true&" +
                            "cachePrepStmts=true&" +
                            "useServerPrepStmts=true"
                    )
                username = System.getenv("DB_USERNAME") ?: "techwikiplus"
                password = System.getenv("DB_PASSWORD") ?: "techwikiplus"

                // 성능 최적화 설정
                maximumPoolSize = 50
                minimumIdle = 20
                connectionTimeout = 30000
                idleTimeout = 600000
                maxLifetime = 1800000

                // 프로그램 종료 시 자동 정리를 위한 설정
                isAutoCommit = false
                leakDetectionThreshold = 60000 // 1분

                // 추가 최적화
                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                addDataSourceProperty("useServerPrepStmts", "true")
                addDataSourceProperty("rewriteBatchedStatements", "true")
            }

        logger.info("Connecting to database: ${config.jdbcUrl}")
        return HikariDataSource(config)
    }

    private fun clearExistingData(dataSource: HikariDataSource) {
        logger.info("Clearing existing data...")

        dataSource.connection.use { conn ->
            // 외래 키 체크 임시 비활성화 (빠른 삭제를 위해)
            conn.createStatement().use { stmt ->
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0")
                stmt.execute("TRUNCATE TABLE users")
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1")
            }
        }

        logger.info("Existing data cleared")
    }

    private fun insertUsersInParallel(dataSource: HikariDataSource) =
        runBlocking {
            withContext(Dispatchers.IO) {
                val chunks = (0 until TOTAL_USERS step CHUNK_SIZE).toList()

                chunks.chunked(PARALLEL_THREADS).forEach { chunkGroup ->
                    val jobs =
                        chunkGroup.map { startIndex ->
                            async {
                                try {
                                    insertUserChunk(dataSource, startIndex, minOf(startIndex + CHUNK_SIZE, TOTAL_USERS))
                                } catch (e: Exception) {
                                    logger.error("Failed to insert chunk starting at $startIndex", e)
                                    throw e
                                }
                            }
                        }
                    jobs.awaitAll()
                }
            }

            // 모든 비동기 작업이 완료되었음을 보장
            logger.info("All parallel insertions completed")
        }

    private fun insertUserChunk(
        dataSource: HikariDataSource,
        startIndex: Int,
        endIndex: Int,
    ) {
        dataSource.connection.use { conn ->
            conn.autoCommit = false

            val sql =
                """
                INSERT INTO users (id, email, nickname, password, status, role, created_at, modified_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

            conn.prepareStatement(sql).use { pstmt ->
                val now = Instant.now()
                var batchCount = 0

                for (i in startIndex until endIndex) {
                    addUserToBatch(pstmt, i, now)
                    batchCount++

                    if (batchCount >= BATCH_SIZE) {
                        executeBatch(pstmt, conn)
                        batchCount = 0
                    }
                }

                // 남은 배치 실행
                if (batchCount > 0) {
                    executeBatch(pstmt, conn)
                }

                conn.commit()
            }
        }
    }

    private fun addUserToBatch(
        pstmt: PreparedStatement,
        index: Int,
        timestamp: Instant,
    ) {
        val id = UUID.randomUUID().toString()
        val email = "user$index@example.com"
        val nickname = "user$index"
        val status = determineUserStatus(index)
        val role = determineUserRole(index)

        pstmt.apply {
            setString(1, id)
            setString(2, email)
            setString(3, nickname)
            setString(4, ENCODED_PASSWORD)
            setString(5, status)
            setString(6, role)
            setTimestamp(7, java.sql.Timestamp.from(timestamp))
            setTimestamp(8, java.sql.Timestamp.from(timestamp))
            addBatch()
        }
    }

    private fun executeBatch(
        pstmt: PreparedStatement,
        conn: Connection,
    ) {
        val results = pstmt.executeBatch()
        val insertedCount = results.sum()

        val count = processedCount.addAndGet(insertedCount)
        if (count % LOG_INTERVAL <= insertedCount) {
            logger.info("Progress: $count / $TOTAL_USERS users inserted (${(count * 100.0 / TOTAL_USERS).format(2)}%)")
        }
    }

    private fun determineUserStatus(index: Int): String {
        return when {
            index % 100 < 90 -> "ACTIVE" // 90%
            index % 100 < 95 -> "PENDING" // 5%
            index % 100 < 98 -> "DORMANT" // 3%
            index % 100 < 99 -> "BANNED" // 1%
            else -> "DELETED" // 1%
        }
    }

    private fun determineUserRole(index: Int): String {
        return if (index % 1000 == 0) "ADMIN" else "USER" // 0.1% ADMIN
    }

    private fun Double.format(digits: Int): String = "%.${digits}f".format(this)
}
