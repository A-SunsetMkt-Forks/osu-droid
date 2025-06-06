package com.reco1l.andengine.sprite

import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.sprite.UISprite.*
import com.reco1l.andengine.sprite.ScaleType.*
import com.reco1l.framework.math.*
import org.anddev.andengine.opengl.texture.region.*
import org.anddev.andengine.opengl.util.*
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL11.*
import kotlin.math.*

/**
 * Sprite that allows to change texture once created.
 */
@Suppress("LeakingThis")
open class UISprite(textureRegion: TextureRegion? = null) : UIBufferedComponent<SpriteVBO>() {

    override var contentWidth: Float
        get() = textureRegion?.width?.toFloat() ?: 0f
        set(_) = Unit

    override var contentHeight: Float
        get() = textureRegion?.height?.toFloat() ?: 0f
        set(_) = Unit


    /**
     * Whether the texture should be flipped horizontally.
     */
    var flippedHorizontal = false
        set(value) {
            field = value
            textureRegion?.isFlippedHorizontal = value
        }

    /**
     * Whether the texture should be flipped vertically.
     */
    var flippedVertical = false
        set(value) {
            field = value
            textureRegion?.isFlippedVertical = value
        }

    /**
     * The texture region of the sprite.
     */
    var textureRegion = textureRegion
        set(value) {
            if (field != value) {
                field = value
                onTextureRegionChanged()
                invalidate(InvalidationFlag.Content)
            }
        }

    /**
     * The X position of the texture.
     */
    var textureX = 0
        set(value) {
            if (field != value) {
                field = value
                textureRegion?.setTexturePosition(value, textureY)
            }
        }

    /**
     * The Y position of the texture.
     */
    var textureY = 0
        set(value) {
            if (field != value) {
                field = value
                textureRegion?.setTexturePosition(textureX, value)
            }
        }

    /**
     * The scale type of the sprite.
     */
    var scaleType: ScaleType = Fit
        set(value) {
            if (field != value) {
                field = value
                invalidateBuffer(BufferInvalidationFlag.Data)
            }
        }

    /**
     * The alignment of the texture.
     *
     * If the scale type is [ScaleType.Stretch] it will not take effect.
     */
    var gravity: Vec2 = Anchor.Center
        set(value) {
            if (field != value) {
                field = value
                invalidateBuffer(BufferInvalidationFlag.Data)
            }
        }


    init {
        width = MatchContent
        height = MatchContent

        onTextureRegionChanged()
    }


    open fun onTextureRegionChanged() {

        val textureRegion = textureRegion ?: return

        textureRegion.setTexturePosition(textureX, textureY)
        textureRegion.isFlippedVertical = flippedVertical
        textureRegion.isFlippedHorizontal = flippedHorizontal

        blendInfo = if (textureRegion.texture.textureOptions.mPreMultipyAlpha) BlendInfo.PreMultiply else BlendInfo.Mixture
    }


    override fun onCreateBuffer(gl: GL10): SpriteVBO {
        return SpriteVBO()
    }

    override fun beginDraw(gl: GL10) {
        super.beginDraw(gl)
        GLHelper.enableTextures(gl)
        GLHelper.enableTexCoordArray(gl)
    }

    override fun onDrawBuffer(gl: GL10) {
        textureRegion?.onApply(gl)
        super.onDrawBuffer(gl)
    }


    inner class SpriteVBO : VertexBuffer(
        drawTopology = GL_TRIANGLE_STRIP,
        vertexCount = 4,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW
    ) {
        override fun update(gl: GL10, entity: UIBufferedComponent<*>, vararg data: Any) {

            val textureWidth = contentWidth
            val textureHeight = contentHeight

            var quadWidth = textureWidth
            var quadHeight = textureHeight

            when (scaleType) {

                Crop -> {
                    val scale = max(width / textureWidth, height / textureHeight)
                    quadWidth = textureWidth * scale
                    quadHeight = textureHeight * scale
                }

                Fit -> {
                    val scale = min(width / textureWidth, height / textureHeight)
                    quadWidth = textureWidth * scale
                    quadHeight = textureHeight * scale
                }

                Stretch -> Unit
            }

            val x = (width - quadWidth) * gravity.x
            val y = (height - quadHeight) * gravity.y

            addQuad(0, x, y, x + quadWidth, y + quadHeight)
        }
    }

}

enum class ScaleType {

    /**
     * Scale the texture to fill the entire sprite cropping the excess.
     */
    Crop,

    /**
     * Scale the texture to fit the sprite without cropping.
     */
    Fit,

    /**
     * Scale the texture to fit the sprite without cropping.
     * The texture will be stretched to fill the entire sprite.
     */
    Stretch
}