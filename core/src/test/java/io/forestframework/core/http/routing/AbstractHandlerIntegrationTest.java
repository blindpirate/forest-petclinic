package io.forestframework.core.http.routing;

import io.forestframework.core.config.Config;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;

import javax.inject.Inject;
import java.io.IOException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbstractHandlerIntegrationTest {
    protected AbstractTraceableRouter router;

    CloseableHttpClient client;

    @Inject
    @Config("forest.http.port")
    Integer port;

    @BeforeEach
    void setUp() {
        client = HttpClients.createDefault();
        router.traces.clear();
    }

    public CloseableHttpResponse sendHttpRequest(String method, String path) {
        String uri = "http://localhost:" + port + path;
        HttpUriRequest request = RequestBuilder.create(method).setUri(uri).build();
        CloseableHttpResponse response;
        try {
            response = client.execute(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return response;
    }

    public String assertStatuscode(Supplier<CloseableHttpResponse> excutable, int statusCode) {
        CloseableHttpResponse response = excutable.get();
        assertEquals(statusCode, response.getStatusLine().getStatusCode());

        String result;
        try {
            result = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public String assert200(Supplier<CloseableHttpResponse> excutable) {
        return assertStatuscode(excutable, 200);
    }

    public String assert404(Supplier<CloseableHttpResponse> excutable) {
        return assertStatuscode(excutable, 404);
    }
}
