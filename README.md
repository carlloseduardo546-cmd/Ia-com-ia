# IA com IA

Um app Android simples de conversa com o Gemini, feito com Kotlin e Jetpack Compose.

## O que já funciona

- Interface de chat em português;
- envio de mensagens para a API Gemini e exibição das respostas;
- histórico da conversa mantido enquanto o app está aberto;
- campo para informar a chave da API durante a sessão.

## Executar

1. Abra o projeto no Android Studio (versão compatível com Android Gradle Plugin 8.7.3) e aguarde a sincronização do Gradle.
2. Execute o app em um dispositivo ou emulador Android (Android 8.0/API 26 ou superior).
3. Crie uma chave no [Google AI Studio](https://aistudio.google.com/app/apikey), toque no ícone de configurações e informe a chave.
4. Envie uma mensagem. É necessária conexão com a internet.

## Segurança

Este é um protótipo que chama o Gemini diretamente do dispositivo. A chave fica apenas na memória durante a sessão, mas uma chave usada em um app distribuído pode ser extraída ou abusada. **Não publique uma chave pessoal nem distribua este app com uma chave embutida.** Para um produto real, faça as chamadas por um backend próprio, com autenticação, limites de uso e proteção da chave. Revogue a chave se ela for exposta.

## Personalizar o modelo

O modelo padrão está definido em `app/src/main/java/com/iamcomia/app/GeminiClient.kt`. Confira os modelos e limites atuais na documentação oficial do Gemini antes de publicar.
