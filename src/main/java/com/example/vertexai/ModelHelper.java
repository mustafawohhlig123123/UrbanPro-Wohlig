package com.example.vertexai;

import java.util.Arrays;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.HarmCategory;
import com.google.cloud.vertexai.api.SafetySetting;
import com.google.cloud.vertexai.api.SafetySetting.HarmBlockThreshold;
import com.google.cloud.vertexai.api.Schema;
import com.google.cloud.vertexai.api.Type;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;

public class ModelHelper {

    public static GenerativeModel buildModel(VertexAI vertexAi, String systemInstruction) {
        try {
            // Define safety settings
            SafetySetting hateSpeech = SafetySetting.newBuilder()
                .setCategory(HarmCategory.HARM_CATEGORY_HATE_SPEECH)
                .setThreshold(HarmBlockThreshold.BLOCK_NONE)
                .build();

            SafetySetting dangerousContent = SafetySetting.newBuilder()
                .setCategory(HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT)
                .setThreshold(HarmBlockThreshold.BLOCK_NONE)
                .build();

            SafetySetting sexuallyExplicit = SafetySetting.newBuilder()
                .setCategory(HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT)
                .setThreshold(HarmBlockThreshold.BLOCK_NONE)
                .build();

            SafetySetting harassment = SafetySetting.newBuilder()
                .setCategory(HarmCategory.HARM_CATEGORY_HARASSMENT)
                .setThreshold(HarmBlockThreshold.BLOCK_NONE)
                .build();

            // Define violation schema
            Schema violationSchema = Schema.newBuilder()
                .setType(Type.OBJECT)
                .putProperties("violation_type", Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription("Type of violation detected")
                    .build())
                .putProperties("timestamp", Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription("Timestamp in mm:ss format")
                    .build())
                .putProperties("description", Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription("Description of the violation")
                    .build())
                .addRequired("violation_type")
                .addRequired("timestamp")
                .addRequired("description")
                .build();

            // Full response schema with assignments
            Schema responseSchema = Schema.newBuilder()
                .setType(Type.OBJECT)
                .putProperties("status", Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription("APPROVED or REJECTED")
                    .build())
                .putProperties("summary", Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription("Parent-friendly summary of what was taught in this chunk")
                    .build())
                .putProperties("assignments", Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription("Assignments given by the teacher for after-class completion")
                    .build())
                .putProperties("violations", Schema.newBuilder()
                    .setType(Type.ARRAY)
                    .setItems(violationSchema)
                    .setDescription("Array of violations found")
                    .build())
                .addRequired("status")
                .addRequired("summary")
                .addRequired("assignments")
                .addRequired("violations")
                .build();

            GenerationConfig config = GenerationConfig.newBuilder()
                .setMaxOutputTokens(8192)
                .setTemperature(0.1f)
                .setTopP(0.95f)
                .setTopK(40)
                .setResponseMimeType("application/json")
                .setResponseSchema(responseSchema)
                .build();

            return new GenerativeModel.Builder()
                .setModelName("gemini-2.5-pro-preview-05-06")
                .setVertexAi(vertexAi)
                .setSystemInstruction(ContentMaker.fromString(systemInstruction))
                .setGenerationConfig(config)
                .setSafetySettings(Arrays.asList(hateSpeech, dangerousContent, sexuallyExplicit, harassment))
                .build();

        } catch (Exception e) {
            System.err.println("Error building GenerativeModel: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to build GenerativeModel", e);
        }
    }

    public static GenerativeModel buildFinalModel(VertexAI vertexAi, String systemInstruction) {
        try {
            // Simpler final schema
            Schema finalViolationSchema = Schema.newBuilder()
                .setType(Type.OBJECT)
                .putProperties("violation_type", Schema.newBuilder()
                    .setType(Type.STRING)
                    .build())
                .putProperties("timestamp", Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription("Timestamp in hh:mm:ss format")
                    .build())
                .putProperties("description", Schema.newBuilder()
                    .setType(Type.STRING)
                    .build())
                .addRequired("violation_type")
                .addRequired("timestamp")
                .addRequired("description")
                .build();

            Schema finalResponseSchema = Schema.newBuilder()
                .setType(Type.OBJECT)
                .putProperties("status", Schema.newBuilder()
                    .setType(Type.STRING)
                    .build())
                .putProperties("summary", Schema.newBuilder()
                    .setType(Type.STRING)
                    .build())
                .putProperties("assignments", Schema.newBuilder()
                    .setType(Type.STRING)
                    .build())
                .putProperties("violations", Schema.newBuilder()
                    .setType(Type.ARRAY)
                    .setItems(finalViolationSchema)
                    .build())
                .addRequired("status")
                .addRequired("summary")
                .addRequired("assignments")
                .addRequired("violations")
                .build();

            GenerationConfig config = GenerationConfig.newBuilder()
                .setMaxOutputTokens(8192)
                .setTemperature(0.0f)
                .setTopP(0.95f)
                .setResponseMimeType("application/json")
                .setResponseSchema(finalResponseSchema)
                .build();

            return new GenerativeModel.Builder()
                .setModelName("gemini-2.0-flash-001")
                .setVertexAi(vertexAi)
                .setSystemInstruction(ContentMaker.fromString(systemInstruction))
                .setGenerationConfig(config)
                .build();

        } catch (Exception e) {
            System.err.println("Error building final GenerativeModel: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to build final GenerativeModel", e);
        }
    }
}
