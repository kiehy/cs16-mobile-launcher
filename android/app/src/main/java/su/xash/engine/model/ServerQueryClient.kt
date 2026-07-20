package su.xash.engine.model

import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToLong

object ServerQueryClient {

	private const val TIMEOUT_MS = 3_000

	private val baseQuery = byteArrayOf(
		0xFF.toByte(),
										0xFF.toByte(),
										0xFF.toByte(),
										0xFF.toByte(),
										0x54,
	) + "Source Engine Query\u0000".toByteArray(Charsets.ISO_8859_1)

	fun query(server: ServerEntry): ServerStatus {
		return try {
			queryInternal(server)
		} catch (exception: Exception) {
			ServerStatus.Offline(
				reason = exception.message ?: "Sem resposta",
			)
		}
	}

	private fun queryInternal(server: ServerEntry): ServerStatus {
		val address = InetAddress.getByName(server.ip)

		DatagramSocket().use { socket ->
			socket.soTimeout = TIMEOUT_MS

			val startedAt = System.nanoTime()

			sendPacket(
				socket = socket,
			  address = address,
			  port = server.port,
			  data = baseQuery,
			)

			var response = receivePacket(socket)

			if (isChallengeResponse(response)) {
				val challenge = response.copyOfRange(5, 9)

				val queryWithChallenge =
				baseQuery + challenge

				sendPacket(
					socket = socket,
			   address = address,
			   port = server.port,
			   data = queryWithChallenge,
				)

				response = receivePacket(socket)
			}

			val elapsedMilliseconds =
			(System.nanoTime() - startedAt) / 1_000_000.0

			val ping = elapsedMilliseconds
			.roundToLong()
			.coerceAtLeast(1L)

			return parseResponse(
				response = response,
				ping = ping,
			)
		}
	}

	private fun sendPacket(
		socket: DatagramSocket,
		address: InetAddress,
		port: Int,
		data: ByteArray,
	) {
		val packet = DatagramPacket(
			data,
			data.size,
			address,
			port,
		)

		socket.send(packet)
	}

	private fun receivePacket(
		socket: DatagramSocket,
	): ByteArray {
		val buffer = ByteArray(65_535)

		val packet = DatagramPacket(
			buffer,
			buffer.size,
		)

		socket.receive(packet)

		return packet.data.copyOf(packet.length)
	}

	private fun isChallengeResponse(
		response: ByteArray,
	): Boolean {
		return response.size >= 9 &&
		hasStandardHeader(response) &&
		response[4].toInt() and 0xFF == 0x41
	}

	private fun parseResponse(
		response: ByteArray,
		ping: Long,
	): ServerStatus {
		if (response.size < 5) {
			throw IllegalStateException(
				"Resposta inválida",
			)
		}

		if (!hasStandardHeader(response)) {
			throw IllegalStateException(
				"Cabeçalho inválido",
			)
		}

		val responseType =
		response[4].toInt() and 0xFF

		return when (responseType) {
			0x49 -> parseSourceResponse(
				response = response,
				ping = ping,
			)

			0x6D -> parseGoldSourceResponse(
				response = response,
				ping = ping,
			)

			else -> {
				throw IllegalStateException(
					"Resposta desconhecida: 0x${responseType.toString(16)}",
				)
			}
		}
	}

	private fun parseSourceResponse(
		response: ByteArray,
		ping: Long,
	): ServerStatus {
		val reader = PacketReader(
			response,
			startPosition = 5,
		)

		reader.readUnsignedByte()

		val serverName = reader.readString()
		val map = reader.readString()

		reader.readString()
		reader.readString()

		reader.readUnsignedShortLittleEndian()

		val players = reader.readUnsignedByte()
		val maxPlayers = reader.readUnsignedByte()
		val bots = reader.readUnsignedByte()

		return ServerStatus.Online(
			serverName = serverName,
			map = map,
			players = players,
			maxPlayers = maxPlayers,
			bots = bots,
			ping = ping,
		)
	}

	private fun parseGoldSourceResponse(
		response: ByteArray,
		ping: Long,
	): ServerStatus {
		val reader = PacketReader(
			response,
			startPosition = 5,
		)

		reader.readString()

		val serverName = reader.readString()
		val map = reader.readString()

		reader.readString()
		reader.readString()

		val players = reader.readUnsignedByte()
		val maxPlayers = reader.readUnsignedByte()

		reader.readUnsignedByte()
		reader.readUnsignedByte()

		reader.readUnsignedByte()
		reader.readUnsignedByte()

		val isMod = reader.readUnsignedByte()

		if (isMod == 1) {
			reader.skipGoldSourceModInfo()
		}

		reader.readUnsignedByte()
		val bots = reader.readUnsignedByte()

		return ServerStatus.Online(
			serverName = serverName,
			map = map,
			players = players,
			maxPlayers = maxPlayers,
			bots = bots,
			ping = ping,
		)
	}

	private fun hasStandardHeader(
		response: ByteArray,
	): Boolean {
		return response.size >= 4 &&
		response[0] == 0xFF.toByte() &&
		response[1] == 0xFF.toByte() &&
		response[2] == 0xFF.toByte() &&
		response[3] == 0xFF.toByte()
	}

	private class PacketReader(
		private val data: ByteArray,
			startPosition: Int,
	) {
		private var position = startPosition

		fun readUnsignedByte(): Int {
			ensureAvailable(1)

			return data[position++].toInt() and 0xFF
		}

		fun readUnsignedShortLittleEndian(): Int {
			ensureAvailable(2)

			val result = ByteBuffer
			.wrap(data, position, 2)
			.order(ByteOrder.LITTLE_ENDIAN)
			.short
			.toInt() and 0xFFFF

			position += 2

			return result
		}

		fun readString(): String {
			val output = ByteArrayOutputStream()

			while (position < data.size) {
				val byte = data[position++]

				if (byte.toInt() == 0) {
					break
				}

				output.write(byte.toInt())
			}

			return output
			.toByteArray()
			.toString(Charsets.ISO_8859_1)
			.trim()
		}

		fun skipGoldSourceModInfo() {
			readString()
			readString()

			ensureAvailable(1)
			position += 1

			ensureAvailable(4)
			position += 4

			ensureAvailable(4)
			position += 4

			ensureAvailable(1)
			position += 1

			ensureAvailable(1)
			position += 1
		}

		private fun ensureAvailable(
			amount: Int,
		) {
			if (position + amount > data.size) {
				throw IllegalStateException(
					"Resposta incompleta",
				)
			}
		}
	}
}
