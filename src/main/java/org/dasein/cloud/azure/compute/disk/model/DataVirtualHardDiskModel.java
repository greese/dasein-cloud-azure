/**
 * Copyright (C) 2013-2015 Dell, Inc
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
