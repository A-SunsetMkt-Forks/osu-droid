package com.reco1l.andengine.modifier

import com.edlplan.framework.easing.Easing
import com.reco1l.andengine.modifier.ModifierType.*


/**
 * A chain of modifiers that can be applied to an entity.
 *
 * An entity is the first node in the chain, and each modifier is a node that follows it.
 */
interface IModifierChain {


    /**
     * Obtains a modifier from the modifier pool or creates a new one.
     *
     * Implementations should override this method to support their own modifier pools
     * as well register the modifier as nested to the current chain and use the [block]
     * callback on it.
     *
     * @param block The block of code to execute on the newly created modifier.
     */
    fun applyModifier(block: UniversalModifier.() -> Unit): UniversalModifier


    // Nested chains

    /**
     * Begins a parallel chain of modifiers.
     */
    fun beginParallel(block: UniversalModifier.() -> Unit): UniversalModifier {
        return applyModifier {
            type = Parallel
            allowNesting = true
            block()
            allowNesting = false
        }
    }


    // Delay

    /**
     * Delays the execution of the next modifier in the chain.
     */
    fun delay(durationSec: Float): UniversalModifier {
        return applyModifier {
            type = Delay
            duration = durationSec
        }
    }


    // Translate

    fun translateTo(valueX: Float, valueY: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return applyModifier {
            type = TranslateXY
            duration = durationSec
            finalValues = floatArrayOf(valueX, valueY)
            eased(easing)
        }
    }

    fun translateToX(value: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return applyModifier {
            type = TranslateX
            duration = durationSec
            finalValues = floatArrayOf(value)
            eased(easing)
        }
    }

    fun translateToY(value: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return applyModifier {
            type = TranslateY
            duration = durationSec
            finalValues = floatArrayOf(value)
            eased(easing)
        }
    }


    // Move

    fun moveTo(valueX: Float, valueY: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return applyModifier {
            type = MoveXY
            duration = durationSec
            finalValues = floatArrayOf(valueX, valueY)
            eased(easing)
        }
    }

    fun moveToX(value: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return applyModifier {
            type = MoveX
            duration = durationSec
            finalValues = floatArrayOf(value)
            eased(easing)
        }
    }

    fun moveToY(value: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return applyModifier {
            type = MoveY
            duration = durationSec
            finalValues = floatArrayOf(value)
            eased(easing)
        }
    }


    // Scale

    fun scaleTo(value: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return applyModifier {
            type = ScaleXY
            duration = durationSec
            finalValues = floatArrayOf(value, value)
            eased(easing)
        }
    }

    fun scaleToX(value: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return applyModifier {
            type = ScaleX
            duration = durationSec
            finalValues = floatArrayOf(value)
            eased(easing)
        }
    }

    fun scaleToY(value: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return applyModifier {
            type = ScaleY
            duration = durationSec
            finalValues = floatArrayOf(value)
            eased(easing)
        }
    }


    // Coloring

    fun fadeTo(value: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return applyModifier {
            type = Alpha
            duration = durationSec
            finalValues = floatArrayOf(value)
            eased(easing)
        }
    }

    fun fadeIn(durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return fadeTo(1f, durationSec, easing)
    }

    fun fadeInFromZero(durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return applyModifier {
            type = Alpha
            duration = durationSec
            initialValues = floatArrayOf(0f)
            finalValues = floatArrayOf(1f)
            eased(easing)
        }
    }

    fun fadeOut(durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return fadeTo(0f, durationSec, easing)
    }

    fun colorTo(red: Float, green: Float, blue: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return applyModifier {
            type = Color
            duration = durationSec
            finalValues = floatArrayOf(red, green, blue)
            eased(easing)
        }
    }


    // Rotation

    fun rotateTo(value: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return applyModifier {
            type = Rotation
            duration = durationSec
            finalValues = floatArrayOf(value)
            eased(easing)
        }
    }

}