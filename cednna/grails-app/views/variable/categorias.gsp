<br>
<div id="div_${con}" class="fieldcontain uk-form-row ${hasErrors(bean: indicadorInstance, field: 'localidad', 'error')} required">

		<div class="uk-grid">

				<div class="uk-width-1-2">
					<label for="localidad" class="uk-form-label">
						<g:message code="indicador.localidad.label" default="Tipo de categoria" />
				
					</label>
					<g:select id="tipo_${con}" name="tipo_${con}" from="${mx.gob.redoaxaca.cednna.domino.Tipo.list()}" optionKey="id" optionValue="descripcion" class="chosen-select sel-var" />
				</div>
				
				<div class="uk-width-1-2">
					<label for="localidad" class="uk-form-label">
						<g:message code="indicador.localidad.label" default="Categoria" />
				
					</label>
					<div id="divTipo_${con}" class="fieldcontain">
							<g:select id="categoria_${con}" name="categoria_${con}" from="${mx.gob.redoaxaca.cednna.domino.Categoria.list()}" optionKey="id" optionValue="descripcion" class="chosen-select "  />
					
					</div>
					
				
			     </div>
		
	     </div>
	    <input type="button" value="-"  id="del_${con}" class="uk-button" />

</div>


<script type="text/javascript">




$(function(){



			$("#del_${con}").click(function(){
				
				$("#div_${con}").remove();
			});	
});
						                		
</script>					                		