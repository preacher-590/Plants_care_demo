package com.example

import com.example.model.firestore.ScientificEvidenceLevel
import com.example.model.firestore.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class EvidenceLevelTest {

    @Test
    fun `test ScientificEvidenceLevel parsing from Firestore string values`() {
        assertEquals(ScientificEvidenceLevel.CLINICALLY_STUDIED, ScientificEvidenceLevel.fromValue("etudie_cliniquement"))
        assertEquals(ScientificEvidenceLevel.CLINICALLY_STUDIED, ScientificEvidenceLevel.fromValue("clinically_studied"))
        assertEquals(ScientificEvidenceLevel.CLINICALLY_STUDIED, ScientificEvidenceLevel.fromValue("étudié cliniquement"))
        
        assertEquals(ScientificEvidenceLevel.TRADITIONAL_USE, ScientificEvidenceLevel.fromValue("usage_traditionnel"))
        assertEquals(ScientificEvidenceLevel.TRADITIONAL_USE, ScientificEvidenceLevel.fromValue("unknown_value"))
        assertEquals(ScientificEvidenceLevel.TRADITIONAL_USE, ScientificEvidenceLevel.fromValue(null))
    }

    @Test
    fun `test sorting priority by verification status then scientific evidence level`() {
        data class MockItem(
            val name: String,
            val verificationStatus: VerificationStatus,
            val evidenceLevel: ScientificEvidenceLevel
        )

        val items = listOf(
            MockItem("Plante A (Traditionnel, Pro)", VerificationStatus.VERIFIED_BY_PROFESSIONAL, ScientificEvidenceLevel.TRADITIONAL_USE),
            MockItem("Plante B (Clinique, Pro)", VerificationStatus.VERIFIED_BY_PROFESSIONAL, ScientificEvidenceLevel.CLINICALLY_STUDIED),
            MockItem("Plante C (Clinique, Révision)", VerificationStatus.UNDER_REVIEW, ScientificEvidenceLevel.CLINICALLY_STUDIED),
            MockItem("Plante D (Traditionnel, Non vérifié)", VerificationStatus.UNVERIFIED, ScientificEvidenceLevel.TRADITIONAL_USE)
        )

        val sorted = items.sortedWith(
            compareByDescending<MockItem> {
                when (it.verificationStatus) {
                    VerificationStatus.VERIFIED_BY_PROFESSIONAL -> 2
                    VerificationStatus.UNDER_REVIEW -> 1
                    else -> 0
                }
            }.thenByDescending {
                when (it.evidenceLevel) {
                    ScientificEvidenceLevel.CLINICALLY_STUDIED -> 1
                    ScientificEvidenceLevel.TRADITIONAL_USE -> 0
                }
            }
        )

        // Plante B should be first because it is verified by pro and clinically studied
        assertEquals("Plante B (Clinique, Pro)", sorted[0].name)
        // Plante A should be second (verified by pro, traditional)
        assertEquals("Plante A (Traditionnel, Pro)", sorted[1].name)
        // Plante C should be third (under review)
        assertEquals("Plante C (Clinique, Révision)", sorted[2].name)
        // Plante D should be fourth (unverified)
        assertEquals("Plante D (Traditionnel, Non vérifié)", sorted[3].name)
    }
}
