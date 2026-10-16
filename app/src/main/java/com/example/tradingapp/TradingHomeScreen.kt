package com.example.tradingapp

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.random.Random
import android.graphics.Paint as NativePaint

// ============================================================
// الألوان
// ============================================================
object C {
    val BG = Color(0xFF0F1621)
    val Surface = Color(0xFF1A2332)
    val SurfaceLight = Color(0xFF243044)
    val Green = Color(0xFF2ECC71)
    val Red = Color(0xFFE74C3C)
    val Blue = Color(0xFF3498DB)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8899AA)
    val Divider = Color(0xFF2A3441)
    val Grid = Color(0xFF22303F)
    val PriceTag = Color(0xFF84A7C9)
}

// ============================================================
// نموذج الشمعة + توليد بيانات البداية (نفس شكل الصورة: هبوط ثم صعود)
// ============================================================
data class Candle(val open: Float, val high: Float, val low: Float, val close: Float)

fun seedCandles(): List<Candle> {
    val anchors = listOf(
        0 to 1.8772f, 5 to 1.8748f, 9 to 1.8742f, 13 to 1.8708f,
        17 to 1.8685f, 21 to 1.8662f, 25 to 1.8644f, 28 to 1.86344f,
        32 to 1.8660f, 36 to 1.8686f, 40 to 1.8678f, 44 to 1.8672f,
        48 to 1.8686f, 52 to 1.86978f
    )

    fun priceAt(i: Int): Float {
        var a = anchors.first()
        var b = anchors.last()
        for (k in 0 until anchors.size - 1) {
            if (i >= anchors[k].first && i <= anchors[k + 1].first) {
                a = anchors[k]; b = anchors[k + 1]; break
            }
        }
        val t = if (b.first == a.first) 0f else (i - a.first).toFloat() / (b.first - a.first)
        return a.second + (b.second - a.second) * t
    }

    val rnd = java.util.Random(42)
    val list = ArrayList<Candle>()
    var prevClose = 1.8772f
    for (i in 0..52) {
        val target = priceAt(i) + (rnd.nextFloat() - 0.5f) * 0.0009f
        val open = prevClose
        val close = target
        val high = maxOf(open, close) + rnd.nextFloat() * 0.0008f
        val low = minOf(open, close) - rnd.nextFloat() * 0.0008f
        list.add(Candle(open, high, low, close))
        prevClose = close
    }
    return list
}

// ============================================================
// أدوات الوقت (UTC+3)
// ============================================================
fun clockNow(): String {
    val f = SimpleDateFormat("HH:mm:ss", Locale.US)
    f.timeZone = TimeZone.getTimeZone("GMT+3")
    return f.format(Date())
}

fun axisTimes(): List<String> {
    val tz = TimeZone.getTimeZone("GMT+3")
    val f = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = tz }
    val now = Calendar.getInstance(tz)
    return listOf(-37, -21, -5, 11).map { off ->
        val c = now.clone() as Calendar
        c.add(Calendar.MINUTE, off)
        f.format(c.time)
    }
}

fun fmt(p: Float): String = String.format(Locale.US, "%.5f", p)

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

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            PairHeader()
            Spacer(Modifier.height(6.dp))
            LiveCandlestickChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            )
            Spacer(Modifier.height(4.dp))
            SentimentBar()
            Spacer(Modifier.height(14.dp))
            TradeInputs()
            Spacer(Modifier.height(12.dp))
            TradeButtons()
            Spacer(Modifier.height(8.dp))
            TimeAxisRow()
            Spacer(Modifier.height(6.dp))
        }

        BottomNavBar()
    }
}

// ============================================================
// الشريط العلوي
// ============================================================
@Composable
fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(C.BG)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // الأفاتار
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF6A4CC7))
                .border(2.dp, Color(0xFF9B7FE0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AvatarIcon(Modifier.size(28.dp))
        }

        Spacer(Modifier.width(10.dp))

        // أيقونة الهدية مع علامة X
        Box {
            GiftIcon(Modifier.size(36.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10182B))
                    .border(1.dp, Color(0xFF3A4A5F), CircleShape)
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val m = size.minDimension * 0.22f
                    val cx = size.width / 2
                    val cy = size.height / 2
                    drawLine(
                        Color.White, Offset(cx - m, cy - m), Offset(cx + m, cy + m),
                        strokeWidth = 2.5f, cap = StrokeCap.Round
                    )
                    drawLine(
                        Color.White, Offset(cx + m, cy - m), Offset(cx - m, cy + m),
                        strokeWidth = 2.5f, cap = StrokeCap.Round
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // بطاقة الرصيد
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(C.Surface)
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

        Spacer(Modifier.width(8.dp))

        // زر المحفظة
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(C.Green),
            contentAlignment = Alignment.Center
        ) {
            WalletIcon(Modifier.size(26.dp))
        }
    }
}

// ============================================================
// رأس الزوج
// ============================================================
@Composable
fun PairHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(C.Surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // الأعلام المتداخلة
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(22.dp)
            ) {
                FlagAE(Modifier.size(22.dp).align(Alignment.CenterStart))
                FlagCN(
                    Modifier
                        .size(22.dp)
                        .align(Alignment.CenterEnd)
                        .border(1.5.dp, C.Surface, CircleShape)
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

        // زر النقاط الثلاث (مربع مستدير مثل الصورة)
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

// ============================================================
// الشارت الحي (شموع متحركة + عداد + خط السعر)
// ============================================================
@Composable
fun LiveCandlestickChart(modifier: Modifier = Modifier) {
    val candles = remember { mutableStateListOf<Candle>().also { it.addAll(seedCandles()) } }
    var secondsLeft by remember { mutableStateOf(25) }
    var clockText by remember { mutableStateOf(clockNow()) }

    // تحديث سعر الشمعة الحالية كل ربع ثانية تقريباً
    LaunchedEffect(Unit) {
        while (true) {
            delay(260)
            val i = candles.lastIndex
            val c = candles[i]
            val drift = (Random.nextFloat() - 0.47f) * 0.0010f
            val nc = c.close + drift
            candles[i] = c.copy(
                close = nc,
                high = maxOf(c.high, nc),
                low = minOf(c.low, nc)
            )
        }
    }

    // العداد: كل 60 ثانية تبدأ شمعة جديدة (M1)
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            clockText = clockNow()
            if (secondsLeft > 1) {
                secondsLeft--
            } else {
                secondsLeft = 60
                val last = candles.last()
                if (candles.size >= 55) candles.removeAt(0)
                candles.add(Candle(last.close, last.close, last.close, last.close))
            }
        }
    }

    val lastClose = candles.last().close

    // فرش النصوص داخل Canvas
    val density = LocalDensity.current
    val axisPx = with(density) { 10.5.sp.toPx() }
    val tagPx = with(density) { 11.5.sp.toPx() }
    val axisPaint = remember(axisPx) {
        NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = C.TextSecondary.toArgb()
            textSize = axisPx
            textAlign = NativePaint.Align.RIGHT
        }
    }
    val tagPaint = remember(tagPx) {
        NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = tagPx
            typeface = Typeface.DEFAULT_BOLD
            textAlign = NativePaint.Align.CENTER
        }
    }
    val smallPaint = remember(axisPx) {
        NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = C.TextSecondary.toArgb()
            textSize = axisPx
            textAlign = NativePaint.Align.RIGHT
        }
    }
    val pillPaint = remember(axisPx) {
        NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = C.TextSecondary.toArgb()
            textSize = axisPx
            textAlign = NativePaint.Align.CENTER
        }
    }

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val labelW = 64.dp.toPx()
            val chartW = size.width - labelW
            val top = 14.dp.toPx()
            val bottom = size.height - 8.dp.toPx()
            val h = bottom - top

            // نطاق السعر تلقائي من الشموع الظاهرة
            var lo = Float.MAX_VALUE
            var hi = Float.MIN_VALUE
            candles.forEach {
                lo = minOf(lo, it.low)
                hi = maxOf(hi, it.high)
            }
            val pad = (hi - lo) * 0.10f
            lo -= pad
            hi += pad
            if (hi <= lo) hi = lo + 0.01f

            fun y(p: Float) = bottom - (p - lo) / (hi - lo) * h

            // خلفية متدرجة
            drawRect(
                Brush.verticalGradient(
                    listOf(Color(0xFF111A28), Color(0xFF0E1622), C.BG)
                )
            )

            // جبال خلفية خفيفة (مرسومة بالكود)
            val mBack = Path().apply {
                moveTo(0f, bottom * 0.62f)
                lineTo(chartW * 0.16f, bottom * 0.30f)
                lineTo(chartW * 0.33f, bottom * 0.56f)
                lineTo(chartW * 0.50f, bottom * 0.22f)
                lineTo(chartW * 0.68f, bottom * 0.52f)
                lineTo(chartW * 0.85f, bottom * 0.28f)
                lineTo(chartW, bottom * 0.46f)
                lineTo(chartW, bottom)
                lineTo(0f, bottom)
                close()
            }
            drawPath(mBack, Color(0xFF1B2635).copy(alpha = 0.45f))

            val mFront = Path().apply {
                moveTo(0f, bottom * 0.82f)
                lineTo(chartW * 0.22f, bottom * 0.55f)
                lineTo(chartW * 0.40f, bottom * 0.78f)
                lineTo(chartW * 0.60f, bottom * 0.50f)
                lineTo(chartW * 0.80f, bottom * 0.75f)
                lineTo(chartW, bottom * 0.62f)
                lineTo(chartW, bottom)
                lineTo(0f, bottom)
                close()
            }
            drawPath(mFront, Color(0xFF151F2D).copy(alpha = 0.60f))

            // الشبكة الأفقية + أسعار المحور Y يمين
            val step = 0.005f
            var g = ceil(lo / step) * step
            while (g <= hi) {
                val gy = y(g)
                drawLine(C.Grid, Offset(0f, gy), Offset(size.width, gy), 1f)
                drawContext.canvas.nativeCanvas.drawText(
                    fmt(g),
                    size.width - 5.dp.toPx(),
                    gy + axisPx * 0.35f,
                    axisPaint
                )
                g += step
            }

            // خطوط عمودية خفيفة
            for (i in 0..4) {
                val gx = i * chartW / 4f
                drawLine(
                    C.Grid.copy(alpha = 0.5f),
                    Offset(gx, top), Offset(gx, bottom), 1f
                )
            }

            // الشموع (الأحدث عند حافة اليمين)
            val n = candles.size
            val cw = chartW / 55f
            candles.forEachIndexed { i, c ->
                val x = chartW - (n - 1 - i) * cw - cw * 0.5f
                if (x < -cw) return@forEachIndexed
                val up = c.close >= c.open
                val col = if (up) C.Green else C.Red
                drawLine(
                    col,
                    Offset(x, y(c.high)),
                    Offset(x, y(c.low)),
                    strokeWidth = 1.3.dp.toPx()
                )
                val bt = y(maxOf(c.open, c.close))
                val bb = y(minOf(c.open, c.close))
                drawRoundRect(
                    col,
                    topLeft = Offset(x - cw * 0.32f, bt),
                    size = Size(cw * 0.64f, (bb - bt).coerceAtLeast(2.dp.toPx())),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }

            // علامة أقل سعر (مثل 1.86344 في الصورة)
            val minIdx = candles.indices.minByOrNull { candles[it].low } ?: 0
            val mc = candles[minIdx]
            val mx = chartW - (n - 1 - minIdx) * cw - cw * 0.5f
            val pillW = 58.dp.toPx()
            val pillH = 18.dp.toPx()
            val px = (mx - pillW / 2).coerceIn(2f, chartW - pillW)
            val py = (y(mc.low) + 8.dp.toPx()).coerceAtMost(bottom - pillH)
            drawRoundRect(
                Color(0xFF22303F),
                topLeft = Offset(px, py),
                size = Size(pillW, pillH),
                cornerRadius = CornerRadius(pillH / 2)
            )
            drawContext.canvas.nativeCanvas.drawText(
                fmt(mc.low),
                px + pillW / 2,
                py + pillH / 2 + axisPx * 0.35f,
                pillPaint
            )

            // خط السعر الحالي المتقطع
            val lc = candles.last().close
            val yc = y(lc.coerceIn(lo, hi))
            val dash = 8.dp.toPx()
            val gap = 6.dp.toPx()
            var dx = 0f
            while (dx < chartW) {
                drawLine(
                    C.PriceTag.copy(alpha = 0.8f),
                    Offset(dx, yc),
                    Offset(minOf(dx + dash, chartW), yc),
                    strokeWidth = 1.5.dp.toPx()
                )
                dx += dash + gap
            }

            // بطاقة السعر الزرقاء على اليمين
            val tagH = 20.dp.toPx()
            val tagW = labelW - 6.dp.toPx()
            val tx = chartW + 3.dp.toPx()
            drawRoundRect(
                C.PriceTag,
                topLeft = Offset(tx, yc - tagH / 2),
                size = Size(tagW, tagH),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            drawContext.canvas.nativeCanvas.drawText(
                fmt(lc),
                tx + tagW / 2,
                yc + tagPx * 0.35f,
                tagPaint
            )

            // M1 فوق البطاقة + العداد تحت الخط
            drawContext.canvas.nativeCanvas.drawText(
                "M1",
                chartW - 6.dp.toPx(),
                yc - 10.dp.toPx(),
                smallPaint
            )
            drawContext.canvas.nativeCanvas.drawText(
                String.format(Locale.US, "00:%02d", secondsLeft),
                chartW - 6.dp.toPx(),
                yc + 22.dp.toPx(),
                smallPaint
            )
        }

        // السعر الحالي + الوقت أعلى اليسار (مثل الصورة)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 4.dp)
        ) {
            Text(fmt(lastClose), color = C.TextSecondary, fontSize = 11.sp)
            Text("$clockText UTC+3", color = C.TextSecondary, fontSize = 11.sp)
        }

        // أيقونة البرق
        LightningIcon(
            Modifier
                .align(Alignment.TopStart)
                .padding(start = 34.dp, top = 60.dp)
                .size(16.dp)
        )
    }
}

// ============================================================
// شريط نسبة الشراء/البيع (متحرك)
// ============================================================
@Composable
fun SentimentBar() {
    var buy by remember { mutableStateOf(33) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2200)
            buy = Random.nextInt(30, 37)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$buy%", color = C.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
        ) {
            val w = maxWidth
            Row(
                Modifier
                    .fillMaxSize()
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
            // العلامة البيضاء عند الحد بين النسبتين
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = w * buy / 100f - 1.dp)
                    .width(2.5.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFB0BEC5))
            )
        }

        Spacer(Modifier.width(8.dp))
        Text("${100 - buy}%", color = C.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ============================================================
// صناديق الوقت والمبلغ
// ============================================================
@Composable
fun TradeInputs() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // الزمن (يسار مثل الصورة)
        Column(Modifier.weight(1f)) {
            Text("الزمن", color = C.TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.Surface)
                    .border(1.dp, C.Divider, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "00:01:00",
                    color = C.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                ClockIcon(Modifier.size(18.dp))
            }
        }

        // المبلغ (يمين مثل الصورة)
        Column(Modifier.weight(1f)) {
            Text("المبلغ", color = C.TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.Surface)
                    .border(1.dp, C.Divider, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "1",
                    color = C.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                DollarIcon(Modifier.size(18.dp))
            }
        }
    }
}

// ============================================================
// أزرار الشراء / AI / البيع
// ============================================================
@Composable
fun TradeButtons() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // شراء (يسار)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(62.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(C.Green)
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("شراء", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("92%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // AI (وسط) — مربع أزرق متوهج بداخله دائرة بيضاء
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF5AC8FA), Color(0xFF1E88E5))
                    )
                )
                .border(2.dp, Color(0xFF8AD4FF), RoundedCornerShape(18.dp))
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "AI",
                    color = Color(0xFF1E88E5),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // بيع (يمين)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(62.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(C.Red)
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("بيع", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("92%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ============================================================
// شريط الأوقات أسفل الأزرار (يتحدث كل ثانية)
// ============================================================
@Composable
fun TimeAxisRow() {
    var key by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            key++
        }
    }
    val times = remember(key) { axisTimes() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        times.forEach {
            Text(it, color = C.TextSecondary, fontSize = 11.sp)
        }
    }
}

// ============================================================
// الشريط السفلي
// ============================================================
@Composable
fun BottomNavBar() {
    var selected by remember { mutableStateOf(2) }

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
            BottomItem("التداولات", selected == 0, { selected = 0 }) { c ->
                TradesIcon(Modifier.size(24.dp), c)
            }
            BottomItem("الرؤى", selected == 1, { selected = 1 }) { c ->
                SignalIcon(Modifier.size(24.dp), c)
            }
            BottomItem("Pro mode", selected == 2, { selected = 2 }, highlight = true) { c ->
                ProIcon(Modifier.size(28.dp), c)
            }
            BottomItem("أول الاجتماعي", selected == 3, { selected = 3 }) { c ->
                SocialIcon(Modifier.size(24.dp), c)
            }
            BottomItem("المزيد", selected == 4, { selected = 4 }) { c ->
                DotsIcon(Modifier.size(22.dp), c)
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
        highlight && selected -> Color(0xFF7FB3E8)
        selected -> C.Blue
        else -> C.TextSecondary
    }
    val labelColor = when {
        highlight && selected -> Color.White
        selected -> C.Blue
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
                    .background(if (selected) Color(0xFF13293F) else Color.Transparent)
                    .border(
                        1.5.dp,
                        if (selected) C.Blue else C.Divider,
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon(iconColor)
            }
        } else {
            Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
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
// الأيقونات — كلها مرسومة بالكود
// ============================================================

@Composable
fun AvatarIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        drawCircle(
            color, size.minDimension * 0.28f,
            Offset(size.width / 2, size.height * 0.36f)
        )
        drawArc(
            color = color,
            startAngle = 180f, sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(size.width * 0.15f, size.height * 0.55f),
            size = Size(size.width * 0.7f, size.height * 0.55f)
        )
    }
}

@Composable
fun GiftIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val blue = Color(0xFF2E9BDE)
        val dark = Color(0xFF1E6FA8)
        val ribbon = Color(0xFFBFE6FF)
        // الصندوق
        drawRoundRect(
            blue,
            topLeft = Offset(w * 0.12f, h * 0.34f),
            size = Size(w * 0.76f, h * 0.54f),
            cornerRadius = CornerRadius(w * 0.08f)
        )
        // الغطاء
        drawRoundRect(
            dark,
            topLeft = Offset(w * 0.08f, h * 0.22f),
            size = Size(w * 0.84f, h * 0.18f),
            cornerRadius = CornerRadius(w * 0.06f)
        )
        // الشريط
        drawRect(ribbon, topLeft = Offset(w * 0.44f, h * 0.22f), size = Size(w * 0.12f, h * 0.66f))
        // الفيونكة
        drawCircle(ribbon, w * 0.11f, Offset(w * 0.38f, h * 0.15f))
        drawCircle(ribbon, w * 0.11f, Offset(w * 0.62f, h * 0.15f))
    }
}

@Composable
fun LightningIcon(modifier: Modifier = Modifier, color: Color = C.TextSecondary) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.58f, 0f)
            lineTo(w * 0.15f, h * 0.55f)
            lineTo(w * 0.42f, h * 0.55f)
            lineTo(w * 0.35f, h)
            lineTo(w * 0.85f, h * 0.40f)
            lineTo(w * 0.55f, h * 0.40f)
            close()
        }
        drawPath(p, color)
    }
}

@Composable
fun DownArrow(modifier: Modifier = Modifier, color: Color = C.TextSecondary) {
    Canvas(modifier) {
        val p = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2, size.height)
            close()
        }
        drawPath(p, color)
    }
}

@Composable
fun DotsIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val r = size.minDimension * 0.11f
        val cy = size.height / 2
        drawCircle(color, r, Offset(size.width * 0.15f, cy))
        drawCircle(color, r, Offset(size.width * 0.5f, cy))
        drawCircle(color, r, Offset(size.width * 0.85f, cy))
    }
}

@Composable
fun WalletIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color,
            topLeft = Offset(w * 0.05f, h * 0.22f),
            size = Size(w * 0.9f, h * 0.62f),
            cornerRadius = CornerRadius(w * 0.1f),
            style = Stroke(width = w * 0.08f)
        )
        drawRoundRect(
            color,
            topLeft = Offset(w * 0.55f, h * 0.4f),
            size = Size(w * 0.4f, h * 0.28f),
            cornerRadius = CornerRadius(w * 0.06f),
            style = Stroke(width = w * 0.07f)
        )
        drawCircle(color, w * 0.05f, Offset(w * 0.73f, h * 0.54f))
    }
}

@Composable
fun ClockIcon(modifier: Modifier = Modifier, color: Color = C.TextSecondary) {
    Canvas(modifier) {
        val r = size.minDimension * 0.4f
        drawCircle(
            color, r, Offset(size.width / 2, size.height / 2),
            style = Stroke(width = size.minDimension * 0.09f)
        )
        drawLine(
            color, Offset(size.width / 2, size.height / 2),
            Offset(size.width / 2, size.height * 0.25f),
            strokeWidth = size.minDimension * 0.09f, cap = StrokeCap.Round
        )
        drawLine(
            color, Offset(size.width / 2, size.height / 2),
            Offset(size.width * 0.72f, size.height / 2),
            strokeWidth = size.minDimension * 0.09f, cap = StrokeCap.Round
        )
    }
}

@Composable
fun DollarIcon(modifier: Modifier = Modifier, color: Color = C.TextSecondary) {
    Canvas(modifier) {
        val r = size.minDimension * 0.4f
        val sw = size.minDimension * 0.09f
        drawCircle(
            color, r, Offset(size.width / 2, size.height / 2),
            style = Stroke(width = sw)
        )
        val cx = size.width / 2
        val cy = size.height / 2
        val s = size.minDimension * 0.16f
        drawLine(color, Offset(cx + s * 0.6f, cy - s), Offset(cx - s * 0.5f, cy - s * 0.2f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(cx - s * 0.5f, cy - s * 0.2f), Offset(cx + s * 0.5f, cy + s * 0.2f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(cx + s * 0.5f, cy + s * 0.2f), Offset(cx - s * 0.6f, cy + s), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(cx, cy - s * 1.4f), Offset(cx, cy - s), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(cx, cy + s), Offset(cx, cy + s * 1.4f), sw, cap = StrokeCap.Round)
    }
}

// علم الإمارات (صحيح: أحمر عمودي + خطوط أفقية خضراء/بيضاء/سوداء)
@Composable
fun FlagAE(modifier: Modifier = Modifier) {
    Canvas(modifier.clip(CircleShape)) {
        val w = size.width
        val h = size.height
        drawRect(Color(0xFF00732F), size = Size(w, h / 3f))
        drawRect(Color.White, topLeft = Offset(0f, h / 3f), size = Size(w, h / 3f))
        drawRect(Color(0xFF151515), topLeft = Offset(0f, 2 * h / 3f), size = Size(w, h / 3f))
        drawRect(Color(0xFFFF0000), size = Size(w * 0.28f, h))
    }
}

// علم الصين
@Composable
fun FlagCN(modifier: Modifier = Modifier) {
    Canvas(modifier.clip(CircleShape)) {
        drawRect(Color(0xFFDE2910))
        drawCircle(
            Color(0xFFFFDE00),
            size.minDimension * 0.16f,
            Offset(size.width * 0.32f, size.height * 0.34f)
        )
    }
}

// ===== أيقونات الشريط السفلي =====

@Composable
fun TradesIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.09f
        // سهم للأعلى
        drawLine(color, Offset(w * 0.3f, h * 0.85f), Offset(w * 0.3f, h * 0.15f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.3f, h * 0.15f), Offset(w * 0.15f, h * 0.32f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.3f, h * 0.15f), Offset(w * 0.45f, h * 0.32f), sw, cap = StrokeCap.Round)
        // سهم للأسفل
        drawLine(color, Offset(w * 0.7f, h * 0.15f), Offset(w * 0.7f, h * 0.85f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.7f, h * 0.85f), Offset(w * 0.55f, h * 0.68f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.7f, h * 0.85f), Offset(w * 0.85f, h * 0.68f), sw, cap = StrokeCap.Round)
    }
}

@Composable
fun SignalIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val cx = size.width / 2
        val cy = size.height * 0.75f
        val sw = size.minDimension * 0.09f
        drawArc(
            color, 180f, 180f, false,
            topLeft = Offset(cx - size.width * 0.42f, cy - size.height * 0.42f),
            size = Size(size.width * 0.84f, size.height * 0.84f),
            style = Stroke(width = sw)
        )
        drawArc(
            color, 180f, 180f, false,
            topLeft = Offset(cx - size.width * 0.26f, cy - size.height * 0.26f),
            size = Size(size.width * 0.52f, size.height * 0.52f),
            style = Stroke(width = sw)
        )
        drawCircle(color, size.minDimension * 0.1f, Offset(cx, cy - size.height * 0.02f))
    }
}

@Composable
fun ProIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color,
            topLeft = Offset(w * 0.08f, h * 0.12f),
            size = Size(w * 0.84f, h * 0.76f),
            cornerRadius = CornerRadius(w * 0.16f),
            style = Stroke(width = w * 0.08f)
        )
        val p = Path().apply {
            moveTo(w * 0.24f, h * 0.62f)
            lineTo(w * 0.40f, h * 0.44f)
            lineTo(w * 0.54f, h * 0.56f)
            lineTo(w * 0.76f, h * 0.34f)
        }
        drawPath(p, color, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
    }
}

@Composable
fun SocialIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        drawCircle(color, size.minDimension * 0.17f, Offset(cx, cy - size.height * 0.16f))
        drawArc(
            color, 180f, 180f, false,
            topLeft = Offset(cx - size.width * 0.28f, cy + size.height * 0.04f),
            size = Size(size.width * 0.56f, size.height * 0.5f),
            style = Stroke(width = size.minDimension * 0.08f)
        )
        drawCircle(color, size.minDimension * 0.09f, Offset(cx - size.width * 0.36f, cy - size.height * 0.14f))
        drawCircle(color, size.minDimension * 0.09f, Offset(cx + size.width * 0.36f, cy - size.height * 0.14f))
    }
}
