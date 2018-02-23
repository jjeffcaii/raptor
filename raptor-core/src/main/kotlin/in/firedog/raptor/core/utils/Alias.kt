package `in`.firedog.raptor.core.utils

typealias Filter<T> = (T) -> Boolean
typealias Callback<T> = (T) -> Unit
typealias Do = () -> Unit