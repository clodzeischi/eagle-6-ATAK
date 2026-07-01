package com.atakmap.android.eagle6.model

import android.content.Context

object MissionStore {

    private lateinit var db: MissionDatabase
    private val listeners = mutableListOf<() -> Unit>()

    fun init(context: Context) {
        db = MissionDatabase(context)
    }

    val activeMissions: List<Mission> get() = db.queryActive()
    val completedMissions: List<Mission> get() = db.queryCompleted()

    fun add(mission: Mission) { db.insert(mission); notifyListeners() }
    fun update(mission: Mission) { db.update(mission); notifyListeners() }
    fun remove(id: String) { db.delete(id); notifyListeners() }
    fun get(id: String): Mission? = db.queryById(id)
    fun clearCompleted() { db.deleteCompleted(); notifyListeners() }

    fun addListener(listener: () -> Unit) = listeners.add(listener)
    fun removeListener(listener: () -> Unit) = listeners.remove(listener)

    private fun notifyListeners() = listeners.forEach { it() }
}
