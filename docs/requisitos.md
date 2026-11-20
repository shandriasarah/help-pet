# Requisitos

## Requisitos funcionais

| Código | Requisito |
|---|---|
| RF01 | O usuário deve poder criar uma conta com nome, e-mail, telefone (WhatsApp) e senha. |
| RF02 | O usuário deve poder entrar com e-mail e senha ou com a conta Google. |
| RF03 | O usuário deve poder recuperar a senha recebendo um link por e-mail. |
| RF04 | O usuário deve poder publicar um anúncio de pet **perdido** com foto, nome, espécie, gênero, raça, características, local visto por último e WhatsApp. |
| RF05 | O usuário deve poder registrar um animal **encontrado**, informando se ele está seguro com a pessoa ou se foi apenas avistado, com foto, espécie, porte, cor, local e WhatsApp. |
| RF06 | O usuário deve poder marcar um pet da lista de desaparecidos como encontrado. |
| RF07 | Ao marcar um pet desaparecido como encontrado, o sistema deve enviar um aviso privado ao dono com o local e o contato de quem encontrou. |
| RF08 | O usuário deve poder publicar um pet **para adoção** com foto, nome, espécie, gênero, raça, idade, saúde, história e WhatsApp. |
| RF09 | O usuário deve poder registrar o interesse em adotar e receber sugestões de pets pela espécie desejada. |
| RF10 | O sistema deve listar todos os pets na tela inicial, com busca por nome, raça ou bairro e filtro por espécie. |
| RF11 | O sistema deve separar os pets em abas: Desaparecidos, Achados e Adoção. |
| RF12 | O sistema deve exibir no mapa a região aproximada dos pets desaparecidos e abrir o perfil do pet ao tocar no marcador. |
| RF13 | O usuário deve poder ver o perfil completo de um pet e abrir uma conversa no WhatsApp com quem publicou. |
| RF14 | O usuário deve poder compartilhar um pet com foto e texto pelo WhatsApp, Instagram ou outro aplicativo. |
| RF15 | O sistema deve exibir as notificações da comunidade e os avisos privados, indicando quantas ainda não foram vistas. |
| RF16 | O usuário deve poder editar e excluir os próprios anúncios. |
| RF17 | O usuário deve poder configurar alertas por espécie e raio de distância. |
| RF18 | O usuário deve poder sair da conta e excluir a conta com todos os seus anúncios. |
| RF19 | Ao enviar a foto, o usuário deve poder ajustar o enquadramento arrastando a imagem. |

## Requisitos não funcionais

| Código | Requisito |
|---|---|
| RNF01 | O aplicativo deve funcionar em Android 7.0 (API 24) ou superior, em celulares e tablets. |
| RNF02 | A interface deve estar em português do Brasil, com mensagens de erro claras. |
| RNF03 | As listas devem se atualizar em tempo real quando um anúncio for criado, editado ou excluído. |
| RNF04 | As fotos devem ser reduzidas (até 1280 px de largura) antes do envio para economizar dados. |
| RNF05 | A autenticação deve ser feita pelo Firebase Authentication. Senhas não são armazenadas pelo app. |
| RNF06 | O acesso ao banco de dados deve exigir usuário autenticado (regras do Firestore). |
| RNF07 | Telas que carregam dados devem mostrar um indicador de carregamento e um aviso quando não houver resultados. |

## Regras de negócio

| Código | Regra |
|---|---|
| RN01 | A senha deve ter pelo menos 6 caracteres, com pelo menos uma letra e um número. |
| RN02 | O telefone deve ter DDD e número, com 10 (fixo) ou 11 (celular) dígitos. O código do país (+55) é adicionado automaticamente. |
| RN03 | O nome do pet deve ter pelo menos 2 letras. |
| RN04 | Todo anúncio tem uma situação: desaparecido, encontrado ou para adoção. Ela é identificada pela etiqueta no nome do anúncio, como "(Perdido)" ou "(Para Adoção)", ou, na falta dela, pelo início da descrição ("Visto em:" para desaparecido e "Saúde:" para adoção). |
| RN05 | O aviso de pet encontrado é privado: só o dono do pet o vê. |
| RN06 | Apenas o autor de um anúncio pode editá-lo ou excluí-lo pela tela de perfil. |
| RN07 | Por segurança, a exclusão da conta só é permitida se o login tiver sido feito nos últimos 5 minutos. |
| RN08 | O mapa mostra apenas pets desaparecidos. Pets da mesma região aparecem em um único marcador com a quantidade. |
