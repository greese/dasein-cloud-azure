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

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * Created by Vlad_Munthiu on 8/20/2014.
 */
@XmlRootElement(name="ConfigurationSet")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConfigurationSetModel {
    @XmlAttribute(name="type")
    private String type;

    @XmlElement(name="ConfigurationSetType")
    private String configurationSetType;

    @XmlElement(name="ComputerName")
    private String computerName;
    @XmlElement(name="AdminPassword")
    private String adminPassword;
    @XmlElement(name="EnableAutomaticUpdates")
    private String enableAutomaticUpdates;
    @XmlElement(name="TimeZone")
    private String timeZone;
    @XmlElement(name="AdminUsername")
    private String adminUsername;

    @XmlElement(name="HostName")
    private String hostName;
    @XmlElement(name="UserName")
    private String userName;
    @XmlElement(name="UserPassword")
    private String userPassword;
    @XmlElement(name="DisableSshPasswordAuthentication")
    private String disableSshPasswordAuthentication;
    @XmlElement(name="CustomData")
    private String customData;

    @XmlElementWrapper(name = "InputEndpoints")
    @XmlElement(name="InputEndpoint")
    private List<InputEndpointModel> inputEndpoints;
    @XmlElementWrapper(name = "SubnetNames")
    @XmlElement(name="SubnetName")
    private List<String> subnetNames;

    public String getConfigurationSetType() {
        return configurationSetType;
    }

    public void setConfigurationSetType(String configurationSetType) {
        this.configurationSetType = configurationSetType;
    }

    public String getComputerName() {
        return computerName;
    }

    public void setComputerName(String computerName) {
        this.computerName = computerName;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getEnableAutomaticUpdates() {
        return enableAutomaticUpdates;
    }

    public void setEnableAutomaticUpdates(String enableAutomaticUpdates) {
        this.enableAutomaticUpdates = enableAutomaticUpdates;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getCustomData() { return customData; }

    public void setCustomData(String customData) { this.customData = customData; }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public String getDisableSshPasswordAuthentication() {
        return disableSshPasswordAuthentication;
    }

    public void setDisableSshPasswordAuthentication(String disableSshPasswordAuthentication) {
        this.disableSshPasswordAuthentication = disableSshPasswordAuthentication;
    }

    public List<InputEndpointModel> getInputEndpoints() {
        return inputEndpoints;
    }

    public void setInputEndpoints(List<InputEndpointModel> inputEndpoints) {
        this.inputEndpoints = inputEndpoints;
    }

    public List<String> getSubnetNames() {
        return subnetNames;
    }

    public void setSubnetNames(List<String> subnetNames) {
        this.subnetNames = subnetNames;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @XmlRootElement(name="InputEndpoint")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class InputEndpointModel
    {
        @XmlElement(name="LocalPort")
        private String localPort;
        @XmlElement(name="Name")
        private String name;
        @XmlElement(name="Port")
        private String port;
        @XmlElement(name="Protocol")
        private String protocol;

        public String getLocalPort() {
            return localPort;
        }

        public void setLocalPort(String localPort) {
            this.localPort = localPort;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }
    }
}



