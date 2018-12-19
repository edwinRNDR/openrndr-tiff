# openrndr-tiff

A simple tiff / .tif loader for OPENRNDR

## Example use

```kotlin
val image = loadTiffBlocking(File("breakfast.tif"))
val tiles = loadTiffTilesBlocking(File("breakfast.tif"), 1024, 1024, overlapWidth=64, overlapHeight=64)
```
