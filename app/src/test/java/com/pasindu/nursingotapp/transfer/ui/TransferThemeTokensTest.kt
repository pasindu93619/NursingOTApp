package com.pasindu.nursingotapp.transfer.ui

import androidx.compose.ui.graphics.Color
import com.pasindu.nursingotapp.ui.theme.OnTransferDangerContainer
import com.pasindu.nursingotapp.ui.theme.OnTransferInfoContainer
import com.pasindu.nursingotapp.ui.theme.OnTransferSuccessContainer
import com.pasindu.nursingotapp.ui.theme.OnTransferThreeWayContainer
import com.pasindu.nursingotapp.ui.theme.OnTransferWarningContainer
import com.pasindu.nursingotapp.ui.theme.TransferAmberSoft
import com.pasindu.nursingotapp.ui.theme.TransferBlueSoft
import com.pasindu.nursingotapp.ui.theme.TransferCardBorder
import com.pasindu.nursingotapp.ui.theme.TransferCardSurface
import com.pasindu.nursingotapp.ui.theme.TransferDangerContainer
import com.pasindu.nursingotapp.ui.theme.TransferDarkSemanticColors
import com.pasindu.nursingotapp.ui.theme.TransferInfoContainer
import com.pasindu.nursingotapp.ui.theme.TransferInk
import com.pasindu.nursingotapp.ui.theme.TransferLightSemanticColors
import com.pasindu.nursingotapp.ui.theme.TransferMintSoft
import com.pasindu.nursingotapp.ui.theme.TransferPurpleSoft
import com.pasindu.nursingotapp.ui.theme.TransferRoseSoft
import com.pasindu.nursingotapp.ui.theme.TransferSlateSoft
import com.pasindu.nursingotapp.ui.theme.TransferSuccessContainer
import com.pasindu.nursingotapp.ui.theme.TransferSurfaceBackground
import com.pasindu.nursingotapp.ui.theme.TransferThreeWayContainer
import com.pasindu.nursingotapp.ui.theme.TransferWarningContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TransferThemeTokensTest {

    @Test
    fun `E1 - centralized transfer semantic tokens match authoritative design-system hex values`() {
        assertEquals(Color(0xFFF8FAFC), TransferSurfaceBackground)
        assertEquals(Color(0xFFFFFFFF), TransferCardSurface)
        assertEquals(Color(0xFFE2E8F0), TransferCardBorder)
        assertEquals(Color(0xFFEFF6FF), TransferInfoContainer)
        assertEquals(Color(0xFF1E3A8A), OnTransferInfoContainer)
        assertEquals(Color(0xFFF3E8FF), TransferThreeWayContainer)
        assertEquals(Color(0xFF581C87), OnTransferThreeWayContainer)
        assertEquals(Color(0xFFECFDF5), TransferSuccessContainer)
        assertEquals(Color(0xFF065F46), OnTransferSuccessContainer)
        assertEquals(Color(0xFFFFFBEB), TransferWarningContainer)
        assertEquals(Color(0xFF92400E), OnTransferWarningContainer)
        assertEquals(Color(0xFFFEF2F2), TransferDangerContainer)
        assertEquals(Color(0xFF991B1B), OnTransferDangerContainer)
    }

    @Test
    fun `E1 - shared soft pill and badge palette matches specification`() {
        assertEquals(Color(0xFFEAF6FF), TransferBlueSoft)
        assertEquals(Color(0xFFEAFBF5), TransferMintSoft)
        assertEquals(Color(0xFFFFF7ED), TransferAmberSoft)
        assertEquals(Color(0xFFFFF1F2), TransferRoseSoft)
        assertEquals(Color(0xFFF3EEFF), TransferPurpleSoft)
        assertEquals(Color(0xFFF1F5F9), TransferSlateSoft)
        assertEquals(Color(0xFF12204A), TransferInk)
    }

    @Test
    fun `E2 - dark semantic colors are defined for architectural readiness without overriding clinical light mode`() {
        assertNotNull(TransferLightSemanticColors)
        assertNotNull(TransferDarkSemanticColors)

        // Light mode is authoritative for clinical safety
        assertEquals(TransferSurfaceBackground, TransferLightSemanticColors.surfaceBackground)
        assertEquals(TransferCardSurface, TransferLightSemanticColors.cardSurface)

        // Dark tokens are prepared with distinct dark-mode surfaces
        assertEquals(Color(0xFF0B1120), TransferDarkSemanticColors.surfaceBackground)
        assertEquals(Color(0xFF111827), TransferDarkSemanticColors.cardSurface)
        assertNotEquals(TransferLightSemanticColors.surfaceBackground, TransferDarkSemanticColors.surfaceBackground)
    }
}
