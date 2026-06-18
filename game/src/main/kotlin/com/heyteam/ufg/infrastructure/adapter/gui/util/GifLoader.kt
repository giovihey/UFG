package com.heyteam.ufg.infrastructure.adapter.gui.util

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadataNode

private const val GIF_DELAY_FACTOR_MS = 10L
private const val GIF_DELAY_MIN_MS = 20L
private const val GIF_DELAY_DEFAULT_MS = 100L

/**
 * Decodes a GIF from the classpath into a list of fully composited [GifFrame]s.
 *
 * Correctly handles:
 * - Partial/delta frames (GIF only stores changed pixels per frame)
 * - All disposal methods: none, doNotDispose, restoreToBackground, restoreToPrevious
 * - Transparency
 * - Missing or malformed metadata attributes (safe fallbacks throughout)
 *
 * @param resourcePath Path relative to the classpath root, e.g. "ufg_vide_clip.gif"
 */
fun loadGifFrames(resourcePath: String): List<GifFrame> {
    val bytes = readGifBytes(resourcePath)
    val tmp = writeTempGifFile(bytes)
    val reader = getGifImageReader()

    reader.input = ImageIO.createImageInputStream(tmp)

    val frameCount = reader.getNumImages(true)
    check(frameCount > 0) { "GIF has no frames: $resourcePath" }

    val (logicalWidth, logicalHeight) = getLogicalScreenSize(reader)
    val frames = mutableListOf<GifFrame>()
    val canvas = BufferedImage(logicalWidth, logicalHeight, BufferedImage.TYPE_INT_ARGB)

    decodeGifFrames(reader, frameCount, canvas, frames)

    return frames
}

private fun readGifBytes(resourcePath: String): ByteArray =
    Thread
        .currentThread()
        .contextClassLoader
        .getResourceAsStream(resourcePath)
        ?.use { it.readBytes() }
        ?: throw IllegalArgumentException("Resource not found: $resourcePath")

private fun writeTempGifFile(bytes: ByteArray): java.io.File =
    java.io.File.createTempFile("ufg_gif", ".gif").apply {
        deleteOnExit()
        writeBytes(bytes)
    }

private fun getGifImageReader(): javax.imageio.ImageReader {
    val reader =
        ImageIO
            .getImageReadersByFormatName("gif")
            .asSequence()
            .firstOrNull()
    check(reader != null) { "No GIF ImageReader available on this JVM" }
    return reader
}

private fun getLogicalScreenSize(reader: javax.imageio.ImageReader): Pair<Int, Int> {
    val root = reader.streamMetadata?.getAsTree("javax_imageio_gif_stream_1.0") as? IIOMetadataNode
    val w = root?.getAttribute("logicalScreenWidth")?.toIntOrNull()
    val h = root?.getAttribute("logicalScreenHeight")?.toIntOrNull()

    return if (isValidDimension(w) && isValidDimension(h)) {
        w!! to h!!
    } else {
        reader.read(0).let { it.width to it.height }
    }
}

private fun isValidDimension(value: Int?): Boolean = value != null && value > 0

private fun decodeGifFrames(
    reader: javax.imageio.ImageReader,
    frameCount: Int,
    canvas: BufferedImage,
    frames: MutableList<GifFrame>,
) {
    var canvasG: Graphics2D = canvas.createGraphics()
    var snapshot: BufferedImage? = null

    for (i in 0 until frameCount) {
        val rawFrame = reader.read(i)
        val frameMetadata = parseFrameMetadata(reader, i)

        // Snapshot for restoreToPrevious
        if (frameMetadata.disposal == "restoreToPrevious") {
            snapshot = BufferedImage(canvas.width, canvas.height, BufferedImage.TYPE_INT_ARGB)
            snapshot.createGraphics().apply {
                drawImage(canvas, 0, 0, null)
                dispose()
            }
        }

        canvasG.drawImage(rawFrame, frameMetadata.posX, frameMetadata.posY, null)

        // Capture fully composited frame
        val composited = BufferedImage(canvas.width, canvas.height, BufferedImage.TYPE_INT_ARGB)
        composited.createGraphics().apply {
            drawImage(canvas, 0, 0, null)
            dispose()
        }
        frames += GifFrame(BitmapPainter(composited.toComposeImageBitmap()), frameMetadata.delayMs)

        // Handle disposal
        when (frameMetadata.disposal) {
            "restoreToBackground" -> {
                canvasG.clearRect(frameMetadata.posX, frameMetadata.posY, rawFrame.width, rawFrame.height)
            }

            "restoreToPrevious" -> {
                canvasG.dispose()
                canvasG = canvas.createGraphics()
                if (snapshot != null) {
                    canvasG.drawImage(snapshot, 0, 0, null)
                }
            }
        }
    }

    canvasG.dispose()
}

private data class FrameMetadata(
    val posX: Int,
    val posY: Int,
    val delayMs: Long,
    val disposal: String,
)

private fun parseFrameMetadata(
    reader: javax.imageio.ImageReader,
    frameIndex: Int,
): FrameMetadata {
    val frameRoot =
        reader.getImageMetadata(frameIndex).getAsTree("javax_imageio_gif_image_1.0") as IIOMetadataNode

    val descNode = frameRoot.getElementsByTagName("ImageDescriptor").item(0) as? IIOMetadataNode
    val posX = descNode?.getAttribute("imageLeftPosition")?.toIntOrNull() ?: 0
    val posY = descNode?.getAttribute("imageTopPosition")?.toIntOrNull() ?: 0

    val gcNode = frameRoot.getElementsByTagName("GraphicControlExtension").item(0) as? IIOMetadataNode
    val rawDelayMs = gcNode?.getAttribute("delayTime")?.toIntOrNull() ?: 0
    val delayMs =
        if (rawDelayMs > 0) {
            (rawDelayMs.toLong() * GIF_DELAY_FACTOR_MS).coerceAtLeast(GIF_DELAY_MIN_MS)
        } else {
            GIF_DELAY_DEFAULT_MS
        }
    val disposal = gcNode?.getAttribute("disposalMethod") ?: "none"

    return FrameMetadata(posX, posY, delayMs, disposal)
}
