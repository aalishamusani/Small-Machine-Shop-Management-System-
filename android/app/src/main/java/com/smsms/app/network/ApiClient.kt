package com.smsms.app.network

import com.smsms.app.BuildConfig
import com.smsms.app.auth.AuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// A single shared Retrofit/OkHttp instance for the whole app, built once.
// Creating a new client per screen would throw away OkHttp's internal
// connection pool and reconnect from scratch every time.
object ApiClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // BODY logging (full request/response) is handy while developing
        // but would leak data into logcat in a release build, so it's
        // tied to BuildConfig.DEBUG.
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        // The free hosting plan puts the backend to sleep when idle, and the
        // first request after that can take 30-60 seconds while it wakes up.
        // OkHttp's default of 10s would show "could not reach the server"
        // for what is really just a slow start, so allow up to 60s.
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        // Order matters: auth header gets added first, then the (larger)
        // logging interceptor logs the fully-built request including it.
        .addInterceptor(AuthInterceptor())
        .addInterceptor(LongRunningTimeoutInterceptor())
        .addInterceptor(loggingInterceptor)
        .build()

    val api: SmsmsApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SmsmsApi::class.java)
}
