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
