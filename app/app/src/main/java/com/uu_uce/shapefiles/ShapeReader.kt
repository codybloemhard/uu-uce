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
    protected var xoff = 0.0f
    protected var yoff = 0.0f
    protected var zoff = 0.0f
    protected var mult = 0.0f
    protected var bmin = p3NaN
    protected var bmax = p3NaN
    var nrCuts: List<Int> = listOf()
    // For each level, the modulo of the heigt lines
    // Ex. mods[0] = 100
    // Than level 0 has 100 meter between each heightline
    // And every heightline height(z) is a multiple of 100
    var mods: List<Int> = listOf()

    abstract fun readInfo(): Pair<p3,p3>
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

    fun readUByte(): UByte{
        return  ubytes[index++].toUInt().toUByte()
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
            val z = reader.readUShort().toFloat()
            val bb1 = p3(
                reader.readUShort().toFloat()/mult + xoff,
                reader.readUShort().toFloat()/mult + yoff,
                reader.readUShort().toFloat()
            )
            val bb2 = p3(
                reader.readUShort().toFloat()/mult + xoff,
                reader.readUShort().toFloat()/mult + yoff,
                reader.readUShort().toFloat()
            )

            val nrPoints = reader.readULong()
            val points: List<p2> = List(nrPoints.toInt()) {
                p2(reader.readUShort().toFloat()/mult + xoff, reader.readUShort().toFloat()/mult + yoff)
            }

            val style = Style(false, floatArrayOf(0.0f,0.0f,0.0f))

            HeightShapeZ(points, style)
        }

        val time1 = System.currentTimeMillis() - time
        Logger.log(LogType.Continuous, "BinShapeReader", "loadtime: $time1")

        return Chunk(shapes, bmin, bmax, LayerType.Height)
    }

    override fun readInfo(): Pair<p3,p3>{
        val reader = FileReader(File(dir, "chunks.info"))

        val nrLODs = reader.readULong()
        nrCuts = List(nrLODs.toInt()) {
            reader.readULong().toInt()
        }

        xoff = reader.readULong().toFloat()
        yoff = reader.readULong().toFloat()
        zoff = reader.readULong().toFloat()
        mult = reader.readULong().toFloat()

        bmin = p3(reader.readUShort().toFloat()/mult + xoff, reader.readUShort().toFloat()/mult + yoff, reader.readUShort().toFloat()/mult)
        bmax = p3(reader.readUShort().toFloat()/mult + xoff, reader.readUShort().toFloat()/mult + yoff, reader.readUShort().toFloat()/mult)

        val nrMods = reader.readULong()
        mods = List(nrMods.toInt()){
            reader.readULong().toInt()
        }
        return Pair(bmin, bmax)
    }
}

class Style(val outline: Boolean, val color: FloatArray)

@ExperimentalUnsignedTypes
class PolygonReader(
    dir: File,
    var hasStyles: Boolean,
    private val styles: List<Style>
): ChunkGetter(dir){
    override fun getChunk(cIndex: ChunkIndex): Chunk {
        val file = File(dir, polyChunkName(cIndex))
        val reader = FileReader(file)

        val xoff = reader.readULong().toFloat()
        val yoff = reader.readULong().toFloat()
        val zoff = reader.readULong().toFloat()
        val mult = reader.readULong().toFloat()

        val bmin = p3(reader.readUShort().toFloat()/mult + xoff, reader.readUShort().toFloat()/mult + yoff, reader.readUShort().toFloat())
        val bmax = p3(reader.readUShort().toFloat()/mult + xoff, reader.readUShort().toFloat()/mult + yoff, reader.readUShort().toFloat())

        val nrShapes = reader.readULong()
        val shapes: List<PolygonZ> = List(nrShapes.toInt()) {
            val nrVertices = reader.readULong()
            val vertices: List<p2> = List(nrVertices.toInt()) {
                p2(reader.readUShort().toFloat()/mult + xoff, reader.readUShort().toFloat()/mult + yoff)
            }
            val nrIndices = reader.readULong()
            val indices: List<Short> = List(nrIndices.toInt()) {
                reader.readUShort().toShort()
            }


            /*val nrOutlineIndices = reader.readULong()
            var outlineIndexPairs = List(nrOutlineIndices.toInt()) {
                Pair(reader.readUShort().toShort(),reader.readUShort().toShort())
            }*/

            val outlineIndices: MutableList<Short> = mutableListOf()
            /*for((start,end) in outlineIndexPairs){
                for(i in start until end){
                    outlineIndices.add(i.toShort())
                    outlineIndices.add(((i + 1)%nrVertices.toInt()).toShort())
                }
            }*/

            val style  =
                if(hasStyles) {
                    val styleIndex = reader.readULong().toInt()
                    styles[styleIndex]
                }
                else Style(false, floatArrayOf(0.2f,0.2f,0.8f))

            val _bmin = p3(reader.readUShort().toFloat()/mult + xoff, reader.readUShort().toFloat()/mult + yoff, reader.readUShort().toFloat())
            val _bmax = p3(reader.readUShort().toFloat()/mult + xoff, reader.readUShort().toFloat()/mult + yoff, reader.readUShort().toFloat())

            PolygonZ(vertices, indices.toMutableList(), outlineIndices.toList(), style)
        }

        return Chunk(shapes, bmin, bmax, LayerType.Water)
    }

    override fun readInfo(): Pair<p3,p3>{
        val reader = FileReader(File(dir, "chunks.polyinfo"))

        bmin = p3(reader.readUInt().toFloat(), reader.readUInt().toFloat(), reader.readUInt().toFloat())
        bmax = p3(reader.readUInt().toFloat(), reader.readUInt().toFloat(), reader.readUInt().toFloat())

        val cuts = reader.readUByte()

        nrCuts = listOf(cuts.toInt())

        return Pair(bmin, bmax)
    }
}