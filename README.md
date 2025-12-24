# OrionStargazer

OrionStargazer is an Android-first augmented reality stargazing experience built for modern devices. The app fuses mobile sensors, astronomy math, catalog data, and Sceneform rendering to help hobbyist astronomers locate and learn about stars, constellations, and bright solar system objects right in the real sky.

## Key Capabilities
1. **Live AR sky overlay** driven by ARCore + Sceneform:
   - Globe of thousands of cataloged stars, color-coded by spectral class.
   - Glow sprites, solid spheres, and optional AGSL shader modes to match device capability.
   - Custom shader option controlled from Settings (glow vs solid vs shader).
2. **Constellation intelligence**:
   - JSON catalog of constellations with animated “draw-in” line segments.
   - Detection algorithm identifies the constellation you’re pointing at and highlights it; you can also switch to “Nearby” mode for an atlas-like view or “Hybrid” for both.
   - Polled data is cached (Alt/Az) for performance, plus segment pooling avoids GC spikes.
3. **Educational overlays**:
   - Reticle overlay picks the nearest visible star.
   - Selected star card surfaces curated facts (magnitude, distance, RA/Dec, spectral type, constellation notes).
   - Highlights dialog summarizes top visible stars and their key stats.
4. **Settings + UX**:
   - Main Menu launcher (Stargazing Mode / Instructions / Settings).
   - Visible Stars bottom sheet with magnitude cutoff, constellation toggle, and quick actions (Main Menu + Highlights).
   - Pinch-to-adjust sensitivity quickly changes the magnitude limit on-screen.
   - Overlay compass + SkyStatusBar keep orientation, FPS, shader caps, and permissions visible at all times.
   - Optional X/Y overlay toggle (shows subtle on-screen sliders for pitch/altitude + heading/azimuth to verify sensor response).
5. **Calibration challenge (Polaris)**:
   - A guided “sanity check” you can start from Settings to verify sensors + reticle behavior.
   - Shows live target Az/Alt for Polaris with on-screen guidance arrows; challenge completes when Polaris is centered.
6. **Permissions + resilience**:
   - Separate camera/location flows with an optional location-only prompt.
   - ARCore availability is probed, and navigation to app settings is provided when needed.

## Technical Overview
- **Languages & frameworks**: Kotlin, Jetpack Compose UI, Sceneform for AR, Room for the star catalog, DataStore for preferences, ARCore for camera/sensor fusion, AGSL runtime shaders for the custom glow overlay.
- **Packages**:
  - `domain/astronomy`: astronomy math (Julian Date, GST, LST, RA/Dec → Alt/Az), detection helpers, planet calculator, heuristic facts.
  - `domain/engine`: `SkyPipeline` orchestrates candidate filtering, fast cone queries, constellation detection, and segment preparation.
  - `data`: Room entities/DAOs, repository, asset seeders, datastore settings.
  - `ui`: Compose screens, overlays, bottom sheet, shader brush, highlights, compass/status bar.
  - `ar`: Sceneform renderer, constellation renderer, shader overlay, session management.
  - `sensors`: Orientation/Location providers abstract SensorManager/LocationManager.
- **Performance features**:
  - Equatorial vector index + cone matcher for star visibility; only surviving stars go through trig calculations.
  - Cached Alt/Az data used across detection & segment builders with hysteresis to reduce flicker.
  - Node pooling in `ConstellationRenderer` to reuse cylinders and avoid GC churn.

## Device Requirements
- **Minimum Android SDK**: 26 (Android 8.0), target 36.
- **Hardware**: ARCore-capable device (camera + motion sensors). Custom shader mode additionally requires RuntimeShader support (API 33+ and GL ES 3.1+).
- **Sensors**: Camera, accelerometer + magnetometer (for orientation), GPS/location for improved alignment, optional gyroscope.
- **Permissions**: CAMERA is required for AR; location permission enhances astronomical accuracy.

## Development Notes
- Manual unit tests cover core CoordinateConverter math, star visibility filtering, and planet generation (`PlanetCalculator` + `StarPositionCalculator` tests).
- Compose-based UI affords quick iteration: bottom sheet content, highlights dialog, settings screen, overlay components, and pinch detection.
- Build features include AGSL shader fallback, magnitude slider persistence, shader performance cap slider, highlights menu, navigate-to-app-settings assistance, plus offline asset seeding (Room + custom CSV import).

## Usage Tips
- Slide the bottom panel up/down or pinch the AR view to tune sensitivity without leaving the main screen.
- Tap a constellation highlight to lock and read curated facts.
- Use Settings to toggle shader modes or pin “Hybrid” detection for both the highlighted constellation and nearby context.
- When the bottom sheet is fully collapsed, drag the subtle handle area at the screen bottom to bring it back up.
- Calibration challenge: open Settings → “Start challenge” and follow the target Az/Alt + arrows until Polaris is centered; “End” returns to Settings.
- If you place the phone flat on a table so the camera points toward the ground (the reticle is looking down), the app is still calculating which stars lie in that direction, so you’re effectively looking at the sky that sits opposite your current location on Earth; rotate the device to point toward the horizon or zenith to see the stars that are actually in front of you.

