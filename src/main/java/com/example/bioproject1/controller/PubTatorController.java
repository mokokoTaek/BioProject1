package com.example.bioproject1.controller;

import com.example.bioproject1.dto.HighlightedAbstract;
import com.example.bioproject1.service.PubTatorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pubtator")
public class PubTatorController {

    private final PubTatorService pubTatorService;

    // 생성자를 통한 의존성 주입 (DI)
    public PubTatorController(PubTatorService pubTatorService) {
        this.pubTatorService = pubTatorService;
    }

    /**
     * 키워드 검색을 위한 GET 엔드포인트
     * 예: /api/pubtator/search?keywords=APOE4 Alzheimer
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchPublications(@RequestParam String keywords) {
        if (keywords == null || keywords.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "keywords 파라미터가 필요합니다."));
        }

        try {
            // 서비스 호출
            List<HighlightedAbstract> results = pubTatorService.searchAndHighlight(keywords);

            if (results.isEmpty()) {
                // 결과는 없지만 요청은 성공
                return ResponseEntity.ok(Map.of("message", "검색 결과가 없습니다."));
            }

            // 성공
            return ResponseEntity.ok(results);

        } catch (IOException | InterruptedException e) {
            // API 호출 중 발생한 예외
            e.printStackTrace(); // 서버 로그에 스택 트레이스 출력
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "API 호출 중 서버 오류 발생: " + e.getMessage()));
        } catch (RuntimeException e) {
            // API가 200이 아닌 응답(404, 400 등)을 반환했을 때
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY) // 502: Bad Gateway (외부 API 오류)
                    .body(Map.of("error", "외부 API 오류: " + e.getMessage()));
        }
    }
}
