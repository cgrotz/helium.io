<%@ page contentType="text/html; charset=utf-8" language="java"%>
<%
	String hostName = request.getServerName() + ":"
			+ request.getServerPort();
%>
<!DOCTYPE html>
<!--[if lt IE 7]>      <html class="no-js lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>         <html class="no-js lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>         <html class="no-js lt-ie9"> <![endif]-->
<!--[if gt IE 8]><!-->
<html class="no-js">
<!--<![endif]-->
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
<title>Roadrunner Admin</title>
<meta name="description" content="">
<meta name="viewport" content="width=device-width">

<link rel="stylesheet" href="css/bootstrap.min.css">
<style>
body {
	padding-top: 60px;
	padding-bottom: 40px;
}
</style>
<link rel="stylesheet" href="css/bootstrap-responsive.min.css">
<link rel="stylesheet" href="css/main.css">
<link rel="stylesheet" href="css/tree.css">

<script src="js/vendor/modernizr-2.6.2-respond-1.1.0.min.js"></script>
<script src="js/vendor/jquery.min.js"></script>

<script src="repo/roadrunner.js"></script>
<script src="js/tree.js"></script>
<script src="js/index.js"></script>
</head>
<body>
	<!--[if lt IE 7]>
            <p class="chromeframe">You are using an <strong>outdated</strong> browser. Please <a href="http://browsehappy.com/">upgrade your browser</a> or <a href="http://www.google.com/chromeframe/?redirect=true">activate Google Chrome Frame</a> to improve your experience.</p>
        <![endif]-->

	<!-- This code is taken from http://twitter.github.com/bootstrap/examples/hero.html -->

	<div class="navbar navbar-inverse navbar-fixed-top">
		<div class="navbar-inner">
			<div class="container">
				<a class="btn btn-navbar" data-toggle="collapse"
					data-target=".nav-collapse"> <span class="icon-bar"></span> <span
					class="icon-bar"></span> <span class="icon-bar"></span>
				</a> <a class="brand" href="#">Roadrunner Admin</a>
				<div class="nav-collapse collapse">
					<ul class="nav">
						<li class="active"><a href="index.html">Home</a></li>
						<li class="dropdown"><a href="#" class="dropdown-toggle"
							data-toggle="dropdown">Account <b class="caret"></b></a>
							<ul class="dropdown-menu">
								<li><a href="j_spring_security_logout">Logout</a></li>
							</ul></li>
					</ul>
				</div>
				<!--/.nav-collapse -->
			</div>
		</div>
	</div>

	<div class="container">
		<div class="row">
			<div class="span12">
				<div class="tree well">
					<ul id="tree">
						
					</ul>
				</div>
			</div>
		</div>

		<hr>

		<footer>
			<p>&copy; Roadrunner 2013</p>
		</footer>

	</div>
	<!-- /container -->

	<script src="js/vendor/bootstrap.min.js"></script>
</body>
</html>
