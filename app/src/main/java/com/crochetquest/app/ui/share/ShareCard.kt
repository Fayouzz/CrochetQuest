package com.crochetquest.app.ui.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.crochetquest.app.domain.cosmetics.CosmeticCatalog
import com.crochetquest.app.domain.projects.ProjectCategory
import com.crochetquest.app.ui.components.CosmeticSprites
import com.crochetquest.app.ui.components.PixelSprite
import com.crochetquest.app.ui.components.Sprites
import com.crochetquest.app.ui.components.TrophySprites
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** What goes on a share card. Everything is drawn on the phone; nothing is uploaded. */
data class ShareCardData(
    val catName: String,
    val accessoryKeys: List<String>,
    val projectName: String,
    val category: ProjectCategory,
    val completedAtMillis: Long?,
    val night: Boolean
)

/**
 * Draws a 1080 x 1350 picture of your cat next to the finished project's keepsake, saves it in
 * the app's cache and opens Android's share sheet. No account, no network, no extra permission.
 */
object ShareCard {
    private const val WIDTH = 1080
    private const val HEIGHT = 1350

    fun render(data: ShareCardData): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val background = if (data.night) 0xFF1F1B2E.toInt() else 0xFFFFF8EF.toInt()
        val panel = if (data.night) 0xFF2B2640.toInt() else 0xFFF4DDE5.toInt()
        val text = if (data.night) 0xFFF5ECE4.toInt() else 0xFF3D3432.toInt()
        val muted = if (data.night) 0xFFC9BDD6.toInt() else 0xFF6F635D.toInt()
        val rose = 0xFFD89BB5.toInt()

        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        fill.color = background
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), fill)

        fill.color = panel
        canvas.drawRoundRect(RectF(48f, 48f, WIDTH - 48f, HEIGHT - 48f), 56f, 56f, fill)

        // dashed "stitched" border
        val stitch = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = rose
            style = Paint.Style.STROKE
            strokeWidth = 6f
            pathEffect = DashPathEffect(floatArrayOf(26f, 18f), 0f)
        }
        canvas.drawRoundRect(RectF(80f, 80f, WIDTH - 80f, HEIGHT - 80f), 44f, 44f, stitch)

        val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        drawCentered(canvas, "I finished a project!", 250f, 70f, text, bold)

        // project name, wrapped to at most two lines
        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = text
            textSize = 92f
            typeface = bold
        }
        val nameWidth = WIDTH - 2 * 150
        val nameLayout = StaticLayout.Builder.obtain(data.projectName, 0, data.projectName.length, namePaint, nameWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(2)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(150f, 310f)
        nameLayout.draw(canvas)
        canvas.restore()

        // the cat (with its accessories) on the left, the project's keepsake on the right
        val catCell = 26
        val catLeft = 150
        val catTop = 560
        drawSprite(canvas, Sprites.cat, catLeft, catTop, catCell)
        data.accessoryKeys
            .mapNotNull { CosmeticCatalog.find(it) }
            .sortedBy { it.slot.ordinal }
            .forEach { item ->
                CosmeticSprites.forItem(item.key)?.let { drawSprite(canvas, it, catLeft, catTop, catCell) }
            }
        drawCentered(canvas, data.catName, catTop + 19 * catCell + 60f, 52f, text, bold, centerX = catLeft + 8f * catCell)

        val trophyCell = 30
        val trophyLeft = 640
        val trophyTop = 700
        drawSprite(canvas, TrophySprites.forCategory(data.category), trophyLeft, trophyTop, trophyCell)
        drawCentered(canvas, data.category.label, trophyTop + 8 * trophyCell + 70f, 44f, muted, bold, centerX = trophyLeft + 4f * trophyCell)

        data.completedAtMillis?.let {
            val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            drawCentered(canvas, "Finished on $date", 1190f, 46f, text, bold)
        }
        drawCentered(canvas, "Made with Crochet Quest", 1250f, 38f, muted, Typeface.DEFAULT)
        return bitmap
    }

    private fun drawCentered(
        canvas: Canvas,
        text: String,
        baseline: Float,
        size: Float,
        color: Int,
        typeface: Typeface,
        centerX: Float = WIDTH / 2f
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(text, centerX, baseline, paint)
    }

    /** Pixel art needs hard edges, so every pixel is a plain square rectangle. */
    private fun drawSprite(canvas: Canvas, sprite: PixelSprite, left: Int, top: Int, cell: Int) {
        val paint = Paint().apply { isAntiAlias = false }
        sprite.rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, char ->
                val color = sprite.colorAt(char)
                if (color != null) {
                    paint.color = color.toArgb()
                    canvas.drawRect(
                        (left + x * cell).toFloat(), (top + y * cell).toFloat(),
                        (left + (x + 1) * cell).toFloat(), (top + (y + 1) * cell).toFloat(),
                        paint
                    )
                }
            }
        }
    }

    /** Renders the card to a PNG in the cache folder. Call from a background thread. */
    fun createFile(context: Context, data: ShareCardData, projectId: Long): File {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // keep only the newest card
        val file = File(dir, "crochet-quest-project-$projectId.png")
        val bitmap = render(data)
        try {
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            bitmap.recycle()
        }
        return file
    }

    /** Opens Android's share sheet for [file]. */
    fun share(context: Context, file: File, projectName: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "I finished \"$projectName\" in Crochet Quest!")
            clipData = ClipData.newRawUri("Share card", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share your finished project"))
    }
}
