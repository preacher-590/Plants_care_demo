package com.example.network.plantnet

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interface Retrofit pour interagir avec l'API REST de Pl@ntNet (v2).
 * Offre gratuite : 500 requêtes / jour.
 */
interface PlantNetApiService {

    /**
     * Soumet une image au moteur d'identification visuelle Pl@ntNet.
     */
    @Multipart
    @POST("v2/identify/{project}")
    suspend fun identifyPlant(
        @Path("project") project: String = "all",
        @Query("api-key") apiKey: String,
        @Query("lang") lang: String = "fr",
        @Part images: MultipartBody.Part,
        @Part organs: MultipartBody.Part? = null
    ): Response<PlantNetResponse>
}
