package tags

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class TagScoreEval(
    val label: String = "",
    val terms: List<Term>
) {
    fun score(tags: Set<Tag>): Double {
        return terms.sumOf { term ->
            if (term.matches(tags)) {
                term.boost
            } else {
                0.0
            }
        }
    }
}

@Serializable
data class Term(
    val boost: Double,
    val matcher: TagMatcher,
) {
    fun matches(tags: Set<Tag>): Boolean {
        return matcher.matches(tags)
    }
}

private fun <E> List<E>.startsWith(subset: List<E>): Boolean {
    return subset.indices.all { index ->
        this.size > index && subset.size > index && this[index] == subset[index]
    }
}

@Immutable
@Serializable
data class TagMatcher(
    val include: Set<Tag> = emptySet(),
    val exclude: Set<Tag> = emptySet(),
) {
    fun matches(tags: Set<Tag>): Boolean {
        val matchInclude = include.all { includeTag ->
            includeTag in tags || tags.any { t ->
                t.namespace.startsWith(includeTag.namespace + includeTag.name)
            }
        }
        val matchExclude = exclude.none { excludeTag ->
            excludeTag in tags || tags.any { t ->
                t.namespace.startsWith(excludeTag.namespace + excludeTag.name)
            }
        }
        return matchInclude && matchExclude
    }
}

fun <T> pickItemToGenerate(options: Map<T, Double>): T {
    require(options.isNotEmpty())
    val randomNumber = Math.random() * options.values.sumOf { it }

    var probabilityIterator = 0.0
    options.forEach { (item, score) ->
        probabilityIterator += score
        if (probabilityIterator >= randomNumber) {
            return item
        }
    }
    error("no option picked")
}

/*
scribbles for UI
{} unfoldable section
[] unfoldable menu item
- menu item

each section / item is indented

{ // playlist name
  // edit screen (TagScoreEval)
  label (TextField)
  [..] // Boost (Int TextField or slider with preset buttons),
  [
      // Boost (Int TextField or slider),
      // includes
      {
           - tag name + delete (& confirm)
           - tag name + delete (& confirm)
           [ // add new
               // list of unused tags
               - tag
               - tag
           ]
      }
      // excludes
      {
           - tag name + delete (& confirm)
           - tag name + delete (& confirm)
           [ // add new
               // list of unused tags
               - tag
               - tag
           ]
      }
      - delete (& confirm)
  ],
  [..],
  + add new
}


 */
