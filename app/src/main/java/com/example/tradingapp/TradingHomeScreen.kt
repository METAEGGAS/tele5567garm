@file:Suppress("FunctionName", "unused")
package com.example.tradingapp

import android.content.Context
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.decode.SvgDecoder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.*
import kotlin.random.Random
import org.json.JSONArray
import org.json.JSONObject
import android.graphics.Paint as NativePaint

const val AVATAR_URL = "https://i.ibb.co/Z68jNfbd/IMG-20250906-195228-439.jpg"
const val AVATAR_FRAME_URL = "https://i.ibb.co/5xvxC8Kw/file-00000000d4f882439f813f969679659f.png"
const val CHART_BG_URL = "https://i.ibb.co/zWHWPv92/file-0000000090c48211944cf52560e3482a.png"
const val GIFT_ICON_URL = "https://cdn-icons-png.flaticon.com/128/18546/18546266.png"

object C {
    val BG=Color(0xFF141126); val Surface=Color(0xFF24203D); val SurfaceRaised=Color(0xFF30284F)
    val Cyan=Color(0xFF58D9F5); val Green=Color(0xFF39FF14); val Red=Color(0xFFFF1A1A)
    val Blue=Cyan; val Yellow=Color(0xFFFFD34D); val TextPrimary=Color(0xFFF5F2FF)
    val TextSecondary=Color(0xFFA9A5C6); val Divider=Color(0xFF3D355A); val Grid=Color(0xFF67558D)
    val PriceTag=Cyan; val PkBg=BG; val PkSurface=Surface; val PkTab=Surface; val PkTabOn=Cyan
    val PkStarOn=Color(0xFFFFD700); val PkPct=Green; val NotifyGreen=Color(0xFF0EA751)
    val BarBg=Color(0xFF344766); val BarOpenGreen=Color(0xFF00E676); val ChartBg=Color(0xFF3C3861)
}
private object Ref {
    val Background=Color(0xFF2B2659); val Border=Color(0xFF223252)
    val Menu=Color(0xFF141A2B); val Row=Color(0xFF0F1524)
    val Muted=Color(0xFFC7D5EF); val Green=Color(0xFF2EBD85)
    val TimeButton=Color(0xFF3E434D); val TimeOn=Color(0xFF007AFF)
    val Entrance=object:Easing {
        // CSS cubic-bezier(.2,1.1,.35,1), with overshoot retained.
        override fun transform(fraction:Float):Float {
            val x=fraction.coerceIn(0f,1f); var lo=0f; var hi=1f
            repeat(22) { val t=(lo+hi)/2; val u=1-t
                if (3*u*u*t*.2f+3*u*t*t*.35f+t*t*t<x) lo=t else hi=t }
            val t=(lo+hi)/2; val u=1-t
            return 3*u*u*t*1.1f+3*u*t*t+t*t*t
        }
    }
    val CssEase=CubicBezierEasing(.25f,.1f,.25f,1f)
    const val NotifyDelay=1500L
    const val NotifyRemove=400L
    const val ResultDuration=4500L
}
private const val MINUTE_MS=60_000L
private const val MIN_PRICE_SPAN=0.00001
private const val LIVE_CANDLE_FRACTION=.80
private const val MINBW=2f
private const val PAD=6f
private const val PINCH_AMPLIFY=4.0
private const val ZOOM_SMOOTH=.22f
private const val INERTIA_DECAY=.93
private const val MIN_VISIBLE_BARS_LIMIT=10.0
private const val MAX_VISIBLE_BARS_LIMIT=120.0
private const val DV_ALPHA=.20f
private val ids=AtomicLong(System.currentTimeMillis())
private fun nextId()=ids.incrementAndGet()
private fun barPosition(time:Long,origin:Long)=(time-origin).toDouble()/MINUTE_MS

data class Candle(val open:Float,val high:Float,val low:Float,val close:Float,val time:Long=0L)
fun clockNow():String=SimpleDateFormat("HH:mm:ss",Locale.US).apply { timeZone=TimeZone.getTimeZone("GMT+3") }.format(Date())
fun fmt(price:Float)=String.format(Locale.US,"%.5f",price)
private fun priceText(price:Double,decimals:Int)=String.format(Locale.US,"%.${decimals}f",price)
private fun candleTimeText(time:Long)=SimpleDateFormat("HH:mm",Locale.US).apply { timeZone=TimeZone.getTimeZone("GMT+3") }.format(Date(time))
private fun remainingSeconds(openTime:Long,now:Long)=ceil((openTime+MINUTE_MS-now)/1000.0).toInt().coerceIn(0,60)
private fun money(value:Double)=String.format(Locale.US,"%,.2f",value)
private fun shortAmount(value:Double)=if(value==floor(value)) value.toLong().toString() else value.toString()
private fun durationMillis(text:String):Long? {
    val parts=text.trim().split(":")
    if(parts.size !in 1..3) return null
    val v=parts.map { it.toLongOrNull()?.takeIf { n->n in 0..999999 } ?: return null }
    val seconds=when(v.size) { 3->v[0]*3600+v[1]*60+v[2]; 2->v[0]*60+v[1]; else->v[0] }
    return (seconds*1000).takeIf { it>0 }
}
object PairSession {
    fun load(context:Context)=context.getSharedPreferences("trading_session",Context.MODE_PRIVATE).getString("last_symbol",null)?.uppercase(Locale.US)?.replace(Regex("[_/\\-]"),"")
    fun save(context:Context,symbol:String) { context.getSharedPreferences("trading_session",Context.MODE_PRIVATE).edit().putString("last_symbol",symbol.uppercase(Locale.US)).apply() }
}
// Symbol catalog is fetched once, saved locally, then refreshed silently in the background.
object PairCatalogStore {
    var allSymbols by mutableStateOf<List<String>>(emptyList())
        private set
    var connected by mutableStateOf<Set<String>>(emptySet())
        private set
    var loaded by mutableStateOf(false)
        private set
    private var app:Context?=null
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private var refreshing=false
    fun load(context:Context) {
        if(app==null) { app=context.applicationContext
            val p=app!!.getSharedPreferences("pair_catalog_v1",Context.MODE_PRIVATE)
            val savedAll=p.getString("all","")?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
            val savedConn=p.getString("connected","")?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
            if(savedAll.isNotEmpty()) { allSymbols=savedAll;connected=savedConn.toSet();loaded=true }
        }
        refresh()
    }
    fun refresh() {
        if(refreshing)return;refreshing=true
        scope.launch {
            val all=BiquoteApi.symbols();val live=BiquoteApi.symbols(true).toSet()
            if(all.isNotEmpty()) { allSymbols=all;if(live.isNotEmpty())connected=live;loaded=true
                app?.getSharedPreferences("pair_catalog_v1",Context.MODE_PRIVATE)?.edit()?.putString("all",all.joinToString(","))?.putString("connected",connected.joinToString(","))?.apply()
            }
            refreshing=false
        }
    }
}
object IconProvider {
    private val FLAGS=mapOf("EUR" to "eu","USD" to "us","GBP" to "gb","JPY" to "jp","CHF" to "ch","AUD" to "au","CAD" to "ca","NZD" to "nz","TRY" to "tr","ZAR" to "za","MXN" to "mx","SEK" to "se","NOK" to "no","SGD" to "sg","HKD" to "hk","PLN" to "pl","DKK" to "dk","HUF" to "hu","CZK" to "cz","ILS" to "il","CNY" to "cn","QAR" to "qa","SAR" to "sa","UAH" to "ua","IDR" to "id","AED" to "ae","THB" to "th","MYR" to "my","PHP" to "ph","INR" to "in","KRW" to "kr","BRL" to "br","ARS" to "ar","CLP" to "cl","COP" to "co","PEN" to "pe","RUB" to "ru")
    private fun flags(code:String)=listOf("https://flagcdn.com/$code.svg","https://flagcdn.com/w160/$code.png","https://flagcdn.com/w80/$code.png")
    // Deterministic random artwork for assets that have no official icon.
    private fun randomIcon(seed:String):String {
        var h=0;for(c in seed)h=h*31+c.code
        val style=listOf("identicon","shapes","bottts-neutral","rings")[abs(h)%4]
        return "https://api.dicebear.com/9.x/$style/png?seed=${URLEncoder.encode(seed,"UTF-8")}"
    }
    fun getPairIcons(symbol:String):List<List<String>> {
        val s=symbol.uppercase(Locale.US).replace(Regex("[_/\\-]"),"")
        if(s.length==6) { val a=FLAGS[s.take(3)]; val b=FLAGS[s.drop(3)]; if(a!=null&&b!=null) return listOf(flags(a)+randomIcon(s.take(3)),flags(b)+randomIcon(s.drop(3))) }
        val id=mapOf("XAUUSD" to 2933245,"XAGUSD" to 2933253,"USOIL" to 2933251,"UKOIL" to 2933251,"NGAS" to 2933254,"US30" to 2933255,"US500" to 2933256,"USTEC" to 2933257)[s]
        if(id!=null) return listOf(listOf("https://cdn-icons-png.flaticon.com/512/2933/$id.png"))
        if(s.endsWith("USDT")||s.endsWith("USD")&&s.length>3) {
            val coin=s.removeSuffix(if(s.endsWith("USDT")) "USDT" else "USD").lowercase(Locale.US)
            return listOf(listOf("https://cdn.jsdelivr.net/npm/cryptocurrency-icons@0.18.1/svg/color/$coin.svg","https://cdn.jsdelivr.net/npm/cryptocurrency-icons@0.18.1/128/color/$coin.png",randomIcon(s)))
        }
        return listOf(listOf("https://assets.parqet.com/logos/symbol/$s?format=png",randomIcon(s)))
    }
}
@Composable fun CurrencyIcon(urls:List<String>,label:String,modifier:Modifier=Modifier) {
    var failed by remember(urls) { mutableIntStateOf(0) }
    val context=LocalContext.current
    val imageLoader=remember(context) { ImageLoader.Builder(context.applicationContext).components { add(SvgDecoder.Factory()) }.build() }
    if(failed<urls.size) AsyncImage(model=urls[failed],imageLoader=imageLoader,contentDescription=label,contentScale=ContentScale.Crop,modifier=modifier.clip(CircleShape).background(C.Surface),onError={failed++})
    else Box(modifier.clip(CircleShape).background(C.SurfaceRaised).border(1.dp,C.Cyan.copy(alpha=.55f),CircleShape),contentAlignment=Alignment.Center) { Text(label.take(2),color=C.Cyan,fontSize=9.sp,fontWeight=FontWeight.ExtraBold) }
}

object BiquoteApi {
    private fun get(path:String,params:List<Pair<String,String>> = emptyList()):String? {
        val qs=if(params.isEmpty()) "" else "?"+params.joinToString("&") { "${it.first}=${URLEncoder.encode(it.second,"UTF-8")}" }
        var c:HttpURLConnection?=null
        return try { c=(URL("https://biquote.io$path$qs").openConnection() as HttpURLConnection).apply { connectTimeout=6000; readTimeout=6000; requestMethod="GET" }
            if(c.responseCode in 200..299) c.inputStream.bufferedReader().use { it.readText() } else null
        } catch(_:Exception) { null } finally { c?.disconnect() }
    }
    suspend fun tick(symbol:String):Double?=withContext(Dispatchers.IO) {
        val txt=get("/api/${symbol.uppercase(Locale.US)}",listOf("allowStale" to "false")) ?: return@withContext null
        try { JSONObject(txt).optDouble("mid",Double.NaN).takeIf { it.isFinite()&&it>0 } } catch(_:Exception) { null }
    }
    suspend fun ohlc(symbol:String,interval:String="1m",limit:Int=300):List<Candle> = withContext(Dispatchers.IO) {
        val txt=get("/api/${symbol.uppercase(Locale.US)}/ohlc",listOf("interval" to interval,"limit" to "$limit")) ?: return@withContext emptyList()
        try { val a=JSONObject(txt).optJSONArray("bars") ?: return@withContext emptyList()
            (0 until a.length()).mapNotNull { i->
                val b=a.optJSONObject(i) ?: return@mapNotNull null
                val raw=b.opt("openTime") ?: b.opt("time") ?: b.opt("t")
                var t=when(raw) { is Number->raw.toLong(); is String->raw.toLongOrNull() ?: runCatching { java.time.Instant.parse(raw).toEpochMilli() }.getOrDefault(0); else->0 }
                if(t in 1..9999999999L) t*=1000
                Candle(b.optDouble("open",b.optDouble("o")).toFloat(),b.optDouble("high",b.optDouble("h")).toFloat(),b.optDouble("low",b.optDouble("l")).toFloat(),b.optDouble("close",b.optDouble("c")).toFloat(),t)
            }.sortedBy { it.time }
        } catch(_:Exception) { emptyList() }
    }
    suspend fun symbols(liveOnly:Boolean=false):List<String> = withContext(Dispatchers.IO) {
        val txt=get("/api/symbols",listOf("liveOnly" to "$liveOnly")) ?: return@withContext emptyList()
        try { val a=JSONArray(txt); (0 until a.length()).mapNotNull { i->
            val s=when(val v=a.opt(i)) { is String->v; is JSONObject->v.optString("name",v.optString("symbol","")); else->"" }
            s.takeIf { it.isNotBlank() }?.uppercase(Locale.US)?.replace(Regex("[_/\\-]"),"")
        }.distinct() } catch(_:Exception) { emptyList() }
    }
}
object SymCatalog {
    private val forex="EUR USD GBP JPY CHF AUD CAD NZD TRY ZAR MXN SEK NOK SGD HKD PLN DKK HUF CZK ILS CNY QAR SAR UAH IDR THB MYR PHP INR KRW BRL ARS CLP COP PEN RUB AED".split(" ").toSet()
    private val crypto="BTC ETH SOL XRP BNB ADA DOGE DOT MATIC AVAX LINK LTC TRX ATOM ETC XLM ALGO VET FIL NEAR APT ARB OP INJ SUI SEI TIA TON PEPE SHIB".split(" ")
    private val commodity="XAU XAG XPT XPD USOIL UKOIL WTI BRENT NGAS".split(" ")
    fun isForex(s:String)=s.length==6&&forex.any { s.startsWith(it) }
    fun isCrypto(s:String)=!isForex(s)&&crypto.any { s.startsWith(it) }
    fun isCommodity(s:String)=!isForex(s)&&!isCrypto(s)&&commodity.any { s.startsWith(it) }
    fun isIndex(s:String)=s in setOf("US30","US500","USTEC","UK100","GER40","FRA40","JP225")
    fun isStock(s:String)=Regex("^[A-Z]{1,5}$").matches(s)&&!isForex(s)&&!isCrypto(s)&&!isCommodity(s)&&!isIndex(s)
    fun pretty(s:String)=if(s.length==6) "${s.take(3)}/${s.drop(3)}" else s
    fun payout(s:String):Int { var h=0L; for(c in s) h=(h*31+c.code) and 0xFFFFFFFFL; return 80+(h%16).toInt() }
}

// Local simulation only: no deposit, withdrawal or order is sent to a broker.
object AccountStore {
    var selected by mutableStateOf("real")
        private set
    var real by mutableDoubleStateOf(.39)
        private set
    var demo by mutableDoubleStateOf(50000.0)
        private set
    private var app:Context?=null
    fun load(context:Context) { if(app!=null) return; app=context.applicationContext
        val p=app!!.getSharedPreferences("qt-reference-accounts-v1",Context.MODE_PRIVATE)
        selected=p.getString("selected","real").let { if(it=="demo") "demo" else "real" }
        real=p.getString("real","0.39")?.toDoubleOrNull()?.takeIf { it.isFinite()&&it>=0 } ?: .39
        demo=p.getString("demo","50000")?.toDoubleOrNull()?.takeIf { it.isFinite()&&it>=0 } ?: 50000.0
    }
    fun balance(account:String=selected)=if(account=="demo") demo else real
    private fun setBalance(account:String,value:Double) { if(account=="demo") demo=value.coerceAtLeast(0.0) else real=value.coerceAtLeast(0.0); save() }
    fun select(value:String) { selected=if(value=="demo") "demo" else "real"; save() }
    fun debit(account:String,value:Double)=setBalance(account,balance(account)-value)
    fun credit(account:String,value:Double)=setBalance(account,balance(account)+value)
    private fun save() { app?.getSharedPreferences("qt-reference-accounts-v1",Context.MODE_PRIVATE)?.edit()?.putString("selected",selected)?.putString("real",real.toString())?.putString("demo",demo.toString())?.apply() }
}
object TradeSettings { var duration by mutableStateOf("00:01:00"); var amount by mutableStateOf("1") }
data class OpenTrade(val id:Long=nextId(),val symbol:String,val direction:Int,val entry:Float,val amount:Double,val openedAt:Long,val expiresAt:Long,val payoutPct:Int,val account:String=AccountStore.selected,val candleTime:Long=openedAt/MINUTE_MS*MINUTE_MS,val exactEntry:Double=entry.toDouble())
data class TradeResult(val trade:OpenTrade,val close:Double,val profit:Double,val closedAt:Long,val visibleUntil:Long=closedAt+Ref.ResultDuration)
data class Quote(val price:Double,val receivedAt:Long)
object TradeStore {
    val currentSymbol=MutableStateFlow("EURUSD"); val livePrice=MutableStateFlow(0f)
    val quotes=mutableStateMapOf<String,Quote>()
    val openTrades=mutableStateListOf<OpenTrade>(); val closedTrades=mutableStateListOf<Pair<OpenTrade,Boolean>>()
    val results=mutableStateListOf<TradeResult>(); val history=mutableStateListOf<TradeResult>()
    fun quote(symbol:String,value:Double) { if(value.isFinite()&&value>0) { quotes[symbol]=Quote(value,System.currentTimeMillis()); if(currentSymbol.value==symbol) livePrice.value=value.toFloat() } }
}

data class TradeNotification(val id:Long,val symbol:String,val price:String,val createdAt:Long=System.currentTimeMillis()) { var visible by mutableStateOf(false); var closing by mutableStateOf(false) }
object NotificationManager {
    val notifications=mutableStateListOf<TradeNotification>()
    private val waiting=ArrayDeque<Pair<String,String>>()
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    fun show(symbol:String,price:String) { waiting.addLast(symbol to price); runQueue() }
    private fun runQueue() { while(notifications.size<3&&waiting.isNotEmpty()) { val(s,p)=waiting.removeFirst(); val n=TradeNotification(nextId(),s,p); notifications.add(n)
        scope.launch { delay(Ref.NotifyDelay); dismiss(n.id) }
    } }
    fun dismiss(id:Long) { val n=notifications.firstOrNull { it.id==id } ?: return; if(n.closing)return
        n.closing=true; n.visible=false
        scope.launch { delay(Ref.NotifyRemove); notifications.remove(n); runQueue() }
    }
}
object TradeMessage {
    var text by mutableStateOf("")
        private set
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate); private var job:Job?=null
    fun show(message:String) { text=message; job?.cancel(); job=scope.launch { delay(2300); text="" } }
}
object TradeSounds {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default); private const val SR=44100
    private fun playTones(tones:List<Triple<Float,Float,Pair<Float,String>>>) { scope.launch {
        val total=tones.maxOf { (it.second+it.third.first*1000).toInt() }+60
        val buf=ShortArray(SR*total/1000)
        for((freq,start,dw) in tones) { val(dur,wave)=dw; val begin=(start/1000*SR).toInt()
            for(i in 0 until (dur*SR).toInt()) { val pos=begin+i; if(pos>=buf.size)break
                val t=i.toDouble()/SR
                // A quiet sine tap with a smooth attack/release avoids the sharp opening click.
                // Other trade-result sounds keep their original envelopes.
                val env=if(wave=="soft") .48*sin(PI*(t/dur).coerceIn(0.0,1.0)).pow(2.0)*exp(-t*10.0) else .9.pow(i/(SR*.08))
                val x=2*PI*freq*t
                val s=when(wave) { "square"->if(sin(x)>=0)1.0 else -1.0; "sawtooth"->2*(t*freq-floor(.5+t*freq)); "triangle"->2/PI*asin(sin(x)); else->sin(x) }
                buf[pos]=(buf[pos]+(s*env*.18*Short.MAX_VALUE).toInt()).coerceIn(-32768,32767).toShort()
            }
        }
        var track:AudioTrack?=null
        try { track=AudioTrack(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SR).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),buf.size*2,AudioTrack.MODE_STATIC,0)
            track.write(buf,0,buf.size); track.play(); delay(total+100L)
        } catch(_:Exception) { } finally { track?.release() }
    } }
    fun open()=playTones(listOf(Triple(540f,0f,.10f to "soft"),Triple(810f,20f,.09f to "soft")))
    fun win()=playTones(listOf(Triple(784f,0f,.28f to "sine"),Triple(1046f,90f,.28f to "sine"),Triple(1318f,180f,.28f to "sine"),Triple(1568f,270f,.40f to "sine")))
    fun lose()=playTones(listOf(Triple(392f,0f,.30f to "sawtooth"),Triple(294f,120f,.30f to "sawtooth"),Triple(196f,240f,.30f to "sawtooth")))
}
private fun openTrade(direction:Int,symbol:String,payout:Int) {
    val amount=TradeSettings.amount.toDoubleOrNull()
    if(amount==null||!amount.isFinite()||amount<=0) { TradeMessage.show("أدخل مبلغ صفقة صحيح"); return }
    val duration=durationMillis(TradeSettings.duration)
    if(duration==null) { TradeMessage.show("أدخل زمناً صحيحاً"); return }
    if(amount>AccountStore.balance()+1e-8) { TradeMessage.show("لا يوجد رصيد كافٍ في حساب "+if(AccountStore.selected=="demo") "الديمو" else "الحقيقي"); return }
    val now=System.currentTimeMillis(); val quote=TradeStore.quotes[symbol]
    if(quote==null||now-quote.receivedAt>15000) { TradeMessage.show("انتظر وصول سعر حديث لهذا الزوج"); return }
    AccountStore.debit(AccountStore.selected,amount)
    TradeStore.openTrades.add(OpenTrade(symbol=symbol,direction=direction,entry=quote.price.toFloat(),amount=amount,openedAt=now,expiresAt=now+duration,payoutPct=payout,exactEntry=quote.price))
    TradeSounds.open()
    val decimals=if(symbol.contains("JPY"))3 else if(symbol=="XAUUSD")2 else 5
    NotificationManager.show(symbol,priceText(quote.price,decimals))
}
@Composable private fun TradeSettlementLoop() {
    // A quote for the selected pair is NEVER used to settle another pair.
    LaunchedEffect(Unit) {
        while(isActive) {
            val now=System.currentTimeMillis()
            val symbols=TradeStore.openTrades.map { it.symbol }.distinct()
            supervisorScope { symbols.map { symbol->async {
                val cached=TradeStore.quotes[symbol]
                if(symbol!=TradeStore.currentSymbol.value||cached==null||now-cached.receivedAt>3000) {
                    BiquoteApi.tick(symbol)?.let { TradeStore.quote(symbol,it) }
                }
            } }.awaitAll() }
            for(tr in TradeStore.openTrades.toList()) {
                if(tr.expiresAt>System.currentTimeMillis())continue
                val q=TradeStore.quotes[tr.symbol] ?: continue
                // Wait for a quote received after expiry; do not fabricate a closing price.
                if(q.receivedAt<tr.expiresAt)continue
                val entry=tr.exactEntry; val win=if(tr.direction>0) q.price>entry else q.price<entry
                val tie=abs(q.price-entry)<1e-10
                val profit=if(tie)0.0 else if(win)tr.amount*tr.payoutPct/100 else -tr.amount
                val result=TradeResult(tr,q.price,profit,System.currentTimeMillis())
                TradeStore.openTrades.remove(tr); TradeStore.closedTrades.add(tr to (win&&!tie))
                TradeStore.results.add(result); TradeStore.history.add(result)
                if(tie)AccountStore.credit(tr.account,tr.amount) else if(win)AccountStore.credit(tr.account,tr.amount+profit)
                if(win&&!tie)TradeSounds.win() else TradeSounds.lose()
            }
            TradeStore.results.removeAll { it.visibleUntil<=System.currentTimeMillis() }
            delay(200)
        }
    }
}

@Composable fun TradingHomeScreen() {
    val context=LocalContext.current
    var pickerOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        AccountStore.load(context)
        val saved=PairSession.load(context); val live=BiquoteApi.symbols(true)
        TradeStore.currentSymbol.value=when { saved!=null&&saved in live->saved; live.isNotEmpty()->live.first(); saved!=null->saved; else->TradeStore.currentSymbol.value }
        TradeStore.currentSymbol.collect { PairSession.save(context,it) }
    }
    TradeSettlementLoop()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(C.BG)) {
            TopBar()
            Box(Modifier.fillMaxWidth().height(1.dp).background(C.Divider))
            LiveCandlestickChart(Modifier.weight(1f).fillMaxWidth()) { pickerOpen=true }
            TradePanel(); BottomNavBar()
        }
        val message=TradeMessage.text
        if(message.isNotEmpty()) Box(Modifier.align(Alignment.TopCenter).padding(top=110.dp,start=18.dp,end=18.dp).shadow(12.dp,RoundedCornerShape(10.dp)).background(Color(0xFF202638),RoundedCornerShape(10.dp)).border(1.dp,Color(0xFFF05B67),RoundedCornerShape(10.dp)).padding(horizontal=18.dp,vertical=13.dp)) { Text(message,color=Color.White,fontSize=14.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center) }
    }
    if(pickerOpen)PairPickerSheet { pickerOpen=false }
}

@Composable fun TopBar() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(Modifier.fillMaxWidth().background(C.BG).padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(C.Yellow),contentAlignment=Alignment.Center) { WalletIcon(Modifier.size(26.dp),Color(0xFF312740)) }
            Spacer(Modifier.width(8.dp))
            BalanceAnchor()
            Spacer(Modifier.weight(1f))
            Box {
                AsyncImage(model=GIFT_ICON_URL,contentDescription="هدية",contentScale=ContentScale.Fit,modifier=Modifier.size(36.dp))
                Box(Modifier.align(Alignment.TopEnd).offset(3.dp,(-3).dp).size(14.dp).clip(CircleShape).background(C.BG).border(1.dp,C.Divider,CircleShape)) { Canvas(Modifier.fillMaxSize()) {
                    val m=size.minDimension*.22f; val cx=size.width/2; val cy=size.height/2
                    drawLine(Color.White,Offset(cx-m,cy-m),Offset(cx+m,cy+m),1.dp.toPx(),StrokeCap.Round)
                    drawLine(Color.White,Offset(cx+m,cy-m),Offset(cx-m,cy+m),1.dp.toPx(),StrokeCap.Round)
                } }
            }
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(52.dp),contentAlignment=Alignment.Center) {
                AsyncImage(model=AVATAR_URL,contentDescription="الصورة الشخصية",contentScale=ContentScale.Crop,modifier=Modifier.size(36.dp).clip(CircleShape).background(C.Surface))
                AsyncImage(model=AVATAR_FRAME_URL,contentDescription=null,contentScale=ContentScale.Fit,modifier=Modifier.size(52.dp))
            }
        }
    }
}
private class ReferencePopupPosition(private val above:Boolean,private val density:Float):PopupPositionProvider {
    override fun calculatePosition(anchorBounds:IntRect,windowSize:IntSize,layoutDirection:LayoutDirection,popupContentSize:IntSize):IntOffset {
        fun dp(n:Int)=(n*density).roundToInt()
        val w=popupContentSize.width; val h=popupContentSize.height
        val x=(anchorBounds.right-w).coerceIn(dp(8),(windowSize.width-w-dp(8)).coerceAtLeast(dp(8)))
        val desired=if(above)anchorBounds.top-h-dp(9) else anchorBounds.bottom+dp(8)
        val y=if(above&&desired<dp(6)) min(anchorBounds.bottom+dp(9),windowSize.height-h-dp(6)) else desired
        return IntOffset(x,y.coerceIn(dp(6),(windowSize.height-h-dp(6)).coerceAtLeast(dp(6))))
    }
}
@Composable private fun BalanceAnchor() {
    var expanded by remember { mutableStateOf(false) }
    val density=LocalDensity.current; val config=LocalConfiguration.current
    Box {
        Row(Modifier.clip(RoundedCornerShape(12.dp)).background(C.Surface).clickable { expanded=!expanded }.padding(horizontal=14.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
            Column {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(if(AccountStore.selected=="demo")"QT Demo" else "QT Real",color=C.TextSecondary,fontSize=10.sp); Spacer(Modifier.width(6.dp))
                    Text("حلال",color=C.Green,fontSize=10.sp); Spacer(Modifier.width(6.dp)); Text("USD",color=C.TextSecondary,fontSize=10.sp)
                }
                Text(money(AccountStore.balance()),color=C.TextPrimary,fontSize=20.sp,fontWeight=FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp)); DownArrow(Modifier.size(10.dp))
        }
        if(expanded)Popup(popupPositionProvider=remember(density.density) { ReferencePopupPosition(false,density.density) },onDismissRequest={expanded=false},properties=PopupProperties(focusable=true)) {
            val width=min(248f,config.screenWidthDp*.78f).dp
            Column(Modifier.width(width).shadow(20.dp,RoundedCornerShape(12.dp)).background(Ref.Menu,RoundedCornerShape(12.dp)).border(1.dp,Ref.Border,RoundedCornerShape(12.dp)).padding(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                AccountRow("real") { AccountStore.select("real"); expanded=false }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF00A63E)).clickable { TradeMessage.show("لشحن الحساب غير متاح للتنفيذ في هذه النسخة"); expanded=false },verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                        ReferenceGlyph("wallet",Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text("لشحن الحساب",color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold)
                    }
                    for(kind in listOf("download","dollar"))Box(Modifier.size(46.dp,40.dp).clip(RoundedCornerShape(8.dp)).border(1.dp,Color(0xFF2B6CB0),RoundedCornerShape(8.dp)).clickable { TradeMessage.show("هذه الخدمة غير متاحة للتنفيذ في هذه النسخة"); expanded=false },contentAlignment=Alignment.Center) { ReferenceGlyph(kind,Modifier.size(20.dp)) }
                }
                AccountRow("demo") { AccountStore.select("demo"); expanded=false }
                Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp)).background(Ref.Row).border(1.dp,Ref.Border,RoundedCornerShape(10.dp)).clickable { expanded=false; TradeMessage.show("الملف الشخصي غير متاح للتنفيذ في هذه النسخة") },contentAlignment=Alignment.Center) { Text("الملف الشخصي »",color=Ref.Muted,fontSize=12.sp,fontWeight=FontWeight.Bold) }
            }
        }
    }
}
@Composable private fun AccountRow(account:String,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Ref.Row).border(1.dp,if(AccountStore.selected==account)Ref.Green else Ref.Border,RoundedCornerShape(10.dp)).clickable(onClick=onClick).padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        ReferenceGlyph(if(account=="demo")"demo" else "coins",Modifier.size(22.dp),Ref.Muted)
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) { Text(if(account=="demo")"QT Demo" else "QT Real",color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Bold); Text("$"+money(AccountStore.balance(account)),color=Ref.Muted,fontSize=12.sp) }
        if(account=="real")Canvas(Modifier.size(48.dp,25.dp)) {
            drawRoundRect(Color(0xFF3A5B8C),size=size,cornerRadius=CornerRadius(8.dp.toPx()),style=Stroke(1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(),3.dp.toPx()))))
            val p=NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.WHITE; textSize=11.dp.toPx(); textAlign=NativePaint.Align.CENTER; typeface=Typeface.DEFAULT_BOLD }
            drawContext.canvas.nativeCanvas.drawText("USD",size.width/2,size.height/2-(p.ascent()+p.descent())/2,p)
        }
    }
}

private fun mergeCandle(list:MutableList<Candle>,incoming:Candle,bulk:Boolean=false):Boolean {
    val c=incoming.close
    if(!c.isFinite()||c<=0||incoming.time<=0)return false
    var o=incoming.open.takeIf { it.isFinite()&&it>0 } ?: c
    var h=incoming.high.takeIf { it.isFinite()&&it>0 } ?: max(o,c)
    var l=incoming.low.takeIf { it.isFinite()&&it>0 } ?: min(o,c)
    h=maxOf(h,o,c); l=minOf(l,o,c)
    val t=incoming.time
    if(list.isEmpty()) { list.add(Candle(o,h,l,c,t)); return true }
    var lo=0; var hi=list.lastIndex; var exact=-1; var insert=list.size
    while(lo<=hi) { val mid=(lo+hi) ushr 1; val mt=list[mid].time
        when { mt==t->{exact=mid;break}; mt<t->lo=mid+1; else->{insert=mid;hi=mid-1} }
    }
    if(exact>=0) { val e=list[exact]; list[exact]=e.copy(high=max(e.high,h),low=min(e.low,l),close=c); return false }
    if(insert>=list.size) { o=list.last().close; h=max(h,o); l=min(l,o); list.add(Candle(o,h,l,c,t)) }
    else list.add(insert,Candle(o,h,l,c,t))
    return true
}
private data class ChartViewport(val visibleBars:Double,val anchorBar:Double,val anchorFraction:Double,val centerPrice:Double,val priceSpan:Double) {
    val centerBar get()=anchorBar+(.5-anchorFraction)*visibleBars
    val minPrice get()=centerPrice-priceSpan/2
    val maxPrice get()=centerPrice+priceSpan/2
    fun fractionFor(bar:Double)=anchorFraction+(bar-anchorBar)/visibleBars
    fun reanchor(bar:Double)=copy(anchorBar=bar,anchorFraction=fractionFor(bar))
}
private fun initialViewport(candles:List<Candle>,origin:Long):ChartViewport {
    if(candles.isEmpty())return ChartViewport(60.0,0.0,LIVE_CANDLE_FRACTION,1.0,MIN_PRICE_SPAN)
    val a=candles.takeLast(min(60,candles.size)); val low=a.minOf { it.low.toDouble() }; val high=a.maxOf { it.high.toDouble() }
    return ChartViewport(60.0,barPosition(candles.last().time,origin),LIVE_CANDLE_FRACTION,(low+high)/2,((high-low)/.70*1.04).coerceAtLeast(MIN_PRICE_SPAN))
}
// Do not allow panning into future space beyond the initial 80% live-candle position.
// History remains scrollable in both directions up to this live boundary.
private fun boundedChartCenter(view:ChartViewport,desired:Double,first:Double,last:Double):Double {
    val liveCenter=last+(.5-LIVE_CANDLE_FRACTION)*view.visibleBars
    val historyCenter=min(first+view.visibleBars/2-1,liveCenter)
    return desired.coerceIn(historyCenter,liveCenter)
}
private fun ChartViewport.atLiveBoundary(last:Double)=
    abs(centerBar-(last+(.5-LIVE_CANDLE_FRACTION)*visibleBars))<.000001
private fun verticalTarget(view:ChartViewport,candles:List<Candle>,origin:Long):Pair<Double,Double> {
    if(candles.isEmpty())return view.centerPrice to view.priceSpan
    val visible=candles.filter { abs(barPosition(it.time,origin)-view.centerBar)<=view.visibleBars/2+.6 }
    if(visible.isEmpty())return view.centerPrice to view.priceSpan
    val low=visible.minOf { it.low.toDouble() }; val high=visible.maxOf { it.high.toDouble() }
    return (high+low)/2 to ((high-low)/.70*1.04).coerceAtLeast(MIN_PRICE_SPAN)
}
private fun nicePriceStep(raw:Double):Double {
    if(!raw.isFinite()||raw<=0)return .001
    val m=10.0.pow(floor(log10(raw))); val n=raw/m
    return (when { n<=1->1.0;n<=2->2.0;n<=2.5->2.5;n<=5->5.0;else->10.0 })*m
}
private fun priceDecimals(step:Double):Int {
    for(d in 5..8) { val s=step*10.0.pow(d.toDouble()); if(abs(s-round(s))<.000001)return d }
    return 8
}
private fun niceTimeStep(raw:Double)=longArrayOf(1,2,5,10,15,20,30,60,120,240).firstOrNull { it>=raw } ?: 240L

@Composable fun PairHeader(modifier:Modifier=Modifier,onClick:()->Unit={}) {
    val sym by TradeStore.currentSymbol.collectAsState()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier,verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(C.Surface),contentAlignment=Alignment.Center) { DotsIcon(Modifier.size(16.dp)) }
            Spacer(Modifier.width(8.dp))
            Row(Modifier.clip(RoundedCornerShape(22.dp)).background(C.Surface).clickable(onClick=onClick).padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                val icons=IconProvider.getPairIcons(sym)
                Box(Modifier.width(34.dp).height(22.dp)) {
                    if(icons.isNotEmpty())CurrencyIcon(icons[0],sym,Modifier.size(22.dp).align(Alignment.CenterStart))
                    if(icons.size>1)CurrencyIcon(icons[1],sym.drop(3),Modifier.size(22.dp).align(Alignment.CenterEnd).border(1.5.dp,C.Surface,CircleShape))
                }
                Spacer(Modifier.width(8.dp)); Text("${SymCatalog.pretty(sym)} OTC",color=C.TextPrimary,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp)); DownArrow(Modifier.size(9.dp))
            }
        }
    }
}

@Composable fun LiveCandlestickChart(modifier:Modifier=Modifier,onOpenPairPicker:()->Unit) {
    val sym by TradeStore.currentSymbol.collectAsState()
    val candles=remember { mutableStateListOf<Candle>() }
    var displayCandle by remember { mutableStateOf<Candle?>(null) }
    var livePrice by remember { mutableFloatStateOf(0f) }
    var origin by remember { mutableLongStateOf(0L) }
    var viewport by remember { mutableStateOf(ChartViewport(60.0,0.0,LIVE_CANDLE_FRACTION,1.0,MIN_PRICE_SPAN)) }
    var bsTarget by remember { mutableDoubleStateOf(60.0) }
    var followLive by remember { mutableStateOf(true) }
    var interacting by remember { mutableStateOf(false) }
    var chartSize by remember { mutableStateOf(IntSize.Zero) }
    var secondsLeft by remember { mutableIntStateOf(60) }
    var clockText by remember { mutableStateOf(clockNow()) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var inertia by remember { mutableDoubleStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var retryKey by remember { mutableIntStateOf(0) }
    var everLoaded by remember { mutableStateOf(false) }
    val resultHits=remember { mutableListOf<Pair<Long,Rect>>() }
    val density=LocalDensity.current
    val axisTextPx=with(density) { 9.sp.toPx() }; val tagTextPx=with(density) { 10.sp.toPx() }
    val axisPaint=remember(axisTextPx) { NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { color=Color.White.toArgb();textSize=axisTextPx;textAlign=NativePaint.Align.RIGHT;typeface=Typeface.DEFAULT } }
    val timePaint=remember(axisTextPx) { NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { color=C.TextSecondary.toArgb();textSize=axisTextPx;textAlign=NativePaint.Align.CENTER } }
    val tagPaint=remember(tagTextPx) { NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { color=C.BG.toArgb();textSize=tagTextPx;textAlign=NativePaint.Align.CENTER;typeface=Typeface.DEFAULT_BOLD } }
    val detailPaint=remember(axisTextPx) { NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { color=C.Cyan.toArgb();textSize=axisTextPx;textAlign=NativePaint.Align.CENTER } }
    fun ppw()=(chartSize.width-6f-PAD).coerceAtLeast(1f)
    fun clampVisible(v:Double):Double {
        val perMin=((ppw()/(max(3f,16f*3.704f)+8f))*.9f).toDouble()
        val perMax=(ppw()/(max(3f,16f*.207f)+8f)).toDouble()
        val minBars=ppw()/perMax; val maxBars=ppw()/perMin
        val lower=max(MIN_VISIBLE_BARS_LIMIT,minBars); val upper=min(MAX_VISIBLE_BARS_LIMIT,maxBars).coerceAtLeast(lower)
        return v.coerceIn(lower,upper)
    }
    fun beginPinch() {
        if(candles.isEmpty())return
        val view=viewport; val live=barPosition(candles.last().time,origin)
        val pivot=if(view.fractionFor(live) in 0.0..1.0)live else candles.minByOrNull { abs(barPosition(it.time,origin)-view.centerBar) }?.let { barPosition(it.time,origin) } ?: view.centerBar
        viewport=view.reanchor(pivot); inertia=0.0
    }
    fun zoomOnly(factor:Double) {
        if(!factor.isFinite()||factor<=0)return
        val amp=if(factor>=1)1+(factor-1)*PINCH_AMPLIFY else 1/(1+(1/factor-1)*PINCH_AMPLIFY)
        val view=viewport; val next=clampVisible(view.visibleBars/amp)
        var zoomed=view.copy(visibleBars=next)
        if(candles.isNotEmpty()) {
            val first=barPosition(candles.first().time,origin); val last=barPosition(candles.last().time,origin)
            val center=boundedChartCenter(zoomed,zoomed.centerBar,first,last)
            if(abs(center-zoomed.centerBar)>1e-9)zoomed=zoomed.copy(anchorBar=center,anchorFraction=.5)
            followLive=zoomed.atLiveBoundary(last)
        }
        viewport=zoomed; bsTarget=next
        val(center,span)=verticalTarget(viewport,candles,origin); viewport=viewport.copy(centerPrice=center,priceSpan=span)
    }
    fun panOnly(dx:Float) {
        val width=chartSize.width.toDouble()
        if(width<=1||!dx.isFinite()||dx==0f||candles.isEmpty())return
        val view=viewport; val desired=view.centerBar-dx/width*view.visibleBars
        val first=barPosition(candles.first().time,origin); val last=barPosition(candles.last().time,origin)
        val center=boundedChartCenter(view,desired,first,last)
        val next=view.copy(anchorBar=center,anchorFraction=.5); val(pc,span)=verticalTarget(next,candles,origin)
        viewport=next.copy(centerPrice=pc,priceSpan=span)
        followLive=next.atLiveBoundary(last)
    }
    val onBegin by rememberUpdatedState<()->Unit>({interacting=true;inertia=0.0})
    val onEnd by rememberUpdatedState<()->Unit>({interacting=false})
    val onPinchStart by rememberUpdatedState<()->Unit>({beginPinch()})
    val onZoom by rememberUpdatedState<(Double)->Unit>({zoomOnly(it)})
    val onPan by rememberUpdatedState<(Float)->Unit>({ dx->
        val previousCenter=viewport.centerBar
        panOnly(dx)
        val moved=viewport.centerBar-previousCenter
        inertia=if(abs(moved)<1e-9)0.0 else inertia*.4+moved*.6
    })

    LaunchedEffect(Unit) {
        var last=System.nanoTime()
        while(isActive) {
            delay(16); val ns=System.nanoTime(); val df=((ns-last)/1_000_000.0).coerceAtMost(60.0); last=ns
            val view=viewport; val diff=bsTarget-view.visibleBars
            if(abs(diff)>.003)viewport=view.copy(visibleBars=view.visibleBars+diff*ZOOM_SMOOTH) else if(view.visibleBars!=bsTarget)viewport=view.copy(visibleBars=bsTarget)
            if(candles.isNotEmpty()) {
                val vp=viewport; val first=barPosition(candles.first().time,origin); val live=barPosition(candles.last().time,origin)
                val center=boundedChartCenter(vp,vp.centerBar,first,live)
                if(abs(center-vp.centerBar)>1e-9) {
                    viewport=vp.copy(anchorBar=center,anchorFraction=.5)
                    if(inertia>0.0)inertia=0.0
                }
                followLive=viewport.atLiveBoundary(live)
            }
            if(abs(inertia)>1e-5&&!interacting&&candles.isNotEmpty()) {
                val vp=viewport; val desired=vp.centerBar+inertia*df*.016
                val first=barPosition(candles.first().time,origin); val lastBar=barPosition(candles.last().time,origin)
                val center=boundedChartCenter(vp,desired,first,lastBar)
                if(center!=desired)inertia=0.0
                val next=vp.copy(anchorBar=center,anchorFraction=.5); val(pc,span)=verticalTarget(next,candles,origin)
                viewport=next.copy(centerPrice=pc,priceSpan=span);inertia*=INERTIA_DECAY.pow(df/16.7)
                followLive=next.atLiveBoundary(lastBar)
            }
        }
    }
    LaunchedEffect(sym,retryKey) {
        val started=System.currentTimeMillis();val initial=!everLoaded
        isLoading=true;loadError=false;candles.clear();displayCandle=null;livePrice=0f;TradeStore.livePrice.value=0f;followLive=true;inertia=0.0
        var success=false
        repeat(10) {
            if(!success) {
                val bars=BiquoteApi.ohlc(sym,"1m",300)
                if(bars.isNotEmpty()) {
                    origin=bars.first().time;for(b in bars)mergeCandle(candles,b,true)
                    while(candles.size>1500)candles.removeAt(0)
                    if(candles.isNotEmpty()) {
                        viewport=initialViewport(candles,origin);bsTarget=viewport.visibleBars;livePrice=candles.last().close
                        displayCandle=candles.last();success=true
                        // Historical OHLC is not represented as a fresh live quote.
                        BiquoteApi.tick(sym)?.let { TradeStore.quote(sym,it);livePrice=it.toFloat() }
                    }
                } else delay(1500)
            }
        }
        if(initial)delay((2150-(System.currentTimeMillis()-started)).coerceAtLeast(0))
        isLoading=false
        if(!success) { loadError=true;return@LaunchedEffect }
        everLoaded=true
        while(isActive) {
            delay(1000)
            BiquoteApi.tick(sym)?.let { tick->
                livePrice=tick.toFloat();TradeStore.quote(sym,tick)
                val lastC=candles.lastOrNull()
                // Update an existing candle only; never create synthetic OHLC history.
                if(lastC!=null&&System.currentTimeMillis()-lastC.time in 0 until MINUTE_MS) {
                    mergeCandle(candles,lastC.copy(high=max(lastC.high,livePrice),low=min(lastC.low,livePrice),close=livePrice))
                }
            }
            val lastBars=BiquoteApi.ohlc(sym,"1m",2)
            for(b in lastBars)mergeCandle(candles,b)
            while(candles.size>1500)candles.removeAt(0)
            if(followLive&&!interacting&&candles.isNotEmpty()) {
                val live=barPosition(candles.last().time,origin)
                val v=viewport.copy(anchorBar=live,anchorFraction=LIVE_CANDLE_FRACTION)
                val(pc,span)=verticalTarget(v,candles,origin);viewport=v.copy(centerPrice=pc,priceSpan=span)
            }
        }
    }
    LaunchedEffect(Unit) {
        while(isActive) {
            delay(16)
            val real=candles.lastOrNull() ?: continue;val dv=displayCandle
            displayCandle=when {
                dv==null->real
                dv.time!=real.time->{val close=if(candles.size>=2)candles[candles.size-2].close else real.open;Candle(close,max(close,real.close),min(close,real.close),real.close,real.time)}
                else->{val o=dv.open+(real.open-dv.open)*DV_ALPHA;val h=dv.high+(real.high-dv.high)*DV_ALPHA;val l=dv.low+(real.low-dv.low)*DV_ALPHA;val c=dv.close+(real.close-dv.close)*DV_ALPHA
                    val final=if(abs(real.close-c)<.0000001f)real.close else c
                    Candle(o,maxOf(h,final,o),minOf(l,final,o),final,real.time)
                }
            }
        }
    }
    LaunchedEffect(Unit) { while(isActive) { delay(100);nowMs=System.currentTimeMillis();clockText=clockNow();candles.lastOrNull()?.let { secondsLeft=remainingSeconds(it.time,nowMs) } } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier.background(C.ChartBg)) {
            AsyncImage(model=CHART_BG_URL,contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha=.25f)))
            if(isLoading) {
                ReferenceChartLoading(Modifier.matchParentSize(),sym+"-"+retryKey,!everLoaded)
            } else if(loadError) {
                Column(Modifier.align(Alignment.Center).background(Ref.Background,RoundedCornerShape(12.dp)).border(1.dp,C.Red,RoundedCornerShape(12.dp)).clickable { retryKey++ }.padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("لا توجد بيانات حقيقية",color=C.Red,fontSize=14.sp,fontWeight=FontWeight.Bold)
                    Text("تحقق من الاتصال، ثم اضغط لإعادة المحاولة",color=C.TextSecondary,fontSize=11.sp)
                }
            } else Canvas(Modifier.fillMaxSize().onSizeChanged { chartSize=it }
                .pointerInput(Unit) { detectTapGestures { pos->resultHits.asReversed().firstOrNull { it.second.contains(pos) }?.let { hit->TradeStore.results.removeAll { it.trade.id==hit.first } } } }
                .pointerInput(Unit) {
                    try { awaitEachGesture {
                        awaitFirstDown(requireUnconsumed=false);onBegin()
                        var pinchStarted=false;var dragging=false;var pan=Offset.Zero;var zoom=1f
                        val slop=viewConfiguration.touchSlop
                        try { do {
                            val event=awaitPointerEvent();val pressed=event.changes.count { it.pressed };val stable=event.changes.count { it.pressed&&it.previousPressed }
                            if(pressed>=2) {
                                if(!pinchStarted) { pinchStarted=true;dragging=false;zoom=1f;onPinchStart() }
                                if(pressed==stable) { val z=event.calculateZoom()
                                    if(z.isFinite()&&z>0) { if(!dragging) { zoom*=z;val r=event.calculateCentroidSize(useCurrent=false);if(abs(1-zoom)*r>slop) { dragging=true;onZoom(zoom.toDouble()) } } else onZoom(z.toDouble()) }
                                };event.changes.forEach { it.consume() }
                            } else if(pressed==1&&stable==1&&!pinchStarted) {
                                val p=event.calculatePan()
                                if(!dragging) { pan+=p;if(abs(pan.x)>slop) { dragging=true;onPan(pan.x-if(pan.x>0)slop else -slop) } } else onPan(p.x)
                                if(dragging)event.changes.forEach { it.consume() }
                            } else if(pinchStarted)event.changes.forEach { it.consume() }
                        } while(event.changes.any { it.pressed }) } finally { onEnd() }
                    } } finally { onEnd() }
                }) {
                val w=size.width;val h=size.height
                resultHits.clear()
                if(w<=1||h<=1||candles.isEmpty())return@Canvas
                val view=viewport;val dv=displayCandle;val lastIdx=candles.lastIndex
                fun xFor(bar:Double)=(view.fractionFor(bar)*w).toFloat()
                fun yFor(price:Double)=((view.maxPrice-price)/view.priceSpan*h).toFloat()
                fun baseline(y:Float,p:NativePaint)=y-(p.ascent()+p.descent())/2
                val native=drawContext.canvas.nativeCanvas
                val rows=(h/58.dp.toPx()).toInt().coerceIn(3,12);val step=nicePriceStep(view.priceSpan/rows);val decimals=priceDecimals(step)
                val pFirst=ceil(view.minPrice/step).toLong();val pLast=floor(view.maxPrice/step).toLong()
                if(pLast>=pFirst&&pLast-pFirst<1000)for(t in pFirst..pLast) {
                    val price=t*step;val y=yFor(price)
                    drawLine(C.Grid.copy(alpha=.34f),Offset(0f,y),Offset(w,y),1.dp.toPx())
                    if(y>=axisTextPx&&y<=h-axisTextPx/2)native.drawText(priceText(price,decimals),w-5.dp.toPx(),baseline(y,axisPaint),axisPaint)
                }
                val cols=(w/64.dp.toPx()).toInt().coerceAtLeast(2);val timeStep=niceTimeStep(view.visibleBars/cols)
                val om=origin/MINUTE_MS;val left=view.centerBar-view.visibleBars/2+om;val right=view.centerBar+view.visibleBars/2+om
                val tFirst=ceil(left/timeStep).toLong();val tLast=floor(right/timeStep).toLong()
                if(tLast>=tFirst&&tLast-tFirst<1000)for(t in tFirst..tLast) {
                    val minute=t*timeStep;val x=xFor((minute-om).toDouble())
                    drawLine(C.Grid.copy(alpha=.28f),Offset(x,0f),Offset(x,h),1.dp.toPx())
                    val label=candleTimeText(minute*MINUTE_MS);val half=timePaint.measureText(label)/2
                    if(x-half>=0&&x+half<=w)native.drawText(label,x,baseline(h-7.dp.toPx(),timePaint),timePaint)
                }
                val spacing=w/view.visibleBars.toFloat();val bw=(spacing*.85f).coerceIn(min(MINBW,spacing*.95f),spacing*.95f)
                val wick=min(1.2.dp.toPx(),bw)
                clipRect(0f,0f,w,h) {
                    // Filter by actual candle time, not list index, including gaps in history.
                    for(i in candles.indices) {
                        val real=candles[i];val candle=if(i==lastIdx&&dv!=null&&dv.time==real.time)dv else real
                        val x=xFor(barPosition(candle.time,origin))
                        if(x+bw/2<0||x-bw/2>w)continue
                        val col=if(candle.close>=candle.open)C.Green else C.Red
                        val yH=yFor(candle.high.toDouble());val yL=yFor(candle.low.toDouble());val yO=yFor(candle.open.toDouble());val yC=yFor(candle.close.toDouble())
                        drawLine(col,Offset(x,yH),Offset(x,yL),wick,StrokeCap.Butt)
                        if(abs(yO-yC)<1.dp.toPx())drawLine(col,Offset(x-bw/2,(yO+yC)/2),Offset(x+bw/2,(yO+yC)/2),1.dp.toPx())
                        else drawRect(col,Offset(x-bw/2,min(yO,yC)),Size(bw,abs(yO-yC)))
                    }
                    drawReferenceTrades(sym,AccountStore.selected,nowMs,{ time->xFor(barPosition(time,origin)) },::yFor,candles.last().time,resultHits)
                }
                val displayPrice=dv?.close ?: if(livePrice>0)livePrice else candles.last().close
                val y=yFor(displayPrice.toDouble())
                if(y in 0f..h&&displayPrice>0) {
                    val dash=7.dp.toPx();val gap=5.dp.toPx();var x=0f
                    while(x<w) { drawLine(C.Cyan.copy(alpha=.85f),Offset(x,y),Offset(min(x+dash,w),y),1.dp.toPx());x+=dash+gap }
                    val label=priceText(displayPrice.toDouble(),decimals);val tw=tagPaint.measureText(label)+14.dp.toPx();val th=20.dp.toPx()
                    drawRoundRect(C.PriceTag,Offset(w-tw-3.dp.toPx(),y-th/2),Size(tw,th),CornerRadius(4.dp.toPx()))
                    native.drawText(label,w-3.dp.toPx()-tw/2,baseline(y,tagPaint),tagPaint)
                    val candleX=xFor(barPosition(candles.last().time,origin))
                    val countdown=String.format(Locale.US,"%02d:%02d",secondsLeft/60,secondsLeft%60)
                    // Three full price-line dash intervals ahead of the active candle.
                    val labelHalf=max(detailPaint.measureText("M1"),detailPaint.measureText(countdown))/2
                    val labelX=min(candleX+3f*(dash+gap),w-labelHalf-5.dp.toPx())
                    if(candleX in 0f..w&&labelX-labelHalf>candleX+bw/2) {
                        native.drawText("M1",labelX,y-11.dp.toPx(),detailPaint)
                        native.drawText(countdown,labelX,y+21.dp.toPx(),detailPaint)
                    }
                }
            }
            PairHeader(Modifier.align(Alignment.TopStart).padding(start=4.dp,top=8.dp),onOpenPairPicker)
            Column(Modifier.align(Alignment.TopStart).padding(start=14.dp,top=60.dp)) {
                Text("$clockText UTC+3",color=C.TextSecondary,fontSize=11.sp)
            }
            TopNotificationOverlay(Modifier.align(Alignment.TopStart))
        }
    }
}

private data class MarkerPlacement(val trade:OpenTrade,val y:Float,val dot:Float,val start:Float,val left:Float)
private fun DrawScope.drawReferenceTrades(symbol:String,account:String,now:Long,xFor:(Long)->Float,yFor:(Double)->Float,lastTime:Long,hits:MutableList<Pair<Long,Rect>>) {
    // All dimensions below correspond to the source canvas CSS pixels, mapped to dp.
    fun px(n:Float)=n*density
    val w=size.width;val h=size.height;val native=drawContext.canvas.nativeCanvas
    val amountPaint=NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { typeface=Typeface.create("Arial",Typeface.BOLD);textSize=px(10f);textAlign=NativePaint.Align.LEFT }
    val taken=mutableListOf<Rect>();val places=mutableListOf<MarkerPlacement>()
    val cap=min(xFor(lastTime),w-px(55f))
    val trades=TradeStore.openTrades.filter { it.symbol==symbol&&it.account==account }.sortedWith(compareBy<OpenTrade> { it.candleTime }.thenBy { it.openedAt })
    for(tr in trades) {
        val y=yFor(tr.exactEntry);if(y<px(16f)||y>h-px(28f))continue
        val dot=min(xFor(tr.candleTime),cap);var end=dot-px(5f);var left=end-px(54f)
        var count=0
        while(count<80&&taken.any { it.overlaps(Rect(left,y-px(13f),end,y+px(13f))) }) { end-=px(56f);left-=px(56f);count++ }
        if(left<px(8f))continue
        taken.add(Rect(left,y-px(13f),end,y+px(13f)));places.add(MarkerPlacement(tr,y,dot,end,left))
    }
    for(p in places) {
        val up=p.trade.direction>0
        if(p.dot>p.start)drawLine((if(up)Color(0xFF4CAF50) else Color(0xFFF44336)).copy(alpha=.8f),Offset(p.start,p.y+px(.5f)),Offset(p.dot,p.y+px(.5f)),px(2f))
        drawCircle(Ref.Green,px(4f),Offset(p.dot,p.y));drawCircle(Color(0xFFEAFFF5),px(1.5f),Offset(p.dot,p.y))
    }
    for(p in places) {
        val tr=p.trade;val up=tr.direction>0;val l=p.left;val y=p.y
        val arrow=Path().apply {
            if(up) { moveTo(l+px(3f),y-px(14f));lineTo(l+px(6.6f),y-px(8f));lineTo(l-px(.6f),y-px(8f)) }
            else { moveTo(l+px(3f),y-px(6f));lineTo(l+px(6.6f),y-px(12f));lineTo(l-px(.6f),y-px(12f)) };close()
        }
        drawPath(arrow,Color.White.copy(alpha=.95f))
        val text="$"+shortAmount(tr.amount);val aw=amountPaint.measureText(text)
        val boxLeft=l+px(7f);val boxTop=y-px(17f)
        val boxSize=Size(aw+px(4f),px(13f));val radius=CornerRadius(px(3.5f))
        // A filled, rounded, softly raised marker rather than a bare square outline.
        for(layer in 3 downTo 1) {
            val spread=px(layer*.6f)
            drawRoundRect(Color.Black.copy(alpha=.045f),Offset(boxLeft-spread,boxTop+px(1.5f)-spread),Size(boxSize.width+2*spread,boxSize.height+2*spread),CornerRadius(radius.x+spread))
        }
        val topColor=if(up)Color(0xFF327C55) else Color(0xFF913C46)
        val bottomColor=if(up)Color(0xFF153E2B) else Color(0xFF49202A)
        drawRoundRect(Brush.verticalGradient(listOf(topColor,bottomColor),startY=boxTop,endY=boxTop+boxSize.height),Offset(boxLeft,boxTop),boxSize,radius)
        val outline=(if(up)Color(0xFF70C491) else Color(0xFFEA8991)).copy(alpha=.8f)
        drawRoundRect(outline,Offset(boxLeft+px(.4f),boxTop+px(.4f)),Size(boxSize.width-px(.8f),boxSize.height-px(.8f)),CornerRadius(px(3.1f)),style=Stroke(px(.8f)))
        drawLine(Color.White.copy(alpha=.20f),Offset(boxLeft+px(3f),boxTop+px(1.2f)),Offset(boxLeft+boxSize.width-px(3f),boxTop+px(1.2f)),px(.6f),StrokeCap.Round)
        amountPaint.color=(if(up)Color(0xFFFFE600) else C.Red).toArgb()
        native.drawText(text,l+px(9f),y-px(7f),amountPaint)
        val left=ceil((tr.expiresAt-now).coerceAtLeast(0)/1000.0).toLong()
        amountPaint.color=Color(0xFFC6D2E4).copy(alpha=.72f).toArgb()
        native.drawText(String.format(Locale.US,"%02d:%02d",left/60,left%60),l+px(9f),y+px(8f),amountPaint)
    }
    val label=android.text.TextPaint(NativePaint.ANTI_ALIAS_FLAG).apply { typeface=Typeface.create("Tahoma",Typeface.NORMAL);textSize=px(9f);color=Color.White.toArgb() }
    val labelText="النتيجة (ربح/خسارة)"
    val labelLayout=android.text.StaticLayout.Builder.obtain(labelText,0,labelText.length,label,px(100f).toInt()).setTextDirection(android.text.TextDirectionHeuristics.RTL).setAlignment(android.text.Layout.Alignment.ALIGN_OPPOSITE).setIncludePad(false).build()
    val profitPaint=NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { typeface=Typeface.create("Arial",Typeface.BOLD);textSize=px(15f);color=Color.White.toArgb();textAlign=NativePaint.Align.LEFT }
    for(result in TradeStore.results) {
        val tr=result.trade
        if(tr.symbol!=symbol||tr.account!=account||result.visibleUntil<=now)continue
        val y=yFor(tr.exactEntry);if(y<px(-30f)||y>h+px(30f))continue
        val ex=min(xFor(tr.candleTime),w-px(67f));val bw=px(126f);val bh=px(48f)
        val top=(y-px(24f)).coerceIn(px(10f),(h-px(72f)).coerceAtLeast(px(10f)))
        val left=if(ex-px(9f)-bw>=px(6f))ex-px(9f)-bw else min(ex+px(9f),w-bw-px(8f))
        val rightOfDot=left>ex;val win=result.profit>0
        val color=if(win)Color(0xFF1FA348) else Color(0xFFF65850).copy(alpha=.92f)
        drawRoundRect(color,Offset(left,top),Size(bw,bh),CornerRadius(px(9f)))
        val tail=Path().apply { if(rightOfDot) { moveTo(left,top+px(16f));lineTo(left-px(7f),top+px(24f));lineTo(left,top+px(32f)) } else { moveTo(left+bw,top+px(16f));lineTo(left+bw+px(7f),top+px(24f));lineTo(left+bw,top+px(32f)) };close() }
        drawPath(tail,color)
        val qx=left+bw-px(14f);val qy=top+px(14f)
        drawLine(Color.White.copy(alpha=.85f),Offset(qx-px(4f),qy-px(4f)),Offset(qx+px(4f),qy+px(4f)),px(1.6f))
        drawLine(Color.White.copy(alpha=.85f),Offset(qx+px(4f),qy-px(4f)),Offset(qx-px(4f),qy+px(4f)),px(1.6f))
        native.save();native.translate(left+px(11f),top+px(18f)-labelLayout.getLineBaseline(0));labelLayout.draw(native);native.restore()
        val text=(if(result.profit>0)"+" else "")+String.format(Locale.US,"%.2f $",result.profit)
        native.drawText(text,left+px(11f),top+px(38f),profitPaint)
        drawCircle(Color.White,px(5.5f),Offset(ex,y));drawCircle(if(win)Ref.Green else Color(0xFFF6465D),px(3.1f),Offset(ex,y))
        hits.add(tr.id to Rect(qx-px(12f),qy-px(12f),qx+px(12f),qy+px(12f)))
    }
}

@Composable fun TopNotificationOverlay(modifier:Modifier=Modifier) {
    Column(modifier.fillMaxWidth().padding(start=6.dp,end=6.dp,top=56.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        NotificationManager.notifications.forEach { notif->key(notif.id) { TradeNotificationCard(notif) } }
    }
}
@Composable fun TradeNotificationCard(notif:TradeNotification) {
    val y=remember(notif.id) { Animatable(-1.4f) };val opacity=remember(notif.id) { Animatable(0f) }
    LaunchedEffect(notif.id) { withFrameNanos { };withFrameNanos { };if(!notif.closing)notif.visible=true }
    LaunchedEffect(notif.visible) {
        launch { y.animateTo(if(notif.visible)0f else -1.4f,tween(380,easing=Ref.Entrance)) }
        launch { opacity.animateTo(if(notif.visible)1f else 0f,tween(300,easing=Ref.CssEase)) }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(Modifier.fillMaxWidth().height(40.dp).graphicsLayer { translationY=y.value*40.dp.toPx();alpha=opacity.value }.shadow(10.dp,RoundedCornerShape(12.dp)).background(Color(0xFF0EA751),RoundedCornerShape(12.dp)).padding(start=14.dp,end=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("${notif.price} :سعر فتح المداولة ${SymCatalog.pretty(notif.symbol)}",color=Color.White,fontSize=13.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis,textAlign=TextAlign.Left,modifier=Modifier.weight(1f))
            Box(Modifier.size(22.dp).clip(CircleShape).background(Color(0xFF2B313F)).clickable { NotificationManager.dismiss(notif.id) },contentAlignment=Alignment.Center) { Text("✕",color=Color.White,fontSize=11.sp,fontWeight=FontWeight.Bold) }
        }
    }
}

// The preset order, colors, grid, dimensions and anchoring come from #tp / .tpc.
private val TimeOptions=listOf("00:15","00:10","00:05","02:00","01:00","00:30","15:00","10:00","05:00","02:00:00","01:00:00","30:00")
@Composable fun TradePanel() {
    Column(Modifier.fillMaxWidth().background(C.BG)) {
        Spacer(Modifier.height(6.dp));SentimentBar();Spacer(Modifier.height(8.dp));TradeInputs()
        Spacer(Modifier.height(8.dp));TradeButtons();Spacer(Modifier.height(4.dp))
    }
}
@Composable fun SentimentBar() {
    var buy by remember { mutableIntStateOf(33) }
    LaunchedEffect(Unit) { while(isActive) { delay(2200);buy=Random.nextInt(30,37) } }
    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
        Text("$buy%",color=C.Green,fontSize=12.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.width(8.dp))
        BoxWithConstraints(Modifier.weight(1f).height(16.dp)) {
            val width=maxWidth
            Row(Modifier.fillMaxWidth().height(10.dp).align(Alignment.Center).clip(RoundedCornerShape(5.dp))) {
                Box(Modifier.weight(buy.toFloat()).fillMaxHeight().background(C.Green));Box(Modifier.weight((100-buy).toFloat()).fillMaxHeight().background(C.Red))
            }
            Box(Modifier.align(Alignment.CenterStart).offset(x=width*(buy/100f)-1.25.dp).width(2.5.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(C.Cyan))
        }
        Spacer(Modifier.width(8.dp));Text("${100-buy}%",color=C.Red,fontSize=12.sp,fontWeight=FontWeight.Bold)
    }
}
@Composable fun TradeInputs() {
    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        TimeInput(Modifier.weight(1f));AmountInput(Modifier.weight(1f))
    }
}
@Composable private fun TimeInput(modifier:Modifier=Modifier) {
    var expanded by remember { mutableStateOf(false) };var editing by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf(TextFieldValue(TradeSettings.duration)) }
    var receivedFocus by remember { mutableStateOf(false) }
    val focus=remember { FocusRequester() };val focusManager=LocalFocusManager.current
    val keyboard=LocalSoftwareKeyboardController.current
    val density=LocalDensity.current;val config=LocalConfiguration.current
    fun commit() {
        if(!editing)return
        val clean=manual.text.filter { it in '0'..'9'||it==':' }.trim()
        TradeSettings.duration=if(durationMillis(clean)!=null)clean else "00:00:05"
        editing=false;receivedFocus=false
    }
    LaunchedEffect(editing,expanded) {
        if(editing&&!expanded) {
            // Let the focusable time popup detach before focusing the text editor.
            withFrameNanos { };withFrameNanos { }
            focus.requestFocus()
            withFrameNanos { };keyboard?.show()
        }
    }
    Column(modifier) {
        Text("الزمن",color=C.TextSecondary,fontSize=12.sp);Spacer(Modifier.height(6.dp))
        Box {
            Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).background(C.Surface).border(1.dp,C.Divider,RoundedCornerShape(10.dp)).clickable(enabled=!editing) { expanded=!expanded }.padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically) {
                if(editing)BasicTextField(value=manual,onValueChange={manual=it.copy(text=it.text.filter { c->c in '0'..'9'||c==':' })},singleLine=true,textStyle=TextStyle(color=C.TextPrimary,fontSize=14.sp,fontWeight=FontWeight.Bold),modifier=Modifier.weight(1f).focusRequester(focus).onFocusChanged {
                    if(it.isFocused)receivedFocus=true else if(receivedFocus)commit()
                },cursorBrush=SolidColor(C.Cyan),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Text,imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={commit();focusManager.clearFocus()}))
                else Text(TradeSettings.duration,color=C.TextPrimary,fontSize=14.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                ClockIcon(Modifier.size(18.dp).clickable { if(editing){commit();focusManager.clearFocus()};expanded=!expanded })
            }
            if(expanded)Popup(popupPositionProvider=remember(density.density) { ReferencePopupPosition(true,density.density) },onDismissRequest={expanded=false},properties=PopupProperties(focusable=true)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(Modifier.width(min(248,config.screenWidthDp-16).coerceAtLeast(1).dp).shadow(24.dp,RoundedCornerShape(14.dp)).background(Ref.Background,RoundedCornerShape(14.dp)).border(1.dp,Ref.Border,RoundedCornerShape(14.dp)).padding(9.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                            TimeOptions.chunked(3).forEach { row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                                row.forEach { time->Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp)).background(if(time==TradeSettings.duration)Ref.TimeOn else Ref.TimeButton).clickable { TradeSettings.duration=time;expanded=false;editing=false },contentAlignment=Alignment.Center) {
                                    Text(time,color=Color.White,fontSize=14.sp,fontWeight=FontWeight.SemiBold,maxLines=1)
                                } }
                            } }
                        }
                        Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp)).background(Ref.TimeButton).clickable {
                            expanded=false;manual=TextFieldValue(TradeSettings.duration,TextRange(TradeSettings.duration.length));receivedFocus=false;editing=true
                        },contentAlignment=Alignment.Center) { Text("ضبط يدوياً",color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold) }
                    }
                }
            }
        }
    }
}
@Composable private fun AmountInput(modifier:Modifier=Modifier) {
    val focusManager=LocalFocusManager.current
    Column(modifier) {
        Text("المبلغ",color=C.TextSecondary,fontSize=12.sp);Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).background(C.Surface).border(1.dp,C.Divider,RoundedCornerShape(10.dp)).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically) {
            BasicTextField(TradeSettings.amount,{value->TradeSettings.amount=value.filter { it in '0'..'9'||it=='.' }},modifier=Modifier.weight(1f),singleLine=true,textStyle=TextStyle(color=C.TextPrimary,fontSize=14.sp,fontWeight=FontWeight.Bold),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal,imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={focusManager.clearFocus()}))
            DollarIcon(Modifier.size(18.dp))
        }
    }
}
@Composable fun TradeButtons() {
    val sym by TradeStore.currentSymbol.collectAsState();val payout=remember(sym) { SymCatalog.payout(sym) }
    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        TradeButton("شراء",C.Green,payout,Modifier.weight(1f)) { openTrade(1,sym,payout) }
        TradeButton("بيع",C.Red,payout,Modifier.weight(1f)) { openTrade(-1,sym,payout) }
    }
}
@Composable private fun TradeButton(label:String,color:Color,payout:Int,modifier:Modifier=Modifier,onClick:()->Unit) {
    Box(modifier.height(48.dp).clip(RoundedCornerShape(12.dp)).background(color).clickable(onClick=onClick),contentAlignment=Alignment.Center) {
        Row(verticalAlignment=Alignment.CenterVertically) { Text(label,color=Color.White,fontSize=15.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.width(8.dp));Text("$payout%",color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Bold) }
    }
}

@Composable fun PairPickerSheet(onDismiss:()->Unit) {
    val tabs=listOf("الجميع","فوركس","عملات رقمية","سلع","مؤشرات","أسهم")
    var selectedTab by remember { mutableIntStateOf(0) };var query by remember { mutableStateOf("") }
    val allSymbols=PairCatalogStore.allSymbols;val connected=PairCatalogStore.connected
    val loading=!PairCatalogStore.loaded;val favorites=remember { mutableStateListOf<String>() }
    val context=LocalContext.current
    LaunchedEffect(Unit) { PairCatalogStore.load(context)
        // Background monitoring: silent periodic refresh, never shows a loading indicator.
        while(isActive) { delay(10000);PairCatalogStore.refresh() } }
    val filtered=allSymbols.filter { s->
        val category=when(selectedTab) { 0->true;1->SymCatalog.isForex(s);2->SymCatalog.isCrypto(s);3->SymCatalog.isCommodity(s);4->SymCatalog.isIndex(s);5->SymCatalog.isStock(s);else->false }
        category&&(query.isBlank()||s.contains(query.uppercase(Locale.US)))
    }.sortedWith(compareBy({it !in connected},{it}))
    // Full-screen Popup keeps Back/outside dismissal working and prevents click-through.
    Popup(alignment=Alignment.TopStart,onDismissRequest=onDismiss,properties=PopupProperties(focusable=true)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.5f)).clickable(onClick=onDismiss)) {
            Column(Modifier.fillMaxWidth(.88f).fillMaxHeight().align(Alignment.CenterStart).clip(RoundedCornerShape(topEnd=18.dp,bottomEnd=18.dp)).background(C.PkBg).clickable(interactionSource=remember { androidx.compose.foundation.interaction.MutableInteractionSource() },indication=null,onClick={})) {
                Row(Modifier.fillMaxWidth().padding(start=14.dp,end=14.dp,top=12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("اختر الأصل",color=C.TextPrimary,fontSize=16.sp,fontWeight=FontWeight.ExtraBold,modifier=Modifier.weight(1f))
                    Text("✕",color=C.TextPrimary,fontSize=20.sp,modifier=Modifier.clickable(onClick=onDismiss).padding(6.dp))
                }
                Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=8.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    tabs.forEachIndexed { i,t->Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if(i==selectedTab)C.PkTabOn else C.PkTab).clickable { selectedTab=i }.padding(horizontal=12.dp,vertical=8.dp)) { Text(t,color=if(i==selectedTab)C.BG else C.TextSecondary,fontSize=11.sp,fontWeight=FontWeight.Bold,maxLines=1) } }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=4.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                    Row(Modifier.height(40.dp).widthIn(min=56.dp).clip(RoundedCornerShape(10.dp)).background(C.PkSurface).border(1.dp,C.Divider,RoundedCornerShape(10.dp)).padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically) { Text("★",color=C.PkStarOn,fontSize=15.sp);Spacer(Modifier.width(6.dp));Text("${favorites.size}",color=C.TextPrimary,fontSize=13.sp,fontWeight=FontWeight.Bold) }
                    Row(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp)).background(C.PkSurface).border(1.dp,C.Divider,RoundedCornerShape(10.dp)).padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically) {
                        BasicTextField(query,{query=it},modifier=Modifier.weight(1f),singleLine=true,textStyle=TextStyle(color=C.TextPrimary,fontSize=13.sp,fontWeight=FontWeight.SemiBold),decorationBox={inner->Box { if(query.isEmpty())Text("بحث...",color=C.TextSecondary,fontSize=13.sp);inner() } })
                        Text("🔍",color=C.TextPrimary,fontSize=15.sp)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),horizontalArrangement=Arrangement.SpaceBetween) { Text(tabs[selectedTab],color=C.TextPrimary,fontSize=12.sp,fontWeight=FontWeight.Bold);Text("${filtered.size} أصل",color=C.PkPct,fontSize=12.sp,fontWeight=FontWeight.Bold) }
                if(loading)Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { ReferenceSpinner() }
                else LazyColumn(Modifier.fillMaxSize()) {
                    items(filtered,key={it}) { s->
                        val icons=IconProvider.getPairIcons(s);val active=s in connected
                        Row(Modifier.fillMaxWidth().clickable(enabled=active) { TradeStore.currentSymbol.value=s;onDismiss() }.padding(horizontal=14.dp,vertical=10.dp).alpha(if(active)1f else .4f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            val fav=s in favorites
                            Text(if(fav)"★" else "☆",color=C.PkStarOn,fontSize=22.sp,modifier=Modifier.clickable(enabled=active) { if(fav)favorites.remove(s) else favorites.add(s) })
                            Box(Modifier.width(46.dp).height(30.dp)) {
                                if(icons.isNotEmpty())CurrencyIcon(icons[0],s,Modifier.size(30.dp).align(Alignment.CenterStart))
                                if(icons.size>1)CurrencyIcon(icons[1],s.drop(3),Modifier.size(30.dp).align(Alignment.CenterEnd).border(1.5.dp,C.PkBg,CircleShape))
                            }
                            Column(Modifier.weight(1f)) { Text(SymCatalog.pretty(s),color=C.TextPrimary,fontSize=15.sp,fontWeight=FontWeight.Bold);if(!active)Text("غير متصل",color=C.Red,fontSize=10.sp,fontWeight=FontWeight.Bold) }
                            Text("+${SymCatalog.payout(s)}%",color=C.PkPct,fontSize=13.sp,fontWeight=FontWeight.ExtraBold)
                        }
                        Box(Modifier.fillMaxWidth().padding(start=70.dp).height(.5.dp).background(C.Divider.copy(alpha=.4f)))
                    }
                }
            }
        }
    }
}

@Composable private fun ReferenceSpinner(modifier:Modifier=Modifier) {
    val transition=rememberInfiniteTransition(label="reference-spinner")
    val rotation by transition.animateFloat(0f,360f,infiniteRepeatable(tween(700,easing=LinearEasing)),label="rotation")
    Canvas(modifier.size(24.dp).graphicsLayer { rotationZ=rotation }) {
        val stroke=2.dp.toPx();val inset=stroke/2
        drawCircle(Color(0xFF395782).copy(alpha=.45f),radius=(size.minDimension-stroke)/2,style=Stroke(stroke))
        drawArc(Color(0xFF83B8FF),-135f,90f,false,Offset(inset,inset),Size(size.width-stroke,size.height-stroke),style=Stroke(stroke))
    }
}
@Composable private fun ReferenceChartLoading(modifier:Modifier=Modifier,token:String,initial:Boolean) {
    val transition=rememberInfiniteTransition(label="reference-shimmer")
    val shimmer by transition.animateFloat(2f,-2f,infiniteRepeatable(tween(1200,easing=LinearEasing)),label="shimmer")
    Box(modifier.background(C.ChartBg)) {
        Canvas(Modifier.matchParentSize()) {
            val bands=listOf(floatArrayOf(.05f,.12f,.20f,8f),floatArrayOf(.05f,.22f,.35f,8f),floatArrayOf(.10f,.35f,.15f,40f),floatArrayOf(.28f,.35f,.15f,60f),floatArrayOf(.46f,.35f,.15f,35f),floatArrayOf(.64f,.35f,.15f,55f),floatArrayOf(.05f,.70f,.90f,1f),floatArrayOf(.05f,.85f,.15f,8f),floatArrayOf(.25f,.85f,.15f,8f),floatArrayOf(.45f,.85f,.15f,8f),floatArrayOf(.65f,.85f,.15f,8f))
            bands.forEach { b->drawRoundRect(Color(0xFF3C5A8C).copy(alpha=.18f),Offset(size.width*b[0],size.height*b[1]),Size(size.width*b[2],b[3]*density),CornerRadius(3*density)) }
            val start=shimmer*size.width
            drawRect(Brush.linearGradient(listOf(Color.Transparent,Color(0xFF83B8FF).copy(alpha=.08f),Color(0xFF83B8FF).copy(alpha=.15f),Color(0xFF83B8FF).copy(alpha=.08f),Color.Transparent),Offset(start,0f),Offset(start+size.width*2,size.height)))
        }
        if(initial)ReferenceSpinner(Modifier.align(Alignment.Center))
        key(token) { ReferenceAnimatedLogo(Modifier.align(Alignment.Center).size(if(initial)175.dp else 120.dp)) }
    }
}
@Composable private fun ReferenceAnimatedLogo(modifier:Modifier=Modifier) {
    // Only the original inline SVG is used, not the HTML application's scripts.
    // SMIL gradients, glow and animation timings are retained verbatim.
    AndroidView(modifier=modifier,factory={context->WebView(context).apply {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        layoutParams=ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)
        settings.javaScriptEnabled=false;settings.blockNetworkLoads=true;settings.allowFileAccess=false;settings.allowContentAccess=false
        isVerticalScrollBarEnabled=false;isHorizontalScrollBarEnabled=false;importantForAccessibility=android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
        setOnTouchListener { _,_->true }
        loadDataWithBaseURL(null,"<html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>html,body{margin:0;width:100%;height:100%;overflow:hidden;background:transparent}body{display:grid;place-items:center}svg{display:block;width:100%;height:100%}</style></head><body>"+REFERENCE_LOADING_SVG+"</body></html>","text/html","UTF-8",null)
    } },onReset=null,onRelease={it.stopLoading();it.destroy()})
}

@Composable fun BottomNavBar() {
    var selected by remember { mutableIntStateOf(2) }
    Column(Modifier.background(C.BG)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Divider))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth().background(C.BG).padding(vertical=6.dp),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
                BottomItem("الشارت",selected==0,{selected=0}) { ChartIcon(Modifier.size(24.dp),it) }
                BottomItem("الدردشة",selected==1,{selected=1}) { ChatIcon(Modifier.size(24.dp),it) }
                BottomItem("العمليات",selected==2,{selected=2}) { HistoryIcon(Modifier.size(24.dp),it) }
                BottomItem("أول الاجتماعي",selected==3,{selected=3}) { SocialIcon(Modifier.size(24.dp),it) }
                BottomItem("المزيد",selected==4,{selected=4}) { DotsIcon(Modifier.size(24.dp),it) }
            }
        }
    }
}
@Composable fun BottomItem(label:String,selected:Boolean,onClick:()->Unit,icon:@Composable (Color)->Unit) {
    val color=if(selected)C.Cyan else C.TextSecondary
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick=onClick).padding(horizontal=6.dp,vertical=4.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(if(selected)C.SurfaceRaised else Color.Transparent).border(1.dp,if(selected)C.Cyan else Color.Transparent,RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center) { icon(color) }
        Spacer(Modifier.height(3.dp));Text(label,color=color,fontSize=10.sp,textAlign=TextAlign.Center,maxLines=1)
    }
}

@Composable private fun ChartIcon(modifier:Modifier=Modifier,color:Color=C.TextPrimary) { Canvas(modifier) {
    val w=size.width;val h=size.height;val stroke=w*.075f
    drawLine(color,Offset(w*.12f,h*.12f),Offset(w*.12f,h*.88f),stroke,StrokeCap.Round)
    drawLine(color,Offset(w*.12f,h*.88f),Offset(w*.90f,h*.88f),stroke,StrokeCap.Round)
    for((x,range) in listOf(.34f to (.40f to .68f),.57f to (.28f to .57f),.80f to (.18f to .43f))) {
        val top=h*range.first;val bottom=h*range.second
        drawLine(color,Offset(w*x,top-h*.10f),Offset(w*x,bottom+h*.10f),stroke*.65f,StrokeCap.Round)
        drawRoundRect(color,Offset(w*(x-.055f),top),Size(w*.11f,bottom-top),CornerRadius(w*.02f))
    }
} }
@Composable private fun ChatIcon(modifier:Modifier=Modifier,color:Color=C.TextPrimary) { Canvas(modifier) {
    val w=size.width;val h=size.height;val stroke=w*.075f
    val bubble=Path().apply {
        moveTo(w*.23f,h*.16f);lineTo(w*.77f,h*.16f)
        quadraticBezierTo(w*.90f,h*.16f,w*.90f,h*.29f);lineTo(w*.90f,h*.63f)
        quadraticBezierTo(w*.90f,h*.76f,w*.77f,h*.76f);lineTo(w*.43f,h*.76f)
        lineTo(w*.20f,h*.92f);lineTo(w*.20f,h*.75f)
        quadraticBezierTo(w*.10f,h*.73f,w*.10f,h*.63f);lineTo(w*.10f,h*.29f)
        quadraticBezierTo(w*.10f,h*.16f,w*.23f,h*.16f);close()
    }
    drawPath(bubble,color,style=Stroke(stroke,cap=StrokeCap.Round,join=StrokeJoin.Round))
    for(x in listOf(.32f,.50f,.68f))drawCircle(color,w*.04f,Offset(w*x,h*.46f))
} }
@Composable private fun HistoryIcon(modifier:Modifier=Modifier,color:Color=C.TextPrimary) { Canvas(modifier) {
    val w=size.width;val h=size.height;val stroke=w*.075f
    drawRoundRect(color,Offset(w*.18f,h*.10f),Size(w*.64f,h*.80f),CornerRadius(w*.08f),style=Stroke(stroke))
    for(y in listOf(.32f,.51f,.70f)) {
        drawCircle(color,w*.025f,Offset(w*.31f,h*y))
        drawLine(color,Offset(w*.43f,h*y),Offset(w*.69f,h*y),stroke*.8f,StrokeCap.Round)
    }
} }

@Composable fun DownArrow(modifier:Modifier=Modifier,color:Color=C.TextSecondary) { Canvas(modifier) { drawPath(Path().apply { moveTo(0f,0f);lineTo(size.width,0f);lineTo(size.width/2,size.height);close() },color) } }
@Composable fun DotsIcon(modifier:Modifier=Modifier,color:Color=C.TextPrimary) { Canvas(modifier) { val r=size.minDimension*.11f;val y=size.height/2;drawCircle(color,r,Offset(size.width*.15f,y));drawCircle(color,r,Offset(size.width*.5f,y));drawCircle(color,r,Offset(size.width*.85f,y)) } }
@Composable fun WalletIcon(modifier:Modifier=Modifier,color:Color=Color.White) { Canvas(modifier) { val w=size.width;val h=size.height
    drawRoundRect(color,Offset(w*.05f,h*.22f),Size(w*.9f,h*.62f),CornerRadius(w*.1f),style=Stroke(w*.08f))
    drawRoundRect(color,Offset(w*.55f,h*.4f),Size(w*.4f,h*.28f),CornerRadius(w*.06f),style=Stroke(w*.07f));drawCircle(color,w*.05f,Offset(w*.73f,h*.54f))
} }
@Composable fun ClockIcon(modifier:Modifier=Modifier,color:Color=C.Cyan) { Canvas(modifier) { val d=size.minDimension;val cx=size.width/2;val cy=size.height/2;val s=d*.09f
    drawCircle(color,d*.4f,Offset(cx,cy),style=Stroke(s));drawLine(color,Offset(cx,cy),Offset(cx,size.height*.25f),s,StrokeCap.Round);drawLine(color,Offset(cx,cy),Offset(size.width*.72f,cy),s,StrokeCap.Round)
} }
@Composable fun DollarIcon(modifier:Modifier=Modifier,color:Color=C.Cyan) { Canvas(modifier) { val d=size.minDimension;val s=d*.09f;val cx=size.width/2;val cy=size.height/2;val m=d*.16f
    drawCircle(color,d*.4f,Offset(cx,cy),style=Stroke(s));drawLine(color,Offset(cx+m*.6f,cy-m),Offset(cx-m*.5f,cy-m*.2f),s,StrokeCap.Round);drawLine(color,Offset(cx-m*.5f,cy-m*.2f),Offset(cx+m*.5f,cy+m*.2f),s,StrokeCap.Round);drawLine(color,Offset(cx+m*.5f,cy+m*.2f),Offset(cx-m*.6f,cy+m),s,StrokeCap.Round)
} }
@Composable fun TradesIcon(modifier:Modifier=Modifier,color:Color=Color.White) { Canvas(modifier) { val w=size.width;val h=size.height;val s=w*.09f
    drawLine(color,Offset(w*.3f,h*.85f),Offset(w*.3f,h*.15f),s,StrokeCap.Round);drawLine(color,Offset(w*.3f,h*.15f),Offset(w*.15f,h*.32f),s,StrokeCap.Round);drawLine(color,Offset(w*.3f,h*.15f),Offset(w*.45f,h*.32f),s,StrokeCap.Round)
    drawLine(color,Offset(w*.7f,h*.15f),Offset(w*.7f,h*.85f),s,StrokeCap.Round);drawLine(color,Offset(w*.7f,h*.85f),Offset(w*.55f,h*.68f),s,StrokeCap.Round);drawLine(color,Offset(w*.7f,h*.85f),Offset(w*.85f,h*.68f),s,StrokeCap.Round)
} }
@Composable fun SignalIcon(modifier:Modifier=Modifier,color:Color=Color.White) { Canvas(modifier) { val cx=size.width/2;val cy=size.height*.75f;val s=size.minDimension*.09f
    drawArc(color,180f,180f,false,Offset(cx-size.width*.42f,cy-size.height*.42f),Size(size.width*.84f,size.height*.84f),style=Stroke(s));drawArc(color,180f,180f,false,Offset(cx-size.width*.26f,cy-size.height*.26f),Size(size.width*.52f,size.height*.52f),style=Stroke(s));drawCircle(color,size.minDimension*.1f,Offset(cx,cy-size.height*.02f))
} }
@Composable fun ProIcon(modifier:Modifier=Modifier,color:Color=Color.White) { Canvas(modifier) { val w=size.width;val h=size.height
    drawRoundRect(color,Offset(w*.08f,h*.12f),Size(w*.84f,h*.76f),CornerRadius(w*.16f),style=Stroke(w*.08f))
    drawPath(Path().apply { moveTo(w*.24f,h*.62f);lineTo(w*.40f,h*.44f);lineTo(w*.54f,h*.56f);lineTo(w*.76f,h*.34f) },color,style=Stroke(w*.08f,cap=StrokeCap.Round))
} }
@Composable fun SocialIcon(modifier:Modifier=Modifier,color:Color=Color.White) { Canvas(modifier) { val cx=size.width/2;val cy=size.height/2
    drawCircle(color,size.minDimension*.17f,Offset(cx,cy-size.height*.16f));drawArc(color,180f,180f,false,Offset(cx-size.width*.28f,cy+size.height*.04f),Size(size.width*.56f,size.height*.5f),style=Stroke(size.minDimension*.08f))
    drawCircle(color,size.minDimension*.09f,Offset(cx-size.width*.36f,cy-size.height*.14f));drawCircle(color,size.minDimension*.09f,Offset(cx+size.width*.36f,cy-size.height*.14f))
} }
@Composable private fun ReferenceGlyph(kind:String,modifier:Modifier=Modifier,color:Color=Color.White) {
    val paths=remember(kind) {
        val data=when(kind) {
            "wallet"->listOf("M6 6H18Q21 6 21 9V17Q21 20 18 20H6Q3 20 3 17V9Q3 6 6 6Z","M3 10h18M16 15h2")
            "download"->listOf("M12 3v12m0 0 4-4m-4 4-4-4","M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2")
            "demo"->listOf("m12 4 10 5-10 5L2 9Z","M6 11.5V16c0 1.5 2.7 3 6 3s6-1.5 6-3v-4.5","M22 9v5")
            "coins"->listOf("M5 6v6c0 1.7 3.1 3 7 3s7-1.3 7-3V6","M5 12v6c0 1.7 3.1 3 7 3s7-1.3 7-3v-6")
            else->listOf("M15 9.5c-.5-1-1.6-1.5-3-1.5-1.7 0-3 .8-3 2.2 0 2.9 6 1.5 6 4.3 0 1.4-1.3 2.2-3 2.2-1.4 0-2.5-.5-3-1.5M12 6v12")
        }
        data.map { androidx.compose.ui.graphics.vector.PathParser().parsePathString(it).toPath() }
    }
    Canvas(modifier) {
        scale(size.width/24,size.height/24,pivot=Offset.Zero) {
            val stroke=if(kind=="wallet")1.8f else 1.7f
            paths.forEach { drawPath(it,color,style=Stroke(stroke)) }
            if(kind=="coins")drawOval(color,Offset(5f,3f),Size(14f,6f),style=Stroke(stroke))
            if(kind=="dollar")drawCircle(color,9f,Offset(12f,12f),style=Stroke(stroke))
        }
    }
}

private const val REFERENCE_LOADING_SVG = """<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512"><defs><linearGradient id="white" x2="1" y2="1"><stop stop-color="#fff"/><stop offset="1" stop-color="#bdcbea"/></linearGradient><linearGradient id="green" x2="0" y2="1"><stop stop-color="#20ffc2"/><stop offset="1" stop-color="#00b98a"/></linearGradient><linearGradient id="red" x2="0" y2="1"><stop stop-color="#ff426b"/><stop offset="1" stop-color="#e90040"/></linearGradient><filter id="glow" x="-40%" y="-40%" width="180%" height="180%"><feGaussianBlur stdDeviation="3" result="blur"/><feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge></filter></defs><path d="M256 82 L389 159 Q400 166 400 181 V326 Q400 341 387 349 L269 418 Q256 426 243 418 L125 349 Q112 341 112 326 V181 Q112 166 125 159 Z" fill="none" stroke="url(#white)" stroke-width="17" stroke-linejoin="round" stroke-linecap="round" stroke-dasharray="1000" stroke-dashoffset="1000" pathLength="1000" filter="url(#glow)"><animate attributeName="stroke-dashoffset" from="1000" to="0" dur="1s" fill="freeze"/></path><g filter="url(#glow)"><g><animateTransform attributeName="transform" type="translate" values="0,-100;0,8;0,-3;0,0" keyTimes="0;0.72;0.9;1" dur=".7s" begin=".65s" fill="freeze"/><g fill="url(#green)"><rect x="153" y="221" width="8" height="120" rx="4"/><rect x="141" y="241" width="32" height="94" rx="5"/></g></g><g><animateTransform attributeName="transform" type="translate" values="0,-130;0,10;0,-3;0,0" keyTimes="0;0.72;0.9;1" dur=".72s" begin=".82s" fill="freeze"/><g fill="url(#red)"><rect x="197" y="179" width="8" height="159" rx="4"/><rect x="185" y="207" width="32" height="117" rx="5"/></g></g><g><animateTransform attributeName="transform" type="translate" values="0,-115;0,9;0,-3;0,0" keyTimes="0;0.72;0.9;1" dur=".7s" begin=".99s" fill="freeze"/><g fill="url(#green)"><rect x="351" y="221" width="8" height="120" rx="4"/><rect x="339" y="241" width="32" height="94" rx="5"/></g></g><g><animateTransform attributeName="transform" type="translate" values="0,-145;0,10;0,-3;0,0" keyTimes="0;0.72;0.9;1" dur=".74s" begin="1.16s" fill="freeze"/><g fill="url(#red)"><rect x="307" y="179" width="8" height="159" rx="4"/><rect x="295" y="207" width="32" height="117" rx="5"/></g></g></g><path d="M256 139 L276 158 V351 L256 369 L236 351 V158 Z" fill="url(#white)" opacity="0" filter="url(#glow)"><animate attributeName="opacity" values="0;1" dur=".45s" begin="1.45s" fill="freeze"/><animateTransform attributeName="transform" type="translate" values="0,-45;0,5;0,-2;0,0" keyTimes="0;0.72;0.9;1" dur=".6s" begin="1.45s" fill="freeze"/></path></svg>"""
