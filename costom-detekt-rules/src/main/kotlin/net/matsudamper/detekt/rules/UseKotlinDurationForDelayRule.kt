package net.matsudamper.detekt.rules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/**
 * Detekt rule that detects delay() calls with numeric literals instead of Kotlin Duration.
 */
class UseKotlinDurationForDelayRule : Rule() {

    override val issue = Issue(
        id = "UseKotlinDurationForDelay",
        severity = Severity.Warning,
        description = "delay() should use Kotlin Duration instead of numeric literals (milliseconds)",
        debt = Debt.FIVE_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        checkDelayCall(expression)
    }

    private fun checkDelayCall(callExpression: KtCallExpression) {
        val calleeExpression = callExpression.calleeExpression
        
        // Check if this is a delay() call
        if (calleeExpression is KtNameReferenceExpression && calleeExpression.text == "delay") {
            val arguments = callExpression.valueArguments
            if (arguments.isNotEmpty()) {
                val firstArgument = arguments[0].getArgumentExpression()
                
                // Check if the argument is a numeric literal
                if (firstArgument is KtConstantExpression) {
                    val text = firstArgument.text
                    if (text.all { it.isDigit() || it == '.' }) {
                        report(
                            CodeSmell(
                                issue,
                                Entity.from(callExpression),
                                "delay($text) should use Kotlin Duration instead of numeric literal. " +
                                "Consider using ${text}.milliseconds or appropriate Duration unit.",
                            ),
                        )
                    }
                }
                
                // Check for Duration method calls that might be acceptable
                if (firstArgument is KtDotQualifiedExpression) {
                    val selectorExpression = firstArgument.selectorExpression
                    if (selectorExpression is KtNameReferenceExpression) {
                        val selectorText = selectorExpression.text
                        val durationMethods = setOf(
                            "nanoseconds", "microseconds", "milliseconds", "seconds", 
                            "minutes", "hours", "days"
                        )
                        if (!durationMethods.contains(selectorText)) {
                            // Check if receiver is numeric literal
                            val receiver = firstArgument.receiverExpression
                            if (receiver is KtConstantExpression) {
                                val receiverText = receiver.text
                                if (receiverText.all { it.isDigit() || it == '.' }) {
                                    report(
                                        CodeSmell(
                                            issue,
                                            Entity.from(callExpression),
                                            "delay($receiverText.$selectorText) should use Kotlin Duration. " +
                                            "Consider using $receiverText.milliseconds or appropriate Duration unit.",
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}