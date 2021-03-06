package com.muleinaction;

import org.mule.api.service.Service;
import org.mule.api.context.notification.EndpointMessageNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.tck.FunctionalTestCase;
import org.mule.context.notification.EndpointMessageNotification;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author John D'Emic (john.demic@gmail.com)
 */
public class HttpPollingFunctionalTestCase extends FunctionalTestCase {

    private static String DEST_DIRECTORY = "./data/polling";

    private CountDownLatch latch = new CountDownLatch(1);

    protected void doSetUp() throws Exception {
        super.doSetUp();

        for (Object o : FileUtils.listFiles(new File(DEST_DIRECTORY), new String[]{"html"}, false)) {
            File file = (File) o;
            file.delete();
        }

        muleContext.registerListener(new EndpointMessageNotificationListener() {
            public void onNotification(final ServerNotification notification) {
                if ("httpPollingService".equals(notification.getResourceIdentifier())) {
                    final EndpointMessageNotification messageNotification = (EndpointMessageNotification) notification;
                    if (messageNotification.getEndpoint().contains("polling")) {
                        latch.countDown();
                    }
                }
            }
        });
    }

    @Override
    protected String getConfigResources() {
        return "conf/http-polling-config.xml";
    }

    public void testCorrectlyInitialized() throws Exception {
        final Service service = muleContext.getRegistry().lookupService(
                "httpPollingService");

        assertNotNull(service);
        assertEquals("httpPollingModel", service.getModel().getName());
        assertTrue("Message did not reach directory on time", latch.await(15, TimeUnit.SECONDS));
        
        File destDir = new File(DEST_DIRECTORY);
        waitForDirNotEmpty(destDir);
        assertEquals(1, FileUtils.listFiles(destDir, new WildcardFileFilter("*.*"), null).size());
    }

    private void waitForDirNotEmpty(File destDir) throws Exception {
        while(FileUtils.listFiles(destDir, new WildcardFileFilter("*.*"), null).size() == 0) {
            Thread.sleep(250);
        }
    }

}
