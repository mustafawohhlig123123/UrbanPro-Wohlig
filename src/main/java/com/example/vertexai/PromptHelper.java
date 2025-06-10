package com.example.vertexai;

public class PromptHelper {
    public static String getSystemInstruction() {
        return "You are an AI moderation assistant for educational content. Your job is to review lecture videos for policy violations. " +
               "You must return a valid JSON response with 'status' (APPROVED/REJECTED) and 'violations' array. " +
               "If any rule is broken, set status to REJECTED and provide violation details. " +
               "All content must be safe for children and free from contact info, abusive behavior, and unprofessional conduct.";
    }

    public static String getUserInstruction(int chunkNumber) {
        int offsetMinutes = (chunkNumber - 1) * 15; // Fixed to 15 minutes per chunk

        return String.format(
            "Review the given lecture video transcript, screen content, Zoom chat, and attachment text using the STRICT rules below. " +
            "These lectures are meant for public consumption by children and students of all ages, so safety and appropriateness are critical.\n\n" +
            
            "CRITICAL: You MUST return a valid JSON response in the exact format specified below. Do not include any explanatory text outside the JSON.\n\n" +
            
            "ONLY mark the video as REJECTED if **specific and clear violations** occur as listed below:\n\n" +
            
            "1. EMAIL_IS_SHARED\n" +
            "- An actual email address is spoken, shown on screen, written in chat, or included in any attachment.\n" +
            "- Email formats include: example@gmail.com, name.lastname@domain.co.in\n" +
            "- Generic references like 'I will share my email later' are NOT violations.\n\n" +
            
            "2. NUMBER_IS_SHARED\n" +
            "- A specific phone number is spoken, shown, written, or attached.\n" +
            "- Phone number formats: 9876543210, +91-9876543210, (022) 23456789\n" +
            "- Generic phrases like 'call me anytime' without actual digits are NOT violations.\n\n" +
            
            "3. LINK_IS_SHARED\n" +
            "- A complete URL, social media handle, or UPI ID is shared.\n" +
            "- Formats: https://example.com, www.instagram.com/username, someone@upi\n" +
            "- Promises to share links later without revealing them are NOT violations.\n\n" +
            
            "4. UNPROFESSIONAL_CONDUCT\n" +
            "- Clearly inappropriate language, abusive behavior, or unsuitable content for children.\n" +
            "- Background distractions, pricing discussions, or bilingual explanations are NOT violations.\n\n" +
            
            "5. USE_OF_ABUSIVE_LANGUAGE\n" +
            "- Rude, insulting, or disrespectful speech directed at individuals.\n" +
            "- General encouragement or neutral statements are NOT violations.\n\n" +
            
            "TIMESTAMP RULES:\n" +
            "- Return timestamps in mm:ss format\n" +
            "- This is chunk number %d representing 15 minutes of video\n" +
            "- Add %d minutes offset to get full-video timestamp\n" +
            "- Example: chunk 2 timestamp 02:15 = full-video timestamp 17:15\n\n" +
            
            "REQUIRED OUTPUT FORMAT - Return ONLY this JSON structure:\n" +
            "{\n" +
            "  \"status\": \"APPROVED\",\n" +
            "  \"violations\": []\n" +
            "}\n\n" +
            
            "OR if violations found:\n" +
            "{\n" +
            "  \"status\": \"REJECTED\",\n" +
            "  \"violations\": [\n" +
            "    {\n" +
            "      \"violation_type\": \"EMAIL_IS_SHARED\",\n" +
            "      \"timestamp\": \"12:47\",\n" +
            "      \"description\": \"Tutor said: 'You can email me at tutorname@gmail.com'\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n\n" +
            
            "IMPORTANT: Return ONLY valid JSON. No markdown, no explanations, no extra text.",
            chunkNumber, offsetMinutes
        );
    }

    public static String getFinalSysInstruction() {
        return "You are an assistant that merges chunk-based video moderation results into a final decision. " +
               "You must return a valid JSON response with the final status and consolidated violations. " +
               "Each chunk response is in valid JSON format with 'status' and 'violations' fields.";
    }

    public static String getFinalUserPrompt() {
        return "You are merging multiple chunk-based video moderation responses into a final decision.\n\n" +
               
               "Each chunk result has this structure:\n" +
               "{\n" +
               "  \"status\": \"APPROVED\" | \"REJECTED\",\n" +
               "  \"violations\": [\n" +
               "    {\n" +
               "      \"violation_type\": \"EMAIL_IS_SHARED\" | \"NUMBER_IS_SHARED\" | \"LINK_IS_SHARED\" | \"UNPROFESSIONAL_CONDUCT\" | \"USE_OF_ABUSIVE_LANGUAGE\",\n" +
               "      \"timestamp\": \"mm:ss\",\n" +
               "      \"description\": \"string\"\n" +
               "    }\n" +
               "  ]\n" +
               "}\n\n" +
               
               "MERGING RULES:\n" +
               "- If ANY chunk is REJECTED, final status is REJECTED\n" +
               "- If ALL chunks are APPROVED, final status is APPROVED\n" +
               "- Merge all violations from all chunks (remove duplicates based on description)\n" +
               "- Convert timestamps from mm:ss to hh:mm:ss format\n\n" +
               
               "REQUIRED OUTPUT FORMAT - Return ONLY this JSON:\n" +
               "{\n" +
               "  \"status\": \"APPROVED\",\n" +
               "  \"violations\": []\n" +
               "}\n\n" +
               
               "OR if violations exist:\n" +
               "{\n" +
               "  \"status\": \"REJECTED\",\n" +
               "  \"violations\": [\n" +
               "    {\n" +
               "      \"violation_type\": \"EMAIL_IS_SHARED\",\n" +
               "      \"timestamp\": \"00:12:47\",\n" +
               "      \"description\": \"Tutor shared email address\"\n" +
               "    }\n" +
               "  ]\n" +
               "}\n\n" +
               
               "IMPORTANT: Return ONLY valid JSON. No markdown, no explanations, no extra text.\n\n" +
               
               "Process these chunk results:";
    }
}