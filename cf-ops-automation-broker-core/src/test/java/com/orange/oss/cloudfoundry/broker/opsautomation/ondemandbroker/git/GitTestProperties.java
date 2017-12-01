package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

//@Service
public class GitTestProperties {

	@Value("${gitUser}")	
	private String gitUser;

	@Value("${gitPassword}")	
	private String gitPassword;

	@Value("${gitUrl}")	
	private String gitUrl;


	public String getGitUser() {
		return gitUser;
	}
	public void setGitUser(String gitUser) {
		this.gitUser = gitUser;
	}
	
	public String getGitPassword() {
		return gitPassword;
	}
	public void setGitPassword(String gitPassword) {
		this.gitPassword = gitPassword;
	}

	public String getGitUrl() {
		return gitUrl;
	}
	public void setGitUrl(String gitUrl) {
		this.gitUrl = gitUrl;
	}

	
}