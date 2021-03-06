package io.forestframework.ext.core

import io.forestframework.core.ForestApplication
import io.forestframework.core.http.result.GetPlainText
import io.forestframework.testfixtures.AbstractForestIntegrationTest
import io.forestframework.testfixtures.DisableAutoScan
import io.forestframework.testfixtures.runBlockingUnit
import io.forestframework.testsupport.ForestIntegrationTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.StringContains.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

@AutoStaticResourceScan
@ForestApplication
class StaticResourceExtensionTestApplication

@ExtraConfig(value = ["static.webroot=StaticResourceTestData"])
@ForestIntegrationTest(appClass = StaticResourceExtensionTestApplication::class)
@DisableAutoScan
class AutoStaticResourceScanExtensionIntegrationTest : AbstractForestIntegrationTest() {
    @ParameterizedTest(name = "can get resource {0}")
    @CsvSource(value = [
        "/css/a/a.css, text/css;charset=UTF-8,                   this is a.css",
        "/css/b.css,   text/css;charset=UTF-8,                   this is b.css",
        "/img/1.png,   image/png,                                BINARY",
        "/img/2.jpg,   image/jpeg,                               BINARY",
        "/js/1.js,     text/javascript;charset=UTF-8,     this is 1.js",
        "/js/2.js,     text/javascript;charset=UTF-8,     this is 2.js",
        "/index.html,  text/html;charset=UTF-8,                  This is HTML",
        "/,            text/html;charset=UTF-8,                  This is HTML",
        "/favicon.ico, image/x-icon,                             BINARY"
    ])
    fun `can get resources`(path: String, expectedContentType: String, expectedContent: String) = runBlockingUnit {
        get(path)
            .assert200()
            .assertContentType(expectedContentType)
            .apply {
                if (expectedContent != "BINARY") {
                    assertThat(bodyAsString(), containsString(expectedContent))
                } else {
                    // At least 100 bytes
                    assertTrue(bodyAsBinary().length() > 100)
                }
            }
    }

    @ParameterizedTest
    @ValueSource(strings = ["/inexisitent", "/js/inexistent", "/css/a/a.css/1"])
    fun `get 404 when resource not found`(path: String) = runBlockingUnit {
        get(path).assert404()
    }
}

@ExtraConfig(value = ["""static.webroots=[
                "StaticResourceTestData/img",
                "StaticResourceTestData/js",
                "StaticResourceTestData/css"
            ]
        """])
@ForestIntegrationTest(appClass = StaticResourceExtensionTestApplication::class)
@DisableAutoScan
class AutoStaticResourceScanExtensionMultipleWebrootsIntegrationTest : AbstractForestIntegrationTest() {
    @ParameterizedTest(name = "can get resource {0}")
    @CsvSource(value = [
        "/a/a.css, text/css;charset=UTF-8,                this is a.css",
        "/b.css,   text/css;charset=UTF-8,                this is b.css",
        "/1.png,   image/png,                             BINARY",
        "/2.jpg,   image/jpeg,                            BINARY",
        "/1.js,     text/javascript;charset=UTF-8, this is 1.js",
        "/2.js,     text/javascript;charset=UTF-8, this is 2.js"
    ])
    fun `can get resources`(path: String, expectedContentType: String, expectedContent: String): Unit = runBlockingUnit {
        get(path)
            .assert200()
            .assertContentType(expectedContentType)
            .apply {
                if (expectedContent != "BINARY") {
                    assertThat(bodyAsString(), containsString(expectedContent))
                }
            }
    }

    @ParameterizedTest(name = "get 404 on {0}")
    @ValueSource(strings = [
        "/css/a/a.css",
        "/css/b.css",
        "/img/1.png",
        "/img/2.jpg",
        "/js/1.js",
        "/js/2.js",
        "/inexisitent",
        "/js/inexistent",
        "/css/a/a.css/",
        "/",
        "/index.html",
        "favicon.ico"
    ])
    fun `get 404 when resource not found`(path: String) = runBlockingUnit {
        get(path).assert404()
    }
}

@ForestIntegrationTest(appClass = RouterWithPredefinedRoot::class)
class AutoStaticResourceScanExtensionTestApplicationWithPredefinedRoot : AbstractForestIntegrationTest() {
    @Test
    fun `index_html is not re-registered for root`() = runBlockingUnit {
        assertEquals("HelloWorld", get("/").bodyAsString())
    }
}

@AutoStaticResourceScan
@ForestApplication
class RouterWithPredefinedRoot {
    @GetPlainText("/")
    fun index() = "HelloWorld"
}
