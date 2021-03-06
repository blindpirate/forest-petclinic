package io.forestframework.core.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.forestframework.testfixtures.withSystemPropertyConfigFile
import io.github.glytching.junit.extension.system.SystemProperties
import io.github.glytching.junit.extension.system.SystemProperty
import io.vertx.core.VertxOptions
import io.vertx.core.http.Http2Settings
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetClientOptions
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions
import io.vertx.redis.client.RedisClientType
import io.vertx.redis.client.RedisOptions
import io.vertx.redis.client.RedisRole
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.util.Collections.emptyList
import java.util.concurrent.TimeUnit

@Suppress("UNCHECKED_CAST")
class ConfigProviderTest {
    val yamlParser = ObjectMapper(YAMLFactory())
    val jsonParser = ObjectMapper()

    @ParameterizedTest(name = "{index}")
    @ValueSource(
        strings = [
            """
aaa:
  bbb:
    ccc:
      stringValue1: ""
      stringValue2: "This is a string"
      intValue: 42
      listValue:
        - "a"
        - 42
        - "c"
    """,
            """
{
    "aaa": {
        "bbb": {

            "ccc": {
                "stringValue1": "",
                "stringValue2": "This is a string",
                "intValue": 42,
                "listValue": ["a", 42, "c" ]
            }
        }
    }
}
        """]
    )
    fun `can read raw property`(configString: String) {
        val parser = if (configString.trim().startsWith("{")) jsonParser else yamlParser
        val provider =
            ConfigProvider(parser.readValue(configString, Map::class.java) as MutableMap<String, Any>, emptyMap())
        assertEquals("", provider.getInstance("aaa.bbb.ccc.stringValue1", String::class.java))
        assertEquals("", provider.getInstance("aaa.bbb.ccc", JsonObject::class.java).getString("stringValue1"))
        assertEquals("This is a string", provider.getInstance("aaa.bbb.ccc.stringValue2", String::class.java))
        assertEquals(42, provider.getInstance("aaa.bbb.ccc.intValue", Integer::class.java))
        assertEquals(42, provider.getInstance("aaa.bbb.ccc", JsonObject::class.java).getInteger("intValue"))
        assertEquals(null, provider.getInstance("aaa.notexist", Integer::class.java))
        assertEquals(null, provider.getInstance("aaa.notexist.notexist", Boolean::class.java))
        assertEquals(null, provider.getInstance("aaa.notexist.notexist", HttpServerOptions::class.java))
    }

    val realWorldConfig = """
http:
  port: 12345
  compressionSupported: true
  initialSettings:
    pushEnabled: false
    maxHeaderListSize: 0x7ffffffe
redis:
  endpoints:
    - redis://localhost:6380
  netClientOptions:
    sslHandshakeTimeout: 1
    sslHandshakeTimeoutUnit: MINUTES
    enabledCipherSuites:
      - a
      - b
    crlPaths:
      - aaa
    enabledSecureTransportProtocols:
      - TLSv1
    tcpQuickAck: true
  role: SENTINEL
  type: CLUSTER
  maxPoolSize: 10
"""

    @Test
    fun `can read Options property`() {
        val provider = ConfigProvider(
            yamlParser.readValue(realWorldConfig, Map::class.java) as MutableMap<String, Any>,
            emptyMap()
        )

        val httpJsonObject = provider.getInstance("http", JsonObject::class.java)
        assertEquals(12345, httpJsonObject.getInteger("port"))

        val httpServerOptions: HttpServerOptions = provider.getInstance("http", HttpServerOptions::class.java)
        assertEquals(12345, httpServerOptions.port)
        assertEquals(12345, provider.getInstance("http.port", Integer::class.java))
        assertEquals(true, httpServerOptions.isCompressionSupported)
        assertEquals(true, provider.getInstance("http.compressionSupported", Boolean::class.java))
        assertEquals(false, httpServerOptions.initialSettings.isPushEnabled)
        assertEquals(false, provider.getInstance("http.initialSettings.pushEnabled", Boolean::class.java))
        assertEquals(0x7ffffffe, httpServerOptions.initialSettings.maxHeaderListSize)
        assertEquals(0x7ffffffe, provider.getInstance("http.initialSettings.maxHeaderListSize", Integer::class.java))

        provider.addDefaultOptions("redis") { RedisOptions() }

        val redisOptions: RedisOptions = provider.getInstance("redis", RedisOptions::class.java)

        assertEquals(1, redisOptions.netClientOptions.sslHandshakeTimeout)
        assertEquals(1, provider.getInstance("redis.netClientOptions.sslHandshakeTimeout", Integer::class.java))
        assertEquals(TimeUnit.MINUTES, redisOptions.netClientOptions.sslHandshakeTimeoutUnit)
        assertEquals(
            TimeUnit.MINUTES,
            provider.getInstance("redis.netClientOptions.sslHandshakeTimeoutUnit", TimeUnit::class.java)
        )
        assertEquals(listOf("a", "b"), ArrayList(redisOptions.netClientOptions.enabledCipherSuites))
        assertEquals(
            listOf("a", "b"),
            provider.getInstance("redis.netClientOptions.enabledCipherSuites", List::class.java)
        )
        assertEquals(
            listOf("a", "b"),
            provider.getInstance("redis.netClientOptions.enabledCipherSuites", ArrayList::class.java)
        )
        assertEquals(listOf("aaa"), redisOptions.netClientOptions.crlPaths)
        assertEquals(listOf("aaa"), provider.getInstance("redis.netClientOptions.crlPaths", List::class.java))
        assertEquals(listOf("aaa"), provider.getInstance("redis.netClientOptions.crlPaths", ArrayList::class.java))
        assertEquals(listOf("TLSv1"), ArrayList(redisOptions.netClientOptions.enabledSecureTransportProtocols))
        assertEquals(
            listOf("TLSv1"),
            provider.getInstance("redis.netClientOptions.enabledSecureTransportProtocols", List::class.java)
        )
        assertEquals(true, redisOptions.netClientOptions.isTcpQuickAck)
        assertEquals(true, provider.getInstance("redis.netClientOptions.tcpQuickAck", Boolean::class.java))

        assertEquals(listOf("redis://localhost:6380"), redisOptions.endpoints)
        assertEquals(listOf("redis://localhost:6380"), provider.getInstance("redis.endpoints", List::class.java))
        assertEquals(RedisRole.SENTINEL, redisOptions.role)
        assertEquals(RedisRole.SENTINEL, provider.getInstance("redis.role", RedisRole::class.java))
        assertEquals(RedisClientType.CLUSTER, redisOptions.type)
        assertEquals(RedisClientType.CLUSTER, provider.getInstance("redis.type", RedisClientType::class.java))
        assertEquals(10, redisOptions.maxPoolSize)
        assertEquals(10, provider.getInstance("redis.maxPoolSize", Integer::class.java))
    }

    @Test
    fun `return default value if not defined`() {
        val provider = ConfigProvider(emptyMap(), emptyMap())

        val httpServerOptions: HttpServerOptions = provider.getInstance("http", HttpServerOptions::class.java)
        assertEquals(HttpServerOptions.DEFAULT_PORT, httpServerOptions.port)
        assertEquals(HttpServerOptions.DEFAULT_COMPRESSION_SUPPORTED, httpServerOptions.isCompressionSupported)
        assertEquals(Http2Settings.DEFAULT_ENABLE_PUSH, httpServerOptions.initialSettings.isPushEnabled)
        assertEquals(
            Http2Settings.DEFAULT_MAX_HEADER_LIST_SIZE.toLong(),
            httpServerOptions.initialSettings.maxHeaderListSize
        )
        assertEquals(null, httpServerOptions.initialSettings.extraSettings)

        provider.addDefaultOptions("redis") { RedisOptions() }
        val redisOptions: RedisOptions = provider.getInstance("redis", RedisOptions::class.java)

        assertEquals(NetClientOptions.DEFAULT_SSL_HANDSHAKE_TIMEOUT, redisOptions.netClientOptions.sslHandshakeTimeout)
        assertEquals(
            NetClientOptions.DEFAULT_SSL_HANDSHAKE_TIMEOUT_TIME_UNIT,
            redisOptions.netClientOptions.sslHandshakeTimeoutUnit
        )
        assertEquals(emptySet<String>(), redisOptions.netClientOptions.enabledCipherSuites)
        assertEquals(emptyList<String>(), redisOptions.netClientOptions.crlPaths)
        assertEquals(
            NetClientOptions.DEFAULT_ENABLED_SECURE_TRANSPORT_PROTOCOLS,
            ArrayList(redisOptions.netClientOptions.enabledSecureTransportProtocols)
        )
        assertEquals(NetClientOptions.DEFAULT_TCP_QUICKACK, redisOptions.netClientOptions.isTcpQuickAck)

        assertEquals(listOf(RedisOptions.DEFAULT_ENDPOINT), redisOptions.endpoints)
        assertEquals(RedisRole.MASTER, redisOptions.role)
        assertEquals(RedisClientType.STANDALONE, redisOptions.type)
        assertEquals(6, redisOptions.maxPoolSize)
    }

    // https://youtrack.jetbrains.com/issue/KT-12794
    @Test
    @SystemProperties(
        SystemProperty(name = "forest.http.port", value = "12345"),
        SystemProperty(name = "forest.http.initialSettings.headerTableSize", value = "8192")
    )
    fun `environment config overwrites default value`() {
        val provider = ConfigProvider.load()

        val httpServerOptions: HttpServerOptions = provider.getInstance("http", HttpServerOptions::class.java)
        assertEquals(12345, httpServerOptions.port)
        assertEquals(8192, httpServerOptions.initialSettings.headerTableSize)
        assertEquals(HttpServerOptions.DEFAULT_COMPRESSION_SUPPORTED, httpServerOptions.isCompressionSupported)
        assertEquals(Http2Settings.DEFAULT_ENABLE_PUSH, httpServerOptions.initialSettings.isPushEnabled)

        val initialSettings: Http2Settings = provider.getInstance("http.initialSettings", Http2Settings::class.java)
        assertEquals(8192, initialSettings.headerTableSize)
    }

    private fun ConfigProvider.assertEnvironmentConfigWritesFileConfig() {
        val httpServerOptions: HttpServerOptions = getInstance("http", HttpServerOptions::class.java)
        assertEquals(12345, httpServerOptions.port)
        assertEquals(8192, httpServerOptions.initialSettings.headerTableSize)
        assertEquals(true, httpServerOptions.isCompressionSupported)
        assertEquals(false, httpServerOptions.initialSettings.isPushEnabled)

        val initialSettings: Http2Settings = getInstance("http.initialSettings", Http2Settings::class.java)
        assertEquals(8192, initialSettings.headerTableSize)

        assertEquals(8192, getInstance("http.initialSettings.headerTableSize", Integer::class.java))
    }

    @Test
    @SystemProperties(
        SystemProperty(name = "forest.http.port", value = "12345"),
        SystemProperty(name = "forest.http.initialSettings.headerTableSize", value = "8192")
    )
    fun `environment config overwrites file config for system property config file`(@TempDir tempDir: File) {
        withSystemPropertyConfigFile(tempDir, realWorldConfig) {
            ConfigProvider.load().assertEnvironmentConfigWritesFileConfig()
        }
    }

    @Test
    @SystemProperties(
        SystemProperty(name = "forest.http.port", value = "12345"),
        SystemProperty(name = "forest.http.initialSettings.headerTableSize", value = "8192")
    )
    fun `environment config overwrites file config for direct config file`(@TempDir tempDir: File) {
        val configFile = tempDir.resolve("tmp.txt").apply {
            writeText(realWorldConfig)
        }
        ConfigProvider.loadFromFileAndEnvironment(configFile).assertEnvironmentConfigWritesFileConfig()
    }

    @Test
    @SystemProperty(
        name = "forest.http", value = """
        {
            "port": 12345,
            "initialSettings": {
                 "headerTableSize": 8192
            }
        }
        """
    )
    fun `environment json config overwrites file config`(@TempDir tempDir: File) {
        withSystemPropertyConfigFile(tempDir, realWorldConfig) {
            val provider = ConfigProvider.load()

            val httpServerOptions: HttpServerOptions = provider.getInstance("http", HttpServerOptions::class.java)
            assertEquals(12345, httpServerOptions.port)
            assertEquals(8192, httpServerOptions.initialSettings.headerTableSize)
            assertEquals(true, httpServerOptions.isCompressionSupported)
            assertEquals(false, httpServerOptions.initialSettings.isPushEnabled)

            val initialSettings: Http2Settings = provider.getInstance("http.initialSettings", Http2Settings::class.java)
            assertEquals(8192, initialSettings.headerTableSize)

            assertEquals(8192, provider.getInstance("http.initialSettings.headerTableSize", Integer::class.java))
        }
    }

    @Test
    @SystemProperty(name = "forest.redis.endpoints", value = "[\"redis://localhost:6380\"]")
    fun `can parse JSON array`() {
        val provider = ConfigProvider.load()
        provider.addDefaultOptions("redis") { RedisOptions() }

        assertEquals(listOf("redis://localhost:6380"), provider.getInstance("redis.endpoints", List::class.java))
        assertEquals(
            listOf("redis://localhost:6380"),
            provider.getInstance("redis", RedisOptions::class.java).endpoints
        )
    }

    @Test
    fun `can add to config`(@TempDir tempDir: File) {
        withSystemPropertyConfigFile(tempDir, realWorldConfig) {
            val provider = ConfigProvider.load()
            provider.addConfig("http.port", "12345")
            provider.addConfig("http.initialSettings.headerTableSize", "8192")

            val httpServerOptions: HttpServerOptions = provider.getInstance("http", HttpServerOptions::class.java)
            assertEquals(12345, httpServerOptions.port)
            assertEquals(8192, httpServerOptions.initialSettings.headerTableSize)
            assertEquals(true, httpServerOptions.isCompressionSupported)
            assertEquals(false, httpServerOptions.initialSettings.isPushEnabled)

            val initialSettings: Http2Settings = provider.getInstance("http.initialSettings", Http2Settings::class.java)
            assertEquals(8192, initialSettings.headerTableSize)

            assertEquals(8192, provider.getInstance("http.initialSettings.headerTableSize", Integer::class.java))
        }
    }

    @Test
    @SystemProperty(
        name = "forest.bridge", value = """
        {
            "outboundPermitteds": [
              {
                 "addressRegex": "auction\\.[0-9]+"
              }
            ]
        }
        """
    )
    fun `can configure option list`() {
        val provider = ConfigProvider.load()

        provider.addDefaultOptions("bridge", ::SockJSBridgeOptions)
        val options = provider.getInstance("bridge", SockJSBridgeOptions::class.java)
        assertEquals(listOf("auction\\.[0-9]+"), options.outboundPermitteds.map { it.addressRegex })
    }

    class TestPOJO {
        var intValue: Int? = null
        var stringValue: String? = null
        var listValue: List<Any>? = null
        var mapValue: Map<String, Any>? = null
        var nestedValue: TestPOJO? = null
    }

    @Test
    fun `can read map to POJO`() {
        val provider = ConfigProvider(
            mapOf(
                "pojo" to
                    mapOf(
                        "intValue" to 1,
                        "stringValue" to "abc",
                        "listValue" to listOf(1, "a", 'b'),
                        "mapValue" to mapOf("a" to 1),
                        "nestedValue" to mapOf(
                            "intValue" to 1,
                            "stringValue" to "abc",
                            "listValue" to listOf(1, "a", 'b'),
                            "mapValue" to mapOf("a" to 1)
                        )
                    )
            ), mapOf()
        )

        val pojo = provider.getInstance("pojo", TestPOJO::class.java)

        assertEquals(1, pojo.intValue)
        assertEquals(1, pojo.nestedValue!!.intValue)
        assertEquals("abc", pojo.stringValue)
        assertEquals("abc", pojo.nestedValue!!.stringValue)
        assertEquals(listOf(1, "a", 'b'), pojo.listValue)
        assertEquals(listOf(1, "a", 'b'), pojo.nestedValue!!.listValue)
        assertEquals(mapOf("a" to 1), pojo.mapValue)
        assertEquals(mapOf("a" to 1), pojo.nestedValue!!.mapValue)
    }

    @Test
    fun `polymorphism setter test`(@TempDir tempDir: File) {
        withSystemPropertyConfigFile(
            tempDir,
            """
http:
  port: 8081
vertx:
  clusterManager:
    type: io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager
    zookeeperHosts: "localhost:2181"
    sessionTimeout: 20001
    connectTimeout: 3001
    rootPath: "io.vertx"
    retry:
      initialSleepTime: 101
      intervalTimes: 10001
      maxTimes: 5
  eventBusOptions:
    clusterPublicHost: localhost
    clusterPublicPort: 8082
    host: localhost
    port: 8082
"""
        ) {
            val vertxOptions = ConfigProvider.load().getInstance("vertx", VertxOptions::class.java)

            val zcm = vertxOptions.clusterManager as ZookeeperClusterManager
            assertEquals("localhost:2181", zcm.config.getString("zookeeperHosts"))
            assertEquals(20001, zcm.config.getInteger("sessionTimeout"))
            assertEquals(3001, zcm.config.getInteger("connectTimeout"))
            assertEquals(10001, zcm.config.getJsonObject("retry").getInteger("intervalTimes"))
            assertEquals("localhost", vertxOptions.eventBusOptions.clusterPublicHost)
            assertEquals("localhost", vertxOptions.eventBusOptions.host)
            assertEquals(8082, vertxOptions.eventBusOptions.port)
            assertEquals(8082, vertxOptions.eventBusOptions.clusterPublicPort)
        }
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_11)
    fun `use environmental variables`(@TempDir dir: File) {
        val mainJava = dir.resolve("Main.java")
        mainJava.writeText(
            """
            import io.forestframework.core.config.ConfigProvider;

            public class Main {
                public static void main(String[] args) {
                    ConfigProvider cp = ConfigProvider.load();
                    System.out.println("aaa.bbb=" + cp.getInstance("aaa.bbb", String.class));
                    System.out.println("aaa.ccc=" + cp.getInstance("aaa.ccc", String.class));
                    System.out.println("aaa.d_d_d=" + cp.getInstance("aaa.d_d_d", String.class));
                }
            }
        """.trimIndent()
        )

        val configFile = dir.resolve("config.json")
        configFile.writeText(
            """
            {
                "aaa": {
                    "bbb": 1,
                    "ccc": "hello"
                }
            }
        """.trimIndent()
        )

        val javaBin = System.getProperty("java.home") + "/bin/java"
        val classPath = System.getProperty("java.class.path")

        val output = dir.resolve("output.txt")
        ProcessBuilder(javaBin, "-cp", classPath, mainJava.absolutePath).apply {
            directory(dir)
            environment()["FOREST_config_file"] = configFile.absolutePath
            environment()["FOREST_aaa_ccc"] = "hi"
            environment()["FOREST_aaa_d__d__d"] = "ValueWithEscapedUnderscore"
        }.redirectOutput(output).redirectError(output).start().waitFor()

        val outputText = output.readText()
        MatcherAssert.assertThat(outputText, CoreMatchers.containsString("aaa.bbb=1"))
        MatcherAssert.assertThat(outputText, CoreMatchers.containsString("aaa.ccc=hi"))
        MatcherAssert.assertThat(outputText, CoreMatchers.containsString("aaa.d_d_d=ValueWithEscapedUnderscore"))
    }
}
