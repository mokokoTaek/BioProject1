package com.example.bioproject1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 텍스트 내의 주석 (예: Gene, Disease)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Annotation(
        List<Location> locations,
        String text, // 주석이 달린 텍스트 (예: "APOE4")
        Infons infons // 주석 정보 (예: type="Gene")
) {}
