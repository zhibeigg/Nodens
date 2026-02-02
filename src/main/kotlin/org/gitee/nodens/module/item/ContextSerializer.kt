package org.gitee.nodens.module.item

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * [NormalContext] 高性能二进制序列化器
 *
 * 相比 JSON 序列化，二进制格式具有以下优势：
 * - 无需字段名，直接写入值
 * - 类型标识只需 1 字节
 * - 无需字符串解析
 *
 * ## 二进制格式说明
 *
 * ```
 * NormalContext: [key:UTF] [hashcode:Int] [variableCount:Int] [Entry * variableCount]
 * Entry: [key:UTF] [Variable]
 * Variable: [type:Byte] [value:根据类型不同]
 * ```
 *
 * @see NormalContext
 * @see Variable
 */
object ContextSerializer {

    // Variable 类型标识符
    private const val TYPE_NULL: Byte = 0
    private const val TYPE_BYTE: Byte = 1
    private const val TYPE_SHORT: Byte = 2
    private const val TYPE_INT: Byte = 3
    private const val TYPE_LONG: Byte = 4
    private const val TYPE_FLOAT: Byte = 5
    private const val TYPE_DOUBLE: Byte = 6
    private const val TYPE_CHAR: Byte = 7
    private const val TYPE_STRING: Byte = 8
    private const val TYPE_BOOLEAN: Byte = 9
    private const val TYPE_ARRAY: Byte = 10
    private const val TYPE_MAP: Byte = 11

    /** 最大嵌套深度限制，防止栈溢出 */
    private const val MAX_DEPTH = 16

    /**
     * 序列化 [NormalContext] 为字节数组
     *
     * @param context 要序列化的上下文对象
     * @return 序列化后的字节数组
     */
    fun serialize(context: NormalContext): ByteArray {
        // 根据变量数量估算容量，减少扩容开销
        val estimatedSize = 64 + context.sourceMap().size * 32
        val baos = ByteArrayOutputStream(estimatedSize)
        DataOutputStream(baos).use { dos ->
            // 写入 key
            dos.writeUTF(context.key)
            // 写入 hashcode
            dos.writeInt(context.hashcode)
            // 写入 variable map
            val map = context.sourceMap()
            dos.writeInt(map.size)
            map.forEach { (key, variable) ->
                dos.writeUTF(key)
                writeVariable(dos, variable, 0)
            }
        }
        return baos.toByteArray()
    }

    /**
     * 从字节数组反序列化 [NormalContext]
     *
     * @param data 序列化的字节数组
     * @return 反序列化后的 [NormalContext] 对象
     */
    fun deserialize(data: ByteArray): NormalContext {
        DataInputStream(ByteArrayInputStream(data)).use { dis ->
            // 读取 key
            val key = dis.readUTF()
            // 读取 hashcode
            val hashcode = dis.readInt()
            // 读取 variable map
            val size = dis.readInt()
            val map = HashMap<String, Variable<*>>(size)
            repeat(size) {
                val varKey = dis.readUTF()
                val variable = readVariable(dis)
                map[varKey] = variable
            }
            return NormalContext(key, map, hashcode)
        }
    }

    /**
     * 将 [Variable] 写入输出流
     *
     * 写入格式: `[type:Byte] [value:根据类型不同]`
     *
     * @param dos 数据输出流
     * @param variable 要写入的变量
     * @param depth 当前嵌套深度
     * @throws IllegalStateException 如果嵌套深度超过限制
     */
    private fun writeVariable(dos: DataOutputStream, variable: Variable<*>, depth: Int) {
        if (depth > MAX_DEPTH) {
            throw IllegalStateException("Variable 嵌套深度超过限制: $MAX_DEPTH")
        }
        when (variable) {
            is NullVariable -> dos.writeByte(TYPE_NULL.toInt())
            is ByteVariable -> {
                dos.writeByte(TYPE_BYTE.toInt())
                dos.writeByte(variable.value.toInt())
            }
            is ShortVariable -> {
                dos.writeByte(TYPE_SHORT.toInt())
                dos.writeShort(variable.value.toInt())
            }
            is IntVariable -> {
                dos.writeByte(TYPE_INT.toInt())
                dos.writeInt(variable.value)
            }
            is LongVariable -> {
                dos.writeByte(TYPE_LONG.toInt())
                dos.writeLong(variable.value)
            }
            is FloatVariable -> {
                dos.writeByte(TYPE_FLOAT.toInt())
                dos.writeFloat(variable.value)
            }
            is DoubleVariable -> {
                dos.writeByte(TYPE_DOUBLE.toInt())
                dos.writeDouble(variable.value)
            }
            is CharVariable -> {
                dos.writeByte(TYPE_CHAR.toInt())
                dos.writeChar(variable.value.code)
            }
            is StringVariable -> {
                dos.writeByte(TYPE_STRING.toInt())
                dos.writeUTF(variable.value)
            }
            is BooleanVariable -> {
                dos.writeByte(TYPE_BOOLEAN.toInt())
                dos.writeBoolean(variable.value)
            }
            is ArrayVariable -> {
                dos.writeByte(TYPE_ARRAY.toInt())
                dos.writeInt(variable.value.size)
                variable.value.forEach { writeVariable(dos, it, depth + 1) }
            }
            is MapVariable -> {
                dos.writeByte(TYPE_MAP.toInt())
                dos.writeInt(variable.value.size)
                variable.value.forEach { (k, v) ->
                    dos.writeUTF(k)
                    writeVariable(dos, v, depth + 1)
                }
            }
            else -> {
                // 对于外部注册的 Variable 类型，回退到 JSON 序列化
                dos.writeByte(-1)
                val json = VariableRegistry.json.encodeToString(
                    kotlinx.serialization.serializer<Variable<*>>(),
                    variable
                )
                dos.writeUTF(json)
            }
        }
    }

    /**
     * 从输入流读取 [Variable]
     *
     * @param dis 数据输入流
     * @param depth 当前嵌套深度
     * @return 读取的 [Variable] 对象
     * @throws IllegalStateException 如果嵌套深度超过限制
     */
    private fun readVariable(dis: DataInputStream, depth: Int = 0): Variable<*> {
        if (depth > MAX_DEPTH) {
            throw IllegalStateException("Variable 嵌套深度超过限制: $MAX_DEPTH")
        }
        return when (val type = dis.readByte()) {
            TYPE_NULL -> NullVariable(null)
            TYPE_BYTE -> ByteVariable(dis.readByte())
            TYPE_SHORT -> ShortVariable(dis.readShort())
            TYPE_INT -> IntVariable(dis.readInt())
            TYPE_LONG -> LongVariable(dis.readLong())
            TYPE_FLOAT -> FloatVariable(dis.readFloat())
            TYPE_DOUBLE -> DoubleVariable(dis.readDouble())
            TYPE_CHAR -> CharVariable(dis.readChar())
            TYPE_STRING -> StringVariable(dis.readUTF())
            TYPE_BOOLEAN -> BooleanVariable(dis.readBoolean())
            TYPE_ARRAY -> {
                val size = dis.readInt()
                val list = ArrayList<Variable<*>>(size)
                repeat(size) { list.add(readVariable(dis, depth + 1)) }
                ArrayVariable(list)
            }
            TYPE_MAP -> {
                val size = dis.readInt()
                val map = HashMap<String, Variable<*>>(size)
                repeat(size) {
                    val key = dis.readUTF()
                    map[key] = readVariable(dis, depth + 1)
                }
                MapVariable(map)
            }
            else -> {
                // 外部注册的 Variable 类型，使用 JSON 反序列化
                val json = dis.readUTF()
                VariableRegistry.json.decodeFromString(
                    kotlinx.serialization.serializer<Variable<*>>(),
                    json
                )
            }
        }
    }
}
