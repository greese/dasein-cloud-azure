package org.dasein.cloud.azure.compute.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Vlad_Munthiu on 7/24/2014.
 */
@XmlRootElement(name="UpdateAffinityGroup", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class UpdateAffinityGroupModel {
    @XmlElement(name="Label", namespace ="http://schemas.microsoft.com/windowsazure")
    private String label;
    @XmlElement(name="Description", namespace ="http://schemas.microsoft.com/windowsazure")
    private String description;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
