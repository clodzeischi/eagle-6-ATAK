# Protocol & Rendering Specification — Eagle-6

## Outbound events

Every mission event fires a **chat message** to all configured TAK chat rooms. The same string is recorded in the mission's SQLite row (via timestamps — the message text is derived from stored data, not stored separately).

> **COT schema is deferred.** COT output is pending coordination with Maven Smart System. Do not implement COT until the schema is confirmed. The inbound rendering spec at the bottom of this file remains a design target for when COT is ready.

There is no local `.log` file. SQLite is the single source of truth for mission history.

---

## Chat message formats

All timestamps are UTC in `HH:mm` format followed by `Z`.

### Planned
Fires when operator confirms a new mission.

```
{HH:mm}Z: {pilot} planned {platform} {missionType} at {activityLocation}, launching at {launchTime}Z.
```
Example:
```
14:30Z: PHOENIX-26 planned Skydio SURVEY at 14J QP 44191 54429, launching at 06:00Z.
```

### Launching
Fires when the 1-minute tick crosses `launchTime` while the plugin is open.

```
{HH:mm}Z: {pilot} launching {platform} {missionType} at {activityLocation}.
```
Example:
```
06:00Z: PHOENIX-26 launching Skydio SURVEY at 14J QP 44191 54429.
```

### Complete
Fires when the 1-minute tick crosses `launchTime + duration` while the plugin is open. Also fires immediately when an edit causes `launchTime + duration` to fall in the past (the "changed" message is suppressed — only "complete" is sent).

```
{HH:mm}Z: {pilot} {platform} {missionType} complete.
```
Example:
```
06:23Z: PHOENIX-26 Skydio SURVEY complete.
```

### Cancelled
Fires when the operator taps `CANCEL MISSION` in Mission Edit. Mission is deleted from SQLite immediately after sending — no history entry is kept.

```
{HH:mm}Z: {pilot} {launchTime}Z {missionType} cancelled.
```
Example:
```
14:35Z: PHOENIX-26 06:00Z SURVEY cancelled.
```

### Changed
Fires when the operator saves edits to an existing mission. Sends full mission context regardless of how many fields changed. Not sent if the edit results in `launchTime + duration` being in the past (send "complete" instead).

```
{HH:mm}Z: {pilot} changed {platform} {missionType} to {activityLocation}, launching at {launchTime}Z.
```
Example:
```
14:40Z: PHOENIX-26 changed DJI Air3 SURVEY to 14J QP 44191 54400, launching at 05:30Z.
```

---

## Message delivery rules

Messages are sent synchronously on their trigger event — there is no background scheduler. Time-based messages (launching, complete) require the plugin to be running. If the plugin is closed, those messages do not fire.

To prevent duplicate firing across tick intervals, use `launched_at` and `completed_at` timestamp fields in SQLite as sent-flags. If the field is non-null, the message has already fired — skip it on subsequent ticks.

---

## Inbound COT — received mission rendering

> **Deferred pending COT schema finalization with Maven Smart System.**

The following is a design target, not yet implemented.

### Graphics elements

| Element | Shape | Style |
|---|---|---|
| Launch / recovery zone | Small circle (radius from Settings: 10–100 m) | Light blue, transparent fill, pulsating opacity |
| Activity area | Larger circle (radius from Settings: 100–1000 m) | Light blue, transparent fill, static |
| Infil route | Dashed polyline | Light blue |
| Exfil route | Dashed polyline | Light blue |

Route path: launch → infilWaypoints → activity → exfilWaypoints → recovery.

### Pulsating animation

The launch/recovery zone circle pulses using a sine-wave driven opacity:

```
opacity(t) = alpha_min + (alpha_max - alpha_min) * (sin(2π * t / period) + 1) / 2
```

- Implement via `ValueAnimator` (repeat `INFINITE`, `REVERSE`) driving the map overlay item's alpha.
- The activity area circle does not pulse — it is static.

### Cleanup

Map graphics are cleared when the mission reaches `launchTime + duration` (the "complete" event).

All overlays for a single mission grouped under a single `MapGroup` keyed by mission UUID for bulk removal.

### Non-Eagle-6 participants

They receive the standard COT and render a normal friendly UAS SIDC icon. No Eagle-6-specific rendering occurs on their device.
