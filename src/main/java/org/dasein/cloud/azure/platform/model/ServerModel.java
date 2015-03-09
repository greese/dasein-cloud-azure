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
