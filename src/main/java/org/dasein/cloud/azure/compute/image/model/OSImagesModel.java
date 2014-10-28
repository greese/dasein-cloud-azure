package org.dasein.cloud.azure.compute.image.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by Vlad_Munthiu on 8/18/2014.
 */
@XmlRootElement(name="Images", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class OSImagesModel {
    @XmlElement(name="OSImage", namespace ="http://schemas.microsoft.com/windowsazure")
    private List<OSImageModel> images;

    public List<OSImageModel> getImages() {
        return images;
    }

    public void setImages(List<OSImageModel> images) {
        this.images = images;
    }
}
