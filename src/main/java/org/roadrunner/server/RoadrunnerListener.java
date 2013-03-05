package org.roadrunner.server;

/**
 * Created with IntelliJ IDEA.
 * User: Balu
 * Date: 23.02.13
 * Time: 21:19
 * To change this template use File | Settings | File Templates.
 */

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

@WebListener()
public class RoadrunnerListener implements ServletContextListener, HttpSessionListener, HttpSessionAttributeListener
{

  // Public constructor is required by servlet spec
  public RoadrunnerListener()
  {
  }

  // -------------------------------------------------------
  // ServletContextListener implementation
  // -------------------------------------------------------
  @Override
  public void contextInitialized(ServletContextEvent sce)
  {
    /*
     * This method is called when the servlet context is initialized(when the
     * Web application is deployed). You can initialize servlet context related
     * data here.
     */
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce)
  {
    /*
     * This method is invoked when the Servlet Context (the Web application) is
     * undeployed or Application Server shuts down.
     */
  }

  // -------------------------------------------------------
  // HttpSessionListener implementation
  // -------------------------------------------------------
  @Override
  public void sessionCreated(HttpSessionEvent se)
  {
    /* Session is created. */
  }

  @Override
  public void sessionDestroyed(HttpSessionEvent se)
  {
    /* Session is destroyed. */
  }

  // -------------------------------------------------------
  // HttpSessionAttributeListener implementation
  // -------------------------------------------------------

  @Override
  public void attributeAdded(HttpSessionBindingEvent sbe)
  {
    /*
     * This method is called when an attribute is added to a session.
     */
  }

  @Override
  public void attributeRemoved(HttpSessionBindingEvent sbe)
  {
    /*
     * This method is called when an attribute is removed from a session.
     */
  }

  @Override
  public void attributeReplaced(HttpSessionBindingEvent sbe)
  {
    /*
     * This method is invoked when an attibute is replaced in a session.
     */
  }
}
