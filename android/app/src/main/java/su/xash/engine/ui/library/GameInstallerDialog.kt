package su.xash.engine.ui.library

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object GameInstallerDialog {

	private const val GAME_PACKAGE_NAME =
	"Counter-Strike 1.6 + Half-Life"

	fun show(
		context: Context,
		onGameSelected: (String) -> Unit
	) {
		MaterialAlertDialogBuilder(context)
		.setTitle("Instalar jogo")
		.setMessage(
			"Instalar Counter-Strike 1.6 e Half-Life?\n\n" +
			"Os arquivos serão baixados e instalados automaticamente."
		)
		.setNegativeButton("Cancelar", null)
		.setPositiveButton("Instalar") { _, _ ->
			onGameSelected(GAME_PACKAGE_NAME)
		}
		.show()
	}
}
