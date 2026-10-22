package com.qubex.app

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.text.method.PasswordTransformationMethod
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
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Standalone Android-framework implementation; no XML, Compose or external libraries.
 * Change the package above to your application's package.
 * Required in AndroidManifest.xml, OUTSIDE <application>:
 * <uses-permission android:name="android.permission.INTERNET" />
 * Register INSIDE <application>:
 * <activity android:name=".LoginActivity" android:exported="false"
 *     android:windowSoftInputMode="adjustResize" />
 *
 * Hero artwork is downloaded from HTTPS and cached on disk. It is not embedded.
 * Authentication is intentionally not simulated: override the four callbacks below
 * to connect your backend, Google Identity flow and destination activities.
 */
open class LoginActivity : Activity() {
    companion object {
        private const val HERO_URL =
            "https://i.ibb.co/fYshkrrj/file-000000001a2c82469485a64af801b707.png"
        private const val STATE_EMAIL = "qubex.email"
        private const val STATE_REMEMBER = "qubex.remember"
    }

    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var screen: LoginScreen
    private var downloadStarted = false
    @Volatile private var disposed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.rgb(2, 8, 24)))
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        hideSystemBars()
        screen = LoginScreen(this)
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(2, 8, 24))
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
            addView(screen, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        setContentView(scroll)
        savedInstanceState?.let {
            screen.email.setText(it.getString(STATE_EMAIL, ""))
            screen.remember = it.getBoolean(STATE_REMEMBER, true)
        }
        loadHero()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_EMAIL, screen.email.text.toString())
        outState.putBoolean(STATE_REMEMBER, screen.remember)
        // Deliberately do not persist the password.
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        disposed = true
        main.removeCallbacksAndMessages(null)
        worker.shutdownNow()
        super.onDestroy()
    }

    /** Replace this callback with your real HTTPS authentication flow. */
    protected open fun onLoginRequested(identifier: String, password: String, rememberMe: Boolean) {
        Toast.makeText(this, "Connect your authentication API in onLoginRequested()", Toast.LENGTH_LONG).show()
    }
    protected open fun onGoogleLoginRequested() {
        Toast.makeText(this, "Connect Google Identity in onGoogleLoginRequested()", Toast.LENGTH_LONG).show()
    }
    protected open fun onForgotPasswordRequested() {
        Toast.makeText(this, "Connect your password recovery screen", Toast.LENGTH_SHORT).show()
    }
    protected open fun onSignUpRequested() {
        Toast.makeText(this, "Connect your sign-up screen", Toast.LENGTH_SHORT).show()
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
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(screen.windowToken, 0)
        onLoginRequested(identifier, password, screen.remember)
    }

    private fun loadHero() {
        if (downloadStarted || disposed) return
        downloadStarted = true
        worker.execute {
            var result: Bitmap? = null
            val cache = File(cacheDir, "qubex_login_hero_v1.png")
            try {
                if (cache.exists()) result = decodeImage(cache)
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
                                    if (Thread.currentThread().isInterrupted) throw InterruptedException()
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
                // Keep the form usable offline, with a tappable retry in the hero area.
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
        return BitmapFactory.decodeFile(file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private inner class LoginScreen(context: Context) : FrameLayout(context) {
        private val backgroundColor = Color.rgb(2, 8, 24)
        private val white = Color.rgb(247, 247, 255)
        private val secondary = Color.rgb(165, 171, 195)
        private val muted = Color.rgb(115, 125, 155)
        private val purple = Color.rgb(139, 79, 255)
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val regular = Typeface.create("sans-serif", Typeface.NORMAL)
        private val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        private var factor = 1f
        private var revealPassword = false
        private var loginPressed = false
        var hero: Bitmap? = null
        var heroFailed = false
        var remember = true
            set(value) { field = value; invalidate() }

        val email = field("Enter your email or phone", false)
        val password = field("Enter your password", true)
        private val retry = hit("Reload artwork") { loadHero() }
        private val toggle = hit("Show password") {
            revealPassword = !revealPassword
            val cursor = password.selectionStart
            password.transformationMethod = if (revealPassword) null else PasswordTransformationMethod.getInstance()
            password.setSelection(cursor.coerceIn(0, password.length()))
            toggleDescription()
            invalidate()
        }
        private val checkbox = hit("Remember me, checked") { remember = !remember; checkboxDescription() }
        private val forgot = hit("Forgot password") { onForgotPasswordRequested() }
        private val login = hit("Log In") { submit() }
        private val google = hit("Continue with Google") { onGoogleLoginRequested() }
        private val signup = hit("Sign Up") { onSignUpRequested() }

        init {
            setWillNotDraw(false)
            isFocusableInTouchMode = true
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            addView(email)
            addView(password)
            addView(retry)
            addView(toggle)
            addView(checkbox)
            addView(forgot)
            addView(login)
            addView(google)
            addView(signup)
            password.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) { submit(); true } else false
            }
            login.setOnTouchListener { _, event ->
                loginPressed = event.actionMasked == MotionEvent.ACTION_DOWN ||
                    event.actionMasked == MotionEvent.ACTION_MOVE
                invalidate()
                false
            }
        }

        private fun toggleDescription() {
            toggle.contentDescription = if (revealPassword) "Hide password" else "Show password"
        }
        private fun checkboxDescription() {
            checkbox.contentDescription = if (remember) "Remember me, checked" else "Remember me, unchecked"
        }

        private fun field(hintText: String, secret: Boolean) = EditText(context).apply {
            hint = hintText
            typeface = regular
            setTextColor(white)
            setHintTextColor(muted)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0, 0, 0, 0)
            gravity = Gravity.CENTER_VERTICAL
            isSingleLine = true
            isSaveEnabled = false
            inputType = if (secret) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            if (secret) transformationMethod = PasswordTransformationMethod.getInstance()
            imeOptions = if (secret) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT
            if (Build.VERSION.SDK_INT >= 26) {
                setAutofillHints(if (secret) View.AUTOFILL_HINT_PASSWORD else View.AUTOFILL_HINT_USERNAME)
            }
            contentDescription = if (secret) "Password" else "Email or Phone"
        }

        private fun hit(description: String, action: () -> Unit): View = View(context).apply {
            contentDescription = description
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(Color.argb(35, 151, 106, 255)),
                null, android.graphics.drawable.ColorDrawable(Color.WHITE))
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val w = MeasureSpec.getSize(widthMeasureSpec)
            factor = w / 491f
            val natural = ceil(1098f * factor).toInt()
            val mode = MeasureSpec.getMode(heightMeasureSpec)
            val size = MeasureSpec.getSize(heightMeasureSpec)
            val h = when (mode) {
                MeasureSpec.EXACTLY -> size
                MeasureSpec.AT_MOST -> min(natural, size)
                else -> natural
            }
            setMeasuredDimension(w, h)
            email.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 16f * factor)
            password.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 16f * factor)
            measureBox(email, 96f, 543f, 407f, 601f)
            measureBox(password, 96f, 655f, 405f, 713f)
            measureBox(retry, 0f, 0f, 491f, 490f)
            measureBox(toggle, 405f, 655f, 460f, 713f)
            measureBox(checkbox, 28f, 728f, 235f, 778f)
            measureBox(forgot, 303f, 728f, 467f, 778f)
            measureBox(login, 34f, 807f, 461f, 869f)
            measureBox(google, 34f, 944f, 461f, 1007f)
            measureBox(signup, 119f, 1040f, 376f, 1093f)
        }

        private fun measureBox(view: View, l: Float, t: Float, r: Float, b: Float) {
            view.tag = floatArrayOf(l, t, r, b)
            view.measure(MeasureSpec.makeMeasureSpec(((r-l)*factor).toInt(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(((b-t)*factor).toInt(), MeasureSpec.EXACTLY))
        }
        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                val box = child.tag as FloatArray
                val x = (box[0]*factor).toInt()
                val y = (box[1]*factor).toInt()
                child.layout(x, y, x + child.measuredWidth, y + child.measuredHeight)
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(backgroundColor)
            val save = canvas.save()
            canvas.scale(factor, factor)
            hero?.let { bitmap ->
                // Preserve the complete supplied artwork, including its own logo and slogan.
                val destination = RectF(0f, 0f, 491f, 491f)
                val fit = min(destination.width()/bitmap.width, destination.height()/bitmap.height)
                val bw = bitmap.width * fit
                val bh = bitmap.height * fit
                paint.shader = null
                paint.alpha = 255
                canvas.drawBitmap(bitmap, null,
                    RectF((491f-bw)/2f, (491f-bh)/2f, (491f+bw)/2f, (491f+bh)/2f), paint)
                paint.shader = LinearGradient(0f, 407f, 0f, 496f,
                    intArrayOf(Color.TRANSPARENT, backgroundColor), null, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 407f, 491f, 497f, paint)
                paint.shader = null
            } ?: run {
                text(canvas, "QUBEX", 245.5f, 202f, 35f, white, true, true)
                text(canvas, if (heroFailed) "Tap to reload artwork" else "Loading artwork…",
                    245.5f, 260f, 16f, secondary, true)
            }

            text(canvas, "Email or Phone", 34f, 532f, 17f, secondary)
            inputBox(canvas, 542f)
            envelope(canvas, 55f, 564f)
            text(canvas, "Password", 34f, 643f, 17f, secondary)
            inputBox(canvas, 654f)
            lock(canvas, 56f, 674f)
            eye(canvas, 429f, 684f)

            paint.style = Paint.Style.FILL
            paint.shader = if (remember) LinearGradient(34f, 737f, 61f, 764f,
                intArrayOf(Color.rgb(153, 80, 255), Color.rgb(92, 53, 245)), null, Shader.TileMode.CLAMP) else null
            paint.color = if (remember) purple else Color.rgb(14, 22, 43)
            canvas.drawRoundRect(RectF(34f, 737f, 61f, 764f), 6f, 6f, paint)
            paint.shader = null
            if (remember) {
                stroke(white, 2.5f)
                canvas.drawPath(Path().apply { moveTo(41f, 750f); lineTo(46f, 755f); lineTo(54f, 745f) }, paint)
            }
            text(canvas, "Remember me", 71f, 757f, 16f, secondary)
            text(canvas, "Forgot password?", 326f, 757f, 16f, purple)

            paint.style = Paint.Style.FILL
            paint.shader = LinearGradient(34f, 807f, 461f, 869f,
                intArrayOf(Color.rgb(100, 57, 249), Color.rgb(106, 59, 243), Color.rgb(76, 56, 215)),
                floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
            paint.alpha = if (loginPressed) 200 else 255
            canvas.drawRoundRect(RectF(34f, 807f, 461f, 869f), 17f, 17f, paint)
            paint.alpha = 255
            paint.shader = null
            text(canvas, "Log In", 247.5f, 845f, 19f, white, true)

            stroke(Color.rgb(47, 56, 78), 1f)
            canvas.drawLine(34f, 907f, 226f, 907f, paint)
            canvas.drawLine(269f, 907f, 461f, 907f, paint)
            text(canvas, "or", 247f, 912f, 16f, secondary, true)

            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(6, 13, 30)
            canvas.drawRoundRect(RectF(34f, 944f, 461f, 1007f), 16f, 16f, paint)
            stroke(Color.rgb(76, 86, 113), 1.1f)
            canvas.drawRoundRect(RectF(34f, 944f, 461f, 1007f), 16f, 16f, paint)
            googleMark(canvas, 162f, 976f)
            text(canvas, "Continue with Google", 190f, 982f, 17f, white)

            val first = "Don’t have an account? "
            val second = "Sign Up"
            paint.typeface = regular
            paint.textSize = 17f
            val firstWidth = paint.measureText(first)
            val secondWidth = paint.measureText(second)
            val start = (491f - firstWidth - secondWidth) / 2f
            text(canvas, first, start, 1074f, 17f, secondary)
            text(canvas, second, start + firstWidth, 1074f, 17f, purple, bold = true)
            canvas.restoreToCount(save)
        }

        private fun inputBox(c: Canvas, y: Float) {
            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(8, 15, 33)
            c.drawRoundRect(RectF(34f, y, 461f, y+59f), 15f, 15f, paint)
            stroke(Color.rgb(31, 40, 62), 1.2f)
            c.drawRoundRect(RectF(34f, y, 461f, y+59f), 15f, 15f, paint)
        }
        private fun text(c: Canvas, value: String, x: Float, y: Float, size: Float,
                         color: Int, center: Boolean = false, bold: Boolean = false) {
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = color
            paint.textSize = size
            paint.typeface = if (bold) medium else regular
            paint.textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT
            c.drawText(value, x, y, paint)
            paint.textAlign = Paint.Align.LEFT
        }
        private fun stroke(color: Int, width: Float) {
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.color = color
            paint.strokeWidth = width
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND
        }
        private fun envelope(c: Canvas, x: Float, y: Float) {
            stroke(Color.rgb(150, 164, 207), 1.7f)
            c.drawRoundRect(RectF(x, y, x+21f, y+17f), 1.5f, 1.5f, paint)
            c.drawPath(Path().apply {
                moveTo(x+1f, y+2f); lineTo(x+10.5f, y+9f); lineTo(x+20f, y+2f)
            }, paint)
        }
        private fun lock(c: Canvas, x: Float, y: Float) {
            stroke(Color.rgb(150, 164, 207), 1.8f)
            c.drawRoundRect(RectF(x+1f, y+8f, x+18f, y+21f), 2f, 2f, paint)
            c.drawArc(RectF(x+4f, y-1f, x+15f, y+14f), 180f, 180f, false, paint)
            c.drawLine(x+9.5f, y+13f, x+9.5f, y+17f, paint)
        }
        private fun eye(c: Canvas, x: Float, y: Float) {
            stroke(Color.rgb(139, 151, 186), 1.7f)
            c.drawPath(Path().apply {
                moveTo(x-10f, y); cubicTo(x-5f, y-10f, x+5f, y-10f, x+11f, y)
                cubicTo(x+5f, y+10f, x-5f, y+10f, x-10f, y)
            }, paint)
            c.drawCircle(x+.5f, y, 3.5f, paint)
            if (!revealPassword) c.drawLine(x-8f, y-8f, x+10f, y+9f, paint)
        }
        private fun googleMark(c: Canvas, x: Float, y: Float) {
            // Multi-colour vector G: no icon bitmap embedded in this file.
            val rect = RectF(x-10f, y-10f, x+10f, y+10f)
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
            c.drawLine(x+10f, y, x+1f, y, paint)
        }
    }
}
