package com.rag.document.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Slf4j
@Service
public class PdfProcessingService {

    public String extractTextFromPdf(File pdfFile) throws IOException {
        log.info("Extracting text from PDF: {}", pdfFile.getName());

        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            log.info("Extracted {} characters from PDF", text.length());
            return text;
        }
    }

    public String[] splitTextIntoChunks(String text, int chunkSize, int overlap) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }

        // Simple chunking strategy
        int length = text.length();
        int numChunks = (int) Math.ceil((double) length / (chunkSize - overlap));
        String[] chunks = new String[numChunks];

        for (int i = 0; i < numChunks; i++) {
            int start = Math.max(0, i * (chunkSize - overlap));
            int end = Math.min(length, start + chunkSize);
            chunks[i] = text.substring(start, end);
        }

        log.info("Split text into {} chunks", chunks.length);
        return chunks;
    }
}
