package com.learnwiremock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Anime {

    private int id;
    private String title;
    private double rating;
    private String maincharacter;
    private String description;

}

