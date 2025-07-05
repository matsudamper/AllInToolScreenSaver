package net.matsudamper.ktlint.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@SinceKtlint("1.0", SinceKtlint.Status.STABLE)
class SampleRule : Rule(
    ruleId = RuleId("custom:java-time-clock"),
    about = About(),
), RuleAutocorrectApproveHandler {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) = Unit
}
