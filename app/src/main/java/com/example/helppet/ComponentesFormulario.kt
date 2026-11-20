package com.example.helppet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/** Proporcao da foto do pet (largura / altura) usada no quadro e no recorte enviado. */
const val PROPORCAO_FOTO = 4f / 3f

private const val IMGBB_API_KEY = "f6731151c6f72e4bc18b9ccfed8f032a"

/** Le a orientacao gravada pela camera (fotos de celular costumam vir "deitadas"). */
private fun rotacaoExif(context: Context, uri: Uri): Int = try {
    context.contentResolver.openInputStream(uri)?.use { entrada ->
        when (ExifInterface(entrada).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } ?: 0
} catch (e: Exception) {
    0
}

/** Largura / altura da foto, ja considerando a rotacao da camera. */
private fun proporcaoDaFoto(context: Context, uri: Uri): Float? = try {
    val opcoes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opcoes) }
    val girada = rotacaoExif(context, uri) % 180 != 0
    val w = if (girada) opcoes.outHeight else opcoes.outWidth
    val h = if (girada) opcoes.outWidth else opcoes.outHeight
    if (w > 0 && h > 0) w.toFloat() / h else null
} catch (e: Exception) {
    null
}

/**
 * Recorta a foto no enquadramento escolhido (vies de -1 a 1 em cada eixo),
 * reduz o tamanho e envia para o ImgBB. Devolve o link da imagem ou null.
 */
suspend fun uploadFotoRecortada(context: Context, uri: Uri, vies: Offset): String? = withContext(Dispatchers.IO) {
    try {
        // 1. Le a foto ja reduzida (evita estourar a memoria com fotos de 12+ megapixels)
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
        var amostra = 1
        while (maxOf(limites.outWidth, limites.outHeight) / (amostra * 2) >= 2048) amostra *= 2
        val opcoes = BitmapFactory.Options().apply { inSampleSize = amostra }
        var foto = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opcoes) }
            ?: return@withContext null

        // 2. Corrige a rotacao da camera
        val rotacao = rotacaoExif(context, uri)
        if (rotacao != 0) {
            val matriz = Matrix().apply { postRotate(rotacao.toFloat()) }
            foto = Bitmap.createBitmap(foto, 0, 0, foto.width, foto.height, matriz, true)
        }

        // 3. Recorta no formato 4:3 usando a posicao que a pessoa arrastou
        val proporcao = foto.width.toFloat() / foto.height
        val (larguraCorte, alturaCorte) = if (proporcao > PROPORCAO_FOTO) {
            (foto.height * PROPORCAO_FOTO).toInt() to foto.height
        } else {
            foto.width to (foto.width / PROPORCAO_FOTO).toInt()
        }
        val x = ((foto.width - larguraCorte) * (vies.x + 1f) / 2f).toInt().coerceIn(0, foto.width - larguraCorte)
        val y = ((foto.height - alturaCorte) * (vies.y + 1f) / 2f).toInt().coerceIn(0, foto.height - alturaCorte)
        var recorte = Bitmap.createBitmap(foto, x, y, larguraCorte, alturaCorte)

        // 4. Limita a 1280px de largura e comprime
        if (recorte.width > 1280) {
            recorte = Bitmap.createScaledBitmap(recorte, 1280, (1280 / PROPORCAO_FOTO).toInt(), true)
        }
        val saida = ByteArrayOutputStream()
        recorte.compress(Bitmap.CompressFormat.JPEG, 85, saida)
        val base64 = Base64.encodeToString(saida.toByteArray(), Base64.DEFAULT)

        // 5. Envia para o ImgBB
        val corpo = FormBody.Builder().add("image", base64).build()
        val pedido = Request.Builder().url("https://api.imgbb.com/1/upload?key=$IMGBB_API_KEY").post(corpo).build()
        OkHttpClient().newCall(pedido).execute().use { resposta ->
            if (!resposta.isSuccessful) return@withContext null
            JSONObject(resposta.body?.string() ?: return@withContext null).getJSONObject("data").getString("url")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Quadro da foto do pet. Sem foto: area tracejada para escolher.
 * Com foto: mostra no formato 4:3 e a pessoa arrasta para cima/baixo (ou para os lados) para enquadrar.
 */
@Composable
fun CaixaFotoAjustavel(
    fotoUri: Uri?,
    vies: Offset,
    aoMudarVies: (Offset) -> Unit,
    corDestaque: Color,
    aoEscolher: () -> Unit,
    textoVazio: String = "Adicionar foto"
) {
    val context = LocalContext.current
    val proporcaoImagem by produceState<Float?>(initialValue = null, fotoUri) {
        value = fotoUri?.let { withContext(Dispatchers.IO) { proporcaoDaFoto(context, it) } }
    }
    val viesAtual by rememberUpdatedState(vies)
    val aoMudarAtual by rememberUpdatedState(aoMudarVies)

    if (fotoUri == null) {
        val tracejado = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .drawBehind {
                    drawRoundRect(
                        color = Color(0xFF758A82),
                        style = Stroke(width = 4f, pathEffect = tracejado),
                        cornerRadius = CornerRadius(40f)
                    )
                }
                .clip(RoundedCornerShape(16.dp))
                .clickable { aoEscolher() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = Color(0xFF758A82), modifier = Modifier.size(30.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Text(textoVazio, color = Color(0xFF758A82), fontSize = 15.sp)
            }
        }
        return
    }

    Column {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PROPORCAO_FOTO)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFEDEFEE))
        ) {
            val larguraPx = constraints.maxWidth.toFloat()
            val alturaPx = constraints.maxHeight.toFloat()
            val r = proporcaoImagem
            // Quanto da foto fica "sobrando" fora do quadro (e o que da para arrastar)
            val sobraX = if (r != null && r > PROPORCAO_FOTO) alturaPx * r - larguraPx else 0f
            val sobraY = if (r != null && r < PROPORCAO_FOTO) larguraPx / r - alturaPx else 0f

            AsyncImage(
                model = fotoUri,
                contentDescription = "Foto do pet",
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(vies.x, vies.y),
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(sobraX, sobraY) {
                        detectDragGestures { mudanca, arrasto ->
                            mudanca.consume()
                            val novoX = if (sobraX > 0f) (viesAtual.x - arrasto.x / (sobraX / 2f)).coerceIn(-1f, 1f) else 0f
                            val novoY = if (sobraY > 0f) (viesAtual.y - arrasto.y / (sobraY / 2f)).coerceIn(-1f, 1f) else 0f
                            aoMudarAtual(Offset(novoX, novoY))
                        }
                    }
            )

            if (sobraX > 0f || sobraY > 0f) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.OpenWith, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Arraste para ajustar a foto", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
        TextButton(onClick = aoEscolher, modifier = Modifier.align(Alignment.End)) {
            Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = corDestaque, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Trocar foto", color = corDestaque, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Seletor de espécie: Cachorro e Gato como botoes e uma caixa para escrever outra espécie. */
@Composable
fun SeletorEspecie(especie: String, aoMudar: (String) -> Unit, corDestaque: Color) {
    val ehOutra = especie.isNotEmpty() && especie != "Cachorro" && especie != "Gato"
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        PilulaOpcao("Cachorro", especie == "Cachorro", corDestaque) { aoMudar("Cachorro") }
        PilulaOpcao("Gato", especie == "Gato", corDestaque) { aoMudar("Gato") }

        // Caixa de texto no mesmo formato dos botoes
        BasicTextField(
            value = if (ehOutra) especie else "",
            onValueChange = { aoMudar(it) },
            singleLine = true,
            textStyle = TextStyle(color = Color(0xFF1C2723), fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(corDestaque),
            modifier = Modifier.weight(1f),
            decorationBox = { campo ->
                Box(
                    modifier = Modifier
                        .height(42.dp)
                        .background(Color.White, RoundedCornerShape(21.dp))
                        .border(
                            BorderStroke(if (ehOutra) 1.5.dp else 1.dp, if (ehOutra) corDestaque else Color(0xFFBDBDBD)),
                            RoundedCornerShape(21.dp)
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (!ehOutra) Text("Outra espécie...", color = Color(0xFF9E9E9E), fontSize = 14.sp)
                    campo()
                }
            }
        )
    }
}

@Composable
fun PilulaOpcao(texto: String, selecionado: Boolean, corDestaque: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(if (selecionado) corDestaque else Color.White)
            .border(BorderStroke(1.dp, if (selecionado) corDestaque else Color(0xFFBDBDBD)), RoundedCornerShape(21.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto,
            color = if (selecionado) Color.White else Color(0xFF424242),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Escolha entre duas ou mais opcoes num unico trilho, com a opcao ativa em destaque (visual "segmentado"). */
@Composable
fun SeletorSegmentado(opcoes: List<Pair<String, String>>, selecionado: String, onSelecionar: (String) -> Unit) {
    val primaryGreen = colorResource(id = R.color.primary_green)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(Color(0xFFE9EDEB), RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        opcoes.forEach { (valor, rotulo) ->
            val ativo = valor == selecionado
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(if (ativo) Modifier.shadow(1.dp, RoundedCornerShape(10.dp)) else Modifier)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (ativo) Color.White else Color.Transparent)
                    .clickable { onSelecionar(valor) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    rotulo,
                    color = if (ativo) primaryGreen else Color(0xFF6B7470),
                    fontSize = 14.sp,
                    fontWeight = if (ativo) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
