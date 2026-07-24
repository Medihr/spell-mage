package com.spellmage.game

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    var gameEngine: GameEngine? = null
    var onGameOverListener: (() -> Unit)? = null

    private var thread: Thread? = null
    private var isRunning = false
    private val holder: SurfaceHolder = getHolder()

    private val bgBitmaps = mutableMapOf<Int, Bitmap>()

    private val paintBlock = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintGlow = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        holder.addCallback(this)
        loadBackgrounds()
    }

    fun loadBackgrounds() {
        val resIds = listOf(
            R.drawable.bg_mago_estelar,
            R.drawable.bg_mago_vortice,
            R.drawable.bg_mago_varita,
            R.drawable.bg_mago_galactico,
            R.drawable.fondomenu
        )

        for (resId in resIds) {
            try {
                val bmp = BitmapFactory.decodeResource(resources, resId)
                if (bmp != null) {
                    bgBitmaps[resId] = bmp
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isRunning = true
        thread = Thread(this)
        thread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        try {
            thread?.join(500)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        thread = null
    }

    override fun run() {
        var lastTime = System.currentTimeMillis()

        while (isRunning) {
            val now = System.currentTimeMillis()
            val elapsed = now - lastTime

            if (elapsed >= 16) { // ~60 FPS
                lastTime = now
                val engine = gameEngine
                if (engine != null && engine.isGameRunning && !engine.isPaused) {
                    val keyboardHeight = height * 0.32f
                    val keyboardTopY = height - keyboardHeight
                    engine.update(width.toFloat(), keyboardTopY)

                    if (engine.hp <= 0 && !engine.hasTriggeredGameOver) {
                        engine.hasTriggeredGameOver = true
                        post {
                            onGameOverListener?.invoke()
                        }
                    }
                }

                drawCanvas()
            }
        }
    }

    private fun drawCanvas() {
        if (!holder.surface.isValid) return

        val canvas = holder.lockCanvas() ?: return

        try {
            val engine = gameEngine
            val bgResId = engine?.selectedBackgroundRes ?: R.drawable.bg_mago_estelar
            val bgBitmap = bgBitmaps[bgResId]

            if (bgBitmap != null) {
                val srcRect = Rect(0, 0, bgBitmap.width, bgBitmap.height)
                val destRect = Rect(0, 0, width, height)
                canvas.drawBitmap(bgBitmap, srcRect, destRect, null)
            } else {
                canvas.drawColor(Color.parseColor("#0B0714"))
            }

            // Darkness Overlay
            paintBlock.style = Paint.Style.FILL
            paintBlock.color = Color.argb(120, 0, 0, 0)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBlock)

            if (engine != null && engine.isGameRunning) {
                val trailList: List<TrailParticle>
                val blockList: List<FallingBlock>
                val sparkList: List<SparkParticle>

                synchronized(engine.lock) {
                    trailList = ArrayList(engine.trailParticles)
                    blockList = ArrayList(engine.blocks)
                    sparkList = ArrayList(engine.sparkParticles)
                }

                // 1. Draw Trail Particles
                for (p in trailList) {
                    paintGlow.color = p.color
                    paintGlow.alpha = (p.alpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawCircle(p.x, p.y, p.size, paintGlow)
                }

                // 2. Draw Falling Blocks
                for (block in blockList) {
                    val isTarget = (engine.targetBlock == block)

                    paintBlock.style = Paint.Style.FILL
                    paintBlock.color = block.colorStyle.bg
                    val rectF = RectF(block.x, block.y, block.x + block.width, block.y + block.height)
                    canvas.drawRoundRect(rectF, 18f, 18f, paintBlock)

                    paintBlock.style = Paint.Style.STROKE
                    paintBlock.strokeWidth = if (isTarget) 8f else 4f
                    paintBlock.color = if (isTarget) Color.YELLOW else block.colorStyle.border
                    canvas.drawRoundRect(rectF, 18f, 18f, paintBlock)

                    paintText.textSize = 52f
                    paintText.typeface = Typeface.DEFAULT_BOLD
                    paintText.textAlign = Paint.Align.CENTER
                    val textY = block.y + block.height / 2f + 18f

                    if (isTarget && block.typedCount > 0) {
                        val typedStr = block.word.substring(0, block.typedCount.coerceAtMost(block.word.length))
                        val untypedStr = block.word.substring(block.typedCount.coerceAtMost(block.word.length))

                        val fullWidth = paintText.measureText(block.word)
                        var startX = (block.x + block.width / 2f) - (fullWidth / 2f)

                        paintText.textAlign = Paint.Align.LEFT

                        paintText.color = Color.YELLOW
                        canvas.drawText(typedStr, startX, textY, paintText)
                        startX += paintText.measureText(typedStr)

                        paintText.color = Color.WHITE
                        canvas.drawText(untypedStr, startX, textY, paintText)
                    } else {
                        paintText.color = Color.WHITE
                        canvas.drawText(block.word, block.x + block.width / 2f, textY, paintText)
                    }
                }

                // 3. Draw Spark Particles
                for (p in sparkList) {
                    paintGlow.color = p.color
                    paintGlow.alpha = (p.alpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawCircle(p.x, p.y, p.size, paintGlow)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val engine = gameEngine ?: return true
            if (!engine.isGameRunning || engine.isPaused) return true

            val touchX = event.x
            val touchY = event.y

            synchronized(engine.lock) {
                for (block in engine.blocks.reversed()) {
                    if (touchX >= block.x && touchX <= block.x + block.width &&
                        touchY >= block.y && touchY <= block.y + block.height) {
                        engine.targetBlock = block
                        break
                    }
                }
            }
        }
        return true
    }
}
