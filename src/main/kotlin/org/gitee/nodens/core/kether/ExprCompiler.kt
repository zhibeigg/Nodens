package org.gitee.nodens.core.kether

/**
 * 纯算术表达式编译器（零外部依赖，可独立单元测试）。
 *
 * [compile] 在解析期把一条表达式字符串编译成扁平 RPN（[Compiled]）；
 * [Compiled.eval] 在求值期用 DoubleArray 栈同步求值，零装箱、无 CompletableFuture、变量按需点读。
 *
 * 支持：`+ - * / %`、一元负号、括号、变量裸名引用、十进制常量，
 * 内置函数 min(2) / max(2) / clamp(3) / round(1) / floor(1) / ceil(1) / abs(1) / pct(1)。
 */
object ExprCompiler {

    const val OP_CONST = 0
    const val OP_VAR = 1
    const val OP_ADD = 2
    const val OP_SUB = 3
    const val OP_MUL = 4
    const val OP_DIV = 5
    const val OP_MOD = 6
    const val OP_NEG = 7
    const val OP_MIN = 8
    const val OP_MAX = 9
    const val OP_CLAMP = 10
    const val OP_ROUND = 11
    const val OP_FLOOR = 12
    const val OP_CEIL = 13
    const val OP_ABS = 14
    const val OP_PCT = 15

    private data class Fn(val op: Int, val arity: Int)

    private val FUNCTIONS: Map<String, Fn> = mapOf(
        "min" to Fn(OP_MIN, 2),
        "max" to Fn(OP_MAX, 2),
        "clamp" to Fn(OP_CLAMP, 3),
        "round" to Fn(OP_ROUND, 1),
        "floor" to Fn(OP_FLOOR, 1),
        "ceil" to Fn(OP_CEIL, 1),
        "abs" to Fn(OP_ABS, 1),
        "pct" to Fn(OP_PCT, 1)
    )

    /** 编译产物：解析期生成一次，闭包持有，整生命周期复用。 */
    class Compiled internal constructor(
        private val ops: IntArray,
        private val args: IntArray,
        private val consts: DoubleArray,
        private val vars: Array<String>,
        private val maxStack: Int
    ) {
        /** 表达式引用到的全部变量名（去重，按首次出现顺序）。 */
        val variableNames: List<String> get() = vars.toList()

        /** 同步求值；[read] 把变量名映射为 Double（缺失建议返回 0.0）。 */
        fun eval(read: (String) -> Double): Double {
            val st = DoubleArray(if (maxStack < 1) 1 else maxStack)
            var sp = 0
            var i = 0
            while (i < ops.size) {
                when (ops[i]) {
                    OP_CONST -> st[sp++] = consts[args[i]]
                    OP_VAR -> st[sp++] = read(vars[args[i]])
                    OP_ADD -> { st[sp - 2] = st[sp - 2] + st[sp - 1]; sp-- }
                    OP_SUB -> { st[sp - 2] = st[sp - 2] - st[sp - 1]; sp-- }
                    OP_MUL -> { st[sp - 2] = st[sp - 2] * st[sp - 1]; sp-- }
                    OP_DIV -> { st[sp - 2] = st[sp - 2] / st[sp - 1]; sp-- }
                    OP_MOD -> { st[sp - 2] = st[sp - 2] % st[sp - 1]; sp-- }
                    OP_NEG -> st[sp - 1] = -st[sp - 1]
                    OP_MIN -> { st[sp - 2] = minOf(st[sp - 2], st[sp - 1]); sp-- }
                    OP_MAX -> { st[sp - 2] = maxOf(st[sp - 2], st[sp - 1]); sp-- }
                    OP_CLAMP -> {
                        val hi = st[sp - 1]; val lo = st[sp - 2]; val x = st[sp - 3]
                        st[sp - 3] = maxOf(lo, minOf(x, hi)); sp -= 2
                    }
                    OP_ROUND -> st[sp - 1] = Math.round(st[sp - 1]).toDouble()
                    OP_FLOOR -> st[sp - 1] = Math.floor(st[sp - 1])
                    OP_CEIL -> st[sp - 1] = Math.ceil(st[sp - 1])
                    OP_ABS -> st[sp - 1] = Math.abs(st[sp - 1])
                    OP_PCT -> st[sp - 1] = st[sp - 1] / 100.0
                }
                i++
            }
            return if (sp > 0) st[sp - 1] else 0.0
        }
    }

    // ===== 词法 =====
    private sealed class Tok {
        data class Num(val v: Double) : Tok()
        data class Ident(val s: String) : Tok()
        data class Oper(val c: Char) : Tok()
        object LParen : Tok()
        object RParen : Tok()
        object Comma : Tok()
    }

    private fun tokenize(src: String): List<Tok> {
        val out = ArrayList<Tok>()
        var i = 0
        val n = src.length
        while (i < n) {
            val c = src[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || (c == '.' && i + 1 < n && src[i + 1].isDigit()) -> {
                    val start = i
                    while (i < n && (src[i].isDigit() || src[i] == '.')) i++
                    out.add(Tok.Num(src.substring(start, i).toDouble()))
                }
                c.isLetter() || c == '_' -> {
                    val start = i
                    while (i < n && (src[i].isLetterOrDigit() || src[i] == '_')) i++
                    out.add(Tok.Ident(src.substring(start, i)))
                }
                c == '(' -> { out.add(Tok.LParen); i++ }
                c == ')' -> { out.add(Tok.RParen); i++ }
                c == ',' -> { out.add(Tok.Comma); i++ }
                c == '+' || c == '-' || c == '*' || c == '/' || c == '%' -> { out.add(Tok.Oper(c)); i++ }
                else -> error("expr 非法字符 '$c' (位置 $i)：$src")
            }
        }
        return out
    }

    // ===== 运算符栈条目 =====
    private sealed class StackEntry
    private data class OpEntry(val c: Char) : StackEntry()   // 运算符 / 一元负 'u' / 左括号 '('
    private data class FnEntry(val name: String, val op: Int, val arity: Int) : StackEntry()

    private fun prec(c: Char): Int = when (c) {
        'u' -> 4
        '*', '/', '%' -> 3
        '+', '-' -> 2
        else -> 0
    }

    private fun opcodeOf(c: Char): Int = when (c) {
        '+' -> OP_ADD; '-' -> OP_SUB; '*' -> OP_MUL; '/' -> OP_DIV; '%' -> OP_MOD; 'u' -> OP_NEG
        else -> error("expr 内部错误：未知运算符 '$c'")
    }

    /** 把表达式编译为 [Compiled]。语法错误抛 [IllegalStateException]。 */
    fun compile(src: String): Compiled {
        val toks = tokenize(src)
        if (toks.isEmpty()) error("expr 表达式为空")

        val ops = ArrayList<Int>()
        val args = ArrayList<Int>()
        val consts = ArrayList<Double>()
        val constIndex = HashMap<Double, Int>()
        val vars = ArrayList<String>()
        val varIndex = HashMap<String, Int>()
        val opStack = ArrayDeque<StackEntry>()

        var depth = 0
        var maxDepth = 0
        fun grow() { depth++; if (depth > maxDepth) maxDepth = depth }
        fun shrink(by: Int) { depth -= by; if (depth < 0) error("expr 操作数不足：$src") }

        fun emitConst(v: Double) {
            val idx = constIndex.getOrPut(v) { consts.add(v); consts.size - 1 }
            ops.add(OP_CONST); args.add(idx); grow()
        }
        fun emitVar(name: String) {
            val idx = varIndex.getOrPut(name) { vars.add(name); vars.size - 1 }
            ops.add(OP_VAR); args.add(idx); grow()
        }
        fun emitOper(c: Char) {
            ops.add(opcodeOf(c)); args.add(0)
            if (c != 'u') shrink(1) // 二元 net -1；一元负 net 0
        }
        fun emitFn(fn: FnEntry) {
            ops.add(fn.op); args.add(0)
            shrink(fn.arity - 1) // net = 1 - arity
        }

        // expectOperand: 当前位置期待一个操作数（用于识别一元负号）
        var expectOperand = true
        var idx = 0
        while (idx < toks.size) {
            val t = toks[idx]
            when (t) {
                is Tok.Num -> { emitConst(t.v); expectOperand = false }
                is Tok.Ident -> {
                    val next = toks.getOrNull(idx + 1)
                    if (next === Tok.LParen) {
                        val fn = FUNCTIONS[t.s] ?: error("expr 未知函数 '${t.s}'：$src")
                        opStack.addLast(FnEntry(t.s, fn.op, fn.arity))
                        // 函数名后的 '(' 由下一轮处理；这里保持 expectOperand=true
                    } else {
                        emitVar(t.s); expectOperand = false
                    }
                }
                is Tok.Oper -> {
                    if (expectOperand) {
                        when (t.c) {
                            '-' -> opStack.addLast(OpEntry('u'))
                            '+' -> { /* 一元正号，忽略 */ }
                            else -> error("expr 运算符 '${t.c}' 缺少左操作数：$src")
                        }
                        // 仍期待操作数
                    } else {
                        while (true) {
                            val top = opStack.lastOrNull()
                            if (top is OpEntry && top.c != '(' &&
                                (prec(top.c) > prec(t.c) || (prec(top.c) == prec(t.c) && t.c != 'u'))
                            ) {
                                opStack.removeLast(); emitOper(top.c)
                            } else break
                        }
                        opStack.addLast(OpEntry(t.c))
                        expectOperand = true
                    }
                }
                is Tok.LParen -> { opStack.addLast(OpEntry('(')); expectOperand = true }
                is Tok.RParen -> {
                    var matched = false
                    while (opStack.isNotEmpty()) {
                        val top = opStack.removeLast()
                        if (top is OpEntry && top.c == '(') { matched = true; break }
                        if (top is OpEntry) emitOper(top.c) else if (top is FnEntry) emitFn(top)
                    }
                    if (!matched) error("expr 括号不匹配（多余的右括号）：$src")
                    // 若紧邻栈顶是函数，弹出并发射
                    val top = opStack.lastOrNull()
                    if (top is FnEntry) { opStack.removeLast(); emitFn(top) }
                    expectOperand = false
                }
                is Tok.Comma -> {
                    while (true) {
                        val top = opStack.lastOrNull()
                        if (top is OpEntry && top.c != '(') { opStack.removeLast(); emitOper(top.c) }
                        else break
                    }
                    expectOperand = true
                }
            }
            idx++
        }
        while (opStack.isNotEmpty()) {
            val top = opStack.removeLast()
            when {
                top is OpEntry && top.c == '(' -> error("expr 括号不匹配（缺少右括号）：$src")
                top is OpEntry -> emitOper(top.c)
                top is FnEntry -> error("expr 函数 '${top.name}' 缺少括号：$src")
            }
        }
        if (depth != 1) error("expr 表达式不完整或参数个数错误（残留栈深 $depth）：$src")

        return Compiled(ops.toIntArray(), args.toIntArray(), consts.toDoubleArray(), vars.toTypedArray(), maxDepth)
    }
}
