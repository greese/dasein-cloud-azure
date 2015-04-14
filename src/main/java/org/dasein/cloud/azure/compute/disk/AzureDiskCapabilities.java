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

package org.dasein.cloud.azure.compute.disk;

import org.dasein.cloud.AbstractCapabilities;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.compute.VolumeCapabilities;
import org.dasein.cloud.compute.VolumeFormat;
import org.dasein.cloud.util.NamingConstraints;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/**
 * Describes the capabilities of Azure with respect to Dasein volume operations.
 * User: daniellemayne
 * Date: 05/03/2014
 * Time: 14:58
 */
public class AzureDiskCapabilities extends AbstractCapabilities<Azure> implements VolumeCapabilities{
    public AzureDiskCapabilities(@Nonnull Azure provider) {
        super(provider);
    }

    @Override
    public boolean canAttach(VmState vmState) throws InternalException, CloudException {
        return true;
    }

    @Override
    public boolean canDetach(VmState vmState) throws InternalException, CloudException {
        return true;
    }

    @Override
    public int getMaximumVolumeCount() throws InternalException, CloudException {
        return 2;
    }

    /**
     * Indicates the maximum IOPS value allowed in the Volume products for the provider.
     *
     * @return the maximum IOPS value
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation determining the limit
     * @throws org.dasein.cloud.CloudException    an error occurred retrieving the limit from the cloud
     */
    @Override
    public int getMaximumVolumeProductIOPS() throws InternalException, CloudException {
        return 0;
    }

    /**
     * Indicates the minimum IOPS value allowed in the Volume products for the provider.
     *
     * @return the minimum IOPS value
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation determining the limit
     * @throws org.dasein.cloud.CloudException    an error occurred retrieving the limit from the cloud
     */
    @Override
    public int getMinimumVolumeProductIOPS() throws InternalException, CloudException {
        return 0;
    }

    /**
     * Indicates the maximum volume size for IOPS Volumes.
     *
     * @return the maximum size of an IOPS volume
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation determining the limit
     * @throws org.dasein.cloud.CloudException    an error occurred retrieving the limit from the cloud
     */
    @Override
    public int getMaximumVolumeSizeIOPS() throws InternalException, CloudException {
        return 0;
    }

    /**
     * Indicates the minimum volume size for IOPS Volumes.
     *
     * @return the minimum size of an IOPS volume
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation determining the limit
     * @throws org.dasein.cloud.CloudException    an error occurred retrieving the limit from the cloud
     */
    @Override
    public int getMinimumVolumeSizeIOPS() throws InternalException, CloudException {
        return 0;
    }

    @Nullable
    @Override
    public Storage<Gigabyte> getMaximumVolumeSize() throws InternalException, CloudException {
        return new Storage<Gigabyte>(1024, Storage.GIGABYTE);
    }

    @Nonnull
    @Override
    public Storage<Gigabyte> getMinimumVolumeSize() throws InternalException, CloudException {
        return new Storage<Gigabyte>(1, Storage.GIGABYTE);
    }

    @Nonnull
    @Override
    public NamingConstraints getVolumeNamingConstraints() throws CloudException, InternalException {
        return null;
    }

    @Nonnull
    @Override
    public String getProviderTermForVolume(@Nonnull Locale locale) {
        return "disk";
    }

    @Nonnull
    @Override
    public Requirement getVolumeProductRequirement() throws InternalException, CloudException {
        return Requirement.NONE;
    }

    @Override
    public boolean isVolumeSizeDeterminedByProduct() throws InternalException, CloudException {
        return false;
    }

    @Nonnull
    @Override
    public Iterable<String> listPossibleDeviceIds(@Nonnull Platform platform) throws InternalException, CloudException {
        ArrayList<String> list = new ArrayList<String>();
        for(int i= 0;i < this.getMaximumVolumeCount();i++){
            list.add(String.valueOf(i));
        }
        return list;
    }

    @Nonnull
    @Override
    public Iterable<VolumeFormat> listSupportedFormats() throws InternalException, CloudException {
        return Collections.singletonList(VolumeFormat.BLOCK);
    }

    @Nonnull
    @Override
    public Requirement requiresVMOnCreate() throws InternalException, CloudException {
        return Requirement.REQUIRED;
    }
}
