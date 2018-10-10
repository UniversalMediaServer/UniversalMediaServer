/*
 * Digital Media Server, for streaming digital media to DLNA compatible devices
 * based on www.ps3mediaserver.org and www.universalmediaserver.com.
 * Copyright (C) 2016 Digital Media Server developers.
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
package net.pms.newgui.components;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Formatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.Timer;
import net.pms.newgui.LooksFrame;

/**
 *  The AnimatedIcon will display a series of {@link IconFrames} which each
 *  contains the {@link Icon} itself and the time to display that particular
 *  frame. The animation sequence will cycle until stopped.
 *
 *  The size of the Icon is determined to be the largest width or height of
 *  any Icon. All other Icons are then aligned within the space available when
 *  the Icon is painted.
 *
 *  An AnimatedIcon cannot be shared by different components. However, the Icons
 *  added to an AnimatedIcon can be shared.
 *
 *  This class is in part based on Rob Camick's AnimatedIcon
 *  (https://tips4java.wordpress.com/2009/06/21/animated-icon/).
 */
public class AnimatedIcon implements Icon, ActionListener {
	public final static float TOP = 0.0f;
	public final static float LEFT = 0.0f;
	public final static float CENTER = 0.5f;
	public final static float BOTTOM = 1.0f;
	public final static float RIGHT = 1.0f;

	private JComponent component;
	private final ArrayList<AnimatedIconFrame> frames = new ArrayList<>();
	private boolean running = false;
	private boolean repeat = false;
	private AnimatedIconStage nextStage = null;
	private AnimatedIconStage permanentStage = null;

	private float alignmentX = CENTER;
	private float alignmentY = CENTER;

	//  Track the X, Y location of the Icon within its parent JComponent so we
	//  can request a repaint of only the Icon and not the entire JComponent

	private int iconX;
	private int iconY;

	//  Used for the implementation of Icon interface

	private int maxIconWidth;
	private int maxIconHeight;

	//  Use to control processing

	private int currentFrameIndex;
	private Timer timer;
	private Random random;

	/**
	 * Create an AnimatedIcon.
	 *
	 * @param component the component the icon will be painted on
	 * @param nextStage the {@link AnimatedIconStage} to call after this animation has finished
	 * @param repeat    determines if the animation should loop once the end is reached
	 * @param frames	 the {@link AnimatedIconFrame}s to be painted as an animation
	 */
	private AnimatedIcon(JComponent component, AnimatedIconStage nextStage, boolean repeat, List<AnimatedIconFrame> frames) {
		this.component = component;
		this.repeat = repeat;
		this.nextStage = nextStage;
		if (nextStage != null && nextStage.permanent) {
			this.permanentStage = nextStage;
		}

		timer = new Timer(frames.get(0).lengthMS, this);
		timer.setRepeats(false);
		setFrames(frames);
	}

	/**
	 * Create an AnimatedIcon.
	 *
	 * @param component the component the icon will be painted on
	 * @param repeat    determines if the animation should loop once the end is reached
	 * @param frames	 the {@link AnimatedIconFrame}s to be painted as an animation
	 */
	public AnimatedIcon(JComponent component, boolean repeat, List<AnimatedIconFrame> frames) {
		this(component, null, repeat, frames);
	}

	/**
	 * Create an AnimatedIcon.
	 *
	 * @param component the component the icon will be painted on
	 * @param nextStage the {@link AnimatedIconStage} to call after this animation has finished
	 * @param frames	 the {@link AnimatedIconFrame}s to be painted as an animation
	 */
	public AnimatedIcon(JComponent component, AnimatedIconStage nextStage, List<AnimatedIconFrame> frames) {
		this(component, nextStage, false, frames);
	}

	/**
	 * Create an AnimatedIcon.
	 *
	 * @param component the component the icon will be painted on
	 * @param nextStage the {@link AnimatedIconStage} to call after this animation has finished
	 * @param repeat    determines if the animation should loop once the end is reached
	 * @param frames	 the {@link AnimatedIconFrame}s to be painted as an animation
	 */
	private AnimatedIcon(JComponent component, AnimatedIconStage nextStage, boolean repeat, final AnimatedIconFrame... frames) {
		this.component = component;
		this.repeat = repeat;
		this.nextStage = nextStage;
		if (nextStage != null && nextStage.permanent) {
			this.permanentStage = nextStage;
		}

		timer = new Timer(frames[0].lengthMS, this);
		timer.setRepeats(false);
		setFrames(frames);
	}

	/**
	 * Create an AnimatedIcon.
	 *
	 * @param component the component the icon will be painted on
	 * @param repeat    determines if the animation should loop once the end is reached
	 * @param frames	 the {@link AnimatedIconFrame}s to be painted as an animation
	 */
	public AnimatedIcon(JComponent component, boolean repeat, final AnimatedIconFrame... frames) {
		this(component, null, repeat, frames);
	}

	/**
	 * Create an AnimatedIcon.
	 *
	 * @param component the component the icon will be painted on
	 * @param nextStage the {@link AnimatedIconStage} to call after this animation has finished
	 * @param frames	 the {@link AnimatedIconFrame}s to be painted as an animation
	 */
	public AnimatedIcon(JComponent component, AnimatedIconStage nextStage, final AnimatedIconFrame... frames) {
		this(component, nextStage, false, frames);
	}

	/**
	 * Create an AnimatedIcon with one static frame.
	 *
	 * @param component the component the icon will be painted on
	 * @param icon the {@link Icon} to use for that one frame
	 */
	public AnimatedIcon(JComponent component, Icon icon) {
		this(component, false, AnimatedIcon.buildAnimation(icon));
	}

	/**
	 * Create an AnimatedIcon with one static frame.
	 *
	 * @param component the component the icon will be painted on
	 * @param resourceName the resource name of the image to use for that one
	 *                     frame.
	 */
	public AnimatedIcon(JComponent component, String resourceName) {
		this(component, false, AnimatedIcon.buildAnimation(resourceName));
	}

	/**
	 * Set the sequence of {@link AnimatedIconFrame}s to animate
	 *
	 * @param frames a {@link List} of {@link AnimatedIconFrame}
	 */
	public void setFrames(final List<AnimatedIconFrame> frames) {
		if (frames == null) {
			throw new NullPointerException("Frames cannot be null");
		}

		this.frames.clear();
		this.frames.ensureCapacity(frames.size());
		for (AnimatedIconFrame frame : frames) {
			if (frame == null) {
				throw new NullPointerException("A frame cannot be null");
			}
			if (frame.icon == null) {
				throw new NullPointerException("An icon cannot be null");
			}
			if (frame.lengthMS < 0) {
				throw new IllegalArgumentException("Length can't be negative");
			}
			if (frame.minLengthMS < 0) {
				throw new IllegalArgumentException("Minimum length can't be negative");
			}

			this.frames.add(frame);
		}
		calculateIconDimensions();
		setCurrentFrameIndex(0, true);
	}

	/**
	 * Set the sequence of {@link AnimatedIconFrame}s to animate
	 *
	 * @param frames an array of {@link AnimatedIconFrame}
	 */
	public void setFrames(final AnimatedIconFrame... frames) {
		if (frames == null) {
			throw new NullPointerException("Frames cannot be null");
		}

		this.frames.clear();
		this.frames.ensureCapacity(frames.length);
		for (AnimatedIconFrame frame : frames) {
			if (frame == null) {
				throw new NullPointerException("A frame cannot be null");
			}
			if (frame.icon == null) {
				throw new NullPointerException("An icon cannot be null");
			}
			if (frame.lengthMS < 0) {
				throw new IllegalArgumentException("Length can't be negative");
			}
			if (frame.minLengthMS < 0) {
				throw new IllegalArgumentException("Minimum length can't be negative");
			}

			this.frames.add(frame);
		}
		calculateIconDimensions();
		setCurrentFrameIndex(0, true);
	}

	/**
	 * Set the {@link AnimatedIconStage} to call after this animation has
	 * finished. Sets repeat to {@code false}.
	 */
	public void setNextStage(AnimatedIconStage nextStage) {
		this.nextStage = nextStage;
		if (nextStage != null && nextStage.permanent) {
			this.permanentStage = nextStage;
		}
		if (!timer.isRunning()) {
			timer.restart();
		}
	}

	/**
	 *  Calculate the width and height of the Icon based on the maximum
	 *  width and height of any individual Icon.
	 */
	private void calculateIconDimensions() {
		maxIconWidth = 0;
		maxIconHeight = 0;

		for (AnimatedIconFrame frame : frames)
		{
			maxIconWidth = Math.max(maxIconWidth, frame.icon.getIconWidth());
			maxIconHeight = Math.max(maxIconHeight, frame.icon.getIconHeight());
		}
	}

	/**
	 *  Get the alignment of the Icon on the x-axis
	 *
	 *  @return the alignment
	 */
	public float getAlignmentX() {
		return alignmentX;
	}

	/**
	 *  Specify the horizontal alignment of the icon.
	 *
	 *  @param alignmentX  common values are LEFT, CENTER (default)  or RIGHT
	 *                     although any value between 0.0 and 1.0 can be used
	 */
	public void setAlignmentX(float alignmentX) {
		this.alignmentX = alignmentX > 1.0f ? 1.0f : alignmentX < 0.0f ? 0.0f : alignmentX;
	}

	/**
	 *  Get the alignment of the icon on the y-axis
	 *
	 *  @return the alignment
	 */
	public float getAlignmentY() {
		return alignmentY;
	}

	/**
	 *  Specify the vertical alignment of the Icon.
	 *
	 *  @param alignmentY  common values TOP, CENTER (default) or BOTTOM
	 *                     although any value between 0.0 and 1.0 can be used
	 */
	public void setAlignmentY(float alignmentY) {
		this.alignmentY = alignmentY > 1.0f ? 1.0f : alignmentY < 0.0f ? 0.0f : alignmentY;
	}

	/**
	 *  Set the index of the frame to be displayed and then repaint the {@link Icon}.
	 *
	 *  @param index the index of the {@link AnimatedIconFrame} to be displayed
	 *  @param paint determines if the new frame should be painted
	 */
	private void setCurrentFrameIndex(int index, boolean paint) {
		currentFrameIndex = index;
		final AnimatedIconFrame frame = frames.get(currentFrameIndex);
		if (frame.random) {
			if (random == null) {
				random = new Random();
			}
			timer.setInitialDelay(frame.minLengthMS + random.nextInt(frame.lengthMS - frame.minLengthMS + 1));
		} else {
			timer.setInitialDelay(frame.lengthMS);
		}
		if (running) {
			timer.restart();
		}
		if (paint) {
			component.repaint(iconX, iconY, maxIconWidth, maxIconHeight);
		}
	}

	/**
	 *  Starts the animation the beginning.
	 */
	public void restart() {
		setCurrentFrameIndex(0, true);
		start();
	}

	/**
	 * Sets the animation to continue from the beginning then continued.
	 */
	public void restartArm() {
		setCurrentFrameIndex(0, false);
	}

	/**
	 * Arms the animation so that the timer is started during the next {@link #paintIcon(Component, Graphics, int, int)}
	 */
	public void start() {
		running = true;
	}

	/**
	 *  Stops the animation. The current frame will be displayed.
	 */
	public void stop() {
		if (timer.isRunning()) {
			timer.stop();
		}
		running = false;
	}

	/**
	 * Pauses the animation at the current frame.
	 */
	public void pause() {
		if (timer.isRunning()) {
			timer.stop();
		}
	}

	/**
	 * Resumes the animation if it is armed/has been paused, automatically
	 * called by {@link #paintIcon(Component, Graphics, int, int)}
	 */
	public void resume() {
		if (running && !timer.isRunning()) {
			timer.restart();
		}
	}

	/**
	 * Stops and resets the animation. The first frame will be displayed.
	 */
	public void reset() {
		running = false;
		if (timer.isRunning()) {
			timer.stop();
		}
		setCurrentFrameIndex(0, true);
	}

	// Implement the Icon Interface

	/**
	 *  Gets the width of this icon.
	 *
	 *  @return the width of the icon in pixels.
	 */
	@Override
	public int getIconWidth() {
		return maxIconWidth;
	}

	/**
	 *  Gets the height of this icon.
	 *
	 *  @return the height of the icon in pixels.
	 */
	@Override
	public int getIconHeight() {
		return maxIconHeight;
	}

   /**
	*  Paints the icons of this compound icon at the specified location
	*
	*  @param c The component on which the icon is painted
	*  @param g the graphics context
	*  @param x the X coordinate of the icon's top-left corner
	*  @param y the Y coordinate of the icon's top-left corner
	*/
	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {

		// If the previous icon was an AnimatedIcon, stop the animation
		if (c instanceof JAnimatedButton) {
			if (((JAnimatedButton) c).getCurrentIcon() != this) {
				if (((JAnimatedButton) c).getCurrentIcon() != null) {
					((JAnimatedButton) c).getCurrentIcon().pause();
				}

				((JAnimatedButton) c).setCurrentIcon(this);
				resume();
			}
		}

		//  Saving the x, y coordinates allows us to only repaint the icon and
		//  not the entire component for each animation

		if (c == component)
		{
			iconX = x;
			iconY = y;
		}

		//  Determine the proper alignment of the Icon, then paint it

		Icon icon = frames.get(currentFrameIndex).icon;
   		int width = getIconWidth();
   		int height = getIconHeight();

		int offsetX = getOffset(width, icon.getIconWidth(), alignmentX);
		int offsetY = getOffset(height, icon.getIconHeight(), alignmentY);

		icon.paintIcon(c, g, x + offsetX, y + offsetY);
	}

	/*
	 *  When the icon value is smaller than the maximum value of all icons the
	 *  icon needs to be aligned appropriately. Calculate the offset to be used
	 *  when painting the icon to achieve the proper alignment.
	 */
	private static int getOffset(int maxValue, int iconValue, float alignment) {
		float offset = (maxValue - iconValue) * alignment;
		return Math.round(offset);
	}

	// Implement the ActionListener interface

	/**
	 *  Controls the animation of the {@link Icon}s when the {@link Timer} fires.
	 */
	public void actionPerformed(ActionEvent e) {
		// Take the appropriate action for the next step in the animation
		// This runs in Swing's event dispatcher thread, so no thread safety
		// is needed.

		int nextFrameIndex = getNextFrameIndex(currentFrameIndex);
		if (nextFrameIndex < 0 || nextFrameIndex == currentFrameIndex) {
			pause();
			if (nextStage != null) {
				if (component instanceof AnimatedIconCallback) {
					((AnimatedIconCallback) component).setNextIcon(nextStage);
				}
				nextStage = permanentStage;
			}
		} else {
			setCurrentFrameIndex(nextFrameIndex, true);
		}
	}

	private int getNextFrameIndex(int currentIndex) {
		if (repeat && nextStage == null) {
			return ++currentIndex % frames.size();
		}
		if (currentIndex >= frames.size() - 1) {
			return -1;
		}
		return ++currentIndex;
	}

	/**
	 * This will build and return an array of {@link AnimatedIconFrame}s based
	 * on a first and last index and a {@link Formatter} formatted resource
	 * name string.<br>
	 * <br>
	 * <bold>TIP:</bold>
	 * Leading zeroes can be specified by using the form <code>%0nd<code> where
	 * n is the total number of digits. To format the number <code>4</code> as
	 * <code>004</code> define it as <code>%03d</code> in the resource name
	 * pattern.
	 *
	 * @param resourceNamePattern the resource named written as a
	 *        {@link Formatter}.
	 * @param firstIdx the first index number to use with the pattern.
	 * @param lastIdx the last index number to use with the pattern.
	 * @param returnToFirst specifies whether the animation should reverse back
	 *        to the first frame after reaching the last frame.
	 * @param durationFirst the duration in milliseconds for the first frame.
	 * @param durationLast the duration in milliseconds for the last frame.
	 * @param duration the duration in milliseconds for all frames but the
	 *        first and the last.
	 * @return The built array of {@link AnimatedIconFrame}s.
	 */
	public static AnimatedIconFrame[] buildAnimation(
			String resourceNamePattern,
			int firstIdx,
			int lastIdx,
			boolean returnToFirst,
			int durationFirst,
			int durationLast,
			int duration
		) {
		AnimatedIconFrame[] result = new AnimatedIconFrame[returnToFirst ? 2 * (lastIdx - firstIdx) : lastIdx - firstIdx + 1];

		int idx = firstIdx;
		for (int i = 0;i <= lastIdx - firstIdx;i++) {
			Icon icon = LooksFrame.readImageIcon(String.format(resourceNamePattern, idx));
			if (icon == null) {
				throw new IllegalArgumentException(String.format(
					"Resource \"%s\" not found, please check your pattern (%s) and indices (%d-%d)!",
					String.format(resourceNamePattern, idx),
					resourceNamePattern,
					firstIdx,
					lastIdx
				));
			}
			if (idx > firstIdx && idx < lastIdx) {
				AnimatedIconFrame frame = new AnimatedIconFrame(icon, duration);
				result[i] = frame;
				if (returnToFirst) {
					result[2 * (lastIdx - firstIdx) - i] = frame;
				}
			} else if (idx == firstIdx) {
				result[i] = new AnimatedIconFrame(icon, durationFirst);
			} else {
				result[i] = new AnimatedIconFrame(icon, durationLast);
			}

			idx++;
		}
		return result;
	}

	/**
	 * This will build and return an array of one {@link AnimatedIconFrame}
	 * containing one icon. This is simply a convenience method for converting
	 * an {@link Icon} into an {@link AnimatedIconFrame} with no animation.
	 *
	 * @param icon the {@link Icon} to use for the single frame "animation".
	 * @return The built array of one {@link AnimatedIconFrame}.
	 */
	public static AnimatedIconFrame[] buildAnimation(Icon icon) {
		if (icon == null) {
			throw new IllegalArgumentException("icon cannot be null!");
		}
		return new AnimatedIconFrame[]{new AnimatedIconFrame(icon, 0)};
	}

	/**
	 * This will build and return an array of one {@link AnimatedIconFrame}
	 * containing one icon loaded from the given resource name.
	 *
	 * @param resourceName the name of the resource to use.
	 * @return The built array of one {@link AnimatedIconFrame}.
	 */
	public static AnimatedIconFrame[] buildAnimation(String resourceName) {
		Icon icon = LooksFrame.readImageIcon(resourceName);
		if (icon == null) {
			throw new IllegalArgumentException(String.format("Resource \"%s\" not found!", resourceName));
		}
		return buildAnimation(icon);
	}

	/**
	 * Defines one frame in an {@link AnimatedIcon} animation.
	 *
	 */
	public static class AnimatedIconFrame {
		/**
		 * The {@link Icon} to display for this frame
		 */
		public final Icon icon;

		/**
		 * The number of milliseconds this frame will be displayed for constant
		 * duration frames or the maximum number of milliseconds this frame
		 * will be displayed for random duration frames.
		 */
		public final int lengthMS;

		/**
		 * The minimum number of milliseconds this frame will displayed when
		 * generating a random value.
		 */
		public final int minLengthMS;

		/**
		 * Indicates if the frame is a random duration frame.
		 */

		public final boolean random;

		/**
		 * Creates an {@link AnimatedIconFrame} frame with constant duration.
		 *
		 * @param icon the {@link Icon} to display for this frame.
		 * @param lengthMS the duration of this frame in milliseconds.
		 */
		public AnimatedIconFrame(Icon icon, int lengthMS) {
			this.icon = icon;
			this.lengthMS = lengthMS;
			this.random = false;
			this.minLengthMS = 0;
		}

		/**
		 * Creates an {@link AnimatedIconFrame} frame with random duration.
		 *
		 * @param icon the {@link Icon} to display for this frame
		 * @param minLengthMS the minimum duration of this frame in milliseconds
		 * @param maxLengthMS the maximum duration of this frame in milliseconds
		 */
		public AnimatedIconFrame(Icon icon, int minLengthMS, int maxLengthMS) {
			this.icon = icon;
			this.minLengthMS = minLengthMS;
			this.lengthMS = maxLengthMS;
			this.random = true;
		}
	}

	/**
	 * Defines icon type used in callback
	 */
	public static enum AnimatedIconType {
		DEFAULTICON,
		PRESSEDICON,
		DISABLEDICON,
		SELECTEDICON,
		DISABLEDSELECTEDICON,
		ROLLOVERICON,
		ROLLOVERSELECTEDICON
	}

	/**
	 * Defines an {@link AnimatedIcon} stage used for callback
	 */
	public static class AnimatedIconStage {

		/**
		 * The icon type for this stage
		 */
		public final AnimatedIconType iconType;

		/**
		 * The icon for this stage
		 */
		public final AnimatedIcon icon;

		/**
		 * Whether this is a permanent stage or a one-time event
		 */
		public final boolean permanent;

		/**
		 * Creates a new {@link AnimatedIcon} stage.
		 *
		 * @param iconType determines which {@link AnimatedIcon} this stage will replace.
		 * @param icon the {@link AnimatedIcon} for this stage.
		 * @param permanent specifies whether this is a permanent stage or a one-time event
		 */
		public AnimatedIconStage(AnimatedIconType iconType, AnimatedIcon icon, boolean permanent) {
			this.iconType = iconType;
			this.icon = icon;
			this.permanent = permanent;
		}
	}
}
