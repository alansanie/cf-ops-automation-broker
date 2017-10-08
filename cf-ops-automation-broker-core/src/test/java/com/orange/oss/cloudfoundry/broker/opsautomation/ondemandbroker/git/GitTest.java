package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.BrokerMediation;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.DefaultBrokerMediationSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.MediationChain;

//@RunWith(SpringRunner.class)
@PropertySource("classpath:/git.properties")
@SpringBootTest
public class GitTest {

	@Autowired
	GitTestProperties gitProperties;
	
	@Test
	public void testGitMediation() {
		
		GitMediation mediation=new GitMediation(gitProperties.getGitUser(), gitProperties.getGitPassword(), gitProperties.getGitUrl());
		List<BrokerMediation> mediations=new ArrayList<BrokerMediation>();
		mediations.add(mediation);
		MediationChain chain=new MediationChain(mediations, new DefaultBrokerMediationSink());
		
		chain.create();
	}
	
}
