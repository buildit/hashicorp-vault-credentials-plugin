package utilities;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HttpJsonServer {

    public static Server startServer(String base, Integer port) throws Exception {

        Server server = new Server(port);

        ServletContextHandler ctx = new ServletContextHandler();
        ctx.setContextPath("/");

        DefaultServlet defaultServlet = new MyDefaultServlet();
        ServletHolder holderPwd = new ServletHolder("default", defaultServlet);
        holderPwd.setInitParameter("resourceBase", base);

        ctx.addServlet(holderPwd, "/*");//LINE N

        server.setHandler(ctx);

        server.start();

        return server;
    }

    private static class MyDefaultServlet extends DefaultServlet {
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            response.setHeader("Content-Type", "application/json");
            super.doGet(request, response);
        }
    }
}
