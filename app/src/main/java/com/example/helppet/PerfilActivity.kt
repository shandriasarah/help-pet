package com.example.helppet

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PerfilActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PerfilScreen(
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finishAffinity()
                    }
                )
            }
        }
    }
}

private object TextosLegais {
    val PRIVACIDADE = "Política de Privacidade" to """
        O HelpPet guarda apenas os dados necessários para ajudar pets a encontrarem um lar ou voltarem para casa.

        • Dados da conta: nome, e-mail e WhatsApp, usados para identificar você e permitir que outras pessoas entrem em contato sobre um pet.
        • Anúncios: fotos, descrição e local aproximado dos pets que você cadastra ficam visíveis para todos os usuários do app.
        • Notificações: alertas gerais são vistos por todos; alertas sobre o seu pet são enviados só para você.

        Não vendemos nem compartilhamos seus dados com terceiros. Você pode editar ou apagar seus anúncios a qualquer momento e excluir sua conta em Perfil > Excluir minha conta, o que apaga também todos os seus anúncios.
    """.trimIndent()

    val TERMOS = "Termos de Uso" to """
        Ao usar o HelpPet, você concorda em:

        • Cadastrar apenas informações verdadeiras sobre os animais e sobre você.
        • Não usar o app para vender animais nem para qualquer fim comercial.
        • Respeitar as outras pessoas nos contatos feitos pelo WhatsApp.
        • Atualizar ou remover o anúncio quando o pet for encontrado ou adotado.

        O HelpPet conecta pessoas, mas não se responsabiliza pelas negociações e encontros entre usuários. Anúncios falsos ou ofensivos podem ser removidos.
    """.trimIndent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val userId = auth.currentUser?.uid

    var userName by remember { mutableStateOf("") }
    val userEmail = auth.currentUser?.email ?: ""
    val creationTimestamp = auth.currentUser?.metadata?.creationTimestamp ?: 0L

    var savedAlertaEspecie by remember { mutableStateOf("Qualquer") }
    var savedAlertaRaio by remember { mutableStateOf("50 km") }

    val memberSince = remember(creationTimestamp) {
        if (creationTimestamp > 0) {
            SimpleDateFormat("MMMM 'de' yyyy", Locale("pt", "BR")).format(Date(creationTimestamp))
        } else {
            "Desconhecido"
        }
    }

    val iniciais = remember(userName) {
        userName.trim().split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
    }

    val backgroundOffWhite = colorResource(id = R.color.background_offwhite)
    val primaryGreen = colorResource(id = R.color.primary_green)
    val textHint = Color(0xFF9E9E9E)
    val colorAccentRed = Color(0xFFC53929)

    var showAlertaDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mostrarSairDialog by remember { mutableStateOf(false) }
    var mostrarExcluirDialog by remember { mutableStateOf(false) }
    var textoLegalAberto by remember { mutableStateOf<Pair<String, String>?>(null) }
    var excluindoConta by remember { mutableStateOf(false) }

    var meusAnimais by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var isLoadingAnimais by remember { mutableStateOf(true) }

    val totalPerdidos = meusAnimais.count {
        it.nome.contains("Perdido", ignoreCase = true) || it.nome.contains("Desaparecido", ignoreCase = true)
    }
    val totalAdocao = meusAnimais.count { it.nome.contains("Para Adoção", ignoreCase = true) }

    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("usuarios").document(userId).get()
                .addOnSuccessListener { doc ->
                    userName = doc.getString("nome") ?: auth.currentUser?.displayName ?: "Usuário"
                    savedAlertaEspecie = doc.getString("alertaEspecie") ?: "Qualquer"
                    savedAlertaRaio = doc.getString("alertaRaio") ?: "50 km"
                }
                .addOnFailureListener {
                    userName = auth.currentUser?.displayName ?: "Usuário"
                }

            db.collection("animais")
                .whereEqualTo("donoId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isLoadingAnimais = false
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        meusAnimais = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Animal::class.java)?.copy(id = doc.id)
                        }
                        isLoadingAnimais = false
                    }
                }
        } else {
            isLoadingAnimais = false
        }
    }

    fun abrirInicio() {
        context.startActivity(Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        })
    }

    fun excluirConta() {
        val user = auth.currentUser ?: return
        // O Firebase so deixa excluir a conta se o login foi feito ha pouco tempo
        val ultimoLogin = user.metadata?.lastSignInTimestamp ?: 0L
        if (System.currentTimeMillis() - ultimoLogin > 5 * 60 * 1000L) {
            Toast.makeText(
                context,
                "Por segurança, saia e entre de novo na conta antes de excluí-la.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        excluindoConta = true
        db.collection("animais").whereEqualTo("donoId", user.uid).get()
            .addOnSuccessListener { snapshot ->
                val lote = db.batch()
                snapshot.documents.forEach { lote.delete(it.reference) }
                lote.delete(db.collection("usuarios").document(user.uid))
                lote.commit()
                    .addOnSuccessListener {
                        user.delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Conta excluída. Obrigado por ajudar os pets!", Toast.LENGTH_LONG).show()
                                onLogout()
                            }
                            .addOnFailureListener { e ->
                                excluindoConta = false
                                Toast.makeText(context, mensagemErroAuth(e), Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener {
                        excluindoConta = false
                        Toast.makeText(context, "Não foi possível apagar seus dados. Tente novamente.", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                excluindoConta = false
                Toast.makeText(context, "Sem conexão. Verifique sua internet e tente de novo.", Toast.LENGTH_LONG).show()
            }
    }

    Scaffold(
        bottomBar = {
            MenuInferior(telaAtual = "Perfil", onAddClick = { abrirInicio() })
        },
        containerColor = backgroundOffWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Cabecalho com avatar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryGreen, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 28.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (iniciais.isNotEmpty()) {
                            Text(iniciais, color = primaryGreen, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = primaryGreen,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = userName.ifEmpty { "Carregando..." },
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (userEmail.isNotEmpty()) {
                        Text(userEmail, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Membro desde $memberSince",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            // Numeros do usuario
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EstatisticaCard("Anúncios", meusAnimais.size, primaryGreen, Modifier.weight(1f))
                EstatisticaCard("Perdidos", totalPerdidos, colorAccentRed, Modifier.weight(1f))
                EstatisticaCard("Adoção", totalAdocao, Color(0xFFB06000), Modifier.weight(1f))
            }

            TituloSecao("MEUS ANÚNCIOS")

            if (isLoadingAnimais) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryGreen)
                }
            } else if (meusAnimais.isEmpty()) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pets,
                            contentDescription = null,
                            tint = Color(0xFFBDBDBD),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Você ainda não cadastrou nenhum pet.",
                            color = textHint,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = { abrirInicio() }) {
                            Text("Cadastrar agora", color = primaryGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    meusAnimais.forEach { animal ->
                        MeuAnimalCard(
                            animal = animal,
                            primaryGreen = primaryGreen,
                            onEdit = {
                                val intent = Intent(context, EditarAnimalActivity::class.java).apply {
                                    putExtra("petId", animal.id)
                                    putExtra("nome", animal.nome)
                                    putExtra("especie", animal.especie)
                                    putExtra("raca", animal.raca)
                                    putExtra("descricao", animal.descricao)
                                    putExtra("telefone", animal.telefoneContato)
                                }
                                context.startActivity(intent)
                            },
                            onDelete = {
                                db.collection("animais").document(animal.id)
                                    .delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Anúncio excluído com sucesso!", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Erro ao excluir anúncio.", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            TituloSecao("CONFIGURAÇÕES")

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
            ) {
                Column {
                    ItemMenuPerfil(
                        icone = Icons.Filled.NotificationsActive,
                        titulo = "Alertas por região",
                        subtitulo = "$savedAlertaEspecie · $savedAlertaRaio",
                        corIcone = primaryGreen,
                        onClick = { showAlertaDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 64.dp), color = Color(0xFFF0F0F0))
                    ItemMenuPerfil(
                        icone = Icons.Filled.Lock,
                        titulo = "Política de Privacidade",
                        corIcone = primaryGreen,
                        onClick = { textoLegalAberto = TextosLegais.PRIVACIDADE }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 64.dp), color = Color(0xFFF0F0F0))
                    ItemMenuPerfil(
                        icone = Icons.Filled.Description,
                        titulo = "Termos de Uso",
                        corIcone = primaryGreen,
                        onClick = { textoLegalAberto = TextosLegais.TERMOS }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                OutlinedButton(
                    onClick = { mostrarSairDialog = true },
                    border = BorderStroke(1.dp, primaryGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = primaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sair da conta", color = primaryGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { mostrarExcluirDialog = true },
                    enabled = !excluindoConta,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (excluindoConta) {
                        CircularProgressIndicator(
                            color = colorAccentRed,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text("Excluir minha conta", color = colorAccentRed, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (mostrarSairDialog) {
        AlertDialog(
            onDismissRequest = { mostrarSairDialog = false },
            containerColor = Color.White,
            title = { Text("Sair da conta?", fontWeight = FontWeight.Bold) },
            text = { Text("Você precisará entrar de novo para cadastrar ou acompanhar pets.") },
            confirmButton = {
                TextButton(onClick = { mostrarSairDialog = false; onLogout() }) {
                    Text("Sair", color = primaryGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSairDialog = false }) { Text("Cancelar", color = Color.Gray) }
            }
        )
    }

    if (mostrarExcluirDialog) {
        AlertDialog(
            onDismissRequest = { mostrarExcluirDialog = false },
            containerColor = Color.White,
            icon = { Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = colorAccentRed) },
            title = { Text("Excluir sua conta?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Isso apaga sua conta e todos os seus anúncios (${meusAnimais.size}). " +
                        "Essa ação não pode ser desfeita."
                )
            },
            confirmButton = {
                TextButton(onClick = { mostrarExcluirDialog = false; excluirConta() }) {
                    Text("Excluir", color = colorAccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarExcluirDialog = false }) { Text("Cancelar", color = Color.Gray) }
            }
        )
    }

    textoLegalAberto?.let { (titulo, corpo) ->
        AlertDialog(
            onDismissRequest = { textoLegalAberto = null },
            containerColor = Color.White,
            title = { Text(titulo, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(corpo, fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFF444444))
                }
            },
            confirmButton = {
                TextButton(onClick = { textoLegalAberto = null }) {
                    Text("Entendi", color = primaryGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAlertaDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAlertaDialog = false },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier.fillMaxHeight()
        ) {
            AlertaRegiaoSheet(
                initialEspecie = savedAlertaEspecie,
                initialRaio = savedAlertaRaio,
                modifier = Modifier.fillMaxHeight(),
                onDismiss = { showAlertaDialog = false },
                onCriarAlerta = { especie, raio ->
                    showAlertaDialog = false
                    if (userId != null) {
                        val dadosAlerta = mapOf(
                            "alertaEspecie" to especie,
                            "alertaRaio" to raio
                        )
                        db.collection("usuarios").document(userId)
                            .set(dadosAlerta, SetOptions.merge())
                            .addOnSuccessListener {
                                savedAlertaEspecie = especie
                                savedAlertaRaio = raio
                                Toast.makeText(context, "Alerta configurado com sucesso!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Erro ao configurar alerta.", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            )
        }
    }
}

@Composable
private fun TituloSecao(texto: String) {
    Text(
        text = texto,
        color = Color(0xFF9E9E9E),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
    )
}

@Composable
private fun EstatisticaCard(rotulo: String, valor: Int, cor: Color, modifier: Modifier = Modifier) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 14.dp)
        ) {
            Text(valor.toString(), color = cor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(rotulo, color = Color(0xFF757575), fontSize = 12.sp)
        }
    }
}

@Composable
private fun ItemMenuPerfil(
    icone: ImageVector,
    titulo: String,
    corIcone: Color,
    onClick: () -> Unit,
    subtitulo: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(corIcone.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icone, contentDescription = null, tint = corIcone, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, color = Color(0xFF1C2723), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitulo != null) {
                Text(subtitulo, color = Color(0xFF9E9E9E), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFBDBDBD)
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFF1C2723), fontSize = 16.sp)
        Text(text = value, color = Color.DarkGray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}


@Composable
fun MeuAnimalCard(animal: Animal, primaryGreen: Color, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }


    val nomeLimpoTemp = animal.nome
        .replace(" (Para Adoção)", "", ignoreCase = true)
        .replace(" (Perdido)", "", ignoreCase = true)
        .replace(" (Desaparecido)", "", ignoreCase = true)
        .replace(" (Está comigo)", "", ignoreCase = true)
        .replace(" (Avistei na rua)", "", ignoreCase = true)
        .replace(" (Encontrado - Está comigo)", "", ignoreCase = true)
        .replace(" (Encontrado - Avistei na rua)", "", ignoreCase = true)
        .trim()

    val nomeLimpo = if (nomeLimpoTemp.equals("Desconhecido", ignoreCase = true)) {
        "${animal.especie} (Resgatado)"
    } else if (nomeLimpoTemp.isEmpty()) {
        "Sem Nome"
    } else {
        nomeLimpoTemp
    }

    val textoTag = when {
        animal.nome.contains("Para Adoção", ignoreCase = true) -> "Para Adoção"
        animal.nome.contains("Perdido", ignoreCase = true) || animal.nome.contains("Desaparecido", ignoreCase = true) -> "Desaparecido"
        else -> "Encontrado"
    }

    val corTagBg = when {
        textoTag == "Desaparecido" -> Color(0xFFFCE8E6)
        textoTag == "Para Adoção" -> Color(0xFFFEF7E0)
        else -> Color(0xFFE6F4EA)
    }
    val corTagText = when {
        textoTag == "Desaparecido" -> Color(0xFFC53929)
        textoTag == "Para Adoção" -> Color(0xFFB06000)
        else -> primaryGreen
    }


    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Excluir Anúncio", fontWeight = FontWeight.Bold) },
            text = { Text("Tens a certeza que queres excluir este anúncio permanentemente?") },
            confirmButton = {
                TextButton(onClick = { showDialog = false; onDelete() }) {
                    Text("Excluir", color = Color(0xFFC53929), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar", color = Color.Gray) }
            }
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = nomeLimpo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1C2723), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(color = corTagBg, shape = RoundedCornerShape(8.dp)) {
                        Text(text = textoTag, color = corTagText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${animal.especie}\n${animal.raca}",
                    fontSize = 13.sp,
                    color = Color(0xFF757575)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = primaryGreen,
                    modifier = Modifier.size(24.dp).clickable { onEdit() }
                )
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = Color(0xFFC53929),
                    modifier = Modifier.size(24.dp).clickable { showDialog = true }
                )
            }
        }
    }
}

@Composable
fun AlertaRegiaoSheet(
    initialEspecie: String,
    initialRaio: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onCriarAlerta: (String, String) -> Unit
) {
    var especieSelecionada by remember { mutableStateOf(initialEspecie) }
    var raioSelecionado by remember { mutableStateOf(initialRaio) }

    val primaryGreen = colorResource(id = R.color.primary_green)
    val textDark = Color(0xFF1C2723)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Alerta de Pets Perdidos", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textDark)
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = textDark)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = Color(0xFFE8F5E9),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Receba uma notificação quando um pet perdido for registrado na sua região.",
                color = primaryGreen,
                fontSize = 14.sp,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Espécie", fontWeight = FontWeight.Bold, color = textDark, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            val especies = listOf("Qualquer", "Cachorro", "Gato", "Pássaro")
            especies.forEach { esp ->
                FilterChip(
                    selected = especieSelecionada == esp,
                    onClick = { especieSelecionada = esp },
                    label = { Text(esp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = primaryGreen,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Raio de busca", fontWeight = FontWeight.Bold, color = textDark, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            val raios = listOf("10 km", "25 km", "50 km", "100 km")
            raios.forEach { raio ->
                FilterChip(
                    selected = raioSelecionado == raio,
                    onClick = { raioSelecionado = raio },
                    label = { Text(raio) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = primaryGreen,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onCriarAlerta(especieSelecionada, raioSelecionado) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Salvar Alerta", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}