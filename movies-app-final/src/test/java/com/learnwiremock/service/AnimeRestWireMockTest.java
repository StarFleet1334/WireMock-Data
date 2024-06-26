package com.learnwiremock.service;

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock;
import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.learnwiremock.dto.Anime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.learnwiremock.endpoints.UtilEndpoints.GET_ALL_ANIME;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(WireMockExtension.class)
public class AnimeRestWireMockTest {

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
    void getAllAnimeAndPrint() {

        stubFor(get(anyUrl()).willReturn(WireMock.aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("all-anime.json")));

    }

    @Test
    void getAllAnime() throws InterruptedException {
        stubFor(get(urlPathEqualTo(GET_ALL_ANIME.getPath())).willReturn(
                aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("all-anime.json")));
        Flux<Anime> animeFlux = animeRestClient.getAllAnime();
        // Preparing to count items in the flux
        AtomicInteger count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);  // Ensures we wait for completion

        animeFlux.subscribe(
                anime -> count.getAndIncrement(),
                error -> {},
                latch::countDown  // Counting down the latch when the flux completes
        );

        latch.await();  // Waiting for the flux to complete
        assertTrue(count.get() > 0, "The anime list should not be empty");
    }

    @Test
    void retrieveAnimeById() throws InterruptedException {
        stubFor(get(urlPathMatching("/animes/[0-9]")).willReturn(
                aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("single-anime.json")));
        Integer animeId = 9;
        Mono<Anime> animeMono = animeRestClient.getAnimeById(animeId);
        AtomicReference<String> title = new AtomicReference<>("");
        AtomicReference<Integer> id = new AtomicReference<>(0);
        CountDownLatch latch = new CountDownLatch(1);

        animeMono.subscribe(
                anime -> {
                    title.set(anime.getTitle());
                    id.set(anime.getId());
                    latch.countDown();
                },
                error -> latch.countDown()
        );

        latch.await();
        assertEquals("Naruto", title.get(), "The anime title should be Naruto");
        assertEquals(animeId, id.get(), "The anime id should be " + animeId);
    }

    @Test
    void addNewAnime() {
        Anime newAnime = Anime.builder()
                .title("Kavawaki")
                .rating(9.2)
                .maincharacter("Bob")
                .description("It is long story")
                .build();

        Mono<Anime> savedAnimeMono = animeRestClient.postNewAnime(newAnime);

    }

    @Test
    void updateAnime() {
        Anime updatedAnime = new Anime(3, "One Piece (Updated)", 9, "Monkey D. Luffy", "The quest continues for the ultimate treasure, One Piece.");
        Mono<Anime> updatedAnimeMono = animeRestClient.updateExistingAnime(3, updatedAnime);

    }

    @Test
    void deleteAnime() {
        Mono<String> deleteResponseMono = animeRestClient.deleteAnimeById(4);

    }

    @AfterEach
    void tearDown() {
        // Optional cleanup can be done here if necessary
        wireMockServer.stop();
    }
}
