package com.muleinaction;

import org.mule.api.service.Service;
import org.mule.api.context.notification.EndpointMessageNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.tck.FunctionalTestCase;
import org.mule.context.notification.EndpointMessageNotification;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.util.UUID;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author John D'Emic (john.demic@gmail.com)
 */
public class FileXmlFilterTransportFunctionalTestCase extends FunctionalTestCase {

    private static String SOURCE_DIRECTORY = "./data/snapshot";
    private static String DEST_DIRECTORY = "./data/archive";

    private CountDownLatch latch = new CountDownLatch(1);

    protected void doSetUp() throws Exception {
        super.doSetUp();

        for (Object o : FileUtils.listFiles(new File(SOURCE_DIRECTORY), new String[]{"xml"}, false)) {
            File file = (File) o;
            file.delete();
        }

        for (Object o : FileUtils.listFiles(new File(DEST_DIRECTORY), new String[]{"bak","xml"}, false)) {
            File file = (File) o;
            file.delete();
        }
        muleContext.registerListener(new EndpointMessageNotificationListener() {
            public void onNotification(final ServerNotification notification) {
                if ("fileService".equals(notification.getResourceIdentifier())) {
                    final EndpointMessageNotification messageNotification = (EndpointMessageNotification) notification;
                    if (messageNotification.getEndpoint().contains("archive")) {
                        latch.countDown();
                    }
                }
            }
        });
    }

    @Override
    protected String getConfigResources() {
        return "conf/file-xml-filter-config.xml";
    }

    public void testCorrectlyInitialized() throws Exception {
        final Service service = muleContext.getRegistry().lookupService(
                "fileService");
        assertNotNull(service);
        assertEquals("fileModel", service.getModel().getName());
    }

    public void testFileIsArchived() throws IOException, InterruptedException {
        String filename = "SNAPSHOT-" + UUID.randomUUID().toString() + ".xml";
        BufferedWriter out = new BufferedWriter(new FileWriter(SOURCE_DIRECTORY + "/" + filename));
        out.write("data");
        out.close();
        assertTrue("Message did not reach directory on time", latch.await(15, TimeUnit.SECONDS));
        assertEquals(1, FileUtils.listFiles(new File(DEST_DIRECTORY), new WildcardFileFilter("*.xml"), null).size());
    }

}
