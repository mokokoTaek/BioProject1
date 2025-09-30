package com.example.bioproject1.dto;

import java.util.Map;

public class FastaAnalysisResult {
    private String id;
    private int length;
    private double gcContent;
    private String sequence;
    private Map<String, Integer> codonUsage;

    public FastaAnalysisResult(String id, int length, double gcContent,
                               String sequence, Map<String, Integer> codonUsage) {
        this.id = id;
        this.length = length;
        this.gcContent = gcContent;
        this.sequence = sequence;
        this.codonUsage = codonUsage;
    }

    public String getId() { return id; }
    public int getLength() { return length; }
    public double getGcContent() { return gcContent; }
    public String getSequence() { return sequence; }
    public Map<String, Integer> getCodonUsage() { return codonUsage; }
}
