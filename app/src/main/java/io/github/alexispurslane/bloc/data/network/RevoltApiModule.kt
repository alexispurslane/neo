package io.github.alexispurslane.bloc.data.network

import android.util.Log
import io.github.alexispurslane.bloc.data.network.models.AutumnFile
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
    copy.body?.writeTo(buffer)
    return buffer.readUtf8()
}

object RequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val stringBody = bodyToString(request)
        Log.d("QUERY NODE INTERCEPTOR", "Outgoing request to ${request.url}, body: \"${stringBody}\"")
        return chain.proceed(request)
    }
}

object RevoltApiModule {
    private var baseUrl: String? = null
    private var revoltApiService: RevoltApiService? = null
    private val okHttpClient by lazy {
        OkHttpClient().newBuilder().addInterceptor(RequestInterceptor).build()
    }

    fun setBaseUrl(newUrl: String): Boolean {
        if (newUrl != baseUrl) {
            baseUrl = newUrl
            revoltApiService = null
            return true
        }
        return false
    }

    fun service(): RevoltApiService {
        assert(this.baseUrl != null)
        if (revoltApiService == null) {
            revoltApiService = Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(this.baseUrl!!)
                .addConverterFactory(JacksonConverterFactory.create())
                .build()
                .create(RevoltApiService::class.java)
        }
        return revoltApiService!!
    }

    fun getResourceUrl(file: AutumnFile): String? {
        if (baseUrl != null) {
            val resourceUrl = baseUrl!!.replace("/api/", "/autumn/")
            return "${resourceUrl}/${file.fileTag}/${file.fileId}"
        } else {
            return null
        }
    }
}