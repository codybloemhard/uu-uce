package com.uu_uce.shapefiles

import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import java.io.File
import kotlin.math.log
import kotlin.math.pow

interface ChunkGetter{
    fun getChunk(cIndex: ChunkIndex):Chunk
}

//simple reader that can read basic types from a binary file
@ExperimentalUnsignedTypes
class FileReader{
    private var index = 0
    private var ubytes: UByteArray

    constructor(file: File){
        val inputStream = file.inputStream()
        ubytes = inputStream.readBytes().toUByteArray()
        inputStream.close()
    }

    constructor(us: UByteArray){
        ubytes = us
    }

    fun readUShort(): UShort{
        return  ((ubytes[index++].toUInt() shl 8) +
                (ubytes[index++].toUInt())).toUShort()
    }

    fun readUInt(): UInt{
        return  (ubytes[index++].toUInt() shl 24) +
                (ubytes[index++].toUInt() shl 16) +
                (ubytes[index++].toUInt() shl 8) +
                (ubytes[index++].toUInt())
    }
    fun readULong(): ULong{
        return  (ubytes[index++].toULong() shl 56) +
                (ubytes[index++].toULong() shl 48) +
                (ubytes[index++].toULong() shl 40) +
                (ubytes[index++].toULong() shl 32) +
                (ubytes[index++].toULong() shl 24) +
                (ubytes[index++].toULong() shl 16) +
                (ubytes[index++].toULong() shl 8) +
                (ubytes[index++].toULong())
    }
}

@ExperimentalUnsignedTypes
class BinShapeReader(
    private var dir: File,
    private var nrOfLODs: Int
): ChunkGetter {
    override fun getChunk(cIndex: ChunkIndex): Chunk {
        //find the correct file and read all information inside
        val time = System.currentTimeMillis()
        val file = File(dir, "test.obj")
        val reader = FileReader(file)

        val xoff = reader.readULong().toDouble()
        val yoff = reader.readULong().toDouble()
        val mult = reader.readULong().toDouble()
        val bmin = p3(reader.readUShort().toDouble()/mult + xoff, reader.readUShort().toDouble()/mult + yoff, reader.readUShort().toDouble())
        val bmax = p3(reader.readUShort().toDouble()/mult + xoff, reader.readUShort().toDouble()/mult + yoff, reader.readUShort().toDouble())

        val nrShapes = reader.readULong()
        var chunkShapes = List(nrShapes.toInt()) {
            val z = reader.readUShort().toDouble()
            val bb1 = p3(
                reader.readUShort().toDouble()/mult + xoff,
                reader.readUShort().toDouble()/mult + yoff,
                reader.readUShort().toDouble()
            )
            val bb2 = p3(
                reader.readUShort().toDouble()/mult + xoff,
                reader.readUShort().toDouble()/mult + yoff,
                reader.readUShort().toDouble()
            )

            val nrPoints = reader.readULong()
            val points: List<p2> = List(nrPoints.toInt()) { j ->
                p2(reader.readUShort().toDouble()/mult + xoff, reader.readUShort().toDouble()/mult + yoff)
            }

            ShapeZ(ShapeType.Polygon, points, bb1, bb2)
        }

        val time1 = System.currentTimeMillis() - time

        //create zoom levels (temporary)
        chunkShapes = chunkShapes.sortedBy{ it.meanZ() }

        val zDens = hashMapOf<Int,Int>()
        val zDensSorted: List<Int>
        chunkShapes.map{ s ->
            val mz = s.meanZ()
            val old = zDens[mz] ?: 0
            val new = old + 1
            zDens.put(mz, new)
        }
        zDensSorted = zDens.keys.sorted()

        val indices: MutableList<Int> = mutableListOf()
        var nrHeights = 0
        var curPow = log(zDensSorted.size.toDouble(), 2.0).toInt() + 1
        var curStep = 0
        var stepSize: Int = 1 shl curPow

        val i = cIndex.third
        val level = (i + 1).toDouble() / nrOfLODs
        val factor = maxOf(level.pow(3), 0.1)
        val totalHeights =
            if (i == nrOfLODs - 1) zDensSorted.size
            else (factor * zDensSorted.size).toInt()

        while (nrHeights < totalHeights) {
            val index: Int = curStep * stepSize
            if (index >= zDensSorted.size) {
                curPow--
                stepSize = 1 shl curPow
                curStep = 1
                continue
            }

            indices.add(index)
            nrHeights++
            curStep += 2
        }

        val shapes: MutableList<ShapeZ> = mutableListOf()
        indices.sort()
        if (indices.isNotEmpty()) {
            var a = 0
            var b = 0
            while (a < indices.size && b < chunkShapes.size) {
                val shape = chunkShapes[b]
                val z = zDensSorted[indices[a]]
                when {
                    shape.meanZ() == z -> {
                        shapes.add(chunkShapes[b])
                        //val factor =(level + level.pow(3))/2
                        //shapes.add(ShapeZ((factor), allShapes[b]))
                        b++
                    }
                    shape.meanZ() < z -> b++
                    else -> a++
                }
            }
        }

        val time2 = System.currentTimeMillis() - time - time1

        Logger.log(LogType.Continuous, "BinShapeReader", "first part: $time1")
        Logger.log(LogType.Continuous, "BinShapeReader", "second part: $time2")

        return Chunk(shapes, bmin, bmax)
    }
}