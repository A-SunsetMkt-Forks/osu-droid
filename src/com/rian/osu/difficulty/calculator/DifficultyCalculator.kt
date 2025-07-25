package com.rian.osu.difficulty.calculator

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.IBeatmap
import com.rian.osu.beatmap.PlayableBeatmap
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.sections.BeatmapHitObjects
import com.rian.osu.difficulty.DifficultyHitObject
import com.rian.osu.difficulty.attributes.DifficultyAttributes
import com.rian.osu.difficulty.attributes.TimedDifficultyAttributes
import com.rian.osu.difficulty.skills.Skill
import com.rian.osu.mods.*
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * A difficulty calculator for calculating star rating.
 */
abstract class DifficultyCalculator<TBeatmap : PlayableBeatmap, TObject : DifficultyHitObject, TAttributes : DifficultyAttributes> {
    protected abstract val difficultyMultiplier: Double

    /**
     * [Mod]s that can alter the star rating when they are used in calculation with one or more [Mod]s.
     */
    protected open val difficultyAdjustmentMods = setOf(
        ModRelax::class, ModAutopilot::class, ModEasy::class, ModReallyEasy::class,
        ModMirror::class, ModHardRock::class, ModHidden::class, ModFlashlight::class,
        ModDifficultyAdjust::class, ModRateAdjust::class, ModTimeRamp::class
    )

    /**
     * Retains [Mod]s that change star rating within a collection of [Mod]s.
     *
     * @param mods The collection of [Mod]s to check.
     * @return A set of [Mod]s that change star rating.
     */
    fun retainDifficultyAdjustmentMods(mods: Iterable<Mod>?) = mods?.toMutableSet()?.also {
        it.retainAll { mod -> difficultyAdjustmentMods.any { it.isInstance(mod) } }
    } ?: emptySet()

    /**
     * Calculates the difficulty of a [Beatmap] with specific [Mod]s.
     *
     * @param beatmap The [Beatmap] whose difficulty is to be calculated.
     * @param mods The [Mod]s to apply to the [Beatmap].
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A structure describing the difficulty of the [Beatmap].
     */
    @JvmOverloads
    fun calculate(beatmap: Beatmap, mods: Iterable<Mod>? = null, scope: CoroutineScope? = null) =
        calculate(createPlayableBeatmap(beatmap, mods, scope), scope)

    /**
     * Calculates the difficulty of a [PlayableBeatmap].
     *
     * @param beatmap The [PlayableBeatmap] whose difficulty is to be calculated.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A structure describing the difficulty of the [PlayableBeatmap].
     */
    @JvmOverloads
    fun calculate(beatmap: TBeatmap, scope: CoroutineScope? = null): TAttributes {
        val skills = createSkills(beatmap)
        val objects = createDifficultyHitObjects(beatmap, scope)

        for (obj in objects) {
            for (skill in skills) {
                scope?.ensureActive()
                skill.process(obj)
            }
        }

        return createDifficultyAttributes(beatmap, skills, objects)
    }

    /**
     * Calculates the difficulty of a [Beatmap] with specific [Mod]s and returns a set of
     * [TimedDifficultyAttributes] representing the difficulty at every relevant time value in the [Beatmap].
     *
     * @param beatmap The [Beatmap] whose difficulty is to be calculated.
     * @param mods The [Mod]s to apply to the [Beatmap].
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return The set of [TimedDifficultyAttributes].
     */
    @JvmOverloads
    fun calculateTimed(
        beatmap: Beatmap,
        mods: Iterable<Mod>? = null,
        scope: CoroutineScope? = null
    ): Array<TimedDifficultyAttributes<TAttributes>> {
        if (beatmap.hitObjects.objects.isEmpty()) {
            return emptyArray()
        }

        return calculateTimed(createPlayableBeatmap(beatmap, mods, scope), scope)
    }

    /**
     * Calculates the difficulty of a [PlayableBeatmap] and returns a set of [TimedDifficultyAttributes]
     * representing the difficulty at every relevant time value in the [PlayableBeatmap].
     *
     * @param beatmap The [PlayableBeatmap] whose difficulty is to be calculated.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return The set of [TimedDifficultyAttributes].
     */
    @JvmOverloads
    fun calculateTimed(beatmap: TBeatmap, scope: CoroutineScope? = null): Array<TimedDifficultyAttributes<TAttributes>> {
        if (beatmap.hitObjects.objects.isEmpty()) {
            return emptyArray()
        }

        val attributes = arrayOfNulls<TimedDifficultyAttributes<TAttributes>>(beatmap.hitObjects.objects.size)
        val skills = createSkills(beatmap)
        val progressiveBeatmap = ProgressiveCalculationBeatmap(beatmap)

        val difficultyObjects = createDifficultyHitObjects(beatmap, scope)
        var currentIndex = 0

        for (i in beatmap.hitObjects.objects.indices) {
            val obj = beatmap.hitObjects.objects[i]

            progressiveBeatmap.hitObjects.add(obj)

            while (currentIndex < difficultyObjects.size && difficultyObjects[currentIndex].obj.endTime <= obj.endTime) {
                for (skill in skills) {
                    scope?.ensureActive()
                    skill.process(difficultyObjects[currentIndex])
                }

                currentIndex++
            }

            attributes[i] = TimedDifficultyAttributes(
                obj.endTime,
                createDifficultyAttributes(progressiveBeatmap, skills, difficultyObjects.sliceArray(0..<currentIndex))
            )
        }

        @Suppress("UNCHECKED_CAST")
        return attributes as Array<TimedDifficultyAttributes<TAttributes>>
    }

    /**
     * Retrieves a [Skill] of a specific type from an array of [Skill]s.
     *
     * @param predicate The predicate to filter the [Skill]s by, if any.
     * @return The [Skill] of the specified type.
     */
    protected inline fun <reified T : Skill<TObject>> Array<Skill<TObject>>.find(predicate: (T) -> Boolean = { true }) =
        firstOrNull { it is T && predicate(it) } as T?

    /**
     * Creates the [Skill]s to calculate the difficulty of a [PlayableBeatmap].
     *
     * @param beatmap The [PlayableBeatmap] whose difficulty will be calculated.
     * @return The [Skill]s.
     */
    protected abstract fun createSkills(beatmap: TBeatmap): Array<Skill<TObject>>

    /**
     * Retrieves the [DifficultyHitObject]s to calculate against.
     *
     * @param beatmap The [PlayableBeatmap] providing the hit objects to generate from.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return The generated [DifficultyHitObject]s.
     */
    protected abstract fun createDifficultyHitObjects(beatmap: TBeatmap, scope: CoroutineScope? = null): Array<TObject>

    /**
     * Calculates the rating of a [Skill] based on its difficulty.
     *
     * @param skill The [Skill] to calculate the rating for.
     * @return The rating of the [Skill].
     */
    protected fun calculateRating(skill: Skill<TObject>) = sqrt(skill.difficultyValue()) * difficultyMultiplier

    /**
     * Creates a [TAttributes] to describe a beatmap's difficulty.
     *
     * @param beatmap The [PlayableBeatmap] whose difficulty was calculated.
     * @param skills The [Skill]s which processed the beatmap.
     * @param objects The [TObject]s that were generated.
     * @return [TAttributes] describing the beatmap's difficulty.
     */
    protected abstract fun createDifficultyAttributes(beatmap: PlayableBeatmap, skills: Array<Skill<TObject>>, objects: Array<TObject>): TAttributes

    /**
     * Constructs a [PlayableBeatmap] from a [Beatmap] with specific parameters.
     *
     * @param beatmap The [Beatmap] to create a [PlayableBeatmap] from.
     * @param mods The [Mod]s that should be applied to the [Beatmap].
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return The [PlayableBeatmap].
     */
    protected abstract fun createPlayableBeatmap(beatmap: Beatmap, mods: Iterable<Mod>?, scope: CoroutineScope?): TBeatmap

    companion object {
        /**
         * The epoch time of the last change to the difficulty calculator, in milliseconds.
         */
        const val VERSION = 1746800175000
    }
}

/**
 * An [IBeatmap] that is used for timed difficulty calculation.
 */
private class ProgressiveCalculationBeatmap(
    private val baseBeatmap: PlayableBeatmap
) : PlayableBeatmap(baseBeatmap, baseBeatmap.mode, baseBeatmap.mods.values) {
    override var maxCombo = 0

    override val hitObjects = object : BeatmapHitObjects() {
        override fun add(obj: HitObject) {
            super.add(obj)

            maxCombo += if (obj is Slider) obj.nestedHitObjects.size else 1
        }

        override fun remove(obj: HitObject): Boolean {
            val removed = super.remove(obj)

            if (removed) {
                maxCombo -= if (obj is Slider) obj.nestedHitObjects.size else 1
            }

            return removed
        }

        override fun remove(index: Int): HitObject? {
            val removed = super.remove(index)

            if (removed != null) {
                maxCombo -= if (removed is Slider) removed.nestedHitObjects.size else 1
            }

            return removed
        }
    }

    override fun createHitWindow() = baseBeatmap.hitWindow
}
