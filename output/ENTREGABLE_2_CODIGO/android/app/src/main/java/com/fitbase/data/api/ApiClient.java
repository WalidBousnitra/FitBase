package com.fitbase.data.api;

import com.fitbase.util.Constants;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * Cliente Retrofit singleton.
 * Conecta con Google Apps Script Web App.
 */
public class ApiClient {

    private static FitBaseApi instancia;

    public static FitBaseApi getApi() {
        if (instancia == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build();

            // Retrofit requiere que baseUrl termine en '/' — asegurar siempre
            String baseUrl = Constants.API_BASE_URL;
            if (!baseUrl.endsWith("/")) baseUrl = baseUrl + "/";

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            instancia = retrofit.create(FitBaseApi.class);
        }
        return instancia;
    }
}
