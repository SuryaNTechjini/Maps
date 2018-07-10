package com.example.techjini.maps;

import android.content.Context;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class GoogleAPICall  {

    private static GoogleAPIService  tripDirectionApiService;

    private static GoogleAPIService createApiService(final Context context) {

        Interceptor interceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                HttpUrl url = request.url().newBuilder().addQueryParameter("key", context.getString(R.string.google_maps_key)).build();
                request = request.newBuilder().url(url).build();
                return chain.proceed(request);
            }
        };

        HttpLoggingInterceptor interceptorLog = new HttpLoggingInterceptor();

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.interceptors().add(interceptor);
        builder.interceptors().add(interceptorLog);
        OkHttpClient client = builder.build();


        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://maps.googleapis.com/maps/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(client)
                .build();

        return retrofit.create(GoogleAPIService.class);
    }

    public synchronized static GoogleAPIService getGoogleAPI(Context context) {
        if (tripDirectionApiService == null) {
            tripDirectionApiService = createApiService(context);
        }

        return tripDirectionApiService;
    }
}
