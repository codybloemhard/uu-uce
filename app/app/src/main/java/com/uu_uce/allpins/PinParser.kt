package com.uu_uce.allpins

import android.util.JsonReader
import com.uu_uce.misc.Logger
import java.io.File
import java.io.FileReader

/**
 * Parse PinData from a json file.
 * @param[pinFile] a json file containing pins.
 * @return a list of PinData which was parsed from the json file.
 */
fun parsePins(pinFile : File) : List<PinData>? {
    val reader = JsonReader(FileReader(pinFile))
    val pinList = mutableListOf<PinData>()

    reader.beginArray()
    try{
        while (reader.hasNext()) {
            val result = parsePin(reader)
            if(result != null){
                pinList.add(result)
            }
        }
        reader.endArray()

        pinFile.delete()
        return pinList
    } catch(e : Exception){
        e.printStackTrace()
        return null
    }
}

/**
 * Parse a single PinData object.
 * @param[reader] the current reader.
 * @return a single PinData object if it was successfully parsed.
 */
private fun parsePin(reader: JsonReader) : PinData? {
    var pinId          = ""
    var location       = ""
    var difficulty     = 0
    var type           = ""
    var title          = ""
    var content        = ""
    var status         = 1
    var predecessorIds = ""
    var followIds      = ""

    try{
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "pin_id"        -> pinId            = reader.nextString()
                "title"         -> title            = reader.nextString()
                "type"          -> type             = reader.nextString()
                "status"        -> status           = reader.nextInt()
                "difficulty"    -> difficulty       = reader.nextInt()
                "location"      -> location         = reader.nextString()
                "pred_id"       -> predecessorIds   = reader.nextString()
                "succ_id"       -> followIds        = reader.nextString()
                "content"       -> content          = reader.nextString()
                else            -> reader.nextString()
            }
        }
        reader.endObject()

        return if(pinId == "" || location == "" || content == ""){
            Logger.error("PinParser", "Essential part of pin was missing")
            null
        } else{
            PinData(pinId, location, difficulty, type, title, content, status, status, predecessorIds, followIds)
        }
    } catch(e : Exception){
        e.printStackTrace()
        return null
    }
}
