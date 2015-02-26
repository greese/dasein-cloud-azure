package org.dasein.cloud.azure.platform;

import org.apache.http.client.methods.RequestBuilder;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.platform.model.CreateDatabaseRestoreModel;
import org.dasein.cloud.azure.platform.model.DatabaseServiceResourceModel;
import org.dasein.cloud.azure.platform.model.ServerModel;
import org.dasein.cloud.azure.platform.model.ServerServiceResourceModel;
import org.dasein.cloud.util.requester.entities.DaseinObjectToXmlEntity;

import java.net.*;

/**
 * Created by Vlad_Munthiu on 11/19/2014.
 */
public class AzureSQLDatabaseSupportRequests{
    private Azure provider;

    private final String RESOURCE_SERVERS = "https://management.core.windows.net/%s/services/sqlservers/servers?contentview=generic";
    private final String RESOURCE_SERVERS_NONGEN = "https://management.core.windows.net/%s/services/sqlservers/servers";
    private final String RESOURCE_SERVER = "https://management.core.windows.net/%s/services/sqlservers/servers/%s";
    private final String RESOURCE_DATABASES = "https://management.core.windows.net/%s/services/sqlservers/servers/%s/databases";
    private final String RESOURCE_DATABASE = "https://management.core.windows.net/%s/services/sqlservers/servers/%s/databases/%s";
    private final String RESOURCE_LIST_DATABASES = "https://management.core.windows.net/%s/services/sqlservers/servers/%s/databases?contentview=generic";
    private final String RESOURCE_SUBSCRIPTION_META = "https://management.core.windows.net/%s/services/sqlservers/subscriptioninfo";
    private final String RESOURCE_LIST_RECOVERABLE_DATABASES = "https://management.core.windows.net/%s/services/sqlservers/servers/%s/recoverabledatabases?contentview=generic";
    private final String RESOURCE_RESTORE_DATABASE_OPERATIONS = "https://management.core.windows.net/%s/services/sqlservers/servers/%s/restoredatabaseoperations";
    private final String RESOURCE_SERVER_FIREWALL = "https://management.core.windows.net/%s/services/sqlservers/servers/%s/firewallrules";
    private final String RESOURCE_FIREWALL_RULE = "https://management.core.windows.net/%s/services/sqlservers/servers/%s/firewallrules/%s";

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

    public RequestBuilder listServersNonGen(){
        RequestBuilder requestBuilder = RequestBuilder.get();
        addAzureCommonHeaders(requestBuilder);
        requestBuilder.setUri(String.format(RESOURCE_SERVERS_NONGEN, this.provider.getContext().getAccountNumber()));
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

    public RequestBuilder deleteDatabase(String serverName, String databaseName) throws InternalException {
        RequestBuilder requestBuilder = RequestBuilder.delete();
        addAzureCommonHeaders(requestBuilder);
        requestBuilder.setUri(getEncodedUri(String.format(RESOURCE_DATABASE, this.provider.getContext().getAccountNumber(), serverName, databaseName)));
        return requestBuilder;
    }
    public RequestBuilder subscriptionMetaRequest(){
        RequestBuilder requestBuilder = RequestBuilder.get();
        addAzureCommonHeaders(requestBuilder);
        requestBuilder.setUri(String.format(RESOURCE_SUBSCRIPTION_META, this.provider.getContext().getAccountNumber()));
        return requestBuilder;
    }

    public RequestBuilder getDatabase(String serverName, String databaseName) throws InternalException {
        RequestBuilder requestBuilder = RequestBuilder.get();
        addAzureCommonHeaders(requestBuilder);
        requestBuilder.setUri(getEncodedUri(String.format(RESOURCE_DATABASE, this.provider.getContext().getAccountNumber(), serverName, databaseName)));
        return requestBuilder;
    }

    public RequestBuilder getRecoverableDatabases(String serverName){
        RequestBuilder requestBuilder = RequestBuilder.get();
        addAzureCommonHeaders(requestBuilder);
        requestBuilder.setUri(String.format(RESOURCE_LIST_RECOVERABLE_DATABASES, this.provider.getContext().getAccountNumber(), serverName));
        return requestBuilder;
    }

    public RequestBuilder addFirewallRule(String serverName, ServerServiceResourceModel firewallRule){
        RequestBuilder requestBuilder = RequestBuilder.post();
        addAzureCommonHeaders(requestBuilder);
        requestBuilder.setUri(String.format(RESOURCE_SERVER_FIREWALL, this.provider.getContext().getAccountNumber(), serverName));
        requestBuilder.setEntity(new DaseinObjectToXmlEntity<ServerServiceResourceModel>(firewallRule));
        return requestBuilder;
    }

    public RequestBuilder listFirewallRules(String serveName){
        RequestBuilder requestBuilder = RequestBuilder.get();
        addAzureCommonHeaders(requestBuilder);
        requestBuilder.setUri(String.format(RESOURCE_SERVER_FIREWALL, this.provider.getContext().getAccountNumber(), serveName));
        return requestBuilder;
    }

    public RequestBuilder deleteFirewallRule(String serverName, String ruleName) throws InternalException {
        RequestBuilder requestBuilder = RequestBuilder.delete();
        addAzureCommonHeaders(requestBuilder);
        requestBuilder.setUri(getEncodedUri(String.format(RESOURCE_FIREWALL_RULE, this.provider.getContext().getAccountNumber(), serverName, ruleName)));
        return requestBuilder;
    }

    private String getEncodedUri(String urlString) throws InternalException {
        try {
            URL url = new URL(urlString);
            return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef()).toString();
        } catch (Exception e) {
            throw new InternalException(e.getMessage());
        }
    }

    private void addAzureCommonHeaders(RequestBuilder requestBuilder){
        requestBuilder.addHeader("x-ms-version", "2012-03-01");
        requestBuilder.addHeader("Content-Type", "application/xml;charset=UTF-8");
    }
}
