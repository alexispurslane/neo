package io.github.alexispurslane.bloc.data.network

import io.github.alexispurslane.bloc.data.network.models.LoginResponse
import io.github.alexispurslane.bloc.data.network.models.LoginRequest
import io.github.alexispurslane.bloc.data.network.models.QueryNodeResponse
import io.github.alexispurslane.bloc.data.network.models.UserProfile
import io.github.alexispurslane.bloc.data.network.models.RevoltUser
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Url

interface RevoltApiService {
    @GET
    suspend fun queryNode(@Url baseUrl: String): Response<QueryNodeResponse>


    // Have to duplicate this since the DEDUCTIVE type discriminator isn't in
    // this version of Jackson.
    @POST("auth/session/login")
    @Headers(
        "Accept: application/json",
        "Content-type: application/json",
    )
    suspend fun login(@Body login: LoginRequest.Basic): Response<LoginResponse>

    @POST("auth/session/login")
    @Headers(
        "Accept: application/json",
        "Content-type: application/json",
    )
    suspend fun login(@Body login: LoginRequest.MFA): Response<LoginResponse>

    @GET("users/{user_id}")
    suspend fun fetchUser(@Header("x-session-token") sessionToken: String, @Path("user_id") userId: String = "@me"): Response<RevoltUser>

    @GET("users/{user_id}/profile")
    suspend fun fetchUserProfile(@Header("x-session-token") sessionToken: String, @Path("user_id") userId: String = "@me"): Response<UserProfile>
}