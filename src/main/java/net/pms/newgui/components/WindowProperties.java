/*
 * Digital Media Server, for streaming digital media to UPnP AV or DLNA
 * compatible devices based on PS3 Media Server and Universal Media Server.
 * Copyright (C) 2016 Digital Media Server developers.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see http://www.gnu.org/licenses/.
 */
package net.pms.newgui.components;

import static org.apache.commons.lang3.StringUtils.isBlank;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class keeps track of desktop screens, their resolution and insets and
 * the size and position of the specified {@link Window}. The properties is
 * saved to and loaded from disk by a {@link WindowPropertiesConfiguration}
 * instance, so that the properties are maintained across restarts.
 * <p>
 * Listeners are used to track changes, and the linked
 * {@link WindowPropertiesConfiguration} is updated continuously. The specified
 * {@link Window} is initialized according to the
 * {@link WindowPropertiesConfiguration} instance and the constructor parameters
 * in the constructor.
 *
 * @author Nadahar
 */
@NotThreadSafe
public class WindowProperties implements WindowListener, ComponentListener {
	private String graphicsDevice;
	private Rectangle screenBounds;
	private Rectangle effectiveScreenBounds;
	private Insets screenInsets;
	private Rectangle windowBounds;
	private byte windowState;
	private Window window;
	private final WindowPropertiesConfiguration windowConfiguration;
	private final Dimension minimumSize;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param window the {@link Window} whose properties to keep track of.
	 * @param standardSize the standard size of {@code window}.
	 * @param mimimumSize the minimum size of {@code window}.
	 * @param windowConfiguration the {@link WindowPropertiesConfiguration}
	 *            instance for reading and writing the window properties.
	 */
	public WindowProperties(
		@Nonnull Window window,
		@Nullable Dimension standardSize,
		@Nullable Dimension mimimumSize,
		@Nullable WindowPropertiesConfiguration windowConfiguration
	) {
		if (window == null) {
			throw new IllegalArgumentException("window cannot be null");
		}
		this.window = window;
		this.minimumSize = mimimumSize;
		if (mimimumSize != null) {
			window.setMinimumSize(mimimumSize);
		}
		this.windowConfiguration = windowConfiguration;
		if (windowConfiguration != null) {
			getConfiguration();
			GraphicsConfiguration windowGraphicsConfiguration = window.getGraphicsConfiguration();
			if (
				windowBounds != null &&
				effectiveScreenBounds != null &&
				graphicsDevice != null &&
				graphicsDevice.equals(windowGraphicsConfiguration.getDevice().getIDstring()) &&
				screenBounds != null &&
				screenBounds.equals(windowGraphicsConfiguration.getBounds())
			) {
				setWindowBounds();
			} else {
				Rectangle screen = effectiveScreenBounds != null ? effectiveScreenBounds : windowGraphicsConfiguration.getBounds();
				if (standardSize != null && screen.width >= standardSize.width && screen.height >= standardSize.height) {
					window.setSize(standardSize);
				} else if (
					mimimumSize != null && (
						window.getWidth() < mimimumSize.width ||
						window.getHeight() < mimimumSize.getHeight()
					)) {
					window.setSize(mimimumSize);
				}
				window.setLocationByPlatform(true);
			}
			if (window instanceof Frame) {
				// Set maximized state
				int maximizedState = windowState & Frame.MAXIMIZED_BOTH;
				if (maximizedState != 0) {
					((Frame) window).setExtendedState(((Frame) window).getExtendedState() | maximizedState);
				}
			}
		}
		window.addWindowListener(this);
		window.addComponentListener(this);
	}

	/**
	 * Unregisters the listeners and releases the {@link Window} set up by the
	 * constructor. This instance will never be GC'ed unless this method is
	 * called first, since the registered listeners has references to this
	 * instance.
	 */
	public void dispose() {
		// Unregister listeners
		if (window != null) {
			window.removeWindowListener(this);
			window.removeComponentListener(this);
			window = null;
		}
	}

	private boolean getConfiguration() {
		if (windowConfiguration == null) {
			return false;
		}
		boolean changed = false;
		if (
			graphicsDevice == null && windowConfiguration.graphicsDevice != null ||
			graphicsDevice != null && !graphicsDevice.equals(windowConfiguration.graphicsDevice)
		) {
			graphicsDevice = windowConfiguration.graphicsDevice;
			changed = true;
		}
		if (
			screenBounds == null && windowConfiguration.screenBounds != null ||
			screenBounds != null && !screenBounds.equals(windowConfiguration.screenBounds)
		) {
			screenBounds = windowConfiguration.screenBounds;
			changed = true;
		}
		if (
			screenInsets == null && windowConfiguration.screenInsets != null ||
			screenInsets != null && !screenInsets.equals(windowConfiguration.screenInsets)
		) {
			screenInsets = windowConfiguration.screenInsets;
			changed = true;
		}
		if (windowState != windowConfiguration.windowState) {
			windowState = windowConfiguration.windowState;
			changed = true;
		}
		if (
			windowBounds == null && windowConfiguration.windowBounds != null ||
			windowBounds != null && !windowBounds.equals(windowConfiguration.windowBounds)
		) {
			windowBounds = windowConfiguration.windowBounds;
			changed = true;
		}
		if (changed) {
			updateEffectiveScreenBounds();
		}
		return changed;
	}

	private void setConfiguration() {
		if (windowConfiguration == null) {
			return;
		}
		boolean changed = false;
		if (
			graphicsDevice == null && windowConfiguration.graphicsDevice != null ||
			graphicsDevice != null && !graphicsDevice.equals(windowConfiguration.graphicsDevice)
		) {
			windowConfiguration.graphicsDevice = graphicsDevice;
			changed = true;
		}
		if (
			screenBounds == null && windowConfiguration.screenBounds != null ||
			screenBounds != null && !screenBounds.equals(windowConfiguration.screenBounds)
		) {
			windowConfiguration.screenBounds = screenBounds;
			changed = true;
		}
		if (
			screenInsets == null && windowConfiguration.screenInsets != null ||
			screenInsets != null && !screenInsets.equals(windowConfiguration.screenInsets)
		) {
			windowConfiguration.screenInsets = screenInsets;
			changed = true;
		}
		if (windowState != windowConfiguration.windowState) {
			windowConfiguration.windowState = windowState;
			changed = true;
		}
		if (
			windowBounds == null && windowConfiguration.windowBounds != null ||
			windowBounds != null && !windowBounds.equals(windowConfiguration.windowBounds)
		) {
			windowConfiguration.windowBounds = windowBounds;
			changed = true;
		}
		if (changed) {
			windowConfiguration.writeConfiguration();
		}
	}


	private boolean updateDevice(@Nullable Window eventWindow) {
		if (eventWindow != window) {
			return false;
		}
		String deviceString = window.getGraphicsConfiguration().getDevice().getIDstring();
		if (deviceString == null) {
			if (graphicsDevice == null) {
				return false;
			}
		} else if (deviceString.equals(graphicsDevice)) {
			return false;
		}
		graphicsDevice = deviceString;
		return true;
	}

	private boolean updateEffectiveScreenBounds() {
		if (screenBounds == null) {
			return false;
		}
		Insets tmpInsets = screenInsets == null ? new Insets(0, 0, 0, 0) : screenInsets;
		Rectangle newEffectiveScreenBounds = new Rectangle(
			screenBounds.x,
			screenBounds.y,
			screenBounds.width - tmpInsets.left - tmpInsets.right,
			screenBounds.height - tmpInsets.top - tmpInsets.bottom
		);
		if (!newEffectiveScreenBounds.equals(effectiveScreenBounds)) {
			effectiveScreenBounds = newEffectiveScreenBounds;
			return true;
		}
		return false;
	}

	private boolean updateProperties(@Nullable Window eventWindow) {
		if (eventWindow != window) {
			return false;
		}
		boolean changed = updateWindowBounds(eventWindow);
		changed |= updateScreenProperties(eventWindow);
		changed |= updateDevice(eventWindow);
		if (changed && windowConfiguration != null) {
			setConfiguration();
		}
		return changed;
	}

	private void setWindowBounds() {
		if (windowBounds == null) {
			return;
		}
		if (effectiveScreenBounds == null) {
			window.setBounds(windowBounds);
			return;
		}
		int deltaX = 0;
		if (windowBounds.x + windowBounds.width > effectiveScreenBounds.x + effectiveScreenBounds.width) {
			deltaX = effectiveScreenBounds.x + effectiveScreenBounds.width - windowBounds.x - windowBounds.width;
		}
		if (windowBounds.x < effectiveScreenBounds.x) {
			deltaX = effectiveScreenBounds.x - windowBounds.x;
		}
		int deltaY = 0;
		if (windowBounds.y + windowBounds.height > effectiveScreenBounds.y + effectiveScreenBounds.height) {
			deltaY = effectiveScreenBounds.y + effectiveScreenBounds.height - windowBounds.y - windowBounds.height;
		}
		if (windowBounds.y < effectiveScreenBounds.y) {
			deltaY = effectiveScreenBounds.y - windowBounds.y;
		}
		if (deltaX != 0 || deltaY != 0) {
			windowBounds.translate(deltaX, deltaY);
		}
		if (!effectiveScreenBounds.contains(windowBounds)) {
			Rectangle newWindowBounds = windowBounds.intersection(effectiveScreenBounds);
			if (newWindowBounds.width < minimumSize.width) {
				newWindowBounds.width = minimumSize.width;
			}
			if (newWindowBounds.height < minimumSize.height) {
				newWindowBounds.height = minimumSize.height;
			}
			windowBounds = newWindowBounds;
		}
		window.setBounds(windowBounds);
	}

	private boolean updateScreenProperties(@Nullable Window eventWindow) {
		if (eventWindow != window) {
			return false;
		}
		boolean changed = updateScreenBounds(eventWindow);
		changed |= updateScreenInsets(eventWindow);
		if (changed && updateEffectiveScreenBounds()) {
			if (windowBounds != null && effectiveScreenBounds != null && !effectiveScreenBounds.contains(windowBounds)) {
				setWindowBounds();
			}
		}
		return changed;
	}

	private boolean updateScreenBounds(@Nonnull Window eventWindow) {
		Rectangle bounds = eventWindow.getGraphicsConfiguration().getBounds();
		if (bounds == null) {
			if (screenBounds == null) {
				return false;
			}
		} else if (bounds.equals(screenBounds)) {
			return false;
		}
		screenBounds = bounds;
		return true;
	}

	private boolean updateScreenInsets(@Nonnull Window eventWindow) {
		Insets insets = eventWindow.getToolkit().getScreenInsets(eventWindow.getGraphicsConfiguration());
		if (insets == null) {
			if (screenInsets == null) {
				return false;
			}
		} else if (insets.equals(screenInsets)) {
			return false;
		}
		screenInsets = insets;
		return true;
	}

	private boolean updateWindowBounds(@Nullable Window eventWindow) {
		if (eventWindow != window) {
			return false;
		}
		int state = eventWindow instanceof Frame ? ((Frame) eventWindow).getExtendedState() : 0;
		Rectangle bounds;
		if (state == 0) {
			bounds = eventWindow.getBounds();
		} else if ((state & Frame.MAXIMIZED_BOTH) != Frame.MAXIMIZED_BOTH) {
			bounds = eventWindow.getBounds();
			// Don't store maximized dimensions
			if ((state & Frame.MAXIMIZED_HORIZ) != 0) {
				bounds.x = windowBounds.x;
				bounds.width = windowBounds.width;
			} else if ((state & Frame.MAXIMIZED_VERT) != 0) {
				bounds.y = windowBounds.y;
				bounds.height = windowBounds.height;
			}
		} else {
			bounds = windowBounds;
		}
		boolean changed = !bounds.equals(windowBounds);
		if (changed) {
			windowBounds = bounds;
		}
		if (windowState != (byte) state) {
			windowState = (byte) state;
			changed = true;
		}
		return changed;
	}

	@Override
	public void windowOpened(WindowEvent e) {
		updateProperties(e.getWindow());
	}

	@Override
	public void windowClosing(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	@Override
	public void componentResized(ComponentEvent e) {
		updateProperties((Window) e.getSource());
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		updateProperties((Window) e.getSource());
	}

	@Override
	public void componentShown(ComponentEvent e) {
	}

	@Override
	public void componentHidden(ComponentEvent e) {
	}

	/**
	 * This class handles storage of window properties for a
	 * {@link WindowProperties} instance.
	 *
	 * @author Nadahar
	 */
	@NotThreadSafe
	public static class WindowPropertiesConfiguration {

		private static final Logger LOGGER = LoggerFactory.getLogger(WindowPropertiesConfiguration.class);

		private static final byte[] MAGIC_BYTES = {(byte) 68, (byte) 71, (byte) 83};
		private static final byte VERSION = 1;
		private static final int BUFFER_SIZE = 128;
		private static final ByteOrder BYTE_ORDER = ByteOrder.BIG_ENDIAN;
		private static final Charset CHARSET = StandardCharsets.US_ASCII;
		private static final byte NULL = (byte) (1 << 7);

		private String graphicsDevice;
		private Rectangle screenBounds;
		private Insets screenInsets;
		private Rectangle windowBounds;
		private byte windowState;
		private Path path;

		/**
		 * Creates a new instance bound to the specified {@link Path}.
		 *
		 * @param path the {@link Path} used to read and write window
		 *            properties.
		 */
		public WindowPropertiesConfiguration(@Nonnull Path path) {
			if (path == null) {
				throw new IllegalArgumentException("path cannot be null");
			}
			this.path = path;
			readConfiguration();
		}

		/**
		 * Finds the {@link GraphicsConfiguration} that matches the previously
		 * stored information, if any.
		 * <p>
		 * <b>Note:</b> This class is <i>not</i> thread-safe, and all other
		 * methods is called either by the constructor or the event dispatcher
		 * thread. Great care should be taken when calling this method to make
		 * sure this {@link WindowPropertiesConfiguration} isn't currently in
		 * use by a {@link WindowProperties} instance at the time.
		 *
		 * @return The matching {@link GraphicsConfiguration} or {@code null} if
		 *         no match was found.
		 */
		@Nullable
		public GraphicsConfiguration getGraphicsConfiguration() {
			if (isBlank(graphicsDevice) || screenBounds == null) {
				LOGGER.debug("No stored graphics device, using the default");
				return null;
			}
			for (GraphicsDevice graphicsDeviceItem : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
				if (graphicsDevice.equals(graphicsDeviceItem.getIDstring())) {
					for (GraphicsConfiguration graphicsConfiguration : graphicsDeviceItem.getConfigurations()) {
						if (screenBounds.equals(graphicsConfiguration.getBounds())) {
							return graphicsConfiguration;
						}
					}
				}
			}
			LOGGER.debug("No matching graphics configuration found, using the default");
			return null;
		}

		void readConfiguration() {
			ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
			try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ)) {
				boolean eof = fillBuffer(buffer, channel, 4);
				buffer.flip();
				if (buffer.remaining() < 4) {
					LOGGER.warn(
						"Invalid window properties configuration file \"{}\", file too short ({} bytes)",
						path,
						buffer.remaining()
					);
					return;
				}
				byte[] bytes = new byte[3];
				buffer.get(bytes);
				if (!Arrays.equals(bytes, MAGIC_BYTES)) {
					LOGGER.warn("Invalid window properties configuration file header in \"{}\"", path);
					return;
				}
				if (buffer.get() != VERSION) {
					LOGGER.debug("Incorrect window properties configuration version, ignoring file \"{}\"", path);
					return;
				}
				if (eof && !buffer.hasRemaining()) {
					LOGGER.warn("Empty window properties configuration, ignoring file \"{}\"", path);
					return;
				}
				ReadResult<Rectangle> rectangle = readRectangle(buffer, channel);
				if (rectangle.error != null) {
					LOGGER.warn("Windows properties configuration file \"{}\" error: {}", path, rectangle.error);
					return;
				}
				screenBounds = rectangle.value;
				eof |= rectangle.eof;
				if (!buffer.hasRemaining()) {
					LOGGER.warn("Window properties configuration file \"{}\" is truncated", path);
					return;
				}
				ReadResult<Insets> insets = readInsets(buffer, channel);
				if (insets.error != null) {
					LOGGER.warn("Windows properties configuration file \"{}\" error: {}", path, rectangle.error);
					return;
				}
				screenInsets = insets.value;
				eof |= insets.eof;
				if (eof && !buffer.hasRemaining()) {
					LOGGER.warn(
						"Window properties configuration file \"{}\" is truncated",
						path
					);
					return;
				}
				rectangle = readRectangle(buffer, channel);
				if (rectangle.error != null) {
					LOGGER.warn("Windows properties configuration file \"{}\" error: {}", path, rectangle.error);
					return;
				}
				windowBounds = rectangle.value;
				windowState = (byte) (rectangle.flags & ~NULL);
				eof |= rectangle.eof;
				if (eof && !buffer.hasRemaining()) {
					LOGGER.warn(
						"Window properties configuration file \"{}\" is truncated",
						path
					);
					return;
				}
				ReadResult<String> string = readString(buffer, channel);
				if (string.error != null) {
					LOGGER.warn("Windows properties configuration file \"{}\" error: {}", path, rectangle.error);
					return;
				}
				graphicsDevice = string.value;
				eof |= string.eof;
				if (buffer.hasRemaining()) {
					LOGGER.warn(
						"Window properties configuration file \"{}\" contains unknown additional data",
						path
					);
					return;
				}
			} catch (IOException e) {
				if (e instanceof NoSuchFileException) {
					LOGGER.debug("Window properties configuration file \"{}\" not found", path);
				} else {
					LOGGER.error("Error reading window properties configuration file \"{}\": {}", path, e.getMessage());
					LOGGER.trace("", e);
				}
			}
		}

		void writeConfiguration() {
			try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(BUFFER_SIZE)) {
				bytes.write(MAGIC_BYTES);
				bytes.write(VERSION);
				writeRectangle(bytes, screenBounds, (byte) 0);
				writeInsets(bytes, screenInsets);
				writeRectangle(bytes, windowBounds, windowState);
				writeString(bytes, graphicsDevice);
				try (SeekableByteChannel channel = Files.newByteChannel(
					path,
					StandardOpenOption.WRITE,
					StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING
				)) {
					ByteBuffer buffer = ByteBuffer.wrap(bytes.toByteArray());
					while (buffer.hasRemaining()) {
						channel.write(buffer);
					}
				}
			} catch (IOException e) {
				LOGGER.error("Error writing window properties configuration file \"{}\": {}", path, e.getMessage());
				LOGGER.trace("", e);
			}
		}

		private static boolean fillBuffer(ByteBuffer buffer, SeekableByteChannel channel, int count) throws IOException {
			int targetPosition = buffer.position() + count;
			if (targetPosition > buffer.capacity()) {
				throw new IndexOutOfBoundsException(
					"Can't read " + count + " bytes into a free space of " + buffer.remaining() + "bytes"
				);
			}
			boolean eof = false;
			while (!eof && buffer.position() < targetPosition) {
				eof = channel.read(buffer) < 0;
			}
			return eof;
		}

		private static ReadResult<String> readString(
			@Nonnull ByteBuffer buffer,
			@Nonnull SeekableByteChannel channel
		) throws IOException {
			boolean terminated = false;
			boolean eof = false;
			ReadResult<Byte> status = readByte(buffer, channel);
			eof |= status.eof;
			if (status.error != null || status.value == null) {
				return new ReadResult<>(null, eof, NULL, false, "Couldn't read string: " + status.error);
			}
			if ((status.value.byteValue() & NULL) != 0) {
				return new ReadResult<>(null, eof, status.value.byteValue(), false, null);
			}

			StringBuilder string = new StringBuilder();
			while (!terminated && !eof) {
				if (!buffer.hasRemaining()) {
					buffer.clear();
					eof = fillBuffer(buffer, channel, buffer.capacity());
					buffer.flip();
				}
				if (buffer.hasRemaining()) {
					int terminator = -1;
					for (int i = buffer.position(); i < buffer.limit(); i++) {
						if (buffer.get(i) == (byte) 0) {
							terminator = i;
							break;
						}
					}
					byte[] bytes;
					if (terminator < 0) {
						bytes = new byte[buffer.remaining()];
					} else {
						terminated = true;
						bytes = new byte[terminator - buffer.position()];
					}
					if (bytes.length > 0) {
						buffer.get(bytes);
						string.append(new String(bytes, CHARSET));
					}
				}
			}
			if (buffer.hasRemaining()) {
				if (buffer.get() != (byte) 0) {
					throw new AssertionError("Buffer has more data that doesn't start with the terminator");
				}
			}
			if (!terminated) {
				return new ReadResult<>(string.toString(), eof, status.value.byteValue(), true, "String is truncated");
			}
			return new ReadResult<>(string.toString(), eof, status.value.byteValue());
		}

		private static ReadResult<Byte> readByte(ByteBuffer buffer, SeekableByteChannel channel) throws IOException {
			boolean eof = false;
			if (!buffer.hasRemaining()) {
				buffer.clear();
				eof = fillBuffer(buffer, channel, 1);
				buffer.flip();
			}
			if (buffer.hasRemaining()) {
				return new ReadResult<>(Byte.valueOf(buffer.get()), eof, (byte) 0);
			}
			return new ReadResult<>(null, eof, NULL, false, "Invalid file format or truncated file");
		}

		private static ReadResult<Integer> readInteger(ByteBuffer buffer, SeekableByteChannel channel) throws IOException {
			boolean eof = false;
			if (buffer.remaining() < 4) {
				buffer.compact();
				eof = fillBuffer(buffer, channel, 4);
				buffer.flip();
			}
			if (buffer.remaining() < 4) {
				return new ReadResult<>(null, eof, NULL, false, "File is truncated");
			}
			return new ReadResult<>(Integer.valueOf(buffer.getInt()), eof, (byte) 0);
		}

		private static ReadResult<Rectangle> readRectangle(ByteBuffer buffer, SeekableByteChannel channel) throws IOException {
			ReadResult<Byte> status = readByte(buffer, channel);
			boolean eof = status.eof;
			if (status.error != null || status.value == null) {
				return new ReadResult<>(null, eof, NULL, false, "Couldn't read rectangle: " + status.error);
			}
			if ((status.value.byteValue() & NULL) != 0) {
				return new ReadResult<>(null, eof, status.value.byteValue(), false, null);
			}

			ReadResult<Integer> integer = readInteger(buffer, channel);
			eof |= integer.eof;
			if (integer.value == null || integer.error != null) {
				return new ReadResult<>(null, eof, NULL, false, "Couldn't read rectangle: " + integer.error);
			}
			int x = integer.value.intValue();
			integer = readInteger(buffer, channel);
			eof |= integer.eof;
			if (integer.value == null || integer.error != null) {
				return new ReadResult<>(null, eof, NULL, false, "Couldn't read rectangle: " + integer.error);
			}
			int y = integer.value.intValue();
			integer = readInteger(buffer, channel);
			eof |= integer.eof;
			if (integer.value == null || integer.error != null) {
				return new ReadResult<>(null, eof, NULL, false, "Couldn't read rectangle: " + integer.error);
			}
			int width = integer.value.intValue();
			integer = readInteger(buffer, channel);
			eof |= integer.eof;
			if (integer.value == null || integer.error != null) {
				return new ReadResult<>(null, eof, NULL, false, "Couldn't read rectangle: " + integer.error);
			}
			int height = integer.value.intValue();
			return new ReadResult<>(new Rectangle(x, y, width, height), eof, status.value.byteValue());
		}

		private static ReadResult<Insets> readInsets(ByteBuffer buffer, SeekableByteChannel channel) throws IOException {
			ReadResult<Byte> status = readByte(buffer, channel);
			boolean eof = status.eof;
			if (status.error != null || status.value == null) {
				return new ReadResult<>(null, eof, NULL, false, "Couldn't read insets: " + status.error);
			}
			if ((status.value.byteValue() & NULL) != 0) {
				return new ReadResult<>(null, eof, status.value.byteValue(), false, null);
			}

			ReadResult<Integer> integer = readInteger(buffer, channel);
			eof |= integer.eof;
			if (integer.value == null || integer.error != null) {
				return new ReadResult<>(null, eof, NULL, false, "Couldn't read insets: " + integer.error);
			}
			int top = integer.value.intValue();
			integer = readInteger(buffer, channel);
			eof |= integer.eof;
			if (integer.value == null || integer.error != null) {
				return new ReadResult<>(null, eof, NULL, false, "Couldn't read insets: " + integer.error);
			}
			int left = integer.value.intValue();
			integer = readInteger(buffer, channel);
			eof |= integer.eof;
			if (integer.value == null || integer.error != null) {
				return new ReadResult<>(null, eof, NULL, false, "Couldn't read insets: " + integer.error);
			}
			int bottom = integer.value.intValue();
			integer = readInteger(buffer, channel);
			eof |= integer.eof;
			if (integer.value == null || integer.error != null) {
				return new ReadResult<>(null, eof, NULL, false, "Couldn't read insets: " + integer.error);
			}
			int right = integer.value.intValue();
			return new ReadResult<>(new Insets(top, left, bottom, right), eof, status.value.byteValue());
		}

		private static void writeRectangle(
			@Nonnull ByteArrayOutputStream bytes,
			@Nullable Rectangle rectangle,
			byte flags
		) throws IOException {
			if (rectangle == null) {
				bytes.write(flags | NULL);
				return;
			}

			ByteBuffer buffer = ByteBuffer.allocate(17);
			buffer.order(BYTE_ORDER);
			buffer.put(flags);
			buffer.putInt(rectangle.x);
			buffer.putInt(rectangle.y);
			buffer.putInt(rectangle.width);
			buffer.putInt(rectangle.height);
			bytes.write(buffer.array());
		}

		private static void writeInsets(@Nonnull ByteArrayOutputStream bytes, @Nullable Insets insets) throws IOException {
			if (insets == null) {
				bytes.write(NULL);
				return;
			}
			ByteBuffer buffer = ByteBuffer.allocate(17);
			buffer.order(BYTE_ORDER);
			buffer.put((byte) 0);
			buffer.putInt(insets.top);
			buffer.putInt(insets.left);
			buffer.putInt(insets.bottom);
			buffer.putInt(insets.right);
			bytes.write(buffer.array());
		}

		private static void writeString(@Nonnull ByteArrayOutputStream bytes, @Nullable String string) throws IOException {
			if (string == null) {
				bytes.write(NULL);
				return;
			}
			bytes.write((byte) 0);
			bytes.write(string.getBytes(CHARSET));
			bytes.write(0);
		}

		@SuppressWarnings("unused")
		private static class ReadResult<T> {
			private final T value;
			private final boolean eof;
			private final byte flags;
			private final boolean partial;
			private final String error;

			public ReadResult(T value, boolean eof, byte flags) {
				this.value = value;
				this.eof = eof;
				this.flags = flags;
				this.partial = false;
				this.error = null;
			}

			public ReadResult(T value, boolean eof, byte flags, boolean partial, String error) {
				this.value = value;
				this.eof = eof;
				this.flags = flags;
				this.partial = partial;
				this.error = error;
			}
		}

	}
}
