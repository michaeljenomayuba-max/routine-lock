package com.michael.routinelock

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView

class MainActivity : AppCompatActivity() {

    private lateinit var listContainer: LinearLayout
    private var currentTab = "routine"

    private var draftLabel = ""
    private var draftStart = "09:00"
    private var draftEnd = "10:00"
    private var draftLocked = true
    private var draftApps = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        renderBody()
    }

    private fun buildUi() {
        val page = LinearLayout(this)
        page.orientation = LinearLayout.VERTICAL
        page.setBackgroundColor(Color.parseColor("#12151D"))

        val header = TextView(this)
        header.text = "Routine Lock"
        header.setTextColor(Color.parseColor("#D8A24A"))
        header.textSize = 22f
        header.setPadding(32, 48, 32, 16)
        page.addView(header)

        val tabRow = LinearLayout(this)
        tabRow.orientation = LinearLayout.HORIZONTAL
        val routineTabBtn = Button(this)
        routineTabBtn.text = "Routine"
        routineTabBtn.setOnClickListener { currentTab = "routine"; renderBody() }
        val historyTabBtn = Button(this)
        historyTabBtn.text = "History"
        historyTabBtn.setOnClickListener { currentTab = "history"; renderBody() }
        tabRow.addView(routineTabBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        tabRow.addView(historyTabBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        page.addView(tabRow)

        val permBtn = Button(this)
        permBtn.text = "Enable Accessibility Permission"
        permBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        page.addView(permBtn)

        val rootScroll = NestedScrollView(this)
        listContainer = LinearLayout(this)
        listContainer.orientation = LinearLayout.VERTICAL
        listContainer.setPadding(24, 16, 24, 16)
        rootScroll.addView(listContainer)
        page.addView(rootScroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val addBtn = Button(this)
        addBtn.text = "+ Add block"
        addBtn.setOnClickListener { openAddBlockDialog() }
        page.addView(addBtn)

        setContentView(page)
        renderBody()
    }

    private fun renderBody() {
        listContainer.removeAllViews()
        if (currentTab == "routine") renderRoutine() else renderHistory()
    }

    private fun renderRoutine() {
        val blocks = Store.getBlocks(this)
        if (blocks.isEmpty()) {
            addInfoText("No blocks yet — tap + Add block below.")
            return
        }
        blocks.forEach { b ->
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(16, 16, 16, 16)

            val info = TextView(this)
            info.setTextColor(Color.parseColor("#F4E9D8"))
            val appsText = if (b.apps.isNotEmpty()) "\n${b.apps.joinToString(", ")}" else ""
            info.text = "${b.start} - ${b.end}  ${if (b.locked) "LOCKED" else "FREE"}\n${b.label}$appsText"

            val delBtn = Button(this)
            delBtn.text = "Delete"
            delBtn.setOnClickListener {
                Store.deleteBlock(this, b.id)
                renderBody()
            }

            row.addView(info, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(delBtn)
            listContainer.addView(row)
        }
    }

    private fun renderHistory() {
        val history = Store.getHistory(this)
        if (history.isEmpty()) {
            addInfoText("Nothing logged yet.")
            return
        }
        history.forEach { entry ->
            val t = TextView(this)
            t.text = entry
            t.setTextColor(Color.parseColor("#F4E9D8"))
            t.textSize = 12f
            t.setPadding(0, 8, 0, 8)
            listContainer.addView(t)
        }
    }

    private fun addInfoText(s: String) {
        val t = TextView(this)
        t.text = s
        t.setTextColor(Color.parseColor("#8A8FA0"))
        t.setPadding(0, 16, 0, 16)
        listContainer.addView(t)
    }

    private fun openAddBlockDialog() {
        draftLabel = ""
        draftStart = "09:00"
        draftEnd = "10:00"
        draftLocked = true
        draftApps = mutableListOf()

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Block")
            .setPositiveButton("Save") { _, _ -> saveBlock() }
            .setNegativeButton("Cancel", null)
            .create()

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(24, 24, 24, 24)

        val labelLabel = TextView(this)
        labelLabel.text = "Label"
        labelLabel.setTextColor(Color.parseColor("#D8A24A"))
        container.addView(labelLabel)
        val labelInput = EditText(this)
        labelInput.setText(draftLabel)
        labelInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { draftLabel = s.toString() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        container.addView(labelInput)

        val startLabel = TextView(this)
        startLabel.text = "Start Time"
        startLabel.setTextColor(Color.parseColor("#D8A24A"))
        startLabel.setPadding(0, 16, 0, 0)
        container.addView(startLabel)
        val startBtn = Button(this)
        startBtn.text = draftStart
        startBtn.setOnClickListener { pickTime { t -> draftStart = t; startBtn.text = t } }
        container.addView(startBtn)

        val endLabel = TextView(this)
        endLabel.text = "End Time"
        endLabel.setTextColor(Color.parseColor("#D8A24A"))
        endLabel.setPadding(0, 16, 0, 0)
        container.addView(endLabel)
        val endBtn = Button(this)
        endBtn.text = draftEnd
        endBtn.setOnClickListener { pickTime { t -> draftEnd = t; endBtn.text = t } }
        container.addView(endBtn)

        val lockedLabel = TextView(this)
        lockedLabel.text = "Mode"
        lockedLabel.setTextColor(Color.parseColor("#D8A24A"))
        lockedLabel.setPadding(0, 16, 0, 0)
        container.addView(lockedLabel)
        val lockedCheck = CheckBox(this)
        lockedCheck.text = "Locked (require allow-list)"
        lockedCheck.isChecked = draftLocked
        lockedCheck.setOnCheckedChangeListener { _, b -> draftLocked = b }
        container.addView(lockedCheck)

        val appLabel = TextView(this)
        appLabel.text = "Allowed Apps"
        appLabel.setTextColor(Color.parseColor("#D8A24A"))
        appLabel.setPadding(0, 16, 0, 0)
        container.addView(appLabel)
        val appBtn = Button(this)
        appBtn.text = "Select Apps (${draftApps.size})"
        appBtn.setOnClickListener { selectApps() }
        container.addView(appBtn)

        val scroll = ScrollView(this)
        scroll.addView(container)
        dialog.setView(scroll)
        dialog.show()
    }

    private fun pickTime(cb: (String) -> Unit) {
        val (h, m) = draftStart.split(":").let { Pair(it[0].toInt(), it[1].toInt()) }
        TimePickerDialog(this, { _, hh, mm ->
            val s = String.format("%02d:%02d", hh, mm)
            cb(s)
        }, h, m, true).show()
    }

    private fun selectApps() {
        val pm = packageManager
        val apps = pm.getInstalledPackages(0).map { it.packageName }.sorted()
        val checked = draftApps.toBooleanArray()
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Apps")
            .setMultiChoiceItems(
                apps.toTypedArray(), checked
            ) { _, idx, isChecked ->
                if (isChecked) draftApps.add(apps[idx])
                else draftApps.remove(apps[idx])
            }
            .setPositiveButton("OK", null)
            .create()
        dialog.show()
    }

    private fun saveBlock() {
        if (draftLabel.isBlank() || draftStart >= draftEnd) {
            Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show()
            return
        }
        val b = Block(
            System.currentTimeMillis(),
            draftStart,
            draftEnd,
            draftLabel,
            draftLocked,
            draftApps
        )
        Store.addBlock(this, b)
        renderBody()
    }
}