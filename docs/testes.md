# Testes

## Testes automatizados

Os testes de unidade ficam em `app/src/test/java/com/example/helppet/` e rodam com:

```bash
./gradlew testDebugUnitTest
```

### `FormValidatorTest` (8 testes)

| Teste | O que verifica |
|---|---|
| Nome vazio | Nome do pet vazio é inválido |
| Nome só com espaços | Nome com apenas espaços é inválido |
| Nome válido | "Rex" é aceito |
| Telefone curto | Telefone com menos de 9 dígitos é inválido |
| Telefone válido | "81999999999" é aceito |
| Celular formatado | "81999998888" vira "(81) 99999-8888" |
| Fixo formatado | "8133334444" vira "(81) 3333-4444" |
| Formatação aos poucos | O número é formatado corretamente enquanto é digitado |

### `StatusPetTest` (5 testes)

| Teste | O que verifica |
|---|---|
| Etiqueta no nome | "(Perdido)", "(Para Adoção)" e "(Está comigo)" definem a situação |
| Descrição de adoção | Anúncio sem etiqueta com descrição "Saúde: …" é para adoção |
| Descrição de perdido | Anúncio sem etiqueta com descrição "Visto em: …" é desaparecido |
| Sem pista | Anúncio sem etiqueta e sem pista conta como encontrado |
| Nome exibido | O nome mostrado não inclui a etiqueta |

## Roteiro de testes manuais

| # | Cenário | Passos | Resultado esperado |
|---|---|---|---|
| 1 | Cadastro com senha fraca | Cadastre-se com a senha "abcdef" | Aviso "A senha precisa ter pelo menos um número" |
| 2 | Cadastro válido | Cadastre-se com dados válidos | Conta criada e app abre na tela inicial |
| 3 | Login com senha errada | Entre com a senha incorreta | Aviso "E-mail ou senha incorretos" abaixo dos campos |
| 4 | Recuperar senha | Toque em "Esqueci minha senha" e envie | Mensagem de link enviado e e-mail recebido |
| 5 | Saudação | Entre com uma conta | "Olá, [primeiro nome]!" na tela inicial |
| 6 | Publicar pet perdido | + → Perdi um animal → preencha e envie | Tela de sucesso; pet aparece em Desaparecidos e no mapa |
| 7 | Ajustar foto | Escolha uma foto e arraste dentro do quadro | A foto se move e o anúncio usa o enquadramento escolhido |
| 8 | Outra espécie | Digite "Coelho" em "Outra espécie..." | Anúncio salvo com espécie "Coelho" e listado em "Outros" |
| 9 | Marcar como encontrado | Com outra conta: + → Encontrei → Escolher da lista → envie | Dono recebe aviso privado com o botão "Falar com quem encontrou" |
| 10 | Contador do sino | Gere um aviso e volte à tela inicial | Bolinha vermelha com o número; zera ao abrir as notificações |
| 11 | Busca | Digite um bairro na busca | Apenas os pets daquele bairro aparecem |
| 12 | Mapa agrupado | Publique dois perdidos citando o mesmo bairro | Um único marcador com o número 2; ao tocar, abre a lista |
| 13 | Compartilhar | Toque no ícone de compartilhar e escolha um pet | Janela do Android com a foto e o texto com link do WhatsApp |
| 14 | WhatsApp | No perfil de um pet, toque em "Falar no WhatsApp" | Conversa aberta com o número de quem publicou |
| 15 | Editar e excluir | Perfil → Meus anúncios → edite e depois exclua | Alterações aparecem em todas as telas; anúncio some após excluir |
| 16 | Telefone incompleto | Digite só 7 dígitos no WhatsApp | Aviso em laranja "Digite o DDD e o número completo" |
