package io.github.alexispurslane.bloc

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
    val autofillNode =
        AutofillNode(onFill = onFill, autofillTypes = autofillTypes)
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

sealed class Either<T, E>() {
    class Success<T, E>(val value: T) : Either<T, E>()
    class Error<T, E>(val value: E) : Either<T, E>()
}

object Constants {
    val LOADING_SCREEN_REMARKS: List<String> = listOf(
        "The state calls its own violence law, but that of the individual, crime.",
        "Whoever will be free must make themself free.",
        "All things are Nothing to Me.",
        "I also love human beings, not just a few individuals, but every one.",
        "The people is dead! Good day, Self!",
        "Revolution is aimed at new arrangements; insurrection leads us no longer to let ourselves be arranged, but to arrange ourselves.",
        "The people of the future will yet fight their way to many a liberty that we do not even miss.",
        "We do not aspire to communal life, but to a life apart.",
        "The people's good is my misfortune!",
        "I have based my affairs on Nothing.",
        "Everything that tries to push its cause over on us is concerned only with itself, and not with us, only with its well-being, and not with ours.",
        "God and humanity have based their affair on nothing, on nothing but themselves. I likewise base my affair on myself.",
        "If God, if humanity, as you affirm, have enough content in themselves to be all in all to themselves, then I feel that I would lack it even less, and that I would have no complaint to make about my “emptiness.”",
        "I am not nothing in the sense of emptiness, but am the creative nothing, the nothing out of which I myself create everything as creator.",
        "Away, then, with every cause that is not completely my affair.",
        "I am myself my own affair, and I am neither good nor bad. Neither makes any sense to me.",
        "The great are only great because we are on our knees. Let us rise!",
        "I have been warned that if I continue to blaspheme, the heavens will strike me. — OK! I say, If the heavens intervene, I am a goner!",
        "God is stupidity and cowardice; God is hypocrisy and falsehood; God is tyranny and misery; God is evil!",
        "Property is theft!",
        "When deeds speak, words are nothing.",
        "Although a firm friend of order, I am an anarchist.",
        "An empty stomach knows no morality.",
        "The emancipation of the working class can only be achieved by the working class itself — without the assistance of governments.",
        "As man seeks justice in equality, so society seeks order in anarchy.",
        "If everyone is my brother, I have no brothers.",
        "Laws! We know what they are, and what they are worth! Spider webs for the rich and powerful, steel chains for the weak and poor, fishing nets in the hands of the government.",
        "To restore religion, it is necessary to condemn the Church.",
        "Any society... will have its limits. And outside the limits of any society, the unruly and the heroic vagabonds will wander with their wild and virgin thoughts, planning ever new and dreadful outbursts of rebellion!",
        "Without music, life would be a mistake.",
        "If I can't dance, it's not my revolution.",
        "People have only as much liberty as they have the intelligence to want and the courage to take.",
        "If voting changed anything, they'd make it illegal.",
        "When we can't dream any longer we die.",
        "The strongest bulwark of authority is uniformity; the least divergence from it is the greatest crime.",
        "And those who were seen dancing were thought to be insane by those who could not hear the music.",
        "You must have chaos within you to give birth to a dancing star.",
        "In heaven, all the interesting people are missing.",
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