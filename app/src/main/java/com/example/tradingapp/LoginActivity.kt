package com.qubex.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

open class LoginActivity : Activity() {

    companion object {
        private const val HERO_URL =
            "https://i.ibb.co/rKvzFXQd/file-0000000016848246a932b72ca8476b79.png"

        private const val STATE_EMAIL = "qubex.email"
        private const val STATE_REMEMBER = "qubex.remember"
        private const val STATE_SIGNUP = "qubex.signup"
        private const val STATE_NAME = "qubex.signup.name"
        private const val STATE_SIGNUP_EMAIL = "qubex.signup.email"
        private const val STATE_PHONE = "qubex.signup.phone"
        private const val STATE_COUNTRY = "qubex.signup.country"
        private const val STATE_TERMS = "qubex.signup.terms"

        private const val DESIGN_WIDTH = 491f
        private const val DESIGN_HEIGHT = 1120f

        // خلفية موحدة للصفحتين ولحواف النظام.
        private val SIGNUP_BACKGROUND = Color.rgb(3, 9, 25)
        private val PLATFORM_BACKGROUND = SIGNUP_BACKGROUND
    }

    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var screen: LoginScreen
    private lateinit var container: FrameLayout
    private var downloadStarted = false

    @Volatile
    private var disposed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()

        // إبقاء سلوك لوحة المفاتيح الأصلي: لا تحريك ولا تصغير للنافذة.
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )
        window.statusBarColor = PLATFORM_BACKGROUND
        window.navigationBarColor = PLATFORM_BACKGROUND
        window.setBackgroundDrawable(ColorDrawable(PLATFORM_BACKGROUND))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        screen = LoginScreen(this)

        // العرض كامل دون padding أو هوامش إضافية.
        // التمرير يحمي أسفل المحتوى على الشاشات القصيرة دون ضغط التصميم.
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = true
            clipChildren = true
            setPadding(0, 0, 0, 0)
            setBackgroundColor(PLATFORM_BACKGROUND)
            addView(
                screen,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        container = FrameLayout(this).apply {
            setBackgroundColor(PLATFORM_BACKGROUND)
            clipChildren = true
            clipToPadding = true
            addView(
                scroll,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            container.setOnApplyWindowInsetsListener { view, insets ->
                // فقط المساحة الآمنة للنظام والنتوء؛ لا نضيف أي هوامش تصميم.
                // لا نستخدم IME insets حتى يبقى سلوك لوحة المفاتيح كما كان.
                val bars = insets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars() or
                        WindowInsets.Type.displayCutout()
                )
                if (
                    view.paddingLeft != bars.left ||
                    view.paddingTop != bars.top ||
                    view.paddingRight != bars.right ||
                    view.paddingBottom != bars.bottom
                ) {
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                }
                insets
            }
        }

        setContentView(container)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            container.requestApplyInsets()
        }

        savedInstanceState?.let { state ->
            screen.email.setText(state.getString(STATE_EMAIL, ""))
            screen.remember = state.getBoolean(STATE_REMEMBER, true)
            screen.fullName.setText(state.getString(STATE_NAME, ""))
            screen.registrationEmail.setText(state.getString(STATE_SIGNUP_EMAIL, ""))
            screen.phone.setText(state.getString(STATE_PHONE, ""))
            screen.countryCode = state.getString(STATE_COUNTRY, "")
            screen.termsAccepted = state.getBoolean(STATE_TERMS, true)
            screen.showRegistration(state.getBoolean(STATE_SIGNUP, false))
        }

        loadHero()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::screen.isInitialized) {
            outState.putString(STATE_EMAIL, screen.email.text.toString())
            outState.putBoolean(STATE_REMEMBER, screen.remember)
            outState.putBoolean(STATE_SIGNUP, screen.registrationVisible)
            outState.putString(STATE_NAME, screen.fullName.text.toString())
            outState.putString(STATE_SIGNUP_EMAIL, screen.registrationEmail.text.toString())
            outState.putString(STATE_PHONE, screen.phone.text.toString())
            outState.putString(STATE_COUNTRY, screen.countryCode)
            outState.putBoolean(STATE_TERMS, screen.termsAccepted)
            // لا نحفظ كلمات المرور داخل Bundle.
        }
        super.onSaveInstanceState(outState)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (::screen.isInitialized && screen.registrationVisible) {
            screen.showRegistration(false)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        disposed = true
        main.removeCallbacksAndMessages(null)
        worker.shutdownNow()
        super.onDestroy()
    }

    protected open fun onLoginRequested(
        identifier: String,
        password: String,
        rememberMe: Boolean
    ) {
        Toast.makeText(
            this,
            "Connect your authentication API in onLoginRequested()",
            Toast.LENGTH_LONG
        ).show()
    }

    protected open fun onGoogleLoginRequested() {
        Toast.makeText(
            this,
            "Connect Google Identity in onGoogleLoginRequested()",
            Toast.LENGTH_LONG
        ).show()
    }

    protected open fun onForgotPasswordRequested() {
        Toast.makeText(
            this,
            "Connect your password recovery screen",
            Toast.LENGTH_SHORT
        ).show()
    }

    protected open fun onSignUpRequested() {
        screen.showRegistration(true)
    }

    protected open fun onCreateAccountRequested(
        fullName: String,
        email: String,
        phoneNumber: String,
        password: String,
        countryCode: String,
        acceptedTerms: Boolean
    ) {
        Toast.makeText(
            this,
            "Connect your registration API in onCreateAccountRequested()",
            Toast.LENGTH_LONG
        ).show()
    }

    protected open fun onTermsOfServiceRequested() {
        Toast.makeText(this, "Connect your Terms of Service page", Toast.LENGTH_SHORT).show()
    }

    protected open fun onPrivacyPolicyRequested() {
        Toast.makeText(this, "Connect your Privacy Policy page", Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(screen.windowToken, 0)
    }

    private fun submit() {
        val identifier = screen.email.text.toString().trim()
        val password = screen.password.text.toString()
        if (identifier.isEmpty()) {
            screen.email.error = "Enter your email or phone"
            screen.email.requestFocus()
            return
        }
        if (password.isEmpty()) {
            screen.password.error = "Enter your password"
            screen.password.requestFocus()
            return
        }
        hideKeyboard()
        onLoginRequested(identifier, password, screen.remember)
    }

    private fun submitRegistration() {
        val name = screen.fullName.text.toString().trim()
        val email = screen.registrationEmail.text.toString().trim()
        val phone = screen.phone.text.toString().trim()
        val password = screen.registrationPassword.text.toString()
        if (name.isEmpty()) {
            screen.fullName.error = "Enter your full name"
            screen.fullName.requestFocus()
            return
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            screen.registrationEmail.error = "Enter a valid email address"
            screen.registrationEmail.requestFocus()
            return
        }
        if (phone.isEmpty()) {
            screen.phone.error = "Enter your phone number"
            screen.phone.requestFocus()
            return
        }
        if (password.isEmpty()) {
            screen.registrationPassword.error = "Create a password"
            screen.registrationPassword.requestFocus()
            return
        }
        if (screen.countryCode.isBlank()) {
            hideKeyboard()
            screen.selectCountry()
            return
        }
        if (!screen.termsAccepted) {
            Toast.makeText(
                this,
                "Please agree to the Terms of Service and Privacy Policy",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        hideKeyboard()
        onCreateAccountRequested(
            fullName = name,
            email = email,
            phoneNumber = phone,
            password = password,
            countryCode = screen.countryCode,
            acceptedTerms = screen.termsAccepted
        )
    }

    private fun updatePlatformColors(registration: Boolean) {
        val color = if (registration) SIGNUP_BACKGROUND else PLATFORM_BACKGROUND
        container.setBackgroundColor(color)
        window.statusBarColor = color
        window.navigationBarColor = color
        window.setBackgroundDrawable(ColorDrawable(color))
    }

    private fun loadHero() {
        if (downloadStarted || disposed) return
        downloadStarted = true
        worker.execute {
            var result: Bitmap? = null
            val cache = File(cacheDir, "qubex_login_hero_v1.png")
            try {
                if (cache.exists()) {
                    result = decodeImage(cache)
                    if (result == null) cache.delete()
                }
                if (result == null) {
                    val connection = URL(HERO_URL).openConnection() as HttpURLConnection
                    val temporary = File(cacheDir, "qubex_login_hero_v1.part")
                    try {
                        connection.connectTimeout = 15000
                        connection.readTimeout = 20000
                        connection.instanceFollowRedirects = true
                        connection.setRequestProperty("User-Agent", "QubexAndroid/1.0")
                        connection.setRequestProperty("Accept", "image/*")
                        check(connection.responseCode in 200..299) { "Image HTTP error" }
                        connection.inputStream.use { input ->
                            FileOutputStream(temporary).use { output ->
                                val buffer = ByteArray(8192)
                                var total = 0
                                while (true) {
                                    if (Thread.currentThread().isInterrupted) {
                                        throw InterruptedException()
                                    }
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    total += count
                                    check(total <= 20 * 1024 * 1024) { "Image is too large" }
                                    output.write(buffer, 0, count)
                                }
                            }
                        }
                        result = decodeImage(temporary)
                        check(result != null) { "Invalid image" }
                        if (!temporary.renameTo(cache)) temporary.delete()
                    } finally {
                        connection.disconnect()
                        temporary.delete()
                    }
                }
            } catch (_: Exception) {
                // إعادة التحميل متاحة عند فشل الصورة.
            }
            val bitmap = result
            main.post {
                if (!disposed) {
                    downloadStarted = false
                    screen.hero = bitmap
                    screen.heroFailed = bitmap == null
                    screen.invalidate()
                }
            }
        }
    }

    private fun decodeImage(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private inner class LoginScreen(context: Context) : FrameLayout(context) {
        private val white = Color.rgb(245, 247, 252)
        private val secondary = Color.rgb(180, 190, 207)
        private val muted = Color.rgb(128, 143, 164)

        // نفس الهوية اللونية للصفحتين دون تغيير وظائفهما.
        private val loginAccent = Color.rgb(142, 96, 255)
        private val loginInputFill = Color.rgb(8, 15, 33)
        private val loginInputBorder = Color.rgb(32, 39, 59)
        private val registrationAccent = Color.rgb(142, 96, 255)
        private val registrationInputFill = Color.rgb(8, 15, 33)
        private val registrationInputBorder = Color.rgb(32, 39, 59)
        private val iconColor = Color.rgb(165, 182, 209)
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val regular = Typeface.create("sans-serif", Typeface.NORMAL)
        private val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        private var factor = 1f
        private var offsetX = 0f
        private var offsetY = 0f
        private var revealPassword = false
        private var revealRegistrationPassword = false
        private var loginPressed = false
        private var registrationPressed = false
        var hero: Bitmap? = null
        var heroFailed = false
        var registrationVisible = false
            private set

        var countryCode: String = ""
            set(value) {
                field = value
                updateCountryDescription()
                invalidate()
            }
        var remember = true
            set(value) {
                field = value
                updateRememberDescription()
                invalidate()
            }
        var termsAccepted = true
            set(value) {
                field = value
                updateTermsDescription()
                invalidate()
            }

        private val backgroundColor: Int
            get() = if (registrationVisible) SIGNUP_BACKGROUND else PLATFORM_BACKGROUND
        private val inputFill: Int
            get() = if (registrationVisible) registrationInputFill else loginInputFill
        private val inputBorder: Int
            get() = if (registrationVisible) registrationInputBorder else loginInputBorder

        val email = field(
            "Enter your email or phone",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            EditorInfo.IME_ACTION_NEXT, "Email or Phone", View.AUTOFILL_HINT_USERNAME
        )
        val password = field(
            "Enter your password",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            EditorInfo.IME_ACTION_DONE, "Password", View.AUTOFILL_HINT_PASSWORD,
            secret = true
        )
        val fullName = field(
            "Enter your full name",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME or
                InputType.TYPE_TEXT_FLAG_CAP_WORDS,
            EditorInfo.IME_ACTION_NEXT, "Full Name", View.AUTOFILL_HINT_NAME
        )
        val registrationEmail = field(
            "Enter your email address",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            EditorInfo.IME_ACTION_NEXT, "Email", View.AUTOFILL_HINT_EMAIL_ADDRESS
        )
        val phone = field(
            "Enter your phone number", InputType.TYPE_CLASS_PHONE,
            EditorInfo.IME_ACTION_NEXT, "Phone Number", View.AUTOFILL_HINT_PHONE
        )
        val registrationPassword = field(
            "Create a password",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            EditorInfo.IME_ACTION_DONE, "Create a password", View.AUTOFILL_HINT_PASSWORD,
            secret = true
        )

        // الصورة نفسها مرسومة دون إطار أو Ripple أو قص بزوايا مستديرة.
        private val retry = View(context).apply {
            contentDescription = "Reload artwork"
            background = null
            setOnClickListener { if (heroFailed) loadHero() }
        }
        private val toggle = hit("Show password") {
            revealPassword = !revealPassword
            setPasswordVisibility(password, revealPassword)
            updateToggleDescriptions()
        }
        private val checkbox = hit("Remember me, checked") { remember = !remember }
        private val forgot = hit("Forgot password") { onForgotPasswordRequested() }
        private val login = hit("Log In") { submit() }
        private val google = hit("Continue with Google") { onGoogleLoginRequested() }
        private val signup = hit("Create Account") {
            hideKeyboard()
            onSignUpRequested()
        }
        private val back = hit("Back to Log In") { showRegistration(false) }
        private val registrationToggle = hit("Show password") {
            revealRegistrationPassword = !revealRegistrationPassword
            setPasswordVisibility(registrationPassword, revealRegistrationPassword)
            updateToggleDescriptions()
        }
        private val country = hit("Select your country") {
            hideKeyboard()
            selectCountry()
        }
        private val agreement = hit("Terms accepted, checked") {
            termsAccepted = !termsAccepted
        }
        private val terms = hit("Terms of Service") { onTermsOfServiceRequested() }
        private val privacy = hit("Privacy Policy") { onPrivacyPolicyRequested() }
        private val createAccount = hit("Create Account") { submitRegistration() }
        private val registrationGoogle = hit("Continue with Google") {
            onGoogleLoginRequested()
        }
        private val returnToLogin = hit("Already have an account? Log In") {
            showRegistration(false)
        }

        private val loginViews: List<View> = listOf(
            email, password, retry, toggle, checkbox, forgot, login, google, signup
        )
        private val registrationViews: List<View> = listOf(
            fullName, registrationEmail, phone, registrationPassword, back,
            registrationToggle, country, agreement, terms, privacy,
            createAccount, registrationGoogle, returnToLogin
        )
        private val allFields: List<EditText> = listOf(
            email, password, fullName, registrationEmail, phone, registrationPassword
        )
        private val countryEntries: List<Pair<String, String>> by lazy {
            Locale.getISOCountries()
                .map { code -> code to Locale("", code).getDisplayCountry(Locale.ENGLISH) }
                .sortedBy { it.second }
        }

        init {
            setWillNotDraw(false)
            isFocusableInTouchMode = true
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            clipChildren = true
            clipToPadding = true
            setPadding(0, 0, 0, 0)
            loginViews.forEach { addView(it) }
            registrationViews.forEach {
                addView(it)
                it.visibility = View.GONE
            }
            email.id = View.generateViewId()
            password.id = View.generateViewId()
            fullName.id = View.generateViewId()
            registrationEmail.id = View.generateViewId()
            phone.id = View.generateViewId()
            registrationPassword.id = View.generateViewId()
            email.nextFocusForwardId = password.id
            fullName.nextFocusForwardId = registrationEmail.id
            registrationEmail.nextFocusForwardId = phone.id
            phone.nextFocusForwardId = registrationPassword.id

            email.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    password.requestFocus()
                    true
                } else false
            }
            fullName.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    registrationEmail.requestFocus()
                    true
                } else false
            }
            registrationEmail.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    phone.requestFocus()
                    true
                } else false
            }
            phone.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    registrationPassword.requestFocus()
                    true
                } else false
            }
            password.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submit()
                    true
                } else false
            }
            registrationPassword.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submitRegistration()
                    true
                } else false
            }
            login.setOnTouchListener { _, event ->
                loginPressed = event.actionMasked == MotionEvent.ACTION_DOWN ||
                    event.actionMasked == MotionEvent.ACTION_MOVE
                invalidate()
                false
            }
            createAccount.setOnTouchListener { _, event ->
                registrationPressed = event.actionMasked == MotionEvent.ACTION_DOWN ||
                    event.actionMasked == MotionEvent.ACTION_MOVE
                invalidate()
                false
            }
        }

        fun showRegistration(show: Boolean) {
            hideKeyboard()
            allFields.forEach {
                it.clearFocus()
                it.error = null
            }
            registrationVisible = show
            loginViews.forEach { it.visibility = if (show) View.GONE else View.VISIBLE }
            registrationViews.forEach { it.visibility = if (show) View.VISIBLE else View.GONE }
            loginPressed = false
            registrationPressed = false
            requestFocus()
            updatePlatformColors(show)
            // تبدأ كل صفحة من أعلاها، وليس من موضع التمرير في الصفحة السابقة.
            (parent as? ScrollView)?.scrollTo(0, 0)
            requestLayout()
            invalidate()
        }

        private fun countryName(): String = countryEntries
            .firstOrNull { it.first == countryCode }?.second.orEmpty()

        fun selectCountry() {
            if (isFinishing || disposed) return
            val labels = countryEntries.map { it.second }.toTypedArray()
            val selected = countryEntries.indexOfFirst { it.first == countryCode }
            AlertDialog.Builder(this@LoginActivity)
                .setTitle("Country / Region")
                .setSingleChoiceItems(labels, selected) { dialog, which ->
                    countryCode = countryEntries[which].first
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun setPasswordVisibility(field: EditText, visible: Boolean) {
            val selectionStart = field.selectionStart.coerceIn(0, field.length())
            val selectionEnd = field.selectionEnd.coerceIn(0, field.length())
            field.transformationMethod = if (visible) null
                else PasswordTransformationMethod.getInstance()
            field.setSelection(min(selectionStart, selectionEnd), max(selectionStart, selectionEnd))
            invalidate()
        }

        private fun updateToggleDescriptions() {
            toggle.contentDescription = if (revealPassword) "Hide password" else "Show password"
            registrationToggle.contentDescription =
                if (revealRegistrationPassword) "Hide password" else "Show password"
        }

        private fun updateRememberDescription() {
            checkbox.contentDescription =
                if (remember) "Remember me, checked" else "Remember me, unchecked"
        }

        private fun updateTermsDescription() {
            agreement.contentDescription =
                if (termsAccepted) "Terms accepted, checked" else "Terms accepted, unchecked"
        }

        private fun updateCountryDescription() {
            val name = countryName()
            country.contentDescription =
                if (name.isEmpty()) "Select your country" else "Country / Region, $name"
        }

        private fun field(
            hintText: String,
            type: Int,
            action: Int,
            description: String,
            autofillHint: String,
            secret: Boolean = false
        ): EditText = EditText(context).apply {
            hint = hintText
            typeface = regular
            setTextColor(white)
            setHintTextColor(muted)
            background = null
            setPadding(0, 0, 0, 0)
            gravity = Gravity.CENTER_VERTICAL
            isSingleLine = true
            isSaveEnabled = false
            includeFontPadding = false
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            inputType = type
            imeOptions = action or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            textDirection = View.TEXT_DIRECTION_LTR
            textAlignment = View.TEXT_ALIGNMENT_GRAVITY
            if (secret) transformationMethod = PasswordTransformationMethod.getInstance()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setAutofillHints(autofillHint)
            contentDescription = description
        }

        private fun hit(description: String, action: () -> Unit): View = View(context).apply {
            contentDescription = description
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            background = RippleDrawable(
                android.content.res.ColorStateList.valueOf(Color.argb(35, 170, 190, 220)),
                null,
                ColorDrawable(Color.WHITE)
            )
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val w = MeasureSpec.getSize(widthMeasureSpec)
            // القياس بالعرض فقط: لا أعمدة فارغة يميناً ويساراً، ولا ضغط للصورة.
            factor = (w / DESIGN_WIDTH).coerceAtLeast(0f)
            offsetX = 0f
            offsetY = 0f
            val contentHeight = ceil((DESIGN_HEIGHT * factor).toDouble()).toInt()
            val h = resolveSize(max(contentHeight, suggestedMinimumHeight), heightMeasureSpec)
            setMeasuredDimension(w, h)
            allFields.forEach {
                it.setTextSize(TypedValue.COMPLEX_UNIT_PX, (16f * factor).coerceAtLeast(1f))
            }
            if (registrationVisible) measureRegistration() else measureLogin()
        }

        private fun measureLogin() {
            measureBox(email, 96f, 543f, 447f, 601f)
            measureBox(password, 96f, 655f, 405f, 713f)
            measureBox(retry, 0f, 0f, 491f, 490f)
            measureBox(toggle, 405f, 655f, 461f, 713f)
            measureBox(checkbox, 28f, 728f, 235f, 778f)
            measureBox(forgot, 303f, 728f, 467f, 778f)
            measureBox(login, 34f, 807f, 461f, 869f)
            measureBox(google, 34f, 944f, 461f, 1007f)
            measureBox(signup, 34f, 1027f, 461f, 1090f)
        }

        private fun measureRegistration() {
            measureBox(back, 20f, 6f, 76f, 60f)
            measureBox(fullName, 96f, 326f, 447f, 384f)
            measureBox(registrationEmail, 96f, 429f, 447f, 487f)
            measureBox(phone, 96f, 532f, 447f, 590f)
            measureBox(registrationPassword, 96f, 635f, 405f, 693f)
            measureBox(registrationToggle, 405f, 635f, 461f, 693f)
            measureBox(country, 34f, 738f, 461f, 796f)
            measureBox(agreement, 28f, 807f, 461f, 851f)
            val size = agreementTextSize()
            val prefixWidth = textWidth("I agree to the ", size)
            val termsWidth = textWidth("Terms of Service", size)
            val andWidth = textWidth(" and ", size)
            val privacyWidth = textWidth("Privacy Policy", size)
            val termsStart = 70f + prefixWidth
            val privacyStart = termsStart + termsWidth + andWidth
            measureBox(terms, termsStart, 807f, termsStart + termsWidth, 851f)
            measureBox(privacy, privacyStart, 807f, privacyStart + privacyWidth, 851f)
            measureBox(createAccount, 34f, 874f, 461f, 935f)
            measureBox(registrationGoogle, 34f, 1001f, 461f, 1063f)
            measureBox(returnToLogin, 34f, 1071f, 461f, 1118f)
        }

        private fun measureBox(view: View, l: Float, t: Float, r: Float, b: Float) {
            view.tag = floatArrayOf(l, t, r, b)
            view.measure(
                MeasureSpec.makeMeasureSpec(((r - l) * factor).roundToInt().coerceAtLeast(0), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(((b - t) * factor).roundToInt().coerceAtLeast(0), MeasureSpec.EXACTLY)
            )
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child.visibility == View.GONE) continue
                val box = child.tag as? FloatArray ?: continue
                val x = (offsetX + box[0] * factor).roundToInt()
                val y = (offsetY + box[1] * factor).roundToInt()
                child.layout(x, y, x + child.measuredWidth, y + child.measuredHeight)
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(backgroundColor)
            if (factor <= 0f) return
            val save = canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(factor, factor)
            canvas.clipRect(0f, 0f, DESIGN_WIDTH, height / factor)
            if (registrationVisible) drawRegistration(canvas) else drawLoginScreen(canvas)
            canvas.restoreToCount(save)
        }

        private fun drawLoginScreen(canvas: Canvas) {
            drawHero(canvas)
            text(canvas, "Email or Phone", 34f, 532f, 17f, secondary)
            inputBox(canvas, 542f)
            envelope(canvas, 55f, 564f)
            text(canvas, "Password", 34f, 643f, 17f, secondary)
            inputBox(canvas, 654f)
            lock(canvas, 56f, 674f)
            eye(canvas, 429f, 684f, revealPassword)
            drawRemember(canvas)
            drawPrimaryButton(canvas, 807f, 62f, "Log In", purple = true, pressed = loginPressed)
            drawDivider(canvas, 907f)
            drawGoogle(canvas, 944f, 63f)
            drawCreateAccount(canvas)
        }

        private fun drawHero(canvas: Canvas) {
            val bitmap = hero
            if (bitmap != null) {
                // العرض الكامل، مع الحفاظ على النسبة الأصلية ودون إطار أو زوايا قص.
                drawBitmapFit(canvas, bitmap, null, RectF(0f, 0f, 491f, 491f))

                // دمج لطيف مع الخلفية الموحدة لإخفاء الحد المستطيل للصورة.
                drawHeroBlend(canvas, RectF(0f, 0f, 491f, 18f), 0f, 0f, 0f, 18f,
                    PLATFORM_BACKGROUND, Color.TRANSPARENT)
                drawHeroBlend(canvas, RectF(0f, 0f, 18f, 491f), 0f, 0f, 18f, 0f,
                    PLATFORM_BACKGROUND, Color.TRANSPARENT)
                drawHeroBlend(canvas, RectF(473f, 0f, 491f, 491f), 473f, 0f, 491f, 0f,
                    Color.TRANSPARENT, PLATFORM_BACKGROUND)
                drawHeroBlend(canvas, RectF(0f, 407f, 491f, 497f), 0f, 407f, 0f, 491f,
                    Color.TRANSPARENT, PLATFORM_BACKGROUND)
            } else {
                text(canvas, "QUBEX", 245.5f, 202f, 35f, white, center = true, bold = true)
                text(canvas, if (heroFailed) "Tap to reload artwork" else "Loading artwork…",
                    245.5f, 260f, 16f, secondary, center = true)
            }
        }

        private fun drawHeroBlend(
            canvas: Canvas, bounds: RectF,
            x0: Float, y0: Float, x1: Float, y1: Float,
            startColor: Int, endColor: Int
        ) {
            paint.style = Paint.Style.FILL
            paint.alpha = 255
            paint.shader = LinearGradient(x0, y0, x1, y1, startColor, endColor, Shader.TileMode.CLAMP)
            canvas.drawRect(bounds, paint)
            paint.shader = null
        }

        private fun drawRegistration(canvas: Canvas) {
            drawBackArrow(canvas)
            drawRegistrationBrand(canvas)
            text(canvas, "Create Account", 245.5f, 225f, 31f, white, center = true, bold = true)
            text(canvas, "Join millions of traders worldwide", 245.5f, 259f, 18f, secondary, center = true)
            text(canvas, "Full Name", 34f, 314f, 16f, secondary)
            inputBox(canvas, 326f, 58f)
            person(canvas, 65f, 355f)
            text(canvas, "Email", 34f, 417f, 16f, secondary)
            inputBox(canvas, 429f, 58f)
            envelope(canvas, 55f, 450f)
            text(canvas, "Phone Number", 34f, 520f, 16f, secondary)
            inputBox(canvas, 532f, 58f)
            phoneIcon(canvas, 55f, 550f)
            text(canvas, "Password", 34f, 623f, 16f, secondary)
            inputBox(canvas, 635f, 58f)
            lock(canvas, 56f, 654f)
            eye(canvas, 429f, 664f, revealRegistrationPassword)
            text(canvas, "Country / Region", 34f, 726f, 16f, secondary)
            inputBox(canvas, 738f, 58f)
            globe(canvas, 65f, 767f)
            val selectedCountry = countryName()
            drawFittedText(
                canvas, if (selectedCountry.isEmpty()) "Select your country" else selectedCountry,
                96f, 773f, 301f, 16f, if (selectedCountry.isEmpty()) muted else white
            )
            chevron(canvas, 432f, 767f)
            drawAgreement(canvas)
            drawPrimaryButton(canvas, 874f, 61f, "Create Account", purple = true, pressed = registrationPressed)
            drawDivider(canvas, 968f)
            drawGoogle(canvas, 1001f, 62f)
            drawInlineCentered(canvas, "Already have an account? ", "Log In", 1101f, 16f,
                secondary, registrationAccent)
        }

        private fun drawRegistrationBrand(canvas: Canvas) {
            val bitmap = hero
            if (bitmap != null) {
                // نفس موضع اقتصاص الرمز في النسخة الأصلية.
                val source = Rect(
                    (bitmap.width * 0.395f).roundToInt(),
                    (bitmap.height * 0.150f).roundToInt(),
                    (bitmap.width * 0.622f).roundToInt(),
                    (bitmap.height * 0.395f).roundToInt()
                )
                drawBitmapFit(canvas, bitmap, source, RectF(205.5f, 45f, 285.5f, 132f))
            } else {
                drawFallbackQ(canvas)
            }
            drawSpacedWord(canvas, "QUBEX", 245.5f, 165f, 33f, 3f)
        }

        private fun drawBitmapFit(canvas: Canvas, bitmap: Bitmap, source: Rect?, destination: RectF) {
            val sourceWidth = source?.width()?.toFloat() ?: bitmap.width.toFloat()
            val sourceHeight = source?.height()?.toFloat() ?: bitmap.height.toFloat()
            if (sourceWidth <= 0f || sourceHeight <= 0f) return
            val fit = min(destination.width() / sourceWidth, destination.height() / sourceHeight)
            val width = sourceWidth * fit
            val height = sourceHeight * fit
            val target = RectF(
                destination.centerX() - width / 2f, destination.centerY() - height / 2f,
                destination.centerX() + width / 2f, destination.centerY() + height / 2f
            )
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.alpha = 255
            paint.color = Color.WHITE
            canvas.drawBitmap(bitmap, source, target, paint)
        }

        private fun drawFallbackQ(canvas: Canvas) {
            paint.style = Paint.Style.FILL
            paint.alpha = 255
            paint.shader = LinearGradient(
                207f, 46f, 286f, 132f,
                intArrayOf(Color.rgb(78, 51, 232), Color.rgb(150, 57, 255)),
                null, Shader.TileMode.CLAMP
            )
            val path = Path().apply {
                fillType = Path.FillType.EVEN_ODD
                moveTo(245.5f, 45f)
                lineTo(282f, 66f)
                lineTo(282f, 109f)
                lineTo(245.5f, 130f)
                lineTo(209f, 109f)
                lineTo(209f, 66f)
                close()
                moveTo(245.5f, 66f)
                lineTo(264f, 77f)
                lineTo(264f, 98f)
                lineTo(245.5f, 109f)
                lineTo(227f, 98f)
                lineTo(227f, 77f)
                close()
            }
            canvas.drawPath(path, paint)
            val tail = Path().apply {
                moveTo(245f, 94f)
                lineTo(284f, 116f)
                lineTo(284f, 133f)
                lineTo(245f, 111f)
                close()
            }
            canvas.drawPath(tail, paint)
            paint.shader = null
        }

        private fun drawBackArrow(canvas: Canvas) {
            stroke(white, 1.9f)
            canvas.drawPath(Path().apply {
                moveTo(50f, 24f)
                lineTo(41f, 33f)
                lineTo(50f, 42f)
                moveTo(42f, 33f)
                lineTo(59f, 33f)
            }, paint)
        }

        private fun drawRemember(canvas: Canvas) {
            drawCheckBox(canvas, 34f, 737f, 27f, remember, purple = true)
            text(canvas, "Remember me", 71f, 757f, 16f, secondary)
            text(canvas, "Forgot password?", 326f, 757f, 16f, loginAccent)
        }

        private fun agreementTextSize(): Float {
            val naturalWidth = textWidth("I agree to the Terms of Service and Privacy Policy", 14f)
            return if (naturalWidth > 391f) 14f * 391f / naturalWidth else 14f
        }

        private fun drawAgreement(canvas: Canvas) {
            drawCheckBox(canvas, 34f, 817f, 25f, termsAccepted, purple = true)
            val size = agreementTextSize()
            var x = 70f
            val prefix = "I agree to the "
            val termsLabel = "Terms of Service"
            val join = " and "
            val privacyLabel = "Privacy Policy"
            text(canvas, prefix, x, 835f, size, secondary)
            x += textWidth(prefix, size)
            text(canvas, termsLabel, x, 835f, size, registrationAccent)
            x += textWidth(termsLabel, size)
            text(canvas, join, x, 835f, size, secondary)
            x += textWidth(join, size)
            text(canvas, privacyLabel, x, 835f, size, registrationAccent)
        }

        private fun drawCheckBox(
            canvas: Canvas, x: Float, y: Float, size: Float,
            checked: Boolean, purple: Boolean
        ) {
            paint.style = Paint.Style.FILL
            paint.alpha = 255
            paint.shader = if (checked) LinearGradient(
                x, y, x + size, y + size,
                if (purple) intArrayOf(Color.rgb(119, 66, 248), Color.rgb(151, 53, 255))
                else intArrayOf(Color.rgb(105, 128, 163), Color.rgb(65, 84, 113)),
                null, Shader.TileMode.CLAMP
            ) else null
            paint.color = if (checked) {
                if (purple) registrationAccent else loginAccent
            } else inputFill
            val bounds = RectF(x, y, x + size, y + size)
            canvas.drawRoundRect(bounds, 6f, 6f, paint)
            paint.shader = null
            if (checked) {
                stroke(white, 2.5f)
                canvas.drawPath(Path().apply {
                    moveTo(x + size * 0.26f, y + size * 0.49f)
                    lineTo(x + size * 0.45f, y + size * 0.68f)
                    lineTo(x + size * 0.76f, y + size * 0.30f)
                }, paint)
            } else {
                stroke(inputBorder, 1.2f)
                canvas.drawRoundRect(bounds, 6f, 6f, paint)
            }
        }

        private fun drawPrimaryButton(
            canvas: Canvas, y: Float, height: Float, label: String,
            purple: Boolean, pressed: Boolean
        ) {
            paint.style = Paint.Style.FILL
            paint.shader = LinearGradient(
                34f, y, 461f, y + height,
                if (purple) intArrayOf(
                    Color.rgb(104, 67, 241), Color.rgb(94, 60, 232), Color.rgb(84, 50, 220)
                ) else intArrayOf(
                    Color.rgb(78, 100, 135), Color.rgb(65, 85, 116), Color.rgb(48, 66, 94)
                ),
                floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
            )
            paint.alpha = if (pressed) 200 else 255
            val bounds = RectF(34f, y, 461f, y + height)
            canvas.drawRoundRect(bounds, 17f, 17f, paint)
            paint.alpha = 255
            paint.shader = null
            centeredTextInBox(canvas, label, bounds, 19f, white)
        }

        private fun drawDivider(canvas: Canvas, y: Float) {
            stroke(Color.rgb(39, 46, 65), 1f)
            canvas.drawLine(34f, y, 226f, y, paint)
            canvas.drawLine(269f, y, 461f, y, paint)
            text(canvas, "or", 247f, y + 5f, 16f, secondary, center = true)
        }

        private fun drawGoogle(canvas: Canvas, y: Float, height: Float) {
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = inputFill
            paint.alpha = 255
            val bounds = RectF(34f, y, 461f, y + height)
            canvas.drawRoundRect(bounds, 16f, 16f, paint)
            stroke(Color.rgb(63, 72, 94), 1.1f)
            canvas.drawRoundRect(bounds, 16f, 16f, paint)
            val label = "Continue with Google"
            val labelWidth = textWidth(label, 17f)
            val iconWidth = 25f
            val gap = 16f
            val start = (DESIGN_WIDTH - (iconWidth + gap + labelWidth)) / 2f
            googleMark(canvas, start + iconWidth / 2f, bounds.centerY())
            paint.typeface = regular
            paint.textSize = 17f
            val metrics = paint.fontMetrics
            val baseline = bounds.centerY() - (metrics.ascent + metrics.descent) / 2f
            text(canvas, label, start + iconWidth + gap, baseline, 17f, white)
        }

        private fun drawCreateAccount(canvas: Canvas) {
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.alpha = 255
            paint.color = Color.rgb(12, 18, 38)
            val bounds = RectF(34f, 1027f, 461f, 1090f)
            canvas.drawRoundRect(bounds, 16f, 16f, paint)
            stroke(Color.rgb(83, 66, 127), 1.2f)
            canvas.drawRoundRect(bounds, 16f, 16f, paint)
            text(canvas, "Create Account", 247.5f, 1065f, 18f, white, center = true, bold = true)
        }

        private fun inputBox(canvas: Canvas, y: Float, height: Float = 59f) {
            val bounds = RectF(34f, y, 461f, y + height)
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.alpha = 255
            paint.color = inputFill
            canvas.drawRoundRect(bounds, 15f, 15f, paint)
            stroke(inputBorder, 1.2f)
            canvas.drawRoundRect(bounds, 15f, 15f, paint)
        }

        private fun textWidth(value: String, size: Float, bold: Boolean = false): Float {
            paint.typeface = if (bold) medium else regular
            paint.textSize = size
            return paint.measureText(value)
        }

        private fun text(
            canvas: Canvas, value: String, x: Float, y: Float, size: Float,
            color: Int, center: Boolean = false, bold: Boolean = false
        ) {
            paint.shader = null
            paint.alpha = 255
            paint.style = Paint.Style.FILL
            paint.color = color
            paint.textSize = size
            paint.typeface = if (bold) medium else regular
            paint.textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT
            canvas.drawText(value, x, y, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        private fun centeredTextInBox(canvas: Canvas, value: String, box: RectF, size: Float, color: Int) {
            paint.typeface = regular
            paint.textSize = size
            val metrics = paint.fontMetrics
            val baseline = box.centerY() - (metrics.ascent + metrics.descent) / 2f
            text(canvas, value, box.centerX(), baseline, size, color, center = true)
        }

        private fun drawInlineCentered(
            canvas: Canvas, first: String, second: String, baseline: Float,
            size: Float, firstColor: Int, secondColor: Int
        ) {
            val firstWidth = textWidth(first, size)
            val secondWidth = textWidth(second, size)
            val start = (DESIGN_WIDTH - firstWidth - secondWidth) / 2f
            text(canvas, first, start, baseline, size, firstColor)
            text(canvas, second, start + firstWidth, baseline, size, secondColor)
        }

        private fun drawSpacedWord(
            canvas: Canvas, value: String, centerX: Float,
            baseline: Float, size: Float, spacing: Float
        ) {
            var width = 0f
            value.forEach { width += textWidth(it.toString(), size, bold = true) }
            width += spacing * (value.length - 1).coerceAtLeast(0)
            var x = centerX - width / 2f
            value.forEach { character ->
                val letter = character.toString()
                text(canvas, letter, x, baseline, size, white, bold = true)
                x += textWidth(letter, size, bold = true) + spacing
            }
        }

        private fun drawFittedText(
            canvas: Canvas, value: String, x: Float, baseline: Float,
            maxWidth: Float, size: Float, color: Int
        ) {
            val measured = textWidth(value, size)
            val fittedSize = if (measured > maxWidth && measured > 0f) size * maxWidth / measured else size
            text(canvas, value, x, baseline, fittedSize, color)
        }

        private fun stroke(color: Int, width: Float) {
            paint.shader = null
            paint.alpha = 255
            paint.style = Paint.Style.STROKE
            paint.color = color
            paint.strokeWidth = width
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND
        }

        private fun envelope(canvas: Canvas, x: Float, y: Float) {
            stroke(iconColor, 1.7f)
            canvas.drawRoundRect(RectF(x, y, x + 21f, y + 17f), 1.5f, 1.5f, paint)
            canvas.drawPath(Path().apply {
                moveTo(x + 1f, y + 2f)
                lineTo(x + 10.5f, y + 9f)
                lineTo(x + 20f, y + 2f)
            }, paint)
        }

        private fun lock(canvas: Canvas, x: Float, y: Float) {
            stroke(iconColor, 1.8f)
            canvas.drawRoundRect(RectF(x + 1f, y + 8f, x + 18f, y + 21f), 2f, 2f, paint)
            canvas.drawArc(RectF(x + 4f, y - 1f, x + 15f, y + 14f), 180f, 180f, false, paint)
            canvas.drawLine(x + 9.5f, y + 13f, x + 9.5f, y + 17f, paint)
        }

        private fun eye(canvas: Canvas, x: Float, y: Float, revealed: Boolean) {
            stroke(iconColor, 1.7f)
            canvas.drawPath(Path().apply {
                moveTo(x - 10f, y)
                cubicTo(x - 5f, y - 10f, x + 5f, y - 10f, x + 11f, y)
                cubicTo(x + 5f, y + 10f, x - 5f, y + 10f, x - 10f, y)
            }, paint)
            canvas.drawCircle(x + 0.5f, y, 3.5f, paint)
            if (!revealed) canvas.drawLine(x - 8f, y - 8f, x + 10f, y + 9f, paint)
        }

        private fun person(canvas: Canvas, x: Float, y: Float) {
            stroke(iconColor, 1.8f)
            canvas.drawOval(RectF(x - 5f, y - 11f, x + 5f, y), paint)
            canvas.drawPath(Path().apply {
                moveTo(x - 10f, y + 11f)
                cubicTo(x - 10f, y + 1f, x + 10f, y + 1f, x + 10f, y + 11f)
            }, paint)
        }

        private fun phoneIcon(canvas: Canvas, x: Float, y: Float) {
            stroke(iconColor, 1.8f)
            canvas.drawPath(Path().apply {
                moveTo(x + 4f, y + 1f)
                lineTo(x + 8f, y + 6f)
                lineTo(x + 5f, y + 10f)
                cubicTo(x + 7f, y + 14f, x + 10f, y + 17f, x + 14f, y + 18f)
                lineTo(x + 17f, y + 15f)
                lineTo(x + 22f, y + 19f)
                lineTo(x + 20f, y + 23f)
                cubicTo(x + 18f, y + 27f, x + 9f, y + 22f, x + 4f, y + 16f)
                cubicTo(x, y + 11f, x - 1f, y + 5f, x + 1f, y + 2f)
                close()
            }, paint)
        }

        private fun globe(canvas: Canvas, x: Float, y: Float) {
            stroke(iconColor, 1.7f)
            canvas.drawCircle(x, y, 11f, paint)
            canvas.drawOval(RectF(x - 4.5f, y - 11f, x + 4.5f, y + 11f), paint)
            canvas.drawLine(x - 10.5f, y, x + 10.5f, y, paint)
        }

        private fun chevron(canvas: Canvas, x: Float, y: Float) {
            stroke(iconColor, 1.8f)
            canvas.drawPath(Path().apply {
                moveTo(x - 6f, y - 3f)
                lineTo(x, y + 3f)
                lineTo(x + 6f, y - 3f)
            }, paint)
        }

        private fun googleMark(canvas: Canvas, x: Float, y: Float) {
            val rect = RectF(x - 10f, y - 10f, x + 10f, y + 10f)
            stroke(Color.rgb(66, 133, 244), 5f)
            paint.strokeCap = Paint.Cap.BUTT
            canvas.drawArc(rect, 0f, 45f, false, paint)
            paint.color = Color.rgb(52, 168, 83)
            canvas.drawArc(rect, 45f, 100f, false, paint)
            paint.color = Color.rgb(251, 188, 5)
            canvas.drawArc(rect, 145f, 65f, false, paint)
            paint.color = Color.rgb(234, 67, 53)
            canvas.drawArc(rect, 210f, 105f, false, paint)
            paint.color = Color.rgb(66, 133, 244)
            canvas.drawLine(x + 10f, y, x + 1f, y, paint)
        }
    }
}
