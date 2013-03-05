package org.roadrunner.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.version.VersionException;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.WsOutbound;
import org.infinispan.schematic.document.ParsingException;
import org.json.JSONException;
import org.json.JSONObject;
import org.modeshape.jcr.ConfigurationException;
import org.roadrunner.server.data.DataService;

import com.google.common.base.Strings;

public class RoadrunnerMessageInbound extends MessageInbound implements EventListener
{
  private static Set<RoadrunnerMessageInbound> connections = new HashSet<RoadrunnerMessageInbound>();

  private Session session;
  private Node rootNode;
  private String repositoryName;

  private String servletPath;

  private String path = null;;

  public RoadrunnerMessageInbound(String servletPath, String path) throws ParsingException, ConfigurationException, LoginException,
      RepositoryException
  {
    this.servletPath = servletPath;
    this.repositoryName = path.indexOf("/") > -1 ? path.substring(0, path.indexOf("/")) : path;
    if (path.length() > repositoryName.length() + 1)
    {
      this.path = path.substring(repositoryName.length() + 1);
    }
    connections.add(this);
    session = DataService.getInstance().getRepository(repositoryName).login();
    int EVENT_MASK = Event.NODE_ADDED | Event.NODE_MOVED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED
        | Event.PROPERTY_CHANGED;
    session.getWorkspace().getObservationManager().addEventListener(this, EVENT_MASK, null, true, null, null, false);
    rootNode = session.getRootNode();
    if (!Strings.isNullOrEmpty(this.path))
    {
      if (rootNode.hasNode(this.path))
      {
        rootNode = rootNode.getNode(this.path);
      }
      else
      {
        rootNode = addNode(rootNode, this.path);
      }
    }
  }

  private Node addNode(Node node2, String path2) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException,
      LockException, RepositoryException
  {
    if (path2.indexOf("/") > -1)
    {
      Node newNode = node2.addNode(path2.substring(0, path2.indexOf("/")));
      session.save();
      return addNode(newNode, path2.substring(path2.indexOf("/") + 1));
    }
    else
    {
      Node childNode = node2.addNode(path2);
      session.save();
      return childNode;
    }
  }

  @Override
  protected void onBinaryMessage(ByteBuffer message) throws IOException
  {
    throw new UnsupportedOperationException("Binary message not supported.");
  }

  @Override()
  protected void onTextMessage(CharBuffer msg) throws IOException
  {
    System.out.println("Received Message: " + msg);
    try
    {
      JSONObject message = new JSONObject(msg.toString());
      String messageType = (String) message.get("type");
      String path = ((String) message.get("path")).substring(servletPath.length() + 1);
      if ("push".equalsIgnoreCase(messageType))
      {
        JSONObject payload = (JSONObject) message.get("payload");
        String nodeName = UUID.randomUUID().toString().replaceAll("-", "");
        Node node;
        if (rootNode.hasNode(nodeName))
        {
          node = rootNode.getNode(nodeName);
        }
        else
        {
          node = rootNode.addNode(nodeName);
        }
        updateNode(payload, node);
        session.save();
      }
      else if ("set".equalsIgnoreCase(messageType))
      {
    	  Object payload = message.get("payload");
          Node node;
          if(session.getRootNode().hasNode(path))
          {
        	  node = session.getRootNode().getNode(path);
          }
          else
          {
        	  node = addNode(session.getRootNode(),path);
          }
          if(payload instanceof JSONObject)
          {
        	  updateNode((JSONObject)payload, node);
          }
          else
          {
        	  node.setProperty("value", ""+payload);
          }
          session.save();
      }
      else if ("on".equalsIgnoreCase(messageType))
      {
        String event_type = (String) message.get("event_type");
      }
    }
    catch (JSONException exp)
    {
      exp.printStackTrace();
    }
    catch (ItemExistsException e)
    {
      e.printStackTrace();
    }
    catch (PathNotFoundException e)
    {
      e.printStackTrace();
    }
    catch (VersionException e)
    {
      e.printStackTrace();
    }
    catch (ConstraintViolationException e)
    {
      e.printStackTrace();
    }
    catch (LockException e)
    {
      e.printStackTrace();
    }
    catch (RepositoryException e)
    {
      e.printStackTrace();
    }
  }

  private void broadcast(String message) throws IOException
  {
    for (RoadrunnerMessageInbound connection : connections)
    {
      CharBuffer buffer = CharBuffer.wrap(message);
      connection.getWsOutbound().writeTextMessage(buffer);
    }
  }

  public void value(JSONObject dataSnapshot) throws JSONException, IOException
  {
    JSONObject broadcast = new JSONObject();
    broadcast.put("type", "value");
    broadcast.put("payload", dataSnapshot);
    getWsOutbound().writeTextMessage(CharBuffer.wrap(broadcast.toString()));
  }

  public void child_added(Node node, String prevChildName) throws JSONException, IOException, RepositoryException
  {
    JSONObject broadcast = new JSONObject();
    broadcast.put("type", "child_added");
    broadcast.put("name", node.getName());
    if (node.getParent() != null)
    {
      broadcast.put("parent", node.getParent().getName());
    }
    broadcast.put("payload", transformToJSON(node));
    broadcast.put("prevChildName", prevChildName);
    getWsOutbound().writeTextMessage(CharBuffer.wrap(broadcast.toString()));
  }

  public void child_removed(JSONObject oldChildSnapshot) throws JSONException, IOException
  {
    JSONObject broadcast = new JSONObject();
    broadcast.put("type", "child_removed");
    broadcast.put("payload", oldChildSnapshot);
    getWsOutbound().writeTextMessage(CharBuffer.wrap(broadcast.toString()));
  }

  public void child_changed(JSONObject childSnapshot, String prevChildName) throws JSONException, IOException
  {
    JSONObject broadcast = new JSONObject();
    broadcast.put("type", "child_changed");
    broadcast.put("payload", childSnapshot);
    broadcast.put("prevChildName", prevChildName);
    getWsOutbound().writeTextMessage(CharBuffer.wrap(broadcast.toString()));
  }

  public void child_moved(JSONObject childSnapshot, String prevChildName) throws JSONException, IOException
  {
    JSONObject broadcast = new JSONObject();
    broadcast.put("type", "child_moved");
    broadcast.put("payload", childSnapshot);
    broadcast.put("prevChildName", prevChildName);
    getWsOutbound().writeTextMessage(CharBuffer.wrap(broadcast.toString()));
  }

  @Override
  protected void onClose(int status)
  {
    super.onClose(status);
    session.logout();
  }

  @Override
  public void onEvent(EventIterator eventIterator)
  {
    try
    {
      while (eventIterator.hasNext())
      {
        Event event = eventIterator.nextEvent();
        if (event.getType() == Event.NODE_ADDED)
        {
          Node node = session.getNodeByIdentifier(event.getIdentifier());
          JSONObject childSnapshot = transformToJSON(node);
          String prevChildName = ""; // TODO
          child_added(node, prevChildName);
        }
        else if (event.getType() == Event.NODE_MOVED)
        {
          Node node = session.getNodeByIdentifier(event.getIdentifier());
          JSONObject childSnapshot = transformToJSON(node);
          String prevChildName = ""; // TODO
          child_moved(childSnapshot, prevChildName);
        }
        else if (event.getType() == Event.NODE_REMOVED)
        {
          Node node = session.getNodeByIdentifier(event.getIdentifier());
          JSONObject childSnapshot = transformToJSON(node);
          child_removed(childSnapshot);
        }
        else if (event.getType() == Event.PROPERTY_ADDED)
        {
          Node node = session.getNodeByIdentifier(event.getIdentifier());
          JSONObject childSnapshot = transformToJSON(node);
          String prevChildName = ""; // TODO
          child_moved(childSnapshot, prevChildName);
        }
        else if (event.getType() == Event.PROPERTY_CHANGED)
        {
          Node node = session.getNodeByIdentifier(event.getIdentifier());
          JSONObject childSnapshot = transformToJSON(node);
          String prevChildName = ""; // TODO
          child_moved(childSnapshot, prevChildName);
        }
        else if (event.getType() == Event.PROPERTY_REMOVED)
        {
          Node node = session.getNodeByIdentifier(event.getIdentifier());
          JSONObject childSnapshot = transformToJSON(node);
          String prevChildName = ""; // TODO
          child_moved(childSnapshot, prevChildName);
        }
      }
    }
    catch (ValueFormatException e)
    {
      throw new RuntimeException(e);
    }
    catch (ItemNotFoundException e)
    {
      throw new RuntimeException(e);
    }
    catch (RepositoryException e)
    {
      throw new RuntimeException(e);
    }
    catch (JSONException e)
    {
      throw new RuntimeException(e);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  private JSONObject transformToJSON(Node node) throws ValueFormatException, RepositoryException, JSONException
  {
    JSONObject object = new JSONObject();
    PropertyIterator itr = node.getProperties();
    while (itr.hasNext())
    {
      Property property = itr.nextProperty();
      if (property.getValue().getType() == PropertyType.BINARY)
      {
        // object.put(property, property.getBinary().);
      }
      else if (property.getValue().getType() == PropertyType.BOOLEAN)
      {
        object.put(property.getName(), property.getBoolean());
      }
      else if (property.getValue().getType() == PropertyType.DATE)
      {
        object.put(property.getName(), new Date(property.getDate().getTimeInMillis()));
      }
      else if (property.getValue().getType() == PropertyType.DECIMAL)
      {
        object.put(property.getName(), property.getDecimal());
      }
      else if (property.getValue().getType() == PropertyType.DOUBLE)
      {
        object.put(property.getName(), property.getDouble());
      }
      else if (property.getValue().getType() == PropertyType.LONG)
      {
        object.put(property.getName(), property.getLong());
      }
      else if (property.getValue().getType() == PropertyType.NAME)
      {
      }
      else if (property.getValue().getType() == PropertyType.PATH)
      {
      }
      else if (property.getValue().getType() == PropertyType.REFERENCE)
      {
      }
      else if (property.getValue().getType() == PropertyType.STRING)
      {
        object.put(property.getName(), property.getString());
      }
      else if (property.getValue().getType() == PropertyType.UNDEFINED)
      {
      }
      else if (property.getValue().getType() == PropertyType.URI)
      {
      }
      else if (property.getValue().getType() == PropertyType.WEAKREFERENCE)
      {
      }
    }
    return object;
  }

  private void updateNode(JSONObject object, Node node) throws ValueFormatException, RepositoryException, JSONException
  {
    Iterator itr = object.keys();
    while (itr.hasNext())
    {
      String key = (String) itr.next();
      Object value = object.get(key);
      node.setProperty(key, "" + value);
    }
  }

  @Override
  protected void onOpen(WsOutbound outbound)
  {
    super.onOpen(outbound);
    {
      try
      {
        sendNode(rootNode);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }

  private void sendNode(Node node) throws RepositoryException, JSONException, IOException
  {
    NodeIterator itr = node.getNodes();
    while (itr.hasNext())
    {
      Node childNode = itr.nextNode();
      if (!childNode.getName().equals("jcr:system"))
      {
        child_added(childNode, null);
      }
    }
  }
}
