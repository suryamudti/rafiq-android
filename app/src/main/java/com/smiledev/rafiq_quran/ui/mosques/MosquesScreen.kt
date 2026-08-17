package com.smiledev.rafiq.ui.mosques

import android.Manifest
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.smiledev.rafiq.R
import com.smiledev.rafiq.core.displayMessage
import androidx.hilt.navigation.compose.hiltViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MosquesScreen(
    onBack: () -> Unit,
    viewModel: MosquesViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        viewModel.checkLocationPermission()
        if (!state.locationGranted) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val yourLocationLabel = stringResource(R.string.your_location)
    val tapForDetailsLabel = stringResource(R.string.tap_for_details)

    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = context.cacheDir
            osmdroidTileCache = context.cacheDir.resolve("tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(-6.2088, 106.8456))

            val compass = CompassOverlay(context, this)
            compass.enableCompass()
            overlays.add(compass)

            val scaleBar = ScaleBarOverlay(this)
            overlays.add(scaleBar)

            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                } else {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                false
            }
        }
    }

    var userMarker by remember { mutableStateOf<Marker?>(null) }

    LaunchedEffect(state.userLocation) {
        state.userLocation?.let { loc ->
            mapView.controller.animateTo(loc)
            userMarker?.let { mapView.overlays.remove(it) }
            val marker = Marker(mapView).apply {
                position = loc
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = yourLocationLabel
                snippet = tapForDetailsLabel
            }
            mapView.overlays.add(marker)
            userMarker = marker
        }
    }

    LaunchedEffect(state.mosques) {
        val toRemove = mapView.overlays.filterIsInstance<Marker>().filter { it != userMarker }
        toRemove.forEach { mapView.overlays.remove(it) }
        state.mosques.forEach { mosque ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(mosque.latitude, mosque.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = mosque.name
                snippet = tapForDetailsLabel
                setOnMarkerClickListener { m, _ ->
                    if (m.isInfoWindowOpen) m.closeInfoWindow() else m.showInfoWindow()
                    true
                }
            }
            mapView.overlays.add(marker)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onDetach()
        }
    }

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
                    Text(
                        text = state.error?.displayMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { mapView },
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
                }
            }
        }
    }
}
