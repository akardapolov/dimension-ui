package ru.dimension.ui.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParsePosition;
import java.util.Locale;
import org.junit.jupiter.api.Test;

public class AbbreviatedNumberFormatTest {

  private final AbbreviatedNumberFormat ruFormat = AbbreviatedNumberFormat.create(new Locale("ru"));
  private final AbbreviatedNumberFormat enFormat = AbbreviatedNumberFormat.create(Locale.ENGLISH);

  @Test
  public void testRussianCompactThousands() {
    String result = ruFormat.format(1500);
    assertTrue(result.startsWith("1,5"), "unexpected: " + result);
    assertTrue(result.contains("тыс."), "unexpected: " + result);
  }

  @Test
  public void testRussianCompactMillions() {
    String result = ruFormat.format(1_300_000);
    assertTrue(result.contains("1,3"), "unexpected: " + result);
    assertTrue(result.contains("млн"), "unexpected: " + result);
  }

  @Test
  public void testRussianCompactBillions() {
    String result = ruFormat.format(2_700_000_000L);
    assertTrue(result.contains("2,7"), "unexpected: " + result);
    assertTrue(result.contains("млрд"), "unexpected: " + result);
  }

  @Test
  public void testRussianThreshold() {
    assertEquals("999", ruFormat.format(999));
    assertEquals("999,5", ruFormat.format(999.5));
    String result = ruFormat.format(1000);
    assertTrue(result.startsWith("1"), "unexpected: " + result);
    assertTrue(result.contains("тыс."), "unexpected: " + result);
  }

  @Test
  public void testEnglishCompact() {
    assertEquals("1.5K", enFormat.format(1500));
    assertEquals("1.3M", enFormat.format(1_300_000));
    assertEquals("2.7B", enFormat.format(2_700_000_000L));
  }

  @Test
  public void testEnglishThreshold() {
    assertEquals("999", enFormat.format(999));
    assertEquals("999.5", enFormat.format(999.5));
    assertEquals("1K", enFormat.format(1000));
  }

  @Test
  public void testZeroAndNegative() {
    assertEquals("0", enFormat.format(0));
    assertEquals("-1.5K", enFormat.format(-1500));
  }

  @Test
  public void testHalfUpRounding() {
    assertEquals("1.3M", enFormat.format(1_250_000));
  }

  @Test
  public void testLongDelegatesToDouble() {
    assertEquals("2.7B", enFormat.format(2_700_000_000L));
    assertEquals("1.5K", enFormat.format(1500L));
  }

  @Test
  public void testParseUnsupported() {
    assertThrows(UnsupportedOperationException.class,
                 () -> enFormat.parse("1.5K", new ParsePosition(0)));
  }
}
