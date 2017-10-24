package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.credhub;

import java.util.ArrayList;
import java.util.List;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest

public class PasswordGenProcessorTest {

	

	@Test
	public void testPasswordGenProcessor() {

		String url="https://credhub.internal.paas";
		String instanceGroupName="ig";
		String propertyName="prop";
		

		PasswordGenProcessor processor=new PasswordGenProcessor(url,instanceGroupName, propertyName);
		
		List<BrokerProcessor> processors=new ArrayList<BrokerProcessor>();
		processors.add(processor);
		ProcessorChain chain=new ProcessorChain(processors, new DefaultBrokerSink());
		chain.create();
	}

	
}
