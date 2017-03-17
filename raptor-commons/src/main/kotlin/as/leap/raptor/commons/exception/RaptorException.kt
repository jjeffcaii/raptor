package `as`.leap.raptor.commons.exception

open class RaptorException(
    code: Int?,
    message: String?,
    cause: Throwable?,
    enableSuppression: Boolean,
    writableStackTrace: Boolean) : Exception(message, cause, enableSuppression, writableStackTrace) {
}