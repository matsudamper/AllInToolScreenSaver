package net.matsudamper.detekt.rules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression

/**
 * Detekt rule that detects java.time.*.now() calls without Clock parameter.
 */
class JavaTimeClockRule : Rule() {

    override val issue = Issue(
        id = "JavaTimeClock",
        severity = Severity.Warning,
        description = "java.time.*.now() should use Clock parameter instead of using default system clock",
        debt = Debt.FIVE_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        checkJavaTimeNowCall(expression)
    }

    private fun checkJavaTimeNowCall(callExpression: KtCallExpression) {
        val calleeExpression = callExpression.calleeExpression

        // Check if this is a qualified call like Instant.now()
        if (calleeExpression is KtDotQualifiedExpression) {
            val selectorExpression = calleeExpression.selectorExpression
            if (selectorExpression is KtNameReferenceExpression && selectorExpression.text == "now") {
                val receiverExpression = calleeExpression.receiverExpression
                if (receiverExpression is KtReferenceExpression) {
                    val receiverText = receiverExpression.text
                    if (isJavaTimeClass(receiverText)) {
                        val callArguments = callExpression.valueArguments
                        if (callArguments.isEmpty()) {
                            report(
                                CodeSmell(
                                    issue,
                                    Entity.from(callExpression),
                                    "java.time.$receiverText.now() should use Clock parameter instead of using default system clock",
                                ),
                            )
                        }
                    }
                }
            }
        }

        // Check if this is a simple now() call
        if (calleeExpression is KtNameReferenceExpression && calleeExpression.text == "now") {
            val callArguments = callExpression.valueArguments
            if (callArguments.isEmpty()) {
                // Check if the file has java.time imports
                val containingFile = callExpression.containingKtFile
                val imports = containingFile.importList?.imports
                val hasJavaTimeImport = imports?.any { importDirective ->
                    val importPath = importDirective.importPath?.pathStr
                    importPath?.startsWith("java.time.") == true
                } ?: false

                if (hasJavaTimeImport) {
                    report(
                        CodeSmell(
                            issue,
                            Entity.from(callExpression),
                            "java.time.*.now() should use Clock parameter instead of using default system clock",
                        ),
                    )
                }
            }
        }
    }

    private fun isJavaTimeClass(className: String): Boolean {
        return className in setOf(
            "Instant",
            "LocalDate",
            "LocalTime",
            "LocalDateTime",
            "ZonedDateTime",
            "OffsetDateTime",
            "Year",
            "YearMonth",
            "MonthDay",
        )
    }
}
