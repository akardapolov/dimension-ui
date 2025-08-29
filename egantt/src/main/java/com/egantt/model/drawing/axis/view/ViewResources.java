/*
 * @(#)ViewResources.java
 *
 * Copyright 2002 DayOrganiser LLP. All rights reserved.
 * PROPRIETARY/QPL. Use is subject to license terms.
 */

package com.egantt.model.drawing.axis.view;

/**
  * DrawingAxisConstants interface
  */
public interface ViewResources
{
	/**
	 *  HorizontalView
	 */
	Integer HORIZONTAL = Integer.valueOf(0);

	/**
	 *  VerticalView
	 */
	Integer VERTICAL = Integer.valueOf(1);

	/**
	 *  DepthView / ZView intended for fake 3D / real 3D
	 */
	Integer DEPTH = Integer.valueOf(2);

	/**
	 *  Angle for real 3D only
	 */
	Integer ANGLE = Integer.valueOf(3);
}

