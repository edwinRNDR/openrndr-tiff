# openrndr-tiff

A simple tiff / .tif loader for OPENRNDR

## Example use

```kotlin
val image = loadTiff(File("breakfast.tif"))
val tiles = loadTiffTiles(File("breakfast.tif"), 1024, 1024, overlapWidth=64, overlapHeight=64)
```
