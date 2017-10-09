package com.clearslide.userstore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.user.api.Properties;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.claim.Claim;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.common.RoleContext;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.api.Property;
import org.wso2.carbon.user.core.util.DatabaseUtil;
import org.wso2.carbon.identity.application.common.model.ThreadLocalProvisioningServiceProvider;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.application.common.model.ProvisioningServiceProviderType;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;

import javax.sql.DataSource;

import java.util.HashMap;
import java.util.Map;

public class ClearslideUserStoreManager extends AbstractUserStoreManager {

    private static Log log = LogFactory.getLog(ClearslideUserStoreManager.class);


    public ClearslideUserStoreManager() {

        log.info("ClearslideUserStoreManager Initializing Started " + System.currentTimeMillis());

        this.tenantId = -1234;
        this.readGroupsEnabled = false;
        this.writeGroupsEnabled = false;

        log.info("ClearslideUserStoreManager initialized...");
    }

    public ClearslideUserStoreManager(RealmConfiguration realmConfig, Map<String, Object> properties) throws UserStoreException {

        log.info("ClearslideUserStoreManager Initializing Started " + System.currentTimeMillis());


        this.realmConfig = realmConfig;
        this.tenantId = -1234;
        this.readGroupsEnabled = false;
        this.writeGroupsEnabled = false;

        this.realmConfig = realmConfig;
        this.dataSource = (DataSource) properties.get("um.datasource");
        if (this.dataSource == null) {
            this.dataSource = DatabaseUtil.getRealmDataSource(realmConfig);
        }

        if (this.dataSource == null) {
            throw new UserStoreException("Data Source is null");
        } else {
            properties.put("um.datasource", this.dataSource);

            this.persistDomain();
            this.doInitialSetup();
            this.initUserRolesCache();
            log.info("Initializing Ended " + System.currentTimeMillis());
        }

        try {
            claimManager = (org.wso2.carbon.user.core.claim.ClaimManager) CarbonContext.getThreadLocalCarbonContext().getUserRealm().getClaimManager();
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            log.error("Error initializing the Claim Manager", e);
        }

        log.info("ClearslideUserStoreManager initialized...");
    }

    @Override
    public Properties getDefaultUserStoreProperties() {

        Property[] mandatoryProperties = ClearSlideUserStoreManagerConstants.CUSTOM_UM_MANDATORY_PROPERTIES.toArray(
                new Property[ClearSlideUserStoreManagerConstants.CUSTOM_UM_MANDATORY_PROPERTIES.size()]);
        Property[] optionalProperties = ClearSlideUserStoreManagerConstants.CUSTOM_UM_OPTIONAL_PROPERTIES.toArray
                (new Property[ClearSlideUserStoreManagerConstants.CUSTOM_UM_OPTIONAL_PROPERTIES.size()]);
        Property[] advancedProperties = ClearSlideUserStoreManagerConstants.CUSTOM_UM_ADVANCED_PROPERTIES.toArray
                (new Property[ClearSlideUserStoreManagerConstants.CUSTOM_UM_ADVANCED_PROPERTIES.size()]);

        Properties properties = new Properties();
        properties.setMandatoryProperties(mandatoryProperties);
        properties.setOptionalProperties(optionalProperties);

        //Since there are no advanced properties yet, following is not used
        //properties.setAdvancedProperties(advancedProperties);
        return properties;
    }

    private String getServiceProviderName(){
        ThreadLocalProvisioningServiceProvider tsp = IdentityApplicationManagementUtil.getThreadLocalProvisioningServiceProvider();
        String clientID = tsp.getServiceProviderName();
        String tenantDomainName = tsp.getTenantDomain();
        String serviceProviderName = null;
        if (tsp.getServiceProviderType() == ProvisioningServiceProviderType.OAUTH) {

            try {
                serviceProviderName = ApplicationManagementService.getInstance().getServiceProviderNameByClientId(clientID, "oauth2", tenantDomainName);
            } catch (IdentityApplicationManagementException e) {
                // handle
            }

        }

        return serviceProviderName;
    }

    private String getTeamIDofServiceProvider(){

        ThreadLocalProvisioningServiceProvider tsp = IdentityApplicationManagementUtil.getThreadLocalProvisioningServiceProvider();
        String clientID = tsp.getServiceProviderName();
        String tenantDomainName = tsp.getTenantDomain();

        String teamID = null;
        String serviceProviderName = null;


        try {
            serviceProviderName = ApplicationManagementService.getInstance().getServiceProviderNameByClientId(clientID, "oauth2", tenantDomainName);

            ServiceProvider serviceProvider = ApplicationManagementService.getInstance().getServiceProvider(serviceProviderName,tenantDomainName);
            teamID = serviceProvider.getDescription();

        } catch (IdentityApplicationManagementException e) {
            // handle
        }

        return teamID;
    }

    @Override
    protected void doAddUser(String userName, Object credential, String[] roleList, Map<String, String> claims, String profileName, boolean requirePasswordChange) throws UserStoreException {


        String APIUrl = realmConfig.getUserStoreProperty(ClearSlideUserStoreManagerConstants.SERVICE_URL_PROPERTY_NAME);
        log.info("doAddUser method called for usre: " + userName);

        for (Map.Entry<String, String> entry : claims.entrySet()) {
            log.info("Key : " + entry.getKey() + ", Value : " + entry.getValue());
        }

        //urn:scim:schemas:core:1.0:id claim contains the SCIM ID of the user

        log.info("Invoking the API : " + APIUrl);


        log.info("Service Provider Name : " + getServiceProviderName());
        log.info("Team ID : " + getTeamIDofServiceProvider());
    }

    @Override
    protected void doDeleteUser(String userName) throws UserStoreException {

        log.info("doDeleteUser method called for user: " + userName);

    }

    public Claim [] getUserClaims (String userName, String profileName) throws UserStoreException{

        log.info("getUserClaims method called for user: " + userName);
        Claim[] claims = super.getUserClaimValues(userName, profileName);
        return claims;
    }

    @Override
    protected String[] doListUsers(String filter, int maxItemLimit) throws UserStoreException {

        log.info("doListUsers method called");

        if (filter.equals("*")) {
            return new String[]{"*multiple-users*"};
        } else {
            //Test only
            return new String[]{"*multiple-users*"};
        }
    }

    @Override
    protected boolean doCheckExistingUser(String userName) throws UserStoreException {

        log.info("doCheckExistingUser method called for userName: "+ userName);

        //hard coding for testing purpose
        if("tharindu".equals(userName)){
            return true;
        }
        return false;
    }


    @Override
    protected String[] getUserListFromProperties(String property, String value, String profileName) throws UserStoreException {

        log.info("getUserListFromProperties method called");
        log.info("property: " + property + ", value: " + value + ", profileName: " + profileName);


        // call the external API and get the username matching this request's user

        if("scimId".equals(property)){
            //value is XXXXX in this URL https://localhost:9443/wso2/scim/Users/XXXXX when making the request


        } else if("uid".equals(property)){
            // value is XXXXX in https://localhost:9443/wso2/scim/Users?filter=username+eq+XXXXX when searching users
        }

        if (profileName == null) {
            profileName = UserCoreConstants.DEFAULT_PROFILE;
        }


        String matchingUserName = "tharindu"; //hard coded for testing purpose
        //add the username to the array

        String [] userNameArray = new String []{matchingUserName};

        return userNameArray;
    }


    // system methods that are important for the usercase

    @Override
    public int getTenantId(String s) throws UserStoreException {
        //log.info("getTenantId method called");
        return -1234;
    }

    @Override
    public int getTenantId() throws UserStoreException {
        //log.info("getTenantId method called");
        return -1234;
    }

    @Override
    public RealmConfiguration getRealmConfiguration() {
        //log.info("getRealmConfiguration method called");


        return this.realmConfig;
    }

    @Override
    public boolean isReadOnly() throws UserStoreException {
        log.info("isReadOnly method called");

        return false;
    }


    // Other methods in the abstract class are not important for this usecase

    @Override
    protected String[] doGetSharedRoleNames(String s, String s1, int i) throws UserStoreException {

        log.info("doGetSharedRoleNames method called");
        return new String[0];
    }

    @Override
    protected Map<String, String> getUserPropertyValues(String s, String[] strings, String s1) throws UserStoreException {

        log.info("getUserPropertyValues method called");

        Map<String, String> propertyValues = new HashMap<>();
        return propertyValues;
    }

    @Override
    protected boolean doCheckExistingRole(String s) throws UserStoreException {

        log.info("doCheckExistingRole method called");
        return false;
    }

    @Override
    protected RoleContext createRoleContext(String s) throws UserStoreException {

        log.info("createRoleContext method called");

        RoleContext rc = new RoleContext();
        return rc;
    }

    @Override
    protected boolean doAuthenticate(String s, Object o) throws UserStoreException {

        log.info("doAuthenticate method called");
        return false;
    }


    @Override
    protected void doUpdateCredential(String s, Object o, Object o1) throws UserStoreException {

        log.info("doUpdateCredential method called");

    }

    @Override
    protected void doUpdateCredentialByAdmin(String s, Object o) throws UserStoreException {

        log.info("doUpdateCredentialByAdmin method called");

    }


    @Override
    protected void doSetUserClaimValue(String s, String s1, String s2, String s3) throws UserStoreException {

        log.info("doSetUserClaimValue method called");

    }

    @Override
    protected void doSetUserClaimValues(String s, Map<String, String> map, String s1) throws UserStoreException {

        log.info("doSetUserClaimValues method called");

    }

    @Override
    protected void doDeleteUserClaimValue(String s, String s1, String s2) throws UserStoreException {

        log.info("doDeleteUserClaimValue method called");

    }

    @Override
    protected void doDeleteUserClaimValues(String s, String[] strings, String s1) throws UserStoreException {

        log.info("doDeleteUserClaimValues method called");

    }

    @Override
    protected void doUpdateUserListOfRole(String s, String[] strings, String[] strings1) throws UserStoreException {

        log.info("doUpdateUserListOfRole method called");

    }

    @Override
    protected void doUpdateRoleListOfUser(String s, String[] strings, String[] strings1) throws UserStoreException {

        log.info("doUpdateRoleListOfUser method called");

    }

    @Override
    protected String[] doGetExternalRoleListOfUser(String s, String s1) throws UserStoreException {
        log.info("doGetExternalRoleListOfUser method called");
        return new String[0];
    }

    @Override
    protected String[] doGetSharedRoleListOfUser(String s, String s1, String s2) throws UserStoreException {

        log.info("doGetSharedRoleListOfUser method called");
        return new String[0];
    }

    @Override
    protected void doAddRole(String s, String[] strings, boolean b) throws UserStoreException {

        log.info("doAddRole method called");

    }

    @Override
    protected void doDeleteRole(String s) throws UserStoreException {

        log.info("doDeleteRole method called");

    }

    @Override
    protected void doUpdateRoleName(String s, String s1) throws UserStoreException {
        log.info("doUpdateRoleName method called");

    }

    @Override
    protected String[] doGetRoleNames(String s, int i) throws UserStoreException {

        log.info("doGetRoleNames method called");
        return new String[]{"testrole"};
    }


    @Override
    protected String[] doGetDisplayNamesForInternalRole(String[] strings) throws UserStoreException {
        log.info("doGetDisplayNamesForInternalRole method called");
        return new String[0];
    }

    @Override
    public boolean doCheckIsUserInRole(String s, String s1) throws UserStoreException {
        log.info("doCheckIsUserInRole method called");
        return false;
    }

    @Override
    protected String[] doGetUserListOfRole(String s, String s1) throws UserStoreException {
        log.info("doGetUserListOfRole method called");
        return new String[0];
    }

    @Override
    public String[] getProfileNames(String s) throws UserStoreException {

        log.info("getProfileNames method called");
        return new String[]{"default"};
    }

    @Override
    public String[] getAllProfileNames() throws UserStoreException {
        log.info("getAllProfileNames method called");
        return new String[]{"default"};
    }

    @Override
    public int getUserId(String s) throws UserStoreException {
        log.info("getUserId method called");
        return 0;
    }

    @Override
    public Map<String, String> getProperties(Tenant tenant) throws UserStoreException {

        log.info("getProperties");

        return this.realmConfig.getUserStoreProperties();
    }

    @Override
    public boolean isBulkImportSupported() throws UserStoreException {
        log.info("isBulkImportSupported method called");
        return false;
    }



    @Override
    public Map<String, String> getProperties(org.wso2.carbon.user.api.Tenant tenant) throws org.wso2.carbon.user.api.UserStoreException {
        log.info("getProperties method called");
        return this.realmConfig.getUserStoreProperties();
    }

    @Override
    public boolean isMultipleProfilesAllowed() {
        log.info("isMultipleProfilesAllowed method called");

        return false;
    }

    @Override
    public void addRememberMe(String s, String s1) throws org.wso2.carbon.user.api.UserStoreException {
        log.info("addRememberMe method called");

    }

    @Override
    public boolean isValidRememberMeToken(String s, String s1) throws org.wso2.carbon.user.api.UserStoreException {
        log.info("isValidRememberMeToken method called");
        return false;
    }


}