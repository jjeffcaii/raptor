package `as`.leap.raptor.commons.exception

open class RaptorException(
    message: String? = null,
    cause: Throwable? = null,
    enableSuppression: Boolean,
    writableStackTrace: Boolean) : Exception(message, cause, enableSuppression, writableStackTrace) {
}