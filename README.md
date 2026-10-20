# Delta Reflex Auto-Tapper — experimental starter

This is an unverified starter project, not a tested APK.

Includes a MediaProjection capture service, simple blue-arrow heuristic, AccessibilityService gesture tap, floating stop control, and GitHub Actions debug build workflow.

Important: Arrow recognition is a heuristic and may produce false positives. Coordinates are approximate based on the supplied screenshot and need device-specific calibration. The floating overlay currently offers Stop and Open App; restart capture from the main activity. Accessibility must be enabled manually, and screen capture requires user consent each session. Android 14/15 foreground-service requirements and build compatibility must be tested. This project has not been compiled here; resolve any build errors before installing.

To use: upload all files to the root of your GitHub repository on `main`. Open Actions and run Build Android APK (or push a commit). Download the artifact from a successful run, unzip it, and install `app-debug.apk`. GitHub Actions limits and artifact availability depend on account/repository settings.
