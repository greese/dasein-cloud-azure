/**
 * Copyright (C) 2013-2014 Dell, Inc
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.dasein.cloud.azure.compute.image.model;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * Created by Vlad_Munthiu on 8/15/2014.
 */
@XmlRootElement(name="VMImage", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class VMImageModel
{
    @XmlElement(name="Name", namespace ="http://schemas.microsoft.com/windowsazure")
    private String name;
    @XmlElement(name="Label", namespace ="http://schemas.microsoft.com/windowsazure")
    private String label;
    @XmlElement(name="Category", namespace ="http://schemas.microsoft.com/windowsazure")
    private String category;
    @XmlElement(name="Description", namespace ="http://schemas.microsoft.com/windowsazure")
    private String description;
    @XmlElement(name="OSDiskConfiguration", namespace ="http://schemas.microsoft.com/windowsazure")
    private OSDiskConfigurationModel osDiskConfiguration;
    @XmlElementWrapper(name = "DataDiskConfigurations", namespace = "http://schemas.microsoft.com/windowsazure")
    @XmlElement(name="DataDiskConfiguration", namespace ="http://schemas.microsoft.com/windowsazure")
    private List<DataDiskConfigurationModel> dataDiskConfigurations;
    @XmlElement(name="ServiceName", namespace ="http://schemas.microsoft.com/windowsazure")
    private String serviceName;
    @XmlElement(name="DeploymentName", namespace ="http://schemas.microsoft.com/windowsazure")
    private String deploymentName;
    @XmlElement(name="RoleName", namespace ="http://schemas.microsoft.com/windowsazure")
    private String roleName;
    @XmlElement(name="Location", namespace ="http://schemas.microsoft.com/windowsazure")
    private String location;
    @XmlElement(name="AffinityGroup", namespace ="http://schemas.microsoft.com/windowsazure")
    private String affinityGroup;
    @XmlElement(name="CreatedTime", namespace ="http://schemas.microsoft.com/windowsazure")
    private String createdTime;
    @XmlElement(name="ModifiedTime", namespace ="http://schemas.microsoft.com/windowsazure")
    private String modifiedTime;
    @XmlElement(name="Language", namespace ="http://schemas.microsoft.com/windowsazure")
    private String language;
    @XmlElement(name="ImageFamily", namespace ="http://schemas.microsoft.com/windowsazure")
    private String imageFamily;
    @XmlElement(name="RecommendedVMSize", namespace ="http://schemas.microsoft.com/windowsazure")
    private String recommendedVMSize;
    @XmlElement(name="IsPremium", namespace ="http://schemas.microsoft.com/windowsazure")
    private String isPremium;
    @XmlElement(name="Eula", namespace ="http://schemas.microsoft.com/windowsazure")
    private String eula;
    @XmlElement(name="IconUri", namespace ="http://schemas.microsoft.com/windowsazure")
    private String iconUri;
    @XmlElement(name="SmallIconUri", namespace ="http://schemas.microsoft.com/windowsazure")
    private String smallIconUri;
    @XmlElement(name="PrivacyUri", namespace ="http://schemas.microsoft.com/windowsazure")
    private String privacyUri;
    @XmlElement(name="PublisherName", namespace ="http://schemas.microsoft.com/windowsazure")
    private String publisherName;
    @XmlElement(name="PublishedDate", namespace ="http://schemas.microsoft.com/windowsazure")
    private String publishedDate;
    @XmlElement(name="ShowInGui", namespace ="http://schemas.microsoft.com/windowsazure")
    private String showInGui;
    @XmlElement(name="PricingDetailLink", namespace ="http://schemas.microsoft.com/windowsazure")
    private String pricingDetailLink;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OSDiskConfigurationModel getOsDiskConfiguration() {
        return osDiskConfiguration;
    }

    public void setOsDiskConfiguration(OSDiskConfigurationModel osDiskConfiguration) {
        this.osDiskConfiguration = osDiskConfiguration;
    }

    public List<DataDiskConfigurationModel> getDataDiskConfigurations() {
        return dataDiskConfigurations;
    }

    public void setDataDiskConfigurations(List<DataDiskConfigurationModel> dataDiskConfigurations) {
        this.dataDiskConfigurations = dataDiskConfigurations;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAffinityGroup() {
        return affinityGroup;
    }

    public void setAffinityGroup(String affinityGroup) {
        this.affinityGroup = affinityGroup;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(String modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getImageFamily() {
        return imageFamily;
    }

    public void setImageFamily(String imageFamily) {
        this.imageFamily = imageFamily;
    }

    public String getRecommendedVMSize() {
        return recommendedVMSize;
    }

    public void setRecommendedVMSize(String recommendedVMSize) {
        this.recommendedVMSize = recommendedVMSize;
    }

    public String getIsPremium() {
        return isPremium;
    }

    public void setIsPremium(String isPremium) {
        this.isPremium = isPremium;
    }

    public String getEula() {
        return eula;
    }

    public void setEula(String eula) {
        this.eula = eula;
    }

    public String getIconUri() {
        return iconUri;
    }

    public void setIconUri(String iconUri) {
        this.iconUri = iconUri;
    }

    public String getSmallIconUri() {
        return smallIconUri;
    }

    public void setSmallIconUri(String smallIconUri) {
        this.smallIconUri = smallIconUri;
    }

    public String getPrivacyUri() {
        return privacyUri;
    }

    public void setPrivacyUri(String privacyUri) {
        this.privacyUri = privacyUri;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public String getShowInGui() {
        return showInGui;
    }

    public void setShowInGui(String showInGui) {
        this.showInGui = showInGui;
    }

    public String getPricingDetailLink() {
        return pricingDetailLink;
    }

    public void setPricingDetailLink(String pricingDetailLink) {
        this.pricingDetailLink = pricingDetailLink;
    }

    @XmlRootElement(name="OSDiskConfiguration", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OSDiskConfigurationModel
    {
        @XmlElement(name="Name", namespace ="http://schemas.microsoft.com/windowsazure")
        private String name;
        @XmlElement(name="HostCaching", namespace ="http://schemas.microsoft.com/windowsazure")
        private String hostCaching;
        @XmlElement(name="OSState", namespace ="http://schemas.microsoft.com/windowsazure")
        private String osState;
        @XmlElement(name="OS", namespace ="http://schemas.microsoft.com/windowsazure")
        private String os;
        @XmlElement(name="MediaLink", namespace ="http://schemas.microsoft.com/windowsazure")
        private String mediaLink;
        @XmlElement(name="LogicalDiskSizeInGB", namespace ="http://schemas.microsoft.com/windowsazure")
        private String logicalDiskSizeInGB;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHostCaching() {
            return hostCaching;
        }

        public void setHostCaching(String hostCaching) {
            this.hostCaching = hostCaching;
        }

        public String getOsState() {
            return osState;
        }

        public void setOsState(String osState) {
            this.osState = osState;
        }

        public String getOs() {
            return os;
        }

        public void setOs(String os) {
            this.os = os;
        }

        public String getMediaLink() {
            return mediaLink;
        }

        public void setMediaLink(String mediaLink) {
            this.mediaLink = mediaLink;
        }

        public String getLogicalDiskSizeInGB() {
            return logicalDiskSizeInGB;
        }

        public void setLogicalDiskSizeInGB(String logicalDiskSizeInGB) {
            this.logicalDiskSizeInGB = logicalDiskSizeInGB;
        }
    }

    @XmlRootElement(name="DataDiskConfiguration", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DataDiskConfigurationModel
    {
        @XmlElement(name="Name", namespace ="http://schemas.microsoft.com/windowsazure")
        private String name;
        @XmlElement(name="HostCaching", namespace ="http://schemas.microsoft.com/windowsazure")
        private String hostCaching;
        @XmlElement(name="Lun", namespace ="http://schemas.microsoft.com/windowsazure")
        private String lun;
        @XmlElement(name="MediaLink", namespace ="http://schemas.microsoft.com/windowsazure")
        private String mediaLink;
        @XmlElement(name="LogicalDiskSizeInGB", namespace ="http://schemas.microsoft.com/windowsazure")
        private String logicalDiskSizeInGB;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHostCaching() {
            return hostCaching;
        }

        public void setHostCaching(String hostCaching) {
            this.hostCaching = hostCaching;
        }

        public String getLun() {
            return lun;
        }

        public void setLun(String lun) {
            this.lun = lun;
        }

        public String getMediaLink() {
            return mediaLink;
        }

        public void setMediaLink(String mediaLink) {
            this.mediaLink = mediaLink;
        }

        public String getLogicalDiskSizeInGB() {
            return logicalDiskSizeInGB;
        }

        public void setLogicalDiskSizeInGB(String logicalDiskSizeInGB) {
            this.logicalDiskSizeInGB = logicalDiskSizeInGB;
        }
    }
}
