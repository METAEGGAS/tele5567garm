package com.example.tradingapp

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

// ============================================================
// الألوان (يمكن تعديلها بسهولة)
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
    val Grid = Color(0xFF1F2A38)
}

// ============================================================
// الشاشة الرئيسية الكاملة
// ============================================================
@Composable
fun TradingHomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(C.BG)
    ) {
        // ===== الشريط العلوي =====
        TopBar()

        // ===== المحتوى (قابل للتمرير) =====
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            PairHeader()
            Spacer(Modifier.height(6.dp))
            CandlestickChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            )
            Spacer(Modifier.height(8.dp))
            SentimentBar()
            Spacer(Modifier.height(12.dp))
            TradeInputs()
            Spacer(Modifier.height(12.dp))
            TradeButtons()
            Spacer(Modifier.height(12.dp))
        }

        // ===== الشريط السفلي =====
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
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF6A4CC7))
                .border(2.dp, Color(0xFF9B7FE0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AvatarIcon(Modifier.size(26.dp))
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
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(C.Green),
            contentAlignment = Alignment.Center
        ) {
            WalletIcon(Modifier.size(24.dp))
        }
    }
}

// ============================================================
// رأس الزوج (العملة)
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
                .clip(RoundedCornerShape(20.dp))
                .background(C.Surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlagAE()
            Spacer(Modifier.width(4.dp))
            FlagCN()
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
                .size(38.dp)
                .clip(CircleShape)
                .background(C.Surface),
            contentAlignment = Alignment.Center
        ) {
            DotsIcon(Modifier.size(16.dp))
        }
    }
}

// ============================================================
// الشارت (شموع مرسومة بالكود)
// ============================================================
@Composable
fun CandlestickChart(modifier: Modifier = Modifier) {
    // بيانات وهمية للشموع (open, close, high, low)
    val candles = remember {
        listOf(
            1.878f to 1.876f, 1.876f to 1.877f, 1.877f to 1.874f,
            1.874f to 1.873f, 1.873f to 1.875f, 1.875f to 1.872f,
            1.872f to 1.870f, 1.870f to 1.868f, 1.868f to 1.866f,
            1.866f to 1.865f, 1.865f to 1.863f, 1.863f to 1.866f,
            1.866f to 1.868f, 1.868f to 1.867f, 1.867f to 1.869f,
            1.869f to 1.871f, 1.871f to 1.870f, 1.870f to 1.872f,
            1.872f to 1.869f, 1.869f to 1.870f, 1.870f to 1.868f,
            1.868f to 1.870f, 1.870f to 1.871f, 1.871f to 1.869f,
            1.869f to 1.872f, 1.872f to 1.870f, 1.870f to 1.871f,
            1.871f to 1.873f, 1.873f to 1.872f, 1.872f to 1.870f
        )
    }

    val priceLine = 1.86978f
    val minPrice = 1.862f
    val maxPrice = 1.881f

    Column(modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val chartTop = 20f
                val chartBottom = size.height - 20f
                val chartHeight = chartBottom - chartTop
                val priceRange = maxPrice - minPrice

                fun priceToY(p: Float): Float {
                    return chartBottom - ((p - minPrice) / priceRange) * chartHeight
                }

                // خطوط الشبكة الأفقية
                val gridPrices = listOf(1.88f, 1.875f, 1.87f, 1.865f)
                gridPrices.forEach { price ->
                    val y = priceToY(price)
                    drawLine(
                        color = C.Grid,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }

                // الشموع
                val candleWidth = size.width / candles.size
                candles.forEachIndexed { i, (open, close) ->
                    val x = i * candleWidth + candleWidth / 2
                    val isGreen = close > open
                    val color = if (isGreen) C.Green else C.Red

                    val high = maxOf(open, close) + 0.0015f
                    val low = minOf(open, close) - 0.0015f

                    val yHigh = priceToY(high)
                    val yLow = priceToY(low)
                    val yOpen = priceToY(open)
                    val yClose = priceToY(close)

                    // ظل الشمعة
                    drawLine(
                        color = color,
                        start = Offset(x, yHigh),
                        end = Offset(x, yLow),
                        strokeWidth = 2f
                    )

                    // جسم الشمعة
                    val top = minOf(yOpen, yClose)
                    val bottom = maxOf(yOpen, yClose)
                    val bodyHeight = (bottom - top).coerceAtLeast(3f)

                    drawRect(
                        color = color,
                        topLeft = Offset(x - candleWidth * 0.3f, top),
                        size = Size(candleWidth * 0.6f, bodyHeight)
                    )
                }

                // خط السعر الحالي
                val yLine = priceToY(priceLine)
                val dashPath = Path().apply {
                    val dashWidth = 15f
                    var startX = 0f
                    while (startX < size.width) {
                        moveTo(startX, yLine)
                        lineTo((startX + dashWidth).coerceAtMost(size.width), yLine)
                        startX += dashWidth * 2
                    }
                }
                drawPath(
                    dashPath,
                    color = Color(0xFF5DADE2),
                    style = Stroke(width = 2f)
                )

                // علامة السعر الحالي على اليمين
                drawRoundRect(
                    color = Color(0xFF5DADE2),
                    topLeft = Offset(size.width - 75f, yLine - 10f),
                    size = Size(75f, 20f),
                    cornerRadius = CornerRadius(4f)
                )
            }

            // نص السعر الحالي
            Text(
                "M1",
                color = C.TextSecondary,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 90.dp, top = 55.dp)
            )
            Text(
                "1.86978",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = 57.dp)
            )

            // الوقت أعلى اليسار
            Text(
                "18:25:35 UTC+3",
                color = C.TextSecondary,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 8.dp)
            )
        }
    }
}

// ============================================================
// شريط نسبة الشراء/البيع
// ============================================================
@Composable
fun SentimentBar() {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("33%", color = C.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))

            // الشريط
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(C.Red)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.33f)
                        .background(C.Green)
                )
            }

            Spacer(Modifier.width(8.dp))
            Text("67%", color = C.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // الوقت
        Column(Modifier.weight(1f)) {
            Text("الزمن", color = C.TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.Surface)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
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

        // المبلغ
        Column(Modifier.weight(1f)) {
            Text("المبلغ", color = C.TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.Surface)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // زر الشراء
        Box(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
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

        // زر AI
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(C.Blue)
                .border(3.dp, Color(0xFF5DADE2), CircleShape)
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "AI",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // زر البيع
        Box(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
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
// الشريط السفلي
// ============================================================
@Composable
fun BottomNavBar() {
    var selected by remember { mutableStateOf(2) } // Pro mode محددة

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(C.BG)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomItem(
            icon = { TradesIcon(Modifier.size(24.dp)) },
            label = "التداولات",
            selected = selected == 0,
            onClick = { selected = 0 }
        )
        BottomItem(
            icon = { SignalIcon(Modifier.size(24.dp)) },
            label = "الرؤى",
            selected = selected == 1,
            onClick = { selected = 1 }
        )
        BottomItem(
            icon = { ProIcon(Modifier.size(26.dp)) },
            label = "Pro mode",
            selected = selected == 2,
            onClick = { selected = 2 },
            highlight = true
        )
        BottomItem(
            icon = { SocialIcon(Modifier.size(24.dp)) },
            label = "أول الاجتماعي",
            selected = selected == 3,
            onClick = { selected = 3 }
        )
        BottomItem(
            icon = { DotsIcon(Modifier.size(22.dp)) },
            label = "المزيد",
            selected = selected == 4,
            onClick = { selected = 4 }
        )
    }
}

@Composable
fun BottomItem(
    icon: @Composable () -> Unit,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    highlight: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = if (highlight) {
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Color(0xFF1B3A5C) else Color.Transparent)
                    .border(
                        2.dp,
                        if (selected) C.Blue else C.Divider,
                        RoundedCornerShape(12.dp)
                    )
            } else Modifier,
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        if (!highlight) {
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                color = if (selected) C.Blue else C.TextSecondary,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================================
// الأيقونات — كلها مرسومة بالكود (قابلة للاستبدال لاحقاً)
// ============================================================

@Composable
fun AvatarIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawCircle(Color.White, size.minDimension * 0.28f,
            Offset(size.width / 2, size.height * 0.36f))
        drawArc(
            color = Color.White,
            startAngle = 180f, sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(size.width * 0.15f, size.height * 0.55f),
            size = Size(size.width * 0.7f, size.height * 0.55f)
        )
    }
}

@Composable
fun DownArrow(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val p = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2, size.height)
            close()
        }
        drawPath(p, C.TextSecondary)
    }
}

@Composable
fun DotsIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension * 0.11f
        val cy = size.height / 2
        drawCircle(Color.White, r, Offset(size.width * 0.15f, cy))
        drawCircle(Color.White, r, Offset(size.width * 0.5f, cy))
        drawCircle(Color.White, r, Offset(size.width * 0.85f, cy))
    }
}

@Composable
fun WalletIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawRoundRect(
            Color.White,
            topLeft = Offset(w * 0.05f, h * 0.22f),
            size = Size(w * 0.9f, h * 0.62f),
            cornerRadius = CornerRadius(w * 0.1f),
            style = Stroke(width = w * 0.08f)
        )
        drawRoundRect(
            Color.White,
            topLeft = Offset(w * 0.55f, h * 0.4f),
            size = Size(w * 0.4f, h * 0.28f),
            cornerRadius = CornerRadius(w * 0.06f),
            style = Stroke(width = w * 0.07f)
        )
        drawCircle(Color.White, w * 0.05f, Offset(w * 0.73f, h * 0.54f))
    }
}

@Composable
fun ClockIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension * 0.4f
        drawCircle(Color.White, r, Offset(size.width / 2, size.height / 2),
            style = Stroke(width = size.minDimension * 0.08f))
        drawLine(Color.White, Offset(size.width / 2, size.height / 2),
            Offset(size.width / 2, size.height * 0.25f),
            strokeWidth = size.minDimension * 0.08f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(size.width / 2, size.height / 2),
            Offset(size.width * 0.72f, size.height / 2),
            strokeWidth = size.minDimension * 0.08f, cap = StrokeCap.Round)
    }
}

@Composable
fun DollarIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension * 0.4f
        drawCircle(Color.White, r, Offset(size.width / 2, size.height / 2),
            style = Stroke(width = size.minDimension * 0.08f))
        val cx = size.width / 2
        val cy = size.height / 2
        val s = size.minDimension * 0.18f
        // حرف S مبسط
        drawLine(Color.White, Offset(cx, cy - s),
            Offset(cx + s * 0.8f, cy - s * 0.4f),
            strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(cx + s * 0.8f, cy - s * 0.4f),
            Offset(cx - s * 0.4f, cy + s * 0.3f),
            strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(cx - s * 0.4f, cy + s * 0.3f),
            Offset(cx, cy + s),
            strokeWidth = 3f, cap = StrokeCap.Round)
    }
}

@Composable
fun FlagAE() {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(Color.White)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(color = Color(0xFF00732F), size = Size(size.width / 3, size.height))
            drawRect(
                color = Color.White,
                topLeft = Offset(size.width / 3, 0f),
                size = Size(size.width / 3, size.height)
            )
            drawRect(
                color = Color.Black,
                topLeft = Offset(size.width * 2 / 3, 0f),
                size = Size(size.width / 3, size.height)
            )
            drawRect(color = Color(0xFFFF0000), size = Size(size.width, size.height / 4))
        }
    }
}

@Composable
fun FlagCN() {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(Color(0xFFDE2910))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Color(0xFFFFDE00),
                size.minDimension * 0.15f,
                Offset(size.width * 0.3f, size.height * 0.35f)
            )
        }
    }
}

// ===== أيقونات الشريط السفلي =====

@Composable
fun TradesIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        // سهم لأسفل
        drawLine(Color.White, Offset(w * 0.25f, h * 0.2f),
            Offset(w * 0.25f, h * 0.8f), strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.25f, h * 0.8f),
            Offset(w * 0.1f, h * 0.65f), strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.25f, h * 0.8f),
            Offset(w * 0.4f, h * 0.65f), strokeWidth = 3f, cap = StrokeCap.Round)
        // رقم 1
        drawLine(Color.White, Offset(w * 0.65f, h * 0.2f),
            Offset(w * 0.65f, h * 0.8f), strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.65f, h * 0.2f),
            Offset(w * 0.5f, h * 0.35f), strokeWidth = 3f, cap = StrokeCap.Round)
    }
}

@Composable
fun SignalIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        // دوائر إشارة
        drawArc(Color.White, 180f, 180f, false,
            topLeft = Offset(cx - size.width * 0.35f, cy - size.height * 0.05f),
            size = Size(size.width * 0.7f, size.height * 0.7f),
            style = Stroke(width = 3f))
        drawArc(Color.White, 180f, 180f, false,
            topLeft = Offset(cx - size.width * 0.22f, cy - size.height * 0.05f),
            size = Size(size.width * 0.44f, size.height * 0.44f),
            style = Stroke(width = 3f))
        drawCircle(Color.White, size.minDimension * 0.1f, Offset(cx, cy))
    }
}

@Composable
fun ProIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        // مستطيل بحواف دائرية
        drawRoundRect(
            Color.White,
            topLeft = Offset(w * 0.1f, h * 0.15f),
            size = Size(w * 0.8f, h * 0.7f),
            cornerRadius = CornerRadius(w * 0.15f),
            style = Stroke(width = 3f)
        )
        // خط متموج داخل (شارت)
        val p = Path().apply {
            moveTo(w * 0.25f, h * 0.6f)
            lineTo(w * 0.4f, h * 0.45f)
            lineTo(w * 0.55f, h * 0.55f)
            lineTo(w * 0.75f, h * 0.35f)
        }
        drawPath(p, Color.White, style = Stroke(width = 3f, cap = StrokeCap.Round))
    }
}

@Composable
fun SocialIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        // رأس الشخص
        drawCircle(Color.White, size.minDimension * 0.18f,
            Offset(cx, cy - size.height * 0.18f))
        // الجسم
        drawArc(Color.White, 180f, 180f, false,
            topLeft = Offset(cx - size.width * 0.28f, cy),
            size = Size(size.width * 0.56f, size.height * 0.5f),
            style = Stroke(width = 3f))
        // شخصين على الجانبين
        drawCircle(Color.White, size.minDimension * 0.1f,
            Offset(cx - size.width * 0.35f, cy - size.height * 0.15f))
        drawCircle(Color.White, size.minDimension * 0.1f,
            Offset(cx + size.width * 0.35f, cy - size.height * 0.15f))
    }
}
