package net.pms.network.mediaserver.handlers.message;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Represents payload of `ContentDirectory:1#Search` request.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Search")
public class SearchRequest extends BrowseSearchRequest {

}
