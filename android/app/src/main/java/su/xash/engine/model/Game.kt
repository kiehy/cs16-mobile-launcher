package su.xash.engine.model

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import su.xash.engine.R
import su.xash.engine.XashActivity
import su.xash.engine.util.showDownloadProgressDialog
import java.io.File
import java.io.FileInputStream

class Game(
	private val ctx: Context,
		val basedir: File,
		val gameInfoFile: File
) {
	private var iconName = "game.ico"

	var title = "Unknown Game"
	var icon: Bitmap? = null
	var cover: Bitmap? = null

	val mobileHacksGames = arrayOf(
		"aom",
		"bdlands",
		"biglolly",
		"bshift",
		"caseclosed",
		"hl_urbicide",
		"induction",
		"redempt",
		"secret",
		"sewer_beta",
		"tot",
		"valve",
		"vendetta"
	)

	/*
	 * Diretório principal usado pelo Xash.
	 *
	 * Para o Counter-Strike integrado, o próprio diretório cstrike será
	 * informado ao motor. A pasta valve continua necessária no armazenamento
	 * porque contém os arquivos-base do Half-Life utilizados pelo CS.
	 */
	var defaultGameDir = "valve"

	private val pref = ctx.getSharedPreferences(
		basedir.name,
		Context.MODE_PRIVATE
	)

	init {
		parseGameInfo(gameInfoFile)

		val iconFile = File(basedir, iconName)

		if (iconFile.exists()) {
			icon = BitmapFactory.decodeFile(iconFile.path)
		}

		try {
			cover = BackgroundBitmap.createBackground(basedir)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	/*
	 * Inicia o jogo.
	 *
	 * connectTarget pode conter ip:porta para conectar diretamente em um
	 * servidor escolhido na aba de servidores.
	 */
	fun startEngine(
		ctx: Context,
		connectTarget: String? = null
	) {
		val gameDir = basedir.name
		var commandLineArgs = ""

		val isCounterStrike =
		gameDir.equals("cstrike", ignoreCase = true) ||
		gameDir.equals("czero", ignoreCase = true)

		if (!isCounterStrike && gameDir != defaultGameDir) {
			commandLineArgs += "-game $gameDir "
		}

		val resolution = pref.getString("resolution", "auto") ?: "auto"

		if (resolution != "auto") {
			val parts = resolution.split("x")

			if (parts.size == 2) {
				commandLineArgs +=
				"-width ${parts[0]} -height ${parts[1]} "
			}
		}

		if (!connectTarget.isNullOrBlank()) {
			commandLineArgs += "+connect $connectTarget "
		}

		/*
		 * O Counter-Strike agora usa as bibliotecas que estão dentro do próprio
		 * APK do launcher. Não procuramos mais o pacote su.xash.cs16client.
		 */
		if (isCounterStrike) {
			if (pref.getBoolean("enable_yapb_bots", true)) {
				commandLineArgs += "-dll @yapb "
			}

			commandLineArgs +=
			pref.getString("arguments", "-console -log")
			?: "-console -log"

			launchEngine(
				ctx = ctx,
				commandLineArgs = commandLineArgs,
				gameDir = gameDir,
				gameLibDir = ctx.applicationInfo.nativeLibraryDir
			)

			return
		}

		if (mobileHacksGames.any {
			it.equals(gameDir, ignoreCase = true)
		}
		) {
			commandLineArgs += "-dll @hl "
		}

		commandLineArgs +=
		pref.getString("arguments", "-console -log")
		?: "-console -log"

		val downloader = GameLibDownloader(ctx)
		val args = commandLineArgs

		if (downloader.isDownloaded(gameDir)) {
			downloader.logExistingLibs(gameDir)

			launchEngine(
				ctx = ctx,
				commandLineArgs = args,
				gameDir = defaultGameDir
			)

			return
		}

		val scope = CoroutineScope(
			Dispatchers.Main + SupervisorJob()
		)

		scope.launch {
			when (val result = downloader.lookupBuild(gameDir)) {
				is GameLibDownloader.Lookup.Available -> {
					showDownloadDialog(
						ctx = ctx,
						downloader = downloader,
						commandLineArgs = args,
						gameDir = defaultGameDir
					)
				}

				is GameLibDownloader.Lookup.NotInManifest -> {
					launchEngine(
						ctx = ctx,
				  commandLineArgs = args,
				  gameDir = defaultGameDir
					)
				}

				is GameLibDownloader.Lookup.Error -> {
					showManifestErrorDialog(
						ctx = ctx,
						commandLineArgs = args,
						gameDir = defaultGameDir,
						cause = result.cause
					)
				}
			}
		}
	}

	private fun showDownloadDialog(
		ctx: Context,
		downloader: GameLibDownloader,
		commandLineArgs: String,
		gameDir: String
	) {
		showDownloadProgressDialog(
			ctx = ctx,
			titleRes = R.string.downloading_game_libs,
			cancelable = true,
			scope = CoroutineScope(
				Dispatchers.Main + SupervisorJob()
			),
			download = { onProgress ->
				downloader.download(
					basedir.name,
					onProgress
				)
			},
			onSuccess = {
				launchEngine(
					ctx = ctx,
				 commandLineArgs = commandLineArgs,
				 gameDir = gameDir
				)
			}
		)
	}

	private fun showManifestErrorDialog(
		ctx: Context,
		commandLineArgs: String,
		gameDir: String,
		cause: Throwable
	) {
		MaterialAlertDialogBuilder(ctx)
		.setTitle(R.string.manifest_error_title)
		.setMessage(
			ctx.getString(
				R.string.manifest_error_message,
				 cause.message
				 ?: cause.javaClass.simpleName
			)
		)
		.setPositiveButton(R.string.launch_anyway) { _, _ ->
			launchEngine(
				ctx = ctx,
				commandLineArgs = commandLineArgs,
				gameDir = gameDir
			)
		}
		.setNegativeButton(
			android.R.string.cancel,
			null
		)
		.show()
	}

	private fun launchEngine(
		ctx: Context,
		commandLineArgs: String,
		gameDir: String,
		gameLibDir: String? = null
	) {
		ctx.startActivity(
			Intent(ctx, XashActivity::class.java).apply {
				flags =
				Intent.FLAG_ACTIVITY_NEW_TASK or
				Intent.FLAG_ACTIVITY_CLEAR_TASK

				putExtra("gamedir", gameDir)
				putExtra("argv", commandLineArgs)
				putExtra(
					"usevolume",
			 pref.getBoolean(
				 "use_volume_buttons",
				 false
			 )
				)
				putExtra("basedir", basedir.parent)

				if (gameLibDir != null) {
					putExtra("gamelibdir", gameLibDir)
				}
			}
		)
	}

	private fun parseGameInfo(file: File) {
		FileInputStream(file).use { inputStream ->
			inputStream.bufferedReader().use { reader ->
				reader.forEachLine { line ->
					val tokens = line.split(
						"\\s+".toRegex(),
											limit = 2
					)

					if (tokens.size >= 2) {
						val key = tokens[0]
						val value = tokens[1].trim('"')

						if (
							key == "title" ||
							key == "game"
						) {
							title = value
						}

						if (key == "icon") {
							iconName = value
						}
					}
				}
			}
		}
	}

	companion object {
		fun getGames(
			ctx: Context,
			root: File
		): List<Game> {
			val games = mutableListOf<Game>()

			root.listFiles()?.forEach { directory ->
				if (directory.isDirectory) {
					val gameInfo =
					checkIfGamedir(directory)

					if (gameInfo != null) {
						games.add(
							Game(
								ctx,
			directory,
			gameInfo
							)
						)
					}
				}
			}

			return games
		}

		fun checkIfGamedir(
			gamedir: File
		): File? {
			gamedir.listFiles()?.forEach { file ->
				if (file.isFile) {
					if (
						file.name.equals(
							"liblist.gam",
					   ignoreCase = true
						)
					) {
						return file
					}

					if (
						file.name.equals(
							"gameinfo.txt",
					   ignoreCase = true
						)
					) {
						return file
					}
				}
			}

			return null
		}
	}
}
