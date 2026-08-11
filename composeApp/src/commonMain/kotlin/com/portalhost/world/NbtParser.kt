package com.portalhost.world

sealed class NbtTag {
    object End : NbtTag()
    data class NbtByte(val value: Byte) : NbtTag()
    data class NbtShort(val value: Short) : NbtTag()
    data class NbtInt(val value: Int) : NbtTag()
    data class NbtLong(val value: Long) : NbtTag()
    data class NbtFloat(val value: Float) : NbtTag()
    data class NbtDouble(val value: Double) : NbtTag()
    data class NbtByteArray(val value: ByteArray) : NbtTag()
    data class NbtString(val value: String) : NbtTag()
    data class NbtList(val tags: List<NbtTag>) : NbtTag()
    data class NbtCompound(val children: MutableMap<String, NbtTag>) : NbtTag()
    data class NbtIntArray(val value: IntArray) : NbtTag()
    data class NbtLongArray(val value: LongArray) : NbtTag()
}

object NbtParser {

    private const val TAG_END = 0
    private const val TAG_BYTE = 1
    private const val TAG_SHORT = 2
    private const val TAG_INT = 3
    private const val TAG_LONG = 4
    private const val TAG_FLOAT = 5
    private const val TAG_DOUBLE = 6
    private const val TAG_BYTE_ARRAY = 7
    private const val TAG_STRING = 8
    private const val TAG_LIST = 9
    private const val TAG_COMPOUND = 10
    private const val TAG_INT_ARRAY = 11
    private const val TAG_LONG_ARRAY = 12

    const val MAX_PAYLOAD = 32 * 1024 * 1024

    /**
     * Parses a root named tag from the given big-endian NBT bytes.
     * Returns the root compound's children, or null on any error.
     */
    fun parse(data: ByteArray): NbtTag.NbtCompound? {
        return try {
            Parser(data).read()
        } catch (_: Exception) {
            null
        }
    }

    private class Parser(private val data: ByteArray) {
        private var pos = 0

        fun read(): NbtTag.NbtCompound? {
            val type = readUByte()
            if (type == TAG_END) return NbtTag.NbtCompound(mutableMapOf())
            readString() // root name
            val root = readPayload(type)
            return root as? NbtTag.NbtCompound
        }

        private fun readPayload(type: Int): NbtTag {
            return when (type) {
                TAG_END -> NbtTag.End
                TAG_BYTE -> NbtTag.NbtByte(readByte())
                TAG_SHORT -> NbtTag.NbtShort(readShort())
                TAG_INT -> NbtTag.NbtInt(readInt())
                TAG_LONG -> NbtTag.NbtLong(readLong())
                TAG_FLOAT -> NbtTag.NbtFloat(Float.fromBits(readInt()))
                TAG_DOUBLE -> NbtTag.NbtDouble(Double.fromBits(readLong()))
                TAG_BYTE_ARRAY -> {
                    val len = readInt()
                    ensure(len)
                    val value = ByteArray(len) { data[pos + it] }
                    pos += len
                    NbtTag.NbtByteArray(value)
                }
                TAG_STRING -> NbtTag.NbtString(readString())
                TAG_LIST -> {
                    val elementType = readUByte()
                    val len = readInt()
                    ensure(len * 4)
                    val tags = ArrayList<NbtTag>(len)
                    repeat(len) { tags.add(readPayload(elementType)) }
                    NbtTag.NbtList(tags)
                }
                TAG_COMPOUND -> {
                    val children = mutableMapOf<String, NbtTag>()
                    while (true) {
                        val childType = readUByte()
                        if (childType == TAG_END) break
                        val name = readString()
                        children[name] = readPayload(childType)
                    }
                    NbtTag.NbtCompound(children)
                }
                TAG_INT_ARRAY -> {
                    val len = readInt()
                    ensure(len * 4)
                    val value = IntArray(len) { readInt() }
                    NbtTag.NbtIntArray(value)
                }
                TAG_LONG_ARRAY -> {
                    val len = readInt()
                    ensure(len * 8)
                    val value = LongArray(len) { readLong() }
                    NbtTag.NbtLongArray(value)
                }
                else -> throw IllegalArgumentException("Unknown NBT tag type $type at offset $pos")
            }
        }

        private fun readByte(): Byte = data[pos++]

        private fun readUByte(): Int = data[pos++].toInt() and 0xFF

        private fun readShort(): Short {
            ensure(2)
            val v = ((readUByte() shl 8) or readUByte()).toShort()
            return v
        }

        private fun readInt(): Int {
            ensure(4)
            return (readUByte() shl 24) or (readUByte() shl 16) or (readUByte() shl 8) or readUByte()
        }

        private fun readLong(): Long {
            ensure(8)
            val high = readInt().toLong()
            val low = readInt().toLong() and 0xFFFFFFFFL
            return (high shl 32) or low
        }

        private fun readString(): String {
            val len = readUShort()
            ensure(len)
            val value = data.copyOfRange(pos, pos + len).toString(Charsets.UTF_8)
            pos += len
            return value
        }

        private fun readUShort(): Int {
            ensure(2)
            return (readUByte() shl 8) or readUByte()
        }

        private fun ensure(bytes: Int) {
            if (bytes < 0 || pos < 0 || pos + bytes > data.size) {
                throw IndexOutOfBoundsException("NBT read out of bounds")
            }
        }
    }
}
