package com.rag.document.service;

import com.rag.document.dto.DocumentResponse;
import com.rag.document.dto.DocumentUploadResponse;
import com.rag.document.entity.Document;
import com.rag.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final PdfProcessingService pdfProcessingService;
    private final OllamaService ollamaService;
    private final VectorStoreService vectorStoreService;

    @Value("${storage.upload-dir}")
    private String uploadDir;

    @Transactional
    public DocumentUploadResponse uploadDocument(MultipartFile file, Long userId) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (!file.getContentType().equals("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }

        // Create upload directory if not exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String filename = UUID.randomUUID().toString() + ".pdf";
        Path filePath = uploadPath.resolve(filename);

        // Save file
        file.transferTo(filePath.toFile());

        // Create document entity
        Document document = Document.builder()
                .filename(filename)
                .originalFilename(file.getOriginalFilename())
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .userId(userId)
                .status("PROCESSING")
                .build();

        document = documentRepository.save(document);
        log.info("Document saved with ID: {}", document.getId());

        // Process document asynchronously (in real application, use async processing)
        processDocument(document);

        return DocumentUploadResponse.builder()
                .id(document.getId())
                .filename(document.getOriginalFilename())
                .fileSize(document.getFileSize())
                .status(document.getStatus())
                .uploadedAt(document.getCreatedAt())
                .build();
    }

    @Transactional
    public void processDocument(Document document) {
        try {
            log.info("Processing document: {}", document.getId());

            // Extract text from PDF
            File pdfFile = new File(document.getFilePath());
            String text = pdfProcessingService.extractTextFromPdf(pdfFile);
            document.setExtractedText(text);

            // Split text into chunks
            String[] chunks = pdfProcessingService.splitTextIntoChunks(text, 500, 50);

            // Generate embeddings and store in vector database
            for (int i = 0; i < chunks.length; i++) {
                String chunk = chunks[i];
                List<Double> embedding = ollamaService.generateEmbedding(chunk);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("originalFilename", document.getOriginalFilename());
                metadata.put("totalChunks", chunks.length);

                String vectorId = vectorStoreService.storeVector(
                        document.getId(),
                        document.getUserId(),
                        i,
                        chunk,
                        embedding,
                        metadata
                );

                if (i == 0) {
                    document.setVectorId(vectorId);
                }
            }

            document.setStatus("COMPLETED");
            documentRepository.save(document);
            log.info("Document processing completed: {}", document.getId());

        } catch (Exception e) {
            log.error("Failed to process document: {}", document.getId(), e);
            document.setStatus("FAILED");
            documentRepository.save(document);
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getUserDocuments(Long userId) {
        return documentRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        return toResponse(document);
    }

    @Transactional
    public void deleteDocument(Long documentId, Long userId) throws IOException {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        // Delete file
        Path filePath = Paths.get(document.getFilePath());
        Files.deleteIfExists(filePath);

        // Delete from database
        documentRepository.delete(document);
        log.info("Document deleted: {}", documentId);
    }

    private DocumentResponse toResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .originalFilename(document.getOriginalFilename())
                .fileSize(document.getFileSize())
                .status(document.getStatus())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
