/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.thumbnails.internal.providers;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.thumbnails.OutputFileFormat;
import org.apache.sling.thumbnails.extension.ThumbnailProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides thumbnails for Microsoft Excel spreadsheets (XLS and XLSX).
 * Generates a preview image showing a grid representation of the spreadsheet.
 */
@Component(service = ThumbnailProvider.class, immediate = true)
public class SpreadsheetThumbnailProvider implements ThumbnailProvider {

    private static final Logger log = LoggerFactory.getLogger(SpreadsheetThumbnailProvider.class);

    private static final int THUMBNAIL_WIDTH = 800;
    private static final int THUMBNAIL_HEIGHT = 600;
    private static final int HEADER_HEIGHT = 30;
    private static final int ROW_HEIGHT = 25;
    private static final int DEFAULT_COL_WIDTH = 100;
    private static final int ROW_HEADER_WIDTH = 40;
    private static final int MAX_ROWS = 20;
    private static final int MAX_COLS = 8;

    @Override
    public boolean applies(Resource resource, String metaType) {
        try {
            MimeType mt = new MimeType(metaType);
            return mt.match("application/vnd.ms-excel")
                    || mt.match("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } catch (MimeTypeParseException e) {
            log.warn("Failed to parse mime type: {}", metaType, e);
            return false;
        }
    }

    @Override
    public InputStream getThumbnail(Resource resource) throws IOException {
        String mimeType = getMimeType(resource);

        try (InputStream is = resource.adaptTo(InputStream.class);
                Workbook workbook = createWorkbook(is, mimeType)) {

            if (is == null) {
                throw new IOException("Cannot read resource: " + resource.getPath());
            }

            BufferedImage image = createSpreadsheetPreview(workbook);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(image, OutputFileFormat.PNG.toString(), os);
            return new ByteArrayInputStream(os.toByteArray());
        }
    }

    private String getMimeType(Resource resource) {
        return resource.getValueMap()
                .get(
                        "jcr:content/jcr:mimeType",
                        resource.getValueMap().get("jcr:mimeType", "application/vnd.ms-excel"));
    }

    private Workbook createWorkbook(InputStream is, String mimeType) throws IOException {
        if (mimeType.contains("openxmlformats")) {
            return new XSSFWorkbook(is);
        } else {
            return new HSSFWorkbook(is);
        }
    }

    private BufferedImage createSpreadsheetPreview(Workbook workbook) {
        BufferedImage image = new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Set rendering hints
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);

        // Get first sheet
        Sheet sheet = workbook.getSheetAt(0);
        if (sheet == null) {
            drawEmptyMessage(g2d);
            g2d.dispose();
            return image;
        }

        // Draw sheet name header
        drawSheetHeader(g2d, sheet.getSheetName());

        // Draw column headers
        drawColumnHeaders(g2d);

        // Draw row headers and cells
        drawCells(g2d, sheet);

        // Draw grid lines
        drawGridLines(g2d, sheet);

        g2d.dispose();
        return image;
    }

    private void drawSheetHeader(Graphics2D g2d, String sheetName) {
        g2d.setColor(new Color(68, 114, 196)); // Excel blue
        g2d.fillRect(0, 0, THUMBNAIL_WIDTH, HEADER_HEIGHT);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2d.drawString("Sheet: " + (sheetName != null ? sheetName : "Sheet1"), 10, 20);
    }

    private void drawColumnHeaders(Graphics2D g2d) {
        g2d.setColor(new Color(242, 242, 242)); // Light gray
        g2d.fillRect(0, HEADER_HEIGHT, THUMBNAIL_WIDTH, ROW_HEIGHT);

        g2d.setColor(new Color(100, 100, 100));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 11));

        int x = ROW_HEADER_WIDTH;
        for (int col = 0; col < MAX_COLS; col++) {
            String colName = getColumnName(col);
            int textWidth = g2d.getFontMetrics().stringWidth(colName);
            g2d.drawString(colName, x + (DEFAULT_COL_WIDTH - textWidth) / 2, HEADER_HEIGHT + 17);
            x += DEFAULT_COL_WIDTH;
        }
    }

    private void drawCells(Graphics2D g2d, Sheet sheet) {
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));

        int rowCount = Math.min(sheet.getLastRowNum() + 1, MAX_ROWS);

        for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
            int y = HEADER_HEIGHT + ROW_HEIGHT + (rowIdx * ROW_HEIGHT);

            // Draw row header background
            g2d.setColor(new Color(242, 242, 242));
            g2d.fillRect(0, y, ROW_HEADER_WIDTH, ROW_HEIGHT);

            // Draw row number
            g2d.setColor(new Color(100, 100, 100));
            String rowNum = String.valueOf(rowIdx + 1);
            int textWidth = g2d.getFontMetrics().stringWidth(rowNum);
            g2d.drawString(rowNum, (ROW_HEADER_WIDTH - textWidth) / 2, y + 17);

            Row row = sheet.getRow(rowIdx);
            if (row != null) {
                int x = ROW_HEADER_WIDTH;
                for (int colIdx = 0; colIdx < MAX_COLS; colIdx++) {
                    Cell cell = row.getCell(colIdx);
                    if (cell != null) {
                        String value = getCellValueAsString(cell);
                        if (value != null && !value.isEmpty()) {
                            g2d.setColor(Color.BLACK);
                            // Truncate if too long
                            if (g2d.getFontMetrics().stringWidth(value) > DEFAULT_COL_WIDTH - 10) {
                                while (value.length() > 0
                                        && g2d.getFontMetrics().stringWidth(value + "...") > DEFAULT_COL_WIDTH - 10) {
                                    value = value.substring(0, value.length() - 1);
                                }
                                value += "...";
                            }
                            g2d.drawString(value, x + 5, y + 17);
                        }
                    }
                    x += DEFAULT_COL_WIDTH;
                }
            }
        }
    }

    private void drawGridLines(Graphics2D g2d, Sheet sheet) {
        g2d.setColor(new Color(217, 217, 217)); // Light grid color

        int rowCount = Math.min(sheet.getLastRowNum() + 1, MAX_ROWS);
        int totalHeight = HEADER_HEIGHT + ROW_HEIGHT + (rowCount * ROW_HEIGHT);
        int totalWidth = ROW_HEADER_WIDTH + (MAX_COLS * DEFAULT_COL_WIDTH);

        // Vertical lines
        int x = ROW_HEADER_WIDTH;
        for (int col = 0; col <= MAX_COLS; col++) {
            g2d.drawLine(x, HEADER_HEIGHT, x, totalHeight);
            x += DEFAULT_COL_WIDTH;
        }

        // Horizontal lines
        for (int row = 0; row <= rowCount + 1; row++) {
            int y = HEADER_HEIGHT + (row * ROW_HEIGHT);
            g2d.drawLine(0, y, totalWidth, y);
        }

        // Row header separator
        g2d.drawLine(ROW_HEADER_WIDTH, HEADER_HEIGHT, ROW_HEADER_WIDTH, totalHeight);
    }

    private void drawEmptyMessage(Graphics2D g2d) {
        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font("SansSerif", Font.ITALIC, 14));
        g2d.drawString("Empty spreadsheet", THUMBNAIL_WIDTH / 2 - 60, THUMBNAIL_HEIGHT / 2);
    }

    private String getColumnName(int columnIndex) {
        StringBuilder sb = new StringBuilder();
        columnIndex++;
        while (columnIndex > 0) {
            columnIndex--;
            sb.insert(0, (char) ('A' + (columnIndex % 26)));
            columnIndex /= 26;
        }
        return sb.toString();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }

        switch (cellType) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double value = cell.getNumericCellValue();
                if (value == Math.floor(value)) {
                    return String.valueOf((long) value);
                }
                return String.format("%.2f", value);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
            case ERROR:
            default:
                return "";
        }
    }
}
