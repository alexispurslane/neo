package io.github.alexispurslane.bloc.ui.composables.emojipicker.utils

fun String.capitalizeWords(): String {
    return this
        .split(' ')
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar {
                it.uppercaseChar()
            }
        }
}