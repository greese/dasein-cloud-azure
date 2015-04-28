package org.dasein.cloud.azure.platform;

import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.platform.AbstractPlatformServices;

import javax.annotation.Nonnull;

/**
 * Created by Vlad_Munthiu on 11/17/2014.
 */
public class AzurePlatformServices extends AbstractPlatformServices<Azure> {
    public AzurePlatformServices(Azure provider) {
        super(provider);
    }

    @Override
    public @Nonnull AzureSqlDatabaseSupport getRelationalDatabaseSupport() {
        return new AzureSqlDatabaseSupport(getProvider());
    }
}
