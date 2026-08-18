package com.example.network.plantnet

import android.graphics.Bitmap
import com.example.BuildConfig
import com.example.network.ErrorType
import com.example.network.ResultState
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Service et Repository gérant l'intégration de l'API REST de Pl@ntNet.
 * Gère l'envoie multipart de la photo, le décodage des résultats et les erreurs HTTP spécifiques.
 */
class PlantNetRepository(
    private val apiService: PlantNetApiService = createApiService()
) {

    companion object {
        private const val BASE_URL = "https://my-api.plantnet.org/"
        private const val CONFIDENCE_THRESHOLD = 0.20 // Seuil minimal de certitude à 20%

        private fun createApiService(): PlantNetApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            return retrofit.create(PlantNetApiService::class.java)
        }
    }

    /**
     * Soumet une image sous forme de Bitmap à l'API Pl@ntNet et convertit la réponse en liste de PlantCandidate.
     */
    suspend fun identifyPlantFromBitmap(
        bitmap: Bitmap,
        organ: String = "auto"
    ): ResultState<List<PlantCandidate>> = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.PLANTNET_API_KEY } catch (_: Exception) { "" }
        if (apiKey.isBlank() || apiKey == "YOUR_PLANTNET_API_KEY_HERE" || apiKey == "MY_PLANTNET_API_KEY") {
            return@withContext ResultState.Error(
                message = "Clé d'accès API Pl@ntNet non configurée. Veuillez renseigner votre clé API Pl@ntNet dans le panneau Secrets d'AI Studio.",
                errorType = ErrorType.UNKNOWN
            )
        }

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val imageBytes = outputStream.toByteArray()

        val requestFile = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("images", "plant_photo.jpg", requestFile)
        val organPart = MultipartBody.Part.createFormData("organs", organ)

        return@withContext try {
            val response = apiService.identifyPlant(
                project = "all",
                apiKey = apiKey,
                lang = "fr",
                images = imagePart,
                organs = organPart
            )

            if (response.isSuccessful) {
                val body = response.body()
                val results = body?.results.orEmpty()

                if (results.isEmpty()) {
                    ResultState.Error(
                        message = "Pl@ntNet n'a détecté aucune espèce sur cette photo. Veuillez essayer avec un cadrage plus précis.",
                        errorType = ErrorType.DOCUMENT_NOT_FOUND
                    )
                } else {
                    val maxScore = results.maxOfOrNull { it.score } ?: 0.0

                    if (maxScore < CONFIDENCE_THRESHOLD) {
                        ResultState.Error(
                            message = "Niveau de certitude trop faible (${(maxScore * 100).toInt()}% < 20%). Veuillez reprendre la photo avec un meilleur cadrage et une meilleure luminosité.",
                            errorType = ErrorType.UNKNOWN
                        )
                    } else {
                        val candidates = results.mapNotNull { res ->
                            val species = res.species ?: return@mapNotNull null
                            val scientificNameClean = species.scientificNameWithoutAuthor?.trim()
                                ?: species.scientificName?.replace(Regex("\\b[A-Z]\\..*"), "")?.trim()
                                ?: "Espèce inconnue"
                            val fullScientificName = species.scientificName ?: scientificNameClean
                            val mainCommonName = species.commonNames?.firstOrNull()?.replaceFirstChar { it.uppercase() }
                                ?: species.genus?.scientificName
                                ?: scientificNameClean
                            val familyName = species.family?.scientificName ?: "Famille inconnue"
                            val referenceImage = res.images?.firstOrNull()?.url?.m
                                ?: res.images?.firstOrNull()?.url?.o

                            PlantCandidate(
                                scientificName = scientificNameClean,
                                fullScientificName = fullScientificName,
                                commonName = mainCommonName,
                                familyName = familyName,
                                confidencePercent = (res.score * 100).toInt().coerceIn(1, 99),
                                referenceImageUrl = referenceImage
                            )
                        }.take(5)

                        if (candidates.isEmpty()) {
                            ResultState.Error(
                                message = "Aucune espèce taxonomique valide renvoyée par Pl@ntNet.",
                                errorType = ErrorType.DESERIALIZATION
                            )
                        } else {
                            ResultState.Success(candidates)
                        }
                    }
                }
            } else {
                when (response.code()) {
                    401, 403 -> ResultState.Error(
                        message = "Clé API Pl@ntNet non valide ou accès refusé. Veuillez vérifier votre clé API.",
                        errorType = ErrorType.UNKNOWN
                    )
                    429 -> ResultState.Error(
                        message = "Le quota journalier de requêtes d'identification Pl@ntNet gratuit (500 requêtes/jour) a été atteint. Veuillez réessayer demain.",
                        errorType = ErrorType.UNKNOWN
                    )
                    400, 404 -> ResultState.Error(
                        message = "Pl@ntNet n'a pas pu analyser cette photo. Veuillez reprendre la photo plus nettement.",
                        errorType = ErrorType.DOCUMENT_NOT_FOUND
                    )
                    in 500..599 -> ResultState.Error(
                        message = "Le serveur Pl@ntNet rencontre un problème temporaire (Code ${response.code()}). Veuillez réessayer plus tard.",
                        errorType = ErrorType.UNKNOWN
                    )
                    else -> ResultState.Error(
                        message = "Erreur HTTP ${response.code()} lors de la connexion à Pl@ntNet.",
                        errorType = ErrorType.UNKNOWN
                    )
                }
            }
        } catch (e: UnknownHostException) {
            ResultState.Error(
                message = "Connexion réseau indisponible. L'identification via l'API Pl@ntNet nécessite une connexion Internet active.",
                cause = e,
                errorType = ErrorType.NETWORK_DISCONNECTED
            )
        } catch (e: SocketTimeoutException) {
            ResultState.Error(
                message = "Le délai d'attente réseau pour Pl@ntNet a expiré. Veuillez vérifier votre connexion et réessayer.",
                cause = e,
                errorType = ErrorType.NETWORK_DISCONNECTED
            )
        } catch (e: IOException) {
            ResultState.Error(
                message = "Erreur de transmission réseau lors de l'envoi de l'image à Pl@ntNet.",
                cause = e,
                errorType = ErrorType.NETWORK_DISCONNECTED
            )
        } catch (e: Exception) {
            ResultState.Error(
                message = "Une erreur imprévue est survenue lors de l'appel à Pl@ntNet.",
                cause = e,
                errorType = ErrorType.UNKNOWN
            )
        }
    }
}
