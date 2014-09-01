package at.tomtasche.nomstagram.vm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

	private final Logger logger;

	private final File beliefDirectory;
	private final File cacheDirectory;

	private File resultsFile;

	public TastyServlet() {
		logger = Logger.getLogger("Beliefer");

		beliefDirectory = new File("deepbelief");
		cacheDirectory = new File("cache");

		resultsFile = new File("results.txt");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO:
		// http://localhost:8080/?photoUrl=http%3A%2F%2Fwww.mcdonalds.ca%2Fcontent%2Fdam%2FCanada%2Fen%2FPromo%2Fblt%2Fimg%2Fburger.png

		String output = initializeBeliefer();

		if (!cacheDirectory.exists()) {
			cacheDirectory.mkdir();
		}

		String photoUrl = req.getParameter("photoUrl");
		photoUrl = URLDecoder.decode(photoUrl, "UTF-8");

		File tempFile = File.createTempFile("photo-", ".png", cacheDirectory);

		URL website = new URL(photoUrl);
		logger.log(Level.SEVERE, website.toString());
		Files.copy(website.openStream(), tempFile.toPath(),
				StandardCopyOption.REPLACE_EXISTING);

		// String output = executeCommand("./deepbelief burger.png",
		// beliefDirectory);

		output = executeCommand("./deepbelief " + tempFile.getAbsolutePath(),
				beliefDirectory);

		FileWriter fileWriter = new FileWriter(resultsFile);
		fileWriter.append(System.getProperty("line.separator"));
		fileWriter.append(output);
		fileWriter.flush();
		fileWriter.close();

		resp.setContentType("text/plain");
		resp.getWriter().println(output);
		resp.getWriter().flush();

		// TODO: delete files in future versions
		// tempFile.delete();
	}

	private String initializeBeliefer() {
		if (beliefDirectory.exists()) {
			logger.log(Level.INFO, "beliefer already initialized");

			return "";
		}

		logger.log(Level.INFO, "beliefer not yet initialized");

		beliefDirectory.mkdir();

		String output = executeCommand(
				"wget -nv -O belief.zip http://ge.tt/api/1/files/9H5Ygvq1/0/blob?download",
				beliefDirectory);

		logger.log(Level.INFO, "beliefer downloaded");

		output = executeCommand("unzip belief.zip", beliefDirectory);

		logger.log(Level.INFO, "beliefer already unzipped");

		// output = executeCommand("./deepbelief", beliefDirectory);

		return output;
	}

	private String executeCommand(String command, File directory) {
		StringBuffer output = new StringBuffer();

		try {
			Process process = Runtime.getRuntime().exec(command, null,
					directory);
			process.waitFor();

			output.append(streamToString(process.getInputStream()));
			output.append(streamToString(process.getErrorStream()));
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