# Todo List App

A modern Android Todo List application built with Jetpack Compose and Firebase integration.

## Architecture Highlights
- Single Activity Architecture with Jetpack Compose
- Firebase Realtime Database for data persistence
- Firebase Authentication for user management
- MVVM architecture pattern
- Coroutines for asynchronous operations
- State management using `remember` and `mutableStateOf`

## Prerequisites
- Android Studio Hedgehog or newer
- Minimum SDK: 24
- Kotlin 1.9.0 or newer
- Google Services JSON file from Firebase Console
- Firebase Project with:
  - Authentication enabled
  - Realtime Database configured in asia-southeast1 region

## Firebase Setup

1. Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app with package name `fm.mrc.todolist`
3. Download `google-services.json` and place it in the `app/` directory
4. Enable Email/Password Authentication in Firebase Console
5. Create a Realtime Database in asia-southeast1 region
6. Set the following Database Rules:
