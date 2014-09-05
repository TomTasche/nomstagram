package at.tomtasche.nomstagram.vm;

import com.jetpac.deepbelief.DeepBelief;
import com.jetpac.deepbelief.DeepBelief.JPCNNLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class TastyServlet extends HttpServlet {

    private static final String LINE_SEPARATOR = System
            .getProperty("line.separator");

    private final Object beliefLock = new Object();

    private final Logger logger;

    private final File cacheDirectory;

    private Pointer networkHandle;

    public TastyServlet() {
        logger = Logger.getLogger("Beliefer");

        cacheDirectory = new File("cache");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // TODO:
        // http://localhost:8080/?photoUrl=http%3A%2F%2Fwww.mcdonalds.ca%2Fcontent%2Fdam%2FCanada%2Fen%2FPromo%2Fblt%2Fimg%2Fburger.png

        if (!cacheDirectory.exists()) {
            cacheDirectory.mkdir();
        }

        String photoUrl = req.getParameter("photoUrl");
        photoUrl = URLDecoder.decode(photoUrl, "UTF-8");

        File tempFile = File.createTempFile("photo-", ".png", cacheDirectory);

        URL website = new URL(photoUrl);
        logger.log(Level.INFO, website.toString());
        Files.copy(website.openStream(), tempFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);

        if (networkHandle == null) {
            logger.log(Level.INFO,
                    "working directory " + new File("").getAbsolutePath());

            JPCNNLibrary beliefer = DeepBelief.JPCNNLibrary.INSTANCE;
            networkHandle = beliefer.jpcnn_create_network(new File(
                    "jetpac.ntwk").toString());
        }

        Pointer imageHandle = JPCNNLibrary.INSTANCE
                .jpcnn_create_image_buffer_from_file(tempFile.getAbsolutePath());

        PointerByReference predictionsValuesRef = new PointerByReference();
        IntByReference predictionsLengthRef = new IntByReference();
        PointerByReference predictionsNamesRef = new PointerByReference();
        IntByReference predictionsNamesLengthRef = new IntByReference();

        long startTimestamp = System.currentTimeMillis();

        synchronized (beliefLock) {
            JPCNNLibrary.INSTANCE.jpcnn_classify_image(networkHandle,
                    imageHandle, 0, 0, predictionsValuesRef,
                    predictionsLengthRef, predictionsNamesRef,
                    predictionsNamesLengthRef);
        }

        long stopTimestamp = System.currentTimeMillis();
        long duration = stopTimestamp - startTimestamp;

        logger.log(Level.INFO, "jpcnn_classify_image() took " + duration
                + " ms");

        JPCNNLibrary.INSTANCE.jpcnn_destroy_image_buffer(imageHandle);

        Pointer predictionsValuesPointer = predictionsValuesRef.getValue();
        int predictionsLength = predictionsLengthRef.getValue();
        Pointer predictionsNamesPointer = predictionsNamesRef.getValue();

        float[] predictionsValues = predictionsValuesPointer.getFloatArray(0,
                predictionsLength);
        Pointer[] predictionsNames = predictionsNamesPointer.getPointerArray(0);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < predictionsLength; i += 1) {
            float predictionValue = predictionsValues[i];

            if (predictionValue > 0.1) {
                String name = predictionsNames[i].getString(0);
                builder.append(String.format("%s = %f", name, predictionValue));
                builder.append(LINE_SEPARATOR);
            }
        }

        // TODO: destroy network on server shutdown
        // JPCNNLibrary.INSTANCE.jpcnn_destroy_network(networkHandle);

        resp.setContentType("text/plain");
        resp.getWriter().println(builder.toString());
        resp.getWriter().flush();

        // TODO: delete files in future versions
        // tempFile.delete();
    }
}