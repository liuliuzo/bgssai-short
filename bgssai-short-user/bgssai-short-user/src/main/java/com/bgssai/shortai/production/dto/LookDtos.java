package com.bgssai.shortai.production.dto;

public final class LookDtos {
    private LookDtos() {
    }

    public static class LookLibraryReq {
        private String libraryId;
        private String libraryName;
        private String description;
        private String status;
        private Integer mountPortrait;

        public String getLibraryId() { return libraryId; }
        public void setLibraryId(String libraryId) { this.libraryId = libraryId; }
        public String getLibraryName() { return libraryName; }
        public void setLibraryName(String libraryName) { this.libraryName = libraryName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getMountPortrait() { return mountPortrait; }
        public void setMountPortrait(Integer mountPortrait) { this.mountPortrait = mountPortrait; }
    }

    public static class LookMaterialReq {
        private String libraryId;
        private String materialId;
        private String materialName;
        private String caption;
        private String materialKind;
        private String viewAngle;
        private String imageBase64;

        public String getLibraryId() { return libraryId; }
        public void setLibraryId(String libraryId) { this.libraryId = libraryId; }
        public String getMaterialId() { return materialId; }
        public void setMaterialId(String materialId) { this.materialId = materialId; }
        public String getMaterialName() { return materialName; }
        public void setMaterialName(String materialName) { this.materialName = materialName; }
        public String getCaption() { return caption; }
        public void setCaption(String caption) { this.caption = caption; }
        public String getMaterialKind() { return materialKind; }
        public void setMaterialKind(String materialKind) { this.materialKind = materialKind; }
        public String getViewAngle() { return viewAngle; }
        public void setViewAngle(String viewAngle) { this.viewAngle = viewAngle; }
        public String getImageBase64() { return imageBase64; }
        public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    }
}
