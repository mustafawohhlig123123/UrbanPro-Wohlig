package com.example.vertexai;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

public class ExcelProcessVideoUrls {
    public void test(VertexAiService vertexSvc) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the path of the Excel file (.xlsx): ");
        String filePath = scanner.nextLine();

        FileInputStream fis = null;
        FileOutputStream fos = null;
        Workbook workbook = null;

        try {
            fis = new FileInputStream(new File(filePath));
            workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(5);

            // Get the header row
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                System.out.println("No header row found.");
                return;
            }

            // Find the index of the column named "Video url"
            int videoUrlCol = -1;
            int lastCol = headerRow.getLastCellNum();
            for (int i = 0; i < lastCol; i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null && "Video url".equalsIgnoreCase(cell.getStringCellValue().trim())) {
                    videoUrlCol = i;
                    break;
                }
            }

            if (videoUrlCol == -1) {
                System.out.println("\"Video url\" column not found.");
                return;
            }

            // Add new column header
            // int newColIndex = lastCol;
            // Cell newHeaderCell = headerRow.createCell(newColIndex);
            // newHeaderCell.setCellValue("Processed URL");
               // Add two new column headers: Status and Processed URL
            int statusColIndex = lastCol;
            Cell statusHeaderCell = headerRow.createCell(statusColIndex);
            statusHeaderCell.setCellValue(" AI Status");
            int resultColIndex = lastCol + 1;
            Cell resultHeaderCell = headerRow.createCell(resultColIndex);
            resultHeaderCell.setCellValue("AI Output");

            // Process each row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell urlCell = row.getCell(videoUrlCol);
                    if (urlCell != null) {
                        String url = urlCell.toString();
                        System.out.println("Processing URL: "+ i + " --- "+ url);
                        // LectureReviewService lectureSvc = new LectureReviewService();
                        // Map<String, Object> result = lectureSvc.reviewLecture(url);
                        
                        // // 👇 You can replace this logic with your own processing
                        // // String result = "Processed: " + url;
                        // System.out.println("Violations: " + result);
                        // Cell newCell = row.createCell(newColIndex);
                        // newCell.setCellValue(result.toString());
                        LectureReviewService lectureSvc = new LectureReviewService();
                        Map<String, Object> result = lectureSvc.reviewLecture(url, vertexSvc);
                        System.out.println("Violations: " + result);

                        // Write only "status" value into the Status column
                        String statusValue = "";
                        if (result.get("status") != null) {
                            statusValue = result.get("status").toString();
                        }
                        Cell statusCell = row.createCell(statusColIndex);
                        statusCell.setCellValue(statusValue);

                        // Write full result map into the Processed URL column
                        Cell resultCell = row.createCell(resultColIndex);
                        resultCell.setCellValue(result.toString());

                        System.gc();
                    }
                }

                if (row == null) continue;

                Cell existingStatus = row.getCell(statusColIndex);
                if (existingStatus != null &&
                    existingStatus.getCellType() != CellType.BLANK &&
                    !existingStatus.toString().trim().isEmpty()) {
                    // Already processed
                    continue;
                }

                Cell urlCell = row.getCell(videoUrlCol);
                if (urlCell == null) continue;
                String url = urlCell.toString();
                System.out.println("Processing URL: " + i + " --- " + url);

                LectureReviewService lectureSvc = new LectureReviewService();
                Map<String, Object> result = lectureSvc.reviewLecture(url, vertexSvc);
                System.out.println("Violations: " + result);

                String statusValue = "";
                if (result.get("status") != null) {
                    statusValue = result.get("status").toString();
                }
                Cell statusCell = row.createCell(statusColIndex);
                statusCell.setCellValue(statusValue);

                Cell resultCell = row.createCell(resultColIndex);
                resultCell.setCellValue(result.toString());

                System.gc();
            }

            // Write back to file
            fis.close(); // must close input stream before writing
            fos = new FileOutputStream(new File(filePath));
            workbook.write(fos);
            System.out.println("Successfully appended data to new column.");


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) fos.close();
                if (workbook != null) workbook.close();
                scanner.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
