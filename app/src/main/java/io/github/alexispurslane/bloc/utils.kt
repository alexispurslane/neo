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
        "I also love human beings, not just a few individuals, but every one.",
        "The people is dead! Good day, Self!",
        "Revolution is aimed at new arrangements; insurrection leads us no longer to let ourselves be arranged, but to arrange ourselves.",
        "The people of the future will yet fight their way to many a liberty that we do not even miss.",
        "We do not aspire to communal life, but to a life apart.",
        "The people's good is my misfortune!",
        "I have based my affairs on Nothing.",
        "Everything that tries to push its cause over on us is concerned only with itself, and not with us, only with its well-being, and not with ours.",
        "God and humanity have based their affair on nothing but themselves. I likewise base my affair on myself.",
        "If God, if humanity, have enough content in themselves to be all in all to themselves, then I feel that I would lack it even less.",
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
        "Any society will have its limits. And outside those limits, the unruly and heroic vagabonds will wander with their wild and virgin thoughts, planning ever new and dreadful rebellion!",
        "Without music, life would be a mistake.",
        "If I can't dance, it's not my revolution.",
        "People have only as much liberty as they have the intelligence to want and the courage to take.",
        "If voting changed anything, they'd make it illegal.",
        "When we can't dream any longer we die.",
        "The strongest bulwark of authority is uniformity; the least divergence from it is the greatest crime.",
        "And those who were seen dancing were thought to be insane by those who could not hear the music.",
        "You must have chaos within you to give birth to a dancing star.",
        "In heaven, all the interesting people are missing.",
        "Prisons are universities of crime, maintained by the state.",
        "Variety is life; uniformity is death.",
        "The tendency of mutual aid in human beings is so deeply interwoven with all the past evolution of human beings, that it remains with us even now.",
        "Lenin is not comparable to any revolutionary figure in history. Revolutionaries have had ideals. Lenin has none.",
        "It is not difficult to see the absurdity of naming a few men and saying to them, \"Make laws regulating all our activities, although none of you knows anything about them!\"",
        "Well-being for all is not a dream.",
        "Struggle so that all may live this rich, overflowing life. And be sure that in this struggle you will find a joy greater than anything else can give.",
        "The working people cannot purchase with their wages the wealth which they have produced.",
        "But capital goes wherever there are men, poor enough to be exploited.",
        "In fact, we know full well today that it is futile to speak of liberty as long as economic slavery exists.",
        "Ask for work. If they don't give you work, ask for bread. If they do not give you work or bread, then take bread.",
        "Every society has the criminals it deserves.",
        "The law is a chariot wheel which binds us all regardless of conditions or place or time.",
        "Do not all theists insist that there can be no morality without the belief in a Divine Power? Based upon fear and hope, such morality has always been a vile product.",
        "I want freedom, the right to self-expression, everybody's right to beautiful, radiant things.",
        "The most absurd apology for authority and law is that they serve to diminish crime. Aside from the fact that the State is itself the greatest criminal, it has come to a standstill in coping with crime.",
        "I do not believe in God, because I believe in man. Whatever his mistakes, man has for thousands of years been working to undo the botched job your god has made.",
        "Politicians promise you heaven before election and give you hell after.",
        "No one is lazy. They grow hopeless from the misery of their present existence, and give up.",
        "If the individual has a right to govern himself, all external government is tyranny. Hence the necessity of abolishing the State.",
        "The shortest way to change a radical into a conservative, a liberal into a tyrant, a man into a beast, is to give him power over his fellows.",
        "The essence of government is control. He who attempts to control another is an invader; and the nature of such invasion is not changed, if it is made by all other people upon one person, as in democracy.",
        "To force a man to pay for the violation of his own liberty is indeed an addition of insult to injury.",
        "The State is said by some to be a necessary evil; it must be made unnecessary.",
        "The government is a tyrant living by theft, and therefore has no business to engage in any business.",
        "The Anarchists never have claimed that liberty will bring perfection; they simply say that its results are vastly preferable to those that follow authority.",
        "Voting is merely a labor-saving device for ascertaining on which side force lies and bowing to the inevitable. It is neither more nor less than a paper representative of the bullet.",
        "The State gives idle capital the power of increase, and, through interest, rent, profit, and taxes, robs industrious labor of its products.",
        "Monopoly and privilege must be destroyed, opportunity afforded, and competition encouraged. This is Liberty's work, and Down with Authority her war-cry.",
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