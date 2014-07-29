package org.dasein.cloud.azure.compute;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureMethod;
import org.dasein.cloud.azure.compute.model.AffinityGroupModel;
import org.dasein.cloud.azure.compute.model.AffinityGroupsModel;
import org.dasein.cloud.azure.compute.model.CreateAffinityGroupModel;
import org.dasein.cloud.azure.compute.model.UpdateAffinityGroupModel;
import org.dasein.cloud.compute.AffinityGroup;
import org.dasein.cloud.compute.AffinityGroupCreateOptions;
import org.dasein.cloud.compute.AffinityGroupFilterOptions;
import org.dasein.cloud.compute.AffinityGroupSupport;
import org.dasein.cloud.identity.ServiceAction;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by Vlad_Munthiu on 7/23/2014.
 */
public class AzureAffinityGroupSupport implements AffinityGroupSupport {
    private Azure provider;

    static private final Logger logger = Logger.getLogger(AzureAffinityGroupSupport.class);

    public static final String RESOURCE_AFFINITYGROUPS = "/affinitygroups";
    public static final String RESOURCE_AFFINITYGROUP = "/affinitygroups/%s";

    public AzureAffinityGroupSupport(@Nonnull Azure provider) { this.provider = provider; }
    /**
     * Creates an affinity group in the cloud
     *
     * @param options the options used when creating the affinity group
     * @return the provider ID of the affinity group
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation creating the affinity group
     * @throws org.dasein.cloud.CloudException    an error occurred within the service provider creating the affinity group
     */
    @Nonnull
    @Override
    public AffinityGroup create(@Nonnull AffinityGroupCreateOptions options) throws InternalException, CloudException {

        if(options == null && options.getName() == null)
            throw new InternalException("Cannot create AffinityGroup. Create options or affinity group name cannot be null.");

        CreateAffinityGroupModel createAffinityGroupModel = new CreateAffinityGroupModel();
        createAffinityGroupModel.setName(options.getName());
        createAffinityGroupModel.setDescription(options.getDescription());
        createAffinityGroupModel.setLocation(provider.getContext().getRegionId());
        createAffinityGroupModel.setLabel(new String(Base64.encodeBase64(options.getName().getBytes())));

        AzureMethod azureMethod = new AzureMethod(this.provider);

        try {
            azureMethod.post(RESOURCE_AFFINITYGROUPS, createAffinityGroupModel);
        }
        catch (JAXBException e)
        {
            logger.error(e.getMessage());
            throw new InternalException(e);
        }

        return AffinityGroup.getInstance(createAffinityGroupModel.getName(),
                createAffinityGroupModel.getName(),
                createAffinityGroupModel.getDescription(),
                createAffinityGroupModel.getLocation(), null);
    }

    /**
     * Deletes the affinity group from the cloud if the affinity group is not empty this method should error
     *
     * @param affinityGroupId the ID of the affinity group to be deleted
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation deleting the affinity group
     * @throws org.dasein.cloud.CloudException    an error occurred within the service provider deleting the affinity group
     */
    @Override
    public void delete(@Nonnull String affinityGroupId) throws InternalException, CloudException {
        if(affinityGroupId == null || affinityGroupId.isEmpty())
            throw new InternalException("Cannot delete affinity group. Please specify the id for the affinity group to remove");

        AzureMethod method = new AzureMethod(this.provider);
        method.invoke("DELETE", this.provider.getContext().getAccountNumber(), String.format(RESOURCE_AFFINITYGROUP, affinityGroupId), null);
    }

    /**
     * Retrieves the details of the specified Affinity Group from the cloud
     *
     * @param affinityGroupId the ID of the affinity group to be retrieved
     * @return the Dasein AffinityGroup object
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation retrieving the affinity group
     * @throws org.dasein.cloud.CloudException    an error occurred within the service provider retrieving the affinity group
     */
    @Nonnull
    @Override
    public AffinityGroup get(@Nonnull String affinityGroupId) throws InternalException, CloudException {
        if(affinityGroupId == null || affinityGroupId.isEmpty())
            throw new InternalException("Please specify the id for the affinity group you want to retrieve.");

        AzureMethod method = new AzureMethod(this.provider);
        final AffinityGroupModel affinityGroupModel = method.get(AffinityGroupModel.class, String.format(RESOURCE_AFFINITYGROUP, affinityGroupId));

        //TODO see if name is enough to be used as an id
        return AffinityGroup.getInstance(affinityGroupModel.getName(),affinityGroupModel.getName(),affinityGroupModel.getDescription(), affinityGroupModel.getLocation(), null);
    }

    /**
     * Lists all of the affinity groups visible to the current account
     *
     * @param options Filtering options for the list
     * @return All the affinity groups visible to current account
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation listing the affinity groups
     * @throws org.dasein.cloud.CloudException    an error occurred within the service provider listing the affinity groups
     */
    @Nonnull
    @Override
    public Iterable<AffinityGroup> list(@Nonnull AffinityGroupFilterOptions options) throws InternalException, CloudException {
        AzureMethod method = new AzureMethod(this.provider);
        final AffinityGroupsModel affinityGroupsModel = method.get(AffinityGroupsModel.class, RESOURCE_AFFINITYGROUPS);

        ArrayList<AffinityGroup> affinityGroups = new ArrayList<AffinityGroup>();

        for(AffinityGroupModel affinityGroupModel : affinityGroupsModel.getAffinityGroups())
        {
            //TODO see if name is enough to be used as an id
            AffinityGroup affinityGroup = AffinityGroup.getInstance(affinityGroupModel.getName(),affinityGroupModel.getName(),affinityGroupModel.getDescription(), affinityGroupModel.getLocation(), null);

            if(options != null && options.matches(affinityGroup))
                affinityGroups.add(affinityGroup);
        }

        return affinityGroups;
    }

    /**
     * Modifies details of the specified affinity group
     *
     * @param affinityGroupId the ID of the affinity group to be modified
     * @param options         the options containing the modified data
     * @return the newly modified Dasein AffinityGroup object
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation modifying the affinity group
     * @throws org.dasein.cloud.CloudException    an error occurred within the service provider modifying the affinity group
     */
    @Override
    public AffinityGroup modify(@Nonnull String affinityGroupId, @Nonnull AffinityGroupCreateOptions options) throws InternalException, CloudException {
        if(affinityGroupId == null|| affinityGroupId.isEmpty())
            throw new InternalException("Cannot modify an affinity group: affinityGroupId cannot be null or empty");

        if(options == null && options.getDescription() == null)
            throw new InternalException("Cannot create AffinityGroup. Create options or affinity group description cannot be null.");

        UpdateAffinityGroupModel updateAffinityGroupModel = new UpdateAffinityGroupModel();
        updateAffinityGroupModel.setDescription(options.getDescription());

        AzureMethod method = new AzureMethod(this.provider);
        try {
            method.put(String.format(RESOURCE_AFFINITYGROUP, affinityGroupId), updateAffinityGroupModel);
        }
        catch (JAXBException e)
        {
            logger.error(e.getMessage());
            throw new InternalException(e);
        }

        return get(affinityGroupId);
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
}
