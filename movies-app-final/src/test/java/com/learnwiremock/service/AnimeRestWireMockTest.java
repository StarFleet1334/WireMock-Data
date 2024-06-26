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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@ExtendWith(WireMockExtension.class)
public class AnimeRestWireMockTest {

    AnimeRestClient animeRestClient;
    WebClient webClient;

    @InjectServer
    WireMockServer wireMockServer;

    @ConfigureWireMock
    Options options = wireMockConfig().
            port(8088)
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

        // Here basically, instead of calling on this endpoint: GET_ALL_ANIME("/animes")
        // It is going to talk to wireMock server
//        Flux<Anime> animeFlux = animeRestClient.getAllAnime();
//        StepVerifier.create(animeFlux.doOnNext(anime -> System.out.println("Anime: " + anime)))
//                .expectNextCount(21)  // Assuming you expect 20 items
//                .expectComplete()
//                .verify();
    }

    @Test
    void getAllAnime() {
        Flux<Anime> animeFlux = animeRestClient.getAllAnime();
//        StepVerifier.create(animeFlux)
//                .expectNextMatches(anime -> {
//                    assertNotNull(anime, "Anime should not be null");
//                    return anime.getTitle().contains("Naruto"); // Check if 'Naruto' is in the list
//                })
//                .expectNextCount(19)  // Expect 19 more elements, adjust number based on expected items
//                .verifyComplete();
    }

    @Test
    void retrieveAnimeById() {
        Integer animeId = 1;
        Mono<Anime> animeMono = animeRestClient.getAnimeById(animeId);
//        StepVerifier.create(animeMono)
//                .assertNext(anime -> {
//                    assertNotNull(anime, "Anime should not be null");
//                    assertEquals("Naruto", anime.getTitle(), "The title should match 'Naruto'");
//                    assertEquals(8.5, anime.getRating(), "Rating should match 8.5");
//                })
//                .expectComplete()
//                .verify();
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

//        StepVerifier.create(savedAnimeMono)
//                .assertNext(savedAnime -> {
//                    assertNotNull(savedAnime, "Saved anime should not be null");
//                    assertEquals("Kavawaki", savedAnime.getTitle(), "The title should match 'Kavawaki'");
//                })
//                .expectComplete()
//                .verify();
    }

    @Test
    void updateAnime() {
        Anime updatedAnime = new Anime(3, "One Piece (Updated)", 9, "Monkey D. Luffy", "The quest continues for the ultimate treasure, One Piece.");
        Mono<Anime> updatedAnimeMono = animeRestClient.updateExistingAnime(3, updatedAnime);

//        StepVerifier.create(updatedAnimeMono)
//                .assertNext(anime -> assertEquals("One Piece (Updated)", anime.getTitle(), "Title should be updated to 'One Piece (Updated)'"))
//                .expectComplete()
//                .verify();
    }

    @Test
    void deleteAnime() {
        Mono<String> deleteResponseMono = animeRestClient.deleteAnimeById(4);
//        StepVerifier.create(deleteResponseMono)
//                .assertNext(response -> assertEquals("Deleted anime with id: 4", response, "Response should confirm deletion of anime id 4"))
//                .expectComplete()
//                .verify();
    }

    @AfterEach
    void tearDown() {
        // Optional cleanup can be done here if necessary

    }
}
