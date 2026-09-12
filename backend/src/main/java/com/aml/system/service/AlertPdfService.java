package com.aml.system.service;

import com.aml.system.model.Alert;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class AlertPdfService {
    private static final PDType1Font REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final float PAGE_MARGIN = 50;
    private static final float LABEL_X = 50;
    private static final float VALUE_X = 180;
    private static final float VALUE_WIDTH = 365;
    private static final float LINE_HEIGHT = 14;
    private static final float BOTTOM_MARGIN = 50;
    private static final float CONTENT_TOP = 45;

    public byte[] generate(Alert alert) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PageWriter writer = new PageWriter(document);
            writer.title("AML Alert Report");
            writer.field("Alert ID", alert.getAlertId());
            writer.field("Transaction ID", alert.getTransactionId());
            writer.field("Customer ID", alert.getCustomerId());
            writer.field("Rule", alert.getRuleCode() + " - " + alert.getRuleName());
            writer.field("Severity", alert.getSeverity());
            writer.field("Triggered amount", alert.getTriggeredAmount());
            writer.field("Batch ID", alert.getBatchId());
            writer.field("Assigned officer", alert.getAssignedOfficerId());
            writer.field("Review status", alert.isReviewed() ? "Reviewed" : "Unreviewed");
            writer.field("Review decision", alert.getReviewDecision());
            writer.field("Reviewed by", alert.getReviewedBy());
            writer.field("Review notes", alert.getReviewNotes());
            writer.field("Created at", alert.getCreatedAt());
            writer.field("Narrative", alert.getNarrative());
            writer.field("Detection metadata", alert.getDetectionMetadataJson());
            writer.close();
            document.save(output);
            return output.toByteArray();
        }
    }

    private static final class PageWriter {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream content;
        private float y;

        private PageWriter(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        private void newPage() throws IOException {
            if (content != null) content.close();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - CONTENT_TOP;
        }

        private void title(String value) throws IOException {
            ensureSpace(28);
            line(value, BOLD, 18, PAGE_MARGIN, y);
            y -= 8;
            content.setStrokingColor(190 / 255f, 198 / 255f, 210 / 255f);
            content.moveTo(PAGE_MARGIN, y);
            content.lineTo(page.getMediaBox().getWidth() - PAGE_MARGIN, y);
            content.stroke();
            y -= 22;
        }

        private void field(String label, Object value) throws IOException {
            String text = sanitize(value == null ? "-" : value.toString());
            List<String> lines = wrap(text, REGULAR, 10, VALUE_WIDTH);
            ensureSpace(LINE_HEIGHT + 6);
            line(label + ":", BOLD, 10, LABEL_X, y);
            for (int index = 0; index < lines.size(); index++) {
                String valueLine = lines.get(index);
                if (index > 0) ensureSpace(LINE_HEIGHT);
                line(valueLine, REGULAR, 10, VALUE_X, y);
                y -= LINE_HEIGHT;
            }
            y -= 7;
        }

        private void ensureSpace(float height) throws IOException {
            if (y - height < BOTTOM_MARGIN) newPage();
        }

        private void line(String value, PDType1Font font, float size, float x, float baseline) throws IOException {
            content.beginText();
            content.setFont(font, size);
            content.newLineAtOffset(x, baseline);
            content.showText(value);
            content.endText();
        }

        private List<String> wrap(String value, PDType1Font font, float size, float width) throws IOException {
            List<String> lines = new ArrayList<>();
            for (String paragraph : value.replace("\r", "").split("\n", -1)) {
                String remaining = paragraph.trim();
                if (remaining.isEmpty()) {
                    lines.add("");
                    continue;
                }
                while (!remaining.isEmpty()) {
                    int breakAt = remaining.length();
                    if (font.getStringWidth(remaining) / 1000f * size > width) {
                        int low = 1;
                        int high = remaining.length();
                        breakAt = 1;
                        while (low <= high) {
                            int mid = (low + high) >>> 1;
                            if (font.getStringWidth(remaining.substring(0, mid)) / 1000f * size <= width) {
                                breakAt = mid;
                                low = mid + 1;
                            } else {
                                high = mid - 1;
                            }
                        }
                    }
                    if (breakAt < remaining.length()) {
                        int space = remaining.lastIndexOf(' ', breakAt);
                        if (space > 0) breakAt = space;
                    }
                    lines.add(remaining.substring(0, breakAt).trim());
                    remaining = remaining.substring(breakAt).trim();
                }
            }
            return lines.isEmpty() ? List.of("") : lines;
        }

        private String sanitize(String value) {
            return value.replaceAll("[^\\x20-\\x7E\\n]", " ");
        }

        private void close() throws IOException {
            content.close();
        }
    }
}
