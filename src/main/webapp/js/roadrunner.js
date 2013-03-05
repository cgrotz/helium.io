var endpoint;
function Roadrunner(roadrunner_url)
{
  var events = {};
  var path = roadrunner_url;
  var self = this;
  if (endpoint == null)
  {
    if (!window.WebSocket)
    {
      window.WebSocket = window.MozWebSocket;
    }
    if (window.WebSocket)
    {
      endpoint = new WebSocket(roadrunner_url);
      endpoint.onopen = function(event)
      {
      };
      endpoint.onclose = function(event)
      {
      };
    }
  }

  endpoint.onmessage = function(event)
  {
    var message = JSON.parse(event.data);
    if (Object.prototype.toString.call(message) === '[object Array]')
    {
      for ( var i = 0; i < message.length; i++)
      {
        self.handleMessage(message[i]);
      }
    }
    else
    {
      self.handleMessage(message);
    }
  };

  this.handleMessage = function(message){
    var snapshot =
    {
      val : function()
      {
        return message.payload;
      },
      parent : function()
      {
        return message.parent;
      },
      name : function()
      {
        return message.name;
      }
    };
    var callback = events[message.type];
    if (callback != null)
    {
      callback(snapshot);
    }
  };
  
  this.sendMessage = function(type,path,payload){
    var message = { type : type, path : path, payload : payload };
    endpoint.send(JSON.stringify(message));
  };
  
  this.child = function(childname){
	  return new Roadrunner(path+"/"+childname);
  }
  
  this.on = function(event_type, callback)
  {
    events[event_type] = callback;
  };

  this.push = function(data)
  {
    this.sendMessage('push', path, data);
  };

  this.set = function(data)
  {
    this.sendMessage('set', path, data);
  };
};