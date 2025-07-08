package net.matsudamper.detekt.rules

import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.junit.Assert.assertEquals
import org.junit.Test

class UseKotlinDurationForDelayRuleTest {

    @Test
    fun shouldReportDelayWithNumericLiteral() {
        val code = """
            import kotlinx.coroutines.delay
            
            suspend fun test() {
                delay(1000)
            }
        """.trimIndent()

        val findings = UseKotlinDurationForDelayRule().compileAndLint(code)
        assertEquals(1, findings.size)
        assertEquals(
            "delay(1000) should use Kotlin Duration instead of numeric literal. Consider using 1000.milliseconds or appropriate Duration unit.",
            findings[0].message
        )
    }

    @Test
    fun shouldReportDelayWithFloatLiteral() {
        val code = """
            import kotlinx.coroutines.delay
            
            suspend fun test() {
                delay(1000.5)
            }
        """.trimIndent()

        val findings = UseKotlinDurationForDelayRule().compileAndLint(code)
        assertEquals(1, findings.size)
        assertEquals(
            "delay(1000.5) should use Kotlin Duration instead of numeric literal. Consider using 1000.5.milliseconds or appropriate Duration unit.",
            findings[0].message
        )
    }

    @Test
    fun shouldNotReportDelayWithDuration() {
        val code = """
            import kotlinx.coroutines.delay
            import kotlin.time.Duration.Companion.seconds
            
            suspend fun test() {
                delay(1.seconds)
            }
        """.trimIndent()

        val findings = UseKotlinDurationForDelayRule().compileAndLint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun shouldNotReportDelayWithMilliseconds() {
        val code = """
            import kotlinx.coroutines.delay
            import kotlin.time.Duration.Companion.milliseconds
            
            suspend fun test() {
                delay(1000.milliseconds)
            }
        """.trimIndent()

        val findings = UseKotlinDurationForDelayRule().compileAndLint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun shouldNotReportDelayWithNanoseconds() {
        val code = """
            import kotlinx.coroutines.delay
            import kotlin.time.Duration.Companion.nanoseconds
            
            suspend fun test() {
                delay(1000000.nanoseconds)
            }
        """.trimIndent()

        val findings = UseKotlinDurationForDelayRule().compileAndLint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun shouldNotReportDelayWithMinutes() {
        val code = """
            import kotlinx.coroutines.delay
            import kotlin.time.Duration.Companion.minutes
            
            suspend fun test() {
                delay(5.minutes)
            }
        """.trimIndent()

        val findings = UseKotlinDurationForDelayRule().compileAndLint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun shouldNotReportDelayWithHours() {
        val code = """
            import kotlinx.coroutines.delay
            import kotlin.time.Duration.Companion.hours
            
            suspend fun test() {
                delay(2.hours)
            }
        """.trimIndent()

        val findings = UseKotlinDurationForDelayRule().compileAndLint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun shouldNotReportDelayWithDays() {
        val code = """
            import kotlinx.coroutines.delay
            import kotlin.time.Duration.Companion.days
            
            suspend fun test() {
                delay(1.days)
            }
        """.trimIndent()

        val findings = UseKotlinDurationForDelayRule().compileAndLint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun shouldNotReportDelayWithVariable() {
        val code = """
            import kotlinx.coroutines.delay
            
            suspend fun test() {
                val timeout = 1000
                delay(timeout)
            }
        """.trimIndent()

        val findings = UseKotlinDurationForDelayRule().compileAndLint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun shouldNotReportOtherFunctionCalls() {
        val code = """
            fun someFunction(millis: Long) {}
            
            fun test() {
                someFunction(1000)
            }
        """.trimIndent()

        val findings = UseKotlinDurationForDelayRule().compileAndLint(code)
        assertEquals(0, findings.size)
    }
}