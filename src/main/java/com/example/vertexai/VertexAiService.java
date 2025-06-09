package com.example.vertexai;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;

public class VertexAiService {
    private final VertexAI vertexAi;

    public VertexAiService(VertexAI vertexAi) {
        this.vertexAi = vertexAi;
    }

    public GenerateContentResponse generateContent(byte[] chunkBytes, String system, String userInstr) {
        try {
            // System.out.println("Generating content for chunk" + userInstr);
            GenerativeModel model = ModelHelper.buildModel(vertexAi, system);
            GenerateContentResponse response = model.generateContent(
                    ContentMaker.fromMultiModalData(
                            PartMaker.fromMimeTypeAndData("video/mp4", chunkBytes),
                            userInstr));
            System.out.println("Response: " + response.getCandidates(0).getContent().getParts(0).getText());
            // System.out.println("Response: " + response);
            return response;
        } catch (Exception e) {
            // Log the error
            System.err.println("Error in generateContent for chunk " + e.getMessage());
            e.printStackTrace();
            // Rethrow as an unchecked exception so upstream can handle or fail fast
            throw new RuntimeException("Failed to generate content for chunk ", e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> formatResponse(GenerateContentResponse resp) {
        // 1. Extract the raw text from the first candidate
        String raw = resp.getCandidates(0)
                .getContent()
                .getParts(0)
                .getText();

        // 2. Clean out markdown fences and preamble
        String cleaned = raw
                .replace("```json", "")
                .replace("```", "")
                .replace("Here's the JSON output for your request:", "")
                .trim();

        // 3. Find the JSON object within the cleaned string
        Matcher m = Pattern.compile("\\{[\\s\\S]*\\}").matcher(cleaned);
        if (!m.find()) {
            throw new IllegalStateException("No valid JSON found in Gemini response.");
        }
        String json = m.group();

        // 4. Parse into a Map
        Gson gson = new Gson();
        return gson.<Map<String, Object>>fromJson(json, Map.class);
    }

    public GenerateContentResponse postProcess(Map<String, Object> chunksResp, String system, String userInstr) {
        // call your FinalAnswer logic here
        try {
            GenerativeModel model = ModelHelper.buildModel(vertexAi, system);
            GenerateContentResponse response = model.generateContent(
                ContentMaker.fromMultiModalData(new Object[]{userInstr, chunksResp.toString()})
            );
            System.out.println("Final Response: " + response.getCandidates(0).getContent().getParts(0).getText());
            return response;
        } catch (Exception e) {
            // Log the error
            System.err.println("Error in generateContent for final response " + e.getMessage());
            e.printStackTrace();
            // Rethrow as an unchecked exception so upstream can handle or fail fast
            throw new RuntimeException("Failed to generate content for final response ", e);
        }
    }
}
