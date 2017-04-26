package net.pms.device;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.model.types.InvalidValueException;
import org.fourthline.cling.support.model.ProtocolInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.dlna.DLNAImageProfile;

/**
 * This class represents a device's {@code ProtocolInfo} elements, typically
 * {@code Source} or {@code Sink} from {@code GetProtocolInfo}.
 */
public class DeviceProtocolInfo {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(DeviceProtocolInfo.class);

	/** The sets lock. */
	private final ReentrantReadWriteLock setsLock = new ReentrantReadWriteLock();

	/** The protocol info set. */
	private final Set<ProtocolInfo> protocolInfoSet = new HashSet<>();

	/** The image profile set. */
	private final Set<DLNAImageProfile> imageProfileSet = new HashSet<>();

	/**
	 * Creates a new empty instance.
	 */
	public DeviceProtocolInfo() {
	}

	/**
	 * Creates a new instance with containing the content from the parsing of
	 * {@code protocolInfoString}.
	 *
	 * @param protocolInfoString a comma separated string of
	 *            {@code ProtocolInfo} representations.
	 */
	public DeviceProtocolInfo(String protocolInfoString) {
		add(protocolInfoString);
	}

	/**
	 * Tries to parse {@code protocolInfoString} and set this to the resulting
	 * {@link ProtocolInfo} instances.
	 *
	 * @param protocolInfoString a comma separated string of
	 *            {@code ProtocolInfo} representations.
	 */
	public void set(String protocolInfoString) {
		setsLock.writeLock().lock();
		try {
			clear();
			add(protocolInfoString);
		} finally {
			setsLock.writeLock().unlock();
		}
	}

	/**
	 * Tries to parse {@code protocolInfoString} and add the resulting
	 * {@link ProtocolInfo} instances.
	 *
	 * @param protocolInfoString a comma separated string of
	 *            {@code ProtocolInfo} representations whose presence is to be
	 *            ensured.
	 * @return {@code true} if this changed as a result of the call. Returns
	 *         {@code false} this already contains the specified element(s).
	 */
	public boolean add(String protocolInfoString) {
		if (StringUtils.isBlank(protocolInfoString)) {
			return false;
		}

		String[] elements = protocolInfoString.split(",");
		boolean result = false;
		setsLock.writeLock().lock();
		try {
			for (String element : elements) {
				try {
					result |= protocolInfoSet.add(new ProtocolInfo(element));
				} catch (InvalidValueException e) {
					LOGGER.warn("Unable to parse ProtocolInfo from \"{}\", this profile will not be registered", e.getMessage());
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
	 * Re-parse {@code protocolInfoSet} and store the results in
	 * {@code imageProfileSet}.
	 */
	protected void updateImageProfiles() {
		setsLock.writeLock().lock();
		try {
			imageProfileSet.clear();
			for (ProtocolInfo protocolInfo : protocolInfoSet) {
				imageProfileSet.addAll(DLNAImageProfile.toDLNAImageProfile(protocolInfo));
			}
		} finally {
			setsLock.writeLock().unlock();
		}
	}

	/*
	 * Standard java.util.Collection methods.
	 */

	/**
	 * Returns the number of elements. If this contains more than
	 * {@link Integer#MAX_VALUE} elements, returns {@link Integer#MAX_VALUE}.
	 *
	 * @return The number of elements.
	 */
	public int size() {
		setsLock.readLock().lock();
		try {
			return protocolInfoSet.size();
		} finally {
			setsLock.readLock().unlock();
		}
	}

	/**
	 * Checks if is empty.
	 *
	 * @return {@code true} if this contains no elements.
	 */
	public boolean isEmpty() {
		setsLock.readLock().lock();
		try {
			return protocolInfoSet.isEmpty();
		} finally {
			setsLock.readLock().unlock();
		}
	}

	/**
	 * Returns {@code true} if this contains the specified element.
	 *
	 * @param protocolInfo the element whose presence is to be tested.
	 * @return {@code true} if this contains the specified element.
	 */
	public boolean contains(ProtocolInfo protocolInfo) {
		setsLock.readLock().lock();
		try {
			return protocolInfoSet.contains(protocolInfo);
		} finally {
			setsLock.readLock().unlock();
		}
	}

	/**
	 * Returns an array containing all of the elements in an unspecified order.
	 * <p>
	 * The returned array will be "safe" in that no references to it are
	 * maintained. (In other words, this method must allocate a new array). The
	 * caller is thus free to modify the returned array.
	 *
	 * @return An array containing all of the elements.
	 */
	public ProtocolInfo[] toArray() {
		setsLock.readLock().lock();
		try {
			return protocolInfoSet.toArray(new ProtocolInfo[protocolInfoSet.size()]);
		} finally {
			setsLock.readLock().unlock();
		}
	}


	/**
	 * Returns {@code true} if this contains all of the elements in the
	 * specified collection.
	 *
	 * @param collection a {@link Collection} to be checked for containment.
	 * @return {@code true} if this collection contains all of the elements in
	 *         {@code collection}.
	 *
	 * @see #contains(ProtocolInfo))
	 */
	public boolean containsAll(Collection<ProtocolInfo> collection) {
		setsLock.readLock().lock();
		try {
			return protocolInfoSet.containsAll(collection);
		} finally {
			setsLock.readLock().unlock();
		}
	}


	/**
	 * Removes all elements.
	 */
	public void clear() {
		setsLock.writeLock().lock();
		try {
			protocolInfoSet.clear();
			imageProfileSet.clear();
		} finally {
			setsLock.writeLock().unlock();
		}
	}

	/**
	 * Ensures that this contains the specified element. Returns {@code true} if
	 * this changed as a result of the call. Returns {@code false} this already
	 * contains the specified element.
	 *
	 * @param protocolInfo element whose presence is to be ensured.
	 * @return {@code true} if this changed as a result of the call.
	 */
	public boolean add(ProtocolInfo protocolInfo) {
		setsLock.writeLock().lock();
		try {
			if (protocolInfoSet.add(protocolInfo)) {
				updateImageProfiles();
				return true;
			}
			return false;
		} finally {
			setsLock.writeLock().unlock();
		}
	}

	/**
	 * Adds all of the elements in the specified collection.
	 *
	 * @param collection a {@link Collection} containing elements to be added.
	 * @return {@code true} if this changed as a result of the call.
	 *
	 * @see #add(ProtocolInfo)
	 */
	public boolean addAll(Collection<? extends ProtocolInfo> collection) {
		setsLock.writeLock().lock();
		try {
			if (protocolInfoSet.addAll(collection)) {
				updateImageProfiles();
				return true;
			}
			return false;
		} finally {
			setsLock.writeLock().unlock();
		}
	}

	/**
	 * Removes a single instance of the {@link ProtocolInfo}, if it is present.
	 * Returns {@code true} if this contained the specified element (or
	 * equivalently, if this changed as a result of the call).
	 *
	 * @param protocolInfo element to be removed, if present.
	 * @return {@code true} if an element was removed as a result of this call.
	 */
	public boolean remove(ProtocolInfo protocolInfo) {
		setsLock.writeLock().lock();
		try {
			if (protocolInfoSet.remove(protocolInfo)) {
				updateImageProfiles();
				return true;
			}
			return false;
		} finally {
			setsLock.writeLock().unlock();
		}
	}

	/**
	 * Removes all elements that are also contained in {@code collection}.
	 *
	 * @param collection a {@link Collection} containing elements to be removed.
	 * @return {@code true} if this call resulted in a change.
	 *
	 * @see #remove(ProtocolInfo))
	 * @see #contains(ProtocolInfo))
	 */
	public boolean removeAll(Collection<ProtocolInfo> collection) {
		setsLock.writeLock().lock();
		try {
			if (protocolInfoSet.removeAll(collection)) {
				updateImageProfiles();
				return true;
			}
			return false;
		} finally {
			setsLock.writeLock().unlock();
		}
	}

	/**
	 * Retains only the elements that are contained in {@code collection}. In
	 * other words, removes all elements that are not contained in
	 * {@code collection}.
	 *
	 * @param collection a {@link Collection} containing elements to be
	 *            retained.
	 * @return {@code true} if this call resulted in a change.
	 *
	 * @see #remove(ProtocolInfo))
	 * @see #contains(ProtocolInfo))
	 */
	public boolean retainAll(Collection<ProtocolInfo> collection) {
		setsLock.writeLock().lock();
		try {
			if (protocolInfoSet.retainAll(collection)) {
				updateImageProfiles();
				return true;
			}
			return false;
		} finally {
			setsLock.writeLock().unlock();
		}
	}

	/*
	 * imageProfileSet "java.util.Collection methods" getters
	 */

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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		setsLock.readLock().lock();
		try {
			return "DeviceProtocolInfo [protocolInfoSet = " + protocolInfoSet + ", imageProfileSet = " + imageProfileSet + "]";
		} finally {
			setsLock.readLock().unlock();
		}
	}
}
