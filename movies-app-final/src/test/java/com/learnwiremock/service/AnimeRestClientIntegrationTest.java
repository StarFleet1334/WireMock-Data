package com.learnwiremock.service;

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock;
import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.learnwiremock.dto.Anime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@ExtendWith(WireMockExtension.class)
public class AnimeRestClientIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnimeRestClientIntegrationTest.class);

    AnimeRestClient animeRestClient;
    WebClient webClient;

    @InjectServer
    WireMockServer wireMockServer;

    @ConfigureWireMock
    Options options = wireMockConfig()
            .port(8088)
            .notifier(new ConsoleNotifier(true))
            .extensions(new ResponseTemplateTransformer(true));

    @BeforeEach
    void setUp() {
        int port = wireMockServer.port();
        String baseUrl = String.format("http://localhost:%s/", port);
        webClient = WebClient.create(baseUrl);
        animeRestClient = new AnimeRestClient(webClient);

        // Enable reverse proxy for wiremock server
        stubFor(any(anyUrl()).willReturn(aResponse().proxiedFrom("http://localhost:8080")));
    }

    @Test
    void postNewAnimeTest() throws InterruptedException {
        // Create stub for POST /animes
        Anime newAnime = Anime.builder()
                .title("Kavawaki")
                .rating(9.2)
                .maincharacter("Bob")
                .description("It is long story")
                .build();

        stubFor(post(urlPathEqualTo("/animes"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{ \"title\": \"Kavawaki\", \"rating\": 9.2, \"maincharacter\": \"Bob\", \"description\": \"It is long story\" }")));

        Mono<Anime> savedAnimeMono = animeRestClient.postNewAnime(newAnime);

        AtomicReference<Anime> postedAnime = new AtomicReference<>();
        CountDownLatch postLatch = new CountDownLatch(1);

        savedAnimeMono.subscribe(
                anime -> {
                    postedAnime.set(anime);
                    postLatch.countDown();
                },
                error -> postLatch.countDown()
        );

        postLatch.await();  // Ensuring the Mono completes before continuing

        Anime savedAnime = postedAnime.get();
        assertNotNull(savedAnime, "The anime should be posted successfully.");
        assertEquals(newAnime.getTitle(), savedAnime.getTitle());
        LOGGER.info("Posted Anime: " + savedAnime);
    }

    @Test
    void getAllAnimeTest() throws InterruptedException {
        // Create stub for GET /animes
        stubFor(get(urlPathEqualTo("/animes"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[{\"title\": \"Kavawaki\", \"rating\": 9.2, \"maincharacter\": \"Bob\", \"description\": \"It is long story\"}]")));

        // Fetch all animes
        Flux<Anime> allAnimeAfterPost = animeRestClient.getAllAnime();

        AtomicReference<List<Anime>> animeListAfter = new AtomicReference<>();
        CountDownLatch latchAfter = new CountDownLatch(1);

        allAnimeAfterPost.collectList().subscribe(
                animes -> {
                    animeListAfter.set(animes);
                    latchAfter.countDown();
                },
                error -> latchAfter.countDown()
        );

        latchAfter.await();  // Ensuring the Flux completes before continuing

        List<Anime> retrievedAnimeList = animeListAfter.get();
        assertNotNull(retrievedAnimeList, "The list of animes after posting should not be null.");
        assertTrue(retrievedAnimeList.stream().anyMatch(anime -> "Kavawaki".equals(anime.getTitle())), "The posted anime should be present in the list.");
        LOGGER.info("Anime List After Post: " + retrievedAnimeList);
    }
}
