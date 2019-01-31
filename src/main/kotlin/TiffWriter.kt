package org.openrndr.tiff

import mil.nga.tiff.FileDirectory
import mil.nga.tiff.Rasters
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffWriter
import mil.nga.tiff.util.TiffConstants
import org.openrndr.draw.*
import java.io.File

fun saveTiffTiles(output: File, tiles: List<List<Tile>>, filter: Filter? = null) {
    val totalWidth = tiles[0].sumBy { it.colorBuffer.width }
    val totalHeight = tiles.sumBy { it[0].colorBuffer.height }

    println("saving $totalWidth $totalHeight")

    val rasters = Rasters(totalWidth, totalHeight, 3, 8, TiffConstants.SAMPLE_FORMAT_UNSIGNED_INT)
    val directory = FileDirectory()
    directory.setImageWidth(totalWidth)
    directory.setImageHeight(totalHeight)
    directory.setBitsPerSample(8)
    directory.compression = TiffConstants.COMPRESSION_DEFLATE
    directory.photometricInterpretation = (TiffConstants.PHOTOMETRIC_INTERPRETATION_RGB)
    directory.setSamplesPerPixel(3)
    directory.setRowsPerStrip(rasters.calculateRowsPerStrip(TiffConstants.PLANAR_CONFIGURATION_CHUNKY))

    directory.planarConfiguration = TiffConstants.PLANAR_CONFIGURATION_CHUNKY
    directory.setSampleFormat(TiffConstants.SAMPLE_FORMAT_UNSIGNED_INT)
    directory.writeRasters = rasters

    val tileWidth = tiles[0][0].colorBuffer.width
    val tileHeight = tiles[0][0].colorBuffer.height

    for ((v, row) in tiles.withIndex()) {
        for ((u, tile) in row.withIndex()) {
            val cb: ColorBuffer?

            if (filter != null) {
                cb = colorBuffer(tile.colorBuffer.width, tile.colorBuffer.height, 1.0, ColorFormat.RGB, ColorType.UINT8)
                filter.apply(tile.colorBuffer, cb)

            } else {
                cb = null
            }

            val shad = cb?.shadow ?: tile.colorBuffer.shadow

            shad.download()
            for (y in 0 until tile.colorBuffer.height) {
                for (x in 0 until tile.colorBuffer.width) {
                    val j = tileHeight * v + y
                    val i = tileWidth * u + x
                    val c = shad.read(x, y)
                    val rgb = arrayOf((c.r * 255).toByte(), (c.g * 255).toByte(), (c.b * 255).toByte())
                    rasters.setPixelSample(0, i, j, rgb[0])
                    rasters.setPixelSample(1, i, j, rgb[1])
                    rasters.setPixelSample(2, i, j, rgb[2])
                }
            }
            cb?.destroy()
        }
    }

    val image = TIFFImage()
    image.add(directory)
    TiffWriter.writeTiff(output, image)
}