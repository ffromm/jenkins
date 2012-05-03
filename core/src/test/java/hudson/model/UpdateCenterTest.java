/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
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
package hudson.model;

import com.trilead.ssh2.crypto.Base64;
import hudson.util.TimeUnit2;
import junit.framework.TestCase;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * Quick test for {@link UpdateCenter}.
 * 
 * @author Kohsuke Kawaguchi
 */
public class UpdateCenterTest extends TestCase {
    public void testData() throws Exception {
        // check if we have the internet connectivity. See HUDSON-2095
        try {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            List<Future<InputStream>> futures = executor.invokeAll(Arrays.asList(new Task()), 2, TimeUnit.SECONDS);
            executor.shutdown();

            if(futures != null && futures.size() > 0 && futures.get(0).isCancelled()) {
                System.out.println("Skipping this test. No internet connectivity in 2 seconds");
                return;
            }
        } catch (Exception e) {
            System.out.println("Skipping this test. No internet connectivity");
            return;
        }

        URL url = new URL("http://updates.jenkins-ci.org/update-center.json?version=build");
        String jsonp = IOUtils.toString(url.openStream());
        JSONObject json = JSONObject.fromObject(jsonp.substring(jsonp.indexOf('(') + 1, jsonp.lastIndexOf(')')));

        UpdateSite us = new UpdateSite("default", url.toExternalForm());
        UpdateSite.Data data = us.new Data(json);
        assertTrue(data.core.url.startsWith("http://updates.jenkins-ci.org/"));
        assertTrue(data.plugins.containsKey("rake"));
        System.out.println(data.core.url);

        // make sure the certificate is valid for a while more
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        JSONObject signature = json.getJSONObject("signature");
        for (Object cert : signature.getJSONArray("certificates")) {
            X509Certificate c = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(Base64.decode(cert.toString().toCharArray())));
            c.checkValidity(new Date(System.currentTimeMillis() + TimeUnit2.DAYS.toMillis(30)));
        }
    }

    class Task implements Callable<InputStream> {
        public InputStream call() throws Exception {
            return new URL("http://updates.jenkins-ci.org/").openStream();
        }
    }
}
