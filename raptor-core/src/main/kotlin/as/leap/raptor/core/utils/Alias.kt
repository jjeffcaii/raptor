package `as`.leap.raptor.core.utils

import `as`.leap.raptor.core.Chunk
import `as`.leap.raptor.core.model.Handshake
import `as`.leap.raptor.core.model.Message

typealias OnMessage = (Message<*>) -> Unit
typealias OnError = (Throwable) -> Unit
typealias OnClose = () -> Unit
typealias OnChunk = (Chunk) -> Unit
typealias OnHandshake = (Handshake) -> Unit
