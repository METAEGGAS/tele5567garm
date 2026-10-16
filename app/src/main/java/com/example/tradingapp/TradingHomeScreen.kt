package com.example.tradingapp

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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

// ============================================================
// الصور
// ============================================================

const val AVATAR_URL =
    "https://i.ibb.co/Z68jNfbd/IMG-20250906-195228-439.jpg"

const val AVATAR_FRAME_URL =
    "https://i.ibb.co/5xvxC8Kw/file-00000000d4f882439f813f969679659f.png"

const val GIFT_ICON_URL =
    "https://cdn-icons-png.flaticon.com/128/18546/18546266.png"

// ============================================================
// ثيم موحد بلون رئيسي واحد
// ============================================================

object C {
    val BG = Color(0xFF141126)
    val Surface = Color(0xFF24203D)
    val SurfaceRaised = Color(0xFF30284F)

    val Purple = Color(0xFF9C7BFF)
    val Cyan = Color(0xFF58D9F5)

    // الحفاظ على دلالة الشراء والبيع
    val Green = Color(0xFF2ECC91)
    val Red = Color(0xFFF06489)

    val Blue = Cyan
    val Yellow = Color(0xFFFFD34D)

    val TextPrimary = Color(0xFFF5F2FF)
    val TextSecondary = Color(0xFFA9A5C6)

    val Divider = Color(0xFF3D355A)
    val Grid = Color(0xFF67558D)
    val PriceTag = Cyan
}

// ============================================================
// إعدادات الرسم
// ============================================================

private const val MINUTE_MS = 60_000L

// كل هامش يمثل 15% من ارتفاع منطقة الشموع.
private const val CANDLE_SAFE_MARGIN = 0.15

// حدود التكبير والتصغير اليدوي.
private const val MIN_VISIBLE_BARS = 10.0
private const val MAX_VISIBLE_BARS = 120.0

private const val MIN_PRICE_SPAN = 0.00001

// موضع الشمعة النشطة الثابت: 80% من عرض الرسم
// أي 20% مساحة مستقبلية أمامها دائمًا.
private const val LIVE_CANDLE_FRACTION = 0.80

private val ChartRightAxisWidth = 64.dp

// ============================================================
// البيانات
// ============================================================

data class Candle(
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val time: Long = 0L
)

private fun Candle.barPosition(): Double =
    time.toDouble() / MINUTE_MS.toDouble()

fun seedCandles(
    lastOpenTime: Long =
        System.currentTimeMillis() / MINUTE_MS * MINUTE_MS
): List<Candle> {
    val anchors = listOf(
        0 to 1.8772f,
        5 to 1.8748f,
        9 to 1.8742f,
        13 to 1.8708f,
        17 to 1.8685f,
        21 to 1.8662f,
        25 to 1.8644f,
        28 to 1.86344f,
        32 to 1.8660f,
        36 to 1.8686f,
        40 to 1.8678f,
        44 to 1.8672f,
        48 to 1.8686f,
        52 to 1.86978f
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

        val fraction =
            if (a.first == b.first) {
                0f
            } else {
                (index - a.first).toFloat() /
                    (b.first - a.first).toFloat()
            }

        return a.second + (b.second - a.second) * fraction
    }

    val random = java.util.Random(42)
    val result = ArrayList<Candle>()
    var previousClose = 1.8772f

    for (index in 0..52) {
        val close =
            priceAt(index) +
                (random.nextFloat() - 0.5f) * 0.0009f

        result.add(
            Candle(
                open = previousClose,
                high = maxOf(previousClose, close) +
                    random.nextFloat() * 0.0008f,
                low = minOf(previousClose, close) -
                    random.nextFloat() * 0.0008f,
                close = close,
                time = lastOpenTime -
                    (52L - index.toLong()) * MINUTE_MS
            )
        )

        previousClose = close
    }

    return result
}

fun clockNow(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT+3")
    }.format(Date())
}

fun fmt(price: Float): String =
    String.format(Locale.US, "%.5f", price)

private fun priceText(price: Double, decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", price)

private fun candleTimeText(time: Long): String {
    return SimpleDateFormat("HH:mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT+3")
    }.format(Date(time))
}

private fun remainingSeconds(openTime: Long, now: Long): Int {
    return ceil(
        (openTime + MINUTE_MS - now).toDouble() / 1000.0
    ).toInt().coerceIn(0, 60)
}

// ============================================================
// نافذة الشارت
//
// X: لا يُخزَّن مركز الزمن إطلاقًا، بل يُحسب دائمًا من قاعدة
//    الشمعة النشطة عند 80% من عرض الرسم.
// Y: مركز سعر ونطاق سعر فقط.
//
// عند التكبير: مركز السعر ثابت والنطاق يتغير بنسبة الشموع،
// فتبقى الشبكة ثابتة في مكانها وتتوسع الشموع سعريًا وزمنيًا
// حول نفس المكان.
// ============================================================

private data class ChartViewport(
    val visibleBars: Double,
    val centerPrice: Double,
    val priceSpan: Double
) {
    val minPrice: Double
        get() = centerPrice - priceSpan / 2.0

    val maxPrice: Double
        get() = centerPrice + priceSpan / 2.0
}

// مركز الزمن مشتق دائمًا من آخر شمعة وقاعدة الـ80%.
private fun centerBarFor(
    visibleBars: Double,
    lastBar: Double
): Double {
    return lastBar -
        (LIVE_CANDLE_FRACTION - 0.5) * visibleBars
}

private fun initialViewport(
    candles: List<Candle>
): ChartViewport {
    val lowest = candles.minOf { it.low.toDouble() }
    val highest = candles.maxOf { it.high.toDouble() }

    return ChartViewport(
        visibleBars = 60.0,
        centerPrice = (lowest + highest) / 2.0,
        priceSpan = (
            (highest - lowest) / 0.70 * 1.04
        ).coerceAtLeast(MIN_PRICE_SPAN)
    )
}

/**
 * يحافظ على جميع الشموع الظاهرة داخل 15%..85% عموديًا.
 *
 * لا يمس الزمن أو مركز السعر إلا عند الحاجة،
 * فلا يهتز المقياس مع كل تحديث صغير.
 */
private fun fitViewportToCandles(
    viewport: ChartViewport,
    candles: List<Candle>
): ChartViewport {
    if (candles.isEmpty()) return viewport

    val lastBar = candles.last().barPosition()
    val centerBar = centerBarFor(viewport.visibleBars, lastBar)

    val halfBars = viewport.visibleBars / 2.0

    val visible = candles.filter {
        abs(it.barPosition() - centerBar) <=
            halfBars + 0.6
    }

    if (visible.isEmpty()) return viewport

    val lowest = visible.minOf { it.low.toDouble() }
    val highest = visible.maxOf { it.high.toDouble() }

    val span = viewport.priceSpan.coerceAtLeast(MIN_PRICE_SPAN)

    // نصف مساحة الأمان المتاحة حول مركز السعر.
    val safeHalfFraction = 0.5 - CANDLE_SAFE_MARGIN

    val allowedLow =
        viewport.centerPrice - span * safeHalfFraction

    val allowedHigh =
        viewport.centerPrice + span * safeHalfFraction

    if (lowest >= allowedLow && highest <= allowedHigh) {
        return viewport.copy(priceSpan = span)
    }

    // مجال المراكز الذي يسمح باستيعاب جميع الشموع
    // دون تغيير حجم النطاق الحالي.
    val minimumCenter = highest - span * safeHalfFraction
    val maximumCenter = lowest + span * safeHalfFraction

    if (minimumCenter <= maximumCenter) {
        return viewport.copy(
            centerPrice = viewport.centerPrice.coerceIn(
                minimumCenter,
                maximumCenter
            ),
            priceSpan = span
        )
    }

    // إذا لم تكفِ المساحة، نوسع النطاق مع احتياط صغير.
    val expandedSpan = maxOf(
        span,
        (highest - lowest) /
            (1.0 - 2.0 * CANDLE_SAFE_MARGIN) * 1.04,
        MIN_PRICE_SPAN
    )

    return viewport.copy(
        centerPrice = (highest + lowest) / 2.0,
        priceSpan = expandedSpan
    )
}

// ============================================================
// خطوات الشبكة
// ============================================================

private fun nicePriceStep(rawStep: Double): Double {
    if (!rawStep.isFinite() || rawStep <= 0.0) {
        return 0.001
    }

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

        if (abs(scaled - kotlin.math.round(scaled)) < 0.000001) {
            return decimals
        }
    }

    return 8
}

private fun niceTimeStep(rawMinutes: Double): Long {
    val steps = longArrayOf(
        1, 2, 5, 10, 15, 20, 30, 60, 120, 240
    )

    return steps.firstOrNull {
        it.toDouble() >= rawMinutes
    } ?: steps.last()
}

// ============================================================
// الشاشة الرئيسية
// ============================================================

@Composable
fun TradingHomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(C.BG)
    ) {
        TopBar()

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(C.Divider)
        )

        // الشارت يملأ كل المساحة المتاحة حتى أقرب نقطة للأسفل،
        // ولوحة التداول تليه مباشرة بدون أي فراغ زائد.
        LiveCandlestickChart(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        TradePanel()

        BottomNavBar()
    }
}

// ============================================================
// الشريط العلوي
// ============================================================

@Composable
fun TopBar() {
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(C.BG)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.Yellow),
                contentAlignment = Alignment.Center
            ) {
                WalletIcon(
                    Modifier.size(26.dp),
                    color = Color(0xFF312740)
                )
            }

            Spacer(Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Surface)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "QT Real",
                            color = C.TextSecondary,
                            fontSize = 10.sp
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            "حلال",
                            color = C.Green,
                            fontSize = 10.sp
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            "USD",
                            color = C.TextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        "0.39",
                        color = C.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(10.dp))
                DownArrow(Modifier.size(10.dp))
            }

            Spacer(Modifier.weight(1f))

            Box {
                AsyncImage(
                    model = GIFT_ICON_URL,
                    contentDescription = "هدية",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(36.dp)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-3).dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(C.BG)
                        .border(1.dp, C.Divider, CircleShape)
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val margin = size.minDimension * 0.22f
                        val cx = size.width / 2f
                        val cy = size.height / 2f

                        drawLine(
                            Color.White,
                            Offset(cx - margin, cy - margin),
                            Offset(cx + margin, cy + margin),
                            1.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        drawLine(
                            Color.White,
                            Offset(cx + margin, cy - margin),
                            Offset(cx - margin, cy + margin),
                            1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = AVATAR_URL,
                    contentDescription = "الصورة الشخصية",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(C.Surface)
                )

                AsyncImage(
                    model = AVATAR_FRAME_URL,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(52.dp)
                )
            }
        }
    }
}

// ============================================================
// اختيار الزوج
// ============================================================

@Composable
fun PairHeader(modifier: Modifier = Modifier) {
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(C.Surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .width(34.dp)
                        .height(22.dp)
                ) {
                    FlagAE(
                        Modifier
                            .size(22.dp)
                            .align(Alignment.CenterStart)
                    )

                    FlagCN(
                        Modifier
                            .size(22.dp)
                            .align(Alignment.CenterEnd)
                            .border(
                                1.5.dp,
                                C.Surface,
                                CircleShape
                            )
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    "AED/CNY OTC",
                    color = C.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.width(6.dp))
                DownArrow(Modifier.size(9.dp))
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Surface),
                contentAlignment = Alignment.Center
            ) {
                DotsIcon(Modifier.size(16.dp))
            }
        }
    }
}

// ============================================================
// الشارت
// ============================================================

@Composable
fun LiveCandlestickChart(modifier: Modifier = Modifier) {
    val candles = remember {
        mutableStateListOf<Candle>().apply {
            addAll(seedCandles())
        }
    }

    var viewport by remember {
        mutableStateOf(initialViewport(candles))
    }

    var chartSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    var secondsLeft by remember {
        mutableStateOf(
            remainingSeconds(
                candles.last().time,
                System.currentTimeMillis()
            )
        )
    }

    var clockText by remember {
        mutableStateOf(clockNow())
    }

    val density = LocalDensity.current

    val rightAxisPx = with(density) {
        ChartRightAxisWidth.toPx()
    }

    // خط المحاور أصغر قليلًا: أسعار الحواف.
    val axisTextPx = with(density) {
        9.sp.toPx()
    }

    val tagTextPx = with(density) {
        10.sp.toPx()
    }

    val axisPaint = remember(axisTextPx) {
        NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = C.TextSecondary.toArgb()
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

    val fittedViewport = fitViewportToCandles(
        viewport,
        candles
    )

    // حفظ التوسيع التلقائي العمودي فقط،
    // دون تقليص تلقائي مع كل تحديث.
    SideEffect {
        if (viewport != fittedViewport) {
            viewport = fittedViewport
        }
    }

    // التكبير حول مكان ثابت:
    // الشموع والنطاق السعري يتوسعان بنفس النسبة معًا،
    // ومركز السعر لا يتحرك، فالشبكة تبقى في موضعها
    // ولا يبدو الشارت ككل أنه ينتقل أو يتكبر حول اللمسة.
    fun applyZoom(factor: Double) {
        if (!factor.isFinite() || factor <= 0.0) {
            return
        }

        val newBars = (
            viewport.visibleBars / factor
        ).coerceIn(MIN_VISIBLE_BARS, MAX_VISIBLE_BARS)

        val ratio = newBars / viewport.visibleBars

        viewport = viewport.copy(
            visibleBars = newBars,
            priceSpan = (
                viewport.priceSpan * ratio
            ).coerceAtLeast(MIN_PRICE_SPAN)
        )
    }

    val currentZoom by rememberUpdatedState<
        (Double) -> Unit
    >(
        newValue = { factor ->
            applyZoom(factor)
        }
    )

    // تحديث تجريبي للأسعار.
    LaunchedEffect(Unit) {
        while (true) {
            delay(260L)

            val now = System.currentTimeMillis()
            val currentMinute =
                now / MINUTE_MS * MINUTE_MS

            // إضافة كل الدقائق الناقصة عند عودة التطبيق.
            // مركز الزمن مشتق من قاعدة الـ80%،
            // فيتقدم تلقائيًا مع كل شمعة جديدة
            // دون أي تحريك يدوي أو استجابة للسحب.
            while (candles.last().time < currentMinute) {
                val previous = candles.last()

                candles.add(
                    Candle(
                        open = previous.close,
                        high = previous.close,
                        low = previous.close,
                        close = previous.close,
                        time = previous.time + MINUTE_MS
                    )
                )

                if (candles.size > 600) {
                    candles.removeAt(0)
                }
            }

            val index = candles.lastIndex
            val current = candles[index]

            val nextClose = (
                current.close +
                    (Random.nextFloat() - 0.5f) * 0.00035f
            ).coerceAtLeast(0.00001f)

            candles[index] = current.copy(
                close = nextClose,
                high = maxOf(current.high, nextClose),
                low = minOf(current.low, nextClose)
            )

            secondsLeft = remainingSeconds(
                candles.last().time,
                now
            )

            clockText = clockNow()
        }
    }

    // LTR محلي لهذا المكون.
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr
    ) {
        Box(
            modifier = modifier
                .background(C.BG)
        ) {
            // خلفية موحدة بلون المنصة الأساسي
            // بدون صورة ولا تدرج لضمان لون واحد في كل الشاشة.

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        chartSize = it
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures {
                            _,
                            _,
                            zoom,
                            _ ->

                            // تجاهل pan والدوران ونقطة اللمس عمدًا.
                            if (
                                zoom.isFinite() &&
                                zoom > 0f &&
                                abs(zoom - 1f) > 0.0001f
                            ) {
                                currentZoom(zoom.toDouble())
                            }
                        }
                    }
            ) {
                // لا يوجد أي بادينج يمين أو يسار:
                // منطقة الشموع تبدأ من حافة الشاشة اليسرى.
                val plotLeft = 0f
                val plotRight = size.width - rightAxisPx

                val plotWidth = plotRight - plotLeft

                // لا يوجد شريط وقت سفلي:
                // الرسم يمتد لكامل ارتفاع الكانفاس.
                val plotHeight = size.height

                if (plotWidth <= 1f || plotHeight <= 1f) {
                    return@Canvas
                }

                val view = fittedViewport

                val lastBar = candles.last().barPosition()
                val centerBar = centerBarFor(
                    view.visibleBars,
                    lastBar
                )

                fun xForBar(bar: Double): Float {
                    return plotLeft + (
                        (
                            bar - centerBar
                        ) / view.visibleBars + 0.5
                    ).toFloat() * plotWidth
                }

                fun yForPrice(price: Double): Float {
                    return (
                        (view.maxPrice - price) /
                            view.priceSpan
                    ).toFloat() * plotHeight
                }

                fun centeredBaseline(
                    centerY: Float,
                    paint: NativePaint
                ): Float {
                    return centerY -
                        (paint.ascent() + paint.descent()) / 2f
                }

                val safeTop =
                    plotHeight * CANDLE_SAFE_MARGIN.toFloat()

                val safeBottom =
                    plotHeight *
                        (1.0 - CANDLE_SAFE_MARGIN).toFloat()

                // =================================================
                // شبكة السعر
                // =================================================

                val targetPriceRows = (
                    plotHeight / 58.dp.toPx()
                ).toInt().coerceIn(3, 12)

                val priceStep = nicePriceStep(
                    view.priceSpan / targetPriceRows
                )

                val decimals = priceDecimals(priceStep)

                val firstPrice =
                    ceil(view.minPrice / priceStep).toLong()

                val lastPrice =
                    floor(view.maxPrice / priceStep).toLong()

                val native = drawContext.canvas.nativeCanvas

                if (lastPrice >= firstPrice) {
                    for (tick in firstPrice..lastPrice) {
                        val price = tick.toDouble() * priceStep
                        val y = yForPrice(price)

                        drawLine(
                            color = C.Grid.copy(alpha = 0.34f),
                            start = Offset(plotLeft, y),
                            end = Offset(plotRight, y),
                            strokeWidth = 1.dp.toPx()
                        )

                        if (
                            y >= axisTextPx &&
                            y <= plotHeight - axisTextPx / 2f
                        ) {
                            native.drawText(
                                priceText(price, decimals),
                                size.width - 5.dp.toPx(),
                                centeredBaseline(y, axisPaint),
                                axisPaint
                            )
                        }
                    }
                }

                // =================================================
                // شبكة الزمن — داخل الشارت نفسه
                // بدون شريط سفلي، بدون خلفية، بدون ظل أو صندوق.
                // =================================================

                val targetTimeColumns = (
                    plotWidth / 64.dp.toPx()
                ).toInt().coerceAtLeast(2)

                val timeStep = niceTimeStep(
                    view.visibleBars / targetTimeColumns
                )

                val firstBar =
                    centerBar - view.visibleBars / 2.0

                val lastBarLimit =
                    centerBar + view.visibleBars / 2.0

                val firstTimeTick =
                    ceil(firstBar / timeStep.toDouble()).toLong()

                val lastTimeTick =
                    floor(lastBarLimit / timeStep.toDouble()).toLong()

                if (lastTimeTick >= firstTimeTick) {
                    for (tick in firstTimeTick..lastTimeTick) {
                        val minute = tick * timeStep
                        val x = xForBar(minute.toDouble())

                        drawLine(
                            color = C.Grid.copy(alpha = 0.28f),
                            start = Offset(x, 0f),
                            end = Offset(x, plotHeight),
                            strokeWidth = 1.dp.toPx()
                        )

                        val label = candleTimeText(
                            minute * MINUTE_MS
                        )

                        val halfText =
                            timePaint.measureText(label) / 2f

                        // نص الوقت مرسوم داخل منطقة الشموع
                        // عند أسفل الحافة مباشرة، بلا خلفية.
                        if (
                            x - halfText >= plotLeft &&
                            x + halfText <= plotRight
                        ) {
                            native.drawText(
                                label,
                                x,
                                centeredBaseline(
                                    plotHeight - 7.dp.toPx(),
                                    timePaint
                                ),
                                timePaint
                            )
                        }
                    }
                }

                // فصل عمود الأسعار عن منطقة الشموع.
                drawLine(
                    C.Divider.copy(alpha = 0.65f),
                    Offset(plotRight, 0f),
                    Offset(plotRight, plotHeight),
                    1.dp.toPx()
                )

                // =================================================
                // رسم الشموع
                // =================================================

                val candleSpacing =
                    plotWidth / view.visibleBars.toFloat()

                val bodyWidth = (
                    candleSpacing * 0.64f
                ).coerceAtLeast(0.7.dp.toPx())

                val wickWidth = minOf(
                    1.2.dp.toPx(),
                    bodyWidth
                )

                clipRect(
                    left = plotLeft,
                    top = 0f,
                    right = plotRight,
                    bottom = plotHeight
                ) {
                    candles.forEach { candle ->
                        val x = xForBar(candle.barPosition())

                        if (
                            x + bodyWidth / 2f >= plotLeft &&
                            x - bodyWidth / 2f <= plotRight
                        ) {
                            val color =
                                if (candle.close >= candle.open) {
                                    C.Green
                                } else {
                                    C.Red
                                }

                            val highY =
                                yForPrice(candle.high.toDouble())

                            val lowY =
                                yForPrice(candle.low.toDouble())

                            val openY =
                                yForPrice(candle.open.toDouble())

                            val closeY =
                                yForPrice(candle.close.toDouble())

                            drawLine(
                                color = color,
                                start = Offset(x, highY),
                                end = Offset(x, lowY),
                                strokeWidth = wickWidth,
                                cap = StrokeCap.Butt
                            )

                            val bodyTop = minOf(openY, closeY)
                            val bodyBottom = maxOf(openY, closeY)
                            val bodyHeight = bodyBottom - bodyTop

                            if (bodyHeight < 1.dp.toPx()) {
                                // شمعة دوجي: خط في موضعها الحقيقي.
                                drawLine(
                                    color = color,
                                    start = Offset(
                                        x - bodyWidth / 2f,
                                        (openY + closeY) / 2f
                                    ),
                                    end = Offset(
                                        x + bodyWidth / 2f,
                                        (openY + closeY) / 2f
                                    ),
                                    strokeWidth = 1.dp.toPx()
                                )
                            } else {
                                drawRect(
                                    color = color,
                                    topLeft = Offset(
                                        x - bodyWidth / 2f,
                                        bodyTop
                                    ),
                                    size = Size(
                                        bodyWidth,
                                        bodyHeight
                                    )
                                )
                            }
                        }
                    }
                }

                // =================================================
                // خط السعر الحالي
                //
                // الشمعة النشطة ثابتة دائمًا عند 80% من العرض
                // عبر centerBar المشتق من قاعدة الـ80%.
                // =================================================

                val currentPrice =
                    candles.last().close.toDouble()

                val currentY = yForPrice(currentPrice)

                if (currentY in 0f..plotHeight) {
                    val dash = 7.dp.toPx()
                    val gap = 5.dp.toPx()

                    var dashX = plotLeft

                    while (dashX < plotRight) {
                        drawLine(
                            color = C.Cyan.copy(alpha = 0.85f),
                            start = Offset(dashX, currentY),
                            end = Offset(
                                minOf(dashX + dash, plotRight),
                                currentY
                            ),
                            strokeWidth = 1.dp.toPx()
                        )

                        dashX += dash + gap
                    }

                    val tagLabel = priceText(
                        currentPrice,
                        decimals
                    )

                    tagPaint.textSize = tagTextPx

                    val availableTextWidth =
                        rightAxisPx - 12.dp.toPx()

                    val measuredWidth =
                        tagPaint.measureText(tagLabel)

                    if (measuredWidth > availableTextWidth) {
                        tagPaint.textSize =
                            tagTextPx *
                                availableTextWidth / measuredWidth
                    }

                    val tagHeight = 22.dp.toPx()

                    drawRoundRect(
                        color = C.PriceTag,
                        topLeft = Offset(
                            plotRight + 2.dp.toPx(),
                            currentY - tagHeight / 2f
                        ),
                        size = Size(
                            rightAxisPx - 6.dp.toPx(),
                            tagHeight
                        ),
                        cornerRadius = CornerRadius(
                            4.dp.toPx()
                        )
                    )

                    native.drawText(
                        tagLabel,
                        plotRight + rightAxisPx / 2f -
                            1.dp.toPx(),
                        centeredBaseline(currentY, tagPaint),
                        tagPaint
                    )

                    native.drawText(
                        "M1",
                        plotRight - 6.dp.toPx(),
                        currentY - 9.dp.toPx(),
                        detailPaint
                    )

                    native.drawText(
                        String.format(
                            Locale.US,
                            "%02d:%02d",
                            secondsLeft / 60,
                            secondsLeft % 60
                        ),
                        plotRight - 6.dp.toPx(),
                        currentY + 19.dp.toPx(),
                        detailPaint
                    )
                }
            }

            PairHeader(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 8.dp)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 60.dp)
            ) {
                Text(
                    fmt(candles.last().close),
                    color = C.TextSecondary,
                    fontSize = 11.sp
                )

                Text(
                    "$clockText UTC+3",
                    color = C.TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ============================================================
// لوحة التداول — هوامش وفراغات أقل
// ============================================================

@Composable
fun TradePanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(C.BG)
    ) {
        Spacer(Modifier.height(6.dp))

        SentimentBar()

        Spacer(Modifier.height(8.dp))

        TradeInputs()

        Spacer(Modifier.height(8.dp))

        TradeButtons()

        Spacer(Modifier.height(4.dp))
    }
}

// ============================================================
// نسبة الشراء والبيع
// ============================================================

@Composable
fun SentimentBar() {
    var buy by remember {
        mutableStateOf(33)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2200L)
            buy = Random.nextInt(30, 37)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$buy%",
            color = C.Green,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.width(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
        ) {
            val availableWidth = maxWidth

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(5.dp))
            ) {
                Box(
                    Modifier
                        .weight(buy.toFloat())
                        .fillMaxHeight()
                        .background(C.Green)
                )

                Box(
                    Modifier
                        .weight((100 - buy).toFloat())
                        .fillMaxHeight()
                        .background(C.Red)
                )
            }

            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(
                        x = availableWidth *
                            (buy / 100f) - 1.25.dp
                    )
                    .width(2.5.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(C.Cyan)
            )
        }

        Spacer(Modifier.width(8.dp))

        Text(
            "${100 - buy}%",
            color = C.Red,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ============================================================
// الوقت والمبلغ
// ============================================================

@Composable
fun TradeInputs() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TradeInput(
            label = "الزمن",
            value = "00:01:00",
            modifier = Modifier.weight(1f)
        ) {
            ClockIcon(Modifier.size(18.dp))
        }

        TradeInput(
            label = "المبلغ",
            value = "1",
            modifier = Modifier.weight(1f)
        ) {
            DollarIcon(Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TradeInput(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    Column(modifier) {
        Text(
            label,
            color = C.TextSecondary,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(C.Surface)
                .border(
                    1.dp,
                    C.Divider,
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value,
                color = C.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            icon()
        }
    }
}

// ============================================================
// أزرار الشراء والبيع
// ============================================================

@Composable
fun TradeButtons() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TradeButton(
            label = "شراء",
            color = C.Green,
            modifier = Modifier.weight(1f)
        )

        TradeButton(
            label = "بيع",
            color = C.Red,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TradeButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .clickable {
                // اربط تنفيذ الصفقة هنا.
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.width(8.dp))

            Text(
                "92%",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ============================================================
// الشريط السفلي — أيقونات بحجم واحد وترتيب معكوس
// المزيد في أقصى اليمين ثم أول الاجتماعي ثم Pro mode
// ثم الرؤى ثم التداولات في أقصى اليسار.
// ============================================================

@Composable
fun BottomNavBar() {
    var selected by remember {
        mutableStateOf(2)
    }

    Column(
        modifier = Modifier.background(C.BG)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(C.Divider)
        )

        // اتجاه ثابت لضمان أن المزيد يبقى في أقصى اليمين
        // مهما كانت اتجاهية النظام.
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Ltr
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(C.BG)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomItem(
                    "التداولات",
                    selected == 0,
                    { selected = 0 }
                ) {
                    TradesIcon(Modifier.size(24.dp), it)
                }

                BottomItem(
                    "الرؤى",
                    selected == 1,
                    { selected = 1 }
                ) {
                    SignalIcon(Modifier.size(24.dp), it)
                }

                BottomItem(
                    "Pro mode",
                    selected == 2,
                    { selected = 2 },
                    highlight = true
                ) {
                    ProIcon(Modifier.size(24.dp), it)
                }

                BottomItem(
                    "أول الاجتماعي",
                    selected == 3,
                    { selected = 3 }
                ) {
                    SocialIcon(Modifier.size(24.dp), it)
                }

                BottomItem(
                    "المزيد",
                    selected == 4,
                    { selected = 4 }
                ) {
                    DotsIcon(Modifier.size(24.dp), it)
                }
            }
        }
    }
}

@Composable
fun BottomItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    highlight: Boolean = false,
    icon: @Composable (Color) -> Unit
) {
    val iconColor = when {
        highlight && selected -> C.Cyan
        selected -> C.Purple
        else -> C.TextSecondary
    }

    val labelColor = when {
        highlight && selected -> C.TextPrimary
        selected -> C.Cyan
        else -> C.TextSecondary
    }

    // كل الأيقونات بنفس الحجم تمامًا،
    // والتمييز يكون بالألوان فقط.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (highlight && selected) {
                        C.SurfaceRaised
                    } else {
                        Color.Transparent
                    }
                )
                .border(
                    1.dp,
                    if (highlight && selected) {
                        C.Cyan
                    } else {
                        Color.Transparent
                    },
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            icon(iconColor)
        }

        Spacer(Modifier.height(3.dp))

        Text(
            label,
            color = labelColor,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// ============================================================
// الأيقونات
// ============================================================

@Composable
fun DownArrow(
    modifier: Modifier = Modifier,
    color: Color = C.TextSecondary
) {
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2f, size.height)
            close()
        }

        drawPath(path, color)
    }
}

@Composable
fun DotsIcon(
    modifier: Modifier = Modifier,
    color: Color = C.TextPrimary
) {
    Canvas(modifier) {
        val radius = size.minDimension * 0.11f
        val cy = size.height / 2f

        drawCircle(
            color,
            radius,
            Offset(size.width * 0.15f, cy)
        )

        drawCircle(
            color,
            radius,
            Offset(size.width * 0.5f, cy)
        )

        drawCircle(
            color,
            radius,
            Offset(size.width * 0.85f, cy)
        )
    }
}

@Composable
fun WalletIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.05f, h * 0.22f),
            size = Size(w * 0.9f, h * 0.62f),
            cornerRadius = CornerRadius(w * 0.1f),
            style = Stroke(width = w * 0.08f)
        )

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.55f, h * 0.4f),
            size = Size(w * 0.4f, h * 0.28f),
            cornerRadius = CornerRadius(w * 0.06f),
            style = Stroke(width = w * 0.07f)
        )

        drawCircle(
            color,
            w * 0.05f,
            Offset(w * 0.73f, h * 0.54f)
        )
    }
}

@Composable
fun ClockIcon(
    modifier: Modifier = Modifier,
    color: Color = C.Cyan
) {
    Canvas(modifier) {
        val dimension = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f
        val stroke = dimension * 0.09f

        drawCircle(
            color,
            dimension * 0.4f,
            Offset(cx, cy),
            style = Stroke(width = stroke)
        )

        drawLine(
            color,
            Offset(cx, cy),
            Offset(cx, size.height * 0.25f),
            stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color,
            Offset(cx, cy),
            Offset(size.width * 0.72f, cy),
            stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun DollarIcon(
    modifier: Modifier = Modifier,
    color: Color = C.Cyan
) {
    Canvas(modifier) {
        val dimension = size.minDimension
        val stroke = dimension * 0.09f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val s = dimension * 0.16f

        drawCircle(
            color,
            dimension * 0.4f,
            Offset(cx, cy),
            style = Stroke(width = stroke)
        )

        drawLine(
            color,
            Offset(cx + s * 0.6f, cy - s),
            Offset(cx - s * 0.5f, cy - s * 0.2f),
            stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color,
            Offset(cx - s * 0.5f, cy - s * 0.2f),
            Offset(cx + s * 0.5f, cy + s * 0.2f),
            stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color,
            Offset(cx + s * 0.5f, cy + s * 0.2f),
            Offset(cx - s * 0.6f, cy + s),
            stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color,
            Offset(cx, cy - s * 1.4f),
            Offset(cx, cy - s),
            stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color,
            Offset(cx, cy + s),
            Offset(cx, cy + s * 1.4f),
            stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun FlagAE(modifier: Modifier = Modifier) {
    Canvas(modifier.clip(CircleShape)) {
        val w = size.width
        val h = size.height

        drawRect(
            Color(0xFF00732F),
            size = Size(w, h / 3f)
        )

        drawRect(
            Color.White,
            topLeft = Offset(0f, h / 3f),
            size = Size(w, h / 3f)
        )

        drawRect(
            Color(0xFF151515),
            topLeft = Offset(0f, 2f * h / 3f),
            size = Size(w, h / 3f)
        )

        drawRect(
            Color.Red,
            size = Size(w * 0.28f, h)
        )
    }
}

@Composable
fun FlagCN(modifier: Modifier = Modifier) {
    Canvas(modifier.clip(CircleShape)) {
        drawRect(Color(0xFFDE2910))

        drawCircle(
            Color(0xFFFFDE00),
            size.minDimension * 0.16f,
            Offset(
                size.width * 0.32f,
                size.height * 0.34f
            )
        )
    }
}

@Composable
fun TradesIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.09f

        drawLine(
            color,
            Offset(w * 0.3f, h * 0.85f),
            Offset(w * 0.3f, h * 0.15f),
            stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color,
            Offset(w * 0.3f, h * 0.15f),
            Offset(w * 0.15f, h * 0.32f),
            stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color,
            Offset(w * 0.3f, h * 0.15f),
            Offset(w * 0.45f, h * 0.32f),
            stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color,
            Offset(w * 0.7f, h * 0.15f),
            Offset(w * 0.7f, h * 0.85f),
            stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color,
            Offset(w * 0.7f, h * 0.85f),
            Offset(w * 0.55f, h * 0.68f),
            stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color,
            Offset(w * 0.7f, h * 0.85f),
            Offset(w * 0.85f, h * 0.68f),
            stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SignalIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.75f
        val stroke = size.minDimension * 0.09f

        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(
                cx - size.width * 0.42f,
                cy - size.height * 0.42f
            ),
            size = Size(
                size.width * 0.84f,
                size.height * 0.84f
            ),
            style = Stroke(width = stroke)
        )

        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(
                cx - size.width * 0.26f,
                cy - size.height * 0.26f
            ),
            size = Size(
                size.width * 0.52f,
                size.height * 0.52f
            ),
            style = Stroke(width = stroke)
        )

        drawCircle(
            color,
            size.minDimension * 0.1f,
            Offset(cx, cy - size.height * 0.02f)
        )
    }
}

@Composable
fun ProIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.08f, h * 0.12f),
            size = Size(w * 0.84f, h * 0.76f),
            cornerRadius = CornerRadius(w * 0.16f),
            style = Stroke(width = w * 0.08f)
        )

        val path = Path().apply {
            moveTo(w * 0.24f, h * 0.62f)
            lineTo(w * 0.40f, h * 0.44f)
            lineTo(w * 0.54f, h * 0.56f)
            lineTo(w * 0.76f, h * 0.34f)
        }

        drawPath(
            path,
            color,
            style = Stroke(
                width = w * 0.08f,
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
fun SocialIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        drawCircle(
            color,
            size.minDimension * 0.17f,
            Offset(cx, cy - size.height * 0.16f)
        )

        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(
                cx - size.width * 0.28f,
                cy + size.height * 0.04f
            ),
            size = Size(
                size.width * 0.56f,
                size.height * 0.5f
            ),
            style = Stroke(
                width = size.minDimension * 0.08f
            )
        )

        drawCircle(
            color,
            size.minDimension * 0.09f,
            Offset(
                cx - size.width * 0.36f,
                cy - size.height * 0.14f
            )
        )

        drawCircle(
            color,
            size.minDimension * 0.09f,
            Offset(
                cx + size.width * 0.36f,
                cy - size.height * 0.14f
            )
        )
    }
}
