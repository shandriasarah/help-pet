# Guia de instalação

## 1. Ambiente de desenvolvimento

### Requisitos
- **Android Studio** atualizado.
- **JDK** compatível com o Android Gradle Plugin 9.3 (o Android Studio já traz um).
- **Git**.
- Um emulador Android ou um celular/tablet Android 7.0 ou superior.

### Passos
1. Clone o repositório:
   ```bash
   git clone https://github.com/yansoarees/help-pet.git
   ```
2. No Android Studio: **File → Open** → selecione a pasta `help-pet`.
3. Confirme que o arquivo `app/google-services.json` existe.
4. Clique em **Sync Project with Gradle Files** (ícone do elefante) e aguarde.
5. Na barra superior, selecione a configuração **app** e um dispositivo.
6. Clique em **Run**.

## 2. Configuração do Firebase

O projeto usa o Firebase **helppet-c8ac9**. Para o **login com Google** funcionar, a impressão digital (SHA-1) de cada computador que compila o app precisa estar cadastrada:

1. No terminal do Android Studio, rode:
   ```bash
   ./gradlew signingReport
   ```
   No Windows: `.\gradlew signingReport`.
2. Copie o valor **SHA1** da variante `debug`.
3. No [console do Firebase](https://console.firebase.google.com): **Configurações do projeto → Seus apps → HelpPet (Android) → Adicionar impressão digital** → cole o SHA-1 → **Salvar**.

Sem esse cadastro, o login por e-mail e senha continua funcionando normalmente.

### Métodos de login ativos
Em **Authentication → Sign-in method**, devem estar ativados **E-mail/senha** e **Google**.

## 3. Executando em um celular ou tablet

### Pelo Android Studio (cabo USB)
1. No aparelho: **Configurações → Sobre o telefone** → toque 7 vezes em **Número da versão**.
2. **Configurações → Opções do desenvolvedor** → ative **Depuração USB**.
3. Conecte o cabo, aceite o aviso no aparelho e selecione-o no Android Studio.
4. Clique em **Run**.

### Instalando o APK
1. Gere o APK: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. O arquivo fica em `app/build/outputs/apk/debug/app-debug.apk`.
2. Envie o arquivo para o aparelho (cabo, Google Drive ou WhatsApp).
3. Abra o arquivo e permita **instalar apps desconhecidos** quando o Android pedir.
4. Toque em **Instalar**. Se o Play Protect avisar, toque em **Mais detalhes → Instalar mesmo assim**.

## 4. Testes

Para rodar os testes automatizados:
```bash
./gradlew testDebugUnitTest
```
Mais detalhes em [Testes](testes.md).

## 5. Problemas comuns

| Problema | Solução |
|---|---|
| "No target device found" | Ligue um emulador (**Device Manager**) ou conecte um aparelho com depuração USB. |
| Botão **Run** desativado | Rode **Sync Project with Gradle Files** e confira se a configuração **app** está selecionada. |
| Login com Google não funciona | Cadastre o SHA-1 do computador no Firebase (seção 2). |
| "App não instalado" ao instalar o APK | Desinstale a versão anterior que foi instalada por outro computador e instale de novo. |
| Mapa sem ruas | Verifique a conexão com a internet; os blocos do mapa são baixados na hora. |
