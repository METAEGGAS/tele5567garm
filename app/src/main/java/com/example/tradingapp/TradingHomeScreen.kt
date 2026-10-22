@file:Suppress("FunctionName", "unused")
package com.example.tradingapp

import android.content.Context
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.decode.SvgDecoder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
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
    val Cyan=Color(0xFF58D9F5); 
    val Green=Color(0xFF00FF00); // أخضر ساطع جداً
    val Red=Color(0xFFFF0000);   // أحمر ساطع جداً
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
private const val LIVE_CANDLE_FRACTION=.50 // الشمعة النشطة في منتصف الشارت عند متابعة السعر
private fun candleBodyWidth(spacing:Float,gapPx:Float):Float {
    if(!spacing.isFinite()||spacing<=0f)return 0f
    val unit=gapPx.coerceAtLeast(.001f)
    return (floor(spacing/unit+.5f)-1f).coerceAtLeast(2f)*unit
}
private const val PAD=6f
private const val PINCH_AMPLIFY=4.0
private const val ZOOM_SMOOTH=.22f
private const val PAN_GAIN=1.65
private const val INERTIA_FRICTION=3.2
private const val MIN_VISIBLE_BARS_LIMIT=10.0
private const val MAX_VISIBLE_BARS_LIMIT=120.0
private val ids=AtomicLong(System.currentTimeMillis())
private fun nextId()=ids.incrementAndGet()
private fun barPosition(time:Long,origin:Long,intervalMs:Long=MINUTE_MS)=(time-origin).toDouble()/intervalMs

data class Candle(
    val open:Double,val high:Double,val low:Double,val close:Double,
    val time:Long=0L,val isClosed:Boolean=false
) {
    constructor(open:Float,high:Float,low:Float,close:Float,time:Long=0L):
        this(open.toDouble(),high.toDouble(),low.toDouble(),close.toDouble(),time,false)
}
private enum class CandleDirection { UP, DOWN, DOJI }
private fun candleDirection(c:Candle)=when {
    c.close>c.open->CandleDirection.UP
    c.close<c.open->CandleDirection.DOWN
    else->CandleDirection.DOJI
}
private object ForexCandleColors {
    val Up=Color(0xFF00FF00) // أخضر نيون
    val Down=Color(0xFFFF0000) // أحمر نيون
    val Doji=Up 
}
private fun candleColor(c:Candle)=when(candleDirection(c)) {
    CandleDirection.UP->ForexCandleColors.Up
    CandleDirection.DOWN->ForexCandleColors.Down
    CandleDirection.DOJI->ForexCandleColors.Doji
}
private fun validCandle(c:Candle)=
    c.time>0&&c.open.isFinite()&&c.high.isFinite()&&c.low.isFinite()&&c.close.isFinite()&&
        c.low>0.0&&c.high>=maxOf(c.open,c.close)&&c.low<=minOf(c.open,c.close)

private fun chartPriceY(price:Double,maxPrice:Double,span:Double,height:Float)=
    ((maxPrice-price)/span*height.toDouble()).toFloat()
private fun forexPriceDecimals(symbol:String)=if(symbol.endsWith("JPY"))3 else 5
fun clockNow():String=SimpleDateFormat("HH:mm:ss",Locale.US).apply { timeZone=TimeZone.getTimeZone("GMT+3") }.format(Date(FeedClock.now()))
fun fmt(price:Float)=String.format(Locale.US,"%.5f",price)
private fun priceText(price:Double,decimals:Int)=String.format(Locale.US,"%.${decimals}f",price)
private fun candleTimeText(time:Long,intervalMs:Long=MINUTE_MS)=SimpleDateFormat(if(intervalMs<MINUTE_MS)"HH:mm:ss" else if(intervalMs>=86_400_000L)"dd/MM" else if(intervalMs>=3_600_000L)"dd/MM HH:mm" else "HH:mm",Locale.US).apply { timeZone=TimeZone.getTimeZone("GMT+3") }.format(Date(time))
private fun remainingSeconds(openTime:Long,now:Long,intervalMs:Long=MINUTE_MS)=ceil((openTime+intervalMs-now)/1000.0).toInt().coerceIn(0,(intervalMs/1000).toInt())
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

object PairCatalogStore {
    private val FOREX_PAIRS = listOf(
        "EURUSD", "GBPUSD", "USDJPY", "AUDUSD", "USDCAD", "USDCHF", "NZDUSD",
        "EURGBP", "EURJPY", "GBPJPY", "AUDJPY", "CADJPY", "CHFJPY", "NZDJPY",
        "EURCHF", "EURAUD", "EURCAD", "EURNZD", "GBPCHF", "GBPAUD", "GBPCAD",
        "GBPNZD", "AUDCHF", "AUDCAD", "AUDNZD", "CADCHF", "NZDCAD", "NZDCHF"
    )
    var allSymbols by mutableStateOf<List<String>>(FOREX_PAIRS)
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
            val savedConn=p.getString("connected","")?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
            if(savedConn.isNotEmpty()) { connected=savedConn.toSet();loaded=true }
        }
        refresh()
    }
    fun refresh() {
        if(refreshing)return;refreshing=true
        scope.launch {
            val live=BiquoteApi.symbols(true).toSet()
            if(live.isNotEmpty()) {
                connected=live.intersect(FOREX_PAIRS.toSet())
                loaded=true
                app?.getSharedPreferences("pair_catalog_v1",Context.MODE_PRIVATE)?.edit()?.putString("connected",connected.joinToString(","))?.apply()
            }
            refreshing=false
        }
    }
}

private object TradingImageStore {
    private val locks=ConcurrentHashMap<String,Mutex>()
    @Volatile private var loader:ImageLoader?=null
    fun loader(context:Context):ImageLoader=loader ?: synchronized(this) {
        loader ?: ImageLoader.Builder(context.applicationContext)
            .components { add(SvgDecoder.Factory()) }.build().also { loader=it }
    }
    private fun file(context:Context,url:String):File {
        val key=MessageDigest.getInstance("SHA-256").digest(url.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return File(File(context.applicationContext.filesDir,"trading_images_v1"),key)
    }
    fun cached(context:Context,url:String):File?=file(context,url).takeIf { it.isFile&&it.length()>0L }
    fun invalidate(context:Context,url:String) { file(context,url).delete() }
    suspend fun download(context:Context,url:String):File?=withContext(Dispatchers.IO) {
        locks.getOrPut(url) { Mutex() }.withLock {
            cached(context,url)?.let { return@withLock it }
            val target=file(context,url)
            var connection:HttpURLConnection?=null
            var temp:File?=null
            try {
                val directory=target.parentFile!!
                if(!directory.isDirectory&&!directory.mkdirs()&&!directory.isDirectory)return@withLock null
                val active=(URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout=6000;readTimeout=8000;instanceFollowRedirects=true
                }
                connection=active
                if(active.responseCode !in 200..299)return@withLock null
                val pending=File.createTempFile("image-",".part",target.parentFile)
                temp=pending
                active.inputStream.use { input->pending.outputStream().buffered().use { output->input.copyTo(output) } }
                if(pending.length()==0L||!pending.renameTo(target))return@withLock null
                target
            } catch(cancelled:CancellationException) { throw cancelled }
            catch(_:Exception) { null }
            finally { temp?.delete();connection?.disconnect() }
        }
    }
}
@Composable private fun CachedTradingImage(
    url:String,contentDescription:String?,modifier:Modifier=Modifier,
    contentScale:ContentScale=ContentScale.Fit,onError:(()->Unit)?=null
) {
    val context=LocalContext.current.applicationContext
    key(url) {
        val initial=remember(context,url) { TradingImageStore.cached(context,url) }
        val model by produceState<Any?>(initialValue=initial,context,url) {
            value=initial ?: TradingImageStore.download(context,url) ?: url
        }
        AsyncImage(model=model,imageLoader=TradingImageStore.loader(context),
            contentDescription=contentDescription,contentScale=contentScale,modifier=modifier,
            onError={ TradingImageStore.invalidate(context,url);onError?.invoke() })
    }
}

object IconProvider {
    private val FLAGS=mapOf("EUR" to "eu","USD" to "us","GBP" to "gb","JPY" to "jp","CHF" to "ch","AUD" to "au","CAD" to "ca","NZD" to "nz","TRY" to "tr","ZAR" to "za","MXN" to "mx","SEK" to "se","NOK" to "no","SGD" to "sg","HKD" to "hk","PLN" to "pl","DKK" to "dk","HUF" to "hu","CZK" to "cz","ILS" to "il","CNY" to "cn","QAR" to "qa","SAR" to "sa","UAH" to "ua","IDR" to "id","AED" to "ae","THB" to "th","MYR" to "my","PHP" to "ph","INR" to "in","KRW" to "kr","BRL" to "br","ARS" to "ar","CLP" to "cl","COP" to "co","PEN" to "pe","RUB" to "ru")
    private fun flags(code:String)=listOf("https://flagcdn.com/$code.svg","https://flagcdn.com/w160/$code.png","https://flagcdn.com/w80/$code.png")
    private fun randomIcon(seed:String):String {
        var h=0;for(c in seed)h=h*31+c.code
        val style=listOf("identicon","shapes","bottts-neutral","rings")[(h and Int.MAX_VALUE)%4]
        return "https://api.dicebear.com/9.x/$style/png?seed=${URLEncoder.encode(seed,"UTF-8")}"
    }
    fun getPairIcons(symbol:String):List<List<String>> {
        val s=symbol.uppercase(Locale.US).replace(Regex("[_/\\-]"),"")
        if(s.length==6) { val a=FLAGS[s.take(3)]; val b=FLAGS[s.drop(3)]; if(a!=null&&b!=null) return listOf(flags(a)+randomIcon(s.take(3)),flags(b)+randomIcon(s.drop(3))) }
        return emptyList()
    }
}
@Composable fun CurrencyIcon(urls:List<String>,label:String,modifier:Modifier=Modifier) {
    val context=LocalContext.current.applicationContext
    val candidates=remember(context,urls) { urls.sortedBy { TradingImageStore.cached(context,it)==null } }
    var failed by remember(candidates) { mutableIntStateOf(0) }
    if(failed<candidates.size) CachedTradingImage(url=candidates[failed],contentDescription=label,contentScale=ContentScale.Crop,modifier=modifier.clip(CircleShape).background(C.Surface),onError={failed++})
    else Box(modifier.clip(CircleShape).background(C.SurfaceRaised).border(1.dp,C.Cyan.copy(alpha=.55f),CircleShape),contentAlignment=Alignment.Center) { Text(label.take(2),color=C.Cyan,fontSize=9.sp,fontWeight=FontWeight.ExtraBold) }
}

object PairNames {
    private val names = mapOf(
        "EURUSD" to "EUR / USD", "GBPUSD" to "GBP / USD", "USDJPY" to "USD / JPY",
        "AUDUSD" to "AUD / USD", "USDCAD" to "USD / CAD", "USDCHF" to "USD / CHF",
        "NZDUSD" to "NZD / USD", "EURGBP" to "EUR / GBP", "EURJPY" to "EUR / JPY",
        "GBPJPY" to "GBP / JPY", "AUDJPY" to "AUD / JPY", "CADJPY" to "CAD / JPY",
        "CHFJPY" to "CHF / JPY", "NZDJPY" to "NZD / JPY", "EURCHF" to "EUR / CHF",
        "EURAUD" to "EUR / AUD", "EURCAD" to "EUR / CAD", "EURNZD" to "EUR / NZD",
        "GBPCHF" to "GBP / CHF", "GBPAUD" to "GBP / AUD", "GBPCAD" to "GBP / CAD",
        "GBPNZD" to "GBP / NZD", "AUDCHF" to "AUD / CHF", "AUDCAD" to "AUD / CAD",
        "AUDNZD" to "AUD / NZD", "CADCHF" to "CAD / CHF", "NZDCAD" to "NZD / CAD",
        "NZDCHF" to "NZD / CHF"
    )

    fun get(symbol: String): String {
        return names[symbol.uppercase()] ?: if (symbol.length == 6) {
            "${symbol.substring(0, 3)} / ${symbol.substring(3)}"
        } else symbol
    }

    fun pretty(symbol: String): String = get(symbol)

    fun payout(symbol: String): Int {
        var h = 0L
        for (c in symbol) h = ((h * 31L + c.code) and 0xFFFFFFFFL)
        return 80 + (h % 16).toInt()
    }
}

private object FeedClock {
    @Volatile private var utcAtSync=0L
    @Volatile private var elapsedAtSync=0L
    @Synchronized fun observe(serverDate:Long,started:Long,finished:Long) {
        val rtt=finished-started
        if(serverDate<=0L||rtt !in 0L..4000L)return
        utcAtSync=serverDate+500L+rtt/2L
        elapsedAtSync=finished
    }
    @Synchronized fun now():Long = if(utcAtSync>0L)
        utcAtSync+(android.os.SystemClock.elapsedRealtime()-elapsedAtSync)
    else System.currentTimeMillis()
}
private data class ChartTick(val price:Double,val time:Long)
private fun sourceTimestamp(raw:Any?):Long {
    val t=when(raw) { is Number->raw.toLong();is String->raw.toLongOrNull() ?: runCatching { java.time.Instant.parse(raw).toEpochMilli() }.getOrDefault(0L);else->0L }
    return if(t in 1..9_999_999_999L)t*1000 else t
}
private fun chartFrameMillis(frame:String):Long=when(frame.firstOrNull()) {
    'S'->(frame.drop(1).toLongOrNull() ?: 5L)*1000
    'M'->(frame.drop(1).toLongOrNull() ?: 1L)*MINUTE_MS
    'H'->(frame.drop(1).toLongOrNull() ?: 1L)*3_600_000L
    'D'->(frame.drop(1).toLongOrNull() ?: 1L)*86_400_000L
    else->MINUTE_MS
}
private fun chartSourceInterval(frame:String)=when(frame) {
    "M5","M10"->"5m";"M15"->"15m";"M30"->"30m";"H1"->"1h";"H4"->"4h";"D1"->"1d";else->"1m"
}
private fun aggregateChartBars(bars:List<Candle>,intervalMs:Long,sourceIntervalMs:Long=intervalMs):List<Candle> {
    if(intervalMs<=0||sourceIntervalMs<=0||intervalMs%sourceIntervalMs!=0L)return emptyList()
    val source=bars.filter(::validCandle).groupBy { it.time }.map { (_,versions)->
        versions.lastOrNull { it.isClosed } ?: versions.last()
    }.sortedBy { it.time }
    if(intervalMs==sourceIntervalMs)return source
    val newestBucket=source.lastOrNull()?.let { it.time/intervalMs*intervalMs } ?: return emptyList()
    return source.groupBy { it.time/intervalMs*intervalMs }.mapNotNull { (time,group)->
        val contiguous=group.withIndex().all { (i,c)->c.time==time+i.toLong()*sourceIntervalMs }
        val complete=group.size.toLong()==intervalMs/sourceIntervalMs
        if(!contiguous||(!complete&&time!=newestBucket))null
        else Candle(group.first().open,group.maxOf { it.high },group.minOf { it.low },
            group.last().close,time,complete&&group.all { it.isClosed })
    }.sortedBy { it.time }
}
private fun chartBarsFromTicks(ticks:List<ChartTick>,intervalMs:Long):List<Candle> {
    if(intervalMs<=0)return emptyList()
    val source=ticks.filter { it.time>0&&it.price.isFinite()&&it.price>0 }.sortedBy { it.time }
    val bars=mutableListOf<Candle>()
    val cursor=ChartTickCursor()
    for(tick in source)updateCandleFromTick(bars,tick.price,tick.time,intervalMs,cursor)
    return bars
}
private fun heikinAshi(bars:List<Candle>):List<Candle> {
    var previousOpen=0.0;var previousClose=0.0
    return bars.mapIndexed { index,c->
        val close=(c.open+c.high+c.low+c.close)/4.0
        val open=if(index==0)(c.open+c.close)/2.0 else (previousOpen+previousClose)/2.0
        previousOpen=open;previousClose=close
        Candle(open,maxOf(c.high,open,close),minOf(c.low,open,close),close,c.time,c.isClosed)
    }
}
private object ChartSettings {
    var frame by mutableStateOf("M1")
    var chartType by mutableStateOf("شموع")
    var countdownEnabled by mutableStateOf(true)
    var autoScrollEnabled by mutableStateOf(true)
}

object BiquoteApi {
    private fun get(path:String,params:List<Pair<String,String>> = emptyList()):String? {
        val qs=if(params.isEmpty()) "" else "?"+params.joinToString("&") { "${it.first}=${URLEncoder.encode(it.second,"UTF-8")}" }
        var c:HttpURLConnection?=null
        val started=android.os.SystemClock.elapsedRealtime()
        return try { c=(URL("https://biquote.io$path$qs").openConnection() as HttpURLConnection).apply {
                connectTimeout=6000;readTimeout=6000;requestMethod="GET";useCaches=false
                setRequestProperty("Cache-Control","no-cache, no-store")
            }
            val status=c.responseCode
            FeedClock.observe(c.date,started,android.os.SystemClock.elapsedRealtime())
            if(status in 200..299)c.inputStream.bufferedReader().use { it.readText() } else null
        } catch(_:Exception) { null } finally { c?.disconnect() }
    }
    suspend fun liveQuote(symbol:String):Quote?=withContext(Dispatchers.IO) {
        val txt=get("/api/${symbol.uppercase(Locale.US)}",listOf("allowStale" to "false")) ?: return@withContext null
        try {
            val response=JSONObject(txt)
            val price=response.optDouble("mid",Double.NaN)
            val sourceTime=sequenceOf(response.opt("timestamp"),response.opt("lastQuoteAt"))
                .map(::sourceTimestamp).firstOrNull { it>0L } ?: return@withContext null
            // Never relabel an old market quote with the local request time.
            if(!price.isFinite()||price<=0.0||response.optBoolean("stale",false)||
                FeedClock.now()-sourceTime !in -5000L..15000L)return@withContext null
            Quote(price,System.currentTimeMillis(),sourceTime)
        } catch(cancelled:CancellationException) { throw cancelled }
        catch(_:Exception) { null }
    }
    suspend fun tick(symbol:String):Double?=liveQuote(symbol)?.price
    suspend fun ohlc(symbol:String,interval:String="1m",limit:Int=300):List<Candle> = withContext(Dispatchers.IO) {
        val txt=get("/api/${symbol.uppercase(Locale.US)}/ohlc",listOf("interval" to interval,"limit" to "$limit")) ?: return@withContext emptyList()
        try {
            val response=JSONObject(txt)
            val a=response.optJSONArray("bars") ?: return@withContext emptyList()
            (0 until a.length()).mapNotNull { i->
                val b=a.optJSONObject(i) ?: return@mapNotNull null
                val t=sequenceOf(b.opt("openTime"),b.opt("time"),b.opt("t"))
                    .map(::sourceTimestamp).firstOrNull { it>0 } ?: 0L
                val candle=Candle(b.optDouble("open",b.optDouble("o")),b.optDouble("high",b.optDouble("h")),b.optDouble("low",b.optDouble("l")),b.optDouble("close",b.optDouble("c")),t,
                    b.has("isOpen")&&!b.optBoolean("isOpen",true))
                if(!validCandle(candle))null else candle
            }.sortedBy { it.time }
        } catch(e:Exception) { emptyList() }
    }
    internal suspend fun chartHistory(symbol:String,frame:String,recent:Boolean=false):List<Candle> {
        val ms=chartFrameMillis(frame)
        if(ms<MINUTE_MS) return emptyList()
        val base=chartSourceInterval(frame)
        val baseMs=when(base) { "5m"->300_000L;"15m"->900_000L;"30m"->1_800_000L;"1h"->3_600_000L;"4h"->14_400_000L;"1d"->86_400_000L;else->MINUTE_MS }
        val factor=(ms/baseMs).toInt().coerceAtLeast(1)
        val source=ohlc(symbol,base,if(recent)maxOf(8,factor*4) else 1000)
        return aggregateChartBars(source,ms,baseMs)
    }
    suspend fun symbols(liveOnly:Boolean=false):List<String> = withContext(Dispatchers.IO) {
        val txt=get("/api/symbols",listOf("liveOnly" to "$liveOnly")) ?: return@withContext emptyList()
        try { val a=JSONArray(txt); (0 until a.length()).mapNotNull { i->
            val s=when(val v=a.opt(i)) { is String->v; is JSONObject->v.optString("name",v.optString("symbol","")); else->"" }
            s.takeIf { it.isNotBlank() }?.uppercase(Locale.US)?.replace(Regex("[_/\\-]"),"")
        }.filter { SymCatalog.isForex(it) }.distinct() } catch(_:Exception) { emptyList() }
    }
}
object SymCatalog {
    private val forex="EUR USD GBP JPY CHF AUD CAD NZD TRY ZAR MXN SEK NOK SGD HKD PLN DKK HUF CZK ILS CNY QAR SAR UAH IDR THB MYR PHP INR KRW BRL ARS CLP COP PEN RUB AED".split(" ").toSet()
    fun isForex(symbol:String):Boolean {
        val s=symbol.uppercase(Locale.US)
        return s.length==6&&s.take(3) in forex&&s.drop(3) in forex&&s.take(3)!=s.drop(3)
    }
    fun pretty(s:String)=if(isForex(s)) "${s.take(3)}/${s.drop(3)}" else s
    fun payout(s:String):Int { var h=0L;for(c in s)h=(h*31+c.code) and 0xFFFFFFFFL;return 80+(h%16).toInt() }
}

object CurrencyStore {
    const val USD="USD"; const val TRY="TRY"
    var selected by mutableStateOf(USD)
    const val USD_PER_TRY=1.0/40.0
    fun toggle() { /* Currency switching is disabled. */ }
    fun display(usd:Double,currency:String=selected)=if(currency==TRY) String.format(Locale.US,"%,.2f",usd/USD_PER_TRY) else money(usd)
}
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
private fun normalizedTradeAmount(text:String):String {
    val clean=text.trim()
    val value=clean.toDoubleOrNull()
    return if(!Regex("[0-9]+(?:\\.[0-9]*)?").matches(clean)||value==null||!value.isFinite()||value<1.0) "1" else clean
}
object TradeSettings {
    var duration by mutableStateOf("00:01:00")
    private var amountState by mutableStateOf("1")
    var amount:String
        get()=amountState
        set(value) { amountState=normalizedTradeAmount(value) }
    var percentageMode by mutableStateOf(false)
    var percentage by mutableStateOf("1")
    fun validPercentage():Boolean {
        val value=percentage.toDoubleOrNull()
        return value!=null&&value.isFinite()&&value>0.0&&value<=100.0
    }
    fun resolvedAmount():Double? {
        if(!percentageMode)return amount.toDoubleOrNull()
        if(!validPercentage())return null
        // Resolve against the selected account immediately before placing the trade.
        // Round down to cents so that 100% never exceeds the available balance.
        return java.math.BigDecimal.valueOf(AccountStore.balance())
            .multiply(java.math.BigDecimal(percentage))
            .divide(java.math.BigDecimal("100"),2,java.math.RoundingMode.DOWN).toDouble()
    }
}
private object InsufficientBalanceState {
    var account by mutableStateOf<String?>(null)
}
data class OpenTrade(val id:Long=nextId(),val symbol:String,val direction:Int,val entry:Float,val amount:Double,val openedAt:Long,val expiresAt:Long,val payoutPct:Int,val account:String=AccountStore.selected,val candleTime:Long=openedAt/MINUTE_MS*MINUTE_MS,val exactEntry:Double=entry.toDouble())
data class TradeResult(val trade:OpenTrade,val close:Double,val profit:Double,val closedAt:Long,val visibleUntil:Long=closedAt+Ref.ResultDuration)
data class Quote(val price:Double,val receivedAt:Long,val sourceTime:Long=0L)
object TradeStore {
    val currentSymbol=MutableStateFlow("EURUSD"); val livePrice=MutableStateFlow(0f)
    val quotes=mutableStateMapOf<String,Quote>()
    val openTrades=mutableStateListOf<OpenTrade>(); val closedTrades=mutableStateListOf<Pair<OpenTrade,Boolean>>()
    val results=mutableStateListOf<TradeResult>(); val history=mutableStateListOf<TradeResult>()
    fun quote(symbol:String,value:Double,sourceTime:Long=0L) {
        if(!value.isFinite()||value<=0)return
        val previous=quotes[symbol]
        if(sourceTime>0L) {
            if(FeedClock.now()-sourceTime !in -5000L..15000L)return
            if(previous!=null&&(sourceTime<previous.sourceTime||
                (sourceTime==previous.sourceTime&&value==previous.price)))return
        }
        quotes[symbol]=Quote(value,System.currentTimeMillis(),sourceTime)
        if(currentSymbol.value==symbol)livePrice.value=value.toFloat()
    }
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
                val env=when(wave) {
                    "soft"->.48*sin(PI*(t/dur).coerceIn(0.0,1.0)).pow(2.0)*exp(-t*10.0)
                    "chime"->.55*(1.0-exp(-t/.018))*exp(-t*5.0)*((dur-t)/.10).coerceIn(0.0,1.0)
                    "success"->.68*(1.0-exp(-t/.008))*exp(-t*6.0)*((dur-t)/.09).coerceIn(0.0,1.0)
                    "loss"->.60*(1.0-exp(-t/.018))*exp(-t*7.0)*((dur-t)/.10).coerceIn(0.0,1.0)
                    else->.9.pow(i/(SR*.08))
                }
                val x=2*PI*freq*t
                val s=when(wave) { "square"->if(sin(x)>=0)1.0 else -1.0; "sawtooth"->2*(t*freq-floor(.5+t*freq)); "triangle"->2/PI*asin(sin(x)); "chime"->(sin(x)+.12*sin(2*x)) / 1.12; "success"->(sin(x)+.24*sin(2*x)*exp(-t*9.0)+.08*sin(3*x)*exp(-t*14.0))/1.32; "loss"->(sin(x)+.16*sin(2*x)*exp(-t*10.0))/1.16; else->sin(x) }
                buf[pos]=(buf[pos]+(s*env*.18*Short.MAX_VALUE).toInt()).coerceIn(-32768,32767).toShort()
            }
        }
        var track:AudioTrack?=null
        try { track=AudioTrack(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SR).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),buf.size*2,AudioTrack.MODE_STATIC,0)
            track.write(buf,0,buf.size); track.play(); delay(total+100L)
        } catch(_:Exception) { } finally { track?.release() }
    } }
    fun open()=playTones(listOf(Triple(540f,0f,.10f to "soft"),Triple(810f,20f,.09f to "soft")))
    fun win()=playTones(listOf(Triple(659.25f,0f,.30f to "success"),Triple(987.77f,75f,.32f to "success"),Triple(1318.51f,150f,.44f to "success")))
    fun lose()=playTones(listOf(Triple(392f,0f,.28f to "loss"),Triple(293.66f,110f,.36f to "loss")))
}
private fun openTrade(direction:Int,symbol:String,payout:Int) {
    if(!SymCatalog.isForex(symbol)) { TradeMessage.show("التداول متاح لأزواج الفوركس فقط");return }
    TradeSettings.amount=TradeSettings.amount
    if(TradeSettings.percentageMode&&!TradeSettings.validPercentage()) {
        TradeMessage.show("أدخل نسبة أكبر من 0 ولا تتجاوز 100٪");return
    }
    val amount=TradeSettings.resolvedAmount()
    if(amount==null||!amount.isFinite()||amount<1.0) { TradeMessage.show("الحد الأدنى للصفقة 1 $"); return }
    val duration=durationMillis(TradeSettings.duration)
    if(duration==null) { TradeMessage.show("أدخل زمناً صحيحاً"); return }
    if(amount>AccountStore.balance()) { InsufficientBalanceState.account=AccountStore.selected; return }
    val now=System.currentTimeMillis(); val quote=TradeStore.quotes[symbol]
    if(quote==null||now-quote.receivedAt>15000) { TradeMessage.show("انتظر وصول سعر حديث لهذا الزوج"); return }
    AccountStore.debit(AccountStore.selected,amount)
    TradeStore.openTrades.add(OpenTrade(symbol=symbol,direction=direction,entry=quote.price.toFloat(),amount=amount,openedAt=now,expiresAt=now+duration,payoutPct=payout,exactEntry=quote.price))
    TradeSounds.open()
    val decimals=if(symbol.contains("JPY"))3 else if(symbol=="XAUUSD")2 else 5
    NotificationManager.show(symbol,priceText(quote.price,decimals))
}
@Composable private fun TradeSettlementLoop() {
    LaunchedEffect(Unit) {
        while(isActive) {
            val now=System.currentTimeMillis()
            val symbols=TradeStore.openTrades.map { it.symbol }.distinct()
            supervisorScope { symbols.map { symbol->async {
                val cached=TradeStore.quotes[symbol]
                if(symbol!=TradeStore.currentSymbol.value||cached==null||now-cached.receivedAt>3000) {
                    BiquoteApi.liveQuote(symbol)?.let { TradeStore.quote(symbol,it.price,it.sourceTime) }
                }
            } }.awaitAll() }
            for(tr in TradeStore.openTrades.toList()) {
                if(tr.expiresAt>System.currentTimeMillis())continue
                val q=TradeStore.quotes[tr.symbol] ?: continue
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
        val saved=PairSession.load(context)?.takeIf { SymCatalog.isForex(it) }; val live=BiquoteApi.symbols(true)
        TradeStore.currentSymbol.value=when { saved!=null&&saved in live->saved; live.isNotEmpty()->live.first(); saved!=null->saved; else->"EURUSD" }
        TradeStore.currentSymbol.collect { symbol->
            if(SymCatalog.isForex(symbol))PairSession.save(context,symbol)
            else TradeStore.currentSymbol.value="EURUSD"
        }
    }
    IndicatorPreferences()
    TradeSettlementLoop()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(C.BG)) {
            TopBar()
            Box(Modifier.fillMaxWidth().height(1.dp).background(C.Divider))
            LiveCandlestickChart(Modifier.weight(1f).fillMaxWidth()) { pickerOpen=true }
            TradePanel()
            ExtraIndicatorPanes()
            BottomNavBar()
        }
        val message=TradeMessage.text
        if(message.isNotEmpty()) Box(Modifier.align(Alignment.TopCenter).padding(top=110.dp,start=18.dp,end=18.dp).shadow(12.dp,RoundedCornerShape(10.dp)).background(Color(0xFF202638),RoundedCornerShape(10.dp)).border(1.dp,Color(0xFFF05B67),RoundedCornerShape(10.dp)).padding(horizontal=18.dp,vertical=13.dp)) { Text(message,color=Color.White,fontSize=14.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center) }
    }
    if(pickerOpen)PairPickerSheet { pickerOpen=false }
    InsufficientBalanceDialog()
    if(HistoryOverlay.open)TradeHistoryPage()
    FirstLaunchPromotion()
}

@Composable fun TopBar() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(Modifier.fillMaxWidth().background(C.BG).padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(C.Yellow),contentAlignment=Alignment.Center) { WalletIcon(Modifier.size(26.dp),Color(0xFF312740)) }
            Spacer(Modifier.width(8.dp))
            BalanceAnchor()
            Spacer(Modifier.weight(1f))
            Box {
                CachedTradingImage(url=GIFT_ICON_URL,contentDescription="هدية",contentScale=ContentScale.Fit,modifier=Modifier.size(36.dp))
                Box(Modifier.align(Alignment.TopEnd).offset(3.dp,(-3).dp).size(14.dp).clip(CircleShape).background(C.BG).border(1.dp,C.Divider,CircleShape)) { Canvas(Modifier.fillMaxSize()) {
                    val m=size.minDimension*.22f; val cx=size.width/2; val cy=size.height/2
                    drawLine(Color.White,Offset(cx-m,cy-m),Offset(cx+m,cy+m),1.dp.toPx(),StrokeCap.Round)
                    drawLine(Color.White,Offset(cx+m,cy-m),Offset(cx-m,cy+m),1.dp.toPx(),StrokeCap.Round)
                } }
            }
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(52.dp),contentAlignment=Alignment.Center) {
                CachedTradingImage(url=AVATAR_URL,contentDescription="الصورة الشخصية",contentScale=ContentScale.Crop,modifier=Modifier.size(36.dp).clip(CircleShape).background(C.Surface))
                CachedTradingImage(url=AVATAR_FRAME_URL,contentDescription=null,contentScale=ContentScale.Fit,modifier=Modifier.size(52.dp))
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
                    Text("حلال",color=C.Green,fontSize=10.sp,modifier=Modifier.alpha(0f)); Spacer(Modifier.width(6.dp)); Text(CurrencyStore.selected,color=C.TextSecondary,fontSize=10.sp)
                }
                Text(CurrencyStore.display(AccountStore.balance()),color=C.TextPrimary,fontSize=20.sp,fontWeight=FontWeight.Bold)
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
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) { Text(if(account=="demo")"QT Demo" else "QT Real",color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Bold); Text(CurrencyStore.display(AccountStore.balance(account))+" "+CurrencyStore.selected,color=Ref.Muted,fontSize=12.sp) }
        if(account=="real")Box(Modifier.size(48.dp,25.dp).clip(RoundedCornerShape(8.dp)).clickable(enabled=false) { CurrencyStore.toggle() }) { Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(Color(0xFF3A5B8C),size=size,cornerRadius=CornerRadius(8.dp.toPx()),style=Stroke(1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(),3.dp.toPx()))))
            val p=NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.WHITE; textSize=11.dp.toPx(); textAlign=NativePaint.Align.CENTER; typeface=Typeface.DEFAULT_BOLD }
            drawContext.canvas.nativeCanvas.drawText(CurrencyStore.selected,size.width/2,size.height/2-(p.ascent()+p.descent())/2,p)
        } }
    }
}

private fun mergeCandle(list:MutableList<Candle>,incoming:Candle,bulk:Boolean=false):Boolean {
    if(!validCandle(incoming))return false
    val index=list.binarySearchBy(incoming.time) { it.time }
    if(index>=0) {
        val previous=list[index]
        val replacement=if(bulk) {
            if(previous.isClosed&&!incoming.isClosed)return false
            incoming
        } else {
            if(previous.isClosed)return false
            previous.copy(high=max(previous.high,incoming.high),
                low=min(previous.low,incoming.low),close=incoming.close)
        }
        if(previous==replacement)return false
        list[index]=replacement
        return true 
    }
    list.add(-index-1,incoming)
    return true
}
private fun mergeReferenceBars(list:MutableList<Candle>,incoming:List<Candle>,bulk:Boolean):Boolean {
    var changed=false
    for(bar in incoming.filter(::validCandle).sortedBy { it.time }) {
        val index=list.binarySearchBy(bar.time) { it.time }
        if(index>=0) {
            val old=list[index]
            val next=old.copy(high=max(old.high,bar.high),low=min(old.low,bar.low),
                close=bar.close,isClosed=old.isClosed||bar.isClosed||index<list.lastIndex)
            if(next!=old) { list[index]=next;changed=true }
        } else {
            val at=-index-1
            val next=if(at==list.size&&list.isNotEmpty()) {
                val previous=list.last()
                if(!previous.isClosed)list[list.lastIndex]=previous.copy(isClosed=true)
                val open=previous.close
                bar.copy(open=open,high=max(bar.high,open),low=min(bar.low,open))
            } else bar
            list.add(at,next);changed=true
        }
    }
    for(i in list.indices) {
        val old=list[i]
        val open=if(bulk&&i>0)list[i-1].close else old.open
        val next=old.copy(open=open,high=max(old.high,open),low=min(old.low,open),
            isClosed=old.isClosed||i<list.lastIndex)
        if(next!=old) { list[i]=next;changed=true }
    }
    return changed
}

private fun referenceBlend(perFrame:Double,dt:Double)=
    1.0-(1.0-perFrame).pow(dt.coerceIn(0.0,.06)*60.0)
private fun referenceDisplayCandle(previous:Candle?,target:Candle,previousClose:Double?,dt:Double):Candle {
    if(previous==null)return target
    if(previous.time!=target.time) {
        val open=previousClose ?: target.open
        return target.copy(open=open,high=open,low=open,close=open)
    }
    val blend=referenceBlend(.20,dt)
    fun mix(from:Double,to:Double)=from+(to-from)*blend
    val open=mix(previous.open,target.open)
    val mixedClose=mix(previous.close,target.close)
    val close=if(abs(target.close-mixedClose)<1e-7)target.close else mixedClose
    return target.copy(open=open,close=close,
        high=maxOf(mix(previous.high,target.high),open,close),
        low=minOf(mix(previous.low,target.low),open,close))
}

private class ChartTickCursor {
    var time=0L
    var price=Double.NaN
}
private fun updateCandleFromTick(list:MutableList<Candle>,price:Double,time:Long,intervalMs:Long,cursor:ChartTickCursor):Boolean {
    if(!price.isFinite()||price<=0.0||time<=0||intervalMs<=0)return false
    if(time<cursor.time)return false
    if(time==cursor.time&&price==cursor.price)return false
    val bucket=time/intervalMs*intervalMs
    val last=list.lastOrNull()
    if(last!=null&&(bucket<last.time||(bucket==last.time&&last.isClosed)))return false
    if(last!=null&&bucket>last.time&&!last.isClosed) {
        list[list.lastIndex]=last.copy(isClosed=true)
    }
    val open=if(last!=null&&bucket>last.time)last.close else price
    val changed=mergeCandle(list,Candle(open,max(open,price),min(open,price),price,bucket))
    cursor.time=time;cursor.price=price
    return changed
}

private data class ChartViewport(val visibleBars:Double,val anchorBar:Double,val anchorFraction:Double,val centerPrice:Double,val priceSpan:Double) {
    val centerBar get()=anchorBar+(.5-anchorFraction)*visibleBars
    val minPrice get()=centerPrice-priceSpan/2
    val maxPrice get()=centerPrice+priceSpan/2
    fun fractionFor(bar:Double)=anchorFraction+(bar-anchorBar)/visibleBars
    fun reanchor(bar:Double)=copy(anchorBar=bar,anchorFraction=fractionFor(bar))
}
private fun initialViewport(candles:List<Candle>,origin:Long,intervalMs:Long=MINUTE_MS,visibleBars:Double=60.0):ChartViewport {
    if(candles.isEmpty())return ChartViewport(visibleBars,0.0,LIVE_CANDLE_FRACTION,1.0,MIN_PRICE_SPAN)
    val view=ChartViewport(visibleBars,barPosition(candles.last().time,origin,intervalMs),LIVE_CANDLE_FRACTION,1.0,MIN_PRICE_SPAN)
    val(center,span)=verticalTarget(view,candles,origin,intervalMs)
    return view.copy(centerPrice=center,priceSpan=span)
}
private fun boundedChartCenter(view:ChartViewport,desired:Double,first:Double,last:Double):Double {
    val liveCenter=last+(.5-LIVE_CANDLE_FRACTION)*view.visibleBars
    val historyCenter=min(first+view.visibleBars/2-1,liveCenter)
    return desired.coerceIn(historyCenter,liveCenter)
}
private fun ChartViewport.atLiveBoundary(last:Double)=
    abs(centerBar-(last+(.5-LIVE_CANDLE_FRACTION)*visibleBars))<.000001
private fun verticalTarget(view:ChartViewport,candles:List<Candle>,origin:Long,intervalMs:Long=MINUTE_MS):Pair<Double,Double> {
    if(candles.isEmpty())return view.centerPrice to view.priceSpan
    val rendered=if(ChartSettings.chartType=="هيكين آشي")heikinAshi(candles) else candles
    val visible=rendered.filter { abs(barPosition(it.time,origin,intervalMs)-view.centerBar)<=view.visibleBars/2+.6 }
    if(visible.isEmpty())return view.centerPrice to view.priceSpan
    val low=visible.minOf { it.low.toDouble() }; val high=visible.maxOf { it.high.toDouble() }
    // توزيع متوازن: 12% حشو من الأعلى والأسفل
    val padding = max(0.0005, (high - low) * 0.12) 
    return (high+low)/2 to (high-low+padding*2).coerceAtLeast(MIN_PRICE_SPAN)
}
// Expand upwards before a visible wick reaches the small marker below the clock.
// Keep the lower price bound fixed; never clip candles to enforce this limit.
private fun clockSafeViewport(view:ChartViewport,candles:List<Candle>,origin:Long,intervalMs:Long,height:Float,ceilingY:Float,headroom:Float):ChartViewport {
    if(candles.isEmpty()||height<=1f)return view
    val rendered=if(ChartSettings.chartType=="هيكين آشي")heikinAshi(candles) else candles
    val visible=rendered.filter { abs(barPosition(it.time,origin,intervalMs)-view.centerBar)<=view.visibleBars/2+.6 }
    val high=visible.maxOfOrNull { it.high } ?: return view
    val safeFraction=((ceilingY+headroom)/height).toDouble().coerceIn(0.0,.80)
    val requiredSpan=((high-view.minPrice)/(1.0-safeFraction)).coerceAtLeast(MIN_PRICE_SPAN)
    if(!requiredSpan.isFinite()||requiredSpan<=view.priceSpan)return view
    return view.copy(centerPrice=view.minPrice+requiredSpan/2,priceSpan=requiredSpan)
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
    var menuOpen by remember { mutableStateOf(false) }
    var chartStyleOpen by remember { mutableStateOf(false) }
    var indicatorsOpen by remember { mutableStateOf(false) }
    var drawingsOpen by remember { mutableStateOf(false) }
    var selectedTool by remember { mutableStateOf("temperature") }
    val density=LocalDensity.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(C.Surface).clickable { menuOpen=!menuOpen },contentAlignment=Alignment.Center) {
                    if(menuOpen)ReferenceGlyph("close",Modifier.size(25.dp),Color(0xFF9CB8CC)) else DotsIcon(Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Row(Modifier.height(44.dp).clip(RoundedCornerShape(14.dp)).background(C.Surface).clickable(onClick=onClick).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
                    val icons=IconProvider.getPairIcons(sym)
                    Box(Modifier.width(34.dp).height(24.dp)) {
                        if(icons.isNotEmpty())CurrencyIcon(icons[0],sym,Modifier.size(24.dp).align(Alignment.CenterStart))
                        if(icons.size>1)CurrencyIcon(icons[1],sym.drop(3),Modifier.size(24.dp).align(Alignment.CenterEnd).border(1.dp,C.Surface,CircleShape))
                    }
                    Spacer(Modifier.width(8.dp));Text(SymCatalog.pretty(sym),color=C.TextPrimary,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp));DownArrow(Modifier.size(9.dp),Color.White)
                }
            }
            if(menuOpen)Popup(popupPositionProvider=remember(density.density) { object:PopupPositionProvider {
                override fun calculatePosition(anchorBounds:IntRect,windowSize:IntSize,layoutDirection:LayoutDirection,popupContentSize:IntSize):IntOffset {
                    val margin=(8*density.density).roundToInt()
                    val x=anchorBounds.left.coerceIn(margin,(windowSize.width-popupContentSize.width-margin).coerceAtLeast(margin))
                    val y=anchorBounds.bottom+(8*density.density).roundToInt()
                    return IntOffset(x,y.coerceIn(margin,(windowSize.height-popupContentSize.height-margin).coerceAtLeast(margin)))
                }
            } },onDismissRequest={menuOpen=false},properties=PopupProperties(focusable=true,clippingEnabled=false)) {
                Column(Modifier.background(C.Surface).padding(5.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        ReferenceToolTile("candles",badge=ChartSettings.frame) { selectedTool="candles";menuOpen=false;chartStyleOpen=true }
                        ReferenceToolTile("settings",checked=IndicatorSettings.acceleratorEnabled||ExtraIndicators.active.isNotEmpty()) { selectedTool="settings";menuOpen=false;indicatorsOpen=true }
                    }
                    ReferenceToolTile("drawing",checked=selectedTool=="drawing") { selectedTool="drawing";menuOpen=false;drawingsOpen=true }
                }
            }
        }
    }
    if(chartStyleOpen)ChartStyleDialog { chartStyleOpen=false }
    if(indicatorsOpen)IndicatorsDialog { indicatorsOpen=false }
    if(drawingsOpen)DrawingsDialog { drawingsOpen=false }
}
@Composable private fun ReferenceToolTile(kind:String,modifier:Modifier=Modifier,badge:String?=null,checked:Boolean=false,onClick:()->Unit) {
    Box(modifier.size(40.dp).clickable(onClick=onClick)) {
        ReferenceGlyph(kind,Modifier.size(23.dp).align(Alignment.Center),Color(0xFF9CB8CC))
        if(badge!=null||checked)Box(Modifier.align(Alignment.TopEnd).size(if(badge!=null)20.dp else 15.dp).clip(CircleShape).background(C.Cyan),contentAlignment=Alignment.Center) {
            if(badge!=null)Text(badge,color=C.BG,fontSize=8.sp,maxLines=1) else ReferenceGlyph("check",Modifier.size(10.dp),C.BG)
        }
    }
}

private val ChartTimeframes=listOf("S5","S10","S15","S30","M1","M2","M3","M5","M10","M15","M30","H1","H4","D1")
private val ChartTypeNames=listOf("رسم بياني","شموع","أشرطة","هيكين آشي")
@Composable private fun PlatformSwitch(checked:Boolean,onToggle:()->Unit) {
    Box(Modifier.width(40.dp).height(22.dp).clip(RoundedCornerShape(11.dp)).background(if(checked)Color(0xFF2E9DDB) else C.SurfaceRaised).border(1.dp,if(checked)C.Cyan else C.Divider,RoundedCornerShape(11.dp)).clickable(onClick=onToggle),contentAlignment=if(checked)Alignment.CenterEnd else Alignment.CenterStart) {
        Box(Modifier.padding(horizontal=3.dp).size(14.dp).clip(CircleShape).background(if(checked)Color(0xFFBEE7FB) else C.TextSecondary))
    }
}
private object ChartMenuColors {
    val Panel=Color(0xFF1B1F2B);val Tile=Color(0xFF25253D);val Selected=Color(0xFF354961)
    val Header=Color(0xFF191F2B);val Icon=Color(0xFFA7C5D8);val Separator=Color(0xFF191D2C)
}
@Composable private fun ChartTypeGlyph(kind:String,modifier:Modifier=Modifier,color:Color=ChartMenuColors.Icon) { Canvas(modifier) {
    val w=size.width;val h=size.height;val stroke=min(w,h)*.045f
    drawLine(color,Offset(w*.12f,h*.90f),Offset(w*.88f,h*.90f),stroke,StrokeCap.Round)
    when(kind) {
        "رسم بياني"->{
            val line=Path().apply { moveTo(w*.12f,h*.55f);lineTo(w*.27f,h*.37f);lineTo(w*.40f,h*.51f);lineTo(w*.57f,h*.25f);lineTo(w*.70f,h*.38f);lineTo(w*.88f,h*.12f) }
            drawPath(line,color,style=Stroke(stroke,cap=StrokeCap.Round,join=StrokeJoin.Round))
            val area=Path().apply { moveTo(w*.12f,h*.68f);lineTo(w*.27f,h*.49f);lineTo(w*.40f,h*.63f);lineTo(w*.57f,h*.38f);lineTo(w*.70f,h*.51f);lineTo(w*.88f,h*.26f);lineTo(w*.88f,h*.80f);lineTo(w*.12f,h*.80f);close() }
            drawPath(area,color)
        }
        "أشرطة"->for((i,x) in listOf(.24f,.50f,.76f).withIndex()) {
            val top=h*listOf(.23f,.36f,.12f)[i];val bottom=h*listOf(.71f,.77f,.70f)[i]
            drawLine(color,Offset(w*x,top),Offset(w*x,bottom),stroke,StrokeCap.Butt)
            drawLine(color,Offset(w*x-w*.10f,top+h*.14f),Offset(w*x,top+h*.14f),stroke,StrokeCap.Butt)
            drawLine(color,Offset(w*x,bottom-h*.14f),Offset(w*x+w*.10f,bottom-h*.14f),stroke,StrokeCap.Butt)
        }
        else->{
            val heikin=kind=="هيكين آشي"
            for((i,x) in listOf(.24f,.52f,.80f).withIndex()) {
                val top=h*(if(heikin)listOf(.12f,.27f,.38f)[i] else listOf(.38f,.30f,.15f)[i])
                val body=h*(if(heikin).32f else listOf(.25f,.22f,.45f)[i])
                drawLine(color,Offset(w*x,top-h*.09f),Offset(w*x,top+body+h*.16f),stroke*.8f,StrokeCap.Round)
                drawRoundRect(color,Offset(w*(x-.08f),top),Size(w*.16f,body),CornerRadius(w*.013f))
            }
        }
    }
} }
@Composable private fun ChartStyleDialog(onDismiss:()->Unit) {
    var tab by remember { mutableIntStateOf(1) }
    val frame=ChartSettings.frame
    val screenHeight=LocalConfiguration.current.screenHeightDp
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(Modifier.widthIn(max=460.dp).fillMaxWidth(.90f).heightIn(max=(screenHeight*.88f).dp).background(ChartMenuColors.Panel).verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth().height(40.dp).background(ChartMenuColors.Header)) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(if(tab==1)ChartMenuColors.Tile.copy(alpha=.45f) else Color.Transparent).clickable { tab=1 },contentAlignment=Alignment.Center) {
                        Text("أنواع الرسوم البيانية",color=C.TextPrimary,fontSize=14.sp,textAlign=TextAlign.Center)
                    }
                    Box(Modifier.weight(1f).fillMaxHeight().background(if(tab==0)ChartMenuColors.Tile.copy(alpha=.45f) else Color.Transparent).clickable { tab=0 },contentAlignment=Alignment.Center) {
                        Text("الأطر الزمنية ($frame)",color=C.TextPrimary,fontSize=14.sp,textAlign=TextAlign.Center,style=TextStyle(textDirection=TextDirection.Rtl))
                    }
                }
                if(tab==0)Column(Modifier.fillMaxWidth().heightIn(min=(screenHeight*.41f).dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    ChartTimeframes.chunked(5).forEach { row->
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                            row.forEach { f->Box(Modifier.weight(1f).aspectRatio(1.25f).background(if(f==frame)ChartMenuColors.Selected else ChartMenuColors.Tile).clickable { ChartSettings.frame=f },contentAlignment=Alignment.Center) {
                                Text(f,color=if(f==frame)Color.White else ChartMenuColors.Icon,fontSize=15.sp)
                            } }
                            repeat(5-row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                } else {
                    Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
                        ChartTypeNames.chunked(2).forEach { row->
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                                row.forEach { name->
                                    Column(Modifier.weight(1f).aspectRatio(1.60f).background(if(name==ChartSettings.chartType)ChartMenuColors.Selected else ChartMenuColors.Tile).clickable { ChartSettings.chartType=name;if(name=="هيكين آشي")TradeMessage.show("هيكين آشي محسوبة وليست أسعار الفتح والإغلاق الأصلية؛ اختر شموع للأسعار الأصلية") },horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                                        ChartTypeGlyph(name,Modifier.size(62.dp,54.dp))
                                        Spacer(Modifier.height(12.dp));Text(name,color=C.TextPrimary,fontSize=14.sp)
                                    }
                                }
                            }
                        }
                    }
                    Column(Modifier.fillMaxWidth().padding(horizontal=30.dp,vertical=18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically) { PlatformSwitch(ChartSettings.countdownEnabled) { ChartSettings.countdownEnabled=!ChartSettings.countdownEnabled };Spacer(Modifier.width(14.dp));Text("تمكين عداد الوقت",color=C.TextPrimary,fontSize=13.sp) }
                        Row(verticalAlignment=Alignment.CenterVertically) { PlatformSwitch(ChartSettings.autoScrollEnabled) { ChartSettings.autoScrollEnabled=!ChartSettings.autoScrollEnabled };Spacer(Modifier.width(14.dp));Text("تمكين التمرير التلقائي",color=C.TextPrimary,fontSize=13.sp) }
                    }
                    Box(Modifier.fillMaxWidth().height(76.dp).background(ChartMenuColors.Header).padding(horizontal=30.dp),contentAlignment=Alignment.CenterStart) {
                        Text("ألوان شمعة مخصصة",color=ChartMenuColors.Icon,fontSize=14.sp,style=TextStyle(textDecoration=TextDecoration.Underline),modifier=Modifier.clickable { TradeMessage.show("ألوان الشموع المخصصة غير متاحة في هذه النسخة") })
                    }
                }
            }
        }
    }
}

private val IndicatorNames=listOf(
    "Accelerator Oscillator","ADX","Alligator","Aroon","Average True Range",
    "Awesome Oscillator","Bears Power","Bollinger Bands","Bollinger Bands Width",
    "Bulls Power","CCI","Donchian Channel","DeMarker","Envelopes","Fractal",
    "Fractal Chaos Bands","مؤشر إيشيموكو كينكو هيو","Keltner Channel","MACD","Momentum",
    "Moving Average","OsMA","Parabolic SAR","RSI","Rate of Change","Schaff Trend Cycle",
    "Stochastic Oscillator","SuperTrend","Volume","مؤشر الدوامة","مؤشر ويليامز %R","ZigZag"
)
private data class IndicatorChartSnapshot(
    val bars:List<Candle>,val viewport:ChartViewport,val origin:Long,
    val intervalMs:Long,val symbol:String,val frame:String
)
private object IndicatorSettings {
    var acceleratorEnabled by mutableStateOf(false)
    var fastPeriod by mutableIntStateOf(5)
    var slowPeriod by mutableIntStateOf(34)
    var signalPeriod by mutableIntStateOf(5)
    var snapshot by mutableStateOf<IndicatorChartSnapshot?>(null)
    val favorites=mutableStateMapOf<String,Boolean>()
}
@Composable private fun IndicatorStar(selected:Boolean,onClick:()->Unit) {
    Canvas(Modifier.size(25.dp).clickable(onClick=onClick).padding(4.dp)) {
        val path=Path()
        for(i in 0 until 10) {
            val angle=-PI/2+i*PI/5
            val radius=size.minDimension*(if(i%2==0).48f else .22f)
            val x=size.width/2+cos(angle).toFloat()*radius
            val y=size.height/2+sin(angle).toFloat()*radius
            if(i==0)path.moveTo(x,y) else path.lineTo(x,y)
        }
        path.close()
        val gold=Color(0xFFFFA51D)
        if(selected)drawPath(path,gold)
        else drawPath(path,gold,style=Stroke(1.25.dp.toPx(),join=StrokeJoin.Round))
    }
}
@Composable private fun IndicatorsDialog(onDismiss:()->Unit) {
    val config=LocalConfiguration.current
    val scroll=rememberScrollState()
    val shape=RoundedCornerShape(12.dp)
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(Modifier.fillMaxWidth(.89f).height((config.screenHeightDp*.71f).dp)
                .clip(shape).background(Color(0xFF222E43)).border(1.dp,Color(0xFF38485E),shape)) {
                Row(Modifier.fillMaxWidth().height(53.dp).padding(start=21.dp,end=12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("المؤشرات",color=Color(0xFFF1F4F8),fontSize=14.sp,style=TextStyle(textDirection=TextDirection.Rtl),modifier=Modifier.weight(1f))
                    Box(Modifier.size(34.dp).clickable(onClick=onDismiss),contentAlignment=Alignment.Center) {
                        ReferenceGlyph("close",Modifier.size(23.dp),Color(0xFFA8C0D2))
                    }
                }
                Box(Modifier.fillMaxWidth().weight(1f).padding(bottom=2.dp)) {
                    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(start=13.dp,end=17.dp,bottom=12.dp)) {
                        IndicatorNames.forEach { name->
                            Row(Modifier.fillMaxWidth().height(25.dp),verticalAlignment=Alignment.CenterVertically) {
                                IndicatorStar(IndicatorSettings.favorites[name]==true) {
                                    IndicatorSettings.favorites[name]=IndicatorSettings.favorites[name]!=true
                                }
                                Text(name,color=Color(0xFFF1F4F8),fontSize=15.sp,maxLines=1,
                                    overflow=TextOverflow.Ellipsis,
                                    style=TextStyle(textDirection=TextDirection.Content),
                                    modifier=Modifier.weight(1f).fillMaxHeight().clickable {
                                        if(name=="Accelerator Oscillator") {
                                            IndicatorSettings.acceleratorEnabled=true
                                            onDismiss()
                                        } else if(ExtraIndicators.enable(name)) {
                                            onDismiss()
                                        } else {
                                            TradeMessage.show(if(name=="Volume")"لم يُفعّل Volume: بيانات الشموع الحالية لا تحتوي حجم تداول حقيقي" else "تُرك $name دون تفعيل؛ لم تُضف له حسابات غير متحقق منها")
                                            onDismiss()
                                        }
                                    }.wrapContentHeight(Alignment.CenterVertically).padding(start=6.dp))
                            }
                        }
                    }
                    Canvas(Modifier.align(Alignment.CenterEnd).width(7.dp).fillMaxHeight().padding(end=2.dp,top=0.dp,bottom=0.dp)) {
                        val total=scroll.maxValue.toFloat()+size.height
                        if(scroll.maxValue>0&&total>0) {
                            val thumb=(size.height*size.height/total).coerceIn(22.dp.toPx(),size.height)
                            val top=(size.height-thumb)*scroll.value/scroll.maxValue.toFloat()
                            drawRoundRect(Color(0xFF7F8999),Offset(0f,top),Size(size.width,thumb),CornerRadius(size.width/2))
                        }
                    }
                }
            }
        }
    }
}
private fun acceleratorValues(bars:List<Candle>,fast:Int,slow:Int,signal:Int):DoubleArray {
    require(fast>0&&slow>0&&signal>0)
    val n=bars.size
    val medianPrefix=DoubleArray(n+1)
    for(i in bars.indices)medianPrefix[i+1]=medianPrefix[i]+(bars[i].high.toDouble()+bars[i].low.toDouble())/2.0
    val start=max(fast,slow)-1
    val ao=DoubleArray(n) { Double.NaN }
    val ac=DoubleArray(n) { Double.NaN }
    var signalSum=0.0
    for(i in start until n) {
        ao[i]=(medianPrefix[i+1]-medianPrefix[i+1-fast])/fast-(medianPrefix[i+1]-medianPrefix[i+1-slow])/slow
        signalSum+=ao[i]
        if(i-start>=signal)signalSum-=ao[i-signal]
        if(i-start+1>=signal)ac[i]=ao[i]-signalSum/signal
    }
    return ac
}
@Composable private fun AcceleratorOscillatorPane() {
    val symbol by TradeStore.currentSymbol.collectAsState()
    val snapshot=IndicatorSettings.snapshot?.takeIf { it.symbol==symbol&&it.frame==ChartSettings.frame }
    val fast=IndicatorSettings.fastPeriod;val slow=IndicatorSettings.slowPeriod;val signal=IndicatorSettings.signalPeriod
    val values=remember(snapshot?.bars,fast,slow,signal) { acceleratorValues(snapshot?.bars ?: emptyList(),fast,slow,signal) }
    var expanded by remember { mutableStateOf(true) }
    var settingsOpen by remember { mutableStateOf(false) }
    val paneHeight=(LocalConfiguration.current.screenHeightDp*.145f).coerceIn(88f,132f).dp
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(Modifier.fillMaxWidth().height(if(expanded)paneHeight else 28.dp).clip(RoundedCornerShape(0.dp)).background(Color(0xFF1D2332))) {
            CachedTradingImage(url=CHART_BG_URL,contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Color(0xFF1D2332).copy(alpha=.88f)))
            Canvas(Modifier.fillMaxSize()) {
                val w=size.width;val h=size.height
                drawLine(Color(0xFF3C6686).copy(alpha=.7f),Offset.Zero,Offset(w,0f),1.dp.toPx())
                if(!expanded)return@Canvas
                val data=snapshot ?: return@Canvas
                val view=data.viewport
                fun xFor(time:Long)=(view.fractionFor((time-data.origin).toDouble()/data.intervalMs)*w).toFloat()
                val visible=data.bars.indices.filter { values[it].isFinite()&&xFor(data.bars[it].time) in -16f..(w+16f) }
                val top=19.dp.toPx();val bottom=h-10.dp.toPx()
                if(bottom<=top)return@Canvas
                val minValue=visible.minOfOrNull { values[it] } ?: 0.0
                val maxValue=visible.maxOfOrNull { values[it] } ?: 0.0
                val range=max(maxValue-minValue,1e-9)
                val upper=max(0.0,maxValue)+range*.08
                val lower=min(0.0,minValue)-range*.08
                fun yFor(v:Double)=(top+(upper-v)/(upper-lower)*(bottom-top)).toFloat()
                val zero=yFor(0.0)
                val grid=Color(0xFF4A5774).copy(alpha=.36f)
                val columns=(w/64.dp.toPx()).toInt().coerceAtLeast(2)
                val timeStep=niceTimeStep(view.visibleBars/columns)
                val om=data.origin/data.intervalMs
                val first=ceil((view.centerBar-view.visibleBars/2+om)/timeStep).toLong()
                val last=floor((view.centerBar+view.visibleBars/2+om)/timeStep).toLong()
                if(last>=first&&last-first<1000)for(t in first..last) {
                    val x=(view.fractionFor((t*timeStep-om).toDouble())*w).toFloat()
                    drawLine(grid,Offset(x,0f),Offset(x,h),1.dp.toPx())
                }
                for(y in listOf(top,zero,bottom))drawLine(grid,Offset(0f,y),Offset(w,y),1.dp.toPx())
                val spacing=w/view.visibleBars.toFloat()
                val width=(spacing*.64f).coerceAtLeast(.5f)
                clipRect(0f,top,w,bottom) {
                    for(i in visible) {
                        val value=values[i];val y=yFor(value)
                        val previous=if(i>0)values[i-1] else Double.NaN
                        val color=if(!previous.isFinite()||value>=previous)Color(0xFF80BF00) else Color(0xFFFF1717)
                        if(abs(y-zero)>.05f)drawRect(color,Offset(xFor(data.bars[i].time)-width/2,min(y,zero)),Size(width,abs(y-zero)))
                    }
                }
                val paint=NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
                    color=Color(0xFF8D98AB).toArgb();textSize=10.sp.toPx();textAlign=NativePaint.Align.RIGHT
                }
                val magnitude=max(abs(upper),abs(lower))
                val decimals=if(magnitude<1.0)ceil(-log10(magnitude.coerceAtLeast(1e-12))).toInt().plus(1).coerceIn(2,8) else 2
                fun label(v:Double)=if(v==0.0)"0" else String.format(Locale.US,"% .${decimals}f",v).trim()
                for((v,y) in listOf(upper to top,0.0 to zero,lower to bottom)) {
                    drawContext.canvas.nativeCanvas.drawText(label(v),w-5.dp.toPx(),(y-(paint.ascent()+paint.descent())/2).coerceIn(11.dp.toPx(),h-2.dp.toPx()),paint)
                }
            }
            Row(Modifier.align(Alignment.TopStart).height(27.dp).padding(start=5.dp),verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(21.dp,26.dp).clickable { expanded=!expanded },contentAlignment=Alignment.Center) {
                    Canvas(Modifier.size(9.dp,6.dp)) {
                        val p=Path().apply {
                            if(expanded) { moveTo(0f,0f);lineTo(size.width,0f);lineTo(size.width/2,size.height) }
                            else { moveTo(0f,0f);lineTo(0f,size.height);lineTo(size.width,size.height/2) }
                            close()
                        }
                        drawPath(p,Color(0xFF9AB4CB))
                    }
                }
                Text("Accelerator Oscillator $fast $slow $signal",color=Color(0xFFF2F3F6),fontSize=12.sp,maxLines=1)
                Box(Modifier.size(27.dp).clickable { settingsOpen=true },contentAlignment=Alignment.Center) {
                    Canvas(Modifier.size(13.dp)) {
                        val stroke=2.dp.toPx();val color=Color(0xFF8FAAC4)
                        drawLine(color,Offset(size.width*.18f,size.height*.78f),Offset(size.width*.75f,size.height*.21f),stroke*2,StrokeCap.Butt)
                        drawLine(color,Offset(size.width*.79f,size.height*.16f),Offset(size.width*.91f,size.height*.28f),stroke,StrokeCap.Butt)
                        drawLine(color,Offset(size.width*.05f,size.height*.95f),Offset(size.width*.19f,size.height*.80f),stroke,StrokeCap.Round)
                    }
                }
            }
            if(expanded&&(snapshot==null||values.none { it.isFinite() }))Text("بانتظار بيانات كافية للمؤشر",color=Color(0xFF8D98AB),fontSize=10.sp,modifier=Modifier.align(Alignment.Center))
        }
    }
    if(settingsOpen)AcceleratorSettingsDialog { settingsOpen=false }
}
@Composable private fun AcceleratorSettingsDialog(onDismiss:()->Unit) {
    var fast by remember { mutableStateOf(IndicatorSettings.fastPeriod.toString()) }
    var slow by remember { mutableStateOf(IndicatorSettings.slowPeriod.toString()) }
    var signal by remember { mutableStateOf(IndicatorSettings.signalPeriod.toString()) }
    val valid=listOf(fast,slow,signal).all { (it.toIntOrNull() ?: 0) in 1..500 }
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(Modifier.fillMaxWidth(.89f).background(Color(0xFF222E43),RoundedCornerShape(12.dp)).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Accelerator Oscillator",color=Color.White,fontSize=17.sp,modifier=Modifier.weight(1f))
                    Box(Modifier.size(28.dp).clickable(onClick=onDismiss),contentAlignment=Alignment.Center) { ReferenceGlyph("close",Modifier.size(20.dp),Color(0xFFA8C0D2)) }
                }
                @Composable fun PeriodRow(label:String,value:String,onChange:(String)->Unit) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(label,color=Color(0xFFC4D1E1),fontSize=14.sp,modifier=Modifier.weight(1f))
                        BasicTextField(value=value,onValueChange={ if(it.length<=3&&it.all { c->c in '0'..'9' })onChange(it) },singleLine=true,
                            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),textStyle=TextStyle(color=Color.White,fontSize=15.sp,textAlign=TextAlign.Center),
                            modifier=Modifier.width(72.dp).background(Color(0xFF171E2C),RoundedCornerShape(6.dp)).padding(10.dp))
                    }
                }
                PeriodRow("الفترة السريعة",fast) { fast=it }
                PeriodRow("الفترة البطيئة",slow) { slow=it }
                PeriodRow("فترة الإشارة",signal) { signal=it }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f).height(40.dp).background(Color(0xFF943B49),RoundedCornerShape(7.dp)).clickable { IndicatorSettings.acceleratorEnabled=false;IndicatorSettings.snapshot=null;onDismiss() },contentAlignment=Alignment.Center) { Text("إزالة المؤشر",color=Color.White,fontSize=14.sp) }
                    Box(Modifier.weight(1f).height(40.dp).background(if(valid)Color(0xFF23884D) else Color(0xFF465163),RoundedCornerShape(7.dp)).clickable(enabled=valid) {
                        IndicatorSettings.fastPeriod=fast.toInt();IndicatorSettings.slowPeriod=slow.toInt();IndicatorSettings.signalPeriod=signal.toInt();onDismiss()
                    },contentAlignment=Alignment.Center) { Text("حفظ",color=Color.White,fontSize=14.sp) }
                }
            }
        }
    }
}

private data class IndicatorInput(val label:String,val initial:Double,val min:Double=1.0,val max:Double=500.0,val integer:Boolean=true)
private data class IndicatorDefinition(val name:String,val overlay:Boolean,val inputs:List<IndicatorInput>,val lines:List<String>,val colors:List<Long>,val histogram:Boolean=false,val lower:Double?=null,val upper:Double?=null)
private data class IndicatorLineStyle(val color:Long,val width:Float=1f,val enabled:Boolean=true)
private data class IndicatorConfig(val values:List<Double>,val styles:List<IndicatorLineStyle>,val visible:Boolean=true)
private data class IndicatorSeries(val name:String,val values:DoubleArray,val shift:Int=0,val histogram:Boolean=false,val dots:Boolean=false)
private val IndicatorPalette=listOf(0xFF2962FFL,0xFFFF6D00L,0xFF00E8C6L,0xFFFFCE00L,0xFF80BF00L,0xFFFF3030L,0xFFFFFFFFL,0xFFB66CFFL)
private val ExtraIndicatorDefinitions:List<IndicatorDefinition> = run {
    fun period(label:String="الفترة",n:Int=14)=IndicatorInput(label,n.toDouble())
    fun factor(label:String,n:Double)=IndicatorInput(label,n,.01,100.0,false)
    fun def(name:String,overlay:Boolean=false,inputs:List<IndicatorInput> = listOf(period()),lines:List<String> = listOf(name),colors:List<Long> = lines.indices.map { IndicatorPalette[it%IndicatorPalette.size] },histogram:Boolean=false,lower:Double?=null,upper:Double?=null)=IndicatorDefinition(name,overlay,inputs,lines,colors,histogram,lower,upper)
    listOf(
        def("ADX",inputs=listOf(period("تسوية ADX"),period("طول DI")),lines=listOf("adx","plusDI","minusDI"),colors=listOf(0xFFEA6800L,0xFF00E8C6L,0xFFFFCE00L)),
        def("Alligator",true,listOf(period("الفترة الفكية",13),IndicatorInput("التحول الفكي",8.0,0.0),period("فترة الأسنان",8),IndicatorInput("التحول السني",5.0,0.0),period("فترة الشفاه",5),IndicatorInput("التحول الشفي",3.0,0.0)),listOf("Jaw","Teeth","Lips"),listOf(0xFF2962FFL,0xFFEA6800L,0xFF80BF00L)),
        def("Aroon",inputs=listOf(period(n=25)),lines=listOf("Aroon Up","Aroon Down"),colors=listOf(0xFF2962FFL,0xFFFF3030L),lower=0.0,upper=100.0),
        def("Average True Range"),
        def("Awesome Oscillator",inputs=listOf(period("فترة قصيرة",5),period("فترة طويلة",34)),lines=listOf("Growing","Falling"),colors=listOf(0xFF80BF00L,0xFFFF1717L),histogram=true),
        def("Bears Power",inputs=listOf(period(n=13)),histogram=true),
        def("Bollinger Bands",true,listOf(period(n=20),factor("الانحراف المعياري",2.0)),listOf("Basis","Upper","Lower")),
        def("Bollinger Bands Width",inputs=listOf(period(n=20),factor("الانحراف المعياري",2.0))),
        def("Bulls Power",inputs=listOf(period(n=13)),histogram=true),
        def("CCI",inputs=listOf(period(n=20))),
        def("Donchian Channel",true,listOf(period(n=20)),listOf("Middle","Upper","Lower")),
        def("DeMarker",lower=0.0,upper=1.0),
        def("Envelopes",true,listOf(period(n=14),factor("الانحراف %",.1)),listOf("Basis","Upper","Lower")),
        def("Fractal",true,listOf(IndicatorInput("شموع على كل جانب",2.0,1.0,20.0)),listOf("High","Low")),
        def("مؤشر إيشيموكو كينكو هيو",true,listOf(period("خط التحويل",9),period("الخط الأساسي",26),period("الفترة B",52),IndicatorInput("الإزاحة",26.0,0.0)),listOf("Tenkan","Kijun","Senkou A","Senkou B","Chikou"),listOf(0xFF2962FFL,0xFFFF3030L,0xFF00E8C6L,0xFFEA6800L,0xFFB66CFFL)),
        def("Keltner Channel",true,listOf(period("فترة EMA",20),period("فترة ATR",10),factor("المضاعف",2.0)),listOf("Basis","Upper","Lower")),
        def("MACD",inputs=listOf(period("فترة سريعة",12),period("فترة بطيئة",26),period("فترة الإشارة",9)),lines=listOf("MACD","Signal","Histogram")),
        def("Momentum",inputs=listOf(period(n=10))),
        def("Moving Average",true,listOf(period(n=14),IndicatorInput("الطريقة: 0 SMA، 1 EMA، 2 SMMA، 3 WMA",0.0,0.0,3.0)),listOf("MA")),
        def("OsMA",inputs=listOf(period("فترة سريعة",12),period("فترة بطيئة",26),period("فترة الإشارة",9)),histogram=true),
        def("Parabolic SAR",true,listOf(factor("خطوة التسارع",.02),factor("أقصى تسارع",.2)),listOf("SAR")),
        def("RSI",lower=0.0,upper=100.0),
        def("Rate of Change",inputs=listOf(period(n=12))),
        def("Stochastic Oscillator",inputs=listOf(period("فترة %K",14),period("تنعيم %K",3),period("فترة %D",3)),lines=listOf("%K","%D"),lower=0.0,upper=100.0),
        def("SuperTrend",true,listOf(period("فترة ATR",10),factor("المضاعف",3.0)),listOf("Up","Down"),listOf(0xFF00C878L,0xFFFF3030L)),
        def("مؤشر الدوامة",inputs=listOf(period(n=14)),lines=listOf("VI+","VI-")),
        def("مؤشر ويليامز %R",lower=-100.0,upper=0.0)
    )
}
private fun IndicatorDefinition.defaults()=IndicatorConfig(inputs.map { it.initial },colors.map { IndicatorLineStyle(it) })
private object ExtraIndicators {
    val active=mutableStateMapOf<String,IndicatorConfig>()
    fun definition(name:String)=ExtraIndicatorDefinitions.firstOrNull { it.name==name }
    fun enable(name:String):Boolean {
        val definition=definition(name) ?: return false
        active[name]=(active[name] ?: definition.defaults()).copy(visible=true)
        return true
    }
}
@Composable private fun IndicatorPreferences() {
    val context=LocalContext.current.applicationContext
    LaunchedEffect(context) {
        val prefs=context.getSharedPreferences("chart_indicators_v1",Context.MODE_PRIVATE)
        try {
            val root=JSONObject(prefs.getString("settings","{}") ?: "{}")
            ExtraIndicatorDefinitions.forEach { definition->
                val saved=root.optJSONObject(definition.name) ?: return@forEach
                val values=saved.optJSONArray("values")
                val styles=saved.optJSONArray("styles")
                val v=definition.inputs.mapIndexed { i,input->values?.optDouble(i,input.initial)?.takeIf { it.isFinite()&&it in input.min..input.max&&(!input.integer||it==floor(it)) } ?: input.initial }
                val s=definition.colors.mapIndexed { i,color->val item=styles?.optJSONObject(i);IndicatorLineStyle(item?.optLong("color",color) ?: color,(item?.optDouble("width",1.0) ?: 1.0).toFloat().takeIf { it.isFinite()&&it in 1f..4f } ?: 1f,item?.optBoolean("enabled",true) ?: true) }
                if(validIndicatorInputs(definition,v))ExtraIndicators.active[definition.name]=IndicatorConfig(v,s,saved.optBoolean("visible",true))
            }
        } catch(_:Exception) { }
        snapshotFlow { ExtraIndicators.active.toMap() }.collect { settings->
            val root=JSONObject()
            settings.forEach { (name,config)->
                val styles=JSONArray();config.styles.forEach { styles.put(JSONObject().put("color",it.color).put("width",it.width.toDouble()).put("enabled",it.enabled)) }
                root.put(name,JSONObject().put("values",JSONArray(config.values)).put("styles",styles).put("visible",config.visible))
            }
            prefs.edit().putString("settings",root.toString()).apply()
        }
    }
}
private fun validIndicatorInputs(d:IndicatorDefinition,v:List<Double>):Boolean {
    if(v.size!=d.inputs.size||v.indices.any { !v[it].isFinite()||v[it] !in d.inputs[it].min..d.inputs[it].max||(d.inputs[it].integer&&v[it]!=floor(v[it])) })return false
    return when(d.name) {
        "Awesome Oscillator","MACD","OsMA"->v[0]<v[1]
        "Parabolic SAR"->v[0]<=v[1]
        else->true
    }
}
private object IndicatorMath {
    fun average(x:DoubleArray,p:Int,kind:Int=0):DoubleArray {
        require(p>0)
        val out=DoubleArray(x.size) { Double.NaN };var sum=0.0;var count=0;var prev=Double.NaN
        for(i in x.indices) {
            if(!x[i].isFinite()) { sum=0.0;count=0;prev=Double.NaN;continue }
            count++;sum+=x[i]
            if(kind==0||kind==3) {
                if(count>p)sum-=x[i-p]
                if(count>=p)out[i]=if(kind==0)sum/p else {
                    var weighted=0.0;for(k in 0 until p)weighted+=x[i-p+1+k]*(k+1);weighted/(p.toDouble()*(p+1)/2)
                }
            } else {
                if(count==p)prev=sum/p
                else if(count>p)prev+=(x[i]-prev)*(if(kind==1)2.0/(p+1) else 1.0/p)
                if(count>=p)out[i]=prev
            }
        }
        return out
    }
    fun diff(a:DoubleArray,b:DoubleArray)=DoubleArray(a.size) { a[it]-b[it] }
    fun tr(b:List<Candle>)=DoubleArray(b.size) { i->
        val hi=b[i].high.toDouble();val lo=b[i].low.toDouble()
        if(i==0)hi-lo else max(hi-lo,max(abs(hi-b[i-1].close),abs(lo-b[i-1].close)))
    }
    fun extreme(x:DoubleArray,p:Int,high:Boolean):DoubleArray {
        val out=DoubleArray(x.size) { Double.NaN }
        for(i in p-1 until x.size) { var v=x[i-p+1];for(j in i-p+2..i)v=if(high)max(v,x[j]) else min(v,x[j]);out[i]=v }
        return out
    }
    fun rangePosition(x:DoubleArray,hi:DoubleArray,lo:DoubleArray,p:Int):DoubleArray {
        val h=extreme(hi,p,true);val l=extreme(lo,p,false)
        return DoubleArray(x.size) { if(!h[it].isFinite())Double.NaN else if(h[it]==l[it])50.0 else 100*(x[it]-l[it])/(h[it]-l[it]) }
    }
    fun sar(b:List<Candle>,step:Double,limit:Double):DoubleArray {
        val out=DoubleArray(b.size) { Double.NaN };if(b.size<2)return out
        var up=b[1].close>=b[0].close;var af=step
        var ep=if(up)max(b[0].high,b[1].high).toDouble() else min(b[0].low,b[1].low).toDouble()
        var value=if(up)b[0].low.toDouble() else b[0].high.toDouble();out[1]=value
        for(i in 2 until b.size) {
            value+=af*(ep-value)
            if(up) {
                value=min(value,min(b[i-1].low,b[i-2].low).toDouble())
                if(b[i].low<value) { up=false;value=ep;ep=b[i].low.toDouble();af=step }
                else if(b[i].high>ep) { ep=b[i].high.toDouble();af=min(limit,af+step) }
            } else {
                value=max(value,max(b[i-1].high,b[i-2].high).toDouble())
                if(b[i].high>value) { up=true;value=ep;ep=b[i].high.toDouble();af=step }
                else if(b[i].low<ep) { ep=b[i].low.toDouble();af=min(limit,af+step) }
            }
            out[i]=value
        }
        return out
    }
    fun calculate(d:IndicatorDefinition,cfg:IndicatorConfig,b:List<Candle>):List<IndicatorSeries> {
        val n=b.size;val v=cfg.values
        fun p(i:Int=0)=v[i].toInt()
        val close=DoubleArray(n) { b[it].close.toDouble() };val high=DoubleArray(n) { b[it].high.toDouble() };val low=DoubleArray(n) { b[it].low.toDouble() }
        val median=DoubleArray(n) { (high[it]+low[it])/2 };val typical=DoubleArray(n) { (high[it]+low[it]+close[it])/3 }
        fun line(i:Int,x:DoubleArray,shift:Int=0,histogram:Boolean=d.histogram,dots:Boolean=false)=IndicatorSeries(d.lines[i],x,shift,histogram,dots)
        fun one(x:DoubleArray)=listOf(line(0,x))
        fun atr(period:Int)=average(tr(b),period,2)
        fun mid(period:Int):DoubleArray { val hi=extreme(high,period,true);val lo=extreme(low,period,false);return DoubleArray(n) { (hi[it]+lo[it])/2 } }
        return when(d.name) {
            "ADX"->{
                val plus=DoubleArray(n) { Double.NaN };val minus=DoubleArray(n) { Double.NaN };val ranges=tr(b)
                if(n>0)ranges[0]=Double.NaN
                for(i in 1 until n) { val u=high[i]-high[i-1];val down=low[i-1]-low[i];plus[i]=if(u>down&&u>0)u else 0.0;minus[i]=if(down>u&&down>0)down else 0.0 }
                val r=average(ranges,p(1),2);val a=average(plus,p(1),2);val z=average(minus,p(1),2)
                val diP=DoubleArray(n) { if(!r[it].isFinite())Double.NaN else if(r[it]==0.0)0.0 else 100*a[it]/r[it] }
                val diM=DoubleArray(n) { if(!r[it].isFinite())Double.NaN else if(r[it]==0.0)0.0 else 100*z[it]/r[it] }
                val dx=DoubleArray(n) { val sum=diP[it]+diM[it];if(!sum.isFinite())Double.NaN else if(sum==0.0)0.0 else 100*abs(diP[it]-diM[it])/sum }
                listOf(line(0,average(dx,p(),2)),line(1,diP),line(2,diM))
            }
            "Alligator"->listOf(line(0,average(median,p(0),2),p(1)),line(1,average(median,p(2),2),p(3)),line(2,average(median,p(4),2),p(5)))
            "Aroon"->{
                val up=DoubleArray(n) { Double.NaN };val down=up.copyOf()
                for(i in p() until n) { var h=i-p();var l=h;for(j in i-p()+1..i) { if(high[j]>=high[h])h=j;if(low[j]<=low[l])l=j };up[i]=100.0*(p()-(i-h))/p();down[i]=100.0*(p()-(i-l))/p() }
                listOf(line(0,up),line(1,down))
            }
            "Average True Range"->one(atr(p()))
            "Awesome Oscillator"->one(diff(average(median,p()),average(median,p(1))))
            "Bears Power"->one(diff(low,average(close,p(),1)))
            "Bulls Power"->one(diff(high,average(close,p(),1)))
            "Bollinger Bands","Bollinger Bands Width"->{
                val mean=average(close,p());val dev=DoubleArray(n) { Double.NaN }
                for(i in p()-1 until n) { var sum=0.0;for(j in i-p()+1..i)sum+=(close[j]-mean[i]).pow(2);dev[i]=sqrt(sum/p())*v[1] }
                if(d.name=="Bollinger Bands Width")one(DoubleArray(n) { if(mean[it]==0.0)Double.NaN else 200*dev[it]/mean[it] })
                else listOf(line(0,mean),line(1,DoubleArray(n) { mean[it]+dev[it] }),line(2,DoubleArray(n) { mean[it]-dev[it] }))
            }
            "CCI"->{
                val mean=average(typical,p());val out=DoubleArray(n) { Double.NaN }
                for(i in p()-1 until n) { var dev=0.0;for(j in i-p()+1..i)dev+=abs(typical[j]-mean[i]);dev/=p();out[i]=if(dev==0.0)0.0 else (typical[i]-mean[i])/(.015*dev) };one(out)
            }
            "Donchian Channel"->{ val hi=extreme(high,p(),true);val lo=extreme(low,p(),false);listOf(line(0,DoubleArray(n) { (hi[it]+lo[it])/2 }),line(1,hi),line(2,lo)) }
            "DeMarker"->{
                val up=DoubleArray(n) { if(it==0)Double.NaN else max(0.0,high[it]-high[it-1]) };val down=DoubleArray(n) { if(it==0)Double.NaN else max(0.0,low[it-1]-low[it]) }
                val a=average(up,p());val z=average(down,p());one(DoubleArray(n) { val sum=a[it]+z[it];if(sum==0.0).5 else a[it]/sum })
            }
            "Envelopes"->{ val mean=average(close,p());listOf(line(0,mean),line(1,DoubleArray(n) { mean[it]*(1+v[1]/100) }),line(2,DoubleArray(n) { mean[it]*(1-v[1]/100) })) }
            "Fractal"->{
                val hi=DoubleArray(n) { Double.NaN };val lo=hi.copyOf();val radius=p()
                for(i in radius until n-radius-1) {
                    var up=true;var down=true
                    for(j in i-radius..i+radius)if(j!=i) { if(high[j]>=high[i])up=false;if(low[j]<=low[i])down=false }
                    if(up)hi[i]=high[i];if(down)lo[i]=low[i]
                }
                listOf(line(0,hi,dots=true),line(1,lo,dots=true))
            }
            "مؤشر إيشيموكو كينكو هيو"->{ val a=mid(p());val z=mid(p(1));listOf(line(0,a),line(1,z),line(2,DoubleArray(n) { (a[it]+z[it])/2 },p(3)),line(3,mid(p(2)),p(3)),line(4,close,-p(3))) }
            "Keltner Channel"->{ val mean=average(close,p(),1);val range=atr(p(1));listOf(line(0,mean),line(1,DoubleArray(n) { mean[it]+range[it]*v[2] }),line(2,DoubleArray(n) { mean[it]-range[it]*v[2] })) }
            "MACD","OsMA"->{
                val macd=diff(average(close,p(),1),average(close,p(1),1));val signal=average(macd,p(2),1);val hist=diff(macd,signal)
                if(d.name=="OsMA")one(hist) else listOf(line(0,macd),line(1,signal),line(2,hist,histogram=true))
            }
            "Momentum","Rate of Change"->one(DoubleArray(n) { if(it<p()||close[it-p()]==0.0)Double.NaN else if(d.name=="Momentum")100*close[it]/close[it-p()] else 100*(close[it]/close[it-p()]-1) })
            "Moving Average"->one(average(close,p(),p(1)))
            "Parabolic SAR"->listOf(line(0,sar(b,v[0],v[1]),dots=true))
            "RSI"->{
                val up=DoubleArray(n) { if(it==0)Double.NaN else max(0.0,close[it]-close[it-1]) };val down=DoubleArray(n) { if(it==0)Double.NaN else max(0.0,close[it-1]-close[it]) }
                val gain=average(up,p(),2);val loss=average(down,p(),2)
                one(DoubleArray(n) { if(!gain[it].isFinite())Double.NaN else if(loss[it]==0.0) { if(gain[it]==0.0)50.0 else 100.0 } else 100-100/(1+gain[it]/loss[it]) })
            }
            "Stochastic Oscillator"->{ val k=average(rangePosition(close,high,low,p()),p(1));listOf(line(0,k),line(1,average(k,p(2)))) }
            "SuperTrend"->{
                val range=atr(p());val up=DoubleArray(n) { Double.NaN };val down=up.copyOf()
                var upper=Double.NaN;var lower=Double.NaN;var bullish=false;var started=false
                for(i in b.indices) {
                    if(!range[i].isFinite())continue
                    val bu=median[i]+v[1]*range[i];val bl=median[i]-v[1]*range[i]
                    if(!started) { upper=bu;lower=bl;started=true;bullish=false }
                    else { val oldUpper=upper;val oldLower=lower
                        upper=if(bu<oldUpper||close[i-1]>oldUpper)bu else oldUpper
                        lower=if(bl>oldLower||close[i-1]<oldLower)bl else oldLower
                        bullish=if(bullish)close[i]>=lower else close[i]>upper
                    }
                    if(bullish)up[i]=lower else down[i]=upper
                }
                listOf(line(0,up),line(1,down))
            }
            "مؤشر الدوامة"->{
                val plus=DoubleArray(n) { if(it==0)Double.NaN else abs(high[it]-low[it-1]) };val minus=DoubleArray(n) { if(it==0)Double.NaN else abs(low[it]-high[it-1]) }
                val r=tr(b);if(n>0)r[0]=Double.NaN
                val a=average(plus,p());val z=average(minus,p());val tr=average(r,p())
                listOf(line(0,DoubleArray(n) { if(tr[it]==0.0)0.0 else a[it]/tr[it] }),line(1,DoubleArray(n) { if(tr[it]==0.0)0.0 else z[it]/tr[it] }))
            }
            "مؤشر ويليامز %R"->{ val k=rangePosition(close,high,low,p());one(DoubleArray(n) { k[it]-100 }) }
            else->emptyList()
        }
    }
}

@Composable private fun ExtraIndicatorPanes() {
    val panes=ExtraIndicators.active.toMap().filter { (name,cfg)->cfg.visible&&ExtraIndicators.definition(name)?.overlay==false }
    if(!IndicatorSettings.acceleratorEnabled&&panes.isEmpty())return
    Column(Modifier.fillMaxWidth().heightIn(max=(LocalConfiguration.current.screenHeightDp*.28f).dp).verticalScroll(rememberScrollState())) {
        if(IndicatorSettings.acceleratorEnabled)AcceleratorOscillatorPane()
        panes.forEach { (name,cfg)->key(name) { ExtraIndicatorPane(ExtraIndicators.definition(name)!!,cfg) } }
    }
}
@Composable private fun ExtraIndicatorHeading(d:IndicatorDefinition,cfg:IndicatorConfig,expanded:Boolean,onExpand:()->Unit,onSettings:()->Unit) {
    Row(Modifier.height(27.dp).padding(start=6.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(if(expanded)"▾" else "▸",color=Color(0xFF9AB4CB),modifier=Modifier.width(20.dp).clickable(onClick=onExpand),fontSize=15.sp)
        val periods=d.inputs.indices.filter { d.inputs[it].integer&&!(d.name=="Alligator"&&it%2==1)&&!(d.name=="Moving Average"&&it==1) }.joinToString(" ") { cfg.values[it].toInt().toString() }
        Text("${d.name} $periods",color=Color(0xFFF2F3F6),fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f,false))
        Box(Modifier.size(27.dp).clickable(onClick=onSettings),contentAlignment=Alignment.Center) { Text("✎",color=Color(0xFF9AB4CB),fontSize=19.sp) }
    }
}
@Composable private fun ExtraIndicatorPane(d:IndicatorDefinition,cfg:IndicatorConfig) {
    val symbol by TradeStore.currentSymbol.collectAsState()
    val snapshot=IndicatorSettings.snapshot?.takeIf { it.symbol==symbol&&it.frame==ChartSettings.frame }
    val series=remember(d,cfg.values,snapshot?.bars) { IndicatorMath.calculate(d,cfg,snapshot?.bars ?: emptyList()) }
    var expanded by remember { mutableStateOf(true) };var settings by remember { mutableStateOf(false) }
    val height=(LocalConfiguration.current.screenHeightDp*.145f).coerceIn(88f,132f).dp
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(Modifier.fillMaxWidth().height(if(expanded)height else 28.dp).background(Color(0xFF1D2332))) {
            Canvas(Modifier.fillMaxSize()) {
                drawLine(Color(0xFF3C6686),Offset.Zero,Offset(size.width,0f),1.dp.toPx())
                if(expanded&&snapshot!=null)drawIndicatorPane(d,cfg,series,snapshot)
            }
            ExtraIndicatorHeading(d,cfg,expanded,{ expanded=!expanded },{ settings=true })
            if(expanded&&(snapshot==null||series.all { s->s.values.none { it.isFinite() } }))Text("بانتظار بيانات كافية للمؤشر",color=Color(0xFF8D98AB),fontSize=10.sp,modifier=Modifier.align(Alignment.Center))
        }
    }
    if(settings)ExtraIndicatorDialog(d) { settings=false }
}
private fun DrawScope.drawIndicatorPane(d:IndicatorDefinition,cfg:IndicatorConfig,series:List<IndicatorSeries>,data:IndicatorChartSnapshot) {
    val w=size.width;val h=size.height;val top=28.dp.toPx();val bottom=h-18.dp.toPx();if(bottom<=top)return
    fun xFor(index:Int,shift:Int=0):Float { val time=data.bars.getOrNull(index)?.time ?: return Float.NaN;return (data.viewport.fractionFor((time-data.origin).toDouble()/data.intervalMs+shift)*w).toFloat() }
    val finite=series.flatMapIndexed { index,s->if(!(if(d.name=="Awesome Oscillator")cfg.styles.any { it.enabled } else cfg.styles.getOrNull(index)?.enabled==true))emptyList() else s.values.indices.filter { s.values[it].isFinite()&&xFor(it,s.shift) in -20f..w+20f }.map { s.values[it] } }
    var low=d.lower ?: finite.minOrNull() ?: 0.0;var high=d.upper ?: finite.maxOrNull() ?: 1.0
    if(series.any { it.histogram }) { low=min(low,0.0);high=max(high,0.0) }
    if(d.lower==null&&d.upper==null) { val margin=max(high-low,max(abs(high)*.01,1e-9))*.08;low-=margin;high+=margin }
    if(high<=low)high=low+1.0
    fun yFor(v:Double)=(top+(high-v)/(high-low)*(bottom-top)).toFloat()
    val grid=Color(0xFF4A5774).copy(alpha=.4f)
    val timeStep=niceTimeStep(data.viewport.visibleBars/(w/64.dp.toPx()).toInt().coerceAtLeast(2));val om=data.origin/data.intervalMs
    val first=ceil((data.viewport.centerBar-data.viewport.visibleBars/2+om)/timeStep).toLong();val last=floor((data.viewport.centerBar+data.viewport.visibleBars/2+om)/timeStep).toLong()
    val timePaint=NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { color=Color(0xFF8D98AB).toArgb();textSize=9.sp.toPx();textAlign=NativePaint.Align.CENTER }
    if(last>=first&&last-first<1000)for(t in first..last) { val x=(data.viewport.fractionFor((t*timeStep-om).toDouble())*w).toFloat();drawLine(grid,Offset(x,0f),Offset(x,h),1.dp.toPx());if(x>20.dp.toPx()&&x<w-25.dp.toPx())drawContext.canvas.nativeCanvas.drawText(candleTimeText(t*timeStep*data.intervalMs,data.intervalMs),x,h-3.dp.toPx(),timePaint) }
    val levels=if(d.name=="Aroon")listOf(0.0,30.0,50.0,70.0,100.0) else listOf(low,(low+high)/2,high)
    val paint=NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { color=Color(0xFF8D98AB).toArgb();textSize=9.sp.toPx();textAlign=NativePaint.Align.RIGHT }
    val magnitude=max(abs(low),abs(high));val decimals=if(magnitude<1)ceil(-log10(magnitude.coerceAtLeast(1e-12))).toInt().plus(1).coerceIn(2,8) else 2
    levels.forEach { value->val y=yFor(value);drawLine(grid,Offset(0f,y),Offset(w,y),1.dp.toPx()) }
    clipRect(0f,top,w,bottom) {
        series.forEachIndexed { index,s->
            val style=cfg.styles.getOrNull(index) ?: return@forEachIndexed
            if(!style.enabled&&d.name!="Awesome Oscillator")return@forEachIndexed
            val color=Color(style.color);val path=Path();var started=false
            for(i in s.values.indices) {
                val value=s.values[i];val x=xFor(i,s.shift)
                if(!value.isFinite()) { started=false;continue }
                val y=yFor(value)
                if(s.histogram) {
                    var barStyle=style
                    if(d.name=="Awesome Oscillator")barStyle=cfg.styles[if(i==0||!s.values[i-1].isFinite()||value>=s.values[i-1])0 else 1]
                    if(!barStyle.enabled||x !in -20f..w+20f)continue
                    val zero=yFor(0.0);val width=(w/data.viewport.visibleBars.toFloat()*(.55f+.1f*barStyle.width).coerceAtMost(.95f)).coerceAtLeast(.5f)
                    drawRect(Color(barStyle.color),Offset(x-width/2,min(y,zero)),Size(width,abs(y-zero)))
                } else if(!started) { path.moveTo(x,y);started=true } else path.lineTo(x,y)
            }
            if(!s.histogram)drawPath(path,color,style=Stroke(style.width.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
        }
    }
    levels.forEach { value->val text=if(value==0.0)"0" else String.format(Locale.US,"%.${decimals}f",value);val y=yFor(value);drawContext.canvas.nativeCanvas.drawText(text,w-4.dp.toPx(),(y-(paint.ascent()+paint.descent())/2).coerceIn(top,bottom),paint) }
}
@Composable private fun ExtraIndicatorOverlayHeadings(modifier:Modifier=Modifier) {
    var editing by remember { mutableStateOf<String?>(null) }
    val overlays=ExtraIndicators.active.toMap().filter { (name,cfg)->cfg.visible&&ExtraIndicators.definition(name)?.overlay==true }
    if(overlays.isNotEmpty())Column(modifier.widthIn(max=330.dp).heightIn(max=108.dp).verticalScroll(rememberScrollState()).background(Color(0xFF1D2332).copy(alpha=.35f))) {
        overlays.forEach { (name,cfg)->key(name) { val d=ExtraIndicators.definition(name)!!;ExtraIndicatorHeading(d,cfg,true,{ editing=name },{ editing=name }) } }
    }
    editing?.let { name->ExtraIndicators.definition(name)?.let { d->ExtraIndicatorDialog(d) { editing=null } } }
}
private fun DrawScope.drawIndicatorOverlays(bars:List<Candle>,view:ChartViewport,origin:Long,intervalMs:Long,overlays:List<Pair<IndicatorConfig,List<IndicatorSeries>>>) {
    fun xFor(index:Int,shift:Int):Float { val time=bars.getOrNull(index)?.time ?: return Float.NaN;return (view.fractionFor((time-origin).toDouble()/intervalMs+shift)*size.width).toFloat() }
    fun yFor(value:Double)=((view.maxPrice-value)/view.priceSpan*size.height).toFloat()
    clipRect(0f,0f,size.width,size.height) {
        overlays.forEach { (cfg,series)->
            if(series.size==5&&series[2].name=="Senkou A"&&cfg.styles[2].enabled&&cfg.styles[3].enabled) {
                val a=series[2];val b=series[3]
                for(i in 1 until bars.size)if(a.values[i-1].isFinite()&&a.values[i].isFinite()&&b.values[i-1].isFinite()&&b.values[i].isFinite()) {
                    val x0=xFor(i-1,a.shift);val x1=xFor(i,a.shift)
                    if(x1<0||x0>size.width)continue
                    val area=Path().apply { moveTo(x0,yFor(a.values[i-1]));lineTo(x1,yFor(a.values[i]));lineTo(x1,yFor(b.values[i]));lineTo(x0,yFor(b.values[i-1]));close() }
                    drawPath(area,Color(if(a.values[i]>=b.values[i])cfg.styles[2].color else cfg.styles[3].color).copy(alpha=.12f))
                }
            }
            series.forEachIndexed { line,s->
                val style=cfg.styles.getOrNull(line) ?: return@forEachIndexed
                if(!style.enabled)return@forEachIndexed
                val color=Color(style.color);val path=Path();var started=false
                for(i in s.values.indices) {
                    val v=s.values[i];if(!v.isFinite()) { started=false;continue }
                    val x=xFor(i,s.shift);val y=yFor(v)
                    if(s.dots) { if(x in -10f..size.width+10f)drawCircle(color,(style.width+1).dp.toPx(),Offset(x,y)) }
                    else if(!started) { path.moveTo(x,y);started=true } else path.lineTo(x,y)
                }
                if(!s.dots)drawPath(path,color,style=Stroke(style.width.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
            }
        }
    }
}

@Composable private fun ExtraIndicatorDialog(d:IndicatorDefinition,onDismiss:()->Unit) {
    val current=ExtraIndicators.active[d.name] ?: return
    val values=remember(d.name) { mutableStateListOf<String>().apply { addAll(current.values.map { if(it==floor(it))it.toInt().toString() else it.toString() }) } }
    val styles=remember(d.name) { mutableStateListOf<IndicatorLineStyle>().apply { addAll(current.styles) } }
    var tab by remember { mutableIntStateOf(1) };var paletteFor by remember { mutableIntStateOf(-1) }
    val parsed=values.map { it.toDoubleOrNull() ?: Double.NaN };val valid=validIndicatorInputs(d,parsed)
    val border=Color(0xFF424C60);val panel=Color(0xFF202839);val blue=Color(0xFF299EE6)
    val config=LocalConfiguration.current
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(Modifier.fillMaxWidth(.89f).heightIn(max=(config.screenHeightDp*.85f).dp).background(panel,RoundedCornerShape(12.dp)).border(1.dp,border,RoundedCornerShape(12.dp)).padding(20.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(d.name,color=Color.White,fontSize=18.sp,modifier=Modifier.weight(1f),maxLines=2)
                    Box(Modifier.size(30.dp).clickable(onClick=onDismiss),contentAlignment=Alignment.Center) { ReferenceGlyph("close",Modifier.size(24.dp),Color(0xFFA8C0D2)) }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    listOf("أساليب","المدخلات").forEachIndexed { index,label->
                        val shape=RoundedCornerShape(12.dp)
                        Box(Modifier.width(98.dp).height(34.dp).background(if(tab==index)Color(0xFF30455E) else Color(0xFF181F30),shape).border(1.dp,if(tab==index)blue else border,shape).clickable { tab=index;paletteFor=-1 },contentAlignment=Alignment.Center) { Text(label,color=Color.White,fontSize=14.sp) }
                    }
                }
                Spacer(Modifier.height(22.dp))
                Column(Modifier.fillMaxWidth().weight(1f,false).heightIn(min=120.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    if(tab==1)d.inputs.forEachIndexed { index,input->
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            Text(input.label,color=Color.White,fontSize=14.sp,style=TextStyle(textDirection=TextDirection.Rtl),modifier=Modifier.weight(1f).padding(end=12.dp))
                            BasicTextField(value=values[index],onValueChange={ text->
                                val normalized=buildString { text.forEach { c->append(when(c) { in '٠'..'٩'->'0'+(c-'٠');in '۰'..'۹'->'0'+(c-'۰');'٫',','->'.';else->c }) } }
                                if(normalized.length<=9&&normalized.all { it in '0'..'9'||(!input.integer&&it=='.') }&&normalized.count { it=='.' }<=1)values[index]=normalized
                            },singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=if(input.integer)KeyboardType.Number else KeyboardType.Decimal),textStyle=TextStyle(color=Color(0xFFB5C5D9),fontSize=15.sp),modifier=Modifier.width(112.dp).background(Color(0xFF0F1D32),RoundedCornerShape(5.dp)).border(1.dp,border,RoundedCornerShape(5.dp)).padding(12.dp))
                        }
                    } else styles.forEachIndexed { index,style->
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            Box(Modifier.size(25.dp).background(if(style.enabled)Color(0xFF626D7D) else Color(0xFF182133),RoundedCornerShape(3.dp)).clickable { styles[index]=style.copy(enabled=!style.enabled) },contentAlignment=Alignment.Center) { if(style.enabled)Text("✓",color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold) }
                            Text(d.lines[index],color=Color.White,fontSize=15.sp,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f).padding(horizontal=8.dp))
                            Row(Modifier.height(38.dp).background(Color(0xFF161D29),RoundedCornerShape(4.dp)).border(1.dp,border,RoundedCornerShape(4.dp)).padding(4.dp),verticalAlignment=Alignment.CenterVertically) {
                                Box(Modifier.size(29.dp).background(Color(style.color),RoundedCornerShape(3.dp)).clickable { paletteFor=if(paletteFor==index)-1 else index })
                                Box(Modifier.width(48.dp).fillMaxHeight().clickable { styles[index]=style.copy(width=if(style.width>=4f)1f else style.width+1f) },contentAlignment=Alignment.Center) { Text("${style.width.toInt()}px",color=Color.White,fontSize=14.sp) }
                            }
                        }
                        if(paletteFor==index)Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { IndicatorPalette.forEach { color->Box(Modifier.size(25.dp).background(Color(color),RoundedCornerShape(3.dp)).border(if(style.color==color)2.dp else 0.dp,Color.White,RoundedCornerShape(3.dp)).clickable { styles[index]=styles[index].copy(color=color);paletteFor=-1 }) } }
                    }
                    if(tab==1&&!valid)Text("تحقق من القيم: فترات صحيحة، والفترة القصيرة أقل من الطويلة؛ خطوة SAR لا تتجاوز الحد الأقصى.",color=Color(0xFFFF9393),fontSize=11.sp)
                }
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                    @Composable fun Action(text:String,enabled:Boolean=true,onClick:()->Unit) {
                        Box(Modifier.width(if(text=="حفظ")67.dp else 48.dp).height(40.dp).background(if(text=="حفظ")Color(0xFF102E32) else Color(0xFF212338),RoundedCornerShape(4.dp)).border(1.dp,if(text=="حفظ")Color(0xFF075D50) else border.copy(alpha=.35f),RoundedCornerShape(4.dp)).alpha(if(enabled)1f else .45f).clickable(enabled=enabled,onClick=onClick),contentAlignment=Alignment.Center) { Text(text,color=Color.White,fontSize=16.sp) }
                    }
                    Action("حذف") { ExtraIndicators.active.remove(d.name);onDismiss() }
                    Action(if(current.visible)"◉" else "○") { ExtraIndicators.active[d.name]=(ExtraIndicators.active[d.name] ?: current).copy(visible=!current.visible) }
                    Action("حفظ",valid) { ExtraIndicators.active[d.name]=IndicatorConfig(parsed,styles.toList(),(ExtraIndicators.active[d.name] ?: current).visible);onDismiss() }
                }
            }
        }
    }
}


@Composable fun LiveCandlestickChart(modifier:Modifier=Modifier,onOpenPairPicker:()->Unit) {
    ChartDrawingPreferences()
    val sym by TradeStore.currentSymbol.collectAsState()
    val frame=ChartSettings.frame
    val account=AccountStore.selected
    val intervalMs=chartFrameMillis(frame)
    val chartType=ChartSettings.chartType
    val autoScrollEnabled by rememberUpdatedState(ChartSettings.autoScrollEnabled)
    fun barPosition(time:Long,origin:Long)=(time-origin).toDouble()/intervalMs
    val candles=remember { mutableStateListOf<Candle>() }
    var livePrice by remember { mutableDoubleStateOf(0.0) }
    var displayCandle by remember(sym,frame,account) { mutableStateOf<Candle?>(null) }
    var origin by remember { mutableLongStateOf(0L) }
    var viewport by remember { mutableStateOf(ChartViewport(60.0,0.0,LIVE_CANDLE_FRACTION,1.0,MIN_PRICE_SPAN)) }
    var bsTarget by remember { mutableDoubleStateOf(60.0) }
    var followLive by remember { mutableStateOf(true) }
    var interacting by remember { mutableStateOf(false) }
    var chartSize by remember { mutableStateOf(IntSize.Zero) }
    var secondsLeft by remember { mutableIntStateOf(60) }
    var clockText by remember { mutableStateOf(clockNow()) }
    var clockHeightPx by remember { mutableIntStateOf(0) }
    var tradeCardsVisible by remember(AccountStore.selected) { mutableStateOf(true) }
    val hasOpenTrades=TradeStore.openTrades.any { it.account==AccountStore.selected }
    var nowMs by remember { mutableLongStateOf(FeedClock.now()) }
    var inertia by remember { mutableDoubleStateOf(0.0) }
    var isLoading by remember(sym,frame,account) { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var retryKey by remember { mutableIntStateOf(0) }
    var everLoaded by remember { mutableStateOf(false) }
    val resultHits=remember { mutableListOf<Pair<Long,Rect>>() }
    val density=LocalDensity.current
    val axisTextPx=with(density) { 9.sp.toPx() }; val tagTextPx=with(density) { 10.sp.toPx() }
    val clockLineHeight=if(clockHeightPx>0)clockHeightPx.toFloat() else with(density) { 16.sp.toPx() }
    val candleCeilingY by rememberUpdatedState(with(density) { 60.dp.toPx()+clockLineHeight+5.dp.toPx() })
    val ceilingHeadroom by rememberUpdatedState(with(density) { 10.dp.toPx() })
    LaunchedEffect(hasOpenTrades) { if(!hasOpenTrades)tradeCardsVisible=true }
    val axisPaint=remember(axisTextPx) { NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { color=Color.White.toArgb();textSize=axisTextPx;textAlign=NativePaint.Align.RIGHT;typeface=Typeface.DEFAULT } }
    val timePaint=remember(axisTextPx) { NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { color=C.TextSecondary.toArgb();textSize=axisTextPx;textAlign=NativePaint.Align.CENTER } }
    val tagPaint=remember(tagTextPx) { NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { color=C.BG.toArgb();textSize=tagTextPx;textAlign=NativePaint.Align.CENTER;typeface=Typeface.DEFAULT_BOLD } }
    val detailPaint=remember(axisTextPx) { NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { color=C.Cyan.toArgb();textSize=axisTextPx;textAlign=NativePaint.Align.CENTER } }
    val pricePulseTransition=rememberInfiniteTransition(label="live-price-pulse")
    val pricePulse by pricePulseTransition.animateFloat(
        initialValue=0f,targetValue=1f,
        animationSpec=infiniteRepeatable(tween(1100,easing=LinearEasing),repeatMode=RepeatMode.Restart),
        label="live-price-halo")
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
    }
    fun panOnly(dx:Float) {
        val width=chartSize.width.toDouble()
        if(width<=1||!dx.isFinite()||dx==0f||candles.isEmpty())return
        val view=viewport; val desired=view.centerBar-dx*PAN_GAIN/width*view.visibleBars
        val first=barPosition(candles.first().time,origin); val last=barPosition(candles.last().time,origin)
        val center=boundedChartCenter(view,desired,first,last)
        val next=view.copy(anchorBar=center,anchorFraction=.5)
        viewport=next
        followLive=next.atLiveBoundary(last)
    }
    val onBegin by rememberUpdatedState<()->Unit>({interacting=true;inertia=0.0})
    val onEnd by rememberUpdatedState<()->Unit>({interacting=false})
    val onPinchStart by rememberUpdatedState<()->Unit>({beginPinch()})
    val onZoom by rememberUpdatedState<(Double)->Unit>({zoomOnly(it)})
    val onPan by rememberUpdatedState<(Float)->Unit>({ dx->panOnly(dx) })
    val onFling by rememberUpdatedState<(Float)->Unit>({ velocity->
        if(chartSize.width>1&&candles.isNotEmpty()&&velocity.isFinite()) {
            inertia=-velocity.coerceIn(-12000f,12000f)*PAN_GAIN/chartSize.width*viewport.visibleBars
        }
    })

    val drawingObjects=ChartDrawings.visible(sym,frame)
    val onDrawingHit by rememberUpdatedState<(Offset)->DrawingHit?>({ position->
        if(isLoading||loadError||chartSize.width<=1||chartSize.height<=1)null
        else drawingHit(position,DrawingProjection(clockSafeViewport(viewport,candles,origin,intervalMs,chartSize.height.toFloat(),candleCeilingY,ceilingHeadroom),origin,intervalMs,chartSize.width.toFloat(),chartSize.height.toFloat()),ChartDrawings.visible(sym,frame),18*density.density)
    })
    val onDrawingDragStart by rememberUpdatedState<(DrawingHit?)->DrawingDrag?>({ hit->
        val d=hit?.id?.let { ChartDrawings.objects[it] }
        if(d!=null)ChartDrawings.selected=d.id
        if(d==null||d.locked)null else DrawingDrag(d,hit!!.handle,DrawingProjection(clockSafeViewport(viewport,candles,origin,intervalMs,chartSize.height.toFloat(),candleCeilingY,ceilingHeadroom),origin,intervalMs,chartSize.width.toFloat(),chartSize.height.toFloat()))
    })
    val onDrawingMove by rememberUpdatedState<(DrawingDrag,Offset)->Unit>({ drag,delta->
        if(ChartDrawings.objects.containsKey(drag.original.id))ChartDrawings.objects[drag.original.id]=drag.moved(delta,candles)
    })
    val onDrawingRestore by rememberUpdatedState<(DrawingDrag)->Unit>({ drag->
        if(ChartDrawings.objects.containsKey(drag.original.id))ChartDrawings.objects[drag.original.id]=drag.original
    })
    val onChartTap by rememberUpdatedState<(Offset,DrawingHit?,Boolean)->Unit>({ position,hit,edit->
        val result=resultHits.asReversed().firstOrNull { it.second.contains(position) }
        if(result!=null)TradeStore.results.removeAll { it.trade.id==result.first }
        else if(hit!=null&&ChartDrawings.objects.containsKey(hit.id)) {
            ChartDrawings.selected=hit.id
            if(edit)ChartDrawings.editing=hit.id
        } else ChartDrawings.selected=null
    })
    val drawingRequest=ChartDrawings.pending
    LaunchedEffect(drawingRequest?.id,sym,frame,isLoading,loadError,chartSize) {
        val request=ChartDrawings.pending ?: return@LaunchedEffect
        if(request.symbol!=sym) { ChartDrawings.pending=null;return@LaunchedEffect }
        if(loadError) {
            ChartDrawings.pending=null;TradeMessage.show("تعذر إضافة الرسم حتى تحميل بيانات الشارت");return@LaunchedEffect
        }
        if(!isLoading&&candles.isNotEmpty()&&chartSize.width>1&&chartSize.height>1) {
            val d=createChartDrawing(request,DrawingProjection(clockSafeViewport(viewport,candles,origin,intervalMs,chartSize.height.toFloat(),candleCeilingY,ceilingHeadroom),origin,intervalMs,chartSize.width.toFloat(),chartSize.height.toFloat()))
            ChartDrawings.objects[d.id]=d;ChartDrawings.selected=d.id;ChartDrawings.pending=null
        }
    }
    LaunchedEffect(sym,frame) { ChartDrawings.selected=null;ChartDrawings.editing=null }

    LaunchedEffect(sym,frame,account,retryKey) {
        var last=withFrameNanos { it }
        while(isActive) {
            val ns=withFrameNanos { it };val dt=((ns-last)/1_000_000_000.0).coerceIn(0.0,.05);last=ns
            val view=viewport;val diff=bsTarget-view.visibleBars
            val blend=1.0-(1.0-ZOOM_SMOOTH).pow(dt*60.0)
            if(abs(diff)>.003)viewport=view.copy(visibleBars=view.visibleBars+diff*blend) else if(view.visibleBars!=bsTarget)viewport=view.copy(visibleBars=bsTarget)
            if(candles.isNotEmpty()) {
                val vp=viewport;val first=barPosition(candles.first().time,origin);val live=barPosition(candles.last().time,origin)
                val center=boundedChartCenter(vp,vp.centerBar,first,live)
                if(abs(center-vp.centerBar)>1e-9) {
                    viewport=vp.copy(anchorBar=center,anchorFraction=.5);inertia=0.0
                    followLive=viewport.atLiveBoundary(live)
                }
            }
            if(!interacting&&candles.isNotEmpty()&&abs(inertia)>0.0) {
                val vp=viewport;val decay=exp(-INERTIA_FRICTION*dt)
                val desired=vp.centerBar+inertia*(1.0-decay)/INERTIA_FRICTION
                val first=barPosition(candles.first().time,origin);val live=barPosition(candles.last().time,origin)
                val center=boundedChartCenter(vp,desired,first,live)
                val next=vp.copy(anchorBar=center,anchorFraction=.5)
                viewport=next
                inertia=if(abs(center-desired)>1e-9)0.0 else inertia*decay
                if(abs(inertia)*chartSize.width/vp.visibleBars<5.0)inertia=0.0
                followLive=next.atLiveBoundary(live)
            }
            if(!isLoading&&candles.isNotEmpty()) {
                val target=candles.last()
                displayCandle=referenceDisplayCandle(displayCandle,target,
                    candles.getOrNull(candles.lastIndex-1)?.close,dt)
                var vp=viewport
                if(autoScrollEnabled&&followLive&&!interacting&&abs(inertia)<.000001) {
                    val live=barPosition(target.time,origin)
                    val desired=live+(.5-LIVE_CANDLE_FRACTION)*vp.visibleBars
                    val center=vp.centerBar+(desired-vp.centerBar)*referenceBlend(.16,dt)
                    vp=vp.copy(anchorBar=if(abs(desired-center)<.004)desired else center,anchorFraction=.5)
                }
                val visible=candles.toMutableList()
                displayCandle?.takeIf { it.time==target.time }?.let { visible[visible.lastIndex]=it }
                val(pc,span)=verticalTarget(vp,visible,origin,intervalMs)
                val ease=referenceBlend(.12,dt)
                val smoothed=vp.copy(centerPrice=vp.centerPrice+(pc-vp.centerPrice)*ease,
                    priceSpan=(vp.priceSpan+(span-vp.priceSpan)*ease).coerceAtLeast(MIN_PRICE_SPAN))
                // Use the unsmoothed highs too, so a fresh tick cannot cross the clock boundary.
                viewport=clockSafeViewport(smoothed,candles,origin,intervalMs,chartSize.height.toFloat(),candleCeilingY,ceilingHeadroom)
            }
        }
    }
    LaunchedEffect(ChartSettings.autoScrollEnabled) {
        if(ChartSettings.autoScrollEnabled&&candles.isNotEmpty()) {
            followLive=true;inertia=0.0
            val v=viewport.copy(anchorBar=barPosition(candles.last().time,origin),anchorFraction=LIVE_CANDLE_FRACTION)
            val(center,span)=verticalTarget(v,candles,origin,intervalMs)
            viewport=v.copy(centerPrice=center,priceSpan=span)
        }
    }
    LaunchedEffect(chartType) {
        if(candles.isNotEmpty()) {
            val(center,span)=verticalTarget(viewport,candles,origin,intervalMs)
            viewport=viewport.copy(centerPrice=center,priceSpan=span)
        }
    }
    LaunchedEffect(sym,frame,account,retryKey) {
        val started=System.currentTimeMillis();val initial=!everLoaded
        isLoading=true;loadError=false;candles.clear();displayCandle=null;livePrice=0.0;TradeStore.livePrice.value=0f;followLive=true;inertia=0.0
        if(!SymCatalog.isForex(sym)) { isLoading=false;loadError=true;return@LaunchedEffect }
        val cursor=ChartTickCursor()
        fun applyLiveQuote(quote:Quote) {
            if(FeedClock.now()-quote.sourceTime !in -5000L..15000L||quote.sourceTime<cursor.time)return
            TradeStore.quote(sym,quote.price,quote.sourceTime)
            updateCandleFromTick(candles,quote.price,quote.sourceTime,intervalMs,cursor)
            livePrice=quote.price
            // Render the accepted tick immediately, not a historical bar close.
            displayCandle=candles.lastOrNull()
        }
        var success=false
        repeat(10) {
            if(!success) {
                val bars=BiquoteApi.chartHistory(sym,frame)
                currentCoroutineContext().ensureActive()
                val bucket=FeedClock.now()/intervalMs*intervalMs
                val history=bars.filter { it.time<=bucket }.map { it.copy(isClosed=it.time<bucket) }
                if(history.isNotEmpty())mergeReferenceBars(candles,history,bulk=true)
                val quote=BiquoteApi.liveQuote(sym)
                currentCoroutineContext().ensureActive()
                if(quote!=null)applyLiveQuote(quote)
                while(candles.size>1500)candles.removeAt(0)
                if(candles.isNotEmpty()) {
                    origin=candles.first().time
                    viewport=initialViewport(candles,origin,intervalMs,clampVisible((ppw()/(8f*density.density)).toDouble()));bsTarget=viewport.visibleBars
                    // History is only the initial backdrop; it never becomes a live quote.
                    success=true
                } else delay(1500)
            }
        }
        val minimumLoading=if(initial)2150L else 450L
        delay((minimumLoading-(System.currentTimeMillis()-started)).coerceAtLeast(0L))
        isLoading=false
        if(!success) { loadError=true;return@LaunchedEffect }
        everLoaded=true
        while(isActive) {
            val quote=BiquoteApi.liveQuote(sym)
            currentCoroutineContext().ensureActive()
            if(quote!=null)applyLiveQuote(quote)
            while(candles.size>1500)candles.removeAt(0)
            // Latest-quote polling is independent of historical OHLC downloads.
            delay(if(quote!=null)250L else 1000L)
        }
    }

    LaunchedEffect(intervalMs) { while(isActive) { delay(100);nowMs=FeedClock.now();clockText=clockNow();candles.lastOrNull()?.let { secondsLeft=remainingSeconds(it.time,nowMs,intervalMs) } } }
    val indicatorBars=if(isLoading||loadError)emptyList() else candles.toList()
    val chartView=clockSafeViewport(viewport,indicatorBars,origin,intervalMs,chartSize.height.toFloat(),candleCeilingY,ceilingHeadroom)
    val overlayConfigs=ExtraIndicators.active.toMap().filter { (name,cfg)->cfg.visible&&ExtraIndicators.definition(name)?.overlay==true }
    val overlaySeries=remember(indicatorBars,overlayConfigs) {
        overlayConfigs.map { (name,cfg)->cfg to IndicatorMath.calculate(ExtraIndicators.definition(name)!!,cfg,indicatorBars) }
    }
    if(IndicatorSettings.acceleratorEnabled||ExtraIndicators.active.isNotEmpty()) {
        val indicatorSnapshot=IndicatorChartSnapshot(indicatorBars,chartView,origin,intervalMs,sym,frame)
        SideEffect { IndicatorSettings.snapshot=indicatorSnapshot }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier.background(C.ChartBg).onSizeChanged { chartSize=it }) {
            CachedTradingImage(url=CHART_BG_URL,contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha=.25f)))
            if(isLoading) {
                ReferenceChartLoading(Modifier.matchParentSize(),sym+"-"+frame+"-"+account+"-"+retryKey,!everLoaded)
            } else if(loadError) {
                Column(Modifier.align(Alignment.Center).background(Ref.Background,RoundedCornerShape(12.dp)).border(1.dp,C.Red,RoundedCornerShape(12.dp)).clickable { retryKey++ }.padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("لا توجد بيانات حقيقية",color=C.Red,fontSize=14.sp,fontWeight=FontWeight.Bold)
                    Text("تحقق من الاتصال وتوفر بيانات هذا الفريم، ثم اضغط لإعادة المحاولة",color=C.TextSecondary,fontSize=11.sp)
                }
            } else Canvas(Modifier.fillMaxSize().onSizeChanged { chartSize=it }
                .pointerInput(Unit) {
                    var lastTapId:Long?=null;var lastTapTime=0L
                    try { awaitEachGesture {
                        val down=awaitFirstDown(requireUnconsumed=false);onBegin()
                        val hit=onDrawingHit(down.position)
                        val drawingDrag=onDrawingDragStart(hit)
                        val tracker=VelocityTracker();tracker.addPosition(down.uptimeMillis,down.position)
                        var pinchStarted=false;var dragging=false;var pan=Offset.Zero;var zoom=1f
                        var drawingDelta=Offset.Zero;var lastPosition=down.position;var releasedAt=down.uptimeMillis
                        var releaseVelocity=0f;var completed=false
                        val slop=viewConfiguration.touchSlop
                        try { do {
                            val event=awaitPointerEvent();val pressed=event.changes.count { it.pressed };val stable=event.changes.count { it.pressed&&it.previousPressed }
                            event.changes.firstOrNull { it.id==down.id }?.let {
                                lastPosition=it.position;releasedAt=it.uptimeMillis
                                if(!pinchStarted)tracker.addPosition(it.uptimeMillis,it.position)
                            }
                            if(pressed>=2) {
                                if(!pinchStarted) {
                                    if(drawingDrag!=null&&dragging)onDrawingRestore(drawingDrag)
                                    pinchStarted=true;dragging=false;zoom=1f;tracker.resetTracking();onPinchStart()
                                }
                                if(pressed==stable) { val z=event.calculateZoom()
                                    if(z.isFinite()&&z>0) { if(!dragging) { zoom*=z;val r=event.calculateCentroidSize(useCurrent=false);if(abs(1-zoom)*r>slop) { dragging=true;onZoom(zoom.toDouble()) } } else onZoom(z.toDouble()) }
                                };event.changes.forEach { it.consume() }
                            } else if(pressed==1&&stable==1&&!pinchStarted) {
                                val movement=event.calculatePan()
                                if(drawingDrag!=null) {
                                    drawingDelta+=movement
                                    if(!dragging&&drawingDelta.getDistance()>slop)dragging=true
                                    if(dragging) { onDrawingMove(drawingDrag,drawingDelta);event.changes.forEach { it.consume() } }
                                } else {
                                    if(!dragging) { pan+=movement;if(abs(pan.x)>slop) { dragging=true;onPan(pan.x-if(pan.x>0)slop else -slop) } } else onPan(movement.x)
                                    if(dragging)event.changes.forEach { it.consume() }
                                }
                            } else if(pinchStarted)event.changes.forEach { it.consume() }
                            if(pressed==0) {
                                completed=true
                                if(dragging&&!pinchStarted&&drawingDrag==null)releaseVelocity=tracker.calculateVelocity().x
                            }
                        } while(event.changes.any { it.pressed }) } finally {
                            if(!completed&&drawingDrag!=null&&!pinchStarted)onDrawingRestore(drawingDrag)
                            onEnd()
                        }
                        if(completed&&!dragging&&!pinchStarted) {
                            val doubleTap=hit!=null&&lastTapId==hit.id&&down.uptimeMillis-lastTapTime in 0L..viewConfiguration.doubleTapTimeoutMillis
                            val longPress=releasedAt-down.uptimeMillis>=viewConfiguration.longPressTimeoutMillis
                            onChartTap(lastPosition,hit,doubleTap||longPress)
                            lastTapId=hit?.id;lastTapTime=if(doubleTap)0L else releasedAt
                        }
                        if(completed&&abs(releaseVelocity)>=80f)onFling(releaseVelocity)
                    } } finally { onEnd() }
                }) {
                val w=size.width;val h=size.height
                resultHits.clear()
                if(w<=1||h<=1||candles.isEmpty())return@Canvas
                val view=chartView
                fun xFor(bar:Double)=(view.fractionFor(bar)*w).toFloat()
                fun yFor(price:Double)=chartPriceY(price,view.maxPrice,view.priceSpan,h)
                fun baseline(y:Float,p:NativePaint)=y-(p.ascent()+p.descent())/2
                val native=drawContext.canvas.nativeCanvas
                val rows=(h/58.dp.toPx()).toInt().coerceIn(3,12);val step=nicePriceStep(view.priceSpan/rows);val decimals=forexPriceDecimals(sym)
                val pFirst=ceil(view.minPrice/step).toLong();val pLast=floor(view.maxPrice/step).toLong()
                if(pLast>=pFirst&&pLast-pFirst<1000)for(t in pFirst..pLast) {
                    val price=t*step;val y=yFor(price)
                    drawLine(C.Grid.copy(alpha=.34f),Offset(0f,y),Offset(w,y),1.dp.toPx())
                    if(y>=axisTextPx&&y<=h-axisTextPx/2)native.drawText(priceText(price,decimals),w-5.dp.toPx(),baseline(y,axisPaint),axisPaint)
                }
                val cols=(w/64.dp.toPx()).toInt().coerceAtLeast(2);val timeStep=niceTimeStep(view.visibleBars/cols)
                val om=origin/intervalMs;val left=view.centerBar-view.visibleBars/2+om;val right=view.centerBar+view.visibleBars/2+om
                val tFirst=ceil(left/timeStep).toLong();val tLast=floor(right/timeStep).toLong()
                if(tLast>=tFirst&&tLast-tFirst<1000)for(t in tFirst..tLast) {
                    val minute=t*timeStep;val x=xFor((minute-om).toDouble())
                    drawLine(C.Grid.copy(alpha=.28f),Offset(x,0f),Offset(x,h),1.dp.toPx())
                    val label=candleTimeText(minute*intervalMs,intervalMs);val half=timePaint.measureText(label)/2
                    if(x-half>=0&&x+half<=w)native.drawText(label,x,baseline(h-7.dp.toPx(),timePaint),timePaint)
                }
                val spacing=w/view.visibleBars.toFloat();val bw=candleBodyWidth(spacing,1.dp.toPx())
                val wick=1.dp.toPx() 
                var candleTipY:Float?=null
                clipRect(0f,0f,w,h) {
                    val source=candles.toMutableList()
                    displayCandle?.takeIf { it.time==source.last().time }?.let { source[source.lastIndex]=it }
                    val rendered=if(chartType=="هيكين آشي")heikinAshi(source) else source
                    candleTipY=rendered.lastOrNull()?.let { yFor(it.close) }
                    if(chartType=="رسم بياني") {
                        val line=Path();var began=false
                        for(candle in rendered) {
                            val x=xFor(barPosition(candle.time,origin));val yClose=yFor(candle.close.toDouble())
                            if(!began) { line.moveTo(x,yClose);began=true } else line.lineTo(x,yClose)
                        }
                        drawPath(line,C.Cyan,style=Stroke(1.8.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
                    } else for(candle in rendered) {
                        val unit=1.dp.toPx()
                        val x=(floor(xFor(barPosition(candle.time,origin))/unit+.5f)+.5f)*unit
                        if(x+bw/2<0||x-bw/2>w)continue
                        val col=candleColor(candle)
                        val yH=yFor(candle.high.toDouble());val yL=yFor(candle.low.toDouble());val yO=yFor(candle.open.toDouble());val yC=yFor(candle.close.toDouble())
                        drawLine(col,Offset(x,yH),Offset(x,yL),wick,StrokeCap.Butt)
                        if(chartType=="أشرطة") {
                            drawLine(col,Offset(x-bw/2,yO),Offset(x,yO),wick,StrokeCap.Butt)
                            drawLine(col,Offset(x,yC),Offset(x+bw/2,yC),wick,StrokeCap.Butt)
                        } else {
                            drawRect(col,Offset(x-bw/2,min(yO,yC)),Size(bw,max(1.dp.toPx(),abs(yO-yC))))
                        }
                    }
                }
                drawIndicatorOverlays(indicatorBars,view,origin,intervalMs,overlaySeries)
                drawChartDrawings(drawingObjects,DrawingProjection(view,origin,intervalMs,w,h))
                val displayPrice=displayCandle?.takeIf { it.time==candles.last().time }?.close ?: candles.last().close
                val y=yFor(displayPrice.toDouble())
                if(y in 0f..h&&displayPrice>0) {
                    drawLine(C.Yellow.copy(alpha=.85f),Offset(0f,y),Offset(w,y),1.dp.toPx())
                    val label=priceText(displayPrice.toDouble(),decimals);val tw=tagPaint.measureText(label)+14.dp.toPx();val th=20.dp.toPx()
                    drawRoundRect(C.Yellow,Offset(w-tw-3.dp.toPx(),y-th/2),Size(tw,th),CornerRadius(4.dp.toPx()))
                    native.drawText(label,w-3.dp.toPx()-tw/2,baseline(y,tagPaint),tagPaint)
                    val candleX=xFor(barPosition(candles.last().time,origin))
                    val countdown=String.format(Locale.US,"%02d:%02d",secondsLeft/60,secondsLeft%60)
                    val labelHalf=max(detailPaint.measureText(frame),detailPaint.measureText(countdown))/2
                    val labelX=min(candleX+36.dp.toPx(),w-labelHalf-5.dp.toPx())
                    if(ChartSettings.countdownEnabled&&candleX in 0f..w&&labelX-labelHalf>candleX+bw/2) {
                        native.drawText(frame,labelX,y-11.dp.toPx(),detailPaint)
                        native.drawText(countdown,labelX,y+21.dp.toPx(),detailPaint)
                    }
                }
                clipRect(0f,0f,w,h) {
                    val liveX=xFor(barPosition(candles.last().time,origin))
                    drawReferenceTrades(sym,AccountStore.selected,nowMs,liveX,{ time->xFor(barPosition(time,origin)) },::yFor,resultHits,intervalMs,tradeCardsVisible,bw/2)
                    // Use the same pixel alignment and close as the moving candle.
                    val unit=1.dp.toPx()
                    val tipX=if(chartType=="رسم بياني")liveX else (floor(liveX/unit+.5f)+.5f)*unit
                    val tipY=candleTipY
                    if(tipY!=null&&tipX in 0f..w&&tipY in 0f..h) {
                        drawCircle(C.Yellow.copy(alpha=.26f*(1f-pricePulse)),(3f+4f*pricePulse).dp.toPx(),Offset(tipX,tipY))
                        drawCircle(C.Yellow,2.4.dp.toPx(),Offset(tipX,tipY))
                    }
                }
            }
            PairHeader(Modifier.align(Alignment.TopStart).padding(start=4.dp,top=8.dp),onOpenPairPicker)
            Column(Modifier.align(Alignment.TopStart).padding(start=14.dp,top=60.dp)) {
                Text("$clockText UTC+3",color=C.TextSecondary,fontSize=11.sp,modifier=Modifier.onSizeChanged { clockHeightPx=it.height })
                // The dot is the maximum wick height, just beneath the platform time.
                Spacer(Modifier.height(3.dp))
                Box(Modifier.size(4.dp).background(C.Yellow,CircleShape))
                if(hasOpenTrades) {
                    Spacer(Modifier.height(3.dp))
                    Box(Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).background(C.Surface.copy(alpha=.92f)).clickable { tradeCardsVisible=!tradeCardsVisible },contentAlignment=Alignment.Center) {
                        TradeVisibilityIcon(tradeCardsVisible,Modifier.size(20.dp))
                    }
                }
            }
            ExtraIndicatorOverlayHeadings(Modifier.align(Alignment.TopStart).padding(start=14.dp,top=if(hasOpenTrades)126.dp else 88.dp))
            DrawingToolbar(sym,frame,Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start=6.dp,end=6.dp,bottom=28.dp))
            TopNotificationOverlay(Modifier.align(Alignment.TopStart))
        }
    }
    if(ChartDrawings.managerOpen)DrawingManagerDialog(sym) { ChartDrawings.managerOpen=false }
    val editingDrawing=ChartDrawings.editing?.let { ChartDrawings.objects[it] }?.takeIf { it.symbol==sym }
    if(editingDrawing!=null)DrawingSettingsDialog(editingDrawing) { ChartDrawings.editing=null }
}

@Composable private fun TradeVisibilityIcon(visible:Boolean,modifier:Modifier=Modifier) {
    Canvas(modifier) {
        val w=size.width;val h=size.height;val color=C.Yellow;val stroke=1.6.dp.toPx()
        val outline=Path().apply {
            moveTo(w*.08f,h*.50f)
            cubicTo(w*.30f,h*.15f,w*.70f,h*.15f,w*.92f,h*.50f)
            cubicTo(w*.70f,h*.85f,w*.30f,h*.85f,w*.08f,h*.50f)
            close()
        }
        drawPath(outline,color,style=Stroke(stroke,cap=StrokeCap.Round,join=StrokeJoin.Round))
        drawCircle(color,w*.13f,Offset(w/2,h/2),style=Stroke(stroke))
        if(!visible)drawLine(color,Offset(w*.15f,h*.15f),Offset(w*.85f,h*.85f),stroke,StrokeCap.Round)
    }
}
private data class MarkerPlacement(val trade:OpenTrade,val dot:Float,val dotY:Float,val end:Float,val card:Rect,val countdown:String)
private fun DrawScope.drawReferenceTrades(symbol:String,account:String,now:Long,liveX:Float,xFor:(Long)->Float,yFor:(Double)->Float,hits:MutableList<Pair<Long,Rect>>,intervalMs:Long=MINUTE_MS,showCards:Boolean=true,candleHalfWidth:Float=0f) {
    fun px(n:Float)=n*density
    val w=size.width;val h=size.height;val native=drawContext.canvas.nativeCanvas
    val amountPaint=NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { typeface=Typeface.create("Arial",Typeface.BOLD);textSize=px(10f);textAlign=NativePaint.Align.LEFT }
    val timerPaint=NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { typeface=Typeface.DEFAULT;textSize=px(9f);textAlign=NativePaint.Align.LEFT;color=Color(0xFFCAD5E5).toArgb() }
    val places=mutableListOf<MarkerPlacement>()
    val trades=TradeStore.openTrades.filter { it.symbol==symbol&&it.account==account }.sortedWith(compareBy<OpenTrade> { it.candleTime }.thenBy { it.openedAt }.thenBy { it.id })
    if(w>0f&&h>0f)for(tr in trades) {
        // Keep the entry on its original candle; only the far end follows new candles.
        val dot=xFor(tr.openedAt/intervalMs*intervalMs)
        val dotY=yFor(tr.exactEntry)
        val end=max(dot,liveX+max(px(28f),candleHalfWidth+px(18f)))
        if(!dot.isFinite()||!dotY.isFinite()||!end.isFinite()||dot>w||end<0f||dotY<0f||dotY>h)continue
        val seconds=ceil((tr.expiresAt-now).coerceAtLeast(0)/1000.0).toLong()
        val countdown=String.format(Locale.US,"%02d:%02d",seconds/60,seconds%60)
        val width=min(max(amountPaint.measureText("$"+shortAmount(tr.amount)),timerPaint.measureText(countdown))+px(30f),(w-px(12f)).coerceAtLeast(1f))
        val height=min(px(38f),h)
        val left=(end+px(12f)).coerceIn(px(6f),(w-width-px(6f)).coerceAtLeast(px(6f)))
        val maxTop=(h-height-px(6f)).coerceAtLeast(0f)
        val preferred=(dotY-height/2).coerceIn(0f,maxTop)
        fun cardAt(top:Float)=Rect(left,top,left+width,top+height)
        var card=cardAt(preferred)
        val laneCount=(h/(height+px(5f))).toInt().coerceIn(1,100)
        var found=false
        for(lane in 0..laneCount) {
            for(sign in if(lane==0)listOf(0) else listOf(-1,1)) {
                val top=(preferred+sign*lane*(height+px(5f))).coerceIn(0f,maxTop)
                val candidate=cardAt(top)
                if(places.none { p->candidate.left<p.card.right+px(4f)&&candidate.right>p.card.left-px(4f)&&candidate.top<p.card.bottom+px(4f)&&candidate.bottom>p.card.top-px(4f) }) {
                    card=candidate;found=true;break
                }
            }
            if(found)break
        }
        places.add(MarkerPlacement(tr,dot,dotY,end,card,countdown))
    }
    for(p in places) {
        val color=if(p.trade.direction>0)Color(0xFF35CF87) else Color(0xFFFF6576)
        // This segment remains visible even after the entry candle scrolls off screen.
        drawLine(color.copy(alpha=.80f),Offset(max(p.dot,0f),p.dotY),Offset(min(p.end,w),p.dotY),px(1.3f),StrokeCap.Round)
        val card=p.card
        val attachX=if(card.left>=p.end)card.left else card.right
        val attachY=(card.top+card.bottom)/2
        if(showCards)drawLine(color.copy(alpha=.55f),Offset(p.end.coerceIn(0f,w),p.dotY),Offset(attachX,attachY),px(.9f),StrokeCap.Round)
        if(p.dot in 0f..w) {
            drawCircle(color.copy(alpha=.15f),px(7f),Offset(p.dot,p.dotY))
            drawCircle(color,px(4f),Offset(p.dot,p.dotY))
            drawCircle(Color.White,px(1.6f),Offset(p.dot,p.dotY))
        }
        if(p.end in 0f..w)drawCircle(color,px(2.5f),Offset(p.end,p.dotY))
    }
    if(showCards)for(p in places) {
        val up=p.trade.direction>0
        val card=p.card;val l=card.left;val top=card.top
        val cardSize=Size(card.width,card.height);val radius=CornerRadius(px(6f))
        val cardColor=if(up)C.Green else C.Red
        drawRoundRect(Color.Black.copy(alpha=.20f),Offset(l,top+px(2f)),cardSize,radius)
        drawRoundRect(cardColor,Offset(l,top),cardSize,radius)
        drawRoundRect(C.Yellow,Offset(l,top),cardSize,radius,style=Stroke(px(1f)))
        drawLine(C.Yellow,Offset(l+px(3f),top+px(8f)),Offset(l+px(3f),card.bottom-px(8f)),px(2f),StrokeCap.Round)
        val arrow=Path().apply {
            if(up) { moveTo(l+px(12f),top+px(8f));lineTo(l+px(16f),top+px(14f));lineTo(l+px(8f),top+px(14f)) }
            else { moveTo(l+px(8f),top+px(9f));lineTo(l+px(16f),top+px(9f));lineTo(l+px(12f),top+px(15f)) }
            close()
        }
        drawPath(arrow,C.Yellow)
        amountPaint.color=Color(0xFF30203C).toArgb()
        timerPaint.color=Color(0xFF40223D).toArgb()
        native.drawText("$"+shortAmount(p.trade.amount),l+px(21f),top+px(16f),amountPaint)
        native.drawText(p.countdown,l+px(21f),top+px(30f),timerPaint)
    }
    val label=android.text.TextPaint(NativePaint.ANTI_ALIAS_FLAG).apply { typeface=Typeface.create("Tahoma",Typeface.NORMAL);textSize=px(9f);color=Color.White.toArgb() }
    val labelText="النتيجة (ربح/خسارة)"
    val labelLayout=android.text.StaticLayout.Builder.obtain(labelText,0,labelText.length,label,px(100f).toInt()).setTextDirection(android.text.TextDirectionHeuristics.RTL).setAlignment(android.text.Layout.Alignment.ALIGN_OPPOSITE).setIncludePad(false).build()
    val profitPaint=NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { typeface=Typeface.create("Arial",Typeface.BOLD);textSize=px(15f);color=Color.White.toArgb();textAlign=NativePaint.Align.LEFT }
    for(result in TradeStore.results) {
        val tr=result.trade
        if(tr.symbol!=symbol||tr.account!=account||result.visibleUntil<=now)continue
        val y=yFor(tr.exactEntry);if(y<px(-30f)||y>h+px(30f))continue
        val ex=min(xFor(tr.openedAt/intervalMs*intervalMs),w-px(67f));val bw=px(126f);val bh=px(48f)
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
    var expanded by remember { mutableStateOf(false) }
    var manualOpen by remember { mutableStateOf(false) }
    val density=LocalDensity.current;val config=LocalConfiguration.current
    Column(modifier) {
        Text("الزمن",color=C.TextSecondary,fontSize=12.sp);Spacer(Modifier.height(6.dp))
        Box {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).background(C.Surface).border(1.dp,C.Divider,RoundedCornerShape(10.dp)).clickable { expanded=!expanded }) {
                    Text(TradeSettings.duration,color=C.TextPrimary,fontSize=14.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,style=TextStyle(textDirection=TextDirection.Ltr),modifier=Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal=38.dp),maxLines=1)
                    ReferenceGlyph("time",Modifier.align(Alignment.CenterStart).padding(start=12.dp).size(20.dp),C.Yellow)
                }
            }
            if(expanded)Popup(popupPositionProvider=remember(density.density) { ReferencePopupPosition(true,density.density) },onDismissRequest={expanded=false},properties=PopupProperties(focusable=true)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(Modifier.width(min(248,config.screenWidthDp-16).coerceAtLeast(1).dp).shadow(24.dp,RoundedCornerShape(14.dp)).background(Ref.Background,RoundedCornerShape(14.dp)).border(1.dp,Ref.Border,RoundedCornerShape(14.dp)).padding(9.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                            TimeOptions.chunked(3).forEach { row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                                row.forEach { time->Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp)).background(if(time==TradeSettings.duration)Ref.TimeOn else Ref.TimeButton).clickable { TradeSettings.duration=time;expanded=false },contentAlignment=Alignment.Center) {
                                    Text(time,color=Color.White,fontSize=14.sp,fontWeight=FontWeight.SemiBold,maxLines=1)
                                } }
                            } }
                        }
                        Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp)).background(Ref.TimeButton).clickable {
                            expanded=false;manualOpen=true
                        },contentAlignment=Alignment.Center) { Text("ضبط يدوياً",color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold) }
                    }
                }
            }
        }
    }
    if(manualOpen)ManualTradeTimeDialog { manualOpen=false }
}
@Composable private fun ManualTradeTimeDialog(onDismiss:()->Unit) {
    val total=(durationMillis(TradeSettings.duration) ?: 60_000L)/1000L
    var hours by remember { mutableStateOf((total/3600L).toString()) }
    var minutes by remember { mutableStateOf((total/60L%60L).toString().padStart(2,'0')) }
    var seconds by remember { mutableStateOf((total%60L).toString().padStart(2,'0')) }
    var error by remember { mutableStateOf(false) }
    val focus=remember { FocusRequester() }
    val keyboard=LocalSoftwareKeyboardController.current
    val focusManager=LocalFocusManager.current
    fun save() {
        val h=hours.toLongOrNull();val m=minutes.toIntOrNull();val sec=seconds.toIntOrNull()
        if(h==null||m==null||sec==null||h !in 0L..999999L||m !in 0..59||sec !in 0..59||h*3600L+m*60L+sec<=0L) { error=true;return }
        TradeSettings.duration=String.format(Locale.US,"%02d:%02d:%02d",h,m,sec)
        focusManager.clearFocus();keyboard?.hide();onDismiss()
    }
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(Modifier.padding(20.dp).widthIn(max=360.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Ref.Menu).border(1.dp,Ref.Border,RoundedCornerShape(14.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Text("ضبط الوقت يدوياً",color=C.TextPrimary,fontSize=17.sp,fontWeight=FontWeight.Bold)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        ManualTimePart("ساعة",hours,{hours=it;error=false},Modifier.weight(1f),focus)
                        ManualTimePart("دقيقة",minutes,{minutes=it;error=false},Modifier.weight(1f))
                        ManualTimePart("ثانية",seconds,{seconds=it;error=false},Modifier.weight(1f),onDone={save()})
                    }
                }
                if(error)Text("أدخل وقتاً أكبر من صفر، والدقائق والثواني من 0 إلى 59",color=C.Red,fontSize=12.sp)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(8.dp)).background(Ref.TimeOn).clickable { save() },contentAlignment=Alignment.Center) { Text("حفظ",color=Color.White,fontWeight=FontWeight.Bold) }
                    Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(8.dp)).background(Ref.TimeButton).clickable(onClick=onDismiss),contentAlignment=Alignment.Center) { Text("إلغاء",color=Color.White) }
                }
            }
        }
        LaunchedEffect(Unit) { withFrameNanos { };focus.requestFocus();withFrameNanos { };keyboard?.show() }
    }
}
@Composable private fun ManualTimePart(label:String,value:String,onChange:(String)->Unit,modifier:Modifier=Modifier,focus:FocusRequester?=null,onDone:(()->Unit)?=null) {
    Column(modifier,horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(label,color=C.TextSecondary,fontSize=12.sp)
        BasicTextField(value=value,onValueChange={text->if(text.length<=(if(label=="ساعة")6 else 2)&&text.all { it in '0'..'9' })onChange(text)},
            modifier=Modifier.fillMaxWidth().height(48.dp).then(if(focus!=null)Modifier.focusRequester(focus) else Modifier),
            singleLine=true,cursorBrush=SolidColor(C.Cyan),textStyle=TextStyle(color=C.TextPrimary,fontSize=18.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center),
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number,imeAction=if(onDone==null)ImeAction.Next else ImeAction.Done),
            keyboardActions=KeyboardActions(onDone={onDone?.invoke()}),
            decorationBox={inner->Box(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(C.Surface).border(1.dp,C.Divider,RoundedCornerShape(8.dp)).padding(8.dp),contentAlignment=Alignment.Center) { inner() }})
    }
}
private val DollarPrefixTransformation=VisualTransformation { text->
    TransformedText(AnnotatedString("$"+text.text),object:OffsetMapping {
        override fun originalToTransformed(offset:Int)=(offset+1).coerceIn(1,text.length+1)
        override fun transformedToOriginal(offset:Int)=(offset-1).coerceIn(0,text.length)
    })
}
private val PercentageSuffixTransformation=VisualTransformation { text->
    TransformedText(AnnotatedString(text.text+"%"),object:OffsetMapping {
        override fun originalToTransformed(offset:Int)=offset.coerceIn(0,text.length)
        override fun transformedToOriginal(offset:Int)=offset.coerceIn(0,text.length)
    })
}
@Composable private fun AmountInput(modifier:Modifier=Modifier) {
    val focusManager=LocalFocusManager.current
    val percentageMode=TradeSettings.percentageMode
    var input by remember(percentageMode) { mutableStateOf(if(percentageMode)TradeSettings.percentage else TradeSettings.amount) }
    val invalidPercentage=percentageMode&&!TradeSettings.validPercentage()
    fun commit() {
        if(percentageMode)TradeSettings.percentage=input else {
            TradeSettings.amount=input;input=TradeSettings.amount
        }
    }
    Column(modifier) {
        Text(if(percentageMode)"النسبة من الرصيد" else "المبلغ",color=C.TextSecondary,fontSize=12.sp);Spacer(Modifier.height(6.dp))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            // Reserve the switch's lower half so it cannot overlap the trade buttons.
            Box(Modifier.fillMaxWidth().height(60.dp)) {
                Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).background(C.Surface).border(1.dp,if(invalidPercentage)C.Red else C.Divider,RoundedCornerShape(10.dp))) {
                    BasicTextField(input,{value->
                        if(value.isEmpty()||Regex("[0-9]*(?:\\.[0-9]*)?").matches(value)) {
                            input=value
                            if(percentageMode)TradeSettings.percentage=value else TradeSettings.amount=value
                        }
                    },modifier=Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal=38.dp).onFocusChanged { if(!it.isFocused)commit() },singleLine=true,cursorBrush=SolidColor(C.Cyan),visualTransformation=if(percentageMode)PercentageSuffixTransformation else DollarPrefixTransformation,textStyle=TextStyle(color=C.TextPrimary,fontSize=14.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,textDirection=TextDirection.Ltr),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal,imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={commit();focusManager.clearFocus()}))
                    if(percentageMode)Text("%",color=C.Yellow,fontSize=20.sp,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.CenterStart).padding(start=14.dp))
                    else ReferenceGlyph("amount",Modifier.align(Alignment.CenterStart).padding(start=12.dp).size(20.dp),C.Yellow)
                }
                Box(Modifier.align(Alignment.TopCenter).offset(y=36.5.dp).height(22.dp).drawBehind {
                    // Cut only the border behind the word, without a raised chip.
                    drawRect(C.Surface,Offset.Zero,Size(size.width,size.height/2))
                    drawRect(C.BG,Offset(0f,size.height/2),Size(size.width,size.height/2))
                }.clickable {
                    commit();focusManager.clearFocus();TradeSettings.percentageMode=!percentageMode
                }.padding(horizontal=10.dp),contentAlignment=Alignment.Center) {
                    Text("تبديل",color=C.Yellow,fontSize=10.sp,fontWeight=FontWeight.SemiBold,style=TextStyle(textDirection=TextDirection.Rtl,platformStyle=androidx.compose.ui.text.PlatformTextStyle(includeFontPadding=false)))
                }
            }
        }
    }
}
@Composable private fun InsufficientBalanceDialog() {
    val account=InsufficientBalanceState.account ?: return
    Dialog(onDismissRequest={InsufficientBalanceState.account=null},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(Modifier.padding(20.dp).widthIn(max=360.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Ref.Menu).border(1.dp,Ref.Border,RoundedCornerShape(14.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                WalletIcon(Modifier.size(40.dp),C.Yellow)
                Text("الرصيد غير كافٍ",color=C.TextPrimary,fontSize=19.sp,fontWeight=FontWeight.Bold)
                Text("لا يوجد رصيد كافٍ لفتح هذه الصفقة في الحساب "+if(account=="demo")"التجريبي" else "الحقيقي",color=C.TextSecondary,fontSize=14.sp,textAlign=TextAlign.Center)
                Text("الرصيد المتاح: "+money(AccountStore.balance(account))+" $",color=C.TextPrimary,fontSize=14.sp)
                Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(8.dp)).background(Ref.TimeOn).clickable { InsufficientBalanceState.account=null },contentAlignment=Alignment.Center) { Text("حسناً",color=Color.White,fontWeight=FontWeight.Bold) }
            }
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
    var query by remember { mutableStateOf("") }
    val allSymbols=PairCatalogStore.allSymbols;val connected=PairCatalogStore.connected
    val loading=!PairCatalogStore.loaded;val favorites=remember { mutableStateListOf<String>() }
    val context=LocalContext.current
    LaunchedEffect(Unit) { PairCatalogStore.load(context)
        while(isActive) { delay(10000);PairCatalogStore.refresh() } }
    val filtered=allSymbols.filter { s->
        val search=query.uppercase(Locale.US).filter { it.isLetterOrDigit() }
        SymCatalog.isForex(s)&&(search.isBlank()||s.contains(search))
    }.sortedWith(compareBy({it !in connected},{it}))
    Popup(alignment=Alignment.TopStart,onDismissRequest=onDismiss,properties=PopupProperties(focusable=true)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.5f)).clickable(onClick=onDismiss)) {
            Column(Modifier.fillMaxWidth(.88f).fillMaxHeight().align(Alignment.CenterStart).clip(RoundedCornerShape(topEnd=18.dp,bottomEnd=18.dp)).background(C.PkBg).clickable(interactionSource=remember { androidx.compose.foundation.interaction.MutableInteractionSource() },indication=null,onClick={})) {
                Row(Modifier.fillMaxWidth().padding(start=14.dp,end=14.dp,top=12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("اختر زوج الفوركس",color=C.TextPrimary,fontSize=16.sp,fontWeight=FontWeight.ExtraBold,modifier=Modifier.weight(1f))
                    Text("✕",color=C.TextPrimary,fontSize=20.sp,modifier=Modifier.clickable(onClick=onDismiss).padding(6.dp))
                }
                Box(Modifier.padding(horizontal=14.dp,vertical=10.dp).background(C.PkTabOn,RoundedCornerShape(8.dp)).padding(horizontal=16.dp,vertical=8.dp)) {
                    Text("فوركس",color=C.BG,fontSize=12.sp,fontWeight=FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=4.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                    Row(Modifier.height(40.dp).widthIn(min=56.dp).clip(RoundedCornerShape(10.dp)).background(C.PkSurface).border(1.dp,C.Divider,RoundedCornerShape(10.dp)).padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically) { Text("★",color=C.PkStarOn,fontSize=15.sp);Spacer(Modifier.width(6.dp));Text("${favorites.size}",color=C.TextPrimary,fontSize=13.sp,fontWeight=FontWeight.Bold) }
                    Row(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp)).background(C.PkSurface).border(1.dp,C.Divider,RoundedCornerShape(10.dp)).padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically) {
                        BasicTextField(query,{query=it},modifier=Modifier.weight(1f),singleLine=true,textStyle=TextStyle(color=C.TextPrimary,fontSize=13.sp,fontWeight=FontWeight.SemiBold),decorationBox={inner->Box { if(query.isEmpty())Text("بحث...",color=C.TextSecondary,fontSize=13.sp);inner() } })
                        Text("🔍",color=C.TextPrimary,fontSize=15.sp)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),horizontalArrangement=Arrangement.SpaceBetween) { Text("فوركس",color=C.TextPrimary,fontSize=12.sp,fontWeight=FontWeight.Bold);Text("${filtered.size} زوج",color=C.PkPct,fontSize=12.sp,fontWeight=FontWeight.Bold) }
                if(loading)Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { ReferenceAnimatedLogo(Modifier.size(120.dp)) }
                else if(filtered.isEmpty())Box(Modifier.fillMaxSize().padding(20.dp),contentAlignment=Alignment.Center) {
                    Text(if(query.isBlank())"لا توجد أزواج فوركس متاحة حالياً" else "لا توجد أزواج مطابقة للبحث",color=C.TextSecondary,textAlign=TextAlign.Center)
                } else LazyColumn(Modifier.fillMaxSize()) {
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
        key(token) { ReferenceAnimatedLogo(Modifier.align(Alignment.Center).size(if(initial)175.dp else 120.dp)) }
    }
}
@Composable private fun ReferenceAnimatedLogo(modifier:Modifier=Modifier) {
    AndroidView(modifier=modifier,factory={context->WebView(context).apply {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        layoutParams=ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)
        settings.javaScriptEnabled=false;settings.blockNetworkLoads=true;settings.allowFileAccess=false;settings.allowContentAccess=false
        isVerticalScrollBarEnabled=false;isHorizontalScrollBarEnabled=false;importantForAccessibility=android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
        setOnTouchListener { _,_->true }
        loadDataWithBaseURL(null,"<html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>html,body{margin:0;width:100%;height:100%;overflow:hidden;background:transparent}body{display:grid;place-items:center}svg{display:block;width:100%;height:100%}</style></head><body>"+REFERENCE_LOADING_SVG+"</body></html>","text/html","UTF-8",null)
    } },onReset=null,onRelease={it.stopLoading();it.destroy()})
}

object HistoryOverlay { var open by mutableStateOf(false) }

private val ThText=C.TextPrimary
private val ThTabBg=C.Surface; private val ThTabBorder=C.Divider
private val ThTabOnBg=C.SurfaceRaised; private val ThTabOnBorder=C.Cyan
private val ThAsset=C.TextPrimary; private val ThStar=C.Yellow
private val ThPct=C.Green; private val ThTime=C.TextPrimary
private val ThAmount=C.TextPrimary; private val ThDir=C.Red
private val ThGain=C.Green; private val ThLoss=C.Red
private val ThEmpty=C.TextSecondary; private val ThDetailBg=C.Surface
private val ThGridBg=C.BG; private val ThBarBg=C.Divider
private val ThBarFill=C.Cyan; private val ThLabel=C.TextSecondary
private val ThGreen=C.Green; private val ThRed=C.Red

private class ThItem(val id:Long,val symbol:String,val direction:Int,val amount:Double,val payoutPct:Int,val openedAt:Long,val expiresAt:Long,val entry:Double,val closePrice:Double,val profit:Double,val isOpen:Boolean,val closedAt:Long=0L)

private fun thFreshQuote(quote:Quote?,now:Long)=
    quote!=null&&quote.sourceTime>0L&&now-quote.sourceTime in -5000L..15000L

private fun thDigits(symbol:String)=if(symbol.contains("JPY"))3 else if(symbol=="XAUUSD")2 else 5
private fun thPrice(value:Double,symbol:String)=if(!(value>0))"—" else String.format(Locale.US,"%.${thDigits(symbol)}f",value)
private fun thMoney(value:Double)="$"+String.format(Locale.US,"%.2f",value)
private fun thClock(ms:Long):String { val n=maxOf(0L,(maxOf(0L,ms)+999)/1000); return String.format(Locale.US,"%02d:%02d",n/60,n%60) }
private fun thPointUnit(symbol:String)=if(symbol.contains("JPY"))0.001 else if(symbol=="XAUUSD")0.01 else 0.00001
private fun thTimeOfDay(time:Long)=SimpleDateFormat("HH:mm:ss.SSS",Locale.UK).apply { timeZone=TimeZone.getTimeZone("GMT+3") }.format(Date(time))
private fun thDurationLabel(durationMs:Long):String=when {
    durationMs%60000L==0L->"M"+(durationMs/60000L)
    durationMs>=60000L->"${durationMs/60000L}m ${durationMs%60000L/1000L}s"
    else->"${(durationMs+999)/1000L}s"
}

@Composable fun TradeHistoryPage() {
    BackHandler { HistoryOverlay.open=false }
    var tab by remember { mutableStateOf("open") }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var now by remember { mutableLongStateOf(FeedClock.now()) }
    LaunchedEffect(Unit) { while(isActive) { now=FeedClock.now(); delay(250) } }
    val account=AccountStore.selected
    val items=if(tab=="open")
        TradeStore.openTrades.filter { it.account==account }.map { ThItem(it.id,it.symbol,it.direction,it.amount,it.payoutPct,it.openedAt,it.expiresAt,it.exactEntry,0.0,0.0,true) }
    else
        TradeStore.history.filter { it.trade.account==account }.map { ThItem(it.trade.id,it.trade.symbol,it.trade.direction,it.trade.amount,it.trade.payoutPct,it.trade.openedAt,it.trade.expiresAt,it.trade.exactEntry,it.close,it.profit,false,it.closedAt) }
    val sorted=items.sortedByDescending { it.openedAt }
    if(selectedId!=null&&sorted.none { it.id==selectedId })selectedId=null
    val padH=(LocalConfiguration.current.screenWidthDp*0.051f).dp
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(Modifier.fillMaxSize().background(C.BG)) {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(start=padH,end=padH,top=16.dp,bottom=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        ThTab("مفتوحة",tab=="open") { tab="open"; selectedId=null }
                        ThTab("مغلقة",tab=="closed") { tab="closed"; selectedId=null }
                    }
                    if(sorted.isEmpty()) {
                        Text("لا توجد تداولات "+(if(tab=="open")"مفتوحة" else "مغلقة"),color=ThEmpty,fontSize=13.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=20.dp))
                    } else LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=16.dp)) {
                        items(sorted,key= { it.id }) { item->
                            TradeHistoryRow(item,selectedId==item.id,now) { selectedId=if(selectedId==item.id)null else item.id }
                            if(selectedId==item.id)TradeHistoryDetail(item,now)
                        }
                    }
                }
                ThBottomNavBar(onHome={ HistoryOverlay.open=false })
            }
        }
    }
}

@Composable private fun RowScope.ThTab(label:String,on:Boolean,onClick:()->Unit) {
    Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(7.dp)).background(if(on)ThTabOnBg else ThTabBg).border(1.dp,if(on)ThTabOnBorder else ThTabBorder,RoundedCornerShape(7.dp)).clickable(onClick=onClick),contentAlignment=Alignment.Center) {
        Text(label,color=ThText,fontSize=17.sp,fontWeight=FontWeight.Medium)
    }
}

@Composable private fun TradeHistoryRow(item:ThItem,selected:Boolean,now:Long,onToggle:()->Unit) {
    val padH=(LocalConfiguration.current.screenWidthDp*0.051f).dp
    val rate=item.payoutPct
    val profit=if(item.isOpen)item.amount*rate/100 else item.profit
    val resultColor=if(item.isOpen)C.Cyan else if(profit>0)C.Green else if(profit<0)C.Red else C.TextSecondary
    val sell=item.direction<0
    val pending=item.isOpen&&item.expiresAt<=now
    val total=if(item.isOpen)item.amount+profit else maxOf(0.0,item.amount+item.profit)
    val progress=if(item.isOpen)((now-item.openedAt).toDouble()/(item.expiresAt-item.openedAt).coerceAtLeast(1L)).toFloat().coerceIn(0f,1f) else 1f
    Column(Modifier.fillMaxWidth().padding(horizontal=padH,vertical=5.dp)
        .clip(RoundedCornerShape(12.dp)).background(if(selected)C.SurfaceRaised else C.Surface)
        .border(1.dp,if(selected)C.Cyan else C.Divider,RoundedCornerShape(12.dp))
        .clickable(onClick=onToggle).padding(13.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background((if(sell)C.Red else C.Green).copy(alpha=.10f),RoundedCornerShape(9.dp)),contentAlignment=Alignment.Center) {
                Text(if(sell)"↘" else "↗",color=if(sell)C.Red else C.Green,fontSize=23.sp,fontWeight=FontWeight.Bold)
            }
            Column(Modifier.weight(1f).padding(horizontal=10.dp)) {
                Text(PairNames.pretty(item.symbol),color=ThAsset,fontSize=15.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text((if(sell)"بيع" else "شراء")+"  •  $rate%",color=C.TextSecondary,fontSize=11.sp)
            }
            Column(horizontalAlignment=Alignment.End) {
                Text(if(pending)"بانتظار سعر الإغلاق" else if(item.isOpen)thClock(item.expiresAt-now) else if(profit>0)"رابحة" else if(profit<0)"خاسرة" else "تعادل",
                    color=resultColor,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
                Text(thDurationLabel(item.expiresAt-item.openedAt)+"  "+if(selected)"▴" else "▾",color=C.TextSecondary,fontSize=11.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Divider))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            ThCell("المبلغ",thMoney(item.amount),ThAmount,TextAlign.Start)
            ThCell(if(item.isOpen)"العائد المحتمل" else "العائد",thMoney(total),if(item.isOpen)C.TextPrimary else resultColor,TextAlign.Center)
            ThCell(if(item.isOpen)"الربح المحتمل" else "النتيجة",(if(profit>0)"+" else "")+thMoney(profit),resultColor,TextAlign.End)
        }
        if(item.isOpen) {
            Spacer(Modifier.height(11.dp))
            Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(3.dp)).background(C.Divider)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(progress).background(C.Cyan))
            }
        }
    }
}

@Composable private fun RowScope.ThCell(label:String,value:String,color:Color,align:TextAlign) {
    Column(Modifier.weight(1f)) {
        Text(label,color=ThLabel,fontSize=13.sp,maxLines=1,textAlign=align,modifier=Modifier.fillMaxWidth())
        Text(value,color=color,fontSize=13.sp,fontWeight=FontWeight.Medium,textAlign=align,modifier=Modifier.fillMaxWidth())
    }
}

@Composable private fun TradeHistoryDetail(item:ThItem,now:Long) {
    val padH=(LocalConfiguration.current.screenWidthDp*0.051f).dp
    val padInner=(LocalConfiguration.current.screenWidthDp*0.035f).dp
    val isOpen=item.isOpen
    val entry=item.entry
    val quote=TradeStore.quotes[item.symbol]
    val current=if(isOpen)quote?.price ?: Double.NaN else item.closePrice
    val quoteIsFresh=thFreshQuote(quote,now)
    val duration=maxOf(1L,item.expiresAt-item.openedAt)
    val left=if(isOpen)maxOf(0L,item.expiresAt-now) else 0L
    val elapsed=if(isOpen)(100.0*(1.0-left.toDouble()/duration)).coerceIn(0.0,100.0) else 100.0
    val hasPrice=entry>0&&current.isFinite()&&current>0
    val difference=if(hasPrice)current-entry else 0.0
    val points=if(hasPrice)kotlin.math.round(difference/thPointUnit(item.symbol)).toLong() else 0L
    val sell=item.direction<0
    val profit=if(isOpen)item.amount*item.payoutPct/100 else item.profit
    Column(Modifier.fillMaxWidth().padding(start=padH,end=padH,bottom=12.dp).clip(RoundedCornerShape(13.dp)).background(ThDetailBg).padding(horizontal=padInner).padding(top=16.dp,bottom=18.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("وقت الفتح:",color=ThLabel,fontSize=13.sp)
                Text(thTimeOfDay(item.openedAt),color=ThText,fontSize=13.sp)
            }
            Text(thDurationLabel(duration),color=ThText,fontSize=13.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=14.dp))
            Column(Modifier.weight(1f),horizontalAlignment=Alignment.End) {
                Text(if(isOpen)"موعد الانتهاء:" else "وقت الإغلاق:",color=ThLabel,fontSize=13.sp)
                Text(thTimeOfDay(if(isOpen)item.expiresAt else item.closedAt.takeIf { it>0L } ?: item.expiresAt),color=ThText,fontSize=13.sp)
            }
        }
        Box(Modifier.padding(top=19.dp,bottom=12.dp).fillMaxWidth().height(9.dp).clip(RoundedCornerShape(8.dp)).background(ThBarBg)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((elapsed/100).toFloat().coerceIn(0f,1f)).clip(RoundedCornerShape(8.dp)).background(ThBarFill))
        }
        Text(if(isOpen&&left==0L)"بانتظار أول سعر من المصدر بعد الانتهاء" else if(isOpen)"الوقت المتبقي : "+thClock(left) else "انتهت الصفقة",color=ThText,fontSize=14.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(bottom=12.dp))
        Text(if(isOpen&&!quoteIsFresh)"Biquote • آخر سعر متاح — بانتظار تحديث" else "Biquote • سعر المصدر دون تعديل",color=C.TextSecondary,fontSize=10.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(bottom=12.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(ThGridBg).padding(horizontal=padInner,vertical=17.dp)) {
            Row(Modifier.fillMaxWidth()) {
                ThCell("توقعاتك:",if(sell)"بيع" else "شراء",if(sell)ThRed else ThGreen,TextAlign.Start)
                ThCell("المنصرف:",thMoney(item.amount),ThText,TextAlign.Center)
                ThCell(if(isOpen)"الربح المحتمل:" else "النتيجة:",thMoney(profit),if(isOpen)C.Cyan else if(profit<0)ThRed else if(profit>0)ThGreen else ThText,TextAlign.End)
            }
            Spacer(Modifier.height(19.dp))
            Row(Modifier.fillMaxWidth()) {
                ThCell("سعر الفتح:",thPrice(entry,item.symbol),ThText,TextAlign.Start)
                ThCell("الفرق:",if(!hasPrice)"—" else if(points>0)"(+$points النقاط)" else if(points<0)"(\u2212${-points} النقاط)" else "(0 النقاط)",if(points>0)ThGreen else if(points<0)ThRed else ThText,TextAlign.Center)
                ThCell(if(isOpen)"آخر سعر:" else "سعر الإغلاق:",thPrice(current,item.symbol),ThText,TextAlign.End)
            }
        }
    }
}

@Composable private fun ThBottomNavBar(onHome:()->Unit={},onOperations:()->Unit={ HistoryOverlay.open=true }) {
    var selected by remember { mutableIntStateOf(2) }
    Column(Modifier.background(C.BG)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Divider))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth().background(C.BG).padding(vertical=6.dp),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
                ThBottomItem("الشارت",selected==0,{selected=0;onHome()}) { ChartIcon(Modifier.size(24.dp),it) }
                ThBottomItem("الدردشة",selected==1,{selected=1}) { ChatIcon(Modifier.size(24.dp),it) }
                ThBottomItem("العمليات",selected==2,{selected=2;onOperations()}) { HistoryIcon(Modifier.size(24.dp),it) }
                ThBottomItem("أول الاجتماعي",selected==3,{selected=3}) { SocialIcon(Modifier.size(24.dp),it) }
                ThBottomItem("المزيد",selected==4,{selected=4}) { DotsIcon(Modifier.size(24.dp),it) }
            }
        }
    }
}
@Composable private fun ThBottomItem(label:String,selected:Boolean,onClick:()->Unit,icon:@Composable (Color)->Unit) {
    val color=if(selected)C.Cyan else C.TextSecondary
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick=onClick).padding(horizontal=6.dp,vertical=4.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(if(selected)C.SurfaceRaised else Color.Transparent).border(1.dp,if(selected)C.Cyan else Color.Transparent,RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center) { icon(color) }
        Spacer(Modifier.height(3.dp));Text(label,color=color,fontSize=10.sp,textAlign=TextAlign.Center,maxLines=1)
    }
}

@Composable fun BottomNavBar(onHome:()->Unit={},onOperations:()->Unit={ HistoryOverlay.open=true }) {
    var selected by remember { mutableIntStateOf(2) }
    val openTradeCount=TradeStore.openTrades.count { it.account==AccountStore.selected }
    Column(Modifier.background(C.BG)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Divider))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth().background(C.BG).padding(vertical=6.dp),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
                BottomItem("الشارت",selected==0,{selected=0;onHome()}) { ChartIcon(Modifier.size(24.dp),it) }
                BottomItem("الدردشة",selected==1,{selected=1}) { ChatIcon(Modifier.size(24.dp),it) }
                BottomItem("العمليات",selected==2,{selected=2;onOperations()}) { color->
                    Box(Modifier.size(30.dp),contentAlignment=Alignment.Center) {
                        HistoryIcon(Modifier.size(24.dp),color)
                        if(openTradeCount>0)Box(Modifier.align(Alignment.TopEnd).heightIn(min=16.dp).widthIn(min=16.dp).background(C.BG,CircleShape).padding(horizontal=3.dp),contentAlignment=Alignment.Center) {
                            Text(openTradeCount.toString(),color=Color(0xFFFF4757),fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=1)
                        }
                    }
                }
                BottomItem("أول الاجتماعي",selected==3,{selected=3}) { SocialIcon(Modifier.size(24.dp),it) }
                BottomItem("المزيد",selected==4,{selected=4}) { DotsIcon(Modifier.size(24.dp),it) }
            }
        }
    }
}
@Composable fun BottomItem(label:String,selected:Boolean,onClick:()->Unit,icon:@Composable (Color)->Unit) {
    val color=if(selected)C.Cyan else C.TextSecondary
    val interactionSource=remember { MutableInteractionSource() }
    Column(Modifier.clickable(interactionSource=interactionSource,indication=null,onClick=onClick).padding(horizontal=6.dp,vertical=4.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.size(34.dp),contentAlignment=Alignment.Center) { icon(color) }
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
            "time"->listOf("M9 2h6M12 2v3M18 5l2-2","M20 13a8 8 0 1 1-16 0 8 8 0 0 1 16 0Z","M12 8v5l3 2")
            "amount"->listOf("M4 6h15a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3h12v3","M21 10h-6a3 3 0 0 0 0 6h6M16 13h.01")
            "close"->listOf("M5 5l14 14M19 5 5 19")
            "check"->listOf("m5 12 4 4L19 6")
            "candles"->listOf("M3 21h18M8 5v4M6 9h4v9H6ZM17 2v4M15 6h4v10h-4Z")
            "people"->listOf("M14 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z","M2 21v-2a6 6 0 0 1 6-6h3a6 6 0 0 1 6 6v2","M18 3a4 4 0 0 1 0 8M19 14a6 6 0 0 1 3 5v2")
            "settings"->listOf("M3 7h12M20 7h1M3 16h3M11 16h10","M20 7a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z","M11 16a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z")
            "temperature"->listOf("M7 4a3 3 0 0 1 6 0v10a5 5 0 1 1-6 0Z","M10 6v11M17 5h4M17 9h3M17 13h4","M12 18a2 2 0 1 1-4 0 2 2 0 0 1 4 0Z")
            "drawing"->listOf("M12 2v2M15 7a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z","M10 10 4 22M14 10l6 12M5 15q7 6 14 0M2 18l3-3M19 15l3 3")
            "bolt"->listOf("m14 2-10 12h7l-1 8 10-13h-7Z")
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
            val originalGlyph=kind=="wallet"||kind=="download"||kind=="demo"||kind=="coins"||kind=="dollar"
            paths.forEach { drawPath(it,color,style=if(originalGlyph)Stroke(stroke) else Stroke(stroke,cap=StrokeCap.Round,join=StrokeJoin.Round)) }
            if(kind=="coins")drawOval(color,Offset(5f,3f),Size(14f,6f),style=Stroke(stroke))
            if(kind=="dollar")drawCircle(color,9f,Offset(12f,12f),style=Stroke(stroke))
        }
    }
}

private const val REFERENCE_LOADING_SVG = """<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512"><defs><linearGradient id="white" x2="1" y2="1"><stop stop-color="#fff"/><stop offset="1" stop-color="#bdcbea"/></linearGradient><linearGradient id="green" x2="0" y2="1"><stop stop-color="#20ffc2"/><stop offset="1" stop-color="#00b98a"/></linearGradient><linearGradient id="red" x2="0" y2="1"><stop stop-color="#ff426b"/><stop offset="1" stop-color="#e90040"/></linearGradient><filter id="glow" x="-40%" y="-40%" width="180%" height="180%"><feGaussianBlur stdDeviation="3" result="blur"/><feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge></filter></defs><path d="M256 82 L389 159 Q400 166 400 181 V326 Q400 341 387 349 L269 418 Q256 426 243 418 L125 349 Q112 341 112 326 V181 Q112 166 125 159 Z" fill="none" stroke="url(#white)" stroke-width="17" stroke-linejoin="round" stroke-linecap="round" stroke-dasharray="1000" stroke-dashoffset="1000" pathLength="1000" filter="url(#glow)"><animate attributeName="stroke-dashoffset" from="1000" to="0" dur="1s" fill="freeze"/></path><g filter="url(#glow)"><g><animateTransform attributeName="transform" type="translate" values="0,-100;0,8;0,-3;0,0" keyTimes="0;0.72;0.9;1" dur=".7s" begin=".65s" fill="freeze"/><g fill="url(#green)"><rect x="153" y="221" width="8" height="120" rx="4"/><rect x="141" y="241" width="32" height="94" rx="5"/></g></g><g><animateTransform attributeName="transform" type="translate" values="0,-130;0,10;0,-3;0,0" keyTimes="0;0.72;0.9;1" dur=".72s" begin=".82s" fill="freeze"/><g fill="url(#red)"><rect x="197" y="179" width="8" height="159" rx="4"/><rect x="185" y="207" width="32" height="117" rx="5"/></g></g><g><animateTransform attributeName="transform" type="translate" values="0,-115;0,9;0,-3;0,0" keyTimes="0;0.72;0.9;1" dur=".7s" begin=".99s" fill="freeze"/><g fill="url(#green)"><rect x="351" y="221" width="8" height="120" rx="4"/><rect x="339" y="241" width="32" height="94" rx="5"/></g></g><g><animateTransform attributeName="transform" type="translate" values="0,-145;0,10;0,-3;0,0" keyTimes="0;0.72;0.9;1" dur=".74s" begin="1.16s" fill="freeze"/><g fill="url(#red)"><rect x="307" y="179" width="8" height="159" rx="4"/><rect x="295" y="207" width="32" height="117" rx="5"/></g></g></g><path d="M256 139 L276 158 V351 L256 369 L236 351 V158 Z" fill="url(#white)" opacity="0" filter="url(#glow)"><animate attributeName="opacity" values="0;1" dur=".45s" begin="1.45s" fill="freeze"/><animateTransform attributeName="transform" type="translate" values="0,-45;0,5;0,-2;0,0" keyTimes="0;0.72;0.9;1" dur=".6s" begin="1.45s" fill="freeze"/></path></svg>"""

private enum class DrawingKind(val title:String,val anchors:Int) {
    HORIZONTAL("خط أفقي",1),VERTICAL("خط رأسي",1),RAY("شعاع",2),
    RETRACEMENT("ارتدادات فيبوناتشي",2),FAN("مروحة فيبوناتشي",2),
    TREND("خط الاتجاه",2),CHANNEL("قناة موازية",3),RECTANGLE("مستطيل",2),PITCHFORK("شوكة أندروز",3)
}
private data class DrawingAnchor(val time:Long,val price:Double)
private data class DrawingLevel(val ratio:Double,val enabled:Boolean=true,val color:Long=0xFF70C8F1L)
private data class ChartDrawing(
    val id:Long,val symbol:String,val kind:DrawingKind,val points:List<DrawingAnchor>,
    val name:String=kind.title,val color:Long=0xFF70C8F1L,val width:Float=1.5f,
    val dash:Int=0,val fill:Boolean=false,val opacity:Float=.12f,
    val extendLeft:Boolean=false,val extendRight:Boolean=false,val labels:Boolean=true,
    val prices:Boolean=true,val locked:Boolean=false,val visible:Boolean=true,
    val snap:Boolean=false,val frameOnly:String?=null,val reverse:Boolean=false,
    val levels:List<DrawingLevel> = when(kind) {
        DrawingKind.RETRACEMENT->listOf(0.0,.236,.382,.5,.618,.786,1.0).map { DrawingLevel(it) }
        DrawingKind.FAN->listOf(.382,.5,.618).map { DrawingLevel(it) }
        else->emptyList()
    }
)
private data class DrawingRequest(val id:Long,val kind:DrawingKind,val symbol:String)
private object ChartDrawings {
    val objects=mutableStateMapOf<Long,ChartDrawing>()
    var selected by mutableStateOf<Long?>(null)
    var editing by mutableStateOf<Long?>(null)
    var managerOpen by mutableStateOf(false)
    var pending by mutableStateOf<DrawingRequest?>(null)
    var loaded=false
    fun request(kind:DrawingKind,symbol:String) { pending=DrawingRequest(nextId(),kind,symbol) }
    fun remove(id:Long) { objects.remove(id);if(selected==id)selected=null;if(editing==id)editing=null }
    fun duplicate(d:ChartDrawing) {
        val n=d.copy(id=nextId(),name=d.name+" — نسخة",locked=false,visible=true)
        objects[n.id]=n;selected=n.id
    }
    fun visible(symbol:String,frame:String)=objects.values.filter { it.symbol==symbol&&it.visible&&(it.frameOnly==null||it.frameOnly==frame) }.sortedBy { it.id }
}
private val DrawingPalette=listOf(0xFF70C8F1L,0xFFFFFFFFL,0xFFFFD34DL,0xFF39CF8AL,0xFFFF667AL,0xFFB38CFFL,0xFFFF963DL,0xFF4192FFL)
private fun drawingJson(d:ChartDrawing):JSONObject {
    val points=JSONArray();d.points.forEach { points.put(JSONObject().put("time",it.time).put("price",it.price)) }
    val levels=JSONArray();d.levels.forEach { levels.put(JSONObject().put("ratio",it.ratio).put("enabled",it.enabled).put("color",it.color)) }
    return JSONObject().put("id",d.id).put("symbol",d.symbol).put("kind",d.kind.name).put("points",points)
        .put("name",d.name).put("color",d.color).put("width",d.width.toDouble()).put("dash",d.dash)
        .put("fill",d.fill).put("opacity",d.opacity.toDouble()).put("left",d.extendLeft).put("right",d.extendRight)
        .put("labels",d.labels).put("prices",d.prices).put("locked",d.locked).put("visible",d.visible)
        .put("snap",d.snap).put("frame",d.frameOnly ?: "").put("reverse",d.reverse).put("levels",levels)
}
private fun readDrawing(j:JSONObject):ChartDrawing? = try {
    val kind=DrawingKind.valueOf(j.getString("kind"));val a=j.getJSONArray("points")
    val points=(0 until a.length()).map { val p=a.getJSONObject(it);DrawingAnchor(p.getLong("time"),p.getDouble("price")) }
    val symbol=j.getString("symbol")
    if(symbol.isBlank()||!validDrawingPoints(kind,points))null
    else {
        val initial=ChartDrawing(j.getLong("id"),symbol,kind,points)
        val lv=j.optJSONArray("levels")
        val levels=if(lv==null)initial.levels else (0 until min(lv.length(),24)).mapNotNull { i->
            val l=lv.optJSONObject(i) ?: return@mapNotNull null;val r=l.optDouble("ratio",Double.NaN)
            if(!r.isFinite()||r !in -10.0..10.0)null else DrawingLevel(r,l.optBoolean("enabled",true),l.optLong("color",initial.color))
        }
        initial.copy(name=j.optString("name",kind.title).take(80),color=j.optLong("color",initial.color),
            width=j.optDouble("width",1.5).toFloat().takeIf { it.isFinite()&&it in .5f..6f } ?: 1.5f,
            dash=j.optInt("dash",0).coerceIn(0,2),fill=j.optBoolean("fill",false),
            opacity=j.optDouble("opacity",.12).toFloat().takeIf { it.isFinite()&&it in 0f..1f } ?: .12f,
            extendLeft=j.optBoolean("left",false),extendRight=j.optBoolean("right",false),
            labels=j.optBoolean("labels",true),prices=j.optBoolean("prices",true),locked=j.optBoolean("locked",false),
            visible=j.optBoolean("visible",true),snap=j.optBoolean("snap",false),
            frameOnly=j.optString("frame","").takeIf { it in ChartTimeframes },reverse=j.optBoolean("reverse",false),levels=levels)
    }
} catch(_:Exception) { null }
@Composable private fun ChartDrawingPreferences() {
    val context=LocalContext.current.applicationContext
    val prefs=remember(context) { context.getSharedPreferences("chart_drawings_v1",Context.MODE_PRIVATE) }
    fun save() {
        val array=JSONArray();ChartDrawings.objects.values.sortedBy { it.id }.forEach { array.put(drawingJson(it)) }
        prefs.edit().putString("objects",array.toString()).apply()
    }
    DisposableEffect(prefs) {
        if(!ChartDrawings.loaded) {
            try {
                val list=JSONArray(prefs.getString("objects","[]") ?: "[]")
                for(i in 0 until list.length())list.optJSONObject(i)?.let { readDrawing(it) }?.let { d->
                    ChartDrawings.objects[d.id]=d
                    if(d.id>ids.get())ids.set(d.id)
                }
            } catch(_:Exception) { }
            ChartDrawings.loaded=true
        }
        onDispose { save() }
    }
    LaunchedEffect(prefs) {
        snapshotFlow { ChartDrawings.objects.toMap() }.collectLatest { delay(220);save() }
    }
}
private data class DrawingProjection(val view:ChartViewport,val origin:Long,val interval:Long,val width:Float,val height:Float) {
    fun screen(p:DrawingAnchor)=Offset((view.fractionFor((p.time-origin).toDouble()/interval)*width).toFloat(),((view.maxPrice-p.price)/view.priceSpan*height).toFloat())
    fun anchor(p:Offset):DrawingAnchor {
        val bar=view.anchorBar+(p.x/width.coerceAtLeast(1f)-view.anchorFraction)*view.visibleBars
        val time=(origin+bar*interval).coerceIn(0.0,32_503_680_000_000.0).roundToLong()
        return DrawingAnchor(time,(view.maxPrice-p.y/height.coerceAtLeast(1f)*view.priceSpan).coerceAtLeast(1e-12))
    }
}
private data class DrawingSegment(val a:Offset,val b:Offset,val color:Long,val guide:Boolean=false)
private data class DrawingCaption(val at:Offset,val text:String,val color:Long)
private data class DrawingGeometry(val lines:List<DrawingSegment>,val areas:List<List<Offset>>,val captions:List<DrawingCaption>,val handles:List<Offset>)
private fun drawingClip(a:Offset,b:Offset,w:Float,h:Float,low:Double=0.0,high:Double=1.0):Pair<Offset,Offset>? {
    if(!a.x.isFinite()||!a.y.isFinite()||!b.x.isFinite()||!b.y.isFinite()||w<=0||h<=0)return null
    val dx=(b.x-a.x).toDouble();val dy=(b.y-a.y).toDouble()
    if(abs(dx)+abs(dy)<1e-7)return null
    var lo=low;var hi=high
    fun slab(start:Double,delta:Double,limit:Double):Boolean {
        if(abs(delta)<1e-10)return start in 0.0..limit
        var x=-start/delta;var y=(limit-start)/delta
        if(x>y) { val t=x;x=y;y=t }
        lo=max(lo,x);hi=min(hi,y);return lo<=hi
    }
    if(!slab(a.x.toDouble(),dx,w.toDouble())||!slab(a.y.toDouble(),dy,h.toDouble()))return null
    if(!lo.isFinite()||!hi.isFinite())return null
    return Offset((a.x+dx*lo).toFloat(),(a.y+dy*lo).toFloat()) to Offset((a.x+dx*hi).toFloat(),(a.y+dy*hi).toFloat())
}
private fun drawingGeometry(d:ChartDrawing,p:DrawingProjection):DrawingGeometry {
    val points=d.points.map { p.screen(it) };val a=points[0];val b=points.getOrElse(1) { a }
    val lines=mutableListOf<DrawingSegment>();val areas=mutableListOf<List<Offset>>();val text=mutableListOf<DrawingCaption>()
    val lo=if(d.extendLeft)Double.NEGATIVE_INFINITY else 0.0;val hi=if(d.extendRight)Double.POSITIVE_INFINITY else 1.0
    fun line(x:Offset,y:Offset,l:Double=0.0,r:Double=1.0,color:Long=d.color,guide:Boolean=false) {
        drawingClip(x,y,p.width,p.height,l,r)?.let { lines.add(DrawingSegment(it.first,it.second,color,guide)) }
    }
    fun caption(at:Offset,label:String,color:Long=d.color) {
        if(at.x.isFinite()&&at.y.isFinite()&&at.y in 0f..p.height)text.add(DrawingCaption(Offset(at.x.coerceIn(4f,max(4f,p.width-4f)),at.y),label,color))
    }
    fun price(value:Double)=priceText(value,priceDecimals(nicePriceStep(p.view.priceSpan/5)))
    when(d.kind) {
        DrawingKind.HORIZONTAL->{
            line(Offset(0f,a.y),Offset(p.width,a.y))
            if(d.labels||d.prices)caption(Offset(8f,a.y),listOfNotNull(d.name.takeIf { d.labels },price(d.points[0].price).takeIf { d.prices }).joinToString("  "))
        }
        DrawingKind.VERTICAL->{
            line(Offset(a.x,0f),Offset(a.x,p.height))
            if(d.labels)caption(Offset(a.x+6f,p.height-24f),candleTimeText(d.points[0].time,p.interval))
        }
        DrawingKind.RAY->line(a,b,0.0,Double.POSITIVE_INFINITY)
        DrawingKind.TREND->line(a,b,lo,hi)
        DrawingKind.RECTANGLE->{
            val left=if(d.extendLeft)0f else min(a.x,b.x);val right=if(d.extendRight)p.width else max(a.x,b.x)
            val top=min(a.y,b.y);val bottom=max(a.y,b.y)
            val corners=listOf(Offset(left,top),Offset(right,top),Offset(right,bottom),Offset(left,bottom))
            if(d.fill)areas.add(corners)
            corners.indices.forEach { line(corners[it],corners[(it+1)%4]) }
        }
        DrawingKind.RETRACEMENT->{
            line(a,b,guide=true)
            val left=if(d.extendLeft)0f else min(a.x,b.x);val right=if(d.extendRight)p.width else max(a.x,b.x)
            val levels=d.levels.filter { it.enabled }.map { level->
                val ratio=if(d.reverse)1.0-level.ratio else level.ratio
                val value=d.points[1].price+(d.points[0].price-d.points[1].price)*ratio
                val y=p.screen(DrawingAnchor(d.points[1].time,value)).y
                line(Offset(left,y),Offset(right,y),color=level.color)
                if(d.labels||d.prices)caption(Offset(left+5f,y),listOfNotNull((shortAmount(level.ratio*100)+"%").takeIf { d.labels },price(value).takeIf { d.prices }).joinToString("  "),level.color)
                y
            }.sorted()
            if(d.fill)levels.zipWithNext().forEach { (y1,y2)->areas.add(listOf(Offset(left,y1),Offset(right,y1),Offset(right,y2),Offset(left,y2))) }
        }
        DrawingKind.FAN->{
            line(a,b,guide=true)
            d.levels.filter { it.enabled }.forEach { level->
                val r=if(d.reverse)level.ratio else 1.0-level.ratio
                val end=Offset(b.x,a.y+(b.y-a.y)*r.toFloat())
                line(a,end,if(d.extendLeft)Double.NEGATIVE_INFINITY else 0.0,Double.POSITIVE_INFINITY,color=level.color)
                if(d.labels)caption(end,shortAmount(level.ratio*100)+"%",level.color)
            }
            if(d.fill) {
                val active=d.levels.filter { it.enabled }.map { if(d.reverse)it.ratio else 1-it.ratio }.sorted()
                if(active.size>=2) {
                    val dx=b.x-a.x
                    if(abs(dx)>.001f) {
                        val edge=if(dx>0)p.width else 0f;val t=(edge-a.x)/dx
                        areas.add(listOf(a,Offset(edge,a.y+(b.y-a.y)*active.first().toFloat()*t),Offset(edge,a.y+(b.y-a.y)*active.last().toFloat()*t)))
                    }
                }
            }
        }
        DrawingKind.CHANNEL->{
            val c=points[2];val dx=b.x-a.x
            val shift=if(abs(dx)>.001f)Offset(0f,c.y-a.y-(b.y-a.y)*(c.x-a.x)/dx) else Offset(c.x-a.x,0f)
            val a2=a+shift;val b2=b+shift
            line(a,b,lo,hi);line(a2,b2,lo,hi);line(a+shift*.5f,b+shift*.5f,lo,hi,guide=true)
            if(d.fill) {
                val first=drawingClip(a,b,p.width,p.height,lo,hi)
                val second=drawingClip(a2,b2,p.width,p.height,lo,hi)
                if(abs(dx)>.001f) {
                    val left=if(d.extendLeft&&dx>0||d.extendRight&&dx<0)0f else min(a.x,b.x)
                    val right=if(d.extendRight&&dx>0||d.extendLeft&&dx<0)p.width else max(a.x,b.x)
                    fun at(x:Float)=Offset(x,a.y+(b.y-a.y)*(x-a.x)/dx)
                    areas.add(listOf(at(left),at(right),at(right)+shift,at(left)+shift))
                } else if(first!=null&&second!=null)areas.add(listOf(first.first,first.second,second.second,second.first))
            }
        }
        DrawingKind.PITCHFORK->{
            val c=points[2];val middle=(b+c)*.5f;val direction=middle-a
            line(b,c,guide=true)
            val l=if(d.extendLeft)Double.NEGATIVE_INFINITY else 0.0
            val r=if(d.extendRight)Double.POSITIVE_INFINITY else 1.0
            line(a,middle,l,r);line(b,b+direction,l,r);line(c,c+direction,l,r)
            if(d.fill) {
                val end=if(d.extendRight) {
                    val length=max(p.width,p.height)*4f
                    direction*(length/direction.getDistance().coerceAtLeast(1f))
                } else direction
                val start=if(d.extendLeft)-end else Offset.Zero
                areas.add(listOf(b+start,b+end,c+end,c+start))
            }
        }
    }
    if(d.labels&&d.kind in listOf(DrawingKind.RAY,DrawingKind.TREND,DrawingKind.CHANNEL,DrawingKind.RECTANGLE,DrawingKind.PITCHFORK))caption(a+Offset(7f,-10f),d.name)
    return DrawingGeometry(lines,areas,text,points)
}
private fun drawingDistance(point:Offset,a:Offset,b:Offset):Float {
    val v=b-a;val length=v.x*v.x+v.y*v.y
    val t=if(length<.0001f)0f else (((point.x-a.x)*v.x+(point.y-a.y)*v.y)/length).coerceIn(0f,1f)
    return (point-(a+v*t)).getDistance()
}
private data class DrawingHit(val id:Long,val handle:Int=-1)
private fun drawingHit(position:Offset,projection:DrawingProjection,objects:List<ChartDrawing>,tolerance:Float):DrawingHit? {
    val selected=objects.firstOrNull { it.id==ChartDrawings.selected }
    if(selected!=null&&!selected.locked) {
        val handles=selected.points.map { projection.screen(it) }
        handles.indices.minByOrNull { (handles[it]-position).getDistance() }?.let { index->
            if((handles[index]-position).getDistance()<=tolerance*1.35f)return DrawingHit(selected.id,index)
        }
    }
    val ordered=objects.asReversed()
    return ordered.firstNotNullOfOrNull { d->
        val g=drawingGeometry(d,projection)
        if(g.lines.any { drawingDistance(position,it.a,it.b)<=tolerance })DrawingHit(d.id) else null
    }
}
private fun DrawScope.drawChartDrawings(objects:List<ChartDrawing>,projection:DrawingProjection) {
    clipRect(0f,0f,size.width,size.height) {
        objects.forEach { d->
            val g=drawingGeometry(d,projection);val color=Color(d.color)
            val effect=when(d.dash) { 1->PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(),5.dp.toPx()));2->PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(),4.dp.toPx()));else->null }
            g.areas.forEach { points->
                if(points.size>=3&&points.all { it.x.isFinite()&&it.y.isFinite() }) {
                    val path=Path().apply { moveTo(points[0].x,points[0].y);points.drop(1).forEach { lineTo(it.x,it.y) };close() }
                    drawPath(path,color.copy(alpha=d.opacity))
                }
            }
            g.lines.forEach { segment->
                drawLine(Color(segment.color).copy(alpha=if(segment.guide).55f else 1f),segment.a,segment.b,
                    (if(segment.guide)min(1f,d.width) else d.width).dp.toPx(),StrokeCap.Butt,
                    pathEffect=if(segment.guide)PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),4.dp.toPx())) else effect)
            }
            val paint=NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { textSize=10.sp.toPx();typeface=Typeface.DEFAULT;textAlign=NativePaint.Align.LEFT }
            g.captions.forEach { caption->
                paint.color=Color(caption.color).toArgb()
                val textWidth=paint.measureText(caption.text)
                val x=caption.at.x.coerceIn(3.dp.toPx(),max(3.dp.toPx(),size.width-textWidth-3.dp.toPx()))
                val y=(caption.at.y-4.dp.toPx()).coerceIn(paint.textSize,size.height.coerceAtLeast(paint.textSize))
                drawContext.canvas.nativeCanvas.drawText(caption.text,x,y,paint)
            }
            if(ChartDrawings.selected==d.id&&!d.locked)g.handles.forEach { point->
                if(point.x in 0f..size.width&&point.y in 0f..size.height) {
                    drawCircle(Color(0xFF222E43),6.dp.toPx(),point)
                    drawCircle(color,6.dp.toPx(),point,style=Stroke(1.7.dp.toPx()))
                }
            }
        }
    }
}
private fun drawingSnap(point:DrawingAnchor,bars:List<Candle>):DrawingAnchor {
    val nearest=bars.minByOrNull { abs(it.time-point.time) } ?: return point
    val price=listOf(nearest.open,nearest.high,nearest.low,nearest.close).minByOrNull { abs(it-point.price) }!!.toDouble()
    return DrawingAnchor(nearest.time,price)
}
private fun validDrawingPoints(kind:DrawingKind,points:List<DrawingAnchor>):Boolean {
    if(points.size!=kind.anchors||points.any { it.time !in 0L..32_503_680_000_000L||!it.price.isFinite()||it.price<=0.0 })return false
    if(points.size<2)return true
    val a=points[0];val b=points[1]
    return when(kind) {
        DrawingKind.FAN,DrawingKind.CHANNEL->a.time!=b.time
        DrawingKind.TREND,DrawingKind.RAY->a!=b
        DrawingKind.RECTANGLE->a.time!=b.time&&a.price!=b.price
        DrawingKind.PITCHFORK->{
            val c=points[2]
            b!=c&&(abs(a.time.toDouble()-(b.time.toDouble()+c.time)/2.0)>.5||abs(a.price-(b.price+c.price)/2)>1e-12)
        }
        else->true
    }
}
private data class DrawingDrag(val original:ChartDrawing,val handle:Int,val projection:DrawingProjection) {
    fun moved(delta:Offset,bars:List<Candle>):ChartDrawing {
        if(original.locked||!delta.x.isFinite()||!delta.y.isFinite())return original
        if(handle<0) {
            val dt=(delta.x/projection.width.coerceAtLeast(1f)*projection.view.visibleBars*projection.interval).roundToLong()
                .coerceIn(-original.points.minOf { it.time },32_503_680_000_000L-original.points.maxOf { it.time })
            val dp=(-delta.y/projection.height.coerceAtLeast(1f)*projection.view.priceSpan).coerceAtLeast(1e-12-original.points.minOf { it.price })
            return original.copy(points=original.points.map { DrawingAnchor(it.time+dt,it.price+dp) })
        }
        val points=original.points.mapIndexed { index,anchor->
            if(handle>=0&&index!=handle)anchor else {
                val shifted=projection.anchor(projection.screen(anchor)+delta)
                if(original.snap&&handle>=0)drawingSnap(shifted,bars) else shifted
            }
        }
        return if(validDrawingPoints(original.kind,points))original.copy(points=points) else original
    }
}
private fun createChartDrawing(request:DrawingRequest,projection:DrawingProjection):ChartDrawing {
    fun at(x:Float,y:Float)=projection.anchor(Offset(projection.width*x,projection.height*y))
    val a=at(.25f,.68f);val b=at(.64f,.40f);val c=at(.62f,.72f)
    val points=when(request.kind) {
        DrawingKind.HORIZONTAL->listOf(at(.50f,.54f))
        DrawingKind.VERTICAL->listOf(at(.46f,.55f))
        DrawingKind.PITCHFORK->listOf(at(.22f,.68f),at(.48f,.32f),at(.65f,.62f))
        else->listOf(a,b,c).take(request.kind.anchors)
    }
    return ChartDrawing(nextId(),request.symbol,request.kind,points,
        extendRight=request.kind in listOf(DrawingKind.RETRACEMENT,DrawingKind.CHANNEL,DrawingKind.PITCHFORK),
        fill=request.kind==DrawingKind.RECTANGLE)
}
@Composable private fun DrawingGlyph(kind:DrawingKind,modifier:Modifier=Modifier,color:Color=Color.White) {
    Canvas(modifier) {
        val w=size.width;val h=size.height;val stroke=1.5.dp.toPx()
        fun point(x:Float,y:Float)=Offset(w*x,h*y)
        fun line(x1:Float,y1:Float,x2:Float,y2:Float)=drawLine(color,point(x1,y1),point(x2,y2),stroke,StrokeCap.Round)
        fun dot(x:Float,y:Float)=drawCircle(color,1.9.dp.toPx(),point(x,y))
        when(kind) {
            DrawingKind.HORIZONTAL->{line(.15f,.5f,.85f,.5f);dot(.15f,.5f);dot(.85f,.5f)}
            DrawingKind.VERTICAL->{line(.5f,.12f,.5f,.88f);dot(.5f,.12f);dot(.5f,.88f)}
            DrawingKind.RAY->{line(.16f,.84f,.90f,.10f);dot(.16f,.84f)}
            DrawingKind.TREND->{line(.16f,.84f,.84f,.16f);dot(.16f,.84f);dot(.84f,.16f)}
            DrawingKind.RETRACEMENT->{listOf(.15f,.40f,.65f,.9f).forEach { line(.1f,it,.9f,it) };line(.15f,.9f,.85f,.15f);dot(.15f,.9f);dot(.85f,.15f)}
            DrawingKind.FAN->{listOf(Offset(.18f,.12f),Offset(.55f,.18f),Offset(.85f,.45f),Offset(.90f,.85f)).forEach { line(.18f,.85f,it.x,it.y) };dot(.18f,.85f)}
            DrawingKind.CHANNEL->{line(.1f,.55f,.85f,.10f);line(.15f,.9f,.90f,.45f);dot(.1f,.55f);dot(.90f,.45f)}
            DrawingKind.RECTANGLE->{line(.15f,.15f,.85f,.15f);line(.85f,.15f,.85f,.85f);line(.85f,.85f,.15f,.85f);line(.15f,.85f,.15f,.15f);dot(.15f,.85f);dot(.85f,.15f)}
            DrawingKind.PITCHFORK->{line(.12f,.70f,.70f,.12f);line(.30f,.88f,.88f,.30f);line(.12f,.88f,.85f,.15f);dot(.12f,.88f)}
        }
    }
}
@Composable private fun DrawingsDialog(onDismiss:()->Unit) {
    val symbol by TradeStore.currentSymbol.collectAsState()
    val configuration=LocalConfiguration.current
    val available=ChartDrawings.objects.values.count { it.symbol==symbol }
    val rowHeight=(configuration.screenWidthDp*.082f).dp.coerceIn(30.dp,48.dp)
    val headerHeight=(configuration.screenWidthDp*.15f).dp.coerceIn(52.dp,70.dp)
    val itemFont=(configuration.screenWidthDp*.035f).coerceIn(14f,18f).sp
    val glyphSize=(configuration.screenWidthDp*.036f).dp.coerceIn(16.dp,24.dp)
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            val shape=RoundedCornerShape(14.dp)
            Column(Modifier.fillMaxWidth(.89f).heightIn(max=(configuration.screenHeightDp*.82f).dp)
                .clip(shape).background(Color(0xFF222E43)).border(1.dp,Color(0xFF38485E),shape)) {
                Row(Modifier.fillMaxWidth().height(headerHeight).padding(start=21.dp,end=14.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("الرسومات",color=Color(0xFFF1F4F8),fontSize=15.sp,modifier=Modifier.weight(1f),style=TextStyle(textDirection=TextDirection.Rtl))
                    if(available>0)DrawingButton("إدارة ($available)") { onDismiss();ChartDrawings.managerOpen=true }
                    Box(Modifier.size(34.dp).clickable(onClick=onDismiss),contentAlignment=Alignment.Center) { ReferenceGlyph("close",Modifier.size(24.dp),Color(0xFFA8C0D2)) }
                }
                Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()).padding(top=10.dp,bottom=7.dp)) {
                    DrawingKind.values().forEach { kind->
                        Row(Modifier.fillMaxWidth().height(rowHeight).clickable { ChartDrawings.request(kind,symbol);onDismiss() }.padding(horizontal=23.dp),verticalAlignment=Alignment.CenterVertically) {
                            DrawingGlyph(kind,Modifier.size(glyphSize))
                            Spacer(Modifier.width(16.dp))
                            Text(kind.title,color=Color(0xFFF1F4F8),fontSize=itemFont,style=TextStyle(textDirection=TextDirection.Rtl),maxLines=1)
                        }
                    }
                }
            }
        }
    }
}
@Composable private fun DrawingButton(title:String,active:Boolean=false,danger:Boolean=false,onClick:()->Unit) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(if(active)Color(0xFF385875) else if(danger)Color(0xFF743447) else Color(0xFF2C3C53))
        .clickable(onClick=onClick).padding(horizontal=10.dp,vertical=9.dp),contentAlignment=Alignment.Center) {
        Text(title,color=Color.White,fontSize=12.sp,maxLines=1,style=TextStyle(textDirection=TextDirection.Content))
    }
}
@Composable private fun DrawingToolbar(symbol:String,frame:String,modifier:Modifier=Modifier) {
    val d=ChartDrawings.selected?.let { ChartDrawings.objects[it] }?.takeIf { it.symbol==symbol&&it.visible&&(it.frameOnly==null||it.frameOnly==frame) } ?: return
    Row(modifier.clip(RoundedCornerShape(9.dp)).background(Color(0xF2222E43)).border(1.dp,Color(0xFF52667F),RoundedCornerShape(9.dp))
        .horizontalScroll(rememberScrollState()).padding(5.dp),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(d.name,color=Color.White,fontSize=11.sp,modifier=Modifier.padding(horizontal=6.dp))
        DrawingButton("إعدادات") { ChartDrawings.editing=d.id }
        DrawingButton(if(d.locked)"فتح القفل" else "قفل",d.locked) { ChartDrawings.objects[d.id]=d.copy(locked=!d.locked) }
        DrawingButton("نسخ") { ChartDrawings.duplicate(d) }
        DrawingButton("إخفاء") { ChartDrawings.objects[d.id]=d.copy(visible=false);ChartDrawings.selected=null }
        DrawingButton("حذف",danger=true) { ChartDrawings.remove(d.id) }
        DrawingButton("الكل") { ChartDrawings.managerOpen=true }
        DrawingButton("×") { ChartDrawings.selected=null }
    }
}
@Composable private fun DrawingManagerDialog(symbol:String,onDismiss:()->Unit) {
    val objects=ChartDrawings.objects.values.filter { it.symbol==symbol }.sortedByDescending { it.id }
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(Modifier.fillMaxWidth(.93f).heightIn(max=(LocalConfiguration.current.screenHeightDp*.78f).dp)
                .clip(RoundedCornerShape(14.dp)).background(Color(0xFF222E43)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("إدارة الرسومات — $symbol",color=Color.White,fontSize=16.sp,modifier=Modifier.weight(1f))
                    DrawingButton("إغلاق",onClick=onDismiss)
                }
                if(objects.isEmpty())Text("لا توجد رسومات لهذا الزوج",color=C.TextSecondary,fontSize=14.sp)
                Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    objects.forEach { d->
                        Column(Modifier.fillMaxWidth().background(Color(0xFF293A51),RoundedCornerShape(8.dp)).padding(9.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                            Text(d.name+(if(d.locked)" • مقفل" else "")+(d.frameOnly?.let { " • $it" } ?: ""),color=Color(d.color),fontSize=14.sp)
                            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                                DrawingButton("إعدادات") { ChartDrawings.editing=d.id;onDismiss() }
                                DrawingButton(if(d.visible)"إخفاء" else "إظهار") { ChartDrawings.objects[d.id]=d.copy(visible=!d.visible) }
                                DrawingButton("تحديد") { ChartDrawings.objects[d.id]=d.copy(visible=true,frameOnly=d.frameOnly?.let { ChartSettings.frame });ChartDrawings.selected=d.id;onDismiss() }
                                DrawingButton("حذف",danger=true) { ChartDrawings.remove(d.id) }
                            }
                        }
                    }
                }
            }
        }
    }
}
private fun drawingNumber(text:String):Double? {
    val normalized=buildString { text.trim().forEach { c->append(when { c==','||c=='،'||c=='٫'->'.';Character.digit(c,10)>=0->('0'.code+Character.digit(c,10)).toChar();else->c }) } }
    return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
}
private fun drawingDateFormat()=SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).apply { timeZone=TimeZone.getTimeZone("GMT+3");isLenient=false }
private fun drawingDate(text:String):Long? = try {
    val value=text.trim();val position=java.text.ParsePosition(0);val date=drawingDateFormat().parse(value,position)
    date?.time?.takeIf { position.index==value.length&&it in 0L..32_503_680_000_000L }
} catch(_:Exception) { null }
@Composable private fun DrawingField(label:String,value:String,numeric:Boolean=false,onChange:(String)->Unit) {
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(label,color=Color(0xFFBCCBDD),fontSize=12.sp)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            BasicTextField(value=value,onValueChange=onChange,singleLine=true,textStyle=TextStyle(color=Color.White,fontSize=14.sp,textDirection=TextDirection.Content),
                cursorBrush=SolidColor(C.Cyan),keyboardOptions=KeyboardOptions(keyboardType=if(numeric)KeyboardType.Decimal else KeyboardType.Text),
                modifier=Modifier.fillMaxWidth().background(Color(0xFF172336),RoundedCornerShape(6.dp)).border(1.dp,Color(0xFF475B73),RoundedCornerShape(6.dp)).padding(10.dp))
        }
    }
}
@Composable private fun DrawingToggle(label:String,checked:Boolean,onToggle:()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onToggle).padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(label,color=Color.White,fontSize=13.sp,modifier=Modifier.weight(1f))
        PlatformSwitch(checked,onToggle)
    }
}
@Composable private fun DrawingSettingsDialog(original:ChartDrawing,onDismiss:()->Unit) {
    var draft by remember(original.id) { mutableStateOf(original) }
    var tab by remember(original.id) { mutableIntStateOf(0) }
    var error by remember(original.id) { mutableStateOf("") }
    val prices=remember(original.id) { mutableStateListOf<String>().apply { addAll(original.points.map { it.price.toString() }) } }
    val times=remember(original.id) { mutableStateListOf<String>().apply { addAll(original.points.map { drawingDateFormat().format(Date(it.time)) }) } }
    val ratios=remember(original.id) { mutableStateListOf<String>().apply { addAll(original.levels.map { it.ratio.toString() }) } }
    val hasLevels=original.kind==DrawingKind.RETRACEMENT||original.kind==DrawingKind.FAN
    val supportsExtension=original.kind in listOf(DrawingKind.TREND,DrawingKind.CHANNEL,DrawingKind.RETRACEMENT,DrawingKind.PITCHFORK,DrawingKind.RECTANGLE)
    val supportsFill=original.kind in listOf(DrawingKind.CHANNEL,DrawingKind.RETRACEMENT,DrawingKind.FAN,DrawingKind.PITCHFORK,DrawingKind.RECTANGLE)
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(Modifier.fillMaxWidth(.94f).heightIn(max=(LocalConfiguration.current.screenHeightDp*.87f).dp)
                .clip(RoundedCornerShape(14.dp)).background(Color(0xFF222E43)).padding(15.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("إعدادات ${original.kind.title}",color=Color.White,fontSize=16.sp,modifier=Modifier.weight(1f))
                    DrawingButton("×",onClick=onDismiss)
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    listOf("النمط","الإحداثيات","المستويات","الظهور").forEachIndexed { index,label->
                        if(index!=2||hasLevels)DrawingButton(label,tab==index) { tab=index;error="" }
                    }
                }
                Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    when(tab) {
                        0->{
                            DrawingField("الاسم",draft.name) { draft=draft.copy(name=it.take(80)) }
                            Text("لون الرسم",color=Color.White,fontSize=13.sp)
                            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                                DrawingPalette.forEach { color->Box(Modifier.size(27.dp).clip(CircleShape).background(Color(color))
                                    .border(if(draft.color==color)3.dp else 1.dp,if(draft.color==color)Color.White else Color.Transparent,CircleShape)
                                    .clickable { draft=draft.copy(color=color,levels=draft.levels.map { it.copy(color=color) }) }) }
                            }
                            Text("سمك الخط",color=Color.White,fontSize=13.sp)
                            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) { listOf(1f,1.5f,2f,3f,4f).forEach { width->DrawingButton(width.toString(),draft.width==width) { draft=draft.copy(width=width) } } }
                            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) { listOf("متصل","متقطع","منقط").forEachIndexed { i,label->DrawingButton(label,draft.dash==i) { draft=draft.copy(dash=i) } } }
                            DrawingToggle("إظهار التسميات",draft.labels) { draft=draft.copy(labels=!draft.labels) }
                            if(original.kind==DrawingKind.HORIZONTAL||original.kind==DrawingKind.RETRACEMENT)DrawingToggle("إظهار الأسعار",draft.prices) { draft=draft.copy(prices=!draft.prices) }
                            if(supportsExtension) {
                                DrawingToggle("امتداد إلى اليسار",draft.extendLeft) { draft=draft.copy(extendLeft=!draft.extendLeft) }
                                DrawingToggle("امتداد إلى اليمين",draft.extendRight) { draft=draft.copy(extendRight=!draft.extendRight) }
                            }
                            if(original.kind==DrawingKind.FAN)DrawingToggle("امتداد المروحة إلى الخلف",draft.extendLeft) { draft=draft.copy(extendLeft=!draft.extendLeft) }
                            if(hasLevels)DrawingToggle("عكس اتجاه فيبوناتشي",draft.reverse) { draft=draft.copy(reverse=!draft.reverse) }
                            if(supportsFill) {
                                DrawingToggle("تعبئة الخلفية",draft.fill) { draft=draft.copy(fill=!draft.fill) }
                                if(draft.fill) {
                                    Text("كثافة التعبئة",color=Color.White,fontSize=13.sp)
                                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)) { listOf(.05f,.12f,.25f,.4f).forEach { opacity->DrawingButton("${(opacity*100).roundToInt()}%",draft.opacity==opacity) { draft=draft.copy(opacity=opacity) } } }
                                }
                            }
                            DrawingToggle("قفل التحريك والمقابض",draft.locked) { draft=draft.copy(locked=!draft.locked) }
                            DrawingToggle("التقاط المقابض لأقرب شمعة وسعر OHLC",draft.snap) { draft=draft.copy(snap=!draft.snap) }
                        }
                        1->{
                            Text("إحداثيات ثابتة بالوقت والسعر • المنطقة الزمنية UTC+3",color=Color(0xFFBCCBDD),fontSize=12.sp)
                            original.points.indices.forEach { index->
                                Text("النقطة ${index+1}",color=C.Cyan,fontSize=14.sp)
                                if(original.kind!=DrawingKind.HORIZONTAL)DrawingField("الوقت: yyyy-MM-dd HH:mm:ss",times[index]) { times[index]=it;error="" }
                                if(original.kind!=DrawingKind.VERTICAL)DrawingField("السعر",prices[index],true) { prices[index]=it;error="" }
                            }
                            Text(when(original.kind) {
                                DrawingKind.CHANNEL->"النقطتان 1 و2 لخط الأساس، والنقطة 3 تحدد البعد الموازي."
                                DrawingKind.PITCHFORK->"النقطة 1 بداية الخط الأوسط، والنقطتان 2 و3 قمتا الشوكة؛ يمر الوسط بمنتصفهما."
                                DrawingKind.HORIZONTAL->"الخط يمتد عبر الشارت عند السعر المحدد."
                                DrawingKind.VERTICAL->"الخط يثبت عند الوقت المحدد."
                                DrawingKind.RAY->"الشعاع يبدأ من النقطة 1 ويمتد بلا نهاية باتجاه النقطة 2."
                                DrawingKind.RETRACEMENT->"0% عند نهاية الحركة (2)، و100% عند بدايتها (1)، ما لم يُفعّل العكس."
                                DrawingKind.FAN->"أشعة من النقطة 1 تمر بمستويات التصحيح عند زمن النقطة 2."
                                else->"اسحب جسم الرسم لنقله بالكامل أو المقابض لتعديل النقاط."
                            },color=Color(0xFFBCCBDD),fontSize=12.sp)
                        }
                        2->{
                            Text("النسبة كعدد عشري: 0.618 = 61.8%، و1.618 = 161.8%",color=Color(0xFFBCCBDD),fontSize=12.sp)
                            draft.levels.forEachIndexed { index,level->
                                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                    Box(Modifier.weight(1f)) { DrawingField("المستوى ${index+1}",ratios[index],true) { ratios[index]=it;error="" } }
                                    DrawingButton(if(level.enabled)"✓" else "—",level.enabled) { draft=draft.copy(levels=draft.levels.mapIndexed { i,l->if(i==index)l.copy(enabled=!l.enabled) else l }) }
                                    Box(Modifier.size(24.dp).clip(CircleShape).background(Color(level.color)).clickable {
                                        val color=DrawingPalette[(DrawingPalette.indexOf(level.color)+1).mod(DrawingPalette.size)]
                                        draft=draft.copy(levels=draft.levels.mapIndexed { i,l->if(i==index)l.copy(color=color) else l })
                                    })
                                    DrawingButton("×") { ratios.removeAt(index);draft=draft.copy(levels=draft.levels.filterIndexed { i,_->i!=index }) }
                                }
                            }
                            if(draft.levels.size<24)DrawingButton("إضافة مستوى") { ratios.add("1.618");draft=draft.copy(levels=draft.levels+DrawingLevel(1.618,color=draft.color)) }
                        }
                        3->{
                            DrawingToggle("إظهار الرسم",draft.visible) { draft=draft.copy(visible=!draft.visible) }
                            DrawingToggle("إظهار على الفريم الحالي فقط (${ChartSettings.frame})",draft.frameOnly!=null) { draft=draft.copy(frameOnly=if(draft.frameOnly==null)ChartSettings.frame else null) }
                            Text("يُحفظ الرسم لهذا الزوج فقط. عند إظهار الرسم على جميع الفريمات تظل نقاطه مرتبطة بالوقت والسعر الأصليين.",color=Color(0xFFBCCBDD),fontSize=13.sp)
                            Text("الضغط يحدد الرسم، والسحب ينقله، والضغط المطوّل أو المزدوج يفتح إعداداته. الرسم المقفل لا يتحرك.",color=Color(0xFFBCCBDD),fontSize=13.sp)
                        }
                    }
                }
                if(error.isNotEmpty())Text(error,color=Color(0xFFFF8A96),fontSize=12.sp)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    DrawingButton("حفظ",active=true) {
                        val anchors=original.points.indices.map { i->
                            val price=if(original.kind==DrawingKind.VERTICAL)original.points[i].price else drawingNumber(prices[i])
                            val time=if(original.kind==DrawingKind.HORIZONTAL||times[i]==drawingDateFormat().format(Date(original.points[i].time)))original.points[i].time else drawingDate(times[i])
                            if(price==null||price<=0.0||time==null)null else DrawingAnchor(time,price)
                        }
                        val levels=ratios.map { drawingNumber(it) }
                        val points=anchors.filterNotNull()
                        val badShape=points.size==original.kind.anchors&&!validDrawingPoints(original.kind,points)
                        when {
                            anchors.any { it==null }->error="أدخل سعرًا موجبًا ووقتًا صحيحًا بالتنسيق الموضح."
                            badShape->error="النقاط متطابقة أو لا تسمح بتكوين هذا الرسم؛ عدّل الإحداثيات."
                            levels.any { it==null||it !in -10.0..10.0 }->error="نسب المستويات يجب أن تكون بين -10 و10."
                            hasLevels&&draft.levels.none { it.enabled }->error="فعّل مستوى واحدًا على الأقل."
                            else->{
                                ChartDrawings.objects[original.id]=draft.copy(name=draft.name.ifBlank { original.kind.title },points=points,levels=draft.levels.mapIndexed { i,l->l.copy(ratio=levels[i]!!) })
                                if(!draft.visible)ChartDrawings.selected=null
                                onDismiss()
                            }
                        }
                    }
                    DrawingButton("إلغاء",onClick=onDismiss)
                    DrawingButton("حذف",danger=true) { ChartDrawings.remove(original.id);onDismiss() }
                }
            }
        }
    }
}


// Promotion shown on every fresh screen entry, until explicitly dismissed.
// All artwork is vector drawing; no screenshot or remote asset.
// Isolated preferences: this feature never changes balances, trades, or account selection.
@Composable private fun FirstLaunchPromotion() {
    val context=LocalContext.current
    val prefs=remember(context) { context.applicationContext.getSharedPreferences("first_launch_promotion_v1",Context.MODE_PRIVATE) }
    var visible by remember { mutableStateOf(true) }
    var copied by remember { mutableStateOf(false) }
    val clipboard=LocalClipboardManager.current
    val close:()->Unit={ visible=false }
    if(!visible)return
    LaunchedEffect(copied) { if(copied) { delay(1800); copied=false } }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false,dismissOnBackPress=true,dismissOnClickOutside=false)) {
        // Use our own dim layer, avoiding double dimming from the dialog window.
        val dialogView=LocalView.current
        SideEffect {
            (dialogView.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.let { window->
                window.setDimAmount(0f)
            }
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.64f)),contentAlignment=Alignment.Center) {
                val panelWidth=minOf(maxWidth.value*.714f,420f,maxHeight.value*.90f/1.42662f)
                val k=panelWidth/586f
                fun d(value:Float)=(value*k).dp
                val promoFontScale=LocalDensity.current.fontScale
                fun t(value:Float)=(value*k/promoFontScale).sp
                val shape=RoundedCornerShape(d(36f))
                Box(Modifier.offset(y=d(40f)).width(panelWidth.dp).height(d(836f))
                    .shadow(d(24f),shape,ambientColor=Color(0xFF7B18FF),spotColor=Color(0xFF8F24FF))
                    .clip(shape)
                    .background(Brush.linearGradient(listOf(Color(0xFF100C28),Color(0xFF060617),Color(0xFF070818))))
                    .border(d(2.4f),Brush.linearGradient(listOf(Color(0xFFBA76FF),Color(0xFF6C26E6),Color(0xFFAE40FF),Color(0xFF681ED9))),shape)) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(Brush.radialGradient(listOf(Color(0xFF7226CA).copy(alpha=.20f),Color.Transparent),Offset(size.width*.47f,size.height*.17f),size.width*.64f))
                        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFB678FF).copy(alpha=.32f),Color.Transparent)),topLeft=Offset(size.width*.07f,0f),size=Size(size.width*.86f,3f*k*density),cornerRadius=CornerRadius(3f))
                    }
                    PromotionGiftVector(Modifier.offset(d(50f),d(30f)).size(d(486f),d(275f)))
                    Box(Modifier.align(Alignment.TopEnd).offset(x=-d(17f),y=d(20f)).size(d(58f)).clip(CircleShape).clickable(onClick=close),contentAlignment=Alignment.Center) {
                        PromotionLineIcon("close",Modifier.size(d(25f)),Color(0xFFC18AFF))
                    }
                    Canvas(Modifier.offset(d(271f),d(277f)).size(d(45f),d(38f))) {
                        val w=size.width;val h=size.height
                        val crown=Path().apply { moveTo(0f,h*.30f);lineTo(w*.24f,h*.52f);lineTo(w*.50f,0f);lineTo(w*.76f,h*.52f);lineTo(w,h*.30f);lineTo(w*.86f,h*.83f);lineTo(w*.14f,h*.83f);close() }
                        drawPath(crown,Brush.verticalGradient(listOf(Color(0xFFD78AFF),Color(0xFF7D14DF))))
                        drawRoundRect(Color(0xFF9F32F4),Offset(w*.14f,h*.9f),Size(w*.72f,h*.1f),CornerRadius(w*.035f))
                    }
                    Row(Modifier.offset(d(34f),d(305f)).size(d(518f),d(78f)),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {
                        Text("ترويجي",color=Color.White,fontSize=t(58f),fontWeight=FontWeight.Bold,style=TextStyle(textDirection=TextDirection.Rtl))
                        Spacer(Modifier.width(d(11f)))
                        Text("رمز",color=Color(0xFF8C24F9),fontSize=t(58f),fontWeight=FontWeight.Bold,style=TextStyle(textDirection=TextDirection.Rtl))
                    }
                    Text("استخدم رمز الترويجي واحصل على مكافآت رائعة!",modifier=Modifier.offset(d(25f),d(390f)).width(d(536f)),color=Color(0xFFF0EDF7),fontSize=t(23f),textAlign=TextAlign.Center,style=TextStyle(textDirection=TextDirection.Rtl),maxLines=1)
                    Text("قد يغير هذا الرمز تجربتك في التداول!",modifier=Modifier.offset(d(30f),d(434f)).width(d(526f)),color=Color(0xFF9B89C5),fontSize=t(18f),textAlign=TextAlign.Center,style=TextStyle(textDirection=TextDirection.Rtl),maxLines=1)
                    val codeShape=RoundedCornerShape(d(30f))
                    Box(Modifier.offset(d(36f),d(485f)).size(d(514f),d(115f)).clip(codeShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF25123E),Color(0xFF0B081F),Color(0xFF13092C))))
                        .border(d(1.7f),Brush.linearGradient(listOf(Color(0xFFD097FF),Color(0xFF5A2588),Color(0xFF9A45F1))),codeShape)) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawCircle(Brush.radialGradient(listOf(Color(0xFF9A42FF).copy(alpha=.30f),Color.Transparent),Offset(size.width*.07f,size.height*.05f),size.height*.9f),size.height*.9f,Offset(size.width*.07f,size.height*.05f))
                        }
                        PromotionLineIcon("ticket",Modifier.offset(d(34f),d(32f)).size(d(54f)),Color(0xFFA333FF))
                        Column(Modifier.offset(d(116f),d(20f)).width(d(195f)),horizontalAlignment=Alignment.CenterHorizontally) {
                            Text("PROMO90",color=Color.White,fontSize=t(33f),fontWeight=FontWeight.Bold,maxLines=1)
                            Spacer(Modifier.height(d(5f)))
                            Text("نسبة المكافأة: 90%",color=Color(0xFF9B89C5),fontSize=t(18f),style=TextStyle(textDirection=TextDirection.Rtl),maxLines=1)
                        }
                        Row(Modifier.align(Alignment.CenterEnd).padding(end=d(24f)).size(d(134f),d(61f)).shadow(d(12f),RoundedCornerShape(d(21f)),ambientColor=Color(0xFF8D16FF),spotColor=Color(0xFF8D16FF)).clip(RoundedCornerShape(d(21f)))
                            .background(Brush.verticalGradient(listOf(Color(0xFFC86EFF),Color(0xFF941FFF),Color(0xFF7900F1))))
                            .clickable { clipboard.setText(AnnotatedString("PROMO90")); copied=true },verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                            Text(if(copied)"تم" else "نسخ",color=Color.White,fontSize=t(25f),fontWeight=FontWeight.Medium)
                            Spacer(Modifier.width(d(13f)))
                            PromotionLineIcon("copy",Modifier.size(d(26f)),Color(0xFFF5E6FF))
                        }
                    }
                    Row(Modifier.offset(d(34f),d(632f)).size(d(518f),d(86f)).shadow(d(18f),RoundedCornerShape(d(29f)),ambientColor=Color(0xFF8F21FF),spotColor=Color(0xFF8F21FF)).clip(RoundedCornerShape(d(29f)))
                        .background(Brush.verticalGradient(listOf(Color(0xFFC99AFF),Color(0xFF9A4CFF),Color(0xFF7B16FA),Color(0xFF861BFF))))
                        .border(d(1f),Color(0xFFC48CFF).copy(alpha=.60f),RoundedCornerShape(d(29f)))
                        .clickable {
                            // A local code selection only: do not pretend that a server granted a bonus.
                            prefs.edit().putString("selected_code","PROMO90").apply()
                            close()
                            TradeMessage.show("تم حفظ الرمز الترويجي PROMO90")
                        },verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                        PromotionLineIcon("check",Modifier.size(d(31f)),Color.White)
                        Spacer(Modifier.width(d(23f)))
                        Text("استخدام",color=Color.White,fontSize=t(29f),fontWeight=FontWeight.Medium)
                    }
                    Row(Modifier.offset(d(34f),d(731f)).size(d(518f),d(78f)).clip(RoundedCornerShape(d(27f)))
                        .background(Color(0xFF090C19).copy(alpha=.76f)).border(d(1.7f),Color(0xFF655B87),RoundedCornerShape(d(27f))).clickable(onClick=close),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                        PromotionLineIcon("close",Modifier.size(d(23f)),Color(0xFF9B93BC))
                        Spacer(Modifier.width(d(26f)))
                        Text("تجاهل",color=Color(0xFFB4AECE),fontSize=t(28f),fontWeight=FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable private fun PromotionLineIcon(kind:String,modifier:Modifier,color:Color) {
    Canvas(modifier) {
        val w=size.width;val h=size.height;val stroke=w*.075f
        when(kind) {
            "close"->{ drawLine(color,Offset(w*.12f,h*.12f),Offset(w*.88f,h*.88f),stroke,StrokeCap.Round);drawLine(color,Offset(w*.88f,h*.12f),Offset(w*.12f,h*.88f),stroke,StrokeCap.Round) }
            "check"->{ val path=Path().apply { moveTo(w*.08f,h*.51f);lineTo(w*.36f,h*.79f);lineTo(w*.93f,h*.14f) };drawPath(path,color,style=Stroke(stroke,cap=StrokeCap.Round,join=StrokeJoin.Round)) }
            "copy"->{ drawRoundRect(color,Offset(w*.28f,h*.10f),Size(w*.61f,h*.73f),CornerRadius(w*.10f),style=Stroke(stroke));drawRoundRect(color,Offset(w*.09f,h*.25f),Size(w*.61f,h*.69f),CornerRadius(w*.10f),style=Stroke(stroke));drawLine(color,Offset(w*.42f,h*.08f),Offset(w*.72f,h*.08f),stroke*1.7f,StrokeCap.Round) }
            "ticket"->{
                val path=Path().apply { moveTo(w*.09f,h*.58f);lineTo(w*.57f,h*.08f);lineTo(w*.73f,h*.23f);cubicTo(w*.62f,h*.35f,w*.78f,h*.50f,w*.89f,h*.39f);lineTo(w*.98f,h*.51f);lineTo(w*.48f,h*.99f);lineTo(w*.35f,h*.85f);cubicTo(w*.44f,h*.74f,w*.29f,h*.60f,w*.19f,h*.70f);close() }
                drawPath(path,color,style=Stroke(stroke,join=StrokeJoin.Round));drawLine(color,Offset(w*.43f,h*.47f),Offset(w*.62f,h*.70f),stroke*.65f,StrokeCap.Round);drawCircle(color,w*.04f,Offset(w*.43f,h*.62f),style=Stroke(stroke*.6f));drawCircle(color,w*.04f,Offset(w*.62f,h*.51f),style=Stroke(stroke*.6f))
            }
        }
    }
}

@Composable private fun PromotionGiftVector(modifier:Modifier=Modifier) {
    Canvas(modifier) {
        scale(size.width/486f,size.height/275f,Offset.Zero) {
            fun path(vararg points:Float):Path=Path().apply { moveTo(points[0],points[1]);var i=2;while(i<points.size) { lineTo(points[i],points[i+1]);i+=2 };close() }
            fun fill(p:Path,colors:List<Color>,start:Offset=Offset(150f,65f),end:Offset=Offset(310f,220f)) {
                drawPath(p,Brush.linearGradient(colors,start,end))
            }
            val violet=Color(0xFF8E24FF);val light=Color(0xFFE2B2FF)
            drawOval(Brush.radialGradient(listOf(Color(0xFF7F04FF).copy(alpha=.65f),Color.Transparent),Offset(244f,209f),192f),Offset(39f,148f),Size(409f,115f))
            for(i in 7 downTo 1)drawOval(Color(0xFF9F00FF).copy(alpha=.018f*(8-i)),Offset(76f-i*2f,172f-i),Size(344f+i*4f,83f+i*2f),style=Stroke(i*2f))
            drawArc(Brush.horizontalGradient(listOf(Color(0xFF48058C),Color(0xFFDE35FF),Color(0xFF7804FF),Color(0xFFC979FF))),-12f,328f,false,Offset(78f,171f),Size(340f,86f),style=Stroke(2.5f))
            drawArc(Color(0xFF9B22FF),12f,235f,false,Offset(111f,180f),Size(275f,63f),style=Stroke(2f))
            drawArc(Color(0xFFF88CFF),15f,160f,false,Offset(128f,181f),Size(245f,57f),style=Stroke(3f))
            // Reflected purple light beneath the present.
            fill(path(174f,225f,318f,225f,333f,262f,162f,262f),listOf(Color(0xFF7610C8).copy(alpha=.24f),Color.Transparent),Offset(240f,225f),Offset(240f,262f))
            // Main present: independently shaded faces and bevels.
            val front=Path().apply { moveTo(144f,112f);lineTo(292f,125f);lineTo(292f,218f);cubicTo(257f,233f,189f,230f,149f,210f);close() }
            fill(front,listOf(Color(0xFFAD47FF),Color(0xFF5C08DB),Color(0xFF310177)),Offset(151f,115f),Offset(286f,221f))
            val side=path(292f,125f,335f,109f,333f,200f,292f,218f)
            fill(side,listOf(Color(0xFF8F2CF3),Color(0xFF39017F),Color(0xFF24054F)),Offset(297f,110f),Offset(337f,210f))
            drawPath(front,Color(0xFFBA6CFF).copy(alpha=.50f),style=Stroke(1.5f))
            val lidFront=Path().apply { moveTo(145f,76f);lineTo(295f,91f);lineTo(295f,134f);lineTo(141f,120f);quadraticBezierTo(134f,118f,136f,109f);lineTo(139f,83f);quadraticBezierTo(140f,77f,145f,76f);close() }
            fill(lidFront,listOf(Color(0xFFCC82FF),Color(0xFF8522F9),Color(0xFF4B09C0)),Offset(150f,76f),Offset(285f,136f))
            fill(path(145f,76f,188f,60f,332f,77f,295f,91f),listOf(Color(0xFFB872FF),Color(0xFF7521D9)),Offset(180f,60f),Offset(302f,92f))
            val lidSide=Path().apply { moveTo(295f,91f);lineTo(332f,77f);quadraticBezierTo(340f,79f,339f,88f);lineTo(337f,112f);lineTo(295f,134f);close() }
            fill(lidSide,listOf(Color(0xFFC16EFF),Color(0xFF6819D4),Color(0xFF3B078A)),Offset(296f,81f),Offset(338f,131f))
            drawPath(lidFront,Color(0xFFC48AFF).copy(alpha=.63f),style=Stroke(1.4f))
            // Satin ribbon wraps across both the lid and the body.
            fill(path(187f,81f,220f,85f,218f,128f,187f,125f),listOf(Color(0xFF9A3AFF),light,Color(0xFF9F43FC)),Offset(185f,90f),Offset(222f,90f))
            fill(path(188f,125f,218f,128f,218f,226f,187f,222f),listOf(Color(0xFF7920DD),Color(0xFFD49AFF),Color(0xFF8930ED)),Offset(186f,160f),Offset(220f,160f))
            fill(path(187f,81f,225f,65f,257f,69f,220f,85f),listOf(Color(0xFFF1C9FF),Color(0xFFB25AFF)),Offset(195f,70f),Offset(230f,86f))
            fill(path(276f,88f,300f,81f,315f,83f,292f,95f,291f,132f,274f,135f),listOf(Color(0xFFCA82FF),Color(0xFF701ACB)),Offset(277f,90f),Offset(300f,130f))
            fill(path(274f,135f,291f,132f,289f,219f,273f,223f),listOf(Color(0xFFBC65FF),Color(0xFF6414BE)),Offset(273f,160f),Offset(292f,160f))
            // Bow: curved folded satin, with darker inner faces and specular edging.
            val left=Path().apply { moveTo(229f,75f);cubicTo(194f,73f,163f,65f,172f,40f);cubicTo(180f,21f,199f,19f,208f,14f);cubicTo(220f,11f,231f,43f,236f,62f);close() }
            fill(left,listOf(Color(0xFFEAC4FF),Color(0xFFAD50FF),Color(0xFF7320DB),Color(0xFF440891)),Offset(197f,15f),Offset(217f,76f))
            val leftInner=Path().apply { moveTo(219f,69f);cubicTo(191f,65f,170f,57f,177f,40f);cubicTo(186f,24f,209f,52f,219f,69f);close() }
            fill(leftInner,listOf(Color(0xFF22063F),Color(0xFF6F15C5),Color(0xFFB65EFF)),Offset(183f,45f),Offset(216f,66f))
            drawPath(leftInner,Color(0xFFE0A4FF),style=Stroke(1.8f))
            val right=Path().apply { moveTo(237f,69f);cubicTo(254f,43f,280f,17f,298f,28f);cubicTo(318f,41f,319f,64f,296f,79f);cubicTo(281f,88f,258f,79f,237f,76f);close() }
            fill(right,listOf(Color(0xFFCD8DFF),Color(0xFF8730E2),Color(0xFFB459FF),Color(0xFF6413C4)),Offset(270f,26f),Offset(287f,85f))
            val rightInner=Path().apply { moveTo(248f,70f);cubicTo(267f,43f,289f,34f,294f,45f);cubicTo(305f,68f,269f,74f,248f,70f);close() }
            fill(rightInner,listOf(Color(0xFF230439),Color(0xFF6B1AB4),Color(0xFF9B43F5)),Offset(263f,57f),Offset(297f,66f))
            drawPath(rightInner,Color(0xFFC785FF),style=Stroke(1.6f))
            val knot=Path().apply { moveTo(221f,58f);quadraticBezierTo(237f,54f,249f,64f);lineTo(245f,81f);quadraticBezierTo(228f,83f,218f,74f);close() }
            fill(knot,listOf(Color(0xFFE5B1FF),Color(0xFFA24CF8),Color(0xFF5B0CAC)),Offset(228f,57f),Offset(235f,82f))
            // Floating confetti.
            fill(path(68f,127f,62f,141f,94f,148f,98f,138f),listOf(light,violet),Offset(65f,128f),Offset(95f,148f))
            fill(path(100f,57f,110f,68f,120f,77f,116f,65f,106f,52f),listOf(Color(0xFFFFF0BC),Color(0xFFFFBD78)))
            fill(path(367f,73f,389f,47f,398f,57f,374f,79f),listOf(light,violet),Offset(386f,48f),Offset(373f,78f))
            fill(path(393f,99f,411f,98f,413f,108f,393f,104f),listOf(Color(0xFFFFEEC6),Color(0xFFF8C282)))
            fill(path(375f,148f,393f,156f,388f,167f,373f,158f),listOf(violet,Color(0xFF4D0BB4)))
            // Gold scalloped discount coupon, rotated as a real vector object.
            val nc=drawContext.canvas.nativeCanvas
            nc.save();nc.rotate(-19f,308f,184f)
            val ticket=Path().apply { moveTo(269f,145f);lineTo(336f,145f);lineTo(336f,152f);cubicTo(324f,154f,326f,166f,336f,166f);lineTo(336f,174f);cubicTo(325f,176f,326f,187f,336f,188f);lineTo(336f,197f);cubicTo(326f,198f,325f,210f,336f,210f);lineTo(336f,219f);lineTo(268f,219f);quadraticBezierTo(263f,219f,263f,214f);lineTo(263f,207f);cubicTo(273f,205f,273f,195f,263f,194f);lineTo(263f,185f);cubicTo(273f,183f,273f,173f,263f,172f);lineTo(263f,151f);quadraticBezierTo(263f,145f,269f,145f);close() }
            fill(ticket,listOf(Color(0xFFFFE48F),Color(0xFFFFD273),Color(0xFFFFEDA6)),Offset(263f,145f),Offset(336f,220f))
            drawPath(ticket,Color(0xFFFFF0AE),style=Stroke(1.2f))
            val paint=NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { textAlign=NativePaint.Align.CENTER;textSize=49f;typeface=Typeface.create("sans-serif",Typeface.BOLD);color=android.graphics.Color.rgb(129,28,61);setShadowLayer(1.2f,1.2f,1.5f,android.graphics.Color.WHITE) }
            nc.drawText("%",300f,201f,paint);nc.restore()
        }
    }
}
