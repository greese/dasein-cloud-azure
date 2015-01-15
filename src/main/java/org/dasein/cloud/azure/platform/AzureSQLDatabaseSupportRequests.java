package org.dasein.cloud.azure.platform;

import org.apache.http.client.methods.RequestBuilder;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.platform.model.CreateDatabaseRestoreModel;
import org.dasein.cloud.azure.platform.model.DatabaseServiceResourceModel;
import org.dasein.cloud.azure.platform.model.ServerModel;
import org.dasein.cloud.util.requester.entities.DaseinObjectToXmlEntity;

/**
 * Created by Vlad_Munthiu on 11/19/2014.
 */
public class AzureSQLDatabaseSupportRequests{
    private Azure provider;

    private final String RESOURCE_SERVERS = "https://management.core.windows.net/%s/services/sqlservers/servers?contentview=generic";
    private final String RESOURCE_SERVER = "https://management.core.windows.net/%s/services/sqlservers/servers/%s";
    private final String RESOURCE_DATABASES = "https://management.core.windows.net/%s/services/sqlservers/servers/%s/databases";
    private final String RESOURCE_DATABASE = "https://management.core.windows.net/%s/services/sqlservers/servers/%s/databases/%s";
    private final String RESOURCE_LIST_DATABASES = "https://management.core.windows.net/%s/services/sqlservers/servers/%s/databases?contentview=generic";
    private final String RESOURCE_SUBSCRIPTION_META = "https://management.core.windows.net/%s/services/sqlservers/subscriptioninfo";
    private final String RESOURCE_LIST_RECOVERABLE_DATABASES = "https://management.core.windows.net/%s/services/sqlservers/servers/%s/recoverabledatabases?contentview=generic";
    private final String RESOURCE_RESTORE_DATABASE_OPERATIONS = "https://management.core.windows.net/%s/services/sqlservers/servers/%s/restoredatabaseoperations";

    public AzureSQLDatabaseSupportRequests(Azure provider){
        this.provider = provider;
    }

    public RequestBuilder createServer(ServerModel serverToCreate){
        RequestBuilder requestBuilder = RequestBuilder.post();
        addAzureCommonHeaders(requestBuilder);
        requestBuilder.setUri(String.format(RESOURCE_SERVERS, this.provider.getContext().getAccountNumber()));
        requestBuilder.setEntity(new DaseinObjectToXmlEntity<ServerModel>(serverToCreate));
        return requestBuilder;
    }

    public RequestBuilder listServers(){
        RequestBuilder requestBuilder = RequestBuilder.get();
        addAzureCommonHeaders(requestBuilder);
        requestBuilder.setUri(String.format(RESOURCE_SERVERS, this.provider.getContext().getAccountNumber()));
        return requestBuilder;
    }

    public RequestBuilder deleteServer(String serverName){
        RequestBuilder requestBuilder = RequestBuilder.delete();
        addAzureCommonHeaders(requestBuilder);
        requestBuilder.setUri(String.format(RESOURCE_SERVER, this.provider.getContext().getAccountNumber(), serverName));
        return requestBuilder;
    }

    public RequestBuilder createDatabase(String serverName, DatabaseServiceResourceModel dbToCreate){
        RequestBuilder requestBuilder = RequestBuilder.post();
        addAzureCommonHeaders(requestBuilder);
        requestBuilder.setUri(String.format(RESOURCE_DATABASES, this.provider.getContext().getAccountNumber(), serverName));
        requestBuilder.setEntity(new DaseinObjectToXmlEntity<DatabaseServiceResourceModel>(dbToCreate));
        return requestBuilder;
    }

    public RequestBuilder createDatabaseFromBackup(String serverName, CreateDatabaseRestoreModel createDatabaseRestoreModel){
        RequestBuilder requestBuilder = RequestBuilder.post();
        addAzureCommonHeaders(requestBuilder);
        requestBuilder.setUri(String.format(RESOURCE_RESTORE_DATABASE_OPERATIONS, this.provider.getContext().getAccountNumber(), serverName));
        requestBuilder.setEntity(new DaseinObjectToXmlEntity<CreateDatabaseRestoreModel>(createDatabaseRestoreModel));
        return requestBuilder;
    }

    public RequestBuilder listDatabases(String serverName){
        RequestBuilder requestBuilder = RequestBuilder.get();
        addAzureCommonHeaders(requestBuilder);
        requestBuilder.setUri(String.format(RESOURCE_LIST_DATABASES, this.provider.getContext().getAccountNumber(), serverName));
        return requestBuilder;
    }

    public RequestBuilder deleteDatabase(String serverName, String databaseName){
        RequestBuilder requestBuilder = RequestBuilder.delete();
        addAzureCommonHeaders(requestBuilder);
        requestBuilder.setUri(String.format(RESOURCE_DATABASE, this.provider.getContext().getAccountNumber(), serverName, databaseName));
        return requestBuilder;
    }
    public RequestBuilder subscriptionMetaRequest(){
        RequestBuilder requestBuilder = RequestBuilder.get();
        addAzureCommonHeaders(requestBuilder);
        requestBuilder.setUri(String.format(RESOURCE_SUBSCRIPTION_META, this.provider.getContext().getAccountNumber()));
        return requestBuilder;
    }

    public RequestBuilder getDatabase(String serverName, String databaseName){
        RequestBuilder requestBuilder = RequestBuilder.get();
        addAzureCommonHeaders(requestBuilder);
        requestBuilder.setUri(String.format(RESOURCE_DATABASE, this.provider.getContext().getAccountNumber(), serverName, databaseName));
        return requestBuilder;
    }

    public RequestBuilder getRecoverableDatabases(String serverName){
        RequestBuilder requestBuilder = RequestBuilder.get();
        addAzureCommonHeaders(requestBuilder);
        requestBuilder.setUri(String.format(RESOURCE_LIST_RECOVERABLE_DATABASES, this.provider.getContext().getAccountNumber(), serverName));
        return requestBuilder;
    }


    private void addAzureCommonHeaders(RequestBuilder requestBuilder){
        requestBuilder.addHeader("x-ms-version", "2012-03-01");
        requestBuilder.addHeader("Content-Type", "application/xml;charset=UTF-8");
    }
}
