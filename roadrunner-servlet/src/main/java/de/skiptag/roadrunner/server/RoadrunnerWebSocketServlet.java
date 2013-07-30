package de.skiptag.roadrunner.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsHttpServletRequestWrapper;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
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
	private static final org.slf4j.Logger logger = LoggerFactory
			.getLogger(RoadrunnerWebSocketServlet.class.getName());
	private Roadrunner roadrunner;
	private String journalDirectoryPath;
	private String snapshotDirectoryPath;
	private boolean productiveMode;

	@Override
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
		String productiveModeString = config.getInitParameter("productiveMode");
		try {
			productiveMode = Boolean.parseBoolean(productiveModeString);
		} catch (Exception e) {
			productiveMode = false;
		}
		journalDirectoryPath = Preconditions.checkNotNull(
				config.getInitParameter("journalDirectory"),
				"JournalDirectory must be configured in web.xml");
		snapshotDirectoryPath = config.getInitParameter("snapshotDirectory");
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		if (roadrunner == null) {
			String basePath = req.getScheme() + "://" + req.getServerName()
					+ ":" + req.getServerPort() + req.getContextPath() + ""
					+ req.getServletPath();
			try {
				Optional<File> snapshotDirectory;
				if (!Strings.isNullOrEmpty(snapshotDirectoryPath)) {
					snapshotDirectory = Optional
							.of(createDirectory(snapshotDirectoryPath));
				} else {
					snapshotDirectory = Optional.absent();
				}
				this.roadrunner = new Roadrunner(basePath,
						createDirectory(journalDirectoryPath),
						snapshotDirectory);

			} catch (IOException e) {
				logger.error("Error loading Roadrunner", e);
			}
		}

		if (req.getMethod().equals("GET")
				&& resourceForURI(req, "/roadrunner.js")) {
			resp.setContentType("application/javascript");
			resp.setCharacterEncoding("UTF-8");

			ServletOutputStream outputStream = resp.getOutputStream();
			outputStream.write(roadrunner.loadJsFile().getBytes());
			outputStream.close();
		} else if (headerContainsToken(req, "upgrade", "websocket")) {
			super.service(req, resp);
		} else if (resourceForURI(req) != null) {
			ServletOutputStream outputStream = resp.getOutputStream();
			Streams.copy(resourceForURI(req).openStream(), outputStream, true);
		} else {
			handleRestCall(req, resp);
		}
	}

	private URL resourceForURI(HttpServletRequest req)
			throws MalformedURLException {
		String requestURI = req.getRequestURI();
		String contextPath = req.getContextPath();
		String resourcePath = requestURI.replaceFirst(contextPath, "");
		return req.getServletContext().getResource(resourcePath);
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
		Path nodePath = new Path(RoadrunnerEvent.extractPath(
				req.getRequestURI(), null));
		if (req.getMethod().equals("GET")) {
			resp.getWriter().append(
					roadrunner.getPersistence().get(nodePath).toString());
		} else if (req.getMethod().equals("POST")
				|| req.getMethod().equals("PUT")) {
			String msg = new String(CharStreams.toString(new InputStreamReader(
					req.getInputStream(), Charsets.UTF_8)));
			roadrunner.handleEvent(RoadrunnerEventType.SET,
					req.getRequestURI(), Optional.fromNullable(msg));
		} else if (req.getMethod().equals("DELETE")) {
			roadrunner.handleEvent(RoadrunnerEventType.SET,
					req.getRequestURI(), null);
		}
		resp.setContentType("application/json; charset=UTF-8");
	}

	public void invalidate(WsHttpServletRequestWrapper wrapper)
			throws NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {
		Method method = WsHttpServletRequestWrapper.class
				.getDeclaredMethod("invalidate");
		boolean accessible = method.isAccessible();
		method.setAccessible(true);
		method.invoke(wrapper);
		method.setAccessible(accessible);
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

	public File createDirectory(String directoryName) throws IOException {
		if (productiveMode) {
			return new File(directoryName);
		} else {
			final File temp;
			temp = File.createTempFile(directoryName, "");
			if (!(temp.delete())) {
				throw new IOException("Could not delete temp file: "
						+ temp.getAbsolutePath());
			}

			if (!(temp.mkdir())) {
				throw new IOException("Could not create temp directory: "
						+ temp.getAbsolutePath());
			}
			return temp;
		}
	}
}
