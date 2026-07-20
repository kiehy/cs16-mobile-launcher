package su.xash.engine.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import kotlin.concurrent.thread

object GameInstaller {

	private const val BUFFER_SIZE = 8192

	private const val COUNTER_STRIKE_URL =
	"https://github.com/kiehy/xash-games/releases/download/xash/xash.zip"

	fun install(
		context: Context,
		gameName: String,
		onFinished: (() -> Unit)? = null
	) {
		val downloadUrl = getDownloadUrl(gameName)

		if (downloadUrl == null) {
			Toast.makeText(
				context,
				"Este jogo ainda não possui um link configurado.",
				Toast.LENGTH_LONG
			).show()

			return
		}

		val mainHandler = Handler(Looper.getMainLooper())

		val progressBar = ProgressBar(
			context,
			null,
			android.R.attr.progressBarStyleHorizontal
		).apply {
			max = 100
			progress = 0
			isIndeterminate = true
		}

		val statusText = TextView(context).apply {
			text = "Preparando download..."
			textSize = 16f
		}

		val container = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL

			val padding = (24 * resources.displayMetrics.density).toInt()
			setPadding(padding, padding, padding, 0)

			addView(
				statusText,
		   LinearLayout.LayoutParams(
			   LinearLayout.LayoutParams.MATCH_PARENT,
			   LinearLayout.LayoutParams.WRAP_CONTENT
		   )
			)

			addView(
				progressBar,
		   LinearLayout.LayoutParams(
			   LinearLayout.LayoutParams.MATCH_PARENT,
			   LinearLayout.LayoutParams.WRAP_CONTENT
		   ).apply {
			   topMargin =
			   (16 * resources.displayMetrics.density).toInt()
		   }
			)
		}

		val dialog = MaterialAlertDialogBuilder(context)
		.setTitle("Instalando $gameName")
		.setView(container)
		.setCancelable(false)
		.create()

		dialog.show()

		thread(name = "GameInstallerThread") {
			val temporaryZip = File(
				context.cacheDir,
				"game_download_${System.currentTimeMillis()}.zip"
			)

			try {
				downloadFile(
					urlString = downloadUrl,
				 destination = temporaryZip,
				 onProgress = { progress, totalKnown ->
					 mainHandler.post {
						 progressBar.isIndeterminate = !totalKnown

						 if (totalKnown) {
							 progressBar.progress = progress
							 statusText.text = "Baixando: $progress%"
						 } else {
							 statusText.text = "Baixando jogo..."
						 }
					 }
				 }
				)

				mainHandler.post {
					progressBar.isIndeterminate = true
					statusText.text = "Extraindo arquivos..."
				}

				val installationDirectory = File(
					"/storage/emulated/0/xash"
				)

				if (!installationDirectory.exists()) {
					if (!installationDirectory.mkdirs()) {
						throw IllegalStateException(
							"Não foi possível criar a pasta ${installationDirectory.absolutePath}"
						)
					}
				}

				extractZip(
					zipFile = temporaryZip,
			   destinationDirectory = installationDirectory,
			   onFileExtracted = { filename ->
				   mainHandler.post {
					   statusText.text = "Extraindo: $filename"
				   }
			   }
				)

				temporaryZip.delete()

				mainHandler.post {
					dialog.dismiss()

					MaterialAlertDialogBuilder(context)
					.setTitle("Instalação concluída")
					.setMessage(
						"$gameName foi instalado em:\n\n" +
						installationDirectory.absolutePath
					)
					.setPositiveButton("OK", null)
					.show()

					onFinished?.invoke()
				}
			} catch (exception: Exception) {
				temporaryZip.delete()

				mainHandler.post {
					dialog.dismiss()

					MaterialAlertDialogBuilder(context)
					.setTitle("Erro na instalação")
					.setMessage(
						exception.message
						?: "Ocorreu um erro desconhecido."
					)
					.setPositiveButton("OK", null)
					.show()
				}
			}
		}
	}

	private fun getDownloadUrl(gameName: String): String? {
		return when (gameName) {
			"Counter-Strike 1.6 + Half-Life" -> COUNTER_STRIKE_URL
			else -> null
		}
	}

	private fun downloadFile(
		urlString: String,
		destination: File,
		onProgress: (progress: Int, totalKnown: Boolean) -> Unit
	) {
		var connection: HttpURLConnection? = null

		try {
			connection = openConnectionFollowingRedirects(urlString)

			val responseCode = connection.responseCode

			if (responseCode !in 200..299) {
				throw IllegalStateException(
					"O servidor respondeu com o código HTTP $responseCode."
				)
			}

			val contentType = connection.contentType.orEmpty()

			if (
				contentType.contains("text/html", ignoreCase = true) ||
				contentType.contains("text/plain", ignoreCase = true)
			) {
				throw IllegalStateException(
					"O Google Drive não enviou o arquivo. " +
					"Confirme se o link está público para qualquer pessoa."
				)
			}

			val totalSize = connection.contentLengthLong
			val totalKnown = totalSize > 0L
			var downloadedSize = 0L

			BufferedInputStream(connection.inputStream).use { input ->
				BufferedOutputStream(
					FileOutputStream(destination)
				).use { output ->
					val buffer = ByteArray(BUFFER_SIZE)

					while (true) {
						val bytesRead = input.read(buffer)

						if (bytesRead == -1) {
							break
						}

						output.write(buffer, 0, bytesRead)
						downloadedSize += bytesRead

						if (totalKnown) {
							val progress =
							((downloadedSize * 100L) / totalSize)
							.toInt()
							.coerceIn(0, 100)

							onProgress(progress, true)
						} else {
							onProgress(0, false)
						}
					}

					output.flush()
				}
			}

			if (!destination.exists() || destination.length() == 0L) {
				throw IllegalStateException(
					"O arquivo baixado está vazio."
				)
			}

			if (!isZipFile(destination)) {
				throw IllegalStateException(
					"O arquivo recebido não é um ZIP válido. " +
					"Confira o arquivo compartilhado no Google Drive."
				)
			}
		} finally {
			connection?.disconnect()
		}
	}

	private fun openConnectionFollowingRedirects(
		initialUrl: String
	): HttpURLConnection {
		var currentUrl = initialUrl

		repeat(5) {
			val connection = URL(currentUrl).openConnection()
			as HttpURLConnection

			connection.instanceFollowRedirects = false
			connection.connectTimeout = 30_000
			connection.readTimeout = 30_000
			connection.requestMethod = "GET"
			connection.setRequestProperty(
				"User-Agent",
				"Mozilla/5.0"
			)
			connection.connect()

			val responseCode = connection.responseCode

			if (
				responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
				responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
				responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
				responseCode == 307 ||
				responseCode == 308
			) {
				val location = connection.getHeaderField("Location")
				?: throw IllegalStateException(
					"O servidor redirecionou sem informar o destino."
				)

				val resolvedUrl = URL(URL(currentUrl), location)
				.toString()

				connection.disconnect()
				currentUrl = resolvedUrl
			} else {
				return connection
			}
		}

		throw IllegalStateException(
			"O download teve redirecionamentos demais."
		)
	}

	private fun isZipFile(file: File): Boolean {
		if (file.length() < 4L) {
			return false
		}

		file.inputStream().use { input ->
			val firstByte = input.read()
			val secondByte = input.read()

			return firstByte == 0x50 && secondByte == 0x4B
		}
	}

	private fun extractZip(
		zipFile: File,
		destinationDirectory: File,
		onFileExtracted: (String) -> Unit
	) {
		val destinationCanonicalPath =
		destinationDirectory.canonicalPath + File.separator

		ZipInputStream(
			BufferedInputStream(zipFile.inputStream())
		).use { zipInput ->
			while (true) {
				val entry = zipInput.nextEntry ?: break

				val outputFile = File(
					destinationDirectory,
					entry.name
				)

				val outputCanonicalPath = outputFile.canonicalPath

				if (
					outputCanonicalPath !=
					destinationDirectory.canonicalPath &&
					!outputCanonicalPath.startsWith(
						destinationCanonicalPath
					)
				) {
					throw SecurityException(
						"O ZIP contém um caminho inválido: ${entry.name}"
					)
				}

				if (entry.isDirectory) {
					if (
						!outputFile.exists() &&
						!outputFile.mkdirs()
					) {
						throw IllegalStateException(
							"Não foi possível criar ${outputFile.absolutePath}"
						)
					}
				} else {
					outputFile.parentFile?.let { parent ->
						if (!parent.exists() && !parent.mkdirs()) {
							throw IllegalStateException(
								"Não foi possível criar ${parent.absolutePath}"
							)
						}
					}

					onFileExtracted(entry.name.substringAfterLast('/'))

					BufferedOutputStream(
						FileOutputStream(outputFile)
					).use { output ->
						val buffer = ByteArray(BUFFER_SIZE)

						while (true) {
							val bytesRead = zipInput.read(buffer)

							if (bytesRead == -1) {
								break
							}

							output.write(buffer, 0, bytesRead)
						}

						output.flush()
					}
				}

				zipInput.closeEntry()
			}
		}
	}
}
