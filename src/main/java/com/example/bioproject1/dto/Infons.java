package com.example.bioproject1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 부가 정보 (예: passage의 type="abstract" 또는 annotation의 type="Gene")
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Infons(
        String type
) {}
