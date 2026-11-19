package com.example.helppet

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                RegisterScreen(
                    onBackToLogin = { finish() },
                    onContaCriada = {
                        // A conta ja fica logada: vai direto para o inicio
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onBackToLogin: () -> Unit, onContaCriada: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmarSenha by rememberSaveable { mutableStateOf("") }

    var nomeErro by remember { mutableStateOf<String?>(null) }
    var emailErro by remember { mutableStateOf<String?>(null) }
    var telefoneErro by remember { mutableStateOf<String?>(null) }
    var senhaErro by remember { mutableStateOf<String?>(null) }
    var confirmarErro by remember { mutableStateOf<String?>(null) }
    var erroGeral by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val backgroundOffWhite = colorResource(id = R.color.background_offwhite)
    val textSecondary = colorResource(id = R.color.text_secondary)

    fun cadastrar() {
        focusManager.clearFocus()
        erroGeral = null

        nomeErro = when {
            name.isBlank() -> "Digite seu nome"
            name.trim().length < 3 -> "Nome muito curto"
            else -> null
        }
        emailErro = when {
            email.isBlank() -> "Digite seu e-mail"
            !FormValidator.validarEmail(email) -> "E-mail inválido. Exemplo: nome@email.com"
            else -> null
        }
        telefoneErro = if (FormValidator.validarTelefoneComDdd(phone)) null else "Digite o DDD + número (10 ou 11 dígitos)"
        senhaErro = FormValidator.erroSenha(password)
        confirmarErro = when {
            confirmarSenha.isEmpty() -> "Confirme sua senha"
            confirmarSenha != password -> "As senhas não são iguais"
            else -> null
        }

        if (listOf(nomeErro, emailErro, telefoneErro, senhaErro, confirmarErro).any { it != null }) {
            erroGeral = "Corrija os campos marcados em vermelho."
            return
        }

        isLoading = true
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { resultado ->
                val userId = resultado.user?.uid
                val primeiroNome = name.trim().split(" ").first()
                if (userId == null) {
                    isLoading = false
                    onContaCriada()
                    return@addOnSuccessListener
                }
                val dadosUsuario = hashMapOf(
                    "nome" to name.trim(),
                    "email" to email.trim(),
                    "telefone" to phone
                )
                db.collection("usuarios").document(userId).set(dadosUsuario)
                    .addOnCompleteListener { tarefa ->
                        isLoading = false
                        val mensagem = if (tarefa.isSuccessful) {
                            "Conta criada! Bem-vindo(a), $primeiroNome!"
                        } else {
                            "Conta criada, mas não conseguimos salvar seu perfil. Você pode continuar."
                        }
                        Toast.makeText(context, mensagem, Toast.LENGTH_LONG).show()
                        onContaCriada()
                    }
            }
            .addOnFailureListener { e ->
                isLoading = false
                when (e) {
                    is FirebaseAuthUserCollisionException -> emailErro = "Este e-mail já está cadastrado"
                    is FirebaseAuthWeakPasswordException -> senhaErro = "Senha muito fraca"
                }
                erroGeral = mensagemErroAuth(e)
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundOffWhite)
    ) {
        Image(
            painter = painterResource(id = R.drawable.patalogo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(250.dp)
                .offset(x = (-40).dp, y = 60.dp)
                .alpha(0.12f)
                .align(Alignment.BottomStart)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToLogin, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = VerdeEscuroBotao
                    )
                }
            }

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo HelpPet",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Crie sua conta",
                color = VerdeEscuroBotao,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Leva menos de um minuto e você já pode ajudar.",
                color = textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            CampoTexto(
                valor = name,
                aoMudar = { name = it; nomeErro = null },
                rotulo = "Nome completo",
                icone = Icons.Filled.Person,
                erro = nomeErro,
                habilitado = !isLoading
            )
            Spacer(modifier = Modifier.height(8.dp))

            CampoTexto(
                valor = email,
                aoMudar = { email = it; emailErro = null },
                rotulo = "E-mail",
                icone = Icons.Filled.Email,
                erro = emailErro,
                tipoTeclado = KeyboardType.Email,
                habilitado = !isLoading
            )
            Spacer(modifier = Modifier.height(8.dp))

            CampoTexto(
                valor = phone,
                aoMudar = { novo ->
                    val digitos = novo.filter { it.isDigit() }
                    if (digitos.length <= 11) phone = digitos
                    telefoneErro = null
                },
                rotulo = "WhatsApp (DDD + número)",
                icone = Icons.Filled.Phone,
                erro = telefoneErro,
                tipoTeclado = KeyboardType.Phone,
                habilitado = !isLoading
            )
            Spacer(modifier = Modifier.height(8.dp))

            CampoSenha(
                valor = password,
                aoMudar = { password = it; senhaErro = null },
                erro = senhaErro,
                acaoTeclado = ImeAction.Next,
                habilitado = !isLoading
            )
            if (password.isNotEmpty()) {
                ListaRequisitosSenha(senha = password)
            }
            Spacer(modifier = Modifier.height(8.dp))

            CampoSenha(
                valor = confirmarSenha,
                aoMudar = { confirmarSenha = it; confirmarErro = null },
                rotulo = "Confirmar senha",
                erro = confirmarErro,
                acaoTeclado = ImeAction.Done,
                aoConfirmar = { cadastrar() },
                habilitado = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            erroGeral?.let { mensagem ->
                BannerErro(mensagem = mensagem)
                Spacer(modifier = Modifier.height(16.dp))
            }

            BotaoPrincipal(
                texto = "Criar conta",
                carregando = isLoading,
                onClick = { cadastrar() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Ao criar a conta, você concorda com os Termos de Uso e a Política de Privacidade do HelpPet.",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Já tem uma conta?", color = Color.Gray, fontSize = 14.sp)
                TextButton(onClick = onBackToLogin) {
                    Text("Entrar", color = VerdeEscuroBotao, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
