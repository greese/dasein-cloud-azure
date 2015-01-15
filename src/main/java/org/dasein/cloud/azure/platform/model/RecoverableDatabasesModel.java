package org.dasein.cloud.azure.platform.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by vmunthiu on 1/14/2015.
 */
@XmlRootElement(name="ServiceResources", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class RecoverableDatabasesModel {
    @XmlElement(name="ServiceResource", namespace ="http://schemas.microsoft.com/windowsazure")
    private List<RecoverableDatabaseModel> recoverableDatabaseModels;

    public List<RecoverableDatabaseModel> getRecoverableDatabaseModels() {
        return recoverableDatabaseModels;
    }

    public void setRecoverableDatabaseModels(List<RecoverableDatabaseModel> recoverableDatabaseModels) {
        this.recoverableDatabaseModels = recoverableDatabaseModels;
    }
}
