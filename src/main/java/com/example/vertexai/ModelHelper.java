package com.example.vertexai;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.HarmCategory;
import com.google.cloud.vertexai.api.SafetySetting;
import com.google.cloud.vertexai.api.SafetySetting.HarmBlockThreshold;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.api.Schema;
import com.google.cloud.vertexai.api.Type;


import java.util.Arrays;

public class ModelHelper {

  public static GenerativeModel buildModel(VertexAI vertexAi, String systemInstruction) {

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

    Schema recipeSchema = Schema.newBuilder()
        .setType(Type.OBJECT)
        .putProperties("status", Schema.newBuilder()
            .setType(Type.STRING)
            .build())
        .putProperties("violations", Schema.newBuilder()
            .setType(Type.ARRAY)
            .setItems(Schema.newBuilder()
                .setType(Type.OBJECT)
                .putProperties("timestamp", Schema.newBuilder().setType(Type.STRING).build())
                .putProperties("violation_type", Schema.newBuilder().setType(Type.STRING).build())
                .build())
            .build())
        .addRequired("status")
        .build();
    
        
    GenerationConfig config = GenerationConfig.newBuilder()
        .setMaxOutputTokens(50092)
        .setTemperature(0.0f)
        .setTopP(0.95f)
        .setSeed(0)
        .setResponseMimeType("application/json")
        .setResponseSchema(recipeSchema)
        .build();

    return new GenerativeModel.Builder()
        .setModelName("gemini-2.5-pro-preview-05-06")
        .setVertexAi(vertexAi)
        .setSystemInstruction(ContentMaker.fromString(systemInstruction))
        .setGenerationConfig(config)
        .setSafetySettings(Arrays.asList(hateSpeech, dangerousContent, sexuallyExplicit, harassment))
        .build();
  }
}
//  private static SafetySetting createSafetySetting(HarmCategory category) {
//     return SafetySetting.newBuilder()
//         .setCategory(category)
//         .setThreshold(HarmBlockThreshold.BLOCK_NONE)
//         .build();
//   }
// }
