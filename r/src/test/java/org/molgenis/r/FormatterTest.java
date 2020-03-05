package org.molgenis.r;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.r.Formatter.quote;
import static org.molgenis.r.Formatter.stringVector;

import org.junit.jupiter.api.Test;

class FormatterTest {

  @Test
  public void testQuoteNulChar() {
    // \0 is the string terminator in C/R and not allowed in string literals.
    // Replace with space.
    assertEquals("\"foo bar\"", quote("foo\0bar"));
  }

  @Test
  public void testQuoteMultiByteUnicode() {
    assertEquals("\"\\u1D4D7\"", quote("\uD835\uDCD7"));
  }

  @Test
  public void testQuoteVerticalTab() {
    assertEquals("\"\\v\"", quote("\u000B"));
  }

  @Test
  public void testQuoteBellCharacter() {
    assertEquals("\"\\a\"", quote("\u0007"));
  }

  @Test
  public void testQuoteDoubleQuote() {
    assertEquals("\"He said: \\\"Hi!\\\"\"", quote("He said: \"Hi!\""));
  }

  @Test
  public void testQuoteSingleQuote() {
    assertEquals("\"It's complicated\"", quote("It's complicated"));
  }

  @Test
  public void testQuoteNewlines() {
    assertEquals("\"With\\nNewlines\"", quote("With\nNewlines"));
  }

  @Test
  public void testQuoteTabs() {
    assertEquals("\"With\\tTabs\"", quote("With\tTabs"));
  }

  @Test
  public void testQuoteBackspaces() {
    assertEquals("\"With\\bBackspaces\"", quote("With\bBackspaces"));
  }

  @Test
  public void testQuoteCarriageReturns() {
    assertEquals("\"With\\r\\nCRLFs\"", quote("With\r\nCRLFs"));
  }

  @Test
  public void testFormatStringList() {
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
