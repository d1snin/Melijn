package me.melijn.melijnbot.commands.image

import com.madgag.gif.fmsware.GifDecoder
import me.melijn.melijnbot.commands.utility.prependZeros
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.message.sendFileRsp
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO


class PngsFromGifCommand : AbstractCommand("command.pngsfromgif") {

    init {
        id = 170
        name = "pngsFromGif"
        aliases = arrayOf("pfg")
        runConditions = arrayOf(RunCondition.USER_SUPPORTER)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val triple = ImageUtils.getImageBytesNMessage(context, "gif") ?: return
        val decoder = GifDecoder()

        ByteArrayInputStream(triple.first).use { bais ->
            decoder.read(bais)
        }

        ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zos ->
                for (i in 0 until decoder.frameCount) {
                    val coolFrame = decoder.getFrame(i)
                    val zipEntry = ZipEntry("frame_${gitGud(i, decoder.frameCount)}.png")

                    zos.putNextEntry(zipEntry)

                    ByteArrayOutputStream().use { baos2 ->
                        ImageIO.write(coolFrame, "png", baos2)
                        baos2.flush()
                        val imageInByte = baos2.toByteArray()
                        zos.write(imageInByte)
                    }
                }
            }

            sendFileRsp(context, baos.toByteArray(), "zip")
        }
    }

    private fun gitGud(cool: Int, maxSize: Int): String {
        val shouldSize = maxSize.toString().length + 1
        return cool.toString().prependZeros(shouldSize)
    }
}