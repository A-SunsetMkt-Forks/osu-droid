package com.reco1l.osu.hud.elements

import com.rian.osu.beatmap.hitobject.HitObject
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal

class HUDNotesPerSecondCounter : HUDStatisticCounter("Notes/sec") {

    override val name = "Notes per second counter"

    private val objects = ArrayDeque<HitObject>()

    override fun onHitObjectLifetimeStart(obj: HitObject) {
        objects.add(obj)
    }

    override fun onManagedUpdate(pSecondsElapsed: Float) {
        val elapsedTimeMs = getGlobal().gameScene.elapsedTime * 1000

        while (objects.isNotEmpty()) {
            val obj = objects.first()

            if (obj.startTime - obj.timePreempt + 1000 >= elapsedTimeMs) {
                break
            }

            objects.removeFirst()
        }

        valueText.text = objects.size.toString()
        super.onManagedUpdate(pSecondsElapsed)
    }
}