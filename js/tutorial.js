var inited = {};

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

function lockLines(code, start, stop)
{
	for(x=start;x <stop+1;x++)
	{
		code.addLineClass(x, "background", "readonlyBackground");
	}
    code.markText({line: start, ch:0}, {line: stop+1, ch:0}, {readOnly: true});
}

function initStep1()
{
	var code = CodeMirror($('.step1-code').get()[0], {
		value: '<html>\n'+
		'  <head>\n'+
		'    [ADD THE SCRIPT TAG HERE]\n'+
		'  </head>\n'+
		'  <body>\n'+
		'  </body>\n'+
		'</html>\n'+
		'',
		mode: mixedMode,
		viewportMargin: Infinity, lineWrapping: true,
		tabMode: "indent",
		lineNumbers: true,
		styleActiveLine: true,
		matchBrackets: true
	});
	lockLines(code, 0,1);
	lockLines(code, 3,6);
}

function initStep2()
{
	var code = CodeMirror($('.step2-code').get()[0], {
		value: '<html>\n'+
		'  <head>\n'+
		'    <script src="http://localhost:8080/helium.io.js"></script>\n'+
		'  </head>\n'+
		'  <body>\n'+
		'    <script>\n'+
		'      [ADD NEW HELIUM.IO CODE HERE]\n'+
		'    </script>\n'+
		'  </body>\n'+
		'</html>',
		mode: mixedMode,
		viewportMargin: Infinity, lineWrapping: true,
		tabMode: "indent",
		lineNumbers: true,
		styleActiveLine: true,
		matchBrackets: true
	});
	lockLines(code, 0,5);
	lockLines(code, 7,10);
}

function initStep3()
{
	var code = CodeMirror($('.step3-code').get()[0], {
		value: '<html>\n'+
		'  <head>\n'+
		'    <script src="http://localhost:8080/helium.io.js"></script>\n'+
		'    <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.9.0/jquery.min.js"></script>\n'+
		'  </head>\n'+
		'  <body>\n'+
		'    <input type="text" id="nameInput" placeholder="Name">\n'+
		'    <input type="text" id="messageInput" placeholder="Message">\n'+
		'    <script>\n'+
		'      var myDataRef = new Helium.io("http://localhost:8080/");\n'+
		'      $("#messageInput").keypress(function (e) {\n'+
		'        if (e.keyCode == 13) {\n'+
		'          var name = $("#nameInput").val();\n'+
		'          var text = $("#messageInput").val();\n'+
		'          [ADD SET() HERE]\n'+
		'          $("#messageInput").val("");\n'+
		'        }\n'+
		'      });\n'+
		'    </script>\n'+
		'  </body>\n'+
		'</html>',
		mode: mixedMode,
		viewportMargin: Infinity, lineWrapping: true,
		tabMode: "indent",
		lineNumbers: true,
		styleActiveLine: true,
		matchBrackets: true
	});
	lockLines(code, 0,13);
	lockLines(code, 15,21);
}

function initStep4()
{
	var code = CodeMirror($('.step4-code').get()[0], {

		value: '<html>\n'+
		'  <head>\n'+
		'    <script src="http://localhost:8080/helium.io.js"></script>\n'+
		'    <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.9.0/jquery.min.js"></script>\n'+
		'  </head>\n'+
		'  <body>\n'+
		'    <input type="text" id="nameInput" placeholder="Name">\n'+
		'    <input type="text" id="messageInput" placeholder="Message">\n'+
		'    <script>\n'+
		'      var myDataRef = new Helium.io("http://localhost:8080/");\n'+
		'      $("#messageInput").keypress(function (e) {\n'+
		'        if (e.keyCode == 13) {\n'+
		'          var name = $("#nameInput").val();\n'+
		'          var text = $("#messageInput").val();\n'+
		'          myDataRef.set("User " + name + " says  "+ text);\n'+
		'          $("#messageInput").val("");\n'+
		'        }\n'+
		'      });\n'+
		'    </script>\n'+
		'  </body>\n'+
		'</html>',
		mode: mixedMode,
		viewportMargin: Infinity, lineWrapping: true,
		tabMode: "indent",
		lineNumbers: true,
		styleActiveLine: true,
		matchBrackets: true
	});
	lockLines(code, 0,13);
	lockLines(code, 15,21);
}

function initStep5()
{
	var code = CodeMirror($('.step5-code').get()[0], {
		value: '<html>\n'+
		'  <head>\n'+
		'    <script src="http://localhost:8080/helium.io.js"></script>\n'+
		'    <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.9.0/jquery.min.js"></script>\n'+
		'  </head>\n'+
		'  <body>\n'+
		'    <input type="text" id="nameInput" placeholder="Name">\n'+
		'    <input type="text" id="messageInput" placeholder="Message">\n'+
		'    <script>\n'+
		'      var myDataRef = new Helium.io("http://localhost:8080/");\n'+
		'      $("#messageInput").keypress(function (e) {\n'+
		'        if (e.keyCode == 13) {\n'+
		'          var name = $("#nameInput").val();\n'+
		'          var text = $("#messageInput").val();\n'+
		'          myDataRef.set({name: name, text: text});\n'+
		'          $("#messageInput").val("");\n'+
		'        }\n'+
		'      });\n'+
		'    </script>\n'+
		'  </body>\n'+
		'</html>',
		mode: mixedMode,
		tabMode: "indent",
		lineNumbers: true,
		styleActiveLine: true,
		matchBrackets: true
	});
	lockLines(code, 0,13);
	lockLines(code, 15,22);
}

function initStep6()
{
	var code = CodeMirror($('.step6-code').get()[0], {
		value: '<html>\n'+
		'  <head>\n'+
		'    <script src="http://localhost:8080/helium.io.js"></script>\n'+
		'    <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.9.0/jquery.min.js"></script>\n'+
		'  </head>\n'+
		'  <body>\n'+
    	'    <div id="messagesDiv"></div>\n'+
		'    <input type="text" id="nameInput" placeholder="Name">\n'+
		'    <input type="text" id="messageInput" placeholder="Message">\n'+
		'    <script>\n'+
		'      var myDataRef = new Helium.io("http://localhost:8080/");\n'+
		'      $("#messageInput").keypress(function (e) {\n'+
		'        if (e.keyCode == 13) {\n'+
		'          var name = $("#nameInput").val();\n'+
		'          var text = $("#messageInput").val();\n'+
		'          myDataRef.push({name: name, text: text});\n'+
		'          $("#messageInput").val("");\n'+
		'        }\n'+
		'      });\n'+
		'    [ADD YOUR CALLBACK HERE]\n'+
		'  </script>\n'+
		'  </body>\n'+
		'</html>',
		mode: mixedMode,
		viewportMargin: Infinity, lineWrapping: true,
		tabMode: "indent",
		lineNumbers: true,
		styleActiveLine: true,
		matchBrackets: true
	});
	lockLines(code, 0,18);
	lockLines(code, 20,22);
}

function initStep7()
{
	var code = CodeMirror($('.step7-code').get()[0], {
		value: '<html>\n'+
		'  <head>\n'+
		'    <script src="http://localhost:8080/helium.io.js"></script>\n'+
		'    <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.9.0/jquery.min.js"></script>\n'+
		'  </head>\n'+
		'  <body>\n'+
    	'    <div id="messagesDiv"></div>\n'+
		'    <input type="text" id="nameInput" placeholder="Name">\n'+
		'    <input type="text" id="messageInput" placeholder="Message">\n'+
		'    <script>\n'+
		'      var myDataRef = new Helium.io("http://localhost:8080/");\n'+
		'      $("#messageInput").keypress(function (e) {\n'+
		'        if (e.keyCode == 13) {\n'+
		'          var name = $("#nameInput").val();\n'+
		'          var text = $("#messageInput").val();\n'+
		'          myDataRef.push({name: name, text: text});\n'+
		'          $("#messageInput").val("");\n'+
		'        }\n'+
		'      });\n'+
		'        myDataRef.on("child_added", function(snapshot) {\n'+
		'        [MESSAGE CALLBACK CODE GOES HERE]\n'+
		'      });\n'+
		'      function displayChatMessage(name, text) {\n'+
		'        $("<div/>").text(text).prepend($("<em/>").text(name+": ")).appendTo($("#messagesDiv"));\n'+
		'        $("#messagesDiv")[0].scrollTop = $("#messagesDiv")[0].scrollHeight;\n'+
		'      };\n'+
		'    </script>\n'+
		' 	</body>\n'+
		'</html>',
		mode: mixedMode,
		viewportMargin: Infinity, lineWrapping: true,
		tabMode: "indent",
		lineNumbers: true,
		styleActiveLine: true,
		matchBrackets: true
	});
	lockLines(code, 0,18);
	lockLines(code, 20,27);
}
		
function initStep8()
{
	var code = CodeMirror($('.step-edit').get()[0], {
		value: '<html>\n'+
		'  <head>\n'+
		'    <script src="http://localhost:8080/helium.io.js"></script>\n'+
		'    <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.9.0/jquery.min.js"></script>\n'+
		'  </head>\n'+
		'  <body>\n'+
    	'    <div id="messagesDiv"></div>\n'+
		'    <input type="text" id="nameInput" placeholder="Name">\n'+
		'    <input type="text" id="messageInput" placeholder="Message">\n'+
		'    <script>\n'+
		'      var myDataRef = new Helium.io("http://localhost:8080/");\n'+
		'      $("#messageInput").keypress(function (e) {\n'+
		'        if (e.keyCode == 13) {\n'+
		'          var name = $("#nameInput").val();\n'+
		'          var text = $("#messageInput").val();\n'+
		'          myDataRef.push({name: name, text: text});\n'+
		'          $("#messageInput").val("");\n'+
		'        }\n'+
		'      });\n'+
		'        myDataRef.on("child_added", function(snapshot) {\n'+
		'        var message = snapshot.val();\n'+
		'        displayChatMessage(message.name, message.text);\n'+
		'      });\n'+
		'      function displayChatMessage(name, text) {\n'+
		'        $("<div/>").text(text).prepend($("<em/>").text(name+": ")).appendTo($("#messagesDiv"));\n'+
		'        $("#messagesDiv")[0].scrollTop = $("#messagesDiv")[0].scrollHeight;\n'+
		'      };\n'+
		'    </script>\n'+
		' 	</body>\n'+
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
		$('.step-play').append(iframe);
		iframe.contentWindow.contents = content;
		iframe.src = 'javascript:window["contents"]';
		$('.step-edit').hide();
		$('.step-play').show();
	});
	$('#edit').click(function() {
		$('.step-play').hide();
		$('.step-play').empty();
		$('.step-edit').show();
	});
}

$(function() {
	var handleHash = function(hash){
		if(hash != '')
		{
	    	$(".step").hide();
			$(hash).show();
			
			$(".switch").removeClass('active');
			$(hash+"switch").addClass('active');
	    	
			$('#prevStep').removeClass('disabled');
			$('#nextStep').removeClass('disabled');
	    	if(hash == '#step1')
	    	{
	    		if(inited[hash] != true)
					initStep1();
				$('#prevStep').addClass('disabled');
				$('#prevStep a').attr('href','#');
				$('#nextStep a').attr('href','#step2');
	    	}
	    	else if(hash == '#step2')
	    	{
	    		if(inited[hash] != true)
					initStep2();
				$('#prevStep a').attr('href','#step1');
				$('#nextStep a').attr('href','#step3');
	    	}
	    	else if(hash == '#step3')
	    	{
				if(inited[hash] != true)
					initStep3();
				$('#prevStep a').attr('href','#step2');
				$('#nextStep a').attr('href','#step4');
	    	}
	    	else if(hash == '#step4')
	    	{
				if(inited[hash] != true)
					initStep4();
				$('#prevStep a').attr('href','#step3');
				$('#nextStep a').attr('href','#step5');
	    	}
	    	else if(hash == '#step5')
	    	{
				if(inited[hash] != true)
					initStep5();
				$('#prevStep a').attr('href','#step4');
				$('#nextStep a').attr('href','#step6');
	    	}
	    	else if(hash == '#step6')
	    	{
				if(inited[hash] != true)
					initStep6();
				$('#prevStep a').attr('href','#step5');
				$('#nextStep a').attr('href','#step7');
	    	}
	    	else if(hash == '#step7')
	    	{
				if(inited[hash] != true)
					initStep7();
				$('#prevStep a').attr('href','#step6');
				$('#nextStep a').attr('href','#step8');
	    	}
	    	else if(hash == '#step8')
	    	{
				if(inited[hash] != true)
					initStep8();
				$('#nextStep').addClass('disabled');
				$('#prevStep a').attr('href','#step7');
				$('#nextStep a').attr('href','#');
	    	}
	    	inited[hash] = true;
	    }
	};
	$(window).on('hashchange', function() {
		handleHash(location.hash);
	});
	if(location.hash == '')
	{
		location.hash = '#step1';
	}
	else
	{
		handleHash(location.hash);
	}
});