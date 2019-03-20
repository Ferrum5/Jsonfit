package com.github.waterpeak.jsonfit.example

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.github.waterpeak.jsonfit.GsonConverter
import com.github.waterpeak.jsonfit.JpiHandler
import com.google.gson.Gson
import okhttp3.OkHttpClient
import java.util.concurrent.Executor

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        JpiHandler.initHandler(OkHttpClient.Builder().build(), GsonConverter(Gson()), object : Executor {

            val handler by lazy { Handler(Looper.getMainLooper()) }

            override fun execute(command: Runnable?) {
                command?.also { handler.post(it) }
            }

        })
    }
}