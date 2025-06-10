package com.example.vertexai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.api.gax.rpc.ResourceExhaustedException;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Candidate;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

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
            GenerateContentResponse response = model.generateContent(
                ContentMaker.fromMultiModalData(new Object[]{
                    PartMaker.fromMimeTypeAndData("video/mp4", chunkBytes), 
                    userInstr
                })
            );
            
            // Validate response before returning
            if (isValidResponse(response)) {
                System.out.println("Response for chunk " + chunkNumber + ": " + getResponseText(response));
                return response;
            } else {
                throw new RuntimeException("Invalid response received from Gemini API");
            }
            
         } catch (ResourceExhaustedException var12) {
            ++retryCount;
            System.err.println("Resource exhausted for chunk " + chunkNumber + ". Retrying in " + waitTime + "ms...");
            
            try {
               Thread.sleep(waitTime);
            } catch (InterruptedException var11) {
               Thread.currentThread().interrupt();
               throw new RuntimeException("Thread interrupted during backoff", var11);
            }
            
            waitTime *= 2L;
         } catch (Exception var13) {
            ++retryCount;
            System.err.println("Error in generating content for chunk " + chunkNumber + ": " + var13.getMessage());
            
            if (retryCount < maxRetries) {
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted during backoff", e);
                }
                waitTime *= 2L;
            }
         }
      }

      throw new RuntimeException("Exceeded maximum retries for chunk " + chunkNumber);
   }

   public Map<String, Object> formatResponse(GenerateContentResponse resp) {
      try {
          if (!isValidResponse(resp)) {
              return createErrorResponse("Empty or invalid response from Gemini API");
          }
          
          String raw = getResponseText(resp);
          if (raw == null || raw.trim().isEmpty()) {
              return createErrorResponse("Empty response text from Gemini API");
          }
          
          // Clean the response
          String cleaned = cleanJsonResponse(raw);
          
          // Try to parse as JSON
          Gson gson = new Gson();
          try {
              Map<String, Object> result = gson.fromJson(cleaned, Map.class);
              if (result == null) {
                  return createErrorResponse("Failed to parse JSON - null result");
              }
              return result;
          } catch (JsonSyntaxException e) {
              System.err.println("JSON parsing failed. Raw response: " + raw);
              System.err.println("Cleaned response: " + cleaned);
              return createErrorResponse("Invalid JSON format: " + e.getMessage());
          }
          
      } catch (Exception e) {
          System.err.println("Error in formatResponse: " + e.getMessage());
          e.printStackTrace();
          return createErrorResponse("Response formatting error: " + e.getMessage());
      }
   }

   public GenerateContentResponse postProcess(List<Map<String, Object>> chunksResp, String system, String userInstr) {
      int maxRetries = 3;
      int retryCount = 0;
      long backoffMillis = 1000L;

      while(retryCount < maxRetries) {
         try {
            GenerativeModel model = ModelHelper.buildModel(this.vertexAi, system);
            
            // Create the input content properly
            String inputData = userInstr + "\n\nChunk Results: " + chunksResp.toString();
            
            GenerateContentResponse response = model.generateContent(
                ContentMaker.fromString(inputData)
            );
            
            // Validate response
            if (isValidResponse(response)) {
                System.out.println("Final Response: " + getResponseText(response));
                return response;
            } else {
                throw new RuntimeException("Invalid response received from final processing");
            }
            
         } catch (ResourceExhaustedException e) {
            System.err.println("Resource exhausted in postProcess. Retrying in " + backoffMillis + "ms...");
            
            try {
               Thread.sleep(backoffMillis);
            } catch (InterruptedException var10) {
               Thread.currentThread().interrupt();
               throw new RuntimeException("Thread interrupted during backoff", var10);
            }

            ++retryCount;
            backoffMillis *= 2L;
            
         } catch (Exception var11) {
            System.err.println("Error in postProcess: " + var11.getMessage());
            var11.printStackTrace();
            
            ++retryCount;
            if (retryCount < maxRetries) {
                try {
                    Thread.sleep(backoffMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted during backoff", e);
                }
                backoffMillis *= 2L;
            }
         }
      }

      throw new RuntimeException("Exceeded maximum retries in postProcess due to errors");
   }
   
   // Helper methods
   private boolean isValidResponse(GenerateContentResponse response) {
       if (response == null) return false;
       
       List<Candidate> candidates = response.getCandidatesList();
       if (candidates == null || candidates.isEmpty()) return false;
       
       Candidate candidate = candidates.get(0);
       if (candidate == null) return false;
       
       Content content = candidate.getContent();
       if (content == null) return false;
       
       return content.getPartsCount() > 0;
   }
   
   private String getResponseText(GenerateContentResponse response) {
       if (!isValidResponse(response)) return null;
       
       try {
           return response.getCandidates(0).getContent().getParts(0).getText();
       } catch (Exception e) {
           System.err.println("Error extracting text from response: " + e.getMessage());
           return null;
       }
   }
   
   private String cleanJsonResponse(String raw) {
       if (raw == null) return "{}";
       
       // Remove common prefixes and suffixes
       String cleaned = raw
           .replace("```json", "")
           .replace("```", "")
           .replace("Here's the JSON output for your request:", "")
           .replace("Here is the JSON response:", "")
           .replace("JSON Response:", "")
           .trim();
       
       // Extract JSON object using regex
       Matcher matcher = Pattern.compile("\\{[\\s\\S]*\\}").matcher(cleaned);
       if (matcher.find()) {
           return matcher.group();
       }
       
       // If no JSON object found, try to extract array
       matcher = Pattern.compile("\\[[\\s\\S]*\\]").matcher(cleaned);
       if (matcher.find()) {
           return matcher.group();
       }
       
       // Return the cleaned string as-is if no JSON pattern found
       return cleaned.isEmpty() ? "{}" : cleaned;
   }
   
   private Map<String, Object> createErrorResponse(String errorMessage) {
       Map<String, Object> errorResp = new HashMap<>();
       errorResp.put("status", "ERROR");
       errorResp.put("error", errorMessage);
       errorResp.put("violations", new java.util.ArrayList<>());
       return errorResp;
   }
}