package com.example.bioproject1.controller;
import com.example.bioproject1.dto.FastaAnalysisResult;
import com.example.bioproject1.service.FastaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/fasta")
public class FastaController {

    private final FastaService fastaService;

    public FastaController(FastaService fastaService) {
        this.fastaService = fastaService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<List<FastaAnalysisResult>> analyzeFasta(@RequestParam("file") MultipartFile file) {
        try {
            List<FastaAnalysisResult> results = fastaService.analyzeFasta(file.getBytes());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
