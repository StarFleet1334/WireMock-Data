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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.learnwiremock.endpoints.UtilEndpoints.GET_ALL_ANIME;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(WireMockExtension.class)

public class AnimeServerFaultTest {

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

    @BeforeEach
    void setUp() {
        int port = wireMockServer.port();
        String baseUrl = String.format("http://localhost:%s/", port);
        System.out.println("baseUrl : " + baseUrl);
        webClient = WebClient.create(baseUrl);
        animeRestClient = new AnimeRestClient(webClient);

        stubFor(any(anyUrl()).willReturn(aResponse().proxiedFrom("http://localhost:8081")));
    }


    @Test
    void getAllAnime() throws InterruptedException {
        stubFor(get(urlPathEqualTo(GET_ALL_ANIME.getPath())).willReturn(serverError()));
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

    @Test
    void getAllAnimeServiceUnavailable() throws InterruptedException {
        stubFor(get(urlPathEqualTo(GET_ALL_ANIME.getPath())).willReturn(serverError()
                .withStatus(HttpStatus.SERVICE_UNAVAILABLE.value())
                .withBody("Service Unavailable")));
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

    @Test
    void getAllAnimeFaultResponse() throws InterruptedException {
        // This one prematurely closes the connection
        stubFor(get(urlPathEqualTo(GET_ALL_ANIME.getPath())).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
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
