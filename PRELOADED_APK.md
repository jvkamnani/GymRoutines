# Build GymRoutines APK With Preloaded Workout

This repo is configured to load a prepackaged Room database from:

`app/src/main/assets/preloaded/workout_routines_database`

## 1) Define your workout

Create a JSON file using this template:

`tools/workout.sample.json`

Top-level shape:

- `routines`: array
- each routine: `name`, `exercises`
- each exercise: `name`, optional `notes`, optional `alternatives`, optional `supersetTag`, optional `warmupSets`, optional `warmupReps`, optional `track`, `sets`
- `track`: booleans `reps`, `weight`, `time`, `distance`
- each set: optional numeric `reps`, `weight`, `time` (seconds), `distance`, optional `kind` (`normal`, `warm_up`, `drop`)

Notes on advanced fields:

- `alternatives`: shown in exercise notes for quick swap reference
- `supersetTag`: same tag across exercises means they are displayed as a superset pair/group
- `warmupSets`: inserts N warm-up rows before working sets
- `warmupReps`: optional default reps for inserted warm-up rows

## 2) Generate DB and build APK

```bash
./tools/build_preloaded_apk.sh /absolute/path/to/your_workout.json
```

This will:

1. Generate a Room-compatible sqlite DB from your JSON.
2. Place it in app assets.
3. Build debug APK.

Expected output path:

`app/build/outputs/apk/debug/app-debug.apk`

## Notes

- On first install, the app starts with your routine already present.
- If the app is already installed, uninstall first to force a clean first-run copy of the preloaded DB.
- You can still use the in-app Data Settings screen for backup/restore.
