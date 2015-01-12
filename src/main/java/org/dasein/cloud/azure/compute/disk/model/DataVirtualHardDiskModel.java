package org.dasein.cloud.azure.compute.disk.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by vmunthiu on 1/9/2015.
 */

@XmlRootElement(name="DataVirtualHardDisk", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class DataVirtualHardDiskModel {
    @XmlElement(name="HostCaching", namespace ="http://schemas.microsoft.com/windowsazure")
    private String hostCaching;
    @XmlElement(name="DiskLabel", namespace ="http://schemas.microsoft.com/windowsazure")
    private String diskLabel;
    @XmlElement(name="DiskName", namespace ="http://schemas.microsoft.com/windowsazure")
    private String diskName;
    @XmlElement(name="Lun", namespace ="http://schemas.microsoft.com/windowsazure")
    private String lun;
    @XmlElement(name="LogicalDiskSizeInGB", namespace ="http://schemas.microsoft.com/windowsazure")
    private String logicalDiskSizeInGB;
    @XmlElement(name="MediaLink", namespace ="http://schemas.microsoft.com/windowsazure")
    private String mediaLink;

    public String getHostCaching() {
        return hostCaching;
    }

    public void setHostCaching(String hostCaching) {
        this.hostCaching = hostCaching;
    }

    public String getDiskName() {
        return diskName;
    }

    public void setDiskName(String diskName) {
        this.diskName = diskName;
    }

    public String getLogicalDiskSizeInGB() {
        return logicalDiskSizeInGB;
    }

    public void setLogicalDiskSizeInGB(String logicalDiskSizeInGB) {
        this.logicalDiskSizeInGB = logicalDiskSizeInGB;
    }

    public String getMediaLink() {
        return mediaLink;
    }

    public void setMediaLink(String mediaLink) {
        this.mediaLink = mediaLink;
    }

    public String getDiskLabel() {
        return diskLabel;
    }

    public void setDiskLabel(String diskLabel) {
        this.diskLabel = diskLabel;
    }

    public String getLun() {
        return lun;
    }

    public void setLun(String lun) {
        this.lun = lun;
    }
}
