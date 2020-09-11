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

import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
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
        addToTrace(Message.CUSTOM_406_ERROR_HANDLER.name());
        response.putHeader("content-type", "application/json");
        response.write("{\"id\":1}");
    }

    @Route(path = "/UNSUPPORTED_MEDIA_TYPE", type = RoutingType.HANDLER, consumes = "text/html")
    public void handler415(HttpServerResponse response) {
        addToTrace("UNSUPPORTED_MEDIA_TYPE");
    }
}

//@Router("/**")
//class CustomErrorHandlerNotDuplicated extends AbstractTraceableRouter {
//
//    @Route(path = "/METHOD_NOT_ALLOWED", type = RoutingType.HANDLER)
//    public void handle405(HttpServerResponse response) {
//        addToTrace(Message.CUSTOM_405_ERROR_HANDLER.name());
//        response.write(Message.CUSTOM_405_ERROR_HANDLER.name());
//    }
//
//    @Route(path = "/NOT_ACCEPTABLE", type = RoutingType.HANDLER, produces = "application/json")
//    public void handler406(HttpServerResponse response) {
//        addToTrace(Message.CUSTOM_406_ERROR_HANDLER.name());
//        response.putHeader("content-type", "application/json");
//        response.write("{\"id\":1}");
//    }
//
//    @Route(path = "/UNSUPPORTED_MEDIA_TYPE", type = RoutingType.HANDLER, consumes = "text/html")
//    public void handler415(HttpServerResponse response) {
//        addToTrace("UNSUPPORTED_MEDIA_TYPE");
////        response.putHeader("content-type", "text/html");
////        response.write("<html>hello</html>");
//    }
//
//
//    @OnError(value = "/METHOD_NOT_ALLOWED", statusCode = HttpStatusCode.METHOD_NOT_ALLOWED)
//    public void on405(HttpServerResponse response, Throwable e) {
//        response.write(Message.CUSTOM_405_ERROR_HANDLER.name());
//        response.write("  e.getMessage():  " + e.getMessage());
//        addToTrace(Message.CUSTOM_405_ERROR_HANDLER.name());
//    }
//
////    @OnError(value = "/**", statusCode = HttpStatusCode.NOT_ACCEPTABLE)
////    public void on406(HttpServerResponse response, Throwable e) {
////        response.write(Message.CUSTOM_4XX_ERROR_HANDLER.name());
////        response.write("  e.getMessage():  " + e.getMessage());
////        addToTrace(Message.CUSTOM_4XX_ERROR_HANDLER.name());
////    }
//
//    @OnError(value = "/UNSUPPORTED_MEDIA_TYPE", statusCode = HttpStatusCode.UNSUPPORTED_MEDIA_TYPE)
//    public void on415(HttpServerResponse response, Throwable e) {
//        response.write(Message.CUSTOM_415_ERROR_HANDLER.name());
//        response.write("  e.getMessage():  " + e.getMessage());
//        addToTrace(Message.CUSTOM_415_ERROR_HANDLER.name());
//    }
//}


@ExtendWith(ForestExtension.class)
@ForestTest(appClass = App.class)
@DisableAutoScan
@IncludeComponents(classes = {
        Default4xxErrorHandler.class,

})
public class Test405406415 extends AbstractHandlerIntegrationTest {

    @Inject
    void setRouter(Default4xxErrorHandler router) {
        this.router = router;
    }

    @Test
    void test405() throws IOException {
        String result = sendHttpRequest("GET", "/METHOD_NOT_ALLOWED").assert405().getBody();

        assertThat(router.traces, equalTo(Collections.emptyList()));
        assertThat(result, equalTo("METHOD_NOT_ALLOWED"));
    }

    @Test
    void test406() throws IOException {
        String result = sendHttpRequest("POST", "/NOT_ACCEPTABLE")
                .assert200()
                .getBody();
        System.out.println(result);
        System.out.println(router.traces);

        assertThat(router.traces, hasItem(Message.CUSTOM_406_ERROR_HANDLER.name()));
        assertThat(result, containsString("{\"id\":1}"));

//        assertThat(router.traces, equalTo(Collections.emptyList()));
//        assertThat(result, equalTo("NOT_ACCEPTABLE"));
    }

    @Test
    void test415() throws IOException {
        String result = sendHttpRequest("POST", "/UNSUPPORTED_MEDIA_TYPE")
                .assert415()
                .getBody();

        System.out.println(result);
        System.out.println(router.traces);

        assertThat(router.traces, equalTo(Collections.emptyList()));
        assertThat(result, equalTo("UNSUPPORTED_MEDIA_TYPE"));
    }
}
