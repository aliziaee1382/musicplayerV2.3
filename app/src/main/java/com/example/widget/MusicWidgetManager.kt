package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.model.Track
import com.example.player.AudioPlayerManager
import com.example.player.RepeatMode

object MusicWidgetManager {

    const val ACTION_TOGGLE_PLAY = "com.example.widget.ACTION_TOGGLE_PLAY"
    const val ACTION_PREV = "com.example.widget.ACTION_PREV"
    const val ACTION_NEXT = "com.example.widget.ACTION_NEXT"
    const val ACTION_FAVORITE = "com.example.widget.ACTION_FAVORITE"
    const val ACTION_SHUFFLE = "com.example.widget.ACTION_SHUFFLE"
    const val ACTION_REPEAT = "com.example.widget.ACTION_REPEAT"

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        val player = AudioPlayerManager.activeInstance

        val currentTrack = player?.currentTrack?.value
        val isPlaying = player?.isPlaying?.value ?: false
        val currentPositionMs = player?.currentPositionMs?.value ?: 0
        val durationMs = player?.durationMs?.value ?: 1
        val isShuffle = player?.isShuffle?.value ?: false
        val repeatMode = player?.repeatMode?.value ?: RepeatMode.OFF

        updateCompactWidget(context, appWidgetManager, currentTrack, isPlaying)
        updateStandardWidget(context, appWidgetManager, currentTrack, isPlaying, currentPositionMs, durationMs)
        updateVinylWidget(context, appWidgetManager, currentTrack, isPlaying)
        updateFullCenterWidget(context, appWidgetManager, currentTrack, isPlaying, currentPositionMs, durationMs, isShuffle, repeatMode)
    }

    private fun getPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MusicWidgetReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getOpenAppPendingIntent(context: Context, openEq: Boolean = false): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (openEq) {
                putExtra("OPEN_EQUALIZER", true)
            }
        }
        val requestCode = if (openEq) 999 else 998
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // --- 1. Compact Widget (2x1) ---
    private fun updateCompactWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        track: Track?,
        isPlaying: Boolean
    ) {
        val component = ComponentName(context, CompactMusicWidget::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(component)
        if (widgetIds.isEmpty()) return

        val views = RemoteViews(context.packageName, R.layout.widget_compact)

        val bgBitmap = createGlassBackgroundBitmap(width = 400, height = 180, cornerRadius = 32f)
        views.setImageViewBitmap(R.id.img_bg, bgBitmap)

        val coverBitmap = createCoverBitmap(context, track, size = 120, cornerRadius = 24f)
        views.setImageViewBitmap(R.id.img_cover, coverBitmap)

        views.setTextViewText(R.id.txt_title, track?.title ?: "No track playing")
        views.setTextViewText(R.id.txt_artist, track?.artist ?: "0003 Player")

        val playPauseBitmap = createPlayPauseIconBitmap(isPlaying = isPlaying, size = 96)
        views.setImageViewBitmap(R.id.btn_play_pause, playPauseBitmap)

        views.setOnClickPendingIntent(R.id.widget_container, getOpenAppPendingIntent(context))
        views.setOnClickPendingIntent(R.id.btn_play_pause, getPendingIntent(context, ACTION_TOGGLE_PLAY, 1))

        appWidgetManager.updateAppWidget(widgetIds, views)
    }

    // --- 2. Standard Widget (4x1) ---
    private fun updateStandardWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        track: Track?,
        isPlaying: Boolean,
        positionMs: Int,
        durationMs: Int
    ) {
        val component = ComponentName(context, StandardMusicWidget::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(component)
        if (widgetIds.isEmpty()) return

        val views = RemoteViews(context.packageName, R.layout.widget_standard)

        val bgBitmap = createGlassBackgroundBitmap(width = 800, height = 200, cornerRadius = 32f)
        views.setImageViewBitmap(R.id.img_bg, bgBitmap)

        val coverBitmap = createCoverBitmap(context, track, size = 130, cornerRadius = 24f)
        views.setImageViewBitmap(R.id.img_cover, coverBitmap)

        views.setTextViewText(R.id.txt_title, track?.title ?: "No track playing")
        views.setTextViewText(R.id.txt_artist, track?.artist ?: "0003 Player")

        views.setImageViewBitmap(R.id.btn_prev, createControlIconBitmap(ControlIconType.PREVIOUS, size = 80))
        views.setImageViewBitmap(R.id.btn_play_pause, createPlayPauseIconBitmap(isPlaying = isPlaying, size = 100))
        views.setImageViewBitmap(R.id.btn_next, createControlIconBitmap(ControlIconType.NEXT, size = 80))

        val progress = if (durationMs > 0) ((positionMs.toFloat() / durationMs) * 100).toInt() else 0
        views.setProgressBar(R.id.progress_bar, 100, progress, false)

        views.setOnClickPendingIntent(R.id.widget_container, getOpenAppPendingIntent(context))
        views.setOnClickPendingIntent(R.id.btn_prev, getPendingIntent(context, ACTION_PREV, 2))
        views.setOnClickPendingIntent(R.id.btn_play_pause, getPendingIntent(context, ACTION_TOGGLE_PLAY, 3))
        views.setOnClickPendingIntent(R.id.btn_next, getPendingIntent(context, ACTION_NEXT, 4))

        appWidgetManager.updateAppWidget(widgetIds, views)
    }

    // --- 3. Vinyl Widget (3x3) ---
    private fun updateVinylWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        track: Track?,
        isPlaying: Boolean
    ) {
        val component = ComponentName(context, VinylMusicWidget::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(component)
        if (widgetIds.isEmpty()) return

        val views = RemoteViews(context.packageName, R.layout.widget_vinyl)

        val bgBitmap = createGlassBackgroundBitmap(width = 500, height = 500, cornerRadius = 48f)
        views.setImageViewBitmap(R.id.img_bg, bgBitmap)

        val vinylBitmap = createVinylBitmap(context, track, size = 360)
        views.setImageViewBitmap(R.id.img_vinyl, vinylBitmap)

        val isFav = track?.isFavorite ?: false
        val favBitmap = createControlIconBitmap(
            if (isFav) ControlIconType.HEART_FILLED else ControlIconType.HEART_OUTLINE,
            size = 80
        )
        views.setImageViewBitmap(R.id.btn_favorite, favBitmap)

        val playPauseBitmap = createPlayPauseIconBitmap(isPlaying = isPlaying, size = 110)
        views.setImageViewBitmap(R.id.btn_play_pause, playPauseBitmap)

        views.setTextViewText(R.id.txt_title, track?.title ?: "Track Title")
        views.setTextViewText(R.id.txt_artist, track?.artist ?: "Artist Name")

        views.setOnClickPendingIntent(R.id.widget_container, getOpenAppPendingIntent(context))
        views.setOnClickPendingIntent(R.id.btn_play_pause, getPendingIntent(context, ACTION_TOGGLE_PLAY, 5))
        views.setOnClickPendingIntent(R.id.btn_favorite, getPendingIntent(context, ACTION_FAVORITE, 6))

        appWidgetManager.updateAppWidget(widgetIds, views)
    }

    // --- 4. Full Command Center Widget (4x2) ---
    private fun updateFullCenterWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        track: Track?,
        isPlaying: Boolean,
        positionMs: Int,
        durationMs: Int,
        isShuffle: Boolean,
        repeatMode: RepeatMode
    ) {
        val component = ComponentName(context, FullCenterMusicWidget::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(component)
        if (widgetIds.isEmpty()) return

        val views = RemoteViews(context.packageName, R.layout.widget_full_center)

        val bgBitmap = createGlassBackgroundBitmap(width = 800, height = 450, cornerRadius = 40f)
        views.setImageViewBitmap(R.id.img_bg, bgBitmap)

        val coverBitmap = createCoverBitmap(context, track, size = 180, cornerRadius = 28f)
        views.setImageViewBitmap(R.id.img_cover, coverBitmap)

        views.setTextViewText(R.id.txt_title, track?.title ?: "No track playing")
        val artistText = if (track != null) "${track.artist} • ${track.album}" else "0003 Player"
        views.setTextViewText(R.id.txt_artist, artistText)

        views.setImageViewBitmap(R.id.btn_eq, createControlIconBitmap(ControlIconType.EQUALIZER, size = 70))
        val isFav = track?.isFavorite ?: false
        val favBitmap = createControlIconBitmap(
            if (isFav) ControlIconType.HEART_FILLED else ControlIconType.HEART_OUTLINE,
            size = 70
        )
        views.setImageViewBitmap(R.id.btn_favorite, favBitmap)

        val progress = if (durationMs > 0) ((positionMs.toFloat() / durationMs) * 100).toInt() else 0
        views.setProgressBar(R.id.progress_bar, 100, progress, false)
        views.setTextViewText(R.id.txt_time_current, formatDuration(positionMs / 1000))
        views.setTextViewText(R.id.txt_time_total, formatDuration(durationMs / 1000))

        val shuffleBitmap = createControlIconBitmap(
            if (isShuffle) ControlIconType.SHUFFLE_ACTIVE else ControlIconType.SHUFFLE_INACTIVE,
            size = 80
        )
        views.setImageViewBitmap(R.id.btn_shuffle, shuffleBitmap)

        views.setImageViewBitmap(R.id.btn_prev, createControlIconBitmap(ControlIconType.PREVIOUS, size = 90))
        views.setImageViewBitmap(R.id.btn_play_pause, createPlayPauseIconBitmap(isPlaying = isPlaying, size = 110))
        views.setImageViewBitmap(R.id.btn_next, createControlIconBitmap(ControlIconType.NEXT, size = 90))

        val repeatType = when (repeatMode) {
            RepeatMode.OFF -> ControlIconType.REPEAT_INACTIVE
            RepeatMode.ALL -> ControlIconType.REPEAT_ALL
            RepeatMode.ONE -> ControlIconType.REPEAT_ONE
        }
        views.setImageViewBitmap(R.id.btn_repeat, createControlIconBitmap(repeatType, size = 80))

        views.setOnClickPendingIntent(R.id.widget_container, getOpenAppPendingIntent(context))
        views.setOnClickPendingIntent(R.id.btn_eq, getOpenAppPendingIntent(context, openEq = true))
        views.setOnClickPendingIntent(R.id.btn_favorite, getPendingIntent(context, ACTION_FAVORITE, 7))
        views.setOnClickPendingIntent(R.id.btn_shuffle, getPendingIntent(context, ACTION_SHUFFLE, 8))
        views.setOnClickPendingIntent(R.id.btn_prev, getPendingIntent(context, ACTION_PREV, 9))
        views.setOnClickPendingIntent(R.id.btn_play_pause, getPendingIntent(context, ACTION_TOGGLE_PLAY, 10))
        views.setOnClickPendingIntent(R.id.btn_next, getPendingIntent(context, ACTION_NEXT, 11))
        views.setOnClickPendingIntent(R.id.btn_repeat, getPendingIntent(context, ACTION_REPEAT, 12))

        appWidgetManager.updateAppWidget(widgetIds, views)
    }

    // --- BITMAP RENDERERS ---

    private fun createGlassBackgroundBitmap(width: Int, height: Int, cornerRadius: Float): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.parseColor("#E01A182E"),
                    Color.parseColor("#D50D0B1A")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glassPaint)

        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width * 0.15f, height * 0.1f, width * 0.7f,
                Color.parseColor("#407A5CFF"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, accentPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.parseColor("#80FFFFFF"),
                    Color.parseColor("#30FFFFFF"),
                    Color.parseColor("#10FFFFFF")
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        val borderRect = RectF(1.5f, 1.5f, width - 1.5f, height - 1.5f)
        canvas.drawRoundRect(borderRect, cornerRadius - 1.5f, cornerRadius - 1.5f, borderPaint)

        return bitmap
    }

    private fun createCoverBitmap(context: Context, track: Track?, size: Int, cornerRadius: Float): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())

        if (track?.albumArtUri != null) {
            try {
                val uri = Uri.parse(track.albumArtUri)
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val original = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (original != null) {
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                        val path = Path().apply {
                            addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
                        }
                        canvas.clipPath(path)
                        val srcRect = Rect(0, 0, original.width, original.height)
                        canvas.drawBitmap(original, srcRect, rect, paint)
                        return output
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val colors = when ((track?.coverGradientIndex ?: 0) % 5) {
            0 -> intArrayOf(Color.parseColor("#4B6CB7"), Color.parseColor("#182848"))
            1 -> intArrayOf(Color.parseColor("#FF512F"), Color.parseColor("#DD2476"))
            2 -> intArrayOf(Color.parseColor("#8A2387"), Color.parseColor("#E94057"))
            3 -> intArrayOf(Color.parseColor("#11998E"), Color.parseColor("#38EF7D"))
            else -> intArrayOf(Color.parseColor("#1F1C2C"), Color.parseColor("#928DAB"))
        }

        val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), colors, null, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, gradPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * 0.45f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val initial = track?.title?.firstOrNull()?.toString()?.uppercase() ?: "🎵"
        val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(initial, size / 2f, yPos, textPaint)

        return output
    }

    private fun createVinylBitmap(context: Context, track: Track?, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        val cy = size / 2f
        val radius = size / 2f

        val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111115")
        }
        canvas.drawCircle(cx, cy, radius, discPaint)

        val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#282830")
            strokeWidth = 2f
        }
        var r = radius * 0.92f
        while (r > radius * 0.48f) {
            canvas.drawCircle(cx, cy, r, groovePaint)
            r -= (radius * 0.08f)
        }

        val centerRadius = radius * 0.42f
        val centerRect = RectF(cx - centerRadius, cy - centerRadius, cx + centerRadius, cy + centerRadius)
        val centerCover = createCoverBitmap(context, track, (centerRadius * 2).toInt(), cornerRadius = centerRadius)

        val path = Path().apply {
            addCircle(cx, cy, centerRadius, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(centerCover, null, centerRect, Paint(Paint.ANTI_ALIAS_FLAG))
        canvas.restore()

        val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0A0910")
        }
        canvas.drawCircle(cx, cy, radius * 0.08f, holePaint)

        val holeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#80FFFFFF")
            strokeWidth = 2f
        }
        canvas.drawCircle(cx, cy, radius * 0.08f, holeBorderPaint)

        return bitmap
    }

    private fun createPlayPauseIconBitmap(isPlaying: Boolean, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        val cy = size / 2f
        val radius = size / 2f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                intArrayOf(Color.parseColor("#8B5CF6"), Color.parseColor("#6366F1")),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, radius, bgPaint)

        val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        if (isPlaying) {
            val barW = size * 0.12f
            val barH = size * 0.4f
            val gap = size * 0.12f
            canvas.drawRoundRect(
                RectF(cx - gap / 2 - barW, cy - barH / 2, cx - gap / 2, cy + barH / 2),
                barW / 2, barW / 2, symbolPaint
            )
            canvas.drawRoundRect(
                RectF(cx + gap / 2, cy - barH / 2, cx + gap / 2 + barW, cy + barH / 2),
                barW / 2, barW / 2, symbolPaint
            )
        } else {
            val path = Path().apply {
                val triH = size * 0.42f
                val triW = size * 0.38f
                moveTo(cx - triW / 3, cy - triH / 2)
                lineTo(cx + triW * 2 / 3, cy)
                lineTo(cx - triW / 3, cy + triH / 2)
                close()
            }
            canvas.drawPath(path, symbolPaint)
        }

        return bitmap
    }

    enum class ControlIconType {
        PREVIOUS, NEXT, HEART_OUTLINE, HEART_FILLED, SHUFFLE_ACTIVE, SHUFFLE_INACTIVE, REPEAT_INACTIVE, REPEAT_ALL, REPEAT_ONE, EQUALIZER
    }

    private fun createControlIconBitmap(type: ControlIconType, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        val cy = size / 2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when (type) {
                ControlIconType.HEART_FILLED -> Color.parseColor("#FF4B72")
                ControlIconType.SHUFFLE_ACTIVE, ControlIconType.REPEAT_ALL, ControlIconType.REPEAT_ONE -> Color.parseColor("#A78BFA")
                ControlIconType.SHUFFLE_INACTIVE, ControlIconType.REPEAT_INACTIVE -> Color.parseColor("#60FFFFFF")
                else -> Color.WHITE
            }
            style = Paint.Style.FILL
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        when (type) {
            ControlIconType.PREVIOUS -> {
                val p = Paint(paint).apply { strokeWidth = size * 0.12f; style = Paint.Style.STROKE }
                canvas.drawLine(cx - size * 0.22f, cy - size * 0.25f, cx - size * 0.22f, cy + size * 0.25f, p)
                val path = Path().apply {
                    moveTo(cx + size * 0.22f, cy - size * 0.25f)
                    lineTo(cx - size * 0.08f, cy)
                    lineTo(cx + size * 0.22f, cy + size * 0.25f)
                }
                canvas.drawPath(path, paint)
            }
            ControlIconType.NEXT -> {
                val p = Paint(paint).apply { strokeWidth = size * 0.12f; style = Paint.Style.STROKE }
                canvas.drawLine(cx + size * 0.22f, cy - size * 0.25f, cx + size * 0.22f, cy + size * 0.25f, p)
                val path = Path().apply {
                    moveTo(cx - size * 0.22f, cy - size * 0.25f)
                    lineTo(cx + size * 0.08f, cy)
                    lineTo(cx - size * 0.22f, cy + size * 0.25f)
                }
                canvas.drawPath(path, paint)
            }
            ControlIconType.HEART_FILLED -> {
                val path = Path().apply {
                    val w = size * 0.35f
                    moveTo(cx, cy + w * 0.8f)
                    cubicTo(cx - w * 1.2f, cy + w * 0.1f, cx - w * 1.1f, cy - w * 0.8f, cx, cy - w * 0.4f)
                    cubicTo(cx + w * 1.1f, cy - w * 0.8f, cx + w * 1.2f, cy + w * 0.1f, cx, cy + w * 0.8f)
                }
                canvas.drawPath(path, paint)
            }
            ControlIconType.HEART_OUTLINE -> {
                val strokeP = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = size * 0.08f
                    color = Color.parseColor("#B3FFFFFF")
                }
                val path = Path().apply {
                    val w = size * 0.35f
                    moveTo(cx, cy + w * 0.8f)
                    cubicTo(cx - w * 1.2f, cy + w * 0.1f, cx - w * 1.1f, cy - w * 0.8f, cx, cy - w * 0.4f)
                    cubicTo(cx + w * 1.1f, cy - w * 0.8f, cx + w * 1.2f, cy + w * 0.1f, cx, cy + w * 0.8f)
                }
                canvas.drawPath(path, strokeP)
            }
            ControlIconType.SHUFFLE_ACTIVE, ControlIconType.SHUFFLE_INACTIVE -> {
                val strokeP = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = size * 0.09f
                }
                canvas.drawLine(cx - size * 0.3f, cy - size * 0.2f, cx + size * 0.1f, cy + size * 0.2f, strokeP)
                canvas.drawLine(cx - size * 0.3f, cy + size * 0.2f, cx + size * 0.1f, cy - size * 0.2f, strokeP)
                val arrow = Path().apply {
                    moveTo(cx + size * 0.1f, cy - size * 0.28f)
                    lineTo(cx + size * 0.3f, cy - size * 0.2f)
                    lineTo(cx + size * 0.1f, cy - size * 0.12f)
                }
                canvas.drawPath(arrow, paint)
            }
            ControlIconType.REPEAT_INACTIVE, ControlIconType.REPEAT_ALL, ControlIconType.REPEAT_ONE -> {
                val strokeP = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = size * 0.08f
                }
                val r = size * 0.25f
                val rect = RectF(cx - r, cy - r, cx + r, cy + r)
                canvas.drawArc(rect, 45f, 270f, false, strokeP)
                val arrow = Path().apply {
                    moveTo(cx + r * 0.6f, cy - r * 1.1f)
                    lineTo(cx + r * 1.1f, cy - r * 0.7f)
                    lineTo(cx + r * 0.6f, cy - r * 0.3f)
                }
                canvas.drawPath(arrow, paint)

                if (type == ControlIconType.REPEAT_ONE) {
                    val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = paint.color
                        textSize = size * 0.32f
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    val yPos = cy - ((textP.descent() + textP.ascent()) / 2f)
                    canvas.drawText("1", cx, yPos, textP)
                }
            }
            ControlIconType.EQUALIZER -> {
                val strokeP = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = size * 0.08f
                }
                val x1 = cx - size * 0.22f
                val x2 = cx
                val x3 = cx + size * 0.22f

                canvas.drawLine(x1, cy - size * 0.28f, x1, cy + size * 0.28f, strokeP)
                canvas.drawLine(x2, cy - size * 0.28f, x2, cy + size * 0.28f, strokeP)
                canvas.drawLine(x3, cy - size * 0.28f, x3, cy + size * 0.28f, strokeP)

                val knobP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
                canvas.drawCircle(x1, cy - size * 0.1f, size * 0.08f, knobP)
                canvas.drawCircle(x2, cy + size * 0.12f, size * 0.08f, knobP)
                canvas.drawCircle(x3, cy - size * 0.04f, size * 0.08f, knobP)
            }
        }

        return bitmap
    }

    private fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%d:%02d".format(m, s)
    }
}
