package de.skiptag.roadrunner.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
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

    @Override
    protected StreamInbound createWebSocketInbound(String subProtocol,
	    HttpServletRequest request) {
	return new RoadrunnerMessageInbound("ws://localhost:8080/", roadrunner);
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
	String snapshotDirectory = Preconditions.checkNotNull(config.getInitParameter("snapshotDirectory"), "SnapshotDirectory must be configured in web.xml");
	try {
	    Optional<File> snapshotDir = Optional.of(new File(snapshotDirectory));
	    this.roadrunner = new Roadrunner(journalDirectory, snapshotDir);
	} catch (IOException e) {
	    logger.error("Error loading Roadrunner", e);
	}
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {
	if (headerContainsToken(req, "upgrade", "websocket")) {
	    super.service(req, resp);
	} else if (req.getMethod().equals("GET")
		&& req.getRequestURI().equals("roadrunner.js")) {
	    resp.setContentType("application/javascript");
	    resp.setCharacterEncoding("UTF-8");

	    ServletOutputStream outputStream = resp.getOutputStream();
	    outputStream.write(roadrunner.loadJsFile().getBytes());
	    outputStream.close();
	} else {
	    handleRestCall(req, resp);
	}
    }

    private void handleRestCall(HttpServletRequest req, HttpServletResponse resp)
	    throws IOException, RuntimeException {
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
	    roadrunner.handleEvent(RoadrunnerEventType.SET, req.getRequestURI(), Optional.fromNullable(msg));
	} else if (req.getMethod().equals("DELETE")) {
	    roadrunner.handleEvent(RoadrunnerEventType.SET, req.getRequestURI(), null);
	}
	resp.setContentType("application/json; charset=UTF-8");
    }

    private String getHttpSocketLocation(HttpServletRequest req) {
	return "";
    }

}
