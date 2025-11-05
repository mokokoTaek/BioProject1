package com.example.bioproject1.service;

import com.example.bioproject1.dto.*; // 모든 DTO 임포트
import com.fasterxml.jackson.core.type.TypeReference; // List 파싱을 위해 추가
import com.fasterxml.jackson.databind.ObjectMapper;
// (수정) Ignore unknown properties during parsing
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PubTatorService {

    private static final String HIGHLIGHT_START = "<mark class='highlight'>";
    private static final String HIGHLIGHT_END = "</mark>";

    // --- 2단계 API 엔드포인트를 모두 v3로 통일 ---
    private static final String SEARCH_API_URL = "https://www.ncbi.nlm.nih.gov/research/pubtator3-api/search/";
    private static final String EXPORT_API_URL = "https://www.ncbi.nlm.nih.gov/research/pubtator3-api/publications/export/biocjson";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public PubTatorService() {
        this.objectMapper = new ObjectMapper();
        // (수정) DTO에 없는 JSON 필드가 있어도 에러 없이 파싱하도록 설정
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * "v3 검색 -> v3 추출" 2단계 로직 구현
     */
    public List<HighlightedAbstract> searchAndHighlight(String keywords) throws IOException, InterruptedException {

        // --- 1단계: (v3/search) 키워드로 PMID 목록 검색 ---
        String freeTextQuery = keywords;
        URI searchUri = UriComponentsBuilder.fromHttpUrl(SEARCH_API_URL)
                .queryParam("text", freeTextQuery)
                .queryParam("limit", 10)
                .build(false)
                .toUri();
        System.out.println("1단계 (검색) API 요청 URI: " + searchUri);
        HttpRequest searchRequest = HttpRequest.newBuilder().uri(searchUri).GET().build();
        HttpResponse<String> searchResponse = httpClient.send(searchRequest, HttpResponse.BodyHandlers.ofString());

        if (searchResponse.statusCode() != 200) {
            throw new RuntimeException("1단계 (검색) API 호출 실패: " + searchResponse.statusCode() + " - " + searchResponse.body());
        }

        PubTatorSearchResponse searchResult = objectMapper.readValue(searchResponse.body(), PubTatorSearchResponse.class);
        if (searchResult.results() == null || searchResult.results().isEmpty()) {
            System.out.println("검색 결과가 없습니다 (PMID 0개): " + keywords);
            return new ArrayList<>();
        }
        List<String> pmids = searchResult.results().stream()
                .map(SearchResult::pmid)
                .collect(Collectors.toList());
        System.out.println("1단계 성공: " + pmids.size() + "개 PMID 획득 (" + String.join(",", pmids) + ")");

        // --- 2단계: (v3/export) 획득한 PMID로 BioC JSON 정보 추출 ---
        String pmidList = String.join(",", pmids);
        URI exportUri = UriComponentsBuilder.fromHttpUrl(EXPORT_API_URL)
                .queryParam("pmids", pmidList)
                .build(false)
                .toUri();
        System.out.println("2단계 (추출) API 요청 URI: " + exportUri);
        HttpRequest exportRequest = HttpRequest.newBuilder().uri(exportUri).GET().build();
        HttpResponse<String> exportResponse = httpClient.send(exportRequest, HttpResponse.BodyHandlers.ofString());

        if (exportResponse.statusCode() != 200) {
            throw new RuntimeException("2단계 (추출) API 호출 실패: " + exportResponse.statusCode() + " - " + exportResponse.body());
        }

        // --- 3단계: 2단계 응답 파싱 ---
        List<BioCDocument> documents = new ArrayList<>(); // 초기화
        try {
            // 1. 새로운 PubTator3ExportResponse DTO ({"PubTator3": [...]}) 로 먼저 시도
            PubTator3ExportResponse apiResponseWrapper = objectMapper.readValue(exportResponse.body(), PubTator3ExportResponse.class);

            if (apiResponseWrapper != null && apiResponseWrapper.documents() != null) {
                documents = apiResponseWrapper.documents();
                System.out.println("파싱 성공 (PubTator3ExportResponse DTO)");
            } else {
                System.out.println("!!! 파싱 성공했으나 PubTator3 필드가 null이거나 비어있음 (PubTator3ExportResponse DTO)");
                System.out.println("--- 원본 응답 (PubTator3 필드 없음) ---");
                System.out.println(exportResponse.body());
                System.out.println("---------------------------------------");
            }

        } catch (Exception e) {
            // 2. 1번 파싱 실패 시, List<BioCDocument> ([{...}, {...}]) 로 직접 시도
            System.out.println("!!! 파싱 1차 실패 (PubTator3ExportResponse DTO): " + e.getMessage());
            System.out.println("!!! 파싱 2차 시도 (List<BioCDocument> DTO)...");
            System.out.println("--- 원본 응답 (JSON 파싱 1차 실패) ---");
            System.out.println(exportResponse.body());
            System.out.println("----------------------------------------");

            try {
                documents = objectMapper.readValue(exportResponse.body(), new TypeReference<List<BioCDocument>>() {});
                System.out.println("파싱 2차 성공 (List<BioCDocument> DTO)");
            } catch (Exception e2) {
                System.out.println("!!! 파싱 2차 실패 (List<BioCDocument> DTO): " + e2.getMessage());
                System.out.println("--- 원본 응답 (JSON 파싱 2차 실패) ---");
                System.out.println(exportResponse.body());
                System.out.println("----------------------------------------");
                // documents는 이미 new ArrayList<>() 로 초기화됨
            }
        }

        List<HighlightedAbstract> finalResults = new ArrayList<>();
        if (documents == null || documents.isEmpty()) {
            System.out.println("API 결과에서 documents 리스트를 추출하지 못했습니다. (응답 본문 확인 필요)");
            return finalResults;
        }

        // --- 4단계: 하이라이팅 ---
        for (BioCDocument doc : documents) {
            String title = "";
            String abstractText = "";
            List<Annotation> abstractAnnotations = new ArrayList<>();

            if (doc.passages() != null) {
                for (BioCPassage passage : doc.passages()) {
                    if (passage.infons() != null && "title".equalsIgnoreCase(passage.infons().type())) {
                        title = passage.text();
                    } else if (passage.infons() != null && "abstract".equalsIgnoreCase(passage.infons().type())) {
                        abstractText = passage.text();
                        if (passage.annotations() != null) {
                            abstractAnnotations.addAll(passage.annotations());
                        }
                    }
                }
            } else {
                System.out.println("Warning: Document ID " + doc.id() + " has null passages.");
            }

            if (!abstractText.isEmpty()) {
                String highlightedAbstract = highlightText(abstractText, abstractAnnotations);
                finalResults.add(new HighlightedAbstract(doc.id(), title, highlightedAbstract));
            }
        }

        System.out.println(keywords + " (사용한 쿼리: " + freeTextQuery + ")로 " + finalResults.size() + "개 논문 처리 완료.");
        return finalResults;
    }

    private String highlightText(String text, List<Annotation> annotations) {
        if (text == null || text.isEmpty() || annotations == null || annotations.isEmpty()) {
            return escapeHtml(text == null ? "" : text);
        }

        annotations.sort(Comparator.comparingInt(a -> {
            Integer offset = null;
            if (a.locations() != null && !a.locations().isEmpty()) {
                offset = a.locations().get(0).offset();
            }
            return offset != null ? offset.intValue() : Integer.MAX_VALUE;
        }));


        StringBuilder highlightedText = new StringBuilder();
        int lastIndex = 0;

        for (Annotation annotation : annotations) {

            if (annotation.locations() == null || annotation.locations().isEmpty()) {
                continue;
            }

            com.example.bioproject1.dto.Location location = annotation.locations().get(0);

            Integer offsetInteger = location.offset();
            if (offsetInteger == null) {
                continue;
            }
            int start = offsetInteger.intValue();

            // --- (수정) length() 반환값(Integer)을 null 체크 후 int로 변환 ---
            Integer lengthInteger = location.length();
            int len = (lengthInteger != null) ? lengthInteger.intValue() : 0; // null이면 길이를 0으로 간주
            int end = start + len;

            if (end > text.length()) {
                end = text.length();
            }

            if (start >= text.length()) {
                continue;
            }

            if (start >= end) {
                continue;
            }

            if (start >= lastIndex) {
                highlightedText.append(escapeHtml(text.substring(lastIndex, start)));
                highlightedText.append(HIGHLIGHT_START);
                highlightedText.append(escapeHtml(text.substring(start, end)));
                highlightedText.append(HIGHLIGHT_END);
                lastIndex = end;
            } else {
                System.out.println("Warning: Skipping overlapping annotation at offset " + start + " (lastIndex: " + lastIndex + ")");
            }
        }

        if (lastIndex < text.length()) {
            highlightedText.append(escapeHtml(text.substring(lastIndex)));
        }

        return highlightedText.toString();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}

