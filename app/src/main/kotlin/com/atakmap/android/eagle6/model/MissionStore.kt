package com.atakmap.android.eagle6.model

object MissionStore {

    private val _missions = mutableListOf<Mission>()
    private val listeners = mutableListOf<() -> Unit>()

    val missions: List<Mission> get() = _missions.toList()

    fun add(mission: Mission) {
        _missions.add(mission)
        notifyListeners()
    }

    fun update(mission: Mission) {
        val idx = _missions.indexOfFirst { it.id == mission.id }
        if (idx >= 0) {
            _missions[idx] = mission
            notifyListeners()
        }
    }

    fun remove(id: String) {
        _missions.removeAll { it.id == id }
        notifyListeners()
    }

    fun get(id: String): Mission? = _missions.find { it.id == id }

    fun addListener(listener: () -> Unit) = listeners.add(listener)
    fun removeListener(listener: () -> Unit) = listeners.remove(listener)

    private fun notifyListeners() = listeners.forEach { it() }
}
