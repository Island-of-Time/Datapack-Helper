package de.miraculixx.webServer.utils

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object CustomNameTag {
    private val spriteSheet = ImageIO.read(javaClass.getResourceAsStream("/assets/pixel-sprites.png"))
    private val charIndex = "abcdefghijklmnopqrstuvwxyz".toCharArray()
    private val specialCharIndex = "#-?!.,:;".toCharArray()
    private val white = Color(255,255,255,255).rgb

    fun createNewNameTag(content: String, destination: File, mainColor: Int, shadowColor: Int, charShadowColor: Int, charColor: Int) {
        var globalWidth = 2 + 3
        val globalHeight = 8

        // Compile all chars
        val charDataList = content.lowercase().map { char ->
            val row = when {
                charIndex.contains(char) -> 0
                char.digitToIntOrNull() != null -> 1
                specialCharIndex.contains(char) -> 2
                else -> 3
            }
            val width = getWidth(char)
            globalWidth += width + 1
            CharData(char, width, row)
        }

        // Generate raw image
        val finalImage = BufferedImage(globalWidth, globalHeight, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until globalWidth) {
            for (y in 0 until globalHeight) {
                when {
                    y == globalHeight -1 && x != 0 -> finalImage.setRGB(x, y, shadowColor)
                    x == globalWidth -1 && y != 0 -> finalImage.setRGB(x, y, shadowColor)
                    x != globalWidth -1 && y != globalHeight -1 -> finalImage.setRGB(x, y, mainColor)
                }
            }
        }

        // Draw chars on image
        var widthCounter = 2
        charDataList.forEach { charData ->
            if (charData.row == 3) return@forEach
            for (x in 0 until charData.width) {
                for (y in 0..4) {
                    val xPos = when (charData.row) {
                        0 -> charIndex.indexOf(charData.char)
                        1 -> charData.char.digitToInt()
                        2 -> specialCharIndex.indexOf(charData.char)
                        else -> return@forEach
                    } * 5 + x
                    val pixel = spriteSheet.getRGB(xPos, y + (charData.row * 6))
                    if (pixel == white) {
                        finalImage.setRGB(widthCounter + x, 1 + y, charColor)
                        finalImage.setRGB(widthCounter + x + 1, 1 + y, charShadowColor)
                    }
                }
            }
            widthCounter += charData.width + 1
        }

        // Save image
        destination.parentFile.mkdirs()
        ImageIO.write(finalImage, "PNG", destination)
    }

    private fun getWidth(char: Char) = when (char) {
        '!', '.', ',', ':', ';' -> 2
        'i', 't', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> 3
        'm', 'v', 'w', 'x', 'y', '#' -> 5
        else -> 4
    }

    private data class CharData(val char: Char, val width: Int, val row: Int)
}