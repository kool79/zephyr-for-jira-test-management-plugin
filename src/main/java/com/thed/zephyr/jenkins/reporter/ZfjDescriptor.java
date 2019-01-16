package com.thed.zephyr.jenkins.reporter;

import static com.thed.zephyr.jenkins.reporter.ZfjConstants.ADD_ZEPHYR_GLOBAL_CONFIG;
import static com.thed.zephyr.jenkins.reporter.ZfjConstants.CYCLE_DURATION_1_DAY;
import static com.thed.zephyr.jenkins.reporter.ZfjConstants.CYCLE_DURATION_30_DAYS;
import static com.thed.zephyr.jenkins.reporter.ZfjConstants.CYCLE_DURATION_7_DAYS;
import static com.thed.zephyr.jenkins.reporter.ZfjConstants.NAME_POST_BUILD_ACTION;
import static com.thed.zephyr.jenkins.reporter.ZfjConstants.NEW_CYCLE_KEY;
import static com.thed.zephyr.jenkins.reporter.ZfjConstants.ATLASSIAN_NET;

import com.thed.zephyr.jenkins.model.Zephyr;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.thed.zephyr.jenkins.model.ZephyrCloudInstance;
import com.thed.zephyr.jenkins.model.ZephyrInstance;
import com.thed.zephyr.jenkins.utils.ConfigurationValidator;
import com.thed.zephyr.jenkins.utils.URLValidator;
import com.thed.zephyr.jenkins.utils.rest.Cycle;
import com.thed.zephyr.jenkins.utils.rest.Project;
import com.thed.zephyr.jenkins.utils.rest.RestClient;
import com.thed.zephyr.jenkins.utils.rest.ServerInfo;
import com.thed.zephyr.jenkins.utils.rest.Version;

@Extension
public final class ZfjDescriptor extends BuildStepDescriptor<Publisher> {

	@Deprecated
	private transient List<ZephyrInstance> jiraInstances;
	@Deprecated
	private transient List<ZephyrCloudInstance> jiraCloudInstances;
	@Deprecated
	private transient String[] config;

	private List<Zephyr> zephyrInstances = new ArrayList<>();

	public ZfjDescriptor() {
		super(ZfjReporter.class);
		load();
	}

	//Backwards compatability
	public Object readResolve() {
		if(jiraInstances != null) {
			zephyrInstances.addAll(jiraInstances);
		}
		if(jiraCloudInstances != null) {
			zephyrInstances.addAll(jiraCloudInstances);
		}
		return this;
	}

    @Override
    public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
        return true;
    }


	@Override
	public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
		//Use modern databinding
		this.jiraCloudInstances = null;
		this.jiraInstances = null;
		this.zephyrInstances = null;

		req.bindJSON(this, formData);
		save();
		return super.configure(req, formData);
	}


    @Override
    public String getDisplayName() {
        return NAME_POST_BUILD_ACTION;
    }

    public ListBoxModel doFillServerAddressItems(@QueryParameter String serverAddress) {
        ListBoxModel m = fetchServerList(serverAddress);
        return m;
    }

	public ListBoxModel doFillProjectKeyItems(@QueryParameter String serverAddress) {

		if (StringUtils.isBlank(serverAddress)) {
	        ListBoxModel mi = fetchServerList(serverAddress);
			serverAddress = mi.get(0).value;
		}

		ListBoxModel m = new ListBoxModel();
		if (serverAddress.trim().equals(ADD_ZEPHYR_GLOBAL_CONFIG) || zephyrInstances.isEmpty()) {
			m.add(ADD_ZEPHYR_GLOBAL_CONFIG);
			return m;
		}
	
		fetchProjectList(serverAddress, m);
	    return m;
	}

	public ListBoxModel doFillVersionKeyItems(@QueryParameter String projectKey, @QueryParameter String serverAddress) {
		
		if (StringUtils.isBlank(serverAddress)) {
	        ListBoxModel mi = fetchServerList(serverAddress);
			serverAddress = mi.get(0).value;
		}
		
		if (StringUtils.isBlank(projectKey)) {
			ListBoxModel mp = new ListBoxModel();
			if (serverAddress.trim().equals(ADD_ZEPHYR_GLOBAL_CONFIG) || zephyrInstances.isEmpty()) {
				mp.add(ADD_ZEPHYR_GLOBAL_CONFIG);
			} else {
				fetchProjectList(serverAddress, mp);
			}
			projectKey = mp.get(0).value;
		}
	
	
		ListBoxModel m = new ListBoxModel();
	
		if (StringUtils.isBlank(projectKey) || projectKey.trim().equals(ADD_ZEPHYR_GLOBAL_CONFIG) || zephyrInstances.isEmpty()) {
			m.add(ADD_ZEPHYR_GLOBAL_CONFIG);
			return m;
		}
	
		fetchVersionList(projectKey, serverAddress, m);
	    return m;
	
	}

	public ListBoxModel doFillCycleKeyItems(@QueryParameter String versionKey, @QueryParameter String serverAddress, @QueryParameter String projectKey) {
		ListBoxModel m = new ListBoxModel();
	
		if (StringUtils.isBlank(serverAddress)) {
	        ListBoxModel ms = fetchServerList(serverAddress);
			serverAddress = ms.get(0).value;
		}
		
		if (StringUtils.isBlank(projectKey)) {
			ListBoxModel mp = new ListBoxModel();
			if (serverAddress.trim().equals(ADD_ZEPHYR_GLOBAL_CONFIG) || this.zephyrInstances.isEmpty()) {
				mp.add(ADD_ZEPHYR_GLOBAL_CONFIG);
			} else {
				fetchProjectList(serverAddress, mp);
			}
			projectKey = mp.get(0).value;
		}
	
		if (StringUtils.isBlank(versionKey)) {
	    	ListBoxModel mv = new ListBoxModel();
			if (StringUtils.isBlank(projectKey) || projectKey.trim().equals(ADD_ZEPHYR_GLOBAL_CONFIG) || this.zephyrInstances.isEmpty()) {
				mv.add(ADD_ZEPHYR_GLOBAL_CONFIG);
			} else {
				fetchVersionList(projectKey, serverAddress, mv);
			}
	
			versionKey = mv.get(0).value;
		}
	
		if (StringUtils.isBlank(versionKey) || versionKey.trim().equals(ADD_ZEPHYR_GLOBAL_CONFIG) || this.zephyrInstances.isEmpty()) {
			m.add(ADD_ZEPHYR_GLOBAL_CONFIG);
			return m;
		}
		
		long versionId = Long.parseLong(versionKey);
	
		RestClient restClient = getRestclient(serverAddress);
		
		Map<String, String> cycles = null;
		if (serverAddress.contains(ATLASSIAN_NET)) {
			cycles = Cycle.getAllCyclesByVersionIdZFJC(versionId, restClient, projectKey);
		} else {
			cycles = Cycle.getAllCyclesByVersionId(versionId, restClient, projectKey);
		}
		
		Set<Entry<String, String>> cycleEntrySet = cycles.entrySet();
	
		for (Iterator<Entry<String, String>> iterator = cycleEntrySet.iterator(); iterator.hasNext();) {
			Entry<String, String> entry = iterator.next();
			m.add(entry.getValue(), entry.getKey());
		}
		
		m.add("New Cycle", NEW_CYCLE_KEY);
		restClient.destroy();
	    return m;
	}

	public ListBoxModel doFillCycleDurationItems(@QueryParameter String versionKey, @QueryParameter String serverAddress) {
		ListBoxModel m = new ListBoxModel();
		m.add(CYCLE_DURATION_30_DAYS);
		m.add(CYCLE_DURATION_7_DAYS);
		m.add(CYCLE_DURATION_1_DAY);
		return m;
	}

	private ListBoxModel fetchServerList(String serverAddress) {
		ListBoxModel m = new ListBoxModel();

		for(Zephyr s : zephyrInstances) {
			if(s instanceof ZephyrInstance) {
				m.add(((ZephyrInstance)s).getServerAddress());
			} else if(s instanceof ZephyrCloudInstance) {
				ZephyrCloudInstance zci = (ZephyrCloudInstance)s;
				m.add(((ZephyrCloudInstance) s).getJiraCloudAddress());
			}
		}

		if (zephyrInstances.isEmpty()) {
			m.add(ADD_ZEPHYR_GLOBAL_CONFIG);
		}

		return m;
	}


    private void fetchProjectList(String serverAddress, ListBoxModel m) {
		RestClient restClient = null;
		
		restClient = getRestclient(serverAddress);
		
		Map<Long, String> projects = Project.getAllProjects(restClient);

		Set<Entry<Long, String>> projectEntrySet = projects.entrySet();

		for (Iterator<Entry<Long, String>> iterator = projectEntrySet.iterator(); iterator.hasNext();) {
			Entry<Long, String> entry = iterator.next();
			m.add(entry.getValue(), entry.getKey()+"");
		}
		restClient.destroy();
	}


	private void fetchVersionList(String projectKey, String serverAddress, ListBoxModel m) {
		long parseLong = Long.parseLong(projectKey);
    	
    	RestClient restClient = getRestclient(serverAddress);
		Map<Long, String> versions = Version.getVersionsByProjectID(parseLong, restClient);

		Set<Entry<Long, String>> versionEntrySet = versions.entrySet();

		for (Iterator<Entry<Long, String>> iterator = versionEntrySet.iterator(); iterator.hasNext();) {
			Entry<Long, String> entry = iterator.next();
			m.add(entry.getValue(), entry.getKey()+"");
		}
		restClient.destroy();
	}

    private RestClient getRestclient(String serverAddress) {
		String tempUserName = null;
		String tempPassword = null;
		RestClient restClient = null;

		for(Zephyr zephyrInstance: zephyrInstances) {
			if(zephyrInstance instanceof ZephyrInstance) {
				ZephyrInstance zi = (ZephyrInstance)zephyrInstance;
				if(zi.getServerAddress().trim().equals(serverAddress)) {
					tempUserName = zi.getUsername();
					tempPassword = zi.getPassword();
					restClient = new RestClient(serverAddress, tempUserName, tempPassword);
				}
			} else {
				ZephyrCloudInstance zi = (ZephyrCloudInstance) zephyrInstance;
				if(serverAddress.contains(ATLASSIAN_NET) && zi.getJiraCloudAddress().trim().equals(serverAddress)) {
					String jiraCloudUserName = zi.getJiraCloudUserName();
					String jiraCloudPassword = zi.getJiraCloudPassword();
					String zephyrCloudAddress = zi.getZephyrCloudAddress();
					String zephyrCloudAccessKey = zi.getZephyrCloudAccessKey();
					String zephyrCloudSecretKey = zi.getZephyrCloudSecretKey();
					restClient = new RestClient(serverAddress, jiraCloudUserName, jiraCloudPassword, zephyrCloudAddress, zephyrCloudAccessKey, zephyrCloudSecretKey);
				}
			}
		}

		return restClient;
	}

	@Deprecated
	public List<ZephyrInstance> getJiraInstances() {
		return jiraInstances;
	}

	public void setZephyrInstances(List<Zephyr> zephyrInstances) { this.zephyrInstances = zephyrInstances; }

	public List<Zephyr> getZephyrInstances() {
		return zephyrInstances;
	}

	@Deprecated
	public void setJiraInstances(List<ZephyrInstance> jiraInstances) {
		this.jiraInstances = jiraInstances;
	}

	@Deprecated
	public List<ZephyrCloudInstance> getJiraCloudInstances() {
		return jiraCloudInstances;
	}

	@Deprecated
	public void setJiraCloudInstances(List<ZephyrCloudInstance> jiraCloudInstances) {
		this.jiraCloudInstances = jiraCloudInstances;
	}

}