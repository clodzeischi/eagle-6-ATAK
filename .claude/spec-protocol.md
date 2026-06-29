# Protocol & Rendering Specification — Eagle-6

## Outbound events

Every mission state change fires two things in parallel:
1. A **COT event** to the TAK server.
2. A **chat message** to all configured TAK chat rooms.
3. A **local log entry** appended to the EUD's `.log` file.

The chat message text and the `__eagle-detail` message field are identical strings (the human-readable log line).

### COT schema

```xml
<event version="2.0"
       uid="EAGLE6-{missionId}-{eventSequence}"
       type="a-f-A-M-H-Q"
       time="{now ISO8601}"
       start="{now ISO8601}"
       stale="{now + 60min ISO8601}"
       how="h-g-i-g-o">
  <point lat="{lat}" lon="{lon}" hae="{hae}" ce="9999999" le="9999999"/>
  <detail>
    <__eagle-detail message="{human-readable log line}" />
  </detail>
</event>
```

**Key decisions:**
- Type `a-f-A-M-H-Q`: friendly air / military helicopter-class UAS (SIDC-compatible).
- Stale time: **always 60 minutes from send time**. This ensures map icons expire automatically if the EUD reboots mid-mission; do not allow icons to persist indefinitely.
- `<point>` location is event-specific (see table below).
- The `<detail>` child `<__eagle-detail>` carries the structured message for Eagle-6 consumers. Standard TAK clients without Eagle-6 see only the SIDC icon; Eagle-6 clients parse this detail for full graphics.
- Standard COT fields (location, altitude, time) remain in the header for potential Maven Smart System parsing downstream.

### COT location by event type

| Event | `<point>` location |
|---|---|
| Launch | Launch location |
| ON TASK | Activity location |
| Retask location | New activity location |
| RTH initiated | Current activity location |
| Landed | Recovery/landing location |

### Chat API

Use native ATAK chat API (intent-based). Details TBD when implementing chat — do not assume a specific intent action until confirmed against the SDK.

### Log file

Append each human-readable log line (with timestamp) to a `.log` file in the plugin's private storage, accessible via the Settings export action to sdcard.

---

## Inbound COT — received mission rendering

Eagle-6 listens for incoming COT events that contain a `<__eagle-detail>` child. When detected:

### For non-Eagle-6 participants
They receive the standard COT and render a normal friendly UAS SIDC icon with the 60-min stale time. No Eagle-6-specific rendering occurs on their device.

### For Eagle-6 participants

Parse the `<__eagle-detail>` message to extract mission context, then draw full mission graphics on the ATAK `MapView`:

#### Graphics elements

| Element | Shape | Style |
|---|---|---|
| Launch / recovery zone | Small circle (radius from Settings: 10–100 m) | Light blue, transparent fill, pulsating opacity |
| Activity area | Larger circle (radius from Settings: 100–1000 m) | Light blue, transparent fill, static |
| Route | Dashed polyline | Light blue |

**Route path:** launch → waypoint₁ → waypoint₂ → … → activity location → … → waypoint₂ → waypoint₁ → recovery/landing zone. (The route is drawn both outbound and return, forming a closed loop.)

#### Pulsating animation

The launch/recovery zone circle pulses using a sine-wave driven opacity:

```
opacity(t) = alpha_min + (alpha_max - alpha_min) * (sin(2π * t / period) + 1) / 2
```

- Target: 60 fps rendering loop.
- Implement via `ValueAnimator` (repeat `INFINITE`, `REVERSE`) driving the map overlay item's alpha — avoids a manual `Handler` loop and integrates with Android's `Choreographer`.
- The activity area circle does **not** pulse — it is static.

#### Stale / cleanup

Schedule removal of all rendered graphics when the COT stale time is reached (60 min from the event's `stale` attribute). If a subsequent COT event with the same `uid` arrives before stale, refresh/update the graphics and reset the cleanup timer.

#### Implementation notes

- All map overlays for a single mission should be grouped under a single `MapGroup` keyed by mission UID for easy bulk removal on stale/landing.
- The 60fps `ValueAnimator` is per-animation, not a global clock — let Android manage frame timing.
- Parse MGRS ↔ lat/lon using ATAK's built-in utilities (available in the SDK); do not bring in a third-party MGRS library.
