package org.dasein.cloud.azure.platform.model;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * Created by Vlad_Munthiu on 11/17/2014.
 */

@XmlRootElement(name="ServiceResource", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServerServiceResourceModel {
    @XmlElement(name="Name", namespace ="http://schemas.microsoft.com/windowsazure")
    private String name;
    @XmlElement(name="Type", namespace ="http://schemas.microsoft.com/windowsazure")
    private String type;
    @XmlElement(name="DatabaseQuota", namespace ="http://schemas.microsoft.com/windowsazure")
    private String databaseQuota;
    @XmlElement(name="ServerQuota", namespace ="http://schemas.microsoft.com/windowsazure")
    private String serverQuota;
    @XmlElement(name="StartIPAddress", namespace ="http://schemas.microsoft.com/windowsazure")
    private String startIpAddress;
    @XmlElement(name="EndIPAddress", namespace ="http://schemas.microsoft.com/windowsazure")
    private String endIpAddress;

    @XmlElementWrapper(name = "Versions", namespace = "http://schemas.microsoft.com/windowsazure")
    @XmlElement(name="Version", namespace ="http://schemas.microsoft.com/windowsazure")
    private List<Version> versions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDatabaseQuota() {
        return databaseQuota;
    }

    public void setDatabaseQuota(String databaseQuota) {
        this.databaseQuota = databaseQuota;
    }

    public String getServerQuota() {
        return serverQuota;
    }

    public void setServerQuota(String serverQuota) {
        this.serverQuota = serverQuota;
    }

    public List<Version> getVersions() {
        return versions;
    }

    public void setVersions(List<Version> versions) {
        this.versions = versions;
    }

    public String getStartIpAddress() {
        return startIpAddress;
    }

    public void setStartIpAddress(String startIpAddress) {
        this.startIpAddress = startIpAddress;
    }

    public String getEndIpAddress() {
        return endIpAddress;
    }

    public void setEndIpAddress(String endIpAddress) {
        this.endIpAddress = endIpAddress;
    }

    @XmlRootElement(name="Version", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Version{
        @XmlElement(name="Name", namespace ="http://schemas.microsoft.com/windowsazure")
        private String name;
        @XmlElement(name="IsDefault", namespace ="http://schemas.microsoft.com/windowsazure")
        private String isDefault;
        @XmlElementWrapper(name = "Editions", namespace = "http://schemas.microsoft.com/windowsazure")
        @XmlElement(name="Edition", namespace ="http://schemas.microsoft.com/windowsazure")
        private List<Edition> editions;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIsDefault() {
            return isDefault;
        }

        public void setIsDefault(String isDefault) {
            this.isDefault = isDefault;
        }

        public List<Edition> getEditions() {
            return editions;
        }

        public void setEditions(List<Edition> editions) {
            this.editions = editions;
        }
    }

    @XmlRootElement(name="Edition", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Edition{
        @XmlElement(name="Name", namespace ="http://schemas.microsoft.com/windowsazure")
        private String name;
        @XmlElement(name="IsDefault", namespace ="http://schemas.microsoft.com/windowsazure")
        private String isDefault;
        @XmlElementWrapper(name = "ServiceLevelObjectives", namespace = "http://schemas.microsoft.com/windowsazure")
        @XmlElement(name="ServiceLevelObjective", namespace ="http://schemas.microsoft.com/windowsazure")
        private List<ServiceLevelObjective> serviceLevelObjectives;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIsDefault() {
            return isDefault;
        }

        public void setIsDefault(String isDefault) {
            this.isDefault = isDefault;
        }

        public List<ServiceLevelObjective> getServiceLevelObjectives() {
            return serviceLevelObjectives;
        }

        public void setServiceLevelObjectives(List<ServiceLevelObjective> serviceLevelObjectives) {
            this.serviceLevelObjectives = serviceLevelObjectives;
        }
    }

    @XmlRootElement(name="ServiceLevelObjective", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ServiceLevelObjective{
        @XmlElement(name="Name", namespace ="http://schemas.microsoft.com/windowsazure")
        private String name;
        @XmlElement(name="IsDefault", namespace ="http://schemas.microsoft.com/windowsazure")
        private String isDefault;
        @XmlElement(name="ID", namespace ="http://schemas.microsoft.com/windowsazure")
        private String id;
        @XmlElementWrapper(name = "MaxSizes", namespace = "http://schemas.microsoft.com/windowsazure")
        @XmlElement(name="MaxSize", namespace ="http://schemas.microsoft.com/windowsazure")
        private List<DatabaseSize> maxSizes;
        @XmlElement(name="PerformanceLevel", namespace ="http://schemas.microsoft.com/windowsazure")
        private PerformanceLevel performanceLevel;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIsDefault() {
            return isDefault;
        }

        public void setIsDefault(String isDefault) {
            this.isDefault = isDefault;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<DatabaseSize> getMaxSizes() {
            return maxSizes;
        }

        public void setMaxSizes(List<DatabaseSize> maxSizes) {
            this.maxSizes = maxSizes;
        }

        public PerformanceLevel getPerformanceLevel() {
            return performanceLevel;
        }

        public void setPerformanceLevel(PerformanceLevel performanceLevel) {
            this.performanceLevel = performanceLevel;
        }
    }

    @XmlRootElement(name="MaxSize", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DatabaseSize{
        @XmlElement(name="Value", namespace ="http://schemas.microsoft.com/windowsazure")
        private String value;
        @XmlElement(name="Unit", namespace ="http://schemas.microsoft.com/windowsazure")
        private String unit;
        @XmlElement(name="IsDefault", namespace ="http://schemas.microsoft.com/windowsazure")
        private String isDefault;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getIsDefault() {
            return isDefault;
        }

        public void setIsDefault(String isDefault) {
            this.isDefault = isDefault;
        }
    }

    @XmlRootElement(name="PerformanceLevel", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PerformanceLevel{
        @XmlElement(name="Value", namespace ="http://schemas.microsoft.com/windowsazure")
        private String value;
        @XmlElement(name="Unit", namespace ="http://schemas.microsoft.com/windowsazure")
        private String unit;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }
    }
}
