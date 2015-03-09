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

package org.dasein.cloud.azure.compute.image;

import org.dasein.cloud.*;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.compute.ImageCapabilities;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.MachineImageFormat;
import org.dasein.cloud.compute.MachineImageType;
import org.dasein.cloud.compute.VmState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Locale;

/**
 * Describes the capabilities of Azure with respect to Dasein image operations.
 * <p>Created by Danielle Mayne: 5/03/14 14:00 PM</p>
 * @author Danielle Mayne
 * @version 2014.03 initial version
 * @since 2014.03
 */
public class OSImageCapabilities extends AbstractCapabilities<Azure> implements ImageCapabilities{
    public OSImageCapabilities(@Nonnull Azure provider) {
        super(provider);
    }

    @Override
    public boolean canBundle(@Nonnull VmState fromState) throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean canImage(@Nonnull VmState fromState) throws CloudException, InternalException {
        return fromState.equals(VmState.STOPPED);
    }

    @Nonnull
    @Override
    public String getProviderTermForImage(@Nonnull Locale locale, @Nonnull ImageClass cls) {
        if (cls.equals(ImageClass.MACHINE)) {
            return "OS Image";
        }
        return "image";
    }

    @Nonnull
    @Override
    public String getProviderTermForCustomImage(@Nonnull Locale locale, @Nonnull ImageClass cls) {
        if (cls.equals(ImageClass.MACHINE)) {
            return "OS Image";
        }
        return "image";
    }

    @Nullable
    @Override
    public VisibleScope getImageVisibleScope() {
        return null;
    }

    @Nonnull
    @Override
    public Requirement identifyLocalBundlingRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Nonnull
    @Override
    public Iterable<MachineImageFormat> listSupportedFormats() throws CloudException, InternalException {
        return Collections.singletonList(MachineImageFormat.VHD);
    }

    @Nonnull
    @Override
    public Iterable<MachineImageFormat> listSupportedFormatsForBundling() throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Iterable<ImageClass> listSupportedImageClasses() throws CloudException, InternalException {
        return Collections.singletonList(ImageClass.MACHINE);
    }

    @Nonnull
    @Override
    public Iterable<MachineImageType> listSupportedImageTypes() throws CloudException, InternalException {
        return Collections.singletonList(MachineImageType.VOLUME);
    }

    @Override
    public boolean supportsDirectImageUpload() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsImageCapture(@Nonnull MachineImageType type) throws CloudException, InternalException {
        return type.equals(MachineImageType.VOLUME);
    }

    /**
     * Indicates whether copying of an image to another region is supported by this cloud.
     *
     * @return true if you can copy images in this cloud to other regions
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider when checking this capability
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein cloud implementation while check this capability
     */
    @Override
    public boolean supportsImageCopy() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsImageSharing() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsImageSharingWithPublic() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsListingAllRegions() throws CloudException, InternalException {return true;}

    @Override
    public boolean supportsPublicLibrary(@Nonnull ImageClass cls) throws CloudException, InternalException {
        return true;
    }

    @Override
    public boolean imageCaptureDestroysVM() throws CloudException, InternalException{ return true; }
}
