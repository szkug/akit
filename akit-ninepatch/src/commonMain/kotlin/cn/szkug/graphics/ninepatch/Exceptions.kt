package cn.szkug.graphics.ninepatch

class DivLengthException(message: String) : Exception(message)

class ChunkNotSerializedException : Exception("NinePatch chunk was not serialized")

class WrongPaddingException(message: String) : Exception(message)
