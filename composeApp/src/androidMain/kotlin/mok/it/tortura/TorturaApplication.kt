package mok.it.tortura

import android.app.Application
import multiplatform.network.cmptoast.AppContext

class TorturaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContext.set(applicationContext)
    }
}
