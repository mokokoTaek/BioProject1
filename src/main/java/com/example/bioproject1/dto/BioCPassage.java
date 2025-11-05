package com.example.bioproject1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BioCPassage(
        Infons infons,
        String text,
        List<Annotation> annotations
) {}
