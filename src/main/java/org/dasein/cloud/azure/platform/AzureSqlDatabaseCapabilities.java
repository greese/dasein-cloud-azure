package org.dasein.cloud.azure.platform;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.platform.RelationalDatabaseCapabilities;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Created by vmunthiu on 1/15/2015.
 */
public class AzureSqlDatabaseCapabilities implements RelationalDatabaseCapabilities {
    @Nonnull
    @Override
    public String getProviderTermForBackup(Locale locale) {
        return null;
    }

    @Nonnull
    @Override
    public String getProviderTermForDatabase(Locale locale) {
        return null;
    }

    @Nonnull
    @Override
    public String getProviderTermForSnapshot(Locale locale) {
        return null;
    }

    @Override
    public boolean supportsFirewallRules() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsHighAvailability() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsLowAvailability() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsMaintenanceWindows() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsAlterDatabase() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsSnapshots() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsDatabaseBackups() throws CloudException, InternalException {
        return true;
    }

    @Override
    public boolean supportsScheduledDatabaseBackups() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsDemandBackups() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsRestoreBackup() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsDeleteBackup() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsBackupConfigurations() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isSupportsFirewallRules() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isSupportsHighAvailability() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isSupportsLowAvailability() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isSupportsMaintenanceWindows() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isSupportsAlterDatabase() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isSupportsSnapshots() throws CloudException, InternalException {
        return false;
    }

    @Nonnull
    @Override
    public String getAccountNumber() {
        return null;
    }

    @Nonnull
    @Override
    public String getRegionId() {
        return null;
    }
}
