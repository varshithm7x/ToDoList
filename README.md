# Modern Todo List App

A feature-rich Todo List application built with Jetpack Compose that offers three distinct ways to organize tasks: Simple List, Calendar-based, and Time-based scheduling.

## Features

### Multiple Task Organization Methods
- **Simple List**: Quick and straightforward task management without dates
- **Calendar Based**: Organize tasks by date for better deadline management
- **Time Based**: Schedule tasks in specific time slots for detailed daily planning

### Key Functionalities
- ✨ Intuitive Material Design 3 UI
- 📅 Date picker for task scheduling
- ⏰ Custom time slot management
- 💾 Persistent storage using SharedPreferences
- 🎯 Task completion tracking
- 🗑️ Task deletion
- 📱 Edge-to-edge design
- 🎓 Interactive tutorial for first-time users

### User Experience
- Smooth animations and transitions
- Expandable/collapsible date sections
- Visual feedback for task completion
- Intuitive navigation with back button support
- Clean and modern interface

## Technical Details

### Built With
- Kotlin
- Jetpack Compose
- Material Design 3
- GSON for data serialization
- Android Architecture Components

### Architecture Highlights
- Single Activity Architecture
- Composable-based UI components
- State management using `remember` and `mutableStateOf`
- Custom type adapters for date handling
- Coroutines for animations

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or newer
- Minimum SDK: [Your minimum SDK version]
- Kotlin 1.8.0 or newer

### Installation
1. Clone the repository
   ```bash
   git clone https://github.com/yourusername/todo-list-app.git
   ```
2. Open the project in Android Studio
3. Build and run the application

## Usage

1. Launch the app
2. Choose your preferred task organization method:
   - Simple List for basic tasks
   - Calendar Based for date-oriented tasks
   - Time Based for scheduled tasks
3. Add tasks using the floating action button
4. Manage tasks by:
   - Marking them as complete
   - Deleting them
   - Organizing them by date or time slot

## Contributing

1. Fork the project from [https://github.com/varshithm7x/ToDoList](https://github.com/varshithm7x/ToDoList)
2. Create your feature branch
   ```bash
   git checkout -b feature/YourFeatureName
   ```
3. Commit your changes
   ```bash
   git commit -m 'Add some YourFeatureName'
   ```
4. Push to the branch
   ```bash
   git push origin feature/YourFeatureName
   ```
5. Open a Pull Request at [https://github.com/varshithm7x/ToDoList/pulls](https://github.com/varshithm7x/ToDoList/pulls)

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

- Material Design 3 for the modern UI components
- Jetpack Compose for the declarative UI framework
- The Android development community for inspiration and resources  
