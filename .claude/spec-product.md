# Product Specification — Eagle-6

## Core concept

Eagle-6 is a UAS flight logger. Every state change in a mission fires a COT event and a TAK chat message that together form an activity log visible to all TAK participants. Operators can see each other's intent and deconflict airspace in real time.

Example log sequence:
```
14:41Z: PHOENIX-26 launched Skydio from 14J QP 44183 55438 to conduct SURVEY at 14J QP 44191 54429 from 400' AGL.
15:01Z: PHOENIX-26 is ON-TASK.
15:03Z: PHOENIX-26 retasked to RECON.
15:07Z: PHOENIX-26 retasked to 14J 44196 54431, 300' AGL.
15:12Z: PHOENIX-26 landed Skydio at 14J QP 44183 55438. SURVEY complete.
```

## Mission status lifecycle

```
(new) → LAUNCHING → ON TASK → RTH → LANDED
```

All transitions are manual (user-driven). After LANDED, the mission is removed from the active list. Missions are in-memory only — a plugin restart clears them. COT stale time is 60 min max, so map icons expire automatically even if the EUD reboots mid-mission.

## Data model

### Mission fields
| Field | Type | Source | Notes |
|---|---|---|---|
| Pilot callsign | String | Dropdown → Settings pilot list | |
| UAS platform | String | Dropdown → Settings platform list | |
| Mission type | String | Dropdown → Settings mission type list | |
| Launch location | MGRS String | Map picker / self-location | |
| Waypoints | List\<MGRS String\> | Map picker | Optional; ordered |
| Activity location | MGRS String | Map picker / self-location | |
| Altitude | String (feet AGL) | Dropdown → Settings altitude list | |
| Expected duration | Int (minutes) | Input field | Default 60 |
| Status | Enum | State machine | LAUNCHING / ON_TASK / RTH / LANDED |

### Form persistence
On every launch, persist last-used dropdown selections to `SharedPreferences`. Pre-populate the New Mission form with these on next open (not the Mission List — only when opening New Mission).

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

## Screens

### Mission List (root / entry view)

Opened when the plugin toolbar button is tapped.

- **Header:** `NEW MISSION` button (→ New Mission screen) and `SETTINGS` button (→ ATAK plugin settings).
- **Body:** Scrollable list of active mission cards. Empty state: `"no active missions"`.
- **Card content:** operator callsign, UAS platform, mission type, mission status.
- **Tap card** → Mission Detail screen.

---

### New Mission

A form. First open: all dropdowns default to index 0 from their respective settings list. Subsequent opens: last-used saved value.

Fields (in order):
1. **Pilot** — dropdown from settings pilot list.
2. **Platform** — dropdown from settings platform list.
3. **Mission type** — dropdown from settings mission type list.
4. **Launch location** — MGRS text display + `[Pick from map]` + `[Reset to self]` buttons. See *Map picker UX* below.
5. **Waypoints** (optional) — `[Add waypoint]` button. Each waypoint uses map picker. Multiple allowed, ordered. Add/remove individually. Each drawn on map.
6. **Activity location** — MGRS text display + map picker buttons.
7. **Altitude** — dropdown from settings altitude list.
8. **Expected duration** — integer input (minutes), default 60.
9. **`LAUNCH`** button — validates form, creates mission in LAUNCHING state, fires COT + chat, navigates to Mission Detail.

#### Map picker UX (reused across New Mission, Mission Detail, Landing)
- Activating the picker: plugin pane background grays out to signal "tap the map."
- User taps map → coordinate is captured, converted to MGRS, displayed in the field, and an appropriate icon is drawn at that location.
- Icon semantics: launch/recovery = down-arrow marker; activity = circle overlay; waypoint = small dot.
- `[Reset to self]` clears any picked location and reverts to current EUD position.

---

### Mission Detail

Accessible from: New Mission (after launch) or Mission List card tap.

Displays: pilot, platform, mission type, current activity location, current status.

Status-dependent actions:

| Current status | Available actions |
|---|---|
| LAUNCHING | **ON TASK** button → fires `15:01Z: PHOENIX-26 is ON-TASK.` |
| ON_TASK | **Retask type** (dropdown, fires retask message), **Retask location** (map picker, fires relocation message), **RTH** button |
| RTH | **LAND** button → fires landing message, removes mission from list |

**Retask type message:** `15:03Z: PHOENIX-26 retasked to SURVEY.`
**Retask location message:** `15:07Z: PHOENIX-26 retasked to 14J 44196 54431, 300' AGL.`
**Landing message:** `15:12Z: PHOENIX-26 landed Skydio at 14J QP 44183 55438. SURVEY complete.`

---

### Landing Screen

Intermediate screen reached when the user taps **RTH** from Mission Detail.

Purpose: let the operator designate and navigate to the recovery zone before marking landed.

- MGRS text display for recovery location, defaulting to current EUD location.
- `[Pick from map]` activates map picker; tapping the map draws a **down-arrow icon** at the chosen location to signal intended landing point.
- `[Reset to self]` reverts to EUD location.
- Optional waypoints (same add/remove mechanic as New Mission) — operator may need a non-straight RTH route.
- **`LAND`** button — transitions mission to LANDED, fires landing COT + chat message, removes mission from active list, returns to Mission List.

---

### Settings

Opened via ATAK's native settings framework (intent → plugin's registered `PreferenceFragment` submenu).

1. **Pilots** — add/remove list. Cannot remove own EUD callsign.
2. **Platforms** — add/remove list. Cannot be empty.
3. **Altitudes** — add/remove list (feet AGL). Cannot be empty. No value < 0.
4. **Mission types** — add/remove list. Cannot be empty.
5. **Launch/recovery zone radius** — integer input (meters), range 10–100.
6. **Activity zone radius** — integer input (meters), range 100–1000.
7. **TAK chat rooms** — multi-select list of available rooms to publish messages to.
8. **Export logs** — copies local `.log` file to OS-accessible location (e.g. sdcard). Shows confirmation toast on success.
9. **Erase logs** — shows confirmation dialog; user must hold confirm button for 5 seconds before deletion.
