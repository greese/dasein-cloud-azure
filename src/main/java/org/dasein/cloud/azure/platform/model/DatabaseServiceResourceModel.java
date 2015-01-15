package org.dasein.cloud.azure.platform.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Vlad_Munthiu on 11/19/2014.
 */

@XmlRootElement(name="ServiceResource", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class DatabaseServiceResourceModel {
    @XmlElement(name="Name", namespace ="http://schemas.microsoft.com/windowsazure")
    private String name;
    @XmlElement(name="Edition", namespace ="http://schemas.microsoft.com/windowsazure")
    private String edition;
    @XmlElement(name="CollationName", namespace ="http://schemas.microsoft.com/windowsazure")
    private String collationName;
    @XmlElement(name="MaxSizeBytes", namespace ="http://schemas.microsoft.com/windowsazure")
    private String maxSizeBytes;
    @XmlElement(name="ServiceObjectiveId", namespace ="http://schemas.microsoft.com/windowsazure")
    private String serviceObjectiveId;
    @XmlElement(name="Type", namespace ="http://schemas.microsoft.com/windowsazure")
    private String type;
    @XmlElement(name="State", namespace ="http://schemas.microsoft.com/windowsazure")
    private String state;
    @XmlElement(name="SelfLink", namespace ="http://schemas.microsoft.com/windowsazure")
    private String selfLink;
    @XmlElement(name="ParentLink", namespace ="http://schemas.microsoft.com/windowsazure")
    private String parentLink;
    @XmlElement(name="Id", namespace ="http://schemas.microsoft.com/windowsazure")
    private String id;
    @XmlElement(name="MaxSizeGB", namespace ="http://schemas.microsoft.com/windowsazure")
    private String maxSizeGB;
    @XmlElement(name="CreationDate", namespace ="http://schemas.microsoft.com/windowsazure")
    private String creationDate;
    @XmlElement(name="IsFederationRoot", namespace ="http://schemas.microsoft.com/windowsazure")
    private String isFederationRoot;
    @XmlElement(name="IsSystemObject", namespace ="http://schemas.microsoft.com/windowsazure")
    private String isSystemObject;
    @XmlElement(name="SizeMB", namespace ="http://schemas.microsoft.com/windowsazure")
    private String sizeMB;
    @XmlElement(name="AssignedServiceObjectiveId", namespace ="http://schemas.microsoft.com/windowsazure")
    private String assignedServiceObjectiveId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getCollationName() {
        return collationName;
    }

    public void setCollationName(String collationName) {
        this.collationName = collationName;
    }

    public String getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(String maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public String getServiceObjectiveId() {
        return serviceObjectiveId;
    }

    public void setServiceObjectiveId(String serviceObjectiveId) {
        this.serviceObjectiveId = serviceObjectiveId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getSelfLink() {
        return selfLink;
    }

    public void setSelfLink(String selfLink) {
        this.selfLink = selfLink;
    }

    public String getParentLink() {
        return parentLink;
    }

    public void setParentLink(String parentLink) {
        this.parentLink = parentLink;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMaxSizeGB() {
        return maxSizeGB;
    }

    public void setMaxSizeGB(String maxSizeGB) {
        this.maxSizeGB = maxSizeGB;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getIsFederationRoot() {
        return isFederationRoot;
    }

    public void setIsFederationRoot(String isFederationRoot) {
        this.isFederationRoot = isFederationRoot;
    }

    public String getIsSystemObject() {
        return isSystemObject;
    }

    public void setIsSystemObject(String isSystemObject) {
        this.isSystemObject = isSystemObject;
    }

    public String getSizeMB() {
        return sizeMB;
    }

    public void setSizeMB(String sizeMB) {
        this.sizeMB = sizeMB;
    }
}
