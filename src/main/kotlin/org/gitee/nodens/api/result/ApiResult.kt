package org.gitee.nodens.api.result

/**
 * Nodens 公开 API 的结构化执行结果。
 */
interface ApiResult {

    /** 操作是否成功 */
    val success: Boolean

    /** 面向调用方/日志的说明 */
    val message: String

    /** 失败原因，成功时通常为 null */
    val throwable: Throwable?

    /** Java/Kotlin 调用中的简短别名 */
    val ok: Boolean
        get() = success
}
