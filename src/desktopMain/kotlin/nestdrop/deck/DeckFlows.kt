package nestdrop.deck

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

fun Deck.startFlows() {
//    transitionTime
//        //TODO: combine with trigger flow
//        .combine(emitTransitionTime.consumeAsFlow()) { a, _ -> a }
//        .combine(forceEmitCurrentState) { a, _ -> a }
//        .onEach {
//            logger.infoF { "$deckName set transition time" }
//            nestdropPortSend(
//                nestdropAddress("sTransitTime"), it
//            )
//        }.launchIn(flowScope)
//
//    presetQueue.index
//        .combine(forceEmitCurrentState) { a, _ -> a }
//        .combine(presetQueues) { index, queues ->
//            queues.getOrNull(index)
//        }.onEach { queue ->
//            presetQueue.value = queue
//        }
//        .launchIn(flowScope)

//    presetQueue
//        .combine(forceEmitCurrentState) { a, _ -> a }
//        .onEach { queue ->
//            logger.infoF { "$deckName queue: ${queue?.name}" }
//            presetQueue.name.value = queue?.name ?: "disabled"
//            if (queue != null) {
//                nestdropPortSend(
//                    OSCMessage("/Queue/${queue.name}", listOf(1))
//                )
//            }
//        }
//        .launchIn(flowScope)

//    presetQueues
//        .onEach {
//            presetQueues.labels.forEachIndexed { index, label ->
//                label.value = it.getOrNull(index)
//                    ?.name
//                    ?: ""
//            }
//        }
//        .launchIn(flowScope)

//    spriteQueues
//        .onEach {
//            spriteQueues.labels.forEachIndexed { index, label ->
//                label.value = it.getOrNull(index)
//                    ?.name
//                    ?: ""
//            }
//        }
//        .launchIn(flowScope)

//    spriteQueue.index
//        .combine(forceEmitCurrentState) { a, _ -> a }
//        .combine(spriteQueues) { index, queues ->
//            queues.getOrNull(index)
//        }
//        .onEach {
//            spriteQueue.value = it
//        }.launchIn(flowScope)

//    spriteQueue
//        .onEach {
//            spriteQueue.name.value = it?.name ?: "unset"
//        }
//        .launchIn(flowScope)
//
//    spriteQueue
//        .onEach {
//            it?.presets?.let { presets ->
//                sprite.labels.forEachIndexed { index, label ->
//                    label.value = presets.getOrNull(index)
//                        ?.substringBeforeLast(".jpg")
//                        ?.substringBeforeLast(".png")
////                        ?.also {
////                            logger.debugF { "label: $it" }
////                        }
//                        ?: ""
//                }
//            }
//        }
//        .launchIn(flowScope)
//
//    sprite.index
//        .combine(forceEmitCurrentState) { a, _ -> a }
//        .combine(spriteQueue) { index, queue ->
//            queue?.presets?.getOrNull(index)
//        }.onEach { spriteName ->
//            logger.infoF { "sprite name $spriteName" }
//            sprite.name.value = spriteName ?: "unset"
//        }
//        .launchIn(flowScope)

//    // do sprite change
//    sprite.index
//        .combine(resyncToTouchOSC) { a, _ -> a }
//        .combine(spriteQueue.filterNotNull()) { index, queue ->
//            index to queue
//        }
//        .runningHistoryAlternative()
//        .onEach { (current, last) ->
//            val (index, queue) = current
//            if (index == -1) {
//                if (last != null) {
//                    val (lastIndex, lastQueue) = last
//                    if (lastIndex != -1) {
//                        presetId(lastQueue, lastIndex, argument = 1)
//                    }
//                }
//                //TODO: unset
//                //TODO: try setting index -1
//            } else {
//                if (index != last?.first) {
//                    presetId(queue, -1)
//                    presetId(queue, index)
//                }
//            }
//        }
//        .launchIn(flowScope)

//    spoutQueue.index
//        .combine(forceEmitCurrentState) { a, _ -> a }
//        .combine(spriteQueues) { index, queues ->
//            queues.getOrNull(index)
//        }
//        .onEach {
//            spoutQueue.value = it
//        }.launchIn(flowScope)
//
//    spoutQueue.onEach {
//        it?.presets?.let { presets ->
//            spout.labels.forEachIndexed { index, label ->
//                label.value = presets
//                    .getOrNull(index)
//                    .orEmpty()
//                    .substringBeforeLast(".jpg")
//                    .substringBeforeLast(".png")
////                        ?.also {
////                            logger.debugF { "label: $it" }
////                        }
//            }
//        }
//    }
//        .launchIn(flowScope)

//    spout.index
//        .combine(resyncToTouchOSC) { a, _ -> a }
//        .combine(spriteQueue) { index, queue ->
//            queue?.presets?.getOrNull(index)
//        }.onEach { spriteName ->
//            logger.infoF { "sprite name $spriteName" }
//            spout.name.value = spriteName ?: "unset"
//        }
//        .launchIn(flowScope)


//    spriteFX.index
//        .combine(spriteFXMap) { fx, spriteFXMap ->
//            spriteFX.name.value = spriteFXMap[fx]
//                ?.let { "FX $fx: $it" }
//                ?: if (fx == -1) "uninitialized" else "unknown FX $fx"
//        }.launchIn(flowScope)
//    spriteFX.index
//        .combine(spriteFX.blendMode) { index, blendMode ->
//            if (blendMode) index + 50 else index
//        }
//        .filter { fx -> fx in (0..99) }
//        .onEach { fx ->
//
//            logger.infoF { "setting $deckName FX to $fx" }
//            nestdropPortSend(nestdropAddress("sSpriteFx"), fx / 99.0f)
//        }.launchIn(flowScope)

//    currentPreset
//        .onEach {
//            preset.name.value = it?.preset ?: "unset"
//        }
//        .launchIn(flowScope)
}