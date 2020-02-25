package org.molgenis.datashield.r;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Map;
import org.apache.commons.text.translate.AggregateTranslator;
import org.apache.commons.text.translate.CharSequenceTranslator;
import org.apache.commons.text.translate.LookupTranslator;
import org.apache.commons.text.translate.UnicodeEscaper;

/** Formats Java objects to executable R strings. */
public class Formatter {
  // See https://stat.ethz.ch/R-manual/R-devel/library/base/html/Quotes.html
  public static final ImmutableMap<CharSequence, CharSequence> R_CTRL_CHARS_ESCAPE =
      ImmutableMap.<CharSequence, CharSequence>builder()
          .put("\n", "\\n")
          .put("\r", "\\r")
          .put("\t", "\\t")
          .put("\b", "\\b")
          .put("\f", "\\f")
          .put("\0", " ")
          .put("\u0007", "\\a")
          .put("\u000B", "\\v")
          .build();

  public static final CharSequenceTranslator ESCAPE_R =
      new AggregateTranslator(
          new LookupTranslator(Map.of("\"", "\\\"", "\\", "\\\\")),
          new LookupTranslator(R_CTRL_CHARS_ESCAPE),
          UnicodeEscaper.outsideOf(32, 0x7f));

  public static String quote(final CharSequence input) {
    return format("\"%s\"", ESCAPE_R.translate(input));
  }

  public static String stringVector(String... arguments) {
    return Arrays.stream(arguments).map(Formatter::quote).collect(joining(",", "c(", ")"));
  }
}
