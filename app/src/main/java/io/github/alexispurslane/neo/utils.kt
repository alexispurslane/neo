package io.github.alexispurslane.neo

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.autofill(
    autofillTypes: List<AutofillType>,
    onFill: ((String) -> Unit),
) = composed {
    val autofill = LocalAutofill.current
    val autofillNode = AutofillNode(onFill = onFill, autofillTypes = autofillTypes)
    LocalAutofillTree.current += autofillNode

    this
        .onGloballyPositioned {
            autofillNode.boundingBox = it.boundsInWindow()
        }
        .onFocusChanged { focusState ->
            autofill?.run {
                if (focusState.isFocused) {
                    requestAutofillForNode(autofillNode)
                } else {
                    cancelAutofillForNode(autofillNode)
                }
            }
        }
}


object Constants {
    val LOADING_SCREEN_REMARKS: List<String> = listOf(
        "Unfortunately, no one can be told what the Matrix is. You have to see it for yourself.",
        "Throughout huma history, we have been dependent on machines to survive. Fate, it seems, is not without a sense of irony.",
        "I'm trying to free your mind, Neo. But I can only show you the door. You're the one that has to walk through it.",
        "You've felt it your entire life, that there's something wrong with the world. You don't know what it is, but it's there, like a splinter in your mind...",
        "What are you waiting for? You're faster than this. Don't think you are, know you are.",
        "If real is what you can feel, smell, taste, and see, then 'real' is simply electrical signals interpreted by your brain.",
        "You have to let it all go, Neo. Fear, doubt, and disbelief. Free your mind.",
        "There is a difference between knowing the path, and walking the path.",
        "To deny our impulses is to deny the very thing that makes us human.",
        "Ever had that feeling where you're not sure if you're awake or dreaming?",
        "There's no escaping reason, no denying purpose. Because as we both know, without purpose, we would not exist.",
        "Nobody cares how it works, as long as it works",
        "Hope: it is the quintessential human delusion, simultaneously the source of your greatest strength and your greatest weakness.",
        "The illusion of choice is a means of manipulation, since, as long as people believe they have a choice, they'll believe they are free.",
        "I'll show these people what you don't want them to see, a world without rules and controls, without borders or boundaries, a world where anything is possible.",
        "Men with power always yearn for more of it.",
        "Denial is the most predictable of human responses.",
        "I've had dreams that weren't just dreams.",
        "You don't realize how fake it all is until you unplug from the Matrix.",
        "I don't know the future. I didn't come here to tell you how this is going to end. I came here to tell you how it's going to begin.",
        "Choice. The problem is choice.",
        "Guns. Lots of guns.",
        "I am here not because of the path that lies before me, but because of the path that lies behind me.",
        "Hear that, Mr. Anderson? That's the sound of inevitability!",
        "Dodge this!",
        "You didn't come here to make a choice. You've already made it. You're here to try to understand why you made it.",
        "I expect just what I've always expected: for you to make up your own damn mind. Believe me or don't.",
        "We are all victims of causality. I drank too much wine; I must take a piss. Cause and effect.",
        "It is remarkable how similar the pattern of love is to the pattern of insanity.",
        "How about I give you the finger, and you give me my phone call.",
        "I know Kung-Fu!",
    )
}

fun <T> List<T>.findIndex(
    predicate: (index: Int, element: T) -> Boolean
): Int? {
    forEachIndexed { index, element ->
        if (predicate(index, element)) return index
    }

    return null
}