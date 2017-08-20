/*
 * Universal Media Server, for streaming any media to DLNA
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
package net.pms.dlna.protocolinfo;

import static org.apache.commons.lang3.StringUtils.isBlank;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.LookupTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.dlna.DLNAImageProfile;
import net.pms.util.ParseException;

/**
 * This class represents a device's {@code ProtocolInfo} elements, typically
 * {@code Source} or {@code Sink} from {@code GetProtocolInfo}.
 * <p>
 * This class is thread-safe.
 *
 * @author Nadahar
 */
public class DeviceProtocolInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	/** The logger. */
	private static final Logger LOGGER = LoggerFactory.getLogger(DeviceProtocolInfo.class);

	/** The static singleton {@code GetProtocolInfo} {@code Source} identifier */
	public static final DeviceProtocolInfoSource<DeviceProtocolInfo> GET_PROTOCOLINFO_SOURCE = new GetProtocolInfoType() {

		private static final long serialVersionUID = 1L;

		@Override
		public String getType() {
			return "GetProtocolInfo Source";
		}
	};

	/** The static singleton {@code GetProtocolInfo} {@code Sink} identifier */
	public static final DeviceProtocolInfoSource<DeviceProtocolInfo> GET_PROTOCOLINFO_SINK = new GetProtocolInfoType() {

		private static final long serialVersionUID = 1L;

		@Override
		public String getType() {
			return "GetProtocolInfo Sink";
		}
	};

	/**
	 * A regex for splitting a comma separated list of {@code protocolInfo}
	 * entries returned from {@code GetProtocolInfo} while taking DLNA comma
	 * escaping rules into account.
	 */
	public static final String COMMA_SPLIT_REGEX = "\\s*(?:(?<!\\\\),|(?<!\\\\)\\\\\\\\,)\\s*";

	/**
	 * A {@link CharSequenceTranslator} for unescaping individual
	 * {@code GetProtocolInfo} elements.
	 */
	public static final CharSequenceTranslator PROTOCOLINFO_UNESCAPE =
		new LookupTranslator(
			new String[][] {
				{"\\\\", "\\"},
				{"\\,", ","}
			}
		);

	/**
	 * A {@link CharSequenceTranslator} for escaping individual
	 * {@code GetProtocolInfo} elements.
	 */
	public static final CharSequenceTranslator PROTOCOLINFO_ESCAPE =
		new LookupTranslator(
			new String[][] {
				{",", "\\,"},
				{"\\", "\\\\"},
			}
		);

	/** The sets lock. */
	protected final ReentrantReadWriteLock setsLock = new ReentrantReadWriteLock();

	/** The {@link Map} of {@link ProtocolInfo} {@link Set}s. */
	protected final HashMap<DeviceProtocolInfoSource<?>, SortedSet<ProtocolInfo>> protocolInfoSets = new HashMap<>();

	/** The image profile set. */
	protected final SortedSet<DLNAImageProfile> imageProfileSet = new TreeSet<>();

	/**
	 * Creates a new empty instance.
	 */
	public DeviceProtocolInfo() {
	}

	/**
	 * Creates a new instance containing the content from the parsing of
	 * {@code protocolInfoString}.
	 *
	 * @param type The {@link DeviceProtocolInfoSource} of
	 *            {@code protocolInfoString}, must be either
	 *            {@link #GET_PROTOCOLINFO_SINK} or
	 *            {@link #GET_PROTOCOLINFO_SOURCE}.
	 * @param protocolInfoString a comma separated string of
	 *            {@code protocolInfo} representations.
	 */
	public DeviceProtocolInfo(GetProtocolInfoType type, String protocolInfoString) {
		add(type, protocolInfoString);
	}

	/**
	 * Tries to parse {@code protocolInfoString} and add the resulting
	 * {@link ProtocolInfo} instances.
	 *
	 * @param type The {@link DeviceProtocolInfoSource} that identifies the
	 *            source of these {@code protocolInfo}s.
	 * @param protocolInfoString a comma separated string of
	 *            {@code protocolInfo} representations whose presence is to be
	 *            ensured.
	 * @return {@code true} if this changed as a result of the call. Returns
	 *         {@code false} this already contains the specified element(s).
	 */
	public boolean add(DeviceProtocolInfoSource<?> type, String protocolInfoString) {
		if (StringUtils.isBlank(protocolInfoString)) {
			return false;
		}

		String[] elements = protocolInfoString.trim().split(COMMA_SPLIT_REGEX);
		boolean result = false;
		setsLock.writeLock().lock();
		try {
			SortedSet<ProtocolInfo> currentSet;
			if (protocolInfoSets.containsKey(type)) {
				currentSet = protocolInfoSets.get(type);
			} else {
				currentSet = new TreeSet<ProtocolInfo>();
				protocolInfoSets.put(type, currentSet);
			}

			SortedSet<ProtocolInfo> tempSet = null;
			for (String element : elements) {
				try {
					tempSet = handleSpecialCaseString(element);
					if (tempSet == null) {
						// No special handling
						result |= currentSet.add(new ProtocolInfo(unescapeString(element)));
					} else {
						// Add the special handling results
						result |= currentSet.addAll(tempSet);
						tempSet = null;
					}
				} catch (ParseException e) {
					LOGGER.warn(
						"Unable to parse protocolInfo from \"{}\", this profile will not be registered: {}",
						element,
						e.getMessage()
					);
					LOGGER.trace("", e);
				}
			}
			updateImageProfiles();
		} finally {
			setsLock.writeLock().unlock();
		}
		return result;
	}

	/**
	 * Re-parses {@code protocolInfoSet} and stores the results in
	 * {@code imageProfileSet}.
	 */
	protected void updateImageProfiles() {
		setsLock.writeLock().lock();
		try {
			imageProfileSet.clear();
			for (SortedSet<ProtocolInfo> set : protocolInfoSets.values()) {
				for (ProtocolInfo protocolInfo : set) {
					imageProfileSet.addAll(DLNAImageProfile.toDLNAImageProfiles(protocolInfo));
				}
			}
		} finally {
			setsLock.writeLock().unlock();
		}
	}


	// Standard java.util.Collection methods.


	/**
	 * Returns the number of elements of the given
	 * {@link DeviceProtocolInfoSource} type. If this contains more than
	 * {@link Integer#MAX_VALUE} elements, returns {@link Integer#MAX_VALUE}.
	 *
	 * @param type the {@link DeviceProtocolInfoSource} type to get the number
	 *            of elements for.
	 * @return The number of elements in the {@link Set} for {@code type}.
	 */
	public int size(DeviceProtocolInfoSource<?> type) {
		setsLock.readLock().lock();
		try {
			SortedSet<ProtocolInfo> set = protocolInfoSets.get(type);
			return set == null ? 0 : set.size();
		} finally {
			setsLock.readLock().unlock();
		}
	}

	/**
	 * Returns the total number of elements of all
	 * {@link DeviceProtocolInfoSource} types. If the result is greater than
	 * {@link Integer#MAX_VALUE} elements, returns {@link Integer#MAX_VALUE}.
	 *
	 * @return The number of elements.
	 */
	public int size() {
		long result = 0;
		setsLock.readLock().lock();
		try {
			for (SortedSet<ProtocolInfo> set : protocolInfoSets.values()) {
				if (set != null) {
					result += set.size();
				}
			}
		} finally {
			setsLock.readLock().unlock();
		}
		return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
	}

	/**
	 * Checks if the {@link Set} for the given {@link DeviceProtocolInfoSource}
	 * type is empty.
	 *
	 * @param type the {@link DeviceProtocolInfoSource} type to check.
	 * @return {@code true} if {@code protocolInfoSets} contains no elements of
	 *         {@code type}.
	 */
	public boolean isEmpty(DeviceProtocolInfoSource<?> type) {
		setsLock.readLock().lock();
		try {
			SortedSet<ProtocolInfo> set = protocolInfoSets.get(type);
			return set == null ? true : set.isEmpty();
		} finally {
			setsLock.readLock().unlock();
		}
	}

	/**
	 * Checks if all the {@link DeviceProtocolInfoSource} {@link Set}s are
	 * empty.
	 *
	 * @return {@code true} if neither of the {@link DeviceProtocolInfoSource}
	 *         {@link Set}s contain any elements, {@code false} otherwise.
	 */
	public boolean isEmpty() {
		setsLock.readLock().lock();
		try {
			for (SortedSet<ProtocolInfo> set : protocolInfoSets.values()) {
				if (set != null && !set.isEmpty()) {
					return false;
				}
			}
		} finally {
			setsLock.readLock().unlock();
		}
		return true;
	}

	/**
	 * Returns {@code true} if the {@link Set} for the given
	 * {@link DeviceProtocolInfoSource} contains the specified element.
	 *
	 * @param type the {@link DeviceProtocolInfoSource} type to check.
	 * @param protocolInfo the element whose presence is to be tested.
	 * @return {@code true} if the {@link Set} for the given
	 *         {@link DeviceProtocolInfoSource} contains the specified element,
	 *         {@code false} otherwise.
	 */
	public boolean contains(DeviceProtocolInfoSource<?> type, ProtocolInfo protocolInfo) {
		setsLock.readLock().lock();
		try {
			SortedSet<ProtocolInfo> set = protocolInfoSets.get(type);
			return set == null ? false : set.contains(protocolInfo);
		} finally {
			setsLock.readLock().unlock();
		}
	}

	/**
	 * Returns {@code true} if any of the {@link DeviceProtocolInfoSource}
	 * {@link Set}s contains the specified element.
	 *
	 * @param protocolInfo the element whose presence is to be tested.
	 * @return {@code true} if any of the {@link DeviceProtocolInfoSource}
	 *         {@link Set}s contains the specified element {@code false}
	 *         otherwise.
	 */
	public boolean contains(ProtocolInfo protocolInfo) {
		setsLock.readLock().lock();
		try {
			for (SortedSet<ProtocolInfo> set : protocolInfoSets.values()) {
				if (set != null && set.contains(protocolInfo)) {
					return true;
				}
			}
		} finally {
			setsLock.readLock().unlock();
		}
		return false;
	}

	/**
	 * Returns a sorted array containing all of the elements in the {@link Set}
	 * for the given {@link DeviceProtocolInfoSource}.
	 * <p>
	 * The returned array will be "safe" in that no reference to it is
	 * maintained. (In other words, this method must allocate a new array). The
	 * caller is thus free to modify the returned array.
	 *
	 * @param type the {@link DeviceProtocolInfoSource} type whose elements to
	 *            convert to an {@code array}.
	 * @return An array containing all the {@link ProtocolInfo} instances in the
	 *         {@link Set} for {@code type}.
	 */
	public ProtocolInfo[] toArray(DeviceProtocolInfoSource<?> type) {
		setsLock.readLock().lock();
		try {
			SortedSet<ProtocolInfo> set = protocolInfoSets.get(type);
			return set == null ? null : set.toArray(new ProtocolInfo[protocolInfoSets.size()]);
		} finally {
			setsLock.readLock().unlock();
		}
	}

	/**
	 * Returns a sorted array containing all of the elements for all of the
	 * {@link DeviceProtocolInfoSource} {@link Set}s.
	 * <p>
	 * The returned array will be "safe" in that no reference to it is
	 * maintained. (In other words, this method must allocate a new array). The
	 * caller is thus free to modify the returned array.
	 *
	 * @return An array containing all the {@link ProtocolInfo} instances.
	 */
	public ProtocolInfo[] toArray() {
		SortedSet<ProtocolInfo> result = new TreeSet<>();
		setsLock.readLock().lock();
		try {
			for (SortedSet<ProtocolInfo> set : protocolInfoSets.values()) {
				if (set != null) {
					result.addAll(set);
				}
			}
		} finally {
			setsLock.readLock().unlock();
		}
		return result.toArray(new ProtocolInfo[result.size()]);
	}

	/**
	 * Returns {@code true} if the {@link Set} for the given
	 * {@link DeviceProtocolInfoSource} contains all of the elements in the
	 * specified collection.
	 *
	 * @param type the {@link DeviceProtocolInfoSource} type to check.
	 * @param collection a {@link Collection} to be checked for containment.
	 * @return {@code true} if {@link Set} for {@code type} contains all of the
	 *         elements in {@code collection}.
	 *
	 * @see #contains(DeviceProtocolInfoSource, ProtocolInfo)
	 * @see #contains(ProtocolInfo)
	 * @see #containsAll(Collection)
	 */

	public boolean containsAll(DeviceProtocolInfoSource<?> type, Collection<ProtocolInfo> collection) {
		setsLock.readLock().lock();
		try {
			SortedSet<ProtocolInfo> set = protocolInfoSets.get(type);
			return set == null ? false : set.containsAll(collection);
		} finally {
			setsLock.readLock().unlock();
		}
	}

	/**
	 * Returns {@code true} if any of the {@link DeviceProtocolInfoSource}
	 * {@link Set}s contains the elements in the specified collection.
	 *
	 * @param collection a {@link Collection} to be checked for containment.
	 * @return {@code true} if any of the {@link DeviceProtocolInfoSource}
	 *         {@link Set}s contains the elements in {@code collection}.
	 *
	 * @see #contains(DeviceProtocolInfoSource, ProtocolInfo)
	 * @see #contains(ProtocolInfo)
	 * @see #containsAll(DeviceProtocolInfoSource, Collection)
	 */
	public boolean containsAll(Collection<ProtocolInfo> collection) {
		setsLock.readLock().lock();
		try {
			for (ProtocolInfo protocolInfo : collection) {
				if (!contains(protocolInfo)) {
					return false;
				}
			}
		} finally {
			setsLock.readLock().unlock();
		}
		return true;
	}

	/**
	 * Removes all elements in the {@link Set} for the given
	 * {@link DeviceProtocolInfoSource}.
	 *
	 * @param type The {@link DeviceProtocolInfoSource} type to clear.
	 */
	public void clear(DeviceProtocolInfoSource<?> type) {
		setsLock.writeLock().lock();
		try {
			SortedSet<ProtocolInfo> set = protocolInfoSets.get(type);
			if (set != null) {
				set.clear();
				updateImageProfiles();
			}
		} finally {
			setsLock.writeLock().unlock();
		}
	}

	/**
	 * Removes all elements in all of the {@link DeviceProtocolInfoSource}
	 * {@link Set}s.
	 */
	public void clear() {
		setsLock.writeLock().lock();
		try {
			protocolInfoSets.clear();
			imageProfileSet.clear();
		} finally {
			setsLock.writeLock().unlock();
		}
	}

	/**
	 * Ensures that the {@link Set} for the given
	 * {@link DeviceProtocolInfoSource} contains the specified element. Returns
	 * {@code true} if {@code protocolInfo} was added a result of the call, or
	 * {@code false} the {@link Set} for the given
	 * {@link DeviceProtocolInfoSource} already contains the specified element.
	 *
	 * @param type the {@link DeviceProtocolInfoSource} type.
	 * @param protocolInfo element whose presence is to be ensured.
	 * @return {@code true} if the {@link Set} for {@code type} changed as a
	 *         result of the call, {@code false} otherwise.
	 */
	public boolean add(DeviceProtocolInfoSource<?> type, ProtocolInfo protocolInfo) {
		setsLock.writeLock().lock();
		try {
			SortedSet<ProtocolInfo> currentSet;
			if (protocolInfoSets.containsKey(type)) {
				currentSet = protocolInfoSets.get(type);
			} else {
				currentSet = new TreeSet<ProtocolInfo>();
				protocolInfoSets.put(type, currentSet);
			}

			if (currentSet.add(protocolInfo)) {
				updateImageProfiles();
				return true;
			}
			return false;
		} finally {
			setsLock.writeLock().unlock();
		}
	}

	/**
	 * Ensures that the {@link Set} for the given
	 * {@link DeviceProtocolInfoSource} contains all of the elements in the
	 * specified collection.
	 *
	 * @param type the {@link DeviceProtocolInfoSource} type.
	 * @param collection a {@link Collection} containing elements to be added.
	 * @return {@code true} if the {@link Set} for {@code type} changed as a
	 *         result of the call, {@code false} otherwise.
	 *
	 * @see #add(DeviceProtocolInfoSource, ProtocolInfo)
	 */
	public boolean addAll(DeviceProtocolInfoSource<?> type, Collection<? extends ProtocolInfo> collection) {
		setsLock.writeLock().lock();
		try {
			SortedSet<ProtocolInfo> currentSet;
			if (protocolInfoSets.containsKey(type)) {
				currentSet = protocolInfoSets.get(type);
			} else {
				currentSet = new TreeSet<ProtocolInfo>();
				protocolInfoSets.put(type, currentSet);
			}

			if (currentSet.addAll(collection)) {
				updateImageProfiles();
				return true;
			}
			return false;
		} finally {
			setsLock.writeLock().unlock();
		}
	}

	/**
	 * Removes a given instance of {@link ProtocolInfo}, if it is present in the
	 * {@link Set} for the given {@link DeviceProtocolInfoSource}. Returns
	 * {@code true} if the {@link Set} for the given
	 * {@link DeviceProtocolInfoSource} contained the specified element (or
	 * equivalently, if the {@link Set} for the given
	 * {@link DeviceProtocolInfoSource} changed as a result of the call).
	 *
	 * @param type the {@link DeviceProtocolInfoSource} type.
	 * @param protocolInfo element to be removed, if present.
	 * @return {@code true} if an element was removed from the {@link Set} for
	 *         {@code type} as a result of this call, {@code false} otherwise.
	 */
	public boolean remove(DeviceProtocolInfoSource<?> type, ProtocolInfo protocolInfo) {
		setsLock.writeLock().lock();
		try {
			SortedSet<ProtocolInfo> set = protocolInfoSets.get(type);
			if (set != null) {
				if (set.remove(protocolInfo)) {
					updateImageProfiles();
					return true;
				}
			}
			return false;
		} finally {
			setsLock.writeLock().unlock();
		}
	}

	/**
	 * Removes all instances of {@code protocolInfo} from all the
	 * {@link DeviceProtocolInfoSource} {@link Set}s, if it is present. Returns
	 * {@code true} if any of the {@link DeviceProtocolInfoSource} {@link Set}s
	 * contained the specified element (or equivalently, if any of the
	 * {@link DeviceProtocolInfoSource} {@link Set}s changed as a result of the
	 * call).
	 *
	 * @param protocolInfo element to be removed, if present.
	 * @return {@code true} if an element was removed as a result of this call,
	 *         {@code false} otherwise.
	 */
	public boolean remove(ProtocolInfo protocolInfo) {
		boolean result = false;
		setsLock.writeLock().lock();
		try {
			for (SortedSet<ProtocolInfo> set : protocolInfoSets.values()) {
				result |= set != null && set.remove(protocolInfo);
			}
			if (result) {
				updateImageProfiles();
			}
		} finally {
			setsLock.writeLock().unlock();
		}
		return result;
	}

	/**
	 * Removes all elements from the {@link Set} for the given
	 * {@link DeviceProtocolInfoSource} that are also contained in
	 * {@code collection}.
	 *
	 * @param type the {@link DeviceProtocolInfoSource} type.
	 * @param collection a {@link Collection} containing the elements to be
	 *            removed.
	 * @return {@code true} if this call resulted in a change.
	 *
	 * @see #remove(DeviceProtocolInfoSource, ProtocolInfo)
	 * @see #remove(ProtocolInfo)
	 * @see #contains(DeviceProtocolInfoSource, ProtocolInfo)
	 * @see #contains(ProtocolInfo)
	 * @see #removeAll(Collection)
	 */
	public boolean removeAll(DeviceProtocolInfoSource<?> type, Collection<ProtocolInfo> collection) {
		setsLock.writeLock().lock();
		try {
			SortedSet<ProtocolInfo> set = protocolInfoSets.get(type);
			if (set != null && set.removeAll(collection)) {
				updateImageProfiles();
				return true;
			}
			return false;
		} finally {
			setsLock.writeLock().unlock();
		}
	}

	/**
	 * Removes all elements from all the {@link DeviceProtocolInfoSource}
	 * {@link Set}s that are also contained in {@code collection}.
	 *
	 * @param collection a {@link Collection} containing the elements to be
	 *            removed.
	 * @return {@code true} if this call resulted in a change.
	 *
	 * @see #remove(DeviceProtocolInfoSource, ProtocolInfo)
	 * @see #remove(ProtocolInfo)
	 * @see #contains(DeviceProtocolInfoSource, ProtocolInfo)
	 * @see #contains(ProtocolInfo)
	 * @see #removeAll(DeviceProtocolInfoSource, Collection)
	 */
	public boolean removeAll(Collection<ProtocolInfo> collection) {
		boolean result = false;
		setsLock.writeLock().lock();
		try {
			for (SortedSet<ProtocolInfo> set : protocolInfoSets.values()) {
				result |= set != null && set.removeAll(collection);
			}
			if (result) {
				updateImageProfiles();
			}
		} finally {
			setsLock.writeLock().unlock();
		}
		return result;
	}

	/**
	 * Retains only the elements that are contained in {@code collection} from
	 * the {@link Set} for the given {@link DeviceProtocolInfoSource}. In other
	 * words, removes all elements that are not contained in {@code collection}
	 * from the {@link Set} for the given {@link DeviceProtocolInfoSource}.
	 *
	 * @param type the {@link DeviceProtocolInfoSource} type.
	 * @param collection a {@link Collection} containing elements to be
	 *            retained.
	 * @return {@code true} if this call resulted in a change.
	 *
	 * @see #remove(ProtocolInfo)
	 * @see #remove(DeviceProtocolInfoSource, ProtocolInfo)
	 * @see #contains(ProtocolInfo)
	 * @see #contains(DeviceProtocolInfoSource, ProtocolInfo)
	 * @see #retainAll(Collection)
	 */
	public boolean retainAll(DeviceProtocolInfoSource<?> type, Collection<ProtocolInfo> collection) {
		setsLock.writeLock().lock();
		try {
			SortedSet<ProtocolInfo> set = protocolInfoSets.get(type);
			if (set != null && set.retainAll(collection)) {
				updateImageProfiles();
				return true;
			}
			return false;
		} finally {
			setsLock.writeLock().unlock();
		}
	}

	/**
	 * Retains only the elements that are contained in {@code collection} in all
	 * the {@link DeviceProtocolInfoSource} {@link Set}s. In other words,
	 * removes all elements from all the {@link DeviceProtocolInfoSource}
	 * {@link Set}s that are not contained in {@code collection}.
	 *
	 * @param collection a {@link Collection} containing elements to be
	 *            retained.
	 * @return {@code true} if this call resulted in a change.
	 *
	 * @see #remove(ProtocolInfo)
	 * @see #remove(DeviceProtocolInfoSource, ProtocolInfo)
	 * @see #contains(ProtocolInfo)
	 * @see #contains(DeviceProtocolInfoSource, ProtocolInfo)
	 * @see #retainAll(DeviceProtocolInfoSource, Collection)
	 */
	public boolean retainAll(Collection<ProtocolInfo> collection) {
		boolean result = false;
		setsLock.writeLock().lock();
		try {
			for (SortedSet<ProtocolInfo> set : protocolInfoSets.values()) {
				result |= set != null && set.retainAll(collection);
			}
			if (result) {
				updateImageProfiles();
			}
		} finally {
			setsLock.writeLock().unlock();
		}
		return result;
	}


	// imageProfileSet "java.util.Collection methods" getters


	/**
	 * Returns the number of {@link DLNAImageProfile} elements. If this contains
	 * more than {@link Integer#MAX_VALUE} elements, returns
	 * {@link Integer#MAX_VALUE}.
	 *
	 * @return The number of {@link DLNAImageProfile} elements.
	 */
	public int imageProfilesSize() {
		setsLock.readLock().lock();
		try {
			return imageProfileSet.size();
		} finally {
			setsLock.readLock().unlock();
		}
	}

	/**
	 * Checks if is image profiles empty.
	 *
	 * @return {@code true} if this contains no {@link DLNAImageProfile}
	 *         elements.
	 */
	public boolean isImageProfilesEmpty() {
		setsLock.readLock().lock();
		try {
			return imageProfileSet.isEmpty();
		} finally {
			setsLock.readLock().unlock();
		}
	}

	/**
	 * Returns {@code true} if this contains the specified
	 * {@link DLNAImageProfile} instance.
	 *
	 * @param imageProfile the {@link DLNAImageProfile} instance whose presence
	 *            is to be tested.
	 * @return {@code true} if this contains the specified
	 *         {@link DLNAImageProfile} instance.
	 */
	public boolean imageProfilesContains(DLNAImageProfile imageProfile) {
		setsLock.readLock().lock();
		try {
			return imageProfileSet.contains(imageProfile);
		} finally {
			setsLock.readLock().unlock();
		}
	}

	/**
	 * Returns an array containing all the {@link DLNAImageProfile} instances in
	 * an unspecified order.
	 * <p>
	 * The returned array will be "safe" in that no references to it are
	 * maintained. (In other words, this method must allocate a new array). The
	 * caller is thus free to modify the returned array.
	 *
	 * @return An array containing all the {@link DLNAImageProfile} instances.
	 */
	public DLNAImageProfile[] imageProfilesToArray() {
		setsLock.readLock().lock();
		try {
			return imageProfileSet.toArray(new DLNAImageProfile[imageProfileSet.size()]);
		} finally {
			setsLock.readLock().unlock();
		}
	}


	/**
	 * Returns {@code true} if this contains all the {@link DLNAImageProfile} instances in the
	 * specified collection.
	 *
	 * @param collection a {@link Collection} to be checked for containment.
	 * @return {@code true} if this collection contains all of the {@link DLNAImageProfile} instances in
	 *         {@code collection}.
	 *
	 * @see #imageProfilesContains(DLNAImageProfile)
	 */
	public boolean imageProfilesContainsAll(Collection<DLNAImageProfile> collection) {
		setsLock.readLock().lock();
		try {
			return imageProfileSet.containsAll(collection);
		} finally {
			setsLock.readLock().unlock();
		}
	}

	@Override
	public String toString() {
		return toString(null, false);
	}

	/**
	 * Returns a string representation of this {@link DeviceProtocolInfo}
	 * instance showing only the {@link ProtocolInfo} instances of the given
	 * {@link DeviceProtocolInfoSource} type. If {@code debug} is {@code true},
	 * verbose output is returned.
	 *
	 * @param type the {@link DeviceProtocolInfoSource} type to include.
	 * @return A string representation of this {@link DeviceProtocolInfo}.
	 */
	public String toString(DeviceProtocolInfoSource<?> type) {
		return toString(type, false);
	}

	/**
	 * Returns a string representation of this {@link DeviceProtocolInfo}
	 * instance. If {@code debug} is {@code true}, verbose output is returned.
	 *
	 * @param debug whether or not verbose output should be generated.
	 * @return A string representation of this {@link DeviceProtocolInfo}.
	 */
	public String toString(boolean debug) {
		return toString(null, debug);
	}

	/**
	 * Returns a string representation of this {@link DeviceProtocolInfo}
	 * instance showing only the {@link ProtocolInfo} instances of the given
	 * {@link DeviceProtocolInfoSource} type. If {@code debug} is {@code true},
	 * verbose output is returned.
	 *
	 * @param type the {@link DeviceProtocolInfoSource} type to include. Use
	 *            {@code null} for all types.
	 * @param debug whether or not verbose output should be generated.
	 * @return A string representation of this {@link DeviceProtocolInfo}.
	 */
	public String toString(DeviceProtocolInfoSource<?> type, boolean debug) {
		StringBuilder sb = new StringBuilder();
		setsLock.readLock().lock();
		try {
			if (protocolInfoSets != null && !protocolInfoSets.isEmpty()) {
				for (Entry<DeviceProtocolInfoSource<?>, SortedSet<ProtocolInfo>>  entry : protocolInfoSets.entrySet()) {
					if (type == null || type.equals(entry.getKey())) {
						if (!entry.getValue().isEmpty()) {
							sb.append(entry.getKey().getType()).append(" entries:\n");
							for (ProtocolInfo protocolInfo : entry.getValue()) {
								if (protocolInfo != null) {
									sb.append("  ").append(debug ?
										protocolInfo.toDebugString() :
										protocolInfo.toString()
									).append("\n");
								}
							}
							sb.append("\n");
						}
					}
				}
			}
			if (imageProfileSet != null && !imageProfileSet.isEmpty()) {
				sb.append("DLNAImageProfile entries:\n");
				for (DLNAImageProfile imageProfile : imageProfileSet) {
					if (imageProfile != null) {
						sb.append("  ").append(imageProfile).append("\n");
					}
				}
			}
		} finally {
			setsLock.readLock().unlock();
		}
		return sb.toString();
	}


	// Static methods


	/**
	 * Escapes {@code protocolInfo} strings for use in {@code GetProtocolInfo}
	 * in accordance with DLNA comma escaping rules.
	 *
	 * @param unescapedString the {@code protocolInfo} string to escape.
	 * @return The escaped {@link String};
	 */
	public static String escapeString(String unescapedString) {
		return PROTOCOLINFO_ESCAPE.translate(unescapedString);
	}

	/**
	 * Unescapes {@code protocolInfo} strings after splitting a string from
	 * {@code GetProtocolInfo} into individual elements in accordance with DLNA
	 * comma escaping rules.
	 *
	 * @param escapedString the {@code protocolInfo} string to unescape.
	 * @return The unescaped {@link String};
	 */
	public static String unescapeString(String escapedString) {
		return PROTOCOLINFO_UNESCAPE.translate(escapedString);
	}

	/**
	 * Handles known special cases, i.e bugs in renderers' {@code protocolInfo}
	 * output so that we are able to parse them despite them being broken.
	 *
	 * @param element the {@code protocolInfo} element to handle if needed.
	 * @return {@code null} if {@code element} doesn't match a known special
	 *         case, or a {@link SortedSet} of {@link ProtocolInfo} instances
	 *         with the result of the parsed special case.
	 * @throws ParseException If {@code element} is a known special case but the
	 *             parsing fails.
	 */
	public static SortedSet<ProtocolInfo> handleSpecialCaseString(String element) throws ParseException {
		if (isBlank(element)) {
			return null;
		}
		switch (element) {
			/*
			 * Seen on a LG-BP550-1, missing comma between elements
			 */
			case "http-get:*:audio/sonyoma:*http-get:*:audio/ogg:*":
				SortedSet<ProtocolInfo> currentSet = new TreeSet<>();
				currentSet.add(new ProtocolInfo("http-get:*:audio/sonyoma:*"));
				currentSet.add(new ProtocolInfo("http-get:*:audio/ogg:*"));
				return currentSet;
		}
		return null;
	}

	/**
	 * This is an abstract implementation of {@link DeviceProtocolInfoSource}
	 * where {@link DeviceProtocolInfo} is the parsing class.
	 *
	 * @author Nadahar
	 */
	public abstract static class GetProtocolInfoType extends DeviceProtocolInfoSource<DeviceProtocolInfo> {

		private static final long serialVersionUID = 1L;

		@Override
		public Class<DeviceProtocolInfo> getClazz() {
			return DeviceProtocolInfo.class;
		}
	}
}
