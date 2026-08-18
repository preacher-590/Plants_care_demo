package com.example.network.wikimedia

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

/**
 * Candidat d'image avec attribution légale Creative Commons en provenance de Wikimedia Commons.
 */
data class WikimediaImageCandidate(
    val title: String,
    val thumbnailUrl: String,
    val fullUrl: String,
    val author: String,
    val license: String
)

/**
 * Résultat de recherche scellé pour l'API Wikimedia Commons.
 */
sealed class WikimediaSearchResult {
    data class Success(
        val candidates: List<WikimediaImageCandidate>,
        val excludedCount: Int = 0
    ) : WikimediaSearchResult()

    data class NetworkError(
        val message: String,
        val cause: Throwable? = null
    ) : WikimediaSearchResult()

    data class ParseError(
        val message: String,
        val cause: Throwable? = null
    ) : WikimediaSearchResult()
}

/**
 * Service réseau interagissant avec l'API publique de Wikimedia Commons.
 * Permet à l'administrateur de rechercher des photographies libres de droits pour les plantes de la base.
 */
object WikimediaService {

    private const val TAG = "WikimediaService"
    private val client = OkHttpClient()

    /**
     * Recherche des images sur Wikimedia Commons pour un nom de plante (scientifique ou commun).
     * Retourne un [WikimediaSearchResult] scellé (Success, NetworkError ou ParseError).
     * Les candidats ne disposant pas d'un auteur ET d'une licence valides sont strictement exclus.
     */
    suspend fun searchPlantImages(query: String, limit: Int = 5): WikimediaSearchResult = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext WikimediaSearchResult.Success(emptyList(), 0)

        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://commons.wikimedia.org/w/api.php?action=query&format=json&generator=search&gsrsearch=$encodedQuery&gsrnamespace=6&gsrlimit=$limit&prop=imageinfo&iiprop=url|extmetadata|mime&iiurlwidth=600&origin=*"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PlantCareAndroidApp/1.0 (contact@plantcare.example.com)")
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (e: IOException) {
                return@withContext WikimediaSearchResult.NetworkError("Problème de connexion réseau avec Wikimedia Commons.", e)
            }

            if (!response.isSuccessful) {
                return@withContext WikimediaSearchResult.NetworkError("Erreur serveur Wikimedia Commons (code HTTP ${response.code}).")
            }

            val responseBody = response.body?.string()
                ?: return@withContext WikimediaSearchResult.ParseError("La réponse réseau de Wikimedia est vide.")

            val json = try {
                JSONObject(responseBody)
            } catch (e: JSONException) {
                return@withContext WikimediaSearchResult.ParseError("Erreur d'analyse JSON de la réponse Wikimedia.", e)
            }

            val queryObj = json.optJSONObject("query")
                ?: return@withContext WikimediaSearchResult.Success(emptyList(), 0)
            val pagesObj = queryObj.optJSONObject("pages")
                ?: return@withContext WikimediaSearchResult.Success(emptyList(), 0)

            val candidates = mutableListOf<WikimediaImageCandidate>()
            var excludedCount = 0
            val keys = pagesObj.keys()

            while (keys.hasNext()) {
                val pageId = keys.next()
                val page = pagesObj.optJSONObject(pageId) ?: continue
                val title = page.optString("title", "")
                val imageInfoArray = page.optJSONArray("imageinfo") ?: continue

                if (imageInfoArray.length() > 0) {
                    val info = imageInfoArray.getJSONObject(0)
                    val mime = info.optString("mime", "")

                    // Ignorer les fichiers SVG, audio ou formats non-image
                    if (mime.contains("svg") || (!mime.contains("image/jpeg") && !mime.contains("image/png") && !mime.contains("image/webp"))) {
                        continue
                    }

                    val thumbUrl = info.optString("thumburl", info.optString("url", ""))
                    val fullUrl = info.optString("url", "")
                    val extmetadata = info.optJSONObject("extmetadata")

                    var author: String? = null
                    var license: String? = null

                    if (extmetadata != null) {
                        val artistObj = extmetadata.optJSONObject("Artist")
                        if (artistObj != null) {
                            val rawArtist = artistObj.optString("value", "")
                            if (rawArtist.isNotBlank()) {
                                author = stripHtml(rawArtist)
                            }
                        }

                        val licenseObj = extmetadata.optJSONObject("LicenseShortName")
                        if (licenseObj != null) {
                            val rawLicense = licenseObj.optString("value", "")
                            if (rawLicense.isNotBlank()) {
                                license = rawLicense
                            }
                        }
                    }

                    // Exclure strictement tout candidat sans attribution complète (auteur et licence non nulls et non vides)
                    if (author.isNullOrBlank() || license.isNullOrBlank()) {
                        excludedCount++
                        Log.d(TAG, "Candidat exclu pour attribution incomplète : title='$title', author='$author', license='$license'")
                        continue
                    }

                    if (thumbUrl.isNotBlank()) {
                        candidates.add(
                            WikimediaImageCandidate(
                                title = title.replace("File:", "").trim(),
                                thumbnailUrl = thumbUrl,
                                fullUrl = fullUrl,
                                author = author,
                                license = license
                            )
                        )
                    }
                }
            }
            Log.d(TAG, "Recherche Wikimedia terminée : ${candidates.size} candidat(s) valide(s), $excludedCount exclu(s) pour attribution incomplète.")
            WikimediaSearchResult.Success(candidates, excludedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inattendue lors de la recherche Wikimedia", e)
            WikimediaSearchResult.ParseError("Erreur inattendue lors du traitement des données Wikimedia.", e)
        }
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "").replace("&nbsp;", " ").trim()
    }
}
