package com.legalaid.backend.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CaseType Enum Tests")
class CaseTypeTest {

    @Nested
    @DisplayName("Enum Values Tests")
    class EnumValuesTests {

        @Test
        @DisplayName("Should have correct enum values")
        void shouldHaveCorrectEnumValues() {
            // Given & When
            CaseType[] values = CaseType.values();

            // Then
            assertThat(values).hasSize(10);
            assertThat(values).containsExactly(
                CaseType.WP, CaseType.CS, CaseType.CR, CaseType.FA, CaseType.PR,
                CaseType.EM, CaseType.CC, CaseType.LA, CaseType.TA, CaseType.MA
            );
        }

        @Test
        @DisplayName("Should have correct codes and descriptions for all enum values")
        void shouldHaveCorrectCodesAndDescriptionsForAllEnumValues() {
            // Then
            assertThat(CaseType.WP.getCode()).isEqualTo("WP");
            assertThat(CaseType.WP.getDescription()).isEqualTo("Writ Petition");

            assertThat(CaseType.CS.getCode()).isEqualTo("CS");
            assertThat(CaseType.CS.getDescription()).isEqualTo("Civil Suit");

            assertThat(CaseType.CR.getCode()).isEqualTo("CR");
            assertThat(CaseType.CR.getDescription()).isEqualTo("Criminal Case");

            assertThat(CaseType.FA.getCode()).isEqualTo("FA");
            assertThat(CaseType.FA.getDescription()).isEqualTo("Family Case");

            assertThat(CaseType.PR.getCode()).isEqualTo("PR");
            assertThat(CaseType.PR.getDescription()).isEqualTo("Property Case");

            assertThat(CaseType.EM.getCode()).isEqualTo("EM");
            assertThat(CaseType.EM.getDescription()).isEqualTo("Employment Case");

            assertThat(CaseType.CC.getCode()).isEqualTo("CC");
            assertThat(CaseType.CC.getDescription()).isEqualTo("Consumer Case");

            assertThat(CaseType.LA.getCode()).isEqualTo("LA");
            assertThat(CaseType.LA.getDescription()).isEqualTo("Labour Case");

            assertThat(CaseType.TA.getCode()).isEqualTo("TA");
            assertThat(CaseType.TA.getDescription()).isEqualTo("Tax Case");

            assertThat(CaseType.MA.getCode()).isEqualTo("MA");
            assertThat(CaseType.MA.getDescription()).isEqualTo("Maintenance Case");
        }
    }

    @Nested
    @DisplayName("fromCategory Method Tests")
    class FromCategoryMethodTests {

        @ParameterizedTest
        @CsvSource({
            "CIVIL, CS",
            "civil, CS",
            "Civil, CS",
            "CRIMINAL, CR",
            "criminal, CR",
            "Criminal, CR",
            "FAMILY, FA",
            "family, FA",
            "Family, FA",
            "PROPERTY, PR",
            "property, PR",
            "Property, PR",
            "EMPLOYMENT, EM",
            "employment, EM",
            "Employment, EM"
        })
        @DisplayName("Should map categories to correct CaseType")
        void shouldMapCategoriesToCorrectCaseType(String category, String expectedCode) {
            // When
            CaseType result = CaseType.fromCategory(category);

            // Then
            assertThat(result.getCode()).isEqualTo(expectedCode);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "UNKNOWN",
            "INVALID",
            "CONTRACT",
            "TORT",
            "BUSINESS",
            "IMMIGRATION",
            "BANKRUPTCY",
            "PERSONAL_INJURY",
            "DIVORCE", // Not directly mapped, should default
            "CUSTODY"  // Not directly mapped, should default
        })
        @DisplayName("Should default to CS for unknown categories")
        void shouldDefaultToCSForUnknownCategories(String category) {
            // When
            CaseType result = CaseType.fromCategory(category);

            // Then
            assertThat(result).isEqualTo(CaseType.CS);
            assertThat(result.getCode()).isEqualTo("CS");
            assertThat(result.getDescription()).isEqualTo("Civil Suit");
        }

        @Test
        @DisplayName("Should default to CS for null category")
        void shouldDefaultToCSForNullCategory() {
            // When
            CaseType result = CaseType.fromCategory(null);

            // Then
            assertThat(result).isEqualTo(CaseType.CS);
            assertThat(result.getCode()).isEqualTo("CS");
            assertThat(result.getDescription()).isEqualTo("Civil Suit");
        }

        @Test
        @DisplayName("Should default to CS for empty category")
        void shouldDefaultToCSForEmptyCategory() {
            // When
            CaseType result = CaseType.fromCategory("");

            // Then
            assertThat(result).isEqualTo(CaseType.CS);
        }

        @Test
        @DisplayName("Should default to CS for blank category")
        void shouldDefaultToCSForBlankCategory() {
            // When
            CaseType result = CaseType.fromCategory("   ");

            // Then
            assertThat(result).isEqualTo(CaseType.CS);
        }

        @Test
        @DisplayName("Should handle mixed case categories")
        void shouldHandleMixedCaseCategories() {
            // Given
            String[] mixedCaseCategories = {
                "CiViL", "cRiMiNaL", "FaMiLy", "PrOpErTy", "EmPlOyMeNt"
            };
            CaseType[] expectedResults = {
                CaseType.CS, CaseType.CR, CaseType.FA, CaseType.PR, CaseType.EM
            };

            // When & Then
            for (int i = 0; i < mixedCaseCategories.length; i++) {
                CaseType result = CaseType.fromCategory(mixedCaseCategories[i]);
                assertThat(result).isEqualTo(expectedResults[i]);
            }
        }

        @Test
        @DisplayName("Should handle categories with leading/trailing whitespace")
        void shouldHandleCategoriesWithWhitespace() {
            // When & Then
            assertThat(CaseType.fromCategory("  CIVIL  ")).isEqualTo(CaseType.CS);
            assertThat(CaseType.fromCategory("\tCRIMINAL\t")).isEqualTo(CaseType.CR);
            assertThat(CaseType.fromCategory("\nFAMILY\n")).isEqualTo(CaseType.FA);
        }
    }

    @Nested
    @DisplayName("Enum Methods Tests")
    class EnumMethodsTests {

        @Test
        @DisplayName("Should support valueOf method")
        void shouldSupportValueOfMethod() {
            // When & Then
            assertThat(CaseType.valueOf("CS")).isEqualTo(CaseType.CS);
            assertThat(CaseType.valueOf("WP")).isEqualTo(CaseType.WP);
            assertThat(CaseType.valueOf("FA")).isEqualTo(CaseType.FA);
        }

        @Test
        @DisplayName("Should support toString method")
        void shouldSupportToStringMethod() {
            // When & Then
            assertThat(CaseType.CS.toString()).isEqualTo("CS");
            assertThat(CaseType.WP.toString()).isEqualTo("WP");
            assertThat(CaseType.FA.toString()).isEqualTo("FA");
        }

        @Test
        @DisplayName("Should support ordinal method")
        void shouldSupportOrdinalMethod() {
            // When & Then
            assertThat(CaseType.WP.ordinal()).isEqualTo(0);
            assertThat(CaseType.CS.ordinal()).isEqualTo(1);
            assertThat(CaseType.CR.ordinal()).isEqualTo(2);
            assertThat(CaseType.FA.ordinal()).isEqualTo(3);
            assertThat(CaseType.PR.ordinal()).isEqualTo(4);
            assertThat(CaseType.EM.ordinal()).isEqualTo(5);
            assertThat(CaseType.CC.ordinal()).isEqualTo(6);
            assertThat(CaseType.LA.ordinal()).isEqualTo(7);
            assertThat(CaseType.TA.ordinal()).isEqualTo(8);
            assertThat(CaseType.MA.ordinal()).isEqualTo(9);
        }

        @Test
        @DisplayName("Should support name method")
        void shouldSupportNameMethod() {
            // When & Then
            assertThat(CaseType.CS.name()).isEqualTo("CS");
            assertThat(CaseType.WP.name()).isEqualTo("WP");
            assertThat(CaseType.FA.name()).isEqualTo("FA");
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should provide meaningful descriptions for all case types")
        void shouldProvideMeaningfulDescriptionsForAllCaseTypes() {
            // Given & When
            CaseType[] allCaseTypes = CaseType.values();

            // Then
            for (CaseType caseType : allCaseTypes) {
                assertThat(caseType.getDescription())
                    .isNotNull()
                    .isNotEmpty()
                    .isNotBlank();
                // Note: Not all descriptions contain "Case" (e.g., "Writ Petition", "Civil Suit")
                // The important thing is that descriptions are meaningful and non-empty
            }
        }

        @Test
        @DisplayName("Should have unique codes for all case types")
        void shouldHaveUniqueCodesForAllCaseTypes() {
            // Given
            CaseType[] allCaseTypes = CaseType.values();

            // When & Then
            for (int i = 0; i < allCaseTypes.length; i++) {
                for (int j = i + 1; j < allCaseTypes.length; j++) {
                    assertThat(allCaseTypes[i].getCode())
                        .isNotEqualTo(allCaseTypes[j].getCode());
                }
            }
        }

        @Test
        @DisplayName("Should have two-character codes for all case types")
        void shouldHaveTwoCharacterCodesForAllCaseTypes() {
            // Given & When
            CaseType[] allCaseTypes = CaseType.values();

            // Then
            for (CaseType caseType : allCaseTypes) {
                assertThat(caseType.getCode()).hasSize(2);
                assertThat(caseType.getCode()).matches("[A-Z]{2}");
            }
        }

        @Test
        @DisplayName("Should be case-insensitive for category mapping")
        void shouldBeCaseInsensitiveForCategoryMapping() {
            // Given
            String[] categories = {"civil", "CIVIL", "Civil", "cIvIl"};

            // When & Then
            for (String category : categories) {
                CaseType result = CaseType.fromCategory(category);
                assertThat(result).isEqualTo(CaseType.CS);
            }
        }

        @Test
        @DisplayName("Should demonstrate fromCategory method is deterministic")
        void shouldDemonstrateFromCategoryMethodIsDeterministic() {
            // Given
            String category = "FAMILY";

            // When
            CaseType result1 = CaseType.fromCategory(category);
            CaseType result2 = CaseType.fromCategory(category);

            // Then
            assertThat(result1).isEqualTo(result2);
            assertThat(result1).isSameAs(result2); // Enum singletons
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long category strings")
        void shouldHandleVeryLongCategoryStrings() {
            // Given
            String longCategory = "CIVIL".repeat(1000);

            // When
            CaseType result = CaseType.fromCategory(longCategory);

            // Then
            assertThat(result).isEqualTo(CaseType.CS);
        }

        @Test
        @DisplayName("Should handle categories with special characters")
        void shouldHandleCategoriesWithSpecialCharacters() {
            // Given
            String[] categoriesWithSpecialChars = {
                "CIVIL@", "CRIMINAL#", "FAMILY$", "PROPERTY%", "EMPLOYMENT&"
            };

            // When & Then
            for (String category : categoriesWithSpecialChars) {
                CaseType result = CaseType.fromCategory(category);
                assertThat(result).isEqualTo(CaseType.CS); // Should default for invalid categories
            }
        }

        @Test
        @DisplayName("Should handle categories with numbers")
        void shouldHandleCategoriesWithNumbers() {
            // Given
            String[] categoriesWithNumbers = {
                "CIVIL1", "CRIMINAL2", "FAMILY3", "PROPERTY4", "EMPLOYMENT5"
            };

            // When & Then
            for (String category : categoriesWithNumbers) {
                CaseType result = CaseType.fromCategory(category);
                assertThat(result).isEqualTo(CaseType.CS); // Should default for invalid categories
            }
        }
    }
}
