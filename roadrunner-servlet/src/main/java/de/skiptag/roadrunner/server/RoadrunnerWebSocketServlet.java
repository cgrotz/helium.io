package de.skiptag.roadrunner.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.util.Base64;
import org.apache.catalina.websocket.Constants;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WsHttpServletRequestWrapper;
import org.apache.tomcat.util.buf.B2CConverter;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.apache.tomcat.util.res.StringManager;
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
public class RoadrunnerWebSocketServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final byte[] WS_ACCEPT = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11".getBytes(B2CConverter.ISO_8859_1);
    private static final StringManager sm = StringManager.getManager(Constants.Package);

    private final Queue<MessageDigest> sha1Helpers = new ConcurrentLinkedQueue<MessageDigest>();
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RoadrunnerWebSocketServlet.class.getName());
    private Roadrunner roadrunner;
    private String journalDirectory;
    private String snapshotDirectory;

    protected StreamInbound createWebSocketInbound(String subProtocol,
	    HttpServletRequest request) {
	String servername;
	if (request.getScheme().startsWith("ws://")) {
	    servername = "http://" + request.getServerName() + ":"
		    + request.getServerPort();
	} else if (request.getScheme().startsWith("wss://")) {
	    servername = "https://" + request.getServerName() + ":"
		    + request.getServerPort();
	} else {
	    servername = request.getScheme() + "://" + request.getServerName()
		    + ":" + request.getServerPort();
	}
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
		RoadrunnerEvent.extractPath(req.getRequestURI(), null));
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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {

	// Information required to send the server handshake message
	String key;
	String subProtocol = null;
	List<String> extensions = Collections.emptyList();

	if (!headerContainsToken(req, "upgrade", "websocket")) {
	    super.doGet(req, resp);
	}

	if (!headerContainsToken(req, "connection", "upgrade")) {
	    super.doGet(req, resp);
	}

	if (!headerContainsToken(req, "sec-websocket-version", "13")) {
	    resp.setStatus(426);
	    resp.setHeader("Sec-WebSocket-Version", "13");
	    return;
	}

	key = req.getHeader("Sec-WebSocket-Key");
	if (key == null) {
	    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
	    return;
	}

	String origin = req.getHeader("Origin");
	if (!verifyOrigin(origin)) {
	    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
	    return;
	}

	List<String> subProtocols = getTokensFromHeader(req, "Sec-WebSocket-Protocol");
	if (!subProtocols.isEmpty()) {
	    subProtocol = selectSubProtocol(subProtocols);

	}

	// TODO Read client handshake - Sec-WebSocket-Extensions

	// TODO Extensions require the ability to specify something (API TBD)
	// that can be passed to the Tomcat internals and process extension
	// data present when the frame is fragmented.

	// If we got this far, all is good. Accept the connection.
	resp.setHeader("Upgrade", "websocket");
	resp.setHeader("Connection", "upgrade");
	resp.setHeader("Sec-WebSocket-Accept", getWebSocketAccept(key));
	if (subProtocol != null) {
	    resp.setHeader("Sec-WebSocket-Protocol", subProtocol);
	}
	if (!extensions.isEmpty()) {
	    // TODO
	}

	WsHttpServletRequestWrapper wrapper = new WsHttpServletRequestWrapper(
		req);
	StreamInbound inbound = createWebSocketInbound(subProtocol, wrapper);
	try {
	    invalidate(wrapper);
	} catch (Exception e) {
	    e.printStackTrace();
	}

	// Small hack until the Servlet API provides a way to do this.
	ServletRequest inner = req;
	// Unwrap the request
	while (inner instanceof ServletRequestWrapper) {
	    inner = ((ServletRequestWrapper) inner).getRequest();
	}
	if (inner instanceof RequestFacade) {
	    ((RequestFacade) inner).doUpgrade(inbound);
	} else {
	    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, sm.getString("servlet.reqUpgradeFail"));
	}
    }

    public void invalidate(WsHttpServletRequestWrapper wrapper)
	    throws NoSuchMethodException, IllegalAccessException,
	    InvocationTargetException {
	Method method = WsHttpServletRequestWrapper.class.getDeclaredMethod("invalidate");
	boolean accessible = method.isAccessible();
	method.setAccessible(true);
	method.invoke(wrapper);
	method.setAccessible(accessible);
    }

    /*
     * This only works for tokens. Quoted strings need more sophisticated
     * parsing.
     */
    private List<String> getTokensFromHeader(HttpServletRequest req,
	    String headerName) {
	List<String> result = new ArrayList<String>();

	Enumeration<String> headers = req.getHeaders(headerName);
	while (headers.hasMoreElements()) {
	    String header = headers.nextElement();
	    String[] tokens = header.split(",");
	    for (String token : tokens) {
		result.add(token.trim());
	    }
	}
	return result;
    }

    private String getWebSocketAccept(String key) throws ServletException {

	MessageDigest sha1Helper = sha1Helpers.poll();
	if (sha1Helper == null) {
	    try {
		sha1Helper = MessageDigest.getInstance("SHA1");
	    } catch (NoSuchAlgorithmException e) {
		throw new ServletException(e);
	    }
	}

	sha1Helper.reset();
	sha1Helper.update(key.getBytes(B2CConverter.ISO_8859_1));
	String result = Base64.encode(sha1Helper.digest(WS_ACCEPT));

	sha1Helpers.add(sha1Helper);

	return result;
    }

    /**
     * Intended to be overridden by sub-classes that wish to verify the origin
     * of a WebSocket request before processing it.
     * 
     * @param origin
     *            The value of the origin header from the request which may be
     *            <code>null</code>
     * 
     * @return <code>true</code> to accept the request. <code>false</code> to
     *         reject it. This default implementation always returns
     *         <code>true</code>.
     */
    protected boolean verifyOrigin(String origin) {
	return true;
    }

    /**
     * Intended to be overridden by sub-classes that wish to select a
     * sub-protocol if the client provides a list of supported protocols.
     * 
     * @param subProtocols
     *            The list of sub-protocols supported by the client in client
     *            preference order. The server is under no obligation to respect
     *            the declared preference
     * @return <code>null</code> if no sub-protocol is selected or the name of
     *         the protocol which <b>must</b> be one of the protocols listed by
     *         the client. This default implementation always returns
     *         <code>null</code>.
     */
    protected String selectSubProtocol(List<String> subProtocols) {
	return null;
    }
}
