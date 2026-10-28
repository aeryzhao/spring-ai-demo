package org.aeryzhao.customerservice.knowledge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 按段落聚合的简单切块器：先把正文按行（段落）拆开，再贪心装箱到 {@code maxChars}，
 * 相邻块之间保留 {@code overlap} 个字符的重叠，避免答案被切断。
 */
public final class DocumentChunker {

    private DocumentChunker() {
    }

    public static List<String> split(String text, int maxChars, int overlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        int limit = Math.max(maxChars, 80);
        int overlapChars = Math.min(Math.max(overlap, 0), limit / 2);

        List<String> paragraphs = Arrays.stream(text.split("\\n+"))
                .map(String::trim)
                .filter(paragraph -> !paragraph.isEmpty())
                .toList();

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (paragraph.length() > limit) {
                flush(chunks, current);
                chunks.addAll(splitLongParagraph(paragraph, limit, overlapChars));
                continue;
            }
            if (current.length() > 0 && current.length() + paragraph.length() + 1 > limit) {
                String tail = tail(current, overlapChars);
                flush(chunks, current);
                current.append(tail);
            }
            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(paragraph);
        }
        flush(chunks, current);
        return chunks;
    }

    private static List<String> splitLongParagraph(String paragraph, int limit, int overlapChars) {
        List<String> pieces = new ArrayList<>();
        int step = Math.max(limit - overlapChars, 1);
        for (int start = 0; start < paragraph.length(); start += step) {
            int end = Math.min(start + limit, paragraph.length());
            pieces.add(paragraph.substring(start, end));
            if (end == paragraph.length()) {
                break;
            }
        }
        return pieces;
    }

    private static void flush(List<String> chunks, StringBuilder current) {
        String trimmed = current.toString().trim();
        if (!trimmed.isEmpty()) {
            chunks.add(trimmed);
        }
        current.setLength(0);
    }

    private static String tail(StringBuilder current, int overlapChars) {
        if (overlapChars <= 0 || current.isEmpty()) {
            return "";
        }
        int start = Math.max(current.length() - overlapChars, 0);
        return current.substring(start);
    }
}
