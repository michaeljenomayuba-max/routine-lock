package com.michael.routinelock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class LockScreenActivity : AppCompatActivity() {

    private val fmt = DateTimeFormatter.ofPattern("HH:mm")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    override fun onResume() {
        super.onResume()
        if (Store.isExitActive(this)) finish()
    }

    private fun render() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#12151D"))
        root.gravity = Gravity.CENTER
        root.setPadding(48, 48, 48, 48)

        val title = TextView(this)
        title.text = "Locked"
        title.setTextColor(Color.parseColor("#D8A24A"))
        title.textSize = 26f
        title.gravity = Gravity.CENTER

        val blocks = Store.getBlocks(this)
        val now = LocalTime.now()
        val active = blocks.find { b ->
            val start = LocalTime.parse(b.start, fmt)
            val end = LocalTime.parse(b.end, fmt)
            !now.isBefore(start) && now.isBefore(end)
        }

        val msg = TextView(this)
        msg.setTextColor(Color.parseColor("#F4E9D8"))
        msg.textSize = 15f
        msg.gravity = Gravity.CENTER
        msg.setPadding(0, 32, 0, 48)
        msg.text = if (active != null)
            "This app isn't on the allow-list for \"${active.label}\" (until ${active.end})."
        else
            "You're outside your usual routine window, held to routine."

        root.addView(title)
        root.addView(msg)

        val exitsUsed = Store.getExitsUsed(this)
        if (exitsUsed < Store.MAX_EXITS) {
            val exitLabel = TextView(this)
            exitLabel.setTextColor(Color.parseColor("#8A8FA0"))
            exitLabel.text = "Exits left today: ${Store.MAX_EXITS - exitsUsed}"
            exitLabel.gravity = Gravity.CENTER
            exitLabel.setPadding(0, 0, 0, 24)
            root.addView(exitLabel)

            listOf(15, 30, 60, 120, 180).forEach { minutes ->
                val btn = Button(this)
                btn.text = "Exit for ${minutes}m"
                btn.setOnClickListener { startExit(minutes) }
                root.addView(btn)
            }
        } else {
            val doneLabel = TextView(this)
            doneLabel.setTextColor(Color.parseColor("#8A8FA0"))
            doneLabel.text = "No exits left — held to routine until 11:30 PM"
            doneLabel.gravity = Gravity.CENTER
            root.addView(doneLabel)
        }

        val homeBtn = Button(this)
        homeBtn.text = "Go Home"
        homeBtn.setOnClickListener {
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            startActivity(homeIntent)
        }
        root.addView(homeBtn)

        setContentView(root)
    }

    private fun startExit(minutes: Int) {
        Store.startExit(this, minutes)
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ExitReceiver::class.java)
        intent.action = "END_EXIT"
        val pi = PendingIntent.getBroadcast(
            this, 1001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        finish()
    }

    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        // deliberately does nothing — no dismiss without using an exit
    }
}