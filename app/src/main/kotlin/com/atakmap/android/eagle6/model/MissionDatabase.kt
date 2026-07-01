package com.atakmap.android.eagle6.model

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.atakmap.coremap.maps.coords.GeoPoint

class MissionDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "eagle6.db"
        private const val DB_VERSION = 1
        private const val TABLE = "missions"

        private const val COL_ID = "id"
        private const val COL_PILOT = "pilot"
        private const val COL_PLATFORM = "platform"
        private const val COL_MISSION_TYPE = "mission_type"
        private const val COL_LAUNCH_TIME = "launch_time_ms"
        private const val COL_LAUNCH_LAT = "launch_lat"
        private const val COL_LAUNCH_LON = "launch_lon"
        private const val COL_INFIL_WP = "infil_waypoints"
        private const val COL_ACTIVITY_LAT = "activity_lat"
        private const val COL_ACTIVITY_LON = "activity_lon"
        private const val COL_EXFIL_WP = "exfil_waypoints"
        private const val COL_RECOVERY_LAT = "recovery_lat"
        private const val COL_RECOVERY_LON = "recovery_lon"
        private const val COL_ALTITUDE = "altitude_ft"
        private const val COL_DURATION = "duration_min"
        private const val COL_CONFIRMED_AT = "confirmed_at"
        private const val COL_LAUNCHED_AT = "launched_at"
        private const val COL_COMPLETED_AT = "completed_at"

        private const val WP_SEP = "|"
        private const val COORD_SEP = ","
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE (
                $COL_ID TEXT PRIMARY KEY,
                $COL_PILOT TEXT NOT NULL,
                $COL_PLATFORM TEXT NOT NULL,
                $COL_MISSION_TYPE TEXT NOT NULL,
                $COL_LAUNCH_TIME INTEGER NOT NULL,
                $COL_LAUNCH_LAT REAL NOT NULL,
                $COL_LAUNCH_LON REAL NOT NULL,
                $COL_INFIL_WP TEXT NOT NULL DEFAULT '',
                $COL_ACTIVITY_LAT REAL NOT NULL,
                $COL_ACTIVITY_LON REAL NOT NULL,
                $COL_EXFIL_WP TEXT NOT NULL DEFAULT '',
                $COL_RECOVERY_LAT REAL NOT NULL,
                $COL_RECOVERY_LON REAL NOT NULL,
                $COL_ALTITUDE TEXT NOT NULL,
                $COL_DURATION INTEGER NOT NULL,
                $COL_CONFIRMED_AT INTEGER NOT NULL,
                $COL_LAUNCHED_AT INTEGER,
                $COL_COMPLETED_AT INTEGER
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    fun insert(mission: Mission) {
        writableDatabase.insertOrThrow(TABLE, null, mission.toValues())
    }

    fun update(mission: Mission) {
        writableDatabase.update(TABLE, mission.toValues(), "$COL_ID = ?", arrayOf(mission.id))
    }

    fun delete(id: String) {
        writableDatabase.delete(TABLE, "$COL_ID = ?", arrayOf(id))
    }

    fun deleteCompleted() {
        writableDatabase.delete(TABLE, "$COL_COMPLETED_AT IS NOT NULL", null)
    }

    fun queryActive(): List<Mission> =
        query("$COL_COMPLETED_AT IS NULL", "$COL_LAUNCH_TIME ASC")

    fun queryCompleted(): List<Mission> =
        query("$COL_COMPLETED_AT IS NOT NULL", "$COL_COMPLETED_AT DESC")

    fun queryById(id: String): Mission? {
        val cursor = readableDatabase.query(TABLE, null, "$COL_ID = ?", arrayOf(id), null, null, null)
        return cursor.use { if (it.moveToFirst()) it.toMission() else null }
    }

    private fun query(selection: String, orderBy: String): List<Mission> {
        val cursor = readableDatabase.query(TABLE, null, selection, null, null, null, orderBy)
        return cursor.use { c ->
            val list = mutableListOf<Mission>()
            while (c.moveToNext()) list.add(c.toMission())
            list
        }
    }

    private fun Mission.toValues(): ContentValues = ContentValues().apply {
        put(COL_ID, id)
        put(COL_PILOT, pilot)
        put(COL_PLATFORM, platform)
        put(COL_MISSION_TYPE, missionType)
        put(COL_LAUNCH_TIME, launchTimeMs)
        put(COL_LAUNCH_LAT, launchLocation.latitude)
        put(COL_LAUNCH_LON, launchLocation.longitude)
        put(COL_INFIL_WP, infilWaypoints.serialize())
        put(COL_ACTIVITY_LAT, activityLocation.latitude)
        put(COL_ACTIVITY_LON, activityLocation.longitude)
        put(COL_EXFIL_WP, exfilWaypoints.serialize())
        put(COL_RECOVERY_LAT, recoveryLocation.latitude)
        put(COL_RECOVERY_LON, recoveryLocation.longitude)
        put(COL_ALTITUDE, altitudeFt)
        put(COL_DURATION, expectedDurationMin)
        put(COL_CONFIRMED_AT, confirmedAt)
        if (launchedAt != null) put(COL_LAUNCHED_AT, launchedAt) else putNull(COL_LAUNCHED_AT)
        if (completedAt != null) put(COL_COMPLETED_AT, completedAt) else putNull(COL_COMPLETED_AT)
    }

    private fun Cursor.toMission(): Mission {
        fun longOrNull(col: String): Long? {
            val idx = getColumnIndexOrThrow(col)
            return if (isNull(idx)) null else getLong(idx)
        }
        return Mission(
            id = getString(getColumnIndexOrThrow(COL_ID)),
            pilot = getString(getColumnIndexOrThrow(COL_PILOT)),
            platform = getString(getColumnIndexOrThrow(COL_PLATFORM)),
            missionType = getString(getColumnIndexOrThrow(COL_MISSION_TYPE)),
            launchTimeMs = getLong(getColumnIndexOrThrow(COL_LAUNCH_TIME)),
            launchLocation = GeoPoint(
                getDouble(getColumnIndexOrThrow(COL_LAUNCH_LAT)),
                getDouble(getColumnIndexOrThrow(COL_LAUNCH_LON))
            ),
            infilWaypoints = getString(getColumnIndexOrThrow(COL_INFIL_WP)).deserialize(),
            activityLocation = GeoPoint(
                getDouble(getColumnIndexOrThrow(COL_ACTIVITY_LAT)),
                getDouble(getColumnIndexOrThrow(COL_ACTIVITY_LON))
            ),
            exfilWaypoints = getString(getColumnIndexOrThrow(COL_EXFIL_WP)).deserialize(),
            recoveryLocation = GeoPoint(
                getDouble(getColumnIndexOrThrow(COL_RECOVERY_LAT)),
                getDouble(getColumnIndexOrThrow(COL_RECOVERY_LON))
            ),
            altitudeFt = getString(getColumnIndexOrThrow(COL_ALTITUDE)),
            expectedDurationMin = getInt(getColumnIndexOrThrow(COL_DURATION)),
            confirmedAt = getLong(getColumnIndexOrThrow(COL_CONFIRMED_AT)),
            launchedAt = longOrNull(COL_LAUNCHED_AT),
            completedAt = longOrNull(COL_COMPLETED_AT)
        )
    }

    private fun List<GeoPoint>.serialize(): String =
        if (isEmpty()) ""
        else joinToString(WP_SEP) { "${it.latitude}$COORD_SEP${it.longitude}" }

    private fun String.deserialize(): List<GeoPoint> {
        if (isBlank()) return emptyList()
        return split(WP_SEP).mapNotNull { entry ->
            val parts = entry.split(COORD_SEP)
            if (parts.size == 2) {
                val lat = parts[0].toDoubleOrNull() ?: return@mapNotNull null
                val lon = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                GeoPoint(lat, lon)
            } else null
        }
    }
}
