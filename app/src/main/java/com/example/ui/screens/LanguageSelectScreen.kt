package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.localization.AppLanguage
import com.example.data.localization.Strings
import com.example.ui.components.HomeaseLogoMark
import com.example.ui.components.PrimaryCtaButton
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.DeepIndigoContainer
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextSlateMuted

@Composable
fun LanguageSelectScreen(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onSkip: () -> Unit
) {
    var selectedLang by remember { mutableStateOf(currentLanguage) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar with skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.testTag("skip_language_button")
                ) {
                    Text(
                        text = Strings.get("skip", selectedLang),
                        color = TextSlateMuted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Main content
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HomeaseLogoMark(size = 72.dp)
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = Strings.get("choose_language", selectedLang),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSlate
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = Strings.get("choose_language_sub", selectedLang),
                    fontSize = 14.sp,
                    color = TextSlateMuted
                )
                Spacer(modifier = Modifier.height(32.dp))

                // English Card
                LanguageCard(
                    title = "English",
                    nativeSubtitle = "Default system language",
                    isSelected = selectedLang == AppLanguage.ENGLISH,
                    onClick = { selectedLang = AppLanguage.ENGLISH },
                    testTag = "lang_card_en"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Urdu Card
                LanguageCard(
                    title = "اردو",
                    nativeSubtitle = "قومی زبان (پاکستان)",
                    isSelected = selectedLang == AppLanguage.URDU,
                    onClick = { selectedLang = AppLanguage.URDU },
                    testTag = "lang_card_ur"
                )
            }

            // Bottom CTA
            PrimaryCtaButton(
                text = Strings.get("continue_btn", selectedLang),
                onClick = { onLanguageSelected(selectedLang) },
                testTag = "continue_language_btn"
            )
        }
    }
}

@Composable
private fun LanguageCard(
    title: String,
    nativeSubtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val borderColor = if (isSelected) DeepIndigo else BorderStroke
    val bgColor = if (isSelected) DeepIndigoContainer else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(20.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) DeepIndigo else TextSlate
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = nativeSubtitle,
                fontSize = 13.sp,
                color = TextSlateMuted
            )
        }

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isSelected) DeepIndigo else Color.Transparent)
                .border(
                    width = 2.dp,
                    color = if (isSelected) DeepIndigo else BorderStroke,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
