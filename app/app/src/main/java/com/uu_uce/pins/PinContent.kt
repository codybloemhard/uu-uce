package com.uu_uce.pins

import android.util.JsonReader
import java.io.StringReader


class PinContent(contentString : String) {
    private val content : List<ContentBlockInterface>
    init{
        content = getContent(contentString)
    }

    private fun getContent(contentString : String) : List<ContentBlockInterface>{
            val reader = JsonReader(StringReader(contentString))

            return readContentBlocks(reader)
        }

    private fun readContentBlocks(reader : JsonReader) :  List<ContentBlockInterface>{
        val contentBlocks : MutableList<ContentBlockInterface> = mutableListOf()

        reader.beginArray()
        while (reader.hasNext()) {
            contentBlocks.add(readBlock(reader))
        }
        reader.endArray()
        return contentBlocks
    }

    private fun readBlock(reader: JsonReader): ContentBlockInterface {
        var blockTag : BlockTag = BlockTag.UNDEFINED
        var blockContent : ContentBlockInterface
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "tag" -> {
                    blockTag = blockTagFromString(reader.nextString())
                }
                "content" -> {
                    val returnBlock = when(blockTag){
                        BlockTag.UNDEFINED  -> error("Undefined block tag")
                        BlockTag.TEXT       -> TextContentBlock(reader.nextString())
                        BlockTag.IMAGE      -> ImageContentBlock(reader.nextString())
                        BlockTag.VIDEO      -> VideoContentBlock(reader.nextString())
                    }
                    reader.endObject()
                    return returnBlock
                }
                else -> {
                    error("Wrong content format")
                }
            }
        }
        error("Wrong content format")
    }
}

interface ContentBlockInterface{
    fun drawContent()
}

class TextContentBlock(val text : String) : ContentBlockInterface{
    override fun drawContent(){

    }
}

class ImageContentBlock(val imagePath : String) : ContentBlockInterface{
    override fun drawContent(){

    }
}

class VideoContentBlock(val videoPath : String) : ContentBlockInterface{
    override fun drawContent(){

    }
}

enum class BlockTag{
    UNDEFINED,
    TEXT,
    IMAGE,
    VIDEO;
}

fun blockTagFromString(tagString : String) : BlockTag{
    return when (tagString) {
        "TEXT"  -> BlockTag.TEXT
        "IMAGE" -> BlockTag.IMAGE
        "VIDEO" -> BlockTag.VIDEO
        else    ->  BlockTag.UNDEFINED
    }
}



