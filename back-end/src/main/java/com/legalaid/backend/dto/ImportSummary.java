package com.legalaid.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class ImportSummary {
    private int totalProcessed;
    private int importedCount;
    private int updatedCount;
    private int skippedCount;
    private List<String> errors;

    public ImportSummary() {
        this.errors = new ArrayList<>();
    }

    public ImportSummary(int totalProcessed, int importedCount, int updatedCount, int skippedCount, List<String> errors) {
        this.totalProcessed = totalProcessed;
        this.importedCount = importedCount;
        this.updatedCount = updatedCount;
        this.skippedCount = skippedCount;
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public int getTotalProcessed() {
        return totalProcessed;
    }

    public void setTotalProcessed(int totalProcessed) {
        this.totalProcessed = totalProcessed;
    }

    public int getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(int importedCount) {
        this.importedCount = importedCount;
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
    }

    public void incrementTotalProcessed() {
        this.totalProcessed++;
    }

    public void incrementImported() {
        this.importedCount++;
    }

    public void incrementUpdated() {
        this.updatedCount++;
    }

    public void incrementSkipped() {
        this.skippedCount++;
    }
}

