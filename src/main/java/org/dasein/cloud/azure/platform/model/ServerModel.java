package org.dasein.cloud.azure.platform.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Vlad_Munthiu on 11/19/2014.
 */
@XmlRootElement(name="Server", namespace ="http://schemas.microsoft.com/sqlazure/2010/12/")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServerModel {
    @XmlElement(name="Name", namespace ="http://schemas.microsoft.com/sqlazure/2010/12/")
    private String name;
    @XmlElement(name="AdministratorLogin", namespace ="http://schemas.microsoft.com/sqlazure/2010/12/")
    private String administratorLogin;
    @XmlElement(name="AdministratorLoginPassword", namespace ="http://schemas.microsoft.com/sqlazure/2010/12/")
    private String administratorLoginPassword;
    @XmlElement(name="Location", namespace ="http://schemas.microsoft.com/sqlazure/2010/12/")
    private String location;

    public String getAdministratorLogin() {
        return administratorLogin;
    }

    public void setAdministratorLogin(String administratorLogin) {
        this.administratorLogin = administratorLogin;
    }

    public String getAdministratorLoginPassword() {
        return administratorLoginPassword;
    }

    public void setAdministratorLoginPassword(String administratorLoginPassword) {
        this.administratorLoginPassword = administratorLoginPassword;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
