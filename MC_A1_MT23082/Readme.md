# Route Journey App

This app is designed to provide users with information about a journey route, including stops, distances between stops, and the progress of the journey. It follows the guidelines outlined below:

## Guidelines

1. **Distance Units**: The app displays distances in both kilometers and miles. Users can switch between the two units using a button.
2. **Next Stop Button**: Users can indicate that they have reached the next stop by clicking a button.
3. **Journey Progress**: The progress of the journey is visualized through a TextBox showing each stop, their distances, total distance covered, total distance left, and a ProgressBar.
4. **Lazy List**: If the route has more than 10 stops, a lazy list is used to efficiently handle the display of stops. Two hardcoded entries of stops are provided in the code, one shown as a normal list and one with a lazy list.
5. **Compatibility**: The app is designed to run on both Android devices and the Android emulator.

## How to Use

1. Clone the repository to your local machine.
2. Open the project in Android Studio.
3. Build and run the app on an Android device or emulator.

## Code Structure

- **Kotlin**: Contains the Kotlin source code for the app.
- **Gradle**: Contains the Gradle configuration files.
- **XML**: Contains XML files for layout designs (if applicable).
- **README.md**: This file, providing an overview of the project and instructions for use.

## Usage

1. Open the app on your Android device or emulator.
2. Use the provided buttons to switch between distance units and indicate reaching the next stop.
3. View the journey progress displayed on the screen.
