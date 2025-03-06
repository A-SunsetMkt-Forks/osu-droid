package com.reco1l.osu.hud.elements

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Axes
import com.reco1l.andengine.DepthInfo
import com.reco1l.andengine.shape.Circle
import com.reco1l.andengine.sprite.ExtendedSprite
import com.reco1l.framework.ColorARGB
import com.reco1l.osu.hud.HUDElement
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.GameHelper

class HUDBackButton : HUDElement() {

    private val requiredPressTimeMs = Config.getInt("back_button_press_time", 300).toFloat()

    private val arrow = ExtendedSprite().apply {
        textureRegion = ResourceManager.getInstance().getTexture("back-arrow")
        anchor = Anchor.Center
        origin = Anchor.Center

        relativeSizeAxes = Axes.Both
        setSize(0.6f, 0.6f)
    }

    private val backCircle = Circle().apply {
        setPortion(0f)
        anchor = Anchor.Center
        origin = Anchor.Center
        color = ColorARGB.White
        depthInfo = DepthInfo.Default

        relativeSizeAxes = Axes.Both
        setSize(1f, 1f)
    }

    private val frontCircle = Circle().apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        color = ColorARGB(0xFF002626)
        depthInfo = DepthInfo.Clear

        relativeSizeAxes = Axes.Both
        setSize(0.95f, 0.95f)
    }


    private var progress = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            val scale = 1f + field / 2f

            alpha = 0.25f * (field + 1)
            backCircle.setPortion(value)
            backCircle.setScale(scale)
            frontCircle.setScale(scale)
            arrow.setScale(scale)
        }

    private var holdDurationMs = 0f
        set(value) {
            field = value.coerceIn(0f, requiredPressTimeMs)
        }


    private var isPressed = false


    init {
        setSize(SIZE, SIZE)
        alpha = 0.25f

        attachChild(frontCircle)
        attachChild(backCircle)
        attachChild(arrow)
    }


    override fun onManagedUpdate(pSecondsElapsed: Float) {

        if (!isInEditMode) {
            val realMsElapsed = pSecondsElapsed * 1000 / GameHelper.getSpeedMultiplier()

            if (isPressed) {
                holdDurationMs += realMsElapsed
            } else {
                holdDurationMs -= realMsElapsed
            }

            progress = if (requiredPressTimeMs > 0) holdDurationMs / requiredPressTimeMs else 1f

            if (progress >= 1f) {
                isPressed = false
                GlobalManager.getInstance().gameScene.pause()
                progress = 0f
            }
        }

        super.onManagedUpdate(pSecondsElapsed)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (isInEditMode) {
            return super.onAreaTouched(event, localX, localY)
        }

        if (event.isActionDown) {
            isPressed = true
            return true
        }

        if (event.isActionMove) {
            if (isPressed) {
                if (localX <= 0f || localY <= 0f || localX >= drawWidth || localY >= drawHeight) {
                    isPressed = false
                }
                return true
            }
        }

        if (event.isActionUp) {
            if (isPressed) {
                isPressed = false
                return true
            }
        }

        return false
    }


    companion object {
        private const val SIZE = 72f
    }
}