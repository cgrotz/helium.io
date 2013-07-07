package de.skiptag.roadrunner.server;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;

import com.google.common.io.Resources;

/**
 * Servlet implementation class ChatServlet
 */
public class RoadrunnerWebSocketServlet extends WebSocketServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected StreamInbound createWebSocketInbound(String subProtocol,
	    HttpServletRequest request) {
	String contextPath = request.getContextPath();
	String servletPath = request.getServletPath();
	String requestURI = request.getRequestURI();
	String collectionPath = requestURI.substring((contextPath + servletPath).length() + 1);
	try {
	    return new RoadrunnerMessageInbound(contextPath + servletPath,
		    collectionPath);
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {
	if (headerContainsToken(req, "upgrade", "websocket")) {
	    super.doGet(req, resp);
	} else {
	    URL url = RoadrunnerWebSocketServlet.class.getClassLoader()
		    .getResource("roadrunner.js");
	    resp.setContentType("application/javascript");
	    resp.setCharacterEncoding("UTF-8");

	    ServletOutputStream outputStream = resp.getOutputStream();
	    Resources.copy(url, outputStream);
	    outputStream.close();
	}
    }

    /*
     * This only works for tokens. Quoted strings need more sophisticated
     * parsing.
     */
    private boolean headerContainsToken(HttpServletRequest req,
	    String headerName, String target) {
	Enumeration<String> headers = req.getHeaders(headerName);
	while (headers.hasMoreElements()) {
	    String header = headers.nextElement();
	    String[] tokens = header.split(",");
	    for (String token : tokens) {
		if (target.equalsIgnoreCase(token.trim())) {
		    return true;
		}
	    }
	}
	return false;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
	super.init(config);
    }

}
