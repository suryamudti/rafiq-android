package com.smiledev.rafiq_quran.ui.mosques

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.core.displayMessage
import com.smiledev.rafiq_quran.theme.DeepBlue700
import com.smiledev.rafiq_quran.theme.Teal500
import com.smiledev.rafiq_quran.theme.Teal700
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val SOURCE_MOSQUES = "mosques"
private const val SOURCE_USER = "user"
private const val ICON_MOSQUE = "ic-mosque-teal"
private const val STYLE_URI = "https://demotiles.maplibre.org/styles/osm-bright-gl-style/style.json"

private fun tintedBitmap(context: Context, @DrawableRes res: Int, tint: Int, sizeDp: Int = 40): Bitmap {
    val d = ContextCompat.getDrawable(context, res)!!
    val w = DrawableCompat.wrap(d).mutate().also { DrawableCompat.setTint(it, tint) }
    val px = (sizeDp * context.resources.displayMetrics.density).toInt()
    w.setBounds(0, 0, px, px)
    return Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888).also { Canvas(it).also(w::draw) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MosquesScreen(
    onBack: () -> Unit,
    viewModel: MosquesViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var selected by remember { mutableStateOf<com.smiledev.rafiq_quran.domain.model.Mosque?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        val granted = viewModel.checkLocationPermission()
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    remember { MapLibre.getInstance(context, null, WellKnownTileServer.MapLibre) }
    val mapView = remember { MapView(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val obs = LifecycleEventObserver { _, e -> when(e) {
            Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
            Lifecycle.Event.ON_START -> mapView.onStart()
            Lifecycle.Event.ON_RESUME -> mapView.onResume()
            Lifecycle.Event.ON_PAUSE -> mapView.onPause()
            Lifecycle.Event.ON_STOP -> mapView.onStop()
            Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
            else -> {}
        }}
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs); mapView.onDestroy() }
    }

    var mapRef by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
    var styleRef by remember { mutableStateOf<Style?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nearby_mosques)) },
                navigationIcon = {
                    Text(stringResource(R.string.back), modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (state.error != null) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = state.error?.displayMessage ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.retry() }) {
                            Text("Retry")
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { mapView.apply {
                            onCreate(null)
                            getMapAsync { map ->
                                mapRef = map
                                map.setStyle(Style.Builder().fromUri(STYLE_URI)) { style ->
                                    styleRef = style
                                    map.cameraPosition = CameraPosition.Builder().target(LatLng(-6.2088, 106.8456)).zoom(14.0).build()
                                    // compass
                                    map.uiSettings.isCompassEnabled = true

                                    // mosque source + layers (cluster)
                                    val mosqueSrc = GeoJsonSource(SOURCE_MOSQUES, FeatureCollection.fromFeatures(emptyList()),
                                        GeoJsonOptions().withCluster(true).withClusterRadius(50).withClusterMaxZoom(14))
                                    style.addSource(mosqueSrc)
                                    style.addImage(ICON_MOSQUE, tintedBitmap(context, R.drawable.ic_mosque, Teal500.toArgb()))
                                    style.addLayer(CircleLayer("clusters", SOURCE_MOSQUES).withProperties(
                                        PropertyFactory.circleColor(Teal700.toArgb()),
                                        PropertyFactory.circleRadius(22f),
                                        PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
                                        PropertyFactory.circleStrokeWidth(2f)).withFilter(Expression.eq(Expression.get("cluster"), true)))
                                    style.addLayer(SymbolLayer("cluster-count", SOURCE_MOSQUES).withProperties(
                                        PropertyFactory.textField(Expression.toString(Expression.get("point_count"))),
                                        PropertyFactory.textSize(14f),
                                        PropertyFactory.textColor(android.graphics.Color.WHITE),
                                        PropertyFactory.textIgnorePlacement(true),
                                        PropertyFactory.textAllowOverlap(true)).withFilter(Expression.eq(Expression.get("cluster"), true)))
                                    style.addLayer(SymbolLayer("unclustered-mosques", SOURCE_MOSQUES).withProperties(
                                        PropertyFactory.iconImage(ICON_MOSQUE),
                                        PropertyFactory.iconAllowOverlap(true)).withFilter(Expression.neq(Expression.get("cluster"), true)))

                                    // user dot
                                    style.addSource(GeoJsonSource(SOURCE_USER, FeatureCollection.fromFeatures(emptyList())))
                                    style.addLayer(CircleLayer("user-dot", SOURCE_USER).withProperties(
                                        PropertyFactory.circleRadius(10f),
                                        PropertyFactory.circleColor(DeepBlue700.toArgb()),
                                        PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
                                        PropertyFactory.circleStrokeWidth(2f)))

                                    map.addOnMapClickListener { pt ->
                                        val screen = map.projection.toScreenLocation(pt)
                                        @Suppress("DEPRECATION")
                                        val feats = map.queryRenderedFeatures(screen, "clusters", "cluster-count", "unclustered-mosques")
                                        val cl = feats.firstOrNull { it.getBooleanProperty("cluster") == true }
                                        if (cl != null) {
                                            val z = mosqueSrc.getClusterExpansionZoom(cl)
                                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(pt, z.toDouble()), 600)
                                            return@addOnMapClickListener true
                                        }
                                        feats.firstOrNull { it.getBooleanProperty("cluster") != true }?.let {
                                            val id = it.getStringProperty("mosque_id")
                                            selected = state.mosques.find { m -> m.id.toString() == id }
                                        } ?: run { selected = null }
                                        selected != null
                                    }
                                }
                            }
                        } },
                        modifier = Modifier.fillMaxSize()
                    )
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    if (state.showPermissionDenied) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                                Text(stringResource(R.string.grant_location_permission))
                            }
                        }
                    }
                    selected?.let { m ->
                        Card(modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(m.name, style = MaterialTheme.typography.titleMedium)
                                Text("geo:${m.latitude},${m.longitude}?q=${m.name}", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { selected = null }) { Text("Close") }
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(state.mosques, styleRef) {
        val src = styleRef?.getSourceAs<GeoJsonSource>(SOURCE_MOSQUES) ?: return@LaunchedEffect
        val feats = state.mosques.map {
            Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude)).apply {
                addStringProperty("mosque_id", it.id.toString()); addStringProperty("name", it.name)
            }
        }
        src.setGeoJson(FeatureCollection.fromFeatures(feats))
    }

    LaunchedEffect(state.userLocation, styleRef) {
        val loc = state.userLocation ?: return@LaunchedEffect
        styleRef?.getSourceAs<GeoJsonSource>(SOURCE_USER)
            ?.setGeoJson(Feature.fromGeometry(Point.fromLngLat(loc.longitude, loc.latitude)))
        mapRef?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 14.0), 800)
    }
}
