package com.learnwiremock.service;

import com.learnwiremock.dto.Anime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.learnwiremock.endpoints.UtilEndpoints.*;

public class AnimeRestClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnimeRestClient.class);
    private WebClient webClient;

    public AnimeRestClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Flux<Anime> getAllAnime() {
        return webClient.get()
                .uri(GET_ALL_ANIME.getPath())
                .retrieve()
                .bodyToFlux(Anime.class)
                .doOnError(WebClientResponseException.class, e -> LOGGER.error("WebClient response error: Status {}, Body {}", e.getRawStatusCode(), e.getResponseBodyAsString(), e))
                .doOnError(Exception.class, e -> LOGGER.error("Error retrieving all animes", e));
    }

    public Mono<Anime> getAnimeById(Integer id) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path(GET_ANIME_BY_ID.getPath()).build(id))
                .retrieve()
                .bodyToMono(Anime.class)
                .doOnError(WebClientResponseException.class, e -> LOGGER.error("WebClient response error: Status {}, Body {}", e.getRawStatusCode(), e.getResponseBodyAsString(), e))
                .doOnError(Exception.class, e -> LOGGER.error("Error retrieving anime by ID: {}", id, e));
    }

    public Mono<Anime> postNewAnime(Anime newAnime) {
        return webClient.post()
                .uri(POST_ANIME.getPath())
                .body(Mono.just(newAnime), Anime.class)
                .retrieve()
                .bodyToMono(Anime.class)
                .doOnError(WebClientResponseException.class, e -> LOGGER.error("WebClient response error: Status {}, Body {}", e.getRawStatusCode(), e.getResponseBodyAsString(), e))
                .doOnError(Exception.class, e -> LOGGER.error("Error posting new anime", e));
    }

    public Mono<Anime> updateExistingAnime(Integer animeId, Anime anime) {
        return webClient.put()
                .uri(uriBuilder -> uriBuilder.path(PUT_ANIME_BY_ID.getPath()).build(animeId))
                .body(Mono.just(anime), Anime.class)
                .retrieve()
                .bodyToMono(Anime.class)
                .doOnError(WebClientResponseException.class, e -> LOGGER.error("WebClient response error: Status {}, Body {}", e.getRawStatusCode(), e.getResponseBodyAsString(), e))
                .doOnError(Exception.class, e -> LOGGER.error("Error updating anime ID: {}", animeId, e));
    }

    public Mono<String> deleteAnimeById(Integer animeId) {
        return webClient.delete()
                .uri(uriBuilder -> uriBuilder.path(DELETE_ANIME_BY_ID.getPath()).build(animeId))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(WebClientResponseException.class, e -> LOGGER.error("WebClient response error: Status {}, Body {}", e.getRawStatusCode(), e.getResponseBodyAsString(), e))
                .doOnError(Exception.class, e -> LOGGER.error("Error deleting anime ID: {}", animeId, e));
    }

    public Mono<String> deleteAnimeByTitle(String title) {
        return webClient.delete()
                .uri(DELETE_ANIME_BY_TITLE.withTitle(title))
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> LOGGER.info("Successfully deleted anime with title: {}", title))
                .doOnError(WebClientResponseException.class, e -> LOGGER.error("WebClient response error: Status {}, Body {}", e.getRawStatusCode(), e.getResponseBodyAsString(), e))
                .doOnError(Exception.class, e -> LOGGER.error("Error deleting anime by title: {}", title, e));
    }
}
