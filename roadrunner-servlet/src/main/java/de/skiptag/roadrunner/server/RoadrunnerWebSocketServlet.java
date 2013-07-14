package de.skiptag.roadrunner.server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.json.JSONException;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;

import de.skiptag.roadrunner.RoadrunnerStandalone;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventType;
import de.skiptag.roadrunner.persistence.Path;

/**
 * Servlet implementation class ChatServlet
 */
public class RoadrunnerWebSocketServlet extends WebSocketServlet {
    private static final long serialVersionUID = 1L;
    private RoadrunnerStandalone roadrunner;

    @Override
    protected StreamInbound createWebSocketInbound(String subProtocol,
	    HttpServletRequest request) {
	return new RoadrunnerMessageInbound(roadrunner);
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
	String journalDirectory = Preconditions.checkNotNull(config.getInitParameter("journalDirectory"), "JournalDirectory must be configured in web.xml");
	this.roadrunner = new RoadrunnerStandalone(journalDirectory);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {
	if (headerContainsToken(req, "upgrade", "websocket")) {
	    super.service(req, resp);
	} else if (req.getMethod().equals("GET")
		&& req.getRequestURI().equals("roadrunner.js")) {
	    URL url = RoadrunnerWebSocketServlet.class.getClassLoader()
		    .getResource("roadrunner.js");
	    resp.setContentType("application/javascript");
	    resp.setCharacterEncoding("UTF-8");

	    ServletOutputStream outputStream = resp.getOutputStream();
	    Resources.copy(url, outputStream);
	    outputStream.close();
	} else {
	    try {
		handleRestCall(req, resp);
	    } catch (JSONException e) {
		throw new RuntimeException(e);
	    }
	}
    }

    private void handleRestCall(HttpServletRequest req, HttpServletResponse resp)
	    throws IOException, JSONException {
	String basePath = getHttpSocketLocation(req);
	Path nodePath = new Path(
		RoadrunnerEvent.extractPath(basePath, req.getRequestURI()));
	if (req.getMethod().equals("GET")) {
	    resp.getWriter().append(roadrunner.getPersistence()
		    .get(nodePath)
		    .toString());
	} else if (req.getMethod().equals("POST")
		|| req.getMethod().equals("PUT")) {
	    String msg = new String(CharStreams.toString(new InputStreamReader(
		    req.getInputStream(), Charsets.UTF_8)));
	    roadrunner.handleEvent(RoadrunnerEventType.SET, req.getRequestURI(), msg);
	} else if (req.getMethod().equals("DELETE")) {
	    roadrunner.handleEvent(RoadrunnerEventType.SET, req.getRequestURI(), null);
	}
	resp.setContentType("application/json; charset=UTF-8");
    }

    private String getHttpSocketLocation(HttpServletRequest req) {
	return "";
    }

}
