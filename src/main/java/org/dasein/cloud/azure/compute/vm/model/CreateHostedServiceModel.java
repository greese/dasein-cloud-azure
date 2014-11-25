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

package org.dasein.cloud.azure.compute.vm.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Vlad_Munthiu on 8/19/2014.
 */
@XmlRootElement(name="CreateHostedService", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class CreateHostedServiceModel {
    @XmlElement(name="ServiceName", namespace ="http://schemas.microsoft.com/windowsazure")
    private String serviceName;
    @XmlElement(name="Label", namespace ="http://schemas.microsoft.com/windowsazure")
    private String label;
    @XmlElement(name="Description", namespace ="http://schemas.microsoft.com/windowsazure")
    private String description;
    @XmlElement(name="AffinityGroup", namespace ="http://schemas.microsoft.com/windowsazure")
    private String affinityGroup;
    @XmlElement(name="Location", namespace ="http://schemas.microsoft.com/windowsazure")
    private String location;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAffinityGroup() {
        return affinityGroup;
    }

    public void setAffinityGroup(String affinityGroup) {
        this.affinityGroup = affinityGroup;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
