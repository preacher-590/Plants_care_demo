package com.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.model.firestore.ScientificEvidenceLevel
import com.example.ui.components.EvidenceLevelBadge
import com.example.ui.theme.PlantCareTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun evidence_level_badge_screenshot() {
    composeTestRule.setContent {
      PlantCareTheme {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          EvidenceLevelBadge(
            level = ScientificEvidenceLevel.CLINICALLY_STUDIED,
            isCompact = false
          )
          EvidenceLevelBadge(
            level = ScientificEvidenceLevel.TRADITIONAL_USE,
            isCompact = false
          )
          EvidenceLevelBadge(
            level = ScientificEvidenceLevel.CLINICALLY_STUDIED,
            isCompact = true
          )
          EvidenceLevelBadge(
            level = ScientificEvidenceLevel.TRADITIONAL_USE,
            isCompact = true
          )
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
