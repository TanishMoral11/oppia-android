package org.oppia.android.util.locale

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan

// Custom span to force LTR alignment for symbols as well
// Custom span to force LTR alignment for all text including symbols
class LeftAlignedSymbolsSpan : ReplacementSpan() {
  override fun getSize(
    paint: Paint,
    text: CharSequence?,
    start: Int,
    end: Int,
    fm: Paint.FontMetricsInt?
  ): Int {
    return paint.measureText(text, start, end).toInt()
  }

  override fun draw(
    canvas: Canvas,
    text: CharSequence,
    start: Int,
    end: Int,
    x: Float,
    top: Int,
    y: Int,
    bottom: Int,
    paint: Paint
  ) {
    val originalAlignment = paint.textAlign
    paint.textAlign = Paint.Align.LEFT

    // Draw the bullet point at the exact x position
    canvas.drawText(text, start, end, x, y.toFloat(), paint)

    // Restore original alignment
    paint.textAlign = originalAlignment
  }
}