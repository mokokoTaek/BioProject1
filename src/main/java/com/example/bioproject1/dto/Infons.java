package com.example.bioproject1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// (수정) DTO에 정의되지 않은 필드(예: "ncbi_homologene")를 무시하도록 설정
@JsonIgnoreProperties(ignoreUnknown = true)
public record Infons(
        String type,
        String identifier, // 'identifier' 필드
        Boolean valid    // (수정) 유효성(valid) 필드 추가
) {}