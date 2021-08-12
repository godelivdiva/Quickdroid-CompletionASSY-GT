package com.quick.completionassygt.Rest;

import com.quick.completionassygt.Model.Login_Model;
import com.quick.completionassygt.Model.User_Model;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface API_Link {
    @FormUrlEncoded
    @POST("loginAndroid")
    Call<Login_Model> login(@Field("username") String user, @Field("password") String password);

    @FormUrlEncoded
    @POST("logUser")
    Call<User_Model> loguser(@Field("username") String user); //api untuk mengecek apakah user terdaftar
}
