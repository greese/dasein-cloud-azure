package org.dasein.cloud.azure.compute.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Vlad_Munthiu on 7/23/2014.
 */

@XmlRootElement(name="CreateAffinityGroup", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class CreateAffinityGroupModel {

    @XmlElement(name="Name", namespace ="http://schemas.microsoft.com/windowsazure")
    private String name;
    @XmlElement(name="Label", namespace ="http://schemas.microsoft.com/windowsazure")
    private String label;
    @XmlElement(name="Description", namespace ="http://schemas.microsoft.com/windowsazure")
    private String description;
    @XmlElement(name="Location", namespace ="http://schemas.microsoft.com/windowsazure")
    private String location;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
