package su.xash.engine.ui.settings

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.core.net.toUri
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import su.xash.engine.R
import su.xash.engine.model.Game
import su.xash.engine.model.GameLibDownloader
import java.text.DateFormat
import java.util.Date

class GameSettingsPreferenceFragment(
	val game: Game
) : PreferenceFragmentCompat() {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		preferenceManager.sharedPreferencesName = game.basedir.name
		setPreferencesFromResource(R.xml.game_preferences, rootKey)

		configureResolutionPreference()

		val packageList = findPreference<ListPreference>("package_name")!!
		packageList.entries = arrayOf(getString(R.string.app_name))
		packageList.entryValues = arrayOf(requireContext().packageName)

		if (packageList.value == null) {
			packageList.setValueIndex(0)
		}

		if (
			game.basedir.name.equals("cstrike", ignoreCase = true) ||
			game.basedir.name.equals("czero", ignoreCase = true)
		) {
			val enableYaPBBots =
			findPreference<SwitchPreferenceCompat>("enable_yapb_bots")!!

			enableYaPBBots.isVisible = true
		}

		populateDownloadedBuildInfo()

		val separatePackages =
		findPreference<SwitchPreferenceCompat>("separate_libraries")!!

		val clientPackage =
		findPreference<ListPreference>("client_package")!!

		val serverPackage =
		findPreference<ListPreference>("server_package")!!

		separatePackages.setOnPreferenceChangeListener { _, newValue ->
			if (newValue == true) {
				packageList.isVisible = false
				clientPackage.isVisible = true
				serverPackage.isVisible = true
			} else {
				packageList.isVisible = true
				clientPackage.isVisible = false
				serverPackage.isVisible = false
			}

			true
		}
	}

	private fun configureResolutionPreference() {
		val resolutionPreference =
		findPreference<EditTextPreference>("resolution") ?: return

		resolutionPreference.setOnBindEditTextListener { editText ->
			editText.inputType =
			InputType.TYPE_CLASS_TEXT or
			InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

			editText.hint = "Exemplo: 1280x720 ou auto"
			editText.setSelectAllOnFocus(true)
		}

		resolutionPreference.setOnPreferenceChangeListener { _, newValue ->
			val resolution = newValue
			?.toString()
			?.trim()
			?.lowercase()
			?.replace(" ", "")
			?: ""

			if (resolution == "auto") {
				resolutionPreference.text = "auto"
				return@setOnPreferenceChangeListener true
			}

			val match = RESOLUTION_PATTERN.matchEntire(resolution)

			if (match == null) {
				showResolutionError(
					"Use o formato largura x altura. Exemplo: 1280x720"
				)

				return@setOnPreferenceChangeListener false
			}

			val width = match.groupValues[1].toIntOrNull()
			val height = match.groupValues[2].toIntOrNull()

			if (width == null || height == null) {
				showResolutionError("A resolução informada é inválida.")
				return@setOnPreferenceChangeListener false
			}

			if (width < MIN_WIDTH || height < MIN_HEIGHT) {
				showResolutionError(
					"A resolução mínima permitida é ${MIN_WIDTH}x${MIN_HEIGHT}."
				)

				return@setOnPreferenceChangeListener false
			}

			if (width > MAX_WIDTH || height > MAX_HEIGHT) {
				showResolutionError(
					"A resolução máxima permitida é ${MAX_WIDTH}x${MAX_HEIGHT}."
				)

				return@setOnPreferenceChangeListener false
			}

			val normalizedResolution = "${width}x${height}"

			resolutionPreference.text = normalizedResolution

			true
		}
	}

	private fun showResolutionError(message: String) {
		Toast.makeText(
			requireContext(),
					   message,
				 Toast.LENGTH_LONG
		).show()
	}

	private fun populateDownloadedBuildInfo() {
		val downloader = GameLibDownloader(requireContext())
		val source = downloader.getSourceInfo(game.basedir.name) ?: return
		val downloadedAt = downloader.getDownloadTime(game.basedir.name)

		val urlPref = findPreference<Preference>("source_url")!!
		urlPref.isVisible = true
		urlPref.summary = source.url ?: "—"
		urlPref.isEnabled = source.url != null

		urlPref.setOnPreferenceClickListener {
			source.url?.let {
				startActivity(Intent(Intent.ACTION_VIEW, it.toUri()))
			}

			true
		}

		val branchPref = findPreference<Preference>("source_branch")!!
		branchPref.isVisible = true
		branchPref.summary = source.branch ?: "—"

		val commitPref = findPreference<Preference>("source_commit")!!
		commitPref.isVisible = true
		commitPref.summary = source.commit ?: "—"
		commitPref.isEnabled = source.commit != null && source.url != null

		commitPref.setOnPreferenceClickListener {
			val target =
			"${source.url!!.trimEnd('/')}/commit/${source.commit}"

			startActivity(Intent(Intent.ACTION_VIEW, target.toUri()))

			true
		}

		val timePref = findPreference<Preference>("downloaded_at")!!
		timePref.isVisible = true

		timePref.summary =
		if (downloadedAt > 0L) {
			DateFormat
			.getDateTimeInstance()
			.format(Date(downloadedAt))
		} else {
			"—"
		}
	}

	companion object {
		private val RESOLUTION_PATTERN =
		Regex("""^(\d{3,5})[xX](\d{3,5})$""")

		private const val MIN_WIDTH = 320
		private const val MIN_HEIGHT = 240

		private const val MAX_WIDTH = 16384
		private const val MAX_HEIGHT = 16384
	}
}
