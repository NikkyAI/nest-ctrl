package utils

import kotlin.math.abs

fun splitIntoLines(
            lines: Int,
            input: String,
            splitter: String = " ",
        ): List<String> {
//            logger.warnF { "splitting >${input}" }
            var startIndex = 0
            val splitLines = (1 until lines).map { n ->
                val fraction = n / lines.toDouble()
                val indices = input.indices.filter {
                    input.substring(it).startsWith(splitter)
                }
                // find best splitting index
                val splitAt = indices.minByOrNull { abs((fraction * input.length) - it) }!!

//                println("splitting ${input.length} into $n / $lines at $splitAt")

                val line = input.substring(startIndex..<splitAt)
//            leftoverInput = leftoverInput.substringAfter(line)//.trimStart()
                startIndex = splitAt + 1
                line.trim()
            } + input.substring(startIndex).trim()

            return splitLines
//                .also {
//                    logger.debugF { "split results:\n${it.joinToString("\n")}" }
//                }
        }