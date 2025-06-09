package com.example.vertexai;

import java.net.URL;
import java.util.Map;
import io.github.cdimascio.dotenv.Dotenv;

import com.google.cloud.vertexai.VertexAI;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class App {
    
  private static final Dotenv dotenv = Dotenv.load();  // loads .env from the root

    private static final String LOCATION = dotenv.get("LOCATION");
    private static final String PROJECT_ID = dotenv.get("PROJECT_ID");
    private static final String VIDEO_URI = dotenv.get("VIDEO_URI");
    
    public static void main(String[] args) {
      try (VertexAI vertexAi = new VertexAI(PROJECT_ID, LOCATION)) {

      VertexAiService vertexSvc = new VertexAiService(vertexAi);
      // LectureReviewService lectureSvc = new LectureReviewService();
      ExcelProcessVideoUrls excelProcessVideoUrls = new ExcelProcessVideoUrls();
      excelProcessVideoUrls.test(vertexSvc); // Assuming this method processes the Excel file and sets up the environment
      // Map<String, Object> result = lectureSvc.reviewLecture(VIDEO_URI, vertexSvc);
      // System.out.println("Final result: " + result);
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
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
