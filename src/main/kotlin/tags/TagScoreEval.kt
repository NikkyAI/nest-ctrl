package tags

import Tag
import kotlinx.serialization.Serializable

@Serializable
data class TagScoreEval(
    val label: String = "",
    val boosts: Map<TagMatcher, Double>
) {
    fun score(tags: Set<Tag>): Double {
        return boosts.entries.sumOf { (matcher, boost) ->
            if(matcher.matches(tags)) {
                boost
            } else {
                0.0
            }
        }
    }
}

@Serializable
data class TagMatcher(
    val include: Set<Tag>,
    val exclude: Set<Tag>,
) {
    fun matches(tags: Set<Tag>): Boolean {
        return include.all { it in tags } && exclude.none { it in tags }
    }
}
