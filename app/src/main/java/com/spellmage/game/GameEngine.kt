package com.spellmage.game

import android.graphics.Color
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

data class BlockColor(val bg: Int, val border: Int, val glow: Int, val isSpecial: String? = null)

class FallingBlock(
    val word: String,
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val speed: Float,
    val colorStyle: BlockColor
) {
    var typedCount: Int = 0
    val id: String = Random.nextInt(100000, 999999).toString()
}

class TrailParticle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    var size: Float,
    val color: Int,
    var alpha: Float = 0.9f
) {
    fun update() {
        x += vx
        y += vy
        alpha -= 0.035f
        size *= 0.95f
    }
}

class SparkParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    val color: Int,
    var alpha: Float = 1.0f
) {
    private val gravity = 0.2f
    private val friction = 0.96f

    fun update() {
        vx *= friction
        vy *= friction
        vy += gravity
        x += vx
        y += vy
        alpha -= 0.025f
    }
}

enum class Difficulty(val key: String, val label: String, val baseSpeed: Float, val spawnIntervalFrames: Int, val coinMult: Float) {
    LENTO("lento", "Lento", 1.2f, 130, 1.0f),
    MEDIO("medio", "Medio", 2.0f, 90, 1.5f),
    RAPIDO("rapido", "Rápido", 3.2f, 60, 2.5f)
}

class GameEngine {
    val lock = Any()

    var hp: Int = 3
    var maxHp: Int = 3
    var coins: Int = 0
    var score: Int = 0
    var currentLevel: Int = 1
    var difficulty: Difficulty = Difficulty.LENTO

    var isGameRunning: Boolean = false
    var isPaused: Boolean = false
    var selectedBackgroundRes: Int = R.drawable.fondojuego

    var isFreezeActive: Boolean = false
    var freezeTimerEndTime: Long = 0

    // Potion Inventory
    var countHealPotions: Int = 1
    var countFreezePotions: Int = 1
    var countNukePotions: Int = 0

    val blocks = mutableListOf<FallingBlock>()
    val trailParticles = mutableListOf<TrailParticle>()
    val sparkParticles = mutableListOf<SparkParticle>()

    var targetBlock: FallingBlock? = null
    var hasTriggeredGameOver: Boolean = false

    private var spawnTimer = 0
    private val wordBank = listOf(
        "A", "E", "I", "O", "U", "SOL", "LUZ", "MAR", "PAZ", "RUNA",
        "MAGIA", "FUEGO", "HIELO", "RAYO", "ALMA", "ORBE", "SABIO", "LIBRO",
        "HECHIZO", "CRISTAL", "VARITA", "ESTRELLA", "COSMOS", "ARCANO",
        "CONJURO", "PODER", "NUBE", "TORMENTA", "ALQUIMIA", "PULSO"
    )

    private val colorPalettes = listOf(
        BlockColor(Color.parseColor("#9D4EDD"), Color.parseColor("#E0AAFF"), Color.parseColor("#809D4EDD")),
        BlockColor(Color.parseColor("#2A9D8F"), Color.parseColor("#A8DADC"), Color.parseColor("#802A9D8F")),
        BlockColor(Color.parseColor("#E63946"), Color.parseColor("#FFB703"), Color.parseColor("#80E63946")),
        BlockColor(Color.parseColor("#00B4D8"), Color.parseColor("#90E0EF"), Color.parseColor("#8000B4D8")),
        BlockColor(Color.parseColor("#D4AF37"), Color.parseColor("#FFF7C2"), Color.parseColor("#80D4AF37")),
        BlockColor(Color.parseColor("#F72585"), Color.parseColor("#7209B7"), Color.parseColor("#80F72585"))
    )

    private val specialLifeStyle = BlockColor(Color.parseColor("#1B4332"), Color.parseColor("#52B788"), Color.parseColor("#E052B788"), "life")
    private val specialBoomStyle = BlockColor(Color.parseColor("#6B0504"), Color.parseColor("#FF758F"), Color.parseColor("#E0FF758F"), "boom")

    fun resetGame() {
        synchronized(lock) {
            hp = 3
            maxHp = 3
            coins = 0
            score = 0
            currentLevel = 1
            countHealPotions = 1
            countFreezePotions = 1
            countNukePotions = 0
            blocks.clear()
            trailParticles.clear()
            sparkParticles.clear()
            targetBlock = null
            spawnTimer = 0
            isFreezeActive = false
            hasTriggeredGameOver = false
            isGameRunning = true
            isPaused = false
        }
    }

    fun updateLevelProgression() {
        currentLevel = when {
            score >= 600 -> 4
            score >= 300 -> 3
            score >= 100 -> 2
            else -> 1
        }
    }

    fun useHealPotion(): Boolean {
        synchronized(lock) {
            if (countHealPotions > 0 && hp < maxHp) {
                countHealPotions--
                hp++
                return true
            }
            return false
        }
    }

    fun useFreezePotion(): Boolean {
        synchronized(lock) {
            if (countFreezePotions > 0) {
                countFreezePotions--
                isFreezeActive = true
                freezeTimerEndTime = System.currentTimeMillis() + 8000
                return true
            }
            return false
        }
    }

    fun useNukePotion(): Boolean {
        synchronized(lock) {
            if (countNukePotions > 0 && blocks.isNotEmpty()) {
                countNukePotions--
                clearAllBlocksOnScreen()
                return true
            }
            return false
        }
    }

    fun clearAllBlocksOnScreen() {
        val copy = ArrayList(blocks)
        blocks.clear()
        targetBlock = null
        for (b in copy) {
            explodeBlock(b, false)
        }
    }

    fun update(screenWidth: Float, keyboardTopY: Float) {
        if (!isGameRunning || isPaused) return

        synchronized(lock) {
            if (isFreezeActive && System.currentTimeMillis() > freezeTimerEndTime) {
                isFreezeActive = false
            }

            spawnTimer++
            if (spawnTimer >= difficulty.spawnIntervalFrames) {
                spawnTimer = 0
                spawnNewBlock(screenWidth)
            }

            val trailIter = trailParticles.iterator()
            while (trailIter.hasNext()) {
                val p = trailIter.next()
                p.update()
                if (p.alpha <= 0f) trailIter.remove()
            }

            val sparkIter = sparkParticles.iterator()
            while (sparkIter.hasNext()) {
                val p = sparkIter.next()
                p.update()
                if (p.alpha <= 0f) sparkIter.remove()
            }

            val speedMult = if (isFreezeActive) 0.4f else 1.0f
            val blockIter = blocks.iterator()
            while (blockIter.hasNext()) {
                val b = blockIter.next()
                b.y += b.speed * speedMult

                if (Random.nextFloat() < 0.5f) {
                    trailParticles.add(
                        TrailParticle(
                            b.x + Random.nextFloat() * b.width,
                            b.y + b.height,
                            (Random.nextFloat() - 0.5f) * 1.2f,
                            -Random.nextFloat() * 1.5f - 0.5f,
                            Random.nextFloat() * 4f + 2f,
                            b.colorStyle.border
                        )
                    )
                }

                if (b.y + b.height >= keyboardTopY) {
                    hp--
                    if (targetBlock == b) {
                        targetBlock = null
                    }
                    blockIter.remove()

                    if (hp <= 0) {
                        isGameRunning = false
                        break
                    }
                }
            }
        }
    }

    private fun spawnNewBlock(screenWidth: Float) {
        val rand = Random.nextFloat()
        val levelSpeedMult = when (currentLevel) {
            2 -> 1.35f
            3 -> 1.70f
            4 -> 2.10f
            else -> 1.0f
        }

        val (word, style) = when {
            rand < 0.05f -> Pair(if (score >= 300 && Random.nextFloat() < 0.4f) "VIDA+2" else "VIDA", specialLifeStyle)
            rand < 0.09f -> Pair("BOOM", specialBoomStyle)
            else -> Pair(wordBank[Random.nextInt(wordBank.size)], colorPalettes[Random.nextInt(colorPalettes.size)])
        }

        val w = max(140f, word.length * 40f + 50f)
        val h = 110f
        val x = Random.nextFloat() * (screenWidth - w - 40f) + 20f
        val speed = (difficulty.baseSpeed * levelSpeedMult + Random.nextFloat() * 0.8f) * 3f

        blocks.add(FallingBlock(word, x, -h, w, h, speed, style))
    }

    fun handleInputChar(char: Char): Boolean {
        if (!isGameRunning || isPaused) return false

        synchronized(lock) {
            val upper = char.uppercaseChar()

            if (targetBlock == null) {
                val matching = blocks.filter { it.word.startsWith(upper) }
                    .maxByOrNull { it.y }
                if (matching != null) {
                    targetBlock = matching
                    matching.typedCount = 1
                    checkBlockDetonation(matching)
                    return true
                }
            } else {
                val target = targetBlock!!
                if (target.typedCount < target.word.length && target.word[target.typedCount] == upper) {
                    target.typedCount++
                    checkBlockDetonation(target)
                    return true
                }
            }
            return false
        }
    }

    private fun checkBlockDetonation(block: FallingBlock) {
        if (block.typedCount >= block.word.length) {
            explodeBlock(block)
            blocks.remove(block)
            if (targetBlock == block) {
                targetBlock = null
            }
        }
    }

    fun explodeBlock(block: FallingBlock, triggerBoomClearAll: Boolean = true) {
        val cx = block.x + block.width / 2f
        val cy = block.y + block.height / 2f

        for (i in 0 until 35) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val spd = Random.nextFloat() * 15f + 4f
            sparkParticles.add(
                SparkParticle(
                    cx, cy,
                    cos(angle) * spd,
                    sin(angle) * spd,
                    Random.nextFloat() * 6f + 3f,
                    if (i % 2 == 0) block.colorStyle.border else Color.YELLOW
                )
            )
        }

        // Special Block Power Effects
        if (block.colorStyle.isSpecial == "life") {
            val heal = if (block.word == "VIDA+2") 2 else 1
            hp = maxHp.coerceAtMost(hp + heal)
            for (k in 0 until 12) {
                sparkParticles.add(SparkParticle(cx, cy, (Random.nextFloat() - 0.5f) * 8f, (Random.nextFloat() - 0.5f) * 8f, 5f, Color.parseColor("#52B788")))
            }
        } else if (block.colorStyle.isSpecial == "boom" && triggerBoomClearAll) {
            // BOOM block clears ALL blocks on screen!
            clearAllBlocksOnScreen()
        }

        score += 10
        val earned = (block.word.length * difficulty.coinMult).toInt()
        coins += max(1, earned)

        updateLevelProgression()
    }
}
