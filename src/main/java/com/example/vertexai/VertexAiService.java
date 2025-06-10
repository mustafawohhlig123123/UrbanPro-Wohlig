package com.example.vertexai;

import com.google.api.gax.rpc.ResourceExhaustedException;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VertexAiService {
   private final VertexAI vertexAi;

   public VertexAiService(VertexAI vertexAi) {
      this.vertexAi = vertexAi;
   }

   public GenerateContentResponse generateContent(byte[] chunkBytes, String system, String userInstr, int chunkNumber) throws InterruptedException {
      int maxRetries = 3;
      int retryCount = 0;
      long waitTime = 1000L;

      while(retryCount < maxRetries) {
         try {
            GenerativeModel model = ModelHelper.buildModel(this.vertexAi, system);
            GenerateContentResponse response = model.generateContent(ContentMaker.fromMultiModalData(new Object[]{PartMaker.fromMimeTypeAndData("video/mp4", chunkBytes), userInstr}));
            System.out.println("Response for chunk " + chunkNumber + ": " + response.getCandidates(0).getContent().getParts(0).getText());
            return response;
         } catch (ResourceExhaustedException var12) {
            ++retryCount;

            try {
               Thread.sleep(waitTime);
            } catch (InterruptedException var11) {
               Thread.currentThread().interrupt();
               throw new RuntimeException("Thread interrupted during backoff", var11);
            }

            System.err.println("Resource exhausted for chunk " + chunkNumber + ". Retrying in " + waitTime + "ms...");
            waitTime *= 2L;
         } catch (Exception var13) {
            ++retryCount;
            System.out.println("Error in generating content for chunk " + chunkNumber + ": " + var13.getMessage());
            System.err.println("Error in generating content for chunk " + chunkNumber + ". Retrying in " + waitTime + "ms...");
         }
      }

      throw new RuntimeException("Exceeded maximum retries due to resource exhaustion or Error in generating.");
   }

   public Map<String, Object> formatResponse(GenerateContentResponse resp) {
      String raw = resp.getCandidates(0).getContent().getParts(0).getText();
      String cleaned = raw.replace("```json", "").replace("```", "").replace("Here's the JSON output for your request:", "").trim();
      Matcher m = Pattern.compile("\\{[\\s\\S]*\\}").matcher(cleaned);
      if (!m.find()) {
         throw new IllegalStateException("No valid JSON found in Gemini response.");
      } else {
         String json = m.group();
         Gson gson = new Gson();
         return (Map)gson.fromJson(json, Map.class);
      }
   }

   public GenerateContentResponse postProcess(List<Map<String, Object>> chunksResp, String system, String userInstr) {
      int maxRetries = 3;
      int retryCount = 0;
      long backoffMillis = 1000L;

      while(retryCount < maxRetries) {
         try {
            GenerativeModel model = ModelHelper.buildModel(this.vertexAi, system);
            GenerateContentResponse response = model.generateContent(ContentMaker.fromMultiModalData(new Object[]{userInstr, chunksResp.toString()}));
            System.out.println("Final Response: " + response.getCandidates(0).getContent().getParts(0).getText());
            return response;
         } catch (Exception var11) {
            if (!var11.getMessage().contains("RESOURCE_EXHAUSTED")) {
               System.err.println("Error in generateContent for final response: " + var11.getMessage());
               var11.printStackTrace();
               throw new RuntimeException("Failed to generate content for final response", var11);
            }

            System.err.println("Resource exhausted. Retrying in " + backoffMillis + "ms...");

            try {
               Thread.sleep(backoffMillis);
            } catch (InterruptedException var10) {
               Thread.currentThread().interrupt();
               throw new RuntimeException("Thread interrupted during backoff", var10);
            }

            ++retryCount;
            backoffMillis *= 2L;
         }
      }

      throw new RuntimeException("Exceeded maximum retries due to resource exhaustion");
   }
}
