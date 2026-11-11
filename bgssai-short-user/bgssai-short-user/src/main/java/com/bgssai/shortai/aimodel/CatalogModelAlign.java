package com.bgssai.shortai.aimodel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Chooses the model id a pipeline step will call.
 * A configured default is kept only when the gateway catalog lists it for that ability.
 * An unlisted Kling image id may move to the registered Kling image model (prefer {@code kling-v3}).
 * An unlisted MiniMax voice id may move to a MiniMax model that is speech-capable.
 * Models from other vendors are never substituted for each other.
 * When the configured id is still absent, a listed company model of the same ability may be used.
 */
public final class CatalogModelAlign {

    private CatalogModelAlign() {
    }

    public static String choose(String ability, String configured, List<TokenHubModelCatalog.HubModel> catalog) {
        String abilityName = ability == null ? "" : ability.trim().toUpperCase(Locale.ROOT);
        String wanted = configured == null ? "" : configured.trim();
        String capability = capabilityOf(abilityName);
        if (capability == null) {
            return wanted;
        }
        if (supports(catalog, wanted, capability)) {
            return wanted;
        }
        if ("IMAGE".equals(abilityName) && klingFamily(wanted)) {
            String registered = klingImage(catalog);
            if (registered != null) {
                return registered;
            }
        }
        if ("VOICE".equals(abilityName) && minimaxFamily(wanted)) {
            String registered = minimaxSpeech(catalog);
            if (registered != null) {
                return registered;
            }
        }
        String company = companyChoice(abilityName, capability, catalog);
        if (company != null) {
            return company;
        }
        return wanted;
    }

    static boolean supports(List<TokenHubModelCatalog.HubModel> catalog, String id, String capability) {
        if (id == null || id.isBlank() || catalog == null) {
            return false;
        }
        for (TokenHubModelCatalog.HubModel model : catalog) {
            if (model != null && id.equals(model.id()) && model.capabilities() != null
                    && model.capabilities().contains(capability)) {
                return true;
            }
        }
        return false;
    }

    private static String klingImage(List<TokenHubModelCatalog.HubModel> catalog) {
        List<TokenHubModelCatalog.HubModel> matches = matching(catalog, "image_generation", true);
        for (TokenHubModelCatalog.HubModel model : matches) {
            if ("kling-v3".equalsIgnoreCase(model.id())) {
                return model.id();
            }
        }
        matches.sort(Comparator.comparing(model -> model.id().toLowerCase(Locale.ROOT)));
        return matches.isEmpty() ? null : matches.get(0).id();
    }

    private static String minimaxSpeech(List<TokenHubModelCatalog.HubModel> catalog) {
        List<TokenHubModelCatalog.HubModel> matches = matching(catalog, "audio_speech", false);
        if (matches.isEmpty()) {
            return null;
        }
        matches.sort(Comparator
                .comparingInt((TokenHubModelCatalog.HubModel model) -> speechRank(model.id()))
                .thenComparing(model -> model.id().toLowerCase(Locale.ROOT)));
        return matches.get(0).id();
    }

    private static List<TokenHubModelCatalog.HubModel> matching(List<TokenHubModelCatalog.HubModel> catalog,
            String capability, boolean kling) {
        List<TokenHubModelCatalog.HubModel> matches = new ArrayList<>();
        if (catalog == null) {
            return matches;
        }
        for (TokenHubModelCatalog.HubModel model : catalog) {
            if (model == null || model.id() == null || model.id().isBlank() || model.capabilities() == null
                    || !model.capabilities().contains(capability)) {
                continue;
            }
            boolean family = kling ? klingFamily(model.id(), model.ownedBy()) : minimaxFamily(model.id(), model.ownedBy());
            if (family) {
                matches.add(model);
            }
        }
        return matches;
    }

    private static int speechRank(String id) {
        String value = id == null ? "" : id.toLowerCase(Locale.ROOT);
        if (value.contains("speech") && value.contains("hd")) {
            return 0;
        }
        if (value.contains("speech") || value.contains("tts")) {
            return 1;
        }
        return 2;
    }

    private static boolean klingFamily(String configured) {
        return klingFamily(configured, "");
    }

    private static boolean klingFamily(String id, String ownedBy) {
        String blob = ((id == null ? "" : id) + " " + (ownedBy == null ? "" : ownedBy)).toLowerCase(Locale.ROOT);
        return blob.contains("kling") || blob.contains("可灵");
    }

    private static boolean minimaxFamily(String configured) {
        return minimaxFamily(configured, "");
    }

    private static boolean minimaxFamily(String id, String ownedBy) {
        String blob = ((id == null ? "" : id) + " " + (ownedBy == null ? "" : ownedBy)).toLowerCase(Locale.ROOT);
        return blob.contains("minimax") || blob.contains("海螺");
    }

    private static String companyChoice(String ability, String capability,
            List<TokenHubModelCatalog.HubModel> catalog) {
        for (String id : preferredCompany(ability)) {
            if (supports(catalog, id, capability)) {
                return listedId(catalog, id);
            }
        }
        List<TokenHubModelCatalog.HubModel> matches = new ArrayList<>();
        if (catalog == null) {
            return null;
        }
        for (TokenHubModelCatalog.HubModel model : catalog) {
            if (model == null || model.id() == null || model.id().isBlank() || model.capabilities() == null
                    || !model.capabilities().contains(capability)) {
                continue;
            }
            if (companyId(ability, model.id()) || ownedByCompany(model.ownedBy())) {
                matches.add(model);
            }
        }
        matches.sort(Comparator.comparing(model -> model.id().toLowerCase(Locale.ROOT)));
        return matches.isEmpty() ? null : matches.get(0).id();
    }

    private static String listedId(List<TokenHubModelCatalog.HubModel> catalog, String id) {
        for (TokenHubModelCatalog.HubModel model : catalog) {
            if (model != null && id.equalsIgnoreCase(model.id())) {
                return model.id();
            }
        }
        return id;
    }

    private static List<String> preferredCompany(String ability) {
        return switch (ability) {
            case "TEXT" -> List.of("bgssai-chat", "qwen", "qwen2.5", "qwen2");
            case "IMAGE" -> List.of("bgssai-image", "sd-turbo");
            case "VIDEO" -> List.of("bgssai-video");
            case "VOICE" -> List.of("bgssai-tts", "piper", "edge-tts");
            default -> List.of();
        };
    }

    private static boolean companyId(String ability, String id) {
        String value = id == null ? "" : id.toLowerCase(Locale.ROOT);
        return switch (ability) {
            case "TEXT" -> "qwen".equals(value) || value.startsWith("qwen2") || "bgssai-chat".equals(value);
            case "IMAGE" -> "sd-turbo".equals(value) || "bgssai-image".equals(value);
            case "VIDEO" -> "bgssai-video".equals(value);
            case "VOICE" -> "piper".equals(value) || "bgssai-tts".equals(value) || "edge-tts".equals(value);
            default -> false;
        };
    }

    private static boolean ownedByCompany(String ownedBy) {
        return ownedBy != null && ownedBy.toLowerCase(Locale.ROOT).contains("bgssai");
    }

    private static String capabilityOf(String ability) {
        return switch (ability) {
            case "TEXT" -> "chat";
            case "IMAGE" -> "image_generation";
            case "VIDEO" -> "video_generation";
            case "VOICE" -> "audio_speech";
            default -> null;
        };
    }
}
