package com.uu_uce.misc

/**
 * A boolean with a listener attached.
 */
class ListenableBoolean {
    private var boo = false
    private var listener: ChangeListener? = null

    fun getValue(): Boolean {
        return boo
    }

    fun setValue(boo: Boolean) {
        this.boo = boo
        if (listener != null) listener!!.onChange()
    }

    fun setListener(listener: ChangeListener?) {
        this.listener = listener
    }

    interface ChangeListener {
        fun onChange()
    }
}

/* This program has been developed by students from the bachelor Computer
# Science at Utrecht University within the Software Project course. ©️ Copyright
# Utrecht University (Department of Information and Computing Sciences)*/

