package com.smiledev.rafiq_quran.ui.mosques

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.core.displayMessage
import com.smiledev.rafiq_quran.domain.model.Mosque
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
import java.util.Locale

private const val SOURCE_MOSQUES = "mosques"
private const val SOURCE_USER = "user"
private const val ICON_MOSQUE = "ic-mosque-teal"
private const val STYLE_URI = "https://tiles.openfreemap.org/styles/bright"

private fun tintedBitmap(context: Context, @DrawableRes res: Int, tint: Int, sizeDp: Int = 40): Bitmap {
    val d = ContextCompat.getDrawable(context, res)!!
    val w = DrawableCompat.wrap(d).mutate().also { DrawableCompat.setTint(it, tint) }
    val px = (sizeDp * context.resources.displayMetrics.density).toInt()
    w.setBounds(0, 0, px, px)
    return Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888).also { Canvas(it).also(w::draw) }
}

private fun formatDistance(meters: Float): String {
    return if (meters >= 1000) {
        String.format(Locale.getDefault(), "%.1f km", meters / 1000f)
    } else {
        "${meters.toInt()} m"
    }
}

private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val results = FloatArray(1)
    android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0]
}

private fun openDirectionsInMaps(context: Context, latitude: Double, longitude: Double, name: String) {
    val encodedName = Uri.encode(name)
    val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedName)")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        } catch (_: Exception) {
            // No browser or maps app available
        }
    }
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
    var selected by remember { mutableStateOf<Mosque?>(null) }
    var isListView by remember { mutableStateOf(false) }

    var cameraTarget by remember { mutableStateOf<LatLng?>(null) }
    var showSearchThisArea by remember { mutableStateOf(false) }

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
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(obs)
        onDispose {
            lifecycle.removeObserver(obs)
            mapView.onDestroy()
        }
    }

    var mapRef by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
    var styleRef by remember { mutableStateOf<Style?>(null) }

    // Reset search button visibility whenever new mosques or searchCenter updates
    LaunchedEffect(state.mosques) {
        showSearchThisArea = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nearby_mosques)) },
                navigationIcon = {
                    Text(
                        text = stringResource(R.string.back),
                        modifier = Modifier
                            .clickable(onClick = onBack)
                            .padding(16.dp)
                    )
                },
                actions = {
                    IconButton(onClick = { isListView = !isListView }) {
                        if (isListView) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = stringResource(R.string.view_map)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = stringResource(R.string.view_list)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (state.error != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.error?.displayMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { viewModel.retry() }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    // MapView stays alive underneath to preserve state and tiles
                    AndroidView(
                        factory = {
                            mapView.apply {
                                onCreate(null)
                                getMapAsync { map ->
                                    mapRef = map
                                    map.setStyle(Style.Builder().fromUri(STYLE_URI)) { style ->
                                        styleRef = style
                                        val initialLat = state.userLocation?.latitude ?: -6.2088
                                        val initialLon = state.userLocation?.longitude ?: 106.8456
                                        map.cameraPosition = CameraPosition.Builder()
                                            .target(LatLng(initialLat, initialLon))
                                            .zoom(14.0)
                                            .build()

                                        map.uiSettings.isCompassEnabled = true

                                        // Mosque source + cluster layers
                                        val mosqueSrc = GeoJsonSource(
                                            SOURCE_MOSQUES,
                                            FeatureCollection.fromFeatures(emptyList()),
                                            GeoJsonOptions()
                                                .withCluster(true)
                                                .withClusterRadius(50)
                                                .withClusterMaxZoom(14)
                                        )
                                        style.addSource(mosqueSrc)
                                        style.addImage(ICON_MOSQUE, tintedBitmap(context, R.drawable.ic_mosque, Teal500.toArgb()))
                                        style.addLayer(
                                            CircleLayer("clusters", SOURCE_MOSQUES).withProperties(
                                                PropertyFactory.circleColor(Teal700.toArgb()),
                                                PropertyFactory.circleRadius(22f),
                                                PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
                                                PropertyFactory.circleStrokeWidth(2f)
                                            ).withFilter(Expression.eq(Expression.get("cluster"), true))
                                        )
                                        style.addLayer(
                                            SymbolLayer("cluster-count", SOURCE_MOSQUES).withProperties(
                                                PropertyFactory.textField(Expression.toString(Expression.get("point_count"))),
                                                PropertyFactory.textSize(14f),
                                                PropertyFactory.textColor(android.graphics.Color.WHITE),
                                                PropertyFactory.textIgnorePlacement(true),
                                                PropertyFactory.textAllowOverlap(true)
                                            ).withFilter(Expression.eq(Expression.get("cluster"), true))
                                        )
                                        style.addLayer(
                                            SymbolLayer("unclustered-mosques", SOURCE_MOSQUES).withProperties(
                                                PropertyFactory.iconImage(ICON_MOSQUE),
                                                PropertyFactory.iconAllowOverlap(true)
                                            ).withFilter(Expression.neq(Expression.get("cluster"), true))
                                        )

                                        // User dot layer
                                        style.addSource(GeoJsonSource(SOURCE_USER, FeatureCollection.fromFeatures(emptyList())))
                                        style.addLayer(
                                            CircleLayer("user-dot", SOURCE_USER).withProperties(
                                                PropertyFactory.circleRadius(10f),
                                                PropertyFactory.circleColor(DeepBlue700.toArgb()),
                                                PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
                                                PropertyFactory.circleStrokeWidth(2f)
                                            )
                                        )

                                        // Map click: clusters or unclustered markers
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

                                        // Camera idle listener: detect when user swipes to another area
                                        map.addOnCameraIdleListener {
                                            val target = map.cameraPosition.target
                                            if (target != null) {
                                                cameraTarget = target
                                                val center = state.searchCenter ?: state.userLocation
                                                if (center != null) {
                                                    val dist = calculateDistanceMeters(
                                                        center.latitude,
                                                        center.longitude,
                                                        target.latitude,
                                                        target.longitude
                                                    )
                                                    if (dist > 800f) {
                                                        showSearchThisArea = true
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlays when in Map View
                    if (!isListView) {
                        // "Search this area" floating button
                        if (showSearchThisArea || state.isSearchingArea) {
                            Button(
                                onClick = {
                                    cameraTarget?.let { target ->
                                        showSearchThisArea = false
                                        viewModel.searchArea(target.latitude, target.longitude)
                                    }
                                },
                                enabled = !state.isSearchingArea,
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 12.dp)
                            ) {
                                if (state.isSearchingArea) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.width(8.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = stringResource(R.string.search_this_area),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        // Mosque count pill badge
                        if (state.mosques.isNotEmpty() && !showSearchThisArea && !state.isSearchingArea) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = 16.dp, top = 12.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = stringResource(R.string.mosques_found_count, state.mosques.size),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Recenter FAB
                        FloatingActionButton(
                            onClick = {
                                val userLoc = state.userLocation
                                if (userLoc != null) {
                                    mapRef?.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(LatLng(userLoc.latitude, userLoc.longitude), 14.0),
                                        800
                                    )
                                    viewModel.searchArea(userLoc.latitude, userLoc.longitude)
                                    showSearchThisArea = false
                                } else {
                                    val granted = viewModel.checkLocationPermission()
                                    if (!granted) {
                                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    } else {
                                        viewModel.fetchLocation()
                                    }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = if (selected != null) 170.dp else 16.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = Teal500
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = stringResource(R.string.recenter_location)
                            )
                        }

                        // Selected Mosque Card
                        selected?.let { m ->
                            Card(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = m.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { selected = null },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text(
                                                text = "✕",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    val distanceMeters = remember(m, state.userLocation) {
                                        state.userLocation?.let { loc ->
                                            calculateDistanceMeters(loc.latitude, loc.longitude, m.latitude, m.longitude)
                                        }
                                    }
                                    if (distanceMeters != null) {
                                        Spacer(Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = Teal500
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = formatDistance(distanceMeters),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = {
                                                openDirectionsInMaps(context, m.latitude, m.longitude, m.name)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Teal500)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.directions))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // List View Overlay
                    if (isListView) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            if (state.mosques.isEmpty() && !state.isLoading && !state.isSearchingArea) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = stringResource(R.string.no_mosques_found),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Button(onClick = { isListView = false }) {
                                            Text(stringResource(R.string.view_map))
                                        }
                                    }
                                }
                            } else {
                                val sortedMosques = remember(state.mosques, state.userLocation, state.searchCenter) {
                                    val refLoc = state.userLocation ?: state.searchCenter
                                    if (refLoc != null) {
                                        state.mosques.sortedBy {
                                            calculateDistanceMeters(refLoc.latitude, refLoc.longitude, it.latitude, it.longitude)
                                        }
                                    } else {
                                        state.mosques
                                    }
                                }
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    item {
                                        Text(
                                            text = stringResource(R.string.mosques_found_count, sortedMosques.size),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                    }
                                    items(sortedMosques, key = { it.id }) { m ->
                                        val distanceMeters = remember(m, state.userLocation) {
                                            state.userLocation?.let { loc ->
                                                calculateDistanceMeters(loc.latitude, loc.longitude, m.latitude, m.longitude)
                                            }
                                        }
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Column(Modifier.padding(14.dp)) {
                                                Text(
                                                    text = m.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (distanceMeters != null) {
                                                    Spacer(Modifier.height(4.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Place,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp),
                                                            tint = Teal500
                                                        )
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(
                                                            text = formatDistance(distanceMeters),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                Spacer(Modifier.height(10.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            isListView = false
                                                            selected = m
                                                            mapRef?.animateCamera(
                                                                CameraUpdateFactory.newLatLngZoom(LatLng(m.latitude, m.longitude), 15.0),
                                                                600
                                                            )
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(stringResource(R.string.view_on_map))
                                                    }
                                                    Spacer(Modifier.width(8.dp))
                                                    Button(
                                                        onClick = {
                                                            openDirectionsInMaps(context, m.latitude, m.longitude, m.name)
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(stringResource(R.string.directions))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    if (state.showPermissionDenied && !isListView) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.fallback_location),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                                    Text(stringResource(R.string.grant_location_permission))
                                }
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
                addStringProperty("mosque_id", it.id.toString())
                addStringProperty("name", it.name)
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
