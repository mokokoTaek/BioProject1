package com.example.bioproject1.controller;

import com.example.bioproject1.dto.HighlightedAbstract;
import com.example.bioproject1.service.PubTatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * PubTator API 검색을 위한 REST 컨트롤러
 * @RestController 어노테이션이 이 클래스를 API 엔드포인트로 등록합니다.
 */
@RestController
@RequestMapping("/api") // 이 클래스의 모든 경로는 /api 로 시작합니다.
public class PubTatorController {

    private final PubTatorService pubTatorService;

    // @Autowired를 통해 PubTatorService를 주입받습니다.
    @Autowired
    public PubTatorController(PubTatorService pubTatorService) {
        this.pubTatorService = pubTatorService;
    }

    /**
     * /api/search 경로로 GET 요청을 처리합니다.
     * @param keywords 'keywords'라는 쿼리 파라미터를 받습니다.
     * @return 하이라이트된 초록 리스트를 JSON 형태로 반환합니다.
     */
    @GetMapping("/search")
    public ResponseEntity<List<HighlightedAbstract>> search(@RequestParam String keywords) {
        try {
            List<HighlightedAbstract> results = pubTatorService.searchAndHighlight(keywords);
            if (results.isEmpty()) {
                // 결과는 있으나 내용이 없는 경우 (200 OK와 빈 리스트 반환)
                return ResponseEntity.ok(results);
            }
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            // API 호출 실패 등 서버 오류가 발생한 경우 (500 Internal Server Error)
            e.printStackTrace(); // 서버 로그에 에러 출력
            return ResponseEntity.status(500).body(null); // 클라이언트에게는 null 반환
        }
    }
}
