package com.screenshotmaker.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileNameSanitizerTest {

    @Test
    void keepsSimpleNamesUnchangedApartFromCase() {
        assertEquals("Home-Page", FileNameSanitizer.sanitize("Home-Page"));
    }

    @Test
    void replacesUnsafeCharactersWithDashes() {
        assertEquals("https-example.com-a-b", FileNameSanitizer.sanitize("https://example.com/a?b"));
    }

    @Test
    void collapsesRepeatedDashes() {
        assertEquals("a-b", FileNameSanitizer.sanitize("a///b"));
    }

    @Test
    void blankOrNullFallsBackToUntitled() {
        assertEquals("untitled", FileNameSanitizer.sanitize(null));
        assertEquals("untitled", FileNameSanitizer.sanitize("   "));
        assertEquals("untitled", FileNameSanitizer.sanitize("###"));
    }

    @Test
    void neverProducesPathTraversalSegments() {
        assertFalse(FileNameSanitizer.sanitize("../../etc/passwd").contains(".."));
        assertEquals("untitled", FileNameSanitizer.sanitize(".."));
    }

    @Test
    void capsLength() {
        String longName = "a".repeat(500);
        assertTrue(FileNameSanitizer.sanitize(longName).length() <= 80);
    }

    @Test
    void sequencePrefixIsZeroPaddedToFourDigits() {
        assertEquals("0001", FileNameSanitizer.sequencePrefix(1));
        assertEquals("0042", FileNameSanitizer.sequencePrefix(42));
        assertEquals("12345", FileNameSanitizer.sequencePrefix(12345));
    }
}
