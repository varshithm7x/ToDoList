# Todo List App

A modern Android todo list application built with Jetpack Compose and Firebase, featuring multiple organization methods and smart notifications.

## Features

- **Multiple Todo Organization Methods**:
  - Simple List View
  - Calendar-Based Organization
  - Time-Based Scheduling

- **User Authentication**:
  - Email/Password Sign Up
  - Secure Login
  - Remember Me Functionality
  - Auto-login for Returning Users

- **Smart Notifications**:
  - Automatic Reminders for Tasks
  - Custom Time Notifications
  - Calendar-based Alerts

- **Real-time Sync**:
  - Firebase Backend Integration
  - Automatic Data Persistence
  - Cross-device Synchronization

- **User Interface**:
  - Material Design 3
  - Smooth Animations
  - Intuitive Navigation

## Firebase Setup

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app to your Firebase project:
   - Package name: `fm.mrc.todolist`
   - Download `google-services.json`
3. Enable Authentication:
   - Go to Authentication > Sign-in method
   - Enable Email/Password authentication
4. Set up Realtime Database:
   - Create a database in test mode
   - Set the following security rules:


### Architecture Highlights
- Single Activity Architecture
- Composable-based UI components
- State management using `remember` and `mutableStateOf`
- Firebase Realtime Database for data persistence
- Firebase Authentication for user management
- Custom type adapters for date handling
- Coroutines for animations and async operations

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or newer
- Minimum SDK: 24
- Kotlin 1.8.0 or newer
- Google Services JSON file from Firebase Console
- Firebase Project with:
  - Authentication enabled
  - Realtime Database configured in asia-southeast1 region

### Firebase Setup
1. Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app with package name `fm.mrc.todolist`
3. Download `google-services.json` and place it in the `app/` directory
4. Enable Email/Password Authentication in Firebase Console
5. Create a Realtime Database in asia-southeast1 region
6. Set the following Database Rules:   ```json
   {
     "rules": {
       "users": {
         "$uid": {
           ".read": "$uid === auth.uid",
           ".write": "$uid === auth.uid",
           "todos": {
             ".read": "$uid === auth.uid",
             ".write": "$uid === auth.uid"
           }
         }
       }
     }
   }   ```

### Installation
1. Clone the repository   ```bash
   git clone https://github.com/yourusername/todo-list-app.git   ```
2. Open the project in Android Studio
3. Ensure you have placed the `google-services.json` file in the app directory
4. Build and run the application

## Usage

1. Launch the app
2. Sign up for a new account or sign in with existing credentials
3. Choose your preferred task organization method:
   - Simple List for basic tasks
   - Calendar Based for date-oriented tasks
   - Time Based for scheduled tasks
4. Add tasks using the floating action button
5. Manage tasks by:
   - Marking them as complete
   - Deleting them
   - Organizing them by date or time slot
   - 

### Troubleshooting
If you experience issues with data persistence:
1. Ensure you have the correct Firebase Database URL
2. Check Firebase Console for any permission issues
3. Verify your device has an internet connection
4. Check Logcat for detailed error messages

## Contributing

1. Fork the project from [https://github.com/varshithm7x/ToDoList](https://github.com/varshithm7x/ToDoList)
2. Create your feature branch   ```bash
   git checkout -b feature/YourFeatureName   ```
