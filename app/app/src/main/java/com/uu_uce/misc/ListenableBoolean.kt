package com.uu_uce.misc

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