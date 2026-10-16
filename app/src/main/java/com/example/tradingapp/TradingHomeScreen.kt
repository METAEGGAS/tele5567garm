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

const val CHART_BG_URL =
    "https://i.ibb.co/zWHWPv92/file-0000000090c48211944cf52560e3482a.png"

const val GIFT_ICON_URL =
    "https://cdn-icons-png.flaticon.com/128/18546/18546266.png"

// ============================================================
// ثيم بنفسجي / سماوي
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

// كل هامش يمثل 15% من ارتفاع منطقة الشموع، دون محور الوقت.
private const val CANDLE_SAFE_MARGIN = 0.15

private const val MIN_VISIBLE_BARS = 8.0
private const val MAX_VISIBLE_BARS = 140.0

private const val MIN_PRICE_SPAN = 0.00001

private val ChartLeftInset = 8.dp
private val ChartRightAxisWidth = 72.dp
private val ChartTimeAxisHeight = 30.dp

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
// X: وقت حقيقي بالمللي ثانية محول إلى وحدات دقيقة.
// Y: قيمة سعر، وليس إحداثيات بكسل مخزنة.
//
// الشموع والشبكة والمحاور تستخدم النافذة نفسها.
// ============================================================

private data class ChartViewport(
    val centerBar: Double,
    val visibleBars: Double,
    val centerPrice: Double,
    val priceSpan: Double
) {
    val minPrice: Double
        get() = centerPrice - priceSpan / 2.0

    val maxPrice: Double
        get() = centerPrice + priceSpan / 2.0
}

private fun initialViewport(
    candles: List<Candle>
): ChartViewport {
    val lowest = candles.minOf { it.low.toDouble() }
    val highest = candles.maxOf { it.high.toDouble() }

    return ChartViewport(
        // ترك مساحة صغيرة بعد آخر شمعة.
        centerBar = candles.last().barPosition() - 22.0,
        visibleBars = 60.0,
        centerPrice = (lowest + highest) / 2.0,
        priceSpan = (
            (highest - lowest) / 0.70 * 1.04
        ).coerceAtLeast(MIN_PRICE_SPAN)
    )
}

/**
 * يحافظ على جميع الشموع الظاهرة داخل 15%..85%.
 *
 * لا يصغر نطاق الأسعار تلقائيًا، بل يوسعه عند الحاجة فقط.
 * بذلك لا يهتز المقياس صعودًا وهبوطًا مع كل تغير صغير.
 */
private fun fitViewportToCandles(
    viewport: ChartViewport,
    candles: List<Candle>
): ChartViewport {
    if (candles.isEmpty()) return viewport

    val halfBars = viewport.visibleBars / 2.0

    val visible = candles.filter {
        abs(it.barPosition() - viewport.centerBar) <=
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

    // إذا لم تكفِ المساحة، نوسع النطاق مع احتياط صغير
    // حتى لا تلامس الذيول حدود الـ15%.
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

/**
 * تكبير حول نقطة محددة داخل الرسم.
 *
 * لا نطبق pan أو rotation.
 * السعر والوقت الموجودان تحت نقطة اللمس يبقيان
 * تحتها أثناء التحويل، ما لم يتطلب هامش الأمان إعادة ضبط Y.
 */
private fun zoomViewport(
    viewport: ChartViewport,
    factor: Double,
    anchorX: Double,
    anchorY: Double
): ChartViewport {
    if (!factor.isFinite() || factor <= 0.0) {
        return viewport
    }

    val newBars = (
        viewport.visibleBars / factor
    ).coerceIn(MIN_VISIBLE_BARS, MAX_VISIBLE_BARS)

    val ratio = newBars / viewport.visibleBars

    val newSpan = (
        viewport.priceSpan * ratio
    ).coerceAtLeast(MIN_PRICE_SPAN)

    val x = anchorX.coerceIn(0.0, 1.0)
    val y = anchorY.coerceIn(0.0, 1.0)

    val anchorBar =
        viewport.centerBar + (x - 0.5) * viewport.visibleBars

    val anchorPrice =
        viewport.centerPrice + (0.5 - y) * viewport.priceSpan

    return ChartViewport(
        centerBar = anchorBar - (x - 0.5) * newBars,
        visibleBars = newBars,
        centerPrice = anchorPrice - (0.5 - y) * newSpan,
        priceSpan = newSpan
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

        // الشارت له مساحة مستقلة تمامًا.
        // لوحة التداول ليست مرسومة فوق الشموع.
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
    // تثبيت ترتيب هذا الشريط فقط على اليسار الفعلي.
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
                        .background(C.Surface)
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

    val leftInsetPx = with(density) {
        ChartLeftInset.toPx()
    }

    val rightAxisPx = with(density) {
        ChartRightAxisWidth.toPx()
    }

    val timeAxisPx = with(density) {
        ChartTimeAxisHeight.toPx()
    }

    val axisTextPx = with(density) {
        10.sp.toPx()
    }

    val tagTextPx = with(density) {
        11.sp.toPx()
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

    // نفس النافذة الفعلية للرسم والتكبير.
    val fittedViewport = fitViewportToCandles(
        viewport,
        candles
    )

    // حفظ التوسيع التلقائي، دون تقليص تلقائي مع كل تحديث.
    SideEffect {
        if (viewport != fittedViewport) {
            viewport = fittedViewport
        }
    }

    fun applyZoom(
        factor: Double,
        anchor: Offset? = null
    ) {
        val plotWidth =
            chartSize.width.toFloat() -
                leftInsetPx -
                rightAxisPx

        val plotHeight =
            chartSize.height.toFloat() - timeAxisPx

        if (plotWidth <= 1f || plotHeight <= 1f) return

        val current = fitViewportToCandles(
            viewport,
            candles
        )

        val anchorX = if (anchor != null) {
            (
                (anchor.x - leftInsetPx) / plotWidth
            ).toDouble().coerceIn(0.0, 1.0)
        } else {
            0.5
        }

        val anchorY = if (anchor != null) {
            (anchor.y / plotHeight)
                .toDouble()
                .coerceIn(0.0, 1.0)
        } else {
            0.5
        }

        viewport = fitViewportToCandles(
            zoomViewport(
                viewport = current,
                factor = factor,
                anchorX = anchorX,
                anchorY = anchorY
            ),
            candles
        )
    }

    // تتبع أحدث نسخة من الدالة داخل كاشف الإيماءات.
    val currentZoom by rememberUpdatedState<
        (Double, Offset?) -> Unit
    >(
        newValue = { factor, anchor ->
            applyZoom(factor, anchor)
        }
    )

    // تحديث تجريبي للأسعار.
    // لا توجد هنا صفقات حقيقية أو اتصال بمزود أسعار.
    LaunchedEffect(Unit) {
        while (true) {
            delay(260L)

            val now = System.currentTimeMillis()
            val currentMinute =
                now / MINUTE_MS * MINUTE_MS

            // إضافة كل الدقائق الناقصة عند عودة التطبيق.
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

                // تحريك نافذة الزمن فقط عند بدء شمعة جديدة،
                // وليس نتيجة سحب أو تكبير.
                viewport = viewport.copy(
                    centerBar = viewport.centerBar + 1.0
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

    // LTR محلي لهذا المكون لضمان بقاء الزوج والتوقيت يسارًا.
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr
    ) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(0.dp))
                .background(C.BG)
        ) {
            AsyncImage(
                model = CHART_BG_URL,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                C.BG.copy(alpha = 0.93f),
                                Color(0xFF231C3E).copy(alpha = 0.92f),
                                C.BG.copy(alpha = 0.96f)
                            )
                        )
                    )
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        chartSize = it
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures {
                            centroid,
                            _,
                            zoom,
                            _ ->

                            // تجاهل pan والدوران عمدًا.
                            if (
                                zoom.isFinite() &&
                                zoom > 0f &&
                                abs(zoom - 1f) > 0.0001f
                            ) {
                                currentZoom(
                                    zoom.toDouble(),
                                    centroid
                                )
                            }
                        }
                    }
            ) {
                val plotLeft = leftInsetPx
                val plotRight = size.width - rightAxisPx

                val plotWidth = plotRight - plotLeft
                val plotHeight = size.height - timeAxisPx

                if (plotWidth <= 1f || plotHeight <= 1f) {
                    return@Canvas
                }

                val view = fittedViewport

                fun xForBar(bar: Double): Float {
                    return plotLeft + (
                        (
                            bar - view.centerBar
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

                        // عدم قص النص عند حافتي مساحة الرسم.
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
                // شبكة الزمن + شريط أوقات الشموع
                // =================================================

                drawRect(
                    color = C.Surface,
                    topLeft = Offset(0f, plotHeight),
                    size = Size(size.width, timeAxisPx)
                )

                drawLine(
                    C.Divider,
                    Offset(0f, plotHeight),
                    Offset(size.width, plotHeight),
                    1.dp.toPx()
                )

                val targetTimeColumns = (
                    plotWidth / 64.dp.toPx()
                ).toInt().coerceAtLeast(2)

                val timeStep = niceTimeStep(
                    view.visibleBars / targetTimeColumns
                )

                val firstBar =
                    view.centerBar - view.visibleBars / 2.0

                val lastBar =
                    view.centerBar + view.visibleBars / 2.0

                val firstTimeTick =
                    ceil(firstBar / timeStep.toDouble()).toLong()

                val lastTimeTick =
                    floor(lastBar / timeStep.toDouble()).toLong()

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

                        // إبقاء النص مرتبطًا بموقع خطه،
                        // بدل إزاحته إلى موضع غير صحيح.
                        if (
                            x - halfText >= plotLeft &&
                            x + halfText <= plotRight
                        ) {
                            native.drawText(
                                label,
                                x,
                                centeredBaseline(
                                    plotHeight + timeAxisPx / 2f,
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
                //
                // fitViewport يجعل الشموع داخل نطاق الأمان.
                // clipRect حماية إضافية من تجاوزات البكسل.
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
                    top = safeTop,
                    right = plotRight,
                    bottom = safeBottom
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
                // لا يتم تثبيت السعر على حافة مصطنعة.
                // إذا كان خارج النافذة لا نرسمه في موضع كاذب.
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

                    // تصغير خط البطاقة فقط إذا احتاج الرقم مساحة أكبر.
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

            // أزرار التكبير لا تحرك الرسم بالسحب.
            // نقطة ارتكازها هي مركز مساحة الرسم.
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp, top = 58.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ChartControl("−") {
                    applyZoom(1.0 / 1.25)
                }

                ChartControl("+") {
                    applyZoom(1.25)
                }

                ChartControl("↺") {
                    viewport = initialViewport(candles)
                }
            }
        }
    }
}

@Composable
private fun ChartControl(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(C.Surface)
            .border(
                1.dp,
                C.Purple.copy(alpha = 0.55f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = C.Cyan,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ============================================================
// لوحة التداول المعتمة
// ============================================================

@Composable
fun TradePanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(C.BG)
    ) {
        // محور الوقت مرسوم في نهاية الشارت مباشرة فوق هذه اللوحة.
        Spacer(Modifier.height(12.dp))

        SentimentBar()

        Spacer(Modifier.height(12.dp))

        TradeInputs()

        Spacer(Modifier.height(10.dp))

        TradeButtons()

        Spacer(Modifier.height(10.dp))
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
// أزرار الشراء والبيع — نفس عرض صناديق الإدخال
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
// الشريط السفلي
// ============================================================

@Composable
fun BottomNavBar() {
    var selected by remember {
        mutableStateOf(2)
    }

    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(C.Divider)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(C.BG)
                .padding(vertical = 8.dp),
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
                ProIcon(Modifier.size(28.dp), it)
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
                DotsIcon(Modifier.size(22.dp), it)
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        if (highlight) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) {
                            C.SurfaceRaised
                        } else {
                            Color.Transparent
                        }
                    )
                    .border(
                        1.5.dp,
                        if (selected) C.Cyan else C.Divider,
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon(iconColor)
            }
        } else {
            Box(
                Modifier.size(30.dp),
                contentAlignment = Alignment.Center
            ) {
                icon(iconColor)
            }
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
