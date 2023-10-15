package com.utc.ccs.trfwsample

import android.app.Application

class DKApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		DKFramework.setApplicationContext(applicationContext)
	}
}