package com.uu_uce.shapefiles

import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import java.io.File
import java.io.FileInputStream

/*
a way of getting chunks, possibly from storage or a server
see documentation at shapefile-linter for file specific information
unsigned types are marked as experimental, but they are perfectly safe to use
 */
abstract class ChunkGetter(
    protected var dir: File){
    abstract fun getChunk(cIndex: ChunkIndex):Chunk
    protected var xoff = 0.0
    protected var yoff = 0.0
    protected var zoff = 0.0
    protected var mult = 0.0
    protected var bmin = p3NaN
    protected var bmax = p3NaN
    var nrCuts: List<Int> = listOf()
    // For each level, the modulo of the heigt lines
    // Ex. mods[0] = 100
    // Than level 0 has 100 meter between each heightline
    // And every heightline height(z) is a multiple of 100
    var mods: List<Int> = listOf()

    //read the information file provided for most layers
    //returns bounding box of the entire layer
    fun readInfo(): Pair<p3,p3>{
        val reader = FileReader(File(dir, "chunks.info"))

        val nrLODs = reader.readULong()
        nrCuts = List(nrLODs.toInt()) {
            reader.readULong().toInt()
        }

        xoff = reader.readULong().toDouble()
        yoff = reader.readULong().toDouble()
        zoff = reader.readULong().toDouble()
        mult = reader.readULong().toDouble()

        bmin = p3(reader.readUShort().toDouble()/mult + xoff, reader.readUShort().toDouble()/mult + yoff, reader.readUShort().toDouble()/mult)
        bmax = p3(reader.readUShort().toDouble()/mult + xoff, reader.readUShort().toDouble()/mult + yoff, reader.readUShort().toDouble()/mult)

        val nrMods = reader.readULong()
        mods = List(nrMods.toInt()){
            reader.readULong().toInt()
        }
        return Pair(bmin, bmax)
    }
}

//simple reader that can read basic types from a binary file
@ExperimentalUnsignedTypes
class FileReader{
    private var index = 0
    private var ubytes: UByteArray

    constructor(file: File){
        val inputStream = FileInputStream(file)
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


//reader for heightline chunks

@ExperimentalUnsignedTypes
class HeightLineReader(
    dir: File
): ChunkGetter(dir) {
    override fun getChunk(cIndex: ChunkIndex): Chunk {
        //find the correct file and read all information inside
        val time = System.currentTimeMillis()
        val file = File(dir, chunkName(cIndex))
        val reader = FileReader(file)

        val lodLevel = reader.readULong()
        val x = reader.readULong()
        val y = reader.readULong()

        val nrShapes = reader.readULong()
        val shapes = List(nrShapes.toInt()) {
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

            HeightShapeZ(points, bb1, bb2)
        }

        val time1 = System.currentTimeMillis() - time
        Logger.log(LogType.Continuous, "BinShapeReader", "loadtime: $time1")

        return Chunk(shapes, bmin, bmax, LayerType.Height)
    }
}

//reader for polygon chunks
@ExperimentalUnsignedTypes
class PolygonReader(
    dir: File
): ChunkGetter(dir) {
    override fun getChunk(cIndex: ChunkIndex): Chunk {
        val file = File(dir, "river")
        val reader = FileReader(file)

        val xoff = reader.readULong().toDouble()
        val yoff = reader.readULong().toDouble()
        val zoff = reader.readULong().toDouble()
        val mult = reader.readULong().toDouble()
        val bmin = p3(reader.readUShort().toDouble()/mult + xoff, reader.readUShort().toDouble()/mult + yoff, reader.readUShort().toDouble())
        val bmax = p3(reader.readUShort().toDouble()/mult + xoff, reader.readUShort().toDouble()/mult + yoff, reader.readUShort().toDouble())

        val nrShapes = reader.readULong()
        val shapes: List<PolygonZ> = List(nrShapes.toInt()) {
            val bbmin = p3(reader.readUShort().toDouble()/mult + xoff, reader.readUShort().toDouble()/mult + yoff, reader.readUShort().toDouble()/mult + zoff)
            val bbmax = p3(reader.readUShort().toDouble()/mult + xoff, reader.readUShort().toDouble()/mult + yoff, reader.readUShort().toDouble()/mult + zoff)

            val nrOuter = reader.readULong()
            val outerRings: List<List<p3>> = List(nrOuter.toInt()) {
                val nrPoints = reader.readULong()
                List(nrPoints.toInt()) {
                    p3(reader.readUShort().toDouble()/mult + xoff, reader.readUShort().toDouble()/mult + yoff, reader.readUShort().toDouble()/mult + zoff)
                }
            }

            val nrInner = reader.readULong()
            val innerRings: List<List<p3>> = List(nrInner.toInt()) {
                val nrPoints = reader.readULong()
                List(nrPoints.toInt()) {
                    p3(reader.readUShort().toDouble()/mult + xoff, reader.readUShort().toDouble()/mult + yoff, reader.readUShort().toDouble()/mult + zoff)
                }
            }

            PolygonZ(outerRings, innerRings, bbmin, bbmax)
        }

        return Chunk(shapes, bmin, bmax, LayerType.Water)
    }
}