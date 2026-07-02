package com.atakmap.android.eagle6.settings

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.preference.Preference
import android.widget.EditText
import android.widget.Toast
import com.atakmap.android.eagle6.chat.ChatRoomManager
import com.atakmap.android.eagle6.model.Eagle6Prefs
import com.atakmap.android.gui.PanEditTextPreference
import com.atakmap.android.gui.PanSwitchPreference
import com.atakmap.android.plugintemplate.plugin.R
import com.atakmap.android.preference.PluginPreferenceFragment

class Eagle6PreferenceFragment : PluginPreferenceFragment {

    companion object {
        // Held only to give PluginPreferenceFragment a context for XML inflation on fragment recreation.
        private var staticCtx: Context? = null
        const val PREF_KEY = "eagle6Preference"
    }

    constructor() : super(staticCtx, R.xml.eagle6_preferences)

    @SuppressLint("ValidFragment")
    constructor(ctx: Context) : super(ctx, R.xml.eagle6_preferences) {
        staticCtx = ctx
    }

    private fun ps(id: Int): String = staticCtx!!.getString(id)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (findPreference("pref_use_zulu_time") as? PanSwitchPreference)?.apply {
            isChecked = Eagle6Prefs.useZuluTime
            summary = if (Eagle6Prefs.useZuluTime) ps(R.string.pref_use_zulu_time_summary_on)
                      else ps(R.string.pref_use_zulu_time_summary_off)
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { pref, newVal ->
                val zulu = newVal as Boolean
                Eagle6Prefs.useZuluTime = zulu
                pref.summary = if (zulu) ps(R.string.pref_use_zulu_time_summary_on)
                               else ps(R.string.pref_use_zulu_time_summary_off)
                true
            }
        }

        setupListPref(
            key = "pref_pilots",
            title = ps(R.string.pref_pilots_title),
            getList = { Eagle6Prefs.pilots },
            saveList = { updated ->
                if (updated.none { it == Eagle6Prefs.selfCallsign }) {
                    Toast.makeText(activity, ps(R.string.pref_error_cannot_remove_callsign), Toast.LENGTH_SHORT).show()
                    false
                } else {
                    Eagle6Prefs.pilots = updated
                    true
                }
            }
        )

        setupListPref(
            key = "pref_platforms",
            title = ps(R.string.pref_platforms_title),
            getList = { Eagle6Prefs.platforms },
            saveList = { updated ->
                if (updated.isEmpty()) {
                    Toast.makeText(activity, ps(R.string.pref_error_platforms_empty), Toast.LENGTH_SHORT).show()
                    false
                } else {
                    Eagle6Prefs.platforms = updated
                    true
                }
            }
        )

        setupListPref(
            key = "pref_mission_types",
            title = ps(R.string.pref_mission_types_title),
            getList = { Eagle6Prefs.missionTypes },
            saveList = { updated ->
                if (updated.isEmpty()) {
                    Toast.makeText(activity, ps(R.string.pref_error_mission_types_empty), Toast.LENGTH_SHORT).show()
                    false
                } else {
                    Eagle6Prefs.missionTypes = updated
                    true
                }
            }
        )

        setupListPref(
            key = "pref_altitudes",
            title = ps(R.string.pref_altitudes_title),
            getList = { Eagle6Prefs.altitudes },
            saveList = { updated ->
                if (updated.isEmpty()) {
                    Toast.makeText(activity, ps(R.string.pref_error_altitudes_empty), Toast.LENGTH_SHORT).show()
                    false
                } else if (updated.any { it.toIntOrNull()?.let { v -> v < 0 } == true }) {
                    Toast.makeText(activity, ps(R.string.pref_error_altitudes_negative), Toast.LENGTH_SHORT).show()
                    false
                } else {
                    Eagle6Prefs.altitudes = updated
                    true
                }
            }
        )

        (findPreference("pref_chat_room_name") as? PanEditTextPreference)?.apply {
            text = Eagle6Prefs.chatRoomName
            summary = Eagle6Prefs.chatRoomName
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { pref, newVal ->
                val name = newVal.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(activity, ps(R.string.pref_error_chat_room_name_empty), Toast.LENGTH_SHORT).show()
                    false
                } else {
                    Eagle6Prefs.chatRoomName = name
                    ChatRoomManager.ensureRoom(name)
                    pref.summary = name
                    true
                }
            }
        }

        (findPreference("pref_launch_radius") as? PanEditTextPreference)?.apply {
            text = Eagle6Prefs.launchZoneRadiusM.toString()
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newVal ->
                val v = newVal.toString().toIntOrNull()
                if (v == null || v !in 10..100) {
                    Toast.makeText(activity, ps(R.string.pref_error_launch_radius_range), Toast.LENGTH_SHORT).show()
                    false
                } else {
                    Eagle6Prefs.launchZoneRadiusM = v
                    true
                }
            }
        }

        (findPreference("pref_activity_radius") as? PanEditTextPreference)?.apply {
            text = Eagle6Prefs.activityZoneRadiusM.toString()
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newVal ->
                val v = newVal.toString().toIntOrNull()
                if (v == null || v !in 100..1000) {
                    Toast.makeText(activity, ps(R.string.pref_error_activity_radius_range), Toast.LENGTH_SHORT).show()
                    false
                } else {
                    Eagle6Prefs.activityZoneRadiusM = v
                    true
                }
            }
        }
    }

    private fun setupListPref(
        key: String,
        title: String,
        getList: () -> List<String>,
        saveList: (List<String>) -> Boolean
    ) {
        val pref = findPreference(key) ?: return
        pref.summary = getList().joinToString(", ")
        pref.setOnPreferenceClickListener {
            showListEditor(title, getList(), saveList) { newList ->
                pref.summary = newList.joinToString(", ")
            }
            true
        }
    }

    private fun showListEditor(
        title: String,
        current: List<String>,
        save: (List<String>) -> Boolean,
        onSaved: (List<String>) -> Unit
    ) {
        val mutable = current.toMutableList()
        val ctx = activity ?: return

        val display = mutable.joinToString("\n• ", prefix = "• ")
        AlertDialog.Builder(ctx)
            .setTitle(title)
            .setMessage(display)
            .setPositiveButton(ps(R.string.pref_list_btn_save)) { _, _ ->
                if (save(mutable)) onSaved(mutable)
            }
            .setNeutralButton(ps(R.string.pref_list_btn_add)) { _, _ ->
                val input = EditText(ctx)
                AlertDialog.Builder(ctx)
                    .setTitle(ps(R.string.pref_list_dialog_add_item))
                    .setView(input)
                    .setPositiveButton(ps(R.string.pref_list_btn_add)) { _, _ ->
                        val v = input.text.toString().trim()
                        when {
                            v.isEmpty() -> {}
                            v.contains("||") -> Toast.makeText(ctx, ps(R.string.pref_error_value_contains_pipe), Toast.LENGTH_SHORT).show()
                            else -> {
                                mutable.add(v)
                                showListEditor(title, mutable, save, onSaved)
                            }
                        }
                    }
                    .setNegativeButton(ps(R.string.pref_list_btn_cancel), null)
                    .show()
            }
            .setNegativeButton(ps(R.string.pref_list_btn_remove)) { _, _ ->
                val arr = mutable.toTypedArray()
                AlertDialog.Builder(ctx)
                    .setTitle(ps(R.string.pref_list_dialog_remove_item))
                    .setItems(arr) { _, idx ->
                        mutable.removeAt(idx)
                        showListEditor(title, mutable, save, onSaved)
                    }
                    .setNegativeButton(ps(R.string.pref_list_btn_cancel), null)
                    .show()
            }
            .show()
    }

    override fun getSubTitle(): String = getSubTitle(ps(R.string.pref_breadcrumb_tools), ps(R.string.pref_breadcrumb_eagle6))
}
