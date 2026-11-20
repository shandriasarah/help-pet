# Limitações e próximos passos

## Limitações conhecidas

| Limitação | Impacto | Possível solução |
|---|---|---|
| **Localização aproximada no mapa** | O marcador fica no centro do bairro citado na descrição, não no ponto exato. Bairros não reconhecidos vão para o centro do Recife. | Pegar a localização do aparelho (GPS) no formulário e salvar latitude e longitude no anúncio. |
| **Notificações só com o app aberto** | Os avisos aparecem no sino, mas não chegam ao celular com o app fechado. | Usar Firebase Cloud Functions para enviar uma notificação push quando um aviso for criado. |
| **Situação guardada no nome** | A situação do anúncio depende da etiqueta no nome, como "(Perdido)". | Criar um campo `status` próprio na coleção `animais`. |
| **Alertas por região não filtram avisos** | A preferência de espécie e raio é salva, mas ainda não é usada para filtrar os alertas. | Filtrar as notificações pela espécie e pela distância quando houver localização. |
| **Chave do ImgBB no código** | Qualquer pessoa com acesso ao código pode usar a chave. | Mover a chave para `local.properties` ou passar a usar o Firebase Storage. |
| **Login com Google exige SHA-1** | Cada computador que gera o app precisa ser cadastrado no Firebase. | Usar uma chave de assinatura única para o projeto. |
| **Layout pensado para celular** | Em tablets o conteúdo fica mais largo que o ideal. | Limitar a largura do conteúdo e usar mais colunas na grade em telas grandes. |
| **Somente Android** | Não há versão para iPhone. | Avaliar Kotlin Multiplatform ou uma versão web. |

## Segurança

Sugestão de regras do Firestore para que cada pessoa só altere os próprios dados:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    match /usuarios/{uid} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == uid;
    }

    match /animais/{id} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && request.resource.data.donoId == request.auth.uid;
      // Quem encontra o pet também atualiza o anúncio (fluxo "Encontrei")
      allow update: if request.auth != null;
      allow delete: if request.auth != null && resource.data.donoId == request.auth.uid;
    }

    match /notificacoes/{id} {
      allow read: if request.auth != null
        && (resource.data.destinatarioId == "TODOS" || resource.data.destinatarioId == request.auth.uid);
      allow create: if request.auth != null;
    }

    match /pedidos_adocao/{id} {
      allow create: if request.auth != null;
    }
  }
}
```

Com a regra de leitura das notificações acima, a busca precisa filtrar por `destinatarioId` no próprio Firestore, em vez de baixar todos os avisos e filtrar no aparelho.

## Próximos passos sugeridos

1. Campo `status` e localização (latitude/longitude) nos anúncios.
2. Notificações push com Cloud Functions.
3. Formulários divididos em etapas, com opção de tirar a foto direto pela câmera.
4. Tela de abertura com a logo.
5. Layout adaptado para tablets.
6. Chat dentro do app, como alternativa ao WhatsApp.
