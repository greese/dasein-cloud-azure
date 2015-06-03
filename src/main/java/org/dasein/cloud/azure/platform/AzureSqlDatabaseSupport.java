package org.dasein.cloud.azure.platform;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.http.client.methods.HttpUriRequest;
import org.dasein.cloud.*;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureRequester;
import org.dasein.cloud.azure.IpUtils;
import org.dasein.cloud.azure.platform.model.*;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.platform.*;
import org.dasein.cloud.util.requester.DriverToCoreMapper;
import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import javax.xml.crypto.Data;
import java.util.*;

/**
 * Created by Vlad_Munthiu on 10/28/2014.
 */

public class AzureSqlDatabaseSupport implements RelationalDatabaseSupport {
    private Azure provider;

    public AzureSqlDatabaseSupport(Azure provider) {
        this.provider = provider;
    }

    @Override
    public void addAccess(String providerDatabaseId, String sourceCidr) throws CloudException, InternalException {
        Database database = getDatabase(providerDatabaseId);
        if(database == null)
            throw new InternalException("Invaid database provider Id");

        if(sourceCidr == null)
            throw new InternalException("Invalid parameter sourceCirs. The parameter cannot be null");

        List<String> ruleParts = Arrays.asList(sourceCidr.split("::"));
        if(ruleParts.size() != 2)
            throw new InternalException("Invalid parameter sourceCidr");

        IpUtils.IpRange ipRange = new IpUtils.IpRange(ruleParts.get(0), ruleParts.get(1));

        ServerServiceResourceModel firewallRule = new ServerServiceResourceModel();
        firewallRule.setName(String.format("%s_%s", database.getName(), new Date().getTime()));
        firewallRule.setStartIpAddress(ipRange.getLow().toDotted());
        firewallRule.setEndIpAddress(ipRange.getHigh().toDotted());

        String serverName = Arrays.asList(database.getProviderDatabaseId().split(":")).get(0);

        HttpUriRequest createRuleRequest = new AzureSQLDatabaseSupportRequests(provider).addFirewallRule(serverName, firewallRule).build();
        new AzureRequester(provider, createRuleRequest).execute();
    }

    @Override
    public void alterDatabase(String providerDatabaseId, boolean applyImmediately, String productSize, int storageInGigabytes, String configurationId, String newAdminUser, String newAdminPassword, int newPort, int snapshotRetentionInDays, TimeWindow preferredMaintenanceWindow, TimeWindow preferredBackupWindow) throws CloudException, InternalException {

    }

    @Override
    public String createFromScratch(String dataSourceName, DatabaseProduct product, String databaseVersion, String withAdminUser, String withAdminPassword, int hostPort) throws CloudException, InternalException {
        if(!isValidAdminUserName(withAdminUser)){
            throw new InternalException("Invalid admin user name");
        }

        if(product == null && product.getName() == null){
            throw new InternalException("Cannot create database. Database product or database product name cannot be empty");
        }

        ServerModel serverToCreate = new ServerModel();
        serverToCreate.setAdministratorLogin(withAdminUser);
        serverToCreate.setAdministratorLoginPassword(withAdminPassword);
        serverToCreate.setLocation(this.provider.getContext().getRegionId());

        HttpUriRequest createServerRequest =  new AzureSQLDatabaseSupportRequests(provider).createServer(serverToCreate).build();
        ServerNameModel resultServerName = new AzureRequester(provider, createServerRequest).withXmlProcessor(ServerNameModel.class).execute();

        try {
            String productGUID = getProductGUID(product);

            DatabaseServiceResourceModel dbToCreate = new DatabaseServiceResourceModel();
            dbToCreate.setName(dataSourceName);
            dbToCreate.setEdition(product.getName());
            dbToCreate.setServiceObjectiveId(productGUID);

            HttpUriRequest createDatabaseRequest = new AzureSQLDatabaseSupportRequests(provider).createDatabase(resultServerName.getName(), dbToCreate).build();
            DatabaseServiceResourceModel database = new AzureRequester(provider, createDatabaseRequest).withXmlProcessor(DatabaseServiceResourceModel.class).execute();
            return String.format("%s:%s", resultServerName.getName(), database.getName());
        }
        catch (Exception ex) {
            //delete server
            HttpUriRequest deleteServerRequest = new AzureSQLDatabaseSupportRequests(provider).deleteServer(resultServerName.getName()).build();
            new AzureRequester(provider, deleteServerRequest).execute();
            throw new CloudException("Could not create database. " + ex.getMessage());
        }
    }

    private boolean isValidAdminUserName(String name){
        List<String> illegalUserName = Arrays.asList("administrator", "admin", "sa", "root", "dbmanager", "loginmanager", "dbo", "guest", "public");

        if(illegalUserName.contains(name)){
            return false;
        }

        return true;
    }

    @Override
    public String createFromLatest(String dataSourceName, String providerDatabaseId, String productSize, String providerDataCenterId, int hostPort) throws InternalException, CloudException {
        return null;
    }

    @Override
    public String createFromSnapshot(String dataSourceName, String providerDatabaseId, String providerDbSnapshotId, String productSize, String providerDataCenterId, int hostPort) throws CloudException, InternalException {
        return null;
    }

    @Override
    public String createFromTimestamp(String dataSourceName, String providerDatabaseId, long beforeTimestamp, String productSize, String providerDataCenterId, int hostPort) throws InternalException, CloudException {
        return null;
    }

    /**
     * Provides access to meta-data about RDS capabilities in the current region of this cloud.
     *
     * @return a description of the features supported by this region of this cloud
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     */
    @Nonnull
    @Override
    public RelationalDatabaseCapabilities getCapabilities() throws InternalException, CloudException {
        return new AzureSqlDatabaseCapabilities();
    }

    @Override
    public DatabaseConfiguration getConfiguration(String providerConfigurationId) throws CloudException, InternalException {
        return null;
    }

    @Override
    public Database getDatabase(final String providerDatabaseId) throws CloudException, InternalException {
        if(providerDatabaseId == null)
            throw new InternalException("Provider database id cannot be null");

        final List<String> providerDatabaseIdParts = Arrays.asList(providerDatabaseId.split(":"));
        if(providerDatabaseIdParts.size() != 2)
            throw new InternalException("Invalid name for the provider database id");

        HttpUriRequest listServersRequest = new AzureSQLDatabaseSupportRequests(this.provider).listServersNonGen().build();

        ServersModel serversModel = new AzureRequester(provider, listServersRequest).withXmlProcessor(ServersModel.class).execute();

        if(serversModel == null || serversModel.getServers() == null)
            return null;

        Object serverFound = CollectionUtils.find(serversModel.getServers(), new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                return providerDatabaseIdParts.get(0).equalsIgnoreCase(((ServerModel)object).getName());
            }
        });

        if(serverFound == null)
            return null;

        HttpUriRequest httpUriRequest = new AzureSQLDatabaseSupportRequests(provider)
                .getDatabase(providerDatabaseIdParts.get(0), providerDatabaseIdParts.get(1)).build();

        Database database = new AzureRequester(provider, httpUriRequest).withXmlProcessor(new DriverToCoreMapper<DatabaseServiceResourceModel, Database>() {
            @Override
            public Database mapFrom(DatabaseServiceResourceModel entity) {
                return databaseFrom(entity, providerDatabaseIdParts.get(0));
            }
        }, DatabaseServiceResourceModel.class).execute();

        //getDatabase is a global search so set server location for database
        database.setProviderRegionId(((ServerModel) serverFound).getLocation());
        return database;
    }

    @Override
    public Iterable<DatabaseEngine> getDatabaseEngines() throws CloudException, InternalException {
        return Arrays.asList(DatabaseEngine.SQLSERVER_EE);
    }

    @Override
    public String getDefaultVersion(@Nonnull DatabaseEngine forEngine) throws CloudException, InternalException {
        return "2.0";
    }

    @Nonnull
    @Override
    public Iterable<String> getSupportedVersions(@Nonnull DatabaseEngine forEngine) throws CloudException, InternalException {
        return Arrays.asList("2.0");
    }

    /**
     * List supported database products
     *
     * @param forEngine database engine, e.g. MySQL, SQL Server EE, etc.
     * @return iteration of the database products supported by the engine
     * @throws org.dasein.cloud.CloudException
     * @throws org.dasein.cloud.InternalException
     * @since 2014.08 for consistency
     */
    @Nonnull
    @Override
    public Iterable<DatabaseProduct> listDatabaseProducts(@Nonnull DatabaseEngine forEngine) throws CloudException, InternalException {
        if(forEngine == null)
            throw new InternalException("Please specify the DatabaseEngine for which you want to retrieve the products.");

        if(!forEngine.name().toString().equalsIgnoreCase("sqlserver_ee"))
            return Arrays.asList();

        ServerServiceResourceModel.Version versionResult = getSubscriptionVersionProducts();

        final ArrayList<DatabaseProduct> products = new ArrayList<DatabaseProduct>();
        CollectionUtils.forAllDo(versionResult.getEditions(), new Closure() {
            @Override
            public void execute(Object input) {
                ServerServiceResourceModel.Edition edition = (ServerServiceResourceModel.Edition)input;
                for(ServerServiceResourceModel.ServiceLevelObjective serviceLevelObjective : edition.getServiceLevelObjectives()){
                    DatabaseProduct product = new DatabaseProduct(serviceLevelObjective.getName(), edition.getName());
                    product.setProviderDataCenterId(provider.getDataCenterId(provider.getContext().getRegionId()));
                    product.setEngine(DatabaseEngine.SQLSERVER_EE);
                    product.setLicenseModel(DatabaseLicenseModel.LICENSE_INCLUDED);
                    products.add(product);
                }
            }
        });

        return products;
    }

    @Override
    public DatabaseSnapshot getSnapshot(String providerDbSnapshotId) throws CloudException, InternalException {
        return null;
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return true;
    }

    @Override
    public Iterable<String> listAccess(String toProviderDatabaseId) throws CloudException, InternalException {
        final ArrayList<String> rules = new ArrayList<String>();

        Database database = getDatabase(toProviderDatabaseId);
        if(database == null)
            throw new InternalException("Invaid database provider Id");

        String serverName = Arrays.asList(database.getProviderDatabaseId().split(":")).get(0);

        HttpUriRequest listRulesRequest = new AzureSQLDatabaseSupportRequests(provider).listFirewallRules(serverName).build();
        ServerServiceResourcesModel rulesModel = new AzureRequester(provider, listRulesRequest).withXmlProcessor(ServerServiceResourcesModel.class).execute();

        if(rulesModel == null || rulesModel.getServerServiceResourcesModels() == null)
            return rules;

        CollectionUtils.forAllDo(rulesModel.getServerServiceResourcesModels(), new Closure() {
            @Override
            public void execute(Object input) {
                ServerServiceResourceModel firewallRule = (ServerServiceResourceModel) input;
                String endIpAddress = firewallRule.getEndIpAddress();
                if(endIpAddress == null)
                    endIpAddress = firewallRule.getStartIpAddress();

                rules.add(String.format("%s::%s::%s", firewallRule.getName(), firewallRule.getStartIpAddress(), endIpAddress));
            }
        });

        return rules;
    }

    @Override
    public Iterable<DatabaseConfiguration> listConfigurations() throws CloudException, InternalException {
        return null;
    }

    @Nonnull
    @Override
    public Iterable<ResourceStatus> listDatabaseStatus() throws CloudException, InternalException {
        return null;
    }

    @Override
    public Iterable<Database> listDatabases() throws CloudException, InternalException {
        ArrayList<Database> databases = new ArrayList<Database>();

        HttpUriRequest httpUriRequest = new AzureSQLDatabaseSupportRequests(this.provider).listServersNonGen().build();

        ServersModel serversModel = new AzureRequester(provider, httpUriRequest).withXmlProcessor(ServersModel.class).execute();

        if(serversModel == null || serversModel.getServers() == null)
            return databases;

        List<ServerModel> servers = serversModel.getServers();
        CollectionUtils.filter(servers, new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                return provider.getContext().getRegionId().equalsIgnoreCase(((ServerModel)object).getLocation());
            }
        });

        for (ServerModel serverModel : servers){
            HttpUriRequest serverHttpUriRequest = new AzureSQLDatabaseSupportRequests(this.provider).listDatabases(serverModel.getName()).build();
            DatabaseServiceResourcesModel databaseServiceResourcesModel =
                    new AzureRequester(provider, serverHttpUriRequest).withXmlProcessor(DatabaseServiceResourcesModel.class).execute();

            if (databaseServiceResourcesModel == null)
                continue;

            for (DatabaseServiceResourceModel databaseModel : databaseServiceResourcesModel.getDatabaseServiceResourceModels()){
                if(!databaseModel.getName().equalsIgnoreCase("master"))
                    databases.add(databaseFrom(databaseModel, serverModel.getName()));
            }
        }

        return databases;
    }

    private Database databaseFrom(DatabaseServiceResourceModel databaseServiceResourceModel, String serverName){
        if(databaseServiceResourceModel == null)
            return null;

        Database database = new Database();
        database.setName(databaseServiceResourceModel.getName());
        database.setProviderDatabaseId(String.format("%s:%s", serverName, databaseServiceResourceModel.getName()));
        database.setProviderRegionId(provider.getContext().getRegionId());
        database.setProviderOwnerId(provider.getContext().getAccountNumber());
        database.setAllocatedStorageInGb(Integer.parseInt(databaseServiceResourceModel.getMaxSizeGB()));
        database.setEngine(DatabaseEngine.SQLSERVER_EE);
        database.setCreationTimestamp(new DateTime(databaseServiceResourceModel.getCreationDate()).getMillis());
        database.setCurrentState(databaseServiceResourceModel.getState().equalsIgnoreCase("normal") ? DatabaseState.AVAILABLE : DatabaseState.UNKNOWN);
        database.setProductSize(databaseServiceResourceModel.getEdition());
        database.setHostName(String.format("%s.database.windows.net", serverName));
        database.setHostPort(1433);
        return database;
    }

    @Override
    public Collection<ConfigurationParameter> listParameters(String forProviderConfigurationId) throws CloudException, InternalException {
        return null;
    }

    @Override
    public Iterable<DatabaseSnapshot> listSnapshots(String forOptionalProviderDatabaseId) throws CloudException, InternalException {
        return null;
    }

    @Override
    public void removeConfiguration(String providerConfigurationId) throws CloudException, InternalException {

    }

    @Override
    public void removeDatabase(String providerDatabaseId) throws CloudException, InternalException {
        if(providerDatabaseId == null)
            throw new InternalException("Provider database id cannot be null");

        List<String> providerDatabaseIdParts = Arrays.asList(providerDatabaseId.split(":"));
        if(providerDatabaseIdParts.size() != 2)
            throw new InternalException("Invalid name for the provider database id");

        HttpUriRequest deleteDatabaseRequest = new AzureSQLDatabaseSupportRequests(provider).deleteDatabase(providerDatabaseIdParts.get(0), providerDatabaseIdParts.get(1)).build();

        new AzureRequester(provider, deleteDatabaseRequest).execute();

        HttpUriRequest serverHttpUriRequest = new AzureSQLDatabaseSupportRequests(this.provider).listDatabases(providerDatabaseIdParts.get(0)).build();
        DatabaseServiceResourcesModel databaseServiceResourcesModel =
                new AzureRequester(provider, serverHttpUriRequest).withXmlProcessor(DatabaseServiceResourcesModel.class).execute();

        if(databaseServiceResourcesModel == null || databaseServiceResourcesModel.getDatabaseServiceResourceModels().size() > 1)
            return;

        if(databaseServiceResourcesModel.getDatabaseServiceResourceModels().size() == 1 && !databaseServiceResourcesModel.getDatabaseServiceResourceModels().get(0).getName().equalsIgnoreCase("master"))
            return;

        HttpUriRequest deleteServerRequest = new AzureSQLDatabaseSupportRequests(provider).deleteServer(providerDatabaseIdParts.get(0)).build();
        new AzureRequester(provider, deleteServerRequest).execute();
    }

    @Override
    public void removeSnapshot(String providerSnapshotId) throws CloudException, InternalException {

    }

    @Override
    public void resetConfiguration(String providerConfigurationId, String... parameters) throws CloudException, InternalException {

    }

    @Override
    public void restart(String providerDatabaseId, boolean blockUntilDone) throws CloudException, InternalException {

    }

    @Override
    public void revokeAccess(String providerDatabaseId, String sourceCide) throws CloudException, InternalException {
        Database database = getDatabase(providerDatabaseId);
        if(database == null)
            throw new InternalException("Invaid database provider Id");

        List<String> ruleParts = Arrays.asList(sourceCide.split("::"));
        if(ruleParts.size() != 3)
            throw new InternalError("Invalid parameter sourceCidr");

        String ruleName = ruleParts.get(0);
        String serverName = Arrays.asList(database.getProviderDatabaseId().split(":")).get(0);

        HttpUriRequest deleteRuleRequest = new AzureSQLDatabaseSupportRequests(provider).deleteFirewallRule(serverName, ruleName).build();
        new AzureRequester(provider, deleteRuleRequest).execute();
    }

    @Override
    public void updateConfiguration(String providerConfigurationId, ConfigurationParameter... parameters) throws CloudException, InternalException {

    }

    @Override
    public DatabaseSnapshot snapshot(String providerDatabaseId, String name) throws CloudException, InternalException {
        return null;
    }

    @Override
    public DatabaseBackup getUsableBackup(String providerDbId, String beforeTimestamp) throws CloudException, InternalException {
        return null;
    }

    @Override
    public Iterable<DatabaseBackup> listBackups(final String forOptionalProviderDatabaseId) throws CloudException, InternalException {
        final ArrayList<DatabaseBackup> backups = new ArrayList<DatabaseBackup>();

        if(forOptionalProviderDatabaseId == null) {
            HttpUriRequest httpUriRequest = new AzureSQLDatabaseSupportRequests(this.provider).listServers().build();
            ServerServiceResourcesModel serversModel = new AzureRequester(provider, httpUriRequest).withXmlProcessor(ServerServiceResourcesModel.class).execute();

            if (serversModel == null)
                return backups;

            CollectionUtils.forAllDo(serversModel.getServerServiceResourcesModels(), new Closure() {
                @Override
                public void execute(Object input) {
                    final ServerServiceResourceModel serverModel = (ServerServiceResourceModel) input;
                    backups.addAll(getBackupsForServer(serverModel.getName()));
                }
            });
        } else {
            List<String> providerDBIdParts = Arrays.asList(forOptionalProviderDatabaseId.split(":"));
            if(providerDBIdParts.size() != 2)
                throw new InternalException("Invalid provider database id");

            final String serverName = providerDBIdParts.get(0);
            final String databaseName = providerDBIdParts.get(1);

            backups.addAll(getBackupsForServer(serverName));
            CollectionUtils.filter(backups, new Predicate() {
                @Override
                public boolean evaluate(Object object) {
                    DatabaseBackup backup = (DatabaseBackup) object;
                    return backup.getProviderDatabaseId().equalsIgnoreCase(String.format("%s:%s", serverName, databaseName));
                }
            });
        }

        return backups;
    }

    private ArrayList<DatabaseBackup> getBackupsForServer(final String serverName){
        final ArrayList<DatabaseBackup> backups = new ArrayList<DatabaseBackup>();
        try {
            HttpUriRequest serverListBackupsRequest = new AzureSQLDatabaseSupportRequests(provider).getRecoverableDatabases(serverName).build();
            RecoverableDatabasesModel recoverableDatabasesModel = new AzureRequester(provider, serverListBackupsRequest).withXmlProcessor(RecoverableDatabasesModel.class).execute();

            CollectionUtils.forAllDo(recoverableDatabasesModel.getRecoverableDatabaseModels(), new Closure() {
                @Override
                public void execute(Object input) {
                    RecoverableDatabaseModel recoverableDatabaseModel = (RecoverableDatabaseModel) input;

                    DatabaseBackup databaseBackup = new DatabaseBackup();
                    databaseBackup.setProviderDatabaseId(String.format("%s:%s", serverName, getDatabaseName(recoverableDatabaseModel)));
                    databaseBackup.setProviderOwnerId(provider.getContext().getAccountNumber());
                    databaseBackup.setProviderRegionId(provider.getContext().getRegionId());
                    databaseBackup.setCurrentState(DatabaseBackupState.AVAILABLE);
                    databaseBackup.setProviderBackupId(recoverableDatabaseModel.getName());
                    backups.add(databaseBackup);
                }
            });

        } catch (CloudException e) {
            e.printStackTrace();
        }

        return backups;
    }

    private String getDatabaseName(RecoverableDatabaseModel recoverableDatabaseModel) {
        if(recoverableDatabaseModel.getName() == null || recoverableDatabaseModel.getName().isEmpty())
            return null;

        //assume is an auto-generated name in the following format: AutomatedSqlExport_databaseName_20150114T100004Z
        if(recoverableDatabaseModel.getName().contains("_")){
            List<String> nameParts = Arrays.asList(recoverableDatabaseModel.getName().split("_"));
            return nameParts.get(1);
        }

        //assume is a backup from a deleted database
        return recoverableDatabaseModel.getName();
    }

    @Override
    public void createFromBackup(DatabaseBackup backup, String databaseCloneToName) throws CloudException, InternalException {
        if(backup == null)
            throw new InternalException("DatabaseBackup parameter cannot be null");

        if(backup.getProviderDatabaseId() == null || !backup.getProviderDatabaseId().contains(":"))
            throw new InternalException("Invalid provider database id for the specified database backup");

        List<String> providerDBIdParts = Arrays.asList(backup.getProviderDatabaseId().split(":"));
        String serverName = providerDBIdParts.get(0);
        String databaseName = providerDBIdParts.get(1);

        CreateDatabaseRestoreModel createDatabaseRestoreModel = new CreateDatabaseRestoreModel();
        createDatabaseRestoreModel.setSourceDatabaseName(databaseName);
        createDatabaseRestoreModel.setTargetDatabaseName(databaseCloneToName);

        HttpUriRequest createFromBackupRequest =
                new AzureSQLDatabaseSupportRequests(provider).createDatabaseFromBackup(serverName, createDatabaseRestoreModel).build();
        new AzureRequester(provider, createFromBackupRequest).execute();
    }

    @Override
    public void removeBackup(DatabaseBackup backup) throws CloudException, InternalException {

    }

    @Override
    public void restoreBackup(DatabaseBackup backup) throws CloudException, InternalException {

    }

    /**
     * Maps the specified Dasein Cloud service action to an identifier specific to an underlying cloud. If there is
     * no mapping that makes any sense, the method will return an empty array.
     *
     * @param action the Dasein Cloud service action
     * @return a list of cloud-specific IDs (e.g. iam:ListGroups) representing an action with this cloud provider
     */
    @Nonnull
    @Override
    public String[] mapServiceAction(@Nonnull ServiceAction action) {
        return new String[0];
    }

    private ServerServiceResourceModel.Version getSubscriptionVersionProducts() throws CloudException {
        HttpUriRequest subscriptionMetaRequest = new AzureSQLDatabaseSupportRequests(provider).subscriptionMetaRequest().build();
        ServerServiceResourceModel serverServiceResourceModel = new AzureRequester(this.provider, subscriptionMetaRequest).withXmlProcessor(ServerServiceResourceModel.class).execute();

        return (ServerServiceResourceModel.Version) CollectionUtils.find(serverServiceResourceModel.getVersions(), new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                return ((ServerServiceResourceModel.Version) object).getName().equalsIgnoreCase("2.0");
            }
        });
    }

    private String getProductGUID(final DatabaseProduct product) throws CloudException {
        ServerServiceResourceModel.Version versionResult = getSubscriptionVersionProducts();

        ServerServiceResourceModel.Edition edition = (ServerServiceResourceModel.Edition) CollectionUtils.find(versionResult.getEditions(), new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                return ((ServerServiceResourceModel.Edition) object).getName().equalsIgnoreCase(product.getName());
            }
        });

        ServerServiceResourceModel.ServiceLevelObjective serviceLevelObjective = (ServerServiceResourceModel.ServiceLevelObjective)CollectionUtils.find(edition.getServiceLevelObjectives(), new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                return ((ServerServiceResourceModel.ServiceLevelObjective) object).getName().equalsIgnoreCase(product.getProductSize());
            }
        });

        return serviceLevelObjective.getId();
    }

    @Override
    public void removeTags(@Nonnull String providerDatabaseId, @Nonnull Tag... tags) throws CloudException, InternalException{}

    @Override
    public void removeTags(@Nonnull String[] providerDatabaseIds, @Nonnull Tag ... tags) throws CloudException, InternalException{}

    @Override
    public void updateTags(@Nonnull String providerDatabaseId, @Nonnull Tag... tags) throws CloudException, InternalException{}

    @Override
    public void updateTags(@Nonnull String[] providerDatabaseIds, @Nonnull Tag... tags) throws CloudException, InternalException{}

    @Override
    public void setTags( @Nonnull String providerDatabaseId, @Nonnull Tag... tags ) throws CloudException, InternalException{}

    @Override
    public void setTags( @Nonnull String[] providerDatabaseIds, @Nonnull Tag... tags ) throws CloudException, InternalException{}
}
