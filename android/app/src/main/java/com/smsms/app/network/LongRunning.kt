package com.smsms.app.network

import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Invocation

// Marks an endpoint whose backend work is known to be slow (e.g. one that
// spawns a Python ML script), so it gets a longer read timeout than the
// OkHttp default of 10s. Kept per-endpoint rather than raising the timeout
// for the whole client, so a genuinely unreachable server still fails fast
// on every other screen.
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LongRunning(val readTimeoutSeconds: Int = 60)

// Retrofit tags every request with the interface method that produced it
// (Invocation), so the interceptor can read the annotation off that method.
class LongRunningTimeoutInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val longRunning = chain.request()
            .tag(Invocation::class.java)
            ?.method()
            ?.getAnnotation(LongRunning::class.java)
            ?: return chain.proceed(chain.request())

        return chain
            .withReadTimeout(longRunning.readTimeoutSeconds, TimeUnit.SECONDS)
            .proceed(chain.request())
    }
}
