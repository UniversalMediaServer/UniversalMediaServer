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
package net.pms.platform.macos;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.sun.jna.DefaultTypeMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import net.pms.util.jna.JnaLongEnum;
import net.pms.util.jna.JnaLongEnumConverter;
import net.pms.util.jna.macos.corefoundation.CoreFoundation;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFArrayRef;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFStringRef;
import net.pms.util.jna.macos.corefoundation.CoreFoundation.CFTypeRef;

/**
 * Partial mapping of Apple's NS (NextStep)/Cocoa Foundation framework.
 * <p>
 * This class is an utility class for the internal {@link NSFoundationIF} that
 * converts the results from ARC managed Objective-C objects to Java objects and
 * releases the native objects.
 *
 * The mappings are from
 * <ul>
 * <li>NSObject.h</li>
 * <li>NSPathUtilities.h</li>
 * </ul>
 *
 * @author Nadahar
 */
@SuppressWarnings({
	"checkstyle:MethodName",
	"checkstyle:LineLength"
})
public class NSFoundation {

	/**
	 * Not to be instantiated.
	 */
	private NSFoundation() {
	}

	/**
	 * @return The login name of the current user.
	 */
	@Nonnull
	public static String nsUserName() {
		CFStringRef cfStringRef = new CFStringRef(
			NSFoundationIF.INSTANCE.CFBridgingRetain(NSFoundationIF.INSTANCE.NSUserName())
		);
		try {
			return cfStringRef.toString();
		} finally {
			CoreFoundation.INSTANCE.CFRelease(cfStringRef);
		}
	}

	/**
	 * @return The full name of the current user.
	 */
	@Nonnull
	public static String nsFullUserName() {
		CFStringRef cfStringRef = new CFStringRef(
			NSFoundationIF.INSTANCE.CFBridgingRetain(NSFoundationIF.INSTANCE.NSFullUserName())
		);
		try {
			return cfStringRef.toString();
		} finally {
			CoreFoundation.INSTANCE.CFRelease(cfStringRef);
		}
	}

	/**
	 * Returns the path to either the user’s or application’s home directory,
	 * depending on the platform.
	 * <p>
	 * In iOS, the home directory is the application’s sandbox directory. In
	 * macOS, it is the application’s sandbox directory or the current user’s
	 * home directory (if the application is not in a sandbox).
	 *
	 * For more information on file-system utilities, see Low-Level File
	 * Management Programming Topics.
	 *
	 * @return The {@link Path} to the home directory.
	 */
	@Nonnull
	public static Path nsHomeDirectory() {
		CFStringRef cfStringRef = new CFStringRef(
			NSFoundationIF.INSTANCE.CFBridgingRetain(NSFoundationIF.INSTANCE.NSHomeDirectory())
		);
		try {
			return Paths.get(cfStringRef.toString());
		} finally {
			CoreFoundation.INSTANCE.CFRelease(cfStringRef);
		}
	}

	/**
	 * Returns the path to a given user’s home directory.
	 * <p>
	 * For more information on file system utilities, see Low-Level File
	 * Management Programming Topics.
	 *
	 * @param userName the user name whose home directory to return.
	 * @return The {@link Path} to the home directory for the user specified by
	 *         {@code userName}.
	 */
	@Nullable
	public static Path nsHomeDirectoryForUser(@Nullable String userName) {
		Pointer nsUserName = userName == null ?
			Pointer.NULL :
			NSFoundationIF.INSTANCE.CFBridgingRelease(CFStringRef.toCFStringRef(userName));
		Pointer nsString = NSFoundationIF.INSTANCE.NSHomeDirectoryForUser(nsUserName);
		if (nsString == null) {
			return null;
		}
		CFStringRef cfStringRef = new CFStringRef(NSFoundationIF.INSTANCE.CFBridgingRetain(nsString));
		try {
			return Paths.get(cfStringRef.toString());
		} finally {
			CoreFoundation.INSTANCE.CFRelease(cfStringRef);
		}
	}

	/**
	 * Returns the path of the temporary directory for the current user.
	 *
	 * For more information about temporary files, see <a href=
	 * "https://developer.apple.com/library/content/documentation/FileManagement/Conceptual/FileSystemProgrammingGuide/Introduction/Introduction.html#//apple_ref/doc/uid/TP40010672"
	 * >File System Programming Guide</a>.
	 *
	 * @return The {@link Path} of the temporary directory for the current user.
	 */
	@Nonnull
	public static Path nsTemporaryDirectory() {
		CFStringRef cfStringRef = new CFStringRef(
			NSFoundationIF.INSTANCE.CFBridgingRetain(NSFoundationIF.INSTANCE.NSTemporaryDirectory())
		);
		try {
			return Paths.get(cfStringRef.toString());
		} finally {
			CoreFoundation.INSTANCE.CFRelease(cfStringRef);
		}
	}

	/**
	 * Returns the root directory of the user’s system.
	 * <p>
	 * For more information on file system utilities, see Low-Level File
	 * Management Programming Topics.
	 *
	 * @return The {@link Path} of the root directory of the user’s system.
	 */
	@Nonnull
	public static Path nsOpenStepRootDirectory() {
		CFStringRef cfStringRef = new CFStringRef(
			NSFoundationIF.INSTANCE.CFBridgingRetain(NSFoundationIF.INSTANCE.NSOpenStepRootDirectory())
		);
		try {
			return Paths.get(cfStringRef.toString());
		} finally {
			CoreFoundation.INSTANCE.CFRelease(cfStringRef);
		}
	}

	/**
	 * Creates a list of directory search paths.
	 * <p>
	 * Creates a list of path strings for the specified directories in the
	 * specified domains. The list is in the order in which you should search
	 * the directories.
	 *
	 * For more information on file system utilities, see <a href=
	 * "https://developer.apple.com/library/content/documentation/FileManagement/Conceptual/FileSystemProgrammingGuide/Introduction/Introduction.html#//apple_ref/doc/uid/TP40010672"
	 * >File System Programming Guide</a>.
	 * <p>
	 * <b>Note</b>: The directory returned by this method may not exist. This
	 * method simply gives you the appropriate location for the requested
	 * directory. Depending on the application’s needs, it may be up to the
	 * developer to create the appropriate directory and any in between.
	 *
	 * @param directory the {@link NSSearchPathDirectory} to find.
	 * @param domainMask zero or more {@link NSSearchPathDomainMask} constants
	 *            combined.
	 * @param expandTilde {@code true} if prefixed {@code "~"} or
	 *            {@code "~user"} should be expanded to its full path value,
	 *            {@code false} otherwise.
	 * @return The {@link ArrayList} of {@link Path}s of folders.
	 */
	@Nonnull
	public static ArrayList<Path> nsSearchPathForDirectoriesInDomains(
		NSSearchPathDirectory directory,
		long domainMask,
		boolean expandTilde
	) {
		ArrayList<Path> result = new ArrayList<>();
		CFArrayRef cfArrayRef = new CFArrayRef(
			NSFoundationIF.INSTANCE.CFBridgingRetain(NSFoundationIF.INSTANCE.NSSearchPathForDirectoriesInDomains(
				directory,
				domainMask,
				expandTilde
			))
		);
		try {
			long count = CoreFoundation.INSTANCE.CFArrayGetCount(cfArrayRef);
			for (int i = 0; i < count; i++) {
				CFTypeRef nsPathString =  CoreFoundation.INSTANCE.CFArrayGetValueAtIndex(cfArrayRef, i);
				if (nsPathString != null) {
					CFStringRef cfStringRef = new CFStringRef(NSFoundationIF.INSTANCE.CFBridgingRetain(nsPathString.getPointer()));
					result.add(Paths.get(cfStringRef.toString()));
					CoreFoundation.INSTANCE.CFRelease(cfStringRef);
				}
			}
			return result;
		} finally {
			CoreFoundation.INSTANCE.CFRelease(cfArrayRef);
		}
	}

	/**
	 * Partial mapping of Apple's NS (NextStep)/Cocoa Foundation framework. This
	 * interface is protected to avoid direct use because it's unclear how to
	 * release "NS" return types from JNA. Use the corresponding methods in
	 * {@link NSFoundation} instead, which will return {@link CFTypeRef} types.
	 * {@link CFTypeRef} types are released with
	 * {@link CoreFoundation#CFRelease} after use.
	 * <p>
	 * The mappings are from:
	 * <ul>
	 * <li>NSObject.h</li>
	 * <li>NSPathUtilities.h</li>
	 * </ul>
	 *
	 * @author Nadahar
	 */
	protected interface NSFoundationIF extends Library {

		/**
		 * A {@link Map} of library options for use with {@link Native#loadLibrary}.
		 */
		Map<String, Object> OPTIONS = Collections.unmodifiableMap(new HashMap<String, Object>() {

			private static final long serialVersionUID = 1L;
			{
				put(Library.OPTION_TYPE_MAPPER, new DefaultTypeMapper() {
					{
						addTypeConverter(JnaLongEnum.class, new JnaLongEnumConverter());
					}
				});
			}
		});

		/**
		 * The static {@link NSFoundationIF} instance.
		 */
		NSFoundationIF INSTANCE = Native.loadLibrary(
			"/System/Library/Frameworks/Foundation.framework/Resources/BridgeSupport/Foundation.dylib",
			NSFoundationIF.class,
			OPTIONS
		);


		// NSObject.h methods


		/**
		 * Casts an Objective-C pointer to a {@link CoreFoundation} pointer and also
		 * transfers ownership to the caller.
		 * <p>
		 * References are owned after using this function and must be released with
		 * {@link CoreFoundation#CFRelease} to avoid leaking references.
		 * <p>
		 * You use this function to cast an Objective-C object as Core
		 * Foundation-style object and take ownership of the object so that you can
		 * manage its lifetime. You are responsible for subsequently releasing the
		 * object with {@link CoreFoundation#CFRelease(CFTypeRef)}.
		 * <p>
		 * <b>Note:</b> The returned type is of a {@link CoreFoundation} type
		 * corresponding to the NS type. However, Java doesn't know what type that
		 * is and thus it is mapped to a {@link CFTypeRef}. Because this instance is
		 * created during type mapping, it isn't possible to cast to it's real type
		 * from Java's point of view. To do this cast, a new instance of the correct
		 * type must be created using the pointer for the returned type, for
		 * example:
		 *
		 * <pre>
		 * <code>
		 * CFStringRef cfStringRef = new CFStringRef(CFBridgingRetain(nsString));
		 * try {
		 *   ...
		 * } finally {
		 *     CoreFoundation.INSTANCE.CFRelease(cfStringRef);
		 * }
		 * </code>
		 * </pre>
		 *
		 * @param x the Objective-C (NS) {@link Pointer} to cast.
		 * @return the {@link CFTypeRef} of the corresponding type.
		 */
		@Nullable
		CFTypeRef CFBridgingRetain(@Nullable Pointer x);

		/**
		 * Moves a {@link CoreFoundation} pointer to Objective-C and also
		 * transfers ownership to ARC.
		 * <p>
		 * <b>Warning: It is unknown if or how the ARC memory management works
		 * from JNA, and since there are no explicit way to release these
		 * objects this method should be used with the utmost care.</b>
		 * <p>
		 * The {@link CFTypeRef} object passed is no longer manually managed and
		 * should <b>NOT</b> be released with {@link CoreFoundation#CFRelease}.
		 *
		 * You use this function to cast a Core Foundation-style object as an
		 * Objective-C object and transfer ownership of the object to ARC such
		 * that you don’t have to release the object.
		 *
		 * @param x the {@link CFTypeRef} to cast.
		 * @return the ARC managed Objective-C (NS) {@link Pointer}.
		 */
		@Nullable
		Pointer CFBridgingRelease(@Nullable CFTypeRef x);


		// NSPathUtilities.h methods


		/**
		 * Returns the login name of the current user.
		 *
		 * @return A {@link Pointer} to a {@code NSString} containing the login name
		 *         of the current user. Since the memory management of these is
		 *         handled by ARC and it's unclear how that works from JNA, this
		 *         must immediately be converted to a {@link CFStringRef} by calling
		 *         {@link #CFBridgingRetain}.
		 */
		Pointer NSUserName();

		/**
		 * Returns a string containing the full name of the current user.
		 *
		 * @return A {@link Pointer} to a {@code NSString} containing the full name
		 *         of the current user. Since the memory management of these is
		 *         handled by ARC and it's unclear how that works from JNA, this
		 *         must immediately be converted to a {@link CFStringRef} by calling
		 *         {@link #CFBridgingRetain}.
		 */
		Pointer NSFullUserName();

		/**
		 * Returns the path to either the user’s or application’s home directory,
		 * depending on the platform.
		 * <p>
		 * In iOS, the home directory is the application’s sandbox directory. In
		 * macOS, it is the application’s sandbox directory or the current user’s
		 * home directory (if the application is not in a sandbox).
		 *
		 * For more information on file-system utilities, see Low-Level File
		 * Management Programming Topics.
		 *
		 * @return A {@link Pointer} to a {@code NSString} containing the path to
		 *         the home directory. Since the memory management of these is
		 *         handled by ARC and it's unclear how that works from JNA, this
		 *         must immediately be converted to a {@link CFStringRef} by calling
		 *         {@link #CFBridgingRetain}.
		 */
		Pointer NSHomeDirectory();

		/**
		 * Returns the path to a given user’s home directory.
		 * <p>
		 * For more information on file system utilities, see Low-Level File
		 * Management Programming Topics.
		 *
		 * @param nsUserName the user name as a {@code NSString} whose home
		 *            directory to return.
		 * @return A {@link Pointer} to a {@code NSString} containing the path
		 *         to the home directory for the user specified by
		 *         {@code userName}. Since the memory management of these is
		 *         handled by ARC and it's unclear how that works from JNA, this
		 *         must immediately be converted to a {@link CFStringRef} by
		 *         calling {@link #CFBridgingRetain}.
		 */
		@Nullable
		Pointer NSHomeDirectoryForUser(@Nullable Pointer nsUserName);

		/**
		 * Returns the path of the temporary directory for the current user.
		 *
		 * For more information about temporary files, see <a href=
		 * "https://developer.apple.com/library/content/documentation/FileManagement/Conceptual/FileSystemProgrammingGuide/Introduction/Introduction.html#//apple_ref/doc/uid/TP40010672"
		 * >File System Programming Guide</a>.
		 *
		 * @return A {@link Pointer} to a {@code NSString} containing the path of
		 *         the temporary directory for the current user. Since the memory
		 *         management of these is handled by ARC and it's unclear how that
		 *         works from JNA, this must immediately be converted to a
		 *         {@link CFStringRef} by calling {@link #CFBridgingRetain}.
		 */
		Pointer NSTemporaryDirectory();

		/**
		 * Returns the root directory of the user’s system.
		 * <p>
		 * For more information on file system utilities, see Low-Level File
		 * Management Programming Topics.
		 *
		 * @return A {@link Pointer} to a {@code NSString} containing the root
		 *         directory of the user’s system. Since the memory management of
		 *         these is handled by ARC and it's unclear how that works from JNA,
		 *         this must immediately be converted to a {@link CFStringRef} by
		 *         calling {@link #CFBridgingRetain}.
		 */
		Pointer NSOpenStepRootDirectory();

		/**
		 * Creates a list of directory search paths.
		 * <p>
		 * Creates a list of path strings for the specified directories in the
		 * specified domains. The list is in the order in which you should search
		 * the directories.
		 *
		 * For more information on file system utilities, see <a href=
		 * "https://developer.apple.com/library/content/documentation/FileManagement/Conceptual/FileSystemProgrammingGuide/Introduction/Introduction.html#//apple_ref/doc/uid/TP40010672"
		 * >File System Programming Guide</a>.
		 * <p>
		 * <b>Note</b>: The directory returned by this method may not exist. This
		 * method simply gives you the appropriate location for the requested
		 * directory. Depending on the application’s needs, it may be up to the
		 * developer to create the appropriate directory and any in between.
		 *
		 * @param directory the {@link NSSearchPathDirectory} to find.
		 * @param domainMask zero or more {@link NSSearchPathDomainMask} constants
		 *            combined.
		 * @param expandTilde {@code true} if prefixed {@code "~"} or
		 *            {@code "~user"} should be expanded to its full path value,
		 *            {@code false} otherwise.
		 * @return A {@link Pointer} to a {@code NSArray} of {@code NSString}s.
		 *         Since the memory management of these is handled by ARC and it's
		 *         unclear how that works from JNA, this must immediately be
		 *         converted to a {@link CFArrayRef} by calling
		 *         {@link #CFBridgingRetain}.
		 */
		Pointer NSSearchPathForDirectoriesInDomains(NSSearchPathDirectory directory, long domainMask, boolean expandTilde);
	}

	/**
	 * This represents the {@code NS_ENUM} with the same name.
	 * <p>
	 * Use {@link #getValue()} to convert a {@link NSSearchPathDirectory} to its
	 * integer value. Use {@link #typeOf} to convert an integer value to a
	 * {@link NSSearchPathDirectory}.
	 */
	public static enum NSSearchPathDirectory implements JnaLongEnum<NSSearchPathDirectory> {

		/** Supported applications ({@code Applications}) */
		NSApplicationDirectory(1),

		/** Unsupported applications, demonstration versions ({@code Demos}) */
		NSDemoApplicationDirectory(2),

		/**
		 * Developer applications ({@code Developer/Applications}).
		 *
		 * @deprecated There is no one single Developer directory.
		 */
		NSDeveloperApplicationDirectory(3),

		/** System and network administration applications ({@code Administration}) */
		NSAdminApplicationDirectory(4),

		/**
		 * Various documentation, support, and configuration files, resources (
		 * {@code Library})
		 */
		NSLibraryDirectory(5),

		/**
		 * Developer resources ({@code Developer})
		 *
		 * @deprecated There is no one single Developer directory.
		 */
		NSDeveloperDirectory(6),

		/** User home directories ({@code Users}) */
		NSUserDirectory(7),

		/** Documentation ({@code Documentation}) */
		NSDocumentationDirectory(8),               //

		/** Documents ({@code Documents}) */
		NSDocumentDirectory(9),

		/**
		 * Location of {@code CoreServices} directory (
		 * {@code System/Library/CoreServices})
		 */
		NSCoreServiceDirectory(10),

		/**
		 * Location of autosaved documents ({@code Documents/Autosaved})
		 *
		 * @since OS X 10.6
		 */
		NSAutosavedInformationDirectory(11),

		/** Location of user's desktop */
		NSDesktopDirectory(12),

		/** Location of discardable cache files ({@code Library/Caches}) */
		NSCachesDirectory(13),

		/**
		 * Location of application support files (plug-ins, etc) (
		 * {@code Library/Application Support})
		 */
		NSApplicationSupportDirectory(14),

		/**
		 * Location of the user's "{@code Downloads}" directory
		 *
		 * @since OS X 10.5
		 */
		NSDownloadsDirectory(15),

		/**
		 * Input methods ({@code Library/Input Methods})
		 *
		 * @since OS X 10.6
		 */
		NSInputMethodsDirectory(16),

		/**
		 * Location of user's Movies directory ({@code ~/Movies})
		 *
		 * @since OS X 10.6
		 */
		NSMoviesDirectory(17),

		/**
		 * Location of user's Music directory ({@code ~/Music})
		 *
		 * @since OS X 10.6
		 */
		NSMusicDirectory(18),

		/**
		 * Location of user's Pictures directory ({@code ~/Pictures})
		 *
		 * @since OS X 10.6
		 */
		NSPicturesDirectory(19),

		/**
		 * Location of system's PPDs directory ({@code Library/Printers/PPDs})
		 *
		 * @since OS X 10.6
		 */
		NSPrinterDescriptionDirectory(20),

		/**
		 * Location of user's Public sharing directory ({@code ~/Public})
		 *
		 * @since OS X 10.6
		 */
		NSSharedPublicDirectory(21),

		/**
		 * Location of the PreferencePanes directory for use with System Preferences
		 * ({@code Library/PreferencePanes})
		 *
		 * @since OS X 10.6
		 */
		NSPreferencePanesDirectory(22),

		/**
		 * Location of the user scripts folder for the calling application (
		 * {@code ~/Library/Application Scripts/code-signing-id})
		 *
		 * @since OS X 10.8
		 */
		NSApplicationScriptsDirectory(23),

		/**
		 * For use with {@code NSFileManager}'s
		 * {@code URLForDirectory:inDomain:appropriateForURL:create:error:}
		 *
		 * @since OS X 10.6
		 */
		NSItemReplacementDirectory(99),

		/** All directories where applications can occur */
		NSAllApplicationsDirectory(100),

		/** All directories where resources can occur */
		NSAllLibrariesDirectory(101),

		/**
		 * Location of {@code Trash} directory
		 *
		 * @since OS X 10.8
		 */
		NSTrashDirectory(102);

		private final long value;

		private NSSearchPathDirectory(long value) {
			this.value = value;
		}

		/**
		 * @return The integer value of this {@link Enum}.
		 */
		@Override
		public long getValue() {
			return value;
		}

		/**
		 * Returns the instance corresponding to {@code value} or {@code null}
		 * if there is no corresponding instance.
		 *
		 * @param value the value to lookup.
		 * @return The {@link NSSearchPathDirectory} instance or {@code null}.
		 */
		public static NSSearchPathDirectory typeOf(long value) {
			for (NSSearchPathDirectory entry : NSSearchPathDirectory.values()) {
				if (value == entry.getValue()) {
					return entry;
				}
			}
			return null;
		}

		@Override
		public NSSearchPathDirectory typeForValue(long value) {
			return typeOf(value);
		}
	}

	/**
	 * These constants represents the {@code NS_OPTIONS} with the same name.
	 *
	 * @see #NSUserDomainMask
	 * @see #NSLocalDomainMask
	 * @see #NSNetworkDomainMask
	 * @see #NSSystemDomainMask
	 * @see #NSAllDomainsMask
	 */
	@SuppressWarnings("checkstyle:ConstantName")
	public static class NSSearchPathDomainMask {

		/** Not to be instantiated. */
		private NSSearchPathDomainMask() {
		}

		/**
		 * User's home directory --- place to install user's personal items
		 * ({@code ~})
		 */
		public static final long NSUserDomainMask = 1L;

		/**
		 * Local to the current machine --- place to install items available to
		 * everyone on this machine ({@code /Library})
		 */
		public static final long NSLocalDomainMask = 2L;

		/**
		 * Publicly available location in the local area network --- place to
		 * install items available on the network ({@code /Network})
		 */
		public static final long NSNetworkDomainMask = 4L;

		/** Provided by Apple, unmodifiable ({@code /System}) */
		public static final long NSSystemDomainMask = 8L;

		/** All domains: all of the above and future items */
		public static final long NSAllDomainsMask = 0x0ffffL;
	}
}
