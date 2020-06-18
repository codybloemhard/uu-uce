package com.uu_uce.allpins

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Holds the pin data as it's stored in the database
 * It's stored to a table called "pins"
 *
 * @property[pinId] the index of this pin, is the primary key
 * @property[location] the location of the pin (UTM Coordinates)
 * @property[difficulty] the difficulty of the pin (0-4)
 * @property[type] the type of the pin ("TEXT","IMAGE","VIDEO","MCQUIZ"
 * @property[title] the title of the pin
 * @property[content] the content shown in the pin, as a JSON String
 * @property[status] the status of the pin
 * @property[startStatus] the status of the pin at initiation
 * @property[predecessorIds] all pins preceding this one
 * @property[followIds] all pins following this one
 * @constructor builds one PinData object with the given data
 */
@Entity(tableName = "pins")
open class PinData(
    @PrimaryKey
    var pinId          : String,
    var location       : String,
    var difficulty     : Int,
    var type           : String,
    var title          : String,
    var content        : String,
    var status         : Int,
    var startStatus    : Int,
    var predecessorIds : String,
    var followIds      : String
)
