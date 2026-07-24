package com.spellmage.game

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class MossyWoodDialogs {

    companion object {

        fun showSettingsDialog(
            context: Context,
            engine: GameEngine,
            onSettingsUpdated: () -> Unit
        ) {
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.dialog_mossy_settings, null)
            dialog.setContentView(view)

            val btnLento = view.findViewById<Button>(R.id.btn_diff_lento)
            val btnMedio = view.findViewById<Button>(R.id.btn_diff_medio)
            val btnRapido = view.findViewById<Button>(R.id.btn_diff_rapido)

            val btnBg1 = view.findViewById<Button>(R.id.btn_bg_1)
            val btnBg2 = view.findViewById<Button>(R.id.btn_bg_2)
            val btnBg3 = view.findViewById<Button>(R.id.btn_bg_3)
            val btnBg4 = view.findViewById<Button>(R.id.btn_bg_4)

            fun updateUI() {
                btnLento.setBackgroundColor(if (engine.difficulty == Difficulty.LENTO) Color.parseColor("#4D8B28") else Color.parseColor("#231208"))
                btnMedio.setBackgroundColor(if (engine.difficulty == Difficulty.MEDIO) Color.parseColor("#4D8B28") else Color.parseColor("#231208"))
                btnRapido.setBackgroundColor(if (engine.difficulty == Difficulty.RAPIDO) Color.parseColor("#4D8B28") else Color.parseColor("#231208"))

                btnBg1.setBackgroundColor(if (engine.selectedBackgroundRes == R.drawable.bg_mago_estelar) Color.parseColor("#4D8B28") else Color.parseColor("#231208"))
                btnBg2.setBackgroundColor(if (engine.selectedBackgroundRes == R.drawable.bg_mago_vortice) Color.parseColor("#4D8B28") else Color.parseColor("#231208"))
                btnBg3.setBackgroundColor(if (engine.selectedBackgroundRes == R.drawable.bg_mago_varita) Color.parseColor("#4D8B28") else Color.parseColor("#231208"))
                btnBg4.setBackgroundColor(if (engine.selectedBackgroundRes == R.drawable.bg_mago_galactico) Color.parseColor("#4D8B28") else Color.parseColor("#231208"))
            }
            updateUI()

            btnLento.setOnClickListener {
                engine.difficulty = Difficulty.LENTO
                updateUI()
            }
            btnMedio.setOnClickListener {
                engine.difficulty = Difficulty.MEDIO
                updateUI()
            }
            btnRapido.setOnClickListener {
                engine.difficulty = Difficulty.RAPIDO
                updateUI()
            }

            btnBg1.setOnClickListener {
                engine.selectedBackgroundRes = R.drawable.bg_mago_estelar
                updateUI()
            }
            btnBg2.setOnClickListener {
                engine.selectedBackgroundRes = R.drawable.bg_mago_vortice
                updateUI()
            }
            btnBg3.setOnClickListener {
                engine.selectedBackgroundRes = R.drawable.bg_mago_varita
                updateUI()
            }
            btnBg4.setOnClickListener {
                engine.selectedBackgroundRes = R.drawable.bg_mago_galactico
                updateUI()
            }

            view.findViewById<Button>(R.id.btn_close_settings).setOnClickListener {
                onSettingsUpdated()
                dialog.dismiss()
            }

            dialog.show()
        }

        fun showShopDialog(
            context: Context,
            engine: GameEngine,
            onCoinsUpdated: () -> Unit
        ) {
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.dialog_mossy_shop, null)
            dialog.setContentView(view)

            val tvCoins = view.findViewById<TextView>(R.id.tv_shop_coins)
            tvCoins.text = "${engine.coins} 🪙"

            view.findViewById<Button>(R.id.btn_buy_max_hp).setOnClickListener {
                if (engine.coins < 200) {
                    Toast.makeText(context, "❌ ¡Monedas insuficientes (se requieren 200)!", Toast.LENGTH_SHORT).show()
                } else {
                    engine.coins -= 200
                    engine.maxHp++
                    engine.hp = Math.min(engine.hp + 1, engine.maxHp)
                    tvCoins.text = "${engine.coins} 🪙"
                    onCoinsUpdated()
                    Toast.makeText(context, "💖 ¡Salud máxima aumentada a ${engine.maxHp} HP!", Toast.LENGTH_SHORT).show()
                }
            }

            view.findViewById<Button>(R.id.btn_buy_heal).setOnClickListener {
                if (engine.coins < 30) {
                    Toast.makeText(context, "❌ ¡Monedas insuficientes (se requieren 30)!", Toast.LENGTH_SHORT).show()
                } else {
                    engine.coins -= 30
                    engine.countHealPotions++
                    tvCoins.text = "${engine.coins} 🪙"
                    onCoinsUpdated()
                    Toast.makeText(context, "❤️ ¡Poción de salud agregada al inventario!", Toast.LENGTH_SHORT).show()
                }
            }

            view.findViewById<Button>(R.id.btn_buy_freeze).setOnClickListener {
                if (engine.coins < 10) {
                    Toast.makeText(context, "❌ ¡Monedas insuficientes!", Toast.LENGTH_SHORT).show()
                } else {
                    engine.coins -= 10
                    engine.countFreezePotions++
                    tvCoins.text = "${engine.coins} 🪙"
                    onCoinsUpdated()
                    Toast.makeText(context, "❄️ ¡Poción de hielo guardada!", Toast.LENGTH_SHORT).show()
                }
            }

            view.findViewById<Button>(R.id.btn_buy_nuke).setOnClickListener {
                if (engine.coins < 20) {
                    Toast.makeText(context, "❌ ¡Monedas insuficientes!", Toast.LENGTH_SHORT).show()
                } else {
                    engine.coins -= 20
                    engine.countNukePotions++
                    tvCoins.text = "${engine.coins} 🪙"
                    onCoinsUpdated()
                    Toast.makeText(context, "💥 ¡Bomba Arcana guardada!", Toast.LENGTH_SHORT).show()
                }
            }

            view.findViewById<Button>(R.id.btn_close_shop).setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }
}
