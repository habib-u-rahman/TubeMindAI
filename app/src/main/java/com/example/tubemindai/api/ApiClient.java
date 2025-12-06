package com.example.tubemindai.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = ApiConfig.BASE_URL;
    
    private static Retrofit retrofit = null;
    private static ApiService apiService = null;

    public static Retrofit getRetrofit() {
        if (retrofit == null) {
            // Create logging interceptor
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Create OkHttp client with increased timeouts for note generation
            // Note generation can take 30-60 seconds, so we need longer timeouts
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)  // Connection timeout: 30 seconds
                    .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)     // Read timeout: 2 minutes (for note generation)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)    // Write timeout: 30 seconds
                    .addInterceptor(loggingInterceptor)
                    .build();

            // Create Retrofit instance
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        if (apiService == null) {
            apiService = getRetrofit().create(ApiService.class);
        }
        return apiService;
    }

    public static void setBaseUrl(String baseUrl) {
        retrofit = null;
        apiService = null;
        // Recreate with new base URL if needed
    }
}

