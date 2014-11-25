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

package org.dasein.cloud.azure.compute.model;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * Created by Vlad_Munthiu on 7/23/2014.
 */

@XmlRootElement(name="AffinityGroup", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class AffinityGroupModel {

    @XmlElement(name="Name", namespace ="http://schemas.microsoft.com/windowsazure")
    private String name;
    @XmlElement(name="Label", namespace ="http://schemas.microsoft.com/windowsazure")
    private String label;
    @XmlElement(name="Description", namespace ="http://schemas.microsoft.com/windowsazure")
    private String description;
    @XmlElement(name="Location", namespace ="http://schemas.microsoft.com/windowsazure")
    private String location;
    @XmlElementWrapper(name = "HostedServices", namespace = "http://schemas.microsoft.com/windowsazure")
    @XmlElement(name="HostedService", namespace ="http://schemas.microsoft.com/windowsazure")
    private List<HostedService> hostedServices;
    @XmlElementWrapper(name = "StorageServices", namespace = "http://schemas.microsoft.com/windowsazure")
    @XmlElement(name="StorageService", namespace ="http://schemas.microsoft.com/windowsazure")
    private List<HostedService> storageServices;
    @XmlElementWrapper(name = "Capabilities", namespace = "http://schemas.microsoft.com/windowsazure")
    @XmlElement(name="Capability", namespace ="http://schemas.microsoft.com/windowsazure")
    private List<String> capabilities;
    @XmlElement(name="CreatedTime", namespace ="http://schemas.microsoft.com/windowsazure")
    private String createdTime;
    @XmlElement(name="ComputeCapabilities", namespace ="http://schemas.microsoft.com/windowsazure")
    private ComputeCapabilities computeCapabilities;

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

    public List<HostedService> getHostedServices() {
        return hostedServices;
    }

    public void setHostedServices(List<HostedService> hostedServices) {
        this.hostedServices = hostedServices;
    }

    public List<HostedService> getStorageServices() {
        return storageServices;
    }

    public void setStorageServices(List<HostedService> storageServices) {
        this.storageServices = storageServices;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public ComputeCapabilities getComputeCapabilities() {
        return computeCapabilities;
    }

    public void setComputeCapabilities(ComputeCapabilities computeCapabilities) {
        this.computeCapabilities = computeCapabilities;
    }

    @XmlRootElement(name="HostedService", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class HostedService
    {
        @XmlElement(name="Url", namespace ="http://schemas.microsoft.com/windowsazure")
        private String url;
        @XmlElement(name="ServiceName", namespace ="http://schemas.microsoft.com/windowsazure")
        private String serviceName;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }
    }

    @XmlRootElement(name="StorageService", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class StorageService
    {
        @XmlElement(name="Url", namespace ="http://schemas.microsoft.com/windowsazure")
        private String url;
        @XmlElement(name="ServiceName", namespace ="http://schemas.microsoft.com/windowsazure")
        private String serviceName;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }
    }

    @XmlRootElement(name="ComputeCapabilities", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ComputeCapabilities
    {
        @XmlElementWrapper(name = "VirtualMachineRoleSizes", namespace = "http://schemas.microsoft.com/windowsazure")
        @XmlElement(name="RoleSize", namespace ="http://schemas.microsoft.com/windowsazure")
        private List<String> virtualMachineRoleSizes;
        @XmlElementWrapper(name = "WebWorkerRoleSizes", namespace = "http://schemas.microsoft.com/windowsazure")
        @XmlElement(name="RoleSize", namespace ="http://schemas.microsoft.com/windowsazure")
        private List<String> webWorkerRoleSizes;

        public List<String> getVirtualMachineRoleSizes() {
            return virtualMachineRoleSizes;
        }

        public void setVirtualMachineRoleSizes(List<String> virtualMachineRoleSizes) {
            this.virtualMachineRoleSizes = virtualMachineRoleSizes;
        }

        public List<String> getWebWorkerRoleSizes() {
            return webWorkerRoleSizes;
        }

        public void setWebWorkerRoleSizes(List<String> webWorkerRoleSizes) {
            this.webWorkerRoleSizes = webWorkerRoleSizes;
        }
    }
}
