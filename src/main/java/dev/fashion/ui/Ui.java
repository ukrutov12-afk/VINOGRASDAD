package dev.fashion.ui;

import java.util.ArrayList;
import java.util.List;

import dev.fashion.gfx.Font;

public final class Ui {
    private Ui() {
    }

    public static boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public static List<String> wrap(Font.Face face, float size, String text, float maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (face.width(candidate, size, 0f) <= maxWidth || line.isEmpty()) {
                line.setLength(0);
                line.append(candidate);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        if (lines.size() > maxLines) {
            List<String> cut = new ArrayList<>(lines.subList(0, maxLines));
            String last = cut.get(maxLines - 1);
            while (!last.isEmpty() && face.width(last + "…", size, 0f) > maxWidth) {
                last = last.substring(0, last.length() - 1);
            }
            cut.set(maxLines - 1, last.stripTrailing() + "…");
            return cut;
        }
        return lines;
    }
}
