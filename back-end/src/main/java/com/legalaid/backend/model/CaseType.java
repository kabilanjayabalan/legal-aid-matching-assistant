package com.legalaid.backend.model;

/**
 * Enum representing different types of legal cases.
 * Each type has a code that will be used in the case number format.
 */
public enum CaseType {
    WP("WP", "Writ Petition"),
    CS("CS", "Civil Suit"),
    CR("CR", "Criminal Case"),
    FA("FA", "Family Case"),
    PR("PR", "Property Case"),
    EM("EM", "Employment Case"),
    CC("CC", "Consumer Case"),
    LA("LA", "Labour Case"),
    TA("TA", "Tax Case"),
    MA("MA", "Maintenance Case");

    private final String code;
    private final String description;

    CaseType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get CaseType from category string
     */
    public static CaseType fromCategory(String category) {
        if (category == null || category.isBlank()) {
            return CS; // Default
        }

        // Trim whitespace and convert to uppercase for matching
        String normalizedCategory = category.trim().toUpperCase();

        switch (normalizedCategory) {
            case "CIVIL":
                return CS;
            case "CRIMINAL":
                return CR;
            case "FAMILY":
                return FA;
            case "PROPERTY":
                return PR;
            case "EMPLOYMENT":
                return EM;
            default:
                return CS; // Default to Civil Suit
        }
    }
}

