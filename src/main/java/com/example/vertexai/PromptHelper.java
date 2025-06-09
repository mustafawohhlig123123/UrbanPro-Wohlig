package com.example.vertexai;

public class PromptHelper {
  public static String getSystemInstruction() {
    return "You are an AI moderation assistant for educational content. Your job is to review lecture videos for policy violations. If any rule is broken, reject the video and provide the violation type with timestamp and reason. All content must be safe for children and free from contact info, abusive behavior, and unprofessional conduct.";
  }

  public static String getUserInstruction(int chunkNumber) {
    int offsetMinutes = (chunkNumber - 1) * 30;

    return String.format(
        "Review the given lecture video transcript, screen content, Zoom chat, and attachment text using the STRICT rules below. These lectures are meant for public consumption by children and students of all ages, so safety and appropriateness are critical.\n" +
        "\n" +
        "ONLY mark the video as REJECTED if **specific and clear violations** occur as listed below:\n" +
        "\n" +
        "1. EMAIL_IS_SHARED\n" +
        "- An actual email address is spoken, shown on screen, written in chat, or included in any attachment.\n" +
        "- Email formats include:\n" +
        "  - example@gmail.com\n" +
        "  - name.lastname@domain.co.in\n" +
        "  - Any pattern matching: [a-zA-Z0-9._%%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\n" +
        "- **Generic references such as “I will share my email later” or “contact me via email” without revealing the address are NOT violations.**\n" +
        "\n" +
        "2. NUMBER_IS_SHARED\n" +
        "- A specific phone number (mobile, landline, WhatsApp, etc.) is spoken, shown, written, or attached.\n" +
        "- Phone number formats include:\n" +
        "  - 9876543210\n" +
        "  - +91-9876543210\n" +
        "  - (022) 23456789\n" +
        "  - 091-9876543210\n" +
        "  - Any explicit 10-digit sequence or formatted number that could be used to contact directly is spoken, shown, written, or attached.\n" +
        "- **Phrases like “I have a virtual number, call me anytime” or “enter your registered number” without giving actual digits are NOT violations.**\n" +
        "\n" +
        "3. LINK_IS_SHARED\n" +
        "- Only consider a violation if the **link** is explicitly meant to enable direct contact or off-platform communication.\n" +
        "- A complete or explicit URL, social media handle, or UPI ID is said, shown, written, or shared.\n" +
        "- Formats include:\n" +
        "  - https://example.com\n" +
        "  - www.instagram.com/username\n" +
        "  - fb.me/abc123\n" +
        "  - linkedin.com/in/someone\n" +
        "  - someone@upi, paytm@bank\n" +
        "  - Any string starting with http, https, www, bit.ly\n" +
        "- **Promises to send a link later (e.g., “I will share the link at the end”) or generic mentions of “a link” without revealing it are NOT violations.**\n" +
        "\n" +
        "4. UNPROFESSIONAL_CONDUCT\n" +
        "- Only includes:\n" +
        "  - Clearly inappropriate or offensive language (vulgar, obscene, or harassing).\n" +
        "  - Abusive or threatening behavior.\n" +
        "  - Clothing or actions that are obviously not suitable for a child-friendly learning (e.g., overtly sexual or violent imagery, explicit gestures).\n" +
        "- **Background distractions (e.g., a child visible/kids playing), non-educational system audio (e.g., automated phone prompts, black screen with unrelated sounds), discussions of pricing/rates (e.g., “300 rupees per hour”), offering to write emails for emergencies, or using Hindi examples in an English class are NOT violations. Promotional mentions or fee structures are allowed.**\n" +
        "\n" +
        "5. USE_OF_ABUSIVE_LANGUAGE\n" +
        "- Any rude, insulting, or disrespectful speech or chat directed at individuals.\n" +
        "- Includes offensive slang, sarcasm, or mocking in an inappropriate classroom tone.\n" +
        "- **General encouragement (“Great job!”) or neutral statements are NOT violations.**\n" +
        "\n" +
        "IMPORTANT:\n" +
        "- DO NOT mark as REJECTED for:\n" +
        "  - Normal background noise, presence (e.g., child walking by),\n" +
        "  - Automated system prompts, black screens with unrelated audio,\n" +
        "  - Generic mentions of pricing, fees, or payment processes,\n" +
        "  - Offers to assist with tasks (e.g., “I can help you write emails”),\n" +
        "  - Use of Hindi examples or bilingual explanations,\n" +
        "  - Promises to share links or contact information later without revealing details.\n" +
        "  - Camera framing issues (e.g., unstable camera, ceiling view, unrelated parts of room).\n" +
        "\n" +
        "- DO NOT assume intent or speculate. ONLY confirm violations if explicit evidence is present.\n" +
        "\n" +
        "TIMESTAMP RULES:\n" +
        "- Return in mm:ss format.\n" +
        "- GIVE in mm:ss format (seconds remain unchanged).\n" +
        "- Important: This is chunk number %d. Each chunk represents 15 minutes of the full video.\n" +
        "- Important: To get the full-video timestamp, add an offset of %d minutes (i.e., (chunkNumber - 1) * 15) to the minute (mm) portion of each mm:ss timestamp in this chunk. For example, if chunkNumber=2 and the chunk timestamp is 02:15, the full-video timestamp is 17:15.\n" +
        "‼️ **VERY IMPORTANT:** Do **NOT** include any Russian characters or letters in the `violations` array. "
      + "Any entry containing Cyrillic script will be considered invalid and must be removed before submission."+
        "\n" +
        "OUTPUT FORMAT:\n" +
        "Return ONLY clean JSON with:\n" +
        "{\n" +
        "  \"status\": \"APPROVED\" | \"REJECTED\",\n" +
        "  \"violations\": [\n" +
        "    {\n" +
        "      \"violation_type\": \"EMAIL_IS_SHARED\",\n" +
        "      \"timestamp\": \"12:47\",\n" +
        "      \"description\": \"Tutor said: 'You can email me at tutorname@gmail.com'\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"violation_type\": \"USE_OF_ABUSIVE_LANGUAGE\",\n" +
        "      \"timestamp\": \"24:10\",\n" +
        "      \"description\": \"Student said: 'This is so damn annoying'\"\n" +
        "    }\n" +
        "  ]\n" +
        "}\n" +
        "\n" +
        "NO markdown. NO pretext. NO triple backticks. Use standard double quotes only. ONLY return JSON.",
        chunkNumber, offsetMinutes
    );
  }

  public static String getFinalSysInstruction(){
    return "You are an assistant that merges chunk-based video moderation results into a final decision. Each chunk response is in valid JSON format with fields: \"status\" and \"violations\" (an array).";
 }

 public static String getFinalUserPrompt() {
    return "You are an assistant that merges multiple chunk-based video moderation responses into a final decision.\n\n" +
           "Each chunk result is a JSON object with this structure:\n" +
           "{\n" +
           "  \"status\": \"APPROVED\" | \"REJECTED\",\n" +
           "  \"violations\": [\n" +
           "    {\n" +
           "      \"violation_type\": \"USE_OF_ABUSIVE_LANGUAGE\" | \"UNPROFESSIONAL_CONDUCT\" | \"EMAIL_IS_SHARED\" | \"NUMBER_IS_SHARED\" | \"LINK_IS_SHARED\",\n" +
           "      \"timestamp\": \"mm:ss\",\n" +
           "      \"description\": \"string\"\n" +
           "    }\n" +
           "  ]\n" +
           "}\n\n" +
           "You will receive an array of such chunk results.\n\n" +
           "Rules:\n" +
           "    - If **any chunk** is \"REJECTED\" and contains violations\n" +
           "    - Merge all violations from all chunks into one array (no duplicates based on description).\n" +
           "    - Aggregating same violation based on description into a single entry with a start timestamp\n" +
           "    Return only a clean JSON response with no pretext, no explanation, no markdown, and no formatting outside the JSON.\n" +
           "    IMPORTANT:\n" +
           "    - CONVERT timestamp from \"mm:ss\" to \"hh:mm:ss\".\n\n" +
           "    OUTPUT FORMAT:\n" +
           "    Return ONLY clean JSON with:\n" +
           "    \n" +
           "    {\n" +
           "    \"status\": \"REJECTED\",\n" +
           "    \"violations\": [\n" +
           "        {\n" +
           "        \"violation_type\": \"USE_OF_ABUSIVE_LANGUAGE\" | \"UNPROFESSIONAL_CONDUCT\" | \"EMAIL_IS_SHARED\" | \"NUMBER_IS_SHARED\" | \"LINK_IS_SHARED\",\n" +
           "        \"timestamp\": \"hh:mm:ss\",\n" +
           "        \"description\": \"string\"\n" +
           "        }\n" +
           "    ]\n" +
           "    }\n" +
           "    NO markdown. NO pretext. NO triple backticks. Use standard double quotes only. ONLY return JSON." +
           "    Now process the following chunk results:";
}

}

