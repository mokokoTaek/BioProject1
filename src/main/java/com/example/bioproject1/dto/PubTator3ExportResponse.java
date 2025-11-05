package com.example.bioproject1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * PubTator3 Export API (.../export/biocjson)의 최상위 응답 구조 DTO.
 * 실제 데이터는 "PubTator3" 키 아래 리스트 형태로 존재합니다.
 * @param documents "PubTator3" 키에 매핑되는 BioCDocument 리스트.
 * (Jackson이 JSON 키 "PubTator3"를 이 필드에 매핑하도록 @JsonProperty 사용)
 */
public record PubTator3ExportResponse(
        @JsonProperty("PubTator3") List<BioCDocument> documents
) {}
