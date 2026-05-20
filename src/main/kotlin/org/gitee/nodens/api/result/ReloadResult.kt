package org.gitee.nodens.api.result

/**
 * 重载类 API 的结构化执行结果。
 */
data class ReloadResult(
    override val success: Boolean,
    override val message: String,
    override val throwable: Throwable? = null,
): ApiResult {

    companion object {

        @JvmStatic
        fun success(message: String): ReloadResult {
            return ReloadResult(true, message)
        }

        @JvmStatic
        fun failure(message: String, throwable: Throwable? = null): ReloadResult {
            return ReloadResult(false, message, throwable)
        }
    }
}
