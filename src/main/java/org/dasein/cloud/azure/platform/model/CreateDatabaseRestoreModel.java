package org.dasein.cloud.azure.platform.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by vmunthiu on 1/14/2015.
 */
/*
<ServiceResource xmlns="http://schemas.microsoft.com/windowsazure" xmlns:i="http://www.w3.org/2001/XMLSchema-instance">
  <SourceDatabaseName>sourceDb</SourceDatabaseName>
  <SourceDatabaseDeletionDate>2013-08-29T21:38:54.5330000Z</SourceDatabaseDeletionDate> <!-- Optional, only applies when restoring a dropped database. -->
  <TargetDatabaseName>targetDb</TargetDatabaseName>
  <TargetUtcPointInTime>2013-09-03T00:00:00.0000000Z</TargetUtcPointInTime> <!-- Optional -->
</ServiceResource>
*/
@XmlRootElement(name="ServiceResource", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class CreateDatabaseRestoreModel {
    @XmlElement(name="SourceDatabaseName", namespace ="http://schemas.microsoft.com/windowsazure")
    private String sourceDatabaseName;
    @XmlElement(name="SourceDatabaseDeletionDate", namespace ="http://schemas.microsoft.com/windowsazure")
    private String sourceDatabaseDeletionDate;
    @XmlElement(name="TargetDatabaseName", namespace ="http://schemas.microsoft.com/windowsazure")
    private String targetDatabaseName;
    @XmlElement(name="TargetUtcPointInTime", namespace ="http://schemas.microsoft.com/windowsazure")
    private String targetUtcPointInTime;

    public String getSourceDatabaseName() {
        return sourceDatabaseName;
    }

    public void setSourceDatabaseName(String sourceDatabaseName) {
        this.sourceDatabaseName = sourceDatabaseName;
    }

    public String getSourceDatabaseDeletionDate() {
        return sourceDatabaseDeletionDate;
    }

    public void setSourceDatabaseDeletionDate(String sourceDatabaseDeletionDate) {
        this.sourceDatabaseDeletionDate = sourceDatabaseDeletionDate;
    }

    public String getTargetDatabaseName() {
        return targetDatabaseName;
    }

    public void setTargetDatabaseName(String targetDatabaseName) {
        this.targetDatabaseName = targetDatabaseName;
    }

    public String getTargetUtcPointInTime() {
        return targetUtcPointInTime;
    }

    public void setTargetUtcPointInTime(String targetUtcPointInTime) {
        this.targetUtcPointInTime = targetUtcPointInTime;
    }
}
