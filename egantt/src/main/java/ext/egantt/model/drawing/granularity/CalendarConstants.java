/*
 * @(#)CalendarConstants.java
 *
 * Copyright 2002 EGANTT LLP. All rights reserved.
 * PROPRIETARY/QPL. Use is subject to license terms.
 */

package ext.egantt.model.drawing.granularity;

import java.util.Calendar;

/**
 *  Easy to use constants for the Calendar based granularities
 */
public interface CalendarConstants
{
	Object[] FORMAT_KEYS = new Object []
	{
      Integer.valueOf(Calendar.YEAR),
      Integer.valueOf(Calendar.MONTH),
      Integer.valueOf(Calendar.DAY_OF_MONTH),
      Integer.valueOf(Calendar.HOUR),
      Integer.valueOf(Calendar.MINUTE),
      Integer.valueOf(Calendar.SECOND),
      Integer.valueOf(Calendar.MILLISECOND)
	};
}
