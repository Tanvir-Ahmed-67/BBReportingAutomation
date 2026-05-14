package org.example;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.FileInputStream;

public class ExcelTest {
    public static void main(String[] args) {
        try (FileInputStream fis = new FileInputStream("test_file.xls");
             Workbook workbook = new HSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            
            // Find column indices
            int dateCol = -1, currCol = -1, countryCol = -1, amountCol = -1;
            for (Cell cell : headerRow) {
                String header = cell.getStringCellValue().toLowerCase();
                if (header.contains("dated")) dateCol = cell.getColumnIndex();
                if (header.contains("currency")) currCol = cell.getColumnIndex();
                if (header.contains("country_name")) countryCol = cell.getColumnIndex();
                if (header.contains("amount_usd")) amountCol = cell.getColumnIndex();
            }
            
            System.out.println("Cols: Date=" + dateCol + ", Curr=" + currCol + ", Country=" + countryCol + ", Amount=" + amountCol);
            
            Row dataRow = sheet.getRow(1);
            if (dataRow != null) {
                DataFormatter formatter = new DataFormatter();
                for (int i = 0; i < 4; i++) {
                    Cell cell = dataRow.getCell(i);
                    String value;
                    if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                        value = new java.text.SimpleDateFormat("dd/MM/yyyy").format(cell.getDateCellValue());
                    } else {
                        value = formatter.formatCellValue(cell);
                    }
                    System.out.println("Col " + i + ": " + value);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
