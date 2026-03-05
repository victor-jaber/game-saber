# Jogo da Memória Bobo (Android)

MVP de jogo simples, offline e sem backend para publicação rápida na Play Store.

## O que já tem
- Jogo da memória com 16 cartas (8 pares)
- Cronômetro de 90 segundos
- Pontuação por desempenho
- Melhor pontuação salva localmente (SharedPreferences)
- Interface em português (pt-BR)

## Como abrir
1. Abra esta pasta no Android Studio.
2. Aguarde sincronização do Gradle.
3. Rode no emulador/dispositivo Android.

## Build de release (AAB)
1. No Android Studio: Build > Generate Signed Bundle / APK.
2. Escolha Android App Bundle (AAB).
3. Configure keystore e senha.
4. Gere a versão release.

## Publicação Play Store (checklist rápido)
- Criar app no Play Console
- Subir arquivo .aab
- Preencher descrição curta/longa em pt-BR
- Adicionar ícone, feature graphic e screenshots
- Definir classificação de conteúdo
- Informar política de privacidade (mesmo simples)
- Enviar para revisão

## Política de privacidade
- Template Markdown: `docs/privacy-policy-ptbr.md`
- Template HTML: `docs/privacy-policy-ptbr.html`
- Ajuste o e-mail de contato e publique o HTML em uma URL pública para usar no Play Console.

## Publicação na Play Store
- Guia passo a passo: `docs/play-store-publicacao-ptbr.md`
- Texto pronto da página da loja: `docs/play-store-listing-ptbr.md`
