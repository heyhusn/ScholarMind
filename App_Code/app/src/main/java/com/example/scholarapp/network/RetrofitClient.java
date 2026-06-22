package com.example.scholarapp.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // ⚠️ Configure the FastAPI Server URL here:
    // - For Android Emulator, use: "http://10.0.2.2:8000/" (points to your PC's localhost)
    // - For a Real Device (on same Wi-Fi), use your PC's local IP, e.g.: "http://192.168.1.100:8000/"
    // - For a Real Device (over Internet), run 'ngrok http 8000' on your PC and paste the new URL, e.g.: "https://your-new-subdomain.ngrok-free.app/"
    private static final String BASE_URL = NetworkConfig.API_BASE_URL;

    private static ApiService apiService = null;

    public static ApiService getApiService() {
        if (apiService == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(NetworkConfig.ENABLE_HTTP_LOGGING
                    ? HttpLoggingInterceptor.Level.BODY
                    : HttpLoggingInterceptor.Level.NONE);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(180, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }
}
