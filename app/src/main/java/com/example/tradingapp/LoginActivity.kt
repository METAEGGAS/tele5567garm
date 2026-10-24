package com.qubex.app

import android.app.Activity
import android.app.AlertDialog
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
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

open class LoginActivity:Activity(){
    companion object{
        private const val HERO_URL="https://i.ibb.co/rKvzFXQd/file-0000000016848246a932b72ca8476b79.png"
        private const val BG=0xFF020818.toInt()
    }
    private val worker=Executors.newSingleThreadExecutor()
    private val main=Handler(Looper.getMainLooper())
    private lateinit var screen:LoginScreen
    private var downloadStarted=false
    @Volatile private var disposed=false

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.statusBarColor=BG
        window.navigationBarColor=BG
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(BG))
        screen=LoginScreen(this)
        setContentView(FrameLayout(this).apply{
            setBackgroundColor(BG)
            addView(screen,FrameLayout.LayoutParams(-1,-1))
        })
        savedInstanceState?.let{
            screen.email.setText(it.getString("email",""))
            screen.fullName.setText(it.getString("name",""))
            screen.phone.setText(it.getString("phone",""))
            screen.remember=it.getBoolean("remember",true)
            screen.agreed=it.getBoolean("agreed",true)
            screen.countryCode=it.getString("country","")
            screen.showPage(it.getBoolean("signup",false))
        }
        loadHero()
    }
    override fun onSaveInstanceState(outState:Bundle){
        if(::screen.isInitialized){
            outState.putString("email",screen.email.text.toString())
            outState.putString("name",screen.fullName.text.toString())
            outState.putString("phone",screen.phone.text.toString())
            outState.putBoolean("remember",screen.remember)
            outState.putBoolean("agreed",screen.agreed)
            outState.putString("country",screen.countryCode)
            outState.putBoolean("signup",screen.signUpMode)
        }
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy(){
        disposed=true
        main.removeCallbacksAndMessages(null)
        worker.shutdownNow()
        super.onDestroy()
    }
    protected open fun onLoginRequested(identifier:String,password:String,rememberMe:Boolean){
        Toast.makeText(this,"Connect your authentication API in onLoginRequested()",Toast.LENGTH_LONG).show()
    }
    protected open fun onCreateAccountRequested(fullName:String,email:String,phone:String,password:String,countryCode:String){
        Toast.makeText(this,"Connect your registration API in onCreateAccountRequested()",Toast.LENGTH_LONG).show()
    }
    protected open fun onGoogleLoginRequested(){
        Toast.makeText(this,"Connect Google Identity in onGoogleLoginRequested()",Toast.LENGTH_LONG).show()
    }
    protected open fun onForgotPasswordRequested(){
        Toast.makeText(this,"Connect your password recovery screen",Toast.LENGTH_SHORT).show()
    }
    protected open fun onSignUpRequested(){screen.showPage(true)}
    protected open fun onTermsRequested(){
        Toast.makeText(this,"Connect your Terms of Service screen",Toast.LENGTH_SHORT).show()
    }
    protected open fun onPrivacyRequested(){
        Toast.makeText(this,"Connect your Privacy Policy screen",Toast.LENGTH_SHORT).show()
    }
    private fun hideKeyboard(){
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(screen.windowToken,0)
    }
    private fun submit(){
        val identifier=screen.email.text.toString().trim()
        val password=screen.password.text.toString()
        if(screen.signUpMode){
            val name=screen.fullName.text.toString().trim()
            val phone=screen.phone.text.toString().trim()
            if(name.isEmpty()){
                screen.fullName.error="Enter your full name"
                screen.fullName.requestFocus()
                return
            }
            if(!android.util.Patterns.EMAIL_ADDRESS.matcher(identifier).matches()){
                screen.email.error="Enter a valid email address"
                screen.email.requestFocus()
                return
            }
            if(phone.isEmpty()){
                screen.phone.error="Enter your phone number"
                screen.phone.requestFocus()
                return
            }
            if(password.isEmpty()){
                screen.password.error="Create a password"
                screen.password.requestFocus()
                return
            }
            if(screen.countryCode.isEmpty()){
                screen.chooseCountry()
                return
            }
            if(!screen.agreed){
                Toast.makeText(this,"Please agree to the Terms of Service and Privacy Policy",Toast.LENGTH_SHORT).show()
                return
            }
            hideKeyboard()
            onCreateAccountRequested(name,identifier,phone,password,screen.countryCode)
        }else{
            if(identifier.isEmpty()){
                screen.email.error="Enter your email or phone"
                screen.email.requestFocus()
                return
            }
            if(password.isEmpty()){
                screen.password.error="Enter your password"
                screen.password.requestFocus()
                return
            }
            hideKeyboard()
            onLoginRequested(identifier,password,screen.remember)
        }
    }
    private fun loadHero(){
        if(downloadStarted||disposed)return
        downloadStarted=true
        worker.execute{
            var result:Bitmap?=null
            val cache=File(cacheDir,"qubex_login_hero_v1.png")
            try{
                if(cache.exists())result=decodeImage(cache)
                if(result==null){
                    val connection=URL(HERO_URL).openConnection() as HttpURLConnection
                    val temporary=File(cacheDir,"qubex_login_hero_v1.part")
                    try{
                        connection.connectTimeout=15000
                        connection.readTimeout=20000
                        connection.instanceFollowRedirects=true
                        connection.setRequestProperty("User-Agent","QubexAndroid/1.0")
                        connection.setRequestProperty("Accept","image/*")
                        check(connection.responseCode in 200..299){"Image HTTP error"}
                        connection.inputStream.use{input->
                            FileOutputStream(temporary).use{output->
                                val buffer=ByteArray(8192)
                                var total=0
                                while(true){
                                    if(Thread.currentThread().isInterrupted)throw InterruptedException()
                                    val count=input.read(buffer)
                                    if(count<0)break
                                    total+=count
                                    check(total<=20*1024*1024){"Image is too large"}
                                    output.write(buffer,0,count)
                                }
                            }
                        }
                        result=decodeImage(temporary)
                        check(result!=null){"Invalid image"}
                        if(!temporary.renameTo(cache))temporary.delete()
                    }finally{
                        connection.disconnect()
                        temporary.delete()
                    }
                }
            }catch(_:Exception){}
            val bitmap=result
            main.post{
                if(!disposed){
                    downloadStarted=false
                    screen.hero=bitmap
                    screen.heroFailed=bitmap==null
                    screen.invalidate()
                }
            }
        }
    }
    private fun decodeImage(file:File):Bitmap?{
        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
        BitmapFactory.decodeFile(file.absolutePath,bounds)
        if(bounds.outWidth<=0||bounds.outHeight<=0)return null
        var sample=1
        while(max(bounds.outWidth,bounds.outHeight)/sample>2048)sample*=2
        return BitmapFactory.decodeFile(file.absolutePath,BitmapFactory.Options().apply{inSampleSize=sample})
    }

    private inner class LoginScreen(context:Context):FrameLayout(context){
        private val white=Color.rgb(247,247,255)
        private val secondary=Color.rgb(165,171,195)
        private val muted=Color.rgb(115,125,155)
        private val purple=Color.rgb(139,79,255)
        private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val regular=Typeface.create("sans-serif",Typeface.NORMAL)
        private val medium=Typeface.create("sans-serif-medium",Typeface.NORMAL)
        private var scaleXFactor=1f
        private var scaleYFactor=1f
        private var revealPassword=false
        private var loginPressed=false
        var signUpMode=false
            private set
        var hero:Bitmap?=null
        var heroFailed=false
        var countryCode=""
        var remember=true
            set(value){field=value;updateDescriptions();invalidate()}
        var agreed=true
            set(value){field=value;updateDescriptions();invalidate()}

        val fullName=field("Enter your full name",InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME)
        val email=field("Enter your email or phone",InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val phone=field("Enter your phone number",InputType.TYPE_CLASS_PHONE)
        val password=field("Enter your password",InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,true)
        private val retry=hit("Reload artwork"){loadHero()}
        private val toggle=hit("Show password"){
            revealPassword=!revealPassword
            val cursor=password.selectionStart
            password.transformationMethod=if(revealPassword)null else PasswordTransformationMethod.getInstance()
            password.setSelection(cursor.coerceIn(0,password.length()))
            updateDescriptions()
            invalidate()
        }
        private val checkbox=hit("Remember me, checked"){remember=!remember}
        private val forgot=hit("Forgot password"){onForgotPasswordRequested()}
        private val login=hit("Log In"){submit()}
        private val google=hit("Continue with Google"){onGoogleLoginRequested()}
        private val signup=hit("Sign Up"){if(signUpMode)showPage(false)else onSignUpRequested()}
        private val back=hit("Back to Log In"){showPage(false)}
        private val country=hit("Select your country"){chooseCountry()}
        private val agreement=hit("Agree to terms, checked"){agreed=!agreed}
        private val terms=hit("Terms of Service"){onTermsRequested()}
        private val privacy=hit("Privacy Policy"){onPrivacyRequested()}

        init{
            setWillNotDraw(false)
            setBackgroundColor(BG)
            layoutDirection=View.LAYOUT_DIRECTION_LTR
            isFocusableInTouchMode=true
            descendantFocusability=ViewGroup.FOCUS_AFTER_DESCENDANTS
            fullName.id=View.generateViewId()
            email.id=View.generateViewId()
            phone.id=View.generateViewId()
            password.id=View.generateViewId()
            listOf(fullName,email,phone,password,retry,toggle,checkbox,forgot,login,google,signup,back,country,agreement,terms,privacy)
                .forEach{addView(it)}
            password.setOnEditorActionListener{_,actionId,_->
                if(actionId==EditorInfo.IME_ACTION_DONE){submit();true}else false
            }
            login.setOnTouchListener{_,event->
                loginPressed=event.actionMasked==MotionEvent.ACTION_DOWN||event.actionMasked==MotionEvent.ACTION_MOVE
                invalidate()
                false
            }
            showPage(false)
        }
        fun showPage(create:Boolean){
            val changed=signUpMode!=create
            if(changed){
                hideKeyboard()
                password.text.clear()
                revealPassword=false
                password.transformationMethod=PasswordTransformationMethod.getInstance()
            }
            signUpMode=create
            listOf(fullName,phone,back,country,agreement,terms,privacy).forEach{
                it.visibility=if(create)View.VISIBLE else View.GONE
            }
            listOf(retry,checkbox,forgot).forEach{
                it.visibility=if(create)View.GONE else View.VISIBLE
            }
            email.hint=if(create)"Enter your email address" else "Enter your email or phone"
            password.hint=if(create)"Create a password" else "Enter your password"
            email.contentDescription=if(create)"Email" else "Email or Phone"
            login.contentDescription=if(create)"Create Account" else "Log In"
            signup.contentDescription=if(create)"Log In" else "Sign Up"
            fullName.nextFocusForwardId=email.id
            email.nextFocusForwardId=if(create)phone.id else password.id
            phone.nextFocusForwardId=password.id
            password.imeOptions=EditorInfo.IME_ACTION_DONE
            if(Build.VERSION.SDK_INT>=26){
                password.setAutofillHints(if(create)View.AUTOFILL_HINT_NEW_PASSWORD else View.AUTOFILL_HINT_PASSWORD)
            }
            listOf(fullName,email,phone,password).forEach{it.error=null;it.clearFocus()}
            loginPressed=false
            updateDescriptions()
            requestLayout()
            invalidate()
        }
        private fun updateDescriptions(){
            if(childCount==0)return
            toggle.contentDescription=if(revealPassword)"Hide password" else "Show password"
            checkbox.contentDescription=if(remember)"Remember me, checked" else "Remember me, unchecked"
            agreement.contentDescription=if(agreed)"Agree to terms, checked" else "Agree to terms, unchecked"
        }
        fun chooseCountry(){
            hideKeyboard()
            val countries=Locale.getISOCountries().map{code->
                code to Locale("",code).getDisplayCountry(Locale.ENGLISH)
            }.sortedBy{it.second}
            val selected=countries.indexOfFirst{it.first==countryCode}
            AlertDialog.Builder(this@LoginActivity)
                .setTitle("Country / Region")
                .setSingleChoiceItems(countries.map{it.second}.toTypedArray(),selected){dialog,index->
                    countryCode=countries[index].first
                    country.contentDescription="Country / Region, ${countries[index].second}"
                    invalidate()
                    dialog.dismiss()
                }.setNegativeButton("Cancel",null).show()
        }
        private fun field(hintText:String,type:Int,secret:Boolean=false)=EditText(context).apply{
            hint=hintText
            typeface=regular
            setTextColor(white)
            setHintTextColor(muted)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0,0,0,0)
            gravity=Gravity.CENTER_VERTICAL
            layoutDirection=View.LAYOUT_DIRECTION_LTR
            textDirection=View.TEXT_DIRECTION_LTR
            isSingleLine=true
            isSaveEnabled=false
            inputType=type
            if(secret)transformationMethod=PasswordTransformationMethod.getInstance()
            imeOptions=if(secret)EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT
            contentDescription=hintText
            if(Build.VERSION.SDK_INT>=26){
                setAutofillHints(when{
                    secret->View.AUTOFILL_HINT_PASSWORD
                    type==InputType.TYPE_CLASS_PHONE->View.AUTOFILL_HINT_PHONE
                    type and InputType.TYPE_TEXT_VARIATION_PERSON_NAME==InputType.TYPE_TEXT_VARIATION_PERSON_NAME->View.AUTOFILL_HINT_NAME
                    else->View.AUTOFILL_HINT_USERNAME
                })
            }
        }
        private fun hit(description:String,action:()->Unit):View=View(context).apply{
            contentDescription=description
            isClickable=true
            isFocusable=true
            setOnClickListener{action()}
            background=android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(Color.argb(35,151,106,255)),
                null,android.graphics.drawable.ColorDrawable(Color.WHITE))
        }
        override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int){
            val w=MeasureSpec.getSize(widthMeasureSpec)
            val h=MeasureSpec.getSize(heightMeasureSpec)
            scaleXFactor=w/491f
            scaleYFactor=min(scaleXFactor,h/1098f)
            setMeasuredDimension(w,h)
            val textScale=min(scaleXFactor,scaleYFactor)
            listOf(fullName,email,phone,password).forEach{
                it.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,16f*textScale)
            }
            if(signUpMode){
                box(back,24f,12f,78f,66f)
                box(fullName,96f,300f,443f,359f)
                box(email,96f,406f,443f,465f)
                box(phone,96f,512f,443f,571f)
                box(password,96f,618f,405f,677f)
                box(toggle,405f,618f,461f,677f)
                box(country,34f,724f,461f,783f)
                box(agreement,28f,797f,67f,844f)
                box(terms,164f,797f,302f,844f)
                box(privacy,331f,797f,461f,844f)
                box(login,34f,864f,461f,926f)
                box(google,34f,991f,461f,1054f)
                box(signup,119f,1058f,376f,1098f)
            }else{
                box(email,96f,543f,407f,601f)
                box(password,96f,655f,405f,713f)
                box(retry,0f,0f,491f,490f)
                box(toggle,405f,655f,460f,713f)
                box(checkbox,28f,728f,235f,778f)
                box(forgot,303f,728f,467f,778f)
                box(login,34f,807f,461f,869f)
                box(google,34f,944f,461f,1007f)
                box(signup,119f,1040f,376f,1093f)
            }
        }
        private fun box(view:View,l:Float,t:Float,r:Float,b:Float){
            view.tag=RectF(l,t,r,b)
            view.measure(
                MeasureSpec.makeMeasureSpec(((r-l)*scaleXFactor).toInt().coerceAtLeast(0),MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(((b-t)*scaleYFactor).toInt().coerceAtLeast(0),MeasureSpec.EXACTLY))
        }
        override fun onLayout(changed:Boolean,left:Int,top:Int,right:Int,bottom:Int){
            for(i in 0 until childCount){
                val child=getChildAt(i)
                if(child.visibility==View.GONE)continue
                val rect=child.tag as? RectF?:continue
                val x=(rect.left*scaleXFactor).toInt()
                val y=(rect.top*scaleYFactor).toInt()
                child.layout(x,y,x+child.measuredWidth,y+child.measuredHeight)
            }
        }
        override fun onDraw(canvas:Canvas){
            super.onDraw(canvas)
            canvas.drawColor(BG)
            val save=canvas.save()
            canvas.scale(scaleXFactor,scaleYFactor)
            if(signUpMode)drawSignUp(canvas)else drawLogin(canvas)
            canvas.restoreToCount(save)
        }
        private fun drawLogin(c:Canvas){
            hero?.let{bitmap->
                val fit=min(491f/bitmap.width,491f/bitmap.height)
                val bw=bitmap.width*fit
                val bh=bitmap.height*fit
                paint.shader=null
                paint.style=Paint.Style.FILL
                paint.alpha=255
                c.drawBitmap(bitmap,null,RectF((491f-bw)/2f,(491f-bh)/2f,(491f+bw)/2f,(491f+bh)/2f),paint)
                paint.shader=LinearGradient(0f,407f,0f,496f,intArrayOf(Color.TRANSPARENT,BG),null,Shader.TileMode.CLAMP)
                c.drawRect(0f,407f,491f,497f,paint)
                paint.shader=null
            }?:run{
                text(c,"QUBEX",245.5f,202f,35f,white,true,true)
                text(c,if(heroFailed)"Tap to reload artwork" else "Loading artwork…",245.5f,260f,16f,secondary,true)
            }
            text(c,"Email or Phone",34f,532f,17f,secondary)
            inputBox(c,542f);envelope(c,55f,564f)
            text(c,"Password",34f,643f,17f,secondary)
            inputBox(c,654f);lock(c,56f,674f);eye(c,429f,684f)
            checkMark(c,34f,737f,remember)
            text(c,"Remember me",71f,757f,16f,secondary)
            text(c,"Forgot password?",326f,757f,16f,purple)
            primaryButton(c,807f,"Log In")
            divider(c,907f)
            googleButton(c,944f)
            footer(c,"Don’t have an account? ","Sign Up",1074f)
        }
        private fun drawSignUp(c:Canvas){
            stroke(white,2f)
            c.drawPath(Path().apply{moveTo(51f,28f);lineTo(42f,37f);lineTo(51f,46f);moveTo(42f,37f);lineTo(60f,37f)},paint)
            brand(c)
            text(c,"Create Account",245.5f,207f,29f,white,true,true)
            text(c,"Join millions of traders worldwide",245.5f,239f,17f,secondary,true)
            text(c,"Full Name",34f,287f,17f,secondary)
            inputBox(c,300f);person(c,65f,329f)
            text(c,"Email",34f,393f,17f,secondary)
            inputBox(c,406f);envelope(c,55f,428f)
            text(c,"Phone Number",34f,499f,17f,secondary)
            inputBox(c,512f);phoneIcon(c,56f,531f)
            text(c,"Password",34f,605f,17f,secondary)
            inputBox(c,618f);lock(c,56f,638f);eye(c,429f,648f)
            text(c,"Country / Region",34f,711f,17f,secondary)
            inputBox(c,724f);globe(c,65f,754f)
            val countryName=if(countryCode.isEmpty())"Select your country" else Locale("",countryCode).getDisplayCountry(Locale.ENGLISH)
            text(c,ellipsize(countryName,316f,16f),96f,761f,16f,if(countryCode.isEmpty())muted else white)
            stroke(secondary,1.8f)
            c.drawPath(Path().apply{moveTo(426f,751f);lineTo(432f,757f);lineTo(438f,751f)},paint)
            checkMark(c,34f,807f,agreed)
            text(c,"I agree to the",70f,827f,15f,secondary)
            text(c,"Terms of Service",166f,827f,15f,purple)
            text(c,"and",306f,827f,15f,secondary)
            text(c,"Privacy Policy",335f,827f,15f,purple)
            primaryButton(c,864f,"Create Account")
            divider(c,960f)
            googleButton(c,991f)
            footer(c,"Already have an account? ","Log In",1083f)
        }
        private fun brand(c:Canvas){
            paint.style=Paint.Style.FILL
            paint.shader=LinearGradient(213f,57f,279f,121f,intArrayOf(Color.rgb(155,76,255),Color.rgb(81,47,233)),null,Shader.TileMode.CLAMP)
            val outer=Path().apply{
                moveTo(245.5f,53f);lineTo(280f,73f);lineTo(280f,108f)
                lineTo(264f,99f);lineTo(264f,82f);lineTo(245.5f,71f)
                lineTo(227f,82f);lineTo(227f,104f);lineTo(245.5f,115f)
                lineTo(255f,109f);lineTo(272f,119f);lineTo(245.5f,134f)
                lineTo(211f,114f);lineTo(211f,73f);close()
            }
            c.drawPath(outer,paint)
            c.drawPath(Path().apply{
                moveTo(248f,94f);lineTo(280f,112f);lineTo(280f,130f)
                lineTo(248f,112f);close()
            },paint)
            paint.shader=null
            text(c,"QUBEX",245.5f,168f,31f,white,true,true)
        }
        private fun ellipsize(value:String,width:Float,size:Float):String{
            paint.typeface=regular
            paint.textSize=size
            if(paint.measureText(value)<=width)return value
            var end=value.length
            while(end>0&&paint.measureText(value.substring(0,end)+"…")>width)end--
            return value.substring(0,end)+"…"
        }
        private fun inputBox(c:Canvas,y:Float){
            paint.shader=null
            paint.style=Paint.Style.FILL
            paint.color=Color.rgb(8,15,33)
            c.drawRoundRect(RectF(34f,y,461f,y+59f),15f,15f,paint)
            stroke(Color.rgb(31,40,62),1.2f)
            c.drawRoundRect(RectF(34f,y,461f,y+59f),15f,15f,paint)
        }
        private fun primaryButton(c:Canvas,y:Float,label:String){
            paint.style=Paint.Style.FILL
            paint.shader=LinearGradient(34f,y,461f,y+62f,
                intArrayOf(Color.rgb(100,57,249),Color.rgb(106,59,243),Color.rgb(76,56,215)),
                floatArrayOf(0f,.55f,1f),Shader.TileMode.CLAMP)
            paint.alpha=if(loginPressed)200 else 255
            c.drawRoundRect(RectF(34f,y,461f,y+62f),17f,17f,paint)
            paint.alpha=255
            paint.shader=null
            text(c,label,247.5f,y+38f,19f,white,true)
        }
        private fun divider(c:Canvas,y:Float){
            stroke(Color.rgb(47,56,78),1f)
            c.drawLine(34f,y,226f,y,paint)
            c.drawLine(269f,y,461f,y,paint)
            text(c,"or",247f,y+5f,16f,secondary,true)
        }
        private fun googleButton(c:Canvas,y:Float){
            paint.shader=null
            paint.style=Paint.Style.FILL
            paint.color=Color.rgb(6,13,30)
            c.drawRoundRect(RectF(34f,y,461f,y+63f),16f,16f,paint)
            stroke(Color.rgb(76,86,113),1.1f)
            c.drawRoundRect(RectF(34f,y,461f,y+63f),16f,16f,paint)
            paint.typeface=regular
            paint.textSize=17f
            val label="Continue with Google"
            val start=(491f-(28f+paint.measureText(label)))/2f
            googleMark(c,start+10f,y+32f)
            text(c,label,start+28f,y+38f,17f,white)
        }
        private fun footer(c:Canvas,first:String,second:String,y:Float){
            paint.typeface=regular
            paint.textSize=17f
            val a=paint.measureText(first)
            paint.typeface=medium
            val b=paint.measureText(second)
            val start=(491f-a-b)/2f
            text(c,first,start,y,17f,secondary)
            text(c,second,start+a,y,17f,purple,bold=true)
        }
        private fun checkMark(c:Canvas,x:Float,y:Float,checked:Boolean){
            paint.style=Paint.Style.FILL
            paint.shader=if(checked)LinearGradient(x,y,x+27f,y+27f,
                intArrayOf(Color.rgb(153,80,255),Color.rgb(92,53,245)),null,Shader.TileMode.CLAMP)else null
            paint.color=if(checked)purple else Color.rgb(14,22,43)
            c.drawRoundRect(RectF(x,y,x+27f,y+27f),6f,6f,paint)
            paint.shader=null
            if(checked){
                stroke(white,2.5f)
                c.drawPath(Path().apply{moveTo(x+7f,y+13f);lineTo(x+12f,y+18f);lineTo(x+20f,y+8f)},paint)
            }else{
                stroke(Color.rgb(76,86,113),1f)
                c.drawRoundRect(RectF(x,y,x+27f,y+27f),6f,6f,paint)
            }
        }
        private fun text(c:Canvas,value:String,x:Float,y:Float,size:Float,color:Int,center:Boolean=false,bold:Boolean=false){
            paint.shader=null
            paint.style=Paint.Style.FILL
            paint.color=color
            paint.alpha=255
            paint.textSize=size
            paint.typeface=if(bold)medium else regular
            paint.textAlign=if(center)Paint.Align.CENTER else Paint.Align.LEFT
            c.drawText(value,x,y,paint)
            paint.textAlign=Paint.Align.LEFT
        }
        private fun stroke(color:Int,width:Float){
            paint.shader=null
            paint.style=Paint.Style.STROKE
            paint.color=color
            paint.alpha=255
            paint.strokeWidth=width
            paint.strokeCap=Paint.Cap.ROUND
            paint.strokeJoin=Paint.Join.ROUND
        }
        private fun envelope(c:Canvas,x:Float,y:Float){
            stroke(Color.rgb(150,164,207),1.7f)
            c.drawRoundRect(RectF(x,y,x+21f,y+17f),1.5f,1.5f,paint)
            c.drawPath(Path().apply{moveTo(x+1f,y+2f);lineTo(x+10.5f,y+9f);lineTo(x+20f,y+2f)},paint)
        }
        private fun lock(c:Canvas,x:Float,y:Float){
            stroke(Color.rgb(150,164,207),1.8f)
            c.drawRoundRect(RectF(x+1f,y+8f,x+18f,y+21f),2f,2f,paint)
            c.drawArc(RectF(x+4f,y-1f,x+15f,y+14f),180f,180f,false,paint)
            c.drawLine(x+9.5f,y+13f,x+9.5f,y+17f,paint)
        }
        private fun eye(c:Canvas,x:Float,y:Float){
            stroke(Color.rgb(139,151,186),1.7f)
            c.drawPath(Path().apply{
                moveTo(x-10f,y);cubicTo(x-5f,y-10f,x+5f,y-10f,x+11f,y)
                cubicTo(x+5f,y+10f,x-5f,y+10f,x-10f,y)
            },paint)
            c.drawCircle(x+.5f,y,3.5f,paint)
            if(!revealPassword)c.drawLine(x-8f,y-8f,x+10f,y+9f,paint)
        }
        private fun person(c:Canvas,x:Float,y:Float){
            stroke(Color.rgb(150,164,207),1.8f)
            c.drawCircle(x,y-6f,5.5f,paint)
            c.drawPath(Path().apply{
                moveTo(x-10f,y+13f);lineTo(x-10f,y+9f)
                cubicTo(x-10f,y-1f,x+10f,y-1f,x+10f,y+9f);lineTo(x+10f,y+13f)
            },paint)
        }
        private fun phoneIcon(c:Canvas,x:Float,y:Float){
            stroke(Color.rgb(150,164,207),1.8f)
            c.drawPath(Path().apply{
                moveTo(x+3f,y);lineTo(x+8f,y+6f);lineTo(x+5f,y+10f)
                cubicTo(x+7f,y+14f,x+10f,y+17f,x+14f,y+18f)
                lineTo(x+18f,y+15f);lineTo(x+23f,y+20f)
                cubicTo(x+20f,y+29f,x+7f,y+24f,x+1f,y+12f)
                cubicTo(x-3f,y+5f,x-1f,y+1f,x+3f,y);close()
            },paint)
        }
        private fun globe(c:Canvas,x:Float,y:Float){
            stroke(Color.rgb(150,164,207),1.7f)
            c.drawCircle(x,y,11f,paint)
            c.drawOval(RectF(x-5f,y-11f,x+5f,y+11f),paint)
            c.drawLine(x-11f,y,x+11f,y,paint)
        }
        private fun googleMark(c:Canvas,x:Float,y:Float){
            val rect=RectF(x-10f,y-10f,x+10f,y+10f)
            stroke(Color.rgb(66,133,244),5f)
            paint.strokeCap=Paint.Cap.BUTT
            c.drawArc(rect,0f,45f,false,paint)
            paint.color=Color.rgb(52,168,83)
            c.drawArc(rect,45f,100f,false,paint)
            paint.color=Color.rgb(251,188,5)
            c.drawArc(rect,145f,65f,false,paint)
            paint.color=Color.rgb(234,67,53)
            c.drawArc(rect,210f,105f,false,paint)
            paint.color=Color.rgb(66,133,244)
            c.drawLine(x+10f,y,x+1f,y,paint)
        }
    }
}
