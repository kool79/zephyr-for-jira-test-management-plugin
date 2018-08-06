package com.thed.zephyr.jenkins.model;

import com.thed.zephyr.jenkins.reporter.ZephyrDescriptor;
import com.thed.zephyr.jenkins.utils.URLValidator;
import com.thed.zephyr.jenkins.utils.rest.RestClient;
import com.thed.zephyr.jenkins.utils.rest.ServerInfo;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

public class ZephyrInstance extends Zephyr {

	@DataBoundConstructor
	public ZephyrInstance(String serverAddress, String username, String password) {
		this.password = password;
		this.username = username;
		this.serverAddress = serverAddress;
	}

	private String serverAddress;
	private String username;
	private String password;
	
	public String getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Extension
	public static class DesciptorImpl extends ZephyrDescriptor {

		@Nonnull
		@Override
		public String getDisplayName() {
			return "JIRA Server/Data Center";
		}

		public FormValidation doTestConnection (
				@QueryParameter String serverAddress,
				@QueryParameter String username,
				@QueryParameter String password) {

			serverAddress = org.apache.commons.lang.StringUtils.removeEnd(serverAddress, "/");

			if (org.apache.commons.lang.StringUtils.isBlank(serverAddress)) {
				return FormValidation.error("Please enter the server name");
			}

			if (org.apache.commons.lang.StringUtils.isBlank(username)) {
				return FormValidation.error("Please enter the username");
			}

			if (org.apache.commons.lang.StringUtils.isBlank(password)) {
				return FormValidation.error("Please enter the password");
			}

			if (!(serverAddress.trim().startsWith("https://") || serverAddress.trim().startsWith("http://"))) {
				return FormValidation.error("Incorrect server address format");
			}

			String jiraURL = URLValidator.validateURL(serverAddress);

			if(!jiraURL.startsWith("http")) {
				return FormValidation.error(jiraURL);
			}
			RestClient restClient = new RestClient(serverAddress, username, password);

			if (!ServerInfo.findServerAddressIsValidZephyrURL(restClient)) {
				return FormValidation.error("This is not a valid Jira Server");
			}

			if (!ServerInfo.validateCredentials(restClient)) {
				return FormValidation.error("Invalid user credentials");
			}
			restClient.destroy();
			return FormValidation.ok("Connection to JIRA has been validated");
		}
	}

	@PostConstruct
	public void finish() {
		this.serverAddress = StringUtils.removeEnd(serverAddress, "/");
	}

}
