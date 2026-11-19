package com.example.helppet

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File
import kotlin.random.Random

class MapaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val osmConf = Configuration.getInstance()
        val cacheDir = File(cacheDir, "osm_clean_tiles_v3")
        cacheDir.mkdirs()
        osmConf.osmdroidBasePath = cacheDir
        osmConf.osmdroidTileCache = cacheDir
        osmConf.userAgentValue = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

        try {
            SqlTileWriter().purgeCache()
        } catch (_: Exception) {}

        setContent {
            MaterialTheme {
                MapaScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaScreen() {
    val context = LocalContext.current
    val primaryGreen = colorResource(id = R.color.primary_green)
    val backgroundOffWhite = colorResource(id = R.color.background_offwhite)

    val db = remember { FirebaseFirestore.getInstance() }
    var animaisNoBanco by remember { mutableStateOf<List<Animal>>(emptyList()) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }


    LaunchedEffect(Unit) {
        db.collection("animais").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                animaisNoBanco = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Animal::class.java)?.copy(id = doc.id)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa de Pets", fontWeight = FontWeight.Bold, color = primaryGreen) },
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = primaryGreen
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        mapViewRef?.tileProvider?.clearTileCache()
                        mapViewRef?.invalidate()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recarregar Mapa",
                            tint = primaryGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundOffWhite)
            )
        },
        bottomBar = {
            MenuInferior(
                telaAtual = "Mapa",
                onAddClick = {
                    context.startActivity(Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    })
                }
            )
        },
        containerColor = backgroundOffWhite
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { ctx ->
                    val arcGisStreetSource = object : OnlineTileSourceBase(
                        "ArcGIS_World_Street_Map",
                        0,
                        19,
                        256,
                        "",
                        arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/")
                    ) {
                        override fun getTileURLString(pMapTileIndex: Long): String {
                            val z = MapTileIndex.getZoom(pMapTileIndex)
                            val y = MapTileIndex.getY(pMapTileIndex)
                            val x = MapTileIndex.getX(pMapTileIndex)
                            return "$baseUrl$z/$y/$x"
                        }
                    }

                    MapView(ctx).apply {
                        mapViewRef = this
                        setUseDataConnection(true)
                        setTileSource(arcGisStreetSource)
                        setMultiTouchControls(true)
                        controller.setZoom(14.2)


                        controller.setCenter(GeoPoint(-8.0578, -34.8829))
                    }
                },
                update = { mapView ->
                    val ctxMapa = mapView.context
                    mapView.overlays.clear()



                    val listaParaExibir = if (animaisNoBanco.isNotEmpty()) {
                        animaisNoBanco.filter { animal -> statusDoAnimal(animal) != StatusPet.ADOCAO }
                    } else {

                        listOf(
                            Animal(
                                id = "demo1",
                                nome = "Cachorro Caramelo (Perdido)",
                                especie = "Cachorro",
                                raca = "Vira-lata",
                                descricao = "Visto por último perto da Av. Conde da Boa Vista"
                            )
                        )
                    }

                    listaParaExibir.forEach { animal ->
                        val ponto = obterCoordenadasRecife(animal.descricao, animal.id.hashCode())
                        val isPerdido = animal.nome.contains("Perdido", ignoreCase = true) ||
                                animal.nome.contains("Desaparecido", ignoreCase = true)


                        val circuloRegiao = Polygon(mapView).apply {
                            points = Polygon.pointsAsCircle(ponto, 450.0)
                            infoWindow = null
                            if (isPerdido) {

                                fillPaint.color = AndroidColor.argb(45, 197, 57, 41)
                                outlinePaint.color = AndroidColor.rgb(197, 57, 41)
                            } else {

                                fillPaint.color = AndroidColor.argb(45, 46, 125, 50)
                                outlinePaint.color = AndroidColor.rgb(46, 125, 50)
                            }
                            outlinePaint.strokeWidth = 3f
                        }
                        mapView.overlays.add(circuloRegiao)


                        // Pin proprio: circulo na cor do status com uma patinha. Um toque abre o perfil do pet.
                        val marcador = Marker(mapView).apply {
                            position = ponto
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = criarIconeMarcador(
                                ctxMapa,
                                if (isPerdido) AndroidColor.rgb(197, 57, 41) else AndroidColor.rgb(27, 77, 62)
                            )
                            infoWindow = null
                            title = nomeDeExibicao(animal)
                            setOnMarkerClickListener { _, _ ->
                                if (animal.id !in listOf("demo1", "demo2", "demo3")) {
                                    abrirDetalhesPet(context, animal)
                                }
                                true
                            }
                        }
                        mapView.overlays.add(marcador)
                    }

                    mapView.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFFC53929), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Desaparecido", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1C2723))
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFF1B4D3E), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Encontrado", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1C2723))
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Toque no pin para ver o perfil do pet", fontSize = 12.sp, color = Color(0xFF757575))
                }
            }
        }
    }
}

/** Desenha o pin do mapa: circulo colorido com borda branca, patinha branca e ponta embaixo. */
private fun criarIconeMarcador(context: Context, cor: Int): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val largura = (56 * d).toInt()
    val altura = (72 * d).toInt()
    val bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val tinta = Paint(Paint.ANTI_ALIAS_FLAG)

    val cx = largura / 2f
    val raio = largura / 2f - 3 * d
    val cy = raio + 2 * d

    // Sombra suave
    tinta.color = AndroidColor.argb(50, 0, 0, 0)
    canvas.drawCircle(cx, cy + 2 * d, raio, tinta)

    // Ponta do pin
    val ponta = Path().apply {
        moveTo(cx - raio * 0.55f, cy + raio * 0.6f)
        lineTo(cx, altura - 1 * d)
        lineTo(cx + raio * 0.55f, cy + raio * 0.6f)
        close()
    }
    tinta.color = cor
    canvas.drawPath(ponta, tinta)

    // Circulo com borda branca
    tinta.color = AndroidColor.WHITE
    canvas.drawCircle(cx, cy, raio, tinta)
    tinta.color = cor
    canvas.drawCircle(cx, cy, raio - 3 * d, tinta)

    // Patinha branca no centro
    ContextCompat.getDrawable(context, R.drawable.ic_nav_pets)?.mutate()?.let { pata ->
        pata.setTint(AndroidColor.WHITE)
        val metade = (raio * 0.8f).toInt()
        pata.setBounds((cx - metade).toInt(), (cy - metade).toInt(), (cx + metade).toInt(), (cy + metade).toInt())
        pata.draw(canvas)
    }

    return BitmapDrawable(context.resources, bitmap)
}


private fun obterCoordenadasRecife(descricao: String, seed: Int): GeoPoint {
    val texto = descricao.lowercase()
    val base = when {
        texto.contains("boa viagem") || texto.contains("pina") -> GeoPoint(-8.1180, -34.8970)
        texto.contains("coelhos") || texto.contains("ilha do leite") -> GeoPoint(-8.0625, -34.8850)
        texto.contains("boa vista") || texto.contains("conde") || texto.contains("derby") -> GeoPoint(-8.0500, -34.8800)
        texto.contains("marco zero") || texto.contains("recife antigo") -> GeoPoint(-8.0631, -34.8711)
        texto.contains("graças") || texto.contains("espinheiro") || texto.contains("aflitos") -> GeoPoint(-8.0440, -34.8970)
        texto.contains("casa amarela") || texto.contains("parnamirim") || texto.contains("jaqueira") -> GeoPoint(-8.0300, -34.9120)
        texto.contains("várzea") || texto.contains("ufpe") || texto.contains("caxangá") -> GeoPoint(-8.0500, -34.9500)
        texto.contains("afogados") || texto.contains("san martin") || texto.contains("bongi") -> GeoPoint(-8.0750, -34.9150)
        texto.contains("imbiribeira") || texto.contains("ipsep") -> GeoPoint(-8.1050, -34.9150)
        texto.contains("olinda") -> GeoPoint(-8.0100, -34.8550)
        else -> GeoPoint(-8.0578, -34.8829)
    }
    val random = Random(seed)
    val latOffset = (random.nextDouble() - 0.5) * 0.003
    val lonOffset = (random.nextDouble() - 0.5) * 0.003
    return GeoPoint(base.latitude + latOffset, base.longitude + lonOffset)
}