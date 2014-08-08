package org.dasein.cloud.azure.compute.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by Vlad_Munthiu on 7/24/2014.
 */
@XmlRootElement(name="AffinityGroups", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class AffinityGroupsModel {
    @XmlElement(name="AffinityGroup", namespace ="http://schemas.microsoft.com/windowsazure")
    private List<AffinityGroupModel> affinityGroups;

    public List<AffinityGroupModel> getAffinityGroups() {
        return affinityGroups;
    }

    public void setAffinityGroups(List<AffinityGroupModel> affinityGroups) {
        this.affinityGroups = affinityGroups;
    }
}
