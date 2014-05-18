package io.helium.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

import io.helium.Helium;
import io.helium.admin.HeliumAdmin;
import io.helium.common.Path;
import io.helium.event.HeliumEvent;
import io.helium.event.HeliumEventType;
import io.helium.json.Node;

/**
 * Servlet implementation class ChatServlet
 */
public class HeliumWebSocketServlet extends WebSocketServlet {

	private static final long							serialVersionUID	= 1L;
	private static final org.slf4j.Logger	logger						= LoggerFactory
																															.getLogger(HeliumWebSocketServlet.class
																																	.getName());
	protected Helium									helium;
	private String												journalDirectoryPath;
	private String												snapshotDirectoryPath;
	private boolean												productiveMode;
	private Node													rule;
	private HeliumAdmin								heliumAdmin;

	@Override
	protected StreamInbound createWebSocketInbound(String subProtocol, HttpServletRequest request) {
		return createInbound(extractBasePath(request));
	}

	private String extractBasePath(HttpServletRequest request) {
		String servername;
		int serverPort = request.getServerPort();
		if (serverPort != 80) {
			if (request.getScheme().startsWith("ws://")) {
				servername = "http://" + request.getServerName() + ":" + serverPort;
			} else if (request.getScheme().startsWith("wss://")) {
				servername = "https://" + request.getServerName() + ":" + serverPort;
			} else {
				servername = request.getScheme() + "://" + request.getServerName() + ":" + serverPort;
			}
		} else {
			if (request.getScheme().startsWith("ws://")) {
				servername = "http://" + request.getServerName();
			} else if (request.getScheme().startsWith("wss://")) {
				servername = "https://" + request.getServerName();
			} else {
				servername = request.getScheme() + "://" + request.getServerName();
			}
		}
		String contextPath = request.getServletContext().getContextPath();
		if (Strings.isNullOrEmpty(contextPath) && !("/".equals(contextPath))) {

			servername += !(servername.endsWith("/") || contextPath.startsWith("/")) ? contextPath : "/"
					+ contextPath;
		}
		return servername;
	}

	public HeliumMessageInbound createInbound(String servername) {
		return new HeliumMessageInbound(new Node(), servername, helium);
	}

	/*
	 * This only works for tokens. Quoted strings need more sophisticated parsing.
	 */
	private boolean headerContainsToken(HttpServletRequest req, String headerName, String target) {
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
		String ruleFileStr = config.getInitParameter("rule");
		try {
			URL ruleUrl = config.getServletContext().getResource(ruleFileStr);
			this.rule = new Node(Streams.asString(ruleUrl.openStream()));
		} catch (MalformedURLException e1) {
			logger.error(e1.getLocalizedMessage(), e1);
		} catch (IOException e) {
			logger.error(e.getLocalizedMessage(), e);
		}
		String productiveModeString = config.getInitParameter("productiveMode");
		try {
			productiveMode = Boolean.parseBoolean(productiveModeString);
		} catch (Exception e) {
			productiveMode = false;
		}
		if (productiveMode) {
			journalDirectoryPath = Preconditions.checkNotNull(
					config.getInitParameter("journalDirectory"),
					"JournalDirectory must be configured in web.xml");
			snapshotDirectoryPath = config.getInitParameter("snapshotDirectory");
		}
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {
		if (helium == null) {
			this.helium = createHelium(req);

			this.heliumAdmin = new HeliumAdmin(helium);
		}
		if (req.getMethod().equals("GET") && resourceForURI(req, "/helium.js")) {
			resp.setContentType("application/javascript");
			resp.setCharacterEncoding("UTF-8");

			ServletOutputStream outputStream = resp.getOutputStream();
			outputStream.write(helium.loadJsFile().getBytes());
			outputStream.close();
		} else if (headerContainsToken(req, "upgrade", "websocket")) {
			super.service(req, resp);
		} else if (resourceForURI(req) != null) {
			try {
				InputStream openStream = resourceForURI(req).openStream();
				ServletOutputStream outputStream = resp.getOutputStream();
				Streams.copy(openStream, outputStream, true);
			} catch (FileNotFoundException e) {
				logger.error("FileNotFound", e);
			}
		} else if (req.getRequestURI().endsWith(".json")) {
			handleRestCall(req, resp);
		} else {
			handleHttpCall(req, resp);
		}
	}

	private void handleHttpCall(HttpServletRequest req, HttpServletResponse resp) {
		try {
			resp.getWriter().append(
					heliumAdmin.servePath(req.getContextPath(), extractBasePath(req), req.getRequestURL()
							.toString(), new Path(req.getRequestURI()), req.getRequestURI()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected Helium createHelium(HttpServletRequest req) {

		String basePath = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
				+ req.getContextPath() + "" + req.getServletPath();
		try {
			if (productiveMode) {
				Optional<File> snapshotDirectory;
				if (!Strings.isNullOrEmpty(snapshotDirectoryPath)) {
					snapshotDirectory = Optional.of(createDirectory(snapshotDirectoryPath));
				} else {
					snapshotDirectory = Optional.absent();
				}
				return createHeliumInstance(basePath, rule, createDirectory(journalDirectoryPath),
						snapshotDirectory);
			} else {
				Optional<File> snapshotDirectory = Optional.of(createTempDirectory());

				return createHeliumInstance(basePath, rule, createTempDirectory(), snapshotDirectory);
			}

		} catch (IOException e) {
			logger.error("Error loading Helium", e);
		}
		return null;
	}

	protected Helium createHeliumInstance(String basePath, Node rule, File directory,
			Optional<File> snapshotDirectory) throws IOException {
		return new Helium(basePath, rule, directory, snapshotDirectory);
	}

	private URL resourceForURI(HttpServletRequest req) throws MalformedURLException {
		String requestURI = req.getRequestURI();
		String contextPath = req.getContextPath();
		String resourcePath = requestURI.replaceFirst(contextPath, "");
		return req.getServletContext().getResource(resourcePath);
	}

	private boolean resourceForURI(HttpServletRequest req, String uri) throws MalformedURLException {
		String requestURI = req.getRequestURI();
		return requestURI.endsWith(uri);
	}

	private void handleRestCall(HttpServletRequest req, HttpServletResponse resp) throws IOException,
			RuntimeException {
		Path nodePath = new Path(HeliumEvent.extractPath(req.getRequestURI().replaceAll("\\.json",
				"")));
		if (req.getMethod().equals("GET")) {
			resp.getWriter().append(helium.getPersistence().get(nodePath).toString());
		} else if (req.getMethod().equals("POST") || req.getMethod().equals("PUT")) {
			String msg = new String(CharStreams.toString(new InputStreamReader(req.getInputStream(),
					Charsets.UTF_8)));
			helium.handleEvent(HeliumEventType.SET, req.getRequestURI(),
					Optional.fromNullable(msg));
		} else if (req.getMethod().equals("DELETE")) {
			helium.handleEvent(HeliumEventType.SET, req.getRequestURI(), null);
		}
		resp.setContentType("application/json; charset=UTF-8");
	}

	public void invalidate(WsHttpServletRequestWrapper wrapper) throws NoSuchMethodException,
			IllegalAccessException, InvocationTargetException {
		Method method = WsHttpServletRequestWrapper.class.getDeclaredMethod("invalidate");
		boolean accessible = method.isAccessible();
		method.setAccessible(true);
		method.invoke(wrapper);
		method.setAccessible(accessible);
	}

	/**
	 * Intended to be overridden by sub-classes that wish to verify the origin of a WebSocket request
	 * before processing it.
	 * 
	 * @param origin
	 *          The value of the origin header from the request which may be <code>null</code>
	 * 
	 * @return <code>true</code> to accept the request. <code>false</code> to reject it. This default
	 *         implementation always returns <code>true</code>.
	 */
	@Override
	protected boolean verifyOrigin(String origin) {
		return true;
	}

	/**
	 * Intended to be overridden by sub-classes that wish to select a sub-protocol if the client
	 * provides a list of supported protocols.
	 * 
	 * @param subProtocols
	 *          The list of sub-protocols supported by the client in client preference order. The
	 *          server is under no obligation to respect the declared preference
	 * @return <code>null</code> if no sub-protocol is selected or the name of the protocol which
	 *         <b>must</b> be one of the protocols listed by the client. This default implementation
	 *         always returns <code>null</code>.
	 */
	@Override
	protected String selectSubProtocol(List<String> subProtocols) {
		return null;
	}

	public File createDirectory(String directoryName) throws IOException {
		return new File(directoryName);
	}

	public File createTempDirectory() throws IOException {
		final File temp;
		temp = File.createTempFile("Temp" + System.currentTimeMillis(), "");
		if (!(temp.delete())) {
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
		}

		if (!(temp.mkdir())) {
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
		}
		return temp;
	}
}
