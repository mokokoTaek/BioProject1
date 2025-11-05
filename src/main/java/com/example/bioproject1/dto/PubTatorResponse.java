package com.example.bioproject1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * PubTator API 응답의 최상위 객체 (biocjson 형식)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PubTatorResponse(
        List<BioCDocument> documents
) {}

