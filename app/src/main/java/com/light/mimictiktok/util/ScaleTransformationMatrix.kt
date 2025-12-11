package com.light.mimictiktok.util

import android.graphics.Matrix
import android.graphics.RectF

class ScaleTransformationMatrix {
    private val matrix = Matrix()
    private val minScale = 1.0f
    private val maxScale = 5.0f
    private val viewRect = RectF()

    fun setViewSize(width: Float, height: Float) {
        viewRect.set(0f, 0f, width, height)
    }

    fun onScale(scaleFactor: Float, focusX: Float, focusY: Float): Matrix {
        val currentScale = getCurrentScale()
        var newScale = currentScale * scaleFactor
        
        // Clamp scale
        if (newScale < minScale) newScale = minScale
        if (newScale > maxScale) newScale = maxScale
        
        // If we are at minScale, we might want to ensure we center it back? 
        // But usually scale gesture detector handles the progression.
        // We need to calculate the actual factor to apply based on clamped newScale
        
        val factor = newScale / currentScale
        
        matrix.postScale(factor, factor, focusX, focusY)
        limitTranslation()
        return matrix
    }

    fun onScroll(distanceX: Float, distanceY: Float): Matrix {
        matrix.postTranslate(-distanceX, -distanceY)
        limitTranslation()
        return matrix
    }
    
    fun reset(): Matrix {
        matrix.reset()
        return matrix
    }

    fun setScale(scale: Float, animate: Boolean = false): Matrix {
        matrix.setScale(scale, scale, viewRect.centerX(), viewRect.centerY())
        limitTranslation()
        return matrix
    }
    
    fun getCurrentScale(): Float {
        val values = FloatArray(9)
        matrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }
    
    fun getMatrix(): Matrix {
        return matrix
    }

    private fun limitTranslation() {
        val values = FloatArray(9)
        matrix.getValues(values)
        val scaleX = values[Matrix.MSCALE_X]
        val scaleY = values[Matrix.MSCALE_Y]
        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]

        val width = viewRect.width()
        val height = viewRect.height()
        
        // If width or height is 0, we can't limit translation correctly
        if (width == 0f || height == 0f) return

        val scaledWidth = width * scaleX
        val scaledHeight = height * scaleY

        var newTransX = transX
        var newTransY = transY

        if (scaledWidth <= width) {
            newTransX = (width - scaledWidth) / 2
        } else {
             if (newTransX > 0) newTransX = 0f
             if (newTransX + scaledWidth < width) newTransX = width - scaledWidth
        }
        
        if (scaledHeight <= height) {
            newTransY = (height - scaledHeight) / 2
        } else {
             if (newTransY > 0) newTransY = 0f
             if (newTransY + scaledHeight < height) newTransY = height - scaledHeight
        }
        
        values[Matrix.MTRANS_X] = newTransX
        values[Matrix.MTRANS_Y] = newTransY
        matrix.setValues(values)
    }
}
