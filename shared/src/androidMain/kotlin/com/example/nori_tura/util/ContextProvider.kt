package com.example.nori_tura.util

import android.content.Context

object ContextProvider {
    private var applicationContext: Context? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    fun getContext(): Context = applicationContext
        ?: throw IllegalStateException("ContextProvider not initialized")
}
