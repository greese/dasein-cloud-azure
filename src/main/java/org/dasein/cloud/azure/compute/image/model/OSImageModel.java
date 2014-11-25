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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Vlad_Munthiu on 8/18/2014.
 */
@XmlRootElement(name="OSImage", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class OSImageModel {
    @XmlElement(name="AffinityGroup", namespace ="http://schemas.microsoft.com/windowsazure")
    private String affinityGroup;
    @XmlElement(name="Category", namespace ="http://schemas.microsoft.com/windowsazure")
    private String category;
    @XmlElement(name="Label", namespace ="http://schemas.microsoft.com/windowsazure")
    private String label;
    @XmlElement(name="Location", namespace ="http://schemas.microsoft.com/windowsazure")
    private String location;
    @XmlElement(name="LogicalSizeInGB", namespace ="http://schemas.microsoft.com/windowsazure")
    private String logicalSizeInGB;
    @XmlElement(name="MediaLink", namespace ="http://schemas.microsoft.com/windowsazure")
    private String mediaLink;
    @XmlElement(name="Name", namespace ="http://schemas.microsoft.com/windowsazure")
    private String name;
    @XmlElement(name="OS", namespace ="http://schemas.microsoft.com/windowsazure")
    private String os;
    @XmlElement(name="Eula", namespace ="http://schemas.microsoft.com/windowsazure")
    private String eula;
    @XmlElement(name="Description", namespace ="http://schemas.microsoft.com/windowsazure")
    private String description;
    @XmlElement(name="ImageFamily", namespace ="http://schemas.microsoft.com/windowsazure")
    private String imageFamily;
    @XmlElement(name="ShowInGui", namespace ="http://schemas.microsoft.com/windowsazure")
    private String showInGui;
    @XmlElement(name="PublishedDate", namespace ="http://schemas.microsoft.com/windowsazure")
    private String publishedDate;
    @XmlElement(name="IsPremium", namespace ="http://schemas.microsoft.com/windowsazure")
    private String isPremium;
    @XmlElement(name="PrivacyUri", namespace ="http://schemas.microsoft.com/windowsazure")
    private String privacyUri;
    @XmlElement(name="RecommendedVMSize", namespace ="http://schemas.microsoft.com/windowsazure")
    private String recommendedVMSize;
    @XmlElement(name="PublisherName", namespace ="http://schemas.microsoft.com/windowsazure")
    private String publisherName;
    @XmlElement(name="PricingDetailLink", namespace ="http://schemas.microsoft.com/windowsazure")
    private String pricingDetailLink;
    @XmlElement(name="SmallIconUri", namespace ="http://schemas.microsoft.com/windowsazure")
    private String smallIconUri;
    @XmlElement(name="Language", namespace ="http://schemas.microsoft.com/windowsazure")
    private String language;

    public String getAffinityGroup() {
        return affinityGroup;
    }

    public void setAffinityGroup(String affinityGroup) {
        this.affinityGroup = affinityGroup;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLogicalSizeInGB() {
        return logicalSizeInGB;
    }

    public void setLogicalSizeInGB(String logicalSizeInGB) {
        this.logicalSizeInGB = logicalSizeInGB;
    }

    public String getMediaLink() {
        return mediaLink;
    }

    public void setMediaLink(String mediaLink) {
        this.mediaLink = mediaLink;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getEula() {
        return eula;
    }

    public void setEula(String eula) {
        this.eula = eula;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageFamily() {
        return imageFamily;
    }

    public void setImageFamily(String imageFamily) {
        this.imageFamily = imageFamily;
    }

    public String getShowInGui() {
        return showInGui;
    }

    public void setShowInGui(String showInGui) {
        this.showInGui = showInGui;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public String getIsPremium() {
        return isPremium;
    }

    public void setIsPremium(String isPremium) {
        this.isPremium = isPremium;
    }

    public String getPrivacyUri() {
        return privacyUri;
    }

    public void setPrivacyUri(String privacyUri) {
        this.privacyUri = privacyUri;
    }

    public String getRecommendedVMSize() {
        return recommendedVMSize;
    }

    public void setRecommendedVMSize(String recommendedVMSize) {
        this.recommendedVMSize = recommendedVMSize;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

    public String getPricingDetailLink() {
        return pricingDetailLink;
    }

    public void setPricingDetailLink(String pricingDetailLink) {
        this.pricingDetailLink = pricingDetailLink;
    }

    public String getSmallIconUri() {
        return smallIconUri;
    }

    public void setSmallIconUri(String smallIconUri) {
        this.smallIconUri = smallIconUri;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
