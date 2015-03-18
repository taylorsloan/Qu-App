package com.qu.qu.net;

import com.qu.qu.net.models.Question;

import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import rx.Observable;

/**
 * Created by Taylor on 3/15/2015.
 */
public interface QuEndpointsService {

    @GET("/questions/")
    Observable<Question> getRandomQuestion();

    @GET("/questions/{id}")
    Observable<Question> getQuestion(@Path("id") int id);

    @POST("/questions/create")
    Observable<Question> createQuestion(@Body Question question);

    @PUT("/questions/{id}/positive")
    Observable<Question> answerPositive(@Path("id") int id);

    @PUT("/questions/{id}/negative")
    Observable<Question> answerNegative(@Path("id") int id);
}
