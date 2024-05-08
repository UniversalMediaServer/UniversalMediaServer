package net.pms.network.mediaserver.jupnp.support.contentdirectory;

import java.util.List;
import net.pms.store.StoreResource;

public class BrowseRequestCallResult {
	public List<StoreResource> resultList;
	public long fromIndex;
	public long toIndex;
	public long totalMatches;
	public long count;
}
