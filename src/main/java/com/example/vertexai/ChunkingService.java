package com.example.vertexai;

import com.google.cloud.vertexai.api.GenerateContentResponse;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;

public class ChunkingService {
    private static final int CHUNK_SEC = 900; // 15 minutes;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    
    public ChunkingService() {}

    public Map<String, Object> splitAndReview(byte[] videoBytes, VertexAiService vertexSvc) throws Exception {
        int totalSec = probeDuration(videoBytes);
        if (totalSec <= CHUNK_SEC) {
            System.out.println("Video is less than " + CHUNK_SEC);
            // No need to keep the pool running if you won’t submit more tasks:
            executor.shutdown(); 
            return reviewSingle(videoBytes, 1, vertexSvc);
        }

        int chunks = (totalSec + CHUNK_SEC - 1) / CHUNK_SEC;
        List<Future<Map<String, Object>>> futures = new ArrayList<>();

        for (int i = 0; i < chunks; i++) {
            final int idx = i + 1;
            final int startSec = i * CHUNK_SEC;
            futures.add(executor.submit(() -> {
                byte[] chunk = extractChunk(videoBytes, startSec, CHUNK_SEC);
                return reviewSingle(chunk, idx, vertexSvc);
            }));
        }

        // wait for all
        List<Map<String, Object>> results = new ArrayList<>();
        for (Future<Map<String, Object>> f : futures) {
            results.add(f.get());
        }

        executor.shutdown();
        System.out.println("Results: " + results + ".");
        String system = PromptHelper.getFinalSysInstruction();
        String userInstr = PromptHelper.getFinalUserPrompt();

        GenerateContentResponse finalResp = vertexSvc.postProcess(results, system, userInstr);
        
        // aggregate violations
        // @SuppressWarnings("unchecked")
        // List<Map<String, Object>> allViolations = results.stream()
        //         .filter(r -> "REJECTED".equals(r.get("status")))
        //         .flatMap(r -> ((List<Map<String, Object>>) r.get("violations")).stream())
        //         .collect(Collectors.toList());

        // Map<String, Object> finalResult = new HashMap<>();
        // if (allViolations.isEmpty()) {
        //     finalResult.put("status", "APPROVED");
        //     finalResult.put("violations", Collections.emptyList());
        //     return finalResult;
        // } else {
        //     finalResult.put("status", "REJECTED");
        //     finalResult.put("violations", allViolations);
        // }

        // GenerateContentResponse finalResp = vertexSvc.postProcess(finalResult, system, userInstr);
        
       
        return vertexSvc.formatResponse(finalResp);
    }

    private Map<String, Object> reviewSingle(byte[] chunkBytes, int chunkNumber, VertexAiService vertexSvc) throws Exception {
        System.out.println("Reviewing chunk " + chunkNumber);
        String system = PromptHelper.getSystemInstruction();
        String userInstr = PromptHelper.getUserInstruction(chunkNumber);

        GenerateContentResponse resp = vertexSvc.generateContent(chunkBytes, system, userInstr, chunkNumber);
        System.gc();
        return vertexSvc.formatResponse(resp);
    }

    private int probeDuration(byte[] videoBytes) throws IOException, InterruptedException {
        // write to temp file
        File tmp = File.createTempFile("video", ".mp4");
        Files.write(tmp.toPath(), videoBytes);

        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe", "-hide_banner", "-loglevel", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                tmp.getAbsolutePath());
        Process p = pb.start();
        String output = IOUtils.toString(p.getInputStream(), "UTF-8").trim();
        p.waitFor();
        tmp.delete();
        return (int) Math.floor(Double.parseDouble(output));
    }

    private byte[] extractChunk(byte[] videoBytes, int startSec, int durationSec)
            throws IOException, InterruptedException {
        // write to temp in
        File in = File.createTempFile("inn", ".mp4");
        File out = File.createTempFile("out", ".mp4");
        Files.write(in.toPath(), videoBytes);

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-hide_banner",
                "-loglevel", "error", 
                "-ss", Integer.toString(startSec),
                "-t", Integer.toString(durationSec),
                "-i", in.getAbsolutePath(),
                "-c:v", "copy",
                "-c:a", "copy",
                "-movflags", "frag_keyframe+empty_moov",
                out.getAbsolutePath());
        Process p = pb.inheritIO().start();
        p.waitFor();

        byte[] chunk = Files.readAllBytes(out.toPath());
        in.delete();
        out.delete();
        return chunk;
    }
}
