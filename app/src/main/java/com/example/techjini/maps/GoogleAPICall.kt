package com.example.techjini.maps

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.jvm.Synchronized
import kotlin.jvm.java

object GoogleAPICall {


    private var tripDirectionApiService: GoogleAPIService? = null

    private fun createApiService(context: Context): GoogleAPIService {

        val interceptor = object : Interceptor{
            override fun intercept(chain: Interceptor.Chain?): Response? {
                var request = chain?.request()
                val url = request?.url()?.newBuilder()?.addQueryParameter("key", context.getString(R.string.google_maps_key))?.build()
                request = request?.newBuilder()?.url(url)?.build()
                return  chain?.proceed(request)
            }

        }

        val interceptorLog = HttpLoggingInterceptor()

        val builder = OkHttpClient.Builder()
        builder.interceptors().add(interceptor)
        builder.interceptors().add(interceptorLog)
        val client = builder.build()


        val retrofit = Retrofit.Builder().baseUrl("https://maps.googleapis.com/maps/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(client)
                .build()

        return retrofit.create(GoogleAPIService::class.java)
    }

    @Synchronized
    fun getGoogleAPI(context: Context): GoogleAPIService ?{
        if (tripDirectionApiService == null) {
            tripDirectionApiService = createApiService(context)
        }

        return tripDirectionApiService
    }
}
