/*
 * Universal Media Server, for streaming any medias to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.newgui;

/**
 * Defines a set of view levels for changing the complexity of the GUI:<br>
 * {@link #Normal}, {@link #Advanced}, {@link #Expert} and {@link #Unknown}.
 * <p/> The <code>ViewLevel</code> class is final and cannot be sub-classed.</p>
 * 
 * @author Nadahar
 */
public class ViewLevel {

  public static final int NORMAL_INT = 0;
  public static final int ADVANCED_INT = 200;
  public static final int EXPERT_INT = 500;
  public static final int DEVELOPER_INT = 1000;
  public static final int UNKNOWN_INT = Integer.MIN_VALUE;

  public static final Integer NORMAL_INTEGER = NORMAL_INT;
  public static final Integer ADVANCED_INTEGER = ADVANCED_INT;
  public static final Integer EXPERT_INTEGER = EXPERT_INT;
  public static final Integer DEVELOPER_INTEGER = DEVELOPER_INT;
  public static final Integer UNKNOWN_INTEGER = UNKNOWN_INT;

  /**
   * <code>NORMAL</code> is meant for normal/novice users.
   */
  public static final ViewLevel NORMAL = new ViewLevel(NORMAL_INT, "Normal");

  /**
   * <code>ADVANCED</code> is meant for advanced users.
   */
  public static final ViewLevel ADVANCED = new ViewLevel(ADVANCED_INT, "Advanced");

  /**
   * <code>EXPERT</code> is meant for expert users.
   */
  public static final ViewLevel EXPERT = new ViewLevel(EXPERT_INT, "Expert");

  /**
   * <code>DEVELOPER</code> is meant for developers where e.g debug information is shown.
   */
  public static final ViewLevel DEVELOPER = new ViewLevel(DEVELOPER_INT, "Developer");

  /**
   * Use <code>UNKNOWN</code> if the view level is unknown.
   */
  public static final ViewLevel UNKNOWN = new ViewLevel(UNKNOWN_INT, "Unknown");

  public final int viewLevelInt;
  public final String viewLevelStr;

  /**
   * Instantiate a ViewLevel object.
   */
  private ViewLevel(int viewLevelInt, String viewLevelStr) {
    this.viewLevelInt = viewLevelInt;
    this.viewLevelStr = viewLevelStr;
  }

  /**
   * Returns the string representation of this ViewLevel.
   */
  public String toString() {
    return viewLevelStr;
  }

  /**
   * Returns the integer representation of this ViewLevel.
   */
  public int toInt() {
    return viewLevelInt;
  }

  /**
   * Convert a ViewLevel to an Integer object.
   *
   * @return This view level's Integer mapping.
   */
  public Integer toInteger() {
    switch (viewLevelInt) {
      case NORMAL_INT:
        return NORMAL_INTEGER;
      case ADVANCED_INT:
        return ADVANCED_INTEGER;
      case EXPERT_INT:
        return EXPERT_INTEGER;
      case DEVELOPER_INT:
        return DEVELOPER_INTEGER;
      case UNKNOWN_INT:
        return UNKNOWN_INTEGER;
      default:
        throw new IllegalStateException("ViewLevel " + viewLevelStr + ", " + viewLevelInt + " is unknown.");
    }
  }

  /**
   * Returns <code>true</code> if this ViewLevel has a higher or equal ViewLevel than
   * the ViewLevel passed as argument, <code>false</code> otherwise.
   */
  public boolean isGreaterOrEqual(ViewLevel r) {
    return viewLevelInt >= r.viewLevelInt;
  }

  /**
   * Convert the string passed as argument to a ViewLevel. If the conversion fails,
   * then this method returns {@link #UNKNOWN}.
   */
  public static ViewLevel toViewLevel(String sArg) {
    return toViewLevel(sArg, ViewLevel.UNKNOWN);
  }

  /**
   * Convert an integer passed as argument to a ViewLevel. If the conversion fails,
   * then this method returns {@link #UNKNOWN}.
   */
  public static ViewLevel toViewLevel(int val) {
    return toViewLevel(val, ViewLevel.UNKNOWN);
  }

  /**
   * Convert an integer passed as argument to a ViewLevel. If the conversion fails,
   * then this method returns the specified default.
   */
  public static ViewLevel toViewLevel(int val, ViewLevel defaultViewLevel) {
    switch (val) {
      case NORMAL_INT:
        return NORMAL;
      case ADVANCED_INT:
        return ADVANCED;
      case EXPERT_INT:
        return EXPERT;
      case DEVELOPER_INT:
        return DEVELOPER;
      case UNKNOWN_INT:
        return UNKNOWN;
      default:
        return defaultViewLevel;
    }
  }

  /**
   * Convert the string passed as argument to a ViewLevel. If the conversion fails,
   * then this method returns the value of <code>defaultLevel</code>.
   */
  public static ViewLevel toViewLevel(String sArg, ViewLevel defaultViewLevel) {
    if (sArg == null) {
      return defaultViewLevel;
    }

    if (sArg.equalsIgnoreCase("Normal")) {
      return ViewLevel.NORMAL;
    }
    if (sArg.equalsIgnoreCase("Advanced")) {
      return ViewLevel.ADVANCED;
    }
    if (sArg.equalsIgnoreCase("Expert")) {
      return ViewLevel.EXPERT;
    }
    if (sArg.equalsIgnoreCase("Developer")) {
      return ViewLevel.DEVELOPER;
    }
    if (sArg.equalsIgnoreCase("Unknown")) {
      return ViewLevel.UNKNOWN;
    }
    return defaultViewLevel;
  }
}
