package com.aura

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.ImageDecoderDecoder
import com.aura.data.exercise.ExerciseStore
import com.aura.data.local.AppDatabase

class AuraApp : Application(), ImageLoaderFactory {
    val db: AppDatabase by lazy { AppDatabase.getInstance(this) }

    // Exercise demonstrations are animated GIFs.
    override fun newImageLoader() =
        ImageLoader.Builder(this).components { add(ImageDecoderDecoder.Factory()) }.build()

    override fun onCreate() {
        super.onCreate()
        ExerciseStore.init(this)
    }
}
