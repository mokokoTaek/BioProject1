package com.example.bioproject1;

import com.example.bioproject1.dto.HighlightedAbstract;
import com.example.bioproject1.service.PubTatorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * PubTatorService를 테스트하기 위한 통합 테스트
 * 실제 API를 호출합니다.
 */
@SpringBootTest // 스프링 부트 컨텍스트를 로드하여 테스트
class PubTatorServiceTest {

    @Autowired // 스프링 컨테이너에서 PubTatorService 빈을 주입받음
    private PubTatorService pubTatorService;

    @Test
    @DisplayName("PubMed 키워드 검색 및 하이라이팅 콘솔 테스트")
    void searchAndHighlight_ConsoleTest() {
        // 1. Given: PubTator API 공식 문서의 예시 쿼리
        String query = "Alzheimer's Disease";
        System.out.println("테스트 시작: 쿼리 = " + query);

        try {
            // 2. When: 서비스 메소드 호출
            List<HighlightedAbstract> results = pubTatorService.searchAndHighlight(query);

            // 3. Then: 결과 확인
            assertNotNull(results, "결과 리스트는 null이 아니어야 합니다.");

            System.out.println("--- 테스트 결과 (총 " + results.size() + "개) ---");

            // 결과가 5개 미만일 수 있으므로 상위 5개 또는 전체 출력
            int count = 0;
            for (HighlightedAbstract result : results) {

                if (count >= 5) break; // 콘솔 출력을 위해 최대 5개만 표시

                System.out.println("\n[PMID: " + result.pmid() + "]");
                System.out.println("  Title: " + result.title());
                System.out.println("  Abstract (HTML): " + result.abstractHtml().substring(0, Math.min(result.abstractHtml().length(), 300)) + "...");
                count++;
            }
            System.out.println("--- 테스트 성공 ---");

        } catch (Exception e) {
            // 테스트 실패 처리
            e.printStackTrace();
            fail("API 호출 중 예외 발생: " + e.getMessage());
        }
    }
}

