package com.github.marekscholle;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@RunWith(VertxUnitRunner.class)
public class UploadTest {
    @Test
    public void test(TestContext context) throws IOException, URISyntaxException, InterruptedException {
        final var vertx = Vertx.vertx();
        final var logger = LoggerFactory.getLogger(getClass());

        final Router router = Router.router(vertx);

        final Handler<RoutingContext> requestHandler = ctx -> {
            logger.info("processing request");
            ctx.request().exceptionHandler(e -> {
                logger.error("ctx.request error", e);
                context.fail();
            });
            ctx.response().exceptionHandler(e -> {
                logger.error("ctx.response error", e);
                context.fail();
            });
            ctx.request().setExpectMultipart(true);
            ctx.response().setStatusCode(200);
            ctx.request().uploadHandler(upload -> {
                logger.info("upload handler {}", upload.name());
                upload.handler(buf -> logger.info("upload handler handler {}", upload.name()));
            });
            ctx.request().endHandler(v -> {
                logger.info("end handler: form attributes=\n{}", ctx.request().formAttributes());
                ctx.response().end();
            });
        };

        router.put("/*")
                .method(HttpMethod.POST)
                .failureHandler(ctx -> {
                    logger.error("failure handler error", ctx.failure());
                    context.fail();
                })
                .handler(requestHandler);
        var s = new ServerSocket(0);
        s.close();
        try {
            logger.info("binding to port {}", s.getLocalPort());
            final var listening = context.async();
            vertx.createHttpServer(new HttpServerOptions()).requestHandler(router).listen(s.getLocalPort(), res -> {
                if (res.succeeded()) {
                    logger.info("listening on port {}", s.getLocalPort());
                    listening.complete();
                } else {
                    logger.error("failed to listen on " + s.getLocalPort(), res.cause());
                    context.fail();
                }
            });
            listening.awaitSuccess();

            final var client = HttpClient.newBuilder().build();

            // some form field large enough to hit Vert.x limit
            final var data = "x".repeat(16 * 1024);
            final HttpRequest upload =
                    HttpRequest.newBuilder()
                            .uri(new URI("http://localhost:" + s.getLocalPort()))
                            .version(HttpClient.Version.HTTP_1_1)
                            .header("Content-Type", "multipart/form-data; boundary=\"boundary\"")
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    "--boundary\n"
                                            + "Content-Disposition: form-data; name=\"key\"\n"
                                            + "\n"
                                            + data
                                            + "\n"
                                            + "--boundary--"))
                            .build();
            final var uploadResponse = client.send(upload, HttpResponse.BodyHandlers.ofString());

            // FAILED
            // there is a bunch of java.io.IOException: Size exceed allowed maximum capacity
            // caused by oversized form field
            Assert.assertEquals(200, uploadResponse.statusCode());
        } finally {
            logger.info("closing vertx");
            vertx.close();
        }
    }
}
