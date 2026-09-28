package com.example.presentation.achievements

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.database.UserSettings
import com.example.util.AchievementProgress
import com.example.util.StreakMilestoneInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

data class ShareableAchievementData(
    val settings: UserSettings,
    val milestoneInfo: StreakMilestoneInfo,
    val achievements: List<AchievementProgress>,
    val focusScore: Int,
    val activeLimitsCount: Int
)

object AchievementCardRenderer {

    private const val CARD_WIDTH = 1080

    /**
     * Generates a high-resolution, shareable Bitmap dynamically sized to fit all user achievements
     * without clipping, overlapping, or losing information.
     */
    suspend fun renderShareableBitmap(data: ShareableAchievementData): Bitmap = withContext(Dispatchers.Default) {
        val items = data.achievements
        val columns = 2
        val rowCount = ceil(items.size / columns.toFloat()).toInt().coerceAtLeast(1)

        val headerAndStatsHeight = 820
        val badgeCardHeight = 230
        val badgeVerticalGap = 24
        val gridHeight = rowCount * badgeCardHeight + (rowCount - 1).coerceAtLeast(0) * badgeVerticalGap
        val footerHeight = 150

        val totalHeight = headerAndStatsHeight + gridHeight + footerHeight

        val bitmap = Bitmap.createBitmap(CARD_WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Deep Obsidian & Midnight Gradient Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, CARD_WIDTH.toFloat(), totalHeight.toFloat(),
                intArrayOf(
                    Color.parseColor("#070E1A"),
                    Color.parseColor("#0D1B2E"),
                    Color.parseColor("#111933"),
                    Color.parseColor("#060C16")
                ),
                floatArrayOf(0f, 0.35f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), totalHeight.toFloat(), bgPaint)

        // Ambient Glow Orbs (Cyan top-right, Violet middle-left, Gold bottom-right)
        drawGlowOrb(canvas, CARD_WIDTH * 0.85f, 180f, 420f, Color.parseColor("#2E00E5FF"))
        drawGlowOrb(canvas, CARD_WIDTH * 0.15f, totalHeight * 0.45f, 460f, Color.parseColor("#269D4EDD"))
        drawGlowOrb(canvas, CARD_WIDTH * 0.82f, totalHeight - 240f, 380f, Color.parseColor("#22FFAB00"))

        // Outer Frame Border
        val frameRect = RectF(24f, 24f, CARD_WIDTH - 24f, totalHeight - 24f)
        val frameBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            shader = LinearGradient(
                0f, 0f, CARD_WIDTH.toFloat(), totalHeight.toFloat(),
                intArrayOf(
                    Color.parseColor("#8000E5FF"),
                    Color.parseColor("#30FFFFFF"),
                    Color.parseColor("#709D4EDD"),
                    Color.parseColor("#60FFCA28")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(frameRect, 48f, 48f, frameBorderPaint)

        // 2. Top Branding Header
        val padX = 64f
        var cursorY = 76f

        // Shield Logo Box
        val logoSize = 86f
        val logoRect = RectF(padX, cursorY, padX + logoSize, cursorY + logoSize)
        val logoBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                logoRect.left, logoRect.top, logoRect.right, logoRect.bottom,
                Color.parseColor("#3800E5FF"),
                Color.parseColor("#389D4EDD"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(logoRect, 24f, 24f, logoBgPaint)
        val logoStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.parseColor("#00E5FF")
        }
        canvas.drawRoundRect(logoRect, 24f, 24f, logoStrokePaint)
        drawShieldIcon(canvas, logoRect.centerX(), logoRect.centerY(), 24f, Color.parseColor("#00E5FF"))

        // Brand Title & Tagline
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("FocusLock", padX + logoSize + 24f, cursorY + 48f, titlePaint)

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("FOCUS STREAK & ACHIEVEMENTS", padX + logoSize + 24f, cursorY + 80f, subtitlePaint)

        // Gen Z Status Pill on Top Right
        val unlockedCount = items.count { it.achievement.isUnlocked }
        val genZTag = when {
            data.milestoneInfo.currentStreak >= 7 -> "YOU'RE ON A ROLL 🔥"
            unlockedCount >= 3 -> "LOCKED IN 🔒"
            else -> "FOCUS MODE: ON ⚡"
        }
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2600E5FF")
        }
        val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val pillTextWidth = pillTextPaint.measureText(genZTag)
        val pillW = pillTextWidth + 44f
        val pillH = 48f
        val pillRect = RectF(CARD_WIDTH - padX - pillW, cursorY + 18f, CARD_WIDTH - padX, cursorY + 18f + pillH)
        canvas.drawRoundRect(pillRect, 24f, 24f, pillPaint)
        val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.parseColor("#8000E5FF")
        }
        canvas.drawRoundRect(pillRect, 24f, 24f, pillBorderPaint)
        canvas.drawText(genZTag, pillRect.left + 22f, pillRect.centerY() + 7f, pillTextPaint)

        cursorY += logoSize + 38f

        // 3. Hero 4-Stat Telemetry Cards Row
        val statGap = 20f
        val totalStatWidth = CARD_WIDTH - (padX * 2)
        val statCardW = (totalStatWidth - statGap * 3) / 4f
        val statCardH = 175f

        val totalFocusMin = data.milestoneInfo.totalFocusMinutes
        val focusFormatted = if (totalFocusMin >= 60) {
            "${totalFocusMin / 60}h ${totalFocusMin % 60}m"
        } else {
            "${totalFocusMin}m"
        }

        val heroStats = listOf(
            StatBoxSpec(
                label = "STREAK",
                value = "${data.milestoneInfo.currentStreak}d 🔥",
                sub = "Best: ${data.milestoneInfo.bestStreak}d",
                accentColor = Color.parseColor("#FF9100")
            ),
            StatBoxSpec(
                label = "LEVEL & XP",
                value = "Lv. ${data.settings.level}",
                sub = "${data.settings.xp} XP earned",
                accentColor = Color.parseColor("#00E5FF")
            ),
            StatBoxSpec(
                label = "DEEP FOCUS",
                value = focusFormatted,
                sub = "${data.milestoneInfo.totalCompletedSessions} sessions",
                accentColor = Color.parseColor("#A855F7")
            ),
            StatBoxSpec(
                label = "BADGES",
                value = "$unlockedCount/${items.size}",
                sub = "Score: ${data.focusScore}",
                accentColor = Color.parseColor("#FFCA28")
            )
        )

        heroStats.forEachIndexed { idx, spec ->
            val left = padX + idx * (statCardW + statGap)
            val rect = RectF(left, cursorY, left + statCardW, cursorY + statCardH)
            drawGlassCard(canvas, rect, 28f, spec.accentColor, isHighlighted = true)

            val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#94A3B8")
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.06f
            }
            canvas.drawText(spec.label, left + 22f, cursorY + 42f, lblPaint)

            val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 38f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(spec.value, left + 22f, cursorY + 102f, valPaint)

            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = spec.accentColor
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(spec.sub, left + 22f, cursorY + 144f, subPaint)
        }

        cursorY += statCardH + 28f

        // 4. Streak & 7-Day Consistency Banner Card
        val streakBannerH = 210f
        val streakRect = RectF(padX, cursorY, CARD_WIDTH - padX, cursorY + streakBannerH)
        drawGlassCard(canvas, streakRect, 30f, Color.parseColor("#00E5FF"), isHighlighted = false)

        val bannerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "${data.milestoneInfo.statusHeadline}  •  7-Day Consistency",
            padX + 28f,
            cursorY + 48f,
            bannerTitlePaint
        )

        val milestoneRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFCA28")
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(
            "Next Milestone: ${data.milestoneInfo.nextMilestoneDays}d",
            CARD_WIDTH - padX - 28f,
            cursorY + 48f,
            milestoneRightPaint
        )

        // 7-day circles inside banner
        val days = data.milestoneInfo.last7Days
        val dayZoneWidth = (CARD_WIDTH - padX * 2 - 56f)
        val dayStep = dayZoneWidth / 7f
        val dayCenterY = cursorY + 112f

        days.forEachIndexed { index, day ->
            val cx = padX + 28f + dayStep * index + dayStep / 2f
            val circleR = 24f

            val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when {
                    day.isCompleted -> Color.parseColor("#FF9100")
                    day.isToday -> Color.parseColor("#2900E5FF")
                    else -> Color.parseColor("#18283E")
                }
            }
            canvas.drawCircle(cx, dayCenterY, circleR, circlePaint)

            val circleBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                color = when {
                    day.isCompleted -> Color.parseColor("#FFCA28")
                    day.isToday -> Color.parseColor("#00E5FF")
                    else -> Color.parseColor("#30FFFFFF")
                }
            }
            canvas.drawCircle(cx, dayCenterY, circleR, circleBorder)

            val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (day.isCompleted) Color.parseColor("#08111E") else Color.parseColor("#CBD5E1")
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                if (day.isCompleted) "✓" else day.dayLabel.take(1),
                cx,
                dayCenterY + 7f,
                checkPaint
            )

            val dayLblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (day.isToday) Color.parseColor("#00E5FF") else Color.parseColor("#94A3B8")
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(day.dayLabel, cx, dayCenterY + 46f, dayLblPaint)
        }

        // Progress bar at bottom of Streak Banner
        val barLeft = padX + 28f
        val barRight = CARD_WIDTH - padX - 28f
        val barTop = cursorY + 178f
        val barBottom = barTop + 12f
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#091322")
        }
        canvas.drawRoundRect(RectF(barLeft, barTop, barRight, barBottom), 6f, 6f, trackPaint)

        val fillWidth = ((barRight - barLeft) * data.milestoneInfo.progressToMilestone.coerceAtLeast(0.06f))
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                barLeft, barTop, barLeft + fillWidth, barBottom,
                intArrayOf(
                    Color.parseColor("#FF9100"),
                    Color.parseColor("#FFCA28"),
                    Color.parseColor("#00E5FF")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(barLeft, barTop, barLeft + fillWidth, barBottom), 6f, 6f, fillPaint)

        cursorY += streakBannerH + 38f

        // 5. Section Title: EARNED BADGES & MILESTONES
        val secHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("EARNED BADGES & MILESTONES ($unlockedCount OF ${items.size} UNLOCKED)", padX, cursorY, secHeaderPaint)
        cursorY += 24f

        // 6. 2-Column Dynamic Achievements Grid
        val colGap = 24f
        val badgeW = (CARD_WIDTH - padX * 2 - colGap) / 2f
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        items.forEachIndexed { index, item ->
            val col = index % columns
            val row = index / columns
            val cardLeft = padX + col * (badgeW + colGap)
            val cardTop = cursorY + row * (badgeCardHeight + badgeVerticalGap)
            val cardRect = RectF(cardLeft, cardTop, cardLeft + badgeW, cardTop + badgeCardHeight)

            val isUnlocked = item.achievement.isUnlocked
            val accentColor = if (isUnlocked) Color.parseColor("#00E5FF") else Color.parseColor("#475569")
            drawGlassCard(canvas, cardRect, 26f, accentColor, isHighlighted = isUnlocked)

            // Badge Icon Circle
            val iconCx = cardLeft + 46f
            val iconCy = cardTop + 54f
            val iconR = 26f
            val iconCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isUnlocked) Color.parseColor("#2EFFCA28") else Color.parseColor("#1AFFFFFF")
            }
            canvas.drawCircle(iconCx, iconCy, iconR, iconCirclePaint)

            val iconStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = if (isUnlocked) Color.parseColor("#FFCA28") else Color.parseColor("#33FFFFFF")
            }
            canvas.drawCircle(iconCx, iconCy, iconR, iconStrokePaint)

            // Trophy / Lock symbol inside circle
            val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isUnlocked) Color.parseColor("#FFCA28") else Color.parseColor("#64748B")
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(if (isUnlocked) "★" else "🔒", iconCx, iconCy + 9f, symbolPaint)

            // XP Pill in top-right of badge card
            val xpText = "+${item.achievement.xpReward} XP"
            val xpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isUnlocked) Color.parseColor("#FFCA28") else Color.parseColor("#94A3B8")
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(xpText, cardRect.right - 22f, cardTop + 42f, xpPaint)

            // Title (truncated safely if needed)
            val titleTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isUnlocked) Color.WHITE else Color.parseColor("#CBD5E1")
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val maxTitleWidth = badgeW - 165f
            val ellipsizedTitle = android.text.TextUtils.ellipsize(
                item.achievement.title,
                titleTextPaint,
                maxTitleWidth,
                android.text.TextUtils.TruncateAt.END
            ).toString()
            canvas.drawText(ellipsizedTitle, cardLeft + 86f, cardTop + 50f, titleTextPaint)

            // Category / Unlock status label under title
            val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isUnlocked) Color.parseColor("#00E5FF") else Color.parseColor("#64748B")
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val unlockDateText = item.achievement.unlockedAt?.let { " • ${dateFormat.format(Date(it))}" } ?: ""
            val statusStr = if (isUnlocked) "UNLOCKED ✓$unlockDateText" else item.categoryTag
            canvas.drawText(statusStr, cardLeft + 86f, cardTop + 76f, statusPaint)

            // Multiline Description inside badge card (max 2 lines, never overflows)
            val descTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#B0C0D4")
                textSize = 19f
            }
            drawMultilineText(
                canvas = canvas,
                text = item.achievement.description,
                paint = descTextPaint,
                x = cardLeft + 24f,
                y = cardTop + 102f,
                maxWidth = (badgeW - 48f).toInt(),
                maxLines = 2
            )

            // Progress Bar at bottom of badge card
            val pLeft = cardLeft + 24f
            val pRight = cardRect.right - 24f
            val pTop = cardRect.bottom - 44f
            val pBottom = pTop + 10f

            val pTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#091220")
            }
            canvas.drawRoundRect(RectF(pLeft, pTop, pRight, pBottom), 5f, 5f, pTrackPaint)

            if (item.progressFraction > 0f) {
                val pFillW = (pRight - pLeft) * item.progressFraction.coerceIn(0.06f, 1f)
                val pFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        pLeft, pTop, pLeft + pFillW, pBottom,
                        if (isUnlocked) Color.parseColor("#00E5FF") else Color.parseColor("#1EA7FD"),
                        if (isUnlocked) Color.parseColor("#FFCA28") else Color.parseColor("#00E5FF"),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(RectF(pLeft, pTop, pLeft + pFillW, pBottom), 5f, 5f, pFillPaint)
            }

            // Progress text below bar
            val progTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isUnlocked) Color.parseColor("#FFCA28") else Color.parseColor("#94A3B8")
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(item.progressLabel, pRight, cardRect.bottom - 14f, progTextPaint)
        }

        // 7. Footer Branding
        val footerY = totalHeight - 66f
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Focus mode: ON. Distractions: BYE.  •  focuslockz.vercel.app 🌱",
            CARD_WIDTH / 2f,
            footerY,
            footerPaint
        )

        bitmap
    }

    private fun drawGlowOrb(canvas: Canvas, cx: Float, cy: Float, radius: Float, centerColor: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(centerColor, Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, radius, paint)
    }

    private fun drawGlassCard(
        canvas: Canvas,
        rect: RectF,
        cornerRadius: Float,
        accentColor: Int,
        isHighlighted: Boolean
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left, rect.top, rect.left, rect.bottom,
                if (isHighlighted) Color.parseColor("#E614263D") else Color.parseColor("#D90F1B2D"),
                if (isHighlighted) Color.parseColor("#EB0C1728") else Color.parseColor("#E00A1220"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = if (isHighlighted) 2.2f else 1.5f
            color = if (isHighlighted) accentColor else Color.parseColor("#28FFFFFF")
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
    }

    private fun drawShieldIcon(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        val path = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + r * 0.85f, cy - r * 0.55f)
            lineTo(cx + r * 0.85f, cy + r * 0.15f)
            quadTo(cx + r * 0.75f, cy + r * 0.82f, cx, cy + r * 1.05f)
            quadTo(cx - r * 0.75f, cy + r * 0.82f, cx - r * 0.85f, cy + r * 0.15f)
            lineTo(cx - r * 0.85f, cy - r * 0.55f)
            close()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            this.color = color
        }
        canvas.drawPath(path, paint)
    }

    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        x: Float,
        y: Float,
        maxWidth: Int,
        maxLines: Int
    ) {
        val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1f)
                .setMaxLines(maxLines)
                .setEllipsize(android.text.TextUtils.TruncateAt.END)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, paint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1f, 2f, false)
        }
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    /**
     * Saves the generated high-resolution achievement image to the device Gallery (MediaStore).
     */
    suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        try {
            val filename = "FocusLock_Achievements_${System.currentTimeMillis()}.png"
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FocusLock")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext null

            resolver.openOutputStream(imageUri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }

            imageUri
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Shares the generated achievement image via Android's native Share Sheet.
     */
    suspend fun shareBitmapImage(context: Context, bitmap: Bitmap) {
        val uri = withContext(Dispatchers.IO) {
            try {
                val shareDir = File(context.cacheDir, "shared_achievements")
                if (!shareDir.exists()) shareDir.mkdirs()
                val file = File(shareDir, "FocusLock_Achievements.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                null
            }
        } ?: return

        withContext(Dispatchers.Main) {
            try {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Locked in with FocusLock 🔒 Check out my focus streak & achievements! https://focuslockz.vercel.app"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(sendIntent, "Share FocusLock Achievements")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (_: Exception) {}
        }
    }

    private data class StatBoxSpec(
        val label: String,
        val value: String,
        val sub: String,
        val accentColor: Int
    )
}
