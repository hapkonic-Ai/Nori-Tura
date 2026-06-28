package com.example.nori_tura.presentation.surgeon

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nori_tura.presentation.components.BrandTopBar
import com.example.nori_tura.presentation.components.ErrorState
import com.example.nori_tura.presentation.components.LoadingState
import com.example.nori_tura.presentation.components.NorituraScaffold
import com.example.nori_tura.ui.theme.NorituraColors

@Composable
fun WhatsAppPreviewScreen(
    recordId: String,
    onBack: () -> Unit,
    viewModel: WhatsAppPreviewViewModel = viewModel(key = recordId) { WhatsAppPreviewViewModel(recordId) }
) {
    val uiState by viewModel.uiState.collectAsState()

    NorituraScaffold(
        topBar = {
            BrandTopBar(
                initials = "DR",
                title = "Preview Message",
                onBack = onBack,
                notificationCount = 0
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NorituraColors.Background)
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            when (val state = uiState) {
                is WhatsAppPreviewViewModel.UiState.Loading -> {
                    LoadingState(modifier = Modifier.fillMaxSize())
                }
                is WhatsAppPreviewViewModel.UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadPreview() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is WhatsAppPreviewViewModel.UiState.Success -> {
                    PreviewContent(
                        state = state,
                        onMessageChanged = viewModel::onMessageChanged,
                        onSend = viewModel::sendMessage
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PreviewContent(
    state: WhatsAppPreviewViewModel.UiState.Success,
    onMessageChanged: (String) -> Unit,
    onSend: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    val preview = state.preview

    PhoneMockup(body = state.editedBody)

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedButton(
        onClick = { isEditing = !isEditing },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(if (isEditing) "Done Editing" else "Edit Message")
    }

    if (isEditing) {
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.editedBody,
            onValueChange = onMessageChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Message text") },
            minLines = 6,
            maxLines = 12,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    SendFeedback(state.sendState)

    Spacer(modifier = Modifier.height(12.dp))

    Button(
        onClick = { onSend("whatsapp") },
        modifier = Modifier.fillMaxWidth(),
        enabled = preview.canSendWhatsApp && state.sendState !is WhatsAppPreviewViewModel.SendState.Sending,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text("Send via WhatsApp")
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
        onClick = { onSend("sms") },
        modifier = Modifier.fillMaxWidth(),
        enabled = preview.canSendSms && state.sendState !is WhatsAppPreviewViewModel.SendState.Sending,
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Message,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text("Send via SMS")
    }

    if (!preview.canSendWhatsApp && !preview.canSendSms) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "WhatsApp and SMS are not configured on the backend. Messages will be logged as stubs.",
            color = NorituraColors.Warning,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PhoneMockup(body: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF1F2C34))
            .padding(12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0B141A))
                .padding(horizontal = 16.dp)
                .padding(top = 28.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(Color.Black)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF25D366)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "N",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Noni Tura",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "+91 .....",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 16.dp
                ),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF005C4B))
            ) {
                Text(
                    text = body,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun SendFeedback(sendState: WhatsAppPreviewViewModel.SendState) {
    when (sendState) {
        is WhatsAppPreviewViewModel.SendState.Sending -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sending...", color = NorituraColors.TextSecondary)
            }
        }
        is WhatsAppPreviewViewModel.SendState.Sent -> {
            Text(
                text = "✅ Message sent via ${sendState.response.channel}",
                color = NorituraColors.PostOp,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
        }
        is WhatsAppPreviewViewModel.SendState.Failed -> {
            Text(
                text = "❌ ${sendState.message}",
                color = NorituraColors.Error,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
        }
        else -> {}
    }
}
