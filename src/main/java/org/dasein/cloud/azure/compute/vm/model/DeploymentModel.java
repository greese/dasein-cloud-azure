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

import org.dasein.cloud.azure.compute.disk.model.DataVirtualHardDiskModel;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * Created by Vlad_Munthiu on 8/19/2014.
 */
@XmlRootElement(name="Deployment")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeploymentModel {
    @XmlElement(name="Name")
    private String name;
    @XmlElement(name="DeploymentSlot")
    private String deploymentSlot;
    @XmlElement(name="Label")
    private String label;
    @XmlElementWrapper(name = "RoleList")
    @XmlElement(name="Role")
    private List<RoleModel> roles;
    @XmlElement(name="VirtualNetworkName")
    private String virtualNetworkName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDeploymentSlot() {
        return deploymentSlot;
    }

    public void setDeploymentSlot(String deploymentSlot) {
        this.deploymentSlot = deploymentSlot;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<RoleModel> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleModel> roles) {
        this.roles = roles;
    }

    public String getVirtualNetworkName() {
        return virtualNetworkName;
    }

    public void setVirtualNetworkName(String virtualNetworkName) {
        this.virtualNetworkName = virtualNetworkName;
    }

    @XmlRootElement(name="Role")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RoleModel {
        @XmlElement(name="RoleName")
        private String roleName;
        @XmlElement(name="RoleType")
        private String roleType;
        @XmlElementWrapper(name = "ConfigurationSets")
        @XmlElement(name="ConfigurationSet")
        private List<ConfigurationSetModel> configurationsSets;
        @XmlElement(name="VMImageName")
        private String vmImageName;
        @XmlElement(name="MediaLocation")
        private String mediaLocation;
        @XmlElementWrapper(name = "DataVirtualHardDisks")
        @XmlElement(name="DataVirtualHardDisk")
        private List<DataVirtualHardDiskModel> dataVirtualDisks;
        @XmlElement(name="OSVirtualHardDisk")
        private OSVirtualHardDiskModel osVirtualDisk;
        @XmlElement(name="RoleSize")
        private String roleSize;
        @XmlElement(name="ProvisionGuestAgent")
        private String provisionGuestAgent;

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }

        public String getRoleType() {
            return roleType;
        }

        public void setRoleType(String roleType) {
            this.roleType = roleType;
        }

        public List<ConfigurationSetModel> getConfigurationsSets() {
            return configurationsSets;
        }

        public void setConfigurationsSets(List<ConfigurationSetModel> configurationsSets) {
            this.configurationsSets = configurationsSets;
        }

        public String getVmImageName() {
            return vmImageName;
        }

        public void setVmImageName(String vmImageName) {
            this.vmImageName = vmImageName;
        }

        public String getMediaLocation() {
            return mediaLocation;
        }

        public void setMediaLocation(String mediaLocation) {
            this.mediaLocation = mediaLocation;
        }

        public OSVirtualHardDiskModel getOsVirtualDisk() {
            return osVirtualDisk;
        }

        public void setOsVirtualDisk(OSVirtualHardDiskModel osVirtualDisk) {
            this.osVirtualDisk = osVirtualDisk;
        }

        public String getRoleSize() {
            return roleSize;
        }

        public void setRoleSize(String roleSize) {
            this.roleSize = roleSize;
        }

        public String getProvisionGuestAgent() {
            return provisionGuestAgent;
        }

        public void setProvisionGuestAgent(String provisionGuestAgent) {
            this.provisionGuestAgent = provisionGuestAgent;
        }

        public List<DataVirtualHardDiskModel> getDataVirtualDisks() {
            return dataVirtualDisks;
        }

        public void setDataVirtualDisks(List<DataVirtualHardDiskModel> dataVirtualDisks) {
            this.dataVirtualDisks = dataVirtualDisks;
        }
    }

    @XmlRootElement(name="OSVirtualHardDisk")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OSVirtualHardDiskModel {
        @XmlElement(name="HostCaching")
        private String hostCaching;
        @XmlElement(name="DiskLabel")
        private String diskLabel;
        @XmlElement(name="MediaLink")
        private String mediaLink;
        @XmlElement(name="SourceImageName")
        private String sourceImageName;

        public String getHostCaching() {
            return hostCaching;
        }

        public void setHostCaching(String hostCaching) {
            this.hostCaching = hostCaching;
        }

        public String getDiskLabel() {
            return diskLabel;
        }

        public void setDiskLabel(String diskLabel) {
            this.diskLabel = diskLabel;
        }

        public String getMediaLink() {
            return mediaLink;
        }

        public void setMediaLink(String mediaLink) {
            this.mediaLink = mediaLink;
        }

        public String getSourceImageName() {
            return sourceImageName;
        }

        public void setSourceImageName(String sourceImageName) {
            this.sourceImageName = sourceImageName;
        }
    }
}
