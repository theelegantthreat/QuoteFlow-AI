package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.screens.StyledQuoteCard
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
  fun greeting_screenshot() {
    composeTestRule.setContent { 
      MyApplicationTheme { 
        StyledQuoteCard(
            text = "Artistic Flair style is now live.",
            author = "QuoteFlow Test",
            fontFamily = "Serif",
            fontSize = 20,
            textColor = "#FFFFFF",
            bgType = "IMAGE",
            bgValue = "img_art_mystic_abyss",
            alignment = "CENTER",
            showShadow = true,
            borderWidth = 1,
            borderColor = "#D0BCFF",
            borderRadius = 16,
            opacity = 1.0f,
            blur = 0
        )
      } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
