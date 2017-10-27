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
        AmazonEC2Cloud orig = new AmazonEC2Cloud("us-east-1", true, "abc", "us-east-1", "ghi", "3", Collections.<SlaveTemplate> emptyList());
        r.jenkins.clouds.add(orig);
        r.submit(r.createWebClient().goTo("configure").getFormByName("config"));

        Cloud actual = r.jenkins.clouds.iterator().next();
        r.assertEqualBeans(orig, actual, "cloudName,region,useInstanceProfileForCredentials,accessId,privateKey,instanceCap");
    }
}
