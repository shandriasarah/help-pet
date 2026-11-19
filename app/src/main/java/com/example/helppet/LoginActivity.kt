package com.example.helppet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            MaterialTheme {
                LoginScreen(
                    onLoginSuccess = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onNavigateToRegister = {
                        startActivity(Intent(this, RegisterActivity::class.java))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val auth = remember { FirebaseAuth.getInstance() }

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var emailErro by remember { mutableStateOf<String?>(null) }
    var senhaErro by remember { mutableStateOf<String?>(null) }
    var erroGeral by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var mostrarRecuperarSenha by remember { mutableStateOf(false) }

    val backgroundOffWhite = colorResource(id = R.color.background_offwhite)
    val primaryGreen = colorResource(id = R.color.primary_green)
    val textSecondary = colorResource(id = R.color.text_secondary)

    val googleSignInClient = remember {
        val opcoes = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("633426246437-nemiqs5ob2gn4n7402q7c6oe1mtr5ems.apps.googleusercontent.com")
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, opcoes)
    }

    fun concluirLoginSocial(nome: String?, emailConta: String?) {
        garantirPerfilUsuario(nome, emailConta)
        isLoading = false
        val primeiroNome = nome?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
        val saudacao = if (primeiroNome != null) "Bem-vindo(a), $primeiroNome!" else "Bem-vindo(a)!"
        Toast.makeText(context, saudacao, Toast.LENGTH_SHORT).show()
        onLoginSuccess()
    }

    val launcherGoogle = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val conta = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val token = conta?.idToken
            if (token == null) {
                isLoading = false
                erroGeral = "Não foi possível entrar com o Google. Tente novamente."
            } else {
                auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null))
                    .addOnSuccessListener { concluirLoginSocial(conta.displayName, conta.email) }
                    .addOnFailureListener { e ->
                        isLoading = false
                        erroGeral = mensagemErroAuth(e)
                    }
            }
        } catch (e: ApiException) {
            isLoading = false
            erroGeral = when (e.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> null
                CommonStatusCodes.NETWORK_ERROR -> "Sem conexão com a internet. Verifique sua rede e tente de novo."
                CommonStatusCodes.DEVELOPER_ERROR -> "Login com Google não está configurado para este aparelho."
                else -> "Não foi possível entrar com o Google. Tente novamente."
            }
        }
    }

    fun entrarComGoogle() {
        focusManager.clearFocus()
        erroGeral = null
        isLoading = true
        // Sai da conta Google anterior para a pessoa poder escolher qual conta usar
        googleSignInClient.signOut().addOnCompleteListener {
            launcherGoogle.launch(googleSignInClient.signInIntent)
        }
    }

    fun entrarComApple() {
        val activity = context as? Activity ?: return
        focusManager.clearFocus()
        erroGeral = null
        isLoading = true

        val provedor = OAuthProvider.newBuilder("apple.com")
        provedor.scopes = listOf("email", "name")

        val tarefa = auth.pendingAuthResult
            ?: auth.startActivityForSignInWithProvider(activity, provedor.build())

        tarefa
            .addOnSuccessListener { resultado ->
                concluirLoginSocial(resultado.user?.displayName, resultado.user?.email)
            }
            .addOnFailureListener { e ->
                isLoading = false
                val cancelado = (e as? FirebaseAuthException)?.errorCode == "ERROR_WEB_CONTEXT_CANCELED"
                erroGeral = if (cancelado) null else "Não foi possível entrar com a Apple. Tente outro método de login."
            }
    }

    fun entrarComEmail() {
        focusManager.clearFocus()
        erroGeral = null
        emailErro = when {
            email.isBlank() -> "Digite seu e-mail"
            !FormValidator.validarEmail(email) -> "E-mail inválido. Exemplo: nome@email.com"
            else -> null
        }
        senhaErro = if (password.isEmpty()) "Digite sua senha" else null
        if (emailErro != null || senhaErro != null) return

        isLoading = true
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener {
                isLoading = false
                onLoginSuccess()
            }
            .addOnFailureListener { e ->
                isLoading = false
                if (e is FirebaseAuthInvalidUserException) {
                    emailErro = "Nenhuma conta encontrada com este e-mail"
                } else if (e is FirebaseAuthInvalidCredentialsException) {
                    senhaErro = if (e.errorCode == "ERROR_WRONG_PASSWORD") "Senha incorreta" else "Confira o e-mail e a senha"
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
            Spacer(modifier = Modifier.height(48.dp))

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo HelpPet",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Bem-vindo de volta!",
                color = VerdeEscuroBotao,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Entre para ajudar mais pets a voltarem para casa.",
                color = textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            CampoTexto(
                valor = email,
                aoMudar = { email = it; emailErro = null; erroGeral = null },
                rotulo = "E-mail",
                icone = Icons.Filled.Email,
                erro = emailErro,
                tipoTeclado = KeyboardType.Email,
                acaoTeclado = ImeAction.Next,
                habilitado = !isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            CampoSenha(
                valor = password,
                aoMudar = { password = it; senhaErro = null; erroGeral = null },
                erro = senhaErro,
                acaoTeclado = ImeAction.Done,
                aoConfirmar = { entrarComEmail() },
                habilitado = !isLoading
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { mostrarRecuperarSenha = true }) {
                    Text("Esqueci minha senha", color = primaryGreen, fontWeight = FontWeight.SemiBold)
                }
            }

            erroGeral?.let { mensagem ->
                BannerErro(mensagem = mensagem)
                Spacer(modifier = Modifier.height(16.dp))
            }

            BotaoPrincipal(
                texto = "Entrar",
                carregando = isLoading,
                onClick = { entrarComEmail() }
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
                Text(
                    " ou continue com ",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
            }

            Spacer(modifier = Modifier.height(20.dp))

            BotaoSocial(
                texto = "Continuar com Google",
                icone = painterResource(id = R.drawable.ic_google),
                corFundo = Color.White,
                corTexto = Color(0xFF1F1F1F),
                corBorda = Color(0xFFDADCE0),
                habilitado = !isLoading,
                onClick = { entrarComGoogle() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            BotaoSocial(
                texto = "Continuar com Apple",
                icone = painterResource(id = R.drawable.ic_apple),
                corFundo = Color.Black,
                corTexto = Color.White,
                corBorda = Color.Black,
                habilitado = !isLoading,
                onClick = { entrarComApple() },
                tamanhoIcone = 30.dp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Não tem uma conta?", color = Color.Gray, fontSize = 14.sp)
                TextButton(onClick = onNavigateToRegister) {
                    Text("Cadastre-se", color = VerdeEscuroBotao, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (mostrarRecuperarSenha) {
        RecuperarSenhaDialog(
            emailInicial = email.trim(),
            onFechar = { mostrarRecuperarSenha = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecuperarSenhaDialog(emailInicial: String, onFechar: () -> Unit) {
    val context = LocalContext.current
    val primaryGreen = colorResource(id = R.color.primary_green)
    var emailRecuperar by remember { mutableStateOf(emailInicial) }
    var erro by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }

    fun enviarLink() {
        if (!FormValidator.validarEmail(emailRecuperar)) {
            erro = "Digite um e-mail válido"
            return
        }
        enviando = true
        val destino = emailRecuperar.trim()
        FirebaseAuth.getInstance().sendPasswordResetEmail(destino)
            .addOnSuccessListener {
                enviando = false
                Toast.makeText(
                    context,
                    "Enviamos um link para $destino. Confira também a caixa de spam.",
                    Toast.LENGTH_LONG
                ).show()
                onFechar()
            }
            .addOnFailureListener { e ->
                enviando = false
                erro = mensagemErroAuth(e)
            }
    }

    AlertDialog(
        onDismissRequest = { if (!enviando) onFechar() },
        containerColor = Color.White,
        title = { Text("Recuperar senha", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Digite o e-mail da sua conta. Vamos enviar um link para você criar uma senha nova.",
                    fontSize = 14.sp,
                    color = Color(0xFF555555)
                )
                Spacer(modifier = Modifier.height(16.dp))
                CampoTexto(
                    valor = emailRecuperar,
                    aoMudar = { emailRecuperar = it; erro = null },
                    rotulo = "E-mail",
                    icone = Icons.Filled.Email,
                    erro = erro,
                    tipoTeclado = KeyboardType.Email,
                    acaoTeclado = ImeAction.Done,
                    aoConfirmar = { enviarLink() },
                    habilitado = !enviando
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { enviarLink() }, enabled = !enviando) {
                Text(
                    if (enviando) "Enviando..." else "Enviar link",
                    color = primaryGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onFechar, enabled = !enviando) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}
