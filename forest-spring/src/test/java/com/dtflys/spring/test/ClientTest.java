package com.dtflys.spring.test;

import com.dtflys.forest.Forest;
import com.dtflys.forest.config.ForestConfiguration;
import junit.framework.TestCase;
import com.dtflys.spring.test.client0.BeastshopClient;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.annotation.Resource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author gongjun[jun.gong@thebeastshop.com]
 * @since 2017-04-24 19:39
 */
public class ClientTest extends TestCase {

    private ClassPathXmlApplicationContext applicationContext;


    public void testScan() {
        applicationContext = new ClassPathXmlApplicationContext(
                new String[] { "classpath:client-test.xml" });
        BeastshopClient beastshopClient =
                (BeastshopClient) applicationContext.getBean("beastshopClient");
        assertNotNull(beastshopClient);
        String result = beastshopClient.index();
        assertNotNull(result);

        Object baseUrl = Forest.config().getVariableValue("baseUrl");
        assertThat(baseUrl).isNotNull().isEqualTo("http://www.thebeastshop.com");
    }

}
