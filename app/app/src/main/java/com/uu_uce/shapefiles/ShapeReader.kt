package com.uu_uce.shapefiles

import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import java.io.File
import java.io.FileInputStream

/**
 * a way of getting chunks, possibly from storage or a server
 * see documentation at shapefile-linter for file specific information
 * unsigned types are marked as experimental, but they are perfectly safe to use
 * @param[dir] directory to get the chunks from
 * @param[layerType] type of chunks this should retrieve
 *
 * @property[xoff] global offset in x direction (only used for heightlines)
 * @property[yoff] global offset in y direction (only used for heightlines)
 * @property[zoff] global offset in z direction (only used for heightlines)
 * @property[mult] global multiplier (only used for heightlines)
 * @property[nrCuts] how many chunks each LOD is cut into
 * @property[mods] modulos of all LODs
 */
abstract class ChunkGetter(
    protected var dir: File,
    protected val layerType: LayerType){

    protected var xoff = 0.0f
    protected var yoff = 0.0f
    protected var zoff = 0.0f
    protected var mult = 0.0f
    protected var bmin = p3NaN
    protected var bmax = p3NaN
    var nrCuts: List<Int> = listOf()
    // For each level, the modulo of the heigtlines
    // Ex. mods[0] = 100
    // Then level 0 has 100 meter between each heightline
    // And every heightline height(z) is a multiple of 100
    var mods: List<Int> = listOf()

    /**
     * read the info file provided with the chunks
     * @return the bounding box of the chunk
     */
    abstract fun readInfo(): Pair<p3,p3>

    /**
     * read a chunk
     * @param[cIndex] the chunk to read
     * @return the read chunk
     */
    abstract fun getChunk(cIndex: ChunkIndex):Chunk
}

/**
 * simple reader that can read basic types from a binary file
 *
 * @property[index] current progression through file
 * @property[ubytes] all file content
 */
@ExperimentalUnsignedTypes
class FileReader{
    private var index = 0
    private var ubytes: UByteArray

    /**
     * creates a file reader based on a file
     * @param[file] the file to read
     */
    constructor(file: File){
        val inputStream = FileInputStream(file)
        ubytes = inputStream.readBytes().toUByteArray()
        inputStream.close()
    }

    /**
     * creates a file reader based on a bytestream
     */
    constructor(us: UByteArray){
        ubytes = us
    }

    /**
     * @return next ubyte
     */
    fun readUByte(): UByte{
        return  ubytes[index++].toUInt().toUByte()
    }

    /**
     * @return next two ubytes as ushort
     */
    fun readUShort(): UShort{
        return  ((ubytes[index++].toUInt() shl 8) +
                (ubytes[index++].toUInt())).toUShort()
    }

    /**
     * @return next four ubytes as uint
     */
    fun readUInt(): UInt{
        return  (ubytes[index++].toUInt() shl 24) +
                (ubytes[index++].toUInt() shl 16) +
                (ubytes[index++].toUInt() shl 8) +
                (ubytes[index++].toUInt())
    }

    /**
     * @return next eight ubytes as ulong
     */
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
/**
 * reader for heightline chunks
 *
 * @property[readValue] function used to read values, can be UByte..ULong
 */
class HeightLineReader(
    dir: File,
    layerType: LayerType
): ChunkGetter(dir,layerType) {
    private var readValue: (reader: FileReader) -> Float = {reader -> reader.readUShort().toFloat()}

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
            val z = readValue(reader)
            val bb1 = p3(
                readValue(reader)/mult + xoff,
                readValue(reader)/mult + yoff,
                readValue(reader)
            )
            val bb2 = p3(
                readValue(reader)/mult + xoff,
                readValue(reader)/mult + yoff,
                readValue(reader)
            )

            val nrPoints = reader.readULong()
            val points: List<p2> = List(nrPoints.toInt()) {
                p2(readValue(reader)/mult + xoff, readValue(reader)/mult + yoff)
            }

            Heightline(points)
        }

        val time1 = System.currentTimeMillis() - time
        Logger.log(LogType.Continuous, "BinShapeReader", "loadtime: $time1")

        return Chunk(shapes, layerType)
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

        val compression = reader.readUByte().toInt()
        readValue = {fileReader ->
            when(compression){
                1 -> fileReader.readUByte().toFloat()
                2 -> fileReader.readUShort().toFloat()
                4 -> fileReader.readUInt().toFloat()
                else -> fileReader.readULong().toFloat()
            }
        }

        bmin = p3(readValue(reader)/mult + xoff, readValue(reader)/mult + yoff, readValue(reader)/mult)
        bmax = p3(readValue(reader)/mult + xoff, readValue(reader)/mult + yoff, readValue(reader)/mult)

        val nrMods = reader.readULong()
        mods = List(nrMods.toInt()){
            reader.readULong().toInt()
        }
        return Pair(bmin, bmax)
    }
}

@ExperimentalUnsignedTypes
/**
 * reader for colored line chunks
 */
class ColoredLineReader(
    dir: File,
    private val lineStyles: List<LineStyle>,
    layerType: LayerType
): ChunkGetter(dir, layerType) {

    override fun getChunk(cIndex: ChunkIndex): Chunk {
        //find the correct file and read all information inside
        val time = System.currentTimeMillis()
        val file = File(dir, geolineChunkName(cIndex))
        val reader = FileReader(file)

        val xoff = reader.readULong().toFloat()
        val yoff = reader.readULong().toFloat()
        val zoff = reader.readULong().toFloat()
        val mult = reader.readULong().toFloat()

        val compression = reader.readUByte().toInt()
        val readValue: () -> Float = {
            when(compression){
                1 -> reader.readUByte().toFloat()
                2 -> reader.readUShort().toFloat()
                4 -> reader.readUInt().toFloat()
                else -> reader.readULong().toFloat()
            }
        }

        val cbmin = p3(readValue()/mult + xoff, readValue()/mult + yoff, readValue())
        val cbmax = p3(readValue()/mult + xoff, readValue()/mult + yoff, readValue())

        val nrShapes = reader.readULong()
        val shapes = List(nrShapes.toInt()) {
            val style = lineStyles[reader.readULong().toInt()]

            val bb1 = p3(
                readValue()/mult + xoff,
                readValue()/mult + yoff,
                readValue()
            )
            val bb2 = p3(
                readValue()/mult + xoff,
                readValue()/mult + yoff,
                readValue()
            )

            val nrPoints = reader.readULong()
            val points: List<p2> = List(nrPoints.toInt()) {
                p2(readValue()/mult + xoff, readValue()/mult + yoff)
            }

            ColoredLine(points, style)
        }

        val time1 = System.currentTimeMillis() - time
        Logger.log(LogType.Continuous, "BinShapeReader", "loadtime: $time1")

        return Chunk(shapes, layerType)
    }

    override fun readInfo(): Pair<p3,p3>{
        val reader = FileReader(File(dir, "chunks.geolineinfo"))

        bmin = p3(reader.readUInt().toFloat(), reader.readUInt().toFloat(), reader.readUInt().toFloat())
        bmax = p3(reader.readUInt().toFloat(), reader.readUInt().toFloat(), reader.readUInt().toFloat())

        val cuts = reader.readUByte()

        nrCuts = listOf(cuts.toInt())

        return Pair(bmin, bmax)
    }
}

@ExperimentalUnsignedTypes
/**
 * reader for polygon chunks
 *
 * @param[hasStyles] whether these polygons have styles
 * @param[polyStyles] all available polygon styles
 */
class PolygonReader(
    dir: File,
    layerType: LayerType,
    private var hasStyles: Boolean,
    private val polyStyles: List<PolyStyle>
): ChunkGetter(dir,layerType){
    override fun getChunk(cIndex: ChunkIndex): Chunk {
        val file = File(dir, polyChunkName(cIndex))

        val reader = FileReader(file)

        val xoff = reader.readULong().toFloat()
        val yoff = reader.readULong().toFloat()
        val zoff = reader.readULong().toFloat()
        val mult = reader.readULong().toFloat()

        val compression = reader.readUByte().toInt()
        val readValue: () -> Float = {
            when(compression){
                1 -> reader.readUByte().toFloat()
                2 -> reader.readUShort().toFloat()
                4 -> reader.readUInt().toFloat()
                else -> reader.readULong().toFloat()
            }
        }

        val bmin = p3(readValue()/mult + xoff, readValue()/mult + yoff, readValue())
        val bmax = p3(readValue()/mult + xoff, readValue()/mult + yoff, readValue())

        val nrShapes = reader.readULong()
        val shapes: List<Polygon> = List(nrShapes.toInt()) {
            val nrVertices = reader.readULong()
            val vertices: List<p2> = List(nrVertices.toInt()) {
                p2(readValue()/mult + xoff, readValue()/mult + yoff)
            }
            val nrIndices = reader.readULong()
            val indices: List<Short> = List(nrIndices.toInt()) {
                reader.readUShort().toShort()
            }

            val style  =
                if(hasStyles) {
                    val styleIndex = reader.readULong().toInt()
                    polyStyles[styleIndex]
                }
                else PolyStyle(floatArrayOf(0.2f,0.2f,0.8f))

            /* Read shape bounding boxes
            val bmi = p3(readValue()/mult + xoff, readValue()/mult + yoff, readValue())
            val bma = p3(readValue()/mult + xoff, readValue()/mult + yoff, readValue())*/
            // Discard shape bounding boxes
            for(i in 0 until 6) readValue()

            Polygon(vertices, indices.toMutableList(), style)
        }

        return Chunk(shapes, layerType)
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