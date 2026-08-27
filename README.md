# Fruit Ninja Bot 2.0

Android-only automation bot. It uses MediaProjection for screen capture and AccessibilityService for touch gestures. No ADB or computer is needed while playing.

## What is included

- Real-time fruit/bomb detection using lightweight on-device image processing.
- Configurable swipe speed before starting.
- Configurable bomb safety margin.
- Page rules that run before fruit slicing.
- Each page rule uses a reference screenshot and an action:
  - TAP: one coordinate.
  - SWIPE: start coordinate + end coordinate.
- Page rule cooldowns prevent repeated presses.
- Loop support: create rules for the end-of-round screen, reward/ad button, next-game button, and start-game button. When a page is recognized, the configured action is performed and the bot continues.
- GitHub Actions builds a ready-to-install debug APK.

## Build on GitHub

1. Upload the contents of this folder to your repository root. Do not upload the outer folder as an extra nesting level.
2. Open GitHub -> Actions -> `Build Fruit Ninja Bot APK`.
3. Run the workflow manually or push to `main`/`master`.
4. When it finishes, open the run and download the `FruitNinjaBot-APK` artifact.
5. The artifact contains `FruitNinjaBot.apk`.

## Phone setup

1. Install the APK.
2. Enable the app in Android Accessibility settings.
3. Open the app and set swipe speed and bomb safety.
4. Add page rules using screenshots of the game pages. The screenshot should be from the same phone/orientation/resolution when possible.
5. For TAP, touch the button once on the screenshot. For SWIPE, touch the start point then the end point.
6. Set a page-match threshold. Start around 0.86; lower it if the page has small visual changes, raise it if false matches happen.
7. Press START BOT and grant screen-capture permission.
8. Open Fruit Ninja. The bot handles page rules and fruit slicing in a continuous loop.

## Suggested rule order

- Game-over/result page -> TAP reward button.
- Reward/ad page -> TAP or SWIPE the required button.
- Continue/next page -> TAP Continue.
- Mode/start page -> SWIPE the start button if the game requires a swipe.
- Actual gameplay -> no page rule needed; the fruit/bomb detector takes over.

## Important limitations

The fruit detector is a lightweight heuristic, not a trained neural network. The page detector compares a compact signature of the reference screenshot to the live screen, so highly animated or substantially different screens may need a lower threshold or a cleaner reference screenshot. The bot intentionally favors safety around bombs over maximizing fruit count.
