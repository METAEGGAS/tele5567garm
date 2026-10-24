package com.qubex.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Suppress("DEPRECATION")
open class LoginActivity : Activity() {

    companion object {
        private const val HERO_URL =
            "https://i.ibb.co/rKvzFXQd/file-0000000016848246a932b72ca8476b79.png"

        private const val STATE_EMAIL = "qubex.email"
        private const val STATE_REMEMBER = "qubex.remember"
        private const val STATE_SIGNUP = "qubex.signup"
        private const val STATE_NAME = "qubex.name"
        private const val STATE_PHONE = "qubex.phone"
        private const val STATE_COUNTRY = "qubex.country"
        private const val STATE_TERMS = "qubex.terms"

        private val PLATFORM_COLOR = Color.rgb(18, 26, 40)
    }

    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    private lateinit var screen: LoginScreen

    private var downloadStarted = false

    @Volatile
    private var disposed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        actionBar?.hide()

        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        window.statusBarColor = PLATFORM_COLOR
        window.navigationBarColor = PLATFORM_COLOR
        window.setBackgroundDrawable(ColorDrawable(PLATFORM_COLOR))

        // تُحسب المساحة المتاحة بعد أشرطة النظام ولوحة المفاتيح.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }

        screen = LoginScreen(this)

        val container = FrameLayout(this).apply {
            setBackgroundColor(PLATFORM_COLOR)

            addView(
                screen,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )

            setOnApplyWindowInsetsListener { view, insets ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val bars = insets.getInsets(
                        WindowInsets.Type.systemBars()
                    )
                    val keyboard = insets.getInsets(
                        WindowInsets.Type.ime()
                    )

                    view.setPadding(
                        bars.left,
                        bars.top,
                        bars.right,
                        max(bars.bottom, keyboard.bottom)
                    )
                } else {
                    view.setPadding(
                        insets.systemWindowInsetLeft,
                        insets.systemWindowInsetTop,
                        insets.systemWindowInsetRight,
                        insets.systemWindowInsetBottom
                    )
                }

                insets
            }
        }

        setContentView(container)
        container.requestApplyInsets()

        savedInstanceState?.let { state ->
            screen.email.setText(state.getString(STATE_EMAIL, ""))
            screen.fullName.setText(state.getString(STATE_NAME, ""))
            screen.phone.setText(state.getString(STATE_PHONE, ""))

            screen.remember = state.getBoolean(
                STATE_REMEMBER,
                true
            )
            screen.acceptedTerms = state.getBoolean(
                STATE_TERMS,
                false
            )
            screen.countryCode = state.getString(
                STATE_COUNTRY,
                ""
            ).orEmpty()

            screen.setSignUpMode(
                state.getBoolean(STATE_SIGNUP, false)
            )
        }

        loadHero()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::screen.isInitialized) {
            outState.putString(
                STATE_EMAIL,
                screen.email.text.toString()
            )
            outState.putString(
                STATE_NAME,
                screen.fullName.text.toString()
            )
            outState.putString(
                STATE_PHONE,
                screen.phone.text.toString()
            )
            outState.putBoolean(
                STATE_REMEMBER,
                screen.remember
            )
            outState.putBoolean(
                STATE_SIGNUP,
                screen.signUpMode
            )
            outState.putBoolean(
                STATE_TERMS,
                screen.acceptedTerms
            )
            outState.putString(
                STATE_COUNTRY,
                screen.countryCode
            )
        }

        // لا تُحفظ كلمة المرور ضمن حالة الصفحة.
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        disposed = true
        main.removeCallbacksAndMessages(null)
        worker.shutdownNow()

        if (::screen.isInitialized) {
            screen.password.text?.clear()
        }

        super.onDestroy()
    }

    override fun onBackPressed() {
        if (::screen.isInitialized && screen.signUpMode) {
            screen.setSignUpMode(false)
        } else {
            super.onBackPressed()
        }
    }

    /*
     * نقاط ربط المصادقة.
     * لا يوجد تخزين لكلمات المرور أو تسجيل لها في السجلات.
     */

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
        screen.setSignUpMode(true)
    }

    protected open fun onCreateAccountRequested(
        fullName: String,
        email: String,
        phoneNumber: String,
        password: String,
        countryCode: String
    ) {
        Toast.makeText(
            this,
            "Connect your registration API in onCreateAccountRequested()",
            Toast.LENGTH_LONG
        ).show()
    }

    protected open fun onTermsRequested() {
        Toast.makeText(
            this,
            "Connect your Terms of Service page",
            Toast.LENGTH_SHORT
        ).show()
    }

    protected open fun onPrivacyRequested() {
        Toast.makeText(
            this,
            "Connect your Privacy Policy page",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun hideKeyboard() {
        val manager = getSystemService(
            Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager

        manager.hideSoftInputFromWindow(screen.windowToken, 0)
    }

    private fun submit() {
        if (screen.signUpMode) {
            submitRegistration()
            return
        }

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

        onLoginRequested(
            identifier,
            password,
            screen.remember
        )
    }

    private fun submitRegistration() {
        val name = screen.fullName.text.toString().trim()
        val email = screen.email.text.toString().trim()
        val phone = screen.phone.text.toString().trim()
        val password = screen.password.text.toString()

        if (name.isEmpty()) {
            screen.fullName.error = "Enter your full name"
            screen.fullName.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            screen.email.error = "Enter a valid email address"
            screen.email.requestFocus()
            return
        }

        if (phone.count { it.isDigit() } < 6) {
            screen.phone.error = "Enter a valid phone number"
            screen.phone.requestFocus()
            return
        }

        if (password.isEmpty()) {
            screen.password.error = "Create a password"
            screen.password.requestFocus()
            return
        }

        if (screen.countryCode.isBlank()) {
            Toast.makeText(
                this,
                "Select your country",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!screen.acceptedTerms) {
            Toast.makeText(
                this,
                "Please accept the Terms of Service and Privacy Policy",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        hideKeyboard()

        onCreateAccountRequested(
            name,
            email,
            phone,
            password,
            screen.countryCode
        )
    }

    private fun loadHero() {
        if (downloadStarted || disposed) return

        downloadStarted = true

        worker.execute {
            var result: Bitmap? = null
            val cache = File(
                cacheDir,
                "qubex_login_hero_v1.png"
            )

            try {
                if (cache.exists()) {
                    result = decodeImage(cache)
                }

                if (result == null && !disposed) {
                    val connection =
                        URL(HERO_URL).openConnection() as HttpURLConnection

                    val temporary = File(
                        cacheDir,
                        "qubex_login_hero_v1.part"
                    )

                    try {
                        connection.connectTimeout = 15000
                        connection.readTimeout = 20000
                        connection.instanceFollowRedirects = true

                        connection.setRequestProperty(
                            "User-Agent",
                            "QubexAndroid/1.0"
                        )
                        connection.setRequestProperty(
                            "Accept",
                            "image/*"
                        )

                        check(connection.responseCode in 200..299) {
                            "Image HTTP error"
                        }

                        connection.inputStream.use { input ->
                            FileOutputStream(temporary).use { output ->
                                val buffer = ByteArray(8192)
                                var total = 0

                                while (true) {
                                    if (
                                        disposed ||
                                        Thread.currentThread().isInterrupted
                                    ) {
                                        throw InterruptedException()
                                    }

                                    val count = input.read(buffer)
                                    if (count < 0) break

                                    total += count

                                    check(total <= 20 * 1024 * 1024) {
                                        "Image is too large"
                                    }

                                    output.write(buffer, 0, count)
                                }
                            }
                        }

                        result = decodeImage(temporary)

                        check(result != null) {
                            "Invalid image"
                        }

                        if (!temporary.renameTo(cache)) {
                            temporary.delete()
                        }
                    } finally {
                        connection.disconnect()
                        temporary.delete()
                    }
                }
            } catch (_: Exception) {
                // تبقى الواجهة قابلة للاستخدام عند فشل الصورة.
            }

            val bitmap = result

            if (!disposed) {
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
    }

    private fun decodeImage(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeFile(
            file.absolutePath,
            bounds
        )

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }

        var sample = 1

        while (
            max(bounds.outWidth, bounds.outHeight) / sample > 2048
        ) {
            sample *= 2
        }

        return BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sample
            }
        )
    }

    private inner class LoginScreen(
        context: Context
    ) : FrameLayout(context) {

        private val backgroundColor = PLATFORM_COLOR
        private val white = Color.rgb(245, 247, 252)
        private val secondary = Color.rgb(181, 189, 205)
        private val muted = Color.rgb(126, 139, 160)
        private val purple = Color.rgb(150, 112, 255)

        private val inputFill = Color.rgb(27, 37, 53)
        private val inputBorder = Color.rgb(54, 67, 86)
        private val iconColor = Color.rgb(165, 180, 207)

        private val paint = Paint(
            Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG
        )

        private val regular = Typeface.create(
            "sans-serif",
            Typeface.NORMAL
        )
        private val medium = Typeface.create(
            "sans-serif-medium",
            Typeface.NORMAL
        )

        private val referenceWidth = 491f
        private val referenceHeight = 1120f

        private var factor = 1f
        private var offsetX = 0f
        private var offsetY = 0f
        private var revealPassword = false

        var hero: Bitmap? = null
        var heroFailed = false

        var signUpMode = false
            private set

        var remember = true
            set(value) {
                field = value
                updateDescriptions()
                invalidate()
            }

        var acceptedTerms = false
            set(value) {
                field = value
                updateDescriptions()
                invalidate()
            }

        var countryCode = ""
            set(value) {
                field = value
                updateDescriptions()
                invalidate()
            }

        val fullName: EditText = field(
            "Enter your full name",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
            View.AUTOFILL_HINT_NAME
        )

        val email: EditText = field(
            "Enter your email or phone",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            View.AUTOFILL_HINT_USERNAME
        )

        val phone: EditText = field(
            "Enter your phone number",
            InputType.TYPE_CLASS_PHONE,
            View.AUTOFILL_HINT_PHONE
        )

        val password: EditText = field(
            "Enter your password",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
            View.AUTOFILL_HINT_PASSWORD,
            true
        )

        private val retry: View = hit("Reload artwork") {
            if (hero == null) loadHero()
        }

        private val toggle: View = hit("Show password") {
            togglePassword()
        }

        private val checkbox: View = hit("Remember me") {
            if (signUpMode) {
                acceptedTerms = !acceptedTerms
            } else {
                remember = !remember
            }
        }

        private val forgot: View = hit("Forgot password") {
            onForgotPasswordRequested()
        }

        private val primary: View = hit("Log In") {
            submit()
        }

        private val google: View = hit("Continue with Google") {
            onGoogleLoginRequested()
        }

        private val switchMode: View = hit("Create Account") {
            if (signUpMode) {
                setSignUpMode(false)
            } else {
                onSignUpRequested()
            }
        }

        private val country: View = hit("Select your country") {
            selectCountry()
        }

        private val back: View = hit("Back to Log In") {
            setSignUpMode(false)
        }

        private val terms: View = hit("Terms of Service") {
            onTermsRequested()
        }

        private val privacy: View = hit("Privacy Policy") {
            onPrivacyRequested()
        }

        init {
            setWillNotDraw(false)
            isFocusableInTouchMode = true

            // يبقى ترتيب النموذج من اليسار إلى اليمين مثل الصورة.
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

            addView(fullName)
            addView(email)
            addView(phone)
            addView(password)
            addView(retry)
            addView(toggle)
            addView(checkbox)
            addView(forgot)
            addView(primary)
            addView(google)
            addView(switchMode)
            addView(country)
            addView(back)
            addView(terms)
            addView(privacy)

            fullName.nextFocusForwardId = email.id
            email.nextFocusForwardId = password.id
            phone.nextFocusForwardId = password.id

            password.imeOptions = EditorInfo.IME_ACTION_DONE
            password.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submit()
                    true
                } else {
                    false
                }
            }

            setSignUpMode(false)
        }

        fun setSignUpMode(enabled: Boolean) {
            val changed = signUpMode != enabled
            signUpMode = enabled

            if (changed) {
                hideKeyboard()
                clearFocus()
                password.text?.clear()
                password.error = null
                email.error = null
                fullName.error = null
                phone.error = null

                revealPassword = false
                password.transformationMethod =
                    PasswordTransformationMethod.getInstance()
            }

            fullName.visibility =
                if (enabled) View.VISIBLE else View.GONE

            phone.visibility =
                if (enabled) View.VISIBLE else View.GONE

            country.visibility =
                if (enabled) View.VISIBLE else View.GONE

            back.visibility =
                if (enabled) View.VISIBLE else View.GONE

            terms.visibility =
                if (enabled) View.VISIBLE else View.GONE

            privacy.visibility =
                if (enabled) View.VISIBLE else View.GONE

            forgot.visibility =
                if (enabled) View.GONE else View.VISIBLE

            retry.visibility =
                if (enabled) View.GONE else View.VISIBLE

            email.hint =
                if (enabled) "Enter your email address"
                else "Enter your email or phone"

            email.contentDescription =
                if (enabled) "Email" else "Email or Phone"

            password.hint =
                if (enabled) "Create a password"
                else "Enter your password"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                email.setAutofillHints(
                    if (enabled) View.AUTOFILL_HINT_EMAIL_ADDRESS
                    else View.AUTOFILL_HINT_USERNAME
                )
            }

            email.nextFocusForwardId =
                if (enabled) phone.id else password.id

            updateDescriptions()
            requestLayout()
            invalidate()
        }

        private fun updateDescriptions() {
            // قد تُستدعى setters أثناء تهيئة الكائن؛ نستخدم
            // الأبناء الموجودين فقط بدل الوصول المبكر للخصائص.
            for (i in 0 until childCount) {
                val child = getChildAt(i)

                when (child.getTag(android.R.id.custom)) {
                    "check" -> {
                        val checked =
                            if (signUpMode) acceptedTerms else remember

                        child.contentDescription =
                            if (signUpMode) {
                                "Accept terms, " +
                                    if (checked) "checked" else "unchecked"
                            } else {
                                "Remember me, " +
                                    if (checked) "checked" else "unchecked"
                            }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            child.stateDescription =
                                if (checked) "Checked" else "Unchecked"
                        }
                    }

                    "toggle" -> {
                        child.contentDescription =
                            if (revealPassword) "Hide password"
                            else "Show password"
                    }

                    "primary" -> {
                        child.contentDescription =
                            if (signUpMode) "Create Account" else "Log In"
                    }

                    "switch" -> {
                        child.contentDescription =
                            if (signUpMode) "Log In" else "Create Account"
                    }

                    "country" -> {
                        child.contentDescription =
                            if (countryCode.isBlank()) {
                                "Select your country"
                            } else {
                                countryName()
                            }
                    }
                }
            }
        }

        private fun togglePassword() {
            revealPassword = !revealPassword

            val cursor = password.selectionStart

            password.transformationMethod =
                if (revealPassword) null
                else PasswordTransformationMethod.getInstance()

            password.setSelection(
                cursor.coerceIn(0, password.length())
            )

            updateDescriptions()
            invalidate()
        }

        private fun field(
            hintText: String,
            type: Int,
            autofillHint: String,
            secret: Boolean = false
        ): EditText {
            return EditText(context).apply {
                id = View.generateViewId()
                hint = hintText
                typeface = regular

                setTextColor(white)
                setHintTextColor(muted)
                setBackgroundColor(Color.TRANSPARENT)
                setPadding(0, 0, 0, 0)

                gravity = Gravity.CENTER_VERTICAL
                textDirection = View.TEXT_DIRECTION_LTR
                isSingleLine = true
                isSaveEnabled = false
                inputType = type

                if (secret) {
                    transformationMethod =
                        PasswordTransformationMethod.getInstance()
                }

                imeOptions =
                    if (secret) EditorInfo.IME_ACTION_DONE
                    else EditorInfo.IME_ACTION_NEXT

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setAutofillHints(autofillHint)
                }

                contentDescription =
                    if (secret) "Password" else hintText
            }
        }

        private fun hit(
            description: String,
            action: () -> Unit
        ): View {
            return View(context).apply {
                contentDescription = description
                isClickable = true
                isFocusable = true

                setOnClickListener {
                    action()
                }

                background = RippleDrawable(
                    ColorStateList.valueOf(
                        Color.argb(38, 160, 132, 255)
                    ),
                    null,
                    ColorDrawable(Color.WHITE)
                )
            }
        }

        private fun countryName(): String {
            return if (countryCode.isBlank()) {
                "Select your country"
            } else {
                Locale("", countryCode).getDisplayCountry(Locale.ENGLISH)
            }
        }

        private fun selectCountry() {
            hideKeyboard()

            val codes = Locale.getISOCountries()
                .sortedBy {
                    Locale("", it).getDisplayCountry(Locale.ENGLISH)
                }

            val names = codes.map {
                Locale("", it).getDisplayCountry(Locale.ENGLISH)
            }.toTypedArray()

            val selected = codes.indexOf(countryCode)

            AlertDialog.Builder(this@LoginActivity)
                .setTitle("Country / Region")
                .setSingleChoiceItems(names, selected) { dialog, which ->
                    countryCode = codes[which]
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int
        ) {
            val w = MeasureSpec.getSize(widthMeasureSpec)
            val h = MeasureSpec.getSize(heightMeasureSpec)

            setMeasuredDimension(w, h)

            // نفس طول التصميم للصفحتين، دون إضافة تمرير.
            factor = min(
                w / referenceWidth,
                h / referenceHeight
            ).coerceAtLeast(0.001f)

            offsetX = (w - referenceWidth * factor) / 2f
            offsetY = (h - referenceHeight * factor) / 2f

            listOf(fullName, email, phone, password).forEach {
                it.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    16f * factor
                )
            }

            checkbox.setTag(android.R.id.custom, "check")
            toggle.setTag(android.R.id.custom, "toggle")
            primary.setTag(android.R.id.custom, "primary")
            switchMode.setTag(android.R.id.custom, "switch")
            country.setTag(android.R.id.custom, "country")

            if (signUpMode) {
                measureBox(back, 22f, 12f, 76f, 66f)

                measureBox(fullName, 96f, 290f, 441f, 348f)
                measureBox(email, 96f, 395f, 441f, 453f)
                measureBox(phone, 96f, 500f, 441f, 558f)
                measureBox(password, 96f, 605f, 405f, 663f)
                measureBox(toggle, 405f, 605f, 461f, 663f)

                measureBox(country, 34f, 710f, 461f, 768f)
                measureBox(checkbox, 26f, 785f, 238f, 833f)
                measureBox(terms, 238f, 786f, 350f, 833f)
                measureBox(privacy, 349f, 786f, 467f, 833f)

                measureBox(primary, 34f, 850f, 461f, 912f)
                measureBox(google, 34f, 966f, 461f, 1028f)
                measureBox(switchMode, 80f, 1043f, 411f, 1099f)
            } else {
                measureBox(retry, 0f, 0f, 491f, 480f)

                measureBox(email, 96f, 518f, 441f, 576f)
                measureBox(password, 96f, 627f, 405f, 685f)
                measureBox(toggle, 405f, 627f, 461f, 685f)

                measureBox(checkbox, 26f, 702f, 242f, 752f)
                measureBox(forgot, 300f, 702f, 467f, 752f)

                measureBox(primary, 34f, 779f, 461f, 841f)
                measureBox(google, 34f, 903f, 461f, 965f)
                measureBox(switchMode, 34f, 1025f, 461f, 1087f)
            }

            updateDescriptions()
        }

        private fun measureBox(
            view: View,
            l: Float,
            t: Float,
            r: Float,
            b: Float
        ) {
            view.tag = floatArrayOf(l, t, r, b)

            view.measure(
                MeasureSpec.makeMeasureSpec(
                    ((r - l) * factor).roundToInt(),
                    MeasureSpec.EXACTLY
                ),
                MeasureSpec.makeMeasureSpec(
                    ((b - t) * factor).roundToInt(),
                    MeasureSpec.EXACTLY
                )
            )
        }

        override fun onLayout(
            changed: Boolean,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int
        ) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)

                if (child.visibility == View.GONE) continue

                val box = child.tag as? FloatArray ?: continue

                val x = (offsetX + box[0] * factor).roundToInt()
                val y = (offsetY + box[1] * factor).roundToInt()

                child.layout(
                    x,
                    y,
                    x + child.measuredWidth,
                    y + child.measuredHeight
                )
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            canvas.drawColor(backgroundColor)

            val save = canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(factor, factor)

            if (signUpMode) {
                drawRegistration(canvas)
            } else {
                drawLogin(canvas)
            }

            canvas.restoreToCount(save)
        }

        private fun drawLogin(c: Canvas) {
            drawHero(c)

            text(c, "Email or Phone", 34f, 506f, 17f, secondary)
            inputBox(c, 518f)
            envelope(c, 55f, 539f)

            text(c, "Password", 34f, 615f, 17f, secondary)
            inputBox(c, 627f)
            lock(c, 56f, 646f)
            eye(c, 429f, 656f)

            drawCheckbox(c, 34f, 714f, remember)
            text(c, "Remember me", 71f, 734f, 16f, secondary)
            text(c, "Forgot password?", 326f, 734f, 16f, purple)

            primaryButton(c, 779f, "Log In")
            divider(c, 873f)
            googleButton(c, 903f)

            text(
                c,
                "Don’t have an account?",
                245.5f,
                1006f,
                16f,
                secondary,
                center = true
            )

            outlineButton(c, 1025f, "Create Account")
        }

        private fun drawRegistration(c: Canvas) {
            stroke(white, 2f)

            c.drawPath(Path().apply {
                moveTo(58f, 26f)
                lineTo(47f, 37f)
                lineTo(58f, 48f)
                moveTo(48f, 37f)
                lineTo(68f, 37f)
            }, paint)

            drawBrand(c)

            text(
                c,
                "Create Account",
                245.5f,
                213f,
                29f,
                white,
                center = true,
                bold = true
            )

            text(
                c,
                "Join millions of traders worldwide",
                245.5f,
                245f,
                17f,
                secondary,
                center = true
            )

            text(c, "Full Name", 34f, 278f, 16f, secondary)
            inputBox(c, 290f)
            person(c, 65f, 319f)

            text(c, "Email", 34f, 383f, 16f, secondary)
            inputBox(c, 395f)
            envelope(c, 55f, 416f)

            text(c, "Phone Number", 34f, 488f, 16f, secondary)
            inputBox(c, 500f)
            phoneIcon(c, 65f, 529f)

            text(c, "Password", 34f, 593f, 16f, secondary)
            inputBox(c, 605f)
            lock(c, 56f, 624f)
            eye(c, 429f, 634f)

            text(c, "Country / Region", 34f, 698f, 16f, secondary)
            inputBox(c, 710f)
            globe(c, 65f, 739f)

            val label = countryName()

            paint.typeface = regular
            paint.textSize = 16f

            val fittedLabel = android.text.TextUtils.ellipsize(
                label,
                android.text.TextPaint(paint),
                300f,
                android.text.TextUtils.TruncateAt.END
            ).toString()

            text(
                c,
                fittedLabel,
                96f,
                745f,
                16f,
                if (countryCode.isBlank()) muted else white
            )

            stroke(iconColor, 1.8f)
            c.drawPath(Path().apply {
                moveTo(427f, 736f)
                lineTo(433f, 742f)
                lineTo(439f, 736f)
            }, paint)

            drawCheckbox(c, 34f, 796f, acceptedTerms)

            text(c, "I agree to the", 71f, 815f, 14f, secondary)
            text(c, "Terms of Service", 244f, 815f, 13f, purple)
            text(c, "&", 345f, 815f, 13f, secondary)
            text(c, "Privacy Policy", 360f, 815f, 13f, purple)

            primaryButton(c, 850f, "Create Account")
            divider(c, 939f)
            googleButton(c, 966f)

            centeredPair(
                c,
                "Already have an account? ",
                "Log In",
                1078f
            )
        }

        private fun drawHero(c: Canvas) {
            val bitmap = hero

            if (bitmap != null) {
                val fit = min(
                    491f / bitmap.width,
                    480f / bitmap.height
                )

                val bw = bitmap.width * fit
                val bh = bitmap.height * fit

                paint.reset()
                paint.isAntiAlias = true
                paint.isFilterBitmap = true

                c.drawBitmap(
                    bitmap,
                    null,
                    RectF(
                        (491f - bw) / 2f,
                        (480f - bh) / 2f,
                        (491f + bw) / 2f,
                        (480f + bh) / 2f
                    ),
                    paint
                )

                paint.style = Paint.Style.FILL
                paint.shader = LinearGradient(
                    0f,
                    372f,
                    0f,
                    481f,
                    intArrayOf(
                        Color.TRANSPARENT,
                        backgroundColor
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )

                c.drawRect(0f, 372f, 491f, 481f, paint)
                paint.shader = null
            } else {
                drawBrand(c)

                text(
                    c,
                    "Trade Global Markets",
                    245.5f,
                    254f,
                    20f,
                    secondary,
                    center = true
                )

                text(
                    c,
                    "Anytime, Anywhere.",
                    245.5f,
                    283f,
                    20f,
                    secondary,
                    center = true
                )

                text(
                    c,
                    if (heroFailed) "Tap to reload artwork"
                    else "Loading artwork…",
                    245.5f,
                    373f,
                    15f,
                    muted,
                    center = true
                )
            }
        }

        private fun drawBrand(c: Canvas) {
            paint.shader = LinearGradient(
                210f,
                53f,
                283f,
                130f,
                intArrayOf(
                    Color.rgb(105, 76, 255),
                    Color.rgb(163, 74, 255)
                ),
                null,
                Shader.TileMode.CLAMP
            )
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 15f
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeCap = Paint.Cap.BUTT
            paint.alpha = 255

            c.drawPath(Path().apply {
                moveTo(245.5f, 57f)
                lineTo(275f, 74f)
                lineTo(275f, 109f)
                lineTo(245.5f, 126f)
                lineTo(216f, 109f)
                lineTo(216f, 74f)
                close()
            }, paint)

            paint.shader = null
            stroke(backgroundColor, 7f)
            c.drawLine(246f, 96f, 284f, 119f, paint)

            stroke(purple, 12f)
            c.drawLine(249f, 102f, 281f, 121f, paint)

            text(
                c,
                "QUBEX",
                245.5f,
                169f,
                33f,
                white,
                center = true,
                bold = true
            )
        }

        private fun inputBox(c: Canvas, y: Float) {
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = inputFill
            paint.alpha = 255

            c.drawRoundRect(
                RectF(34f, y, 461f, y + 58f),
                15f,
                15f,
                paint
            )

            stroke(inputBorder, 1.1f)

            c.drawRoundRect(
                RectF(34f, y, 461f, y + 58f),
                15f,
                15f,
                paint
            )
        }

        private fun primaryButton(
            c: Canvas,
            y: Float,
            title: String
        ) {
            paint.style = Paint.Style.FILL
            paint.alpha = 255

            paint.shader = LinearGradient(
                34f,
                y,
                461f,
                y + 62f,
                intArrayOf(
                    Color.rgb(111, 79, 242),
                    Color.rgb(87, 68, 211)
                ),
                null,
                Shader.TileMode.CLAMP
            )

            c.drawRoundRect(
                RectF(34f, y, 461f, y + 62f),
                16f,
                16f,
                paint
            )

            paint.shader = null

            text(
                c,
                title,
                247.5f,
                y + 38f,
                18f,
                white,
                center = true,
                bold = true
            )
        }

        private fun outlineButton(
            c: Canvas,
            y: Float,
            title: String
        ) {
            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(31, 40, 60)
            paint.shader = null

            c.drawRoundRect(
                RectF(34f, y, 461f, y + 62f),
                16f,
                16f,
                paint
            )

            stroke(Color.rgb(112, 92, 167), 1.2f)

            c.drawRoundRect(
                RectF(34f, y, 461f, y + 62f),
                16f,
                16f,
                paint
            )

            text(
                c,
                title,
                247.5f,
                y + 38f,
                18f,
                purple,
                center = true,
                bold = true
            )
        }

        private fun divider(c: Canvas, y: Float) {
            stroke(Color.rgb(59, 70, 89), 1f)

            c.drawLine(34f, y, 226f, y, paint)
            c.drawLine(269f, y, 461f, y, paint)

            text(
                c,
                "or",
                247f,
                y + 5f,
                16f,
                secondary,
                center = true
            )
        }

        private fun googleButton(c: Canvas, y: Float) {
            paint.style = Paint.Style.FILL
            paint.shader = null
            paint.color = Color.rgb(24, 34, 49)

            c.drawRoundRect(
                RectF(34f, y, 461f, y + 62f),
                16f,
                16f,
                paint
            )

            stroke(Color.rgb(77, 91, 113), 1.1f)

            c.drawRoundRect(
                RectF(34f, y, 461f, y + 62f),
                16f,
                16f,
                paint
            )

            val title = "Continue with Google"

            paint.typeface = regular
            paint.textSize = 17f

            val titleWidth = paint.measureText(title)
            val groupWidth = 28f + 15f + titleWidth
            val start = (491f - groupWidth) / 2f

            googleMark(c, start + 14f, y + 31f)

            text(
                c,
                title,
                start + 43f,
                y + 37f,
                17f,
                white
            )
        }

        private fun drawCheckbox(
            c: Canvas,
            x: Float,
            y: Float,
            checked: Boolean
        ) {
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.alpha = 255

            if (checked) {
                paint.shader = LinearGradient(
                    x,
                    y,
                    x + 27f,
                    y + 27f,
                    intArrayOf(
                        Color.rgb(156, 101, 255),
                        Color.rgb(104, 72, 231)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            } else {
                paint.color = inputFill
            }

            c.drawRoundRect(
                RectF(x, y, x + 27f, y + 27f),
                6f,
                6f,
                paint
            )

            paint.shader = null

            if (checked) {
                stroke(white, 2.5f)

                c.drawPath(Path().apply {
                    moveTo(x + 7f, y + 13f)
                    lineTo(x + 12f, y + 18f)
                    lineTo(x + 21f, y + 8f)
                }, paint)
            } else {
                stroke(Color.rgb(92, 105, 128), 1.2f)

                c.drawRoundRect(
                    RectF(x, y, x + 27f, y + 27f),
                    6f,
                    6f,
                    paint
                )
            }
        }

        private fun centeredPair(
            c: Canvas,
            first: String,
            second: String,
            y: Float
        ) {
            paint.typeface = regular
            paint.textSize = 16f
            val firstWidth = paint.measureText(first)

            paint.typeface = medium
            val secondWidth = paint.measureText(second)

            val start = (491f - firstWidth - secondWidth) / 2f

            text(c, first, start, y, 16f, secondary)
            text(
                c,
                second,
                start + firstWidth,
                y,
                16f,
                purple,
                bold = true
            )
        }

        private fun text(
            c: Canvas,
            value: String,
            x: Float,
            y: Float,
            size: Float,
            color: Int,
            center: Boolean = false,
            bold: Boolean = false
        ) {
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = color
            paint.alpha = 255
            paint.textSize = size
            paint.typeface = if (bold) medium else regular

            paint.textAlign =
                if (center) Paint.Align.CENTER else Paint.Align.LEFT

            c.drawText(value, x, y, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        private fun stroke(color: Int, width: Float) {
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.color = color
            paint.alpha = 255
            paint.strokeWidth = width
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND
        }

        private fun envelope(c: Canvas, x: Float, y: Float) {
            stroke(iconColor, 1.7f)

            c.drawRoundRect(
                RectF(x, y, x + 21f, y + 17f),
                1.5f,
                1.5f,
                paint
            )

            c.drawPath(Path().apply {
                moveTo(x + 1f, y + 2f)
                lineTo(x + 10.5f, y + 9f)
                lineTo(x + 20f, y + 2f)
            }, paint)
        }

        private fun lock(c: Canvas, x: Float, y: Float) {
            stroke(iconColor, 1.8f)

            c.drawRoundRect(
                RectF(x + 1f, y + 8f, x + 18f, y + 21f),
                2f,
                2f,
                paint
            )

            c.drawArc(
                RectF(x + 4f, y - 1f, x + 15f, y + 14f),
                180f,
                180f,
                false,
                paint
            )

            c.drawLine(
                x + 9.5f,
                y + 13f,
                x + 9.5f,
                y + 17f,
                paint
            )
        }

        private fun eye(c: Canvas, x: Float, y: Float) {
            stroke(iconColor, 1.7f)

            c.drawPath(Path().apply {
                moveTo(x - 10f, y)
                cubicTo(
                    x - 5f, y - 10f,
                    x + 5f, y - 10f,
                    x + 11f, y
                )
                cubicTo(
                    x + 5f, y + 10f,
                    x - 5f, y + 10f,
                    x - 10f, y
                )
            }, paint)

            c.drawCircle(x + 0.5f, y, 3.5f, paint)

            if (!revealPassword) {
                c.drawLine(
                    x - 8f,
                    y - 8f,
                    x + 10f,
                    y + 9f,
                    paint
                )
            }
        }

        private fun person(c: Canvas, x: Float, y: Float) {
            stroke(iconColor, 1.8f)

            c.drawCircle(x, y - 7f, 5.5f, paint)

            c.drawPath(Path().apply {
                moveTo(x - 10f, y + 12f)
                cubicTo(
                    x - 10f, y - 1f,
                    x + 10f, y - 1f,
                    x + 10f, y + 12f
                )
            }, paint)
        }

        private fun phoneIcon(c: Canvas, x: Float, y: Float) {
            stroke(iconColor, 1.8f)

            c.drawPath(Path().apply {
                moveTo(x - 7f, y - 11f)
                lineTo(x - 2f, y - 6f)
                lineTo(x - 5f, y - 2f)

                cubicTo(
                    x - 3f, y + 2f,
                    x + 1f, y + 6f,
                    x + 5f, y + 7f
                )

                lineTo(x + 8f, y + 3f)
                lineTo(x + 13f, y + 7f)
                lineTo(x + 10f, y + 12f)

                cubicTo(
                    x - 1f, y + 14f,
                    x - 14f, y + 1f,
                    x - 11f, y - 8f
                )

                close()
            }, paint)
        }

        private fun globe(c: Canvas, x: Float, y: Float) {
            stroke(iconColor, 1.6f)

            c.drawCircle(x, y, 11f, paint)

            c.drawOval(
                RectF(x - 5f, y - 11f, x + 5f, y + 11f),
                paint
            )

            c.drawLine(x - 10f, y, x + 10f, y, paint)
        }

        private fun googleMark(c: Canvas, x: Float, y: Float) {
            val rect = RectF(
                x - 10f,
                y - 10f,
                x + 10f,
                y + 10f
            )

            stroke(Color.rgb(66, 133, 244), 5f)
            paint.strokeCap = Paint.Cap.BUTT

            c.drawArc(rect, 0f, 45f, false, paint)

            paint.color = Color.rgb(52, 168, 83)
            c.drawArc(rect, 45f, 100f, false, paint)

            paint.color = Color.rgb(251, 188, 5)
            c.drawArc(rect, 145f, 65f, false, paint)

            paint.color = Color.rgb(234, 67, 53)
            c.drawArc(rect, 210f, 105f, false, paint)

            paint.color = Color.rgb(66, 133, 244)
            c.drawLine(x + 10f, y, x + 1f, y, paint)
        }
    }
}
