package net.matsudamper.detekt.rules

import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.junit.Assert.assertEquals
import org.junit.Test

class JavaTimeClockRuleTest {

    @Test
    fun shouldReportInstantNowWithoutClockParameter() {
        val code = """
            import java.time.Instant
            
            fun test() {
                val now = Instant.now()
            }
        """.trimIndent()

        val findings = JavaTimeClockRule().compileAndLint(code)
        assertEquals(1, findings.size)
        assertEquals("java.time.*.now() should use Clock parameter instead of using default system clock", findings[0].message)
    }

    @Test
    fun shouldReportLocalDateTimeNowWithoutClockParameter() {
        val code = """
            import java.time.LocalDateTime
            
            fun test() {
                val now = LocalDateTime.now()
            }
        """.trimIndent()

        val findings = JavaTimeClockRule().compileAndLint(code)
        assertEquals(1, findings.size)
        assertEquals("java.time.*.now() should use Clock parameter instead of using default system clock", findings[0].message)
    }

    @Test
    fun shouldNotReportInstantNowWithClockParameter() {
        val code = """
            import java.time.Instant
            import java.time.Clock
            
            fun test() {
                val clock = Clock.systemUTC()
                val now = Instant.now(clock)
            }
        """.trimIndent()

        val findings = JavaTimeClockRule().compileAndLint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun shouldNotReportNonJavaTimeNowCalls() {
        val code = """
            fun test() {
                val now = System.currentTimeMillis()
            }
        """.trimIndent()

        val findings = JavaTimeClockRule().compileAndLint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun shouldReportSimpleNowCallWhenJavaTimeIsImported() {
        val code = """
            import java.time.*
            
            fun test() {
                val now = now()
            }
        """.trimIndent()

        val findings = JavaTimeClockRule().compileAndLint(code)
        assertEquals(1, findings.size)
        assertEquals("java.time.*.now() should use Clock parameter instead of using default system clock", findings[0].message)
    }
}
