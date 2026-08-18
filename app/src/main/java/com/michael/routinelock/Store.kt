package com.michael.routinelock

import android.content.Context
import org.json.JSONArray
import java.time.LocalDate
import java.time.LocalTime

object Store {
    private const val PREFS = "routine_lock_prefs"
    private const val KEY_BLOCKS = "blocks"
    private const val KEY_EXITS_USED = "exits_used"
    private const val KEY_EXIT_ACTIVE = "exit_active"
    private const val KEY_DAY_LOCKED = "day_locked"
    private const val KEY_LAST_RESET_DATE = "last_reset_date"
    private const val KEY_HISTORY = "history_log"

    const val MAX_EXITS = 3
    const val RESET_HOUR = 23
    const val RESET_MINUTE = 30

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getBlocks(ctx: Context): List<Block> {
        val raw = prefs(ctx).getString(KEY_BLOCKS, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<Block>()
        for (i in 0 until arr.length()) list.add(Block.fromJson(arr.getJSONObject(i)))
        return list.sortedBy { it.start }
    }

    fun saveBlocks(ctx: Context, blocks: List<Block>) {
        val arr = JSONArray()
        blocks.forEach { arr.put(it.toJson()) }
        prefs(ctx).edit().putString(KEY_BLOCKS, arr.toString()).apply()
    }

    fun addBlock(ctx: Context, block: Block) {
        val list = getBlocks(ctx).toMutableList()
        list.add(block)
        saveBlocks(ctx, list)
    }

    fun deleteBlock(ctx: Context, id: Long) {
        saveBlocks(ctx, getBlocks(ctx).filter { it.id != id })
    }

    fun getExitsUsed(ctx: Context): Int = prefs(ctx).getInt(KEY_EXITS_USED, 0)

    fun isExitActive(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_EXIT_ACTIVE, false)

    fun isDayLocked(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_DAY_LOCKED, false)

    fun startExit(ctx: Context, durationMinutes: Int) {
        val used = getExitsUsed(ctx) + 1
        val editor = prefs(ctx).edit()
        editor.putInt(KEY_EXITS_USED, used)
        editor.putBoolean(KEY_EXIT_ACTIVE, true)
        if (used >= MAX_EXITS) editor.putBoolean(KEY_DAY_LOCKED, true)
        editor.apply()
        addHistory(ctx, "Exit used ($used/$MAX_EXITS) — ${durationMinutes}m granted")
        if (used >= MAX_EXITS) addHistory(ctx, "All exits spent — held to routine until 11:30 PM")
    }

    fun endExit(ctx: Context) {
        prefs(ctx).edit().putBoolean(KEY_EXIT_ACTIVE, false).apply()
        addHistory(ctx, "Exit ended — back to routine")
    }

    fun addHistory(ctx: Context, entry: String) {
        val now = LocalTime.now().toString().let { if (it.length >= 5) it.substring(0, 5) else it }
        val list = getHistory(ctx).toMutableList()
        list.add(0, "$now — $entry")
        val arr = JSONArray()
        list.take(200).forEach { arr.put(it) }
        prefs(ctx).edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    fun getHistory(ctx: Context): List<String> {
        val raw = prefs(ctx).getString(KEY_HISTORY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) list.add(arr.getString(i))
        return list
    }

    fun maybeDailyReset(ctx: Context) {
        val today = LocalDate.now().toString()
        val lastReset = prefs(ctx).getString(KEY_LAST_RESET_DATE, "")
        val now = LocalTime.now()
        val pastResetTime = now.hour > RESET_HOUR || (now.hour == RESET_HOUR && now.minute >= RESET_MINUTE)
        if (pastResetTime && lastReset != today) {
            prefs(ctx).edit()
                .putInt(KEY_EXITS_USED, 0)
                .putBoolean(KEY_EXIT_ACTIVE, false)
                .putBoolean(KEY_DAY_LOCKED, false)
                .putString(KEY_LAST_RESET_DATE, today)
                .apply()
            addHistory(ctx, "Daily reset — full access, exits reset to $MAX_EXITS")
        }
    }
}