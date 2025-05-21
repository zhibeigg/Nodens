package org.gitee.nodens.core.kether

import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.RegainProcessor
import org.gitee.nodens.core.AttributeManager.groupMap
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory
import org.gitee.nodens.util.*
import taboolib.common.OpenResult
import taboolib.common.platform.function.info
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*
import java.util.concurrent.CompletableFuture

object AttributeActions {

    @KetherParser(["attributeNumber"], namespace = NODENS_NAMESPACE, shared = true)
    private fun attributeNumber() = combinationParser {
        it.group(
            text(),
            text()
        ).apply(it) { group, key ->
            future {
                val number = groupMap[group]?.numbers?.get(key) ?: return@future CompletableFuture.completedFuture(null)
                CompletableFuture.completedFuture(number)
            }
        }
    }

    @KetherParser(["attributeMemory"], namespace = NODENS_NAMESPACE, shared = true)
    private fun memory() = scriptParser {
        actionNow {
            livingEntity().attributeMemory()
        }
    }

    @KetherParser(["attribute"], namespace = NODENS_NAMESPACE, shared = true)
    private fun addAttribute() = scriptParser {
        it.switch {
            case("add") {
                it.switch {
                    case("attacker") {
                        it.addDamageSource { id, number, processors, value ->
                            val source = DamageProcessor.DamageSource(id, number, value)
                            processors.forEach { processor ->
                                processor.damageSources[id] = source
                            }
                        }
                    }
                    case("defender") {
                        actionNow {
                            it.addDamageSource { id, number, processors, value ->
                                val source = DamageProcessor.DefenceSource(id, number, value)
                                processors.forEach { processor ->
                                    processor.defenceSources[id] = source
                                }
                            }
                        }
                    }
                    case("healer") {
                        it.addRegainSource { id, number, processors, value ->
                            val source = RegainProcessor.RegainSource(id, number, value)
                            processors.forEach { processor ->
                                processor.regainSources[id] = source
                            }
                        }
                    }
                    case("passive") {
                        actionNow {
                            it.addRegainSource { id, number, processors, value ->
                                val source = RegainProcessor.ReduceSource(id, number, value)
                                processors.forEach { processor ->
                                    processor.reduceSources[id] = source
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun QuestReader.addDamageSource(function: (id: String, number: IAttributeGroup.Number, processors: List<DamageProcessor>, value: Double) -> Unit): ScriptAction<Any?> {
        val number = nextParsedAction()
        val processors = nextParsedAction()
        val id = nextParsedAction()
        val value = nextParsedAction()
        return actionNow {
            run(number).thenAccept { number ->
                val number = number as IAttributeGroup.Number
                run(processors).thenAccept { processors ->
                    val list = processors as List<DamageProcessor>
                    run(id).str { id ->
                        run(value).double { value ->
                            function(id, number, list, value)
                        }
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun QuestReader.addRegainSource(function: (id: String, number: IAttributeGroup.Number, processors: List<RegainProcessor>, value: Double) -> Unit): ScriptAction<Any?> {
        val number = nextParsedAction()
        val processors = nextParsedAction()
        val id = nextParsedAction()
        val value = nextParsedAction()
        return actionNow {
            run(number).thenAccept { number ->
                val number = number as IAttributeGroup.Number
                run(processors).thenAccept { processors ->
                    val list = processors as List<RegainProcessor>
                    run(id).str { id ->
                        run(value).double { value ->
                            function(id, number, list, value)
                        }
                    }
                }
            }
        }
    }

    @KetherParser(["handleAttacker"], namespace = NODENS_NAMESPACE, shared = true)
    private fun handleAttacker() = combinationParser {
        it.group(
            number(),
            damageProcessors()
        ).apply(it) { number, processors ->
            future {
                val attribute = livingEntity().attributeMemory()?.mergedAttribute(number, true) ?: return@future CompletableFuture.completedFuture(processors)
                processors.forEach { processor ->
                    number.handleAttacker(processor, attribute)
                }
                CompletableFuture.completedFuture(processors)
            }
        }
    }

    @KetherParser(["handleDefender"], namespace = NODENS_NAMESPACE, shared = true)
    private fun handleDefender() = combinationParser {
        it.group(
            number(),
            damageProcessors()
        ).apply(it) { number, processors ->
            future {
                val attribute = livingEntity().attributeMemory()?.mergedAttribute(number, true) ?: return@future CompletableFuture.completedFuture(processors)
                processors.forEach { processor ->
                    number.handleDefender(processor, attribute)
                }
                CompletableFuture.completedFuture(processors)
            }
        }
    }

    @KetherParser(["handleHealer"], namespace = NODENS_NAMESPACE, shared = true)
    private fun handleHealer() = combinationParser {
        it.group(
            number(),
            regainProcessors()
        ).apply(it) { number, processors ->
            future {
                val attribute = livingEntity().attributeMemory()?.mergedAttribute(number, true) ?: return@future CompletableFuture.completedFuture(processors)
                processors.forEach { processor ->
                    number.handleHealer(processor, attribute)
                }
                CompletableFuture.completedFuture(processors)
            }
        }
    }

    @KetherParser(["handlePassive"], namespace = NODENS_NAMESPACE, shared = true)
    private fun handlePassive() = combinationParser {
        it.group(
            number(),
            regainProcessors()
        ).apply(it) { number, processors ->
            future {
                val attribute = livingEntity().attributeMemory()?.mergedAttribute(number, true) ?: return@future CompletableFuture.completedFuture(processors)
                processors.forEach { processor ->
                    number.handlePassive(processor, attribute)
                }
                CompletableFuture.completedFuture(processors)
            }
        }
    }

    @KetherParser(["callDamage"], namespace = NODENS_NAMESPACE, shared = true)
    private fun callDamage() = combinationParser {
        it.group(
            damageProcessors()
        ).apply(it) { processors ->
            future {
                processors.forEach { processor ->
                    processor.callDamage()
                }
                CompletableFuture.completedFuture(processors)
            }
        }
    }

    @KetherParser(["callRegain"], namespace = NODENS_NAMESPACE, shared = true)
    private fun callRegain() = combinationParser {
        it.group(
            regainProcessors()
        ).apply(it) { processors ->
            future {
                processors.forEach { processor ->
                    processor.callRegain()
                }
                CompletableFuture.completedFuture(processors)
            }
        }
    }

    @KetherParser(["handleDamage"], namespace = NODENS_NAMESPACE, shared = true)
    private fun handleDamage() = combinationParser {
        it.group(
            damageProcessors()
        ).apply(it) { processors ->
            future {
                processors.forEach { processor ->
                    processor.handle()
                }
                CompletableFuture.completedFuture(processors)
            }
        }
    }

    @KetherParser(["handleRegain"], namespace = NODENS_NAMESPACE, shared = true)
    private fun handleRegain() = combinationParser {
        it.group(
            regainProcessors()
        ).apply(it) { processors ->
            future {
                processors.forEach { processor ->
                    processor.handle()
                }
                CompletableFuture.completedFuture(processors)
            }
        }
    }

    @KetherParser(["combatPower"], namespace = NODENS_NAMESPACE, shared = true)
    private fun combatPower() = scriptParser {
        it.switch {
            case("all") {
                actionNow {
                    val memory = livingEntity().attributeMemory() ?: return@actionNow 0
                    memory.getCombatPower().values.sum()
                }
            }
            case("group") {
                val group = it.nextParsedAction()
                actionFuture { future ->
                    val memory = livingEntity().attributeMemory() ?: return@actionFuture future.complete(0.0)
                    run(group).str { group ->
                        future.complete(memory.getCombatPower().filter { entry -> entry.key.group.name == group }.values.sum())
                    }
                }
            }
            case("number") {
                val group = it.nextParsedAction()
                val number = it.nextParsedAction()
                actionFuture { future ->
                    val memory = livingEntity().attributeMemory() ?: return@actionFuture future.complete(0.0)
                    run(group).str { group ->
                        run(number).str { number ->
                            future.complete(memory.getCombatPower().filter { (n, _) -> n.group.name == group && n.name == number }.values.sum())
                        }
                    }
                }
            }
        }
    }

    @KetherProperty(bind = RegainProcessor::class, true)
    fun propertyRegainProcessor() = object : ScriptProperty<RegainProcessor>("RegainProcessor.operator") {

        override fun read(instance: RegainProcessor, key: String): OpenResult {
            return when (key) {
                "regainSources" -> OpenResult.successful(instance.regainSources)
                "reduceSources" -> OpenResult.successful(instance.reduceSources)
                "healer" -> OpenResult.successful(instance.healer)
                "passive" -> OpenResult.successful(instance.passive)
                "reason" -> OpenResult.successful(instance.reason)
                "regain" -> OpenResult.successful(instance.getFinalRegain())
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: RegainProcessor, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }

    @KetherProperty(bind = DamageProcessor::class, true)
    fun propertyDamageProcessor() = object : ScriptProperty<DamageProcessor>("DamageProcessor.operator") {

        override fun read(instance: DamageProcessor, key: String): OpenResult {
            return when (key) {
                "damageSources" -> OpenResult.successful(instance.damageSources)
                "defenceSources" -> OpenResult.successful(instance.defenceSources)
                "attacker" -> OpenResult.successful(instance.attacker)
                "defender" -> OpenResult.successful(instance.defender)
                "type" -> OpenResult.successful(instance.damageType)
                "crit" -> OpenResult.successful(instance.crit)
                "damage" -> OpenResult.successful(instance.getFinalDamage())
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: DamageProcessor, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }

    @KetherProperty(bind = IAttributeGroup.Number::class, true)
    fun propertyNumber() = object : ScriptProperty<IAttributeGroup.Number>("IAttributeGroup.Number.operator") {

        override fun read(instance: IAttributeGroup.Number, key: String): OpenResult {
            return when (key) {
                "name" -> OpenResult.successful(instance.name)
                "group" -> OpenResult.successful(instance.group.name)
                "combatPower" -> OpenResult.successful(instance.config.combatPower)
                "valueType" -> OpenResult.successful(instance.config.valueType.name)
                "handlePriority" -> OpenResult.successful(instance.config.handlePriority)
                "syncPriority" -> OpenResult.successful(instance.config.syncPriority)
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: IAttributeGroup.Number, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }
}