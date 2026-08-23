package com.screenshotmaker.engine.web;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RobotsTxtCheckerParsingTest {

    @Test
    void collectsDisallowRulesUnderTheWildcardUserAgent() {
        String robotsTxt = """
                User-agent: *
                Disallow: /admin
                Disallow: /private

                User-agent: SomeOtherBot
                Disallow: /everything
                """;

        List<String> rules = RobotsTxtChecker.parseWildcardDisallowRules(robotsTxt);

        assertEquals(List.of("/admin", "/private"), rules);
    }

    @Test
    void ignoresCommentsAndBlankLines() {
        String robotsTxt = """
                # comment
                User-agent: *
                # another comment
                Disallow: /admin

                Disallow: /secret
                """;

        assertEquals(List.of("/admin", "/secret"), RobotsTxtChecker.parseWildcardDisallowRules(robotsTxt));
    }

    @Test
    void emptyDisallowMeansNothingIsBlocked() {
        String robotsTxt = "User-agent: *\nDisallow:\n";
        List<String> rules = RobotsTxtChecker.parseWildcardDisallowRules(robotsTxt);
        assertTrue(rules.contains(""));
    }

    @Test
    void rulesOutsideTheWildcardGroupAreIgnored() {
        String robotsTxt = "User-agent: GoogleBot\nDisallow: /only-for-google\n";
        assertTrue(RobotsTxtChecker.parseWildcardDisallowRules(robotsTxt).isEmpty());
    }
}
