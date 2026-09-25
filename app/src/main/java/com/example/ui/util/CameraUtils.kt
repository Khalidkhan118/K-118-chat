package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility functions for camera captures and image handling in K118 chat.
 */
object CameraUtils {

    /**
     * Creates a destination File and content Uri using FileProvider for the camera intent.
     */
    fun createImageUri(context: Context): Pair<File, Uri> {
        val storageDir = File(context.filesDir, "chat_camera_photos").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val photoFile = File(storageDir, "IMG_$timeStamp.jpg")
        val authority = "${context.packageName}.fileprovider"
        val photoUri = FileProvider.getUriForFile(context, authority, photoFile)
        return Pair(photoFile, photoUri)
    }

    /**
     * Generates a sample camera photo bitmap and saves it to storage.
     * Useful for automated testing and fallback if device/emulator lacks a hardware camera.
     */
    fun createSampleCameraPhoto(context: Context): Pair<File, Uri> {
        val (file, uri) = createImageUri(context)
        val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply {
            color = Color.rgb(15, 23, 42) // Slate 900
        }
        canvas.drawRect(0f, 0f, 800f, 600f, bgPaint)

        // Frame border
        val framePaint = Paint().apply {
            color = Color.rgb(59, 130, 246) // Blue 500
            strokeWidth = 6f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(40f, 40f, 760f, 560f, 32f, 32f, framePaint)

        // Camera lens graphic
        val bodyPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(220f, 150f, 580f, 410f, 24f, 24f, bodyPaint)

        val lensOuterPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(400f, 280f, 90f, lensOuterPaint)

        val lensInnerPaint = Paint().apply {
            color = Color.rgb(37, 99, 235)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(400f, 280f, 70f, lensInnerPaint)

        val reflectionPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
            alpha = 200
        }
        canvas.drawCircle(425f, 255f, 18f, reflectionPaint)

        // Title and timestamp
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 34f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("K118 Camera Capture", 400f, 480f, textPaint)

        val subTextPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 22f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val timeString = SimpleDateFormat("MMM dd, yyyy  HH:mm:ss", Locale.getDefault()).format(Date())
        canvas.drawText(timeString, 400f, 520f, subTextPaint)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return Pair(file, uri)
    }
}
