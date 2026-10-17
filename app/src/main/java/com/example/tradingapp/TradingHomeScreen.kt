package com.example.tradingapp

import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*
import kotlin.random.Random
import android.graphics.Paint as NativePaint

const val AVATAR_URL = "https://i.ibb.co/Z68jNfbd/IMG-20250906-195228-439.jpg"
const val AVATAR_FRAME_URL = "https://i.ibb.co/5xvxC8Kw/file-00000000d4f882439f813f969679659f.png"
const val CHART_BG_URL = "https://i.ibb.co/zWHWPv92/file-0000000090c48211944cf52560e3482a.png"
const val GIFT_ICON_URL = "https://cdn-icons-png.flaticon.com/128/18546/18546266.png"

object C {
    val BG = Color(0xFF141126)
    val Surface = Color(0xFF24203D)
    val SurfaceRaised = Color(0xFF30284F)
    val Cyan = Color(0xFF58D9F5)
    val Green = Color(0xFF39FF14)
    val Red = Color(0xFFFF1A1A)
    val Blue = Cyan
    val Yellow = Color(0xFFFFD34D)
    val TextPrimary = Color(0xFFF5F2FF)
    val TextSecondary = Color(0xFFA9A5C6)
    val Divider = Color(0xFF3D355A)
    val Grid = Color(0xFF67558D)
    val PriceTag = Cyan
    val PkBg = Color(0xFF2B2659)
    val PkBd = Color(0xFF223252)
    val PkTab = Color(0xFF1B2233)
    val PkTabOn = Color(0xFF2F80ED)
    val PkStarOn = Color(0xFFF5A623)
    val PkPct = Color(0xFF71B982)
    val BarBg = Color(0xFF344766)
    val BarOpenGreen = Color(0xFF2EBD85)
    val ChartBg = Color(0xFF3C3861)
}

private const val MINUTE_MS = 60_000L
private const val MIN_PRICE_SPAN = 0.00001
private const val LIVE_CANDLE_FRACTION = 0.80
private const val MINBW = 2f
private const val PAD = 6f
private const val WHEEL_FACTOR = 1.32
private const val PINCH_AMPLIFY = 4.0
private const val ZOOM_SMOOTH = 0.22f
private const val INERTIA_DECAY = 0.93
private const val MIN_VISIBLE_BARS_LIMIT = 10.0
private const val MAX_VISIBLE_BARS_LIMIT = 120.0
private const val DV_ALPHA = 0.20f

private fun barPosition(time: Long, origin: Long): Double =
    (time - origin).toDouble() / MINUTE_MS.toDouble()

data class Candle(
    val open: Float, val high: Float, val low: Float, val close: Float,
    val time: Long = 0L
)

fun clockNow(): String = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("GMT+3")
}.format(Date())

fun fmt(price: Float): String = String.format(Locale.US, "%.5f", price)
private fun priceText(price: Double, decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", price)
private fun candleTimeText(time: Long): String =
    SimpleDateFormat("HH:mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT+3")
    }.format(Date(time))
private fun remainingSeconds(openTime: Long, now: Long): Int =
    ceil((openTime + MINUTE_MS - now).toDouble() / 1000.0).toInt().coerceIn(0, 60)

object BiquoteApi {
    private const val BASE = "https://biquote.io"

    private fun get(path: String, params: List<Pair<String, String>> = emptyList()): String? {
        val qs = if (params.isEmpty()) "" else "?" + params.joinToString("&") {
            "${it.first}=${URLEncoder.encode(it.second, "UTF-8")}"
        }
        return try {
            val c = (URL(BASE + path + qs).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"; connectTimeout = 6000; readTimeout = 6000
            }
            if (c.responseCode in 200..299) c.inputStream.bufferedReader().use { it.readText() }
            else null
        } catch (e: Exception) { null }
    }

    suspend fun tick(symbol: String): Double? = withContext(Dispatchers.IO) {
        val txt = get("/api/${symbol.uppercase()}", listOf("allowStale" to "false")) ?: return@withContext null
        try { JSONObject(txt).optDouble("mid", Double.NaN).takeIf { it.isFinite() && it > 0 } }
        catch (e: Exception) { null }
    }

    suspend fun latest(symbols: List<String>): Map<String, Double> = withContext(Dispatchers.IO) {
        if (symbols.isEmpty()) return@withContext emptyMap()
        val params = symbols.take(100).map { "symbols" to it }
        val txt = get("/api/latest", params) ?: return@withContext emptyMap()
        try {
            val arr = JSONArray(txt)
            val map = mutableMapOf<String, Double>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("symbol", obj.optString("name", ""))
                val mid = obj.optDouble("mid", Double.NaN)
                if (name.isNotBlank() && mid.isFinite() && mid > 0) {
                    map[name.uppercase()] = mid
                }
            }
            map
        } catch (e: Exception) { emptyMap() }
    }

    suspend fun ohlc(symbol: String, interval: String = "1m", limit: Int = 300): List<Candle> =
        withContext(Dispatchers.IO) {
            val txt = get("/api/${symbol.uppercase()}/ohlc",
                listOf("interval" to interval, "limit" to limit.toString())) ?: return@withContext emptyList()
            try {
                val root = JSONObject(txt)
                val arr: JSONArray = root.optJSONArray("bars") ?: return@withContext emptyList()
                val list = ArrayList<Candle>(arr.length())
                for (i in 0 until arr.length()) {
                    val b = arr.getJSONObject(i)
                    val t = (b.opt("openTime") ?: b.opt("time") ?: b.opt("t"))
                    val ts = when (t) {
                        is Number -> t.toLong()
                        is String -> try { java.time.Instant.parse(t).toEpochMilli() }
                                    catch (e: Exception) { t.toLongOrNull() ?: 0L }
                        else -> 0L
                    }
                    list.add(Candle(
                        open = b.optDouble("open", b.optDouble("o")).toFloat(),
                        high = b.optDouble("high", b.optDouble("h")).toFloat(),
                        low = b.optDouble("low", b.optDouble("l")).toFloat(),
                        close = b.optDouble("close", b.optDouble("c")).toFloat(),
                        time = ts
                    ))
                }
                list.sortedBy { it.time }
            } catch (e: Exception) { emptyList() }
        }

    suspend fun symbols(liveOnly: Boolean = false): List<String> = withContext(Dispatchers.IO) {
        val txt = get("/api/symbols", listOf("liveOnly" to liveOnly.toString())) ?: return@withContext emptyList()
        try {
            val arr = JSONArray(txt)
            val out = ArrayList<String>()
            for (i in 0 until arr.length()) {
                val v = arr.opt(i)
                val s = when (v) {
                    is String -> v
                    is JSONObject -> v.optString("name", v.optString("symbol", ""))
                    else -> ""
                }
                if (s.isNotBlank()) out.add(s.uppercase().replace(Regex("[_/\\-]"), ""))
            }
            out.distinct()
        } catch (e: Exception) { emptyList() }
    }
}

object SymCatalog {
    private val FOREX_CODES = setOf(
        "EUR", "USD", "GBP", "JPY", "CHF", "AUD", "CAD", "NZD", "TRY", "ZAR",
        "MXN", "SEK", "NOK", "SGD", "HKD", "PLN", "DKK", "HUF", "CZK", "ILS",
        "CNY", "QAR", "SAR", "UAH", "IDR", "THB", "MYR", "PHP", "INR", "KRW",
        "BRL", "ARS", "CLP", "COP", "PEN", "RUB", "AED"
    )
    private val CRYPTO_CODES = setOf(
        "BTC", "ETH", "SOL", "XRP", "BNB", "ADA", "DOGE", "DOT", "MATIC", "AVAX",
        "LINK", "LTC", "TRX", "ATOM", "ETC", "XLM", "ALGO", "VET", "FIL", "NEAR",
        "APT", "ARB", "OP", "INJ", "SUI", "SEI", "TIA", "TON", "PEPE", "SHIB",
        "BCH", "EOS", "XMR", "DASH", "ZEC", "COMP", "MKR", "AAVE", "UNI", "SNX",
        "CRV", "SUSHI", "YFI", "BAL", "REN", "KNC", "ZRX", "BAT", "OMG", "STORJ",
        "MANA", "SAND", "AXS", "ENJ", "CHZ", "GALA", "IMX", "APE", "LDO", "RPL",
        "SSV", "BLUR", "WLD", "JUP", "PYTH", "STRK", "DYM", "MANTA", "ALT",
        "PIXEL", "PORTAL", "AEVO", "ETHFI", "ENA", "REZ", "SAGA", "TAO",
        "FET", "AGIX", "OCEAN", "RNDR", "AKT", "GLM", "HNT", "AR", "SC", "HIVE",
        "BLZ", "GRT", "LPT", "THETA", "FTM", "ONE", "HBAR", "ICP", "EGLD",
        "FLOW", "ROSE", "KAVA", "CELO", "BAND", "API3"
    )
    private val COMMODITY_CODES = setOf(
        "XAU", "XAG", "XPT", "XPD", "USOIL", "UKOIL", "WTI", "BRENT", "NGAS",
        "COPPER", "CORN", "WHEAT", "SOYBEAN", "COFFEE", "SUGAR", "COTTON", "COCOA"
    )
    private val INDEX_CODES = setOf(
        "US30", "US500", "USTEC", "UK100", "GER40", "FRA40", "JP225", "AUS200",
        "HK50", "EUSTX50", "SPX500", "NAS100", "DJ30"
    )

    val FLAGS = mapOf(
        "EUR" to "eu","USD" to "us","GBP" to "gb","JPY" to "jp","CHF" to "ch","AUD" to "au",
        "CAD" to "ca","NZD" to "nz","TRY" to "tr","ZAR" to "za","MXN" to "mx","SEK" to "se",
        "NOK" to "no","SGD" to "sg","HKD" to "hk","PLN" to "pl","DKK" to "dk","HUF" to "hu",
        "CZK" to "cz","ILS" to "il","CNY" to "cn","QAR" to "qa","SAR" to "sa","UAH" to "ua",
        "IDR" to "id","AED" to "ae"
    )

    // الترتيب مهم: الفوركس أولاً (لتفادي تعارض البادئات مثل ARS مع AR الكريبتو)
    fun isForex(s: String): Boolean =
        s.length == 6 && FOREX_CODES.any { s.startsWith(it) }

    fun isCrypto(s: String): Boolean =
        !isForex(s) && CRYPTO_CODES.any { s.startsWith(it) }

    fun isCommodity(s: String): Boolean =
        !isForex(s) && !isCrypto(s) && COMMODITY_CODES.any { s.startsWith(it) }

    fun isIndex(s: String): Boolean =
        INDEX_CODES.contains(s)

    fun isStock(s: String): Boolean =
        Regex("^[A-Z]{1,5}$").matches(s) &&
            !isForex(s) && !isCrypto(s) && !isCommodity(s) && !isIndex(s)

    fun pretty(s: String): String =
        if (s.length == 6) "${s.substring(0,3)}/${s.substring(3)}" else s

    fun base(s: String) = if (s.length >= 6) s.substring(0,3) else s
    fun quote(s: String) = if (s.length >= 6) s.substring(3) else ""
    fun flagUrl(cc: String?): String? = cc?.let { "https://flagcdn.com/${it}.svg" }

    fun cryptoIconUrl(symbol: String): String? {
        val base = base(symbol).lowercase()
        return if (base in CRYPTO_CODES.map { it.lowercase() }.toSet())
            "https://cdn.jsdelivr.net/npm/cryptocurrency-icons@0.18.1/svg/color/$base.svg"
        else null
    }

    fun payout(symbol: String): Int {
        var h = 0L
        for (c in symbol) h = ((h * 31L + c.code) and 0xFFFFFFFFL)
        return 80 + (h % 16).toInt()
    }
}

private fun mergeCandle(list: MutableList<Candle>, incoming: Candle, bulk: Boolean = false): Boolean {
    var c = incoming.close
    if (!c.isFinite() || c <= 0f) return false
    var o = incoming.open
    var h = incoming.high
    var l = incoming.low
    if (!o.isFinite() || o <= 0f) o = c
    if (!h.isFinite() || h <= 0f) h = maxOf(o, c)
    if (!l.isFinite() || l <= 0f) l = minOf(o, c)
    h = maxOf(h, o, c)
    l = minOf(l, o, c)
    val t = incoming.time
    if (t <= 0L) return false

    val n = list.size
    if (n == 0) {
        list.add(Candle(o, h, l, c, t))
        return true
    }

    var lo = 0; var hi = n - 1; var exact = -1; var insertAt = n
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        val mt = list[mid].time
        when {
            mt == t -> { exact = mid; break }
            mt < t -> lo = mid + 1
            else -> { insertAt = mid; hi = mid - 1 }
        }
    }

    if (exact >= 0) {
        val e = list[exact]
        list[exact] = e.copy(
            high = maxOf(e.high, h),
            low = minOf(e.low, l),
            close = c
        )
        return false
    }

    if (insertAt >= n) {
        o = list[n - 1].close
        h = maxOf(h, o)
        l = minOf(l, o)
        list.add(Candle(o, h, l, c, t))
    } else {
        list.add(insertAt, Candle(o, h, l, c, t))
    }
    return true
}

object TradeSounds {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private const val SR = 44100

    private fun playTones(tones: List<Triple<Float, Float, Pair<Float, String>>>) {
        scope.launch {
            val totalMs = tones.maxOf { (it.second + it.third.first * 1000f).toInt() } + 60
            val numSamples = (SR * totalMs / 1000)
            val buf = ShortArray(numSamples)
            for ((freq, startMs, dw) in tones) {
                val (dur, wave) = dw
                val startIdx = (startMs / 1000f * SR).toInt()
                val lenIdx = (dur * SR).toInt()
                for (i in 0 until lenIdx) {
                    val pos = startIdx + i
                    if (pos >= buf.size) break
                    val t = i.toDouble() / SR
                    val env = (0.9).pow(i / (SR * 0.08))
                    val x = 2.0 * PI * freq * t
                    val s = when (wave) {
                        "square"   -> if (sin(x) >= 0) 1.0 else -1.0
                        "sawtooth" -> 2.0 * (t * freq - floor(0.5 + t * freq))
                        "triangle" -> 2.0 / PI * asin(sin(x))
                        else       -> sin(x)
                    }
                    val sample = (s * env * 0.18 * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767)
                    buf[pos] = (buf[pos].toInt() + sample).coerceIn(-32768, 32767).toShort()
                }
            }
            try {
                val track = AudioTrack(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                    android.media.AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SR)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                    buf.size * 2, AudioTrack.MODE_STATIC, 0
                )
                track.write(buf, 0, buf.size)
                track.play()
                delay(totalMs.toLong() + 100)
                track.release()
            } catch (_: Exception) {}
        }
    }

    fun open() = playTones(listOf(
        Triple(1900f, 0f,   0.05f to "square"),
        Triple(760f,  0f,   0.06f to "triangle")
    ))
    fun win() = playTones(listOf(
        Triple(784f,  0f,   0.28f to "sine"),
        Triple(1046f, 90f,  0.28f to "sine"),
        Triple(1318f, 180f, 0.28f to "sine"),
        Triple(1568f, 270f, 0.40f to "sine")
    ))
    fun lose() = playTones(listOf(
        Triple(392f,  0f,   0.30f to "sawtooth"),
        Triple(294f, 120f,  0.30f to "sawtooth"),
        Triple(196f, 240f,  0.30f to "sawtooth")
    ))
}

private data class ChartViewport(
    val visibleBars: Double,
    val anchorBar: Double,
    val anchorFraction: Double,
    val centerPrice: Double,
    val priceSpan: Double
) {
    val centerBar get() = anchorBar + (0.5 - anchorFraction) * visibleBars
    val minPrice get() = centerPrice - priceSpan / 2.0
    val maxPrice get() = centerPrice + priceSpan / 2.0
    fun fractionFor(bar: Double) = anchorFraction + (bar - anchorBar) / visibleBars
    fun reanchor(bar: Double) = copy(anchorBar = bar, anchorFraction = fractionFor(bar))
}

private fun initialViewport(candles: List<Candle>, origin: Long): ChartViewport {
    if (candles.isEmpty()) return ChartViewport(60.0, 0.0, LIVE_CANDLE_FRACTION, 1.0, MIN_PRICE_SPAN)
    val visible = candles.takeLast(min(60, candles.size))
    val low = visible.minOf { it.low.toDouble() }
    val high = visible.maxOf { it.high.toDouble() }
    return ChartViewport(
        visibleBars = 60.0,
        anchorBar = barPosition(candles.last().time, origin),
        anchorFraction = LIVE_CANDLE_FRACTION,
        centerPrice = (low + high) / 2.0,
        priceSpan = ((high - low) / 0.70 * 1.04).coerceAtLeast(MIN_PRICE_SPAN)
    )
}

private fun verticalTarget(view: ChartViewport, candles: List<Candle>, origin: Long): Pair<Double, Double> {
    if (candles.isEmpty()) return view.centerPrice to view.priceSpan
    val visible = candles.filter {
        abs(barPosition(it.time, origin) - view.centerBar) <= view.visibleBars / 2.0 + 0.6
    }
    if (visible.isEmpty()) return view.centerPrice to view.priceSpan
    val low = visible.minOf { it.low.toDouble() }
    val high = visible.maxOf { it.high.toDouble() }
    return (high + low) / 2.0 to
        ((high - low) / (1.0 - 2.0 * 0.15) * 1.04).coerceAtLeast(MIN_PRICE_SPAN)
}

private fun nicePriceStep(rawStep: Double): Double {
    if (!rawStep.isFinite() || rawStep <= 0.0) return 0.001
    val magnitude = 10.0.pow(floor(log10(rawStep)))
    val normalized = rawStep / magnitude
    val nice = when {
        normalized <= 1.0 -> 1.0; normalized <= 2.0 -> 2.0
        normalized <= 2.5 -> 2.5; normalized <= 5.0 -> 5.0
        else -> 10.0
    }
    return nice * magnitude
}

private fun priceDecimals(step: Double): Int {
    for (decimals in 5..8) {
        val scaled = step * 10.0.pow(decimals.toDouble())
        if (abs(scaled - kotlin.math.round(scaled)) < 0.000001) return decimals
    }
    return 8
}

private fun niceTimeStep(rawMinutes: Double): Long =
    longArrayOf(1, 2, 5, 10, 15, 20, 30, 60, 120, 240)
        .firstOrNull { it.toDouble() >= rawMinutes } ?: 240L

data class OpenTrade(
    val id: Long = System.currentTimeMillis(),
    val symbol: String,
    val direction: Int,
    val entry: Float,
    val amount: Double,
    val openedAt: Long,
    val expiresAt: Long,
    val payoutPct: Int
)

object TradeStore {
    val currentSymbol = MutableStateFlow("EURUSD")
    val openTrades = mutableStateListOf<OpenTrade>()
    val closedTrades = mutableStateListOf<Pair<OpenTrade, Boolean>>()
}

@Composable
fun TradingHomeScreen() {
    var pickerOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(C.BG)) {
        TopBar()
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Divider))
        LiveCandlestickChart(
            Modifier.weight(1f).fillMaxWidth(),
            onOpenPairPicker = { pickerOpen = true }
        )
        TradePanel()
        BottomNavBar()
    }
    if (pickerOpen) {
        PairPickerSheet(onDismiss = { pickerOpen = false })
    }
}

@Composable
fun TopBar() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier.fillMaxWidth().background(C.BG).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(C.Yellow),
                contentAlignment = Alignment.Center) { WalletIcon(Modifier.size(26.dp), Color(0xFF312740)) }
            Spacer(Modifier.width(8.dp))
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).background(C.Surface)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("QT Real", color = C.TextSecondary, fontSize = 10.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("حلال", color = C.Green, fontSize = 10.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("USD", color = C.TextSecondary, fontSize = 10.sp)
                    }
                    Text("0.39", color = C.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp)); DownArrow(Modifier.size(10.dp))
            }
            Spacer(Modifier.weight(1f))
            Box {
                AsyncImage(model = GIFT_ICON_URL, contentDescription = "هدية",
                    contentScale = ContentScale.Fit, modifier = Modifier.size(36.dp))
                Box(Modifier.align(Alignment.TopEnd).offset(3.dp, (-3).dp).size(14.dp)
                    .clip(CircleShape).background(C.BG).border(1.dp, C.Divider, CircleShape)) {
                    Canvas(Modifier.fillMaxSize()) {
                        val m = size.minDimension * 0.22f
                        val cx = size.width / 2f; val cy = size.height / 2f
                        drawLine(Color.White, Offset(cx - m, cy - m), Offset(cx + m, cy + m),
                            1.dp.toPx(), cap = StrokeCap.Round)
                        drawLine(Color.White, Offset(cx + m, cy - m), Offset(cx - m, cy + m),
                            1.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                AsyncImage(model = AVATAR_URL, contentDescription = "الصورة الشخصية",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(C.Surface))
                AsyncImage(model = AVATAR_FRAME_URL, contentDescription = null,
                    contentScale = ContentScale.Fit, modifier = Modifier.size(52.dp))
            }
        }
    }
}

@Composable
fun PairHeader(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val sym by TradeStore.currentSymbol.collectAsState()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(RoundedCornerShape(22.dp)).background(C.Surface)
                    .clickable { onClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val base = SymCatalog.base(sym); val quote = SymCatalog.quote(sym)
                Box(Modifier.width(34.dp).height(22.dp)) {
                    val cryptoIcon = SymCatalog.cryptoIconUrl(sym)
                    if (cryptoIcon != null) {
                        AsyncImage(model = cryptoIcon, contentDescription = null,
                            modifier = Modifier.size(22.dp).clip(CircleShape).align(Alignment.CenterStart))
                    } else {
                        val baseFlag = SymCatalog.flagUrl(SymCatalog.FLAGS[base])
                        if (baseFlag != null) AsyncImage(model = baseFlag, contentDescription = null,
                            modifier = Modifier.size(22.dp).clip(CircleShape).align(Alignment.CenterStart))
                        else FlagAE(Modifier.size(22.dp).align(Alignment.CenterStart))
                    }
                    val quoteFlag = SymCatalog.flagUrl(SymCatalog.FLAGS[quote])
                    if (quoteFlag != null) AsyncImage(model = quoteFlag, contentDescription = null,
                        modifier = Modifier.size(22.dp).clip(CircleShape).align(Alignment.CenterEnd)
                            .border(1.5.dp, C.Surface, CircleShape))
                    else if (cryptoIcon == null) FlagCN(Modifier.size(22.dp).align(Alignment.CenterEnd)
                        .border(1.5.dp, C.Surface, CircleShape))
                }
                Spacer(Modifier.width(8.dp))
                Text("${SymCatalog.pretty(sym)} OTC", color = C.TextPrimary, fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp)); DownArrow(Modifier.size(9.dp))
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(C.Surface),
                contentAlignment = Alignment.Center) { DotsIcon(Modifier.size(16.dp)) }
        }
    }
}

@Composable
fun LiveCandlestickChart(modifier: Modifier = Modifier, onOpenPairPicker: () -> Unit) {
    val sym by TradeStore.currentSymbol.collectAsState()
    val candles = remember { mutableStateListOf<Candle>() }
    var displayCandle by remember { mutableStateOf<Candle?>(null) }
    var livePrice by remember { mutableStateOf(0f) }
    var origin by remember { mutableStateOf(0L) }
    var viewport by remember { mutableStateOf(ChartViewport(60.0, 0.0, LIVE_CANDLE_FRACTION, 1.0, MIN_PRICE_SPAN)) }
    var bsTarget by remember { mutableStateOf(60.0) }
    var followLive by remember { mutableStateOf(true) }
    var interacting by remember { mutableStateOf(false) }
    var chartSize by remember { mutableStateOf(IntSize.Zero) }
    var secondsLeft by remember { mutableStateOf(60) }
    var clockText by remember { mutableStateOf(clockNow()) }
    var inertia by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }

    val density = LocalDensity.current
    val axisTextPx = with(density) { 9.sp.toPx() }
    val tagTextPx = with(density) { 10.sp.toPx() }
    val axisPaint = remember(axisTextPx) {
        NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.toArgb(); textSize = axisTextPx
            textAlign = NativePaint.Align.RIGHT; typeface = Typeface.DEFAULT
        }
    }
    val timePaint = remember(axisTextPx) {
        NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = C.TextSecondary.toArgb(); textSize = axisTextPx
            textAlign = NativePaint.Align.CENTER
        }
    }
    val tagPaint = remember(tagTextPx) {
        NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = C.BG.toArgb(); textSize = tagTextPx
            textAlign = NativePaint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
        }
    }
    val detailPaint = remember(axisTextPx) {
        NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = C.Cyan.toArgb(); textSize = axisTextPx
            textAlign = NativePaint.Align.RIGHT
        }
    }

    fun ppw(): Float = (chartSize.width - 6f - PAD).coerceAtLeast(1f)
    fun pxPerBarMin(): Double = ((ppw() / (max(3f, 16f * 3.704f) + 8f)) * 0.9f).toDouble()
    fun pxPerBarMax(): Double = (ppw() / (max(3f, 16f * 0.207f) + 8f)).toDouble()
    fun clampVisible(v: Double): Double {
        val minBars = ppw().toDouble() / pxPerBarMax()
        val maxBars = ppw().toDouble() / pxPerBarMin()
        return v.coerceIn(
            max(MIN_VISIBLE_BARS_LIMIT, minBars),
            min(MAX_VISIBLE_BARS_LIMIT, maxBars)
        )
    }

    fun beginInteraction() { interacting = true }
    fun endInteraction() { interacting = false }

    fun beginPinch() {
        if (candles.isEmpty()) return
        val view = viewport
        val live = barPosition(candles.last().time, origin)
        val liveFraction = view.fractionFor(live)
        val pivot = if (liveFraction in 0.0..1.0) live else {
            candles.minByOrNull { abs(barPosition(it.time, origin) - view.centerBar) }
                ?.let { barPosition(it.time, origin) } ?: view.centerBar
        }
        viewport = view.reanchor(pivot)
        inertia = 0.0
    }

    fun zoomOnly(factor: Double) {
        if (!factor.isFinite() || factor <= 0.0) return
        val amplified = if (factor >= 1.0) 1.0 + (factor - 1.0) * PINCH_AMPLIFY
                        else 1.0 / (1.0 + (1.0 / factor - 1.0) * PINCH_AMPLIFY)
        val view = viewport
        val next = clampVisible(view.visibleBars / amplified)
        if (next == view.visibleBars) return
        viewport = view.copy(visibleBars = next)
        bsTarget = next
    }

    fun panOnly(dx: Float) {
        val width = chartSize.width.toDouble()
        if (width <= 1.0 || !dx.isFinite() || dx == 0f || candles.isEmpty()) return
        val view = viewport
        val desiredCenter = view.centerBar - dx.toDouble() / width * view.visibleBars
        val first = barPosition(candles.first().time, origin)
        val last = barPosition(candles.last().time, origin)
        val half = view.visibleBars / 2.0
        val minCenter = first + half - 1.0
        val maxCenter = last + half - 2.0
        val center = if (minCenter <= maxCenter)
            desiredCenter.coerceIn(minCenter, maxCenter) else view.centerBar
        val next = view.copy(anchorBar = center, anchorFraction = 0.5)
        val (priceCenter, span) = verticalTarget(next, candles, origin)
        viewport = next.copy(centerPrice = priceCenter, priceSpan = span)
        val liveCenter = last + (0.5 - LIVE_CANDLE_FRACTION) * next.visibleBars
        followLive = abs(center - liveCenter) < 0.75
    }

    val onBegin by rememberUpdatedState<() -> Unit>({ beginInteraction() })
    val onEnd by rememberUpdatedState<() -> Unit>({ endInteraction() })
    val onPinchStart by rememberUpdatedState<() -> Unit>({ beginPinch() })
    val onZoom by rememberUpdatedState<(Double) -> Unit>({ zoomOnly(it) })
    val onPan by rememberUpdatedState<(Float) -> Unit>({
        panOnly(it)
        inertia = inertia * 0.4 + (-it.toDouble() / chartSize.width.coerceAtLeast(1) * viewport.visibleBars) * 0.6
    })

    LaunchedEffect(Unit) {
        var last = System.nanoTime()
        while (true) {
            delay(16L)
            val nowNs = System.nanoTime()
            val df = ((nowNs - last) / 1_000_000.0).coerceAtMost(60.0)
            last = nowNs
            val view = viewport
            val diff = bsTarget - view.visibleBars
            if (abs(diff) > 0.003) {
                viewport = view.copy(visibleBars = view.visibleBars + diff * ZOOM_SMOOTH)
            } else if (view.visibleBars != bsTarget) {
                viewport = view.copy(visibleBars = bsTarget)
            }
            if (abs(inertia) > 1e-5 && !interacting && candles.isNotEmpty()) {
                val vp = viewport
                val newCenter = vp.centerBar + inertia * df * 0.016
                val first = barPosition(candles.first().time, origin)
                val lastB = barPosition(candles.last().time, origin)
                val half = vp.visibleBars / 2.0
                val minC = first + half - 1.0
                val maxC = lastB + half - 2.0
                val clamped = if (minC <= maxC) newCenter.coerceIn(minC, maxC) else vp.centerBar
                if (clamped != newCenter) inertia = 0.0
                viewport = vp.copy(anchorBar = clamped, anchorFraction = 0.5)
                inertia *= INERTIA_DECAY.pow(df / 16.7)
            }
        }
    }

    LaunchedEffect(sym) {
        isLoading = true
        candles.clear()
        displayCandle = null
        livePrice = 0f
        followLive = true
        inertia = 0.0

        var success = false
        var retries = 0
        while (!success && retries < 10) {
            val initialBars = BiquoteApi.ohlc(sym, "1m", 300)
            if (initialBars.isNotEmpty()) {
                origin = initialBars.first().time
                for (b in initialBars) mergeCandle(candles, b, bulk = true)
                while (candles.size > 1500) candles.removeAt(0)
                viewport = initialViewport(candles, origin)
                bsTarget = viewport.visibleBars
                livePrice = candles.last().close
                displayCandle = candles.last().copy()
                success = true
                isLoading = false
            } else {
                retries++
                delay(1500L)
            }
        }

        if (!success) {
            isLoading = false
            return@LaunchedEffect
        }

        while (true) {
            delay(1000L)
            val tick = BiquoteApi.tick(sym)
            if (tick != null && tick > 0) livePrice = tick.toFloat()
            val lastBars = BiquoteApi.ohlc(sym, "1m", 2)
            if (lastBars.isNotEmpty()) {
                for (b in lastBars) mergeCandle(candles, b)
                if (followLive && !interacting && candles.isNotEmpty()) {
                    val live = barPosition(candles.last().time, origin)
                    if (abs(viewport.anchorBar - live) > 0.001 ||
                        abs(viewport.anchorFraction - LIVE_CANDLE_FRACTION) > 0.001) {
                        viewport = viewport.copy(
                            anchorBar = live,
                            anchorFraction = LIVE_CANDLE_FRACTION
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            if (candles.isEmpty()) continue
            val real = candles.last()
            val dv = displayCandle
            when {
                dv == null -> displayCandle = real
                dv.time != real.time -> {
                    val prevClose = if (candles.size >= 2) candles[candles.size - 2].close else real.open
                    displayCandle = Candle(prevClose, prevClose, prevClose, real.close, real.time)
                }
                else -> {
                    val nO = dv.open + (real.open - dv.open) * DV_ALPHA
                    val nH = dv.high + (real.high - dv.high) * DV_ALPHA
                    val nL = dv.low  + (real.low  - dv.low)  * DV_ALPHA
                    val nC = dv.close + (real.close - dv.close) * DV_ALPHA
                    val finalClose = if (abs(real.close - nC) < 0.0000001f) real.close else nC
                    displayCandle = Candle(
                        open = nO,
                        high = maxOf(nH, finalClose),
                        low = minOf(nL, finalClose),
                        close = finalClose,
                        time = real.time
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(300L)
            val now = System.currentTimeMillis()
            clockText = clockNow()
            if (candles.isNotEmpty()) {
                secondsLeft = remainingSeconds(candles.last().time, now)
            }
            val closedNow = TradeStore.openTrades.filter { it.expiresAt <= now }
            for (tr in closedNow) {
                TradeStore.openTrades.remove(tr)
                val price = if (livePrice > 0f) livePrice else (candles.lastOrNull()?.close ?: tr.entry)
                val win = if (tr.direction > 0) price > tr.entry else price < tr.entry
                TradeStore.closedTrades.add(tr to win)
                if (win) TradeSounds.win() else TradeSounds.lose()
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier.background(C.ChartBg)) {
            AsyncImage(
                model = CHART_BG_URL,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.25f)))

            if (isLoading) {
                CircularProgressIndicator(
                    color = C.Cyan,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Canvas(
                    Modifier.fillMaxSize().onSizeChanged { chartSize = it }
                        .pointerInput(Unit) {
                            try {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    onBegin()
                                    var pinchStarted = false
                                    var dragging = false
                                    var accumulatedPan = Offset.Zero
                                    var accumulatedZoom = 1f
                                    val slop = viewConfiguration.touchSlop
                                    try {
                                        do {
                                            val event = awaitPointerEvent()
                                            val pressed = event.changes.count { it.pressed }
                                            val stable = event.changes.count { it.pressed && it.previousPressed }
                                            if (pressed >= 2) {
                                                if (!pinchStarted) {
                                                    pinchStarted = true; dragging = false
                                                    accumulatedZoom = 1f; onPinchStart()
                                                }
                                                if (pressed == stable) {
                                                    val zoom = event.calculateZoom()
                                                    if (zoom.isFinite() && zoom > 0f) {
                                                        if (!dragging) {
                                                            accumulatedZoom *= zoom
                                                            val radius = event.calculateCentroidSize(useCurrent = false)
                                                            if (abs(1f - accumulatedZoom) * radius > slop) {
                                                                dragging = true
                                                                onZoom(accumulatedZoom.toDouble())
                                                            }
                                                        } else onZoom(zoom.toDouble())
                                                    }
                                                }
                                                event.changes.forEach { it.consume() }
                                            } else if (pressed == 1 && stable == 1 && !pinchStarted) {
                                                val pan = event.calculatePan()
                                                if (!dragging) {
                                                    accumulatedPan += pan
                                                    if (abs(accumulatedPan.x) > slop) {
                                                        dragging = true
                                                        val excess = accumulatedPan.x -
                                                            if (accumulatedPan.x > 0f) slop else -slop
                                                        onPan(excess)
                                                    }
                                                } else onPan(pan.x)
                                                if (dragging) event.changes.forEach { it.consume() }
                                            } else if (pinchStarted) {
                                                event.changes.forEach { it.consume() }
                                            }
                                        } while (event.changes.any { it.pressed })
                                    } finally { onEnd() }
                                }
                            } finally { onEnd() }
                        }
                ) {
                    val plotWidth = size.width; val plotHeight = size.height
                    if (plotWidth <= 1f || plotHeight <= 1f) return@Canvas
                    if (candles.isEmpty()) return@Canvas
                    val view = viewport
                    val dv = displayCandle
                    val lastIdx = candles.lastIndex

                    fun xForBar(bar: Double): Float =
                        (view.fractionFor(bar) * plotWidth.toDouble()).toFloat()
                    fun yForPrice(price: Double): Float =
                        ((view.maxPrice - price) / view.priceSpan * plotHeight.toDouble()).toFloat()
                    fun baseline(y: Float, paint: NativePaint): Float =
                        y - (paint.ascent() + paint.descent()) / 2f

                    val native = drawContext.canvas.nativeCanvas

                    val rows = (plotHeight / 58.dp.toPx()).toInt().coerceIn(3, 12)
                    val priceStep = nicePriceStep(view.priceSpan / rows)
                    val decimals = priceDecimals(priceStep)
                    val firstPrice = ceil(view.minPrice / priceStep).toLong()
                    val lastPrice = floor(view.maxPrice / priceStep).toLong()
                    if (lastPrice >= firstPrice) {
                        for (tick in firstPrice..lastPrice) {
                            val price = tick.toDouble() * priceStep
                            val y = yForPrice(price)
                            drawLine(C.Grid.copy(alpha = 0.34f), Offset(0f, y), Offset(plotWidth, y), 1.dp.toPx())
                            if (y >= axisTextPx && y <= plotHeight - axisTextPx / 2f) {
                                native.drawText(priceText(price, decimals), plotWidth - 5.dp.toPx(),
                                    baseline(y, axisPaint), axisPaint)
                            }
                        }
                    }

                    val columns = (plotWidth / 64.dp.toPx()).toInt().coerceAtLeast(2)
                    val timeStep = niceTimeStep(view.visibleBars / columns)
                    val originMinute = origin / MINUTE_MS
                    val left = view.centerBar - view.visibleBars / 2.0 + originMinute
                    val right = view.centerBar + view.visibleBars / 2.0 + originMinute
                    val firstTick = ceil(left / timeStep).toLong()
                    val lastTick = floor(right / timeStep).toLong()
                    if (lastTick >= firstTick) {
                        for (tick in firstTick..lastTick) {
                            val minute = tick * timeStep
                            val x = xForBar((minute - originMinute).toDouble())
                            drawLine(C.Grid.copy(alpha = 0.28f), Offset(x, 0f), Offset(x, plotHeight), 1.dp.toPx())
                            val label = candleTimeText(minute * MINUTE_MS)
                            val halfText = timePaint.measureText(label) / 2f
                            if (x - halfText >= 0f && x + halfText <= plotWidth) {
                                native.drawText(label, x,
                                    baseline(plotHeight - 7.dp.toPx(), timePaint), timePaint)
                            }
                        }
                    }

                    val spacing = plotWidth / view.visibleBars.toFloat()
                    val bodyWidth = (spacing * 0.85f).coerceIn(MINBW, spacing * 0.95f)
                    val wickWidth = minOf(1.2.dp.toPx(), bodyWidth)
                    val firstBar = max(0, floor(view.centerBar - view.visibleBars / 2.0).toInt() - 2)
                    val lastBar = min(lastIdx, ceil(view.centerBar + view.visibleBars / 2.0).toInt() + 2)

                    clipRect(0f, 0f, plotWidth, plotHeight) {
                        for (i in firstBar..lastBar) {
                            val real = candles[i]
                            val candle = if (i == lastIdx && dv != null && dv.time == real.time) dv else real
                            val x = xForBar(barPosition(candle.time, origin))
                            if (x + bodyWidth / 2f < 0f || x - bodyWidth / 2f > plotWidth) continue
                            val color = if (candle.close >= candle.open) C.Green else C.Red
                            val yH = yForPrice(candle.high.toDouble())
                            val yL = yForPrice(candle.low.toDouble())
                            val yO = yForPrice(candle.open.toDouble())
                            val yC = yForPrice(candle.close.toDouble())
                            drawLine(color, Offset(x, yH), Offset(x, yL), wickWidth, cap = StrokeCap.Butt)
                            val top = minOf(yO, yC)
                            val height = abs(yO - yC)
                            if (height < 1.dp.toPx()) {
                                drawLine(color, Offset(x - bodyWidth / 2f, (yO + yC) / 2f),
                                    Offset(x + bodyWidth / 2f, (yO + yC) / 2f), 1.dp.toPx())
                            } else {
                                drawRect(color, Offset(x - bodyWidth / 2f, top), Size(bodyWidth, height))
                            }
                        }
                        TradeStore.openTrades.filter { it.symbol == sym }.forEach { tr ->
                            val yEntry = yForPrice(tr.entry.toDouble())
                            val xEntry = xForBar(barPosition(tr.openedAt, origin))
                            val col = if (tr.direction > 0) C.BarOpenGreen else C.Red
                            drawLine(col.copy(alpha = 0.8f), Offset(xEntry, yEntry),
                                Offset(plotWidth - 54f, yEntry), 2.dp.toPx())
                            drawCircle(col, 4.dp.toPx(), Offset(xEntry, yEntry))
                        }
                    }

                    val displayPrice = dv?.close ?: (if (livePrice > 0f) livePrice else candles.last().close)
                    val currentY = yForPrice(displayPrice.toDouble())
                    if (currentY in 0f..plotHeight && displayPrice > 0f) {
                        val dash = 7.dp.toPx(); val gap = 5.dp.toPx(); var x = 0f
                        while (x < plotWidth) {
                            drawLine(C.Cyan.copy(alpha = 0.85f), Offset(x, currentY),
                                Offset(minOf(x + dash, plotWidth), currentY), 1.dp.toPx())
                            x += dash + gap
                        }
                        val label = priceText(displayPrice.toDouble(), decimals)
                        val tagWidth = tagPaint.measureText(label) + 14.dp.toPx()
                        val tagHeight = 20.dp.toPx()
                        drawRoundRect(C.PriceTag,
                            Offset(plotWidth - tagWidth - 3.dp.toPx(), currentY - tagHeight / 2f),
                            Size(tagWidth, tagHeight), CornerRadius(4.dp.toPx()))
                        native.drawText(label, plotWidth - 3.dp.toPx() - tagWidth / 2f,
                            baseline(currentY, tagPaint), tagPaint)
                        native.drawText("M1", plotWidth - 6.dp.toPx(),
                            currentY - 11.dp.toPx(), detailPaint)
                        native.drawText(String.format(Locale.US, "%02d:%02d",
                            secondsLeft / 60, secondsLeft % 60), plotWidth - 6.dp.toPx(),
                            currentY + 21.dp.toPx(), detailPaint)
                    }
                }
            }

            PairHeader(Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 8.dp),
                onClick = onOpenPairPicker)
            Column(Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 60.dp)) {
                val displayPrice = displayCandle?.close ?: (if (livePrice > 0f) livePrice else 0f)
                Text(fmt(displayPrice), color = C.TextSecondary, fontSize = 11.sp)
                Text("$clockText UTC+3", color = C.TextSecondary, fontSize = 11.sp)
            }
            OpenTradeProgressBars(Modifier.align(Alignment.BottomStart).padding(12.dp))
        }
    }
}

@Composable
fun OpenTradeProgressBars(modifier: Modifier = Modifier) {
    val trades = TradeStore.openTrades
    if (trades.isEmpty()) return
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) { tick = System.currentTimeMillis(); delay(100L) }
    }
    Column(modifier.width(200.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        trades.forEach { tr ->
            val duration = (tr.expiresAt - tr.openedAt).coerceAtLeast(1L)
            val elapsed = (tick - tr.openedAt).coerceIn(0L, duration)
            val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1D2131))
                    .padding(10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${SymCatalog.pretty(tr.symbol)}  ${if (tr.direction > 0) "شراء" else "بيع"}",
                        color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    val remain = ((tr.expiresAt - tick) / 1000L).coerceAtLeast(0L)
                    Text(String.format(Locale.US, "%02d:%02d", remain / 60, remain % 60),
                        color = C.TextSecondary, fontSize = 11.sp)
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(8.dp))
                    .background(C.BarBg)) {
                    Box(Modifier.fillMaxWidth(progress).fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(C.BarOpenGreen))
                }
            }
        }
    }
}

@Composable
fun PairPickerSheet(onDismiss: () -> Unit) {
    val tabs = listOf("الجميع", "فوركس", "عملات رقمية", "سلع", "مؤشرات", "أسهم")
    var selectedTab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var allSymbols by remember { mutableStateOf<List<String>>(emptyList()) }
    var connectedMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    val favorites = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        val remoteSymbols = BiquoteApi.symbols(liveOnly = false)
        val symbols = remoteSymbols.distinct().sorted()
        allSymbols = symbols

        if (symbols.isNotEmpty()) {
            val conn = mutableMapOf<String, Boolean>()
            coroutineScope {
                val jobs = symbols.chunked(100).map { chunk ->
                    async(Dispatchers.IO) {
                        val latest = BiquoteApi.latest(chunk)
                        chunk.map { it to latest.containsKey(it) }
                    }
                }
                jobs.awaitAll().flatten().forEach { (sym, connected) ->
                    conn[sym] = connected
                }
            }
            connectedMap = conn
        }
        isLoading = false
    }

    val filtered = allSymbols.filter { sym ->
        val catOk = when (selectedTab) {
            0 -> true
            1 -> SymCatalog.isForex(sym)
            2 -> SymCatalog.isCrypto(sym)
            3 -> SymCatalog.isCommodity(sym)
            4 -> SymCatalog.isIndex(sym)
            5 -> SymCatalog.isStock(sym)
            else -> false
        }
        catOk && (query.isBlank() || sym.contains(query.uppercase()))
    }.sortedWith(compareBy({ !(connectedMap[it] ?: false) }, { it }))

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))
        .clickable(onClick = onDismiss)) {
        Box(
            Modifier.fillMaxWidth(0.88f).fillMaxHeight()
                .align(Alignment.CenterStart)
                .background(C.PkBg)
                .border(1.dp, C.PkBd, RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                .clickable(enabled = false, onClick = {})
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("اختر الأصل", color = Color.White,
                        fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f))
                    Text("✕", color = Color(0xFFCFD9E8), fontSize = 19.sp,
                        modifier = Modifier.clickable(onClick = onDismiss).padding(6.dp))
                }

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEachIndexed { i, t ->
                        val on = i == selectedTab
                        Box(Modifier.clip(RoundedCornerShape(8.dp))
                            .background(if (on) C.PkTabOn else C.PkTab)
                            .clickable { selectedTab = i }
                            .padding(horizontal = 10.dp, vertical = 7.dp)) {
                            Text(t, color = if (on) Color.White else Color(0xFFA9B6CC),
                                fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }

                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.height(38.dp).widthIn(min = 54.dp)
                        .clip(RoundedCornerShape(10.dp)).background(C.PkTab)
                        .border(1.dp, C.PkBd, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("★", color = C.PkStarOn, fontSize = 14.sp)
                        Spacer(Modifier.width(5.dp))
                        Text("${favorites.size}", color = Color(0xFFCFD9E8),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.weight(1f).height(38.dp)
                        .clip(RoundedCornerShape(10.dp)).background(C.PkTab)
                        .border(1.dp, C.PkBd, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = query, onValueChange = { query = it },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                            decorationBox = { inner ->
                                if (query.isEmpty())
                                    Text("بحث", color = Color(0xFF7A8B9E), fontSize = 12.sp)
                                inner()
                            }
                        )
                        Text("🔍", color = Color(0xFF9FB0C9), fontSize = 14.sp)
                    }
                }

                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tabs[selectedTab], color = Color(0xFF8EA0B8),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${filtered.size} أصل", color = Color(0xFF8EA0B8),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = C.Cyan)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(filtered) { c ->
                            val base = SymCatalog.base(c); val quote = SymCatalog.quote(c)
                            val cryptoIcon = SymCatalog.cryptoIconUrl(c)
                            val isConnected = connectedMap[c] ?: false

                            Row(Modifier.fillMaxWidth()
                                .clickable(enabled = isConnected) {
                                    TradeStore.currentSymbol.value = c
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                                .alpha(if (isConnected) 1f else 0.5f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                val fav = favorites.contains(c)
                                Text(if (fav) "★" else "☆",
                                    color = C.PkStarOn, fontSize = 20.sp,
                                    modifier = Modifier.clickable(enabled = isConnected) {
                                        if (fav) favorites.remove(c) else favorites.add(c)
                                    })
                                Box(Modifier.width(42.dp).height(28.dp)) {
                                    if (cryptoIcon != null) {
                                        AsyncImage(model = cryptoIcon, contentDescription = null,
                                            modifier = Modifier.size(28.dp).clip(CircleShape)
                                                .border(2.dp, C.PkBg, CircleShape)
                                                .align(Alignment.CenterStart))
                                    } else {
                                        val baseFlag = SymCatalog.flagUrl(SymCatalog.FLAGS[base])
                                        if (baseFlag != null) AsyncImage(model = baseFlag, contentDescription = null,
                                            modifier = Modifier.size(28.dp).clip(CircleShape)
                                                .border(2.dp, C.PkBg, CircleShape)
                                                .align(Alignment.CenterStart))
                                        else Box(Modifier.size(28.dp).clip(CircleShape)
                                            .background(Color(0xFFF0B90B))
                                            .border(2.dp, C.PkBg, CircleShape)
                                            .align(Alignment.CenterStart),
                                            contentAlignment = Alignment.Center) {
                                            Text(base.take(1), color = Color(0xFF1B2233),
                                                fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                    }
                                    if (cryptoIcon == null) {
                                        Box(Modifier.offset(x = 14.dp)) {
                                            val quoteFlag = SymCatalog.flagUrl(SymCatalog.FLAGS[quote])
                                            if (quoteFlag != null) AsyncImage(model = quoteFlag,
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp).clip(CircleShape)
                                                    .border(2.dp, C.PkBg, CircleShape))
                                            else Box(Modifier.size(28.dp).clip(CircleShape)
                                                .background(Color(0xFFF0B90B))
                                                .border(2.dp, C.PkBg, CircleShape),
                                                contentAlignment = Alignment.Center) {
                                                Text(quote.take(1), color = Color(0xFF1B2233),
                                                    fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                        }
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(SymCatalog.pretty(c), color = Color(0xFFDFE8F5),
                                        fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                    if (!isConnected) {
                                        Text("غير متصل", color = C.Red, fontSize = 10.sp)
                                    }
                                }
                                Text("+${SymCatalog.payout(c)}%", color = C.PkPct,
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TradePanel() {
    Column(Modifier.fillMaxWidth().background(C.BG)) {
        Spacer(Modifier.height(6.dp)); SentimentBar()
        Spacer(Modifier.height(8.dp)); TradeInputs()
        Spacer(Modifier.height(8.dp)); TradeButtons()
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun SentimentBar() {
    var buy by remember { mutableStateOf(33) }
    LaunchedEffect(Unit) { while (true) { delay(2200L); buy = Random.nextInt(30, 37) } }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text("$buy%", color = C.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        BoxWithConstraints(Modifier.weight(1f).height(16.dp)) {
            val availableWidth = maxWidth
            Row(Modifier.fillMaxWidth().height(10.dp).align(Alignment.Center)
                .clip(RoundedCornerShape(5.dp))) {
                Box(Modifier.weight(buy.toFloat()).fillMaxHeight().background(C.Green))
                Box(Modifier.weight((100 - buy).toFloat()).fillMaxHeight().background(C.Red))
            }
            Box(Modifier.align(Alignment.CenterStart)
                .offset(x = availableWidth * (buy / 100f) - 1.25.dp)
                .width(2.5.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(C.Cyan))
        }
        Spacer(Modifier.width(8.dp))
        Text("${100 - buy}%", color = C.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TradeInputs() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TradeInput("الزمن", "00:01:00", Modifier.weight(1f)) { ClockIcon(Modifier.size(18.dp)) }
        TradeInput("المبلغ", "1", Modifier.weight(1f)) { DollarIcon(Modifier.size(18.dp)) }
    }
}

@Composable
private fun TradeInput(label: String, value: String, modifier: Modifier = Modifier,
                      icon: @Composable () -> Unit) {
    Column(modifier) {
        Text(label, color = C.TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp))
            .background(C.Surface).border(1.dp, C.Divider, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(value, color = C.TextPrimary, fontSize = 14.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            icon()
        }
    }
}

@Composable
fun TradeButtons() {
    val sym by TradeStore.currentSymbol.collectAsState()
    val payout = remember(sym) { SymCatalog.payout(sym) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TradeButton("شراء", C.Green, payout, Modifier.weight(1f)) { openTrade(+1, sym, payout) }
        TradeButton("بيع", C.Red, payout, Modifier.weight(1f)) { openTrade(-1, sym, payout) }
    }
}

private fun openTrade(direction: Int, symbol: String, payout: Int) {
    val now = System.currentTimeMillis()
    TradeStore.openTrades.add(OpenTrade(
        symbol = symbol, direction = direction,
        entry = 1.0f,
        amount = 1.0, openedAt = now, expiresAt = now + 60_000L,
        payoutPct = payout
    ))
    TradeSounds.open()
}

@Composable
private fun TradeButton(label: String, color: Color, payout: Int,
                       modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.height(48.dp).clip(RoundedCornerShape(12.dp)).background(color)
        .clickable { onClick() },
        contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("$payout%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BottomNavBar() {
    var selected by remember { mutableStateOf(2) }
    Column(Modifier.background(C.BG)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Divider))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth().background(C.BG).padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically) {
                BottomItem("التداولات", selected == 0, { selected = 0 }) { TradesIcon(Modifier.size(24.dp), it) }
                BottomItem("الرؤى", selected == 1, { selected = 1 }) { SignalIcon(Modifier.size(24.dp), it) }
                BottomItem("Pro mode", selected == 2, { selected = 2 }) { ProIcon(Modifier.size(24.dp), it) }
                BottomItem("أول الاجتماعي", selected == 3, { selected = 3 }) { SocialIcon(Modifier.size(24.dp), it) }
                BottomItem("المزيد", selected == 4, { selected = 4 }) { DotsIcon(Modifier.size(24.dp), it) }
            }
        }
    }
}

@Composable
fun BottomItem(label: String, selected: Boolean, onClick: () -> Unit,
              icon: @Composable (Color) -> Unit) {
    val color = if (selected) C.Cyan else C.TextSecondary
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
        .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
            .background(if (selected) C.SurfaceRaised else Color.Transparent)
            .border(1.dp, if (selected) C.Cyan else Color.Transparent, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center) { icon(color) }
        Spacer(Modifier.height(3.dp))
        Text(label, color = color, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
fun DownArrow(modifier: Modifier = Modifier, color: Color = C.TextSecondary) {
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2f, size.height); close()
        }
        drawPath(path, color)
    }
}

@Composable
fun DotsIcon(modifier: Modifier = Modifier, color: Color = C.TextPrimary) {
    Canvas(modifier) {
        val r = size.minDimension * 0.11f; val y = size.height / 2f
        drawCircle(color, r, Offset(size.width * 0.15f, y))
        drawCircle(color, r, Offset(size.width * 0.5f, y))
        drawCircle(color, r, Offset(size.width * 0.85f, y))
    }
}

@Composable
fun WalletIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawRoundRect(color, Offset(w * 0.05f, h * 0.22f), Size(w * 0.9f, h * 0.62f),
            CornerRadius(w * 0.1f), style = Stroke(width = w * 0.08f))
        drawRoundRect(color, Offset(w * 0.55f, h * 0.4f), Size(w * 0.4f, h * 0.28f),
            CornerRadius(w * 0.06f), style = Stroke(width = w * 0.07f))
        drawCircle(color, w * 0.05f, Offset(w * 0.73f, h * 0.54f))
    }
}

@Composable
fun ClockIcon(modifier: Modifier = Modifier, color: Color = C.Cyan) {
    Canvas(modifier) {
        val d = size.minDimension; val cx = size.width / 2f; val cy = size.height / 2f
        val s = d * 0.09f
        drawCircle(color, d * 0.4f, Offset(cx, cy), style = Stroke(width = s))
        drawLine(color, Offset(cx, cy), Offset(cx, size.height * 0.25f), s, cap = StrokeCap.Round)
        drawLine(color, Offset(cx, cy), Offset(size.width * 0.72f, cy), s, cap = StrokeCap.Round)
    }
}

@Composable
fun DollarIcon(modifier: Modifier = Modifier, color: Color = C.Cyan) {
    Canvas(modifier) {
        val d = size.minDimension; val stroke = d * 0.09f
        val cx = size.width / 2f; val cy = size.height / 2f; val s = d * 0.16f
        drawCircle(color, d * 0.4f, Offset(cx, cy), style = Stroke(width = stroke))
        drawLine(color, Offset(cx + s * 0.6f, cy - s), Offset(cx - s * 0.5f, cy - s * 0.2f), stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(cx - s * 0.5f, cy - s * 0.2f), Offset(cx + s * 0.5f, cy + s * 0.2f), stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(cx + s * 0.5f, cy + s * 0.2f), Offset(cx - s * 0.6f, cy + s), stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(cx, cy - s * 1.4f), Offset(cx, cy - s), stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(cx, cy + s), Offset(cx, cy + s * 1.4f), stroke, cap = StrokeCap.Round)
    }
}

@Composable
fun FlagAE(modifier: Modifier = Modifier) {
    Canvas(modifier.clip(CircleShape)) {
        val w = size.width; val h = size.height
        drawRect(Color(0xFF00732F), size = Size(w, h / 3f))
        drawRect(Color.White, Offset(0f, h / 3f), Size(w, h / 3f))
        drawRect(Color(0xFF151515), Offset(0f, 2f * h / 3f), Size(w, h / 3f))
        drawRect(Color.Red, size = Size(w * 0.28f, h))
    }
}

@Composable
fun FlagCN(modifier: Modifier = Modifier) {
    Canvas(modifier.clip(CircleShape)) {
        drawRect(Color(0xFFDE2910))
        drawCircle(Color(0xFFFFDE00), size.minDimension * 0.16f,
            Offset(size.width * 0.32f, size.height * 0.34f))
    }
}

@Composable
fun TradesIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width; val h = size.height; val s = w * 0.09f
        drawLine(color, Offset(w * 0.3f, h * 0.85f), Offset(w * 0.3f, h * 0.15f), s, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.3f, h * 0.15f), Offset(w * 0.15f, h * 0.32f), s, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.3f, h * 0.15f), Offset(w * 0.45f, h * 0.32f), s, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.7f, h * 0.15f), Offset(w * 0.7f, h * 0.85f), s, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.7f, h * 0.85f), Offset(w * 0.55f, h * 0.68f), s, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.7f, h * 0.85f), Offset(w * 0.85f, h * 0.68f), s, cap = StrokeCap.Round)
    }
}

@Composable
fun SignalIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val cx = size.width / 2f; val cy = size.height * 0.75f; val s = size.minDimension * 0.09f
        drawArc(color, 180f, 180f, false, Offset(cx - size.width * 0.42f, cy - size.height * 0.42f),
            Size(size.width * 0.84f, size.height * 0.84f), style = Stroke(width = s))
        drawArc(color, 180f, 180f, false, Offset(cx - size.width * 0.26f, cy - size.height * 0.26f),
            Size(size.width * 0.52f, size.height * 0.52f), style = Stroke(width = s))
        drawCircle(color, size.minDimension * 0.1f, Offset(cx, cy - size.height * 0.02f))
    }
}

@Composable
fun ProIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawRoundRect(color, Offset(w * 0.08f, h * 0.12f), Size(w * 0.84f, h * 0.76f),
            CornerRadius(w * 0.16f), style = Stroke(width = w * 0.08f))
        val path = Path().apply {
            moveTo(w * 0.24f, h * 0.62f); lineTo(w * 0.40f, h * 0.44f)
            lineTo(w * 0.54f, h * 0.56f); lineTo(w * 0.76f, h * 0.34f)
        }
        drawPath(path, color, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
    }
}

@Composable
fun SocialIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val cx = size.width / 2f; val cy = size.height / 2f
        drawCircle(color, size.minDimension * 0.17f, Offset(cx, cy - size.height * 0.16f))
        drawArc(color, 180f, 180f, false, Offset(cx - size.width * 0.28f, cy + size.height * 0.04f),
            Size(size.width * 0.56f, size.height * 0.5f), style = Stroke(width = size.minDimension * 0.08f))
        drawCircle(color, size.minDimension * 0.09f, Offset(cx - size.width * 0.36f, cy - size.height * 0.14f))
        drawCircle(color, size.minDimension * 0.09f, Offset(cx + size.width * 0.36f, cy - size.height * 0.14f))
    }
}
