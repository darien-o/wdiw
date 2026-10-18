package io.github.darieno.wdiw.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import io.github.darieno.wdiw.ui.theme.Ink
import io.github.darieno.wdiw.ui.theme.Paper

/** A QR code for a phone camera. Drawn dark on a light tile, which is what scanners expect. */
@Composable
fun QrCode(text: String, description: String, modifier: Modifier = Modifier, size: Dp = 148.dp) {
    val matrix = remember(text) {
        QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 0))
    }
    Canvas(
        modifier
            .size(size)
            .background(Paper, RoundedCornerShape(14.dp))
            .padding(12.dp)
            .semantics { contentDescription = description },
    ) {
        val cell = this.size.width / matrix.width
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) {
                    // Overlap by a hair so neighbouring cells leave no seams after rounding.
                    drawRect(Ink, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
                }
            }
        }
    }
}
