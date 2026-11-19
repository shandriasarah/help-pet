package com.example.helppet

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                android.util.Log.d("FCM_TOKEN", "TUA_CHAVE: ${task.result}")
            } else {
                android.util.Log.e("FCM_TOKEN", "Erro ao obter token", task.exception)
            }
        }

        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val backgroundOffWhite = colorResource(id = R.color.background_offwhite)
    val primaryGreen = colorResource(id = R.color.primary_green)

    val db = remember { FirebaseFirestore.getInstance() }
    var animais by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddMenu by remember { mutableStateOf(false) }
    var busca by remember { mutableStateOf("") }

    var categoriaSelecionada by remember { mutableStateOf("Todos") }
    val categorias = listOf("Todos", "Cachorros", "Gatos", "Outros")

    LaunchedEffect(Unit) {
        db.collection("animais")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    isLoading = false
                    return@addSnapshotListener
                }
                animais = snapshot.documents.mapNotNull { document ->
                    document.toObject(Animal::class.java)?.copy(id = document.id)
                }
                isLoading = false
            }
    }

    val termo = busca.trim()
    val animaisFiltrados = animais.filter { animal ->
        val passaCategoria = when (categoriaSelecionada) {
            "Cachorros" -> animal.especie.equals("Cachorro", ignoreCase = true)
            "Gatos" -> animal.especie.equals("Gato", ignoreCase = true)
            "Outros" -> !animal.especie.equals("Cachorro", ignoreCase = true) &&
                !animal.especie.equals("Gato", ignoreCase = true)
            else -> true
        }
        val passaBusca = termo.isEmpty() ||
            nomeDeExibicao(animal).contains(termo, ignoreCase = true) ||
            animal.raca.contains(termo, ignoreCase = true) ||
            animal.especie.contains(termo, ignoreCase = true) ||
            animal.descricao.contains(termo, ignoreCase = true)
        passaCategoria && passaBusca
    }

    Scaffold(
        bottomBar = {
            MenuInferior(
                telaAtual = "Inicio",
                onAddClick = { showAddMenu = true }
            )
        },
        containerColor = backgroundOffWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp)) {
                HeaderSection()
            }

            BarraBusca(
                texto = busca,
                aoMudar = { busca = it },
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categorias) { categoria ->
                    FilterChipCustom(
                        texto = categoria,
                        isSelected = categoria == categoriaSelecionada,
                        onClick = { categoriaSelecionada = categoria },
                        primaryGreen = primaryGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (termo.isEmpty()) "Pets cadastrados" else "Resultados da busca",
                    color = colorResource(id = R.color.text_primary),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (!isLoading) {
                    Text(
                        text = "${animaisFiltrados.size} ${if (animaisFiltrados.size == 1) "pet" else "pets"}",
                        color = colorResource(id = R.color.text_secondary),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when {
                    isLoading -> repeat(2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            CardEsqueleto(modifier = Modifier.weight(1f))
                            CardEsqueleto(modifier = Modifier.weight(1f))
                        }
                    }
                    animaisFiltrados.isEmpty() -> EstadoVazio(
                        titulo = if (termo.isEmpty()) "Nenhum pet por aqui ainda" else "Nada encontrado",
                        mensagem = if (termo.isEmpty()) "Seja o primeiro a cadastrar um pet." else "Tente buscar por outro nome, raça ou bairro.",
                        textoBotao = if (termo.isEmpty()) "Cadastrar um pet" else "Limpar busca",
                        onBotao = { if (termo.isEmpty()) showAddMenu = true else busca = "" }
                    )
                    else -> animaisFiltrados.chunked(2).forEach { linha ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            linha.forEach { animal ->
                                CardPetGrade(animal = animal, modifier = Modifier.weight(1f))
                            }
                            if (linha.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showAddMenu) {
            MenuAdicionarPet(onDismiss = { showAddMenu = false })
        }
    }
}

@Composable
fun FilterChipCustom(texto: String, isSelected: Boolean, onClick: () -> Unit, primaryGreen: Color) {
    val bgColor = if (isSelected) primaryGreen else Color(0xFFFFFFFF)
    val textColor = if (isSelected) Color.White else Color.DarkGray
    val borderColor = if (isSelected) primaryGreen else Color(0xFFE0E0E0)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = texto,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun AnimalCardPremium(animal: Animal, primaryGreen: Color) {
    val textSecondary = colorResource(id = R.color.text_secondary)
    val context = LocalContext.current


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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(context, DetalhesPetActivity::class.java).apply {
                    putExtra("nome", animal.nome)
                    putExtra("especie", animal.especie)
                    putExtra("raca", animal.raca)
                    putExtra("genero", animal.genero)
                    putExtra("idade", animal.idade)
                    putExtra("fotoUrl", animal.fotoUrl)
                    putExtra("descricao", animal.descricao)
                    putExtra("telefone", animal.telefoneContato)
                }
                context.startActivity(intent)
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            AsyncImage(
                model = animal.fotoUrl.ifEmpty { "https://via.placeholder.com/600x400?text=Sem+Foto" },
                contentDescription = nomeLimpo,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = nomeLimpo,
                        color = colorResource(id = R.color.text_primary),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )

                    val textoTag = when {
                        animal.nome.contains("Perdido", ignoreCase = true) || animal.nome.contains("Desaparecido", ignoreCase = true) -> "Desaparecido"
                        animal.nome.contains("Para Adoção", ignoreCase = true) -> "Para Adoção"
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

                    Box(
                        modifier = Modifier.background(corTagBg, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = textoTag, color = corTagText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }


                }

                Spacer(modifier = Modifier.height(8.dp))

                val localizacao = if (animal.descricao.contains("Encontrado em:")) {
                    animal.descricao.substringAfter("Encontrado em:").trim()
                } else if (animal.descricao.contains("Visto em:")) {
                    animal.descricao.substringAfter("Visto em:").substringBefore(".").trim()
                } else {
                    "Recife, PE"
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Local", tint = textSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = localizacao, color = textSecondary, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AttributeBadge("Recente")
                    if (animal.raca.isNotEmpty() && animal.raca != "Desconhecida" && animal.raca != "ND") {
                        AttributeBadge(animal.raca)
                    }
                }
            }
        }
    }
}

@Composable
fun AttributeBadge(text: String) {
    Box(
        modifier = Modifier.background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = text, color = Color.DarkGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun HeaderSection() {
    val context = LocalContext.current
    val primaryGreen = colorResource(id = R.color.primary_green)
    val textPrimary = colorResource(id = R.color.text_primary)
    val textSecondary = colorResource(id = R.color.text_secondary)

    var userName by remember { mutableStateOf("...") }
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    LaunchedEffect(Unit) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("usuarios").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val nomeCompleto = document.getString("nome") ?: "Visitante"
                        val primeiroNome = nomeCompleto.split(" ").firstOrNull() ?: "Visitante"
                        userName = primeiroNome
                    } else {
                        userName = "Visitante"
                    }
                }
                .addOnFailureListener {
                    userName = "Visitante"
                }
        } else {
            userName = "Visitante"
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "HelpPet", color = primaryGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Icon(
                painter = painterResource(id = R.drawable.ic_bell),
                contentDescription = "Notificações",
                tint = primaryGreen,
                modifier = Modifier
                    .size(32.dp)
                    .clickable {
                        context.startActivity(Intent(context, NotificacoesActivity::class.java))
                    }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Olá, $userName!", color = textPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(text = "Juntos podemos ajudar\nmais pets.", color = textSecondary, fontSize = 16.sp)
    }
}

@Composable
fun ActionCard(title: String, iconId: Int, cardBgColor: Color, accentColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(76.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = cardBgColor), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).background(color = accentColor, shape = CircleShape), contentAlignment = Alignment.Center) {
                Icon(painter = painterResource(id = iconId), contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, color = accentColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(text = "›", color = accentColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}