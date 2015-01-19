package org.dasein.cloud.azure.platform.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by vmunthiu on 11/25/2014.
 */
@XmlRootElement(name="ServiceResources", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class DatabaseServiceResourcesModel {
    @XmlElement(name="ServiceResource", namespace ="http://schemas.microsoft.com/windowsazure")
    private List<DatabaseServiceResourceModel> databaseServiceResourceModels;

    public List<DatabaseServiceResourceModel> getDatabaseServiceResourceModels() {
        return databaseServiceResourceModels;
    }

    public void setDatabaseServiceResourceModels(List<DatabaseServiceResourceModel> databaseServiceResourceModels) {
        this.databaseServiceResourceModels = databaseServiceResourceModels;
    }
}
