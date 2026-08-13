package com.anthonykeyboard.app

object KeyboardLayouts {

    val PERSIAN = listOf(
        listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "چ"),
        listOf("ش", "س", "ی", "ب", "ل", "ا", "ت", "ن", "م", "ک", "گ"),
        listOf("ظ", "ط", "ز", "ر", "ذ", "د", "پ", "و", "ژ")
    )

    val ENGLISH = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("z", "x", "c", "v", "b", "n", "m")
    )

    fun allLetters(): List<String> {
        return (PERSIAN.flatten() + ENGLISH.flatten())
    }
}
