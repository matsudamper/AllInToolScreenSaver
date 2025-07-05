package net.matsudamper.ktlint.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression

/**
 * An ktlint rule that detects java.time.*.now() calls without Clock parameter.
 */
@SinceKtlint("1.0", SinceKtlint.Status.STABLE)
class JavaTimeClockRule :
    Rule(
        ruleId = RuleId("custom:java-time-clock"),
        about = About(),
    ),
    RuleAutocorrectApproveHandler {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == ElementType.CALL_EXPRESSION) {
            val callExpression = node.psi as? KtCallExpression
            if (callExpression != null) {
                checkJavaTimeNowCall(callExpression, emit)
            }
        }
    }

    private fun checkJavaTimeNowCall(
        callExpression: KtCallExpression,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
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
                            emit(
                                callExpression.textOffset,
                                "java.time.$receiverText.now() should use Clock parameter instead of using default system clock",
                                false,
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
                    emit(
                        callExpression.textOffset,
                        "java.time.*.now() should use Clock parameter instead of using default system clock",
                        false,
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
