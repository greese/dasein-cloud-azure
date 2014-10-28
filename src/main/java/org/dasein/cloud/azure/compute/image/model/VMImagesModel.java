package org.dasein.cloud.azure.compute.image.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by Vlad_Munthiu on 8/15/2014.
 */
@XmlRootElement(name="VMImages", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class VMImagesModel
{
    @XmlElement(name="VMImage", namespace ="http://schemas.microsoft.com/windowsazure")
    private List<VMImageModel> vmImages;

    public List<VMImageModel> getVmImages() {
        return vmImages;
    }

    public void setVmImages(List<VMImageModel> vmImages) {
        this.vmImages = vmImages;
    }
}
