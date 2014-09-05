package at.tomtasche.nomstagram.vm;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class MainApplication {

    public static void main(String[] args) throws Exception {
        QueuedThreadPool threadPool = new QueuedThreadPool(100, 50);
        Server server = new Server(threadPool);

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);

        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new TastyServlet()), "/*");

        server.setHandler(context);

        server.start();
        server.join();
    }
}
