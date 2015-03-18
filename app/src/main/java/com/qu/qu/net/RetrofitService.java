package com.qu.qu.net;

import com.qu.qu.AppConstants;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

public class RetrofitService {

    public static QuEndpointsService createRestAdapter() {
        RequestInterceptor requestInterceptor = request -> {
            request.addHeader("Content-Type", "application/json");
            request.addHeader("User-Agent", "Qu-Android-App");
            request.addHeader("Api-Key", AppConstants.API_KEY);
        };
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(AppConstants.SERVER_URL)
                .setRequestInterceptor(requestInterceptor)
                .build();
        return restAdapter.create(QuEndpointsService.class);
    }
}
