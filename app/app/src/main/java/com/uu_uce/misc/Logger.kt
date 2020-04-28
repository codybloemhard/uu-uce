package com.uu_uce.misc

import android.util.Log

enum class LogType{ Event, Continuous, Info, NotImplemented }

class Logger{
    companion object {
        private var typeMask = hashMapOf<LogType, Boolean>()
        private val tagMask = hashMapOf<String, Boolean>()
        private var defaultTagMask = true

        init{
            typeMask=hashMapOf(
                LogType.Continuous      to true,
                LogType.Event           to true,
                LogType.Info            to true,
                LogType.NotImplemented  to true)
        }

        fun setTagEnabled(tag: String, enabled: Boolean){
            tagMask[tag] = enabled
        }

        fun setTypeEnabled(type: LogType, enabled: Boolean){
            typeMask[type] = enabled
        }

        private fun log(tag: String, msg: String){
            if(!tagMask.containsKey(tag))
                tagMask[tag] = defaultTagMask
            if(tagMask[tag] == true)
                Log.d(tag, msg)
        }

        fun log(type: LogType, tag: String, msg: String){
            if(!typeMask.containsKey(type))
                return
            if(typeMask[type] == true)
                log(tag, msg)
        }

        fun error(tag: String, msg: String){
            Log.e(tag,msg)
        }
    }
}