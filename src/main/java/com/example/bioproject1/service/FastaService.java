package com.example.bioproject1.service;

import com.example.bioproject1.dto.FastaAnalysisResult;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.io.FastaReaderHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

@Service
public class FastaService {

    public List<FastaAnalysisResult> analyzeFasta(byte[] fileBytes) throws Exception {
        // 임시 파일 생성
        File tempFile = File.createTempFile("upload", ".fasta");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(fileBytes);
        }

        // FASTA 읽기
        LinkedHashMap<String, DNASequence> sequences =
                FastaReaderHelper.readFastaDNASequence(tempFile);

        List<FastaAnalysisResult> results = new ArrayList<>();

        for (String id : sequences.keySet()) {
            DNASequence seq = sequences.get(id);
            String sequence = seq.getSequenceAsString();

            int length = sequence.length();
            double gcContent = calcGCContent(sequence);
            Map<String, Integer> codonUsage = calcCodonUsage(sequence);

            //콘솔용 테스트
            System.out.println("====== FASTA 분석 결과 ======");
            System.out.println("ID: " + id);
            System.out.println("Length: " + length);
            System.out.printf("GC Content: %.2f%%\n", gcContent);
            System.out.println("Sequence (앞 50bp): " +
                    (sequence.length() > 50 ? sequence.substring(0, 50) + "..." : sequence));
            System.out.println("Codon Usage: " + codonUsage);
            System.out.println("============================");

            results.add(new FastaAnalysisResult(id, length, gcContent, sequence, codonUsage));
        }

        return results;
    }

    private double calcGCContent(String seq) {
        long gcCount = seq.chars().filter(ch -> ch == 'G' || ch == 'C').count();
        return (double) gcCount / seq.length() * 100;
    }

    private Map<String, Integer> calcCodonUsage(String seq) {
        Map<String, Integer> codonUsage = new HashMap<>();
        for (int i = 0; i + 3 <= seq.length(); i += 3) {
            String codon = seq.substring(i, i + 3);
            codonUsage.put(codon, codonUsage.getOrDefault(codon, 0) + 1);
        }
        return codonUsage;
    }
}
