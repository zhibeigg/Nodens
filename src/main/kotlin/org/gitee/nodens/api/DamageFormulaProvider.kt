package org.gitee.nodens.api

import org.gitee.nodens.common.DamageProcessor

/**
 * 外部伤害公式提供者。
 *
 * 返回 null 表示不接管本次伤害，Nodens 会继续尝试下一个提供者或回退到 handle.yml。
 * 返回非 null 数值表示接管本次最终伤害。
 */
fun interface DamageFormulaProvider {

    fun calculate(processor: DamageProcessor): Double?
}
