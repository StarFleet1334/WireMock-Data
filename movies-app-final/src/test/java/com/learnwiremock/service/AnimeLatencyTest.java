package com.learnwiremock.service;

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock;
import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.learnwiremock.dto.Anime;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.learnwiremock.endpoints.UtilEndpoints.GET_ALL_ANIME;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(WireMockExtension.class)

public class AnimeLatencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnimeRestWireMockTest.class);

    AnimeRestClient animeRestClient;
    WebClient webClient;

    @InjectServer
    WireMockServer wireMockServer;

    @ConfigureWireMock
    Options options = wireMockConfig().
            port(8088)
            // This notifier prints in the console all the necessary information
            .notifier(new ConsoleNotifier(true))
            .extensions(new ResponseTemplateTransformer(true));

    // This one takes read/write timeout
    TcpClient tcpClient = TcpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,5000)
            .doOnConnected(connection -> {
                connection.addHandlerLast(new ReadTimeoutHandler(5))
                        .addHandlerLast(new WriteTimeoutHandler(5));
            });
    // providing timeout 5 seconds

    @BeforeEach
    void setUp() {
        int port = wireMockServer.port();
        String baseUrl = String.format("http://localhost:%s/", port);
        System.out.println("baseUrl : " + baseUrl);
        // we have to inject timeout instance which is TcpClient
        webClient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(HttpClient.from(tcpClient)))
                .baseUrl(baseUrl).build();
        animeRestClient = new AnimeRestClient(webClient);
    }

    @Test
    void getAllAnimeLatencyFixedDelay() throws InterruptedException {
        // This one prematurely closes the connection
        stubFor(get(urlPathEqualTo(GET_ALL_ANIME.getPath())).willReturn(ok().withFixedDelay(10000)));
//        stubFor(get(urlPathEqualTo(GET_ALL_ANIME.getPath())).willReturn(ok().withUniformRandomDelay(3000,10000)));


        Flux<Anime> animeFlux = animeRestClient.getAllAnime();
        // Preparing to count items in the flux
        AtomicInteger count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);  // Ensures we wait for completion

        animeFlux.subscribe(
                anime -> count.getAndIncrement(),
                error -> {},
                latch::countDown  // Counting down the latch when the flux completes
        );
        // This part would be printed:
        // LOGGER.error("Error retrieving all animes", e)
        latch.await();  // Waiting for the flux to complete
        assertTrue(count.get() > 0, "The anime list should not be empty");
    }
}
