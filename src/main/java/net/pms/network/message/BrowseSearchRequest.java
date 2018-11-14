package net.pms.network.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * This is a hybrid of Search and Browse request message created just to satisfy current Browser/Search implementation
 * handler. Should be split up into separate classes as soon as possible.
 */

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class BrowseSearchRequest {

    @XmlElement(name = "ObjectID")
    private String objectId;

    @XmlElement(name = "ContainerID")
    private String containerId;

    @XmlElement(name = "StartingIndex")
    private Integer startingIndex;

    @XmlElement(name = "RequestedCount")
    private Integer requestedCount;

    @XmlElement(name = "SearchCriteria")
    private String searchCriteria;

    @XmlElement(name = "SortCriteria")
    private String sortCriteria;

    @XmlElement(name = "Filter")
    private String filter;

    @XmlElement(name = "BrowseFlag")
    private String browseFlag;

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public Integer getStartingIndex() {
        return startingIndex;
    }

    public void setStartingIndex(Integer startingIndex) {
        this.startingIndex = startingIndex;
    }

    public Integer getRequestedCount() {
        return requestedCount;
    }

    public void setRequestedCount(Integer requestedCount) {
        this.requestedCount = requestedCount;
    }

    public String getSearchCriteria() {
        return searchCriteria;
    }

    public void setSearchCriteria(String searchCriteria) {
        this.searchCriteria = searchCriteria;
    }

    public String getSortCriteria() {
        return sortCriteria;
    }

    public void setSortCriteria(String sortCriteria) {
        this.sortCriteria = sortCriteria;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getBrowseFlag() {
        return browseFlag;
    }

    public void setBrowseFlag(String browseFlag) {
        this.browseFlag = browseFlag;
    }

    @Override
    public String toString() {
        return "BrowseSearchRequest{" +
                "objectId='" + objectId + "'" +
                ", containerId='" + containerId + "'" +
                ", startingIndex=" + startingIndex +
                ", requestedCount=" + requestedCount +
                ", searchCriteria='" + searchCriteria + "'" +
                ", sortCriteria='" + sortCriteria + "'" +
                ", filter='" + filter + "'" +
                ", browseFlag='" + browseFlag + "'" +
                '}';
    }
}
