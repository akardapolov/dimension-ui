package ru.dimension.ui.helper;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;
import ru.dimension.ui.prompt.Internationalization;

public final class AbbreviatedNumberFormat extends NumberFormat {

  private static final double ABBREVIATION_THRESHOLD = 1000.0;

  private final NumberFormat plainFormat;
  private final NumberFormat compactFormat;

  private AbbreviatedNumberFormat(Locale locale) {
    this.plainFormat = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(locale));
    this.compactFormat = NumberFormat.getCompactNumberInstance(locale, NumberFormat.Style.SHORT);
    this.compactFormat.setMaximumFractionDigits(1);
    this.compactFormat.setMinimumFractionDigits(0);
    this.compactFormat.setRoundingMode(RoundingMode.HALF_UP);
  }

  public static AbbreviatedNumberFormat create() {
    return create(new Locale(Internationalization.getLanguage()));
  }

  public static AbbreviatedNumberFormat create(Locale locale) {
    return new AbbreviatedNumberFormat(locale);
  }

  @Override
  public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
    if (Math.abs(number) < ABBREVIATION_THRESHOLD) {
      return plainFormat.format(number, toAppendTo, pos);
    }
    return compactFormat.format(number, toAppendTo, pos);
  }

  @Override
  public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
    return format((double) number, toAppendTo, pos);
  }

  @Override
  public Number parse(String source, ParsePosition parsePosition) {
    throw new UnsupportedOperationException();
  }
}
