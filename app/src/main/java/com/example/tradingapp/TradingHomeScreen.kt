package com.example.tradingapp

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
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
    val Purple = Color(0xFF9C7BFF)
    val Cyan = Color(0xFF58D9F5)
    val Green = Color(0xFF00E676)
    val Red = Color(0xFFFF2D55)
    val Blue = Cyan
    val Yellow = Color(0xFFFFD34D)
    val TextPrimary = Color(0xFFF5F2FF)
    val TextSecondary = Color(0xFFA9A5C6)
    val Divider = Color(0xFF3D355A)
    val Grid = Color(0xFF67558D)
    val PriceTag = Cyan
}

private const val MINUTE_MS = 60_000L
private const val CANDLE_SAFE_MARGIN = 0.15
private const val MIN_VISIBLE_BARS = 10.0
private const val MAX_VISIBLE_BARS = 120.0
private const val MIN_PRICE_SPAN = 0.00001
private const val LIVE_CANDLE_FRACTION = 0.80
private const val FOLLOW_SNAP_BARS = 0.75

// زمن محلي نسبي بدل أرقام الدقائق الكبيرة: يقلل خطأ الحساب العشري.
private fun barPosition(time: Long, origin: Long): Double =
    (time - origin).toDouble() / MINUTE_MS.toDouble()

data class Candle(
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val time: Long = 0L
)

fun seedCandles(
    lastOpenTime: Long = System.currentTimeMillis() / MINUTE_MS * MINUTE_MS
): List<Candle> {
    val anchors = listOf(
        0 to 1.8772f, 5 to 1.8748f, 9 to 1.8742f,
        13 to 1.8708f, 17 to 1.8685f, 21 to 1.8662f,
        25 to 1.8644f, 28 to 1.86344f, 32 to 1.8660f,
        36 to 1.8686f, 40 to 1.8678f, 44 to 1.8672f,
        48 to 1.8686f, 52 to 1.86978f
    )
    fun priceAt(index: Int): Float {
        var a = anchors.first()
        var b = anchors.last()
        for (k in 0 until anchors.lastIndex) {
            if (index in anchors[k].first..anchors[k + 1].first) {
                a = anchors[k]
                b = anchors[k + 1]
                break
            }
        }
        val fraction = if (a.first == b.first) 0f else
            (index - a.first).toFloat() / (b.first - a.first).toFloat()
        return a.second + (b.second - a.second) * fraction
    }
    val random = java.util.Random(42)
    val result = ArrayList<Candle>()
    var previousClose = 1.8772f
    for (index in 0..52) {
        val close = priceAt(index) + (random.nextFloat() - 0.5f) * 0.0009f
        result.add(
            Candle(
                open = previousClose,
                high = maxOf(previousClose, close) + random.nextFloat() * 0.0008f,
                low = minOf(previousClose, close) - random.nextFloat() * 0.0008f,
                close = close,
                time = lastOpenTime - (52L - index.toLong()) * MINUTE_MS
            )
        )
        previousClose = close
    }
    return result
}

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

/*
 * الإحداثي الأفقي معرف مباشرة من محور ثابت، وليس من مركز يعاد تقييده.
 * x(bar) = [anchorFraction + (bar - anchorBar) / visibleBars] * width
 * عند تغيير visibleBars وحده يظل x(anchorBar) ثابتاً.
 */
private data class ChartViewport(
    val visibleBars: Double,
    val anchorBar: Double,
    val anchorFraction: Double,
    val centerPrice: Double,
    val priceSpan: Double
) {
    val centerBar: Double
        get() = anchorBar + (0.5 - anchorFraction) * visibleBars
    val minPrice: Double get() = centerPrice - priceSpan / 2.0
    val maxPrice: Double get() = centerPrice + priceSpan / 2.0

    fun fractionFor(bar: Double): Double =
        anchorFraction + (bar - anchorBar) / visibleBars

    // تغيير المرجع بدون تغيير الصورة أو إحداثي أي شمعة نظرياً.
    fun reanchor(bar: Double): ChartViewport = copy(
        anchorBar = bar,
        anchorFraction = fractionFor(bar)
    )
}

private fun initialViewport(candles: List<Candle>, origin: Long): ChartViewport {
    val low = candles.minOf { it.low.toDouble() }
    val high = candles.maxOf { it.high.toDouble() }
    return ChartViewport(
        visibleBars = 60.0,
        anchorBar = barPosition(candles.last().time, origin),
        anchorFraction = LIVE_CANDLE_FRACTION,
        centerPrice = (low + high) / 2.0,
        priceSpan = ((high - low) / 0.70 * 1.04).coerceAtLeast(MIN_PRICE_SPAN)
    )
}

private fun verticalTarget(
    view: ChartViewport,
    candles: List<Candle>,
    origin: Long
): Pair<Double, Double> {
    val visible = candles.filter {
        abs(barPosition(it.time, origin) - view.centerBar) <= view.visibleBars / 2.0 + 0.6
    }
    if (visible.isEmpty()) return view.centerPrice to view.priceSpan
    val low = visible.minOf { it.low.toDouble() }
    val high = visible.maxOf { it.high.toDouble() }
    return (high + low) / 2.0 to
        ((high - low) / (1.0 - 2.0 * CANDLE_SAFE_MARGIN) * 1.04)
            .coerceAtLeast(MIN_PRICE_SPAN)
}

private fun nicePriceStep(rawStep: Double): Double {
    if (!rawStep.isFinite() || rawStep <= 0.0) return 0.001
    val magnitude = 10.0.pow(floor(log10(rawStep)))
    val normalized = rawStep / magnitude
    val nice = when {
        normalized <= 1.0 -> 1.0
        normalized <= 2.0 -> 2.0
        normalized <= 2.5 -> 2.5
        normalized <= 5.0 -> 5.0
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

@Composable
fun TradingHomeScreen() {
    Column(Modifier.fillMaxSize().background(C.BG)) {
        TopBar()
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Divider))
        LiveCandlestickChart(Modifier.weight(1f).fillMaxWidth())
        TradePanel()
        BottomNavBar()
    }
}

@Composable
fun TopBar() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier.fillMaxWidth().background(C.BG)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(C.Yellow),
                contentAlignment = Alignment.Center
            ) { WalletIcon(Modifier.size(26.dp), Color(0xFF312740)) }
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
                Spacer(Modifier.width(10.dp))
                DownArrow(Modifier.size(10.dp))
            }
            Spacer(Modifier.weight(1f))
            Box {
                AsyncImage(
                    model = GIFT_ICON_URL, contentDescription = "هدية",
                    contentScale = ContentScale.Fit, modifier = Modifier.size(36.dp)
                )
                Box(
                    Modifier.align(Alignment.TopEnd).offset(3.dp, (-3).dp)
                        .size(14.dp).clip(CircleShape).background(C.BG)
                        .border(1.dp, C.Divider, CircleShape)
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val m = size.minDimension * 0.22f
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        drawLine(Color.White, Offset(cx - m, cy - m), Offset(cx + m, cy + m),
                            1.dp.toPx(), cap = StrokeCap.Round)
                        drawLine(Color.White, Offset(cx + m, cy - m), Offset(cx - m, cy + m),
                            1.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = AVATAR_URL, contentDescription = "الصورة الشخصية",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(C.Surface)
                )
                AsyncImage(
                    model = AVATAR_FRAME_URL, contentDescription = null,
                    contentScale = ContentScale.Fit, modifier = Modifier.size(52.dp)
                )
            }
        }
    }
}

@Composable
fun PairHeader(modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(RoundedCornerShape(22.dp)).background(C.Surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(34.dp).height(22.dp)) {
                    FlagAE(Modifier.size(22.dp).align(Alignment.CenterStart))
                    FlagCN(Modifier.size(22.dp).align(Alignment.CenterEnd)
                        .border(1.5.dp, C.Surface, CircleShape))
                }
                Spacer(Modifier.width(8.dp))
                Text("AED/CNY OTC", color = C.TextPrimary, fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                DownArrow(Modifier.size(9.dp))
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(C.Surface),
                contentAlignment = Alignment.Center
            ) { DotsIcon(Modifier.size(16.dp)) }
        }
    }
}

@Composable
fun LiveCandlestickChart(modifier: Modifier = Modifier) {
    val candles = remember {
        mutableStateListOf<Candle>().apply { addAll(seedCandles()) }
    }
    // لا يتغير حتى بعد حذف أقدم الشموع من القائمة.
    val origin = remember { candles.first().time }
    val now0 = remember { System.currentTimeMillis() }
    var viewport by remember { mutableStateOf(initialViewport(candles, origin)) }
    var followLive by remember { mutableStateOf(true) }
    var interacting by remember { mutableStateOf(false) }
    var chartSize by remember { mutableStateOf(IntSize.Zero) }
    var secondsLeft by remember {
        mutableStateOf(remainingSeconds(candles.last().time, now0))
    }
    var clockText by remember { mutableStateOf(clockNow()) }

    val density = LocalDensity.current
    val axisTextPx = with(density) { 9.sp.toPx() }
    val tagTextPx = with(density) { 10.sp.toPx() }
    val axisPaint = remember(axisTextPx) {
        NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.toArgb()
            textSize = axisTextPx
            textAlign = NativePaint.Align.RIGHT
            typeface = Typeface.DEFAULT
        }
    }
    val timePaint = remember(axisTextPx) {
        NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = C.TextSecondary.toArgb()
            textSize = axisTextPx
            textAlign = NativePaint.Align.CENTER
        }
    }
    val tagPaint = remember(tagTextPx) {
        NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = C.BG.toArgb()
            textSize = tagTextPx
            textAlign = NativePaint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
    }
    val detailPaint = remember(axisTextPx) {
        NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = C.Cyan.toArgb()
            textSize = axisTextPx
            textAlign = NativePaint.Align.RIGHT
        }
    }

    fun beginInteraction() { interacting = true }
    fun endInteraction() { interacting = false }

    fun beginPinch() {
        val view = viewport
        val live = barPosition(candles.last().time, origin)
        val liveFraction = view.fractionFor(live)
        // في اللايف: الشمعة الأخيرة هي المحور، بمكانها الحالي تماماً.
        // عند استعراض تاريخ قديم: أقرب شمعة لمنتصف الشاشة هي المحور.
        val pivot = if (liveFraction in 0.0..1.0) live else {
            candles.minByOrNull {
                abs(barPosition(it.time, origin) - view.centerBar)
            }?.let { barPosition(it.time, origin) } ?: view.centerBar
        }
        viewport = view.reanchor(pivot)
    }

    fun zoomOnly(factor: Double) {
        if (!factor.isFinite() || factor <= 0.0) return
        val view = viewport
        val next = (view.visibleBars / factor)
            .coerceIn(MIN_VISIBLE_BARS, MAX_VISIBLE_BARS)
        if (next == view.visibleBars) return
        // لا سحب، لا تقييد للمركز، لا تغيير لمحور السعر أو للارتكاز.
        viewport = view.copy(visibleBars = next)
    }

    fun panOnly(dx: Float) {
        val width = chartSize.width.toDouble()
        if (width <= 1.0 || !dx.isFinite() || dx == 0f) return
        val view = viewport
        val desiredCenter = view.centerBar - dx.toDouble() / width * view.visibleBars
        val first = barPosition(candles.first().time, origin)
        val last = barPosition(candles.last().time, origin)
        val half = view.visibleBars / 2.0
        // حدود للسحب اليدوي فقط. لا تستخدم هذه الحدود إطلاقاً في التكبير.
        val minCenter = first - half + 2.0
        val maxCenter = last + half - 2.0
        val center = if (minCenter <= maxCenter)
            desiredCenter.coerceIn(minCenter, maxCenter) else view.centerBar
        val next = view.copy(
            anchorBar = center,
            anchorFraction = 0.5
        )
        // إعادة الملاءمة الرأسية للسحب المتعمد فقط، وليس للتكبير.
        val (priceCenter, span) = verticalTarget(next, candles, origin)
        viewport = next.copy(centerPrice = priceCenter, priceSpan = span)
        val liveCenter = last + (0.5 - LIVE_CANDLE_FRACTION) * next.visibleBars
        followLive = abs(center - liveCenter) < FOLLOW_SNAP_BARS
    }

    // يحافظ pointerInput على أحدث الحالة بدون إعادة تشغيل الإيماءة.
    val onBegin by rememberUpdatedState<() -> Unit>({ beginInteraction() })
    val onEnd by rememberUpdatedState<() -> Unit>({ endInteraction() })
    val onPinchStart by rememberUpdatedState<() -> Unit>({ beginPinch() })
    val onZoom by rememberUpdatedState<(Double) -> Unit>({ zoomOnly(it) })
    val onPan by rememberUpdatedState<(Float) -> Unit>({ panOnly(it) })

    // لا توجد حلقة تحرك المحور الرأسي كل إطار.
    // إحداثي أي سعر ثابت ما لم يسحب المستخدم بإصبع واحد عمداً.
    // تغير OHLC الحي نفسه طبيعي ولا يعني تحريك نافذة الشارت.
    LaunchedEffect(Unit) {
        while (true) {
            delay(260L)
            val now = System.currentTimeMillis()
            val minute = now / MINUTE_MS * MINUTE_MS
            while (candles.last().time < minute) {
                val previous = candles.last()
                candles.add(Candle(
                    previous.close, previous.close, previous.close, previous.close,
                    previous.time + MINUTE_MS
                ))
                if (candles.size > 600) candles.removeAt(0)
            }
            val index = candles.lastIndex
            val current = candles[index]
            val close = (current.close + (Random.nextFloat() - 0.5f) * 0.00035f)
                .coerceAtLeast(0.00001f)
            candles[index] = current.copy(
                close = close,
                high = maxOf(current.high, close),
                low = minOf(current.low, close)
            )
            if (followLive && !interacting) {
                val live = barPosition(candles.last().time, origin)
                val view = viewport
                // لا تستخدم الوقت الكسري؛ الشموع لا تزحف مع مرور الثواني.
                // تتحرك نافذة اللايف فقط عند بدء شمعة جديدة.
                val desiredCenter = live + (0.5 - LIVE_CANDLE_FRACTION) * view.visibleBars
                if (abs(desiredCenter - view.centerBar) > 1e-9) {
                    viewport = view.copy(
                        anchorBar = live,
                        anchorFraction = LIVE_CANDLE_FRACTION
                    )
                }
            }
            secondsLeft = remainingSeconds(candles.last().time, now)
            clockText = clockNow()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier.background(C.BG)) {
            AsyncImage(
                model = CHART_BG_URL, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()
            )
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(
                C.BG.copy(alpha = 0.80f),
                Color(0xFF231C3E).copy(alpha = 0.78f),
                C.BG.copy(alpha = 0.84f)
            ))))
            Canvas(
                Modifier.fillMaxSize().onSizeChanged { chartSize = it }
                    .pointerInput(Unit) {
                        // finally خارج AwaitPointerEventScope لإعادة الحالة حتى عند الإلغاء.
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
                                                pinchStarted = true
                                                dragging = false
                                                accumulatedZoom = 1f
                                                onPinchStart()
                                            }
                                            // إهمال حركة انتقال إضافة/رفع إصبع.
                                            if (pressed == stable) {
                                                val zoom = event.calculateZoom()
                                                if (zoom.isFinite() && zoom > 0f) {
                                                    if (!dragging) {
                                                        accumulatedZoom *= zoom
                                                        val radius = event.calculateCentroidSizeCompat()
                                                        if (abs(1f - accumulatedZoom) * radius > slop) {
                                                            dragging = true
                                                            onZoom(accumulatedZoom.toDouble())
                                                        }
                                                    } else {
                                                        onZoom(zoom.toDouble())
                                                    }
                                                }
                                                // لا calculatePan أثناء القرص إطلاقاً.
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
                                            } else {
                                                onPan(pan.x)
                                            }
                                            if (dragging) event.changes.forEach { it.consume() }
                                        } else if (pinchStarted) {
                                            // بعد رفع أحد إصبعي القرص لا يتحول الباقي لسحب.
                                            event.changes.forEach { it.consume() }
                                        }
                                    } while (event.changes.any { it.pressed })
                                } finally {
                                    onEnd()
                                }
                            }
                        } finally {
                            onEnd()
                        }
                    }
            ) {
                val plotWidth = size.width
                val plotHeight = size.height
                if (plotWidth <= 1f || plotHeight <= 1f) return@Canvas
                val view = viewport
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
                        drawLine(C.Grid.copy(alpha = 0.34f), Offset(0f, y),
                            Offset(plotWidth, y), 1.dp.toPx())
                        if (y >= axisTextPx && y <= plotHeight - axisTextPx / 2f) {
                            native.drawText(priceText(price, decimals), plotWidth - 5.dp.toPx(),
                                baseline(y, axisPaint), axisPaint)
                        }
                    }
                }
                val columns = (plotWidth / 64.dp.toPx()).toInt().coerceAtLeast(2)
                val timeStep = niceTimeStep(view.visibleBars / columns)
                // محاذاة علامات الوقت على الزمن المطلق، رغم استخدام أصل نسبي للرسم.
                val originMinute = origin / MINUTE_MS
                val left = view.centerBar - view.visibleBars / 2.0 + originMinute
                val right = view.centerBar + view.visibleBars / 2.0 + originMinute
                val firstTick = ceil(left / timeStep).toLong()
                val lastTick = floor(right / timeStep).toLong()
                if (lastTick >= firstTick) {
                    for (tick in firstTick..lastTick) {
                        val minute = tick * timeStep
                        val x = xForBar((minute - originMinute).toDouble())
                        drawLine(C.Grid.copy(alpha = 0.28f), Offset(x, 0f),
                            Offset(x, plotHeight), 1.dp.toPx())
                        val label = candleTimeText(minute * MINUTE_MS)
                        val halfText = timePaint.measureText(label) / 2f
                        if (x - halfText >= 0f && x + halfText <= plotWidth) {
                            native.drawText(label, x,
                                baseline(plotHeight - 7.dp.toPx(), timePaint), timePaint)
                        }
                    }
                }
                val spacing = plotWidth / view.visibleBars.toFloat()
                val bodyWidth = (spacing * 0.64f).coerceAtLeast(0.7.dp.toPx())
                val wickWidth = minOf(1.2.dp.toPx(), bodyWidth)
                clipRect(0f, 0f, plotWidth, plotHeight) {
                    candles.forEach { candle ->
                        val x = xForBar(barPosition(candle.time, origin))
                        if (x + bodyWidth / 2f >= 0f && x - bodyWidth / 2f <= plotWidth) {
                            val color = if (candle.close >= candle.open) C.Green else C.Red
                            val highY = yForPrice(candle.high.toDouble())
                            val lowY = yForPrice(candle.low.toDouble())
                            val openY = yForPrice(candle.open.toDouble())
                            val closeY = yForPrice(candle.close.toDouble())
                            drawLine(color, Offset(x, highY), Offset(x, lowY),
                                wickWidth, cap = StrokeCap.Butt)
                            val top = minOf(openY, closeY)
                            val height = abs(openY - closeY)
                            if (height < 1.dp.toPx()) {
                                drawLine(color, Offset(x - bodyWidth / 2f, (openY + closeY) / 2f),
                                    Offset(x + bodyWidth / 2f, (openY + closeY) / 2f), 1.dp.toPx())
                            } else {
                                drawRect(color, Offset(x - bodyWidth / 2f, top), Size(bodyWidth, height))
                            }
                        }
                    }
                }
                val currentPrice = candles.last().close.toDouble()
                val currentY = yForPrice(currentPrice)
                if (currentY in 0f..plotHeight) {
                    val dash = 7.dp.toPx()
                    val gap = 5.dp.toPx()
                    var x = 0f
                    while (x < plotWidth) {
                        drawLine(C.Cyan.copy(alpha = 0.85f), Offset(x, currentY),
                            Offset(minOf(x + dash, plotWidth), currentY), 1.dp.toPx())
                        x += dash + gap
                    }
                    val label = priceText(currentPrice, decimals)
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
            PairHeader(Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 8.dp))
            Column(Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 60.dp)) {
                Text(fmt(candles.last().close), color = C.TextSecondary, fontSize = 11.sp)
                Text("$clockText UTC+3", color = C.TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

// نصف قطر الإصبعين السابق لتطبيق حد اللمس بدون تحريك نقطة الارتكاز.
private fun androidx.compose.ui.input.pointer.PointerEvent.calculateCentroidSizeCompat(): Float {
    val center = calculateCentroid(useCurrent = false)
    if (center == Offset.Unspecified) return 0f
    var total = 0f
    var count = 0
    changes.forEach {
        if (it.pressed && it.previousPressed) {
            total += (it.previousPosition - center).getDistance()
            count++
        }
    }
    return if (count == 0) 0f else total / count
}

@Composable
fun TradePanel() {
    Column(Modifier.fillMaxWidth().background(C.BG)) {
        Spacer(Modifier.height(6.dp))
        SentimentBar()
        Spacer(Modifier.height(8.dp))
        TradeInputs()
        Spacer(Modifier.height(8.dp))
        TradeButtons()
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun SentimentBar() {
    var buy by remember { mutableStateOf(33) }
    LaunchedEffect(Unit) {
        while (true) { delay(2200L); buy = Random.nextInt(30, 37) }
    }
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
private fun TradeInput(
    label: String, value: String, modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
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
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TradeButton("شراء", C.Green, Modifier.weight(1f))
        TradeButton("بيع", C.Red, Modifier.weight(1f))
    }
}

@Composable
private fun TradeButton(label: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.height(48.dp).clip(RoundedCornerShape(12.dp)).background(color)
        .clickable { /* اربط تنفيذ الصفقة هنا كما في الكود الأصلي. */ },
        contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("92%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
fun BottomItem(
    label: String, selected: Boolean, onClick: () -> Unit,
    icon: @Composable (Color) -> Unit
) {
    val color = if (selected) C.Cyan else C.TextSecondary
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
        .padding(horizontal = 6.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
        val r = size.minDimension * 0.11f
        val y = size.height / 2f
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
