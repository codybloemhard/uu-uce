package com.uu_uce.pins

import android.util.JsonReader
import android.util.JsonToken
import java.io.InputStreamReader


class PinContent(
    private val path : String ){
    //val content : List<ContentBlock>
    init{
        //content = getContent(path)
    }

    /*private fun getContent(path : String) : List<ContentBlock>{
            val reader : JsonReader = JsonReader(InputStreamReader(System.in));
            reader.use { reader ->
                return readContentBlocks(reader);
            }
        }*/

    private fun readContentBlocks(reader : JsonReader) :  List<ContentBlock>{
        val contentBlocks : MutableList<ContentBlock> = mutableListOf()

        reader.beginArray();
        while (reader.hasNext()) {
            //contentBlocks.add(readBlock(reader));
        }
        reader.endArray();
        return contentBlocks;
    }

    private fun readBlock(reader: JsonReader): ContentBlock? {
        /*var id: Long = -1
        var text: String? = null
        var user: User? = null
        var geo: List<Double?>? = null
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (name == "id") {
                id = reader.nextLong()
            } else if (name == "text") {
                text = reader.nextString()
            } else if (name == "geo" && reader.peek() != JsonToken.NULL) {
                geo = readDoublesArray(reader)
            } else if (name == "user") {
                user = readUser(reader)
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()
        return ContentBlock()*/
        return ContentBlock()
    }
}

class ContentBlock(){

}




