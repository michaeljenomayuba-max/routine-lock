package com.michael.routinelock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ExitReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "END_EXIT" -> Store.endExit(context)
            "DAILY_RESET" -> Store.maybeDailyReset(context)
        }
    }
}