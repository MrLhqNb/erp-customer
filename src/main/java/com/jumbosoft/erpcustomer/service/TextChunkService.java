package com.jumbosoft.erpcustomer.service;

import com.jumbosoft.erpcustomer.config.VectorStoreProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunkService {

    private final VectorStoreProperties properties;

    public TextChunkService(VectorStoreProperties properties) {
        this.properties = properties;
    }

    public List<String> split(String text) {
        return split(text, properties.getChunkSize(), properties.getChunkOverlap());
    }

    public List<String> split(String text, int chunkSize, int chunkOverlap) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        if (text.length() <= chunkSize) {
            List<String> result = new ArrayList<>();
            result.add(text);
            return result;
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // Try to break at sentence boundary within the last 20% of the chunk
            if (end < text.length()) {
                int boundary = findSentenceBoundary(text, end - chunkSize / 5, end);
                if (boundary > start) {
                    end = boundary;
                }
            }

            chunks.add(text.substring(start, end).trim());
            start = end - chunkOverlap;
            if (start >= text.length()) {
                break;
            }
        }
        return chunks;
    }

    private int findSentenceBoundary(String text, int searchStart, int searchEnd) {
        for (int i = searchEnd - 1; i >= searchStart && i >= 0; i--) {
            char c = text.charAt(i);
            // Chinese sentence-ending punctuation
            if (c == '。' || c == '？' || c == '！'   // 。？！
                    || c == '；' || c == '，'              // ；，
                    || c == '\n') {
                return i + 1;
            }
            // English sentence-ending punctuation
            if (c == '.' || c == '!' || c == '?') {
                // Check it's not a decimal point
                if (c == '.' && i > 0 && i < text.length() - 1
                        && Character.isDigit(text.charAt(i - 1))
                        && Character.isDigit(text.charAt(i + 1))) {
                    continue;
                }
                return i + 1;
            }
        }
        return -1;
    }
}
