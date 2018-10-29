/*
 * The MIT License
 *
 * Copyright (c) 2004-, Kohsuke Kawaguchi, Sun Microsystems, Inc., and a number of other of contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.ec2;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeSpotPriceHistoryRequest;
import com.amazonaws.services.ec2.model.DescribeSpotPriceHistoryResult;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.SpotPrice;
import hudson.model.Label;
import hudson.model.Node;
import hudson.slaves.Cloud;
import org.junit.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * @author Kohsuke Kawaguchi
 */
public class AmazonEC2CloudTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
        AmazonEC2Cloud.testMode = true;
    }

    @After
    public void tearDown() throws Exception {
        AmazonEC2Cloud.testMode = false;
    }

    @Test
    @Ignore
    public void testConfigRoundtrip() throws Exception {
        AmazonEC2Cloud orig = new AmazonEC2Cloud("us-east-1", true, "abc", "us-east-1", "ghi", "3", Collections.<SlaveTemplate> emptyList(),"roleArn", "roleSessionName");
r.jenkins.clouds.add(orig);
        r.submit(r.createWebClient().goTo("configure").getFormByName("config"));

        Cloud actual = r.jenkins.clouds.iterator().next();
        r.assertEqualBeans(orig, actual, "cloudName,region,useInstanceProfileForCredentials,accessId,privateKey,instanceCap,roleArn,roleSessionName");
    }

    @Test
    public void testCheapestSpotInstanceUsed() {

        final String expensiveZone = "eu-west-1a";
        final String middleZone = "eu-west-1b";
        final String cheapZone = "eu-west-1c";

        final String label = "myLabel";
        SlaveTemplate expensiveSpot = new SlaveTemplate("1", expensiveZone, new SpotConfiguration("0.33"), "default", "foo", InstanceType.M1Large, false, label, Node.Mode.NORMAL, "", "bar", "bbb", "aaa", "10", "fff", null, "-Xmx1g", false, "subnet 456", null, null, false, null, "iamInstanceProfile", false, false, false, null, true, "", false, true);
        SlaveTemplate cheapestSpot = new SlaveTemplate("2", cheapZone, new SpotConfiguration("0.33"), "default", "foo", InstanceType.M1Large, false, label, Node.Mode.NORMAL, "", "bar", "bbb", "aaa", "10", "fff", null, "-Xmx1g", false, "subnet 456", null, null, false, null, "iamInstanceProfile", false, false, false, null, true, "", false, true);
        SlaveTemplate standard = new SlaveTemplate("3", "eu-west-1b", null, "default", "foo", InstanceType.M1Large, false, label, Node.Mode.NORMAL, "", "bar", "bbb", "aaa", "10", "fff", null, "-Xmx1g", false, "subnet 456", null, null, false, null, "iamInstanceProfile", false, false, false, null, true, "", false, true);
        SlaveTemplate middleSpot = new SlaveTemplate("4", middleZone, new SpotConfiguration("0.33"), "default", "foo", InstanceType.M1Large, false, label, Node.Mode.NORMAL, "", "bar", "bbb", "aaa", "10", "fff", null, "-Xmx1g", false, "subnet 456", null, null, false, null, "iamInstanceProfile", false, false, false, null, true, "", false, true);

        List<SlaveTemplate> templates = new ArrayList<>();
        // The expensive one is first in the list. This is important.
        templates.add(expensiveSpot);
        templates.add(cheapestSpot);
        templates.add(standard);
        templates.add(middleSpot);

        // Mock an Amazon EC2 Connection
        AmazonEC2 ec2 = Mockito.mock(AmazonEC2.class);
        Mockito.when(ec2.describeSpotPriceHistory(any(DescribeSpotPriceHistoryRequest.class))).thenAnswer(
                invocation -> {
                    final String zone = invocation.getArgumentAt(0, DescribeSpotPriceHistoryRequest.class).getAvailabilityZone();

                    DescribeSpotPriceHistoryResult result = new DescribeSpotPriceHistoryResult();
                    SpotPrice spotPrice = new SpotPrice();
                    result.setSpotPriceHistory(Collections.singletonList(spotPrice));

                    spotPrice.setAvailabilityZone(zone);
                    spotPrice.setInstanceType(InstanceType.M1Large);

                    if (zone.equals(expensiveZone)) {
                        spotPrice.setSpotPrice("34.3");
                    } else if (zone.equals(middleZone)) {
                        spotPrice.setSpotPrice("1.12");
                    } else if (zone.equals(cheapZone)) {
                        spotPrice.setSpotPrice("0.3324");
                    } else {
                        // die
                        throw new IllegalArgumentException("Can't recognise zone");
                    }

                    return result;
                });

        // Spy on the EC2Cloud object to intercept the call to .connect() and return our mocked cloud
        EC2Cloud rawCloud = new AmazonEC2Cloud("", false, "", "eu-west-1", "", "", templates);
        EC2Cloud spiedCloud = Mockito.spy(rawCloud);
        doReturn(ec2).when(spiedCloud).connect();

        SlaveTemplate template = spiedCloud.getTemplate(Label.parse(label).iterator().next());

        assertEquals(cheapestSpot.ami, template.ami);
    }
}
