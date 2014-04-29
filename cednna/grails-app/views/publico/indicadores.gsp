
<%@ page import="mx.gob.redoaxaca.cednna.domino.Indicador" %>
<!DOCTYPE html>
<html>
	<head>		
		<meta name="layout" content="public">
		<script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.8.2/jquery.min.js"></script>
		<link rel="stylesheet" href="http://code.jquery.com/ui/1.10.3/themes/smoothness/jquery-ui.css" />
		<link rel="stylesheet" href="${resource(dir: 'css', file: 'uikit.min.css')}" type="text/css"/>
		<!-- Almost Flat style -->
		<link rel="stylesheet" href="${resource(dir: 'css', file: 'uikit.almost-flat.min.css')}" type="text/css"/>
	  <script src="http://code.jquery.com/jquery-1.9.1.js"></script>
	  <script src="http://code.jquery.com/ui/1.10.3/jquery-ui.js"></script>
	  <script src="https://code.jquery.com/jquery-1.10.2.min.js"></script>
		<script type="text/javascript" src="http://latex.codecogs.com/latexit.js"></script>

		<script src="https://code.jquery.com/jquery-1.10.2.min.js"></script>

			<g:javascript src="uikit.min.js"/>
	</head>
	<body>
	
		<div class="uk-container">
        	<div class="uk-grid">
				<div class="uk-width-1-2"><div class="uk-panel-space uk-container-center"><img src="${request.getContextPath()}/img/logo_sist.png"  title="CEDNNA" alt="CEDNNA" witdh="283px" height="215px"></div></div>
                <div class="uk-width-1-2"><div class="uk-panel-space uk-container-center"><iframe width="400" height="215" src="http://www.youtube.com/embed/dzlPdjD_3is?rel=0&autoplay=1" frameborder="0" allowfullscreen></iframe></div></div>
            </div>
		</div>

<div class="uk-container uk-container-center">	
	<div class="uk-grid" data-uk-grid-margin="">

		<g:each var="eje" in="${mx.gob.redoaxaca.cednna.domino.Eje.list()}">

			<div id="bot_prog" class="uk-width-medium-1-4">
				<div class="uk-panel">
					<g:link action="detalleIndicador" controller="publico"  value="${eje.descripcion}" id="${eje.id}">
					<div class="bot${eje.id}"></div>
					</g:link>			
				</div>
			</div>
			
		</g:each>
	</div>
</div>
	
		


		
		<script src="${resource(dir: 'js', file: 'highcharts/js/highcharts.js')}"  type="text/javascript" charset="utf-8"></script>
	  	<script src="${resource(dir: 'js', file: 'highcharts/js/modules/exporting.js')}"  type="text/javascript" charset="utf-8"></script>
	  	
	</body>
</html>


			
