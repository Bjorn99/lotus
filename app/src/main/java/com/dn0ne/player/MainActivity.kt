package com.dn0ne.player

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import java.io.File
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.request.transitionFactory
import coil3.transition.CrossfadeTransition
import com.dn0ne.player.app.data.MetadataWriter
import com.dn0ne.player.app.data.decodeLyrics
import com.dn0ne.player.app.domain.metadata.Metadata
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.presentation.PlayerScreen
import com.dn0ne.player.app.presentation.PlayerViewModel
import com.dn0ne.player.app.presentation.components.EmbeddedArtFetcher
import com.dn0ne.player.app.presentation.components.EmbeddedArtInterceptor
import com.dn0ne.player.app.presentation.components.EmbeddedArtKeyer
import com.dn0ne.player.app.presentation.components.settings.Theme
import com.dn0ne.player.app.presentation.components.snackbar.ObserveAsEvents
import com.dn0ne.player.app.presentation.components.snackbar.ScaffoldWithSnackbarEvents
import com.dn0ne.player.app.presentation.components.snackbar.SnackbarController
import com.dn0ne.player.app.presentation.components.snackbar.SnackbarEvent
import com.dn0ne.player.core.data.MusicScanner
import com.dn0ne.player.core.data.Settings
import com.dn0ne.player.core.presentation.Routes
import com.dn0ne.player.setup.data.SetupState
import com.dn0ne.player.setup.presentation.SetupScreen
import com.dn0ne.player.setup.presentation.SetupViewModel
import com.dn0ne.player.ui.theme.MusicPlayerTheme
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.getViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        SingletonImageLoader.setSafe {
            ImageLoader.Builder(applicationContext)
                .transitionFactory(CrossfadeTransition.Factory())
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(applicationContext, 0.25)
                        .build()
                }
                .components {
                    add(EmbeddedArtFetcher.Factory(applicationContext))
                    add(EmbeddedArtKeyer())
                    add(EmbeddedArtInterceptor())
                }
                .build()
        }

        val setupViewModel = getViewModel<SetupViewModel>()
        setupViewModel.onAudioPermissionRequest(checkAudioPermission())

        val setupState = get<SetupState>()
        val settingsToast = Toast.makeText(
            this,
            resources.getString(R.string.grant_permission_in_settings),
            Toast.LENGTH_SHORT
        )

        val requestAudioPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    setupViewModel.onAudioPermissionRequest(true)
                } else {
                    settingsToast.show()
                    goToAppSettings()
                }
            }

        var isWritePermissionGranted = checkWritePermission()
        val requestWritePermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                isWritePermissionGranted = isGranted
            }

        var trackToMetadataPair: Pair<Track, Metadata>? = null
        val requestOneTimeWritePermissionLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                trackToMetadataPair?.let {
                    val metadataWriter: MetadataWriter = get()
                    lifecycleScope.launch {
                        // The after-consent retry does the same full file copy +
                        // jaudiotagger read/write; keep it off the main thread
                        // too, or granting write permission ANRs on large files.
                        val result = withContext(Dispatchers.IO) {
                            metadataWriter.writeMetadata(
                                track = it.first,
                                metadata = it.second,
                                onSecurityError = {}
                            )
                        }

                        checkMetadataWriteResult(result)
                    }
                }
            }

        val pickedCoverArtChannel = Channel<ByteArray>()
        val pickCoverArt =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                uri?.let {
                    lifecycleScope.launch {
                        contentResolver.openInputStream(it)?.use { input ->
                            pickedCoverArtChannel.send(input.readBytes())
                        }
                    }
                }
            }

        var shouldScanPickedFolder = false
        val pickedFolderChannel = Channel<String>()
        val pickFolder =
            registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                uri?.let {
                    lifecycleScope.launch {
                        pickedFolderChannel.send(
                            getPathFromFolderUri(it)
                        )
                    }
                }
            }

        val sidecarFolderPicker =
            registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                if (uri != null) {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    get<Settings>().sidecarFolderUri = uri.toString()
                }
            }

        val pickedLyricsFileContentChannel = Channel<String>()
        val pickLyricsFile =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                uri?.let {
                    var lyrics: String? = null
                    contentResolver.openInputStream(it)?.use { input ->
                        lyrics = input.readBytes().decodeLyrics()
                    }

                    lyrics?.let {
                        lifecycleScope.launch {
                            pickedLyricsFileContentChannel.send(
                                it
                            )
                        }
                    }
                }
            }

        val pickedPlaylistChannel = Channel<Triple<String, String, String?>>()
        val playlistPicker =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                uri?.let {
                    val name = Uri.decode(it.toString()).substringAfterLast('/').substringBeforeLast('.')
                    val m3uDirectory = getDirectoryFromDocumentUri(it)
                    var content: String? = null
                    contentResolver.openInputStream(it)?.use { input ->
                        content = input.readBytes().toString(Charsets.UTF_8)
                    }

                    content?.let {
                        lifecycleScope.launch {
                            pickedPlaylistChannel.send(Triple(name, content, m3uDirectory))
                        }
                    }
                }
            }

        val startDestination = if (checkAudioPermission() && setupState.isComplete) {
            Routes.Player
        } else Routes.Setup

        setContent {
            MusicPlayerTheme {
                ScaffoldWithSnackbarEvents(modifier = Modifier.fillMaxSize()) {

                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable<Routes.Setup> {
                            SetupScreen(
                                viewModel = setupViewModel,
                                requestAudioPermission = {
                                    when {
                                        checkAudioPermission() -> {
                                            setupViewModel.onAudioPermissionRequest(true)
                                        }

                                        else -> {
                                            requestAudioPermissionLauncher.launch(
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    Manifest.permission.READ_MEDIA_AUDIO
                                                } else Manifest.permission.READ_EXTERNAL_STORAGE,
                                            )
                                        }
                                    }
                                },
                                onFolderPick = { shouldScan ->
                                    shouldScanPickedFolder = shouldScan
                                    pickFolder.launch(null)
                                },
                                onFinishSetupClick = {
                                    navController.navigate(Routes.Player) {
                                        popUpTo(Routes.Setup) {
                                            inclusive = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                            )

                            ObserveAsEvents(pickedFolderChannel.receiveAsFlow()) { path ->
                                if (shouldScanPickedFolder) {
                                    lifecycleScope.launch {
                                        get<MusicScanner>().scanFolder(path)
                                        shouldScanPickedFolder = false
                                    }
                                    return@ObserveAsEvents
                                }
                                setupViewModel.onFolderPicked(path)
                            }
                        }

                        composable<Routes.Player> {
                            val viewModel = getViewModel<PlayerViewModel>()
                            val mediaSessionToken =
                                SessionToken(
                                    application,
                                    ComponentName(application, PlaybackService::class.java)
                                )

                            val controllerFuture =
                                MediaController.Builder(application, mediaSessionToken).buildAsync()
                            controllerFuture.addListener(
                                {
                                    viewModel.player = controllerFuture.get()
                                },
                                MoreExecutors.directExecutor()
                            )

                            val appearance by viewModel.settings.appearance.collectAsState()
                            val isDarkTheme = when (appearance) {
                                Theme.Appearance.System -> isSystemInDarkTheme()
                                Theme.Appearance.Light -> false
                                Theme.Appearance.Dark -> true
                            }
                            LaunchedEffect(appearance) {
                                WindowCompat.getInsetsController(window, window.decorView)
                                    .apply {
                                        isAppearanceLightStatusBars = !isDarkTheme
                                        isAppearanceLightNavigationBars = !isDarkTheme
                                    }

                            }

                            val useDynamicColor by viewModel.settings.useDynamicColor.collectAsState()
                            MusicPlayerTheme(
                                dynamicColor = useDynamicColor
                            ) {
                                PlayerScreen(
                                    viewModel = viewModel,
                                    onCoverArtPick = {
                                        pickCoverArt.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                    onFolderPick = { shouldScan ->
                                        shouldScanPickedFolder = shouldScan
                                        pickFolder.launch(null)
                                    },
                                    onSidecarFolderPick = {
                                        sidecarFolderPicker.launch(null)
                                    },
                                    onLyricsPick = {
                                        pickLyricsFile.launch(arrayOf("text/plain", "application/lrc"))
                                    },
                                    onPlaylistPick = {
                                        playlistPicker.launch(arrayOf("audio/x-mpegurl"))
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            val coroutineScope = rememberCoroutineScope()
                            ObserveAsEvents(flow = viewModel.pendingMetadata) { pendingWrite ->
                                val track = pendingWrite.track
                                val metadata = pendingWrite.metadata
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    trackToMetadataPair = track to metadata

                                    val metadataWriter: MetadataWriter = get()
                                    coroutineScope.launch {
                                        // writeMetadata copies the whole audio
                                        // file and runs jaudiotagger read/write;
                                        // keep it off the main thread to avoid an
                                        // ANR on large files. The consent
                                        // launcher must fire on the UI thread, so
                                        // onSecurityError re-dispatches to it.
                                        val result = withContext(Dispatchers.IO) {
                                            metadataWriter.writeMetadata(
                                                track = track,
                                                metadata = metadata,
                                                onSecurityError = { intentSender ->
                                                    coroutineScope.launch {
                                                        requestOneTimeWritePermissionLauncher.launch(
                                                            IntentSenderRequest.Builder(intentSender).build()
                                                        )
                                                    }
                                                }
                                            )
                                        }

                                        checkMetadataWriteResult(result)
                                        pendingWrite.onComplete(result)
                                    }
                                } else {
                                    if (!isWritePermissionGranted) {
                                        requestWritePermissionLauncher.launch(
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        )

                                        if (!isWritePermissionGranted) {
                                            coroutineScope.launch {
                                                SnackbarController.sendEvent(
                                                    SnackbarEvent(
                                                        message = R.string.write_permission_denied
                                                    )
                                                )
                                            }
                                        }

                                        pendingWrite.onComplete(Result.Error(DataError.Local.NoWritePermission))
                                        return@ObserveAsEvents
                                    }

                                    val metadataWriter: MetadataWriter = get()
                                    coroutineScope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            metadataWriter.writeMetadata(
                                                track = track,
                                                metadata = metadata,
                                                onSecurityError = {}
                                            )
                                        }

                                        checkMetadataWriteResult(result)
                                        pendingWrite.onComplete(result)
                                    }
                                }
                            }

                            ObserveAsEvents(pickedCoverArtChannel.receiveAsFlow()) { bytes ->
                                viewModel.setPickedCoverArtBytes(bytes)
                            }

                            ObserveAsEvents(pickedFolderChannel.receiveAsFlow()) { path ->
                                if (shouldScanPickedFolder) {
                                    lifecycleScope.launch {
                                        get<MusicScanner>().scanFolder(path)
                                        shouldScanPickedFolder = false
                                    }
                                    return@ObserveAsEvents
                                }
                                viewModel.onFolderPicked(path)
                            }

                            ObserveAsEvents(pickedLyricsFileContentChannel.receiveAsFlow()) { lyrics ->
                                viewModel.onLyricsPicked(lyrics)
                            }

                            ObserveAsEvents(pickedPlaylistChannel.receiveAsFlow()) { (name, content, directory) ->
                                viewModel.parseM3U(name, content, directory)
                            }

                            if (viewModel.settings.scanOnAppLaunch.value) {
                                lifecycleScope.launch {
                                    get<MusicScanner>().refreshMedia(showMessages = false)
                                }
                            }

                            if (intent.action == Intent.ACTION_VIEW) {
                                val trackUri = intent.data
                                trackUri?.let { uri ->
                                    MediaScannerConnection.scanFile(
                                        this@MainActivity,
                                        arrayOf(uri.path),
                                        null,
                                        object : MediaScannerConnection.OnScanCompletedListener {
                                            override fun onScanCompleted(
                                                p0: String?,
                                                p1: Uri?
                                            ) {
                                                viewModel.playTrackFromUri(uri)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.action == Intent.ACTION_VIEW) {
            val trackUri = intent.data
            trackUri?.let { uri ->
                MediaScannerConnection.scanFile(
                    this@MainActivity,
                    arrayOf(uri.path),
                    arrayOf("audio/*"),
                    object : MediaScannerConnection.OnScanCompletedListener {
                        override fun onScanCompleted(
                            p0: String?,
                            p1: Uri?
                        ) {
                            getViewModel<PlayerViewModel>().playTrackFromUri(uri)
                        }
                    }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
    }

    private fun checkAudioPermission(): Boolean =
        checkSelfPermission(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    private fun checkWritePermission(): Boolean =
        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun goToAppSettings() {
        val intent = Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun checkMetadataWriteResult(result: Result<Unit, DataError.Local>) {
        lifecycleScope.launch {
            when (result) {
                is Result.Error -> {
                    when (result.error) {
                        DataError.Local.NoReadPermission -> {
                            SnackbarController.sendEvent(
                                SnackbarEvent(
                                    message = R.string.no_read_permission
                                )
                            )
                        }

                        DataError.Local.NoWritePermission -> {
                            SnackbarController.sendEvent(
                                SnackbarEvent(
                                    message = R.string.no_write_permission
                                )
                            )
                        }

                        DataError.Local.FailedToRead -> {
                            SnackbarController.sendEvent(
                                SnackbarEvent(
                                    message = R.string.failed_to_read
                                )
                            )
                        }

                        DataError.Local.FailedToWrite -> {
                            SnackbarController.sendEvent(
                                SnackbarEvent(
                                    message = R.string.failed_to_write
                                )
                            )
                        }

                        DataError.Local.NoLyricsFound,
                        DataError.Local.Unknown -> {
                            SnackbarController.sendEvent(
                                SnackbarEvent(
                                    message = R.string.unknown_error_occurred
                                )
                            )
                        }
                    }
                }

                is Result.Success -> {
                    SnackbarController.sendEvent(
                        SnackbarEvent(
                            message = R.string.metadata_change_succeed
                        )
                    )
                }
            }
        }
    }

    private fun extractStoragePath(uri: Uri, schemePrefix: String): String {
        val decoded = Uri.decode(uri.toString())
        val sd = decoded.substringAfter("$schemePrefix/").substringBefore(':')
            .takeIf { it != "primary" } ?: "emulated/0"
        val path = decoded.substringAfterLast(':')
        return "/storage/$sd/$path"
    }

    private fun getPathFromFolderUri(uri: Uri): String =
        extractStoragePath(uri, "tree")

    private fun getDirectoryFromDocumentUri(uri: Uri): String? =
        File(extractStoragePath(uri, "document")).parent
}