package com.example.network

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.model.CareInstructions
import com.example.model.Plant
import com.example.model.PlantData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Résultat d'une analyse d'image par l'API Gemini Vision.
 */
sealed class GeminiResult {
    data class Success(val plant: Plant, val confidence: Int) : GeminiResult()
    data class NoPlantDetected(val reason: String) : GeminiResult()
    data class Error(val message: String) : GeminiResult()
}

/**
 * Repository responsable des appels à l'API Gemini Vision multimodal
 * pour l'identification automatique des espèces végétales.
 */
class GeminiPlantRepository {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Analyse une image Bitmap via l'API Gemini 3.5 Flash et renvoie le résultat sous forme structurée.
     */
    suspend fun identifyPlantFromImage(bitmap: Bitmap): GeminiResult = withContext(Dispatchers.IO) {
        // 1. Vérification sécurisée de la clé API
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "null") {
            return@withContext GeminiResult.Error(
                "Clé API Gemini non configurée.\n\nVeuillez configurer votre clé API Gemini dans le panneau Secrets de Google AI Studio pour activer l'identification visuelle par IA."
            )
        }

        try {
            // 2. Encodage de l'image en Base64 JPEG (redimensionnement préventif)
            val base64Image = bitmap.toResizedBase64(maxDimension = 1024)

            // 3. Prompt structuré exigeant une réponse JSON stricte
            val promptText = """
                Tu es un botaniste expert et pharmacognosiste spécialisé dans l'identification des plantes médicinales et aromatiques.
                Analyse la photo ci-jointe.

                Consignes impératives :
                1. Si la photo contient une plante, fleur, feuille, arbrisseau ou herbe :
                   Réponds UNIQUEMENT par un objet JSON respectant exactement cette structure :
                   {
                     "plantIdentified": true,
                     "name": "Nom commun usuel en français",
                     "scientificName": "Nom scientifique latin (ex: Lavandula angustifolia)",
                     "category": "Catégorie (ex: Plante Médicinale, Herbe Aromatique, Arbre Soigneur)",
                     "shortDescription": "Description concise de 2 phrases sur les propriétés et bienfaits majeurs.",
                     "fullDescription": "Description détaillée de l'histoire, des principes actifs et des vertus traditionnelles.",
                     "ailmentsAndBenefits": ["Problème ou vertu 1", "Problème ou vertu 2", "Problème ou vertu 3"],
                     "confidence": 92,
                     "careInstructions": {
                       "watering": "Recommandation d'arrosage",
                       "sunlight": "Exposition au soleil conseillée",
                       "difficulty": "Facile, Modéré ou Expert"
                     },
                     "errorMessage": null
                   }

                2. Si la photo NE contient PAS de plante ou si le sujet est complètement illisible :
                   Réponds UNIQUEMENT par cet objet JSON :
                   {
                     "plantIdentified": false,
                     "name": "",
                     "scientificName": "",
                     "category": "",
                     "shortDescription": "",
                     "fullDescription": "",
                     "ailmentsAndBenefits": [],
                     "confidence": 0,
                     "careInstructions": { "watering": "", "sunlight": "", "difficulty": "" },
                     "errorMessage": "Aucune plante n'a été détectée sur cette photo. Veuillez reprendre une photo bien cadrée de la plante."
                   }

                Ne rajoute aucun texte explicatif en dehors du JSON.
            """.trimIndent()

            // 4. Construction de la requête HTTP JSON pour l'API Gemini REST
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            // Part 1: Prompt textuel
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                            // Part 2: Image Base64
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val httpRequest = Request.Builder()
                .url(endpointUrl)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            // 5. Exécution de la requête réseau
            val httpResponse = okHttpClient.newCall(httpRequest).execute()

            if (!httpResponse.isSuccessful) {
                val errorCode = httpResponse.code
                val errorBody = httpResponse.body?.string() ?: ""
                return@withContext when (errorCode) {
                    400, 403 -> GeminiResult.Error(
                        "Clé API Gemini invalide ou droits insuffisants ($errorCode). Veuillez vérifier votre clé d'accès."
                    )
                    429 -> GeminiResult.Error(
                        "Quota de requêtes Gemini dépassé. Veuillez réespacer vos analyses."
                    )
                    500, 503 -> GeminiResult.Error(
                        "Le service Gemini AI est momentanément indisponible ($errorCode). Veuillez réespayer dans quelques secondes."
                    )
                    else -> GeminiResult.Error(
                        "Erreur réseau lors de l'appel Gemini (Code $errorCode) : $errorBody"
                    )
                }
            }

            val responseBodyString = httpResponse.body?.string()
                ?: return@withContext GeminiResult.Error("Réponse vide reçue du serveur Gemini.")

            // 6. Extraction et parsing sécurisé du JSON de la réponse Gemini
            val geminiResponseJson = JSONObject(responseBodyString)
            val candidates = geminiResponseJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext GeminiResult.Error("Aucune réponse générée par le modèle Gemini.")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

            if (rawText.isBlank()) {
                return@withContext GeminiResult.Error("Le modèle IA a renvoyé un contenu texte vide.")
            }

            // Nettoyage tolérant du texte JSON (au cas où il contienne du markdown ```json)
            val cleanedJsonText = cleanJsonText(rawText)

            val parsedData = JSONObject(cleanedJsonText)
            val plantIdentified = parsedData.optBoolean("plantIdentified", false)

            if (!plantIdentified) {
                val reason = parsedData.optString(
                    "errorMessage",
                    "Aucune plante n'a été identifiée sur la photo transmise."
                )
                return@withContext GeminiResult.NoPlantDetected(reason)
            }

            // Extraction des champs
            val name = parsedData.optString("name", "Plante Inconnue")
            val scientificName = parsedData.optString("scientificName", "Espèce non identifiée")
            val category = parsedData.optString("category", "Plante Médicinale")
            val shortDescription = parsedData.optString("shortDescription", "Analyse botanique réalisée via Gemini AI.")
            val fullDescription = parsedData.optString("fullDescription", shortDescription)
            val confidence = parsedData.optInt("confidence", 90)

            val ailmentsArray = parsedData.optJSONArray("ailmentsAndBenefits")
            val ailmentsList = mutableListOf<String>()
            if (ailmentsArray != null) {
                for (i in 0 until ailmentsArray.length()) {
                    ailmentsList.add(ailmentsArray.getString(i))
                }
            }
            if (ailmentsList.isEmpty()) {
                ailmentsList.add("Propriétés médicinales générales")
            }

            val careObj = parsedData.optJSONObject("careInstructions")
            val watering = careObj?.optString("watering", "Arrosage modéré") ?: "Arrosage modéré"
            val sunlight = careObj?.optString("sunlight", "Lumière naturelle") ?: "Lumière naturelle"
            val difficulty = careObj?.optString("difficulty", "Facile") ?: "Facile"

            // Vérifier si la plante correspond à une entrée de la base locale pour conserver l'harmonie des couleurs
            val existingMatch = PlantData.mockPlants.find {
                it.name.equals(name, ignoreCase = true) ||
                        it.scientificName.equals(scientificName, ignoreCase = true)
            }

            val plantId = existingMatch?.id ?: ("gemini_" + System.currentTimeMillis())
            val colorHex = existingMatch?.colorHex ?: 0xFF2E7D32 // Vert botanique par défaut

            val plant = Plant(
                id = plantId,
                name = name,
                scientificName = scientificName,
                category = category,
                shortDescription = shortDescription,
                fullDescription = fullDescription,
                ailmentsAndBenefits = ailmentsList,
                careInstructions = CareInstructions(
                    watering = watering,
                    sunlight = sunlight,
                    difficulty = difficulty
                ),
                colorHex = colorHex,
                matchedKeywords = ailmentsList + listOf(name, scientificName, category)
            )

            GeminiResult.Success(plant, confidence)

        } catch (e: SocketTimeoutException) {
            GeminiResult.Error("Temps d'attente dépassé (Timeout). L'analyse de l'image par Gemini a pris trop de temps.")
        } catch (e: UnknownHostException) {
            GeminiResult.Error("Impossible de contacter les serveurs Gemini. Veuillez vérifier votre connexion Internet.")
        } catch (e: org.json.JSONException) {
            GeminiResult.Error("Erreur de formatage lors du parsing du JSON renvoyé par le modèle Gemini.")
        } catch (e: Exception) {
            GeminiResult.Error("Une erreur inattendue est survenue lors du scan : ${e.localizedMessage}")
        }
    }

    /**
     * Nettoie une chaîne pour extraire un bloc JSON valide même si du texte ou markdown l'entoure.
     */
    private fun cleanJsonText(rawText: String): String {
        var text = rawText.trim()
        if (text.startsWith("```")) {
            text = text.substringAfter("\n")
        }
        if (text.endsWith("```")) {
            text = text.substringBeforeLast("```")
        }
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1)
        }
        return text.trim()
    }

    /**
     * Redimensionne et compresse une image Bitmap en Base64.
     */
    private fun Bitmap.toResizedBase64(maxDimension: Int): String {
        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
            val ratio = width.toFloat() / height.toFloat()
            val newWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
            val newHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
            Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
        } else {
            this
        }
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
