package com.example.bioproject1.dto;

/**
 * 최종적으로 컨트롤러가 반환할 DTO
 */
public record HighlightedAbstract(
        String pmid,
        String title,
        String abstractHtml // 하이라이트된 초록 HTML
) {}
