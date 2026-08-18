package com.example

import com.example.model.firestore.ScanHistoryDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitaires pour la gestion de l'historique des scans et du modèle ScanHistoryDocument.
 */
class ScanHistoryTest {

    @Test
    fun `test ScanHistoryDocument default values and instantiation`() {
        val now = System.currentTimeMillis()
        val doc = ScanHistoryDocument(
            id = "scan_123",
            uid = "user_abc",
            plantId = "plant_menthe",
            nomIdentifie = "Mentha piperita",
            commonName = "Menthe poivrée",
            scoreConfiance = 95,
            dateScan = now,
            imageThumbnailUrl = "https://example.com/menthe.jpg",
            verificationStatus = "VERIFIED_BY_PROFESSIONAL",
            scientificEvidenceLevel = "CLINICALLY_STUDIED"
        )

        assertEquals("scan_123", doc.id)
        assertEquals("user_abc", doc.uid)
        assertEquals("plant_menthe", doc.plantId)
        assertEquals("Mentha piperita", doc.nomIdentifie)
        assertEquals("Menthe poivrée", doc.commonName)
        assertEquals(95, doc.scoreConfiance)
        assertEquals(now, doc.dateScan)
        assertEquals("https://example.com/menthe.jpg", doc.imageThumbnailUrl)
        assertEquals("VERIFIED_BY_PROFESSIONAL", doc.verificationStatus)
        assertEquals("CLINICALLY_STUDIED", doc.scientificEvidenceLevel)
    }

    @Test
    fun `test ScanHistory list chronological sorting descending`() {
        val scan1 = ScanHistoryDocument(id = "1", dateScan = 1000L, nomIdentifie = "Plante A")
        val scan2 = ScanHistoryDocument(id = "2", dateScan = 3000L, nomIdentifie = "Plante B")
        val scan3 = ScanHistoryDocument(id = "3", dateScan = 2000L, nomIdentifie = "Plante C")

        val list = listOf(scan1, scan2, scan3)
        val sortedList = list.sortedByDescending { it.dateScan }

        assertEquals("2", sortedList[0].id) // 3000L
        assertEquals("3", sortedList[1].id) // 2000L
        assertEquals("1", sortedList[2].id) // 1000L
    }

    @Test
    fun `test ScanHistory unindexed plant identification`() {
        val unindexedScan = ScanHistoryDocument(
            id = "scan_456",
            uid = "user_xyz",
            plantId = null,
            nomIdentifie = "Taraxacum officinale",
            commonName = "Pissenlit",
            scoreConfiance = 82
        )

        assertEquals(null, unindexedScan.plantId)
        assertEquals("Taraxacum officinale", unindexedScan.nomIdentifie)
        assertEquals(82, unindexedScan.scoreConfiance)
    }
}
