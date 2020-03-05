package com.uu_uce.misc

import android.util.Log

enum class LogType{ Event, Continues }

class Logger{
    companion object {
        private var typeMask = hashMapOf<LogType, Boolean>()
        private val tagMask = hashMapOf<String, Boolean>()

        init{
            typeMask[LogType.Continues] = true
            typeMask[LogType.Event] = true
        }

        fun setTagEnabled(tag: String, enabled: Boolean){
            tagMask[tag] = enabled
        }

        fun setTypeEnabled(type: LogType, enabled: Boolean){
            typeMask[type] = enabled
        }

        fun log(tag: String, msg: String){
            if(!tagMask.containsKey(tag))
                tagMask[tag] = true
            if(tagMask[tag] == true)
                Log.d(tag, msg)
        }

        fun logTyped(type: LogType, tag: String, msg: String){
            if(!typeMask.containsKey(type))
                return
            if(typeMask[type] == true)
                log(tag, msg)
        }
    }
}