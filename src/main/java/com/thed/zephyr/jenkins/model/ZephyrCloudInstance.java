package com.thed.zephyr.jenkins.model;

import com.thed.zephyr.jenkins.reporter.ZephyrDescriptor;
import com.thed.zephyr.jenkins.utils.URLValidator;
import com.thed.zephyr.jenkins.utils.rest.RestClient;
import com.thed.zephyr.jenkins.utils.rest.ServerInfo;
import hudson.Extension;
import hudson.util.FormValidation;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.PostConstruct;
import java.util.Map;

public class ZephyrCloudInstance extends Zephyr {

	private String jiraCloudAddress;
	private String zephyrCloudAddress;
	private String jiraCloudUserName;
	private String jiraCloudPassword;
	private String zephyrCloudAccessKey;
	private String zephyrCloudSecretKey;

	@DataBoundConstructor
	public ZephyrCloudInstance(String jiraCloudAddress, String zephyrCloudAddress, String jiraCloudUserName,
							   String jiraCloudPassword, String zephyrCloudAccessKey, String zephyrCloudSecretKey) {
		this.jiraCloudAddress = jiraCloudAddress;
		this.zephyrCloudAddress = zephyrCloudAddress;
		this.jiraCloudUserName = jiraCloudUserName;
		this.jiraCloudPassword = jiraCloudPassword;
		this.zephyrCloudAccessKey = zephyrCloudAccessKey;
		this.zephyrCloudSecretKey = zephyrCloudSecretKey;
	}

	public String getJiraCloudAddress() {
		return jiraCloudAddress;
	}

	public void setJiraCloudAddress(String jiraCloudAddress) {
		this.jiraCloudAddress = jiraCloudAddress;
	}

	public String getZephyrCloudAddress() {
		return zephyrCloudAddress;
	}

	public void setZephyrCloudAddress(String zephyrCloudAddress) {
		this.zephyrCloudAddress = zephyrCloudAddress;
	}

	public String getJiraCloudUserName() {
		return jiraCloudUserName;
	}

	public void setJiraCloudUserName(String jiraCloudUserName) {
		this.jiraCloudUserName = jiraCloudUserName;
	}

	public String getJiraCloudPassword() {
		return jiraCloudPassword;
	}

	public void setJiraCloudPassword(String jiraCloudPassword) {
		this.jiraCloudPassword = jiraCloudPassword;
	}

	public String getZephyrCloudAccessKey() {
		return zephyrCloudAccessKey;
	}

	public void setZephyrCloudAccessKey(String zephyrCloudAccessKey) {
		this.zephyrCloudAccessKey = zephyrCloudAccessKey;
	}

	public String getZephyrCloudSecretKey() {
		return zephyrCloudSecretKey;
	}

	public void setZephyrCloudSecretKey(String zephyrCloudSecretKey) {
		this.zephyrCloudSecretKey = zephyrCloudSecretKey;
	}

	@Extension
	public static class DescriptorImpl extends ZephyrDescriptor {

		public FormValidation doTestZephyrCloudConnection(
				@QueryParameter String jiraCloudAddress,
				@QueryParameter String zephyrCloudAddress,
				@QueryParameter String jiraCloudUserName,
				@QueryParameter String jiraCloudPassword,
				@QueryParameter String zephyrCloudAccessKey,
				@QueryParameter String zephyrCloudSecretKey) {

			jiraCloudAddress = org.apache.commons.lang.StringUtils.removeEnd(jiraCloudAddress, "/");
			zephyrCloudAddress = org.apache.commons.lang.StringUtils.removeEnd(zephyrCloudAddress, "/");

			if (org.apache.commons.lang.StringUtils.isBlank(jiraCloudAddress)) {
				return FormValidation.error("Please enter the JIRA Cloud URL");
			}

			if (org.apache.commons.lang.StringUtils.isBlank(zephyrCloudAddress)) {
				return FormValidation.error("Please enter the Zephyr for JIRA Cloud base URL");
			}

			if (org.apache.commons.lang.StringUtils.isBlank(jiraCloudUserName)) {
				return FormValidation.error("Please enter the JIRA Cloud user name");
			}
			if (org.apache.commons.lang.StringUtils.isBlank(jiraCloudPassword)) {
				return FormValidation.error("Please enter the JIRA Cloud user password");
			}
			if (org.apache.commons.lang.StringUtils.isBlank(zephyrCloudAccessKey)) {
				return FormValidation.error("Please enter the Zephyr for JIRA Cloud access key");
			}
			if (org.apache.commons.lang.StringUtils.isBlank(zephyrCloudSecretKey)) {
				return FormValidation.error("Please enter the Zephyr for JIRA Cloud secret key");
			}

			if (!(jiraCloudAddress.trim().startsWith("https://") || jiraCloudAddress.trim().startsWith("http://"))) {
				return FormValidation.error("Incorrect server address format (JIRA Cloud)");
			}

			if (!(zephyrCloudAddress.trim().startsWith("https://") || zephyrCloudAddress.trim().startsWith("http://"))) {
				return FormValidation.error("Incorrect server address format (Zephyr for JIRA Cloud)");
			}

			String jiraCloudAddr = URLValidator.validateURL(jiraCloudAddress);

			if(!jiraCloudAddr.startsWith("http")) {
				return FormValidation.error(jiraCloudAddr);
			}

			String zephyrCloudAddr = URLValidator.validateURL(zephyrCloudAddress);

			if(!zephyrCloudAddr.startsWith("http")) {
				return FormValidation.error(zephyrCloudAddr);
			}

			RestClient restClient = new RestClient(jiraCloudAddr, jiraCloudUserName, jiraCloudPassword);

			if (!ServerInfo.findServerAddressIsValidZephyrURL(restClient)) {
				return FormValidation.error("This is not a valid Jira Server");
			}

			if (!ServerInfo.validateCredentials(restClient)) {
				return FormValidation.error("Invalid user credentials");
			}
			restClient.destroy();

			RestClient restClient2  = new RestClient(jiraCloudAddress, jiraCloudUserName, jiraCloudPassword, zephyrCloudAddress, zephyrCloudAccessKey, zephyrCloudSecretKey);


			Map<Boolean, String> findServerAddressIsValidZephyrCloudURL = ServerInfo.findServerAddressIsValidZephyrCloudURL(restClient2);
			if (!findServerAddressIsValidZephyrCloudURL.containsKey(true)) {
				return FormValidation.error(findServerAddressIsValidZephyrCloudURL.get(false));
			}

			return FormValidation.ok("Validated sucessfully !");
		}
	}

	@PostConstruct
	public void finish() {
		jiraCloudAddress = StringUtils.removeEnd(jiraCloudAddress, "/");
		zephyrCloudAddress = StringUtils.removeEnd(zephyrCloudAddress, "/");
	}

}
