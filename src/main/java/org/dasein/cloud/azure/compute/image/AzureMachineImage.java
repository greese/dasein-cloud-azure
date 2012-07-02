package org.dasein.cloud.azure.compute.image;

import org.dasein.cloud.compute.MachineImage;

/**
 * Created by IntelliJ IDEA.
 * User: greese
 * Date: 5/24/12
 * Time: 8:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class AzureMachineImage extends MachineImage {
    private String mediaLink;

    public AzureMachineImage() { }

    public String getMediaLink() {
        return mediaLink;
    }

    public void setMediaLink(String mediaLink) {
        this.mediaLink = mediaLink;
    }
}
