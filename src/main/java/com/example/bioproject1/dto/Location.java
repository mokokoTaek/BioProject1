package com.example.bioproject1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 텍스트 내의 주석 위치 (offset과 length)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Location(
        int offset,
        int length
) {}
