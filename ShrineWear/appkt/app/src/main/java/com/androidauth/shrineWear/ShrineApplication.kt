package com.androidauth.shrineWear

import android.app.Application

class ShrineApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    Graph.provide(this)
  }
}