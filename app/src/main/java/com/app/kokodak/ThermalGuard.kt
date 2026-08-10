package com.app.kokodak

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class ThermalGuard(private val context: Context) {

    /**
     * Проверява температурата на батерията в градуси Целзий.
     */
    fun getBatteryTemperature(): Float {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        return temp / 10.0f // Температурата се връща в десетични от градуса (напр. 420 = 42.0°C)
    }

    /**
     * Връща true, ако устройството е прегряло (над 45°C).
     */
    fun isOverheating(): Boolean {
        return getBatteryTemperature() > 45.0f
    }
}