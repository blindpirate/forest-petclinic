package io.forestframework.core.http.routing;

import com.google.inject.Inject;
import io.forestframework.core.ForestApplication;
import io.forestframework.core.http.HttpMethod;
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

    @Route(path = "/NOT_ACCEPTABLE", type = RoutingType.HANDLER, produces = "text/html")
    public void handler406(HttpServerResponse response) {
        addToTrace(Message.CUSTOM_406_ERROR_HANDLER.name());
        response.putHeader("content-type", "text/html");
        response.write("<html>hello</html>");

//        response.putHeader("content-type", "application/json");
//        response.write("{\"id\":1}");
    }

//    @Route(path = "/UNSUPPORTED_MEDIA_TYPE", type = RoutingType.HANDLER)
//    public void handler415(HttpServerResponse response) {
//        addToTrace("UNSUPPORTED_MEDIA_TYPE");
//        throw new HttpException(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE);
//    }

//    @OnError(value = "/**", statusCode = HttpStatusCode.METHOD_NOT_ALLOWED)
//    public void on405(HttpServerResponse response, Throwable e) {
//        response.write(Message.CUSTOM_4XX_ERROR_HANDLER.name());
//        response.write("  e.getMessage():  " + e.getMessage());
//        addToTrace(Message.CUSTOM_4XX_ERROR_HANDLER.name());
//    }
//
//    @OnError(value = "/**", statusCode = HttpStatusCode.NOT_ACCEPTABLE)
//    public void on406(HttpServerResponse response, Throwable e) {
//        response.write(Message.CUSTOM_4XX_ERROR_HANDLER.name());
//        response.write("  e.getMessage():  " + e.getMessage());
//        addToTrace(Message.CUSTOM_4XX_ERROR_HANDLER.name());
//    }
//
//    @OnError(value = "/**", statusCode = HttpStatusCode.UNSUPPORTED_MEDIA_TYPE)
//    public void on415(HttpServerResponse response, Throwable e) {
//        response.write(Message.CUSTOM_4XX_ERROR_HANDLER.name());
//        response.write("  e.getMessage():  " + e.getMessage());
//        addToTrace(Message.CUSTOM_4XX_ERROR_HANDLER.name());
//    }
}


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
        System.out.println(result);
        System.out.println(router.traces);

        assertThat(router.traces, equalTo(Collections.emptyList()));
    }

    @Test
    void test406() throws IOException {
        String result = sendHttpRequest("POST", "/NOT_ACCEPTABLE")
                .assert406()
                .getBody();
        System.out.println(result);
        System.out.println(router.traces);

        assertThat(router.traces, equalTo(Collections.emptyList()));
    }
}
