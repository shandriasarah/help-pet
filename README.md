# 🐾 HelpPet

Aplicativo Android que conecta a comunidade para **encontrar pets desaparecidos**, **avisar quem perdeu um animal** e **facilitar a adoção responsável**.

Projeto acadêmico desenvolvido em Kotlin com Jetpack Compose e Firebase.

---

## Funcionalidades

| Área | O que faz |
|---|---|
| **Conta** | Cadastro e login com e-mail e senha ou com a conta Google, recuperação de senha por e-mail |
| **Perdi um animal** | Publica um alerta com foto, características, local onde foi visto e WhatsApp do dono |
| **Encontrei um animal** | Registra um animal achado ou marca um pet da lista de desaparecidos como encontrado, avisando o dono em particular |
| **Colocar para adoção** | Publica um pet para adoção com idade, saúde e história |
| **Quero adotar** | Registra o perfil de quem quer adotar e sugere pets compatíveis |
| **Início** | Busca por nome, raça ou bairro, filtro por espécie e grade com todos os pets |
| **Animais** | Abas de Desaparecidos, Achados e Adoção |
| **Mapa** | Mostra a região aproximada dos pets desaparecidos |
| **Perfil do pet** | Foto, informações, características e contato direto pelo WhatsApp |
| **Compartilhar** | Envia a foto e as informações do pet pelo WhatsApp, Instagram ou outro app |
| **Notificações** | Alertas da comunidade e avisos privados quando alguém encontra o seu pet |
| **Meu perfil** | Meus anúncios (editar e excluir), alertas por região, termos e exclusão da conta |

## Tecnologias

- **Linguagem:** Kotlin
- **Interface:** Jetpack Compose (Material 3)
- **Autenticação:** Firebase Authentication (e-mail/senha e Google)
- **Banco de dados:** Cloud Firestore
- **Hospedagem de imagens:** ImgBB
- **Mapa:** osmdroid com mapa de ruas da ArcGIS
- **Imagens:** Coil
- **Requisitos:** Android 7.0 (API 24) ou superior

## Como rodar

1. Clone o repositório e abra a pasta no **Android Studio**.
2. Confirme que o arquivo `app/google-services.json` está presente.
3. Clique em **Sync Project with Gradle Files**.
4. Escolha um emulador ou celular e clique em **Run**.

O passo a passo completo, incluindo a configuração do login com Google, está no [Guia de instalação](docs/guia-de-instalacao.md).

## Documentação

| Documento | Conteúdo |
|---|---|
| [Visão geral](docs/visao-geral.md) | Proposta, problema, público-alvo e objetivos |
| [Requisitos](docs/requisitos.md) | Requisitos funcionais, não funcionais e regras de negócio |
| [Casos de uso](docs/casos-de-uso.md) | Atores, diagrama e fluxos principais |
| [Arquitetura](docs/arquitetura.md) | Estrutura do código, telas, navegação e integrações |
| [Banco de dados](docs/banco-de-dados.md) | Coleções do Firestore e seus campos |
| [Manual do usuário](docs/manual-do-usuario.md) | Como usar cada tela do aplicativo |
| [Guia de instalação](docs/guia-de-instalacao.md) | Ambiente de desenvolvimento, Firebase e instalação do APK |
| [Testes](docs/testes.md) | Testes automatizados e roteiro de testes manuais |
| [Limitações e próximos passos](docs/limitacoes-e-melhorias.md) | O que ainda pode evoluir |

## Equipe

Desenvolvido por Yan Gabriel e equipe.
