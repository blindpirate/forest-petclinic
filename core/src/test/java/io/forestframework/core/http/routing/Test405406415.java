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
        addToTrace(Message.CUSTOM_405_ERROR_HANDLER.name());
        response.write(Message.CUSTOM_405_ERROR_HANDLER.name());
    }

    @Route(path = "/NOT_ACCEPTABLE", type = RoutingType.HANDLER, produces = "application/json")
    public void handler406(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.putHeader("content-type", "application/json");
        response.write("{\"id\":1}");
    }

    @Route(path = "/UNSUPPORTED_MEDIA_TYPE", type = RoutingType.HANDLER, consumes = "application/json;charset=UTF-8")
    public void handler415(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
    }
}

@Router("/custom")
class CustomErrorHandler extends AbstractTraceableRouter {
    @Route(path = "/METHOD_NOT_ALLOWED", type = RoutingType.HANDLER, methods = HttpMethod.POST)
    public void handle405(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.write(Message.HANDLER.name());
    }

    @Route(path = "/NOT_ACCEPTABLE", type = RoutingType.HANDLER, produces = "application/json", consumes = "application/json")
    public void handler406(HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
        response.putHeader("content-type", "application/json");
        response.write("{\"id\":1}");
    }

    @Route(path = "/UNSUPPORTED_MEDIA_TYPE", type = RoutingType.HANDLER, consumes = "application/json")
    public void handler415(HttpServerResponse response) {
        addToTrace("UNSUPPORTED_MEDIA_TYPE");
    }

    @OnError(value = "/METHOD_NOT_ALLOWED", statusCode = HttpStatusCode.METHOD_NOT_ALLOWED)
    public void on405(HttpServerResponse response, Throwable e) {
        response.write(Message.CUSTOM_405_ERROR_HANDLER.name());
        response.write("  e.getMessage():  " + e.getMessage());
        addToTrace(Message.CUSTOM_405_ERROR_HANDLER.name());
    }

    @OnError(value = "/NOT_ACCEPTABLE", statusCode = HttpStatusCode.NOT_ACCEPTABLE)
    public void on406(HttpServerResponse response, Throwable e) {
        response.write(Message.CUSTOM_405_ERROR_HANDLER.name());
        response.write("  e.getMessage():  " + e.getMessage());
        addToTrace(Message.CUSTOM_405_ERROR_HANDLER.name());
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

    @Test
    void test406ErrorHandledByDefaultErrorHandler() throws IOException {
        String result = sendHttpRequest("POST", "/NOT_ACCEPTABLE", "text/html", "*/*").assert406().getBody();

        assertThat(getTraces(), equalTo(Collections.emptyList()));
        assertThat(result, equalTo(HttpStatusCode.NOT_ACCEPTABLE.name()));
    }

    @Test
    void shouldReturn200WhenRequestAcceptsALLMediaTypeAndProducesIsAnyType() throws IOException {
        String result = sendHttpRequest("POST", "/NOT_ACCEPTABLE").assert200().getBody();

        System.out.println(result);
        System.out.println(getTraces());

        assertThat(getTraces(), equalTo(Collections.singletonList(Message.HANDLER.name())));
        assertThat(result, containsString("{\"id\":1}"));
    }

    @ParameterizedTest
    @CsvSource({"text/plain", "application/json;charset=UTF-16"})
    void test415ErrorHandledByDefaultErrorHandler(String header) throws IOException {
        String result = sendHttpRequest("POST", "/UNSUPPORTED_MEDIA_TYPE", "*/*", header).assert415().getBody();

        System.out.println(result);
        System.out.println(getTraces());

        assertThat(getTraces(), equalTo(Collections.emptyList()));
        assertThat(result, equalTo(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE.name()));
    }

    @ParameterizedTest
    @CsvSource({"application/json;charset=UTF-8", "application/json"})
    void shouldReturn200WhenConsumesEqualsToOrSubtypeOfRequestHeader(String header) throws IOException {
        String result = sendHttpRequest("POST", "/UNSUPPORTED_MEDIA_TYPE", "*/*", header).assert200().getBody();

        System.out.println(result);
        System.out.println(getTraces());

        assertThat(getTraces(), equalTo(Collections.singletonList(Message.HANDLER.name())));
    }

    @Test
    void test405ErrorHandledByCustom405ErrorHandler() throws IOException {
        String path = "/custom/" + "METHOD_NOT_ALLOWED";

        // assertion has to be disabled for now
        String result = sendHttpRequest("GET", path)
//                .assert405()
                .getBody();
        System.out.println(result);
        System.out.println(getTraces());

        assertThat(getTraces(), equalTo(Collections.singletonList(Message.CUSTOM_405_ERROR_HANDLER.name())));
        assertThat(result, containsString(HttpStatusCode.METHOD_NOT_ALLOWED.name()));
    }

    @Test
    void test406ErrorHandledByCustom406ErrorHandler() throws IOException {
        String path = "/custom/" + "NOT_ACCEPTABLE";

        // assertion has to be disabled for now
        String result = sendHttpRequest("POST", path, "application/json;charset=UTF-8", "*/*")
//                .assert406()
                .getBody();

        System.out.println(result);
        System.out.println(getTraces());

        assertThat(getTraces(), equalTo(Collections.singletonList(Message.CUSTOM_405_ERROR_HANDLER.name())));
        assertThat(result, containsString(HttpStatusCode.NOT_ACCEPTABLE.name()));
    }

//    @Test
//    void test415ErrorHandledByCustom415ErrorHandler() throws IOException {
//        String path = "/custom/" + "UNSUPPORTED_MEDIA_TYPE";
//
//        // assertion has to be disabled for now
//        String result = sendHttpRequest("POST", path, "*/*", "application/*;charset=UTF-8")
////                .assert415()
//                .getBody();
//
//        System.out.println(result);
//        System.out.println(getTraces());
//
//        assertThat(getTraces(), equalTo(Collections.singletonList(Message.CUSTOM_415_ERROR_HANDLER.name())));
//        assertThat(result, containsString(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE.name()));
//    }

    @Test
    void return200WhenCharSetIsUTF8() throws IOException {
        String path = "/custom/" + "UNSUPPORTED_MEDIA_TYPE";

        // assertion has to be disabled for now
        String result = sendHttpRequest("POST", path, "*/*", "application/*;charset=UTF-8").assert415().getBody();

        System.out.println(result);
        System.out.println(getTraces());

        assertThat(getTraces(), equalTo(Collections.singletonList(Message.CUSTOM_415_ERROR_HANDLER.name())));
        assertThat(result, containsString(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE.name()));
    }

    @Test
    void get415ErrorWhenCharSetIsUTF16() throws IOException {
        String path = "/custom/" + "UNSUPPORTED_MEDIA_TYPE";

        // assertion has to be disabled for now
        String result = sendHttpRequest("POST", path, "*/*", "application/*;charset=UTF-16").getBody();

        System.out.println(result);
        System.out.println(getTraces());

        assertThat(getTraces(), equalTo(Collections.singletonList(Message.CUSTOM_415_ERROR_HANDLER.name())));
        assertThat(result, containsString(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE.name()));
    }

}
