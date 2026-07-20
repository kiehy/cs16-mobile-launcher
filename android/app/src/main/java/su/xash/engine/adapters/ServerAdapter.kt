package su.xash.engine.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import su.xash.engine.databinding.CardServerBinding
import su.xash.engine.model.ServerEntry
import su.xash.engine.model.ServerStatus

class ServerAdapter(
	private val onConnect: (ServerEntry) -> Unit,
) : RecyclerView.Adapter<ServerAdapter.ServerViewHolder>() {

	private val servers =
	mutableListOf<ServerEntry>()

	fun submitList(
		newServers: List<ServerEntry>,
	) {
		servers.clear()
		servers.addAll(newServers)
		notifyDataSetChanged()
	}

	fun updateServer(
		updatedServer: ServerEntry,
	) {
		val position = servers.indexOfFirst {
			it.id == updatedServer.id
		}

		if (position == -1) {
			return
		}

		servers[position] = updatedServer
		notifyItemChanged(position)
	}

	override fun onCreateViewHolder(
		parent: ViewGroup,
		viewType: Int,
	): ServerViewHolder {
		val binding = CardServerBinding.inflate(
			LayoutInflater.from(parent.context),
												parent,
										  false,
		)

		return ServerViewHolder(binding)
	}

	override fun onBindViewHolder(
		holder: ServerViewHolder,
		position: Int,
	) {
		holder.bind(servers[position])
	}

	override fun getItemCount(): Int =
	servers.size

	inner class ServerViewHolder(
		private val binding: CardServerBinding,
	) : RecyclerView.ViewHolder(binding.root) {

		fun bind(
			server: ServerEntry,
		) {
			binding.serverName.text =
			server.name

			binding.serverAddress.text =
			server.connectTarget

			when (val status = server.status) {
				ServerStatus.Loading -> {
					binding.serverStatus.text =
					"Consultando servidor..."

					binding.serverDetails.text =
					""

					binding.serverDetails.visibility =
					View.GONE

					binding.connectButton.isEnabled =
					false
				}

				is ServerStatus.Online -> {
					binding.serverStatus.text =
					"🟢 Online"

					binding.serverDetails.visibility =
					View.VISIBLE

					binding.serverDetails.text =
					"${status.map}  •  " +
					"${status.players}/${status.maxPlayers} jogadores  •  " +
					"${status.ping} ms"

					binding.connectButton.isEnabled =
					true
				}

				is ServerStatus.Offline -> {
					binding.serverStatus.text =
					"🔴 Offline"

					binding.serverDetails.visibility =
					View.VISIBLE

					binding.serverDetails.text =
					status.reason

					binding.connectButton.isEnabled =
					false
				}
			}

			if (server.description.isBlank()) {
				binding.serverDescription.visibility =
				View.GONE
			} else {
				binding.serverDescription.visibility =
				View.VISIBLE

				binding.serverDescription.text =
				server.description
			}

			binding.connectButton.setOnClickListener {
				if (server.status is ServerStatus.Online) {
					onConnect(server)
				}
			}

			binding.root.setOnClickListener {
				if (server.status is ServerStatus.Online) {
					onConnect(server)
				}
			}
		}
	}
}
