package net.pms.network.message;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Represents payload of `ContentDirectory:1#Browse` request.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Browse")
public class BrowseRequest extends BrowseSearchRequest {

}
