var mixedMode = {
  name: "htmlmixed",
  scriptTypes: [{
    matches: /\/x-handlebars-template|\/x-mustache/i,
    mode: null
  }, {
    matches: /(text|application)\/(x-)?vb(a|script)/i,
    mode: "vbscript"
  }]
};
$(function() {
var code = CodeMirror($('.edit').get()[0], {
    value: '<html>\n'+
    '<head>\n'+
    '  <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">\n'+
    '  <meta charset="utf-8">\n'+
    '  <script src="http://localhost:8080/helium.io.js"></script>\n'+
    '  <script src="js/jquery.js"></script>\n'+
    '  <script src="js/idle.js"></script>\n'+
    '</head>\n'+
    '<body>\n'+
    '<div id="presenceDiv"></div>\n'+
    '<script>\n'+
    '  // Prompt the user for a name to use.\n'+
    '  var name = prompt("Your name?", "Guest"),\n'+
    '      currentStatus = "★ online";\n'+
    '\n'+
    '  // Get a reference to the presence data in Helium.io.\n'+
    '  var userListRef = new Helium.io("http://localhost:8080");\n'+
    '\n'+
    '  // Generate a reference to a new location for my user with push.\n'+
    '  var myUserRef = userListRef.push();\n'+
    '\n'+
    '  // Get a reference to my own presence status.\n'+
    '  var connectedRef = new Helium.ioOnlineSwitch("http://localhost:8080", function(isOnline) {\n'+
    '    if (isOnline) {\n'+
    '      // If we lose our internet connection, we want ourselves removed from the list.\n'+
    '      myUserRef.onDisconnect().remove();\n'+
    '\n'+
    '      // Set our initial online status.\n'+
    '      setUserStatus("★ online");\n'+
    '    } else {\n'+
    '\n'+
    '      // We need to catch anytime we are marked as offline and then set the correct status. We\n'+
    '      // could be marked as offline 1) on page load or 2) when we lose our internet connection\n'+
    '      // temporarily.\n'+
    '      setUserStatus(currentStatus);\n'+
    '    }\n'+
    '  });\n'+
    '\n'+
    '  // A helper function to let us set our own state.\n'+
    '  function setUserStatus(status) {\n'+
    '    // Set our status in the list of online users.\n'+
    '    currentStatus = status;\n'+
    '    myUserRef.set({ name: name, status: status });\n'+
    '  }\n'+
    '\n'+
    '  function getMessageId(snapshot) {\n'+
    '    return snapshot.name().replace(/[^a-z0-9\-\_]/gi,"");\n'+
    '  }\n'+
    '\n'+
    '  // Update our GUI to show someone"s online status.\n'+
    '  userListRef.on("child_added", function(snapshot) {\n'+
    '    var user = snapshot.val();\n'+
    '    \n'+
    '    $("<div/>")\n'+
    '      .attr("id", getMessageId(snapshot))\n'+
    '      .text(user.name + " is currently " + user.status)\n'+
    '      .appendTo("#presenceDiv");\n'+
    '  });\n'+
    '\n'+
    '  // Update our GUI to remove the status of a user who has left.\n'+
    '  userListRef.on("child_removed", function(snapshot) {\n'+
    '    $("#presenceDiv").children("#" + getMessageId(snapshot))\n'+
    '      .remove();\n'+
    '  });\n'+
    '\n'+
    '  // Update our GUI to change a user"s status.\n'+
    '  userListRef.on("child_changed", function(snapshot) {\n'+
    '    var user = snapshot.val();\n'+
    '    if( $("#presenceDiv").children("#" + getMessageId(snapshot)).length>0)\n'+
    '    {\n'+
    '      $("#presenceDiv").children("#" + getMessageId(snapshot))\n'+
    '        .text(user.name + " is currently " + user.status);\n'+
    '    }\n'+
    '    else\n'+
    '    {\n'+
    '      $("<div/>")\n'+
    '        .attr("id", getMessageId(snapshot))\n'+
    '        .text(user.name + " is currently " + user.status)\n'+
    '        .appendTo("#presenceDiv");\n'+
    '    }\n'+
    '  });\n'+
    '\n'+
    '  // Use idle/away/back events created by idle.js to update our status information.\n'+
    '  document.onIdle = function () {\n'+
    '    setUserStatus("☆ idle");\n'+
    '  }\n'+
    '  document.onAway = function () {\n'+
    '    setUserStatus("☄ away");\n'+
    '  }\n'+
    '  document.onBack = function (isIdle, isAway) {\n'+
    '    setUserStatus("★ online");\n'+
    '  }\n'+
    '\n'+
    '  setIdleTimeout(5000);\n'+
    '  setAwayTimeout(10000);\n'+
    '</script>\n'+
    '</body>\n'+
    '</html>',
    mode: mixedMode,
    viewportMargin: Infinity, lineWrapping: true,
    tabMode: "indent",
    lineNumbers: true,
    styleActiveLine: true,
    matchBrackets: true
  });

  $('#play').click(function() {
    var content = code.getValue();
    var iframe = document.createElement('iframe');
    iframe.width = "100%";
    iframe.height = "400px";
    $('.play').append(iframe);
    iframe.contentWindow.contents = content;
    iframe.src = 'javascript:window["contents"]';
    $('.edit').hide();
    $('.play').show();
  });
  $('#edit').click(function() {
    $('.play').hide();
    $('.play').empty();
    $('.edit').show();
  });
});

