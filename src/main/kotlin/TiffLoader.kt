package org.openrndr.tiff

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import mil.nga.tiff.TiffReader
import org.openrndr.draw.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Tile(val x: Int, val y: Int, val colorBuffer: ColorBuffer)

suspend fun loadTiffTiles(
    input: File,
    tileWidth: Int, tileHeight: Int,
    overlapWidth: Int = 0, overlapHeight: Int = 0
): List<List<Tile>> {
    val image = TiffReader.readTiff(input)
    val directories = image.fileDirectories

    val imageWidth = directories[0].imageWidth.toInt()
    val imageHeight = directories[0].imageHeight.toInt()

    val xTiles = Math.ceil(imageWidth.toDouble() / (tileWidth - 2 * overlapWidth)).toInt()
    val yTiles = Math.ceil(imageHeight.toDouble() / (tileHeight - 2 * overlapHeight)).toInt()

    val tiles = mutableListOf<MutableList<Tile>>()
    val rasters = directories[0].readRasters()

    val components = rasters.fieldTypes.size

    // this is not the best/fastest way to do it.
    for (y in 0 until yTiles) {

        val row = mutableListOf<Tile>()

        for (x in 0 until xTiles) {
            val xOff = (x * (tileWidth - overlapWidth))
            val yOff = (y * (tileHeight - overlapHeight))
            val width = Math.min(imageWidth - xOff, tileWidth)
            val height = Math.min(imageHeight - yOff, tileHeight)

            val cb = colorBuffer(width, height, format = when(components) {
                3 -> ColorFormat.RGB
                4 -> ColorFormat.RGBa
                else -> throw IllegalArgumentException("only supporting RGB and RGBa formats")
            })
            cb.flipV = true

            val bb = ByteBuffer.allocateDirect(width * height * components)
            bb.order(ByteOrder.nativeOrder())
            bb.rewind()

            for (v in 0 until height) {
                val pixelRow = rasters.getPixelRow(yOff + v, ByteOrder.nativeOrder())
                var r = 0
                var g = 0
                var b = 0
                var a = 0
                for (u in 0 until xOff * components step components) {
                    r += pixelRow[u]
                    g += pixelRow[u + 1]
                    b += pixelRow[u + 2]
                    if (components == 4) {
                        a += pixelRow[u + 3]
                    }
                }
                for (u in 0 until width * components step components) {
                    r += pixelRow[xOff * components + u]
                    g += pixelRow[xOff * components + u + 1]
                    b += pixelRow[xOff * components + u + 2]
                    if (components == 4) {
                        a += pixelRow[xOff * components + u + 3]
                    }
                    bb.put((r and 0xff).toByte())
                    bb.put((g and 0xff).toByte())
                    bb.put((b and 0xff).toByte())
                    if (components == 4) {
                        bb.put((a and 0xff).toByte())
                    }
                }
            }
            bb.rewind()
            cb.write(bb)

            val tile = Tile(xOff, yOff, cb)
            row.add(tile)
            yield()
        }
        tiles.add(row)
    }
    return tiles
}

fun loadTiffTilesBlocking(file: File, tileWidth: Int, tileHeight: Int): List<List<Tile>> {
    return runBlocking { loadTiffTiles(file, tileWidth, tileHeight) }
}

suspend fun loadTiff(file: File): ColorBuffer = loadTiffTiles(file, Int.MAX_VALUE, Int.MAX_VALUE)[0][0].colorBuffer


fun loadTiffBlocking(file: File) : ColorBuffer {
    return runBlocking { loadTiff(file) }
}