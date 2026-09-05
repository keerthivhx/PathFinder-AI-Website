package com.example

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.repository.BuildingRepository
import com.example.ui.components.IndoorMapCanvas
import com.example.ui.theme.MyApplicationTheme
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
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun app_map_screenshot() {
    val repo = BuildingRepository()
    val hosp = repo.getBuildingById("hosp")!!

    composeTestRule.setContent {
      MyApplicationTheme {
        IndoorMapCanvas(
          building = hosp,
          activeFloor = 0,
          startNode = hosp.nodes.first(),
          destinationNode = hosp.nodes.getOrNull(2),
          activeRoute = null,
          activeStepIndex = 0,
          onNodeClicked = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/indoor_map.png")
  }
}

