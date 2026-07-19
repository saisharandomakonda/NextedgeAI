package com.syncari.core.model;

import java.util.*;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import com.syncari.core.model.misc.InstanceType;
import com.syncari.core.model.misc.OrganizationType;
import com.syncari.core.model.security.OAuthConfig;
import com.syncari.core.model.util.Status;

import lombok.Data;

@Data
public class Organization extends UUIDAuditModel {
	@NotNull(message = "Org name is required")
	private String name;
	private String logoLocation;
	private Status status;
	private List<Instance> instances = new ArrayList<>();
	private SSOAuthConfig ssoConfig;

    private OrganizationType orgType = OrganizationType.standard;
    // If partner org, the Oauth configurations, if any to use instead of Syncari ones for all instances in the org.
    private Map<String, OAuthConfig> oauthConfigs = new HashMap<String, OAuthConfig>();
	private String deletedBy;
	private Date deletedAt;
	private String maxNumberOfInstances;
	private String insightsProviderOrgId;

	public void addInstance(Instance instance) {
		instances.add(instance);
	}
	public Organization(String name) {
		this.name = name;
	}

	public Organization() {

	}

	public Optional<Instance> getInstance(String syncariId) {
		return instances.stream().filter(i -> syncariId.equals(i.getSyncariId())).findFirst();
	}

	public void removeInstance(String syncariId) {
	    ListIterator<Instance> iter = instances.listIterator();
	    while(iter.hasNext()){
	        if(syncariId.equals(iter.next().getSyncariId())){
	            iter.remove();
	        }
	    }
	}

	public Optional<Instance> getActiveInstance(String syncariId) {
		return instances.stream().filter(i -> syncariId.equals(i.getSyncariId()) && i.isActive()).findFirst();
	}

	public List<Instance> getActiveInstances() {
		return instances.stream().filter(i -> i.isActive()).collect(Collectors.toList());
	}

	public Set<String> getAllSyncariIds() {
	    return instances.stream().map(i -> i.getSyncariId()).collect(Collectors.toSet());
	}
	
	public Optional<Instance> getInstanceByName(String instanceName) {
		return instances.stream().filter(i -> instanceName.equals(i.getName())).findFirst();
	}

	public boolean isActive(){
		return status == null || status== Status.ACTIVE;
	}

	public boolean isSSOEnabled(){
		return ssoConfig != null;
	}

    public boolean isPartner() {
        return orgType == OrganizationType.partner;
    }
	public boolean isTrial() {
		return orgType == OrganizationType.trial;
	}

}
