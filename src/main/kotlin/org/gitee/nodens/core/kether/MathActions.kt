package org.gitee.nodens.core.kether

import org.gitee.nodens.common.Source
import org.gitee.nodens.util.NODENS_NAMESPACE
import taboolib.common5.Coerce
import taboolib.module.kether.*

/**
 * Nodens 高性能算术 Kether 动作。
 *
 * - `expr "<表达式>"`：解析期把整条算式编译成 RPN（[ExprCompiler]），求值期同步、零 CompletableFuture、
 *   零装箱栈、变量按需点读（不像 calc 每次拷贝整张变量表）。支持 `+ - * / %`、一元负、括号、变量裸名，
 *   内置 `min/max/clamp/round/floor/ceil/abs/pct`。
 * - `clamp <值> min <下限> max <上限>`：同步钳制，等价 `expr "clamp(x,lo,hi)"` 的便捷写法。
 * - `sumSource <源持有者> [ attribute [ "A" "B" ] ]`：Java 侧一次遍历，按属性名汇总 `Source.value`；
 *   省略 `attribute` 时汇总全部源。取代脚本里 `for i in &x then { case &i[attribute] [...] }` 的逐源累加。
 *
 * 注意：数字参数一律用 TabooLib 的 [double]，绝不用 Nodens `util/Kether.number()`（它返回
 * `IAttributeGroup.Number` 属性对象，误用会 ClassCastException）。纯算术无 I/O，全程 [now]/[actionNow] 同步。
 */
object MathActions {

    /**
     * 解析期：`it.nextToken()` 取原始表达式串编译一次（闭包缓存）；
     * 求值期：[actionNow] 同步对当前帧变量求值。
     */
    @KetherParser(["expr"], namespace = NODENS_NAMESPACE, shared = true)
    fun parserExpr() = scriptParser {
        val compiled = ExprCompiler.compile(it.nextToken())
        actionNow {
            val frame = this
            compiled.eval { name -> frame.readVar(name) }
        }
    }

    /** `clamp <值> min <下限> max <上限>` —— 同步钳制为 [lo, hi]。 */
    @KetherParser(["clamp"], namespace = NODENS_NAMESPACE, shared = true)
    fun parserClamp() = combinationParser {
        it.group(
            double(),
            command("min", then = double()),
            command("max", then = double())
        ).apply(it) { x, lo, hi ->
            now { maxOf(lo, minOf(x, hi)) }
        }
    }

    /** `sumSource <持有者> [ attribute [ "A" "B" ] ]` —— 按属性名汇总 Source.value；省略 attribute 时汇总全部。 */
    @KetherParser(["sumSource"], namespace = NODENS_NAMESPACE, shared = true)
    fun parserSumSource() = combinationParser {
        it.group(
            any(),
            command("attribute", then = text().listOf()).option()
        ).apply(it) { holder, attrs ->
            now { sumSources(holder, attrs) }
        }
    }

    /** 跨帧单键深查（当前帧优先，逐级向父帧回溯），缺失返回 0.0；不拷贝整张变量表。 */
    private fun ScriptFrame.readVar(name: String): Double {
        var frame: ScriptFrame? = this
        while (frame != null) {
            val v = frame.variables().getOrNull<Any?>(name)
            if (v != null) return Coerce.toDouble(v)
            frame = frame.parent().orElse(null)
        }
        return 0.0
    }

    private fun sumSources(holder: Any?, attrs: List<String>?): Double {
        val seq: Iterable<*> = when (holder) {
            is Map<*, *> -> holder.values
            is Iterable<*> -> holder
            is Array<*> -> holder.asList()
            else -> return 0.0
        }
        var sum = 0.0
        when {
            attrs.isNullOrEmpty() -> for (o in seq) if (o is Source) sum += o.amount
            attrs.size == 1 -> {
                val a = attrs[0]
                for (o in seq) if (o is Source && o.attributeName == a) sum += o.amount
            }
            else -> {
                val set = attrs.toHashSet()
                for (o in seq) if (o is Source && o.attributeName in set) sum += o.amount
            }
        }
        return sum
    }
}
