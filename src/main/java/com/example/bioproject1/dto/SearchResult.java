package com.example.bioproject1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * '/search/' API의 응답 내 'results' 배열의 개별 항목을 매핑하기 위한 DTO입니다.
 * pmid만 필요합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchResult(
        String pmid
) {}
