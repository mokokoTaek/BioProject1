package com.example.bioproject1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * PubTator3 '/search/' API의 응답을 매핑하기 위한 DTO입니다.
 * (예: {"total": 123, "results": [ ... ]})
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PubTatorSearchResponse(
        int total,
        List<SearchResult> results
) {}
