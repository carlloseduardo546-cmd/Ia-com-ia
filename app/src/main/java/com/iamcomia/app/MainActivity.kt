package com.iamcomia.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

internal enum class MessageRole { USER, ASSISTANT, ERROR }
internal data class ChatMessage(val role: MessageRole, val text: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFF6750A4),
                secondary = Color(0xFF625B71),
                background = Color(0xFFF8F7FC),
                surface = Color(0xFFF8F7FC)
            )) {
                ChatScreen()
            }
        }
    }
}

@Composable
private fun ChatScreen() {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var draft by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val client = remember { GeminiClient() }

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    suspend fun sendMessage() {
        val text = draft.trim()
        if (text.isEmpty() || isLoading) return
        if (apiKey.isBlank()) {
            showSettings = true
            return
        }
        messages.add(ChatMessage(MessageRole.USER, text))
        draft = ""
        isLoading = true
        try {
            val answer = client.reply(apiKey, messages.toList())
            messages.add(ChatMessage(MessageRole.ASSISTANT, answer))
        } catch (error: Exception) {
            messages.add(ChatMessage(MessageRole.ERROR, error.message ?: "Não foi possível conectar. Tente novamente."))
        } finally {
            isLoading = false
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF8F7FC)).imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text("IA com IA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Seu assistente Gemini", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Configurações")
            }
            IconButton(onClick = { messages.clear() }) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Limpar conversa")
            }
        }
        Surface(Modifier.fillMaxWidth().height(1.dp), color = Color(0xFFE7E2EC)) {}

        if (messages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Olá! 👋", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("O que você gostaria de saber?", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message -> MessageBubble(message) }
                if (isLoading) item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Pensando…", color = Color.Gray)
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                tonalElevation = 1.dp
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    enabled = !isLoading,
                    maxLines = 5,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF1D1B20)),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) Text("Escreva sua mensagem…", color = Color.Gray)
                        inner()
                    }
                )
            }
            Spacer(Modifier.size(8.dp))
            IconButton(
                onClick = { scope.launch { sendMessage() } },
                enabled = draft.isNotBlank() && !isLoading,
                modifier = Modifier.size(50.dp).background(
                    if (draft.isNotBlank() && !isLoading) Color(0xFF6750A4) else Color(0xFFD8D2E0),
                    CircleShape
                )
            ) {
                Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.White)
            }
        }
    }

    if (showSettings) {
        var keyDraft by remember(apiKey) { mutableStateOf(apiKey) }
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Configurar Gemini") },
            text = {
                Column {
                    Text("Cole sua chave do Google AI Studio. Ela fica apenas na memória enquanto o app está aberto.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF1EFF5)) {
                        BasicTextField(
                            value = keyDraft,
                            onValueChange = { keyDraft = it },
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            decorationBox = { inner ->
                                if (keyDraft.isEmpty()) Text("Chave da API", color = Color.Gray)
                                inner()
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { apiKey = keyDraft.trim(); showSettings = false }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { apiKey = ""; showSettings = false }) { Text("Remover chave") }
            }
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val bubbleColor = when (message.role) {
        MessageRole.USER -> Color(0xFFE9DDF7)
        MessageRole.ASSISTANT -> Color.White
        MessageRole.ERROR -> Color(0xFFFFEDEA)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Surface(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.88f else 0.96f),
            shape = RoundedCornerShape(20.dp),
            color = bubbleColor
        ) {
            Text(message.text, Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = Color(0xFF24212A))
        }
    }
}
