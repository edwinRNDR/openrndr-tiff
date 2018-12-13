package org.openrndr.tiff

import mil.nga.tiff.TiffReader
import org.openrndr.draw.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder


class Tile(val x: Int, val y: Int, val colorBuffer: ColorBuffer)

fun loadTiffTiles(
    input: File,
    tileWidth: Int, tileHeight: Int,
    overlapWidth: Int = 0, overlapHeight: Int = 0
): List<Tile> {
    val image = TiffReader.readTiff(input)
    val directories = image.fileDirectories

    val imageWidth = directories[0].imageWidth.toInt()
    val imageHeight = directories[0].imageHeight.toInt()

    val xTiles = Math.ceil(imageWidth.toDouble() / (tileWidth - 2 * overlapWidth)).toInt()
    val yTiles = Math.ceil(imageHeight.toDouble() / (tileHeight - 2 * overlapHeight)).toInt()

    val tiles = mutableListOf<Tile>()
    val rasters = directories[0].readRasters()

    val components = rasters.fieldTypes.size

    // this is not the best/fastest way to do it.
    for (y in 0 until yTiles) {
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
                val row = rasters.getPixelRow(yOff + v, ByteOrder.nativeOrder())
                var r = 0
                var g = 0
                var b = 0
                var a = 0
                for (u in 0 until xOff * components step components) {
                    r += row[u]
                    g += row[u + 1]
                    b += row[u + 2]
                    if (components == 4) {
                        a += row[u + 3]
                    }
                }
                for (u in 0 until width * components step components) {
                    r += row[xOff * components + u]
                    g += row[xOff * components + u + 1]
                    b += row[xOff * components + u + 2]
                    if (components == 4) {
                        a += row[xOff * components + u + 3]
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
            tiles.add(tile)
        }
    }
    return tiles
}

fun loadTiff(file: File): ColorBuffer = loadTiffTiles(file, Int.MAX_VALUE, Int.MAX_VALUE)[0].colorBuffer
//
//fun main(args: Array<String>) = application {
//    configure {
//        width = 1920
//        height = 1080
//    }
//    program {
//        val tiles = loadTiffTiles(File("data/test-image-001.tif"), 4096, 4096, overlapHeight = 400)
//        var offset = Vector2.ZERO
//        mouse.dragged.listen {
//            offset += it.dragDisplacement
//        }
//        extend {
//            drawer.translate(offset)
//            tiles.forEach {
//                drawer.isolated {
//                    drawer.drawStyle.colorMatrix =
//                            tint(ColorRGBa.WHITE.opacify(0.5 + 0.5 * Math.cos((it.x + it.y).toDouble())))
//                    drawer.translate(it.x.toDouble(), it.y.toDouble())
//                    drawer.image(it.colorBuffer)
//                }
//            }
//        }
//    }
//}
//
//
//
