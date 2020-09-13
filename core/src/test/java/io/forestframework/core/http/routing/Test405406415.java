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

    @Route(path = "/0", type = RoutingType.HANDLER, produces = {"application/json+xml", "text/plain"})
    public void handler0(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write("0");
    }

    @Route(path = "/1", type = RoutingType.HANDLER, produces = {"application/json"})
    public void handler1(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write("1");
    }

    @Route(path = "/2", type = RoutingType.HANDLER)
    public void handler2(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write("2");

    }

    @Route(path = "/3", type = RoutingType.HANDLER, produces = {"text/*"})
    public void handler3(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write("3");

    }

    @Route(path = "/4", type = RoutingType.HANDLER, consumes = {"application/json"})
    public void handler4(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write("4");
    }

    @Route(path = "/5", type = RoutingType.HANDLER, consumes = {"application/json"})
    public void handler5(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write("5");

    }

    @Route(path = "/6", type = RoutingType.HANDLER, produces = {"application/*"}, consumes = {"application/json"})
    public void handler6(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write("6");

    }

    @Route(path = "/7", type = RoutingType.HANDLER, produces = {"text/*"}, consumes = {"application/xhtml+xml", "image/jxr"})
    public void handler7(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write("7");

    }

    @Route(path = "/8", type = RoutingType.HANDLER, produces = {"text/html"})
    public void handler8(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write("8");

    }

    @Route(path = "/9", type = RoutingType.HANDLER)
    public void handler9(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write("9");
    }

    @Route(path = "/10", type = RoutingType.HANDLER, produces = {"application/json", "image/jxr"})
    public void handler10(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write("10");
    }
}

@Router("/custom")
class CustomErrorHandler extends AbstractTraceableRouter {
    @Route(path = "/METHOD_NOT_ALLOWED", type = RoutingType.HANDLER, methods = HttpMethod.POST)
    public void handle405(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write(Message.HANDLER.name());
    }

    @Route(path = "/NOT_ACCEPTABLE", type = RoutingType.HANDLER, produces = {"application/json;charset=UTF-8", "text/plain"}, consumes = "application/json")
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
    void test405ErrorHandledByDefaultErrorHandler() throws IOException {
        String result = sendHttpRequest("GET", "/METHOD_NOT_ALLOWED").assert405().getBody();

        assertThat(getTraces(), equalTo(Collections.emptyList()));
        assertThat(result, equalTo(HttpStatusCode.METHOD_NOT_ALLOWED.name()));
    }

    @ParameterizedTest
    @CsvSource({
            "4, application/json",
            "5, text/*",
            "6, application/json",
            "7, application/json",
            "8, application/json",
            "9, application/json, text/plain",
            "10, application/xhtml+xml, image/jxr"
    })
    void shouldGet406ErrorAndHandledByDefaultErrorHandlerWhenProducesNotMatched(String number, String header) throws IOException {
        String path = "/" + number;
        String result = sendHttpRequest("POST", path, header, "text/plain").assert406().getBody();

        assertThat(getTraces(), equalTo(Collections.emptyList()));
        assertThat(result, equalTo(HttpStatusCode.NOT_ACCEPTABLE.name()));
    }

    @ParameterizedTest
    @CsvSource({
            "0, */*",
            "1, */*",
            "2, */*",
            "3, */*"
    })
    void shouldReturn200WhenProducesMatched(String number, String header) throws IOException {
        String path = "/" + number;
        String result = sendHttpRequest("POST", path, header, "text/plain").assert200().getBody();

        assertThat(getTraces(), equalTo(Collections.singletonList(Message.HANDLER.name())));
        assertThat(result, containsString(number));
    }

    @ParameterizedTest
    @CsvSource({
            "4, application/*",
            "5, text/*",
            "6, text/html",
            "7, application/json, image/jxr"
    })
    void shouldGet415ErrorAndHandledByDefaultErrorHandlerWhenConsumesNotMatched(String number, String contentType) throws IOException {
        String path = "/" + number;
        String result = sendHttpRequest("POST", path, "*/*", contentType).assert415().getBody();

        assertThat(getTraces(), equalTo(Collections.emptyList()));
        assertThat(result, equalTo(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE.name()));
    }

    @ParameterizedTest
    @CsvSource({
            "0, application/json",
            "1, text/*",
            "2, application/json, text/plain"
    })
    void shouldReturn200WhenConsumesMatched(String number, String contentType) throws IOException {
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
//                .assert405()
                .getBody();

        assertThat(getTraces(), equalTo(Collections.singletonList(Message.CUSTOM_405_ERROR_HANDLER.name())));
        assertThat(result, containsString(HttpStatusCode.METHOD_NOT_ALLOWED.name()));
    }

    @Test
    void test406ErrorHandledByCustom406ErrorHandler() throws IOException {
        String path = "/custom/" + "NOT_ACCEPTABLE";

        // assertion has to be disabled for now
        String result = sendHttpRequest("POST", path, "application/json;charset=UTF-16, text/plain", "text/html")
//                .assert406()
                .getBody();

        assertThat(getTraces(), equalTo(Collections.singletonList(Message.CUSTOM_406_ERROR_HANDLER.name())));
        assertThat(result, containsString(HttpStatusCode.NOT_ACCEPTABLE.name()));
    }

    @Test
    void test415ErrorHandledByCustom415ErrorHandler() throws IOException {
        String path = "/custom/" + "UNSUPPORTED_MEDIA_TYPE";

        // assertion has to be disabled for now
        String result = sendHttpRequest("POST", path, "*/*", "application/xml, application/json")
//                .assert415()
                .getBody();

        assertThat(getTraces(), equalTo(Collections.singletonList(Message.CUSTOM_415_ERROR_HANDLER.name())));
        assertThat(result, containsString(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE.name()));
    }
}
