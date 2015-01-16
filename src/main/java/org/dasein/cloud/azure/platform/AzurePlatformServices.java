package org.dasein.cloud.azure.platform;

import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.platform.AbstractPlatformServices;

import javax.annotation.Nonnull;

/**
 * Created by Vlad_Munthiu on 11/17/2014.
 */
public class AzurePlatformServices extends AbstractPlatformServices {
    private Azure cloud;

    public AzurePlatformServices(Azure cloud){
        this.cloud = cloud;
    }

    @Override
    public @Nonnull AzureSqlDatabaseSupport getRelationalDatabaseSupport() {
        return new AzureSqlDatabaseSupport(cloud);
    }
}
