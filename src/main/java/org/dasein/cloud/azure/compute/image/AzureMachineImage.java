/**
 * Copyright (C) 2013-2014 Dell, Inc
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
    private String azureImageType;

    public AzureMachineImage() { }

    public String getMediaLink() {
        return mediaLink;
    }

    public void setMediaLink(String mediaLink) {
        this.mediaLink = mediaLink;
    }

    public String getAzureImageType() {
        return azureImageType;
    }

    public void setAzureImageType(String azureImageType) {
        this.azureImageType = azureImageType;
    }
}
