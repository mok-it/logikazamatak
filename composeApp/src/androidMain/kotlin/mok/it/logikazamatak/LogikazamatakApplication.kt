package mok.it.logikazamatak

import android.app.Application
import multiplatform.network.cmptoast.AppContext

class LogikazamatakApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContext.set(applicationContext)
    }
}
