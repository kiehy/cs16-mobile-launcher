package su.xash.engine.ui.servers

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import su.xash.engine.R
import su.xash.engine.adapters.ServerAdapter
import su.xash.engine.databinding.FragmentServersBinding
import su.xash.engine.model.ServerEntry
import su.xash.engine.model.ServerQueryClient
import su.xash.engine.model.ServerRepository
import su.xash.engine.ui.library.LibraryViewModel
import java.util.concurrent.Executors

class ServersFragment : Fragment() {

	private var _binding: FragmentServersBinding? = null

		private val binding: FragmentServersBinding
			get() = _binding!!

			private val libraryViewModel:
				LibraryViewModel by activityViewModels()

				private lateinit var serverAdapter: ServerAdapter

					private val serverExecutor =
					Executors.newFixedThreadPool(4)

					private var isLoadingServers = false

					override fun onCreateView(
						inflater: LayoutInflater,
						container: ViewGroup?,
						savedInstanceState: Bundle?,
					): View {
						_binding = FragmentServersBinding.inflate(
							inflater,
							container,
							false,
						)

						serverAdapter = ServerAdapter { server ->
							connectToServer(server)
						}

						binding.serversList.layoutManager =
						LinearLayoutManager(requireContext())

						binding.serversList.adapter =
						serverAdapter

						return binding.root
					}

					override fun onResume() {
						super.onResume()

						libraryViewModel.reloadGames(
							requireContext(),
						)

						loadServers()
					}

					private fun loadServers() {
						if (isLoadingServers) {
							return
						}

						isLoadingServers = true

						val applicationContext =
						requireContext().applicationContext

						serverExecutor.execute {
							val result = ServerRepository.loadServers(
								applicationContext,
							)

							activity?.runOnUiThread {
								if (_binding == null) {
									isLoadingServers = false
									return@runOnUiThread
								}

								isLoadingServers = false

								if (result.servers.isEmpty()) {
									serverAdapter.submitList(
										emptyList(),
									)

									showServerLoadError(
										result.errorMessage,
									)

									return@runOnUiThread
								}

								val loadingServers = result.servers.map {
									it.copy(
										status = su.xash.engine.model.ServerStatus.Loading,
									)
								}

								serverAdapter.submitList(
									loadingServers,
								)

								if (result.loadedFromCache) {
									Toast.makeText(
										requireContext(),
												   "Exibindo a última lista salva.",
						Toast.LENGTH_LONG,
									).show()
								}

								queryServerStatuses(
									loadingServers,
								)
							}
						}
					}

					private fun queryServerStatuses(
						servers: List<ServerEntry>,
					) {
						servers.forEach { server ->
							serverExecutor.execute {
								val status =
								ServerQueryClient.query(server)

								val updatedServer =
								server.copy(status = status)

								activity?.runOnUiThread {
									if (_binding == null) {
										return@runOnUiThread
									}

									serverAdapter.updateServer(
										updatedServer,
									)
								}
							}
						}
					}

					private fun connectToServer(
						server: ServerEntry,
					) {
						val installedGames =
						libraryViewModel.installedGames.value

						val targetGame =
						installedGames?.firstOrNull { game ->
							when (server.game.lowercase()) {
								"cstrike",
								"cs16",
								"counter-strike" -> {
									game.basedir.name.equals(
										"cstrike",
								  ignoreCase = true,
									) || game.basedir.name.equals(
										"czero",
									   ignoreCase = true,
									)
								}

								"valve",
								"halflife",
								"half-life" -> {
									game.basedir.name.equals(
										"valve",
								  ignoreCase = true,
									)
								}

								else -> false
							}
						}

						if (targetGame == null) {
							showGameRequiredDialog(
								server.game,
							)

							return
						}

						targetGame.startEngine(
							requireContext(),
											   server.connectTarget,
						)
					}

					private fun showServerLoadError(
						errorMessage: String?,
					) {
						MaterialAlertDialogBuilder(
							requireContext(),
						)
						.setTitle("Servidores indisponíveis")
						.setMessage(
							errorMessage
							?: "Não foi possível carregar a lista de servidores.",
						)
						.setNegativeButton(
							"Fechar",
						 null,
						)
						.setPositiveButton(
							"Tentar novamente",
						) { _, _ ->
							loadServers()
						}
						.show()
					}

					private fun showGameRequiredDialog(
						game: String,
					) {
						val gameName = when (game.lowercase()) {
							"valve",
							"halflife",
							"half-life" -> "Half-Life"

							else -> "Counter-Strike 1.6"
						}

						MaterialAlertDialogBuilder(
							requireContext(),
						)
						.setTitle("Jogo necessário")
						.setMessage(
							"$gameName precisa estar instalado antes de conectar.",
						)
						.setPositiveButton(
							R.string.game_apk_install,
						) { _, _ ->
							startActivity(
								Intent(
									Intent.ACTION_VIEW,
			   Uri.parse(
				   "https://github.com/kiehy/xash-games/releases/download/xash/xash.zip",
			   ),
								),
							)
						}
						.setNegativeButton(
							android.R.string.cancel,
						 null,
						)
						.show()
					}

					override fun onDestroyView() {
						super.onDestroyView()
						_binding = null
					}

					override fun onDestroy() {
						serverExecutor.shutdownNow()
						super.onDestroy()
					}
}
