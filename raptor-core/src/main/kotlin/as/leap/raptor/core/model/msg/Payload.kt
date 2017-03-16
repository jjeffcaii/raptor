package `as`.leap.raptor.core.model.msg

import `as`.leap.raptor.core.model.ChunkType

interface Payload {
  fun type(): ChunkType
}