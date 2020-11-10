package net.pms.network.message;

import jakarta.xml.bind.annotation.*;

/**
 * Represents payload of `ContentDirectory:1#Browse` request.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Browse")
public class BrowseRequest extends BrowseSearchRequest {

}
