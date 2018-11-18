package net.pms.network.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * Represents payload of `ContentDirectory:1#Search` request.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Search")
public class SearchRequest extends BrowseSearchRequest {

}
