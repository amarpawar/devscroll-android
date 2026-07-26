package dev.devscroll.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// Matches the palette used by TerminalToast so the dialog reads as part of the same UI.
private val TerminalBg = Color(0xFF0F0F14)
private val TerminalMuted = Color(0xFF565F89)
private val TerminalText = Color(0xFFA9B1D6)
private val TerminalDanger = Color(0xFFF7768E)
private val TerminalPrompt = Color(0xFFBB9AF7)

/**
 * Confirmation for irreversible actions, styled to match [TerminalToast].
 *
 * The app has no other dialogs and a committed terminal aesthetic, so a stock
 * Material AlertDialog would be visually out of place.
 *
 * @param passwordValue non-null puts the dialog in re-authentication mode, requiring the
 *   user to retype their password before [onConfirm] can fire.
 */
@Composable
fun TerminalConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isBusy: Boolean = false,
    passwordValue: String? = null,
    onPasswordChange: (String) -> Unit = {},
    reauthHint: String? = null,
) {
    Dialog(onDismissRequest = { if (!isBusy) onDismiss() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalBg, RoundedCornerShape(6.dp))
                .border(1.5.dp, TerminalDanger.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).background(TerminalDanger, CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFE0AF68), CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(TerminalPrompt, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = TerminalMuted,
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "!",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TerminalDanger,
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TerminalText,
                    )
                }

                if (reauthHint != null) {
                    Text(
                        text = "// $reauthHint",
                        style = MaterialTheme.typography.labelSmall,
                        color = TerminalMuted,
                    )
                }

                if (passwordValue != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, TerminalMuted)
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = "$",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TerminalPrompt,
                        )
                        BasicTextField(
                            value = passwordValue,
                            onValueChange = onPasswordChange,
                            singleLine = true,
                            enabled = !isBusy,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            cursorBrush = SolidColor(TerminalPrompt),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DialogButton(
                        label = "cancel",
                        color = TerminalMuted,
                        enabled = !isBusy,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    DialogButton(
                        label = if (isBusy) "working..." else confirmLabel,
                        color = TerminalDanger,
                        // Reauth mode: no confirming on an empty password.
                        enabled = !isBusy && (passwordValue == null || passwordValue.isNotBlank()),
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val effective = if (enabled) color else color.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .border(1.dp, effective)
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = effective,
            fontWeight = FontWeight.Bold,
        )
    }
}
