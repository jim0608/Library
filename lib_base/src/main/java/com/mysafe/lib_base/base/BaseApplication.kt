package com.mysafe.lib_base.base

import android.app.Application

open class BaseApplication : Application() {
    //endregion
    override fun onCreate() {
        super.onCreate()
    }

    companion object {
        //region 单例
        private var singleton: BaseApplication? = null
        fun GetSingleton(): BaseApplication? {
            if (singleton == null) singleton = BaseApplication()
            return singleton
        }
    }
}