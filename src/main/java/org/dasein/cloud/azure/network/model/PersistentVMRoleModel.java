package org.dasein.cloud.azure.network.model;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * Created by Vlad_Munthiu on 7/31/2014.
 */

@XmlRootElement(name="PersistentVMRole", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class PersistentVMRoleModel
{
    @XmlElement(name="RoleName", namespace ="http://schemas.microsoft.com/windowsazure")
    private String roleName;
    @XmlElement(name="OsVersion", namespace ="http://schemas.microsoft.com/windowsazure")
    private String osVersion;
    @XmlElement(name="RoleType", namespace ="http://schemas.microsoft.com/windowsazure")
    private String roleType;
    @XmlElementWrapper(name = "ConfigurationSets", namespace = "http://schemas.microsoft.com/windowsazure")
    @XmlElement(name="ConfigurationSet", namespace ="http://schemas.microsoft.com/windowsazure")
    private List<ConfigurationSet> configurationSets;
    @XmlElement(name="OSVirtualHardDisk", namespace ="http://schemas.microsoft.com/windowsazure")
    private OSVirtualHardDisk osVirtualHardDisk;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public List<ConfigurationSet> getConfigurationSets() {
        return configurationSets;
    }

    public void setConfigurationSets(List<ConfigurationSet> configurationSets) {
        this.configurationSets = configurationSets;
    }

    public OSVirtualHardDisk getOsVirtualHardDisk() {
        return osVirtualHardDisk;
    }

    public void setOsVirtualHardDisk(OSVirtualHardDisk osVirtualHardDisk) {
        this.osVirtualHardDisk = osVirtualHardDisk;
    }

    @XmlRootElement(name="ConfigurationSet", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ConfigurationSet
    {
        @XmlElement(name="ConfigurationSetType", namespace ="http://schemas.microsoft.com/windowsazure")
        private String configurationSetType;
        @XmlElementWrapper(name = "InputEndpoints", namespace = "http://schemas.microsoft.com/windowsazure")
        @XmlElement(name="InputEndpoint", namespace ="http://schemas.microsoft.com/windowsazure")
        private List<InputEndpoint> inputEndpoints;

        public String getConfigurationSetType() {
            return configurationSetType;
        }

        public void setConfigurationSetType(String configurationSetType) {
            this.configurationSetType = configurationSetType;
        }

        public List<InputEndpoint> getInputEndpoints() {
            return inputEndpoints;
        }

        public void setInputEndpoints(List<InputEndpoint> inputEndpoints) {
            this.inputEndpoints = inputEndpoints;
        }
    }

    @XmlRootElement(name="InputEndpoint", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class InputEndpoint
    {
        @XmlElement(name="LocalPort", namespace ="http://schemas.microsoft.com/windowsazure")
        private String localPort;
        @XmlElement(name="Name", namespace ="http://schemas.microsoft.com/windowsazure")
        private String name;
        @XmlElement(name="Port", namespace ="http://schemas.microsoft.com/windowsazure")
        private String port;
        @XmlElement(name="Protocol", namespace ="http://schemas.microsoft.com/windowsazure")
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

    @XmlRootElement(name="OSVirtualHardDisk", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OSVirtualHardDisk
    {
        @XmlElement(name="HostCaching", namespace ="http://schemas.microsoft.com/windowsazure")
        private String hostCaching;
        @XmlElement(name="DiskLabel", namespace ="http://schemas.microsoft.com/windowsazure")
        private String diskLabel;
        @XmlElement(name="MediaLink", namespace ="http://schemas.microsoft.com/windowsazure")
        private String mediaLink;
        @XmlElement(name="SourceImageName", namespace ="http://schemas.microsoft.com/windowsazure")
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
