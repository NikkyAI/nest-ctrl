package tags

import androidx.compose.runtime.Immutable
import com.akuleshov7.ktoml.annotations.TomlInlineTable
import com.akuleshov7.ktoml.annotations.TomlLiteral
import com.akuleshov7.ktoml.annotations.TomlMultiline
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Immutable
@Serializable
//@TomlInlineTable
data class PresetPlaylist(
    @TomlLiteral
    val label: String,
    val terms: List<Term>
) {
    fun score(tags: Set<Tag>): Int {
        return terms.sumOf { term ->
            if (term.matches(tags)) {
                term.boost
            } else {
                0
            }
        }
    }
}

@Serializable
data class TermDouble(
    val boost: Double,
    val matcher: TagMatcher,
) {
    fun toTerm() = Term(
        boost = boost.roundToInt(),
        include = matcher.include,
        exclude = matcher.exclude,
    )
}
@Serializable
data class Term(
    val boost: Int,
    @TomlMultiline
    val include: Set<Tag> = emptySet(),
    @TomlMultiline
    val exclude: Set<Tag> = emptySet(),
) {
    val matcher by lazy {
        TagMatcher(include = include, exclude = exclude)
    }
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
data class TagMatcher (
    val include: Set<Tag> = setOf(),
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val exclude: Set<Tag> = setOf(),
) {
    fun matches(tags: Set<Tag>): Boolean {
        val matchInclude = include.all { includeTag ->
            includeTag in tags || tags.any { t ->
                t.namespace.startsWith(includeTag.namespace + includeTag.name)
            }
        }
//        val matchExclude = exclude.none { excludeTag ->
//            excludeTag in tags || tags.any { t ->
//                t.namespace.startsWith(excludeTag.namespace + excludeTag.name)
//            }
//        }
        return matchInclude // && matchExclude
    }
}

fun <T> pickItemToGenerate(options: Map<T, Int>): T {
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
