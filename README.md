# HorseCalc 🏇🪙

**HorseCalc** is an intelligent Android companion application for physical horse racing board games (such as *Across the Board*, *Wooden Horse Racing*, *Derby Day*, and *Pony Pegs*).

It automates the tedious end-of-race chore: instantly tallying the coin pot using **on-device Computer Vision**, calculating exact 4-card share valuations, solving the physical coin distribution among winning players, and applying the official "extra nickel" rollover rule.

---

## Key Features

- **On-Device Computer Vision Intake**: Snap a top-down photo of the coin pot or choose from your gallery. Runs entirely on-device (no cloud upload or latency) using radial gradient circular Hough voting and 1D K-Means radius clustering.
- **Interactive Verification & Overlays**: Visual color-coded detection rings projected directly onto the photo:
  - 🟡 **Gold Ring**: Quarters ($0.25)
  - 🔵 **Cyan Ring**: Dimes ($0.10)
  - 🟠 **Amber Ring**: Nickels ($0.05)
- **Zero-Crop Full-Resolution Capture**: Direct full-sensor camera capture via AndroidX [`FileProvider`](file:///Users/Ed.Sponsler/Developer/horsecalc/app/src/main/res/xml/file_paths.xml) and aspect-ratio-preserving preview with full-screen zoom inspection.
- **Instant Manual Tally Adjustments**: Interactive stepper rows allow instant correction if coins are stacked or partially hidden.
- **Fair Coin Partitioning Engine**: A constraint satisfaction backtracking solver partitions physical quarters, dimes, and nickels among winners proportionally, preventing one winner from receiving all heavy nickels while another gets all quarters.
- **"Extra Nickel" Rollover Engine**: Automatically calculates when pots cannot be evenly split into four 5-cent increments, reserving the physical 5¢, 10¢, or 15¢ remainder for the next race.
- **Turf & Derby Aesthetic**: Built with Jetpack Compose Material 3 featuring Turf Green, Derby Gold, and Dark Slate styling.

---

## Technical Stack & Architecture

- **Platform**: Android 8.0 (API 26) through Android 15 (API 35)
- **Language**: Kotlin 2.0+ (Coroutines, StateFlow)
- **UI Toolkit**: Jetpack Compose with Material 3
- **Architecture**: Clean Architecture + MVI (Model-View-Intent)
- **Vision Engine**: [`CvCoinDetector`](file:///Users/Ed.Sponsler/Developer/horsecalc/app/src/main/java/com/horsecalc/app/vision/CvCoinDetector.kt) (EXIF orientation normalization, bare-surface rejection, Sobel edge direction gradients, circular Hough voting, 1.35x spatial NMS, 1D K-Means clustering)
- **Image Loading**: Coil Compose
- **Camera Integration**: AndroidX `FileProvider` + `ActivityResultContracts.TakePicture`

For in-depth mathematical specifications and component diagrams, see [`ARCHITECTURE.md`](file:///Users/Ed.Sponsler/Developer/horsecalc/ARCHITECTURE.md).

---

## Prerequisites

- **Host Machine**: macOS, Linux, or Windows with:
  - **Android SDK** (API 35 platform & platform-tools `adb`)
  - **JDK 17 or JDK 21** (e.g. `/opt/homebrew/opt/openjdk@21` on macOS)
  - **Android Studio Ladybug / Koala** or newer (optional for IDE usage)
- **Target Device**: Google Pixel (Pixel 6, 7, 7 Pro, 8, 8 Pro, 9, etc.) or any physical Android device running Android 8.0+.

---

## Google Pixel Setup Guide (Developer Mode & Permissions)

To install and run **HorseCalc** on your physical Google Pixel phone, follow these steps to enable USB debugging and configure permissions:

### Step 1: Enable Developer Options on Pixel
1. Open the **Settings** app on your Pixel.
2. Scroll to the bottom and tap **About phone**.
3. Scroll all the way down to find **Build number**.
4. Tap **Build number** rapidly **7 times**.
5. When prompted, enter your device PIN or pattern.
6. A toast notification will appear: *"You are now a developer!"*

### Step 2: Enable USB Debugging
1. Go back to **Settings** > **System**.
2. Tap **Developer options** (or **Developer settings**).
3. Scroll down to the **Debugging** section.
4. Toggle **USB debugging** to **ON**.
5. *(Optional)* Toggle **Stay awake** to **ON** to prevent the phone from sleeping while plugged in.

### Step 3: Connect Pixel to Computer & Authorize ADB
1. Connect your Pixel to your computer using a USB-C cable.
2. Unlock your phone screen. An alert dialog will pop up:
   > **Allow USB debugging?**
   > *The computer's RSA key fingerprint is: ...*
3. Check the box: ☑ **Always allow from this computer**.
4. Tap **Allow**.
5. On your computer terminal, verify the connection:
   ```bash
   adb devices
   ```
   You should see your device listed as `device` (not `unauthorized`):
   ```text
   List of devices attached
   29221FDH3006N0    device
   ```

### Step 4: Configure App Permissions
- **Camera Permission (`android.permission.CAMERA`)**:
  - The first time you tap **Take Photo** in the app, Android will present a permission dialog. Select **"While using the app"**.
  - Alternatively, you can grant it ahead of time via ADB:
    ```bash
    adb shell pm grant com.horsecalc.app android.permission.CAMERA
    ```
  - To check or toggle manually on your phone: Go to **Settings** > **Apps** > **HorseCalc** > **Permissions** > **Camera** > Select **Allow only while using the app**.
- **Photo / Gallery Access**:
  - HorseCalc uses Android's modern system **Photo Picker** (`ActivityResultContracts.GetContent()`), which is sandboxed and requires zero broad storage permissions.

---

## Building and Installing

### Option 1: Fast Command-Line Install (Recommended)

1. Clone or navigate to the repository directory:
   ```bash
   cd /Users/Ed.Sponsler/Developer/horsecalc
   ```

2. Compile the debug APK using the Gradle wrapper:
   ```bash
   JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew assembleDebug
   ```

3. Install the APK directly onto your connected Pixel phone:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

4. Launch the application:
   ```bash
   adb shell am start -n com.horsecalc.app/.MainActivity
   ```

### Option 2: Using Android Studio

1. Launch **Android Studio**.
2. Select **File** > **Open...** and select the `/Users/Ed.Sponsler/Developer/horsecalc` folder.
3. Allow Gradle to sync.
4. In the top toolbar device selector, choose your connected Google Pixel phone.
5. Click the green **Run** button (or press `Shift + F10`).

---

## How to Use HorseCalc

### Step 1: Pot Intake & Coin Detection
1. Place the pot coins flat on a table (avoid heavy stacking for best results).
2. Tap **Take Photo** to capture directly at full sensor resolution, or tap **Gallery** to select an existing photo.
3. The on-device vision engine analyzes the image in $<300\text{ ms}$:
   - View color-coded rings over detected coins.
   - Toggle **Tags On / Tags Off** to hide/show labels.
   - Tap the image preview to enter full-screen inspection mode.
4. Review the auto-tallied counts (Quarters, Dimes, Nickels) and adjust using the **+** / **-** buttons if any stacked coin was obscured.
5. Tap **Next: Assign Winners**.

### Step 2: Assign Winning Cards
1. In physical horse racing card games, 4 cards in the deck match the winning horse.
2. Select a quick preset:
   - **Solo Winner**: 1 player holds all 4 cards (100% share).
   - **Split 2-Way**: 2 players hold 2 cards each (50% / 50%).
   - **3-Way Split**: 1 player has 2 cards, 2 players have 1 card each (50% / 25% / 25%).
   - **4-Way Split**: 4 players have 1 card each (25% each).
3. Or customize player names and tap **+** / **-** to distribute cards (the total must equal 4).
4. Tap **Calculate Payouts**.

### Step 3: Distribute Payouts & Rollover
1. View each winner's exact physical coin breakdown (number of Quarters, Dimes, and Nickels).
2. If the pot could not be evenly divided into four 5¢ increments, check the **Rollover Remainder Banner** (e.g. *"$0.10 Rollover to Next Pot"*). Place those physical coins back into the pot for the next race.
3. Check the **Paid** toggle next to each winner as physical coins are handed out.
4. Tap **Start Next Game** to clear winners and roll over remaining coins.

---

## Running Automated Tests

Run the domain engine and payout solver test suite:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew test
```

Tests cover:
- Solo, 2-way, 3-way, and 4-way share partitions.
- Exact integer coin backtracking solver under tight coin constraints.
- Extra nickel rollover remainder isolation.
- Quota validation (rejecting calculations if cards assigned $\ne 4$).

---

## Project Structure

```text
horsecalc/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml              // Camera permissions & FileProvider config
│   │   ├── res/xml/file_paths.xml           // FileProvider cache paths
│   │   └── java/com/horsecalc/app/
│   │       ├── MainActivity.kt              // Compose edge-to-edge root activity
│   │       ├── model/                       // Coin, CoinPot, Player, PayoutShare
│   │       ├── domain/                      // CalculatePotUseCase, DistributePotUseCase
│   │       ├── vision/                      // CvCoinDetector, CoinDetector interface
│   │       └── ui/
│   │           ├── screens/                 // PotIntakeScreen, WinnersScreen, PayoutScreen
│   │           ├── components/              // CoinCounterRow, WinnerCardPicker, PayoutCard
│   │           ├── theme/                   // Color, Theme, Type
│   │           └── viewmodel/               // HorseCalcViewModel, UiState, Events
│   └── src/test/java/com/horsecalc/app/     // DistributePotUseCaseTest suite
├── ARCHITECTURE.md                          // Detailed system architecture & CV algorithms
└── README.md                                // Project documentation & setup guide
```
