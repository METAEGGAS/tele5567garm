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
import kotlinx.coroutines.flow.MutableStateFlow
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
private const val LIVE_CANDLE_FRACTION=.55
private const val MINBW=2f
private const val PAD=6f
private const val PINCH_AMPLIFY=4.0
private const val ZOOM_SMOOTH=.22f
private const val PAN_GAIN=1.65
private const val INERTIA_FRICTION=3.2
private const val MIN_VISIBLE_BARS_LIMIT=10.0
private const val MAX_VISIBLE_BARS_LIMIT=120.0
private const val DV_ALPHA=.20f
private val ids=AtomicLong(System.currentTimeMillis())
private fun nextId()=ids.incrementAndGet()
private fun barPosition(time:Long,origin:Long,intervalMs:Long=MINUTE_MS)=(time-origin).toDouble()/intervalMs

data class Candle(val open:Float,val high:Float,val low:Float,val close:Float,val time:Long=0L)
fun clockNow():String=SimpleDateFormat("HH:mm:ss",Locale.US).apply { timeZone=TimeZone.getTimeZone("GMT+3") }.format(Date())
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
// Successful image downloads live in filesDir, not the evictable HTTP cache.
// All remote artwork in this screen shares one loader and one persistent store.
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
    val context=LocalContext.current.applicationContext
    // A previously successful fallback is tried locally before any remote URL.
    val candidates=remember(context,urls) { urls.sortedBy { TradingImageStore.cached(context,it)==null } }
    var failed by remember(candidates) { mutableIntStateOf(0) }
    if(failed<candidates.size) CachedTradingImage(url=candidates[failed],contentDescription=label,contentScale=ContentScale.Crop,modifier=modifier.clip(CircleShape).background(C.Surface),onError={failed++})
    else Box(modifier.clip(CircleShape).background(C.SurfaceRaised).border(1.dp,C.Cyan.copy(alpha=.55f),CircleShape),contentAlignment=Alignment.Center) { Text(label.take(2),color=C.Cyan,fontSize=9.sp,fontWeight=FontWeight.ExtraBold) }
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
private fun aggregateChartBars(bars:List<Candle>,intervalMs:Long):List<Candle> =
    bars.filter { it.time>0&&it.open.isFinite()&&it.high.isFinite()&&it.low.isFinite()&&it.close.isFinite()&&it.low>0&&it.high>=maxOf(it.open,it.close)&&it.low<=minOf(it.open,it.close) }
        .sortedBy { it.time }.distinctBy { it.time }.groupBy { it.time/intervalMs*intervalMs }.map { (time,group)->
            Candle(group.first().open,group.maxOf { it.high },group.minOf { it.low },group.last().close,time)
        }.sortedBy { it.time }
private fun chartBarsFromTicks(ticks:List<ChartTick>,intervalMs:Long):List<Candle> =
    ticks.filter { it.time>0&&it.price.isFinite()&&it.price>0 }.sortedBy { it.time }
        .groupBy { it.time/intervalMs*intervalMs }.map { (time,group)->
            Candle(group.first().price.toFloat(),group.maxOf { it.price }.toFloat(),group.minOf { it.price }.toFloat(),group.last().price.toFloat(),time)
        }.sortedBy { it.time }
private fun heikinAshi(bars:List<Candle>):List<Candle> {
    var previousOpen=0f;var previousClose=0f
    return bars.mapIndexed { index,c->
        val close=(c.open+c.high+c.low+c.close)/4f
        val open=if(index==0)(c.open+c.close)/2f else (previousOpen+previousClose)/2f
        previousOpen=open;previousClose=close
        Candle(open,maxOf(c.high,open,close),minOf(c.low,open,close),close,c.time)
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
    private fun parseChartTick(json:JSONObject):ChartTick? {
        val price=json.optDouble("mid",Double.NaN)
        val time=sourceTimestamp(json.opt("timestamp") ?: json.opt("lastQuoteAt"))
        return if(price.isFinite()&&price>0&&time>0)ChartTick(price,time) else null
    }
    private suspend fun chartTickHistory(symbol:String):List<ChartTick> = withContext(Dispatchers.IO) {
        val txt=get("/api/${symbol.uppercase(Locale.US)}/history",listOf("count" to "1000")) ?: return@withContext emptyList()
        try { val a=JSONArray(txt);(0 until a.length()).mapNotNull { i->a.optJSONObject(i)?.let { parseChartTick(it) } }.sortedBy { it.time } }
        catch(_:Exception) { emptyList() }
    }
    internal suspend fun chartHistory(symbol:String,frame:String,recent:Boolean=false):List<Candle> {
        val ms=chartFrameMillis(frame)
        if(ms<MINUTE_MS) {
            val ticks=chartTickHistory(symbol)
            val bars=chartBarsFromTicks(ticks,ms)
            // The history limit may start inside a bucket; do not label that
            // truncated first bucket as a complete historical candle.
            return if(ticks.isNotEmpty()&&bars.isNotEmpty()&&ticks.first().time>bars.first().time)bars.drop(1) else bars
        }
        val base=chartSourceInterval(frame)
        val baseMs=when(base) { "5m"->300_000L;"15m"->900_000L;"30m"->1_800_000L;"1h"->3_600_000L;"4h"->14_400_000L;"1d"->86_400_000L;else->MINUTE_MS }
        val factor=(ms/baseMs).toInt().coerceAtLeast(1)
        val source=ohlc(symbol,base,if(recent)maxOf(4,factor*3) else minOf(1000,300*factor))
        val bars=aggregateChartBars(source,ms)
        return if(source.isNotEmpty()&&bars.isNotEmpty()&&source.first().time>bars.first().time)bars.drop(1) else bars
    }
    private suspend fun sourceChartTick(symbol:String):ChartTick? = withContext(Dispatchers.IO) {
        val txt=get("/api/${symbol.uppercase(Locale.US)}",listOf("allowStale" to "false")) ?: return@withContext null
        try {
            val j=JSONObject(txt)
            if(j.optBoolean("stale",false)||j.optString("marketState")=="closed"||j.optDouble("quoteAgeSeconds",0.0)>15.0)return@withContext null
            parseChartTick(j)
        } catch(_:Exception) { null }
    }
    // Keep tick timestamps so repeated REST snapshots cannot manufacture new candles.
    internal suspend fun updateChartQuote(symbol:String,onTick:(Double,Long)->Unit) {
        sourceChartTick(symbol)?.let { onTick(it.price,it.time) }
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
}
private object InsufficientBalanceState {
    var account by mutableStateOf<String?>(null)
}
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
    TradeSettings.amount=TradeSettings.amount
    val amount=TradeSettings.amount.toDoubleOrNull()
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
            TradePanel()
            if(IndicatorSettings.acceleratorEnabled)AcceleratorOscillatorPane()
            if(IndicatorSettings.adxEnabled)AdxPane()
            if(IndicatorSettings.aroonEnabled)AroonPane()
            if(IndicatorSettings.atrEnabled)AtrPane()
            if(IndicatorSettings.awesomeEnabled)AwesomeOscillatorPane()
            BottomNavBar()
        }
        val message=TradeMessage.text
        if(message.isNotEmpty()) Box(Modifier.align(Alignment.TopCenter).padding(top=110.dp,start=18.dp,end=18.dp).shadow(12.dp,RoundedCornerShape(10.dp)).background(Color(0xFF202638),RoundedCornerShape(10.dp)).border(1.dp,Color(0xFFF05B67),RoundedCornerShape(10.dp)).padding(horizontal=18.dp,vertical=13.dp)) { Text(message,color=Color.White,fontSize=14.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center) }
    }
    if(pickerOpen)PairPickerSheet { pickerOpen=false }
    InsufficientBalanceDialog()
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
                    Text("حلال",color=C.Green,fontSize=10.sp,modifier=Modifier.alpha(0f)); Spacer(Modifier.width(6.dp)); Text("USD",color=C.TextSecondary,fontSize=10.sp)
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
    if(exact>=0) { val e=list[exact]; list[exact]=Candle(if(bulk)o else e.open,max(e.high,h),min(e.low,l),c,t); return false }
    if(insert>=list.size)list.add(Candle(o,h,l,c,t))
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
private fun initialViewport(candles:List<Candle>,origin:Long,intervalMs:Long=MINUTE_MS):ChartViewport {
    if(candles.isEmpty())return ChartViewport(60.0,0.0,LIVE_CANDLE_FRACTION,1.0,MIN_PRICE_SPAN)
    val a=candles.takeLast(min(60,candles.size)); val low=a.minOf { it.low.toDouble() }; val high=a.maxOf { it.high.toDouble() }
    return ChartViewport(60.0,barPosition(candles.last().time,origin,intervalMs),LIVE_CANDLE_FRACTION,(low+high)/2,((high-low)/.70*1.04).coerceAtLeast(MIN_PRICE_SPAN))
}
// Do not allow panning into future space beyond the initial 55% live-candle position.
// History remains scrollable in both directions up to this live boundary.
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
    var menuOpen by remember { mutableStateOf(false) }
    var chartStyleOpen by remember { mutableStateOf(false) }
    var indicatorsOpen by remember { mutableStateOf(false) }
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
                    Spacer(Modifier.width(8.dp));Text("${SymCatalog.pretty(sym)} OTC",color=C.TextPrimary,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
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
                        ReferenceToolTile("people",checked=selectedTool=="people") { selectedTool="people" }
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        ReferenceToolTile("settings",checked=IndicatorSettings.acceleratorEnabled) { selectedTool="settings";menuOpen=false;indicatorsOpen=true }
                        ReferenceToolTile("temperature",checked=selectedTool=="temperature") { selectedTool="temperature" }
                    }
                    ReferenceToolTile("drawing",checked=selectedTool=="drawing") { selectedTool="drawing" }
                }
            }
        }
    }
    if(chartStyleOpen)ChartStyleDialog { chartStyleOpen=false }
    if(indicatorsOpen)IndicatorsDialog { indicatorsOpen=false }
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
    var tab by remember { mutableIntStateOf(1) } // Always open chart types first.
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
                                    Column(Modifier.weight(1f).aspectRatio(1.60f).background(if(name==ChartSettings.chartType)ChartMenuColors.Selected else ChartMenuColors.Tile).clickable { ChartSettings.chartType=name },horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
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


// Indicator UI is deliberately isolated from trading, accounts and the chart feed.
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
// Per-line style of an indicator plot: visibility, color and width are user editable.
private class LineStyle(visibleInit:Boolean,colorInit:Color,widthInit:Float) {
    var visible by mutableStateOf(visibleInit)
    var color by mutableStateOf(colorInit)
    var widthDp by mutableStateOf(widthInit)
}
private object IndicatorSettings {
    var acceleratorEnabled by mutableStateOf(false)
    var fastPeriod by mutableIntStateOf(5)
    var slowPeriod by mutableIntStateOf(34)
    var signalPeriod by mutableIntStateOf(5)
    var snapshot by mutableStateOf<IndicatorChartSnapshot?>(null)
    val favorites=mutableStateMapOf<String,Boolean>()
    // ADX
    var adxEnabled by mutableStateOf(false)
    var adxVisible by mutableStateOf(true)
    var adxPeriod by mutableIntStateOf(14)
    var adxDiLength by mutableIntStateOf(14)
    val adxLine=LineStyle(true,Color(0xFFFF6D00),1f)
    val plusDiLine=LineStyle(true,Color(0xFF26C6DA),1f)
    val minusDiLine=LineStyle(true,Color(0xFFFFD54F),1f)
    // Alligator
    var alligatorEnabled by mutableStateOf(false)
    var alligatorVisible by mutableStateOf(true)
    var jawPeriod by mutableIntStateOf(13)
    var jawShift by mutableIntStateOf(8)
    var teethPeriod by mutableIntStateOf(8)
    var teethShift by mutableIntStateOf(5)
    var lipsPeriod by mutableIntStateOf(5)
    var lipsShift by mutableIntStateOf(3)
    val jawLine=LineStyle(true,Color(0xFF2962FF),1f)
    val teethLine=LineStyle(true,Color(0xFFFF6D00),1f)
    val lipsLine=LineStyle(true,Color(0xFF9CCC65),1f)
    // Aroon
    var aroonEnabled by mutableStateOf(false)
    var aroonVisible by mutableStateOf(true)
    var aroonPeriod by mutableIntStateOf(25)
    val aroonUpLine=LineStyle(true,Color(0xFF2962FF),1f)
    val aroonDownLine=LineStyle(true,Color(0xFFFF5252),1f)
    // Average True Range
    var atrEnabled by mutableStateOf(false)
    var atrVisible by mutableStateOf(true)
    var atrPeriod by mutableIntStateOf(14)
    val atrLine=LineStyle(true,Color(0xFF2962FF),1f)
    // Awesome Oscillator
    var awesomeEnabled by mutableStateOf(false)
    var awesomeVisible by mutableStateOf(true)
    var awesomeFast by mutableIntStateOf(5)
    var awesomeSlow by mutableIntStateOf(34)
    val awesomeUp=LineStyle(true,Color(0xFF4CAF50),1f)
    val awesomeDown=LineStyle(true,Color(0xFFFF5252),1f)
    fun anyActive()=acceleratorEnabled||adxEnabled||alligatorEnabled||aroonEnabled||atrEnabled||awesomeEnabled
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
                                        when(name) {
                                            "Accelerator Oscillator"->IndicatorSettings.acceleratorEnabled=true
                                            "ADX"->IndicatorSettings.adxEnabled=true
                                            "Alligator"->IndicatorSettings.alligatorEnabled=true
                                            "Aroon"->IndicatorSettings.aroonEnabled=true
                                            "Average True Range"->IndicatorSettings.atrEnabled=true
                                            "Awesome Oscillator"->IndicatorSettings.awesomeEnabled=true
                                            else->TradeMessage.show("حسابات مؤشر $name غير متاحة في هذه النسخة")
                                        }
                                        onDismiss()
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
// AC = AO - SMA(AO, signal); AO = SMA(median, fast) - SMA(median, slow).
// Warm-up values stay NaN: insufficient history is never replaced with invented data.
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

// ----- ADX / Alligator / Aroon / Average True Range / Awesome Oscillator -----
// Standard definitions are used: Wilder smoothing for ADX and ATR, a period+1
// window for Aroon, SMMA over the median price with forward shifts for the
// Alligator, and SMA(median,fast)-SMA(median,slow) for the Awesome Oscillator.
// Warm-up values stay NaN: insufficient history is never replaced with invented data.
private fun trueRangeComponents(bars:List<Candle>):Triple<DoubleArray,DoubleArray,DoubleArray> {
    val n=bars.size
    val tr=DoubleArray(n){Double.NaN};val pdm=DoubleArray(n){0.0};val mdm=DoubleArray(n){0.0}
    for(i in 1 until n) {
        val h=bars[i].high.toDouble();val l=bars[i].low.toDouble();val pc=bars[i-1].close.toDouble()
        tr[i]=maxOf(h-l,abs(h-pc),abs(l-pc))
        val up=h-bars[i-1].high;val dn=bars[i-1].low-l
        pdm[i]=if(up>dn&&up>0.0)up else 0.0
        mdm[i]=if(dn>up&&dn>0.0)dn else 0.0
    }
    return Triple(tr,pdm,mdm)
}
private fun wilderSum(src:DoubleArray,period:Int):DoubleArray {
    val n=src.size;val out=DoubleArray(n){Double.NaN};var sum=0.0
    for(i in 1 until n) {
        val v=src[i].takeIf { it.isFinite() } ?: 0.0
        if(i<=period) { sum+=v;if(i==period)out[i]=sum }
        else out[i]=out[i-1]-out[i-1]/period+v
    }
    return out
}
private fun adxValues(bars:List<Candle>,diLength:Int,smoothing:Int):Triple<DoubleArray,DoubleArray,DoubleArray> {
    val n=bars.size
    val adx=DoubleArray(n){Double.NaN};val plus=DoubleArray(n){Double.NaN};val minus=DoubleArray(n){Double.NaN}
    if(n<2||diLength<1||smoothing<1)return Triple(adx,plus,minus)
    val(tr,pdm,mdm)=trueRangeComponents(bars)
    val trS=wilderSum(tr,diLength);val pS=wilderSum(pdm,diLength);val mS=wilderSum(mdm,diLength)
    val dx=DoubleArray(n){Double.NaN}
    for(i in diLength until n) {
        val a=trS[i]
        if(a.isFinite()&&a>0.0) {
            plus[i]=100.0*pS[i]/a;minus[i]=100.0*mS[i]/a
            val s=plus[i]+minus[i]
            dx[i]=if(s>0.0)100.0*abs(plus[i]-minus[i])/s else 0.0
        }
    }
    var sum=0.0;var count=0
    for(i in diLength until n) {
        val v=dx[i];if(!v.isFinite())continue
        if(count<smoothing) { sum+=v;count++;if(count==smoothing)adx[i]=sum/smoothing }
        else adx[i]=(adx[i-1]*(smoothing-1)+v)/smoothing
    }
    return Triple(adx,plus,minus)
}
private fun atrValues(bars:List<Candle>,period:Int):DoubleArray {
    val n=bars.size;val out=DoubleArray(n){Double.NaN}
    if(n<2||period<1)return out
    val(tr,_,_)=trueRangeComponents(bars)
    val s=wilderSum(tr,period)
    for(i in period until n)if(s[i].isFinite())out[i]=s[i]/period
    return out
}
private fun aroonValues(bars:List<Candle>,period:Int):Pair<DoubleArray,DoubleArray> {
    val n=bars.size;val up=DoubleArray(n){Double.NaN};val down=DoubleArray(n){Double.NaN}
    if(period<1)return up to down
    for(i in period until n) {
        var hi=i;var lo=i
        for(j in i-period..i) {
            if(bars[j].high>=bars[hi].high)hi=j
            if(bars[j].low<=bars[lo].low)lo=j
        }
        up[i]=100.0*(period-(i-hi))/period
        down[i]=100.0*(period-(i-lo))/period
    }
    return up to down
}
private fun smmaMedian(bars:List<Candle>,period:Int):DoubleArray {
    val n=bars.size;val out=DoubleArray(n){Double.NaN}
    if(period<1)return out
    var sum=0.0
    for(i in 0 until n) {
        val m=(bars[i].high.toDouble()+bars[i].low.toDouble())/2.0
        if(i<period) { sum+=m;if(i==period-1)out[i]=sum/period }
        else out[i]=(out[i-1]*(period-1)+m)/period
    }
    return out
}
private fun alligatorValues(bars:List<Candle>,jawPeriod:Int,jawShift:Int,teethPeriod:Int,teethShift:Int,lipsPeriod:Int,lipsShift:Int):Triple<DoubleArray,DoubleArray,DoubleArray> {
    fun shifted(period:Int,shift:Int):DoubleArray {
        val base=smmaMedian(bars,period)
        val out=DoubleArray(bars.size){Double.NaN}
        for(i in base.indices)if(base[i].isFinite()) { val j=i+shift;if(j<out.size)out[j]=base[i] }
        return out
    }
    return Triple(shifted(jawPeriod,jawShift),shifted(teethPeriod,teethShift),shifted(lipsPeriod,lipsShift))
}
private fun awesomeValues(bars:List<Candle>,fast:Int,slow:Int):DoubleArray {
    val n=bars.size;val out=DoubleArray(n){Double.NaN}
    if(fast<1||slow<1)return out
    val prefix=DoubleArray(n+1)
    for(i in bars.indices)prefix[i+1]=prefix[i]+(bars[i].high.toDouble()+bars[i].low.toDouble())/2.0
    val start=max(fast,slow)-1
    for(i in start until n)out[i]=(prefix[i+1]-prefix[i+1-fast])/fast-(prefix[i+1]-prefix[i+1-slow])/slow
    return out
}

@Composable private fun PencilIcon(modifier:Modifier=Modifier,color:Color=Color(0xFF8FAAC4)) { Canvas(modifier) {
    val stroke=2.dp.toPx()
    drawLine(color,Offset(size.width*.18f,size.height*.78f),Offset(size.width*.75f,size.height*.21f),stroke*2,StrokeCap.Butt)
    drawLine(color,Offset(size.width*.79f,size.height*.16f),Offset(size.width*.91f,size.height*.28f),stroke,StrokeCap.Butt)
    drawLine(color,Offset(size.width*.05f,size.height*.95f),Offset(size.width*.19f,size.height*.80f),stroke,StrokeCap.Round)
} }
@Composable private fun TrashIcon(modifier:Modifier=Modifier,color:Color=Color(0xFFA8C0D2)) { Canvas(modifier) {
    val w=size.width;val h=size.height;val s=w*.08f
    drawLine(color,Offset(w*.2f,h*.28f),Offset(w*.8f,h*.28f),s,StrokeCap.Round)
    drawLine(color,Offset(w*.42f,h*.28f),Offset(w*.42f,h*.18f),s,StrokeCap.Round)
    drawLine(color,Offset(w*.58f,h*.18f),Offset(w*.58f,h*.28f),s,StrokeCap.Round)
    drawLine(color,Offset(w*.3f,h*.28f),Offset(w*.34f,h*.84f),s,StrokeCap.Round)
    drawLine(color,Offset(w*.7f,h*.28f),Offset(w*.66f,h*.84f),s,StrokeCap.Round)
    drawLine(color,Offset(w*.34f,h*.84f),Offset(w*.66f,h*.84f),s,StrokeCap.Round)
} }
@Composable private fun EyeIcon(modifier:Modifier=Modifier,on:Boolean,color:Color=Color(0xFFA8C0D2)) { Canvas(modifier) {
    val w=size.width;val h=size.height;val s=w*.07f
    val p=Path().apply { moveTo(w*.08f,h*.5f);quadraticBezierTo(w*.5f,h*.05f,w*.92f,h*.5f);quadraticBezierTo(w*.5f,h*.95f,w*.08f,h*.5f);close() }
    drawPath(p,color,style=Stroke(s))
    drawCircle(color,w*.09f,Offset(w*.5f,h*.5f))
    if(!on)drawLine(color,Offset(w*.15f,h*.85f),Offset(w*.85f,h*.15f),s*1.2f,StrokeCap.Round)
} }
@Composable private fun PaneHeader(expanded:Boolean,title:String,onToggle:()->Unit,onSettings:()->Unit,modifier:Modifier=Modifier) {
    Row(modifier.height(27.dp).padding(start=5.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(21.dp,26.dp).clickable(onClick=onToggle),contentAlignment=Alignment.Center) {
            Canvas(Modifier.size(9.dp,6.dp)) {
                val p=Path().apply {
                    if(expanded) { moveTo(0f,0f);lineTo(size.width,0f);lineTo(size.width/2,size.height) }
                    else { moveTo(0f,0f);lineTo(0f,size.height);lineTo(size.width,size.height/2) }
                    close()
                }
                drawPath(p,Color(0xFF9AB4CB))
            }
        }
        Text(title,color=Color(0xFFF2F3F6),fontSize=12.sp,maxLines=1)
        Box(Modifier.size(27.dp).clickable(onClick=onSettings),contentAlignment=Alignment.Center) { PencilIcon(Modifier.size(13.dp)) }
    }
}

// Generic sub-pane that plots one or more indicator lines against the shared
// chart snapshot, with grid, right-hand scale labels and the reference header.
@Composable private fun IndicatorLinePane(
    title:String,
    lines:List<Pair<DoubleArray,LineStyle>>,
    paneVisible:Boolean,
    fixedLower:Double?=null,
    fixedUpper:Double?=null,
    onSettings:()->Unit
) {
    val symbol by TradeStore.currentSymbol.collectAsState()
    val snapshot=IndicatorSettings.snapshot?.takeIf { it.symbol==symbol&&it.frame==ChartSettings.frame }
    var expanded by remember { mutableStateOf(true) }
    val paneHeight=(LocalConfiguration.current.screenHeightDp*.145f).coerceIn(88f,132f).dp
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(Modifier.fillMaxWidth().height(if(expanded&&paneVisible)paneHeight else 28.dp).background(Color(0xFF1D2332))) {
            CachedTradingImage(url=CHART_BG_URL,contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Color(0xFF1D2332).copy(alpha=.88f)))
            Canvas(Modifier.fillMaxSize()) {
                val w=size.width;val h=size.height
                drawLine(Color(0xFF3C6686).copy(alpha=.7f),Offset.Zero,Offset(w,0f),1.dp.toPx())
                if(!expanded||!paneVisible)return@Canvas
                val data=snapshot ?: return@Canvas
                val view=data.viewport
                fun xFor(time:Long)=(view.fractionFor((time-data.origin).toDouble()/data.intervalMs)*w).toFloat()
                val top=19.dp.toPx();val bottom=h-10.dp.toPx()
                if(bottom<=top)return@Canvas
                val visibleIndices=data.bars.indices.filter { xFor(data.bars[it].time) in -16f..(w+16f) }
                var lower=Double.POSITIVE_INFINITY;var upper=Double.NEGATIVE_INFINITY
                for((values,style) in lines)if(style.visible)for(i in visibleIndices) {
                    val v=values.getOrNull(i) ?: continue
                    if(v.isFinite()) { if(v<lower)lower=v;if(v>upper)upper=v }
                }
                if(fixedLower!=null)lower=fixedLower
                if(fixedUpper!=null)upper=fixedUpper
                if(!lower.isFinite()||!upper.isFinite()) { lower=0.0;upper=1.0 }
                if(upper-lower<1e-9) { upper+=.5;lower-=.5 }
                val pad=(upper-lower)*.06
                if(fixedLower==null)lower-=pad
                if(fixedUpper==null)upper+=pad
                fun yFor(v:Double)=(top+(upper-v)/(upper-lower)*(bottom-top)).toFloat()
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
                for(y in listOf(top,(top+bottom)/2,bottom))drawLine(grid,Offset(0f,y),Offset(w,y),1.dp.toPx())
                clipRect(0f,top,w,bottom) {
                    for((values,style) in lines) {
                        if(!style.visible)continue
                        val path=Path();var pen=false
                        for(i in data.bars.indices) {
                            val v=values.getOrNull(i) ?: Double.NaN
                            if(!v.isFinite()) { pen=false;continue }
                            val x=xFor(data.bars[i].time);val y=yFor(v)
                            if(!pen) { path.moveTo(x,y);pen=true } else path.lineTo(x,y)
                        }
                        drawPath(path,style.color,style=Stroke((style.widthDp.dp.toPx()).coerceAtLeast(.5f),cap=StrokeCap.Round,join=StrokeJoin.Round))
                    }
                }
                val paint=NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { color=Color(0xFF8D98AB).toArgb();textSize=10.sp.toPx();textAlign=NativePaint.Align.RIGHT }
                val magnitude=max(abs(upper),abs(lower))
                val decimals=if(magnitude<1.0)ceil(-log10(magnitude.coerceAtLeast(1e-12))).toInt().plus(1).coerceIn(2,8) else if(magnitude<10.0)2 else 1
                fun label(v:Double)=String.format(Locale.US,"%.${'$'}{decimals}f",v)
                for((v,y) in listOf(upper to top,(upper+lower)/2 to (top+bottom)/2,lower to bottom)) {
                    drawContext.canvas.nativeCanvas.drawText(label(v),w-5.dp.toPx(),(y-(paint.ascent()+paint.descent())/2).coerceIn(11.dp.toPx(),h-2.dp.toPx()),paint)
                }
            }
            PaneHeader(expanded,title,{expanded=!expanded},onSettings,Modifier.align(Alignment.TopStart))
            if(expanded&&paneVisible&&(snapshot==null||lines.none { (values,style)->style.visible&&values.any { v->v.isFinite() } }))Text("بانتظار بيانات كافية للمؤشر",color=Color(0xFF8D98AB),fontSize=10.sp,modifier=Modifier.align(Alignment.Center))
        }
    }
}

private data class FieldRef(val get:()->Int,val set:(Int)->Unit)
private val StylePalette=listOf(Color(0xFFFF6D00),Color(0xFF26C6DA),Color(0xFFFFD54F),Color(0xFF2962FF),Color(0xFFFF5252),Color(0xFF66BB6A),Color(0xFFAB47BC),Color(0xFFECF0F6))
@Composable private fun IndicatorTab(label:String,selected:Boolean,onClick:()->Unit) {
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if(selected)Color(0xFF1D4E6B) else Color(0xFF1B2436)).border(1.dp,if(selected)Color(0xFF2E9DDB) else Color.Transparent,RoundedCornerShape(8.dp)).clickable(onClick=onClick).padding(horizontal=22.dp,vertical=8.dp)) { Text(label,color=Color.White,fontSize=14.sp) }
}
// Settings dialog shared by the five indicators: أساليب / المدخلات tabs, the
// delete / visibility / save row, matching the reference layout.
@Composable private fun IndicatorConfigDialog(
    title:String,
    fields:List<Pair<String,FieldRef>>,
    styles:List<Pair<String,LineStyle>>,
    visible:Boolean,
    onToggleVisible:()->Unit,
    onRemove:()->Unit,
    onDismiss:()->Unit
) {
    var tab by remember { mutableIntStateOf(1) } // Opens on المدخلات, like the reference.
    val texts=remember { fields.map { mutableStateOf(it.second.get().toString()) } }
    val valid=texts.all { (it.value.toIntOrNull() ?: -1) in 0..500 }
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(Modifier.fillMaxWidth(.86f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF28324A)).padding(horizontal=20.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(title,color=Color.White,fontSize=18.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
                    Box(Modifier.size(30.dp).clickable(onClick=onDismiss),contentAlignment=Alignment.Center) { ReferenceGlyph("close",Modifier.size(22.dp),Color(0xFFA8C0D2)) }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    IndicatorTab("أساليب",tab==0) { tab=0 }
                    IndicatorTab("المدخلات",tab==1) { tab=1 }
                }
                if(tab==1)Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    fields.forEachIndexed { i,field->
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            Text(field.first,color=Color(0xFFC4D1E1),fontSize=14.sp,style=TextStyle(textDirection=TextDirection.Rtl),modifier=Modifier.weight(1f))
                            BasicTextField(value=texts[i].value,onValueChange={ v->if(v.length<=3&&v.all { c->c in '0'..'9' })texts[i].value=v },singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),textStyle=TextStyle(color=Color.White,fontSize=15.sp,textAlign=TextAlign.Center),modifier=Modifier.width(76.dp).background(Color(0xFF171E2C),RoundedCornerShape(6.dp)).padding(10.dp))
                        }
                    }
                } else Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    styles.forEach { (name,style)->
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            Box(Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(if(style.visible)Color(0xFF8A93A6) else Color(0xFF3A4358)).clickable { style.visible=!style.visible },contentAlignment=Alignment.Center) {
                                if(style.visible)ReferenceGlyph("check",Modifier.size(14.dp),Color(0xFF22293A))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(name,color=Color(0xFFF1F4F8),fontSize=14.sp,modifier=Modifier.weight(1f))
                            Box(Modifier.width(52.dp).height(30.dp).clip(RoundedCornerShape(5.dp)).background(style.color).border(1.dp,Color(0xFF4A5774),RoundedCornerShape(5.dp)).clickable {
                                val idx=StylePalette.indexOfFirst { it.value==style.color.value }
                                style.color=StylePalette[(if(idx<0)0 else idx+1)%StylePalette.size]
                            })
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.width(64.dp).height(30.dp).clip(RoundedCornerShape(5.dp)).border(1.dp,Color(0xFF4A5774),RoundedCornerShape(5.dp)).clickable { style.widthDp=if(style.widthDp>=4f)1f else style.widthDp+1f },contentAlignment=Alignment.Center) { Text("${'$'}{style.widthDp.toInt()}px",color=Color(0xFFF1F4F8),fontSize=13.sp) }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(50.dp,42.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF33273B)).clickable { onRemove();onDismiss() },contentAlignment=Alignment.Center) { TrashIcon(Modifier.size(20.dp)) }
                    Box(Modifier.size(50.dp,42.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1B2436)).clickable(onClick=onToggleVisible),contentAlignment=Alignment.Center) { EyeIcon(Modifier.size(22.dp),visible) }
                    Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(8.dp)).background(if(valid)Color(0xFF1E3A34) else Color(0xFF465163)).border(1.dp,if(valid)Color(0xFF2EBD85) else Color.Transparent,RoundedCornerShape(8.dp)).clickable(enabled=valid) {
                        fields.forEachIndexed { i,field->texts[i].value.toIntOrNull()?.let { v->field.second.set(v) } }
                        onDismiss()
                    },contentAlignment=Alignment.Center) { Text("حفظ",color=Color.White,fontSize=15.sp,fontWeight=FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable private fun AdxPane() {
    val symbol by TradeStore.currentSymbol.collectAsState()
    val snapshot=IndicatorSettings.snapshot?.takeIf { it.symbol==symbol&&it.frame==ChartSettings.frame }
    val smoothing=IndicatorSettings.adxPeriod;val di=IndicatorSettings.adxDiLength
    val(adx,plus,minus)=remember(snapshot?.bars,smoothing,di) { adxValues(snapshot?.bars ?: emptyList(),di,smoothing) }
    var settingsOpen by remember { mutableStateOf(false) }
    IndicatorLinePane("ADX ${'$'}smoothing ${'$'}di",listOf(adx to IndicatorSettings.adxLine,plus to IndicatorSettings.plusDiLine,minus to IndicatorSettings.minusDiLine),IndicatorSettings.adxVisible) { settingsOpen=true }
    if(settingsOpen)IndicatorConfigDialog("ADX",
        listOf("تسوية ADX" to FieldRef({IndicatorSettings.adxPeriod},{v->IndicatorSettings.adxPeriod=v}),"طول DI" to FieldRef({IndicatorSettings.adxDiLength},{v->IndicatorSettings.adxDiLength=v})),
        listOf("adx" to IndicatorSettings.adxLine,"plusDI" to IndicatorSettings.plusDiLine,"minusDI" to IndicatorSettings.minusDiLine),
        IndicatorSettings.adxVisible,{IndicatorSettings.adxVisible=!IndicatorSettings.adxVisible},{IndicatorSettings.adxEnabled=false}) { settingsOpen=false }
}
@Composable private fun AroonPane() {
    val symbol by TradeStore.currentSymbol.collectAsState()
    val snapshot=IndicatorSettings.snapshot?.takeIf { it.symbol==symbol&&it.frame==ChartSettings.frame }
    val period=IndicatorSettings.aroonPeriod
    val(up,down)=remember(snapshot?.bars,period) { aroonValues(snapshot?.bars ?: emptyList(),period) }
    var settingsOpen by remember { mutableStateOf(false) }
    IndicatorLinePane("Aroon ${'$'}period",listOf(up to IndicatorSettings.aroonUpLine,down to IndicatorSettings.aroonDownLine),IndicatorSettings.aroonVisible,fixedLower=0.0,fixedUpper=100.0) { settingsOpen=true }
    if(settingsOpen)IndicatorConfigDialog("Aroon",
        listOf("الفترة" to FieldRef({IndicatorSettings.aroonPeriod},{v->IndicatorSettings.aroonPeriod=v})),
        listOf("aroonUp" to IndicatorSettings.aroonUpLine,"aroonDown" to IndicatorSettings.aroonDownLine),
        IndicatorSettings.aroonVisible,{IndicatorSettings.aroonVisible=!IndicatorSettings.aroonVisible},{IndicatorSettings.aroonEnabled=false}) { settingsOpen=false }
}
@Composable private fun AtrPane() {
    val symbol by TradeStore.currentSymbol.collectAsState()
    val snapshot=IndicatorSettings.snapshot?.takeIf { it.symbol==symbol&&it.frame==ChartSettings.frame }
    val period=IndicatorSettings.atrPeriod
    val values=remember(snapshot?.bars,period) { atrValues(snapshot?.bars ?: emptyList(),period) }
    var settingsOpen by remember { mutableStateOf(false) }
    IndicatorLinePane("Average True Range ${'$'}period",listOf(values to IndicatorSettings.atrLine),IndicatorSettings.atrVisible) { settingsOpen=true }
    if(settingsOpen)IndicatorConfigDialog("Average True Range",
        listOf("الفترة" to FieldRef({IndicatorSettings.atrPeriod},{v->IndicatorSettings.atrPeriod=v})),
        listOf("atr" to IndicatorSettings.atrLine),
        IndicatorSettings.atrVisible,{IndicatorSettings.atrVisible=!IndicatorSettings.atrVisible},{IndicatorSettings.atrEnabled=false}) { settingsOpen=false }
}
@Composable private fun AwesomeOscillatorPane() {
    val symbol by TradeStore.currentSymbol.collectAsState()
    val snapshot=IndicatorSettings.snapshot?.takeIf { it.symbol==symbol&&it.frame==ChartSettings.frame }
    val fast=IndicatorSettings.awesomeFast;val slow=IndicatorSettings.awesomeSlow
    val values=remember(snapshot?.bars,fast,slow) { awesomeValues(snapshot?.bars ?: emptyList(),fast,slow) }
    var expanded by remember { mutableStateOf(true) }
    var settingsOpen by remember { mutableStateOf(false) }
    val paneVisible=IndicatorSettings.awesomeVisible
    val paneHeight=(LocalConfiguration.current.screenHeightDp*.145f).coerceIn(88f,132f).dp
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(Modifier.fillMaxWidth().height(if(expanded&&paneVisible)paneHeight else 28.dp).background(Color(0xFF1D2332))) {
            CachedTradingImage(url=CHART_BG_URL,contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Color(0xFF1D2332).copy(alpha=.88f)))
            Canvas(Modifier.fillMaxSize()) {
                val w=size.width;val h=size.height
                drawLine(Color(0xFF3C6686).copy(alpha=.7f),Offset.Zero,Offset(w,0f),1.dp.toPx())
                if(!expanded||!paneVisible)return@Canvas
                val data=snapshot ?: return@Canvas
                val view=data.viewport
                fun xFor(time:Long)=(view.fractionFor((time-data.origin).toDouble()/data.intervalMs)*w).toFloat()
                val visibleBars=data.bars.indices.filter { values.getOrNull(it)?.isFinite()==true&&xFor(data.bars[it].time) in -16f..(w+16f) }
                val top=19.dp.toPx();val bottom=h-10.dp.toPx()
                if(bottom<=top)return@Canvas
                val minValue=visibleBars.minOfOrNull { values[it] } ?: 0.0
                val maxValue=visibleBars.maxOfOrNull { values[it] } ?: 0.0
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
                    for(i in visibleBars) {
                        val value=values[i];val y=yFor(value)
                        val previous=values.getOrNull(i-1) ?: Double.NaN
                        val color=if(!previous.isFinite()||value>=previous)IndicatorSettings.awesomeUp.color else IndicatorSettings.awesomeDown.color
                        if(abs(y-zero)>.05f)drawRect(color,Offset(xFor(data.bars[i].time)-width/2,min(y,zero)),Size(width,abs(y-zero)))
                    }
                }
                val paint=NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { color=Color(0xFF8D98AB).toArgb();textSize=10.sp.toPx();textAlign=NativePaint.Align.RIGHT }
                val magnitude=max(abs(upper),abs(lower))
                val decimals=if(magnitude<1.0)ceil(-log10(magnitude.coerceAtLeast(1e-12))).toInt().plus(1).coerceIn(2,8) else 2
                fun label(v:Double)=if(v==0.0)"0" else String.format(Locale.US,"% .${'$'}{decimals}f",v).trim()
                for((v,y) in listOf(upper to top,0.0 to zero,lower to bottom)) {
                    drawContext.canvas.nativeCanvas.drawText(label(v),w-5.dp.toPx(),(y-(paint.ascent()+paint.descent())/2).coerceIn(11.dp.toPx(),h-2.dp.toPx()),paint)
                }
            }
            PaneHeader(expanded,"Awesome Oscillator ${'$'}fast ${'$'}slow",{expanded=!expanded},{settingsOpen=true},Modifier.align(Alignment.TopStart))
            if(expanded&&paneVisible&&(snapshot==null||values.none { it.isFinite() }))Text("بانتظار بيانات كافية للمؤشر",color=Color(0xFF8D98AB),fontSize=10.sp,modifier=Modifier.align(Alignment.Center))
        }
    }
    if(settingsOpen)IndicatorConfigDialog("Awesome Oscillator",
        listOf("فترة قصيرة" to FieldRef({IndicatorSettings.awesomeFast},{v->IndicatorSettings.awesomeFast=v}),"فترة طويلة" to FieldRef({IndicatorSettings.awesomeSlow},{v->IndicatorSettings.awesomeSlow=v})),
        listOf("الصاعد" to IndicatorSettings.awesomeUp,"الهابط" to IndicatorSettings.awesomeDown),
        IndicatorSettings.awesomeVisible,{IndicatorSettings.awesomeVisible=!IndicatorSettings.awesomeVisible},{IndicatorSettings.awesomeEnabled=false}) { settingsOpen=false }
}
// The Alligator is drawn over the main chart, on the same price scale as the candles.
@Composable private fun AlligatorOverlay(modifier:Modifier=Modifier) {
    val symbol by TradeStore.currentSymbol.collectAsState()
    val snapshot=IndicatorSettings.snapshot?.takeIf { it.symbol==symbol&&it.frame==ChartSettings.frame }
    val s=IndicatorSettings
    val(jaw,teeth,lips)=remember(snapshot?.bars,s.jawPeriod,s.jawShift,s.teethPeriod,s.teethShift,s.lipsPeriod,s.lipsShift) {
        alligatorValues(snapshot?.bars ?: emptyList(),s.jawPeriod,s.jawShift,s.teethPeriod,s.teethShift,s.lipsPeriod,s.lipsShift)
    }
    var settingsOpen by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier) {
            Canvas(Modifier.matchParentSize()) {
                if(!s.alligatorVisible)return@Canvas
                val data=snapshot ?: return@Canvas
                val w=size.width;val h=size.height
                val view=data.viewport
                fun xFor(time:Long)=(view.fractionFor((time-data.origin).toDouble()/data.intervalMs)*w).toFloat()
                fun yFor(price:Double)=((view.maxPrice-price)/view.priceSpan*h).toFloat()
                clipRect(0f,0f,w,h) {
                    for((values,style) in listOf(jaw to s.jawLine,teeth to s.teethLine,lips to s.lipsLine)) {
                        if(!style.visible)continue
                        val path=Path();var pen=false
                        for(i in data.bars.indices) {
                            val v=values.getOrNull(i) ?: Double.NaN
                            if(!v.isFinite()) { pen=false;continue }
                            val x=xFor(data.bars[i].time);val y=yFor(v)
                            if(!pen) { path.moveTo(x,y);pen=true } else path.lineTo(x,y)
                        }
                        drawPath(path,style.color,style=Stroke((style.widthDp.dp.toPx()).coerceAtLeast(.5f),cap=StrokeCap.Round,join=StrokeJoin.Round))
                    }
                }
            }
            Row(Modifier.align(Alignment.TopStart).padding(start=8.dp,top=84.dp).height(27.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("Alligator ${'$'}{s.jawPeriod} ${'$'}{s.teethPeriod} ${'$'}{s.lipsPeriod}",color=Color(0xFFF2F3F6),fontSize=12.sp,maxLines=1,modifier=Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF1D2332).copy(alpha=.65f)).padding(horizontal=6.dp,vertical=3.dp))
                Box(Modifier.size(27.dp).clickable { settingsOpen=true },contentAlignment=Alignment.Center) { PencilIcon(Modifier.size(13.dp)) }
            }
        }
    }
    if(settingsOpen)IndicatorConfigDialog("Alligator",
        listOf(
            "الفترة الفكية" to FieldRef({s.jawPeriod},{v->s.jawPeriod=v}),
            "التحول الفكي" to FieldRef({s.jawShift},{v->s.jawShift=v}),
            "فترة الأسنان" to FieldRef({s.teethPeriod},{v->s.teethPeriod=v}),
            "التحول السني" to FieldRef({s.teethShift},{v->s.teethShift=v}),
            "فترة الشفاه" to FieldRef({s.lipsPeriod},{v->s.lipsPeriod=v}),
            "التحول الشفهي" to FieldRef({s.lipsShift},{v->s.lipsShift=v})
        ),
        listOf("jaw" to s.jawLine,"teeth" to s.teethLine,"lips" to s.lipsLine),
        s.alligatorVisible,{s.alligatorVisible=!s.alligatorVisible},{s.alligatorEnabled=false}) { settingsOpen=false }
}

@Composable fun LiveCandlestickChart(modifier:Modifier=Modifier,onOpenPairPicker:()->Unit) {
    val sym by TradeStore.currentSymbol.collectAsState()
    val frame=ChartSettings.frame
    val intervalMs=chartFrameMillis(frame)
    val chartType=ChartSettings.chartType
    val autoScrollEnabled by rememberUpdatedState(ChartSettings.autoScrollEnabled)
    fun barPosition(time:Long,origin:Long)=(time-origin).toDouble()/intervalMs
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
        val(center,span)=verticalTarget(viewport,candles,origin,intervalMs); viewport=viewport.copy(centerPrice=center,priceSpan=span)
    }
    fun panOnly(dx:Float) {
        val width=chartSize.width.toDouble()
        if(width<=1||!dx.isFinite()||dx==0f||candles.isEmpty())return
        val view=viewport; val desired=view.centerBar-dx*PAN_GAIN/width*view.visibleBars
        val first=barPosition(candles.first().time,origin); val last=barPosition(candles.last().time,origin)
        val center=boundedChartCenter(view,desired,first,last)
        val next=view.copy(anchorBar=center,anchorFraction=.5); val(pc,span)=verticalTarget(next,candles,origin,intervalMs)
        viewport=next.copy(centerPrice=pc,priceSpan=span)
        followLive=next.atLiveBoundary(last)
    }
    val onBegin by rememberUpdatedState<()->Unit>({interacting=true;inertia=0.0})
    val onEnd by rememberUpdatedState<()->Unit>({interacting=false})
    val onPinchStart by rememberUpdatedState<()->Unit>({beginPinch()})
    val onZoom by rememberUpdatedState<(Double)->Unit>({zoomOnly(it)})
    val onPan by rememberUpdatedState<(Float)->Unit>({ dx->panOnly(dx) })
    // Velocity is bars/second, independent of pointer-event frequency and display refresh rate.
    val onFling by rememberUpdatedState<(Float)->Unit>({ velocity->
        if(chartSize.width>1&&candles.isNotEmpty()&&velocity.isFinite()) {
            inertia=-velocity.coerceIn(-12000f,12000f)*PAN_GAIN/chartSize.width*viewport.visibleBars
        }
    })

    LaunchedEffect(intervalMs) {
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
                // Receiving a new bar must not cancel follow mode while the next
                // network request is suspended; only gestures/clamping change it.
            }
            if(!interacting&&candles.isNotEmpty()&&abs(inertia)>0.0) {
                val vp=viewport;val decay=exp(-INERTIA_FRICTION*dt)
                val desired=vp.centerBar+inertia*(1.0-decay)/INERTIA_FRICTION
                val first=barPosition(candles.first().time,origin);val live=barPosition(candles.last().time,origin)
                val center=boundedChartCenter(vp,desired,first,live)
                val next=vp.copy(anchorBar=center,anchorFraction=.5);val(pc,span)=verticalTarget(next,candles,origin,intervalMs)
                viewport=next.copy(centerPrice=pc,priceSpan=span)
                inertia=if(abs(center-desired)>1e-9)0.0 else inertia*decay
                if(abs(inertia)*chartSize.width/vp.visibleBars<5.0)inertia=0.0
                followLive=next.atLiveBoundary(live)
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
    LaunchedEffect(sym,frame,retryKey) {
        val started=System.currentTimeMillis();val initial=!everLoaded
        isLoading=true;loadError=false;candles.clear();displayCandle=null;livePrice=0f;TradeStore.livePrice.value=0f;followLive=true;inertia=0.0
        var success=false
        repeat(10) {
            if(!success) {
                val bars=BiquoteApi.chartHistory(sym,frame)
                if(bars.isNotEmpty()) {
                    origin=bars.first().time;for(b in bars)mergeCandle(candles,b,true)
                    while(candles.size>1500)candles.removeAt(0)
                    if(candles.isNotEmpty()) {
                        viewport=initialViewport(candles,origin,intervalMs);bsTarget=viewport.visibleBars;livePrice=candles.last().close
                        displayCandle=candles.last();success=true
                        // Historical OHLC is not represented as a fresh live quote.
                        BiquoteApi.updateChartQuote(sym) { price,_->TradeStore.quote(sym,price);livePrice=price.toFloat() }
                    }
                } else delay(1500)
            }
        }
        if(initial)delay((2150-(System.currentTimeMillis()-started)).coerceAtLeast(0))
        isLoading=false
        if(!success) { loadError=true;return@LaunchedEffect }
        everLoaded=true
        var newestTickTime=0L
        var lastRefresh=System.currentTimeMillis()
        while(isActive) {
            delay(1000)
            val refreshEvery=if(intervalMs<MINUTE_MS)3000L else 5000L
            // History is merged before the newest quote so an older server bar cannot
            // replace the latest close. For seconds, never subdivide a minute candle.
            if(System.currentTimeMillis()-lastRefresh>=refreshEvery) {
                val lastBars=BiquoteApi.chartHistory(sym,frame,true)
                for(b in lastBars) {
                    val existing=candles.firstOrNull { it.time==b.time }
                    if(existing!=null&&newestTickTime>=b.time&&newestTickTime<b.time+intervalMs)
                        mergeCandle(candles,b.copy(close=existing.close),true)
                    else mergeCandle(candles,b,true)
                }
                lastRefresh=System.currentTimeMillis()
            }
            BiquoteApi.updateChartQuote(sym) { price,tickTime->
                if(tickTime>=newestTickTime) {
                    livePrice=price.toFloat();TradeStore.quote(sym,price)
                    val bucket=tickTime/intervalMs*intervalMs
                    val lastC=candles.lastOrNull()
                    if(lastC==null||bucket>=lastC.time) {
                        val existing=lastC?.takeIf { it.time==bucket }
                        mergeCandle(candles,if(existing!=null)existing.copy(high=max(existing.high,livePrice),low=min(existing.low,livePrice),close=livePrice) else Candle(livePrice,livePrice,livePrice,livePrice,bucket))
                    }
                    newestTickTime=tickTime
                }
            }
            while(candles.size>1500)candles.removeAt(0)
            if(autoScrollEnabled&&followLive&&!interacting&&candles.isNotEmpty()) {
                val live=barPosition(candles.last().time,origin)
                val v=viewport.copy(anchorBar=live,anchorFraction=LIVE_CANDLE_FRACTION)
                val(pc,span)=verticalTarget(v,candles,origin,intervalMs);viewport=v.copy(centerPrice=pc,priceSpan=span)
            }
        }
    }

    LaunchedEffect(intervalMs) {
        while(isActive) {
            delay(16)
            val real=candles.lastOrNull() ?: continue;val dv=displayCandle
            displayCandle=when {
                dv==null->real
                dv.time!=real.time->real
                else->{val o=dv.open+(real.open-dv.open)*DV_ALPHA;val h=dv.high+(real.high-dv.high)*DV_ALPHA;val l=dv.low+(real.low-dv.low)*DV_ALPHA;val c=dv.close+(real.close-dv.close)*DV_ALPHA
                    val final=if(abs(real.close-c)<.0000001f)real.close else c
                    Candle(o,maxOf(h,final,o),minOf(l,final,o),final,real.time)
                }
            }
        }
    }
    LaunchedEffect(intervalMs) { while(isActive) { delay(100);nowMs=System.currentTimeMillis();clockText=clockNow();candles.lastOrNull()?.let { secondsLeft=remainingSeconds(it.time,nowMs,intervalMs) } } }
    // Publish only the existing chart data; no separate feed or simulated indicator bars.
    if(IndicatorSettings.anyActive()) {
        val indicatorBars=if(isLoading||loadError)emptyList() else candles.map { real->
            displayCandle?.takeIf { it.time==real.time } ?: real
        }
        val indicatorSnapshot=IndicatorChartSnapshot(indicatorBars,viewport,origin,intervalMs,sym,frame)
        SideEffect { IndicatorSettings.snapshot=indicatorSnapshot }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier.background(C.ChartBg)) {
            CachedTradingImage(url=CHART_BG_URL,contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha=.25f)))
            if(isLoading) {
                ReferenceChartLoading(Modifier.matchParentSize(),sym+"-"+frame+"-"+retryKey,!everLoaded)
            } else if(loadError) {
                Column(Modifier.align(Alignment.Center).background(Ref.Background,RoundedCornerShape(12.dp)).border(1.dp,C.Red,RoundedCornerShape(12.dp)).clickable { retryKey++ }.padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("لا توجد بيانات حقيقية",color=C.Red,fontSize=14.sp,fontWeight=FontWeight.Bold)
                    Text("تحقق من الاتصال وتوفر بيانات هذا الفريم، ثم اضغط لإعادة المحاولة",color=C.TextSecondary,fontSize=11.sp)
                }
            } else Canvas(Modifier.fillMaxSize().onSizeChanged { chartSize=it }
                .pointerInput(Unit) { detectTapGestures { pos->resultHits.asReversed().firstOrNull { it.second.contains(pos) }?.let { hit->TradeStore.results.removeAll { it.trade.id==hit.first } } } }
                .pointerInput(Unit) {
                    try { awaitEachGesture {
                        val down=awaitFirstDown(requireUnconsumed=false);onBegin()
                        val tracker=VelocityTracker();tracker.addPosition(down.uptimeMillis,down.position)
                        var pinchStarted=false;var dragging=false;var pan=Offset.Zero;var zoom=1f
                        var releaseVelocity=0f;var completed=false
                        val slop=viewConfiguration.touchSlop
                        try { do {
                            val event=awaitPointerEvent();val pressed=event.changes.count { it.pressed };val stable=event.changes.count { it.pressed&&it.previousPressed }
                            if(!pinchStarted)event.changes.firstOrNull { it.id==down.id }?.let { tracker.addPosition(it.uptimeMillis,it.position) }
                            if(pressed>=2) {
                                if(!pinchStarted) { pinchStarted=true;dragging=false;zoom=1f;tracker.resetTracking();onPinchStart() }
                                if(pressed==stable) { val z=event.calculateZoom()
                                    if(z.isFinite()&&z>0) { if(!dragging) { zoom*=z;val r=event.calculateCentroidSize(useCurrent=false);if(abs(1-zoom)*r>slop) { dragging=true;onZoom(zoom.toDouble()) } } else onZoom(z.toDouble()) }
                                };event.changes.forEach { it.consume() }
                            } else if(pressed==1&&stable==1&&!pinchStarted) {
                                val p=event.calculatePan()
                                if(!dragging) { pan+=p;if(abs(pan.x)>slop) { dragging=true;onPan(pan.x-if(pan.x>0)slop else -slop) } } else onPan(p.x)
                                if(dragging)event.changes.forEach { it.consume() }
                            } else if(pinchStarted)event.changes.forEach { it.consume() }
                            if(pressed==0) {
                                completed=true
                                if(dragging&&!pinchStarted)releaseVelocity=tracker.calculateVelocity().x
                            }
                        } while(event.changes.any { it.pressed }) } finally { onEnd() }
                        if(completed&&abs(releaseVelocity)>=80f)onFling(releaseVelocity)
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
                val om=origin/intervalMs;val left=view.centerBar-view.visibleBars/2+om;val right=view.centerBar+view.visibleBars/2+om
                val tFirst=ceil(left/timeStep).toLong();val tLast=floor(right/timeStep).toLong()
                if(tLast>=tFirst&&tLast-tFirst<1000)for(t in tFirst..tLast) {
                    val minute=t*timeStep;val x=xFor((minute-om).toDouble())
                    drawLine(C.Grid.copy(alpha=.28f),Offset(x,0f),Offset(x,h),1.dp.toPx())
                    val label=candleTimeText(minute*intervalMs,intervalMs);val half=timePaint.measureText(label)/2
                    if(x-half>=0&&x+half<=w)native.drawText(label,x,baseline(h-7.dp.toPx(),timePaint),timePaint)
                }
                val spacing=w/view.visibleBars.toFloat();val bw=(spacing*.85f).coerceIn(min(MINBW,spacing*.95f),spacing*.95f)
                val wick=min(1.2.dp.toPx(),bw)
                clipRect(0f,0f,w,h) {
                    val source=candles.mapIndexed { index,real->if(index==lastIdx&&dv!=null&&dv.time==real.time)dv else real }
                    val rendered=if(chartType=="هيكين آشي")heikinAshi(source) else source
                    if(chartType=="رسم بياني") {
                        val line=Path();var began=false
                        for(candle in rendered) {
                            val x=xFor(barPosition(candle.time,origin));val yClose=yFor(candle.close.toDouble())
                            if(!began) { line.moveTo(x,yClose);began=true } else line.lineTo(x,yClose)
                        }
                        drawPath(line,C.Cyan,style=Stroke(1.8.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
                    } else for(candle in rendered) {
                        val x=xFor(barPosition(candle.time,origin))
                        if(x+bw/2<0||x-bw/2>w)continue
                        val col=if(candle.close>=candle.open)C.Green else C.Red
                        val yH=yFor(candle.high.toDouble());val yL=yFor(candle.low.toDouble());val yO=yFor(candle.open.toDouble());val yC=yFor(candle.close.toDouble())
                        drawLine(col,Offset(x,yH),Offset(x,yL),wick,StrokeCap.Butt)
                        if(chartType=="أشرطة") {
                            drawLine(col,Offset(x-bw/2,yO),Offset(x,yO),wick,StrokeCap.Butt)
                            drawLine(col,Offset(x,yC),Offset(x+bw/2,yC),wick,StrokeCap.Butt)
                        } else if(abs(yO-yC)<1.dp.toPx())drawLine(col,Offset(x-bw/2,(yO+yC)/2),Offset(x+bw/2,(yO+yC)/2),1.dp.toPx())
                        else drawRect(col,Offset(x-bw/2,min(yO,yC)),Size(bw,abs(yO-yC)))
                    }
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
                    val labelHalf=max(detailPaint.measureText(frame),detailPaint.measureText(countdown))/2
                    val labelX=min(candleX+3f*(dash+gap),w-labelHalf-5.dp.toPx())
                    if(ChartSettings.countdownEnabled&&candleX in 0f..w&&labelX-labelHalf>candleX+bw/2) {
                        native.drawText(frame,labelX,y-11.dp.toPx(),detailPaint)
                        native.drawText(countdown,labelX,y+21.dp.toPx(),detailPaint)
                    }
                }
                clipRect(0f,0f,w,h) {
                    val liveX=xFor(barPosition(candles.last().time,origin))
                    drawReferenceTrades(sym,AccountStore.selected,nowMs,liveX,{ time->xFor(barPosition(time,origin)) },::yFor,resultHits,intervalMs)
                }
            }
            if(IndicatorSettings.alligatorEnabled)AlligatorOverlay(Modifier.matchParentSize())
            PairHeader(Modifier.align(Alignment.TopStart).padding(start=4.dp,top=8.dp),onOpenPairPicker)
            Column(Modifier.align(Alignment.TopStart).padding(start=14.dp,top=60.dp)) {
                Text("$clockText UTC+3",color=C.TextSecondary,fontSize=11.sp)
            }
            TopNotificationOverlay(Modifier.align(Alignment.TopStart))
        }
    }
}

private data class MarkerPlacement(val trade:OpenTrade,val y:Float,val dot:Float,val dotY:Float,val right:Float,val left:Float,val width:Float)
// Trade markers are chart-anchored, never screen-anchored: every label sits on one straight
// horizontal line at its entry price, behind its entry dot, and never ahead of the active
// (live) candle. All positions come from xFor/yFor, so they pan and zoom with the chart.
private fun DrawScope.drawReferenceTrades(symbol:String,account:String,now:Long,liveX:Float,xFor:(Long)->Float,yFor:(Double)->Float,hits:MutableList<Pair<Long,Rect>>,intervalMs:Long=MINUTE_MS) {
    fun px(n:Float)=n*density
    val w=size.width;val h=size.height;val native=drawContext.canvas.nativeCanvas
    val amountPaint=NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { typeface=Typeface.create("Arial",Typeface.BOLD);textSize=px(10f);textAlign=NativePaint.Align.LEFT }
    val places=mutableListOf<MarkerPlacement>()
    val trades=TradeStore.openTrades.filter { it.symbol==symbol&&it.account==account }.sortedWith(compareBy<OpenTrade> { it.candleTime }.thenBy { it.openedAt }.thenBy { it.id })
    if(w>0f&&h>0f)for(tr in trades) {
        val dot=xFor(tr.openedAt/intervalMs*intervalMs)
        val dotY=yFor(tr.exactEntry)
        if(dotY<px(-40f)||dotY>h+px(40f))continue
        val width=amountPaint.measureText("$"+shortAmount(tr.amount))+px(16f)
        // The label ends behind the entry dot and never crosses ahead of the active candle.
        val right=min(dot,liveX)-px(6f)
        places.add(MarkerPlacement(tr,dotY,dot,dotY,right,right-width,width))
    }
    for(p in places) {
        val color=if(p.trade.direction>0)Color(0xFF4CAF50) else Color(0xFFF44336)
        // One straight horizontal line on the entry price, from the label to the dot.
        drawLine(color.copy(alpha=.6f),Offset(p.right,p.dotY),Offset(p.dot,p.dotY),px(1f))
        drawCircle(color,px(4f),Offset(p.dot,p.dotY));drawCircle(Color(0xFFEAFFF5),px(1.5f),Offset(p.dot,p.dotY))
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
    var expanded by remember { mutableStateOf(false) }
    var manualOpen by remember { mutableStateOf(false) }
    val density=LocalDensity.current;val config=LocalConfiguration.current
    Column(modifier) {
        Text("الزمن",color=C.TextSecondary,fontSize=12.sp);Spacer(Modifier.height(6.dp))
        Box {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).background(C.Surface).border(1.dp,C.Divider,RoundedCornerShape(10.dp)).clickable { expanded=!expanded }) {
                    Text(TradeSettings.duration,color=C.TextPrimary,fontSize=14.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,style=TextStyle(textDirection=TextDirection.Ltr),modifier=Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal=38.dp),maxLines=1)
                    ReferenceGlyph("time",Modifier.align(Alignment.CenterEnd).padding(end=12.dp).size(20.dp),C.TextSecondary)
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
        // This editor owns its own window: focus is no longer lost to the preset popup.
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
@Composable private fun AmountInput(modifier:Modifier=Modifier) {
    val focusManager=LocalFocusManager.current
    Column(modifier) {
        Text("المبلغ",color=C.TextSecondary,fontSize=12.sp);Spacer(Modifier.height(6.dp))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).background(C.Surface).border(1.dp,C.Divider,RoundedCornerShape(10.dp))) {
                BasicTextField(TradeSettings.amount,{value->
                    if(value.isEmpty()||Regex("[0-9]*(?:\\.[0-9]*)?").matches(value))TradeSettings.amount=value
                },modifier=Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal=38.dp).onFocusChanged { if(!it.isFocused)TradeSettings.amount=TradeSettings.amount },singleLine=true,cursorBrush=SolidColor(C.Cyan),visualTransformation=DollarPrefixTransformation,textStyle=TextStyle(color=C.TextPrimary,fontSize=14.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,textDirection=TextDirection.Ltr),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal,imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={TradeSettings.amount=TradeSettings.amount;focusManager.clearFocus()}))
                ReferenceGlyph("amount",Modifier.align(Alignment.CenterEnd).padding(end=12.dp).size(20.dp),C.TextSecondary)
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
