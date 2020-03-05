package com.uu_uce.pins

abstract class PinContent {
    abstract fun getTextContent(): String?
}

class PinTextContent : PinContent(){
    var text : String = "Lorem ipsum en dan nog wat dingen"

    override fun getTextContent() : String {
        return text
    }
}


