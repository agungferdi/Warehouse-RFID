package com.warehouse.rfid.edge

import android.content.Context

/**
 * RFID power per activity type, set once from the Settings screen instead of live on the scan
 * screen. Tag Registration and Record Activity keep independent values since they typically need
 * very different ranges (short-range single-tag registration vs. long-range bulk sweeps).
 */
object PowerSettings {
    private const val PREFS_NAME = "power_settings"
    private const val KEY_REGISTRATION = "power_registration"
    private const val KEY_RECORD_ACTIVITY = "power_record_activity"

    private const val DEFAULT_REGISTRATION = 5
    private const val DEFAULT_RECORD_ACTIVITY = 30

    fun getPower(context: Context, activityType: ActivityType): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = if (activityType == ActivityType.INBOUND) KEY_REGISTRATION else KEY_RECORD_ACTIVITY
        val default = if (activityType == ActivityType.INBOUND) DEFAULT_REGISTRATION else DEFAULT_RECORD_ACTIVITY
        return prefs.getInt(key, default).coerceIn(POWER_DB_MIN, POWER_DB_MAX)
    }

    fun getRegistrationPower(context: Context): Int = getPower(context, ActivityType.INBOUND)

    fun getRecordActivityPower(context: Context): Int = getPower(context, ActivityType.STOCK_OPNAME)

    fun setRegistrationPower(context: Context, value: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_REGISTRATION, value.coerceIn(POWER_DB_MIN, POWER_DB_MAX))
            .apply()
    }

    fun setRecordActivityPower(context: Context, value: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_RECORD_ACTIVITY, value.coerceIn(POWER_DB_MIN, POWER_DB_MAX))
            .apply()
    }
}
