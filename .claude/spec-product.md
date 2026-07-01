# Product Specification — Eagle-6

## Core concept

Eagle-6 is a UAS mission planner and activity broadcaster. Operators plan missions in advance (pilot, platform, type, route, timing) and broadcast intent to all TAK network participants. The tool is fire-and-forget: once a mission is confirmed, status is computed from the clock. No manual state transitions are required.

The operator is responsible for having the plugin open at key mission moments (launch, completion) for those messages to transmit. If the plugin is closed, only the "planned" message has gone out — no launch, no complete, nothing else. That is the operator's responsibility, not the tool's.

Example log sequence:
```
14:30Z: PHOENIX-26 planned Skydio SURVEY at 14J QP 44191 54429, launching at 06:00Z.
06:00Z: PHOENIX-26 launching Skydio SURVEY at 14J QP 44191 54429.
06:23Z: PHOENIX-26 Skydio SURVEY complete.
```

With a cancellation:
```
14:30Z: PHOENIX-26 planned Skydio SURVEY at 14J QP 44191 54429, launching at 06:00Z.
14:35Z: PHOENIX-26 06:00Z SURVEY cancelled.
```

With a change:
```
14:40Z: PHOENIX-26 changed DJI Air3 SURVEY to 14J QP 44191 54400, launching at 05:30Z.
```

---

## Mission status (computed, not stored)

Status is derived at display time from the current clock relative to `launchTime` and `launchTime + duration`. No status field is persisted. The UI refreshes on a 1-minute tick.

| Condition | Card label |
|---|---|
| `now < launchTime − 60 min` | `launching at HH:mm` |
| `launchTime − 60 min ≤ now < launchTime` | `launching in X min` |
| `launchTime ≤ now < launchTime + duration − 10 min` | `active` |
| `launchTime + duration − 10 min ≤ now < launchTime + duration` | `returning in X min` |
| `now ≥ launchTime + duration` | `complete` — mission fires "complete" message and moves to history |

The "returning in X min" countdown is a display label only — it does not trigger a message.

---

## Message triggers

| Event | Trigger | Plugin must be open? |
|---|---|---|
| Planned | Operator confirms New Mission | No — immediate on user action |
| Launching | Clock reaches `launchTime` | Yes |
| Complete | Clock reaches `launchTime + duration` | Yes |
| Cancelled | Operator cancels via Mission Edit | No — immediate on user action |
| Changed | Operator saves edits to existing mission | No — immediate on user action |

**Special case:** if an edit produces a `launchTime + duration` already in the past, send "complete" immediately and move the mission to history — do not send "changed."

Time-based triggers (launching, complete) are checked on the 1-minute UI tick. Fire once per threshold crossing; store a flag in SQLite to prevent re-firing.

---

## Data model

### Mission fields

| Field | Type | Notes |
|---|---|---|
| id | String (UUID) | Primary key |
| Pilot callsign | String | From settings pilot list |
| UAS platform | String | From settings platform list |
| Mission type | String | From settings mission type list |
| Launch time | Timestamp (UTC) | Absolute; no default, must be set |
| Launch location | MGRS String | Map picker or EUD position at confirm time |
| Infil waypoints | List\<MGRS String\> | Optional; ordered; launch → activity |
| Activity location | MGRS String | Map picker |
| Exfil waypoints | List\<MGRS String\> | Optional; ordered; activity → recovery |
| Recovery location | MGRS String | Map picker or EUD position at confirm time |
| Altitude | String (feet AGL) | From settings altitude list |
| Expected duration | Int (minutes) | No default |
| confirmed\_at | Timestamp | When "planned" message was sent |
| launched\_at | Timestamp? | When "launching" message was sent; null if not yet fired |
| completed\_at | Timestamp? | When "complete" message was sent; null if not yet fired |

Launch and recovery locations are snapshotted at confirm time from the current EUD position if not explicitly picked.

### Persistence

All missions (active and completed) are stored in **SQLite** on the EUD. Plugin restart does not clear missions. Waypoint lists stored as delimited strings in the mission row.

Cancelled missions are **deleted immediately** from SQLite when the operator cancels — they do not appear in history.

### Form persistence

On every mission confirm, persist last-used dropdown index selections to `SharedPreferences`. Pre-populate New Mission form on next open (not Mission List — only when opening the form).

### Settings (SharedPreferences)

| Setting | Type | Constraints | Default |
|---|---|---|---|
| Pilot list | List\<String\> | Cannot remove own callsign | [EUD callsign] |
| Platform list | List\<String\> | Cannot be empty | ["GENERIC-UAS"] |
| Mission type list | List\<String\> | Cannot be empty | ["SEARCH"] |
| Altitude list | List\<String\> ft AGL | Cannot be empty; no negatives | ["400"] |
| Launch/recovery zone radius | Int (meters) | 10–100 m | 50 |
| Activity zone radius | Int (meters) | 100–1000 m | 300 |
| TAK chat rooms | List\<String\> | Multi-select | [] |

---

## Screens

### Mission List (root / entry view)

Opened when the plugin toolbar button is tapped.

- **Header:** `NEW MISSION` button (→ New Mission) and `SETTINGS` button (→ ATAK plugin settings).
- **Active section:** Scrollable list of active mission cards sorted by launch time ascending. Empty state: `"no active missions"`.
- **Card content (active):** pilot callsign, platform, mission type, computed status label.
- **History section:** Completed missions below a visual separator, sorted by `completed_at` descending (most recent first). Each card shows pilot, platform, mission type, and completion time. A **`CLEAR COMPLETE`** button at the top of this section deletes all completed missions from SQLite. Hidden entirely if there are no completed missions.
- **Tap active card** → Mission Edit screen.
- **Tap completed card** → Mission History Detail screen (read-only).
- **1-minute tick:** recomputes status labels and fires time-based messages (launching, complete) for any mission that has crossed a threshold since the last tick.

---

### New Mission

A form. First open: dropdowns default to index 0. Subsequent opens: last-used saved index.

Fields (in order):
1. **Pilot** — dropdown from settings pilot list.
2. **Platform** — dropdown from settings platform list.
3. **Mission type** — dropdown from settings mission type list.
4. **Launch time** — date + time picker (UTC). No default.
5. **Launch location** — MGRS text display + `[Pick from map]` + `[Reset to self]`. Defaults to current EUD position.
6. **Infil waypoints** (optional) — `[Add waypoint]` button. Each uses map picker. Ordered; add/remove individually.
7. **Activity location** — MGRS text display + map picker buttons.
8. **Exfil waypoints** (optional) — `[Add waypoint]` button. Waypoints from activity back to recovery. Ordered; add/remove individually.
9. **Recovery location** — MGRS text display + `[Pick from map]` + `[Reset to self]`. Defaults to current EUD position.
10. **Altitude** — dropdown from settings altitude list.
11. **Expected duration** — integer input (minutes). No default.
12. **`CONFIRM`** button — validates all fields, assigns UUID, saves to SQLite, fires "planned" message, returns to Mission List.
13. **`CANCEL`** button — discards form, returns to Mission List. No message sent. Nothing written to SQLite.

#### Map picker UX
- Activating the picker: plugin pane background grays out to signal "tap the map."
- User taps map → coordinate captured, converted to MGRS, displayed in field, icon drawn at that location.
- Icon semantics: launch/recovery = down-arrow marker; activity = circle overlay; waypoint = small dot.
- `[Reset to self]` reverts to current EUD position.

---

### Mission Edit

Accessible by tapping an active mission card on the Mission List.

Displays all mission fields in editable form, pre-populated with current values. Layout mirrors New Mission.

Actions:
- **`SAVE`** — validates, saves to SQLite, fires "changed" message. Exception: if updated `launchTime + duration` is already in the past, fires "complete" instead and moves mission to history.
- **`CANCEL MISSION`** — fires "cancelled" message, deletes mission from SQLite immediately (no history entry), returns to Mission List.
- **`← BACK`** — discards unsaved edits, returns to Mission List. No message.

Any field can be edited at any time, including while the mission is active. There is no distinction between pre-launch and in-flight editing.

---

### Mission History Detail

Accessible by tapping a completed mission card on the Mission List.

Read-only view. Displays all mission fields (same layout as Mission Edit but non-editable). Draws mission graphics locally on the ATAK map: launch zone, activity zone, route (infil + exfil). Graphics are local only — no COT is re-sent.

- **`← BACK`** — clears local graphics, returns to Mission List.

---

### Settings

Opened via ATAK's native settings framework (intent → plugin's registered `PreferenceFragment` submenu).

1. **Pilots** — add/remove list. Cannot remove own EUD callsign.
2. **Platforms** — add/remove list. Cannot be empty.
3. **Mission types** — add/remove list. Cannot be empty.
4. **Altitudes** — add/remove list (feet AGL). Cannot be empty. No value < 0.
5. **Launch/recovery zone radius** — integer input (meters), range 10–100.
6. **Activity zone radius** — integer input (meters), range 100–1000.
7. **TAK chat rooms** — multi-select list of available rooms.
