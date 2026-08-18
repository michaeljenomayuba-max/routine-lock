package com.michael.routinelock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class LockAccessibilityService : AccessibilityService() {

    private val fmt = DateTimeFormatter.ofPattern("HH:mm")

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg.startsWith("com.android.systemui")) return

        Store.maybeDailyReset(this)

        if (shouldBlock(pkg)) {
            val intent = Intent(this, LockScreenActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("blockedPackage", pkg)
            startActivity(intent)
        }
    }

    private fun shouldBlock(pkg: String): Boolean {
        if (Store.isExitActive(this)) return false

        val now = LocalTime.now()
        val blocks = Store.getBlocks(this)
        val active = blocks.find { b ->
            val start = LocalTime.parse(b.start, fmt)
            val end = LocalTime.parse(b.end, fmt)
            !now.isBefore(start) && now.isBefore(end)
        }

        if (Store.isDayLocked(this)) {
            return active == null || !active.apps.contains(pkg)
        }

        if (active == null || !active.locked) return false
        return !active.apps.contains(pkg)
    }

    override fun onInterrupt() {}
}