@file:OptIn(
    androidx.media3.common.util.UnstableApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.example.novatv

import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

// ============================================================
// NovaTV v9.15.1 - Continuar visual + navegación DPAD corregida
// ============================================================

// ============================================================
// TMDb - respaldo opcional de metadatos para Películas y Series
// ============================================================
// 1) Crea una cuenta en TMDb.
// 2) En Ajustes > API copia tu "API Read Access Token".
// 3) Pégalo ENTRE las comillas de abajo.
// Si queda vacío, NovaTV funciona normalmente usando solo Xtream.
private const val TMDB_BEARER_TOKEN_NOVATV = ""

// Idioma preferido para sinopsis y metadatos de respaldo.
private const val TMDB_IDIOMA_NOVATV = "es-ES"
private const val TMDB_IMAGEN_BASE_NOVATV = "https://image.tmdb.org/t/p/"

// ============================================================
// Supabase - catálogo privado NovaTV
// ============================================================
// ============================================================
// Xtream - host base centralizado
// ============================================================
private const val XTREAM_BASE_URL_NOVATV = "https://tvgopremium.net:8443"

private const val SUPABASE_URL_NOVATV = "https://mktyemnpejqpmollyjiw.supabase.co"
private const val SUPABASE_PUBLISHABLE_KEY_NOVATV = ""
private const val SUPABASE_EMAIL_NOVATV = ""
private const val SUPABASE_PASSWORD_NOVATV = ""



// ============================================================
// Catálogo privado NovaTV - Supabase
// ============================================================
data class PeliculaNovaTV(
    val id: Long,
    val titulo: String,
    val tmdbId: Int?,
    val categorias: List<String>,
    val url: String,
    val portada: String,
    val fondo: String,
    val descripcion: String
)

private fun iniciarSesionSupabaseNovaTV(email: String, password: String): String {
    val conexion = URL("$SUPABASE_URL_NOVATV/auth/v1/token?grant_type=password")
        .openConnection() as HttpURLConnection
    try {
        conexion.requestMethod = "POST"
        conexion.connectTimeout = 15000
        conexion.readTimeout = 15000
        conexion.doOutput = true
        conexion.setRequestProperty("apikey", SUPABASE_PUBLISHABLE_KEY_NOVATV)
        conexion.setRequestProperty("Content-Type", "application/json")

        val cuerpo = JSONObject().put("email", email).put("password", password).toString()
        conexion.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(cuerpo) }

        val codigo = conexion.responseCode
        val stream = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
        val respuesta = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (codigo !in 200..299) throw Exception("Supabase Auth HTTP $codigo")

        return JSONObject(respuesta).optString("access_token", "")
            .ifBlank { throw Exception("Supabase no devolvió access_token") }
    } finally {
        conexion.disconnect()
    }
}

private fun leerCatalogoPrivadoNovaTV(token: String): List<PeliculaNovaTV> {
    val conexion = URL("$SUPABASE_URL_NOVATV/functions/v1/catalogo-novatv")
        .openConnection() as HttpURLConnection
    try {
        conexion.requestMethod = "GET"
        conexion.connectTimeout = 15000
        conexion.readTimeout = 20000
        conexion.instanceFollowRedirects = true
        conexion.setRequestProperty("apikey", SUPABASE_PUBLISHABLE_KEY_NOVATV)
        conexion.setRequestProperty("Authorization", "Bearer $token")
        conexion.setRequestProperty("Accept", "application/json")

        val codigo = conexion.responseCode
        val stream = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
        val respuesta = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (codigo !in 200..299) throw Exception("Catálogo NovaTV HTTP $codigo")

        val root = JSONObject(respuesta)
        if (!root.optBoolean("ok", false)) {
            throw Exception(root.optString("error", "Respuesta inválida del catálogo NovaTV"))
        }

        val array = root.optJSONArray("peliculas") ?: JSONArray()
        val resultado = mutableListOf<PeliculaNovaTV>()

        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val catsJson = o.optJSONArray("categorias")
            val cats = mutableListOf<String>()
            if (catsJson != null) {
                for (j in 0 until catsJson.length()) {
                    catsJson.optString(j, "").trim().takeIf { it.isNotBlank() }?.let(cats::add)
                }
            }

            resultado.add(
                PeliculaNovaTV(
                    id = o.optLong("id"),
                    titulo = o.optString("titulo", "Sin título"),
                    tmdbId = if (o.isNull("tmdb_id")) null else o.optInt("tmdb_id"),
                    categorias = cats,
                    url = o.optString("url", ""),
                    portada = o.optString("portada", ""),
                    fondo = o.optString("fondo", ""),
                    descripcion = o.optString("descripcion", "")
                )
            )
        }
        return resultado
    } finally {
        conexion.disconnect()
    }
}

fun cargarCatalogoPrivadoNovaTV(
    email: String,
    password: String,
    onResultado: (List<PeliculaNovaTV>) -> Unit,
    onError: (String) -> Unit
) {
    Thread {
        try {
            if (SUPABASE_PUBLISHABLE_KEY_NOVATV.isBlank()) {
                throw Exception("Falta configurar la Publishable Key de Supabase")
            }
            val token = iniciarSesionSupabaseNovaTV(email, password)
            onResultado(leerCatalogoPrivadoNovaTV(token))
        } catch (e: Exception) {
            onError("Error cargando catálogo NovaTV: ${e.message}")
        }
    }.start()
}


fun probarCatalogoPrivadoNovaTVAndroid(
    onResultado: (String) -> Unit
) {
    if (SUPABASE_EMAIL_NOVATV.isBlank() || SUPABASE_PASSWORD_NOVATV.isBlank()) {
        onResultado("Faltan las credenciales locales de Supabase")
        return
    }

    cargarCatalogoPrivadoNovaTV(
        email = SUPABASE_EMAIL_NOVATV,
        password = SUPABASE_PASSWORD_NOVATV,
        onResultado = { lista ->
            val cantidad = lista.size
            val palabra = if (cantidad == 1) "película" else "películas"
            onResultado("Catálogo NovaTV conectado: $cantidad $palabra")
        },
        onError = { error ->
            onResultado(error)
        }
    )
}


// ============================================================
// v9.17.6 - Lectura del catálogo privado de Series desde Supabase.
// Esta etapa no modifica todavía las pantallas de Series.
// ============================================================
data class SeriePrivadaNovaTV(
    val id: Long,
    val titulo: String,
    val tmdbId: Int?,
    val categorias: List<String>,
    val portada: String,
    val fondo: String,
    val descripcion: String
)

data class TemporadaPrivadaNovaTV(
    val id: Long,
    val serieId: Long,
    val numero: Int,
    val titulo: String
)

data class EpisodioPrivadoNovaTV(
    val id: Long,
    val temporadaId: Long,
    val numero: Int,
    val titulo: String,
    val url: String,
    val portada: String,
    val descripcion: String
)

data class CatalogoSeriesPrivadoNovaTV(
    val series: List<SeriePrivadaNovaTV>,
    val temporadas: List<TemporadaPrivadaNovaTV>,
    val episodios: List<EpisodioPrivadoNovaTV>
)

private fun leerCatalogoSeriesPrivadoNovaTV(token: String): CatalogoSeriesPrivadoNovaTV {
    val conexion = URL("$SUPABASE_URL_NOVATV/functions/v1/catalogo-novatv")
        .openConnection() as HttpURLConnection
    try {
        conexion.requestMethod = "GET"
        conexion.connectTimeout = 15000
        conexion.readTimeout = 20000
        conexion.instanceFollowRedirects = true
        conexion.setRequestProperty("apikey", SUPABASE_PUBLISHABLE_KEY_NOVATV)
        conexion.setRequestProperty("Authorization", "Bearer $token")
        conexion.setRequestProperty("Accept", "application/json")

        val codigo = conexion.responseCode
        val stream = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
        val respuesta = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (codigo !in 200..299) throw Exception("Catálogo de Series HTTP $codigo")

        val root = JSONObject(respuesta)
        if (!root.optBoolean("ok", false)) {
            throw Exception("La respuesta del catálogo no es válida")
        }
        if (!root.optBoolean("catalogo_series_completo", false)) {
            throw Exception("El catálogo de Series no está disponible completamente")
        }

        // Los campos deben existir: una función antigua no se confunde con
        // un catálogo vacío.
        val seriesJson = root.optJSONArray("series")
            ?: throw Exception("Falta la lista de series")
        val temporadasJson = root.optJSONArray("temporadas")
            ?: throw Exception("Falta la lista de temporadas")
        val episodiosJson = root.optJSONArray("episodios")
            ?: throw Exception("Falta la lista de episodios")

        val series = buildList {
            for (i in 0 until seriesJson.length()) {
                val o = seriesJson.getJSONObject(i)
                val categoriasJson = o.optJSONArray("categorias")
                val categorias = buildList {
                    if (categoriasJson != null) {
                        for (j in 0 until categoriasJson.length()) {
                            categoriasJson.optString(j, "")
                                .trim()
                                .takeIf { it.isNotEmpty() }
                                ?.let { add(it) }
                        }
                    }
                }
                add(
                    SeriePrivadaNovaTV(
                        id = o.getLong("id"),
                        titulo = o.optString("titulo", "Sin título"),
                        tmdbId = if (o.isNull("tmdb_id")) null else o.optInt("tmdb_id"),
                        categorias = categorias,
                        portada = o.optString("portada", ""),
                        fondo = o.optString("fondo", ""),
                        descripcion = o.optString("descripcion", "")
                    )
                )
            }
        }

        val temporadas = buildList {
            for (i in 0 until temporadasJson.length()) {
                val o = temporadasJson.getJSONObject(i)
                add(
                    TemporadaPrivadaNovaTV(
                        id = o.getLong("id"),
                        serieId = o.getLong("serie_id"),
                        numero = o.getInt("numero"),
                        titulo = o.optString("titulo", "")
                    )
                )
            }
        }

        val episodios = buildList {
            for (i in 0 until episodiosJson.length()) {
                val o = episodiosJson.getJSONObject(i)
                add(
                    EpisodioPrivadoNovaTV(
                        id = o.getLong("id"),
                        temporadaId = o.getLong("temporada_id"),
                        numero = o.getInt("numero"),
                        titulo = o.optString("titulo", ""),
                        url = o.optString("url", ""),
                        portada = o.optString("portada", ""),
                        descripcion = o.optString("descripcion", "")
                    )
                )
            }
        }

        return CatalogoSeriesPrivadoNovaTV(
            series = series,
            temporadas = temporadas,
            episodios = episodios
        )
    } finally {
        conexion.disconnect()
    }
}

fun cargarCatalogoPrivadoSeriesNovaTV(
    email: String,
    password: String,
    onResultado: (CatalogoSeriesPrivadoNovaTV) -> Unit,
    onError: (String) -> Unit
) {
    Thread {
        try {
            if (SUPABASE_PUBLISHABLE_KEY_NOVATV.isBlank()) {
                throw Exception("Falta configurar la Publishable Key de Supabase")
            }
            if (email.isBlank() || password.isBlank()) {
                throw Exception("Faltan las credenciales locales de Supabase")
            }
            val token = iniciarSesionSupabaseNovaTV(email, password)
            onResultado(leerCatalogoSeriesPrivadoNovaTV(token))
        } catch (e: Exception) {
            onError("Error cargando Series NovaTV: ${e.message}")
        }
    }.start()
}


// ============================================================
// v9.17.12 - TV privada NovaTV desde Supabase
// ============================================================
data class CanalPrivadoNovaTV(
    val id: Long,
    val nombre: String,
    val categorias: List<String>,
    val url: String,
    val logo: String,
    val epgId: String
)

private fun leerCanalesPrivadosNovaTV(token: String): List<CanalPrivadoNovaTV> {
    val conexion = URL("$SUPABASE_URL_NOVATV/functions/v1/catalogo-novatv")
        .openConnection() as HttpURLConnection
    try {
        conexion.requestMethod = "GET"
        conexion.connectTimeout = 15000
        conexion.readTimeout = 20000
        conexion.instanceFollowRedirects = true
        conexion.setRequestProperty("apikey", SUPABASE_PUBLISHABLE_KEY_NOVATV)
        conexion.setRequestProperty("Authorization", "Bearer $token")
        conexion.setRequestProperty("Accept", "application/json")

        val codigo = conexion.responseCode
        val stream = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
        val respuesta = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (codigo !in 200..299) throw Exception("Catálogo TV NovaTV HTTP $codigo")

        val root = JSONObject(respuesta)
        if (!root.optBoolean("ok", false)) {
            throw Exception(root.optString("error", "Respuesta inválida del catálogo"))
        }
        if (!root.optBoolean("catalogo_tv_completo", false)) {
            throw Exception("El catálogo privado de TV no está disponible completamente")
        }

        val array = root.optJSONArray("canales")
            ?: throw Exception("Falta la lista de canales NovaTV")

        return buildList {
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                val catsJson = o.optJSONArray("categorias")
                val cats = buildList {
                    if (catsJson != null) {
                        for (j in 0 until catsJson.length()) {
                            catsJson.optString(j, "")
                                .trim()
                                .takeIf { it.isNotBlank() }
                                ?.let { add(it) }
                        }
                    }
                }

                val url = o.optString("url", "").trim()
                if (url.isBlank()) continue

                add(
                    CanalPrivadoNovaTV(
                        id = o.optLong("id"),
                        nombre = o.optString("nombre", "Canal NovaTV"),
                        categorias = cats,
                        url = url,
                        logo = o.optString("logo", ""),
                        epgId = o.optString("epg_id", "")
                    )
                )
            }
        }
    } finally {
        conexion.disconnect()
    }
}

fun cargarCanalesPrivadosNovaTV(
    email: String,
    password: String,
    onResultado: (List<CanalPrivadoNovaTV>) -> Unit,
    onError: (String) -> Unit
) {
    Thread {
        try {
            if (SUPABASE_PUBLISHABLE_KEY_NOVATV.isBlank()) {
                throw Exception("Falta configurar la Publishable Key de Supabase")
            }
            if (email.isBlank() || password.isBlank()) {
                throw Exception("Faltan las credenciales locales de Supabase")
            }

            val token = iniciarSesionSupabaseNovaTV(email, password)
            onResultado(leerCanalesPrivadosNovaTV(token))
        } catch (e: Exception) {
            onError("Error cargando TV NovaTV: ${e.message}")
        }
    }.start()
}

data class CategoriaTV(
    val id: String,
    val nombre: String
)

data class CanalTV(
    val id: String,
    val nombre: String,
    val extension: String,
    val logo: String = "",
    val urlDirectaNovaTV: String = "",
    val epgIdNovaTV: String = ""
)

data class ProgramaEPGNovaTV(
    val titulo: String = "",
    val inicio: Long = 0L,
    val fin: Long = 0L
)

data class EPGCanalNovaTV(
    val ahora: ProgramaEPGNovaTV? = null,
    val siguiente: ProgramaEPGNovaTV? = null
)


data class CategoriaPelicula(
    val id: String,
    val nombre: String
)

data class Pelicula(
    val id: String,
    val nombre: String,
    val extension: String,
    val portada: String = "",
    val agregado: Long = 0L,
    val categoriaId: String = "",
    val urlDirectaNovaTV: String = "",
    val tmdbIdNovaTV: Int? = null,
    val fondoNovaTV: String = "",
    val descripcionNovaTV: String = ""
)

data class CategoriaSerie(val id: String, val nombre: String)

// v9.14: categorías internas de NovaTV. No existen en el panel Xtream.
private const val CAT_FAVORITOS_NOVATV = "__NOVATV_FAVORITOS__"
private const val CAT_RECIENTES_NOVATV = "__NOVATV_RECIENTES__"
private const val CAT_CONTINUAR_NOVATV = "__NOVATV_CONTINUAR__"
private const val CAT_PRIVADA_NOVATV = "__NOVATV_PRIVADA__"
private const val CAT_PRIVADA_TV_NOVATV = "__NOVATV_TV_PRIVADA__"
private const val CAT_PRIVADA_SERIES_NOVATV = "__NOVATV_SERIES_PRIVADAS__"

data class Serie(
    val id: String,
    val nombre: String,
    val portada: String = "",
    val agregado: Long = 0L,
    val categoriaId: String = "",
    val tmdbIdNovaTV: Int? = null,
    val fondoNovaTV: String = "",
    val descripcionNovaTV: String = ""
)

data class Episodio(
    val id: String,
    val titulo: String,
    val extension: String,
    val temporada: Int,
    val numero: Int = 0,
    val urlDirectaNovaTV: String = "",
    val portadaNovaTV: String = "",
    val descripcionNovaTV: String = ""
)

// v9.9: progreso persistente para Películas y Series.
data class ProgresoNovaTV(
    val tipo: String,
    val id: String,
    val titulo: String,
    val extension: String,
    val posicionMs: Long,
    val duracionMs: Long,
    val actualizado: Long,
    val serieId: String = "",
    val serieNombre: String = "",
    val temporada: Int = 0,
    val indiceEpisodio: Int = 0,
    val portada: String = "",
    val urlDirectaNovaTV: String = ""
)

fun cargarProgresosNovaTV(context: Context): List<ProgresoNovaTV> {
    val raw = context.getSharedPreferences("novatv_progreso", Context.MODE_PRIVATE)
        .getString("items", "[]") ?: "[]"
    return try {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(ProgresoNovaTV(
                    tipo = o.optString("tipo"), id = o.optString("id"),
                    titulo = o.optString("titulo"), extension = o.optString("extension"),
                    posicionMs = o.optLong("posicionMs"), duracionMs = o.optLong("duracionMs"),
                    actualizado = o.optLong("actualizado"),
                    serieId = o.optString("serieId", ""),
                    serieNombre = o.optString("serieNombre", ""),
                    temporada = o.optInt("temporada", 0),
                    indiceEpisodio = o.optInt("indiceEpisodio", 0),
                    portada = o.optString("portada", ""),
                    urlDirectaNovaTV = o.optString("urlDirectaNovaTV", "")
                ))
            }
        }.sortedByDescending { it.actualizado }
    } catch (_: Exception) { emptyList() }
}

fun guardarProgresosNovaTV(context: Context, datos: List<ProgresoNovaTV>) {
    val arr = JSONArray()
    datos.sortedByDescending { it.actualizado }.take(50).forEach { x ->
        arr.put(JSONObject().apply {
            put("tipo", x.tipo); put("id", x.id); put("titulo", x.titulo); put("extension", x.extension)
            put("posicionMs", x.posicionMs); put("duracionMs", x.duracionMs); put("actualizado", x.actualizado)
            put("serieId", x.serieId); put("serieNombre", x.serieNombre)
            put("temporada", x.temporada); put("indiceEpisodio", x.indiceEpisodio)
            put("portada", x.portada); put("urlDirectaNovaTV", x.urlDirectaNovaTV)
        })
    }
    context.getSharedPreferences("novatv_progreso", Context.MODE_PRIVATE).edit().putString("items", arr.toString()).apply()
}

fun actualizarProgresoNovaTV(context: Context, item: ProgresoNovaTV) {
    val actual = cargarProgresosNovaTV(context).filterNot { it.tipo == item.tipo && it.id == item.id }.toMutableList()
    val dur = item.duracionMs
    val visto = if (dur > 0) item.posicionMs.toDouble() / dur.toDouble() else 0.0
    // No guardar menos de 60 s; retirar al llegar al 95% o quedar menos de 2 min.
    if (item.posicionMs >= 60_000L && !(dur > 0 && (visto >= 0.95 || dur - item.posicionMs <= 120_000L))) actual.add(item)
    guardarProgresosNovaTV(context, actual)
}

fun borrarProgresoNovaTV(context: Context, tipo: String, id: String) {
    guardarProgresosNovaTV(context, cargarProgresosNovaTV(context).filterNot { it.tipo == tipo && it.id == id })
}

fun tiempoNovaTV(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0)
    val h = total / 3600; val m = (total % 3600) / 60; val sec = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h,m,sec) else "%d:%02d".format(m,sec)
}

// ============================================================
// v9.17.11 - Detección explícita de HLS para URLs directas NovaTV.
// Reconoce .m3u8 aunque después exista ?firma=... o #fragmento.
// ============================================================
private fun esHlsDirectoNovaTV(url: String): Boolean {
    val ruta = url
        .substringBefore('?')
        .substringBefore('#')
        .trim()
        .lowercase(Locale.ROOT)
    return ruta.endsWith(".m3u8")
}

private fun mediaItemDirectoNovaTV(url: String): MediaItem {
    val builder = MediaItem.Builder().setUri(url)
    if (esHlsDirectoNovaTV(url)) {
        builder.setMimeType(MimeTypes.APPLICATION_M3U8)
    }
    return builder.build()
}

private const val USER_AGENT_STREAM_NOVATV =
    "Mozilla/5.0 (Linux; Android 10; NovaTV) AppleWebKit/537.36 " +
    "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

data class DetallePelicula(
    val descripcion: String = "",
    val portada: String = "",
    val fondo: String = "",
    val anio: String = "",
    val duracion: String = "",
    val genero: String = "",
    val rating: String = ""
)

data class DetalleSerie(
    val descripcion: String = "",
    val portada: String = "",
    val fondo: String = "",
    val anio: String = "",
    val genero: String = "",
    val rating: String = ""
)
data class Favorito(
    val tipo: String,
    val id: String,
    val nombre: String,
    val extension: String = "",
    val urlDirectaNovaTV: String = "",
    val logoNovaTV: String = ""
)

data class ResultadoBusqueda(
    val tipo: String,
    val id: String,
    val nombre: String,
    val extension: String = ""
)

data class CuentaInfo(
    val nombre: String = "",
    val estado: String = "",
    val vencimientoEpoch: Long = 0L,
    val conexionesActivas: Int = 0,
    val conexionesMaximas: Int = 0
)

fun guardarSesion(context: Context, usuario: String, clave: String, cuenta: CuentaInfo) {
    context.getSharedPreferences("novatv_sesion", Context.MODE_PRIVATE)
        .edit()
        .putString("usuario", usuario)
        .putString("clave", clave)
        .putString("nombre", cuenta.nombre)
        .putString("estado", cuenta.estado)
        .putLong("vencimiento", cuenta.vencimientoEpoch)
        .putInt("activas", cuenta.conexionesActivas)
        .putInt("maximas", cuenta.conexionesMaximas)
        .apply()
}

fun borrarSesion(context: Context) {
    context.getSharedPreferences("novatv_sesion", Context.MODE_PRIVATE).edit().clear().apply()
}

fun guardarUltimoUsuarioNovaTV(context: Context, usuario: String) {
    context.getSharedPreferences("novatv_login_memoria", Context.MODE_PRIVATE)
        .edit()
        .putString("ultimo_usuario", usuario)
        .apply()
}

fun cargarUltimoUsuarioNovaTV(context: Context): String {
    return context.getSharedPreferences("novatv_login_memoria", Context.MODE_PRIVATE)
        .getString("ultimo_usuario", "") ?: ""
}

fun cargarCuentaGuardada(context: Context): CuentaInfo {
    val p = context.getSharedPreferences("novatv_sesion", Context.MODE_PRIVATE)
    return CuentaInfo(
        nombre = p.getString("nombre", "") ?: "",
        estado = p.getString("estado", "") ?: "",
        vencimientoEpoch = p.getLong("vencimiento", 0L),
        conexionesActivas = p.getInt("activas", 0),
        conexionesMaximas = p.getInt("maximas", 0)
    )
}

fun diasRestantes(epochSegundos: Long): Long {
    if (epochSegundos <= 0L) return -1L
    val diferencia = epochSegundos * 1000L - System.currentTimeMillis()
    if (diferencia <= 0L) return 0L
    return (diferencia + 86_399_999L) / 86_400_000L
}

fun fechaVencimiento(epochSegundos: Long): String {
    if (epochSegundos <= 0L) return "Sin fecha"
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    formato.timeZone = TimeZone.getDefault()
    return formato.format(Date(epochSegundos * 1000L))
}

class PulsacionLargaFavorito {
    var inicio: Long = 0L
    var activo: Boolean = false

    fun comenzar() {
        if (!activo) {
            inicio = SystemClock.uptimeMillis()
            activo = true
        }
    }

    fun terminar(esMismaTecla: Boolean): Boolean {
        if (!activo || !esMismaTecla) return false
        val larga = SystemClock.uptimeMillis() - inicio >= 650L
        activo = false
        return larga
    }

    fun cancelar() {
        activo = false
    }
}

fun claveFavorito(tipo: String, id: String) = "$tipo:$id"

fun cargarFavoritosGuardados(context: Context): Set<String> =
    context.getSharedPreferences("novatv_favoritos", Context.MODE_PRIVATE)
        .getStringSet("items", emptySet())?.toSet() ?: emptySet()

fun guardarFavoritos(context: Context, favoritos: Set<String>) {
    context.getSharedPreferences("novatv_favoritos", Context.MODE_PRIVATE)
        .edit().putStringSet("items", favoritos).apply()
}

fun cargarDatosFavoritos(context: Context): List<Favorito> {
    val prefs = context.getSharedPreferences("novatv_favoritos", Context.MODE_PRIVATE)
    val raw = prefs.getString("datos", "[]") ?: "[]"
    return try {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    Favorito(
                        tipo = o.optString("tipo"),
                        id = o.optString("id"),
                        nombre = o.optString("nombre"),
                        extension = o.optString("extension"),
                        urlDirectaNovaTV = o.optString("urlDirectaNovaTV", ""),
                        logoNovaTV = o.optString("logoNovaTV", "")
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

fun guardarDatosFavoritos(context: Context, datos: List<Favorito>) {
    val arr = JSONArray()
    datos.forEach { f ->
        arr.put(
            JSONObject().apply {
                put("tipo", f.tipo)
                put("id", f.id)
                put("nombre", f.nombre)
                put("extension", f.extension)
                put("urlDirectaNovaTV", f.urlDirectaNovaTV)
                put("logoNovaTV", f.logoNovaTV)
            }
        )
    }
    context.getSharedPreferences("novatv_favoritos", Context.MODE_PRIVATE)
        .edit().putString("datos", arr.toString()).apply()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NovaTVApp()
        }
    }
}

@Composable
fun NovaTVApp() {

    val activity = LocalContext.current as ComponentActivity

    val prefsSesion = remember { activity.getSharedPreferences("novatv_sesion", Context.MODE_PRIVATE) }
    val usuarioGuardado = remember { prefsSesion.getString("usuario", "") ?: "" }
    val claveGuardada = remember { prefsSesion.getString("clave", "") ?: "" }
    val ultimoUsuarioGuardado = remember { cargarUltimoUsuarioNovaTV(activity) }

    var usuario by remember {
        mutableStateOf(usuarioGuardado.ifBlank { ultimoUsuarioGuardado })
    }
    var clave by remember { mutableStateOf(claveGuardada) }
    var pantalla by remember { mutableStateOf(if (usuarioGuardado.isNotBlank() && claveGuardada.isNotBlank()) "INICIO" else "LOGIN") }
    var mensaje by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var cuentaInfo by remember { mutableStateOf(cargarCuentaGuardada(activity)) }
    var textoBusqueda by remember { mutableStateOf("") }
    var resultadosBusqueda by remember { mutableStateOf<List<ResultadoBusqueda>>(emptyList()) }
    var indiceBusqueda by remember { mutableIntStateOf(0) }
    var buscando by remember { mutableStateOf(false) }

    // Actualización manual/automática del contenido.
    val prefsContenido = remember {
        activity.getSharedPreferences("novatv_contenido", Context.MODE_PRIVATE)
    }
    var ultimaActualizacionContenido by remember {
        mutableStateOf(prefsContenido.getLong("ultima_actualizacion", 0L))
    }
    var actualizandoContenido by remember { mutableStateOf(false) }
    var estadoActualizacion by remember { mutableStateOf("") }

    var categorias by remember {
        mutableStateOf<List<CategoriaTV>>(emptyList())
    }

    var canales by remember {
        mutableStateOf<List<CanalTV>>(emptyList())
    }

    var categoriaActual by remember { mutableStateOf("") }
    var indiceCategoriaSeleccionada by remember { mutableIntStateOf(0) }
    var indiceCanalSeleccionado by remember { mutableIntStateOf(0) }
    var indiceCanalInicial by remember { mutableIntStateOf(0) }

    var categoriasPeliculas by remember { mutableStateOf<List<CategoriaPelicula>>(emptyList()) }
    var peliculas by remember { mutableStateOf<List<Pelicula>>(emptyList()) }
    var categoriaPeliculaActual by remember { mutableStateOf("") }
    var indiceCategoriaPelicula by remember { mutableIntStateOf(0) }
    var indicePelicula by remember { mutableIntStateOf(0) }

    var categoriasSeries by remember { mutableStateOf<List<CategoriaSerie>>(emptyList()) }
    var series by remember { mutableStateOf<List<Serie>>(emptyList()) }
    var catalogoSeriesPrivado by remember { mutableStateOf<CatalogoSeriesPrivadoNovaTV?>(null) }
    var episodios by remember { mutableStateOf<List<Episodio>>(emptyList()) }
    var serieActual by remember { mutableStateOf("") }
    var indiceCategoriaSerie by remember { mutableIntStateOf(0) }
    var indiceSerie by remember { mutableIntStateOf(0) }
    var indiceTemporada by remember { mutableIntStateOf(0) }
    var indiceEpisodio by remember { mutableIntStateOf(0) }
    var temporadas by remember { mutableStateOf<List<Int>>(emptyList()) }

    var detallePelicula by remember { mutableStateOf<DetallePelicula?>(null) }
    var detalleSerie by remember { mutableStateOf<DetalleSerie?>(null) }
    // v9.13: recomendaciones TMDb cruzadas únicamente con el catálogo Xtream cargado.
    var recomendacionesPeliculas by remember { mutableStateOf<List<Pelicula>>(emptyList()) }
    var recomendacionesSeries by remember { mutableStateOf<List<Serie>>(emptyList()) }
    var catalogoPeliculasRecomendacion by remember { mutableStateOf<List<Pelicula>>(emptyList()) }
    var catalogoSeriesRecomendacion by remember { mutableStateOf<List<Serie>>(emptyList()) }

    // Caché temporal en memoria: se borra al cerrar completamente NovaTV.
    val cacheCanales = remember { mutableMapOf<String, List<CanalTV>>() }
    val cachePeliculas = remember { mutableMapOf<String, List<Pelicula>>() }
    val cacheSeries = remember { mutableMapOf<String, List<Serie>>() }
    val cacheEpisodios = remember { mutableMapOf<String, List<Episodio>>() }
    val cacheDetallePeliculas = remember { mutableMapOf<String, DetallePelicula>() }
    val cacheDetalleSeries = remember { mutableMapOf<String, DetalleSerie>() }

    // Recuerda la posición dentro de cada categoría/serie durante esta sesión.
    val posicionCanales = remember { mutableMapOf<String, Int>() }
    val posicionPeliculas = remember { mutableMapOf<String, Int>() }
    val posicionSeries = remember { mutableMapOf<String, Int>() }
    val posicionTemporadas = remember { mutableMapOf<String, Int>() }
    val posicionEpisodios = remember { mutableMapOf<String, Int>() }

    // Evita lanzar dos peticiones iguales si se pulsa varias veces rápidamente.
    val cargasEnCurso = remember { mutableSetOf<String>() }
    var cargandoCategoriasTV by remember { mutableStateOf(false) }
    var cargandoCategoriasPeliculas by remember { mutableStateOf(false) }
    var cargandoCategoriasSeries by remember { mutableStateOf(false) }

    var categoriaActualId by remember { mutableStateOf("") }
    var categoriaPeliculaActualId by remember { mutableStateOf("") }
    var categoriaSerieActualId by remember { mutableStateOf("") }
    var serieActualId by remember { mutableStateOf("") }

    var favoritos by remember { mutableStateOf(cargarFavoritosGuardados(activity)) }
    var datosFavoritos by remember { mutableStateOf(cargarDatosFavoritos(activity)) }
    var progresos by remember { mutableStateOf(cargarProgresosNovaTV(activity)) }
    var progresoSeleccionado by remember { mutableStateOf<ProgresoNovaTV?>(null) }
    var indiceContinuarViendo by remember { mutableIntStateOf(0) }
    var volverDesdeOpcionesContinuar by remember { mutableStateOf("CONTINUAR_VIENDO") }
    var posicionInicialVOD by remember { mutableLongStateOf(0L) }
    var abrirEpisodiosDetalle by remember { mutableStateOf(false) }
    var indiceTipoFavorito by remember { mutableIntStateOf(0) }
    var indiceFavorito by remember { mutableIntStateOf(0) }
    var tipoFavoritoActual by remember { mutableStateOf("TV") }
    var volverDesdePlayer by remember { mutableStateOf("CATEGORIAS") }
    var volverDesdePelicula by remember { mutableStateOf("PELICULAS") }
    // v9.15.3: destino independiente al salir del reproductor de película.
    // Evita saltar la ficha/descripción y volver directamente a la categoría.
    var volverDesdePlayerPelicula by remember { mutableStateOf("PELICULAS") }
    var volverDesdeTemporadas by remember { mutableStateOf("SERIES") }

    // v9.12 Control parental
    var pinParental by remember { mutableStateOf(cargarPinParentalNovaTV(activity)) }
    var bloqueosParental by remember { mutableStateOf(cargarBloqueosParentalNovaTV(activity)) }
    var errorPinParental by remember { mutableStateOf("") }
    var destinoPinParental by remember { mutableStateOf("AJUSTES") }
    var accionParentalPendiente by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pantallaRetornoPinParental by remember { mutableStateOf("INICIO") }
    var categoriaParentalAutorizadaTemporal by remember { mutableStateOf<String?>(null) }

    fun categoriaBloqueadaNovaTV(tipo: String, id: String, nombre: String): Boolean {
        val k = claveBloqueoNovaTV(tipo, id)
        if (categoriaParentalAutorizadaTemporal == k) return false
        return categoriaAdultosNovaTV(nombre) || k in bloqueosParental
    }

    fun pedirPinNovaTV(destino: String = "ACCESO", accion: () -> Unit) {
        pantallaRetornoPinParental = pantalla
        destinoPinParental = destino
        accionParentalPendiente = accion
        errorPinParental = ""
        pantalla = "PIN_PARENTAL"
    }

    fun alternarFavorito(fav: Favorito) {
        val claveFav = claveFavorito(fav.tipo, fav.id)
        if (claveFav in favoritos) {
            favoritos = favoritos - claveFav
            datosFavoritos = datosFavoritos.filterNot { it.tipo == fav.tipo && it.id == fav.id }
        } else {
            favoritos = favoritos + claveFav
            datosFavoritos = (datosFavoritos + fav).distinctBy { claveFavorito(it.tipo, it.id) }
        }
        guardarFavoritos(activity, favoritos)
        guardarDatosFavoritos(activity, datosFavoritos)
    }

    fun actualizarContenidoNovaTV() {
        if (usuario.isBlank() || clave.isBlank() || actualizandoContenido) return

        actualizandoContenido = true
        estadoActualizacion = "Actualizando contenido..."

        // Sólo vaciamos la caché temporal. No se borran sesión, favoritos
        // ni posiciones del usuario.
        cacheCanales.clear()
        cachePeliculas.clear()
        cacheSeries.clear()
        cacheEpisodios.clear()
        cacheDetallePeliculas.clear()
        cacheDetalleSeries.clear()
        cargasEnCurso.clear()

        var pendientes = 3
        var errores = 0

        fun finalizarParte(correcto: Boolean) {
            if (!correcto) errores++
            pendientes--

            if (pendientes <= 0) {
                actualizandoContenido = false

                if (errores == 0) {
                    val ahora = System.currentTimeMillis()
                    ultimaActualizacionContenido = ahora
                    prefsContenido.edit()
                        .putLong("ultima_actualizacion", ahora)
                        .apply()
                    estadoActualizacion = "✅ Actualizado con éxito"
                } else {
                    estadoActualizacion = "⚠️ No se pudo actualizar todo el contenido"
                }
            }
        }

        cargandoCategoriasTV = true
        cargarCategorias(
            usuario = usuario,
            clave = clave,
            onResultado = { lista ->
                activity.runOnUiThread {
                    cargandoCategoriasTV = false
                    categorias = lista
                    if (indiceCategoriaSeleccionada !in lista.indices) {
                        indiceCategoriaSeleccionada = 0
                    }
                    finalizarParte(true)
                }
            },
            onError = {
                activity.runOnUiThread {
                    cargandoCategoriasTV = false
                    finalizarParte(false)
                }
            }
        )

        cargandoCategoriasPeliculas = true
        cargarCategoriasPeliculas(
            usuario = usuario,
            clave = clave,
            onResultado = { lista ->
                activity.runOnUiThread {
                    cargandoCategoriasPeliculas = false
                    categoriasPeliculas = lista
                    if (indiceCategoriaPelicula !in lista.indices) {
                        indiceCategoriaPelicula = 0
                    }
                    finalizarParte(true)
                }
            },
            onError = {
                activity.runOnUiThread {
                    cargandoCategoriasPeliculas = false
                    finalizarParte(false)
                }
            }
        )

        cargandoCategoriasSeries = true
        cargarCategoriasSeries(
            usuario,
            clave,
            onResultado = { lista ->
                activity.runOnUiThread {
                    cargandoCategoriasSeries = false
                    categoriasSeries = lista
                    if (indiceCategoriaSerie !in lista.indices) {
                        indiceCategoriaSerie = 0
                    }
                    finalizarParte(true)
                }
            },
            onError = {
                activity.runOnUiThread {
                    cargandoCategoriasSeries = false
                    finalizarParte(false)
                }
            }
        )
    }

    // Revisión automática: mientras NovaTV siga abierta, comprueba cada minuto
    // si han pasado 4 horas desde la última actualización correcta.
    LaunchedEffect(usuario, clave) {
        if (usuario.isNotBlank() && clave.isNotBlank()) {
            delay(5000)

            val intervaloActualizacion = 4L * 60L * 60L * 1000L

            while (true) {
                val ahora = System.currentTimeMillis()
                val tocaActualizar =
                    ultimaActualizacionContenido == 0L ||
                            ahora - ultimaActualizacionContenido >= intervaloActualizacion

                if (
                    tocaActualizar &&
                    !actualizandoContenido &&
                    !cargandoCategoriasTV &&
                    !cargandoCategoriasPeliculas &&
                    !cargandoCategoriasSeries
                ) {
                    actualizarContenidoNovaTV()
                }

                delay(60_000)
            }
        }
    }

    // v9.14: Favoritos viven dentro de cada sección y Recientes dentro de Películas/Series.
    fun peliculasFavoritasSeccionNovaTV(): List<Pelicula> {
        val ids = datosFavoritos.filter { it.tipo == "PELICULA" }.map { it.id }.toSet()
        val conocidas = (catalogoPeliculasRecomendacion + cachePeliculas.values.flatten() + peliculas).associateBy { it.id }
        return datosFavoritos.filter { it.tipo == "PELICULA" }.map { f ->
            conocidas[f.id] ?: Pelicula(f.id, f.nombre, f.extension.ifBlank { "mp4" })
        }.distinctBy { it.id }
    }
    fun seriesFavoritasSeccionNovaTV(): List<Serie> {
        val conocidas = (catalogoSeriesRecomendacion + cacheSeries.values.flatten() + series).associateBy { it.id }
        return datosFavoritos.filter { it.tipo == "SERIE" }.map { f ->
            conocidas[f.id] ?: Serie(f.id, f.nombre)
        }.distinctBy { it.id }
    }
    fun canalesFavoritosSeccionNovaTV(): List<CanalTV> {
        val conocidas = (cacheCanales.values.flatten() + canales).associateBy { it.id }
        return datosFavoritos.filter { it.tipo == "TV" }.map { f ->
            conocidas[f.id] ?: CanalTV(
                id = f.id,
                nombre = f.nombre,
                extension = f.extension.ifBlank { "ts" },
                logo = f.logoNovaTV,
                urlDirectaNovaTV = f.urlDirectaNovaTV
            )
        }.distinctBy { it.id }
    }
    fun peliculasRecientesSeccionNovaTV(): List<Pelicula> {
        val base = catalogoPeliculasRecomendacion.distinctBy { it.id }.filter { p ->
            val cat = categoriasPeliculas.firstOrNull { it.id == p.categoriaId }
            cat == null || !categoriaBloqueadaNovaTV("PEL", cat.id, cat.nombre)
        }
        return if (base.any { it.agregado > 0L }) base.sortedByDescending { it.agregado }.take(80) else base.take(80)
    }
    fun seriesRecientesSeccionNovaTV(): List<Serie> {
        val base = catalogoSeriesRecomendacion.distinctBy { it.id }.filter { x ->
            val cat = categoriasSeries.firstOrNull { it.id == x.categoriaId }
            cat == null || !categoriaBloqueadaNovaTV("SER", cat.id, cat.nombre)
        }
        return if (base.any { it.agregado > 0L }) base.sortedByDescending { it.agregado }.take(80) else base.take(80)
    }

    when (pantalla) {

        "LOGIN" -> {
            PantallaLogin(
                usuario = usuario,
                clave = clave,
                cargando = cargando,
                mensaje = mensaje,
                onUsuarioChange = {
                    usuario = it
                    mensaje = ""
                },
                onClaveChange = {
                    clave = it
                    mensaje = ""
                },
                onLogin = {
                    if (usuario.isBlank() || clave.isBlank()) {
                        mensaje = "Ingresa usuario y contraseña"
                    } else {
                        cargando = true
                        mensaje = ""

                        validarLogin(
                            usuario = usuario,
                            clave = clave,
                            onCorrecto = { cuenta ->
                                activity.runOnUiThread {
                                    cuentaInfo = cuenta
                                    guardarSesion(activity, usuario, clave, cuenta)
                                    guardarUltimoUsuarioNovaTV(activity, usuario)
                                    cargando = false
                                    pantalla = "INICIO"
                                    mensaje = ""
                                    indiceCategoriaSeleccionada = 0
                                }

                                if (!cargandoCategoriasTV && categorias.isEmpty()) {
                                    cargandoCategoriasTV = true
                                    cargarCategorias(
                                        usuario = usuario,
                                        clave = clave,
                                        onResultado = { lista ->
                                            activity.runOnUiThread {
                                                cargandoCategoriasTV = false
                                                categorias = lista
                                                if (indiceCategoriaSeleccionada !in lista.indices) {
                                                    indiceCategoriaSeleccionada = 0
                                                }
                                            }
                                        },
                                        onError = { error ->
                                            activity.runOnUiThread {
                                                cargandoCategoriasTV = false
                                                mensaje = error
                                            }
                                        }
                                    )
                                }
                            },
                            onError = { error ->
                                activity.runOnUiThread {
                                    cargando = false
                                    mensaje = error
                                }
                            }
                        )
                    }
                }
            )
        }

        "INICIO" -> {
            // v9.10.1: recuperar portadas de elementos guardados antes de v9.10.
            LaunchedEffect(progresos.map { it.tipo + ":" + it.id + ":" + it.portada }) {
                progresos.filter { it.portada.isBlank() }.forEach { p ->
                    completarPortadaProgresoNovaTV(p) { portada ->
                        activity.runOnUiThread {
                            val actualizado = p.copy(portada = portada)
                            actualizarProgresoNovaTV(activity, actualizado)
                            progresos = cargarProgresosNovaTV(activity)
                        }
                    }
                }
            }

            PantallaInicioNovaTV(
                progresos = emptyList(), // v9.15: Continuar vive dentro de Películas/Series
                onSeleccionarContinuar = { p ->
                    progresoSeleccionado = p
                    volverDesdeOpcionesContinuar = "INICIO"
                    pantalla = "OPCIONES_CONTINUAR"
                },
                onTV = {
                    mensaje = ""
                    pantalla = "CATEGORIAS"

                    if (categorias.isEmpty() && !cargandoCategoriasTV) {
                        cargandoCategoriasTV = true
                        mensaje = "Cargando categorías..."
                        cargarCategorias(
                            usuario = usuario,
                            clave = clave,
                            onResultado = { lista ->
                                activity.runOnUiThread {
                                    cargandoCategoriasTV = false
                                    categorias = lista
                                    if (indiceCategoriaSeleccionada !in lista.indices) {
                                        indiceCategoriaSeleccionada = 0
                                    }
                                    mensaje = if (lista.isEmpty()) "No se encontraron categorías" else ""
                                }
                            },
                            onError = { error ->
                                activity.runOnUiThread {
                                    cargandoCategoriasTV = false
                                    mensaje = error
                                }
                            }
                        )
                    }
                },
                onPeliculas = {
                    mensaje = ""
                    pantalla = "CATEGORIAS_PELICULAS"
                    if (catalogoPeliculasRecomendacion.isEmpty()) {
                        cargarCatalogoPeliculasRecomendacionNovaTV(usuario, clave) { catalogo ->
                            activity.runOnUiThread { catalogoPeliculasRecomendacion = catalogo }
                        }
                    }

                    if (categoriasPeliculas.isEmpty() && !cargandoCategoriasPeliculas) {
                        cargandoCategoriasPeliculas = true
                        mensaje = "Cargando categorías de películas..."
                        cargarCategoriasPeliculas(
                            usuario = usuario,
                            clave = clave,
                            onResultado = { lista ->
                                activity.runOnUiThread {
                                    cargandoCategoriasPeliculas = false
                                    categoriasPeliculas = lista
                                    if (indiceCategoriaPelicula !in lista.indices) {
                                        indiceCategoriaPelicula = 0
                                    }
                                    mensaje = if (lista.isEmpty()) "No se encontraron categorías de películas" else ""
                                }
                            },
                            onError = { error ->
                                activity.runOnUiThread {
                                    cargandoCategoriasPeliculas = false
                                    mensaje = error
                                }
                            }
                        )
                    }
                },
                onSeries = {
                    mensaje = ""
                    pantalla = "CATEGORIAS_SERIES"
                    if (catalogoSeriesRecomendacion.isEmpty()) {
                        cargarCatalogoSeriesRecomendacionNovaTV(usuario, clave) { catalogo ->
                            activity.runOnUiThread { catalogoSeriesRecomendacion = catalogo }
                        }
                    }

                    if (categoriasSeries.isEmpty() && !cargandoCategoriasSeries) {
                        cargandoCategoriasSeries = true
                        mensaje = "Cargando categorías de series..."
                        cargarCategoriasSeries(
                            usuario,
                            clave,
                            onResultado = { lista ->
                                activity.runOnUiThread {
                                    cargandoCategoriasSeries = false
                                    categoriasSeries = lista
                                    if (indiceCategoriaSerie !in lista.indices) {
                                        indiceCategoriaSerie = 0
                                    }
                                    mensaje = if (lista.isEmpty()) "No se encontraron categorías de series" else ""
                                }
                            },
                            onError = { e ->
                                activity.runOnUiThread {
                                    cargandoCategoriasSeries = false
                                    mensaje = e
                                }
                            }
                        )
                    }
                },
                onFavoritos = {
                    indiceTipoFavorito = 0
                    indiceFavorito = 0
                    mensaje = ""
                    pantalla = "TIPOS_FAVORITOS"
                },
                onBuscar = {
                    textoBusqueda = ""
                    resultadosBusqueda = emptyList()
                    indiceBusqueda = 0
                    mensaje = ""
                    pantalla = "BUSQUEDA"
                },
                onActualizar = {
                    actualizarContenidoNovaTV()
                },
                actualizandoContenido = actualizandoContenido,
                estadoActualizacion = estadoActualizacion,
                ultimaActualizacion = ultimaActualizacionContenido,
                onParental = {
                    // Cargamos las tres familias para que el administrador pueda elegir bloqueos manuales.
                    if (categorias.isEmpty() && !cargandoCategoriasTV) { cargandoCategoriasTV=true; cargarCategorias(usuario, clave, { l -> activity.runOnUiThread { categorias=l; cargandoCategoriasTV=false } }, { _ -> activity.runOnUiThread { cargandoCategoriasTV=false } }) }
                    if (categoriasPeliculas.isEmpty() && !cargandoCategoriasPeliculas) { cargandoCategoriasPeliculas=true; cargarCategoriasPeliculas(usuario, clave, { l -> activity.runOnUiThread { categoriasPeliculas=l; cargandoCategoriasPeliculas=false } }, { _ -> activity.runOnUiThread { cargandoCategoriasPeliculas=false } }) }
                    if (categoriasSeries.isEmpty() && !cargandoCategoriasSeries) { cargandoCategoriasSeries=true; cargarCategoriasSeries(usuario, clave, { l -> activity.runOnUiThread { categoriasSeries=l; cargandoCategoriasSeries=false } }, { _ -> activity.runOnUiThread { cargandoCategoriasSeries=false } }) }
                    errorPinParental = ""
                    if (pinParental.isBlank()) { destinoPinParental = "CREAR"; accionParentalPendiente = { pantalla = "CONTROL_PARENTAL" }; pantalla = "PIN_PARENTAL" }
                    else pedirPinNovaTV("AJUSTES") { pantalla = "CONTROL_PARENTAL" }
                },
                onCuenta = {
                    mensaje = ""
                    pantalla = "CUENTA"
                    actualizarCuenta(
                        usuario = usuario,
                        clave = clave,
                        onResultado = { cuenta ->
                            activity.runOnUiThread {
                                cuentaInfo = cuenta
                                guardarSesion(activity, usuario, clave, cuenta)
                            }
                        },
                        onError = { error -> activity.runOnUiThread { mensaje = error } }
                    )
                },
                mensaje = mensaje
            )
        }

        "CONTINUAR_VIENDO" -> {
            PantallaContinuarViendoNovaTV(
                progresos = progresos,
                indiceSeleccionado = indiceContinuarViendo,
                onIndiceChange = { indiceContinuarViendo = it },
                onVolver = { pantalla = "INICIO" },
                onSeleccionar = { p ->
                    progresoSeleccionado = p
                    volverDesdeOpcionesContinuar = "CONTINUAR_VIENDO"
                    pantalla = "OPCIONES_CONTINUAR"
                }
            )
        }

        "OPCIONES_CONTINUAR" -> {
            progresoSeleccionado?.let { p ->
                PantallaOpcionesContinuarNovaTV(
                    progreso = p,
                    onVolver = { pantalla = volverDesdeOpcionesContinuar },
                    onContinuar = { posicionInicialVOD = p.posicionMs; pantalla = "PLAYER_CONTINUAR" },
                    onDesdeInicio = { posicionInicialVOD = 0L; pantalla = "PLAYER_CONTINUAR" }
                )
            } ?: run { pantalla = "INICIO" }
        }

        "PLAYER_CONTINUAR" -> {
            progresoSeleccionado?.let { p ->
                val guardar: (Long, Long) -> Unit = { pos, dur ->
                    val nombreSerieProgreso = p.titulo.substringBefore(" · ")
                    val contextoSerieValido = p.tipo == "EPISODIO" && serieActualId.isNotBlank() &&
                        serieActual.equals(nombreSerieProgreso, ignoreCase = true)
                    actualizarProgresoNovaTV(
                        activity,
                        p.copy(
                            posicionMs = pos,
                            duracionMs = dur,
                            actualizado = System.currentTimeMillis(),
                            serieId = p.serieId.ifBlank { if (contextoSerieValido) serieActualId else "" },
                            serieNombre = p.serieNombre.ifBlank { if (contextoSerieValido) serieActual else "" },
                            temporada = if (p.temporada != 0) p.temporada else if (contextoSerieValido) episodios.firstOrNull { it.id == p.id }?.temporada ?: 0 else 0,
                            indiceEpisodio = if (p.indiceEpisodio != 0) p.indiceEpisodio else if (contextoSerieValido) {
                                val temp = episodios.firstOrNull { it.id == p.id }?.temporada
                                episodios.filter { it.temporada == temp }.indexOfFirst { it.id == p.id }.coerceAtLeast(0)
                            } else 0
                        )
                    )
                    progresos = cargarProgresosNovaTV(activity)
                }
                if (p.tipo == "PELICULA") {
                    ReproductorPeliculaNovaTV(usuario, clave, Pelicula(p.id,p.titulo,p.extension,p.portada), posicionInicialVOD, guardar) { pantalla = volverDesdeOpcionesContinuar }
                } else {
                    ReproductorEpisodioNovaTV(
                        usuario,
                        clave,
                        Episodio(
                            p.id,
                            p.titulo.substringAfter(" · ", p.titulo),
                            p.extension,
                            p.temporada,
                            urlDirectaNovaTV = p.urlDirectaNovaTV
                        ),
                        posicionInicialVOD,
                        guardar
                    ) {
                        val nombreSerieProgreso = p.serieNombre.ifBlank { p.titulo.substringBefore(" · ") }
                        val serieIdDestino = p.serieId.ifBlank {
                            if (serieActualId.isNotBlank() && serieActual.equals(nombreSerieProgreso, ignoreCase = true)) serieActualId else ""
                        }
                        if (serieIdDestino.startsWith("NOVATV_SERIE_")) {
                            pantalla = volverDesdeOpcionesContinuar
                        } else if (serieIdDestino.isNotBlank()) {
                            val serieContinuar = Serie(serieIdDestino, nombreSerieProgreso)
                            serieActualId = serieContinuar.id
                            serieActual = serieContinuar.nombre
                            series = listOf(serieContinuar)
                            indiceSerie = 0
                            mensaje = "Cargando episodios..."
                            cargarDetalleSerieCompleto(
                                usuario = usuario,
                                clave = clave,
                                serie = serieContinuar,
                                onResultado = { detalle, lista ->
                                    activity.runOnUiThread {
                                        detalleSerie = detalle
                                        episodios = lista
                                        temporadas = lista.map { it.temporada }.distinct().sorted()
                                        indiceTemporada = temporadas.indexOf(p.temporada).takeIf { it >= 0 } ?: 0
                                        val temporadaElegida = temporadas.getOrNull(indiceTemporada)
                                        val eps = if (temporadaElegida != null) lista.filter { it.temporada == temporadaElegida } else emptyList()
                                        indiceEpisodio = eps.indexOfFirst { it.id == p.id }.takeIf { it >= 0 }
                                            ?: p.indiceEpisodio.coerceIn(0, (eps.size - 1).coerceAtLeast(0))
                                        abrirEpisodiosDetalle = true
                                        mensaje = ""
                                        pantalla = "DETALLE_SERIE"
                                    }
                                },
                                onError = { error ->
                                    activity.runOnUiThread {
                                        mensaje = error
                                        pantalla = volverDesdeOpcionesContinuar
                                    }
                                }
                            )
                        } else {
                            pantalla = volverDesdeOpcionesContinuar
                        }
                    }
                }
            } ?: run { pantalla = "INICIO" }
        }

        "BUSQUEDA" -> {
            PantallaBusquedaNovaTV(
                texto = textoBusqueda,
                resultados = resultadosBusqueda,
                indiceSeleccionado = indiceBusqueda,
                buscando = buscando,
                mensaje = mensaje,
                onTextoChange = { textoBusqueda = it },
                onIndiceChange = { indiceBusqueda = it },
                onVolver = { pantalla = "INICIO" },
                onBuscar = {
                    if (textoBusqueda.trim().length < 2) {
                        mensaje = "Escribe al menos 2 letras"
                    } else {
                        buscando = true
                        mensaje = "Buscando..."
                        buscarTodoNovaTV(
                            usuario = usuario,
                            clave = clave,
                            texto = textoBusqueda.trim(),
                            onResultado = { lista ->
                                activity.runOnUiThread {
                                    resultadosBusqueda = lista
                                    indiceBusqueda = 0
                                    buscando = false
                                    mensaje = if (lista.isEmpty()) "No se encontraron resultados" else ""
                                }
                            },
                            onError = { error ->
                                activity.runOnUiThread {
                                    buscando = false
                                    mensaje = error
                                }
                            }
                        )
                    }
                },
                onAbrir = { r ->
                    when (r.tipo) {
                        "TV" -> {
                            canales = listOf(CanalTV(r.id, r.nombre, r.extension.ifBlank { "ts" }))
                            indiceCanalInicial = 0
                            indiceCanalSeleccionado = 0
                            volverDesdePlayer = "BUSQUEDA"
                            pantalla = "PLAYER"
                        }
                        "PELICULA" -> {
                            peliculas = listOf(Pelicula(r.id, r.nombre, r.extension.ifBlank { "mp4" }))
                            indicePelicula = 0
                            volverDesdePelicula = "BUSQUEDA"
                            volverDesdePlayerPelicula = "BUSQUEDA"
                            pantalla = "PLAYER_PELICULA"
                        }
                        "SERIE" -> {
                            serieActual = r.nombre
                            mensaje = "Cargando temporadas..."
                            volverDesdeTemporadas = "BUSQUEDA"
                            cargarEpisodiosSerie(
                                usuario, clave, r.id,
                                onResultado = { listaEps -> activity.runOnUiThread {
                                    episodios = listaEps
                                    temporadas = listaEps.map { it.temporada }.distinct().sorted()
                                    indiceTemporada = 0
                                    mensaje = if (listaEps.isEmpty()) "No se encontraron episodios" else ""
                                    pantalla = "TEMPORADAS"
                                }},
                                onError = { e -> activity.runOnUiThread { mensaje = e } }
                            )
                        }
                    }
                }
            )
        }


        "PIN_PARENTAL" -> {
            PantallaPinParentalNovaTV(
                creando = pinParental.isBlank() || destinoPinParental == "CREAR_PIN",
                error = errorPinParental,
                onVolver = {
                    accionParentalPendiente = null
                    errorPinParental = ""
                    pantalla = pantallaRetornoPinParental
                },
                onAceptar = { pinIngresado ->
                    if (pinParental.isBlank() || destinoPinParental == "CREAR_PIN") {
                        guardarPinParentalNovaTV(activity, pinIngresado); pinParental = pinIngresado; errorPinParental = ""
                        val accion = accionParentalPendiente; accionParentalPendiente = null; accion?.invoke() ?: run { pantalla = "CONTROL_PARENTAL" }
                    } else if (pinIngresado == pinParental) {
                        errorPinParental = ""; val accion = accionParentalPendiente; accionParentalPendiente = null; accion?.invoke() ?: run { pantalla = "INICIO" }
                    } else errorPinParental = "PIN incorrecto"
                }
            )
        }

        "CONTROL_PARENTAL" -> {
            val catsParental = categorias.map { CategoriaParentalNovaTV("TV", it.id, it.nombre) } +
                categoriasPeliculas.map { CategoriaParentalNovaTV("PEL", it.id, it.nombre) } +
                categoriasSeries.map { CategoriaParentalNovaTV("SER", it.id, it.nombre) }
            PantallaControlParentalNovaTV(
                categorias = catsParental, bloqueos = bloqueosParental,
                onToggle = { cat ->
                    if (!categoriaAdultosNovaTV(cat.nombre)) {
                        val k = claveBloqueoNovaTV(cat.tipo, cat.id)
                        bloqueosParental = if (k in bloqueosParental) bloqueosParental - k else bloqueosParental + k
                        guardarBloqueosParentalNovaTV(activity, bloqueosParental)
                    }
                },
                onCambiarPin = { destinoPinParental = "CREAR_PIN"; accionParentalPendiente = { pantalla = "CONTROL_PARENTAL" }; pantalla = "PIN_PARENTAL" },
                onVolver = { pantalla = "INICIO" }
            )
        }

        "CUENTA" -> {
            PantallaCuentaNovaTV(
                cuenta = cuentaInfo,
                mensaje = mensaje,
                onVolver = { pantalla = "INICIO" },
                onCerrarSesion = {
                    borrarSesion(activity)
                    prefsContenido.edit().clear().apply()
                    ultimaActualizacionContenido = 0L
                    actualizandoContenido = false
                    estadoActualizacion = ""
                    // Conservamos solo el último nombre de usuario para facilitar
                    // el próximo inicio. La contraseña se borra al cerrar sesión.
                    usuario = cargarUltimoUsuarioNovaTV(activity)
                    clave = ""
                    cuentaInfo = CuentaInfo()

                    cacheCanales.clear()
                    cachePeliculas.clear()
                    cacheSeries.clear()
                    cacheEpisodios.clear()
                    posicionCanales.clear()
                    posicionPeliculas.clear()
                    posicionSeries.clear()
                    posicionTemporadas.clear()
                    posicionEpisodios.clear()
                    cargasEnCurso.clear()
                    categorias = emptyList()
                    categoriasPeliculas = emptyList()
                    categoriasSeries = emptyList()
                    canales = emptyList()
                    peliculas = emptyList()
                    series = emptyList()
                    episodios = emptyList()

                    mensaje = ""
                    pantalla = "LOGIN"
                }
            )
        }

        "TIPOS_FAVORITOS" -> {
            val tipos = listOf("📺 CANALES", "🎬 PELÍCULAS", "📺 SERIES")
            PantallaListaSimple(
                titulo = "⭐ FAVORITOS",
                subtitulo = "Elige una sección",
                nombres = tipos,
                mensaje = "",
                indiceSeleccionado = indiceTipoFavorito,
                onIndiceChange = { indiceTipoFavorito = it },
                onVolver = { pantalla = "INICIO" },
                onSeleccionar = { i ->
                    tipoFavoritoActual = when(i) {
                        0 -> "TV"
                        1 -> "PELICULA"
                        else -> "SERIE"
                    }
                    indiceFavorito = 0
                    pantalla = "FAVORITOS_LISTA"
                }
            )
        }

        "FAVORITOS_LISTA" -> {
            val lista = datosFavoritos.filter { it.tipo == tipoFavoritoActual }
            PantallaFavoritos(
                favoritos = lista,
                indiceSeleccionado = indiceFavorito,
                onIndiceChange = { indiceFavorito = it },
                onVolver = { pantalla = "TIPOS_FAVORITOS" },
                onQuitar = { fav -> alternarFavorito(fav) },
                onAbrir = { fav ->
                    when (fav.tipo) {
                        "TV" -> {
                            val listaTv = datosFavoritos.filter { it.tipo == "TV" }.map {
                                CanalTV(it.id, it.nombre, it.extension.ifBlank { "ts" })
                            }
                            val idx = listaTv.indexOfFirst { it.id == fav.id }.coerceAtLeast(0)
                            canales = listaTv
                            indiceCanalInicial = idx
                            indiceCanalSeleccionado = idx
                            volverDesdePlayer = "FAVORITOS_LISTA"
                            pantalla = "PLAYER"
                        }
                        "PELICULA" -> {
                            val listaPel = datosFavoritos.filter { it.tipo == "PELICULA" }.map {
                                Pelicula(it.id, it.nombre, it.extension.ifBlank { "mp4" })
                            }
                            peliculas = listaPel
                            indicePelicula = listaPel.indexOfFirst { it.id == fav.id }.coerceAtLeast(0)
                            volverDesdePelicula = "FAVORITOS_LISTA"
                            volverDesdePlayerPelicula = "FAVORITOS_LISTA"
                            pantalla = "PLAYER_PELICULA"
                        }
                        "SERIE" -> {
                            serieActual = fav.nombre
                            mensaje = "Cargando temporadas..."
                            volverDesdeTemporadas = "FAVORITOS_LISTA"
                            cargarEpisodiosSerie(
                                usuario, clave, fav.id,
                                onResultado = { listaEps -> activity.runOnUiThread {
                                    episodios = listaEps
                                    temporadas = listaEps.map { it.temporada }.distinct().sorted()
                                    indiceTemporada = 0
                                    mensaje = if (listaEps.isEmpty()) "No se encontraron episodios" else ""
                                    pantalla = "TEMPORADAS"
                                }},
                                onError = { e -> activity.runOnUiThread { mensaje = e } }
                            )
                        }
                    }
                }
            )
        }

        "CATEGORIAS_PELICULAS" -> {
            val categoriasPeliculasVista = listOf(
                CategoriaPelicula(CAT_CONTINUAR_NOVATV, "▶ CONTINUAR VIENDO"),
                CategoriaPelicula(CAT_FAVORITOS_NOVATV, "⭐ FAVORITOS"),
                CategoriaPelicula(CAT_RECIENTES_NOVATV, "🆕 RECIENTEMENTE AÑADIDAS"),
                CategoriaPelicula(CAT_PRIVADA_NOVATV, "✨ NOVATV")
            ) + categoriasPeliculas
            PantallaCategoriasPeliculas(
                categorias = categoriasPeliculasVista,
                peliculas = peliculas,
                mensaje = mensaje,
                indiceSeleccionado = indiceCategoriaPelicula,
                indicePeliculaSeleccionada = indicePelicula,
                onIndiceChange = { nuevoIndice ->
                    indiceCategoriaPelicula = nuevoIndice
                    categoriasPeliculasVista.getOrNull(nuevoIndice)?.let { cat ->
                        if (categoriaParentalAutorizadaTemporal != claveBloqueoNovaTV("PEL", cat.id)) categoriaParentalAutorizadaTemporal = null
                        if (categoriaBloqueadaNovaTV("PEL", cat.id, cat.nombre)) {
                            categoriaPeliculaActual = cat.nombre
                            categoriaPeliculaActualId = ""
                            peliculas = emptyList()
                            indicePelicula = 0
                            mensaje = "🔒 Categoría protegida · Pulsa OK para ingresar"
                        }
                    }
                },
                onIndicePeliculaChange = {
                    indicePelicula = it
                    if (categoriaPeliculaActualId.isNotBlank()) posicionPeliculas[categoriaPeliculaActualId] = it
                },
                onVolver = {
                    categoriaParentalAutorizadaTemporal = null
                    mensaje = ""
                    pantalla = "INICIO"
                },
                favoritos = favoritos,
                onFavorito = { pelicula ->
                    alternarFavorito(Favorito("PELICULA", pelicula.id, pelicula.nombre, pelicula.extension))
                },
                esCategoriaBloqueada = { cat -> cat.id != CAT_CONTINUAR_NOVATV && cat.id != CAT_FAVORITOS_NOVATV && cat.id != CAT_RECIENTES_NOVATV && cat.id != CAT_PRIVADA_NOVATV && categoriaBloqueadaNovaTV("PEL", cat.id, cat.nombre) },
                onAbrirPelicula = { indice ->
                    if (categoriaPeliculaActualId == CAT_CONTINUAR_NOVATV) {
                        peliculas.getOrNull(indice)?.let { pel ->
                            progresos.filter { it.tipo == "PELICULA" }.firstOrNull { it.id == pel.id }?.let { p ->
                                progresoSeleccionado = p
                                volverDesdeOpcionesContinuar = "CATEGORIAS_PELICULAS"
                                pantalla = "OPCIONES_CONTINUAR"
                            }
                        }
                        return@PantallaCategoriasPeliculas
                    }
                    indicePelicula = indice
                    if (categoriaPeliculaActualId.isNotBlank()) posicionPeliculas[categoriaPeliculaActualId] = indice
                    volverDesdePelicula = "CATEGORIAS_PELICULAS"
                    detallePelicula = cacheDetallePeliculas[peliculas[indice].id]
                    pantalla = "DETALLE_PELICULA"
                    recomendacionesPeliculas = emptyList()
                    val seleccionRecomendacion = peliculas[indice]
                    val lanzarRecsPelicula: (List<Pelicula>) -> Unit = { catalogo ->
                        cargarRecomendacionesPeliculasNovaTV(seleccionRecomendacion, catalogo) { lista -> activity.runOnUiThread { if (peliculas.getOrNull(indicePelicula)?.id == seleccionRecomendacion.id) recomendacionesPeliculas = lista } }
                    }
                    if (categoriaPeliculaActualId != CAT_PRIVADA_NOVATV) {
                        if (catalogoPeliculasRecomendacion.isNotEmpty()) lanzarRecsPelicula(catalogoPeliculasRecomendacion)
                        else cargarCatalogoPeliculasRecomendacionNovaTV(usuario, clave) { catalogo -> activity.runOnUiThread { catalogoPeliculasRecomendacion = catalogo; lanzarRecsPelicula(catalogo) } }
                    }

                    if (detallePelicula == null) {
                        mensaje = "Cargando información..."
                        val seleccion = peliculas[indice]
                        cargarDetallePelicula(
                            usuario = usuario,
                            clave = clave,
                            pelicula = seleccion,
                            onResultado = { detalle ->
                                activity.runOnUiThread {
                                    cacheDetallePeliculas[seleccion.id] = detalle
                                    if (peliculas.getOrNull(indicePelicula)?.id == seleccion.id) {
                                        detallePelicula = detalle
                                        mensaje = ""
                                    }
                                }
                            },
                            onError = { e ->
                                activity.runOnUiThread {
                                    if (peliculas.getOrNull(indicePelicula)?.id == seleccion.id) mensaje = e
                                }
                            }
                        )
                    }
                },
                onCategoria = { categoria ->
                    if (categoria.id == CAT_CONTINUAR_NOVATV) {
                        categoriaParentalAutorizadaTemporal = null
                        categoriaPeliculaActual = "▶ CONTINUAR VIENDO"; categoriaPeliculaActualId = CAT_CONTINUAR_NOVATV
                        val pendientes = progresos.filter { it.tipo == "PELICULA" }.sortedByDescending { it.actualizado }
                        peliculas = pendientes.map { Pelicula(it.id, it.titulo, it.extension, it.portada) }
                        indicePelicula = posicionPeliculas[CAT_CONTINUAR_NOVATV] ?: 0
                        if (indicePelicula !in peliculas.indices) indicePelicula = 0
                        mensaje = if (peliculas.isEmpty()) "No tienes películas pendientes" else ""
                        return@PantallaCategoriasPeliculas
                    }
                    if (categoria.id == CAT_FAVORITOS_NOVATV) {
                        categoriaParentalAutorizadaTemporal = null
                        categoriaPeliculaActual = "⭐ FAVORITOS"; categoriaPeliculaActualId = CAT_FAVORITOS_NOVATV
                        peliculas = peliculasFavoritasSeccionNovaTV(); indicePelicula = posicionPeliculas[CAT_FAVORITOS_NOVATV] ?: 0
                        if (indicePelicula !in peliculas.indices) indicePelicula = 0
                        mensaje = if (peliculas.isEmpty()) "Aún no tienes películas favoritas" else ""
                        return@PantallaCategoriasPeliculas
                    }
                    if (categoria.id == CAT_PRIVADA_NOVATV) {
                        categoriaParentalAutorizadaTemporal = null
                        categoriaPeliculaActual = "✨ NOVATV"
                        categoriaPeliculaActualId = CAT_PRIVADA_NOVATV
                        peliculas = emptyList()
                        indicePelicula = 0
                        mensaje = "Cargando catálogo privado NovaTV..."

                        cargarCatalogoPrivadoNovaTV(
                            email = SUPABASE_EMAIL_NOVATV,
                            password = SUPABASE_PASSWORD_NOVATV,
                            onResultado = { listaNova ->
                                activity.runOnUiThread {
                                    if (categoriaPeliculaActualId == CAT_PRIVADA_NOVATV) {
                                        peliculas = listaNova.map { p ->
                                            Pelicula(
                                                id = "NOVATV_${p.id}",
                                                nombre = p.titulo,
                                                extension = p.url.substringAfterLast('.', "mp4").substringBefore('?').takeIf { it.length in 2..5 } ?: "mp4",
                                                portada = p.portada,
                                                agregado = 0L,
                                                categoriaId = CAT_PRIVADA_NOVATV,
                                                urlDirectaNovaTV = p.url,
                                                tmdbIdNovaTV = p.tmdbId,
                                                fondoNovaTV = p.fondo,
                                                descripcionNovaTV = p.descripcion
                                            )
                                        }
                                        mensaje = if (peliculas.isEmpty()) {
                                            "Catálogo NovaTV conectado, pero no hay películas"
                                        } else {
                                            "✅ Catálogo NovaTV conectado: ${peliculas.size} película${if (peliculas.size == 1) "" else "s"}"
                                        }
                                    }
                                }
                            },
                            onError = { error ->
                                activity.runOnUiThread {
                                    if (categoriaPeliculaActualId == CAT_PRIVADA_NOVATV) mensaje = error
                                }
                            }
                        )
                        return@PantallaCategoriasPeliculas
                    }
                    if (categoria.id == CAT_RECIENTES_NOVATV) {
                        categoriaParentalAutorizadaTemporal = null
                        categoriaPeliculaActual = "🆕 RECIENTEMENTE AÑADIDAS"; categoriaPeliculaActualId = CAT_RECIENTES_NOVATV
                        peliculas = peliculasRecientesSeccionNovaTV(); indicePelicula = posicionPeliculas[CAT_RECIENTES_NOVATV] ?: 0
                        if (indicePelicula !in peliculas.indices) indicePelicula = 0
                        mensaje = if (peliculas.isEmpty()) "Cargando recientes..." else ""
                        if (catalogoPeliculasRecomendacion.isEmpty()) cargarCatalogoPeliculasRecomendacionNovaTV(usuario, clave) { catalogo -> activity.runOnUiThread { catalogoPeliculasRecomendacion = catalogo; if (categoriaPeliculaActualId == CAT_RECIENTES_NOVATV) { peliculas = peliculasRecientesSeccionNovaTV(); mensaje = if (peliculas.isEmpty()) "No hay películas recientes disponibles" else "" } } }
                        return@PantallaCategoriasPeliculas
                    }
                    if (categoriaBloqueadaNovaTV("PEL", categoria.id, categoria.nombre)) {
                        pedirPinNovaTV("ACCESO") {
                            categoriaParentalAutorizadaTemporal = claveBloqueoNovaTV("PEL", categoria.id)
                            categoriaPeliculaActual = categoria.nombre
                            categoriaPeliculaActualId = categoria.id
                            indicePelicula = posicionPeliculas[categoria.id] ?: 0
                            val guardadas = cachePeliculas[categoria.id]
                            if (guardadas != null) { peliculas = guardadas; if (indicePelicula !in guardadas.indices) indicePelicula = 0; mensaje = if (guardadas.isEmpty()) "No se encontraron películas" else "" }
                            else { peliculas = emptyList(); val ck = "PEL:${categoria.id}"; if (cargasEnCurso.add(ck)) { mensaje = "Cargando películas..."; cargarPeliculas(usuario, clave, categoria.id, { lista -> activity.runOnUiThread { cargasEnCurso.remove(ck); cachePeliculas[categoria.id]=lista; if(categoriaPeliculaActualId==categoria.id){peliculas=lista; if(indicePelicula !in lista.indices) indicePelicula=0; mensaje=if(lista.isEmpty()) "No se encontraron películas" else ""} } }, { e -> activity.runOnUiThread { cargasEnCurso.remove(ck); mensaje=e } }) } }
                            pantalla = "CATEGORIAS_PELICULAS"
                        }
                        return@PantallaCategoriasPeliculas
                    }
                    categoriaPeliculaActual = categoria.nombre
                    categoriaPeliculaActualId = categoria.id
                    indicePelicula = posicionPeliculas[categoria.id] ?: 0

                    val guardadas = cachePeliculas[categoria.id]
                    if (guardadas != null) {
                        peliculas = guardadas
                        if (indicePelicula !in guardadas.indices) indicePelicula = 0
                        mensaje = if (guardadas.isEmpty()) "No se encontraron películas" else ""
                    } else {
                        peliculas = emptyList()
                        val claveCarga = "PEL:${categoria.id}"
                        if (cargasEnCurso.add(claveCarga)) {
                            mensaje = "Cargando películas..."
                            cargarPeliculas(
                                usuario = usuario,
                                clave = clave,
                                categoriaId = categoria.id,
                                onResultado = { lista ->
                                    activity.runOnUiThread {
                                        cargasEnCurso.remove(claveCarga)
                                        cachePeliculas[categoria.id] = lista
                                        if (categoriaPeliculaActualId == categoria.id) {
                                            peliculas = lista
                                            if (indicePelicula !in lista.indices) indicePelicula = 0
                                            mensaje = if (lista.isEmpty()) "No se encontraron películas" else ""
                                        }
                                    }
                                },
                                onError = { error ->
                                    activity.runOnUiThread {
                                        cargasEnCurso.remove(claveCarga)
                                        if (categoriaPeliculaActualId == categoria.id) mensaje = error
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }

        "PELICULAS" -> {
            PantallaPeliculas(
                categoria = categoriaPeliculaActual,
                peliculas = peliculas,
                mensaje = mensaje,
                indiceSeleccionado = indicePelicula,
                onIndiceChange = {
                    indicePelicula = it
                    if (categoriaPeliculaActualId.isNotBlank()) posicionPeliculas[categoriaPeliculaActualId] = it
                },
                onVolver = { pantalla = "CATEGORIAS_PELICULAS" },
                favoritos = favoritos,
                onFavorito = { pelicula -> alternarFavorito(Favorito("PELICULA", pelicula.id, pelicula.nombre, pelicula.extension)) },
                onPelicula = { indice ->
                    indicePelicula = indice
                    if (categoriaPeliculaActualId.isNotBlank()) posicionPeliculas[categoriaPeliculaActualId] = indice
                    volverDesdePelicula = "PELICULAS"
                    volverDesdePlayerPelicula = "PELICULAS"
                    pantalla = "PLAYER_PELICULA"
                }
            )
        }

        "DETALLE_PELICULA" -> {
            val seleccion = peliculas.getOrNull(indicePelicula)
            if (seleccion != null) {
                PantallaDetallePeliculaNovaTV(
                    pelicula = seleccion,
                    detalle = detallePelicula,
                    mensaje = mensaje,
                    esFavorita = claveFavorito("PELICULA", seleccion.id) in favoritos,
                    recomendaciones = recomendacionesPeliculas,
                    onAbrirRecomendacion = { i ->
                        val destino = recomendacionesPeliculas.getOrNull(i)
                        if (destino != null) {
                            var idxDestino = peliculas.indexOfFirst { it.id == destino.id }
                            if (idxDestino < 0) { peliculas = peliculas + destino; idxDestino = peliculas.lastIndex }
                            if (idxDestino >= 0) {
                                indicePelicula = idxDestino
                                detallePelicula = cacheDetallePeliculas[destino.id]
                                recomendacionesPeliculas = emptyList()
                                cargarRecomendacionesPeliculasNovaTV(destino, catalogoPeliculasRecomendacion.ifEmpty { peliculas }) { lista -> activity.runOnUiThread { if (peliculas.getOrNull(indicePelicula)?.id == destino.id) recomendacionesPeliculas = lista } }
                                if (detallePelicula == null) {
                                    mensaje = "Cargando información..."
                                    cargarDetallePelicula(usuario, clave, destino, { d -> activity.runOnUiThread { cacheDetallePeliculas[destino.id] = d; if (peliculas.getOrNull(indicePelicula)?.id == destino.id) { detallePelicula = d; mensaje = "" } } }, { e -> activity.runOnUiThread { mensaje = e } })
                                }
                            }
                        }
                    },
                    onVolver = {
                        cachePeliculas[categoriaPeliculaActualId]?.let { peliculas = it }
                        indicePelicula = posicionPeliculas[categoriaPeliculaActualId] ?: 0
                        pantalla = volverDesdePelicula
                    },
                    onReproducir = { volverDesdePlayerPelicula = "DETALLE_PELICULA"; pantalla = "PLAYER_PELICULA" },
                    onFavorito = {
                        alternarFavorito(Favorito("PELICULA", seleccion.id, seleccion.nombre, seleccion.extension))
                    }
                )
            } else {
                pantalla = volverDesdePelicula
            }
        }

        "PLAYER_PELICULA" -> {
            ReproductorPeliculaNovaTV(
                usuario = usuario,
                clave = clave,
                pelicula = peliculas[indicePelicula],
                posicionInicialMs = cargarProgresosNovaTV(activity).firstOrNull { it.tipo == "PELICULA" && it.id == peliculas[indicePelicula].id }?.posicionMs ?: 0L,
                onProgreso = { pos, dur ->
                    actualizarProgresoNovaTV(activity, ProgresoNovaTV("PELICULA", peliculas[indicePelicula].id, peliculas[indicePelicula].nombre, peliculas[indicePelicula].extension, pos, dur, System.currentTimeMillis(), portada = peliculas[indicePelicula].portada.ifBlank { detallePelicula?.portada ?: "" }))
                    progresos = cargarProgresosNovaTV(activity)
                },
                onSalir = { pantalla = volverDesdePlayerPelicula }
            )
        }

        "CATEGORIAS_SERIES" -> {
            val categoriasSeriesVista = listOf(
                CategoriaSerie(CAT_CONTINUAR_NOVATV, "▶ CONTINUAR SERIE"),
                CategoriaSerie(CAT_FAVORITOS_NOVATV, "⭐ FAVORITOS"),
                CategoriaSerie(CAT_RECIENTES_NOVATV, "🆕 RECIENTEMENTE AÑADIDAS"),
                CategoriaSerie(CAT_PRIVADA_SERIES_NOVATV, "✨ NOVATV")
            ) + categoriasSeries
            PantallaSeriesNovaTV(
                categorias = categoriasSeriesVista,
                series = series,
                mensaje = mensaje,
                indiceCategoria = indiceCategoriaSerie,
                indiceSerie = indiceSerie,
                favoritos = favoritos,
                onVolver = { categoriaParentalAutorizadaTemporal = null; mensaje = ""; pantalla = "INICIO" },
                onIndiceCategoriaChange = { nuevoIndice ->
                    indiceCategoriaSerie = nuevoIndice
                    categoriasSeriesVista.getOrNull(nuevoIndice)?.let { cat ->
                        if (categoriaParentalAutorizadaTemporal != claveBloqueoNovaTV("SER", cat.id)) categoriaParentalAutorizadaTemporal = null
                        if (categoriaBloqueadaNovaTV("SER", cat.id, cat.nombre)) {
                            categoriaSerieActualId = ""
                            series = emptyList()
                            indiceSerie = 0
                            mensaje = "🔒 Categoría protegida · Pulsa OK para ingresar"
                        }
                    }
                },
                onIndiceSerieChange = {
                    indiceSerie = it
                    if (categoriaSerieActualId.isNotBlank()) posicionSeries[categoriaSerieActualId] = it
                },
                esCategoriaBloqueada = { cat -> cat.id != CAT_CONTINUAR_NOVATV && cat.id != CAT_FAVORITOS_NOVATV && cat.id != CAT_RECIENTES_NOVATV && cat.id != CAT_PRIVADA_SERIES_NOVATV && categoriaBloqueadaNovaTV("SER", cat.id, cat.nombre) },
                onFavorito = { serie ->
                    alternarFavorito(Favorito("SERIE", serie.id, serie.nombre))
                },
                onCategoria = { cat ->
                    if (cat.id == CAT_PRIVADA_SERIES_NOVATV) {
                        categoriaParentalAutorizadaTemporal = null
                        categoriaSerieActualId = CAT_PRIVADA_SERIES_NOVATV
                        series = emptyList()
                        indiceSerie = 0
                        mensaje = "Cargando catálogo privado de Series..."

                        cargarCatalogoPrivadoSeriesNovaTV(
                            email = SUPABASE_EMAIL_NOVATV,
                            password = SUPABASE_PASSWORD_NOVATV,
                            onResultado = { catalogo ->
                                activity.runOnUiThread {
                                    if (categoriaSerieActualId == CAT_PRIVADA_SERIES_NOVATV) {
                                        catalogoSeriesPrivado = catalogo
                                        series = catalogo.series.map { item ->
                                            Serie(
                                                id = "NOVATV_SERIE_${item.id}",
                                                nombre = item.titulo,
                                                portada = item.portada,
                                                categoriaId = CAT_PRIVADA_SERIES_NOVATV,
                                                tmdbIdNovaTV = item.tmdbId,
                                                fondoNovaTV = item.fondo,
                                                descripcionNovaTV = item.descripcion
                                            )
                                        }
                                        cacheSeries[CAT_PRIVADA_SERIES_NOVATV] = series
                                        mensaje = if (series.isEmpty()) {
                                            "Catálogo NovaTV conectado, pero todavía no hay series"
                                        } else {
                                            "Catálogo NovaTV conectado: ${series.size} serie${if (series.size == 1) "" else "s"}"
                                        }
                                    }
                                }
                            },
                            onError = { error ->
                                activity.runOnUiThread {
                                    if (categoriaSerieActualId == CAT_PRIVADA_SERIES_NOVATV) mensaje = error
                                }
                            }
                        )
                        return@PantallaSeriesNovaTV
                    }
                    if (cat.id == CAT_CONTINUAR_NOVATV) {
                        categoriaParentalAutorizadaTemporal = null
                        categoriaSerieActualId = CAT_CONTINUAR_NOVATV
                        val pendientes = progresos.filter { it.tipo == "EPISODIO" }.sortedByDescending { it.actualizado }
                        val unicos = pendientes.distinctBy { it.serieId.ifBlank { it.serieNombre.ifBlank { it.titulo.substringBefore(" · ") } } }
                        series = unicos.map { p -> Serie(p.serieId.ifBlank { "progreso:${p.id}" }, p.serieNombre.ifBlank { p.titulo.substringBefore(" · ") }, p.portada) }
                        indiceSerie = posicionSeries[CAT_CONTINUAR_NOVATV] ?: 0
                        if (indiceSerie !in series.indices) indiceSerie = 0
                        mensaje = if (series.isEmpty()) "No tienes series pendientes" else ""
                        return@PantallaSeriesNovaTV
                    }
                    if (cat.id == CAT_FAVORITOS_NOVATV) {
                        categoriaParentalAutorizadaTemporal = null
                        categoriaSerieActualId = CAT_FAVORITOS_NOVATV
                        series = seriesFavoritasSeccionNovaTV(); indiceSerie = posicionSeries[CAT_FAVORITOS_NOVATV] ?: 0
                        if (indiceSerie !in series.indices) indiceSerie = 0
                        mensaje = if (series.isEmpty()) "Aún no tienes series favoritas" else ""
                        return@PantallaSeriesNovaTV
                    }
                    if (cat.id == CAT_RECIENTES_NOVATV) {
                        categoriaParentalAutorizadaTemporal = null
                        categoriaSerieActualId = CAT_RECIENTES_NOVATV
                        series = seriesRecientesSeccionNovaTV(); indiceSerie = posicionSeries[CAT_RECIENTES_NOVATV] ?: 0
                        if (indiceSerie !in series.indices) indiceSerie = 0
                        mensaje = if (series.isEmpty()) "Cargando recientes..." else ""
                        if (catalogoSeriesRecomendacion.isEmpty()) cargarCatalogoSeriesRecomendacionNovaTV(usuario, clave) { catalogo -> activity.runOnUiThread { catalogoSeriesRecomendacion = catalogo; if (categoriaSerieActualId == CAT_RECIENTES_NOVATV) { series = seriesRecientesSeccionNovaTV(); mensaje = if (series.isEmpty()) "No hay series recientes disponibles" else "" } } }
                        return@PantallaSeriesNovaTV
                    }
                    if (categoriaBloqueadaNovaTV("SER", cat.id, cat.nombre)) {
                        pedirPinNovaTV("ACCESO") {
                            categoriaParentalAutorizadaTemporal = claveBloqueoNovaTV("SER", cat.id)
                            categoriaSerieActualId = cat.id; indiceSerie = posicionSeries[cat.id] ?: 0
                            val guardadas = cacheSeries[cat.id]
                            if (guardadas != null) { series=guardadas; if(indiceSerie !in guardadas.indices) indiceSerie=0; mensaje=if(guardadas.isEmpty()) "No se encontraron series" else "" }
                            else { series=emptyList(); val ck="SER:${cat.id}"; if(cargasEnCurso.add(ck)){ mensaje="Cargando series..."; cargarSeries(usuario, clave, cat.id, { lista -> activity.runOnUiThread { cargasEnCurso.remove(ck); cacheSeries[cat.id]=lista; if(categoriaSerieActualId==cat.id){series=lista; if(indiceSerie !in lista.indices) indiceSerie=0; mensaje=if(lista.isEmpty()) "No se encontraron series" else ""} } }, { e -> activity.runOnUiThread { cargasEnCurso.remove(ck); mensaje=e } }) } }
                            pantalla = "CATEGORIAS_SERIES"
                        }
                        return@PantallaSeriesNovaTV
                    }
                    categoriaSerieActualId = cat.id
                    indiceSerie = posicionSeries[cat.id] ?: 0

                    val guardadas = cacheSeries[cat.id]
                    if (guardadas != null) {
                        series = guardadas
                        if (indiceSerie !in guardadas.indices) indiceSerie = 0
                        mensaje = if (guardadas.isEmpty()) "No se encontraron series" else ""
                    } else {
                        series = emptyList()
                        val claveCarga = "SER:${cat.id}"
                        if (cargasEnCurso.add(claveCarga)) {
                            mensaje = "Cargando series..."
                            cargarSeries(
                                usuario,
                                clave,
                                cat.id,
                                onResultado = { lista ->
                                    activity.runOnUiThread {
                                        cargasEnCurso.remove(claveCarga)
                                        cacheSeries[cat.id] = lista
                                        if (categoriaSerieActualId == cat.id) {
                                            series = lista
                                            if (indiceSerie !in lista.indices) indiceSerie = 0
                                            mensaje = if (lista.isEmpty()) "No se encontraron series" else ""
                                        }
                                    }
                                },
                                onError = { e ->
                                    activity.runOnUiThread {
                                        cargasEnCurso.remove(claveCarga)
                                        if (categoriaSerieActualId == cat.id) mensaje = e
                                    }
                                }
                            )
                        }
                    }
                },
                onAbrirSerie = { indice ->
                    if (categoriaSerieActualId == CAT_PRIVADA_SERIES_NOVATV) {
                        val seleccion = series.getOrNull(indice)
                        val catalogo = catalogoSeriesPrivado
                        if (seleccion == null || catalogo == null) {
                            mensaje = "No se pudo abrir la serie NovaTV"
                            return@PantallaSeriesNovaTV
                        }

                        val idPrivado = seleccion.id
                            .removePrefix("NOVATV_SERIE_")
                            .toLongOrNull()
                        val seriePrivada = idPrivado?.let { id ->
                            catalogo.series.firstOrNull { it.id == id }
                        }

                        if (seriePrivada == null) {
                            mensaje = "No se encontraron los datos de la serie NovaTV"
                            return@PantallaSeriesNovaTV
                        }

                        indiceSerie = indice
                        posicionSeries[CAT_PRIVADA_SERIES_NOVATV] = indice
                        serieActual = seleccion.nombre
                        serieActualId = seleccion.id
                        abrirEpisodiosDetalle = false
                        recomendacionesSeries = emptyList()

                        val temporadasSerie = catalogo.temporadas
                            .filter { it.serieId == seriePrivada.id }
                            .sortedBy { it.numero }
                        val temporadaPorId = temporadasSerie.associateBy { it.id }

                        val listaEpisodios = catalogo.episodios
                            .mapNotNull { item ->
                                val temporada = temporadaPorId[item.temporadaId]
                                    ?: return@mapNotNull null
                                val extension = item.url
                                    .substringBefore('?')
                                    .substringBefore('#')
                                    .substringAfterLast('.', "mp4")
                                    .lowercase(Locale.ROOT)
                                    .takeIf { it.length in 2..5 }
                                    ?: "mp4"

                                Episodio(
                                    id = "NOVATV_EP_${item.id}",
                                    titulo = item.titulo.ifBlank { "Episodio ${item.numero}" },
                                    extension = extension,
                                    temporada = temporada.numero,
                                    numero = item.numero,
                                    urlDirectaNovaTV = item.url,
                                    portadaNovaTV = item.portada,
                                    descripcionNovaTV = item.descripcion
                                )
                            }
                            .sortedWith(
                                compareBy<Episodio> { it.temporada }
                                    .thenBy { it.numero }
                                    .thenBy { it.titulo }
                            )

                        episodios = listaEpisodios
                        temporadas = temporadasSerie
                            .map { it.numero }
                            .distinct()
                            .sorted()
                        cacheEpisodios[seleccion.id] = listaEpisodios

                        indiceTemporada = posicionTemporadas[seleccion.id] ?: 0
                        if (indiceTemporada !in temporadas.indices) indiceTemporada = 0

                        val temporadaInicial = temporadas.getOrNull(indiceTemporada)
                        val epsIniciales = if (temporadaInicial != null) {
                            listaEpisodios.filter { it.temporada == temporadaInicial }
                        } else emptyList()

                        indiceEpisodio = if (temporadaInicial != null) {
                            (posicionEpisodios["${seleccion.id}:$temporadaInicial"] ?: 0)
                                .coerceIn(0, (epsIniciales.size - 1).coerceAtLeast(0))
                        } else 0

                        detalleSerie = DetalleSerie(
                            descripcion = seleccion.descripcionNovaTV,
                            portada = seleccion.portada,
                            fondo = seleccion.fondoNovaTV
                        )

                        mensaje = when {
                            temporadas.isEmpty() ->
                                "Esta serie todavía no tiene temporadas"
                            listaEpisodios.isEmpty() ->
                                "La serie tiene temporadas, pero todavía no tiene episodios"
                            else -> ""
                        }

                        pantalla = "DETALLE_SERIE"

                        // Completar la ficha en segundo plano con el tmdb_id exacto,
                        // sin consultar Xtream con un ID interno de Supabase.
                        Thread {
                            val detalleCompleto = detalleSeriePrivadaNovaTV(seleccion)
                            activity.runOnUiThread {
                                if (serieActualId == seleccion.id) {
                                    detalleSerie = detalleCompleto
                                    cacheDetalleSeries[seleccion.id] = detalleCompleto
                                }
                            }
                        }.start()

                        return@PantallaSeriesNovaTV
                    }
                    if (categoriaSerieActualId == CAT_CONTINUAR_NOVATV) {
                        series.getOrNull(indice)?.let { ser ->
                            val p = progresos.filter { it.tipo == "EPISODIO" }.sortedByDescending { it.actualizado }.firstOrNull {
                                (it.serieId.isNotBlank() && it.serieId == ser.id) ||
                                it.serieNombre.equals(ser.nombre, ignoreCase = true) ||
                                it.titulo.substringBefore(" · ").equals(ser.nombre, ignoreCase = true)
                            }
                            if (p != null) {
                                progresoSeleccionado = p
                                volverDesdeOpcionesContinuar = "CATEGORIAS_SERIES"
                                pantalla = "OPCIONES_CONTINUAR"
                            }
                        }
                        return@PantallaSeriesNovaTV
                    }
                    if (indice in series.indices) {
                        indiceSerie = indice
                        if (categoriaSerieActualId.isNotBlank()) posicionSeries[categoriaSerieActualId] = indice
                        val seleccion = series[indice]
                        abrirEpisodiosDetalle = false
                        serieActual = seleccion.nombre
                        serieActualId = seleccion.id
                        detalleSerie = cacheDetalleSeries[seleccion.id]
                        episodios = cacheEpisodios[seleccion.id] ?: emptyList()
                        temporadas = episodios.map { it.temporada }.distinct().sorted()
                        indiceTemporada = posicionTemporadas[seleccion.id] ?: 0
                        if (indiceTemporada !in temporadas.indices) indiceTemporada = 0
                        indiceEpisodio = 0
                        mensaje = if (detalleSerie == null || episodios.isEmpty()) "Cargando información..." else ""
                        pantalla = "DETALLE_SERIE"
                        recomendacionesSeries = emptyList()
                        val lanzarRecsSerie: (List<Serie>) -> Unit = { catalogo ->
                            cargarRecomendacionesSeriesNovaTV(seleccion, catalogo) { lista -> activity.runOnUiThread { if (serieActualId == seleccion.id) recomendacionesSeries = lista } }
                        }
                        if (catalogoSeriesRecomendacion.isNotEmpty()) lanzarRecsSerie(catalogoSeriesRecomendacion)
                        else cargarCatalogoSeriesRecomendacionNovaTV(usuario, clave) { catalogo -> activity.runOnUiThread { catalogoSeriesRecomendacion = catalogo; lanzarRecsSerie(catalogo) } }

                        if (detalleSerie == null || cacheEpisodios[seleccion.id] == null) {
                            cargarDetalleSerieCompleto(
                                usuario = usuario,
                                clave = clave,
                                serie = seleccion,
                                onResultado = { detalle, lista ->
                                    activity.runOnUiThread {
                                        cacheDetalleSeries[seleccion.id] = detalle
                                        cacheEpisodios[seleccion.id] = lista
                                        if (serieActualId == seleccion.id) {
                                            detalleSerie = detalle
                                            episodios = lista
                                            temporadas = lista.map { it.temporada }.distinct().sorted()
                                            indiceTemporada = posicionTemporadas[seleccion.id] ?: 0
                                            if (indiceTemporada !in temporadas.indices) indiceTemporada = 0
                                            indiceEpisodio = 0
                                            mensaje = if (lista.isEmpty()) "No se encontraron episodios" else ""
                                        }
                                    }
                                },
                                onError = { e ->
                                    activity.runOnUiThread {
                                        if (serieActualId == seleccion.id) mensaje = e
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }

        "SERIES" -> {
            // Compatibilidad con rutas antiguas: vuelve a la nueva pantalla visual.
            pantalla = "CATEGORIAS_SERIES"
        }

        "DETALLE_SERIE" -> {
            val seleccion = series.getOrNull(indiceSerie)
            if (seleccion != null) {
                PantallaDetalleSerieNovaTV(
                    serie = seleccion,
                    detalle = detalleSerie,
                    episodios = episodios,
                    temporadas = temporadas,
                    mensaje = mensaje,
                    indiceTemporada = indiceTemporada,
                    indiceEpisodio = indiceEpisodio,
                    esFavorita = claveFavorito("SERIE", seleccion.id) in favoritos,
                    progresos = progresos,
                    recomendaciones = recomendacionesSeries,
                    onAbrirRecomendacion = { i ->
                        val destino = recomendacionesSeries.getOrNull(i)
                        var idxDestino = series.indexOfFirst { it.id == destino?.id }
                        if (destino != null && idxDestino < 0) { series = series + destino; idxDestino = series.lastIndex }
                        if (destino != null && idxDestino >= 0) {
                            indiceSerie = idxDestino
                            serieActualId = destino.id; serieActual = destino.nombre
                            detalleSerie = cacheDetalleSeries[destino.id]
                            episodios = cacheEpisodios[destino.id] ?: emptyList()
                            temporadas = episodios.map { it.temporada }.distinct().sorted()
                            indiceTemporada = 0; indiceEpisodio = 0; abrirEpisodiosDetalle = false
                            recomendacionesSeries = emptyList()
                            cargarRecomendacionesSeriesNovaTV(destino, catalogoSeriesRecomendacion.ifEmpty { series }) { lista -> activity.runOnUiThread { if (serieActualId == destino.id) recomendacionesSeries = lista } }
                            if (detalleSerie == null || cacheEpisodios[destino.id] == null) {
                                mensaje = "Cargando información..."
                                cargarDetalleSerieCompleto(usuario, clave, destino, { d, eps -> activity.runOnUiThread { cacheDetalleSeries[destino.id]=d; cacheEpisodios[destino.id]=eps; if (serieActualId==destino.id) { detalleSerie=d; episodios=eps; temporadas=eps.map { it.temporada }.distinct().sorted(); mensaje="" } } }, { e -> activity.runOnUiThread { mensaje=e } })
                            }
                        }
                    },
                    modoEpisodiosInicial = abrirEpisodiosDetalle,
                    onVolver = {
                        abrirEpisodiosDetalle = false
                        cacheSeries[categoriaSerieActualId]?.let { series = it }
                        indiceSerie = posicionSeries[categoriaSerieActualId] ?: 0
                        pantalla = "CATEGORIAS_SERIES"
                    },
                    onFavorito = {
                        alternarFavorito(Favorito("SERIE", seleccion.id, seleccion.nombre))
                    },
                    onTemporadaChange = { nuevo ->
                        indiceTemporada = nuevo
                        if (serieActualId.isNotBlank()) posicionTemporadas[serieActualId] = nuevo
                        indiceEpisodio = 0
                    },
                    onEpisodioChange = { nuevo ->
                        indiceEpisodio = nuevo
                        val temporadaNumero = temporadas.getOrNull(indiceTemporada) ?: 0
                        if (serieActualId.isNotBlank()) posicionEpisodios["$serieActualId:$temporadaNumero"] = nuevo
                    },
                    onReproducirEpisodio = { indice ->
                        indiceEpisodio = indice
                        val temporadaNumero = temporadas.getOrNull(indiceTemporada)
                        val epsTemporada = if (temporadaNumero != null) {
                            episodios.filter { it.temporada == temporadaNumero }
                        } else emptyList()
                        val episodioSeleccionado = epsTemporada.getOrNull(indice)

                        if (
                            categoriaSerieActualId == CAT_PRIVADA_SERIES_NOVATV &&
                            episodioSeleccionado?.urlDirectaNovaTV.isNullOrBlank()
                        ) {
                            android.widget.Toast.makeText(
                                activity,
                                "Este episodio NovaTV no tiene una URL de reproducción",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            abrirEpisodiosDetalle = true
                            pantalla = "PLAYER_EPISODIO_DETALLE"
                        }
                    }
                )
            } else {
                pantalla = "CATEGORIAS_SERIES"
            }
        }

        "PLAYER_EPISODIO_DETALLE" -> {
            val temporadaNumero = temporadas.getOrNull(indiceTemporada)
            val epsTemporada = if (temporadaNumero != null) episodios.filter { it.temporada == temporadaNumero } else emptyList()
            val ep = epsTemporada.getOrNull(indiceEpisodio)
            if (ep != null) {
                ReproductorEpisodioNovaTV(
                    usuario = usuario,
                    clave = clave,
                    episodio = ep,
                    posicionInicialMs = cargarProgresosNovaTV(activity).firstOrNull { it.tipo == "EPISODIO" && it.id == ep.id }?.posicionMs ?: 0L,
                    onProgreso = { pos, dur ->
                        actualizarProgresoNovaTV(
                            activity,
                            ProgresoNovaTV(
                                tipo = "EPISODIO", id = ep.id, titulo = "${serieActual} · ${ep.titulo}",
                                extension = ep.extension, posicionMs = pos, duracionMs = dur,
                                actualizado = System.currentTimeMillis(), serieId = serieActualId,
                                serieNombre = serieActual, temporada = ep.temporada, indiceEpisodio = indiceEpisodio,
                                portada = detalleSerie?.portada?.ifBlank { series.getOrNull(indiceSerie)?.portada ?: "" }
                                    ?: (series.getOrNull(indiceSerie)?.portada ?: ""),
                                urlDirectaNovaTV = ep.urlDirectaNovaTV
                            )
                        )
                        progresos = cargarProgresosNovaTV(activity)
                    },
                    puedeAnterior = indiceEpisodio > 0,
                    puedeSiguiente = indiceEpisodio < epsTemporada.lastIndex,
                    onAnterior = {
                        if (indiceEpisodio > 0) indiceEpisodio--
                    },
                    onSiguiente = {
                        if (indiceEpisodio < epsTemporada.lastIndex) indiceEpisodio++
                    },
                    onSalir = { abrirEpisodiosDetalle = true; pantalla = "DETALLE_SERIE" }
                )
            } else {
                pantalla = "DETALLE_SERIE"
            }
        }

        "TEMPORADAS" -> {
            PantallaListaSimple(
                titulo = serieActual,
                subtitulo = "Temporadas",
                nombres = temporadas.map { "Temporada $it" },
                mensaje = mensaje,
                indiceSeleccionado = indiceTemporada,
                onIndiceChange = {
                    indiceTemporada = it
                    if (serieActualId.isNotBlank()) posicionTemporadas[serieActualId] = it
                },
                onVolver = { pantalla = volverDesdeTemporadas },
                onSeleccionar = { i ->
                    indiceTemporada = i
                    if (serieActualId.isNotBlank()) posicionTemporadas[serieActualId] = i

                    val temporadaNumero = temporadas.getOrNull(i) ?: 0
                    val clavePosicion = "$serieActualId:$temporadaNumero"
                    indiceEpisodio = posicionEpisodios[clavePosicion] ?: 0
                    pantalla = "EPISODIOS"
                }
            )
        }

        "EPISODIOS" -> {
            val epsTemporada = if (temporadas.isNotEmpty()) episodios.filter { it.temporada == temporadas[indiceTemporada] } else emptyList()
            PantallaListaSimple(
                titulo = serieActual,
                subtitulo = if(temporadas.isNotEmpty()) "Temporada ${temporadas[indiceTemporada]}" else "Episodios",
                nombres = epsTemporada.map { tituloEpisodioNovaTV(it) },
                mensaje = mensaje,
                indiceSeleccionado = indiceEpisodio,
                onIndiceChange = {
                    indiceEpisodio = it
                    val temporadaNumero = temporadas.getOrNull(indiceTemporada) ?: 0
                    if (serieActualId.isNotBlank()) posicionEpisodios["$serieActualId:$temporadaNumero"] = it
                },
                onVolver = { pantalla = "TEMPORADAS" },
                onSeleccionar = { i ->
                    indiceEpisodio = i
                    val temporadaNumero = temporadas.getOrNull(indiceTemporada) ?: 0
                    if (serieActualId.isNotBlank()) posicionEpisodios["$serieActualId:$temporadaNumero"] = i
                    pantalla = "PLAYER_EPISODIO"
                }
            )
        }

        "PLAYER_EPISODIO" -> {
            val epsTemporada = episodios.filter { it.temporada == temporadas[indiceTemporada] }
            ReproductorEpisodioNovaTV(
                usuario = usuario,
                clave = clave,
                episodio = epsTemporada[indiceEpisodio],
                posicionInicialMs = cargarProgresosNovaTV(activity).firstOrNull { it.tipo == "EPISODIO" && it.id == epsTemporada[indiceEpisodio].id }?.posicionMs ?: 0L,
                onProgreso = { pos, dur ->
                    val ep = epsTemporada[indiceEpisodio]
                    actualizarProgresoNovaTV(
                        activity,
                        ProgresoNovaTV(
                            tipo = "EPISODIO", id = ep.id, titulo = "${serieActual} · ${ep.titulo}",
                            extension = ep.extension, posicionMs = pos, duracionMs = dur,
                            actualizado = System.currentTimeMillis(), serieId = serieActualId,
                            serieNombre = serieActual, temporada = ep.temporada, indiceEpisodio = indiceEpisodio,
                            portada = detalleSerie?.portada?.ifBlank { series.getOrNull(indiceSerie)?.portada ?: "" }
                                ?: (series.getOrNull(indiceSerie)?.portada ?: ""),
                            urlDirectaNovaTV = ep.urlDirectaNovaTV
                        )
                    )
                    progresos = cargarProgresosNovaTV(activity)
                },
                puedeAnterior = indiceEpisodio > 0,
                puedeSiguiente = indiceEpisodio < epsTemporada.lastIndex,
                onAnterior = { if (indiceEpisodio > 0) indiceEpisodio-- },
                onSiguiente = { if (indiceEpisodio < epsTemporada.lastIndex) indiceEpisodio++ },
                onSalir = { pantalla = "EPISODIOS" }
            )
        }

        "CATEGORIAS" -> {
            val categoriasTVVista = listOf(
                CategoriaTV(CAT_FAVORITOS_NOVATV, "⭐ FAVORITOS"),
                CategoriaTV(CAT_PRIVADA_TV_NOVATV, "✨ NOVATV")
            ) + categorias
            PantallaTVEnVivo(
                usuario = usuario,
                clave = clave,
                categorias = categoriasTVVista,
                canales = canales,
                mensaje = mensaje,
                indiceCategoria = indiceCategoriaSeleccionada,
                indiceCanal = indiceCanalSeleccionado,
                favoritos = favoritos,
                onVolverInicio = {
                    categoriaParentalAutorizadaTemporal = null
                    mensaje = ""
                    pantalla = "INICIO"
                },
                esCategoriaBloqueada = { cat ->
                    cat.id != CAT_FAVORITOS_NOVATV &&
                    cat.id != CAT_PRIVADA_TV_NOVATV &&
                    categoriaBloqueadaNovaTV("TV", cat.id, cat.nombre)
                },
                onCategoriaSoloSeleccionada = { indice, categoria ->
                    if (categoriaParentalAutorizadaTemporal != claveBloqueoNovaTV("TV", categoria.id)) categoriaParentalAutorizadaTemporal = null
                    if (categoriaActualId.isNotBlank()) posicionCanales[categoriaActualId] = indiceCanalSeleccionado
                    indiceCategoriaSeleccionada = indice
                    categoriaActual = categoria.nombre
                    categoriaActualId = ""
                    canales = emptyList()
                    indiceCanalSeleccionado = 0
                    mensaje = "🔒 Categoría protegida · Pulsa OK para ingresar"
                },
                onCategoriaEntrar = { indice, categoria ->
                    if (categoriaBloqueadaNovaTV("TV", categoria.id, categoria.nombre)) {
                        pedirPinNovaTV("ACCESO") {
                            categoriaParentalAutorizadaTemporal = claveBloqueoNovaTV("TV", categoria.id)
                            indiceCategoriaSeleccionada = indice
                            categoriaActual = categoria.nombre
                            categoriaActualId = categoria.id
                            indiceCanalSeleccionado = posicionCanales[categoria.id] ?: 0
                            val guardados = cacheCanales[categoria.id]
                            if (guardados != null) {
                                canales = guardados
                                if (indiceCanalSeleccionado !in guardados.indices) indiceCanalSeleccionado = 0
                                mensaje = if (guardados.isEmpty()) "No se encontraron canales" else ""
                            } else {
                                canales = emptyList()
                                val ck = "TV:${categoria.id}"
                                if (cargasEnCurso.add(ck)) {
                                    mensaje = "Cargando canales..."
                                    cargarCanales(usuario, clave, categoria.id, { lista -> activity.runOnUiThread {
                                        cargasEnCurso.remove(ck); cacheCanales[categoria.id] = lista
                                        if (categoriaActualId == categoria.id) {
                                            canales = lista
                                            if (indiceCanalSeleccionado !in lista.indices) indiceCanalSeleccionado = 0
                                            mensaje = if (lista.isEmpty()) "No se encontraron canales" else ""
                                        }
                                    } }, { e -> activity.runOnUiThread { cargasEnCurso.remove(ck); mensaje = e } })
                                }
                            }
                            pantalla = "CATEGORIAS"
                        }
                    }
                },
                onCategoriaChange = { indice, categoria ->
                    if (categoria.id == CAT_FAVORITOS_NOVATV) {
                        categoriaParentalAutorizadaTemporal = null
                        if (categoriaActualId.isNotBlank()) posicionCanales[categoriaActualId] = indiceCanalSeleccionado
                        indiceCategoriaSeleccionada = indice; categoriaActual = "⭐ FAVORITOS"; categoriaActualId = CAT_FAVORITOS_NOVATV
                        canales = canalesFavoritosSeccionNovaTV(); indiceCanalSeleccionado = posicionCanales[CAT_FAVORITOS_NOVATV] ?: 0
                        if (indiceCanalSeleccionado !in canales.indices) indiceCanalSeleccionado = 0
                        mensaje = if (canales.isEmpty()) "Aún no tienes canales favoritos" else ""
                        return@PantallaTVEnVivo
                    }
                    if (categoria.id == CAT_PRIVADA_TV_NOVATV) {
                        categoriaParentalAutorizadaTemporal = null
                        if (categoriaActualId.isNotBlank()) {
                            posicionCanales[categoriaActualId] = indiceCanalSeleccionado
                        }

                        indiceCategoriaSeleccionada = indice
                        categoriaActual = "✨ NOVATV"
                        categoriaActualId = CAT_PRIVADA_TV_NOVATV
                        indiceCanalSeleccionado = posicionCanales[CAT_PRIVADA_TV_NOVATV] ?: 0

                        // Mostrar cache si existe, pero refrescar siempre desde Supabase
                        // para que cambios del panel aparezcan sin un APK nuevo.
                        canales = cacheCanales[CAT_PRIVADA_TV_NOVATV] ?: emptyList()
                        if (indiceCanalSeleccionado !in canales.indices) indiceCanalSeleccionado = 0
                        mensaje = "Cargando TV privada NovaTV..."

                        cargarCanalesPrivadosNovaTV(
                            email = SUPABASE_EMAIL_NOVATV,
                            password = SUPABASE_PASSWORD_NOVATV,
                            onResultado = { listaPrivada ->
                                activity.runOnUiThread {
                                    val lista = listaPrivada.map { item ->
                                        val ruta = item.url
                                            .substringBefore('?')
                                            .substringBefore('#')
                                            .lowercase(Locale.ROOT)
                                        val extension = when {
                                            ruta.endsWith(".m3u8") -> "m3u8"
                                            ruta.endsWith(".ts") -> "ts"
                                            ruta.endsWith(".mpd") -> "mpd"
                                            else -> "stream"
                                        }

                                        CanalTV(
                                            id = "NOVATV_TV_${item.id}",
                                            nombre = item.nombre,
                                            extension = extension,
                                            logo = item.logo,
                                            urlDirectaNovaTV = item.url,
                                            epgIdNovaTV = item.epgId
                                        )
                                    }

                                    cacheCanales[CAT_PRIVADA_TV_NOVATV] = lista

                                    if (categoriaActualId == CAT_PRIVADA_TV_NOVATV) {
                                        canales = lista
                                        if (indiceCanalSeleccionado !in lista.indices) {
                                            indiceCanalSeleccionado = 0
                                        }
                                        mensaje = if (lista.isEmpty()) {
                                            "Catálogo NovaTV conectado, pero todavía no hay canales"
                                        } else {
                                            "✅ TV NovaTV conectada: ${lista.size} canal${if (lista.size == 1) "" else "es"}"
                                        }
                                    }
                                }
                            },
                            onError = { error ->
                                activity.runOnUiThread {
                                    if (categoriaActualId == CAT_PRIVADA_TV_NOVATV) {
                                        mensaje = error
                                    }
                                }
                            }
                        )
                        return@PantallaTVEnVivo
                    }
                    if (categoriaParentalAutorizadaTemporal != claveBloqueoNovaTV("TV", categoria.id)) categoriaParentalAutorizadaTemporal = null
                    if (categoriaBloqueadaNovaTV("TV", categoria.id, categoria.nombre)) {
                        indiceCategoriaSeleccionada = indice
                        categoriaActual = categoria.nombre
                        categoriaActualId = ""
                        canales = emptyList()
                        indiceCanalSeleccionado = 0
                        mensaje = "🔒 Categoría protegida · Pulsa OK para ingresar"
                        return@PantallaTVEnVivo
                    }
                    // Guardamos la posición del canal de la categoría que dejamos.
                    if (categoriaActualId.isNotBlank()) {
                        posicionCanales[categoriaActualId] = indiceCanalSeleccionado
                    }

                    indiceCategoriaSeleccionada = indice
                    categoriaActual = categoria.nombre
                    categoriaActualId = categoria.id
                    indiceCanalSeleccionado = posicionCanales[categoria.id] ?: 0

                    val guardados = cacheCanales[categoria.id]
                    if (guardados != null) {
                        canales = guardados
                        if (indiceCanalSeleccionado !in guardados.indices) indiceCanalSeleccionado = 0
                        mensaje = if (guardados.isEmpty()) "No se encontraron canales" else ""
                    } else {
                        canales = emptyList()
                        val claveCarga = "TV:${categoria.id}"
                        if (cargasEnCurso.add(claveCarga)) {
                            mensaje = "Cargando canales..."
                            cargarCanales(
                                usuario = usuario,
                                clave = clave,
                                categoriaId = categoria.id,
                                onResultado = { lista ->
                                    activity.runOnUiThread {
                                        cargasEnCurso.remove(claveCarga)
                                        cacheCanales[categoria.id] = lista
                                        if (categoriaActualId == categoria.id) {
                                            canales = lista
                                            if (indiceCanalSeleccionado !in lista.indices) indiceCanalSeleccionado = 0
                                            mensaje = if (lista.isEmpty()) "No se encontraron canales" else ""
                                        }
                                    }
                                },
                                onError = { error ->
                                    activity.runOnUiThread {
                                        cargasEnCurso.remove(claveCarga)
                                        if (categoriaActualId == categoria.id) mensaje = error
                                    }
                                }
                            )
                        }
                    }
                },
                onCanalIndiceChange = {
                    indiceCanalSeleccionado = it
                    if (categoriaActualId.isNotBlank()) posicionCanales[categoriaActualId] = it
                },
                onFavorito = { canal ->
                    alternarFavorito(
                        Favorito(
                            tipo = "TV",
                            id = canal.id,
                            nombre = canal.nombre,
                            extension = canal.extension,
                            urlDirectaNovaTV = canal.urlDirectaNovaTV,
                            logoNovaTV = canal.logo
                        )
                    )
                },
                onCanal = { indice ->
                    indiceCanalSeleccionado = indice
                    indiceCanalInicial = indice
                    if (categoriaActualId.isNotBlank()) posicionCanales[categoriaActualId] = indice
                    volverDesdePlayer = "CATEGORIAS"
                    pantalla = "PLAYER"
                }
            )
        }

        "PLAYER" -> {
            ReproductorNovaTV(
                usuario = usuario,
                clave = clave,
                canales = canales,
                indiceInicial = indiceCanalInicial,
                onSalir = {
                    indiceCanalSeleccionado = indiceCanalInicial
                    pantalla = volverDesdePlayer
                },
                onCanalCambiado = { nuevoIndice ->
                    indiceCanalInicial = nuevoIndice
                    indiceCanalSeleccionado = nuevoIndice
                    if (categoriaActualId.isNotBlank()) posicionCanales[categoriaActualId] = nuevoIndice
                }
            )
        }
    }
}

@Composable
fun PantallaContinuarViendoNovaTV(
    progresos: List<ProgresoNovaTV>,
    indiceSeleccionado: Int,
    onIndiceChange: (Int) -> Unit,
    onVolver: () -> Unit,
    onSeleccionar: (ProgresoNovaTV) -> Unit
) {
    val nombres = progresos.map { p ->
        val porcentaje = if (p.duracionMs > 0) ((p.posicionMs * 100) / p.duracionMs).coerceIn(0,100) else 0
        "${if (p.tipo == "PELICULA") "🎬" else "▣"} ${p.titulo}   ·   ${tiempoNovaTV(p.posicionMs)}   ·   $porcentaje%"
    }
    PantallaListaSimple(
        titulo = "CONTINUAR VIENDO",
        subtitulo = "Retoma donde lo dejaste",
        nombres = nombres, mensaje = if (progresos.isEmpty()) "No hay contenido pendiente" else "",
        indiceSeleccionado = indiceSeleccionado.coerceIn(0, (progresos.size - 1).coerceAtLeast(0)),
        onIndiceChange = onIndiceChange, onVolver = onVolver,
        onSeleccionar = { i -> progresos.getOrNull(i)?.let(onSeleccionar) }
    )
}

@Composable
fun PantallaOpcionesContinuarNovaTV(
    progreso: ProgresoNovaTV,
    onVolver: () -> Unit,
    onContinuar: () -> Unit,
    onDesdeInicio: () -> Unit
) {
    val fondo = Color(0xFF08070B)
    val tarjeta = Color(0xFF17131D)
    val violeta = Color(0xFF9B5CFF)
    val violetaSuave = Color(0xFFC7A4FF)
    val textoSecundario = Color(0xFFAAA3B5)
    var seleccionado by remember { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { focus.requestFocus() }

    Box(
        Modifier
            .fillMaxSize()
            .background(fondo)
    ) {
        if (progreso.portada.isNotBlank()) {
            ImagenRemotaNovaTV(
                progreso.portada,
                progreso.titulo,
                Modifier.fillMaxSize(),
                ContentScale.Crop
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xCC08070B),
                                Color(0xEE08070B),
                                Color(0xFF08070B)
                            )
                        )
                    )
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .focusRequester(focus)
                .focusable()
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (e.key) {
                        Key.DirectionUp -> {
                            seleccionado = (seleccionado - 1).coerceAtLeast(0)
                            true
                        }
                        Key.DirectionDown -> {
                            seleccionado = (seleccionado + 1).coerceAtMost(1)
                            true
                        }
                        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                            if (seleccionado == 0) onContinuar() else onDesdeInicio()
                            true
                        }
                        Key.Back, Key.Escape -> {
                            onVolver()
                            true
                        }
                        else -> false
                    }
                }
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Box(
                    Modifier
                        .width(150.dp)
                        .height(220.dp)
                        .background(Color(0xFF17131D), RoundedCornerShape(16.dp))
                        .border(2.dp, violeta, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (progreso.portada.isNotBlank()) {
                        ImagenRemotaNovaTV(
                            progreso.portada,
                            progreso.titulo,
                            Modifier.fillMaxSize(),
                            ContentScale.Crop
                        )
                    } else {
                        Text(
                            if (progreso.tipo == "PELICULA") "🎬" else "▣",
                            color = violetaSuave,
                            style = MaterialTheme.typography.displaySmall
                        )
                    }
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        if (progreso.tipo == "PELICULA") "CONTINUAR PELÍCULA" else "CONTINUAR SERIE",
                        color = violetaSuave,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        progreso.titulo,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 3
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Guardado en ${tiempoNovaTV(progreso.posicionMs)}",
                        color = textoSecundario,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (progreso.duracionMs > 0L) {
                        val avance = (progreso.posicionMs.toFloat() / progreso.duracionMs.toFloat()).coerceIn(0f, 1f)
                        Spacer(Modifier.height(12.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(Color(0xFF342D3D), RoundedCornerShape(4.dp))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(avance)
                                    .height(6.dp)
                                    .background(violeta, RoundedCornerShape(4.dp))
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    val activoContinuar = seleccionado == 0
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(if (activoContinuar) Color(0xFF3B1E5E) else tarjeta, RoundedCornerShape(14.dp))
                            .border(if (activoContinuar) 3.dp else 1.dp, if (activoContinuar) violeta else Color(0xFF403748), RoundedCornerShape(14.dp))
                            .clickable { seleccionado = 0; onContinuar() }
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Text(
                            "▶ CONTINUAR DESDE ${tiempoNovaTV(progreso.posicionMs)}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    val activoInicio = seleccionado == 1
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(if (activoInicio) Color(0xFF3B1E5E) else tarjeta, RoundedCornerShape(14.dp))
                            .border(if (activoInicio) 3.dp else 1.dp, if (activoInicio) violeta else Color(0xFF403748), RoundedCornerShape(14.dp))
                            .clickable { seleccionado = 1; onDesdeInicio() }
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Text(
                            "↺ EMPEZAR DESDE EL PRINCIPIO",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        "↑ ↓ elegir   •   OK abrir   •   Atrás volver",
                        color = textoSecundario,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// v9.10.1: completa automáticamente portadas de progresos antiguos usando el respaldo TMDb ya configurado.
fun completarPortadaProgresoNovaTV(
    progreso: ProgresoNovaTV,
    onResultado: (String) -> Unit
) {
    if (progreso.portada.isNotBlank() || !tmdbActivoNovaTV()) return
    Thread {
        try {
            val portada = if (progreso.tipo == "PELICULA") {
                respaldoPeliculaTmdbNovaTV(
                    Pelicula(progreso.id, progreso.titulo, progreso.extension),
                    DetallePelicula()
                ).portada
            } else {
                val nombreSerie = progreso.serieNombre.ifBlank {
                    progreso.titulo.substringBefore(" · ").substringBefore(" - ").trim()
                }
                respaldoSerieTmdbNovaTV(
                    Serie(progreso.serieId, nombreSerie),
                    DetalleSerie()
                ).portada
            }
            if (portada.isNotBlank()) onResultado(portada)
        } catch (_: Exception) { }
    }.start()
}


// ============================================================
// v9.12.4 - Control parental NovaTV: PIN sólo al ingresar
// Bloqueo automático +18/Adultos y bloqueo manual por categoría.
// ============================================================
private const val PREFS_PARENTAL_NOVATV = "novatv_parental"

fun categoriaAdultosNovaTV(nombre: String): Boolean {
    val n = nombre.lowercase(Locale.getDefault())
    val palabras = listOf("adult", "adulto", "adultos", "xxx", "+18", "18+", "porno", "porn", "erotic", "erotico", "erótica", "erotica")
    return palabras.any { n.contains(it) }
}

fun claveBloqueoNovaTV(tipo: String, id: String) = "$tipo:$id"

fun cargarPinParentalNovaTV(context: Context): String {
    val guardado = context.getSharedPreferences(PREFS_PARENTAL_NOVATV, Context.MODE_PRIVATE)
        .getString("pin", null)
        ?.trim()
        .orEmpty()
    // PIN inicial provisto por el proveedor. Si el usuario lo cambia desde
    // Control parental, se seguirá usando el PIN personalizado guardado.
    return if (guardado.length == 4 && guardado.all { it.isDigit() }) guardado else "0000"
}

fun guardarPinParentalNovaTV(context: Context, pin: String) {
    context.getSharedPreferences(PREFS_PARENTAL_NOVATV, Context.MODE_PRIVATE).edit().putString("pin", pin).apply()
}

fun cargarBloqueosParentalNovaTV(context: Context): Set<String> =
    context.getSharedPreferences(PREFS_PARENTAL_NOVATV, Context.MODE_PRIVATE).getStringSet("bloqueos", emptySet())?.toSet() ?: emptySet()

fun guardarBloqueosParentalNovaTV(context: Context, bloqueos: Set<String>) {
    context.getSharedPreferences(PREFS_PARENTAL_NOVATV, Context.MODE_PRIVATE).edit().putStringSet("bloqueos", bloqueos).apply()
}

data class CategoriaParentalNovaTV(val tipo: String, val id: String, val nombre: String)

@Composable
fun PantallaPinParentalNovaTV(
    creando: Boolean,
    error: String,
    onVolver: () -> Unit,
    onAceptar: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    // -1 = campo PIN, 0 = volver, 1 = desbloquear/guardar
    var zona by remember { mutableIntStateOf(-1) }
    val focus = remember { FocusRequester() }
    val violeta = Color(0xFF9B5CFF)
    val violetaSuave = Color(0xFFC7A4FF)
    val tarjeta = Color(0xFF17131D)

    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF08070B))
            .padding(28.dp)
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionDown -> {
                        if (zona == -1) zona = 1
                        true
                    }
                    Key.DirectionUp -> {
                        if (zona >= 0) {
                            zona = -1
                            focus.requestFocus()
                        }
                        true
                    }
                    Key.DirectionLeft -> {
                        if (zona >= 0) zona = 0
                        true
                    }
                    Key.DirectionRight -> {
                        if (zona >= 0) zona = 1
                        true
                    }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        when (zona) {
                            0 -> onVolver()
                            1 -> if (pin.length == 4) onAceptar(pin)
                            else -> if (pin.length == 4) onAceptar(pin)
                        }
                        true
                    }
                    Key.Back, Key.Escape -> {
                        onVolver()
                        true
                    }
                    else -> false
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (creando) "🔐 CREAR PIN" else "🔐 CONTROL PARENTAL",
            color = violetaSuave,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (creando) "Crea un PIN de 4 dígitos" else "Introduce el PIN parental de 4 dígitos",
            color = Color.White
        )
        Spacer(Modifier.height(18.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { nuevo ->
                pin = nuevo.filter { it.isDigit() }.take(4)
                zona = -1
            },
            label = { Text("PIN") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .width(260.dp)
                .focusRequester(focus)
                .border(if (zona == -1) 2.dp else 0.dp, if (zona == -1) violeta else Color.Transparent, RoundedCornerShape(6.dp))
        )

        if (error.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = Color(0xFFFF8A80))
        }

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val volverActivo = zona == 0
            Box(
                Modifier
                    .background(if (volverActivo) Color(0xFF3B1E5E) else tarjeta, RoundedCornerShape(12.dp))
                    .border(if (volverActivo) 3.dp else 1.dp, if (volverActivo) violeta else Color(0xFF403748), RoundedCornerShape(12.dp))
                    .clickable { zona = 0; onVolver() }
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text("← VOLVER", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }

            val aceptarActivo = zona == 1
            Box(
                Modifier
                    .background(if (aceptarActivo) Color(0xFF3B1E5E) else tarjeta, RoundedCornerShape(12.dp))
                    .border(if (aceptarActivo) 3.dp else 1.dp, if (aceptarActivo) violeta else Color(0xFF403748), RoundedCornerShape(12.dp))
                    .clickable {
                        zona = 1
                        if (pin.length == 4) onAceptar(pin)
                    }
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text(
                    if (creando) "GUARDAR PIN" else "DESBLOQUEAR",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            if (zona == -1) "Escribe 4 dígitos y pulsa ↓ para elegir" else "← → elegir   •   OK confirmar   •   ↑ volver al PIN",
            color = Color(0xFFAAA3B5),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun PantallaControlParentalNovaTV(
    categorias: List<CategoriaParentalNovaTV>,
    bloqueos: Set<String>,
    onToggle: (CategoriaParentalNovaTV) -> Unit,
    onCambiarPin: () -> Unit,
    onVolver: () -> Unit
) {
    val lista = categorias.distinctBy { claveBloqueoNovaTV(it.tipo, it.id) }
    var indice by remember { mutableIntStateOf(0) }
    val state = rememberLazyListState()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(indice) { if (lista.isNotEmpty()) state.animateScrollToItem(indice.coerceIn(0, lista.lastIndex)) }
    Column(
        Modifier.fillMaxSize().background(Color(0xFF08070B)).padding(18.dp).focusRequester(focus).focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionUp -> { indice = (indice - 1).coerceAtLeast(0); true }
                    Key.DirectionDown -> { indice = (indice + 1).coerceAtMost((lista.size - 1).coerceAtLeast(0)); true }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> { lista.getOrNull(indice)?.let(onToggle); true }
                    Key.Back -> { onVolver(); true }
                    else -> false
                }
            }
    ) {
        Text("🔐 CONTROL PARENTAL", color = Color(0xFFC8A7FF), style = MaterialTheme.typography.headlineSmall)
        Text("Las categorías +18 se bloquean automáticamente. También puedes bloquear cualquier otra.", color = Color(0xFFAAA3B5))
        Spacer(Modifier.height(10.dp))
        Button(onClick = onCambiarPin) { Text("CAMBIAR PIN") }
        Spacer(Modifier.height(10.dp))
        LazyColumn(state = state, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
            itemsIndexed(lista) { i, cat ->
                val manual = claveBloqueoNovaTV(cat.tipo, cat.id) in bloqueos
                val automatico = categoriaAdultosNovaTV(cat.nombre)
                val activo = manual || automatico
                Row(
                    Modifier.fillMaxWidth().background(if (i == indice) Color(0xFF251638) else Color(0xFF15121B), RoundedCornerShape(10.dp))
                        .border(if (i == indice) 2.dp else 1.dp, if (i == indice) Color(0xFF9B5CFF) else Color(0xFF2A2432), RoundedCornerShape(10.dp))
                        .clickable { indice = i; onToggle(cat) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (activo) "🔒" else "🔓", fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(cat.nombre, color = Color.White)
                        Text(when(cat.tipo){"TV"->"TV EN VIVO";"PEL"->"PELÍCULAS";else->"SERIES"}, color = Color(0xFFAAA3B5), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(if (automatico) "+18 AUTO" else if (manual) "BLOQUEADA" else "LIBRE", color = if (activo) Color(0xFFC8A7FF) else Color(0xFFAAA3B5))
                }
            }
        }
    }
}

@Composable
fun PantallaInicioNovaTV(
    progresos: List<ProgresoNovaTV>,
    onSeleccionarContinuar: (ProgresoNovaTV) -> Unit,
    onTV: () -> Unit,
    onPeliculas: () -> Unit,
    onSeries: () -> Unit,
    onFavoritos: () -> Unit,
    onBuscar: () -> Unit,
    onActualizar: () -> Unit,
    actualizandoContenido: Boolean,
    estadoActualizacion: String,
    ultimaActualizacion: Long,
    onParental: () -> Unit,
    onCuenta: () -> Unit,
    mensaje: String
) {
    // v9.16: Inicio pensado primero para TV.
    // Tarjetas bastante más grandes + fondo cinematográfico rotativo de TMDb.
    val opciones = listOf(
        "📺" to "TV EN VIVO",
        "🎬" to "PELÍCULAS",
        "▣" to "SERIES",
        "⌕" to "BUSCAR",
        "↻" to "ACTUALIZAR CONTENIDO",
        "🔐" to "CONTROL PARENTAL",
        "●" to "MI CUENTA"
    )

    val fondoBase = Color(0xFF060508)
    val tarjeta = Color(0xD9181420)
    val tarjetaActiva = Color(0xF04A2675)
    val violeta = Color(0xFF9B5CFF)
    val violetaSuave = Color(0xFFD2B6FF)
    val textoSecundario = Color(0xFFC2BACB)

    var seleccionadoMenu by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val scroll = rememberScrollState()

    var banners by remember { mutableStateOf<List<BannerInicioNovaTV>>(emptyList()) }
    var bannerIndice by remember { mutableIntStateOf(0) }

    fun abrirMenu(i: Int) {
        when (opciones.getOrNull(i)?.second) {
            "TV EN VIVO" -> onTV()
            "PELÍCULAS" -> onPeliculas()
            "SERIES" -> onSeries()
            "BUSCAR" -> onBuscar()
            "ACTUALIZAR CONTENIDO" -> if (!actualizandoContenido) onActualizar()
            "CONTROL PARENTAL" -> onParental()
            "MI CUENTA" -> onCuenta()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()

        // Se carga fuera del hilo de interfaz. Si TMDb falla, el Home sigue funcionando
        // con el fondo negro/violeta normal.
        banners = withContext(Dispatchers.IO) {
            cargarBannersInicioTmdbNovaTV()
        }
        bannerIndice = 0
    }

    // Banner nuevo cada 9 segundos. No cambia el foco ni interfiere con el mando.
    LaunchedEffect(banners.size) {
        if (banners.size > 1) {
            while (true) {
                delay(9000)
                bannerIndice = (bannerIndice + 1) % banners.size
            }
        }
    }

    // Mantener visible la tarjeta elegida y recuperar la cabecera al volver arriba.
    LaunchedEffect(seleccionadoMenu) {
        val destino = when {
            seleccionadoMenu <= 2 -> 0
            else -> ((seleccionadoMenu - 2) * 92).coerceAtMost(scroll.maxValue)
        }
        scroll.animateScrollTo(destino)
    }

    val bannerActual = banners.getOrNull(
        bannerIndice.coerceIn(0, (banners.size - 1).coerceAtLeast(0))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fondoBase)
    ) {
        // Fondo rotativo. ImagenRemotaNovaTV ya usa la caché de imágenes de NovaTV.
        if (bannerActual != null && bannerActual.fondo.isNotBlank()) {
            ImagenRemotaNovaTV(
                url = bannerActual.fondo,
                descripcion = bannerActual.titulo,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Oscurece el fondo para mantener texto y foco legibles en cualquier película.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.30f),
                            Color.Black.copy(alpha = 0.66f),
                            Color.Black.copy(alpha = 0.94f)
                        )
                    )
                )
        )

        Column(
            Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) {
                        return@onPreviewKeyEvent false
                    }

                    when (e.key) {
                        Key.DirectionDown -> {
                            seleccionadoMenu =
                                (seleccionadoMenu + 1).coerceAtMost(opciones.lastIndex)
                            true
                        }

                        Key.DirectionUp -> {
                            seleccionadoMenu =
                                (seleccionadoMenu - 1).coerceAtLeast(0)
                            true
                        }

                        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                            abrirMenu(seleccionadoMenu)
                            true
                        }

                        else -> false
                    }
                }
                .verticalScroll(scroll)
                .padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera grande para que sea claramente visible a distancia en TV.
            Text(
                "NovaTV",
                color = Color.White,
                fontSize = 42.sp
            )
            Text(
                "TU ENTRETENIMIENTO",
                color = violetaSuave,
                fontSize = 15.sp
            )

            if (bannerActual != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    bannerActual.subtitulo,
                    color = violetaSuave,
                    fontSize = 12.sp
                )
                Text(
                    bannerActual.titulo,
                    color = Color.White,
                    fontSize = 18.sp,
                    maxLines = 1
                )
            }

            Spacer(Modifier.height(18.dp))

            // Menú grande: aproximadamente 88% del ancho disponible.
            opciones.forEachIndexed { i, (icono, titulo) ->
                val activo = i == seleccionadoMenu
                val escala by animateFloatAsState(
                    targetValue = if (activo) 1.035f else 1f,
                    label = "escalaMenuInicio"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .graphicsLayer {
                            scaleX = escala
                            scaleY = escala
                        }
                        .background(
                            if (activo) tarjetaActiva else tarjeta,
                            RoundedCornerShape(18.dp)
                        )
                        .border(
                            if (activo) 3.dp else 1.dp,
                            if (activo) Color(0xFFD5B7FF)
                            else Color.White.copy(alpha = 0.14f),
                            RoundedCornerShape(18.dp)
                        )
                        .clickable {
                            seleccionadoMenu = i
                            abrirMenu(i)
                        }
                        .padding(horizontal = 22.dp, vertical = 15.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(54.dp)
                                .height(46.dp)
                                .background(
                                    if (activo) violeta else Color(0xCC292230),
                                    RoundedCornerShape(13.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                icono,
                                color = Color.White,
                                fontSize = 26.sp
                            )
                        }

                        Column {
                            Text(
                                titulo,
                                color = Color.White,
                                fontSize = 22.sp
                            )

                            if (activo) {
                                Text(
                                    "OK PARA ENTRAR",
                                    color = Color(0xFFE5D5FF),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(11.dp))
            }

            Spacer(Modifier.height(8.dp))

            val ultima = if (ultimaActualizacion > 0) {
                SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    Locale.getDefault()
                ).format(Date(ultimaActualizacion))
            } else {
                "Nunca"
            }

            Text(
                "Última actualización: $ultima",
                color = textoSecundario,
                style = MaterialTheme.typography.bodySmall
            )

            if (actualizandoContenido) {
                Spacer(Modifier.height(7.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        Modifier.height(20.dp),
                        color = violeta
                    )
                    Text(
                        "Actualizando contenido...",
                        color = Color.White
                    )
                }
            } else if (estadoActualizacion.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    estadoActualizacion,
                    color = violetaSuave
                )
            }

            if (mensaje.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    mensaje,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
fun PantallaBusquedaNovaTV(
    texto: String,
    resultados: List<ResultadoBusqueda>,
    indiceSeleccionado: Int,
    buscando: Boolean,
    mensaje: String,
    onTextoChange: (String) -> Unit,
    onIndiceChange: (Int) -> Unit,
    onVolver: () -> Unit,
    onBuscar: () -> Unit,
    onAbrir: (ResultadoBusqueda) -> Unit
) {
    val listState = rememberLazyListState()

    val focusTexto = remember { FocusRequester() }
    val focusBoton = remember { FocusRequester() }
    val focusResultados = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusTexto.requestFocus()
    }

    LaunchedEffect(indiceSeleccionado, resultados.size) {
        if (resultados.isNotEmpty() && indiceSeleccionado in resultados.indices) {
            listState.animateScrollToItem(indiceSeleccionado)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onVolver) {
                Text("← VOLVER")
            }

            Spacer(Modifier.padding(5.dp))

            Text(
                "🔍 BUSCAR",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = texto,
            onValueChange = onTextoChange,
            label = { Text("Canal, película o serie") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusTexto)
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) {
                        return@onPreviewKeyEvent false
                    }

                    when (e.key) {
                        Key.DirectionDown -> {
                            focusBoton.requestFocus()
                            true
                        }

                        Key.Back, Key.Escape -> {
                            onVolver()
                            true
                        }

                        else -> false
                    }
                }
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onBuscar,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusBoton)
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) {
                        return@onPreviewKeyEvent false
                    }

                    when (e.key) {
                        Key.DirectionUp -> {
                            focusTexto.requestFocus()
                            true
                        }

                        Key.DirectionDown -> {
                            if (resultados.isNotEmpty()) {
                                focusResultados.requestFocus()
                                true
                            } else {
                                false
                            }
                        }

                        Key.Back, Key.Escape -> {
                            onVolver()
                            true
                        }

                        else -> false
                    }
                }
        ) {
            Text(if (buscando) "BUSCANDO..." else "BUSCAR")
        }

        if (buscando) {
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator()
        }

        if (mensaje.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(mensaje)
        }

        if (resultados.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))

            Text("Resultados: ${resultados.size}")

            Spacer(Modifier.height(6.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusResultados)
                    .focusable()
                    .onPreviewKeyEvent { e ->
                        if (e.type != KeyEventType.KeyDown) {
                            return@onPreviewKeyEvent false
                        }

                        when (e.key) {
                            Key.DirectionDown -> {
                                onIndiceChange(
                                    if (indiceSeleccionado >= resultados.lastIndex) {
                                        0
                                    } else {
                                        indiceSeleccionado + 1
                                    }
                                )
                                true
                            }

                            Key.DirectionUp -> {
                                if (indiceSeleccionado <= 0) {
                                    focusBoton.requestFocus()
                                } else {
                                    onIndiceChange(indiceSeleccionado - 1)
                                }
                                true
                            }

                            Key.Enter,
                            Key.NumPadEnter,
                            Key.DirectionCenter -> {
                                if (indiceSeleccionado in resultados.indices) {
                                    onAbrir(resultados[indiceSeleccionado])
                                }
                                true
                            }

                            Key.Back,
                            Key.Escape -> {
                                onVolver()
                                true
                            }

                            else -> false
                        }
                    }
            ) {
                items(
                    count = resultados.size,
                    key = { "${resultados[it].tipo}:${resultados[it].id}" }
                ) { i ->
                    val r = resultados[i]
                    val sel = i == indiceSeleccionado

                    val etiqueta = when (r.tipo) {
                        "TV" -> "📺 CANAL"
                        "PELICULA" -> "🎬 PELÍCULA"
                        else -> "📺 SERIE"
                    }

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .then(
                                if (sel) {
                                    Modifier
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = .16f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(8.dp)
                                        )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                onIndiceChange(i)
                                onAbrir(r)
                            }
                            .padding(
                                vertical = 14.dp,
                                horizontal = 12.dp
                            )
                    ) {
                        Text(
                            (if (sel) "▶ " else "") + r.nombre,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            etiqueta,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun PantallaCuentaNovaTV(
    cuenta: CuentaInfo,
    mensaje: String,
    onVolver: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var seleccionado by remember { mutableIntStateOf(0) }
    val scroll = rememberScrollState()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(seleccionado) {
        if (seleccionado == 0) scroll.animateScrollTo(0)
        else scroll.animateScrollTo(scroll.maxValue)
    }

    val dias = diasRestantes(cuenta.vencimientoEpoch)
    val textoDias = if (dias < 0) "Sin información" else "$dias días"
    val nombre = cuenta.nombre.ifBlank { "Usuario" }
    val estado = cuenta.estado.ifBlank { "Sin información" }
    val violeta = Color(0xFF9B5CFF)
    val violetaClaro = Color(0xFFD6BBFF)
    val tarjeta = Color(0xE6181420)

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF09070C), Color(0xFF130B1D), Color.Black)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (e.key) {
                        Key.DirectionDown, Key.DirectionUp -> {
                            seleccionado = if (seleccionado == 0) 1 else 0
                            true
                        }
                        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                            if (seleccionado == 0) onVolver() else onCerrarSesion()
                            true
                        }
                        Key.Back, Key.Escape -> { onVolver(); true }
                        else -> false
                    }
                }
                .verticalScroll(scroll)
                .padding(horizontal = 42.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("NOVA TV", color = violetaClaro, fontSize = 14.sp)
            Text("MI CUENTA", color = Color.White, fontSize = 38.sp)
            Spacer(Modifier.height(8.dp))
            Text(nombre, color = Color.White, fontSize = 24.sp)
            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .background(tarjeta, RoundedCornerShape(22.dp))
                    .border(1.dp, Color.White.copy(alpha = .14f), RoundedCornerShape(22.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                @Composable
                fun etiquetaCuenta(etiqueta: String, valor: String) {
                    Column {
                        Text(etiqueta, color = Color(0xFFAAA1B4), fontSize = 13.sp)
                        Text(valor, color = Color.White, fontSize = 20.sp)
                    }
                }

                etiquetaCuenta("ESTADO", estado)
                HorizontalDivider(color = Color.White.copy(alpha = .10f))
                etiquetaCuenta("VENCIMIENTO", fechaVencimiento(cuenta.vencimientoEpoch))
                HorizontalDivider(color = Color.White.copy(alpha = .10f))
                etiquetaCuenta("DÍAS RESTANTES", textoDias)
                HorizontalDivider(color = Color.White.copy(alpha = .10f))
                etiquetaCuenta(
                    "DISPOSITIVOS",
                    "${cuenta.conexionesActivas} en uso / ${cuenta.conexionesMaximas} permitidos"
                )
            }

            if (mensaje.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(mensaje, color = violetaClaro)
            }

            Spacer(Modifier.height(24.dp))

            listOf("←  VOLVER", "CERRAR SESIÓN").forEachIndexed { i, titulo ->
                val activo = seleccionado == i
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .padding(vertical = 6.dp)
                        .background(
                            if (activo) Color(0xFF542B83) else Color(0xE61A1620),
                            RoundedCornerShape(16.dp)
                        )
                        .border(
                            if (activo) 3.dp else 1.dp,
                            if (activo) violetaClaro else Color.White.copy(alpha = .16f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            seleccionado = i
                            if (i == 0) onVolver() else onCerrarSesion()
                        }
                        .padding(horizontal = 22.dp, vertical = 17.dp)
                ) {
                    Text(
                        (if (activo) "▶  " else "") + titulo,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PantallaLogin(
    usuario: String,
    clave: String,
    cargando: Boolean,
    mensaje: String,
    onUsuarioChange: (String) -> Unit,
    onClaveChange: (String) -> Unit,
    onLogin: () -> Unit
) {
    var mostrarClave by remember { mutableStateOf(false) }

    val focoUsuario = remember { FocusRequester() }
    val focoClave = remember { FocusRequester() }
    val focoVerClave = remember { FocusRequester() }
    val focoLogin = remember { FocusRequester() }

    val violeta = Color(0xFF9B5CFF)
    val violetaClaro = Color(0xFFD6BBFF)

    LaunchedEffect(Unit) {
        focoUsuario.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF070609), Color(0xFF160B20), Color.Black)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 42.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NovaTV",
                color = Color.White,
                fontSize = 46.sp
            )
            Text(
                text = "TU ENTRETENIMIENTO",
                color = violetaClaro,
                fontSize = 15.sp
            )

            Spacer(Modifier.height(34.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .background(Color(0xD9120F17), RoundedCornerShape(22.dp))
                    .border(1.dp, Color.White.copy(alpha = .14f), RoundedCornerShape(22.dp))
                    .padding(24.dp)
            ) {
                Text("INICIAR SESIÓN", color = Color.White, fontSize = 26.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Ingresa los datos de tu cuenta NovaTV",
                    color = Color(0xFFB8AFBF),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(22.dp))

                OutlinedTextField(
                    value = usuario,
                    onValueChange = onUsuarioChange,
                    label = { Text("Usuario") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focoClave.requestFocus() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = violeta,
                        focusedLabelColor = violetaClaro,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        unfocusedBorderColor = Color(0xFF6C6175),
                        cursorColor = violeta
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focoUsuario)
                        .onPreviewKeyEvent { e ->
                            if (e.type == KeyEventType.KeyDown &&
                                e.key == Key.DirectionDown
                            ) {
                                focoClave.requestFocus()
                                true
                            } else {
                                false
                            }
                        }
                )

                Spacer(Modifier.height(15.dp))

                OutlinedTextField(
                    value = clave,
                    onValueChange = onClaveChange,
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = if (mostrarClave) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!cargando) onLogin()
                        }
                    ),
                    trailingIcon = {
                        TextButton(
                            onClick = { mostrarClave = !mostrarClave },
                            modifier = Modifier
                                .focusRequester(focoVerClave)
                                .onPreviewKeyEvent { e ->
                                    if (e.type != KeyEventType.KeyDown) false
                                    else when (e.key) {
                                        Key.DirectionLeft -> {
                                            focoClave.requestFocus(); true
                                        }
                                        Key.DirectionDown -> {
                                            focoLogin.requestFocus(); true
                                        }
                                        Key.DirectionUp -> {
                                            focoUsuario.requestFocus(); true
                                        }
                                        else -> false
                                    }
                                }
                        ) {
                            Text(
                                if (mostrarClave) "OCULTAR" else "VER",
                                color = violetaClaro,
                                fontSize = 12.sp
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = violeta,
                        focusedLabelColor = violetaClaro,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        unfocusedBorderColor = Color(0xFF6C6175),
                        cursorColor = violeta
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focoClave)
                        .onPreviewKeyEvent { e ->
                            if (e.type != KeyEventType.KeyDown) {
                                false
                            } else {
                                when (e.key) {
                                    Key.DirectionUp -> {
                                        focoUsuario.requestFocus()
                                        true
                                    }
                                    Key.DirectionDown -> {
                                        focoLogin.requestFocus()
                                        true
                                    }
                                    Key.DirectionRight -> {
                                        focoVerClave.requestFocus()
                                        true
                                    }
                                    else -> false
                                }
                            }
                        }
                )

                Spacer(Modifier.height(22.dp))

                Button(
                    onClick = onLogin,
                    enabled = !cargando,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6537A0),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .focusRequester(focoLogin)
                        .onPreviewKeyEvent { e ->
                            if (e.type == KeyEventType.KeyDown &&
                                e.key == Key.DirectionUp
                            ) {
                                focoClave.requestFocus()
                                true
                            } else {
                                false
                            }
                        }
                ) {
                    if (cargando) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text("INICIAR SESIÓN", fontSize = 18.sp)
                    }
                }

                if (mensaje.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        mensaje,
                        color = Color(0xFFFFC2D0),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "NovaTV recuerda el último usuario utilizado. La contraseña se borra al cerrar sesión.",
                color = Color(0xFF92899B),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun PantallaFavoritos(
    favoritos: List<Favorito>,
    indiceSeleccionado: Int,
    onIndiceChange: (Int) -> Unit,
    onVolver: () -> Unit,
    onQuitar: (Favorito) -> Unit,
    onAbrir: (Favorito) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val pulsacionLarga = remember { PulsacionLargaFavorito() }
    LaunchedEffect(favoritos.size) { focusRequester.requestFocus() }
    LaunchedEffect(indiceSeleccionado, favoritos.size) {
        if (favoritos.isNotEmpty() && indiceSeleccionado in favoritos.indices) {
            listState.animateScrollToItem(indiceSeleccionado)
        }
    }

    Column(
        Modifier.fillMaxSize().focusRequester(focusRequester).focusable()
            .onPreviewKeyEvent { e ->
                val esOk = e.key == Key.Enter || e.key == Key.NumPadEnter || e.key == Key.DirectionCenter
                if (esOk) {
                    if (e.type == KeyEventType.KeyDown) {
                        pulsacionLarga.comenzar()
                        return@onPreviewKeyEvent true
                    }
                    if (e.type == KeyEventType.KeyUp) {
                        val larga = pulsacionLarga.terminar(true)
                        if (indiceSeleccionado in favoritos.indices) {
                            if (larga) onQuitar(favoritos[indiceSeleccionado])
                            else onAbrir(favoritos[indiceSeleccionado])
                        }
                        return@onPreviewKeyEvent true
                    }
                }
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                pulsacionLarga.cancelar()
                when(e.key) {
                    Key.DirectionDown -> { if(favoritos.isNotEmpty()) onIndiceChange(if(indiceSeleccionado>=favoritos.lastIndex) 0 else indiceSeleccionado+1); true }
                    Key.DirectionUp -> { if(favoritos.isNotEmpty()) onIndiceChange(if(indiceSeleccionado<=0) favoritos.lastIndex else indiceSeleccionado-1); true }
                    Key.Back, Key.Escape -> { onVolver(); true }
                    else -> false
                }
            }.padding(20.dp)
    ) {
        Button(onClick=onVolver) { Text("← INICIO") }
        Spacer(Modifier.height(15.dp))
        Text("⭐ FAVORITOS", style=MaterialTheme.typography.headlineMedium)
        Text("OK/toque: abrir   •   Mantener presionado: quitar ⭐", style=MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        if(favoritos.isEmpty()) {
            Text("No tienes favoritos en esta sección.")
        } else LazyColumn(state=listState, modifier=Modifier.fillMaxSize()) {
            items(count=favoritos.size, key={ "${favoritos[it].tipo}:${favoritos[it].id}" }) { i ->
                val f=favoritos[i]; val sel=i==indiceSeleccionado
                Column(
                    Modifier.fillMaxWidth()
                        .then(if(sel) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha=.16f), RoundedCornerShape(8.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier)
                        .combinedClickable(
                            onClick = { onIndiceChange(i); onAbrir(f) },
                            onLongClick = { onIndiceChange(i); onQuitar(f) }
                        )
                        .padding(vertical=18.dp,horizontal=12.dp)
                ) {
                    Text((if(sel) "▶ " else "") + "⭐ ${f.nombre}  [${f.tipo}]", style=MaterialTheme.typography.titleMedium)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun PantallaListaSimple(
    titulo: String,
    subtitulo: String,
    nombres: List<String>,
    mensaje: String,
    indiceSeleccionado: Int,
    onIndiceChange: (Int) -> Unit,
    onVolver: () -> Unit,
    favoritosMarcados: List<Boolean> = emptyList(),
    onFavorito: ((Int) -> Unit)? = null,
    onSeleccionar: (Int) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val pulsacionLarga = remember { PulsacionLargaFavorito() }
    LaunchedEffect(nombres.size) { focusRequester.requestFocus() }
    LaunchedEffect(indiceSeleccionado, nombres.size) {
        if (nombres.isNotEmpty() && indiceSeleccionado in nombres.indices) listState.animateScrollToItem(indiceSeleccionado)
    }
    Column(
        Modifier.fillMaxSize().focusRequester(focusRequester).focusable()
            .onPreviewKeyEvent { e ->
                val esOk = e.key == Key.Enter || e.key == Key.NumPadEnter || e.key == Key.DirectionCenter
                if (esOk && onFavorito != null) {
                    if (e.type == KeyEventType.KeyDown) {
                        pulsacionLarga.comenzar()
                        return@onPreviewKeyEvent true
                    }
                    if (e.type == KeyEventType.KeyUp) {
                        // Un KEY_UP heredado de la pantalla anterior no debe seleccionar
                        // automáticamente el elemento que conservó el foco.
                        if (!pulsacionLarga.activo) {
                            return@onPreviewKeyEvent true
                        }

                        val larga = pulsacionLarga.terminar(true)
                        if (indiceSeleccionado in nombres.indices) {
                            if (larga) onFavorito(indiceSeleccionado)
                            else onSeleccionar(indiceSeleccionado)
                        }
                        return@onPreviewKeyEvent true
                    }
                }
                if(e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                pulsacionLarga.cancelar()
                when(e.key) {
                    Key.DirectionDown -> { if(nombres.isNotEmpty()) onIndiceChange(if(indiceSeleccionado>=nombres.lastIndex) 0 else indiceSeleccionado+1); true }
                    Key.DirectionUp -> { if(nombres.isNotEmpty()) onIndiceChange(if(indiceSeleccionado<=0) nombres.lastIndex else indiceSeleccionado-1); true }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> { if(indiceSeleccionado in nombres.indices) onSeleccionar(indiceSeleccionado); true }
                    Key.Back, Key.Escape -> { onVolver(); true }
                    else -> false
                }
            }.padding(20.dp)
    ) {
        Button(onClick=onVolver) { Text("← VOLVER") }
        Spacer(Modifier.height(15.dp))
        Text(titulo, style=MaterialTheme.typography.headlineMedium)
        Text(subtitulo, style=MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        if(nombres.isEmpty()) {
            if(mensaje.startsWith("Cargando")) CircularProgressIndicator()
            Spacer(Modifier.height(10.dp)); Text(mensaje)
        } else LazyColumn(state=listState, modifier=Modifier.fillMaxSize()) {
            items(count=nombres.size) { i ->
                val sel=i==indiceSeleccionado
                Column(
                    Modifier.fillMaxWidth()
                        .then(if(sel) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha=.16f), RoundedCornerShape(8.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier)
                        .combinedClickable(
                            onClick = { onIndiceChange(i); onSeleccionar(i) },
                            onLongClick = {
                                onIndiceChange(i)
                                onFavorito?.invoke(i)
                            }
                        )
                        .padding(vertical=18.dp,horizontal=12.dp)
                ) {
                    val estrella = if (i < favoritosMarcados.size && favoritosMarcados[i]) " ⭐" else ""
                    Text((if(sel) "▶ ${nombres[i]}" else nombres[i]) + estrella, style=MaterialTheme.typography.titleMedium)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun ReproductorEpisodioNovaTV(
    usuario: String,
    clave: String,
    episodio: Episodio,
    posicionInicialMs: Long = 0L,
    onProgreso: (Long, Long) -> Unit = { _, _ -> },
    puedeAnterior: Boolean = false,
    puedeSiguiente: Boolean = false,
    onAnterior: () -> Unit = {},
    onSiguiente: () -> Unit = {},
    onSalir: () -> Unit
) {
    val context = LocalContext.current
    var avisoPista by remember { mutableStateOf("") }
    var controlesVisibles by remember { mutableStateOf(true) }
    var menuCCAbierto by remember { mutableStateOf(false) }
    var opcionesCC by remember { mutableStateOf<List<OpcionSubtituloNovaTV>>(emptyList()) }
    var errorReproduccionVOD by remember { mutableStateOf<String?>(null) }
    var esperandoVOD by remember { mutableStateOf(true) }
    var selectorEpisodiosVisible by remember { mutableStateOf(false) }
    var botonEpisodioSeleccionado by remember { mutableIntStateOf(1) } // 0 anterior, 1 play/pausa, 2 siguiente

    // v9.9.4: al terminar un episodio mostramos una cuenta regresiva
    // para reproducir el siguiente sin volver a la lista.
    var siguienteAutomaticoVisible by remember { mutableStateOf(false) }
    var segundosSiguienteAutomatico by remember { mutableIntStateOf(10) }
    var botonSiguienteAutomatico by remember { mutableIntStateOf(0) } // 0 reproducir ahora, 1 cancelar

    val httpFactory = remember(episodio.id, episodio.urlDirectaNovaTV) {
        DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .apply {
                if (episodio.urlDirectaNovaTV.isNotBlank()) {
                    setUserAgent(USER_AGENT_STREAM_NOVATV)
                }
            }
    }

    val player = remember(episodio.id) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
            .build()
    }

    val playerViewRef = remember { arrayOfNulls<PlayerView>(1) }

    DisposableEffect(episodio.id) {
        val ext = episodio.extension.ifBlank { "mp4" }
        val url = episodio.urlDirectaNovaTV.ifBlank {
            "$XTREAM_BASE_URL_NOVATV/series/${Uri.encode(usuario)}/${Uri.encode(clave)}/${episodio.id}.$ext"
        }

        errorReproduccionVOD = null
        esperandoVOD = true

        player.setMediaItem(
            if (episodio.urlDirectaNovaTV.isNotBlank()) {
                mediaItemDirectoNovaTV(url)
            } else {
                MediaItem.fromUri(url)
            }
        )
        player.prepare()
        if (posicionInicialMs > 0L) player.seekTo(posicionInicialMs)
        player.playWhenReady = true

        playerViewRef[0]?.post {
            playerViewRef[0]?.requestFocus()
            playerViewRef[0]?.showController()
        }

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> esperandoVOD = true
                    Player.STATE_READY -> {
                        esperandoVOD = false
                        errorReproduccionVOD = null
                    }
                    Player.STATE_ENDED -> {
                        esperandoVOD = false
                        if (puedeSiguiente) {
                            selectorEpisodiosVisible = false
                            siguienteAutomaticoVisible = true
                            segundosSiguienteAutomatico = 10
                            botonSiguienteAutomatico = 0
                            controlesVisibles = true
                            playerViewRef[0]?.showController()
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                esperandoVOD = false
                errorReproduccionVOD =
                    if (episodio.urlDirectaNovaTV.isNotBlank()) {
                        "No se pudo reproducir la URL directa de NovaTV"
                    } else {
                        "No se pudo reproducir este contenido"
                    }
                controlesVisibles = true
                playerViewRef[0]?.showController()
            }
        }

        player.addListener(listener)

        onDispose {
            val pos = player.currentPosition.coerceAtLeast(0L)
            val dur = player.duration.coerceAtLeast(0L)
            onProgreso(pos, dur)
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(episodio.id) {
        while (true) { delay(10_000); onProgreso(player.currentPosition.coerceAtLeast(0L), player.duration.coerceAtLeast(0L)) }
    }

    LaunchedEffect(episodio.id, esperandoVOD) {
        if (esperandoVOD) {
            delay(20000)

            if (esperandoVOD && errorReproduccionVOD == null) {
                errorReproduccionVOD =
                    "El contenido tarda demasiado en responder"
                controlesVisibles = true
                playerViewRef[0]?.showController()
            }
        }
    }

    LaunchedEffect(episodio.id, siguienteAutomaticoVisible) {
        if (siguienteAutomaticoVisible && puedeSiguiente) {
            while (siguienteAutomaticoVisible && segundosSiguienteAutomatico > 0) {
                delay(1000)
                if (siguienteAutomaticoVisible) segundosSiguienteAutomatico--
            }
            if (siguienteAutomaticoVisible && segundosSiguienteAutomatico <= 0) {
                siguienteAutomaticoVisible = false
                onSiguiente()
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    playerViewRef[0] = this

                    setBackgroundColor(android.graphics.Color.BLACK)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                    // La información superior aparece/desaparece junto
                    // con los controles nativos de ExoPlayer.
                    controllerShowTimeoutMs = 3000
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            controlesVisibles =
                                visibility == android.view.View.VISIBLE || menuCCAbierto
                        }
                    )

                    isFocusable = true
                    isFocusableInTouchMode = true
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

                    requestFocus()
                    post {
                        requestFocus()
                        showController()
                    }

                    setOnKeyListener { view, keyCode, event ->
                        if (event.action != KeyEvent.ACTION_DOWN) {
                            return@setOnKeyListener false
                        }

                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                if (siguienteAutomaticoVisible) {
                                    botonSiguienteAutomatico = 0
                                } else if (selectorEpisodiosVisible) {
                                    botonEpisodioSeleccionado = (botonEpisodioSeleccionado - 1).coerceAtLeast(0)
                                } else {
                                    player.seekTo((player.currentPosition - 15000L).coerceAtLeast(0L))
                                }
                                showController(); view.requestFocus(); true
                            }

                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (siguienteAutomaticoVisible) {
                                    botonSiguienteAutomatico = 1
                                } else if (selectorEpisodiosVisible) {
                                    botonEpisodioSeleccionado = (botonEpisodioSeleccionado + 1).coerceAtMost(2)
                                } else {
                                    val destino = player.currentPosition + 15000L
                                    val duracion = player.duration
                                    player.seekTo(if (duracion > 0L) destino.coerceAtMost(duracion) else destino)
                                }
                                showController(); view.requestFocus(); true
                            }

                            KeyEvent.KEYCODE_DPAD_CENTER,
                            KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                if (siguienteAutomaticoVisible) {
                                    if (botonSiguienteAutomatico == 0) {
                                        siguienteAutomaticoVisible = false
                                        onSiguiente()
                                    } else {
                                        siguienteAutomaticoVisible = false
                                    }
                                } else if (!selectorEpisodiosVisible) {
                                    selectorEpisodiosVisible = true
                                    botonEpisodioSeleccionado = 1
                                } else {
                                    when (botonEpisodioSeleccionado) {
                                        0 -> if (puedeAnterior) onAnterior()
                                        2 -> if (puedeSiguiente) onSiguiente()
                                        else -> if (player.isPlaying) player.pause() else player.play()
                                    }
                                }
                                showController(); view.requestFocus(); true
                            }

                            KeyEvent.KEYCODE_DPAD_UP -> {
                                avisoPista =
                                    cambiarPistaSiguiente(player, C.TRACK_TYPE_AUDIO)
                                showController()
                                view.requestFocus()
                                true
                            }

                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                avisoPista =
                                    cambiarPistaSiguiente(player, C.TRACK_TYPE_TEXT)
                                showController()
                                view.requestFocus()
                                true
                            }

                            KeyEvent.KEYCODE_BACK,
                            KeyEvent.KEYCODE_ESCAPE -> {
                                when {
                                    siguienteAutomaticoVisible -> siguienteAutomaticoVisible = false
                                    selectorEpisodiosVisible -> selectorEpisodiosVisible = false
                                    else -> onSalir()
                                }
                                true
                            }

                            else -> false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (errorReproduccionVOD != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        Color.Black.copy(alpha = 0.80f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = errorReproduccionVOD ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Pulsa Atrás para volver",
                    color = Color.White
                )
            }
        }

        if (siguienteAutomaticoVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.88f), RoundedCornerShape(18.dp))
                    .border(2.dp, Color(0xFF9B5CFF), RoundedCornerShape(18.dp))
                    .padding(horizontal = 28.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SIGUIENTE EPISODIO",
                    color = Color(0xFFC8A7FF),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Reproduciendo en $segundosSiguienteAutomatico segundos",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            siguienteAutomaticoVisible = false
                            onSiguiente()
                        },
                        border = if (botonSiguienteAutomatico == 0) BorderStroke(2.dp, Color(0xFFD7B8FF)) else null
                    ) {
                        Text("▶ REPRODUCIR AHORA")
                    }
                    Button(
                        onClick = { siguienteAutomaticoVisible = false },
                        border = if (botonSiguienteAutomatico == 1) BorderStroke(2.dp, Color(0xFFD7B8FF)) else null
                    ) {
                        Text("CANCELAR")
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "←/→ elegir   •   OK confirmar   •   Atrás cancelar",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (selectorEpisodiosVisible && !siguienteAutomaticoVisible) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp)
                    .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(14.dp)).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val labels = listOf("⏮ ANTERIOR", if (player.isPlaying) "⏸ PAUSA" else "▶ PLAY", "SIGUIENTE ⏭")
                labels.forEachIndexed { i, label ->
                    val habilitado = when(i) { 0 -> puedeAnterior; 2 -> puedeSiguiente; else -> true }
                    Button(
                        onClick = { if (habilitado) { botonEpisodioSeleccionado = i; when(i){0->onAnterior();2->onSiguiente();else->if(player.isPlaying)player.pause()else player.play()} } },
                        enabled = habilitado,
                        border = if (i == botonEpisodioSeleccionado) BorderStroke(2.dp, Color(0xFFC8A7FF)) else null
                    ) { Text(label) }
                }
            }
        }

        if (controlesVisibles || menuCCAbierto) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 58.dp, bottom = 42.dp)
            ) {
                Button(
                    onClick = {
                        opcionesCC = obtenerOpcionesSubtitulos(player)
                        menuCCAbierto = true
                        controlesVisibles = true
                        playerViewRef[0]?.showController()
                    },
                    modifier = Modifier.height(40.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 0.dp
                    )
                ) {
                    Text("CC")
                }

                DropdownMenu(
                    expanded = menuCCAbierto,
                    onDismissRequest = {
                        menuCCAbierto = false
                        playerViewRef[0]?.showController()
                    }
                ) {
                    if (opcionesCC.size <= 1) {
                        DropdownMenuItem(
                            text = { Text("No hay subtítulos disponibles") },
                            onClick = { menuCCAbierto = false },
                            enabled = false
                        )
                    } else {
                        opcionesCC.forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcion.nombre) },
                                onClick = {
                                    avisoPista =
                                        seleccionarSubtituloNovaTV(player, opcion)
                                    menuCCAbierto = false
                                    controlesVisibles = true
                                    playerViewRef[0]?.showController()
                                    playerViewRef[0]?.post {
                                        playerViewRef[0]?.requestFocus()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (controlesVisibles || menuCCAbierto) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = tituloEpisodioNovaTV(episodio),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = if (avisoPista.isBlank()) {
                        if (selectorEpisodiosVisible) "←/→ Elegir   OK Confirmar   Atrás Cerrar controles" else "↑ Audio   ↓ Subtítulos   ←/→ Buscar   OK Controles"
                    } else {
                        avisoPista
                    },
                    color = Color.White
                )
            }
        }
    }
}


@Composable
fun ImagenRemotaNovaTV(
    url: String,
    descripcion: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var bitmap by remember(url) { mutableStateOf(cacheLogosNovaTV.get(url)) }

    LaunchedEffect(url) {
        if (bitmap == null && url.isNotBlank()) {
            bitmap = descargarLogoNovaTV(url)
        }
    }

    val imagen = bitmap
    if (imagen != null) {
        Image(
            bitmap = imagen.asImageBitmap(),
            contentDescription = descripcion,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF1B1523)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "N",
                color = Color(0xFFC7A4FF),
                style = MaterialTheme.typography.headlineLarge
            )
        }
    }
}

@Composable
fun PantallaDetallePeliculaNovaTV(
    pelicula: Pelicula,
    detalle: DetallePelicula?,
    mensaje: String,
    esFavorita: Boolean,
    recomendaciones: List<Pelicula> = emptyList(),
    onAbrirRecomendacion: (Int) -> Unit = {},
    onVolver: () -> Unit,
    onReproducir: () -> Unit,
    onFavorito: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val entradaDetalleMs = remember { SystemClock.elapsedRealtime() }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var boton by remember { mutableIntStateOf(0) }
    var zonaRecomendaciones by remember { mutableStateOf(false) }
    var indiceRecomendacion by remember { mutableIntStateOf(0) }
    val recomendacionesState = rememberLazyListState()
    LaunchedEffect(indiceRecomendacion, recomendaciones.size) { if (indiceRecomendacion in recomendaciones.indices) recomendacionesState.animateScrollToItem(indiceRecomendacion) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val fondo = detalle
        ?.let { it.fondo.ifBlank { it.portada } }
        ?.ifBlank { pelicula.portada }
        ?: pelicula.portada
    val portada = detalle?.portada?.ifBlank { pelicula.portada } ?: pelicula.portada

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> {
                        if (zonaRecomendaciones) { zonaRecomendaciones = false; scope.launch { scrollState.animateScrollTo((scrollState.maxValue - 180).coerceAtLeast(0)) } }
                        else scope.launch { scrollState.animateScrollTo((scrollState.value - 220).coerceAtLeast(0)) }
                        true
                    }
                    Key.DirectionDown -> {
                        if (!zonaRecomendaciones && recomendaciones.isNotEmpty()) { zonaRecomendaciones = true; scope.launch { delay(50); scrollState.animateScrollTo(scrollState.maxValue) } }
                        else scope.launch { scrollState.animateScrollTo((scrollState.value + 220).coerceAtMost(scrollState.maxValue)) }
                        true
                    }
                    Key.DirectionLeft -> { if (zonaRecomendaciones) indiceRecomendacion=(indiceRecomendacion-1).coerceAtLeast(0) else boton=0; true }
                    Key.DirectionRight -> { if (zonaRecomendaciones) indiceRecomendacion=(indiceRecomendacion+1).coerceAtMost((recomendaciones.size-1).coerceAtLeast(0)) else boton=1; true }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        if (zonaRecomendaciones && indiceRecomendacion in recomendaciones.indices) onAbrirRecomendacion(indiceRecomendacion)
                        else if (boton == 0) onReproducir() else onFavorito()
                        true
                    }
                    Key.Back, Key.Escape -> {
                        // Al volver desde el player, consume el mismo Back físico
                        // durante unos milisegundos para no saltar también la ficha.
                        if (SystemClock.elapsedRealtime() - entradaDetalleMs >= 350L) onVolver()
                        true
                    }
                    else -> false
                }
            }
    ) {
        ImagenRemotaNovaTV(
            url = fondo,
            descripcion = pelicula.nombre,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.96f),
                            Color.Black.copy(alpha = 0.82f),
                            Color.Black.copy(alpha = 0.28f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(32.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(230.dp)
                    .height(340.dp)
                    .background(Color(0xFF18131F), RoundedCornerShape(18.dp))
                    .border(3.dp, Color(0xFF9B6DFF), RoundedCornerShape(18.dp))
            ) {
                ImagenRemotaNovaTV(
                    url = portada,
                    descripcion = "Portada de ${pelicula.nombre}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.width(34.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PELÍCULA",
                    color = Color(0xFFD8C2FF),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .background(Color(0x992B1747), RoundedCornerShape(50))
                        .border(1.dp, Color(0xFF8E5FEA), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = pelicula.nombre,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(Modifier.height(10.dp))

                val meta = listOfNotNull(
                    detalle?.anio?.takeIf { it.isNotBlank() },
                    detalle?.genero?.takeIf { it.isNotBlank() },
                    detalle?.duracion?.takeIf { it.isNotBlank() },
                    detalle?.rating?.takeIf { it.isNotBlank() }?.let { "★ $it" }
                ).joinToString("  •  ")

                if (meta.isNotBlank()) {
                    Text(meta, color = Color(0xFFC9B6EE), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(14.dp))
                }

                if (detalle == null && mensaje.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.width(28.dp).height(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(mensaje, color = Color.White)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = .42f), RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0x554B4256), RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = detalle?.descripcion?.ifBlank { "Sin descripción disponible." }
                                ?: "Sin descripción disponible.",
                            color = Color(0xFFF0EBF6),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 8
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    BotonDetalleNovaTV(
                        texto = "▶  REPRODUCIR",
                        seleccionado = boton == 0 && !zonaRecomendaciones,
                        onClick = { boton = 0; focusRequester.requestFocus(); onReproducir() }
                    )
                    BotonDetalleNovaTV(
                        texto = if (esFavorita) "★  QUITAR FAVORITO" else "☆  AÑADIR A FAVORITOS",
                        seleccionado = boton == 1 && !zonaRecomendaciones,
                        onClick = { boton = 1; focusRequester.requestFocus(); onFavorito() }
                    )
                }

                if (recomendaciones.isNotEmpty()) {
                    Spacer(Modifier.height(26.dp))
                    Text("TAMBIÉN TE PUEDE GUSTAR", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(10.dp))
                    LazyRow(state = recomendacionesState, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        items(count = recomendaciones.size, key = { recomendaciones[it].id }) { i ->
                            val r = recomendaciones[i]; val sel = zonaRecomendaciones && i == indiceRecomendacion
                            Column(Modifier.width(118.dp).background(Color(0xFF15121B), RoundedCornerShape(12.dp)).border(if(sel) 3.dp else 1.dp, if(sel) Color(0xFFB88AFF) else Color(0xFF3B3345), RoundedCornerShape(12.dp)).clickable { zonaRecomendaciones=true; indiceRecomendacion=i; focusRequester.requestFocus(); onAbrirRecomendacion(i) }.padding(6.dp)) {
                                ImagenRemotaNovaTV(r.portada, r.nombre, Modifier.fillMaxWidth().height(150.dp), ContentScale.Crop)
                                Spacer(Modifier.height(6.dp)); Text(r.nombre, color=Color.White, maxLines=2, style=MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (recomendaciones.isNotEmpty()) "↓ recomendaciones   ← → elegir   OK abrir   Atrás volver" else "← → elegir acción   OK aceptar   Atrás volver",
                    color = Color(0xFF9D93AA)
                )
            }
        }
    }
}

@Composable
fun BotonDetalleNovaTV(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    val escala by animateFloatAsState(if (seleccionado) 1.05f else 1f, label = "botonDetalle")
    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = escala; scaleY = escala }
            .background(
                if (seleccionado) Color(0xFF7138D9) else Color(0xCC18151E),
                RoundedCornerShape(14.dp)
            )
            .border(
                if (seleccionado) 3.dp else 1.dp,
                if (seleccionado) Color(0xFFD0AEFF) else Color(0xFF4B4256),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 15.dp)
    ) {
        Text(texto, color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun PantallaSeriesNovaTV(
    categorias: List<CategoriaSerie>,
    series: List<Serie>,
    mensaje: String,
    indiceCategoria: Int,
    indiceSerie: Int,
    favoritos: Set<String>,
    onVolver: () -> Unit,
    onIndiceCategoriaChange: (Int) -> Unit,
    onIndiceSerieChange: (Int) -> Unit,
    esCategoriaBloqueada: (CategoriaSerie) -> Boolean,
    onFavorito: (Serie) -> Unit,
    onCategoria: (CategoriaSerie) -> Unit,
    onAbrirSerie: (Int) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val categoriasState = rememberLazyListState()
    val seriesState = rememberLazyGridState()
    val pulsacionLarga = remember { PulsacionLargaFavorito() }
    var zona by remember { mutableIntStateOf(0) }

    LaunchedEffect(categorias.size) {
        focusRequester.requestFocus()
        if (categorias.isNotEmpty() && indiceCategoria in categorias.indices && series.isEmpty()) {
            val inicial = categorias[indiceCategoria]
            if (!esCategoriaBloqueada(inicial)) onCategoria(inicial)
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF08070B), Color(0xFF120A19), Color(0xFF08070B))))
    ) {
        val columnas = when {
            maxWidth >= 1000.dp -> 6
            maxWidth >= 760.dp -> 5
            maxWidth >= 520.dp -> 4
            else -> 2
        }
        val alturaPoster = if (maxWidth >= 760.dp) 190.dp else 205.dp

        LaunchedEffect(indiceCategoria, categorias.size) {
            if (indiceCategoria in categorias.indices) categoriasState.animateScrollToItem(indiceCategoria)
        }
        LaunchedEffect(indiceSerie, series.size) {
            if (indiceSerie in series.indices) seriesState.animateScrollToItem(indiceSerie)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    val esOk = event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter

                    if (zona == 1 && esOk) {
                        if (event.type == KeyEventType.KeyDown) {
                            pulsacionLarga.comenzar()
                            return@onPreviewKeyEvent true
                        }
                        if (event.type == KeyEventType.KeyUp) {
                            if (!pulsacionLarga.activo) return@onPreviewKeyEvent true
                            val larga = pulsacionLarga.terminar(true)
                            if (indiceSerie in series.indices) {
                                if (larga) onFavorito(series[indiceSerie]) else onAbrirSerie(indiceSerie)
                            }
                            return@onPreviewKeyEvent true
                        }
                    }

                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    pulsacionLarga.cancelar()

                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (zona == 0 && categorias.isNotEmpty()) {
                                val n = if (indiceCategoria <= 0) categorias.lastIndex else indiceCategoria - 1
                                onIndiceCategoriaChange(n)
                                if (!esCategoriaBloqueada(categorias[n])) onCategoria(categorias[n])
                            } else if (zona == 1 && series.isNotEmpty()) {
                                onIndiceSerieChange((indiceSerie - 1).coerceAtLeast(0))
                            }
                            true
                        }
                        Key.DirectionRight -> {
                            if (zona == 0 && categorias.isNotEmpty()) {
                                val n = if (indiceCategoria >= categorias.lastIndex) 0 else indiceCategoria + 1
                                onIndiceCategoriaChange(n)
                                if (!esCategoriaBloqueada(categorias[n])) onCategoria(categorias[n])
                            } else if (zona == 1 && series.isNotEmpty()) {
                                onIndiceSerieChange((indiceSerie + 1).coerceAtMost(series.lastIndex))
                            }
                            true
                        }
                        Key.DirectionDown -> {
                            if (zona == 0) {
                                if (series.isNotEmpty()) zona = 1
                            } else if (series.isNotEmpty()) {
                                onIndiceSerieChange((indiceSerie + columnas).coerceAtMost(series.lastIndex))
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            if (zona == 1 && series.isNotEmpty()) {
                                if (indiceSerie < columnas) zona = 0
                                else onIndiceSerieChange((indiceSerie - columnas).coerceAtLeast(0))
                            }
                            true
                        }
                        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                            if (zona == 0 && indiceCategoria in categorias.indices) onCategoria(categorias[indiceCategoria])
                            true
                        }
                        Key.Back, Key.Escape -> { onVolver(); true }
                        else -> false
                    }
                }
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("SERIES", color = Color.White, fontSize = 34.sp)
                    Text(
                        "Temporadas, favoritos y continuar serie",
                        color = Color(0xFFB69AF2),
                        fontSize = 14.sp
                    )
                }
                Text(
                    if (zona == 0) "← → Categorías   ↓ Series" else "↑ Categorías   Mantén OK ★",
                    color = Color(0xFF9D94A8),
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(18.dp))

            LazyRow(
                state = categoriasState,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().height(70.dp)
            ) {
                items(count = categorias.size, key = { categorias[it].id }) { i ->
                    val cat = categorias[i]
                    val seleccionada = i == indiceCategoria
                    val enfocada = seleccionada && zona == 0
                    Box(
                        modifier = Modifier
                            .background(
                                if (seleccionada) Color(0xFF2B1747) else Color(0xFF17171E),
                                RoundedCornerShape(14.dp)
                            )
                            .border(
                                if (enfocada) 3.dp else if (seleccionada) 2.dp else 1.dp,
                                if (seleccionada) Color(0xFF9B6DFF) else Color(0xFF33333D),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                zona = 0
                                onIndiceCategoriaChange(i)
                                onCategoria(cat)
                                focusRequester.requestFocus()
                            }
                            .padding(horizontal = 22.dp, vertical = 14.dp)
                    ) {
                        Text(cat.nombre, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            if (series.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (mensaje.startsWith("Cargando")) CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text(mensaje.ifBlank { "No se encontraron series" }, color = Color.White)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnas),
                    state = seriesState,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(count = series.size, key = { series[it].id }) { i ->
                        val serie = series[i]
                        val seleccionada = zona == 1 && i == indiceSerie
                        val favorita = claveFavorito("SERIE", serie.id) in favoritos
                        val escala by animateFloatAsState(if (seleccionada) 1.055f else 1f, label = "serieCard")
                        Column(
                            modifier = Modifier
                                .graphicsLayer { scaleX = escala; scaleY = escala }
                                .background(if (seleccionada) Color(0xFF1E142B) else Color(0xFF111116), RoundedCornerShape(16.dp))
                                .border(
                                    if (seleccionada) 3.dp else 1.dp,
                                    if (seleccionada) Color(0xFFB88AFF) else Color(0xFF2A2730),
                                    RoundedCornerShape(16.dp)
                                )
                                .combinedClickable(
                                    onClick = {
                                        zona = 1
                                        onIndiceSerieChange(i)
                                        focusRequester.requestFocus()
                                        onAbrirSerie(i)
                                    },
                                    onLongClick = {
                                        zona = 1
                                        onIndiceSerieChange(i)
                                        focusRequester.requestFocus()
                                        onFavorito(serie)
                                    }
                                )
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(alturaPoster),
                                contentAlignment = Alignment.Center
                            ) {
                                ImagenRemotaNovaTV(
                                    url = serie.portada,
                                    descripcion = "Portada de ${serie.nombre}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (favorita) {
                                    Text(
                                        "★",
                                        color = Color(0xFFC8A7FF),
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = .72f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                serie.nombre,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun limpiarTituloEpisodioNovaTV(titulo: String): String {
    var limpio = titulo.trim()

    // Muchos paneles Xtream envían:
    // "Nombre de la serie (2025-2026) - S01E01 - Título del episodio".
    // Si existe SxxExx en cualquier parte, conservamos primero lo que viene después.
    val codigoTemporadaEpisodio = Regex(
        "(?i)\\bS\\d{1,2}\\s*E\\d{1,3}\\b\\s*[-:·]?\\s*"
    )
    val coincidencia = codigoTemporadaEpisodio.find(limpio)
    if (coincidencia != null) {
        val despues = limpio.substring(coincidencia.range.last + 1)
            .trim()
            .trimStart('-', ':', '·', ' ')
            .trim()
        if (despues.isNotBlank()) {
            limpio = despues
        }
    }

    // Limpieza adicional para otros formatos habituales.
    limpio = limpio
        .replace(Regex("(?i)^\\s*(episodio|episode)\\s*\\d+\\s*[-:·]?\\s*"), "")
        .replace(Regex("(?i)^\\s*S\\d{1,2}\\s*E\\d{1,3}\\s*[-:·]?\\s*"), "")
        .replace(Regex("(?i)^\\s*T\\d{1,2}\\s*[·\\- ]+\\s*E\\d{1,3}\\s*[·\\-:]?\\s*"), "")
        .trim()
        .trimStart('-', ':', '·', ' ')
        .trim()

    return limpio.ifBlank { titulo.trim() }
}

fun duracionCortaNovaTV(ms: Long): String {
    if (ms <= 0L) return ""
    val totalMin = ms / 60000L
    val horas = totalMin / 60L
    val minutos = totalMin % 60L
    return if (horas > 0L) "${horas}h ${minutos}min" else "${totalMin} min"
}

fun tituloEpisodioNovaTV(ep: Episodio): String {
    val numero = if (ep.numero > 0) ep.numero else Regex("(?i)(?:episodio|episode|ep|e)\\s*[-.:#]?\\s*(\\d+)")
        .find(limpiarTituloEpisodioNovaTV(ep.titulo))?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    val limpio = limpiarTituloEpisodioNovaTV(ep.titulo)
        .replace(Regex("(?i)^\\s*(?:episodio|episode|ep|e)\\s*[-.:#]?\\s*\\d+\\s*[-:|.]?\\s*"), "")
        .trim()
    val prefijo = "T${ep.temporada.toString().padStart(2, '0')} · E${numero.toString().padStart(2, '0')}"
    return if (limpio.isBlank() || limpio.equals("Episodio $numero", true)) prefijo else "$prefijo · $limpio"
}

@Composable
fun PantallaDetalleSerieNovaTV(
    serie: Serie,
    detalle: DetalleSerie?,
    episodios: List<Episodio>,
    temporadas: List<Int>,
    mensaje: String,
    indiceTemporada: Int,
    indiceEpisodio: Int,
    esFavorita: Boolean,
    progresos: List<ProgresoNovaTV> = emptyList(),
    recomendaciones: List<Serie> = emptyList(),
    onAbrirRecomendacion: (Int) -> Unit = {},
    modoEpisodiosInicial: Boolean = false,
    onVolver: () -> Unit,
    onFavorito: () -> Unit,
    onTemporadaChange: (Int) -> Unit,
    onEpisodioChange: (Int) -> Unit,
    onReproducirEpisodio: (Int) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val entradaDetalleMs = remember { SystemClock.elapsedRealtime() }
    val temporadasState = rememberLazyListState()
    val episodiosState = rememberLazyListState()
    val detalleScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val recomendacionesState = rememberLazyListState()
    var indiceRecomendacion by remember { mutableIntStateOf(0) }
    var zona by remember(modoEpisodiosInicial) { mutableIntStateOf(if (modoEpisodiosInicial) 2 else 0) } // 0 favorito, 1 temporadas, 2 episodios, 3 recomendaciones
    val modoEpisodios = zona == 2
    LaunchedEffect(indiceRecomendacion, recomendaciones.size) { if (indiceRecomendacion in recomendaciones.indices) recomendacionesState.animateScrollToItem(indiceRecomendacion) }

    val temporadaActual = temporadas.getOrNull(indiceTemporada)
    val epsTemporada = if (temporadaActual != null) episodios.filter { it.temporada == temporadaActual } else emptyList()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(indiceTemporada, temporadas.size) {
        if (indiceTemporada in temporadas.indices) temporadasState.animateScrollToItem(indiceTemporada)
    }
    LaunchedEffect(indiceEpisodio, epsTemporada.size) {
        if (indiceEpisodio in epsTemporada.indices) episodiosState.animateScrollToItem(indiceEpisodio)
    }

    val fondo = detalle
        ?.let { it.fondo.ifBlank { it.portada } }
        ?.ifBlank { serie.portada }
        ?: serie.portada

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> {
                        if (zona == 0 && temporadas.isNotEmpty()) {
                            zona = 1
                            scope.launch { delay(60); detalleScrollState.animateScrollTo(detalleScrollState.maxValue) }
                        }
                        else if (zona == 1 && recomendaciones.isNotEmpty()) { zona = 3; scope.launch { delay(50); detalleScrollState.animateScrollTo(detalleScrollState.maxValue) } }
                        else if (zona == 1 && epsTemporada.isNotEmpty()) zona = 2
                        else if (zona == 3 && epsTemporada.isNotEmpty()) zona = 2
                        else if (zona == 2 && epsTemporada.isNotEmpty()) {
                            onEpisodioChange((indiceEpisodio + 1).coerceAtMost(epsTemporada.lastIndex))
                        }
                        true
                    }
                    Key.DirectionUp -> {
                        if (zona == 2) {
                            if (indiceEpisodio <= 0) {
                                zona = 1
                            } else {
                                onEpisodioChange((indiceEpisodio - 1).coerceAtLeast(0))
                            }
                        } else if (zona == 3) {
                            zona = 1
                        } else if (zona == 1) {
                            zona = 0
                            scope.launch { detalleScrollState.animateScrollTo(0) }
                        }
                        true
                    }
                    Key.DirectionLeft -> {
                        if (zona == 1 && temporadas.isNotEmpty()) { val n=(indiceTemporada-1).coerceAtLeast(0); onTemporadaChange(n) }
                        else if (zona == 3) indiceRecomendacion=(indiceRecomendacion-1).coerceAtLeast(0)
                        true
                    }
                    Key.DirectionRight -> {
                        if (zona == 1 && temporadas.isNotEmpty()) { val n=(indiceTemporada+1).coerceAtMost(temporadas.lastIndex); onTemporadaChange(n) }
                        else if (zona == 3) indiceRecomendacion=(indiceRecomendacion+1).coerceAtMost((recomendaciones.size-1).coerceAtLeast(0))
                        true
                    }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        if (zona == 0) onFavorito()
                        else if (zona == 3 && indiceRecomendacion in recomendaciones.indices) onAbrirRecomendacion(indiceRecomendacion)
                        else if (zona == 2 && indiceEpisodio in epsTemporada.indices) onReproducirEpisodio(indiceEpisodio)
                        true
                    }
                    Key.Back, Key.Escape -> {
                        // Evita que el Back que cerró el episodio atraviese la
                        // recomposición y cierre también la ficha de la serie.
                        if (SystemClock.elapsedRealtime() - entradaDetalleMs >= 350L) onVolver()
                        true
                    }
                    else -> false
                }
            }
    ) {
        ImagenRemotaNovaTV(
            url = fondo,
            descripcion = serie.nombre,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Black.copy(alpha = .97f),
                        Color.Black.copy(alpha = .88f),
                        Color.Black.copy(alpha = .45f)
                    )
                )
            )
        )

        if (modoEpisodios) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 34.dp, vertical = 26.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = serie.nombre,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1
                        )
                        Text(
                            text = if (temporadaActual != null) "Temporada $temporadaActual" else "Episodios",
                            color = Color(0xFFC7A4FF),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Text(
                        text = "↑↓ Episodios   OK Reproducir   ↑ en el primero: volver",
                        color = Color(0xFFB5A9C4)
                    )
                }

                Spacer(Modifier.height(18.dp))

                if (epsTemporada.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = mensaje.ifBlank { "No se encontraron episodios" },
                            color = Color.White
                        )
                    }
                } else {
                    LazyColumn(
                        state = episodiosState,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 26.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(count = epsTemporada.size, key = { epsTemporada[it].id }) { i ->
                            val ep = epsTemporada[i]
                            val seleccionado = i == indiceEpisodio
                            val escala by animateFloatAsState(
                                targetValue = if (seleccionado) 1.015f else 1f,
                                label = "episodioDetalle"
                            )

                            val progresoEp = progresos.firstOrNull {
                                it.tipo == "EPISODIO" && it.id == ep.id
                            }
                            val duracionEp = progresoEp?.duracionMs ?: 0L
                            val posicionEp = progresoEp?.posicionMs ?: 0L
                            val fraccionEp = if (duracionEp > 0L) {
                                (posicionEp.toFloat() / duracionEp.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                            val porcentajeEp = (fraccionEp * 100f).toInt()
                            val vistoEp = duracionEp > 0L &&
                                (fraccionEp >= 0.95f || duracionEp - posicionEp <= 120_000L)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = escala
                                        scaleY = escala
                                    }
                                    .background(
                                        if (seleccionado) Color(0xFF4A2675)
                                        else Color.Black.copy(alpha = .76f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .border(
                                        if (seleccionado) 3.dp else 1.dp,
                                        if (seleccionado) Color(0xFFD0AEFF) else Color(0xFF514758),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        onEpisodioChange(i)
                                        focusRequester.requestFocus()
                                        onReproducirEpisodio(i)
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = tituloEpisodioNovaTV(ep),
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2
                                    )

                                    if (duracionEp > 0L || posicionEp > 0L) {
                                        Spacer(Modifier.height(6.dp))

                                        val estadoEp = when {
                                            vistoEp && duracionEp > 0L ->
                                                "${duracionCortaNovaTV(duracionEp)} · ✓ Visto"
                                            duracionEp > 0L && posicionEp > 0L ->
                                                "${duracionCortaNovaTV(duracionEp)} · $porcentajeEp% visto"
                                            duracionEp > 0L ->
                                                duracionCortaNovaTV(duracionEp)
                                            else -> ""
                                        }

                                        if (estadoEp.isNotBlank()) {
                                            Text(
                                                text = estadoEp,
                                                color = if (seleccionado) Color(0xFFE6D5FF) else Color(0xFFB9AEC5),
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1
                                            )
                                        }

                                        if (duracionEp > 0L && posicionEp > 0L) {
                                            Spacer(Modifier.height(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .background(
                                                        Color.White.copy(alpha = .16f),
                                                        RoundedCornerShape(50)
                                                    )
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(fraccionEp.coerceAtLeast(0.01f))
                                                        .height(4.dp)
                                                        .background(
                                                            if (vistoEp) Color(0xFFCFAEFF) else Color(0xFF9B5CFF),
                                                            RoundedCornerShape(50)
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(detalleScrollState)
                    .padding(28.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .width(155.dp)
                            .height(225.dp)
                            .background(Color(0xFF18131F), RoundedCornerShape(16.dp))
                            .border(3.dp, Color(0xFF9B6DFF), RoundedCornerShape(16.dp))
                    ) {
                        ImagenRemotaNovaTV(
                            url = detalle?.portada?.ifBlank { serie.portada } ?: serie.portada,
                            descripcion = "Portada de ${serie.nombre}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(Modifier.width(26.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "SERIE",
                            color = Color(0xFFD8C2FF),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .background(Color(0x992B1747), RoundedCornerShape(50))
                                .border(1.dp, Color(0xFF8E5FEA), RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(serie.nombre, color = Color.White, style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(8.dp))
                        val meta = listOfNotNull(
                            detalle?.anio?.takeIf { it.isNotBlank() },
                            detalle?.genero?.takeIf { it.isNotBlank() },
                            detalle?.rating?.takeIf { it.isNotBlank() }?.let { "★ $it" }
                        ).joinToString("  •  ")
                        if (meta.isNotBlank()) Text(meta, color = Color(0xFFC9B6EE))

                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = .40f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0x554B4256), RoundedCornerShape(12.dp))
                                .padding(13.dp)
                        ) {
                            Text(
                                detalle?.descripcion?.ifBlank { "Sin descripción disponible." }
                                    ?: if (mensaje.isNotBlank()) mensaje else "Sin descripción disponible.",
                                color = Color(0xFFF0EBF6),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 4
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                        BotonDetalleNovaTV(
                            texto = if (esFavorita) "★  QUITAR FAVORITO" else "☆  AÑADIR A FAVORITOS",
                            seleccionado = zona == 0,
                            onClick = { zona = 0; focusRequester.requestFocus(); onFavorito() }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TEMPORADAS", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "${temporadas.size} ${if (temporadas.size == 1) "temporada" else "temporadas"}",
                        color = Color(0xFFC7A4FF),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.height(6.dp))

                LazyRow(
                    state = temporadasState,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(count = temporadas.size) { i ->
                        val seleccionada = i == indiceTemporada
                        val enfocada = zona == 1 && seleccionada
                        Box(
                            modifier = Modifier
                                .background(
                                    if (seleccionada) Color(0xFF6F35B5) else Color(0xFF211B29),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    if (enfocada) 3.dp else if (seleccionada) 2.dp else 1.dp,
                                    if (enfocada) Color(0xFFD7B8FF) else if (seleccionada) Color(0xFFA875FF) else Color(0xFF5A5063),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    zona = 1
                                    onTemporadaChange(i)
                                    focusRequester.requestFocus()
                                }
                                .padding(horizontal = 18.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = "Temporada ${temporadas[i]}",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (recomendaciones.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    Text("TAMBIÉN TE PUEDE GUSTAR", color=Color.White, style=MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(state=recomendacionesState, horizontalArrangement=Arrangement.spacedBy(12.dp), modifier=Modifier.fillMaxWidth()) {
                        items(count=recomendaciones.size, key={ recomendaciones[it].id }) { i ->
                            val r=recomendaciones[i]; val sel=zona==3 && i==indiceRecomendacion
                            Column(Modifier.width(112.dp).background(Color(0xFF15121B), RoundedCornerShape(12.dp)).border(if(sel) 3.dp else 1.dp, if(sel) Color(0xFFB88AFF) else Color(0xFF3B3345), RoundedCornerShape(12.dp)).clickable { zona=3; indiceRecomendacion=i; focusRequester.requestFocus(); onAbrirRecomendacion(i) }.padding(6.dp)) {
                                ImagenRemotaNovaTV(r.portada, r.nombre, Modifier.fillMaxWidth().height(142.dp), ContentScale.Crop)
                                Spacer(Modifier.height(6.dp)); Text(r.nombre, color=Color.White, maxLines=2, style=MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = .55f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF514758), RoundedCornerShape(12.dp))
                        .clickable {
                            if (epsTemporada.isNotEmpty()) {
                                zona = 2
                                if (indiceEpisodio !in epsTemporada.indices) onEpisodioChange(0)
                                focusRequester.requestFocus()
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EPISODIOS · OK REPRODUCIR",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (epsTemporada.isEmpty()) "Sin episodios" else "${epsTemporada.size} episodios   ↓ Ver lista",
                        color = Color(0xFFC7A4FF)
                    )
                }
            }
        }
    }
}

@Composable
fun PantallaCategoriasPeliculas(
    categorias: List<CategoriaPelicula>,
    peliculas: List<Pelicula>,
    mensaje: String,
    indiceSeleccionado: Int,
    indicePeliculaSeleccionada: Int,
    onIndiceChange: (Int) -> Unit,
    onIndicePeliculaChange: (Int) -> Unit,
    onVolver: () -> Unit,
    favoritos: Set<String>,
    onFavorito: (Pelicula) -> Unit,
    esCategoriaBloqueada: (CategoriaPelicula) -> Boolean,
    onAbrirPelicula: (Int) -> Unit,
    onCategoria: (CategoriaPelicula) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val categoriasState = rememberLazyListState()
    val peliculasState = rememberLazyGridState()
    val pulsacionLarga = remember { PulsacionLargaFavorito() }
    var zona by remember { mutableIntStateOf(0) } // 0 = categorías, 1 = películas

    LaunchedEffect(categorias.size) {
        focusRequester.requestFocus()
        if (categorias.isNotEmpty() && indiceSeleccionado in categorias.indices && peliculas.isEmpty()) {
            val inicial = categorias[indiceSeleccionado]
            if (!esCategoriaBloqueada(inicial)) onCategoria(inicial)
        }
    }

    LaunchedEffect(indiceSeleccionado, categorias.size) {
        if (categorias.isNotEmpty() && indiceSeleccionado in categorias.indices) {
            categoriasState.animateScrollToItem(indiceSeleccionado)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF08070B), Color(0xFF120A19), Color(0xFF08070B))))
    ) {
        val columnas = when {
            maxWidth >= 1000.dp -> 6
            maxWidth >= 760.dp -> 5
            maxWidth >= 520.dp -> 4
            else -> 2
        }
        val alturaPoster = if (maxWidth >= 760.dp) 190.dp else 205.dp

        LaunchedEffect(indicePeliculaSeleccionada, peliculas.size, columnas) {
            if (peliculas.isNotEmpty() && indicePeliculaSeleccionada in peliculas.indices) {
                peliculasState.animateScrollToItem(indicePeliculaSeleccionada)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    val esOk = event.key == Key.Enter ||
                        event.key == Key.NumPadEnter ||
                        event.key == Key.DirectionCenter

                    if (zona == 1 && esOk) {
                        if (event.type == KeyEventType.KeyDown) {
                            pulsacionLarga.comenzar()
                            return@onPreviewKeyEvent true
                        }
                        if (event.type == KeyEventType.KeyUp) {
                            if (!pulsacionLarga.activo) return@onPreviewKeyEvent true
                            val larga = pulsacionLarga.terminar(true)
                            if (indicePeliculaSeleccionada in peliculas.indices) {
                                if (larga) onFavorito(peliculas[indicePeliculaSeleccionada])
                                else onAbrirPelicula(indicePeliculaSeleccionada)
                            }
                            return@onPreviewKeyEvent true
                        }
                    }

                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    pulsacionLarga.cancelar()

                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (zona == 0) {
                                if (categorias.isNotEmpty()) {
                                    val nuevo = if (indiceSeleccionado <= 0) categorias.lastIndex else indiceSeleccionado - 1
                                    onIndiceChange(nuevo)
                                    if (!esCategoriaBloqueada(categorias[nuevo])) onCategoria(categorias[nuevo])
                                }
                            } else if (peliculas.isNotEmpty()) {
                                val nuevo = (indicePeliculaSeleccionada - 1).coerceAtLeast(0)
                                onIndicePeliculaChange(nuevo)
                            }
                            true
                        }

                        Key.DirectionRight -> {
                            if (zona == 0) {
                                if (categorias.isNotEmpty()) {
                                    val nuevo = if (indiceSeleccionado >= categorias.lastIndex) 0 else indiceSeleccionado + 1
                                    onIndiceChange(nuevo)
                                    if (!esCategoriaBloqueada(categorias[nuevo])) onCategoria(categorias[nuevo])
                                }
                            } else if (peliculas.isNotEmpty()) {
                                val nuevo = (indicePeliculaSeleccionada + 1).coerceAtMost(peliculas.lastIndex)
                                onIndicePeliculaChange(nuevo)
                            }
                            true
                        }

                        Key.DirectionDown -> {
                            if (zona == 0) {
                                if (peliculas.isNotEmpty()) zona = 1
                            } else if (peliculas.isNotEmpty()) {
                                val nuevo = (indicePeliculaSeleccionada + columnas).coerceAtMost(peliculas.lastIndex)
                                onIndicePeliculaChange(nuevo)
                            }
                            true
                        }

                        Key.DirectionUp -> {
                            if (zona == 1 && peliculas.isNotEmpty()) {
                                if (indicePeliculaSeleccionada < columnas) {
                                    zona = 0
                                } else {
                                    onIndicePeliculaChange((indicePeliculaSeleccionada - columnas).coerceAtLeast(0))
                                }
                            }
                            true
                        }

                        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                            if (zona == 0 && indiceSeleccionado in categorias.indices) {
                                onCategoria(categorias[indiceSeleccionado])
                            }
                            true
                        }

                        Key.Back, Key.Escape -> {
                            onVolver()
                            true
                        }

                        else -> false
                    }
                }
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PELÍCULAS",
                        color = Color.White,
                        fontSize = 34.sp
                    )
                    Text(
                        text = "Estrenos, favoritos y continuar viendo",
                        color = Color(0xFFB69AF2),
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = if (zona == 0) "← → Categorías   ↓ Películas" else "↑ Categorías   Mantén OK ★",
                    color = Color(0xFF9D94A8),
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            if (categorias.isEmpty()) {
                if (mensaje.startsWith("Cargando")) CircularProgressIndicator()
                Spacer(Modifier.height(10.dp))
                Text(mensaje, color = Color.White)
            } else {
                LazyRow(
                    state = categoriasState,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().height(70.dp)
                ) {
                    items(count = categorias.size, key = { categorias[it].id }) { indice ->
                        val item = categorias[indice]
                        val seleccionada = indice == indiceSeleccionado
                        val enfocada = seleccionada && zona == 0
                        val escala by animateFloatAsState(
                            targetValue = if (enfocada) 1.06f else 1f,
                            label = "categoriaPelicula"
                        )

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = escala
                                    scaleY = escala
                                }
                                .background(
                                    if (seleccionada) Color(0xFF2B1747) else Color(0xFF17171E),
                                    RoundedCornerShape(14.dp)
                                )
                                .border(
                                    width = if (enfocada) 3.dp else if (seleccionada) 2.dp else 1.dp,
                                    color = if (seleccionada) Color(0xFF9B6DFF) else Color(0xFF33333D),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    zona = 0
                                    onIndiceChange(indice)
                                    onCategoria(item)
                                    focusRequester.requestFocus()
                                }
                                .padding(horizontal = 26.dp, vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.nombre,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                if (peliculas.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (mensaje.startsWith("Cargando")) CircularProgressIndicator()
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = mensaje.ifBlank { "No se encontraron películas" },
                                color = Color.White
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnas),
                        state = peliculasState,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(count = peliculas.size, key = { peliculas[it].id }) { indice ->
                            val pelicula = peliculas[indice]
                            val seleccionada = zona == 1 && indice == indicePeliculaSeleccionada
                            val esFavorita = claveFavorito("PELICULA", pelicula.id) in favoritos
                            val escala by animateFloatAsState(
                                targetValue = if (seleccionada) 1.055f else 1f,
                                label = "posterPelicula"
                            )

                            Column(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = escala
                                        scaleY = escala
                                    }
                                    .background(if (seleccionada) Color(0xFF1E142B) else Color(0xFF111116), RoundedCornerShape(16.dp))
                                    .border(
                                        width = if (seleccionada) 3.dp else 1.dp,
                                        color = if (seleccionada) Color(0xFFB88AFF) else Color(0xFF2A2730),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            zona = 1
                                            onIndicePeliculaChange(indice)
                                            focusRequester.requestFocus()
                                            onAbrirPelicula(indice)
                                        },
                                        onLongClick = {
                                            zona = 1
                                            onIndicePeliculaChange(indice)
                                            focusRequester.requestFocus()
                                            onFavorito(pelicula)
                                        }
                                    )
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(alturaPoster)
                                        .background(Color(0xFF21172D), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PosterPeliculaNovaTV(
                                        url = pelicula.portada,
                                        nombre = pelicula.nombre,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (esFavorita) {
                                        Text(
                                            text = "★",
                                            color = Color(0xFFC8A7FF),
                                            style = MaterialTheme.typography.titleLarge,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .background(
                                                    Color.Black.copy(alpha = 0.72f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = pelicula.nombre,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PosterPeliculaNovaTV(
    url: String,
    nombre: String,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(url) { mutableStateOf(cacheLogosNovaTV.get(url)) }

    LaunchedEffect(url) {
        if (bitmap == null && url.isNotBlank()) {
            bitmap = descargarLogoNovaTV(url)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val imagen = bitmap
        if (imagen != null) {
            Image(
                bitmap = imagen.asImageBitmap(),
                contentDescription = "Carátula de $nombre",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "N",
                    color = Color(0xFFC7A4FF),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = nombre,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 3
                )
            }
        }
    }
}

@Composable
fun PantallaPeliculas(
    categoria: String,
    peliculas: List<Pelicula>,
    mensaje: String,
    indiceSeleccionado: Int,
    onIndiceChange: (Int) -> Unit,
    onVolver: () -> Unit,
    favoritos: Set<String>,
    onFavorito: (Pelicula) -> Unit,
    onPelicula: (Int) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val pulsacionLarga = remember { PulsacionLargaFavorito() }
    LaunchedEffect(peliculas.size) { focusRequester.requestFocus() }
    LaunchedEffect(indiceSeleccionado, peliculas.size) {
        if (peliculas.isNotEmpty() && indiceSeleccionado in peliculas.indices) listState.animateScrollToItem(indiceSeleccionado)
    }

    Column(
        modifier = Modifier.fillMaxSize().focusRequester(focusRequester).focusable()
            .onPreviewKeyEvent { event ->
                val esOk = event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter
                if (esOk) {
                    if (event.type == KeyEventType.KeyDown) {
                        pulsacionLarga.comenzar()
                        return@onPreviewKeyEvent true
                    }
                    if (event.type == KeyEventType.KeyUp) {
                        // Evita el "doble ENTER" al entrar desde una categoría ya cacheada.
                        if (!pulsacionLarga.activo) {
                            return@onPreviewKeyEvent true
                        }

                        val larga = pulsacionLarga.terminar(true)
                        if (indiceSeleccionado in peliculas.indices) {
                            if (larga) onFavorito(peliculas[indiceSeleccionado])
                            else onPelicula(indiceSeleccionado)
                        }
                        return@onPreviewKeyEvent true
                    }
                }
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                pulsacionLarga.cancelar()
                when(event.key) {
                    Key.DirectionDown -> { if (peliculas.isNotEmpty()) onIndiceChange(if(indiceSeleccionado>=peliculas.lastIndex) 0 else indiceSeleccionado+1); true }
                    Key.DirectionUp -> { if (peliculas.isNotEmpty()) onIndiceChange(if(indiceSeleccionado<=0) peliculas.lastIndex else indiceSeleccionado-1); true }
                    Key.Back, Key.Escape -> { onVolver(); true }
                    else -> false
                }
            }.padding(20.dp)
    ) {
        Button(onClick=onVolver) { Text("← CATEGORÍAS") }
        Spacer(Modifier.height(15.dp))
        Text(categoria, style=MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(10.dp))
        if(peliculas.isEmpty()) {
            if(mensaje.startsWith("Cargando")) CircularProgressIndicator()
            Spacer(Modifier.height(10.dp)); Text(mensaje)
        } else {
            Text("Películas: ${peliculas.size}")
            Spacer(Modifier.height(10.dp))
            LazyColumn(state=listState, modifier=Modifier.fillMaxSize()) {
                items(count=peliculas.size, key={ peliculas[it].id }) { indice ->
                    val item=peliculas[indice]; val sel=indice==indiceSeleccionado
                    Column(
                        modifier=Modifier.fillMaxWidth()
                            .then(if(sel) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha=.16f), RoundedCornerShape(8.dp))
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier)
                            .combinedClickable(
                                onClick = { onIndiceChange(indice); onPelicula(indice) },
                                onLongClick = { onIndiceChange(indice); onFavorito(item) }
                            )
                            .padding(vertical=18.dp,horizontal=12.dp)
                    ) {
                        val estrella = if (claveFavorito("PELICULA", item.id) in favoritos) " ⭐" else ""
                        Text((if(sel) "▶ ${item.nombre}" else item.nombre) + estrella, style=MaterialTheme.typography.titleMedium)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}


data class OpcionSubtituloNovaTV(
    val nombre: String,
    val grupo: androidx.media3.common.TrackGroup?,
    val indice: Int?
)

fun obtenerOpcionesSubtitulos(player: ExoPlayer): List<OpcionSubtituloNovaTV> {
    val resultado = mutableListOf(
        OpcionSubtituloNovaTV(
            nombre = "Desactivados",
            grupo = null,
            indice = null
        )
    )

    val grupos = player.currentTracks.groups.filter {
        it.type == C.TRACK_TYPE_TEXT && it.isSupported
    }

    var numero = 1
    for (grupo in grupos) {
        for (i in 0 until grupo.length) {
            if (grupo.isTrackSupported(i)) {
                val formato = grupo.mediaTrackGroup.getFormat(i)
                val nombre = formato.label?.takeIf { it.isNotBlank() }
                    ?: formato.language?.takeIf { it.isNotBlank() }
                    ?: "Subtítulo $numero"

                resultado.add(
                    OpcionSubtituloNovaTV(
                        nombre = nombre,
                        grupo = grupo.mediaTrackGroup,
                        indice = i
                    )
                )
                numero++
            }
        }
    }

    return resultado
}

fun seleccionarSubtituloNovaTV(
    player: ExoPlayer,
    opcion: OpcionSubtituloNovaTV
): String {
    val builder = player.trackSelectionParameters
        .buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_TEXT)

    if (opcion.grupo == null || opcion.indice == null) {
        player.trackSelectionParameters = builder
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()

        return "Subtítulos: desactivados"
    }

    player.trackSelectionParameters = builder
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        .setOverrideForType(
            androidx.media3.common.TrackSelectionOverride(
                opcion.grupo,
                opcion.indice
            )
        )
        .build()

    return "Subtítulos: ${opcion.nombre}"
}

fun cambiarPistaSiguiente(player: ExoPlayer, tipo: Int): String {
    val grupos = player.currentTracks.groups.filter { it.type == tipo && it.isSupported }
    val opciones = mutableListOf<Pair<androidx.media3.common.TrackGroup, Int>>()

    for (grupo in grupos) {
        for (i in 0 until grupo.length) {
            if (grupo.isTrackSupported(i)) {
                opciones.add(grupo.mediaTrackGroup to i)
            }
        }
    }

    if (opciones.isEmpty()) {
        return if (tipo == C.TRACK_TYPE_AUDIO) "No hay otros audios" else "No hay subtítulos"
    }

    val actual = opciones.indexOfFirst { (trackGroup, indice) ->
        grupos.any { g ->
            g.mediaTrackGroup == trackGroup && g.isTrackSelected(indice)
        }
    }

    // En subtítulos agregamos una opción extra: apagados.
    if (tipo == C.TRACK_TYPE_TEXT && actual == opciones.lastIndex) {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        return "Subtítulos: desactivados"
    }

    val siguiente = if (actual < 0) 0 else (actual + 1) % opciones.size
    val (grupo, indice) = opciones[siguiente]

    player.trackSelectionParameters = player.trackSelectionParameters
        .buildUpon()
        .setTrackTypeDisabled(tipo, false)
        .clearOverridesOfType(tipo)
        .setOverrideForType(androidx.media3.common.TrackSelectionOverride(grupo, indice))
        .build()

    val formato = grupo.getFormat(indice)
    val nombre = formato.label?.takeIf { it.isNotBlank() }
        ?: formato.language?.takeIf { it.isNotBlank() }
        ?: "Pista ${siguiente + 1}"

    return if (tipo == C.TRACK_TYPE_AUDIO) "Audio: $nombre" else "Subtítulos: $nombre"
}

@Composable
fun ReproductorPeliculaNovaTV(
    usuario: String,
    clave: String,
    pelicula: Pelicula,
    posicionInicialMs: Long = 0L,
    onProgreso: (Long, Long) -> Unit = { _, _ -> },
    onSalir: () -> Unit
) {
    val context = LocalContext.current
    var avisoPista by remember { mutableStateOf("") }
    var controlesVisibles by remember { mutableStateOf(true) }
    var menuCCAbierto by remember { mutableStateOf(false) }
    var opcionesCC by remember { mutableStateOf<List<OpcionSubtituloNovaTV>>(emptyList()) }
    var errorReproduccionVOD by remember { mutableStateOf<String?>(null) }
    var esperandoVOD by remember { mutableStateOf(true) }

    val httpFactory = remember(pelicula.id, pelicula.urlDirectaNovaTV) {
        DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .apply {
                if (pelicula.urlDirectaNovaTV.isNotBlank()) {
                    setUserAgent(USER_AGENT_STREAM_NOVATV)
                }
            }
    }

    val player = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
            .build()
    }

    val playerViewRef = remember { arrayOfNulls<PlayerView>(1) }

    DisposableEffect(pelicula.id) {
        val ext = pelicula.extension.ifBlank { "mp4" }
        val url = pelicula.urlDirectaNovaTV.ifBlank {
            "$XTREAM_BASE_URL_NOVATV/movie/${Uri.encode(usuario)}/${Uri.encode(clave)}/${pelicula.id}.$ext"
        }

        errorReproduccionVOD = null
        esperandoVOD = true

        player.setMediaItem(
            if (pelicula.urlDirectaNovaTV.isNotBlank()) {
                mediaItemDirectoNovaTV(url)
            } else {
                MediaItem.fromUri(url)
            }
        )
        player.prepare()
        if (posicionInicialMs > 0L) player.seekTo(posicionInicialMs)
        player.playWhenReady = true

        playerViewRef[0]?.post {
            playerViewRef[0]?.requestFocus()
            playerViewRef[0]?.showController()
        }

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> esperandoVOD = true
                    Player.STATE_READY -> {
                        esperandoVOD = false
                        errorReproduccionVOD = null
                    }
                    Player.STATE_ENDED -> esperandoVOD = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                esperandoVOD = false
                errorReproduccionVOD =
                    "No se pudo reproducir este contenido"
                controlesVisibles = true
                playerViewRef[0]?.showController()
            }
        }

        player.addListener(listener)

        onDispose {
            val pos = player.currentPosition.coerceAtLeast(0L)
            val dur = player.duration.coerceAtLeast(0L)
            onProgreso(pos, dur)
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(pelicula.id) {
        while (true) { delay(10_000); onProgreso(player.currentPosition.coerceAtLeast(0L), player.duration.coerceAtLeast(0L)) }
    }

    LaunchedEffect(pelicula.id, esperandoVOD) {
        if (esperandoVOD) {
            delay(20000)

            if (esperandoVOD && errorReproduccionVOD == null) {
                errorReproduccionVOD =
                    "El contenido tarda demasiado en responder"
                controlesVisibles = true
                playerViewRef[0]?.showController()
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    playerViewRef[0] = this

                    setBackgroundColor(android.graphics.Color.BLACK)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                    // El título y las ayudas se sincronizan con los
                    // controles nativos del reproductor.
                    controllerShowTimeoutMs = 3000
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            controlesVisibles =
                                visibility == android.view.View.VISIBLE || menuCCAbierto
                        }
                    )

                    isFocusable = true
                    isFocusableInTouchMode = true
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

                    requestFocus()
                    post {
                        requestFocus()
                        showController()
                    }

                    setOnKeyListener { view, keyCode, event ->
                        if (event.action != KeyEvent.ACTION_DOWN) {
                            return@setOnKeyListener false
                        }

                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                player.seekTo(
                                    (player.currentPosition - 15000L).coerceAtLeast(0L)
                                )
                                showController()
                                view.requestFocus()
                                true
                            }

                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                val destino = player.currentPosition + 15000L
                                val duracion = player.duration
                                player.seekTo(
                                    if (duracion > 0L) destino.coerceAtMost(duracion)
                                    else destino
                                )
                                showController()
                                view.requestFocus()
                                true
                            }

                            KeyEvent.KEYCODE_DPAD_CENTER,
                            KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                if (player.isPlaying) player.pause() else player.play()
                                showController()
                                view.requestFocus()
                                true
                            }

                            KeyEvent.KEYCODE_DPAD_UP -> {
                                avisoPista =
                                    cambiarPistaSiguiente(player, C.TRACK_TYPE_AUDIO)
                                showController()
                                view.requestFocus()
                                true
                            }

                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                avisoPista =
                                    cambiarPistaSiguiente(player, C.TRACK_TYPE_TEXT)
                                showController()
                                view.requestFocus()
                                true
                            }

                            KeyEvent.KEYCODE_BACK,
                            KeyEvent.KEYCODE_ESCAPE -> {
                                onSalir()
                                true
                            }

                            else -> false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (errorReproduccionVOD != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        Color.Black.copy(alpha = 0.80f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = errorReproduccionVOD ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Pulsa Atrás para volver",
                    color = Color.White
                )
            }
        }

        if (controlesVisibles || menuCCAbierto) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 58.dp, bottom = 42.dp)
            ) {
                Button(
                    onClick = {
                        opcionesCC = obtenerOpcionesSubtitulos(player)
                        menuCCAbierto = true
                        controlesVisibles = true
                        playerViewRef[0]?.showController()
                    },
                    modifier = Modifier.height(40.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 0.dp
                    )
                ) {
                    Text("CC")
                }

                DropdownMenu(
                    expanded = menuCCAbierto,
                    onDismissRequest = {
                        menuCCAbierto = false
                        playerViewRef[0]?.showController()
                    }
                ) {
                    if (opcionesCC.size <= 1) {
                        DropdownMenuItem(
                            text = { Text("No hay subtítulos disponibles") },
                            onClick = { menuCCAbierto = false },
                            enabled = false
                        )
                    } else {
                        opcionesCC.forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcion.nombre) },
                                onClick = {
                                    avisoPista =
                                        seleccionarSubtituloNovaTV(player, opcion)
                                    menuCCAbierto = false
                                    controlesVisibles = true
                                    playerViewRef[0]?.showController()
                                    playerViewRef[0]?.post {
                                        playerViewRef[0]?.requestFocus()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (controlesVisibles || menuCCAbierto) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = pelicula.nombre,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = if (avisoPista.isBlank()) {
                        "↑ Audio   ↓ Subtítulos   ←/→ Buscar   OK Play/Pausa"
                    } else {
                        avisoPista
                    },
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun PantallaTVEnVivo(
    usuario: String,
    clave: String,
    categorias: List<CategoriaTV>,
    canales: List<CanalTV>,
    mensaje: String,
    indiceCategoria: Int,
    indiceCanal: Int,
    favoritos: Set<String>,
    onVolverInicio: () -> Unit,
    esCategoriaBloqueada: (CategoriaTV) -> Boolean,
    onCategoriaSoloSeleccionada: (Int, CategoriaTV) -> Unit,
    onCategoriaEntrar: (Int, CategoriaTV) -> Unit,
    onCategoriaChange: (Int, CategoriaTV) -> Unit,
    onCanalIndiceChange: (Int) -> Unit,
    onFavorito: (CanalTV) -> Unit,
    onCanal: (Int) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val categoriasState = rememberLazyListState()
    val canalesState = rememberLazyGridState()
    val pulsacionLarga = remember { PulsacionLargaFavorito() }

    // 0 = categorías de arriba, 1 = cuadrícula de canales.
    var zonaFoco by remember { mutableIntStateOf(0) }
    var categoriaInicialCargada by remember { mutableStateOf(false) }
    var epgSeleccionado by remember { mutableStateOf<EPGCanalNovaTV?>(null) }
    var cargandoEPG by remember { mutableStateOf(false) }

    val fondo = Color(0xFF08070B)
    val tarjeta = Color(0xFF15121B)
    val tarjetaActiva = Color(0xFF251638)
    val violeta = Color(0xFF9B5CFF)
    val violetaSuave = Color(0xFFC7A4FF)
    val textoSecundario = Color(0xFFAAA3B5)

    // Índice inteligente: si una variante del mismo canal sí trae logo
    // (por ejemplo ESPN HD) podemos reutilizarlo para ESPN FHD/UHD.
    val logosAlternativos = remember(canales) {
        canales
            .asSequence()
            .filter { it.logo.isNotBlank() }
            .groupBy { normalizarNombreCanalNovaTV(it.nombre) }
            .mapValues { (_, variantes) -> variantes.first().logo }
    }

    LaunchedEffect(canales.getOrNull(indiceCanal)?.id) {
        val canal = canales.getOrNull(indiceCanal)
        if (canal == null || canal.urlDirectaNovaTV.isNotBlank()) {
            epgSeleccionado = null
            cargandoEPG = false
        } else {
            cargandoEPG = true
            epgSeleccionado = obtenerEPGNovaTV(usuario, clave, canal.id)
            cargandoEPG = false
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    // Al entrar por primera vez en TV EN VIVO cargamos la categoría recordada.
    LaunchedEffect(categorias.size) {
        if (categorias.isNotEmpty() && !categoriaInicialCargada) {
            val seguro = indiceCategoria.coerceIn(categorias.indices)
            categoriaInicialCargada = true
            val inicial = categorias[seguro]
            if (esCategoriaBloqueada(inicial)) onCategoriaSoloSeleccionada(seguro, inicial)
            else onCategoriaChange(seguro, inicial)
        }
    }

    // Mantiene la categoría activa visible al desplazarse horizontalmente.
    LaunchedEffect(indiceCategoria, categorias.size) {
        if (categorias.isNotEmpty() && indiceCategoria in categorias.indices) {
            categoriasState.animateScrollToItem(indiceCategoria)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(fondo, Color(0xFF140A1D), fondo)))
    ) {
        // Guardamos el ancho fuera de los items del LazyVerticalGrid.
        // Así evitamos el conflicto de receptores de Compose dentro de cada tarjeta.
        val anchoPantalla = maxWidth

        // En TV quedan 4 tarjetas grandes por fila. En teléfono se reducen para
        // que el nombre siga siendo legible.
        val columnas = when {
            anchoPantalla >= 900.dp -> 4
            anchoPantalla >= 600.dp -> 3
            else -> 2
        }

        LaunchedEffect(indiceCanal, canales.size, columnas) {
            if (canales.isNotEmpty() && indiceCanal in canales.indices) {
                canalesState.animateScrollToItem(indiceCanal)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    val esOk = event.key == Key.Enter ||
                            event.key == Key.NumPadEnter ||
                            event.key == Key.DirectionCenter

                    if (zonaFoco == 1 && esOk) {
                        if (event.type == KeyEventType.KeyDown) {
                            pulsacionLarga.comenzar()
                            return@onPreviewKeyEvent true
                        }
                        if (event.type == KeyEventType.KeyUp) {
                            if (!pulsacionLarga.activo) return@onPreviewKeyEvent true
                            val larga = pulsacionLarga.terminar(true)
                            if (indiceCanal in canales.indices) {
                                if (larga) onFavorito(canales[indiceCanal])
                                else onCanal(indiceCanal)
                            }
                            return@onPreviewKeyEvent true
                        }
                    }

                    if (event.type != KeyEventType.KeyDown) {
                        return@onPreviewKeyEvent false
                    }

                    pulsacionLarga.cancelar()

                    if (zonaFoco == 0) {
                        when (event.key) {
                            Key.DirectionLeft -> {
                                if (categorias.isNotEmpty()) {
                                    val nuevo = if (indiceCategoria <= 0) categorias.lastIndex else indiceCategoria - 1
                                    val cat = categorias[nuevo]
                                    if (esCategoriaBloqueada(cat)) onCategoriaSoloSeleccionada(nuevo, cat)
                                    else onCategoriaChange(nuevo, cat)
                                }
                                true
                            }

                            Key.DirectionRight -> {
                                if (categorias.isNotEmpty()) {
                                    val nuevo = if (indiceCategoria >= categorias.lastIndex) 0 else indiceCategoria + 1
                                    val cat = categorias[nuevo]
                                    if (esCategoriaBloqueada(cat)) onCategoriaSoloSeleccionada(nuevo, cat)
                                    else onCategoriaChange(nuevo, cat)
                                }
                                true
                            }

                            Key.DirectionDown, Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                                val cat = categorias.getOrNull(indiceCategoria)
                                if (cat != null && esCategoriaBloqueada(cat)) {
                                    onCategoriaEntrar(indiceCategoria, cat)
                                } else if (canales.isNotEmpty()) {
                                    zonaFoco = 1
                                }
                                true
                            }

                            Key.Back, Key.Escape -> {
                                onVolverInicio()
                                true
                            }

                            else -> false
                        }
                    } else {
                        when (event.key) {
                            Key.DirectionLeft -> {
                                if (canales.isNotEmpty() && indiceCanal % columnas > 0) {
                                    onCanalIndiceChange(indiceCanal - 1)
                                }
                                true
                            }

                            Key.DirectionRight -> {
                                val siguiente = indiceCanal + 1
                                if (
                                    canales.isNotEmpty() &&
                                    indiceCanal % columnas < columnas - 1 &&
                                    siguiente <= canales.lastIndex
                                ) {
                                    onCanalIndiceChange(siguiente)
                                }
                                true
                            }

                            Key.DirectionDown -> {
                                val siguienteFila = indiceCanal + columnas
                                if (siguienteFila <= canales.lastIndex) {
                                    onCanalIndiceChange(siguienteFila)
                                }
                                true
                            }

                            Key.DirectionUp -> {
                                val filaAnterior = indiceCanal - columnas
                                if (filaAnterior >= 0) {
                                    onCanalIndiceChange(filaAnterior)
                                } else {
                                    zonaFoco = 0
                                }
                                true
                            }

                            Key.Back, Key.Escape -> {
                                // Primer Back vuelve a la barra de categorías; otro Back sale a Inicio.
                                zonaFoco = 0
                                true
                            }

                            else -> false
                        }
                    }
                }
                .padding(horizontal = 22.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TV EN VIVO",
                        color = Color.White,
                        fontSize = 34.sp
                    )
                    Text(
                        text = "Canales en directo · programación y favoritos",
                        color = violetaSuave,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = if (zonaFoco == 0) "← → Categorías   ↓ Canales" else "↑ Categorías   OK Ver   Mantener OK ★",
                    color = textoSecundario,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            if (categorias.isEmpty()) {
                CircularProgressIndicator(color = violeta)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (mensaje.isBlank()) "Cargando categorías..." else mensaje,
                    color = Color.White
                )
                return@Column
            }

            LazyRow(
                state = categoriasState,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
            ) {
                items(
                    count = categorias.size,
                    key = { categorias[it].id }
                ) { indice ->
                    val categoria = categorias[indice]
                    val activa = indice == indiceCategoria
                    val enfocada = activa && zonaFoco == 0
                    val escala by animateFloatAsState(
                        targetValue = if (enfocada) 1.06f else 1f,
                        label = "categoriaTV"
                    )

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = escala
                                scaleY = escala
                            }
                            .height(64.dp)
                            .background(
                                if (activa) tarjetaActiva else tarjeta,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = if (enfocada) 3.dp else if (activa) 2.dp else 1.dp,
                                color = if (activa) violeta else Color(0xFF302A39),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                zonaFoco = 0
                                if (esCategoriaBloqueada(categoria)) onCategoriaEntrar(indice, categoria)
                                else onCategoriaChange(indice, categoria)
                            }
                            .padding(horizontal = 26.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = categoria.nombre,
                            color = if (activa) Color.White else Color(0xFFE5E0EA),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categorias.getOrNull(indiceCategoria)?.nombre ?: "",
                    color = violetaSuave,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.width(12.dp))
                if (canales.isNotEmpty()) {
                    Text(
                        text = "${canales.size} canales",
                        color = textoSecundario
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (canales.isNotEmpty()) {
                val canalSel = canales.getOrNull(indiceCanal)
                val epg = epgSeleccionado
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .background(Color(0xFF100D16), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF352448), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = canalSel?.nombre.orEmpty(),
                            color = violetaSuave,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1
                        )
                        Text(
                            text = when {
                                cargandoEPG -> "Cargando programación…"
                                epg?.ahora != null -> "Ahora ${horaEPGNovaTV(epg.ahora.inicio)}–${horaEPGNovaTV(epg.ahora.fin)}  •  ${epg.ahora.titulo}"
                                else -> "Programación no disponible"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }
                    if (epg?.siguiente != null) {
                        Spacer(Modifier.width(18.dp))
                        Text(
                            text = "Siguiente ${horaEPGNovaTV(epg.siguiente.inicio)}  •  ${epg.siguiente.titulo}",
                            color = textoSecundario,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            modifier = Modifier.weight(.75f)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (canales.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (mensaje == "Cargando canales...") {
                            CircularProgressIndicator(color = violeta)
                            Spacer(Modifier.height(12.dp))
                        }
                        Text(
                            text = if (mensaje.isBlank()) "Selecciona una categoría" else mensaje,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnas),
                    state = canalesState,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        count = canales.size,
                        key = { canales[it].id }
                    ) { indice ->
                        val canal = canales[indice]
                        val seleccionado = indice == indiceCanal && zonaFoco == 1
                        val esFavorito = claveFavorito("TV", canal.id) in favoritos
                        val escala by animateFloatAsState(
                            targetValue = if (seleccionado) 1.045f else 1f,
                            label = "canalTV"
                        )

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = escala
                                    scaleY = escala
                                }
                                .height(if (anchoPantalla >= 600.dp) 132.dp else 118.dp)
                                .background(
                                    if (seleccionado) tarjetaActiva else tarjeta,
                                    RoundedCornerShape(14.dp)
                                )
                                .border(
                                    width = if (seleccionado) 3.dp else 1.dp,
                                    color = if (seleccionado) violeta else Color(0xFF302A39),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .combinedClickable(
                                    onClick = {
                                        zonaFoco = 1
                                        onCanalIndiceChange(indice)
                                        onCanal(indice)
                                    },
                                    onLongClick = {
                                        zonaFoco = 1
                                        onCanalIndiceChange(indice)
                                        onFavorito(canal)
                                    }
                                )
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    LogoCanalNovaTV(
                                        url = canal.logo,
                                        urlAlternativa = logosAlternativos[normalizarNombreCanalNovaTV(canal.nombre)].orEmpty(),
                                        nombre = canal.nombre,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (anchoPantalla >= 600.dp) 62.dp else 52.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = canal.nombre,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2
                                    )
                                }

                                if (esFavorito) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .background(
                                                Color(0xCC251638),
                                                RoundedCornerShape(50)
                                            )
                                            .border(
                                                1.dp,
                                                violeta,
                                                RoundedCornerShape(50)
                                            )
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "★",
                                            color = violetaSuave,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val cacheLogosNovaTV = object : LruCache<String, Bitmap>(180) {}

// ============================================================
// IPTV-org - respaldo opcional de logos de canales
// Prioridad: Xtream -> variante Xtream -> IPTV-org -> iniciales NovaTV
// No usa streams de IPTV-org; solo su catálogo público de canales/logos.
// ============================================================
private const val IPTV_ORG_CANALES_NOVATV = "https://iptv-org.github.io/api/channels.json"
private const val IPTV_ORG_LOGOS_NOVATV = "https://iptv-org.github.io/api/logos.json"

private data class LogoIptvOrgNovaTV(
    val url: String,
    val ancho: Int,
    val enUso: Boolean
)

private val lockIptvOrgNovaTV = Any()
@Volatile private var indiceIptvOrgCargadoNovaTV = false
private val idsIptvOrgPorNombreNovaTV = mutableMapOf<String, MutableList<String>>()
private val logoIptvOrgPorCanalNovaTV = mutableMapOf<String, LogoIptvOrgNovaTV>()

private fun normalizarNombreCanalNovaTV(nombre: String): String {
    val sinAcentos = Normalizer.normalize(nombre, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")

    return sinAcentos
        .uppercase(Locale.ROOT)
        // Prefijos habituales del panel/proveedor.
        .replace(Regex("^(ARG|AR|LATAM|LAT|US|USA|MX|MEX|ES|ESP|CL|CHI|UY|URU|PE|PER|CO|COL)\\s*[:|\\-]\\s*"), "")
        // Calidad de imagen: no forma parte de la identidad del canal.
        .replace(Regex("\\b(FHD|FULL\\s*HD|HD|SD|UHD|4K|HEVC|H265|H264|HDR)\\b"), " ")
        // Marcadores habituales que tampoco cambian el canal.
        .replace(Regex("\\b(1080P?|720P?|576P?|480P?|50FPS|60FPS|LATINO|CASTELLANO|DUAL|SUB|VIP)\\b"), " ")
        .replace(Regex("[^A-Z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
}

private fun paisPreferidoIptvOrgNovaTV(nombreOriginal: String): String? {
    val n = nombreOriginal.trim().uppercase(Locale.ROOT)
    return when {
        Regex("^(ARG|AR)\\s*[:|\\-]").containsMatchIn(n) -> "ar"
        Regex("^(MX|MEX)\\s*[:|\\-]").containsMatchIn(n) -> "mx"
        Regex("^(ES|ESP)\\s*[:|\\-]").containsMatchIn(n) -> "es"
        Regex("^(US|USA)\\s*[:|\\-]").containsMatchIn(n) -> "us"
        Regex("^(CL|CHI)\\s*[:|\\-]").containsMatchIn(n) -> "cl"
        Regex("^(UY|URU)\\s*[:|\\-]").containsMatchIn(n) -> "uy"
        Regex("^(PE|PER)\\s*[:|\\-]").containsMatchIn(n) -> "pe"
        Regex("^(CO|COL)\\s*[:|\\-]").containsMatchIn(n) -> "co"
        else -> null
    }
}

private fun inicialesCanalNovaTV(nombre: String): String {
    val limpio = normalizarNombreCanalNovaTV(nombre)
    if (limpio.isBlank()) return "TV"

    val palabras = limpio.split(" ").filter { it.isNotBlank() }
    return when {
        palabras.size >= 2 -> (palabras[0].take(1) + palabras[1].take(1)).take(3)
        palabras[0].length <= 4 -> palabras[0].take(4)
        else -> palabras[0].take(3)
    }
}

private fun leerTextoHttpNovaTV(url: String): String {
    var actual = url
    var intentos = 0
    while (intentos < 4) {
        val conexion = URL(actual).openConnection() as HttpURLConnection
        conexion.connectTimeout = 9000
        conexion.readTimeout = 15000
        conexion.instanceFollowRedirects = false
        conexion.setRequestProperty("User-Agent", "NovaTV/9.7")
        conexion.connect()

        val codigo = conexion.responseCode
        if (codigo in 300..399) {
            val destino = conexion.getHeaderField("Location")
            conexion.disconnect()
            if (destino.isNullOrBlank()) throw IllegalStateException("Redirección sin destino")
            actual = URL(URL(actual), destino).toString()
            intentos++
            continue
        }

        if (codigo !in 200..299) {
            conexion.disconnect()
            throw IllegalStateException("HTTP $codigo")
        }

        return conexion.inputStream.bufferedReader().use { it.readText() }.also { conexion.disconnect() }
    }
    throw IllegalStateException("Demasiadas redirecciones")
}

private fun cargarIndiceIptvOrgNovaTVSiHaceFalta() {
    if (indiceIptvOrgCargadoNovaTV) return

    synchronized(lockIptvOrgNovaTV) {
        if (indiceIptvOrgCargadoNovaTV) return@synchronized

        try {
            val canalesJson = JSONArray(leerTextoHttpNovaTV(IPTV_ORG_CANALES_NOVATV))
            for (i in 0 until canalesJson.length()) {
                val c = canalesJson.optJSONObject(i) ?: continue
                val id = c.optString("id").trim()
                if (id.isBlank()) continue

                val nombres = mutableListOf(c.optString("name"))
                val alternativos = c.optJSONArray("alt_names")
                if (alternativos != null) {
                    for (j in 0 until alternativos.length()) nombres.add(alternativos.optString(j))
                }

                for (nombre in nombres) {
                    val clave = normalizarNombreCanalNovaTV(nombre)
                    if (clave.isBlank()) continue
                    val ids = idsIptvOrgPorNombreNovaTV.getOrPut(clave) { mutableListOf() }
                    if (id !in ids) ids.add(id)
                }
            }

            val logosJson = JSONArray(leerTextoHttpNovaTV(IPTV_ORG_LOGOS_NOVATV))
            for (i in 0 until logosJson.length()) {
                val l = logosJson.optJSONObject(i) ?: continue
                val canal = l.optString("channel").trim()
                val url = l.optString("url").trim()
                val formato = l.optString("format").uppercase(Locale.ROOT)
                if (canal.isBlank() || url.isBlank()) continue

                // BitmapFactory no decodifica SVG. Elegimos formatos raster compatibles
                // también con Android 9, que es importante para LDPlayer/TV Box.
                if (formato !in setOf("PNG", "JPEG", "JPG", "WEBP", "GIF")) continue

                val candidato = LogoIptvOrgNovaTV(
                    url = url,
                    ancho = l.optInt("width", 0),
                    enUso = l.optBoolean("in_use", false)
                )
                val actualLogo = logoIptvOrgPorCanalNovaTV[canal]
                val mejor = actualLogo == null ||
                    (candidato.enUso && !actualLogo.enUso) ||
                    (candidato.enUso == actualLogo.enUso && candidato.ancho > actualLogo.ancho)
                if (mejor) logoIptvOrgPorCanalNovaTV[canal] = candidato
            }
        } catch (_: Exception) {
            // El respaldo nunca debe romper TV. Se podrá reintentar más adelante.
            idsIptvOrgPorNombreNovaTV.clear()
            logoIptvOrgPorCanalNovaTV.clear()
            return@synchronized
        }

        indiceIptvOrgCargadoNovaTV = true
    }
}

private suspend fun buscarLogoIptvOrgNovaTV(nombre: String): String = withContext(Dispatchers.IO) {
    cargarIndiceIptvOrgNovaTVSiHaceFalta()
    if (!indiceIptvOrgCargadoNovaTV) return@withContext ""

    val clave = normalizarNombreCanalNovaTV(nombre)
    if (clave.isBlank()) return@withContext ""

    val candidatos = idsIptvOrgPorNombreNovaTV[clave].orEmpty()
    if (candidatos.isEmpty()) return@withContext ""

    val pais = paisPreferidoIptvOrgNovaTV(nombre)
    val ordenados = if (pais != null) {
        candidatos.sortedByDescending { it.lowercase(Locale.ROOT).endsWith(".$pais") }
    } else candidatos

    ordenados.firstNotNullOfOrNull { logoIptvOrgPorCanalNovaTV[it]?.url } ?: ""
}

private suspend fun descargarLogoNovaTV(url: String): Bitmap? = withContext(Dispatchers.IO) {
    if (url.isBlank()) return@withContext null

    cacheLogosNovaTV.get(url)?.let { return@withContext it }

    try {
        var actual = url
        var intentos = 0
        var resultado: Bitmap? = null

        while (intentos < 4 && resultado == null) {
            val conexion = URL(actual).openConnection() as HttpURLConnection
            conexion.connectTimeout = 7000
            conexion.readTimeout = 7000
            conexion.instanceFollowRedirects = false
            conexion.setRequestProperty("User-Agent", "NovaTV/9.7")
            conexion.connect()

            val codigo = conexion.responseCode
            if (codigo in 300..399) {
                val destino = conexion.getHeaderField("Location")
                conexion.disconnect()
                if (destino.isNullOrBlank()) break
                actual = URL(URL(actual), destino).toString()
                intentos++
            } else if (codigo in 200..299) {
                resultado = conexion.inputStream.use { BitmapFactory.decodeStream(it) }
                conexion.disconnect()
            } else {
                conexion.disconnect()
                break
            }
        }

        if (resultado != null) cacheLogosNovaTV.put(url, resultado)
        resultado
    } catch (_: Exception) {
        null
    }
}

@Composable
fun LogoCanalNovaTV(
    url: String,
    urlAlternativa: String = "",
    nombre: String,
    modifier: Modifier = Modifier
) {
    val clave = "$url|$urlAlternativa|$nombre"
    var bitmap by remember(clave) {
        mutableStateOf(
            cacheLogosNovaTV.get(url)
                ?: cacheLogosNovaTV.get(urlAlternativa)
        )
    }

    LaunchedEffect(clave) {
        if (bitmap != null) return@LaunchedEffect

        // 1) Logo original de Xtream.
        var descargado = descargarLogoNovaTV(url)

        // 2) Logo de una variante equivalente ya presente en Xtream.
        if (descargado == null && urlAlternativa.isNotBlank() && urlAlternativa != url) {
            descargado = descargarLogoNovaTV(urlAlternativa)
        }

        // 3) Respaldo público de logos IPTV-org, por nombre normalizado.
        if (descargado == null) {
            val respaldo = buscarLogoIptvOrgNovaTV(nombre)
            if (respaldo.isNotBlank()) descargado = descargarLogoNovaTV(respaldo)
        }

        bitmap = descargado
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val logo = bitmap
        if (logo != null) {
            Image(
                bitmap = logo.asImageBitmap(),
                contentDescription = "Logo de $nombre",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            // 4) Último respaldo: iniciales limpias con identidad NovaTV.
            Box(
                modifier = Modifier
                    .height(52.dp)
                    .background(Color(0xFF21172D), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF6F42A8), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = inicialesCanalNovaTV(nombre),
                    color = Color(0xFFC7A4FF),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ReproductorNovaTV(
    usuario: String,
    clave: String,
    canales: List<CanalTV>,
    indiceInicial: Int,
    onSalir: () -> Unit,
    onCanalCambiado: (Int) -> Unit
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var canalActual by remember { mutableIntStateOf(indiceInicial) }
    var barraInfoVisible by remember { mutableStateOf(true) }
    var errorReproduccionTV by remember { mutableStateOf<String?>(null) }
    var epgPlayer by remember { mutableStateOf<EPGCanalNovaTV?>(null) }

    LaunchedEffect(canales.getOrNull(canalActual)?.id) {
        val canal = canales.getOrNull(canalActual)
        epgPlayer = if (canal != null && canal.urlDirectaNovaTV.isBlank()) {
            obtenerEPGNovaTV(usuario, clave, canal.id)
        } else null
    }

    // LibVLC se usa SOLO para TV en vivo. Películas y series siguen usando ExoPlayer.\n    // v9.15.3 Fast Live: caché moderada de 600 ms para acelerar primer canal y zapping\n    // sin forzar decodificación por software ni cambiar el motor que ya es estable.
    val libVLC = remember {
        LibVLC(
            context,
            arrayListOf(
                "--network-caching=400",
                "--live-caching=400",
                "--http-reconnect"
            )
        )
    }

    val mediaPlayerVLC = remember { MediaPlayer(libVLC) }
    val videoLayoutRef = remember { arrayOfNulls<VLCVideoLayout>(1) }
    var reconexionProgramada by remember { mutableStateOf(false) }

    fun recuperarFocoPlayer() {
        videoLayoutRef[0]?.post {
            videoLayoutRef[0]?.requestFocus()
        }
    }

    fun reproducir(indice: Int) {
        if (canales.isEmpty() || indice !in canales.indices) return

        reconexionProgramada = false
        errorReproduccionTV = null
        canalActual = indice
        onCanalCambiado(indice)

        val canal = canales[indice]
        val userPath = Uri.encode(usuario)
        val passPath = Uri.encode(clave)
        val extension = canal.extension.ifBlank { "ts" }
        val url = canal.urlDirectaNovaTV.ifBlank {
            "$XTREAM_BASE_URL_NOVATV/live/" +
                    "$userPath/" +
                    "$passPath/" +
                    "${canal.id}." +
                    extension
        }

        try {
            mediaPlayerVLC.stop()

            val media = Media(libVLC, Uri.parse(url)).apply {
                // Video por hardware cuando esté disponible. VLC conserva sus
                // decodificadores de audio por software para formatos como MP2.
                setHWDecoderEnabled(true, false)
                addOption(":network-caching=400")
                addOption(":live-caching=400")
                addOption(":http-reconnect=true")
                if (canal.urlDirectaNovaTV.isNotBlank()) {
                    addOption(":http-user-agent=Mozilla/5.0 (Linux; Android 10; NovaTV) AppleWebKit/537.36")
                }
            }

            mediaPlayerVLC.media = media
            media.release()
            mediaPlayerVLC.play()

            barraInfoVisible = true
            recuperarFocoPlayer()
        } catch (_: Exception) {
            errorReproduccionTV = "Canal no disponible en este momento"
            barraInfoVisible = true
            recuperarFocoPlayer()
        }
    }

    LaunchedEffect(canalActual) {
        barraInfoVisible = true
        delay(4000)
        barraInfoVisible = false
    }

    DisposableEffect(Unit) {
        val listener = MediaPlayer.EventListener { event ->
            mainHandler.post {
                when (event.type) {
                    MediaPlayer.Event.Playing -> {
                        errorReproduccionTV = null
                    }

                    MediaPlayer.Event.EndReached -> {
                        if (!reconexionProgramada) {
                            reconexionProgramada = true
                            val indiceQueTermino = canalActual
                            videoLayoutRef[0]?.postDelayed({
                                if (canalActual == indiceQueTermino) {
                                    reproducir(indiceQueTermino)
                                } else {
                                    reconexionProgramada = false
                                }
                            }, 500)
                        }
                    }

                    MediaPlayer.Event.EncounteredError -> {
                        errorReproduccionTV =
                            "Canal no disponible en este momento"
                        barraInfoVisible = true
                        recuperarFocoPlayer()
                    }
                }
            }
        }

        mediaPlayerVLC.setEventListener(listener)
        reproducir(canalActual)

        onDispose {
            mediaPlayerVLC.setEventListener(null)
            try {
                mediaPlayerVLC.stop()
            } catch (_: Exception) {
            }
            try {
                mediaPlayerVLC.detachViews()
            } catch (_: Exception) {
            }
            mediaPlayerVLC.release()
            libVLC.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VLCVideoLayout(ctx).apply {
                    videoLayoutRef[0] = this
                    setBackgroundColor(android.graphics.Color.BLACK)

                    isFocusable = true
                    isFocusableInTouchMode = true
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

                    mediaPlayerVLC.attachViews(
                        this,
                        null,
                        false,
                        false
                    )
                    // Igual que el ZOOM que usábamos con ExoPlayer: rellenar la pantalla.
                    mediaPlayerVLC.setVideoScale(MediaPlayer.ScaleType.SURFACE_FILL)

                    requestFocus()
                    post { requestFocus() }

                    setOnFocusChangeListener { view, tieneFoco ->
                        if (!tieneFoco) {
                            view.postDelayed({ view.requestFocus() }, 80)
                        }
                    }

                    setOnClickListener {
                        barraInfoVisible = true
                        postDelayed({ barraInfoVisible = false }, 4000)
                    }

                    setOnKeyListener { _, keyCode, event ->
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            when (keyCode) {
                                KeyEvent.KEYCODE_DPAD_UP,
                                KeyEvent.KEYCODE_CHANNEL_UP -> {
                                    val anterior = if (canalActual <= 0) {
                                        canales.lastIndex
                                    } else {
                                        canalActual - 1
                                    }
                                    reproducir(anterior)
                                    true
                                }

                                KeyEvent.KEYCODE_DPAD_DOWN,
                                KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                                    val siguiente = if (canalActual >= canales.lastIndex) {
                                        0
                                    } else {
                                        canalActual + 1
                                    }
                                    reproducir(siguiente)
                                    true
                                }

                                KeyEvent.KEYCODE_DPAD_CENTER,
                                KeyEvent.KEYCODE_ENTER,
                                KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                    if (errorReproduccionTV != null) {
                                        reproducir(canalActual)
                                    } else {
                                        barraInfoVisible = true
                                        postDelayed({ barraInfoVisible = false }, 4000)
                                    }
                                    true
                                }

                                KeyEvent.KEYCODE_BACK,
                                KeyEvent.KEYCODE_ESCAPE -> {
                                    onSalir()
                                    true
                                }

                                else -> false
                            }
                        } else {
                            false
                        }
                    }
                }
            },
            update = {
                // Conservamos el foco del mando después de recomposiciones.
                if (!it.hasFocus()) it.post { it.requestFocus() }
            }
        )

        if (errorReproduccionTV != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        Color.Black.copy(alpha = 0.78f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = errorReproduccionTV ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "↑/↓ Cambiar canal   •   OK Reintentar   •   Atrás Salir",
                    color = Color.White
                )
            }
        }

        if (barraInfoVisible && canales.isNotEmpty() && canalActual in canales.indices) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.68f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSalir,
                    modifier = Modifier.height(44.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 0.dp
                    )
                ) {
                    Text("← CANALES")
                }

                Spacer(modifier = Modifier.padding(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${canalActual + 1}/${canales.size}  " + canales[canalActual].nombre,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    val epg = epgPlayer
                    if (epg?.ahora != null) {
                        Text(
                            text = "Ahora ${horaEPGNovaTV(epg.ahora.inicio)}–${horaEPGNovaTV(epg.ahora.fin)}  •  ${epg.ahora.titulo}" +
                                    (epg.siguiente?.let { "   |   Siguiente ${horaEPGNovaTV(it.inicio)} • ${it.titulo}" } ?: ""),
                            color = Color(0xFFD8C4FF),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Zapping táctil para teléfonos y tablets. Las flechas aparecen y
        // desaparecen junto con la barra informativa del canal.
        if (barraInfoVisible && canales.isNotEmpty() && canalActual in canales.indices) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        val anterior = if (canalActual <= 0) {
                            canales.lastIndex
                        } else {
                            canalActual - 1
                        }
                        reproducir(anterior)
                    },
                    modifier = Modifier.height(50.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 18.dp,
                        vertical = 0.dp
                    ),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.Black.copy(alpha = 0.58f),
                        contentColor = Color.White
                    )
                ) {
                    Text("▲", style = MaterialTheme.typography.titleLarge)
                }

                Button(
                    onClick = {
                        val siguiente = if (canalActual >= canales.lastIndex) {
                            0
                        } else {
                            canalActual + 1
                        }
                        reproducir(siguiente)
                    },
                    modifier = Modifier.height(50.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 18.dp,
                        vertical = 0.dp
                    ),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.Black.copy(alpha = 0.58f),
                        contentColor = Color.White
                    )
                ) {
                    Text("▼", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

fun validarLogin(
    usuario: String,
    clave: String,
    onCorrecto: (CuentaInfo) -> Unit,
    onError: (String) -> Unit
) {

    Thread {

        try {

            val userEncoded =
                URLEncoder.encode(
                    usuario,
                    "UTF-8"
                )

            val passEncoded =
                URLEncoder.encode(
                    clave,
                    "UTF-8"
                )

            val apiUrl =
                "$XTREAM_BASE_URL_NOVATV/player_api.php" +
                        "?username=$userEncoded" +
                        "&password=$passEncoded"

            val conexion =
                URL(apiUrl)
                    .openConnection()
                        as HttpURLConnection

            conexion.connectTimeout = 15000
            conexion.readTimeout = 15000
            conexion.requestMethod = "GET"
            conexion.instanceFollowRedirects = true

            val respuesta =
                conexion.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            conexion.disconnect()

            val json =
                JSONObject(respuesta)

            val userInfo =
                json.optJSONObject(
                    "user_info"
                )

            val auth =
                userInfo?.optString(
                    "auth",
                    "0"
                )

            if (auth == "1") {

                val cuenta = CuentaInfo(
                    nombre = userInfo.optString("username", usuario),
                    estado = userInfo.optString("status", ""),
                    vencimientoEpoch = userInfo.optString("exp_date", "0").toLongOrNull() ?: 0L,
                    conexionesActivas = userInfo.optString("active_cons", "0").toIntOrNull() ?: 0,
                    conexionesMaximas = userInfo.optString("max_connections", "0").toIntOrNull() ?: 0
                )
                onCorrecto(cuenta)

            } else {

                onError(
                    "Usuario o contraseña incorrectos"
                )
            }

        } catch (e: Exception) {

            onError(
                "Error de conexión: ${e.message}"
            )
        }

    }.start()
}

fun buscarTodoNovaTV(
    usuario: String,
    clave: String,
    texto: String,
    onResultado: (List<ResultadoBusqueda>) -> Unit,
    onError: (String) -> Unit
) {
    Thread {
        try {
            val u = URLEncoder.encode(usuario, "UTF-8")
            val p = URLEncoder.encode(clave, "UTF-8")
            val base = "$XTREAM_BASE_URL_NOVATV/player_api.php?username=$u&password=$p"
            val consulta = texto.lowercase(Locale.getDefault())
            val resultados = mutableListOf<ResultadoBusqueda>()

            fun leerArray(action: String): JSONArray {
                val c = URL("$base&action=$action").openConnection() as HttpURLConnection
                c.connectTimeout = 15000
                c.readTimeout = 30000
                c.requestMethod = "GET"
                c.instanceFollowRedirects = true
                val respuesta = c.inputStream.bufferedReader().use { it.readText() }
                c.disconnect()
                return JSONArray(respuesta)
            }

            val tv = leerArray("get_live_streams")
            for (i in 0 until tv.length()) {
                val o = tv.getJSONObject(i)
                val nombre = o.optString("name", "Sin nombre")
                if (nombre.lowercase(Locale.getDefault()).contains(consulta)) {
                    resultados.add(
                        ResultadoBusqueda(
                            "TV",
                            o.optString("stream_id"),
                            nombre,
                            o.optString("container_extension", "ts")
                        )
                    )
                }
            }

            val vod = leerArray("get_vod_streams")
            for (i in 0 until vod.length()) {
                val o = vod.getJSONObject(i)
                val nombre = o.optString("name", "Sin nombre")
                if (nombre.lowercase(Locale.getDefault()).contains(consulta)) {
                    resultados.add(
                        ResultadoBusqueda(
                            "PELICULA",
                            o.optString("stream_id"),
                            nombre,
                            o.optString("container_extension", "mp4")
                        )
                    )
                }
            }

            val ser = leerArray("get_series")
            for (i in 0 until ser.length()) {
                val o = ser.getJSONObject(i)
                val nombre = o.optString("name", "Sin nombre")
                if (nombre.lowercase(Locale.getDefault()).contains(consulta)) {
                    resultados.add(
                        ResultadoBusqueda(
                            "SERIE",
                            o.optString("series_id"),
                            nombre
                        )
                    )
                }
            }

            onResultado(resultados.sortedBy { it.nombre.lowercase(Locale.getDefault()) }.take(300))
        } catch (e: Exception) {
            onError("Error en búsqueda: ${e.message}")
        }
    }.start()
}

fun actualizarCuenta(
    usuario: String,
    clave: String,
    onResultado: (CuentaInfo) -> Unit,
    onError: (String) -> Unit
) {
    validarLogin(
        usuario = usuario,
        clave = clave,
        onCorrecto = onResultado,
        onError = onError
    )
}

fun cargarCategorias(
    usuario: String,
    clave: String,
    onResultado: (List<CategoriaTV>) -> Unit,
    onError: (String) -> Unit
) {

    Thread {

        try {

            val userEncoded =
                URLEncoder.encode(
                    usuario,
                    "UTF-8"
                )

            val passEncoded =
                URLEncoder.encode(
                    clave,
                    "UTF-8"
                )

            val apiUrl =
                "$XTREAM_BASE_URL_NOVATV/player_api.php" +
                        "?username=$userEncoded" +
                        "&password=$passEncoded" +
                        "&action=get_live_categories"

            val conexion =
                URL(apiUrl)
                    .openConnection()
                        as HttpURLConnection

            conexion.connectTimeout = 15000
            conexion.readTimeout = 15000
            conexion.requestMethod = "GET"
            conexion.instanceFollowRedirects = true

            val respuesta =
                conexion.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            conexion.disconnect()

            val array =
                JSONArray(respuesta)

            val lista =
                mutableListOf<CategoriaTV>()

            for (
            i in 0 until array.length()
            ) {

                val item =
                    array.getJSONObject(i)

                val id =
                    item.optString(
                        "category_id"
                    )

                val nombre =
                    item.optString(
                        "category_name",
                        "Sin nombre"
                    )

                if (id.isNotBlank()) {

                    lista.add(
                        CategoriaTV(
                            id = id,
                            nombre = nombre
                        )
                    )
                }
            }

            onResultado(lista)

        } catch (e: Exception) {

            onError(
                "Error cargando categorías: ${e.message}"
            )
        }

    }.start()
}

private fun decodificarTextoEPGNovaTV(valor: String): String {
    if (valor.isBlank()) return ""
    return try {
        val bytes = android.util.Base64.decode(valor, android.util.Base64.DEFAULT)
        val texto = String(bytes, Charsets.UTF_8).trim()
        if (texto.isNotBlank()) texto else valor
    } catch (_: Exception) {
        valor
    }
}

private fun epochEPGNovaTV(item: JSONObject, campoTimestamp: String, campoTexto: String): Long {
    val directo = item.optLong(campoTimestamp, 0L)
    if (directo > 0L) return directo
    val texto = item.optString(campoTexto, "")
    if (texto.isBlank()) return 0L
    return try {
        val formato = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        (formato.parse(texto)?.time ?: 0L) / 1000L
    } catch (_: Exception) { 0L }
}

fun horaEPGNovaTV(epochSegundos: Long): String {
    if (epochSegundos <= 0L) return "--:--"
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochSegundos * 1000L))
}

suspend fun obtenerEPGNovaTV(usuario: String, clave: String, streamId: String): EPGCanalNovaTV? =
    withContext(Dispatchers.IO) {
        try {
            val u = URLEncoder.encode(usuario, "UTF-8")
            val p = URLEncoder.encode(clave, "UTF-8")
            val id = URLEncoder.encode(streamId, "UTF-8")
            val url = "$XTREAM_BASE_URL_NOVATV/player_api.php?username=$u&password=$p&action=get_short_epg&stream_id=$id&limit=4"
            val conexion = URL(url).openConnection() as HttpURLConnection
            conexion.connectTimeout = 8000
            conexion.readTimeout = 8000
            conexion.instanceFollowRedirects = true
            conexion.setRequestProperty("User-Agent", "NovaTV")
            val texto = conexion.inputStream.bufferedReader().use { it.readText() }
            conexion.disconnect()
            val raiz = JSONObject(texto)
            val listings = raiz.optJSONArray("epg_listings") ?: JSONArray()
            val programas = mutableListOf<ProgramaEPGNovaTV>()
            for (i in 0 until listings.length()) {
                val item = listings.optJSONObject(i) ?: continue
                val titulo = decodificarTextoEPGNovaTV(item.optString("title", "")).ifBlank { "Sin título" }
                val inicio = epochEPGNovaTV(item, "start_timestamp", "start")
                val fin = epochEPGNovaTV(item, "stop_timestamp", "end")
                programas += ProgramaEPGNovaTV(titulo, inicio, fin)
            }
            val ahoraEpoch = System.currentTimeMillis() / 1000L
            val indiceAhora = programas.indexOfFirst { it.inicio <= ahoraEpoch && (it.fin == 0L || it.fin > ahoraEpoch) }
            val ahora = if (indiceAhora >= 0) programas[indiceAhora] else programas.firstOrNull { it.fin > ahoraEpoch } ?: programas.firstOrNull()
            val siguiente = if (indiceAhora >= 0) programas.getOrNull(indiceAhora + 1) else programas.dropWhile { it != ahora }.drop(1).firstOrNull()
            EPGCanalNovaTV(ahora, siguiente)
        } catch (_: Exception) {
            null
        }
    }

fun cargarCanales(
    usuario: String,
    clave: String,
    categoriaId: String,
    onResultado: (List<CanalTV>) -> Unit,
    onError: (String) -> Unit
) {

    Thread {

        try {

            val userEncoded =
                URLEncoder.encode(
                    usuario,
                    "UTF-8"
                )

            val passEncoded =
                URLEncoder.encode(
                    clave,
                    "UTF-8"
                )

            val categoryEncoded =
                URLEncoder.encode(
                    categoriaId,
                    "UTF-8"
                )

            val apiUrl =
                "$XTREAM_BASE_URL_NOVATV/player_api.php" +
                        "?username=$userEncoded" +
                        "&password=$passEncoded" +
                        "&action=get_live_streams" +
                        "&category_id=$categoryEncoded"

            val conexion =
                URL(apiUrl)
                    .openConnection()
                        as HttpURLConnection

            conexion.connectTimeout = 15000
            conexion.readTimeout = 15000
            conexion.requestMethod = "GET"
            conexion.instanceFollowRedirects = true

            val respuesta =
                conexion.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            conexion.disconnect()

            val array =
                JSONArray(respuesta)

            val lista =
                mutableListOf<CanalTV>()

            for (
            i in 0 until array.length()
            ) {

                val item =
                    array.getJSONObject(i)

                val id =
                    item.optString(
                        "stream_id"
                    )

                val nombre =
                    item.optString(
                        "name",
                        "Canal"
                    )

                val extension =
                    item.optString(
                        "container_extension",
                        "ts"
                    )

                val logo =
                    item.optString(
                        "stream_icon",
                        ""
                    )

                if (id.isNotBlank()) {

                    lista.add(
                        CanalTV(
                            id = id,
                            nombre = nombre,
                            extension = extension,
                            logo = logo
                        )
                    )
                }
            }

            onResultado(lista)

        } catch (e: Exception) {

            onError(
                "Error cargando canales: ${e.message}"
            )
        }

    }.start()
}

fun cargarCategoriasPeliculas(
    usuario: String,
    clave: String,
    onResultado: (List<CategoriaPelicula>) -> Unit,
    onError: (String) -> Unit
) {
    Thread {
        try {
            val u=URLEncoder.encode(usuario,"UTF-8")
            val p=URLEncoder.encode(clave,"UTF-8")
            val url="$XTREAM_BASE_URL_NOVATV/player_api.php?username=$u&password=$p&action=get_vod_categories"
            val c=URL(url).openConnection() as HttpURLConnection
            c.connectTimeout=15000; c.readTimeout=15000; c.requestMethod="GET"; c.instanceFollowRedirects=true
            val respuesta=c.inputStream.bufferedReader().use { it.readText() }
            c.disconnect()
            val json=JSONArray(respuesta)
            val lista=mutableListOf<CategoriaPelicula>()
            for(i in 0 until json.length()) {
                val o=json.getJSONObject(i)
                lista.add(CategoriaPelicula(o.optString("category_id"), o.optString("category_name","Sin nombre")))
            }
            onResultado(lista)
        } catch(e:Exception) { onError("Error cargando categorías de películas: ${e.message}") }
    }.start()
}

fun cargarPeliculas(
    usuario: String,
    clave: String,
    categoriaId: String,
    onResultado: (List<Pelicula>) -> Unit,
    onError: (String) -> Unit
) {
    Thread {
        try {
            val u=URLEncoder.encode(usuario,"UTF-8")
            val p=URLEncoder.encode(clave,"UTF-8")
            val cat=URLEncoder.encode(categoriaId,"UTF-8")
            val url="$XTREAM_BASE_URL_NOVATV/player_api.php?username=$u&password=$p&action=get_vod_streams&category_id=$cat"
            val c=URL(url).openConnection() as HttpURLConnection
            c.connectTimeout=15000; c.readTimeout=15000; c.requestMethod="GET"; c.instanceFollowRedirects=true
            val respuesta=c.inputStream.bufferedReader().use { it.readText() }
            c.disconnect()
            val json=JSONArray(respuesta)
            val lista=mutableListOf<Pelicula>()
            for(i in 0 until json.length()) {
                val o=json.getJSONObject(i)
                lista.add(
                    Pelicula(
                        id = o.optString("stream_id"),
                        nombre = o.optString("name", "Sin nombre"),
                        extension = o.optString("container_extension", "mp4"),
                        portada = o.optString("stream_icon", ""),
                        agregado = o.optString("added", "0").toLongOrNull() ?: 0L,
                        categoriaId = o.optString("category_id", categoriaId)
                    )
                )
            }
            onResultado(lista)
        } catch(e:Exception) { onError("Error cargando películas: ${e.message}") }
    }.start()
}



private fun tmdbActivoNovaTV(): Boolean =
    TMDB_BEARER_TOKEN_NOVATV.trim().isNotBlank()

private fun extraerAnioNovaTV(titulo: String): String {
    return Regex("""\b(19|20)\d{2}\b""")
        .find(titulo)
        ?.value
        .orEmpty()
}

private fun limpiarTituloParaTmdbNovaTV(tituloOriginal: String): String {
    var titulo = tituloOriginal.trim()

    // Quita prefijos habituales del panel: ARG:, LAT:, ES:, MX:, etc.
    titulo = titulo.replace(
        Regex("""^(ARG|LAT|LATINO|ES|ESP|MX|MEX|US|USA|UK|BR|BRA|CL|CHI|PE|PER|CO|COL)\s*[:\-|]\s*""", RegexOption.IGNORE_CASE),
        ""
    )

    // Quita rangos/años entre paréntesis o corchetes.
    titulo = titulo.replace(Regex("""[\(\[]\s*(19|20)\d{2}(?:\s*[-–]\s*(19|20)\d{2})?\s*[\)\]]"""), " ")

    // Quita etiquetas de calidad/idioma que suelen romper las búsquedas.
    titulo = titulo.replace(
        Regex("""\b(2160P|1080P|720P|4K|UHD|FHD|FULL\s*HD|HD|SD|WEB[- ]?DL|WEBRIP|BLURAY|BRRIP|DVDRIP|HDR|HDR10|DOLBY|ATMOS|LATINO|CASTELLANO|CAST|DUAL|SUB|SUBTITULADO|ESP|SPA)\b""", RegexOption.IGNORE_CASE),
        " "
    )

    // Quita un año suelto al final.
    titulo = titulo.replace(Regex("""\b(19|20)\d{2}\b"""), " ")

    // Limpieza final.
    titulo = titulo
        .replace(Regex("""[_|]+"""), " ")
        .replace(Regex("""\s{2,}"""), " ")
        .trim(' ', '-', ':', '.', '_')

    return titulo.ifBlank { tituloOriginal.trim() }
}

private fun pedirJsonTmdbNovaTV(endpoint: String): JSONObject? {
    if (!tmdbActivoNovaTV()) return null

    val conexion = URL("https://api.themoviedb.org/3$endpoint")
        .openConnection() as HttpURLConnection

    return try {
        conexion.connectTimeout = 12000
        conexion.readTimeout = 12000
        conexion.instanceFollowRedirects = true
        conexion.setRequestProperty(
            "Authorization",
            "Bearer ${TMDB_BEARER_TOKEN_NOVATV.trim()}"
        )
        conexion.setRequestProperty("accept", "application/json")

        if (conexion.responseCode !in 200..299) return null

        JSONObject(
            conexion.inputStream.bufferedReader().use { it.readText() }
        )
    } catch (_: Exception) {
        null
    } finally {
        conexion.disconnect()
    }
}

private fun urlImagenTmdbNovaTV(path: String, tamano: String): String {
    if (path.isBlank() || path == "null") return ""
    if (path.startsWith("http://") || path.startsWith("https://")) return path
    return "$TMDB_IMAGEN_BASE_NOVATV$tamano$path"
}


data class BannerInicioNovaTV(
    val titulo: String,
    val fondo: String,
    val subtitulo: String = ""
)

private fun cargarBannersInicioTmdbNovaTV(): List<BannerInicioNovaTV> {
    if (!tmdbActivoNovaTV()) return emptyList()

    return try {
        val lang = URLEncoder.encode(TMDB_IDIOMA_NOVATV, "UTF-8")

        // Estrenos actuales en cines; si TMDb no responde, probamos populares.
        val root = pedirJsonTmdbNovaTV(
            "/movie/now_playing?language=$lang&page=1&region=AR"
        ) ?: pedirJsonTmdbNovaTV(
            "/movie/popular?language=$lang&page=1"
        ) ?: return emptyList()

        val resultados = root.optJSONArray("results") ?: JSONArray()
        val banners = mutableListOf<BannerInicioNovaTV>()

        for (i in 0 until resultados.length()) {
            val item = resultados.optJSONObject(i) ?: continue
            val backdrop = item.optString("backdrop_path", "")
            if (backdrop.isBlank() || backdrop == "null") continue

            val titulo = item.optString("title", item.optString("original_title", "")).trim()
            if (titulo.isBlank()) continue

            val fecha = item.optString("release_date", "")
            val anio = fecha.take(4).takeIf { it.length == 4 }.orEmpty()

            banners += BannerInicioNovaTV(
                titulo = titulo,
                fondo = urlImagenTmdbNovaTV(backdrop, "w1280"),
                subtitulo = if (anio.isNotBlank()) "ESTRENO · $anio" else "ESTRENO"
            )

            // No necesitamos descargar una lista enorme para el Inicio.
            if (banners.size >= 8) break
        }

        banners
    } catch (_: Exception) {
        emptyList()
    }
}

private fun elegirResultadoTmdbNovaTV(
    resultados: JSONArray,
    anioBuscado: String,
    campoFecha: String
): JSONObject? {
    if (resultados.length() == 0) return null

    if (anioBuscado.isNotBlank()) {
        for (i in 0 until resultados.length()) {
            val item = resultados.optJSONObject(i) ?: continue
            val anio = item.optString(campoFecha, "").take(4)
            if (anio == anioBuscado) return item
        }
    }

    return resultados.optJSONObject(0)
}

private fun respaldoPeliculaTmdbNovaTV(
    pelicula: Pelicula,
    actual: DetallePelicula
): DetallePelicula {
    if (!tmdbActivoNovaTV()) return actual

    return try {
        val tituloBusqueda = limpiarTituloParaTmdbNovaTV(pelicula.nombre)
        val anioBuscado = extraerAnioNovaTV(pelicula.nombre)
        val query = URLEncoder.encode(tituloBusqueda, "UTF-8")
        val lang = URLEncoder.encode(TMDB_IDIOMA_NOVATV, "UTF-8")

        val busqueda = pedirJsonTmdbNovaTV(
            "/search/movie?query=$query&language=$lang&include_adult=false&page=1"
        ) ?: return actual

        val elegido = elegirResultadoTmdbNovaTV(
            busqueda.optJSONArray("results") ?: JSONArray(),
            anioBuscado,
            "release_date"
        ) ?: return actual

        val tmdbId = elegido.optInt("id", 0)
        if (tmdbId <= 0) return actual

        val detalleTmdb = pedirJsonTmdbNovaTV(
            "/movie/$tmdbId?language=$lang"
        ) ?: elegido

        val generos = detalleTmdb.optJSONArray("genres")
        val generoTmdb = buildList {
            if (generos != null) {
                for (i in 0 until generos.length()) {
                    generos.optJSONObject(i)?.optString("name")
                        ?.takeIf { it.isNotBlank() }
                        ?.let { add(it) }
                }
            }
        }.joinToString(", ")

        val runtime = detalleTmdb.optInt("runtime", 0)
        val duracionTmdb = if (runtime > 0) "${runtime} min" else ""

        val ratingNum = detalleTmdb.optDouble("vote_average", 0.0)
        val ratingTmdb = if (ratingNum > 0.0) {
            String.format(Locale.US, "%.1f", ratingNum)
        } else ""

        val release = detalleTmdb.optString(
            "release_date",
            elegido.optString("release_date", "")
        )

        actual.copy(
            descripcion = actual.descripcion.ifBlank {
                detalleTmdb.optString("overview", elegido.optString("overview", ""))
            },
            portada = actual.portada.ifBlank {
                urlImagenTmdbNovaTV(
                    detalleTmdb.optString("poster_path", elegido.optString("poster_path", "")),
                    "w780"
                )
            },
            fondo = actual.fondo.ifBlank {
                urlImagenTmdbNovaTV(
                    detalleTmdb.optString("backdrop_path", elegido.optString("backdrop_path", "")),
                    "w1280"
                )
            },
            anio = actual.anio.ifBlank {
                if (release.length >= 4) release.take(4) else release
            },
            duracion = actual.duracion.ifBlank { duracionTmdb },
            genero = actual.genero.ifBlank { generoTmdb },
            rating = actual.rating.ifBlank { ratingTmdb }
        )
    } catch (_: Exception) {
        actual
    }
}


private fun detalleSeriePrivadaNovaTV(serie: Serie): DetalleSerie {
    var detalle = DetalleSerie(
        descripcion = serie.descripcionNovaTV,
        portada = serie.portada,
        fondo = serie.fondoNovaTV
    )

    if (!tmdbActivoNovaTV()) return detalle

    return try {
        val tmdbId = serie.tmdbIdNovaTV
        if (tmdbId != null && tmdbId > 0) {
            val lang = URLEncoder.encode(TMDB_IDIOMA_NOVATV, "UTF-8")
            val tmdb = pedirJsonTmdbNovaTV("/tv/$tmdbId?language=$lang")
                ?: return detalle

            val generos = tmdb.optJSONArray("genres")
            val genero = buildList {
                if (generos != null) {
                    for (i in 0 until generos.length()) {
                        generos.optJSONObject(i)?.optString("name")
                            ?.takeIf { it.isNotBlank() }
                            ?.let { add(it) }
                    }
                }
            }.joinToString(", ")

            val ratingNum = tmdb.optDouble("vote_average", 0.0)
            val fecha = tmdb.optString("first_air_date", "")

            detalle.copy(
                descripcion = detalle.descripcion.ifBlank {
                    tmdb.optString("overview", "")
                },
                portada = detalle.portada.ifBlank {
                    urlImagenTmdbNovaTV(tmdb.optString("poster_path", ""), "w780")
                },
                fondo = detalle.fondo.ifBlank {
                    urlImagenTmdbNovaTV(tmdb.optString("backdrop_path", ""), "w1280")
                },
                anio = if (fecha.length >= 4) fecha.take(4) else fecha,
                genero = genero,
                rating = if (ratingNum > 0.0) {
                    String.format(Locale.US, "%.1f", ratingNum)
                } else ""
            )
        } else {
            // Si no hay ID exacto, conservar el respaldo por título ya existente.
            respaldoSerieTmdbNovaTV(serie, detalle)
        }
    } catch (_: Exception) {
        detalle
    }
}

private fun respaldoSerieTmdbNovaTV(
    serie: Serie,
    actual: DetalleSerie
): DetalleSerie {
    if (!tmdbActivoNovaTV()) return actual

    return try {
        val tituloBusqueda = limpiarTituloParaTmdbNovaTV(serie.nombre)
        val anioBuscado = extraerAnioNovaTV(serie.nombre)
        val query = URLEncoder.encode(tituloBusqueda, "UTF-8")
        val lang = URLEncoder.encode(TMDB_IDIOMA_NOVATV, "UTF-8")

        val busqueda = pedirJsonTmdbNovaTV(
            "/search/tv?query=$query&language=$lang&include_adult=false&page=1"
        ) ?: return actual

        val elegido = elegirResultadoTmdbNovaTV(
            busqueda.optJSONArray("results") ?: JSONArray(),
            anioBuscado,
            "first_air_date"
        ) ?: return actual

        val tmdbId = elegido.optInt("id", 0)
        if (tmdbId <= 0) return actual

        val detalleTmdb = pedirJsonTmdbNovaTV(
            "/tv/$tmdbId?language=$lang"
        ) ?: elegido

        val generos = detalleTmdb.optJSONArray("genres")
        val generoTmdb = buildList {
            if (generos != null) {
                for (i in 0 until generos.length()) {
                    generos.optJSONObject(i)?.optString("name")
                        ?.takeIf { it.isNotBlank() }
                        ?.let { add(it) }
                }
            }
        }.joinToString(", ")

        val ratingNum = detalleTmdb.optDouble("vote_average", 0.0)
        val ratingTmdb = if (ratingNum > 0.0) {
            String.format(Locale.US, "%.1f", ratingNum)
        } else ""

        val firstAirDate = detalleTmdb.optString(
            "first_air_date",
            elegido.optString("first_air_date", "")
        )

        actual.copy(
            descripcion = actual.descripcion.ifBlank {
                detalleTmdb.optString("overview", elegido.optString("overview", ""))
            },
            portada = actual.portada.ifBlank {
                urlImagenTmdbNovaTV(
                    detalleTmdb.optString("poster_path", elegido.optString("poster_path", "")),
                    "w780"
                )
            },
            fondo = actual.fondo.ifBlank {
                urlImagenTmdbNovaTV(
                    detalleTmdb.optString("backdrop_path", elegido.optString("backdrop_path", "")),
                    "w1280"
                )
            },
            anio = actual.anio.ifBlank {
                if (firstAirDate.length >= 4) firstAirDate.take(4) else firstAirDate
            },
            genero = actual.genero.ifBlank { generoTmdb },
            rating = actual.rating.ifBlank { ratingTmdb }
        )
    } catch (_: Exception) {
        actual
    }
}


fun cargarCatalogoPeliculasRecomendacionNovaTV(usuario:String, clave:String, onResultado:(List<Pelicula>)->Unit) {
    Thread { try {
        val u=URLEncoder.encode(usuario,"UTF-8"); val p=URLEncoder.encode(clave,"UTF-8")
        val c=URL("$XTREAM_BASE_URL_NOVATV/player_api.php?username=$u&password=$p&action=get_vod_streams").openConnection() as HttpURLConnection
        c.connectTimeout=15000; c.readTimeout=25000; c.instanceFollowRedirects=true
        val a=JSONArray(c.inputStream.bufferedReader().use{it.readText()}); c.disconnect()
        val l=mutableListOf<Pelicula>()
        for(i in 0 until a.length()){ val o=a.optJSONObject(i)?:continue; l.add(Pelicula(o.optString("stream_id"),o.optString("name","Sin nombre"),o.optString("container_extension","mp4"),o.optString("stream_icon",""),o.optString("added","0").toLongOrNull() ?: 0L,o.optString("category_id",""))) }
        onResultado(l.distinctBy{it.id})
    } catch(_:Exception){ onResultado(emptyList()) } }.start()
}

fun cargarCatalogoSeriesRecomendacionNovaTV(usuario:String, clave:String, onResultado:(List<Serie>)->Unit) {
    Thread { try {
        val u=URLEncoder.encode(usuario,"UTF-8"); val p=URLEncoder.encode(clave,"UTF-8")
        val c=URL("$XTREAM_BASE_URL_NOVATV/player_api.php?username=$u&password=$p&action=get_series").openConnection() as HttpURLConnection
        c.connectTimeout=15000; c.readTimeout=25000; c.instanceFollowRedirects=true
        val a=JSONArray(c.inputStream.bufferedReader().use{it.readText()}); c.disconnect()
        val l=mutableListOf<Serie>()
        for(i in 0 until a.length()){ val o=a.optJSONObject(i)?:continue; l.add(Serie(o.optString("series_id"),o.optString("name","Sin nombre"),o.optString("cover",o.optString("stream_icon","")),o.optString("last_modified",o.optString("added","0")).toLongOrNull() ?: 0L,o.optString("category_id",""))) }
        onResultado(l.distinctBy{it.id})
    } catch(_:Exception){ onResultado(emptyList()) } }.start()
}

// ============================================================
// v9.13 - "También te puede gustar"
// TMDb decide similitud; NovaTV SOLO muestra coincidencias existentes
// en la lista Xtream que el usuario está navegando.
// ============================================================
private fun claveTituloRecomendacionNovaTV(titulo: String): String =
    limpiarTituloParaTmdbNovaTV(titulo)
        .lowercase(Locale.getDefault())
        .replace(Regex("[^a-z0-9áéíóúüñ ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

fun cargarRecomendacionesPeliculasNovaTV(
    pelicula: Pelicula,
    catalogo: List<Pelicula>,
    onResultado: (List<Pelicula>) -> Unit
) {
    Thread {
        try {
            if (!tmdbActivoNovaTV() || catalogo.isEmpty()) { onResultado(emptyList()); return@Thread }
            val lang = URLEncoder.encode(TMDB_IDIOMA_NOVATV, "UTF-8")
            val q = URLEncoder.encode(limpiarTituloParaTmdbNovaTV(pelicula.nombre), "UTF-8")
            val busqueda = pedirJsonTmdbNovaTV("/search/movie?query=$q&language=$lang&include_adult=false&page=1")
                ?: run { onResultado(emptyList()); return@Thread }
            val elegido = elegirResultadoTmdbNovaTV(busqueda.optJSONArray("results") ?: JSONArray(), extraerAnioNovaTV(pelicula.nombre), "release_date")
                ?: run { onResultado(emptyList()); return@Thread }
            val id = elegido.optInt("id", 0)
            if (id <= 0) { onResultado(emptyList()); return@Thread }
            val root = pedirJsonTmdbNovaTV("/movie/$id/recommendations?language=$lang&page=1")
                ?: pedirJsonTmdbNovaTV("/movie/$id/similar?language=$lang&page=1")
            val arr = root?.optJSONArray("results") ?: JSONArray()
            val mapa = catalogo.filter { it.id != pelicula.id }.groupBy { claveTituloRecomendacionNovaTV(it.nombre) }
            val salida = mutableListOf<Pelicula>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val candidatos = listOf(o.optString("title"), o.optString("original_title"))
                var encontrado: Pelicula? = null
                for (t in candidatos) {
                    val k = claveTituloRecomendacionNovaTV(t)
                    if (k.isNotBlank()) { encontrado = mapa[k]?.firstOrNull(); if (encontrado != null) break }
                }
                if (encontrado != null && salida.none { it.id == encontrado.id }) salida.add(encontrado)
                if (salida.size >= 12) break
            }
            onResultado(salida)
        } catch (_: Exception) { onResultado(emptyList()) }
    }.start()
}

fun cargarRecomendacionesSeriesNovaTV(
    serie: Serie,
    catalogo: List<Serie>,
    onResultado: (List<Serie>) -> Unit
) {
    Thread {
        try {
            if (!tmdbActivoNovaTV() || catalogo.isEmpty()) { onResultado(emptyList()); return@Thread }
            val lang = URLEncoder.encode(TMDB_IDIOMA_NOVATV, "UTF-8")
            val q = URLEncoder.encode(limpiarTituloParaTmdbNovaTV(serie.nombre), "UTF-8")
            val busqueda = pedirJsonTmdbNovaTV("/search/tv?query=$q&language=$lang&include_adult=false&page=1")
                ?: run { onResultado(emptyList()); return@Thread }
            val elegido = elegirResultadoTmdbNovaTV(busqueda.optJSONArray("results") ?: JSONArray(), extraerAnioNovaTV(serie.nombre), "first_air_date")
                ?: run { onResultado(emptyList()); return@Thread }
            val id = elegido.optInt("id", 0)
            if (id <= 0) { onResultado(emptyList()); return@Thread }
            val root = pedirJsonTmdbNovaTV("/tv/$id/similar?language=$lang&page=1")
            val arr = root?.optJSONArray("results") ?: JSONArray()
            val mapa = catalogo.filter { it.id != serie.id }.groupBy { claveTituloRecomendacionNovaTV(it.nombre) }
            val salida = mutableListOf<Serie>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val candidatos = listOf(o.optString("name"), o.optString("original_name"))
                var encontrado: Serie? = null
                for (t in candidatos) {
                    val k = claveTituloRecomendacionNovaTV(t)
                    if (k.isNotBlank()) { encontrado = mapa[k]?.firstOrNull(); if (encontrado != null) break }
                }
                if (encontrado != null && salida.none { it.id == encontrado.id }) salida.add(encontrado)
                if (salida.size >= 12) break
            }
            onResultado(salida)
        } catch (_: Exception) { onResultado(emptyList()) }
    }.start()
}

fun cargarDetallePelicula(
    usuario: String,
    clave: String,
    pelicula: Pelicula,
    onResultado: (DetallePelicula) -> Unit,
    onError: (String) -> Unit
) {
    Thread {
        try {
            if (pelicula.urlDirectaNovaTV.isNotBlank()) {
                var detalle = DetallePelicula(
                    descripcion = pelicula.descripcionNovaTV,
                    portada = pelicula.portada,
                    fondo = pelicula.fondoNovaTV
                )

                val tmdbId = pelicula.tmdbIdNovaTV
                if (tmdbId != null && tmdbId > 0 && tmdbActivoNovaTV()) {
                    val lang = URLEncoder.encode(TMDB_IDIOMA_NOVATV, "UTF-8")
                    val tmdb = pedirJsonTmdbNovaTV("/movie/$tmdbId?language=$lang")
                    if (tmdb != null) {
                        val generos = tmdb.optJSONArray("genres")
                        val genero = buildList {
                            if (generos != null) for (i in 0 until generos.length()) {
                                generos.optJSONObject(i)?.optString("name")
                                    ?.takeIf { it.isNotBlank() }?.let { add(it) }
                            }
                        }.joinToString(", ")
                        val runtime = tmdb.optInt("runtime", 0)
                        val rating = tmdb.optDouble("vote_average", 0.0)
                        val fecha = tmdb.optString("release_date", "")
                        detalle = detalle.copy(
                            descripcion = detalle.descripcion.ifBlank { tmdb.optString("overview", "") },
                            portada = detalle.portada.ifBlank { urlImagenTmdbNovaTV(tmdb.optString("poster_path", ""), "w780") },
                            fondo = detalle.fondo.ifBlank { urlImagenTmdbNovaTV(tmdb.optString("backdrop_path", ""), "w1280") },
                            anio = if (fecha.length >= 4) fecha.take(4) else fecha,
                            duracion = if (runtime > 0) "$runtime min" else "",
                            genero = genero,
                            rating = if (rating > 0.0) String.format(Locale.US, "%.1f", rating) else ""
                        )
                    }
                } else if (tmdbActivoNovaTV()) {
                    detalle = respaldoPeliculaTmdbNovaTV(pelicula, detalle)
                }

                onResultado(detalle)
                return@Thread
            }
            val u = URLEncoder.encode(usuario, "UTF-8")
            val p = URLEncoder.encode(clave, "UTF-8")
            val id = URLEncoder.encode(pelicula.id, "UTF-8")
            val c = URL("$XTREAM_BASE_URL_NOVATV/player_api.php?username=$u&password=$p&action=get_vod_info&vod_id=$id")
                .openConnection() as HttpURLConnection
            c.connectTimeout = 15000
            c.readTimeout = 15000
            c.instanceFollowRedirects = true
            val root = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            c.disconnect()

            val info = root.optJSONObject("info") ?: JSONObject()
            val movieData = root.optJSONObject("movie_data") ?: JSONObject()
            val backdrop = info.optJSONArray("backdrop_path")?.optString(0)
                ?: info.optString("backdrop_path", "")

            val detalleXtream = DetallePelicula(
                descripcion = info.optString("plot", info.optString("description", "")),
                portada = info.optString(
                    "movie_image",
                    info.optString("cover_big", pelicula.portada)
                ).ifBlank { pelicula.portada },
                fondo = backdrop,
                anio = info.optString("releasedate", info.optString("year", "")),
                duracion = info.optString("duration", info.optString("duration_secs", "")),
                genero = info.optString("genre", ""),
                rating = info.optString("rating", movieData.optString("rating", ""))
            )

            // Xtream siempre tiene prioridad. TMDb solo completa campos vacíos.
            onResultado(
                respaldoPeliculaTmdbNovaTV(
                    pelicula = pelicula,
                    actual = detalleXtream
                )
            )
        } catch (e: Exception) {
            onError("Error cargando información de la película: ${e.message}")
        }
    }.start()
}

fun cargarDetalleSerieCompleto(
    usuario: String,
    clave: String,
    serie: Serie,
    onResultado: (DetalleSerie, List<Episodio>) -> Unit,
    onError: (String) -> Unit
) {
    Thread {
        try {
            val u = URLEncoder.encode(usuario, "UTF-8")
            val p = URLEncoder.encode(clave, "UTF-8")
            val sid = URLEncoder.encode(serie.id, "UTF-8")
            val c = URL("$XTREAM_BASE_URL_NOVATV/player_api.php?username=$u&password=$p&action=get_series_info&series_id=$sid")
                .openConnection() as HttpURLConnection
            c.connectTimeout = 15000
            c.readTimeout = 15000
            c.instanceFollowRedirects = true
            val root = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            c.disconnect()

            val info = root.optJSONObject("info") ?: JSONObject()
            val backdrop = info.optJSONArray("backdrop_path")?.optString(0)
                ?: info.optString("backdrop_path", "")

            val detalleXtream = DetalleSerie(
                descripcion = info.optString("plot", info.optString("description", "")),
                portada = info.optString("cover", serie.portada).ifBlank { serie.portada },
                fondo = backdrop,
                anio = info.optString("releaseDate", info.optString("year", "")),
                genero = info.optString("genre", ""),
                rating = info.optString("rating", "")
            )

            // TMDb solo completa metadatos faltantes. Temporadas/episodios siguen
            // viniendo del servidor Xtream para no alterar la reproducción.
            val detalle = respaldoSerieTmdbNovaTV(
                serie = serie,
                actual = detalleXtream
            )

            val eps = root.optJSONObject("episodes") ?: JSONObject()
            val lista = mutableListOf<Episodio>()
            val keys = eps.keys()
            while (keys.hasNext()) {
                val seasonKey = keys.next()
                val season = seasonKey.toIntOrNull() ?: 0
                val arr = eps.optJSONArray(seasonKey) ?: continue
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val num = o.optString("episode_num", (i + 1).toString())
                    val title = o.optString("title", "Episodio $num")
                    lista.add(
                        Episodio(
                            id = o.optString("id"),
                            titulo = "Episodio $num - $title",
                            extension = o.optString("container_extension", "mp4"),
                            temporada = season,
                            numero = num.toIntOrNull() ?: (i + 1)
                        )
                    )
                }
            }

            onResultado(
                detalle,
                lista.sortedWith(compareBy<Episodio> { it.temporada }.thenBy { it.numero }.thenBy { it.titulo })
            )
        } catch (e: Exception) {
            onError("Error cargando información de la serie: ${e.message}")
        }
    }.start()
}

fun cargarCategoriasSeries(usuario:String, clave:String, onResultado:(List<CategoriaSerie>)->Unit, onError:(String)->Unit) {
    Thread { try {
        val u=URLEncoder.encode(usuario,"UTF-8"); val p=URLEncoder.encode(clave,"UTF-8")
        val c=URL("$XTREAM_BASE_URL_NOVATV/player_api.php?username=$u&password=$p&action=get_series_categories").openConnection() as HttpURLConnection
        c.connectTimeout=15000;c.readTimeout=15000;c.instanceFollowRedirects=true
        val a=JSONArray(c.inputStream.bufferedReader().use{it.readText()}); c.disconnect()
        val l=mutableListOf<CategoriaSerie>()
        for(i in 0 until a.length()){val o=a.getJSONObject(i);l.add(CategoriaSerie(o.optString("category_id"),o.optString("category_name","Sin nombre")))}
        onResultado(l)
    } catch(e:Exception){onError("Error cargando categorías de series: ${e.message}")} }.start()
}

fun cargarSeries(usuario:String, clave:String, categoriaId:String, onResultado:(List<Serie>)->Unit, onError:(String)->Unit) {
    Thread { try {
        val u=URLEncoder.encode(usuario,"UTF-8");val p=URLEncoder.encode(clave,"UTF-8");val cat=URLEncoder.encode(categoriaId,"UTF-8")
        val c=URL("$XTREAM_BASE_URL_NOVATV/player_api.php?username=$u&password=$p&action=get_series&category_id=$cat").openConnection() as HttpURLConnection
        c.connectTimeout=15000;c.readTimeout=15000;c.instanceFollowRedirects=true
        val a=JSONArray(c.inputStream.bufferedReader().use{it.readText()});c.disconnect()
        val l=mutableListOf<Serie>()
        for(i in 0 until a.length()){
            val o=a.getJSONObject(i)
            l.add(
                Serie(
                    id = o.optString("series_id"),
                    nombre = o.optString("name","Sin nombre"),
                    portada = o.optString("cover", o.optString("stream_icon", "")),
                    agregado = o.optString("last_modified", o.optString("added", "0")).toLongOrNull() ?: 0L,
                    categoriaId = o.optString("category_id", categoriaId)
                )
            )
        }
        onResultado(l)
    }catch(e:Exception){onError("Error cargando series: ${e.message}")} }.start()
}

fun cargarEpisodiosSerie(usuario:String, clave:String, serieId:String, onResultado:(List<Episodio>)->Unit, onError:(String)->Unit) {
    Thread { try {
        val u=URLEncoder.encode(usuario,"UTF-8");val p=URLEncoder.encode(clave,"UTF-8");val sid=URLEncoder.encode(serieId,"UTF-8")
        val c=URL("$XTREAM_BASE_URL_NOVATV/player_api.php?username=$u&password=$p&action=get_series_info&series_id=$sid").openConnection() as HttpURLConnection
        c.connectTimeout=15000;c.readTimeout=15000;c.instanceFollowRedirects=true
        val root=JSONObject(c.inputStream.bufferedReader().use{it.readText()});c.disconnect()
        val eps=root.optJSONObject("episodes") ?: JSONObject()
        val l=mutableListOf<Episodio>()
        val keys=eps.keys()
        while(keys.hasNext()){
            val seasonKey=keys.next(); val season=seasonKey.toIntOrNull() ?: 0
            val arr=eps.optJSONArray(seasonKey) ?: continue
            for(i in 0 until arr.length()){
                val o=arr.getJSONObject(i)
                val num=o.optString("episode_num",(i+1).toString())
                val title=o.optString("title","Episodio $num")
                l.add(Episodio(o.optString("id"),"Episodio $num - $title",o.optString("container_extension","mp4"),season,num.toIntOrNull() ?: (i + 1)))
            }
        }
        onResultado(l.sortedWith(compareBy<Episodio>{it.temporada}.thenBy{it.titulo}))
    }catch(e:Exception){onError("Error cargando episodios: ${e.message}")} }.start()
}
