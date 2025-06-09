package com.example.vertexai;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class LectureReviewService {

    public Map<String, Object> reviewLecture(String urlStr, VertexAiService vertexSvc) throws Exception {
        try {
            ChunkingService chunkSvc = new ChunkingService();
            byte[] videoBytes = downloadVideoAsBytes(urlStr);
            return chunkSvc.splitAndReview(videoBytes, vertexSvc);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return errorMap;
        }
    }

    public static byte[] downloadVideoAsBytes(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        try (InputStream in = url.openStream(); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192]; // buffer size
            int bytesRead;
            while ((bytesRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            return buffer.toByteArray(); // returns the video content in memory
        }
    }

}
