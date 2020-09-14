package io.forestframework.core.http.routing;

import com.google.inject.Inject;
import io.forestframework.core.ForestApplication;
import io.forestframework.core.http.HttpMethod;
import io.forestframework.core.http.HttpStatusCode;
import io.forestframework.core.http.Router;
import io.forestframework.ext.core.IncludeComponents;
import io.forestframework.testfixtures.DisableAutoScan;
import io.forestframework.testsupport.ForestExtension;
import io.forestframework.testsupport.ForestTest;
import io.vertx.core.http.HttpServerResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@ForestApplication
class App {
}

@Router("/**")
class Default4xxErrorHandler extends AbstractTraceableRouter {
    @Route(path = "/METHOD_NOT_ALLOWED", type = RoutingType.HANDLER, methods = HttpMethod.POST)
    public void handle405(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write(Message.HANDLER.name());
    }

    @Route(path = "/1", type = RoutingType.HANDLER, produces = {"application/json"}, consumes = {"application/json"})
    public void handler1(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write("1");
    }

    @Route(path = "/2", type = RoutingType.HANDLER, produces = {"text/*"}, consumes = {"application/json", "image/jxr"})
    public void handler2(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write("2");
    }

    @Route(path = "/3", type = RoutingType.HANDLER, produces = {"application/*"})
    public void handler3(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write("3");
    }

    @Route(path = "/4", type = RoutingType.HANDLER)
    public void handler4(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write("4");
    }

    @Route(path = "/5", type = RoutingType.HANDLER, produces = {"application/json", "text/plain"})
    public void handler5(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write("5");
    }
}

@Router("/custom")
class CustomErrorHandler extends AbstractTraceableRouter {
    @Route(path = "/METHOD_NOT_ALLOWED", type = RoutingType.HANDLER, methods = HttpMethod.POST)
    public void handle405(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write(Message.HANDLER.name());
    }

    @Route(path = "/NOT_ACCEPTABLE", type = RoutingType.HANDLER, produces = {"application/json;charset=UTF-8", "text/plain"})
    public void handler406(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write(Message.HANDLER.name());
    }

    @Route(path = "/UNSUPPORTED_MEDIA_TYPE", type = RoutingType.HANDLER, consumes = "application/json")
    public void handler415(HttpServerResponse response) {
        addToTrace("UNSUPPORTED_MEDIA_TYPE");
        response.write(Message.HANDLER.name());
    }

    @OnError(value = "/METHOD_NOT_ALLOWED", statusCode = HttpStatusCode.METHOD_NOT_ALLOWED)
    public void on405(HttpServerResponse response, Throwable e) {
        response.write(Message.CUSTOM_405_ERROR_HANDLER.name());
        response.write("  e.getMessage():  " + e.getMessage());
        addToTrace(Message.CUSTOM_405_ERROR_HANDLER.name());
    }

    @OnError(value = "/NOT_ACCEPTABLE", statusCode = HttpStatusCode.NOT_ACCEPTABLE)
    public void on406(HttpServerResponse response, Throwable e) {
        response.write(Message.CUSTOM_406_ERROR_HANDLER.name());
        response.write("  e.getMessage():  " + e.getMessage());
        addToTrace(Message.CUSTOM_406_ERROR_HANDLER.name());
    }

    @OnError(value = "/UNSUPPORTED_MEDIA_TYPE", statusCode = HttpStatusCode.UNSUPPORTED_MEDIA_TYPE)
    public void on415(HttpServerResponse response, Throwable e) {
        response.write(Message.CUSTOM_415_ERROR_HANDLER.name());
        response.write("  e.getMessage():  " + e.getMessage());
        addToTrace(Message.CUSTOM_415_ERROR_HANDLER.name());
    }
}

@ExtendWith(ForestExtension.class)
@ForestTest(appClass = App.class)
@DisableAutoScan
@IncludeComponents(classes = {
        Default4xxErrorHandler.class,
        CustomErrorHandler.class
})
public class Test405406415 extends AbstractMultipleRoutersIntegrationTest {

    @Inject
    void setRouter(Default4xxErrorHandler default4xxErrorHandler, CustomErrorHandler customErrorHandler) {
        this.addToRouters(default4xxErrorHandler, customErrorHandler);
    }

    @Test
    void get405AndHandledByDefaultErrorHandler() throws IOException {
        String result = sendHttpRequest("GET", "/METHOD_NOT_ALLOWED").assert405().getBody();

        assertThat(getTraces(), equalTo(Collections.emptyList()));
        assertThat(result, equalTo(HttpStatusCode.METHOD_NOT_ALLOWED.name()));
    }

    @Test
    void get406WhenProducesAndAcceptSingle() throws IOException {
        // produces = "application/json"   accept = "text/html"
        String result = sendHttpRequest("POST", "/1", "text/html", "text/html").assert406().getBody();

        assertThat(getTraces(), equalTo(Collections.emptyList()));
        assertThat(result, equalTo(HttpStatusCode.NOT_ACCEPTABLE.name()));
    }

    @Test
    void get200WhenProducesAndAcceptSingle() throws IOException {
        // produces = "application/json"   accept = "application/json"
        String result = sendHttpRequest("POST", "/1", "application/json", "application/json").assert200().getBody();

        assertThat(getTraces(), equalTo(Collections.singletonList(Message.HANDLER.name())));
        assertThat(result, containsString("1"));
    }

    @ParameterizedTest
    @CsvSource({
            "2, text/html",
            "3, text/html, application/json;level=1;q=0.9, text/plain;q=0.8",
            "1, text/*"
    })
    void get406WhenWildcard(String number, String accept) throws IOException {
        // produces = "text/*"                accept="text/html"
        // produces = "application/*"         accept="text/html, application/json; level=1; q=0.9, text/plain; q=0.8"
        // produces = "application/json"      accept="text/*"
        String path = "/" + number;
        String result = sendHttpRequest("POST", path, accept, "text/html").assert406().getBody();

        assertThat(getTraces(), equalTo(Collections.emptyList()));
        assertThat(result, equalTo(HttpStatusCode.NOT_ACCEPTABLE.name()));
    }

    @Test
    void get200WhenWildcard() throws IOException {
        // produces = "application/json"      accept = "application/*"
        String result = sendHttpRequest("POST", "/1", "application/*", "application/json").assert200().getBody();

        assertThat(getTraces(), equalTo(Collections.singletonList(Message.HANDLER.name())));
        assertThat(result, containsString("1"));
    }

    @ParameterizedTest
    @CsvSource({
            "4, text/*",
            "4, application/xhtml+xml;q=0.7"
    })
    void get406WhenDoubleWildcard(String number, String accept) throws IOException {
        // produces = "*/*"  accept = "text/*"
        // produces = "*/*"  accept = "application/xhtml+xml;q=0.7"
        String path = "/" + number;
        String result = sendHttpRequest("POST", path, accept, "text/html").assert406().getBody();

        assertThat(getTraces(), equalTo(Collections.emptyList()));
        assertThat(result, equalTo(HttpStatusCode.NOT_ACCEPTABLE.name()));
    }

    @ParameterizedTest
    @CsvSource({
            "4, */*;level=1;q=0.9",
            "2, */*;q=0.10"
    })
    void get200WhenDoubleWildcard(String number, String accept) throws IOException {
        // produces = "*/*"  accept = "*/*;level=1;q=0.9"
        // produces = "*/*"  accept = "*/*;q=0.1"
        String path = "/" + number;
        String result = sendHttpRequest("POST", path, accept, "application/json, image/jxr").assert200().getBody();

        assertThat(getTraces(), equalTo(Collections.singletonList(Message.HANDLER.name())));
        assertThat(result, containsString(number));
    }

    @Test
    void get406WhenProducesAndAcceptsMultiple() throws IOException {
        // produces = "application/json, text/plain"      accept = "application/xhtml+xml, text/plain; level=2"
        String result = sendHttpRequest("POST", "/5", "application/xhtml+xml, text/plain;level=2", "text/html").assert406().getBody();

        assertThat(getTraces(), equalTo(Collections.emptyList()));
        assertThat(result, equalTo(HttpStatusCode.NOT_ACCEPTABLE.name()));
    }

    @Test
    void get200WhenProducesAndAcceptsMultiple() throws IOException {
        // produces = "application/json, text/plain"     accept = "text/html, application/json; q=0.9, text/plain; q=0.8"
        String result = sendHttpRequest("POST", "/5", "text/html, application/json; q=0.9, text/plain; q=0.8", "text/html").assert200().getBody();

        assertThat(getTraces(), equalTo(Collections.singletonList(Message.HANDLER.name())));
        assertThat(result, containsString("5"));
    }

    @ParameterizedTest
    @CsvSource({
            "1, application/*",
            "1, text/*",
            "1, text/html",
            "2, application/*"
    })
    void get415ErrorAndHandledByDefaultErrorHandlerWhenConsumesNotMatched(String number, String contentType) throws IOException {
        // consumes = "application/json"               contentType = "application/*"
        // consumes = "application/json"               contentType = "text/*"
        // consumes = "application/json"               contentType = "text/html"
        // consumes = "application/json, image/jxr"    contentType = "application/*"

        String path = "/" + number;
        String result = sendHttpRequest("POST", path, "*/*", contentType).assert415().getBody();

        assertThat(getTraces(), equalTo(Collections.emptyList()));
        assertThat(result, equalTo(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE.name()));
    }

    @ParameterizedTest
    @CsvSource({
            "3, application/json",
            "3, text/*",
    })
    void get200WhenConsumesMatched(String number, String contentType) throws IOException {
        // consumes = "*/*"               contentType = "application/json"
        // consumes = "*/*"               contentType = "text/*"
        String path = "/" + number;
        String result = sendHttpRequest("POST", path, "*/*", contentType).assert200().getBody();

        assertThat(getTraces(), equalTo(Collections.singletonList(Message.HANDLER.name())));
        assertThat(result, containsString(number));
    }

    @Test
    void test405ErrorHandledByCustom405ErrorHandler() throws IOException {
        String path = "/custom/" + "METHOD_NOT_ALLOWED";

        // assertion has to be disabled for now
        String result = sendHttpRequest("GET", path)
        //      .assert405()
                .getBody();

        assertThat(getTraces(), equalTo(Collections.singletonList(Message.CUSTOM_405_ERROR_HANDLER.name())));
        assertThat(result, containsString(HttpStatusCode.METHOD_NOT_ALLOWED.name()));
    }

    @Test
    void test406ErrorHandledByCustom406ErrorHandler() throws IOException {
        String path = "/custom/" + "NOT_ACCEPTABLE";

        // assertion has to be disabled for now
        String result = sendHttpRequest("POST", path, "application/json;charset=UTF-16, text/plain", "text/html")
        //      .assert406()
                .getBody();

        assertThat(getTraces(), equalTo(Collections.singletonList(Message.CUSTOM_406_ERROR_HANDLER.name())));
        assertThat(result, containsString(HttpStatusCode.NOT_ACCEPTABLE.name()));
    }

    @Test
    void test415ErrorHandledByCustom415ErrorHandler() throws IOException {
        String path = "/custom/" + "UNSUPPORTED_MEDIA_TYPE";

        // assertion has to be disabled for now
        String result = sendHttpRequest("POST", path, "*/*", "application/xml, application/json")
        //      .assert415()
                .getBody();

        assertThat(getTraces(), equalTo(Collections.singletonList(Message.CUSTOM_415_ERROR_HANDLER.name())));
        assertThat(result, containsString(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE.name()));
    }
}
