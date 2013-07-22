package de.skiptag.roadrunner.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;

import de.skiptag.roadrunner.Roadrunner;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEvent;
import de.skiptag.roadrunner.disruptor.event.RoadrunnerEventType;
import de.skiptag.roadrunner.persistence.Path;

/**
 * Servlet implementation class ChatServlet
 */
public class RoadrunnerWebSocketServlet extends WebSocketServlet {
    private static final long serialVersionUID = 1L;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RoadrunnerWebSocketServlet.class.getName());
    private Roadrunner roadrunner;
    private String journalDirectory;
    private String snapshotDirectory;

    @Override
    protected StreamInbound createWebSocketInbound(String subProtocol,
	    HttpServletRequest request) {

	String servername = ("http".equals(request.getScheme()) ? "ws://"
		: "wss://")
		+ request.getServerName()
		+ ":"
		+ request.getServerPort();
	return new RoadrunnerMessageInbound(servername, roadrunner);
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
	journalDirectory = Preconditions.checkNotNull(config.getInitParameter("journalDirectory"), "JournalDirectory must be configured in web.xml");
	snapshotDirectory = Preconditions.checkNotNull(config.getInitParameter("snapshotDirectory"), "SnapshotDirectory must be configured in web.xml");
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {
	if (roadrunner == null) {
	    String basePath = req.getScheme() + "://" + req.getServerName()
		    + ":" + req.getServerPort() + req.getContextPath() + ""
		    + req.getServletPath();
	    try {
		Optional<File> snapshotDir = Optional.of(new File(
			snapshotDirectory));
		this.roadrunner = new Roadrunner(basePath, journalDirectory,
			snapshotDir);
	    } catch (IOException e) {
		logger.error("Error loading Roadrunner", e);
	    }
	}

	if (resourceForURI(req) != null) {
	    ServletOutputStream outputStream = resp.getOutputStream();
	    Streams.copy(resourceForURI(req).openStream(), outputStream, true);
	} else if (req.getMethod().equals("GET")
		&& resourceForURI(req, "/roadrunner.js")) {
	    resp.setContentType("application/javascript");
	    resp.setCharacterEncoding("UTF-8");

	    ServletOutputStream outputStream = resp.getOutputStream();
	    outputStream.write(roadrunner.loadJsFile().getBytes());
	    outputStream.close();
	} else if (headerContainsToken(req, "upgrade", "websocket")) {
	    super.service(req, resp);
	} else {
	    handleRestCall(req, resp);
	}
    }

    private URL resourceForURI(HttpServletRequest req)
	    throws MalformedURLException {
	String requestURI = req.getRequestURI();
	String contextPath = req.getContextPath();
	String resourcePath = requestURI.replaceAll(contextPath, "");
	URL url = req.getServletContext().getResource(resourcePath);
	return url;
    }

    private boolean resourceForURI(HttpServletRequest req, String uri)
	    throws MalformedURLException {
	String requestURI = req.getRequestURI();
	String contextPath = req.getContextPath();
	String resourcePath = requestURI.replaceAll(contextPath, "");
	return uri.equals(resourcePath);
    }

    private void handleRestCall(HttpServletRequest req, HttpServletResponse resp)
	    throws IOException, RuntimeException {
	Path nodePath = new Path(
		RoadrunnerEvent.extractPath(req.getRequestURI()));
	if (req.getMethod().equals("GET")) {
	    resp.getWriter().append(roadrunner.getPersistence()
		    .get(nodePath)
		    .toString());
	} else if (req.getMethod().equals("POST")
		|| req.getMethod().equals("PUT")) {
	    String msg = new String(CharStreams.toString(new InputStreamReader(
		    req.getInputStream(), Charsets.UTF_8)));
	    roadrunner.handleEvent(RoadrunnerEventType.SET, req.getRequestURI(), Optional.fromNullable(msg));
	} else if (req.getMethod().equals("DELETE")) {
	    roadrunner.handleEvent(RoadrunnerEventType.SET, req.getRequestURI(), null);
	}
	resp.setContentType("application/json; charset=UTF-8");
    }
}
