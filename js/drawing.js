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
'  <script src="http://localhost:8080/roadrunner.js"></script>\n'+
'  <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.9.0/jquery.min.js"></script>\n'+
'  <link rel="stylesheet" type="text/css" href="css/drawing.css">\n'+
'</head>\n'+
'<body>\n'+
'<div>\n'+
'  <canvas id="drawing-canvas" width="480" height="420"></canvas>\n'+
'</div>\n'+
'<div id="colorholder"></div>\n'+
'<script>\n'+
'  $(document).ready(function () {\n'+
'    //Set up some globals\n'+
'    var pixSize = 8, lastPoint = null, currentColor = "000", mouseDown = 0;\n'+
'\n'+
'    //Create a reference to the pixel data for our drawing.\n'+
'    var pixelDataRef = new Roadrunner("http://localhost:8080/drawing/points");\n'+
'\n'+
'    // Set up our canvas\n'+
'    var myCanvas = document.getElementById("drawing-canvas");\n'+
'    var myContext = myCanvas.getContext ? myCanvas.getContext("2d") : null;\n'+
'    if (myContext == null) {\n'+
'      alert("You must use a browser that supports HTML5 Canvas to run this demo.");\n'+
'      return;\n'+
'    }\n'+
'\n'+
'    //Setup each color palette & add it to the screen\n'+
'    var colors = ["fff","000","f00","0f0","00f","88f","f8d","f88","f05","f80","0f8","cf0","08f","408","ff8","8ff"];\n'+
'    for (c in colors) {\n'+
'      var item = $("<div/>").css("background-color", "#" + colors[c]).addClass("colorbox");\n'+
'      item.click((function () {\n'+
'        var col = colors[c];\n'+
'        return function () {\n'+
'          currentColor = col;\n'+
'        };\n'+
'      })());\n'+
'      item.appendTo("#colorholder");\n'+
'    }\n'+
'\n'+
'    //Keep track of if the mouse is up or down\n'+
'    myCanvas.onmousedown = function () {mouseDown = 1;};\n'+
'    myCanvas.onmouseout = myCanvas.onmouseup = function () {\n'+
'      mouseDown = 0; lastPoint = null;\n'+
'    };\n'+
'\n'+
'    //Draw a line from the mouse"s last position to its current position\n'+
'    var drawLineOnMouseMove = function(e) {\n'+
'      if (!mouseDown) return;\n'+
'\n'+
'      e.preventDefault();\n'+
'\n'+
'      // Bresenham"s line algorithm. We use this to ensure smooth lines are drawn\n'+
'      var offset = $("canvas").offset();\n'+
'      var x1 = Math.floor((e.pageX - offset.left) / pixSize - 1),\n'+
'        y1 = Math.floor((e.pageY - offset.top) / pixSize - 1);\n'+
'      var x0 = (lastPoint == null) ? x1 : lastPoint[0];\n'+
'      var y0 = (lastPoint == null) ? y1 : lastPoint[1];\n'+
'      var dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);\n'+
'      var sx = (x0 < x1) ? 1 : -1, sy = (y0 < y1) ? 1 : -1, err = dx - dy;\n'+
'      while (true) {\n'+
'        //write the pixel into Firebase, or if we are drawing white, remove the pixel\n'+
'        pixelDataRef.child(x0 + ":" + y0).set(currentColor === "fff" ? null : currentColor);\n'+
'\n'+
'        if (x0 == x1 && y0 == y1) break;\n'+
'        var e2 = 2 * err;\n'+
'        if (e2 > -dy) {\n'+
'          err = err - dy;\n'+
'          x0 = x0 + sx;\n'+
'        }\n'+
'        if (e2 < dx) {\n'+
'          err = err + dx;\n'+
'          y0 = y0 + sy;\n'+
'        }\n'+
'      }\n'+
'      lastPoint = [x1, y1];\n'+
'    };\n'+
'    $(myCanvas).mousemove(drawLineOnMouseMove);\n'+
'    $(myCanvas).mousedown(drawLineOnMouseMove);\n'+
'\n'+
'    // Add callbacks that are fired any time the pixel data changes and adjusts the canvas appropriately.\n'+
'    // Note that child_added events will be fired for initial pixel data as well.\n'+
'    var drawPixel = function(snapshot) {\n'+
'      var coords = snapshot.name().split(":");\n'+
'      myContext.fillStyle = "#" + snapshot.val();\n'+
'      myContext.fillRect(parseInt(coords[0]) * pixSize, parseInt(coords[1]) * pixSize, pixSize, pixSize);\n'+
'    };\n'+
'    var clearPixel = function(snapshot) {\n'+
'      var coords = snapshot.name().split(":");\n'+
'      myContext.clearRect(parseInt(coords[0]) * pixSize, parseInt(coords[1]) * pixSize, pixSize, pixSize);\n'+
'    };\n'+
'    pixelDataRef.on("child_added", drawPixel);\n'+
'    pixelDataRef.on("child_changed", drawPixel);\n'+
'    pixelDataRef.on("child_removed", clearPixel);\n'+
'  });\n'+
'</script>\n'+
'</body>\n'+
'</html>\n',
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

