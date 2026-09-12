package com.example.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VerifyRequest(val txId: String, val planId: String)

@JsonClass(generateAdapter = true)
data class VerifyResponse(val success: Boolean, val message: String?, val error: String?, val planId: String?, val durationDays: Int?)

interface PremiumApi {
    @POST("verify-transaction")
    suspend fun verifyTransaction(@Body request: VerifyRequest): VerifyResponse

    companion object {
        // REPLACE WITH YOUR BACKEND URL ONCE DEPLOYED!
        private const val BASE_URL = "https://your-backend-url.com/"

        fun create(): PremiumApi {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
            return retrofit.create(PremiumApi::class.java)
        }
    }
}
