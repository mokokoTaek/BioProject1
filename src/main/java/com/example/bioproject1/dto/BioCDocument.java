package com.example.bioproject1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BioCDocument(
        String id, // 예: "PMID"
        List<BioCPassage> passages
) {}
