package ru.surfstudio.shadow_layout

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlin.math.max
import kotlin.math.min

class ShadowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        const val DEFAULT_RADIUS = 1
        const val DEFAULT_OFFSET = 0
        const val DEFAULT_ALPHA_PERCENT = 50
        const val DEFAULT_DOWNSCALE_RATE = 4
        const val MIN_BLUR_RADIUS = 1
        const val MAX_BLUR_RADIUS = 25
    }

    private var shadowBitmap: Bitmap? = null
    private val shadowPaint: Paint = Paint().apply {
        isAntiAlias = true
        isDither = true
        isFilterBitmap = true
    }
    private val shadowRect = Rect()

    private var shadowTopOffset: Int = DEFAULT_OFFSET
    private var shadowRightOffset: Int = DEFAULT_OFFSET
    private var shadowLeftOffset: Int = DEFAULT_OFFSET
    private var shadowBottomOffset: Int = DEFAULT_OFFSET
    private var shadowBlurRadius: Int = DEFAULT_RADIUS
    private var shadowAlphaPercent: Int = DEFAULT_ALPHA_PERCENT
    private var blurType: BlurType = BlurType.RENDERSCRIPT
    private var downscaleRate: Int = DEFAULT_DOWNSCALE_RATE

    init {
        clipToPadding = false
        obtainAttrs(context, attrs)
    }

    private fun obtainAttrs(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ShadowLayout)
        blurType = BlurType.getById(typedArray.getInteger(R.styleable.ShadowLayout_blurType, 0))

        downscaleRate =
            typedArray.getInteger(R.styleable.ShadowLayout_downscaleRate, DEFAULT_DOWNSCALE_RATE)
        require(downscaleRate > 0) { "DownscaleRate must be > 0, current downscaleRate: $downscaleRate" }

        shadowBlurRadius = safeCreateBlurRadius(
            typedArray.getInteger(R.styleable.ShadowLayout_shadowRadius, DEFAULT_RADIUS)
        )
        require(shadowBlurRadius > 0) { "ShadowBlurRadius must be > 0, current shadowBlurRadius: $shadowBlurRadius" }

        shadowAlphaPercent = typedArray.getInteger(
            R.styleable.ShadowLayout_shadowAlphaPercent,
            DEFAULT_ALPHA_PERCENT
        )
        shadowLeftOffset = typedArray.getDimensionPixelOffset(
            R.styleable.ShadowLayout_shadowLeftOffset,
            DEFAULT_OFFSET
        )
        shadowRightOffset = typedArray.getDimensionPixelOffset(
            R.styleable.ShadowLayout_shadowRightOffset,
            DEFAULT_OFFSET
        )
        shadowTopOffset = typedArray.getDimensionPixelOffset(
            R.styleable.ShadowLayout_shadowTopOffset,
            DEFAULT_OFFSET
        )
        shadowBottomOffset = typedArray.getDimensionPixelOffset(
            R.styleable.ShadowLayout_shadowBottomOffset,
            DEFAULT_OFFSET
        )

        shadowPaint.alpha = (shadowAlphaPercent / 100f * 255).toInt()

        resetPadding()
        typedArray.recycle()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearShadowBitmap()
    }

    override fun dispatchDraw(canvas: Canvas?) {
        if (shadowBitmap == null) {
            shadowBitmap = createShadowBitmap()
        }

        shadowBitmap?.let {
            updateShadowRect()
            canvas?.drawBitmap(it, null, shadowRect, shadowPaint)
        }

        super.dispatchDraw(canvas)
    }

    private fun updateShadowRect() {
        shadowRect.run {
            top = getTop() - shadowTopOffset
            left = getLeft() - shadowLeftOffset
            right = getRight() + shadowRightOffset
            bottom = getBottom() + shadowBottomOffset
        }
    }

    /**
     * Перерисовка тени (очищение и отрисовка bitmap заново).
     * Может понадобиться, когда контент дочерней View обновляется.
     *
     * Redrawing of the existing shadow (clearing and redrawing).
     * Could be useful when the content of the child view is changed.
     */
    fun redrawShadow() {
        clearShadowBitmap()
        invalidate()
    }

    private fun createShadowBitmap(): Bitmap? {
        val downscalePadding = (shadowBlurRadius / downscaleRate)

        val sourceBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val sourceCanvas = Canvas(sourceBitmap)
        background?.draw(sourceCanvas)
        super.dispatchDraw(sourceCanvas)

        val downscaledBitmap = Bitmap.createScaledBitmap(
            sourceBitmap,
            sourceBitmap.width / downscaleRate,
            sourceBitmap.height / downscaleRate,
            false
        )

        val paddedBitmap = downscaledBitmap.setPadding(downscalePadding)
        val radius = max(shadowBlurRadius / downscaleRate, MIN_BLUR_RADIUS)
        val blurredBitmap = when (blurType) {
            BlurType.RENDERSCRIPT -> paddedBitmap.renderscriptBlur(context, radius)
            BlurType.STACK -> paddedBitmap.stackBlur(radius)
        }

        val shadowWidth = width + shadowBlurRadius * 2
        val shadowHeight = height + shadowBlurRadius * 2
        return Bitmap.createScaledBitmap(
            blurredBitmap,
            shadowWidth,
            shadowHeight,
            true
        )
    }

    private fun resetPadding() {
        // Set padding for shadow bitmap
        //int left, int top, int right, int bottom
        setPadding(
            paddingLeft + max(shadowLeftOffset, 0),
            paddingTop + max(shadowTopOffset, 0),
            paddingRight + max(shadowRightOffset, 0),
            paddingBottom + max(shadowBottomOffset, 0)
        )
    }

    private fun safeCreateBlurRadius(desiredRadius: Int): Int {
        return when {
            blurType == BlurType.RENDERSCRIPT && desiredRadius < MIN_BLUR_RADIUS -> MIN_BLUR_RADIUS
            blurType == BlurType.RENDERSCRIPT && desiredRadius > MAX_BLUR_RADIUS -> MAX_BLUR_RADIUS
            else -> desiredRadius
        }
    }

    private fun clearShadowBitmap() {
        if (shadowBitmap != null) {
            shadowBitmap?.recycle()
            shadowBitmap = null
        }
    }
}