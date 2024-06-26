package com.learnwiremock.endpoints;

/**
 * Enum to manage API endpoints for the Anime application,
 * providing methods to get formatted endpoints with parameters.
 */
public enum UtilEndpoints {
    GET_ALL_ANIME("/animes"),
    GET_ANIME_BY_ID("/animes/{id}"),
    PUT_ANIME_BY_ID("/animes/{id}"),
    DELETE_ANIME_BY_ID("/animes/{id}"),
    POST_ANIME("/animes");

    private final String path;

    UtilEndpoints(String path) {
        this.path = path;
    }

    /**
     * Returns the endpoint path.
     *
     * @return A string representing the path.
     */
    public String getPath() {
        return path;
    }

    /**
     * Replaces path parameters with actual values.
     *
     * @param id The ID to replace in the path.
     * @return The endpoint path with the specified ID.
     */
    public String withId(int id) {
        return path.replace("{id}", Integer.toString(id));
    }
}

