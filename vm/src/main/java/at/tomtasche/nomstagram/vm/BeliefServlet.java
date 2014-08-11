package at.tomtasche.nomstagram.vm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by tom on 02.08.14.
 */
public class BeliefServlet extends HttpServlet {

    private final Logger logger;

    public BeliefServlet() {
        logger = Logger.getLogger("Beliefer");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.getWriter().println("belief me!");

        File beliefDirectory = new File("deepbelief");

        if (beliefDirectory.exists()) {
            // String output = executeCommand("ls", exampleDirectory);
            String output = executeCommand("./deepbelief burger.png", beliefDirectory);
        } else {
            beliefDirectory.mkdir();

            String output = executeCommand(
                    "wget -nv -O belief.zip http://ge.tt/api/1/files/9H5Ygvq1/0/blob?download",
                    beliefDirectory);

            output = executeCommand("unzip belief.zip", beliefDirectory);

            output = executeCommand("./deepbelief", beliefDirectory);
        }
    }

    private String executeCommand(String command, File directory) {
        StringBuffer output = new StringBuffer();

        try {
            Process process = Runtime.getRuntime().exec(command, null,
                    directory);
            process.waitFor();

            streamToString(process.getInputStream());
            streamToString(process.getErrorStream());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return output.toString();
    }

    private String streamToString(InputStream stream) throws IOException {
        StringBuilder builder = new StringBuilder();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream));

        String line = "";
        while ((line = reader.readLine()) != null) {
            builder.append(line + "\n");

            logger.log(Level.FINE, line);
        }

        return builder.toString();
    }
}
