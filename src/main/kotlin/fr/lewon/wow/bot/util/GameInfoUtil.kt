package fr.lewon.wow.bot.util

object GameInfoUtil {

    fun isMatch(red: Byte, green: Byte, blue: Byte): Boolean? {
        return isBigger(red, green) && isBigger(red, blue) && areClose(blue, green)
    }

    private fun isBigger(red: Byte, other: Byte): Boolean {
        return red * 0.5 > other
    }

    private fun areClose(color1: Byte, color2: Byte): Boolean {
        val max = maxOf(color1, color2)
        val min = minOf(color1, color2)
        return min * 2.0 > max - 20
    }

}