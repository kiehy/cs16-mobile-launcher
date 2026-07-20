package su.xash.engine.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import su.xash.engine.R

class AppSettingsPreferenceFragment() : PreferenceFragmentCompat() {
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		preferenceManager.sharedPreferencesName = "app_preferences";
		setPreferencesFromResource(R.xml.app_preferences, rootKey);

		findPreference<Preference>("crash_logs")?.setOnPreferenceClickListener {
			findNavController().navigate(R.id.action_appSettingsFragment_to_crashLogsFragment)
			true
		}

		findPreference<Preference>("discord_join")?.setOnPreferenceClickListener {
			val url = preferenceManager.sharedPreferences?.getString(
				"discord_url", "https://discord.gg/suacomunidade"
			)
			try {
				startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
			} catch (e: ActivityNotFoundException) {
				Toast.makeText(requireContext(), R.string.discord_open_error, Toast.LENGTH_SHORT).show()
			}
			true
		}
	}
}
