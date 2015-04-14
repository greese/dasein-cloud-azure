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

package org.dasein.cloud.azure.storage.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Vlad_Munthiu on 7/17/2014.
 */

@XmlRootElement(name="CreateStorageServiceInput", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class CreateStorageServiceInputModel
{
    @XmlElement(name="ServiceName", namespace ="http://schemas.microsoft.com/windowsazure")
    private String serviceName;
    @XmlElement(name="Description", namespace ="http://schemas.microsoft.com/windowsazure")
    private String description;
    @XmlElement(name="Label", namespace ="http://schemas.microsoft.com/windowsazure")
    private String label;
    @XmlElement(name="Location", namespace ="http://schemas.microsoft.com/windowsazure")
    private String location;
    @XmlElement(name="GeoReplicationEnabled", namespace ="http://schemas.microsoft.com/windowsazure")
    private String geoReplicationEnabled;
    @XmlElement(name="SecondaryReadEnabled", namespace ="http://schemas.microsoft.com/windowsazure")
    private String secondaryReadEnabled;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getGeoReplicationEnabled() {
        return geoReplicationEnabled;
    }

    public void setGeoReplicationEnabled(String geoReplicationEnabled) {
        this.geoReplicationEnabled = geoReplicationEnabled;
    }

    public String getSecondaryReadEnabled() {
        return secondaryReadEnabled;
    }

    public void setSecondaryReadEnabled(String secondaryReadEnabled) {
        this.secondaryReadEnabled = secondaryReadEnabled;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
