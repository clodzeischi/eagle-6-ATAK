package com.atakmap.android.eagle6.settings

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.preference.Preference
import android.widget.EditText
import android.widget.Toast
import com.atakmap.android.eagle6.model.Eagle6Settings
import com.atakmap.android.plugintemplate.plugin.R
import com.atakmap.android.preference.PluginPreferenceFragment

class Eagle6PreferenceFragment : PluginPreferenceFragment {

    companion object {
        private var staticPluginContext: Context? = null
        const val PREF_KEY = "eagle6Preference"
    }

    constructor() : super(staticPluginContext, R.xml.eagle6_preferences)

    @SuppressLint("ValidFragment")
    constructor(pluginContext: Context) : super(pluginContext, R.xml.eagle6_preferences) {
        staticPluginContext = pluginContext
    }

    private lateinit var settings: Eagle6Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = staticPluginContext ?: return
        settings = Eagle6Settings(ctx)

        setupListPref(
            key = "pref_pilots",
            title = "Pilots",
            getList = { settings.pilots },
            saveList = { updated ->
                val self = settings.selfCallsign
                if (updated.none { it == self }) {
                    Toast.makeText(activity, "Cannot remove your own callsign.", Toast.LENGTH_SHORT).show()
                    false
                } else {
                    settings.pilots = updated
                    true
                }
            }
        )

        setupListPref(
            key = "pref_platforms",
            title = "Platforms",
            getList = { settings.platforms },
            saveList = { updated ->
                if (updated.isEmpty()) {
                    Toast.makeText(activity, "Platforms list cannot be empty.", Toast.LENGTH_SHORT).show()
                    false
                } else {
                    settings.platforms = updated
                    true
                }
            }
        )

        setupListPref(
            key = "pref_mission_types",
            title = "Mission Types",
            getList = { settings.missionTypes },
            saveList = { updated ->
                if (updated.isEmpty()) {
                    Toast.makeText(activity, "Mission types list cannot be empty.", Toast.LENGTH_SHORT).show()
                    false
                } else {
                    settings.missionTypes = updated
                    true
                }
            }
        )

        setupListPref(
            key = "pref_altitudes",
            title = "Altitudes (ft AGL)",
            getList = { settings.altitudes },
            saveList = { updated ->
                if (updated.isEmpty()) {
                    Toast.makeText(activity, "Altitude list cannot be empty.", Toast.LENGTH_SHORT).show()
                    false
                } else if (updated.any { it.toIntOrNull()?.let { v -> v < 0 } == true }) {
                    Toast.makeText(activity, "Altitudes cannot be negative.", Toast.LENGTH_SHORT).show()
                    false
                } else {
                    settings.altitudes = updated
                    true
                }
            }
        )

        findPreference("pref_launch_radius")
            ?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newVal ->
            val v = newVal.toString().toIntOrNull()
            if (v == null || v !in 10..100) {
                Toast.makeText(activity, "Launch zone radius must be 10–100 m.", Toast.LENGTH_SHORT).show()
                false
            } else {
                settings.launchZoneRadiusM = v; true
            }
        }

        findPreference("pref_activity_radius")
            ?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newVal ->
            val v = newVal.toString().toIntOrNull()
            if (v == null || v !in 100..1000) {
                Toast.makeText(activity, "Activity zone radius must be 100–1000 m.", Toast.LENGTH_SHORT).show()
                false
            } else {
                settings.activityZoneRadiusM = v; true
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

        fun rebuild(dialog: AlertDialog) {
            // Rebuild items list display – using simple string join in a TextView
            // We refresh the dialog message rather than rebuilding the whole dialog
            dialog.setMessage(mutable.joinToString("\n• ", prefix = "• "))
        }

        val display = mutable.joinToString("\n• ", prefix = "• ")
        AlertDialog.Builder(ctx)
            .setTitle(title)
            .setMessage(display)
            .setPositiveButton("Save") { _, _ ->
                if (save(mutable)) onSaved(mutable)
            }
            .setNeutralButton("Add") { _, _ ->
                val input = EditText(ctx)
                AlertDialog.Builder(ctx)
                    .setTitle("Add item")
                    .setView(input)
                    .setPositiveButton("Add") { _, _ ->
                        val v = input.text.toString().trim()
                        if (v.isNotEmpty()) {
                            mutable.add(v)
                            showListEditor(title, mutable, save, onSaved)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Remove") { _, _ ->
                val arr = mutable.toTypedArray()
                AlertDialog.Builder(ctx)
                    .setTitle("Remove item")
                    .setItems(arr) { _, idx ->
                        mutable.removeAt(idx)
                        showListEditor(title, mutable, save, onSaved)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .show()
    }

    override fun getSubTitle(): String = getSubTitle("Tool Preferences", "Eagle-6 Preferences")
}
