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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;


@ExtendWith(WireMockExtension.class)

public class AnimeSelectiveProxyTest {

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

        // here we enable reverse proxy for wiremock server
        stubFor(any(anyUrl()).willReturn(aResponse().proxiedFrom("http://localhost:8080")));
    }
    // Basically, we can use selective proxying
    // when new functionalities to the existing service are but not yet implemented.

    // To use selective proxying, we should up and run our Swagger API

    @Test
    void deleteAnimeByTitleSelectiveProxying() throws InterruptedException {
        Anime newAnime = Anime.builder()
                .title("Kavawaki")
                .rating(9.2)
                .maincharacter("Bob")
                .description("It is long story")
                .build();

        Mono<Anime> savedAnimeMono = animeRestClient.postNewAnime(newAnime);

        String title = "Naruto";
        String deleteMessage = "Deleted anime with title: " + title;

        // Encode the title in the URL
        String encodedTitle = title.replace(" ", "%20");

        stubFor(delete(urlPathEqualTo("/animes/title/" + encodedTitle))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(deleteMessage)));

        Mono<String> deleteResponseMono = animeRestClient.deleteAnimeByTitle(title);

        // Setup for asynchronous result checking
        AtomicReference<String> result = new AtomicReference<>("");
        CountDownLatch latch = new CountDownLatch(1);

        deleteResponseMono.subscribe(
                response -> {
                    result.set(response);
                    latch.countDown();
                },
                error -> latch.countDown()
        );
        LOGGER.info("Deleted anime with title: " + title);
        LOGGER.info("Result : " + result.get());
        latch.await();  // Ensuring the Mono completes before assertion
    }
    @Test
    void getAllAnimeTest() throws InterruptedException {

        // Now post a new anime
        Anime newAnime = Anime.builder()
                .title("Kavawaki")
                .rating(9.2)
                .maincharacter("Bob")
                .description("It is long story")
                .build();

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

        // Fetch all animes after posting
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
        LOGGER.info("Anime List After Post: " + retrievedAnimeList);

        assertNotNull(retrievedAnimeList, "The list of animes after posting should not be null.");
        assertFalse(retrievedAnimeList.isEmpty(), "The list of animes after posting should not be empty.");
        assertTrue(retrievedAnimeList.stream().anyMatch(anime -> "Kavawaki".equals(anime.getTitle())), "The posted anime should be present in the list.");
    }

}
