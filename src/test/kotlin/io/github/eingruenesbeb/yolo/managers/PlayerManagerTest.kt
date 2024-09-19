package io.github.eingruenesbeb.yolo.managers

// Uncomment when generating randomized test data.
import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import io.github.eingruenesbeb.yolo.Yolo
import io.github.eingruenesbeb.yolo.player.GhostState
import io.github.eingruenesbeb.yolo.player.ReviveResult
import io.github.eingruenesbeb.yolo.player.YoloPlayer
import io.github.eingruenesbeb.yolo.player.YoloPlayerData
import io.github.eingruenesbeb.yolo.serialize.MiniMessageKSerializer
import io.github.eingruenesbeb.yolo.serialize.NullableLocationKSerializer
import kotlinx.serialization.*
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.random.nextInt


class PlayerManagerTest {
    private var serverMock: ServerMock? = null

    @BeforeEach
    fun setUp() {
        serverMock = MockBukkit.mock()
        serverMock!!.addSimpleWorld("world")
        serverMock!!.addSimpleWorld("world_the_end")
        serverMock!!.addSimpleWorld("world_nether")
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
        serverMock = null
    }

    @Test
    fun testDataLoad() {
        // Generated test data
        File("src/test/resources/examplePlayerData").copyRecursively(File(serverMock!!.pluginsFolder.path.plus("/Yolo-${Yolo.VERSION}/player_data")))

        val yolo = MockBukkit.load(Yolo::class.java)

        val loadedData = yolo.playerManager.playerRegistry.values.map { it.yoloPlayerData }.sortedBy { it.uuid }
        @OptIn(ExperimentalSerializationApi::class)
        val deserializedTestPlayerData = buildList {
            File("src/test/resources/examplePlayerData").listFiles()!!.forEach {
                this.add(Cbor.decodeFromByteArray(YoloPlayerData.serializer(), it.readBytes()))
            }
        }.sortedBy { it.uuid }

        assert(loadedData == deserializedTestPlayerData) /*{
            "Loaded Data:\n$loadedData\nActual data:\n$deserializedTestPlayerData"
        }*/
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testLegacyDataLoad() {
        File("src/test/resources/exampleLegacyPlayerData/yolo_player_data.json").copyTo(File(serverMock!!.pluginsFolder.path.plus("/Yolo-${Yolo.VERSION}/data/yolo_player_data.json")))
        val deserializedConvertedData = buildList {
            File("src/test/resources/exampleLegacyPlayerData/converted").listFiles()!!.forEach {
                this.add(Cbor.decodeFromByteArray(YoloPlayerData.serializer(), it.readBytes()))
            }
        }.sortedBy { it.uuid }

        val yolo = MockBukkit.load(Yolo::class.java)

        val loadedData = yolo.playerManager.playerRegistry.values.map { it.yoloPlayerData }.sortedBy { it.uuid }

        assert(loadedData == deserializedConvertedData) /*{
            "Loaded Data:\n$loadedData\nActual data:\n$deserializedConvertedData"
        }*/
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testAutoSave() {
        val testData = buildList {
            (0..99).forEach { _ ->
                this.add(newRandomPlayerEntry())
            }
        }.toMutableList()

        val yolo = MockBukkit.load(Yolo::class.java)

        yolo.playerManager.playerRegistry.putAll(testData.associate { it.uuid to YoloPlayer(it) })
        with(newRandomPlayerEntry()) {
            testData.add(this)
            yolo.playerManager.playerRegistry[this.uuid] = YoloPlayer(this)
        }

        TimeUnit.SECONDS.sleep(1)  // This shouldn't take longer than 3 seconds.

        val savedDataDeserialized = buildList {
            File(yolo.dataFolder.path.plus("/player_data")).listFiles()!!.forEach {
                this.add(Cbor.decodeFromByteArray(YoloPlayerData.serializer(), it.readBytes()))
            }
        }

        assert(testData.sortedBy { it.uuid } == savedDataDeserialized.sortedBy { it.uuid }) /*{
            "Loaded Data:\n$testData\nActual data:\n$savedDataDeserialized"
        }*/
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testAllPlayerSave() {
        val testData = buildList {
            (0..99).forEach { _ ->
                this.add(newRandomPlayerEntry())
            }
        }.sortedBy { it.uuid }

        val yolo = MockBukkit.load(Yolo::class.java)
        val playerDataFolder = File(yolo.dataFolder.path.plus("/player_data"))

        yolo.playerManager.playerRegistry.putAll(testData.associate { it.uuid to YoloPlayer(it) })
        yolo.playerManager.saveAllPlayerData()

        val savedDataDeserialized = buildList {
            playerDataFolder.listFiles()!!.forEach {
                this.add(Cbor.decodeFromByteArray(YoloPlayerData.serializer(), it.readBytes()))
            }
        }.sortedBy { it.uuid }

        assert(testData == savedDataDeserialized) /*{
            "Loaded Data:\n${testData.sortedBy { it.uuid }}\nActual data:\n${savedDataDeserialized.sortedBy { it.uuid }}"
        }*/
    }

    companion object {
        //Only use once to generate test data.
        @Suppress("unused")
        @JvmStatic
        //@BeforeAll
        @OptIn(ExperimentalSerializationApi::class)
        fun createRandomPlayerData() {
            MockBukkit.mock()
            MockBukkit.getMock()!!.addSimpleWorld("world")
            MockBukkit.getMock()!!.addSimpleWorld("world_the_end")
            MockBukkit.getMock()!!.addSimpleWorld("world_nether")

            MockBukkit.load(Yolo::class.java)
            // Directory to store the generated .cbor files
            val outputDir = File("src/test/resources/examplePlayerData")
            if (!outputDir.exists()) {
                outputDir.mkdir()
            }

            // Generate example data
            val exampleDataList = buildList {
                (0..100).forEach { _ ->
                    this.add(newRandomPlayerEntry())
                }
            }

            for (playerData in exampleDataList) {
                val uuid = playerData.uuid
                val serializedData = Cbor.encodeToByteArray(playerData)
                val outputFile = File(outputDir, "$uuid.cbor")
                outputFile.writeBytes(serializedData)
                println("Serialized data for player $uuid to ${outputFile.absolutePath}")
            }
        }

        // Function to create an example YoloPlayerData instance
        private fun newRandomPlayerEntry(): YoloPlayerData {
            val uuid = UUID.randomUUID()
            return YoloPlayerData(
                uuid,
                randomLocationOrNull(),
                Random.Default.nextBoolean(),
                Random.Default.nextBoolean(),
                Random.Default.nextBoolean(),
                Random.Default.nextBoolean(),
                Component.text(":3"),
                Random.Default.nextBoolean(),
                GhostState(uuid, Random.nextBoolean(), Random.nextInt(0..600)),
                randomReviveResultList()
            )
        }

        private fun randomLocationOrNull(): Location? {
            if (Random.Default.nextFloat() <= 0.3) return null
            else {
                val world = MockBukkit.getMock()!!.worlds.random()
                val x = Random.Default.nextDouble(-world.worldBorder.size - world.worldBorder.center.x, world.worldBorder.size - world.worldBorder.center.x)
                val y = Random.Default.nextDouble(world.minHeight.toDouble(), world.maxHeight.toDouble())
                val z = Random.Default.nextDouble(-world.worldBorder.size - world.worldBorder.center.z, world.worldBorder.size - world.worldBorder.center.z)
                return Location(world, x, y, z)
            }
        }

        private fun randomReviveResultList(): MutableList<ReviveResult> {
            return buildList {
                (0..Random.Default.nextInt(51)).forEach { _ ->
                    this.add(ReviveResult(Random.Default.nextBoolean(), Random.Default.nextBoolean(), Random.Default.nextBoolean(), randomLocationOrNull()))
                }
            }.toMutableList()
        }

        @OptIn(ExperimentalSerializationApi::class)
        @JvmStatic
        //@BeforeAll
        @Suppress("unused")
        fun generateLegacyPlayerData() {
            MockBukkit.mock()
            MockBukkit.getMock()!!.addSimpleWorld("world")
            MockBukkit.getMock()!!.addSimpleWorld("world_the_end")
            MockBukkit.getMock()!!.addSimpleWorld("world_nether")
            MockBukkit.load(Yolo::class.java)

            with(MockBukkit.getMock()!!) {
                this.addSimpleWorld("world")
                this.addSimpleWorld("world_the_end")
                this.addSimpleWorld("world_nether")
            }

            val outputDir = File("src/test/resources/exampleLegacyPlayerData")
            if (!outputDir.exists()) {
                outputDir.mkdir()
            }

            val randomizedLegacyData = LegacyYoloPlayerData(buildMap {
                (0..99).forEach { _ ->
                    val uuid = UUID.randomUUID()
                    this[uuid.toString()] = LegacyPlayerState(
                        randomLocationOrNull(),
                        Random.Default.nextBoolean(),
                        Random.Default.nextBoolean(),
                        Random.Default.nextBoolean(),
                        Random.Default.nextBoolean(),
                        Component.text(">:3"),
                        LegacyGhostState(
                            Random.Default.nextBoolean(),
                            Random.Default.nextInt(0..600).toLong(),
                            uuid
                        )
                    )
                }
            })

            val randomizedConvertedData = randomizedLegacyData.data.map { (uuidString, legacyData) ->
                legacyData.toNewFormat(UUID.fromString(uuidString))
            }

            File(outputDir.path.plus("/yolo_player_data.json")).writeText(Json.encodeToString(randomizedLegacyData))

            with(File(outputDir.path.plus("/converted"))) {
                if (!this.exists()) this.mkdir()
            }

            randomizedConvertedData.forEach {
                File(outputDir.path.plus("/converted/${it.uuid}.cbor")).writeBytes(Cbor.encodeToByteArray(it))
            }
        }

        @Serializable
        private data class LegacyYoloPlayerData(val data: Map<String, LegacyPlayerState>)

        @Serializable
        private data class LegacyGhostState(
            var enabled: Boolean = false,
            private var ticksLeft: Long = 0,
            @Transient private val attachedPlayerID: UUID? = null
        ) {
            fun toNewFormat(): GhostState = GhostState(attachedPlayerID!!, enabled, ticksLeft.toInt())
        }

        @Serializable
        private data class LegacyPlayerState(
            @Serializable(with= NullableLocationKSerializer::class) var latestDeathPos: Location?,
            var isDead: Boolean = false,
            var isToRevive: Boolean = false,
            var isTeleportToDeathPos: Boolean = true,
            var isRestoreInventory: Boolean = true,
            @Serializable(with = MiniMessageKSerializer::class) var banMessage: Component = Component.text(""),
            val ghostState: LegacyGhostState = LegacyGhostState(false, 0)
        ) {
            fun toNewFormat(uuid: UUID): YoloPlayerData = YoloPlayerData(
                uuid,
                latestDeathPos,
                isDead,
                isToRevive,
                isTeleportToDeathPos,
                isRestoreInventory,
                banMessage,
                false,
                ghostState.toNewFormat(),
                mutableListOf()
            )
        }
    }
}
