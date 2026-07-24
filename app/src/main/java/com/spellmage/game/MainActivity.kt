package com.spellmage.game

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val gameEngine = GameEngine()

    private lateinit var gameView: GameView
    private lateinit var hudBar: View
    private lateinit var mainMenuOverlay: View
    private lateinit var transparentKeyboard: TransparentKeyboardView
    private lateinit var tvHp: TextView
    private lateinit var tvCoins: TextView

    private lateinit var btnPlay: Button
    private lateinit var btnSettings: Button
    private lateinit var btnShop: Button
    private lateinit var btnPause: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameView = findViewById(R.id.game_view)
        hudBar = findViewById(R.id.hud_bar)
        mainMenuOverlay = findViewById(R.id.main_menu_overlay)
        transparentKeyboard = findViewById(R.id.transparent_keyboard)
        tvHp = findViewById(R.id.tv_hp)
        tvCoins = findViewById(R.id.tv_coins)

        btnPlay = findViewById(R.id.btn_play)
        btnSettings = findViewById(R.id.btn_settings)
        btnShop = findViewById(R.id.btn_shop)
        btnPause = findViewById(R.id.btn_pause)

        gameView.gameEngine = gameEngine

        // Keyboard Typing Callback
        transparentKeyboard.onKeyTypedListener = { char ->
            gameEngine.handleInputChar(char)
            updateHUD()
        }

        transparentKeyboard.onKeyActionListener = { action ->
            if (action == "CLR") {
                gameEngine.targetBlock = null
            } else if (action == "⌫") {
                gameEngine.targetBlock?.let {
                    if (it.typedCount > 0) it.typedCount--
                }
            }
            updateHUD()
        }

        gameView.onGameOverListener = {
            runOnUiThread {
                showGameOver()
            }
        }

        btnPlay.setOnClickListener {
            if (gameEngine.isPaused) {
                gameEngine.isPaused = false
                mainMenuOverlay.visibility = View.GONE
            } else {
                startGame()
            }
        }

        btnSettings.setOnClickListener {
            MossyWoodDialogs.showSettingsDialog(this, gameEngine) {
                updateHUD()
            }
        }

        btnShop.setOnClickListener {
            MossyWoodDialogs.showShopDialog(this, gameEngine) {
                updateHUD()
            }
        }

        btnPause.setOnClickListener {
            togglePause()
        }
    }

    private fun startGame() {
        gameEngine.resetGame()
        updateHUD()

        mainMenuOverlay.visibility = View.GONE
        hudBar.visibility = View.VISIBLE
        transparentKeyboard.visibility = View.VISIBLE
    }

    private fun togglePause() {
        gameEngine.isPaused = !gameEngine.isPaused
        if (gameEngine.isPaused) {
            mainMenuOverlay.visibility = View.VISIBLE
            btnPlay.text = "▶️ CONTINUAR"
        } else {
            mainMenuOverlay.visibility = View.GONE
        }
    }

    private fun showGameOver() {
        hudBar.visibility = View.GONE
        transparentKeyboard.visibility = View.GONE
        mainMenuOverlay.visibility = View.VISIBLE
        btnPlay.text = "🔄 REINTENTAR"
    }

    private fun updateHUD() {
        val hpHearts = StringBuilder()
        for (i in 0 until gameEngine.hp) {
            hpHearts.append("❤️ ")
        }
        tvHp.text = hpHearts.toString().trim()
        tvCoins.text = "${gameEngine.coins} 🪙"
    }
}
