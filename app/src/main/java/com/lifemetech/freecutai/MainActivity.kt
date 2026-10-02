package com.lifemetech.freecutai

import android.app.Activity
import android.graphics.*
import android.media.MediaRecorder
import android.os.Bundle
import android.view.Surface
import android.widget.*
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : Activity() {
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var generate: Button
    private var lastVideo: File? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 30, 28, 24)
            setBackgroundColor(Color.rgb(7, 9, 20))
        }
        val title = TextView(this).apply {
            text = "✦ FreeCut AI"
            textSize = 30f
            setTextColor(Color.WHITE)
        }
        val sub = TextView(this).apply {
            text = "Fast local MP4 video generator"
            setTextColor(Color.LTGRAY)
        }
        input = EditText(this).apply {
            hint = "Example: 5 AI tools students should know"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            minLines = 5
            gravity = android.view.Gravity.TOP
        }
        generate = Button(this).apply {
            text = "⚡ GENERATE MP4"
            setOnClickListener { generateVideo() }
        }
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            visibility = android.view.View.GONE
        }
        status = TextView(this).apply {
            text = "Ready — works offline"
            setTextColor(Color.LTGRAY)
            setPadding(0, 12, 0, 10)
        }
        val open = Button(this).apply {
            text = "▶ OPEN LAST VIDEO"
            setOnClickListener {
                lastVideo?.let { f ->
                    status.text = "Saved: \${f.absolutePath}"
                } ?: run { status.text = "Generate a video first." }
            }
        }
        root.addView(title)
        root.addView(sub)
        root.addView(input, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(generate)
        root.addView(progress, LinearLayout.LayoutParams(-1, 10))
        root.addView(status)
        root.addView(open)
        setContentView(root)
    }

    private fun generateVideo() {
        val text = input.text.toString().trim().ifEmpty { "A quick guide to artificial intelligence" }
        generate.isEnabled = false
        progress.visibility = android.view.View.VISIBLE
        progress.progress = 0
        status.text = "Rendering real MP4…"

        Thread {
            try {
                val dir = File(getExternalFilesDir(null), "videos").apply { mkdirs() }
                val file = File(dir, "freecut_\${System.currentTimeMillis()}.mp4")
                renderMp4(text, file)
                lastVideo = file
                runOnUiThread {
                    progress.progress = 100
                    status.text = "✓ MP4 ready • \${file.length() / 1024} KB"
                    generate.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Render error: \${e.message ?: "unknown"}"
                    generate.isEnabled = true
                }
            }
        }.start()
    }

    private fun renderMp4(text: String, output: File) {
        val width = 720
        val height = 1280
        val fps = 30
        val seconds = 15
        val total = fps * seconds

        val recorder = MediaRecorder()
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        recorder.setVideoSize(width, height)
        recorder.setVideoFrameRate(fps)
        recorder.setVideoEncodingBitRate(4_000_000)
        recorder.setOutputFile(output.absolutePath)
        recorder.prepare()

        val surface: Surface = recorder.surface
        recorder.start()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create("sans", Typeface.BOLD)

        for (frame in 0 until total) {
            val t = frame.toFloat() / fps
            canvas.drawColor(Color.rgb(8, 10, 24))

            val cx = width * (0.5f + 0.18f * sin(t * 1.1f))
            val cy = height * (0.34f + 0.08f * cos(t * 1.4f))
            paint.shader = RadialGradient(
                cx, cy, width * 0.65f,
                Color.rgb(110, 70, 255), Color.rgb(8, 10, 24),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.shader = null

            paint.color = Color.WHITE
            paint.textSize = 42f
            canvas.drawText("FREECUT AI", width / 2f, height * 0.20f, paint)

            paint.textSize = 48f
            val lines = wrap(text, 22)
            var y = height * 0.47f
            for (line in lines.take(6)) {
                canvas.drawText(line, width / 2f, y, paint)
                y += 68f
            }

            paint.color = Color.LTGRAY
            paint.textSize = 25f
            canvas.drawText("Fast local video • \${frame + 1}/\$total", width / 2f, height * 0.90f, paint)

            val drawCanvas = surface.lockCanvas(null)
            try {
                drawCanvas.drawBitmap(bitmap, null, Rect(0, 0, width, height), paint)
            } finally {
                surface.unlockCanvasAndPost(drawCanvas)
            }

            runOnUiThread { progress.progress = (frame + 1) * 100 / total }
        }

        surface.release()
        recorder.stop()
        recorder.reset()
        recorder.release()
        bitmap.recycle()
    }

    private fun wrap(text: String, max: Int): List<String> {
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var line = ""
        for (word in words) {
            val next = if (line.isEmpty()) word else "\$line \$word"
            if (next.length > max && line.isNotEmpty()) {
                lines.add(line)
                line = word
            } else line = next
        }
        if (line.isNotEmpty()) lines.add(line)
        return lines
    }
}
