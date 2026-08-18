package com.bgssai.shortai.production.support;

public final class PromptComposer {

    private PromptComposer() {
    }

    public static String compose(String desc, String dialogue, String extra, String visualStyle) {
        StringBuilder sb = new StringBuilder();
        sb.append("vertical 9:16, ").append(visualStyle == null ? "MODERN_URBAN" : visualStyle);
        if (desc != null && !desc.isBlank()) {
            sb.append(", ").append(desc.trim());
        }
        if (dialogue != null && !dialogue.isBlank()) {
            sb.append(", dialogue: ").append(dialogue.trim());
        }
        if (extra != null && !extra.isBlank()) {
            sb.append(", ").append(extra.trim());
        }
        return sb.toString();
    }
}
