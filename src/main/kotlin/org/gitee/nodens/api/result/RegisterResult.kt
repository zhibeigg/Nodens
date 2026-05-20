package org.gitee.nodens.api.result

/**
 * 注册/注销类 API 的结构化执行结果。
 */
data class RegisterResult(
    override val success: Boolean,
    override val message: String,
    override val throwable: Throwable? = null,
): ApiResult {

    companion object {

        @JvmStatic
        fun success(message: String): RegisterResult {
            return RegisterResult(true, message)
        }

        @JvmStatic
        fun failure(message: String, throwable: Throwable? = null): RegisterResult {
            return RegisterResult(false, message, throwable)
        }
    }
}
