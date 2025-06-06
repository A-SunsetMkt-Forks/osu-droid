package com.rian.osu.difficulty.evaluators

import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModHidden
import com.rian.osu.mods.ModTraceable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * An evaluator for calculating osu!droid flashlight difficulty.
 */
object DroidFlashlightEvaluator {
    private const val MAX_OPACITY_BONUS = 0.4
    private const val HIDDEN_BONUS = 0.2
    private const val TRACEABLE_CIRCLE_BONUS = 0.15
    private const val TRACEABLE_OBJECT_BONUS = 0.1
    private const val MIN_VELOCITY = 0.5
    private const val SLIDER_MULTIPLIER = 1.3
    private const val MIN_ANGLE_MULTIPLIER = 0.2

    /**
     * Evaluates the difficulty of memorizing and hitting the current object, based on:
     *
     * - distance between a number of previous objects and the current object,
     * - the visual opacity of the current object,
     * - the angle made by the current object,
     * - length and speed of the current object (for sliders),
     * - whether the Hidden mod is enabled,
     * - and whether the Traceable mod is enabled.
     *
     * @param current The current object.
     * @param mods The mods used.
     * @param withSliders Whether to take slider difficulty into account.
     */
    @JvmStatic
    fun evaluateDifficultyOf(current: DroidDifficultyHitObject, mods: Iterable<Mod>, withSliders: Boolean): Double {
        if (
            current.obj is Spinner ||
            // Exclude overlapping objects that can be tapped at once.
            current.isOverlapping(true)
        ) {
            return 0.0
        }

        val scalingFactor = 52 / current.obj.difficultyRadius

        var smallDistNerf = 1.0
        var cumulativeStrainTime = 0.0
        var result = 0.0
        var last = current
        var angleRepeatCount = 0.0

        for (i in 0 until min(current.index, 10)) {
            val currentObject = current.previous(i)!!

            cumulativeStrainTime += last.strainTime

            // Exclude overlapping objects that can be tapped at once.
            if (currentObject.obj !is Spinner) {
                val jumpDistance = current.obj.difficultyStackedPosition
                    .getDistance(currentObject.obj.difficultyStackedEndPosition)

                // We want to nerf objects that can be easily seen within the Flashlight circle radius.
                if (i == 0) {
                    smallDistNerf = min(1.0, jumpDistance / 75.0)
                }

                // We also want to nerf stacks so that only the first object of the stack is accounted for.
                val stackNerf = min(1.0, currentObject.lazyJumpDistance / scalingFactor / 25)

                // Bonus based on how visible the object is.
                val opacityBonus = 1 + MAX_OPACITY_BONUS * (1 - current.opacityAt(currentObject.obj.startTime, mods))
                result += stackNerf * opacityBonus * scalingFactor * jumpDistance / cumulativeStrainTime

                // Objects further back in time should count less for the nerf.
                if (currentObject.angle != null && current.angle != null &&
                    abs(currentObject.angle!! - current.angle!!) < 0.02) {
                    angleRepeatCount += max(0.0, 1 - 0.1 * i)
                }
            }

            last = currentObject as DroidDifficultyHitObject
        }

        result = (smallDistNerf * result).pow(2)

        if (mods.any { it is ModHidden }) {
            // Additional bonus for Hidden due to there being no approach circles.
            result *= 1 + HIDDEN_BONUS
        } else if (mods.any { it is ModTraceable }) {
            // Additional bonus for Traceable due to there being no primary or secondary object pieces.
            result *= 1 +
                // Additional bonus for hit circles due to there being no circle piece, which is the primary piece.
                if (current.obj is HitCircle) TRACEABLE_CIRCLE_BONUS
                // The rest of the objects only hide secondary pieces.
                else TRACEABLE_OBJECT_BONUS
        }

        // Nerf patterns with repeated angles.
        result *= MIN_ANGLE_MULTIPLIER + (1 - MIN_ANGLE_MULTIPLIER) / (angleRepeatCount + 1)

        var sliderBonus = 0.0

        if (current.obj is Slider && withSliders) {
            // Invert the scaling factor to determine the true travel distance independent of circle size.
            val pixelTravelDistance = current.lazyTravelDistance / scalingFactor

            // Reward sliders based on velocity.
            sliderBonus = max(0.0, pixelTravelDistance / current.travelTime - MIN_VELOCITY).pow(0.5)

            // Longer sliders require more memorization.
            sliderBonus *= pixelTravelDistance

            // Nerf sliders with repeats, as less memorization is required.
            if (current.obj.repeatCount > 0)
                sliderBonus /= current.obj.repeatCount + 1
        }

        result += sliderBonus * SLIDER_MULTIPLIER

        return result
    }
}