package net.matsudamper.detekt.rules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class CustomRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "custom"

    override fun instance(config: Config): RuleSet {
        return RuleSet(
            ruleSetId,
            listOf(
                JavaTimeClockRule()
            )
        )
    }
} 