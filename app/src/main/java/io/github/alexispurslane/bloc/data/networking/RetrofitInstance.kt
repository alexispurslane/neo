package io.github.alexispurslane.bloc.data.networking

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
fun bodyToString(request: Request): String {
    val copy = request.newBuilder().build()
    val buffer = Buffer()
    copy.body()?.writeTo(buffer)
    return buffer.readUtf8()
}

object RequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val stringBody = bodyToString(request)
        Log.d("QUERY NODE INTERCEPTOR", "Outgoing request to ${request.url()}, body: \"${stringBody}\"")
        return chain.proceed(request)
    }
}

object RetrofitInstance {
    private var baseUrl: String? = null;
    private var retrofitInstance: Retrofit? = null;
    private var revoltApiService: RevoltApiService? = null;
    private val okHttpClient by lazy {
        OkHttpClient().newBuilder().addInterceptor(RequestInterceptor).build()
    }

    fun revoltApiService(baseUrl: String): RevoltApiService {
        if (revoltApiService == null || this.baseUrl != baseUrl) {
            val rfi = Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(baseUrl)
                .addConverterFactory(JacksonConverterFactory.create())
                .build()
            val ras = rfi.create(RevoltApiService::class.java)

            this.baseUrl = baseUrl
            retrofitInstance = rfi
            revoltApiService = ras
        }
        return revoltApiService!!
    }
}