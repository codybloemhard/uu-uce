package com.uu_uce.allpins

import android.util.JsonReader
import com.uu_uce.misc.Logger
import java.io.File
import java.io.FileReader


fun parsePins(pinFile : File) : List<PinData> {
    val reader = JsonReader(FileReader(pinFile))
    val pinList = mutableListOf<PinData>()

    reader.beginArray()
    while (reader.hasNext()) {
        val result = parsePin(reader)
        if(result != null){
            pinList.add(result)
        }
    }
    reader.endArray()

    return pinList
}

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

    reader.beginObject()
    while (reader.hasNext()) {
        when (reader.nextName()) {
            "pin_id"        -> pinId            = reader.nextString()
            "title"         -> title            = reader.nextString()
            "type"          -> type             = reader.nextString()
            "status"        -> status           = reader.nextInt()
            "difficulty"    -> difficulty       = reader.nextInt()
            "location_utm"  -> location         = reader.nextString()
            "pred_id"       -> predecessorIds   = reader.nextString()
            "succ_id"       -> followIds        = reader.nextString()
            "content"       -> content          = reader.nextString()
        }
    }
    reader.endObject()

    if(pinId == "" || location == "" || type == "" || content == ""){
        Logger.error("PinParser", "Essential part of pin was missing")
        return null
    }
    else{
        return PinData(pinId, location, difficulty, type, title, content, status, predecessorIds, followIds)
    }
}
