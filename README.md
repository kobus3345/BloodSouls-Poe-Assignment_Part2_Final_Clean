Blood Souls - Dark Souls Style Android Action Game

Technology Stack:Kotlin, Java, LibGDX, Android SDK, Firebase Authentication, Firebase Firestore, Google Single Sign-On (SSO), REST API, Tiled Map Editor (`.tmx`).



Project Overview
Blood Souls is a 2D dark fantasy action-adventure game for Android built using Kotlin and LibGDX. The game features a complete player account system, multi-language support (English, Afrikaans, Xhosa, Tswana), local and cloud save data synchronization, tile-based collision physics, and a hazard damage system on a custom sewer dungeon map (`sewer_map.tmx`).


 Features & Requirements Covered

 1 User Authentication & Password Encryption
Player Registration & Login**: Built-in account creation and login screens with dark fantasy UI styling.
Password Encryption: All user passwords are encrypted using SHA-256 has (`SecurityUtils.kt`) before saving or transmitting.
Google Single Sign-On (SSO): Integrated Google Play Services Auth (`play-services-auth:21.3.0`) allowing one-tap Google login.
Firebase Authentication: User accounts and tokens are managed via Firebase Auth (`FirebaseAuthService.kt`) with fallback to hosted REST API endpoints (`ApiService.kt`).

2 Multi-Language 
Supports 4 Languages:
  English (`res/values/strings.xml`)
  Afrikaans (`res/values-af/strings.xml`)
  Xhosa(`res/values-xh/strings.xml`)
  swana (`res/values-tn/strings.xml`)
Dynamic Language Switching: Selecting a language in Options instantly updates all UI titles, buttons, and screen texts across the entire app.


 3 Hosted REST API & Cloud Database Integration
Firebase Firestore Database: Connected to live hosted Firebase Firestore adnroidapiproject to store user account records and player progress under the users collection.
Hosted REST API: Communicates with hosted REST API endpoints (https://reqres.in/api) for background authentication and preference syncing.


4 Logging
 Comprehensive Android (Log.i, Log.d, Log.e) and LibGDX (Gdx.app.log) logging statements throughout MainActivity, GameplayScreen, ApiService, FirebaseAuthService, SettingsManager, and UserProgressManager to illustrate execution flow, network responses, locale changes, tile collisions, and hazard damage.


Youtube link: https://youtu.be/zhj_DN7z5-Y
