package io.forestframework.core.http.routing

import com.google.inject.Injector
import io.forestframework.core.Forest
import io.forestframework.core.config.ConfigProvider
import io.forestframework.core.http.HttpMethod
import io.forestframework.core.http.routing.RoutingType.HANDLER
import io.forestframework.core.modules.WebRequestHandlingModule
import io.forestframework.ext.api.DefaultStartupContext
import io.forestframework.ext.api.Extension
import io.forestframework.ext.api.StartupContext
import io.forestframework.ext.core.HttpServerExtension
import io.forestframework.testfixtures.assertStatusCode
import io.forestframework.testfixtures.get
import io.forestframework.testfixtures.runBlockingUnit
import io.forestframework.testsupport.BindFreePortExtension
import io.vertx.core.Vertx
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

data class ProducesAcceptCase(
    val description: String,
    val produces: List<String> = listOf("*/*"),
    val accept: String = "*/*",
    val expectedStatusCode: Int = 200,
    val httpMethod: HttpMethod = HttpMethod.GET
)

data class ConsumesContentTypeCase(
    val description: String,
    val consumes: List<String> = listOf("*/*"),
    val contentType: String = "*/*",
    val expectedStatusCode: Int = 200,
    val httpMethod: HttpMethod = HttpMethod.GET
)


// Put your test cases here
val producesAcceptCases = listOf(
    ProducesAcceptCase("single", produces = listOf("application/json"), accept = "application/json", expectedStatusCode = 200)
)

val consumesContentTypeCases = listOf(
    ConsumesContentTypeCase("OK", consumes = listOf("*/*"), contentType = "application/json", expectedStatusCode = 200)
)

class MediaTypeNegotiatingIntegrationTest {
    @TestFactory
    fun testProducesAccept(): Iterable<DynamicTest> = producesAcceptCases.map(this::testOne)

    private fun testOne(case: ProducesAcceptCase) = DynamicTest.dynamicTest(case.description) {
        runBlockingUnit {
            val vertx = Vertx.vertx()
            val port = startTestApplication(vertx, case.httpMethod, produces = case.produces)

            val httpClient = vertx.createHttpClient()
            httpClient.get(port, "/test", mapOf("Accept" to case.accept)).assertStatusCode(case.expectedStatusCode)
        }
    }

    /**
     * Creates a mock server, registers the routing dynamically, waits for the server startup, then returns the port
     */
    private fun startTestApplication(vertx: Vertx,
                                     httpMethod: HttpMethod = HttpMethod.GET,
                                     produces: List<String> = listOf("*/*"),
                                     consumes: List<String> = listOf("*/*")): Int {
        val configProvider = ConfigProvider(HashMap(), HashMap())
        val extensions = listOf(
            BindFreePortExtension(),
            object : Extension {
                override fun beforeInjector(startupContext: StartupContext?) {
                    startupContext!!.componentClasses.add(WebRequestHandlingModule::class.java)

                }

                override fun afterInjector(injector: Injector) {
                    injector.getInstance(RoutingManager::class.java).getRouting(HANDLER).add(
                        DefaultRouting(false,
                            HANDLER,
                            "/test",
                            "",
                            listOf(httpMethod),
                            MediaTypeNegotiatingIntegrationTest::class.java.getMethod("dummy"),
                            0,
                            produces,
                            consumes)
                    )
                }
            },
            HttpServerExtension()
        )
        val context = DefaultStartupContext(vertx, MediaTypeNegotiatingIntegrationTest::class.java, configProvider, extensions)
        Forest.run(context)

        return configProvider.getInstance("forest.http.port", Integer::class.java).toInt()
    }

    @TestFactory
    fun testConsumesContentType() = consumesContentTypeCases.map(this::testOne)

    private fun testOne(case: ConsumesContentTypeCase) = DynamicTest.dynamicTest(case.description) {
        runBlockingUnit {
            val vertx = Vertx.vertx()
            val port = startTestApplication(vertx, case.httpMethod, consumes = case.consumes)

            val httpClient = vertx.createHttpClient()
            httpClient.get(port, "/test", mapOf("Content-Type" to case.contentType)).assertStatusCode(case.expectedStatusCode)
        }
    }

    fun dummy() {}
}