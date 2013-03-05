package org.roadrunner.server;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.infinispan.schematic.document.ParsingException;
import org.modeshape.jcr.ConfigurationException;

/**
 * Servlet implementation class ChatServlet
 */
@WebServlet(value = "/rr/*", loadOnStartup = 1)
public class RoadrunnerWebSocketServlet extends WebSocketServlet
{
  private static final long serialVersionUID = 1L;

  @Override
  protected StreamInbound createWebSocketInbound(String subProtocol, HttpServletRequest request)
  {
    String contextPath = request.getContextPath();
    String servletPath = request.getServletPath();
    String requestURI = request.getRequestURI();
    String collectionPath = requestURI.substring((contextPath + servletPath).length() + 1);
    try
    {
      return new RoadrunnerMessageInbound(contextPath + servletPath, collectionPath);
    }
    catch (ParsingException e)
    {
      throw new RuntimeException(e);
    }
    catch (ConfigurationException e)
    {
      throw new RuntimeException(e);
    }
    catch (LoginException e)
    {
      throw new RuntimeException(e);
    }
    catch (RepositoryException e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void init(ServletConfig config) throws ServletException
  {
    super.init(config);
  }

}
