package com.darkforge.bloodsouls

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.BloodSouls.Souls.R
import com.BloodSouls.Souls.databinding.ActivityMainBinding
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import com.darkforge.bloodsouls.net.ApiService
import com.darkforge.bloodsouls.net.FirebaseAuthService
import com.darkforge.bloodsouls.util.LocaleHelper
import com.darkforge.bloodsouls.util.ProgressData
import com.darkforge.bloodsouls.util.SecurityUtils
import com.darkforge.bloodsouls.util.SettingsManager
import com.darkforge.bloodsouls.util.UserProgressManager
import com.darkforge.bloodsouls.views.BloodBackgroundView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import java.io.IOException


class MainActivity : FragmentActivity(), AndroidFragmentApplication.Callbacks {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var root: BloodBackgroundView
    private lateinit var contentContainer: ViewGroup
    private lateinit var settingsManager: SettingsManager
    private lateinit var progressManager: UserProgressManager
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var binding: ActivityMainBinding
    private lateinit var databaseHelper: DatabaseHelper
    private var currentScreenView: View? = null
    private var ourFontSize = 14f
    private var mediaPlayer: MediaPlayer? = null

    private val CHANNEL_ID = "bloodsouls_notifications"
    private val NOTIFICATION_ID = 1001
    private var pendingNotification: Pair<String, String>? = null

    override fun exit() {
        Log.i(TAG, "LibGDX exit callback received. Returning to main menu screen.")
        runOnUiThread {
            showScreen(GameScreen.MENU)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.screen_container)
        if (fragment is GameFragment) {
            Log.i(TAG, "Back pressed during gameplay fragment. Returning to menu.")
            showScreen(GameScreen.MENU)
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val settings = SettingsManager(newBase)
        Log.i(TAG, "attachBaseContext applying saved locale: ${settings.language}")
        val context = LocaleHelper.applyLocale(newBase, settings.language)
        super.attachBaseContext(context)
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "googleSignInLauncher activity result received: ${result.resultCode}")
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            val email = account?.email ?: "GoogleUser_${System.currentTimeMillis() % 1000}"
            Log.i(TAG, "Google Sign-In successful for email: $email")

            if (idToken != null) {
                Log.d(TAG, "Exchanging Google ID Token with Firebase Auth...")
                FirebaseAuthService.signInWithGoogleCredential(idToken, object : FirebaseAuthService.AuthCallback {
                    override fun onSuccess(userId: String) {
                        val username = if (email.contains("@")) email.substringBefore("@") else email
                        Log.i(TAG, "Google SSO Firebase auth successful. User: $email, UID: $userId")
                        settingsManager.loggedInUser = email
                        settingsManager.authToken = userId

                        if (FirebaseAuthService.lastGoogleSignInWasNewUser) {
                            showNotification("Welcome to Blood Souls!", "Your Google account has been created successfully.")
                        } else {
                            showNotification("Welcome Back to Blood Souls!", "You have successfully signed in with Google.")
                        }
                        navigate(GameScreen.MENU)
                    }

                    override fun onError(error: String) {
                        Log.e(TAG, "Firebase authentication failed: $error")
                        Log.e(TAG, "Google authentication failed. Please try again. Error: $error")
                    }
                })
            } else {
                Log.e(TAG, "Google authentication token is missing.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Google Sign-In API exception (${e.message}). Initializing fallback Google session...")
            val fallbackUsername = "GoogleUser_${System.currentTimeMillis() % 10000}"
            settingsManager.loggedInUser = fallbackUsername
            settingsManager.authToken = "google_sso_fallback_${System.currentTimeMillis()}"
            navigate(GameScreen.MENU)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate initializing MainActivity...")
        settingsManager = SettingsManager(this)

        ourFontSize = settingsManager.fontSize

        resetGuestFontSize()

        createNotificationChannel()

        progressManager = UserProgressManager(this)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(binding.root)
        databaseHelper = DatabaseHelper(this)


        root = findViewById(R.id.blood_background)
        contentContainer = findViewById(R.id.screen_container)

        Log.d(TAG, "MainActivity view inflation complete. Showing initial MENU screen...")
        showScreen(GameScreen.MENU)
    }

    fun showScreen(screen: GameScreen) {
        Log.i(TAG, "showScreen transition requested: $screen")
        root.visibility = View.VISIBLE

        val layoutId = when (screen) {
            GameScreen.LOGIN -> R.layout.screen_login
            GameScreen.REGISTER -> R.layout.screen_register
            GameScreen.MENU -> R.layout.second_main_menu
            GameScreen.NEW_GAME -> R.layout.screen_new_game
            GameScreen.CONTINUE -> R.layout.screen_continue
            GameScreen.OPTIONS -> R.layout.screen_options
            GameScreen.ABOUT -> R.layout.screen_about
            GameScreen.EXIT -> R.layout.screen_exit
        }

        contentContainer.removeAllViews()

        val inflatedView = layoutInflater.inflate(layoutId, contentContainer, false)
        contentContainer.addView(inflatedView)

        currentScreenView = inflatedView

        try {
            inflatedView.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.fade_in)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Animation failed for screen $screen: ${e.message}")
        }

        when (screen) {
            GameScreen.LOGIN -> setupLogin(inflatedView)
            GameScreen.REGISTER -> setupRegister(inflatedView)
            GameScreen.MENU -> setupMainMenu(inflatedView)
            GameScreen.NEW_GAME -> setupNewGame(inflatedView)
            GameScreen.CONTINUE -> setupContinue(inflatedView)
            GameScreen.OPTIONS -> setupOptions(inflatedView)
            GameScreen.ABOUT -> setupAbout(inflatedView)
            GameScreen.EXIT -> setupExit(inflatedView)
        }

        updateFontSize()
    }

    private fun launchGameplayFragment() {
        Log.i(TAG, "Launching GameFragment directly inside MainActivity screen_container...")

        stopBackgroundMusic()

        root.visibility = View.GONE
        contentContainer.removeAllViews()
        supportFragmentManager.beginTransaction()
            .replace(R.id.screen_container, GameFragment())
            .commitAllowingStateLoss()
    }

    private fun clearScreen() {
        Log.d(TAG, "clearScreen removing all active views from container")
        contentContainer.removeAllViews()
    }

    private fun loginDatabase(email: String, password: String): Boolean {
        val result = databaseHelper.readUser(email, password)
        Log.d(TAG, "Login database check: Email=$email Result=$result")
        return result
    }

    private fun setupLogin(view: View) {
        Log.d(TAG, "Setting up LOGIN screen UI handlers")
        val editEmail = view.findViewById<EditText>(R.id.edit_login_email)
        val editPassword = view.findViewById<EditText>(R.id.edit_login_password)
        val txtStatus = view.findViewById<TextView>(R.id.login_status)
        val btnLogin = view.findViewById<Button>(R.id.btn_do_login)
        val btnGoogleSso = view.findViewById<Button>(R.id.btn_google_sso)
        val linkRegister = view.findViewById<TextView>(R.id.link_go_register)
        val linkGuest = view.findViewById<TextView>(R.id.link_guest_login)
        val returnText = view.findViewById<TextView>(R.id.return_login)

        linkRegister?.setOnClickListener { navigate(GameScreen.REGISTER) }
        linkGuest?.setOnClickListener { navigate(GameScreen.MENU) }
        returnText?.setOnClickListener { navigate(GameScreen.MENU) }

        btnLogin?.setOnClickListener {
            val email = editEmail.text.toString().trim()
            val rawPassword = editPassword.text.toString()
            Log.i(TAG, "User clicked LOGIN for email: $email")

            if (email.isEmpty() || rawPassword.isEmpty()) {
                txtStatus?.visibility = View.VISIBLE
                "Please fill in all fields".also { txtStatus?.text = it }
                return@setOnClickListener
            }

            Log.d(TAG, "Hashing password using SHA-256 before authentication...")
            val hashedPassword = SecurityUtils.hashPassword(rawPassword)

            txtStatus?.visibility = View.VISIBLE
            "Authenticating...".also { txtStatus?.text = it }

            ApiService.loginUser(email, hashedPassword, object : ApiService.ApiCallback<String> {
                    override fun onSuccess(result: String) {
                        Log.d(TAG, "API LOGIN SUCCESS")
                        var localUserExists = loginDatabase(email, hashedPassword)

                        if (!localUserExists) {
                            Log.i(TAG, "User authenticated online but missing locally. Syncing credentials downstream...")
                            databaseHelper.insertUser(email, hashedPassword)
                            localUserExists = true
                        }

                        val username = if (email.contains("@")) email.substringBefore("@") else email

                        settingsManager.userEmail = email
                        settingsManager.loggedInUser = username
                        settingsManager.authToken = result

                        val savedSize = databaseHelper.getUserFontSize(email)
                        ourFontSize = savedSize.coerceIn(8f, 40f)
                        settingsManager.fontSize = ourFontSize
                        updateFontSize()

                        "Login successful!".also { txtStatus?.text = it }

                        showNotification("Welcome Back to Blood Souls!", "You have successfully logged in.")

                        navigate(GameScreen.MENU)
                    }
                    override fun onError(errorMessage: String) {
                        Log.d(TAG, "API LOGIN FAILED")
                        Log.e(TAG, "API Login Error: $errorMessage")
                        val localLogin = loginDatabase(email, hashedPassword)
                        Log.d(TAG, "Offline login result = $localLogin")
                        if (localLogin) {
                            val username = if (email.contains("@"))email.substringBefore("@") else email
                            settingsManager.userEmail = email
                            settingsManager.loggedInUser = username

                            val savedSize = databaseHelper.getUserFontSize(email)
                            ourFontSize = savedSize.coerceIn(8f, 40f)
                            settingsManager.fontSize = ourFontSize
                            updateFontSize()

                            "Offline login successful!".also { txtStatus?.text = it }
                            navigate(GameScreen.MENU)
                        } else {
                            "Incorrect email or password.".also { txtStatus?.text = it }
                        }
                    }
                })
        }

        btnGoogleSso?.setOnClickListener {
            Log.i(TAG, "User clicked SIGN IN WITH GOOGLE SSO on Login screen")
            val webClientId = getString(R.string.default_web_client_id_fallback)
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInClient.signOut().addOnCompleteListener {
                Log.d(TAG, "Previous Google account cleared. Opening account chooser.")

                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        }
    }

    private fun registerDatabase(email: String, password: String): Boolean {
        val insertRowId = databaseHelper.insertUser(email, password)
        return insertRowId != -1L
    }
    private fun setupRegister(view: View) {
        Log.d(TAG, "Setting up REGISTER screen UI handlers")
        val editEmail = view.findViewById<EditText>(R.id.edit_reg_email)
        val editPassword = view.findViewById<EditText>(R.id.edit_reg_password)
        val editConfirm = view.findViewById<EditText>(R.id.edit_reg_confirm_password)
        val txtStatus = view.findViewById<TextView>(R.id.register_status)
        val btnRegister = view.findViewById<Button>(R.id.btn_do_register)
        val btnGoogleSso = view.findViewById<Button>(R.id.btn_reg_google_sso)
        val linkLogin = view.findViewById<TextView>(R.id.link_go_login)

        linkLogin?.setOnClickListener { navigate(GameScreen.LOGIN) }

        btnRegister?.setOnClickListener {
            val email = editEmail?.text?.toString()?.trim() ?: ""
            val rawPassword = editPassword?.text?.toString() ?: ""
            val confirmPassword = editConfirm?.text?.toString() ?: ""
            Log.i(TAG, "User clicked REGISTER ACCOUNT for email: $email")

            if (email.isEmpty() || rawPassword.isEmpty() || confirmPassword.isEmpty()) {
                Log.w(TAG, "Registration validation failed: empty fields")
                txtStatus?.visibility = View.VISIBLE
                "Please fill in all fields".also { txtStatus?.text = it }
                return@setOnClickListener
            }
            else if(!email.matches(Regex("^[A-Za-z0-9+_.-]+@(gmail\\.com|hotmail\\.com|yahoo\\.com)$"))){
                Log.w(TAG, "Registration validation failed: invalid input")
                txtStatus?.visibility = View.VISIBLE
                "Please enter a valid email (gmail, hotmail, yahoo)".also { txtStatus?.text = it }
                return@setOnClickListener
            }
            else if(!rawPassword.matches(Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#\$%^&+=!]).{8,}$"))){
                Log.w(TAG, "Registration validation failed: invalid input")
                txtStatus?.visibility = View.VISIBLE
                "Password must be at least 8 characters, include uppercase, lowercase, digit, and special character".also { txtStatus?.text = it }
                return@setOnClickListener
            }

            if (rawPassword != confirmPassword) {
                Log.w(TAG, "Registration validation failed: password mismatch")
                txtStatus?.visibility = View.VISIBLE
                "Passwords do not match".also { txtStatus?.text = it }
                return@setOnClickListener
            }

            if (databaseHelper.userExists(email)) {
                txtStatus?.visibility = View.VISIBLE
                "User already exists".also { txtStatus?.text = it }
                return@setOnClickListener
            }

            Log.d(TAG, "Encrypting password using salted SHA-256 hash...")
            val hashedPassword = SecurityUtils.hashPassword(rawPassword)

            txtStatus?.visibility = View.VISIBLE
            "Creating account...".also { txtStatus?.text = it }

            ApiService.registerUser(email, hashedPassword, object : ApiService.ApiCallback<String> {
                override fun onSuccess(result: String) {
                    val username = if (email.contains("@"))email.substringBefore("@") else email
                    Log.i(TAG, "Account created successfully for user: $username (Token: $result)")
                    settingsManager.userEmail = email
                    settingsManager.loggedInUser = username
                    settingsManager.authToken = result

                    val saved = registerDatabase(email, hashedPassword)

                    if (saved) {
                        txtStatus?.visibility = View.VISIBLE
                        "Account created successfully!".also { txtStatus?.text = it }

                        showNotification("Welcome to Blood Souls!", "Your account has been created successfully.")

                        navigate(GameScreen.LOGIN)
                    }
                    else {
                        Log.e(TAG, "Failed to save registered user locally.")

                        txtStatus?.visibility = View.VISIBLE
                        "Account created, but local saving failed.".also { txtStatus?.text = it }
                    }
                }


                override fun onError(errorMessage: String) {
                    Log.e(TAG, "Registration error callback: $errorMessage")
                    "Registration failed: $errorMessage".also { txtStatus?.text = it }
                }
            })
        }

        btnGoogleSso?.setOnClickListener {
            Log.i(TAG, "User clicked SIGN IN WITH GOOGLE SSO on Register screen")
            val webClientId = getString(R.string.default_web_client_id_fallback)
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInClient.signOut().addOnCompleteListener {
                Log.d(TAG, "Previous Google account cleared. Opening account chooser.")

                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        }
    }

    private fun setupMainMenu(view: View) {
        Log.d(TAG, "Setting up MAIN MENU screen UI handlers")
        val newGame = view.findViewById<TextView>(R.id.menu_new_game)
        val continueGame = view.findViewById<TextView>(R.id.menu_continue)
        val options = view.findViewById<TextView>(R.id.menu_options)
        val account = view.findViewById<TextView>(R.id.menu_account)
        val about = view.findViewById<TextView>(R.id.menu_about)
        val exit = view.findViewById<TextView>(R.id.menu_exit)
        val userStatus = view.findViewById<TextView>(R.id.user_status)


        val loggedInUser = settingsManager.loggedInUser
        if (!loggedInUser.isNullOrEmpty()) {
            Log.i(TAG, "Main Menu active session: $loggedInUser")
            userStatus?.text = getString(R.string.user_status_logged_in, loggedInUser)
            userStatus?.setTextColor(0xFF00FF88.toInt())
            account?.text = getString(R.string.menu_logout, loggedInUser)
            account?.setOnClickListener {
                Log.i(TAG, "User clicked LOGOUT")

                FirebaseAuthService.signOut()

                googleSignInClient.signOut().addOnCompleteListener {
                    Log.i(TAG, "Google Sign-In session cleared.")
                }

                settingsManager.logout()

                ourFontSize = 14f
                settingsManager.fontSize = 14f

                Log.i(TAG, "Font size reset to default: 14sp")
                showScreen(GameScreen.MENU)
            }
        } else {
            Log.i(TAG, "Main Menu active session: Guest / Not Logged In")
            userStatus?.text = getString(R.string.user_status_not_logged_in)
            userStatus?.setTextColor(0xFFAAAAAA.toInt())
            account?.text = getString(R.string.menu_account)
            account?.setOnClickListener { navigate(GameScreen.LOGIN) }
        }

        newGame?.setOnClickListener { navigate(GameScreen.NEW_GAME) }
        continueGame?.setOnClickListener { navigate(GameScreen.CONTINUE) }
        options?.setOnClickListener { navigate(GameScreen.OPTIONS) }
        about?.setOnClickListener { navigate(GameScreen.ABOUT) }
        exit?.setOnClickListener { navigate(GameScreen.EXIT) }

        animateTitle(view)
    }

    private fun setupNewGame(view: View) {
        Log.d(TAG, "Setting up NEW GAME screen UI handlers")
        val penitent = view.findViewById<Button>(R.id.difficulty_penitent)
        val cursed = view.findViewById<Button>(R.id.difficulty_cursed)
        val damned = view.findViewById<Button>(R.id.difficulty_damned)
        val begin = view.findViewById<Button>(R.id.begin_journey)

        if (penitent != null && cursed != null && damned != null) {
            val currentDiff = settingsManager.difficulty
            Log.d(TAG, "Current selected difficulty: $currentDiff")
            val selectedBtn = when (currentDiff) {
                "PENITENT" -> penitent
                "DAMNED" -> damned
                else -> cursed
            }
            selectDifficulty(penitent, cursed, damned, selectedBtn)

            penitent.setOnClickListener {
                Log.i(TAG, "User selected difficulty: PENITENT")
                settingsManager.difficulty = "PENITENT"
                selectDifficulty(penitent, cursed, damned, penitent)
            }
            cursed.setOnClickListener {
                Log.i(TAG, "User selected difficulty: CURSED")
                settingsManager.difficulty = "CURSED"
                selectDifficulty(penitent, cursed, damned, cursed)
            }
            damned.setOnClickListener {
                Log.i(TAG, "User selected difficulty: DAMNED")
                settingsManager.difficulty = "DAMNED"
                selectDifficulty(penitent, cursed, damned, damned)
            }
        }

        begin?.setOnClickListener {
            Log.i(TAG, "User clicked BEGIN JOURNEY. Saving starting progress and launching GameFragment...")
            progressManager.saveProgress(
                ProgressData(
                    slotId = 1,
                    mapName = "sewer_map.tmx",
                    difficulty = settingsManager.difficulty,
                    health = 100
                ),
                userId = settingsManager.authToken
            )

            launchGameplayFragment()
        }

        view.findViewById<TextView>(R.id.return_new_game)?.setOnClickListener {
            navigate(GameScreen.MENU)
        }
    }

    private fun selectDifficulty(
        penitent: Button,
        cursed: Button,
        damned: Button,
        selected: Button
    ) {
        penitent.setBackgroundResource(if (selected == penitent) R.drawable.bg_selected else R.drawable.bg_button)
        cursed.setBackgroundResource(if (selected == cursed) R.drawable.bg_selected else R.drawable.bg_button)
        damned.setBackgroundResource(if (selected == damned) R.drawable.bg_selected else R.drawable.bg_button)
    }

    private fun setupContinue(view: View) {
        Log.d(TAG, "Setting up CONTINUE screen UI handlers")
        view.findViewById<TextView>(R.id.return_continue)?.setOnClickListener {
            navigate(GameScreen.MENU)
        }

        val slot1 = view.findViewById<View>(R.id.save_slot_1)
        val slot2 = view.findViewById<View>(R.id.save_slot_2)

        slot1?.setOnClickListener {
            Log.i(TAG, "User selected SAVE SLOT #1")
            val progress = progressManager.loadProgress(1)
            progressManager.saveProgress(progress, userId = settingsManager.authToken)

            launchGameplayFragment()
        }

        slot2?.setOnClickListener {
            Log.i(TAG, "User selected SAVE SLOT #2")
            val progress = progressManager.loadProgress(2)
            progressManager.saveProgress(progress, userId = settingsManager.authToken)

            launchGameplayFragment()
        }
    }

    private fun setupOptions(view: View) {
        Log.d(TAG, "Setting up OPTIONS screen UI handlers")
        view.findViewById<TextView>(R.id.return_options)?.setOnClickListener {
            navigate(GameScreen.MENU)
        }

        val bloodLow = view.findViewById<Button>(R.id.blood_low)
        val bloodMedium = view.findViewById<Button>(R.id.blood_medium)
        val bloodHigh = view.findViewById<Button>(R.id.blood_high)

        if (bloodLow != null && bloodMedium != null && bloodHigh != null) {
            bloodLow.text = getString(R.string.intensity_low)
            bloodMedium.text = getString(R.string.intensity_med)
            bloodHigh.text = getString(R.string.intensity_high)

            val currentBlood = settingsManager.bloodIntensity
            Log.d(TAG, "Current Blood Intensity setting: $currentBlood")
            val activeBloodBtn = when (currentBlood) {
                "LOW" -> bloodLow
                "MED" -> bloodMedium
                else -> bloodHigh
            }
            selectBlood(bloodLow, bloodMedium, bloodHigh, activeBloodBtn)

            bloodLow.setOnClickListener {
                Log.i(TAG, "User selected Blood Intensity: LOW")
                settingsManager.bloodIntensity = "LOW"
                selectBlood(bloodLow, bloodMedium, bloodHigh, bloodLow)
                syncSettingsToCloud()
            }
            bloodMedium.setOnClickListener {
                Log.i(TAG, "User selected Blood Intensity: MED")
                settingsManager.bloodIntensity = "MED"
                selectBlood(bloodLow, bloodMedium, bloodHigh, bloodMedium)
                syncSettingsToCloud()
            }
            bloodHigh.setOnClickListener {
                Log.i(TAG, "User selected Blood Intensity: HIGH")
                settingsManager.bloodIntensity = "HIGH"
                selectBlood(bloodLow, bloodMedium, bloodHigh, bloodHigh)
                syncSettingsToCloud()
            }
        }

        setupLanguageButtons(view)
        setupFontSize(view)
        setupBackgroundMusic(view)
    }

    private fun syncSettingsToCloud() {
        val user = settingsManager.loggedInUser ?: "Guest"
        Log.i(TAG, "Triggering cloud settings sync for user: $user")
        ApiService.syncSettings(
            username = user,
            bloodIntensity = settingsManager.bloodIntensity,
            language = settingsManager.language,
            difficulty = settingsManager.difficulty,
            callback = object : ApiService.ApiCallback<Boolean> {
                override fun onSuccess(result: Boolean) {
                    Log.d(TAG, "syncSettingsToCloud callback success: $result")
                }
                override fun onError(errorMessage: String) {
                    Log.w(TAG, "syncSettingsToCloud callback error: $errorMessage")
                }
            }
        )
    }

    private fun selectBlood(
        low: Button,
        medium: Button,
        high: Button,
        selected: Button
    ) {
        low.setBackgroundResource(if (selected == low) R.drawable.bg_selected else R.drawable.bg_button)
        medium.setBackgroundResource(if (selected == medium) R.drawable.bg_selected else R.drawable.bg_button)
        high.setBackgroundResource(if (selected == high) R.drawable.bg_selected else R.drawable.bg_button)
    }

    private fun setupLanguageButtons(view: View) {
        val english = view.findViewById<TextView>(R.id.language_english)
        val afrikaans = view.findViewById<TextView>(R.id.language_afrikaans)
        val xhosa = view.findViewById<TextView>(R.id.language_xhosa)
        val tswana = view.findViewById<TextView>(R.id.language_tswana)

        val currentLang = settingsManager.language
        Log.d(TAG, "setupLanguageButtons active language: $currentLang")
        val languages = listOfNotNull(english, afrikaans, xhosa, tswana)

        languages.forEach { langTv ->
            val langName = when (langTv.id) {
                R.id.language_afrikaans -> "AFRIKAANS"
                R.id.language_xhosa -> "XHOSA"
                R.id.language_tswana -> "TSWANA"
                else -> "ENGLISH"
            }

            if (langName.equals(currentLang, ignoreCase = true)) {
                langTv.setBackgroundResource(R.drawable.bg_selected)
            } else {
                langTv.setBackgroundResource(R.drawable.bg_button)
            }

            langTv.setOnClickListener {
                if (!langName.equals(currentLang, ignoreCase = true)) {
                    Log.i(TAG, "Language changed from '$currentLang' to '$langName'. Recreating Activity to apply new locale globally.")
                    settingsManager.language = langName
                    syncSettingsToCloud()
                    recreate()
                }
            }
        }
    }

    private fun updateFontSize() {
        currentScreenView?.let { resizeAllTextViews(it) }}

    private fun resizeAllTextViews(view: View) {
        if (view is TextView) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, ourFontSize)
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) { resizeAllTextViews(view.getChildAt(i)) }
        }
    }

    private fun setupFontSize(view: View){
        val increase = view.findViewById<Button>(R.id.font_increase)
        val decrease = view.findViewById<Button>(R.id.font_decrease)

        increase?.setBackgroundResource(R.drawable.bg_button)
        decrease?.setBackgroundResource(R.drawable.bg_button)

        increase?.setOnClickListener {
            if (ourFontSize < 22f) {
                ourFontSize = (ourFontSize + 4f).coerceAtMost(22f)

                saveFontSize()

                updateFontSize()
                increase.setBackgroundResource(R.drawable.bg_selected)
                decrease?.setBackgroundResource(R.drawable.bg_button)
            }
        }

        decrease?.setOnClickListener {
            if (ourFontSize > 8f) {
                ourFontSize = (ourFontSize - 4f).coerceAtMost(22f)

                saveFontSize()

                updateFontSize()
                decrease.setBackgroundResource(R.drawable.bg_selected)
                increase?.setBackgroundResource(R.drawable.bg_button)
            }
        }
    }

    private fun saveFontSize() {
        settingsManager.fontSize = ourFontSize

        val email = settingsManager.userEmail

        if (!email.isNullOrEmpty()) {
            databaseHelper.updateUserFontSize(email, ourFontSize)

            Log.d(TAG, "Font size saved for registered user: $ourFontSize")
        }
    }

    private fun resetGuestFontSize() {
        val loggedInUser = settingsManager.loggedInUser

        if (loggedInUser.isNullOrEmpty()) {
            Log.i(TAG, "No logged-in user. Resetting font size to default: 14sp")

            ourFontSize = 14f
            settingsManager.fontSize = 14f
        }
    }
    private fun setupBackgroundMusic(view: View){
        val play = findViewById<Button>(R.id.music_on)
        val pause = findViewById<Button>(R.id.music_off)

        play?.setBackgroundResource(R.drawable.bg_button)
        pause?.setBackgroundResource(R.drawable.bg_button)

        play.setOnClickListener {
            playMusic()
            play.setBackgroundResource(R.drawable.bg_selected)
            pause?.setBackgroundResource(R.drawable.bg_button)
        }

        pause.setOnClickListener {
            pauseMusic()
            pause.setBackgroundResource(R.drawable.bg_selected)
            play?.setBackgroundResource(R.drawable.bg_button)
        }
    }

    private fun playMusic(){
        mediaPlayer?.release()
        mediaPlayer = null

        try {
            val player = MediaPlayer()
            mediaPlayer = player

            player.setAudioStreamType(AudioManager.STREAM_MUSIC)
            player.isLooping = true

            val assetFileDescriptor = assets.openFd("bloodsouls_background.mp3")

            mediaPlayer!!.setDataSource(assetFileDescriptor.fileDescriptor,assetFileDescriptor.startOffset,assetFileDescriptor.length)
            assetFileDescriptor.close()
            mediaPlayer!!.prepare()
            mediaPlayer!!.start()

            Toast.makeText(this, "Music started playing", Toast.LENGTH_SHORT).show()
        }
        catch (e: IOException){
            Log.e(TAG, "Error loading background music", e)

            mediaPlayer?.release()
            mediaPlayer = null

            Toast.makeText(this, "Error loading audio file", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this,"Music started playing", Toast.LENGTH_SHORT).show()
    }

    private fun pauseMusic(){
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer!!.pause()
            mediaPlayer!!.seekTo(0)

            Toast.makeText(this,"Music has been stopped", Toast.LENGTH_SHORT).show()

        }
        else{
            Toast.makeText(this,"Music has not played", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopBackgroundMusic() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop() }
                player.release()
            }
            mediaPlayer = null
            Log.i(TAG, "Background music stopped for gameplay.")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping background music", e)
            mediaPlayer = null }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Blood Souls Notifications", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Blood Souls account notifications" }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                pendingNotification?.let { notificationData ->
                    val title = notificationData.first
                    val message = notificationData.second
                    postNotification(title, message)
                }
            } else {
                Log.w(TAG, "Notification permission denied")
            }
            pendingNotification = null
        }

    private fun showNotification(title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingNotification = title to message

            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        postNotification(title, message)
    }

    private fun postNotification(title: String, message: String) {

        try {
            val intent = Intent(this, MainActivity::class.java)

            val pendingIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val notification = builder.build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)

            Log.d(TAG, "Notification displayed: $title")

        } catch (e: Exception) {
            Log.e(TAG, "Notification failed", e)
        }
    }

    private fun setupAbout(view: View) {
        Log.d(TAG, "Setting up ABOUT screen UI handlers")
        view.findViewById<TextView>(R.id.return_about)?.setOnClickListener {
            navigate(GameScreen.MENU)
        }
    }
    private fun setupExit(view: View) {
        Log.d(TAG, "Setting up EXIT screen UI handlers")
        val stay = view.findViewById<Button>(R.id.stay_button)
        val abandon = view.findViewById<Button>(R.id.abandon_button)

        stay?.setOnClickListener { navigate(GameScreen.MENU) }
        abandon?.setOnClickListener {
            Log.i(TAG, "User confirmed ABANDON. Finishing MainActivity...")
            finish()
        }

        view.findViewById<TextView>(R.id.return_exit)?.setOnClickListener {
            navigate(GameScreen.MENU)
        }
    }

    private fun animateTitle(view: View) {
        val title = view.findViewById<TextView>(R.id.game_title) ?: return
        title.alpha = 0.85f
        title.animate()
            .alpha(1f)
            .setDuration(3000)
            .withEndAction {
                if (title.isAttachedToWindow) {
                    title.animate()
                        .alpha(0.85f)
                        .setDuration(3000)
                        .withEndAction {
                            if (title.isAttachedToWindow) {
                                animateTitle(view)
                            }
                        }
                        .start()
                }
            }
            .start()
    }

    private fun navigate(screen: GameScreen) {
        Log.d(TAG, "navigate called -> target screen: $screen")
        if (contentContainer.childCount > 0) {
            val current = contentContainer.getChildAt(0)
            try {
                val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
                current?.startAnimation(fadeOut)
            } catch (e: Exception) {
                Log.w(TAG, "Fade-out animation error: ${e.message}")
            }
        }

        clearScreen()
        showScreen(screen)
    }
}
