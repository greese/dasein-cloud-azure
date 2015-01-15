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
public class ServerServiceResourcesModel {
    @XmlElement(name="ServiceResource", namespace ="http://schemas.microsoft.com/windowsazure")
    private List<ServerServiceResourceModel> serverServiceResourcesModels;

    public List<ServerServiceResourceModel> getServerServiceResourcesModels() {
        return serverServiceResourcesModels;
    }

    public void setServerServiceResourcesModels(List<ServerServiceResourceModel> serverServiceResourcesModels) {
        this.serverServiceResourcesModels = serverServiceResourcesModels;
    }
}
