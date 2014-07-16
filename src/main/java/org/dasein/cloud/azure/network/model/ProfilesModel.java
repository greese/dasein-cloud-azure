package org.dasein.cloud.azure.network.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by Vlad_Munthiu on 6/16/2014.
 */

@XmlRootElement(name="Profiles", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProfilesModel
{
    @XmlElement(name="Profile", namespace ="http://schemas.microsoft.com/windowsazure")
    private List<ProfileModel> profiles;

    public List<ProfileModel> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<ProfileModel> profiles) {
        this.profiles = profiles;
    }
}
