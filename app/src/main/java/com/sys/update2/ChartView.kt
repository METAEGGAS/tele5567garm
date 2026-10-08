package com.sys.update2

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * ChartView — رسم بياني بسيط (Sparkline)
 * يعرض آخر N نقطة سعرية
 */
class ChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val maxPoints = 60
    private val points = ArrayDeque<Double>()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3D8BFF")
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#30363D")
        strokeWidth = 1f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B949E")
        textSize = 24f
    }

    private var label: String = ""

    fun setData(asset: String, data: List<Double>) {
        label = asset
        points.clear()
        data.takeLast(maxPoints).forEach { points.add(it) }
        invalidate()
    }

    fun pushPoint(price: Double) {
        points.add(price)
        while (points.size > maxPoints) points.removeFirst()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        // خلفية
        canvas.drawColor(Color.parseColor("#0D1117"))

        if (points.size < 2) {
            canvas.drawText(
                "في انتظار البيانات...",
                w / 2 - 100,
                h / 2,
                textPaint
            )
            return
        }

        // حدود
        var minV = Double.MAX_VALUE
        var maxV = -Double.MAX_VALUE
        for (p in points) {
            minV = min(minV, p)
            maxV = max(maxV, p)
        }
        val range = (maxV - minV).coerceAtLeast(0.0001)
        val padding = range * 0.15
        minV -= padding
        maxV += padding

        // Grid
        for (i in 0..4) {
            val y = h * i / 4f
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        // Line
        val path = Path()
        val fillPath = Path()
        val stepX = w / (points.size - 1).toFloat()

        var i = 0
        for (p in points) {
            val x = i * stepX
            val y = (h - ((p - minV) / (maxV - minV)) * h).toFloat()
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            i++
        }
        fillPath.lineTo(w, h)
        fillPath.close()

        // Fill with gradient
        fillPaint.shader = android.graphics.LinearGradient(
            0f, 0f, 0f, h,
            Color.parseColor("#403D8BFF"),
            Color.parseColor("#003D8BFF"),
            android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawPath(fillPath, fillPaint)

        canvas.drawPath(path, linePaint)

        // Label
        canvas.drawText("$label", 20f, 30f, textPaint)

        // Current price
        val last = points.last()
        canvas.drawText(
            String.format("%.5f", last),
            w - 200f, 30f, textPaint
        )
    }
}
