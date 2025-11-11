package com.example.bioproject1.service;

import com.example.bioproject1.dto.*; // 모든 DTO 임포트
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature; // 추가
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*; // List, ArrayList, Comparator, Arrays, LinkedList, Set 사용
import java.util.stream.Collectors;

@Service
public class PubTatorService {

    private static final String HIGHLIGHT_START = "<mark class='highlight'>";
    private static final String HIGHLIGHT_END = "</mark>";

    // (v3) 1단계: 검색 API (키워드 -> PMID)
    private static final String SEARCH_API_URL = "https://www.ncbi.nlm.nih.gov/research/pubtator3-api/search/";
    // (v3) 2단계: 추출 API (PMID -> 상세정보)
    private static final String EXPORT_API_URL = "https://www.ncbi.nlm.nih.gov/research/pubtator3-api/publications/export/biocjson";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public PubTatorService() {
        this.objectMapper = new ObjectMapper();
        // DTO에 없는 필드가 JSON에 있어도 에러가 나지 않도록 설정
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * 2단계 API 호출 로직 (v3 검색 -> v3 추출)
     */
    public List<HighlightedAbstract> searchAndHighlight(String keywords) throws IOException, InterruptedException {

        System.out.println("--- (V7 - 접촉 병합 방지(>=) / Identifier 필터) 로직 실행 중 ---");

        // --- 1단계: 키워드(free text)로 PMID 목록 검색 ---
        URI searchUri = UriComponentsBuilder.fromHttpUrl(SEARCH_API_URL)
                .queryParam("text", keywords)
                .queryParam("limit", "10") // 10개로 제한
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
            System.out.println("1단계 (검색) 결과 없음.");
            return new ArrayList<>();
        }

        String pmids = searchResult.results().stream()
                .map(SearchResult::pmid)
                .collect(Collectors.joining(","));

        System.out.println("1단계 성공: 10개 PMID 획득 (" + pmids + ")");


        // --- 2단계: 획득한 PMID로 상세 정보(BioC JSON) 추출 ---
        URI exportUri = UriComponentsBuilder.fromHttpUrl(EXPORT_API_URL)
                .queryParam("pmids", pmids)
                .build(false)
                .toUri();

        System.out.println("2단계 (추출) API 요청 URI: " + exportUri);
        HttpRequest exportRequest = HttpRequest.newBuilder().uri(exportUri).GET().build();
        HttpResponse<String> exportResponse = httpClient.send(exportRequest, HttpResponse.BodyHandlers.ofString());

        if (exportResponse.statusCode() != 200) {
            throw new RuntimeException("2단계 (추출) API 호출 실패: " + exportResponse.statusCode() + " - " + exportResponse.body());
        }

        String exportBody = exportResponse.body();
        List<BioCDocument> documents;

        try {
            // 원본 JSON 구조 {"PubTator3": [...]} 에 맞는 DTO로 파싱
            PubTator3ExportResponse responseDto = objectMapper.readValue(exportBody, PubTator3ExportResponse.class);

            // DTO의 필드명(documents)에 맞춰 'documents()' 호출로 변경
            documents = responseDto.documents();

            System.out.println("파싱 성공 (PubTator3ExportResponse DTO)");

        } catch (Exception e) {
            System.err.println("!!! 2단계 (추출) JSON 파싱 실패: " + e.getMessage());
            System.out.println("--- 2단계 원본 응답 ---");
            System.out.println(exportBody);
            System.out.println("----------------------");
            documents = new ArrayList<>();
        }

        List<HighlightedAbstract> finalResults = new ArrayList<>();
        if (documents == null || documents.isEmpty()) {
            System.out.println("API 결과에 documents가 없거나 비어있습니다. (응답 본문 확인 필요)");
            return finalResults;
        }

        // --- (수정) 3단계: 하이라이팅 로직 변경 ---
        for (BioCDocument doc : documents) {
            String title = "";
            List<BioCPassage> abstractPassages = new ArrayList<>();

            for (BioCPassage passage : doc.passages()) {
                if (passage.infons() != null && "title".equalsIgnoreCase(passage.infons().type())) {
                    title = passage.text();
                } else if (passage.infons() != null && "abstract".equalsIgnoreCase(passage.infons().type())) {
                    // 초록 구절들을 리스트에 수집 (결합은 나중에)
                    abstractPassages.add(passage);
                }
            }

            if (!abstractPassages.isEmpty()) {
                // (NEW) 새 헬퍼 메소드 호출
                MergeResult mergeResult = buildCombinedAbstractAndIntervals(abstractPassages);
                String highlightedHtml = renderHighlight(mergeResult.combinedText, mergeResult.intervals);

                // (수정) 프론트엔드(index.html)가 사용하는 'abstractHtml' 필드명으로 DTO 생성
                finalResults.add(new HighlightedAbstract(doc.id(), title, highlightedHtml));
            }
        }
        // --- (수정 완료) ---

        System.out.println(keywords + " (사용한 쿼리: " + keywords + ")로 " + finalResults.size() + "개 논문 처리 완료.");
        return finalResults;
    }


    // XSS 방지
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    // --- (NEW) 제안해주신 헬퍼 메소드들 추가 ---

    // 보정된 구간 목록과 결합 텍스트를 담는 간단 DTO
    static class MergeResult {
        final String combinedText;
        final List<int[]> intervals;
        MergeResult(String t, List<int[]> iv) { this.combinedText = t; this.intervals = iv; }
    }

    /**
     * (NEW) abstract passage들을 결합하고, 각 annotation의 offset을 base로 보정
     * (제안해주신 로직 기반으로 수정)
     */
    private MergeResult buildCombinedAbstractAndIntervals(List<BioCPassage> abstractPassages) {
        StringBuilder sb = new StringBuilder();
        List<int[]> allIntervals = new ArrayList<>();
        int baseOffset = 0;
        boolean firstPassage = true;

        for (BioCPassage passage : abstractPassages) {
            String passageText = Optional.ofNullable(passage.text()).orElse("");
            if (passageText.isEmpty() && (passage.annotations() == null || passage.annotations().isEmpty())) {
                continue; // 텍스트도 주석도 없는 빈 구절은 건너뜀
            }

            if (!firstPassage) {
                // passage 사이에 공백 하나 넣어 문장 경계 보존 (offset 보정에 반영)
                sb.append(" ");
                baseOffset += 1; // 공백 1칸 추가
            }
            firstPassage = false;

            // 이 passage의 text를 붙이고, 이 순간의 base를 기록
            int currentPassageBase = baseOffset;
            sb.append(passageText);

            // 이 passage 내 annotation들을 base로 보정해서 intervals에 추가
            if (passage.annotations() != null) {
                for (Annotation ann : passage.annotations()) {
                    String type = ann.infons() != null ? ann.infons().type() : null;
                    Boolean valid = (ann.infons() != null && ann.infons().valid() != null) ? ann.infons().valid() : true;
                    String annText = ann.text();
                    String identifier = ann.infons() != null ? ann.infons().identifier() : null; // (NEW)


                    // (필터링 1) 핵심 타입이 아니면 건너뜀
                    if (type == null || !isCoreType(type)) continue;
                    // (필터링 2) 텍스트가 너무 짧으면 건너뜀
                    if (annText == null || annText.trim().length() <= 2) continue;
                    // (필터링 3) API가 명시적으로 'invalid'라고 한 것은 건너뜀
                    if (!valid) continue;
                    // (필터링 4) (NEW) 식별자가 없는 저품질 주석 건너뜀
                    if (identifier == null || identifier.equals("-")) continue;

                    if (ann.locations() == null) continue;
                    for (com.example.bioproject1.dto.Location loc : ann.locations()) {
                        Integer off = loc.offset();
                        Integer len = loc.length();
                        if (off == null || len == null || len <= 0) continue;

                        // ★ passage 기준 offset을 combined 기준으로 보정
                        int start = currentPassageBase + off;
                        int end = start + len;
                        allIntervals.add(new int[]{start, end});
                    }
                }
            }
            // 다음 passage의 base는 현재 텍스트의 총 길이가 됨
            baseOffset = sb.length();
        }

        // 겹치는 구간 병합(접촉은 병합 X)
        List<int[]> merged = mergeOverlaps(allIntervals, sb.length());
        return new MergeResult(sb.toString(), merged);
    }

    /**
     * (NEW) 하이라이트할 타입인지 확인
     * (제안해주신 로직 기반)
     */
    private boolean isCoreType(String type) {
        // PubTator 웹사이트가 하이라이트하는 주요 타입들
        return "Gene".equalsIgnoreCase(type)
                || "Disease".equalsIgnoreCase(type)
                || "Chemical".equalsIgnoreCase(type)
                || "Species".equalsIgnoreCase(type)
                || "Phenotype".equalsIgnoreCase(type) // (예: amyloid accumulation)
                || "Mutation".equalsIgnoreCase(type)
                || "CellLine".equalsIgnoreCase(type)
                || "SequenceVariant".equalsIgnoreCase(type)
                || "Protein".equalsIgnoreCase(type);
    }

    /**
     * (NEW) 겹침 병합 (접촉은 병합하지 않음)
     * (제안해주신 로직 기반 + "접촉 병합" 오류 수정)
     */
    private List<int[]> mergeOverlaps(List<int[]> intervals, int textLen) {
        if (intervals == null || intervals.isEmpty()) return Collections.emptyList();

        intervals.sort(Comparator.comparingInt(a -> a[0]));

        LinkedList<int[]> merged = new LinkedList<>();
        for (int[] cur : intervals) {

            // --- (수정) "접촉 병합" 버그 수정 ---
            // 'cur[0] > ...' (겹침만 병합) -> 'cur[0] >= ...' (겹침+접촉 병합)
            // 아니요, '접촉'을 방지하려면 'cur[0] >'가 맞습니다.
            // "al. In FAD, mutation"은 '접촉'이 아니라 '겹침' 문제였습니다.
            // (잘못된 진단 수정)
            // 'se activity, nd' -> [se activity,] [nd]
            // "접촉"을 방지하려면 cur[0] >= (X) -> cur[0] > (O)

            // (V6 코드) cur[0] > merged.getLast()[1]
            // (V5 코드) cur[0] >= merged.getLast()[1]

            // "se activity, nd"가 붙는 문제를 해결하려면
            // [se activity,] (100, 110)
            // [ nd] (110, 113)
            // cur[0] (110) > merged.getLast()[1] (110) -> false -> 병합됨 (V6의 버그)

            // "접촉"을 방지하려면 cur[0] >= merged.getLast()[1]
            // cur[0] (110) >= merged.getLast()[1] (110) -> true -> 병합 안 됨
            if (merged.isEmpty() || cur[0] >= merged.getLast()[1]) {
                merged.add(new int[]{cur[0], cur[1]});
            } else {
                // 겹치는 경우 (start < last.end), 마지막 구간의 끝(end)을 더 먼 쪽으로 확장
                merged.getLast()[1] = Math.max(merged.getLast()[1], cur[1]);
            }
            // --- (수정 완료) ---
        }

        // 범위 방어
        for (int[] iv : merged) {
            iv[0] = Math.max(0, Math.min(iv[0], textLen));
            iv[1] = Math.max(0, Math.min(iv[1], textLen));
        }
        return merged;
    }

    /**
     * (NEW) 병합된 구간 리스트를 HTML로 렌더링
     * (제안해주신 로직 기반)
     */
    private String renderHighlight(String text, List<int[]> intervals) {
        if (text == null || text.isEmpty()) return "";
        if (intervals == null || intervals.isEmpty()) return escapeHtml(text);

        StringBuilder out = new StringBuilder();
        int last = 0;
        for (int[] iv : intervals) {
            int s = iv[0]; // (병합 시 이미 범위 방어 완료됨)
            int e = iv[1];
            if (s < last) s = last;
            if (s >= e) continue;

            out.append(escapeHtml(text.substring(last, s)));
            out.append(HIGHLIGHT_START).append(escapeHtml(text.substring(s, e))).append(HIGHLIGHT_END);
            last = e;
        }
        if (last < text.length()) out.append(escapeHtml(text.substring(last)));
        return out.toString();
    }
}