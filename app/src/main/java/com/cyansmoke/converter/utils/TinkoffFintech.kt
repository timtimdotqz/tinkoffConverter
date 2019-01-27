package com.cyansmoke.converter.utils

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
//класс application для получения контекста из любой точки приложения
@SuppressLint("Registered")
class TinkoffFintech : Application() {
    init {
        instance = this
    }

    companion object {
        private var instance: TinkoffFintech? = null

        fun getContext(): Context {
            return instance!!.applicationContext
        }
    }

}