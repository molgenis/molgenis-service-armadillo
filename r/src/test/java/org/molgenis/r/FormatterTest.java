package org.molgenis.r;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.r.Formatter.quote;
import static org.molgenis.r.Formatter.stringVector;

import org.junit.jupiter.api.Test;

class FormatterTest {

  @Test
  void testQuoteNulChar() {
    // \0 is the string terminator in C/R and not allowed in string literals.
    // Replace with space.
    assertEquals("\"foo bar\"", quote("foo\0bar"));
  }

  @Test
  void testQuoteMultiByteUnicode() {
    assertEquals("\"\\u1D4D7\"", quote("\uD835\uDCD7"));
  }

  @Test
  void testQuoteVerticalTab() {
    assertEquals("\"\\v\"", quote("\u000B"));
  }

  @Test
  void testQuoteBellCharacter() {
    assertEquals("\"\\a\"", quote("\u0007"));
  }

  @Test
  void testQuoteDoubleQuote() {
    assertEquals("\"He said: \\\"Hi!\\\"\"", quote("He said: \"Hi!\""));
  }

  @Test
  void testQuoteSingleQuote() {
    assertEquals("\"It's complicated\"", quote("It's complicated"));
  }

  @Test
  void testQuoteNewlines() {
    assertEquals("\"With\\nNewlines\"", quote("With\nNewlines"));
  }

  @Test
  void testQuoteTabs() {
    assertEquals("\"With\\tTabs\"", quote("With\tTabs"));
  }

  @Test
  void testQuoteBackspaces() {
    assertEquals("\"With\\bBackspaces\"", quote("With\bBackspaces"));
  }

  @Test
  void testQuoteCarriageReturns() {
    assertEquals("\"With\\r\\nCRLFs\"", quote("With\r\nCRLFs"));
  }

  @Test
  void testFormatStringList() {
    assertEquals(
        "c(\"Title\",\"Description\",\"Author\",\"Maintainer\",\"Date/Publication\",\"AggregateMethods\",\"AssignMethods\",\"Options\",\"Version\")",
        stringVector(
            "Title",
            "Description",
            "Author",
            "Maintainer",
            "Date/Publication",
            "AggregateMethods",
            "AssignMethods",
            "Options",
            "Version"));
  }
}
