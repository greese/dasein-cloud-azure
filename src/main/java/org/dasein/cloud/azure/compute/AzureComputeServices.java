/**
 * Copyright (C) 2012 enStratus Networks Inc
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

package org.dasein.cloud.azure.compute;

import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.compute.disk.AzureDisk;
import org.dasein.cloud.azure.compute.image.AzureOSImage;
import org.dasein.cloud.azure.compute.vm.AzureVM;
import org.dasein.cloud.compute.AbstractComputeServices;

import javax.annotation.Nonnull;

/**
 * Implements interaction between Dasein Cloud services and the Microsoft Azure cloud services that support
 * the Dasein Cloud services.
 * @author George Reese (george.reese@imaginary.com)
 * @since 2012.04.1
 * @version 2012.04.1
 */
public class AzureComputeServices extends AbstractComputeServices {
    private Azure provider;

    public AzureComputeServices(@Nonnull Azure provider) { this.provider = provider; }

    @Override
    public AzureOSImage getImageSupport() {
        return new AzureOSImage(provider);
    }

    @Override
    public AzureVM getVirtualMachineSupport() {
        return new AzureVM(provider);
    }
    
    @Override
    public AzureDisk getVolumeSupport() {
        return new AzureDisk(provider);
    }
}
