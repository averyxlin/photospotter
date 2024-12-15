package com.example.android.ui

object TagColours {
    private val tagColourMap = HashMap<Int, Int>()

    fun getColor(tagId: Int): Int? {
        return tagColourMap[tagId]
    }

    fun setColor(tagId: Int, color: Int) {
        tagColourMap[tagId] = color
    }

    fun removeColor(tagId: Int) {
        tagColourMap.remove(tagId)
    }

    fun clear() {
        tagColourMap.clear()
    }

    fun exportMap(): Map<Int, Int> {
        return HashMap(tagColourMap)
    }
}