package org.dasein.cloud.azure.platform.model;

import javax.xml.bind.annotation.*;

/**
 * Created by Vlad_Munthiu on 11/19/2014.
 */
@XmlRootElement(name="ServerName", namespace ="http://schemas.microsoft.com/sqlazure/2010/12/")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServerNameModel {
    @XmlValue
    private String name;
    @XmlAttribute(name="FullyQualifiedDomainName", namespace ="http://schemas.microsoft.com/sqlazure/2010/12/")
    private String fullyQualifiedName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public void setFullyQualifiedName(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
    }
}
