package com.bcardoso.whitenoise.services

import android.app.IntentService
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver

class CustomResultReceiver(handler: Handler?, private val appReceiver: AppReceiver) : ResultReceiver(handler) {
    interface AppReceiver {
        fun onReceiveResult(resultCode: Int, resultData: Bundle?)
    }

    override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
        appReceiver.onReceiveResult(resultCode, resultData)
    }
}

class CustomIntentService : IntentService("CustomIntentService") {
    companion object {
        const val STATUS_RUNNING = 0
        const val STATUS_FINISHED = 1
        const val STATUS_ERROR = 2
    }

    override fun onHandleIntent(intent: Intent?) {
        intent?.let {
            val receiver = it.getParcelableExtra<ResultReceiver>("receiver")
            val b = Bundle()
            b.putString("action", intent?.action)
            receiver?.send(STATUS_FINISHED, b)
        }
    }
}