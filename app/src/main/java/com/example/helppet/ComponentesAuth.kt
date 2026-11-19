package com.example.helppet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore

val VerdeEscuroBotao = Color(0xFF0D4722)
val VermelhoErro = Color(0xFFC53929)
private val CinzaBorda = Color(0xFFE0E0E0)

/** Traduz os erros do Firebase Auth para mensagens claras em portugues. */
fun mensagemErroAuth(e: Exception?): String = when (e) {
    is FirebaseNetworkException ->
        "Sem conexão com a internet. Verifique sua rede e tente de novo."
    is FirebaseTooManyRequestsException ->
        "Muitas tentativas seguidas. Aguarde alguns minutos e tente de novo."
    is FirebaseAuthRecentLoginRequiredException ->
        "Por segurança, saia e entre de novo na conta antes de continuar."
    is FirebaseAuthWeakPasswordException ->
        "Senha fraca: use pelo menos 6 caracteres, com letras e números."
    is FirebaseAuthUserCollisionException ->
        "Já existe uma conta com este e-mail. Tente entrar ou recuperar a senha."
    is FirebaseAuthInvalidUserException -> when (e.errorCode) {
        "ERROR_USER_DISABLED" -> "Esta conta foi desativada."
        else -> "Não encontramos nenhuma conta com este e-mail."
    }
    is FirebaseAuthInvalidCredentialsException -> when (e.errorCode) {
        "ERROR_INVALID_EMAIL" -> "O e-mail digitado não é válido."
        "ERROR_WRONG_PASSWORD" -> "Senha incorreta. Tente de novo ou toque em \"Esqueci minha senha\"."
        else -> "E-mail ou senha incorretos. Confira os dados ou toque em \"Esqueci minha senha\"."
    }
    else -> "Algo deu errado. Tente novamente."
}

/** Cria o documento em "usuarios" para quem entrou com Google/Apple pela primeira vez. */
fun garantirPerfilUsuario(nome: String?, email: String?) {
    val user = FirebaseAuth.getInstance().currentUser ?: return
    val ref = FirebaseFirestore.getInstance().collection("usuarios").document(user.uid)
    ref.get().addOnSuccessListener { doc ->
        if (!doc.exists()) {
            ref.set(
                hashMapOf(
                    "nome" to (nome ?: user.displayName ?: "Usuário"),
                    "email" to (email ?: user.email ?: ""),
                    "telefone" to ""
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun coresCampo(primaryGreen: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = primaryGreen,
    unfocusedBorderColor = CinzaBorda,
    focusedLabelColor = primaryGreen,
    focusedLeadingIconColor = primaryGreen,
    cursorColor = primaryGreen,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    errorContainerColor = Color.White
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoTexto(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    icone: ImageVector,
    modifier: Modifier = Modifier,
    erro: String? = null,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    acaoTeclado: ImeAction = ImeAction.Next,
    aoConfirmar: () -> Unit = {},
    habilitado: Boolean = true
) {
    val primaryGreen = colorResource(id = R.color.primary_green)
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        leadingIcon = { Icon(imageVector = icone, contentDescription = null) },
        isError = erro != null,
        supportingText = if (erro != null) {
            { Text(erro) }
        } else null,
        singleLine = true,
        enabled = habilitado,
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado, imeAction = acaoTeclado),
        keyboardActions = KeyboardActions(onDone = { aoConfirmar() }),
        shape = RoundedCornerShape(14.dp),
        colors = coresCampo(primaryGreen),
        modifier = modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoSenha(
    valor: String,
    aoMudar: (String) -> Unit,
    modifier: Modifier = Modifier,
    rotulo: String = "Senha",
    erro: String? = null,
    acaoTeclado: ImeAction = ImeAction.Done,
    aoConfirmar: () -> Unit = {},
    habilitado: Boolean = true
) {
    val primaryGreen = colorResource(id = R.color.primary_green)
    var visivel by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        leadingIcon = { Icon(imageVector = Icons.Filled.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { visivel = !visivel }) {
                Icon(
                    imageVector = if (visivel) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visivel) "Ocultar senha" else "Mostrar senha"
                )
            }
        },
        visualTransformation = if (visivel) VisualTransformation.None else PasswordVisualTransformation(),
        isError = erro != null,
        supportingText = if (erro != null) {
            { Text(erro) }
        } else null,
        singleLine = true,
        enabled = habilitado,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = acaoTeclado),
        keyboardActions = KeyboardActions(onDone = { aoConfirmar() }),
        shape = RoundedCornerShape(14.dp),
        colors = coresCampo(primaryGreen),
        modifier = modifier.fillMaxWidth()
    )
}

/** Checklist ao vivo das regras da senha (fica verde conforme a pessoa digita). */
@Composable
fun ListaRequisitosSenha(senha: String, modifier: Modifier = Modifier) {
    val primaryGreen = colorResource(id = R.color.primary_green)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, top = 2.dp, bottom = 4.dp)
    ) {
        FormValidator.requisitosSenha(senha).forEach { (texto, cumprido) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (cumprido) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (cumprido) primaryGreen else Color(0xFFBDBDBD),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = texto,
                    fontSize = 13.sp,
                    color = if (cumprido) primaryGreen else Color(0xFF757575)
                )
            }
        }
    }
}

@Composable
fun BannerErro(mensagem: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFFFCE8E6),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = VermelhoErro,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = mensagem, color = VermelhoErro, fontSize = 14.sp)
        }
    }
}

@Composable
fun BotaoPrincipal(
    texto: String,
    carregando: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !carregando,
        colors = ButtonDefaults.buttonColors(
            containerColor = VerdeEscuroBotao,
            disabledContainerColor = VerdeEscuroBotao.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth().height(56.dp)
    ) {
        if (carregando) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(texto, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BotaoSocial(
    texto: String,
    icone: Painter,
    corFundo: Color,
    corTexto: Color,
    corBorda: Color,
    habilitado: Boolean,
    onClick: () -> Unit,
    tamanhoIcone: Dp = 22.dp
) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, corBorda),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = corFundo,
            contentColor = corTexto,
            disabledContainerColor = corFundo.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) {
        Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            Image(painter = icone, contentDescription = null, modifier = Modifier.size(tamanhoIcone))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(texto, color = corTexto, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
