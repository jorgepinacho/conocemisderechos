package mx.gob.redoaxaca.cednna.publico

import grails.converters.JSON
import grails.plugins.springsecurity.Secured
import groovy.sql.Sql

import java.text.Normalizer

import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

import mx.gob.redoaxaca.cednna.domino.*

import org.apache.commons.lang.StringUtils
import org.jggug.kobo.commons.lang.CollectionUtils

import com.redoaxaca.java.DetalleIndicador
import com.redoaxaca.java.RVariable
import com.redoaxaca.java.Resultado
import com.redoaxaca.java.ResultadoIndicador
import com.redoaxaca.java.ResultadoTemporal


@Secured( ['IS_AUTHENTICATED_ANONYMOUSLY'])
class PublicoController {
	
	def sessionFactory
	def dataSource
	def dataTablesService
	def publicoService
	def tablasService


    def index() { 
		
	}
	
	def semaforo = {
		
	}
	
	def calendario = {
		
	}
	
	def directorio = {
	}

	def datosdirectorio = {
		def query="  from cat_directorio "
		
		render dataTablesService.datosParaTablaQuery('from cat_directorio',params,
	    [
		'dir_id',
		'dir_nombre',
		'dir_cargo',
		'dir_dependencia',
		'dir_correo',
		'dir_telefono',
		'dir_website',
		],  
		[
		'dir_id',
		'dir_nombre',
		'dir_cargo',
		'dir_dependencia',
		'dir_correo',
		'dir_telefono',
		'dir_website',
		],1,"text") as JSON

	}
	
	def documentoList = {
		def query = "from cat_documento"
		render dataTablesService.datosParaTablaQuery(query,params,
			[
			"doc_id as id",
			"doc_titulo as titulo",
			"doc_nivel as nivel",
			"tipo_documento_id as tipo",
			"doc_url as url"
			],
			[
			"tipo_documento_id",
			"doc_titulo",
			"doc_nivel"
			],
			[
			"id",
			"titulo",
			"nivel",
			"tipo",
			"url"
			],1,"text") as JSON
	}
	
	def indicadores = {
		def urlvideo = Valor.findByKey('urlvideo').valor
		def listaEjes = Eje.findAllByStatus(1, [sort: "orden", order: "asc"])
		[urlvideo: urlvideo, listaEjes: listaEjes]
	}
		
	def contacto = {
		def contactos = Contacto.getAll()
		[contactos: contactos]
	}
	
	def ayuda = {
		
	}	
	
	def actualizarMapa(Long id) {
		int tipo = params.idTipo.toInteger()
		
		DetalleIndicador detalleIndicador = visorIndicador(id,tipo)
		def resultadosIndicador = detalleIndicador.resultados
		def indicador = Indicador.get(id)
		def decimales = indicador?.decimales
		def ubicaciones = []
		def coordenadas = []
		def aux = [:]
		def coordenadasList = []
		def sql = ""
		def sqlNombre=""
		def db = new Sql(dataSource)
		def db2 = new Sql(dataSource)
				
		def sqlNombreId = ""
		def sqlQuery = ""
		
		if(resultadosIndicador.size()!=0){
		
			resultadosIndicador.each { resultado ->
				switch(tipo){
					case 1:
						def idEstado = 20
						sqlNombreId+="ent_id = "+idEstado+" or "
						sqlQuery+="ccoo.estado_coordenadas_id = "+idEstado+" or "
						break
					case 2:
						sqlNombreId+="crg_id = "+resultado.idRegion+" or "
						sqlQuery+="regi.region_coordenadas_id = "+resultado.idRegion+" or "
						break
					case 3:
						sqlNombreId+="mun_id = "+resultado.idMunicipio+" or "
						sqlQuery+="muni.municipio_coordenadas_id = "+resultado.idMunicipio+" or "
						break
				}
			}
			
			switch(tipo){
				case 1:
					sqlNombre = "select ent_descripcion descripcion from cat_entidad where " + sqlNombreId.substring(0,sqlNombreId.length()-3) + " ORDER BY ent_id;"
					sql = "select ccoo.estado_coordenadas_id as id, coor.latitud, coor.longitud from coordenada coor join cat_entidad_coordenada ccoo on (coor.id = ccoo.coordenada_id) where "+sqlQuery.substring(0,sqlQuery.length()-3) + " ORDER BY ccoo.estado_coordenadas_id,coordenadas_idx;"
					break
				case 2:
					sqlNombre = "select crg_descripcion descripcion from cat_region where " + sqlNombreId.substring(0,sqlNombreId.length()-3) + " ORDER BY crg_id;"
					sql = "select regi.region_coordenadas_id as id, coor.latitud, coor.longitud from coordenada coor join cat_region_coordenada regi on (coor.id = regi.coordenada_id) where "+sqlQuery.substring(0,sqlQuery.length()-3) + " ORDER BY regi.region_coordenadas_id,coordenadas_idx;"
					break
				case 3:
					sqlNombre = "select mun_descripcion descripcion from cat_municipio where " + sqlNombreId.substring(0,sqlNombreId.length()-3) + " ORDER BY mun_id;"
					sql = "select muni.municipio_coordenadas_id as id, coor.latitud, coor.longitud from coordenada coor join cat_municipio_coordenada muni on (coor.id = muni.coordenada_id) where "+sqlQuery.substring(0,sqlQuery.length()-3) + " ORDER BY muni.municipio_coordenadas_id,coordenadas_idx;"
					break
			}
			
			def nombre  = db2.rows(sqlNombre)
			def result  = db.rows(sql)
			
			System.out.println(sqlNombre)
			System.out.println(sql)
			
			def index = 0
			resultadosIndicador.each { resultado ->
				def ubicacionNombre = nombre.get(index)
				if(tipo==1)
					ubicacionNombre = [descripcion:'Oaxaca']
				
				def datos = []
				def anios = []
				resultado.resultados.each { r ->
					anios.add(r?.anio)
					datos.add(r?.indicador.round(decimales))
				}
				
				ubicaciones.add(["descripcion": ubicacionNombre.get("descripcion"), "anios":anios, "datos": datos])
				index++
			}
			
			def idAux
			if(result.size()>0){
				idAux = result?.id?.get(0)
			}
			
			def cooorAux = []
			
			result.each {
				if(it?.id != idAux){
					coordenadasList.add(cooorAux)
					cooorAux = new ArrayList()
					idAux = it?.id
				}
				cooorAux.add("new google.maps.LatLng(" + it?.latitud + ","+it?.longitud+")")
			}
			if(result.size()>0){
				coordenadasList.add(cooorAux)
			}
							
			aux.put("lugar",["ubicaciones":ubicaciones])
			def jsodata = aux as JSON
			render(template:"mapa", model:[coordenadasList:coordenadasList, aux:jsodata, resultadosIndicador:resultadosIndicador])
		}else{
			render(text:"No existen valores para el indicador a este nivel", encoding: "UTF-8")
		}
	}
	
	def String remplazarAcentos(String s){
		String texto = Normalizer.normalize(s, Normalizer.Form.NFD)
		texto = texto.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
		return texto;
	}
	
	
	def actualizarSemaforo(){
		def dependencia = Dependencia.get(params.id)
		def indicadores = Indicador.findAllByDependencia(dependencia)
		render(template:"indicadorSemaforo", model:[indicadores:indicadores])
	}
	
	def infoIndicador (Long id) {
		def eje = Eje.get(id)
		if(eje){
			def divisiones=Division.findAllByEje(eje)
			render(template:"division", model: [divisiones: divisiones])
		}else{
		redirect(action:"indicadores")
		}		
	}
	
	def detalleIndicador(Long id){		
		//Mostrar vista de indicadores		
		def eje = Eje.get(id)
		if(eje){								
			def divisiones=Division.findAllByEje(eje)	
			def niveles = []
			println 'eje?.tipoEje?.id:'+eje?.tipoEje?.id
			if( eje?.tipoEje?.id!=null && eje?.tipoEje?.id!=1){
				niveles = Nivel.findAllByTipoNivel(eje?.tipoEje).sort{it.id}
			}
			[divisiones: divisiones, ejeInstance: eje, tipo:eje?.tipoEje?.id, niveles:niveles]
		}else{
			redirect(action:"indicadores")
		}
	}
	
	def mostrarIndicador(Long id){
		def indicador = Indicador.get(id)
		
		if(indicador){
			params.paginado = false
			DetalleIndicador detalleIndicador = visorIndicadorPaginado(id,1,params)
			
			//Cambiar f�rmula
			String formula = crearFormula(indicador)
						
			def tamVariables = indicador.variables.size()
			
			def eje = Eje.get(params.ejeInstance.toLong())
			int maximaLongitud = 55
			
			def nombreIndicador = StringUtils.abbreviate(indicador?.nombre,maximaLongitud)
			
			[resultados:detalleIndicador?.resultados?.resultados[0],nombreIndicador: nombreIndicador, ejeInstance:eje, indicadorInstance: indicador, tipo:'1', tamVariables:tamVariables, formula:formula]
		}
		else{
			redirect(action:"indicadores")
		}
	}
	
	String crearFormula(Indicador indicador){
		//Cambiar valores de la formula por la descripci�n
		def formula = ""
		def formulaOriginal = indicador?.formula?.sentencia
		def var = [:]
		def fin = false
		indicador?.variables.each {
			var.put(it.clave , CatOrigenDatos.findByClave(it.claveVar)?.descripcion)
		}
		
		for (int i=0; i<formulaOriginal.length(); i++) {
			for(int j=0; j<var.size(); j++){
				if(!fin && var.get(formulaOriginal.charAt(i).toString())!=null){
					formula += var.get(formulaOriginal.charAt(i).toString())
					fin = true
				}
			}
			if(!fin){
				formula += formulaOriginal.charAt(i).toString()
			}
			fin = false
		}
		return formula
	}
	
	def getTitulosDatosCalculo(Long id){
		def indicadorInstance = Indicador.get(id)
		int tipo = params.idTipo.toInteger()
		def titulos = []
		def anios = []
		def titulosAnios = []
		
		titulos.add( "Variable")
		if(tipo==1){
			titulos.add("Estado")
		}else if(tipo==2){
			titulos.add("Regiones")
		}else{
			titulos.add( "Municipios")
		}
		
		getAnosPorIndicador(id,tipo).each{
			if(indicadorInstance?.etiquetaPeriodo){
				def periodo = Periodo.get(it.periodo.toLong())
				anios.add( 'Total')
				anios.add( 'Mujeres')
				anios.add( 'Hombres')
				titulosAnios.add( periodo.descripcion)
			}else{
				anios.add('Total')
				anios.add( 'Mujeres')
				anios.add( 'Hombres')
				titulosAnios.add(it.anio.toString())
			}
		}
		render template:'tablaDatosCalculo', model:[titulos:titulos, titulosAnios:titulosAnios, anios:anios]
	}
	
	def getTablaDatosCalculo(Long id){
		int tipo = params.idTipo.toInteger()
		def indicadorInstance = Indicador.get(id)
		params.paginado = true
		DetalleIndicador detalleIndicador = visorIndicadorPaginado(id,tipo,params)
		
		def tamVariables = indicadorInstance.variables.size()
		def datosCalculo = detalleIndicador.rVariables
		if(tipo==2){
			datosCalculo.each {
				it.valores = it.valores.sort{it.region}
			}
		}else if(tipo==3){
			datosCalculo.each {
				it.valores = it.valores.sort{it.municipio}
			}
		}
		
		def datos = publicoService.getTablaDatosCalculo(datosCalculo, tamVariables, tipo)
		def totalRecords = datos.size()
		
		def result = ["bDestroy": true, "bRetrieve": true,'sEcho':1, 'iTotalRecords':totalRecords, 'iTotalDisplayRecords':totalRecords, 'aaData':datos, 'aaSorting': [],   'oLanguage':["sUrl": "../../datatables/language/spanish.txt"]]
		render result as JSON
	}
	
	def getTablaIndicador(Long id){
		int sEcho = 0
		def indicadorInstance = Indicador.get(id)
		if(params.sEcho){
			sEcho = params.sEcho.toInteger()
			sEcho++
		}
		
		int tipo = params.idTipo.toInteger()
		def titulos = []
		def columnasSinOrdenar = []
		def indexColumnas = 0
		def indices = []
		
		switch (tipo){
			case 1:
				titulos.add([sTitle : "Estado"])
				indexColumnas++
				break
			case 2:
				titulos.add([sTitle : "Regi&oacute;n"])
				indexColumnas++
				break
			case 3:
				titulos.add([sTitle : "Regi&oacute;n"])
				titulos.add([sTitle : "Municipio"])
				indexColumnas+=2
				break
			case 4:
				titulos.add([sTitle : "Regi&oacute;n"])
				titulos.add([sTitle : "Municipio"])
				titulos.add([sTitle : "Localidad"])
				indexColumnas+=3
				break
		}
		
		getAnosPorIndicador(id,tipo).each{
			if(indicadorInstance?.etiquetaPeriodo){
				def periodo = Periodo.get(it.periodo.toLong())
				titulos.add([sTitle : periodo.descripcion])
			}else{
				titulos.add([sTitle : it.anio.toString()])
			}
			indices.add(indexColumnas)
			indexColumnas++
		}
		
		columnasSinOrdenar.add(bSortable: false, aTargets: indices)
		
		def metodo = createLink(controller:'publico', action:'getTablaIndicadorJson', id:id, params:[idTipo:tipo])
		
		def result = ["bServerSide": true,"bProcessing": true, "sAjaxSource":metodo,"bDestroy": true, "bRetrieve": true, 'aoColumns':titulos, 'aoColumnDefs': columnasSinOrdenar,'oLanguage':["sUrl": "../../datatables/language/spanish.txt"]]
		render result as JSON
	}
	
	def getTablaIndicadorJson(Long id){
		int sEcho = 0
		if(params.sEcho){
			sEcho = params.sEcho.toInteger()
			sEcho++
		}
		
		int tipo = params.idTipo.toInteger()
		def indicador = Indicador.get(id)
		
		DetalleIndicador detalleIndicador = visorIndicadorPaginado(id,tipo,params)
		def resultadosIndicador = detalleIndicador.resultados
		
		def datos = publicoService.getTablaIndicador(resultadosIndicador, tipo)
		def totalRecords = datos.size()
		if(tipo==3){
			totalRecords = 570
		}
		
		def result = ['sEcho':sEcho, 'iTotalRecords':totalRecords, 'iTotalDisplayRecords':totalRecords, 'aaData':datos]
		
		render result as JSON
	}
	
	def actualizarAreaGrafica(Long id){
		def tipo = params.idTipo.toInteger()
		def indicadorInstance = Indicador.get(id)
		def listaArea = []
		if(tipo==1){
			listaArea = Estado.getAll()
		}else if(tipo==2){
			listaArea = Region.getAll()
		}else if(tipo==3){
			listaArea = Municipio.getAll()
		}
		
		render(template:'selectGrafica', model:[tipo: tipo, opcionesAreaGrafica:listaArea, indicadorInstance:indicadorInstance])
	}
	
	def crearGrafica(Long id, int tipo){
		
		def jsondata = null
		
		def indicador = Indicador.get(id)
		def decimales= indicador?.decimales
		params.paginado = false
		DetalleIndicador detalleIndicador = visorIndicadorPaginado(id,tipo,params)
	
		if(detalleIndicador.resultados.size()>0){
			if(tipo==2){
				detalleIndicador.resultados.sort{it.region}
			}else if(tipo==3){
				CollectionUtils.extendMetaClass()
				detalleIndicador.resultados.sort{remplazarAcentos(it.region)}{remplazarAcentos(it.municipio)}
			}
			
			def resultadosIndicador = detalleIndicador.resultados.get(0)
			def resultados = []
			def titulo = "Oaxaca"
			
			for(int i=0;i<detalleIndicador.resultados.size();i++){
				def resultado = detalleIndicador.resultados.get(i)
				if(tipo==2){
					if(resultado.idRegion.toString().equals(params.idArea)){
						resultadosIndicador = resultado
					}
				}else if(tipo==3){
					if(resultado.idMunicipio.toString().equals(params.idArea)){
						resultadosIndicador = resultado
					}
				}
			}
	
			resultados = resultadosIndicador?.resultados
			switch(tipo){
				case 2:
					titulo = resultadosIndicador?.region
					break
				case 3:
					titulo = resultadosIndicador?.municipio
					break
			}
			
			//Creaci�n de arreglo para Highcharts
			def series = []
			def categorias = []
			def datos = []
						
			def a = [:]
			a.put("yAxis", [title: [text: '%']])
			a.put("tooltip", [valueSuffix: '%'])
			a.put("legend", [layout: "vertical", align: "right", verticalAlign: "middle", borderWidth: 0])
			resultados.each { result ->
				if(result?.indicador!=null){
					categorias.add(result?.anio)
					datos.add(result?.indicador.round(decimales))
				}
			}
			
			a.put("xAxis", [categories: categorias] )
			def serie = [name: "Indicador", data: datos]
			series << serie
			a.put("series", series)
			
			//Convertir el arreglo a JSON
			jsondata = a as JSON
		}
		return jsondata
	}
	
	def actualizarGrafica(Long id){
		def tipo = params.idTipo.toInteger()
		def jsondata = crearGrafica(id, tipo)
		String nivel = "Oaxaca"
		switch(tipo){
			case 2:
				nivel = Region.get(params.idArea.toLong())
				break
			case 3:
				nivel = Municipio.get(params.idArea.toLong())
				break
		}
		render(template:"graficaIndicador", model:[tablaJSON:jsondata, nivel: nivel])
	}
	
	def getTablaBuscador(){
		int sEcho = 0
		if(params.sEcho){
			sEcho = params.sEcho.toInteger()
			sEcho++
		}
		
		def datos = tablasService.getTablaBuscador(params, false)
		def totalRecords = tablasService.getTablaBuscador(params, true)
		
		def result = ['sEcho':sEcho, 'iTotalRecords':totalRecords, 'iTotalDisplayRecords':totalRecords, 'aaData':datos]
		render result as JSON
	}
	
	def getAnosPorIndicador(Long id, int idTipo){
		def indicadorInstance = Indicador.get(id)
		String claves = ""
		String periodo = ""
		String tipo = ""
		int tamVariables = indicadorInstance.variables.size()
		int cont = 1
		for(v in indicadorInstance.variables){
			claves += " cvv_clave = '" + v.claveVar + "'"
			if(cont<tamVariables){
				claves += " or "
			}
			cont++
		}
		
		switch(idTipo){
			case 2:
				tipo = "and cvv_region is not null"
				break
			case 3:
				tipo = "and cvv_municipio is not null"
				break
		}
		
		if(indicadorInstance?.etiquetaPeriodo){
			periodo = " and cvv_ped_id is not null "
		}else{
			periodo = " and cvv_ped_id is null "
		}
		
		String sqlAnios = """
			select count(anio) v, anio, cvv_ped_id periodo from (
				select DISTINCT (cvv_anio) as anio, cvv_clave, cvv_ped_id   
				from cat_variable 
				where (${claves}) ${periodo} ${tipo}
			) as p 
			group by anio, cvv_ped_id
			having count(anio) = (select count(*) from cat_dvariable where cdv_ind_id = ${indicadorInstance?.id})
			order by 2
		"""
		
		def sqlAnio = new Sql(sessionFactory.currentSession.connection())
		def titulos = sqlAnio.rows(sqlAnios)
		return titulos
	}
	
	DetalleIndicador visorIndicadorPaginado(Long id,int idTipo,params){
		def indicadorInstance = Indicador.get(id);
		def opcion= idTipo;

		def formula =  indicadorInstance?.formula?.sentencia
		def sentencia= indicadorInstance?.formula?.variables
		def variables= sentencia.split("\\|")
		def List<ResultadoIndicador> resultados= new ArrayList<ResultadoIndicador>()

		def List<RVariable> resutaldoVariables = new ArrayList<RVariable>()
		def List<RVariable> rVariables = new ArrayList<RVariable>()
		def List<ResultadoTemporal> listTemp = new ArrayList<ResultadoTemporal>()
		RVariable temVar

		def num=0
		def letra
		def valorBase
		
		def aniosPorBuscar = getAnosPorIndicador(id,idTipo)
		
		aniosPorBuscar.each{
			def anio = it.anio
			boolean  b = true
			//println 'opcion:'+opcion + ' a�o:'+anio
			
			switch (opcion) {

				case 1:
				/***
				 * PROCESO DE SALIDA POR ESTADO
				 *
				 * */

				/***
				 * Comienza la busqueda en el origen de datos en base a las variable
				 * */

					for(vari in indicadorInstance.variables.sort{it.clave}){
						def sql = new Sql(sessionFactory.currentSession.connection())
						
						String intervaloSql = "select cdv_intervalo intervalo from cat_dvariable where cdv_clavevar= '${vari?.claveVar}' and cdv_ind_id = ${indicadorInstance?.id}"
						def intervalos = sql.rows(intervaloSql)
						int intervalo
						
						intervalos.each{
							intervalo = it.intervalo.toInteger()
						}
						
						
						println 'intervalo:'+intervalo

						def query = "SELECT "+
								"clave, "+
								"descripcion,"+
								"sum(o.mujeres) as mujeres, "+
								"sum(o.hombres) as hombres , "+
								"sum(o.total) as total "+
								"FROM (SELECT cat_variable.cvv_clave AS clave, "+
								"	cat_variable.cvv_descripcion AS descripcion, "+
								"	cat_variable.cvv_region AS region_id, "+
								"	cat_variable.cvv_municipio AS municipio_id, "+
								"	cat_variable.cvv_localidad AS localidad_id, "+
								"    cat_variable.cvv_mujeres AS mujeres, "+
								"	cat_variable.cvv_hombres AS hombres, "+
								"	cat_variable.cvv_poblacion_total AS total "+
								" FROM cat_variable "+
								"where "+
								" cvv_clave='"+vari.claveVar+"'    and   cvv_anio="+(anio.toInteger()-intervalo).toString()+" "

						if(vari.categorias){
							query=query+" and "+
									"("

						}
						def queryTipo="select ctt_id from cat_categoria ca ,cat_tipo ct where ca.cct_ctt_id=ct.ctt_id "+
								" and ca.cct_id in ( select cdc_cct_id from cat_dvariable_categoria where cdc_cdv_id = "+ vari.id+") group by ctt_id"

						def resultTipo
						//println 'Query1:'+queryTipo
						def result = sql.rows(queryTipo.toString())
						
						def tamTipo =result.size()
						def cc=1
						//println 'result:'+result
//						println 'result.size():'+result.size()
						result?.each{
							def queryCat="select cct_id from cat_categoria ca ,cat_tipo ct where ca.cct_ctt_id=ct.ctt_id "+
									" and ca.cct_id in ( select cdc_cct_id from cat_dvariable_categoria where cdc_cdv_id = "+  vari.id+" ) and ctt_id ="+ it.ctt_id
//							println 'Query2:'+queryCat
							resultTipo= sql.rows(queryCat.toString())
							def tam =resultTipo.size()
							def c=1
							resultTipo?.each
							{
								query=query+" cvv_id in (select  cvc_cvv_id from cat_variable_categoria where cvc_cct_id = "+it.cct_id+")  "
								if(c!=tam)
									query=query+" or "
								c++
							}

							if(cc!=tam)
								query=query+" and "

							cc++
						}

						if(vari.categorias){
							query=query+"  ) "
						}

						query=query+") o LEFT JOIN cat_region cr ON cr.crg_id = o.region_id LEFT JOIN cat_municipio cm ON cm.mun_id = o.municipio_id LEFT JOIN cat_localidad cl ON cl.ctl_id = o.localidad_id  group by clave,descripcion"
						println 'QueryTotal:'+query
						def resultTotal = sql.rows(query.toString())
						println 'query:'+query

						def queryVariable = "select cod_descripcion descripcion from cat_origen_datos where  cod_clave='${vari.claveVar}'"
						def descripcionVariable = sql.rows(queryVariable.toString())

						if(resultTotal.size()>0){
//							System.out.println("*LA CONSULTA ES : "+query);
//							println 'resultTotal.size():'+resultTotal.size()
							temVar= new RVariable()
							temVar.letra=vari.clave

							resultTotal?.each
							{
								//System.out.println("LA CONSULTA ES : "+query);
//								System.out.println("Variable "+vari.clave+" Mujeres : "+it.mujeres+" Hombres : "+it.hombres +" -- "+anio)
								ResultadoTemporal valorTem = new ResultadoTemporal()
								switch (vari.poblacion.clave) {
									case "H":
										valorTem.indicador=it.hombres
										valorTem.hombres = it.hombres
										valorTem.mujeres = 0
										break;
									case "M":
										valorTem.indicador=it.mujeres
										valorTem.mujeres = it.mujeres
										valorTem.hombres = 0
										break;
									case "T":
										valorTem.indicador=it.total
										valorTem.mujeres = it.mujeres
										valorTem.hombres = it.hombres
										break;
									default:
										break;
								}
								
								valorTem.anio=anio
								temVar.valores.add(valorTem)
								temVar.descripcion=descripcionVariable[0]?.descripcion
							}
							rVariables.add(temVar)
							resutaldoVariables.add(temVar)
						}


						sql.close()
					}

				/***
				 * Comienza el calculo del indicador en base a las variables
				 * */
				//											System.out.println("ANIO : "+anio+ "  -   variables "+rVariables.size());
					
					if(indicadorInstance.variables.size()!=rVariables.size()){
						rVariables.each{
							resutaldoVariables.remove(it)
						}
						rVariables = []
					}
					
					if(rVariables.size()>0){

						rVariables.each { var->
							if(var?.valores?.get(0)?.indicador!=null){
								formula=formula.replaceAll(var.letra, String.valueOf(var?.valores?.get(0)?.indicador))
							}
						}
//						System.out.println(formula);

						ResultadoTemporal rTemp = new ResultadoTemporal()


						ScriptEngineManager script = new ScriptEngineManager();
						ScriptEngine js = script.getEngineByName("JavaScript");
						
						try {
							rTemp.resultadoIndicador =js.eval("eval('"+formula+"')")
							if(rTemp.resultadoIndicador.toString().equals('Infinity')){
								rTemp.resultadoIndicador = null
							}
							rTemp.anio=anio
							listTemp.add(rTemp)
						} catch (Exception e) {
							rTemp.resultadoIndicador = null
							rTemp.anio=anio
							listTemp.add(rTemp)
						}

						formula= indicadorInstance?.formula?.sentencia
					}

				/***
				 * Comienza el proceso de ordenamiento para salida
				 * */
				//												System.out.println("Valor final : "+listTemp.size()+ "anio"+anio );
//				println 'listTemp:'+listTemp
					listTemp.each { actual->
						def ban=0
						if(resultados.size()>0){
							Resultado res= new Resultado()
							res.anio=actual.anio
							
							if(actual.resultadoIndicador!=null && !Double.isNaN(actual.resultadoIndicador)){
								res.indicador=actual.resultadoIndicador
							}else{
								res.indicador=null;
							}
							resultados.get(0).resultados.add(res)
							//																System.out.println("Veces que entro al sistema 1 ");
						}else{
							Resultado res= new Resultado()
							res.anio=actual.anio
							if(actual.resultadoIndicador!=null && !Double.isNaN(actual.resultadoIndicador)){
								res.indicador=actual.resultadoIndicador
							}else{
								res.indicador=null;
							}
							ResultadoIndicador ri =  new  ResultadoIndicador()

							ri.resultados.add(res)
							resultados.add(ri)
							//																System.out.println("Veces que entro al sistema 2 ");
						}
					}

					num=0
					rVariables= new ArrayList<RVariable>()
					listTemp = new ArrayList<ResultadoTemporal>()
					break;
				case 2:

				/***
				 * PROCESO DE SALIDA POR REGIONES
				 *
				 * */


				/***
				 * Comienza la busqueda en el origen de datos en base a las variable
				 * */

					for(vari in indicadorInstance.variables.sort{it.clave}){
						def sql = new Sql(sessionFactory.currentSession.connection())
						
						String intervaloSql = "select cdv_intervalo intervalo from cat_dvariable where cdv_clavevar= '${vari?.claveVar}' and cdv_ind_id = ${indicadorInstance?.id}"
						def intervalos = sql.rows(intervaloSql)
						int intervalo
						intervalos.each{
							intervalo = it.intervalo.toInteger()
						}

						def query = "SELECT o.region_id,"+
								"descripcion,"+
								"COALESCE(cr.crg_descripcion, ''::character varying) AS region,"+
								"sum(o.mujeres) as mujeres, "+
								"sum(o.hombres) as hombres , "+
								"sum(o.total)  as total "+
								"FROM (SELECT cat_variable.cvv_clave AS clave, "+
								"	cat_variable.cvv_descripcion AS descripcion, "+
								"	cat_variable.cvv_region AS region_id, "+
								"	cat_variable.cvv_municipio AS municipio_id, "+
								"	cat_variable.cvv_localidad AS localidad_id, "+
								"    cat_variable.cvv_mujeres AS mujeres, "+
								"	cat_variable.cvv_hombres AS hombres, "+
								"	cat_variable.cvv_poblacion_total AS total "+
								" FROM cat_variable "+
								"where "+
								" cvv_clave='"+vari.claveVar+"' and   cvv_region is not null     and   cvv_anio="+(anio.toInteger()-intervalo).toString()+"  "

						if(vari.categorias){
							query=query+" and "+"("
						}
						def queryTipo="select ctt_id from cat_categoria ca ,cat_tipo ct where ca.cct_ctt_id=ct.ctt_id "+
								" and ca.cct_id in ( select cdc_cct_id from cat_dvariable_categoria where cdc_cdv_id = "+ vari.id+") group by ctt_id"

//						println 'queryTipo1:'+queryTipo
						def resultTipo
						def result = sql.rows(queryTipo.toString())

						def tamTipo =result.size()
						def cc=1
						result?.each{

							def queryCat="select cct_id from cat_categoria ca ,cat_tipo ct where ca.cct_ctt_id=ct.ctt_id "+
									" and ca.cct_id in ( select cdc_cct_id from cat_dvariable_categoria where cdc_cdv_id = "+  vari.id+" ) and ctt_id ="+ it.ctt_id
//							println 'queryCat2:'+queryCat
							resultTipo= sql.rows(queryCat.toString())
							def tam =resultTipo.size()
							def c=1
							resultTipo?.each{
								query=query+" cvv_id in (select  cvc_cvv_id from cat_variable_categoria where cvc_cct_id = "+it.cct_id+")  "
								if(c!=tam)
									query=query+" or "
								c++
							}

							if(cc!=tam)
								query=query+" and "

							cc++

						}

						if(vari.categorias){
							query=query+"  ) "
						}

						query=query+") o LEFT JOIN cat_region cr ON cr.crg_id = o.region_id LEFT JOIN cat_municipio cm ON cm.mun_id = o.municipio_id LEFT JOIN cat_localidad cl ON cl.ctl_id = o.localidad_id "
								
							if(params?.sSearch!=null && params?.sSearch!=''){
									query +=
											""" WHERE
					            UPPER(cr.crg_descripcion) LIKE UPPER ('%${params?.sSearch}%')   					                                    
					        """
							}
						query +="GROUP BY "+
								"o.region_id,  "+
								"region,descripcion"
								
						query += " order by region "  + (params.sSortDir_0 != null ? params.sSortDir_0 : '' )
							
						if(!params.paginado){
							query += " LIMIT "+ (params.iDisplayLength != null ?params.iDisplayLength:'10') +" OFFSET " + (params.iDisplayStart!=null?params.iDisplayStart:'0')
						}

//						println 'query3:'+query
						//System.out.println("LA CONSULTA ES : "+query);
						def resultTotal = sql.rows(query.toString())
						
						def queryVariable = "select cod_descripcion descripcion from cat_origen_datos where  cod_clave='${vari.claveVar}'"
						def descripcionVariable = sql.rows(queryVariable.toString())
						
						if(resultTotal.size()>0){
							//System.out.println("LA CONSULTA ES : "+query);
							temVar= new RVariable()
							temVar.letra=vari.clave

							resultTotal?.each{
								//	System.out.println("Variable "+vari.clave+" Region-ID : "+it.region_id + " Region : "+it.region + " Mujeres : "+it.mujeres+" Hombres : "+it.hombres +" -- "+anio)
								ResultadoTemporal valorTem = new ResultadoTemporal()
								
								switch (vari.poblacion.clave) {
									case "H":
										valorTem.indicador=it.hombres
										valorTem.hombres = it.hombres
										valorTem.mujeres = 0
										break;
									case "M":
										valorTem.indicador=it.mujeres
										valorTem.mujeres = it.mujeres
										valorTem.hombres = 0
										break;
									case "T":
										valorTem.indicador=it.total
										valorTem.mujeres = it.mujeres
										valorTem.hombres = it.hombres
										break;
									default:
										break;
								}
								
								valorTem.region=it.region
								valorTem.idRegion =it.region_id
								valorTem.anio=anio
								temVar.descripcion=descripcionVariable[0]?.descripcion
								temVar.valores.add(valorTem)
							}
							rVariables.add(temVar)
							resutaldoVariables.add(temVar)
							
						}
						
						sql.close()
					}	

				/***
				 * Comienza el calculo del indicador en base a las variables
				 * */
					
					if(indicadorInstance.variables.size()!=rVariables.size()){
						rVariables.each{
							resutaldoVariables.remove(it)
						}
						rVariables = []
					}
					
					if(rVariables.size()>0){
						num=rVariables.get(0).letra
						letra=rVariables.get(0).valores.size()

						rVariables.each {
							if( it.valores.size()<num){
								num=it.valores.size()
								letra=it.letra
								valorBase=it.valores
							}
						}
						
						valorBase.each {base->
							formula=formula.replaceAll(letra, String.valueOf(base.indicador))
							rVariables.each {
								var->
								if(var.letra!=letra){
									var.valores.each {
										if(base.idRegion==it.idRegion){
											formula=formula.replaceAll(var.letra, String.valueOf(it.indicador))
										}
									}
								}
							}
//							System.out.println(formula);

							ResultadoTemporal rTemp = new ResultadoTemporal()

							ScriptEngineManager script = new ScriptEngineManager();
							ScriptEngine js = script.getEngineByName("JavaScript");
							try {
								rTemp.resultadoIndicador =js.eval("eval('"+formula+"')")
								if(rTemp.resultadoIndicador.toString().equals('Infinity')){
									rTemp.resultadoIndicador = null
								}
								rTemp.region= base.region
								rTemp.idRegion= base.idRegion
								rTemp.anio=base.anio
								listTemp.add(rTemp)

							} catch (Exception e) {
								rTemp.resultadoIndicador = null
								rTemp.region= base.region
								rTemp.idRegion= base.idRegion
								rTemp.anio=base.anio
								listTemp.add(rTemp)
							}

							formula= indicadorInstance?.formula?.sentencia
						}
						/***
					 * Comienza el proceso de ordenamiento para salida
					 * */

						listTemp.each {
							actual->
							def ban=0
							if(resultados.size()>0){

								resultados.each {

									if(it.idRegion==actual.idRegion){
										Resultado res= new Resultado()
										res.anio=actual.anio
										if(actual.resultadoIndicador!=null && !Double.isNaN(actual.resultadoIndicador)){
											res.indicador=actual.resultadoIndicador
										}else{
											res.indicador=null;
										}
										it.resultados.add(res)
										ban=1
									}
								}

								if(ban!=1){
									Resultado res= new Resultado()
									res.anio=actual.anio
									if(actual.resultadoIndicador!=null && !Double.isNaN(actual.resultadoIndicador)){
										res.indicador=actual.resultadoIndicador
									}else{
										res.indicador=null;
									}
									ResultadoIndicador ri =  new  ResultadoIndicador()
									ri.region=actual.region
									ri.idRegion=actual.idRegion
									ri.resultados.add(res)
									resultados.add(ri)

								}
							}else{
								Resultado res= new Resultado()
								res.anio=actual.anio
								if(actual.resultadoIndicador!=null && !Double.isNaN(actual.resultadoIndicador)){
									res.indicador=actual.resultadoIndicador
								}else{
									res.indicador=null;
								}
								ResultadoIndicador ri =  new  ResultadoIndicador()
								ri.region=actual.region
								ri.idRegion=actual.idRegion
								ri.resultados.add(res)
								resultados.add(ri)
							}
						}
					}

					num=0
					rVariables= new ArrayList<RVariable>()
					listTemp = new ArrayList<ResultadoTemporal>()

					break;

				case 3:




				/***
				 * PROCESO DE SALIDA POR MUNICIPIOS
				 *
				 * */


				/***
				 * Comienza la busqueda en el origen de datos en base a las variable
				 * */
					String orden='region '
					if(params?.iSortCol_0){
						switch (params?.iSortCol_0) {
							case '0':
								orden = 'region '
								break
							case '1':
								orden = 'municipio '
								break
						}
					}

					for(vari in indicadorInstance.variables.sort{it.clave}){
						def sql = new Sql(sessionFactory.currentSession.connection())
						String intervaloSql = "select cdv_intervalo intervalo from cat_dvariable where cdv_clavevar= '${vari?.claveVar}' and cdv_ind_id = ${indicadorInstance?.id}"
						def intervalos = sql.rows(intervaloSql)
						int intervalo
						intervalos.each{
							intervalo = it.intervalo.toInteger()
						}
						def query = "SELECT o.region_id,"+
								"descripcion,"+
								"COALESCE(cr.crg_descripcion, ''::character varying) AS region,"+
								"o.municipio_id,"+
								"COALESCE(cm.mun_descripcion, ''::character varying) AS municipio,"+
								"sum(o.mujeres) as mujeres, "+
								"sum(o.hombres) as hombres , "+
								"sum(o.total)  as total "+
								"FROM (SELECT cat_variable.cvv_clave AS clave, "+
								"	cat_variable.cvv_descripcion AS descripcion, "+
								"	cat_variable.cvv_region AS region_id, "+
								"	cat_variable.cvv_municipio AS municipio_id, "+
								"	cat_variable.cvv_localidad AS localidad_id, "+
								"    cat_variable.cvv_mujeres AS mujeres, "+
								"	cat_variable.cvv_hombres AS hombres, "+
								"	cat_variable.cvv_poblacion_total AS total "+
								" FROM cat_variable "+
								"where "+
								" cvv_clave='"+vari.claveVar+"' and   cvv_region is not null     and   cvv_anio="+(anio.toInteger()-intervalo).toString()+" and   cvv_municipio is not null  "

						if(vari.categorias){
							query=query+" and "+"("
						}
						
						def queryTipo="select ctt_id from cat_categoria ca ,cat_tipo ct where ca.cct_ctt_id=ct.ctt_id "+
								" and ca.cct_id in ( select cdc_cct_id from cat_dvariable_categoria where cdc_cdv_id = "+ vari.id+") group by ctt_id"

//						println 'queryTipo1:'+queryTipo
						def resultTipo
						def result = sql.rows(queryTipo.toString())

						def tamTipo =result.size()
						def cc=1
						result?.each{
							def queryCat="select cct_id from cat_categoria ca ,cat_tipo ct where ca.cct_ctt_id=ct.ctt_id "+
									" and ca.cct_id in ( select cdc_cct_id from cat_dvariable_categoria where cdc_cdv_id = "+  vari.id+" ) and ctt_id ="+ it.ctt_id
//							println 'queryCat2:'+queryCat
							resultTipo= sql.rows(queryCat.toString())
							def tam =resultTipo.size()
							def c=1
							resultTipo?.each{
								query=query+" cvv_id in (select  cvc_cvv_id from cat_variable_categoria where cvc_cct_id = "+it.cct_id+")  "
								if(c!=tam)
									query=query+" or "
								c++
							}

							if(cc!=tam)
								query=query+" and "
							cc++
						}

						if(vari.categorias){
							query=query+"  ) "
						}

						query=query+") o LEFT JOIN cat_region cr ON cr.crg_id = o.region_id LEFT JOIN cat_municipio cm ON cm.mun_id = o.municipio_id LEFT JOIN cat_localidad cl ON cl.ctl_id = o.localidad_id "
						if(params?.sSearch!=null && params?.sSearch!=''){
							query +=
									""" WHERE
					            UPPER(cr.crg_descripcion) LIKE UPPER ('%${params?.sSearch}%') 
					            OR UPPER(cm.mun_descripcion) LIKE UPPER ('%${params?.sSearch}%')  					                                    
					        """
						}
						
						query +="GROUP BY "+
								"o.region_id,  "+
								"region,"+
								"o.municipio_id, " +
								"municipio,descripcion"
								
						
								
						query += " order by " + orden + (params.sSortDir_0 != null ? params.sSortDir_0 : '' )
						if(!params.paginado){
							query += " LIMIT "+ (params.iDisplayLength != null ?params.iDisplayLength:'10') +" OFFSET " + (params.iDisplayStart!=null?params.iDisplayStart:'0')
						}
						
						

//						println 'query3:'+query
						//System.out.println("LA CONSULTA ES : "+query);
						def resultTotal = sql.rows(query.toString())
//						println 'resultTotal:'+resultTotal
						
						def queryVariable = "select cod_descripcion descripcion from cat_origen_datos where  cod_clave='${vari.claveVar}'"
						def descripcionVariable = sql.rows(queryVariable.toString())
						
						if(resultTotal.size()>0){
//							System.out.println("LA CONSULTA ES : "+query);
							temVar= new RVariable()
							temVar.letra=vari.clave

							resultTotal?.each{
								//	System.out.println("Variable "+vari.clave+" Region-ID : "+it.region_id + " Region : "+it.region + " Mujeres : "+it.mujeres+" Hombres : "+it.hombres +" -- "+anio)
								ResultadoTemporal valorTem = new ResultadoTemporal()
								switch (vari.poblacion.clave) {
									case "H":
										valorTem.indicador=it.hombres
										valorTem.hombres = it.hombres
										valorTem.mujeres = 0
										break;
									case "M":
										valorTem.indicador=it.mujeres
										valorTem.mujeres = it.mujeres
										valorTem.hombres = 0
										break;
									case "T":
										valorTem.indicador=it.total
										valorTem.mujeres = it.mujeres
										valorTem.hombres = it.hombres
										break;
									default:
										break;
								}
								
								valorTem.region=it.region
								valorTem.idRegion =it.region_id
								valorTem.municipio=it.municipio
								valorTem.idMunicipio = it.municipio_id
								valorTem.anio=anio
								temVar.valores.add(valorTem)
								temVar.descripcion=descripcionVariable[0]?.descripcion
							}

							rVariables.add(temVar)
							resutaldoVariables.add(temVar)
						}
						sql.close()
					}

				/***
				 * Comienza el calculo del indicador en base a las variables
				 * */
					if(indicadorInstance.variables.size()!=rVariables.size()){
						rVariables.each{
							resutaldoVariables.remove(it)
						}
						rVariables = []
					}

					if(rVariables.size()>0){

						letra=rVariables.get(0).letra
						num=rVariables.get(0).valores.size()



						rVariables.each {
							if( it.valores.size()!=0){
								num=it.valores.size()
								letra=it.letra
								valorBase=it.valores


							}
						}
						
						valorBase.each {base->
							formula=formula.replaceAll(letra, String.valueOf(base.indicador))
							rVariables.each {
								var->
								if(var.letra!=letra){
									var.valores.each {
										if(base.idMunicipio==it.idMunicipio){
											formula=formula.replaceAll(var.letra, String.valueOf(it.indicador))
										}
									}
								}
							}
//							System.out.println(formula);

							ResultadoTemporal rTemp = new ResultadoTemporal()

							ScriptEngineManager script = new ScriptEngineManager();
							ScriptEngine js = script.getEngineByName("JavaScript");
							try {

								rTemp.resultadoIndicador =js.eval("eval('"+formula+"')")
								if(rTemp.resultadoIndicador.toString().equals('Infinity')){
									rTemp.resultadoIndicador = null
								}
								rTemp.region= base.region
								rTemp.idRegion= base.idRegion
								rTemp.municipio= base.municipio
								rTemp.idMunicipio= base.idMunicipio
								rTemp.anio=base.anio
								listTemp.add(rTemp)

							} catch (Exception e) {
								rTemp.resultadoIndicador = null
								rTemp.region= base.region
								rTemp.idRegion= base.idRegion
								rTemp.municipio= base.municipio
								rTemp.idMunicipio= base.idMunicipio
								rTemp.anio=base.anio
								listTemp.add(rTemp)
							}
							formula= indicadorInstance?.formula?.sentencia
						}

						/***
					 * Comienza el proceso de ordenamiento para salida
					 * */
						listTemp.each {
							actual->
							def ban=0
							if(resultados.size()>0){

								resultados.each {

									if(it.idMunicipio==actual.idMunicipio){
										Resultado res= new Resultado()
										res.anio=actual.anio
										if(actual.resultadoIndicador!=null && !Double.isNaN(actual.resultadoIndicador)){
											res.indicador=actual.resultadoIndicador
										}else{
											res.indicador=null;
										}
										it.resultados.add(res)
										ban=1
									}
								}


								if(ban!=1){
									Resultado res= new Resultado()
									res.anio=actual.anio
									if(actual.resultadoIndicador!=null && !Double.isNaN(actual.resultadoIndicador)){
										res.indicador=actual.resultadoIndicador
									}else{
										res.indicador=null;
									}
									ResultadoIndicador ri =  new  ResultadoIndicador()
									ri.region=actual.region
									ri.idRegion=actual.idRegion
									ri.municipio= actual.municipio
									ri.idMunicipio= actual.idMunicipio
									ri.resultados.add(res)
									resultados.add(ri)

								}
							}else{
								Resultado res= new Resultado()
								res.anio=actual.anio
								if(actual.resultadoIndicador!=null && !Double.isNaN(actual.resultadoIndicador)){
									res.indicador=actual.resultadoIndicador
								}else{
									res.indicador=null;
								}
								ResultadoIndicador ri =  new  ResultadoIndicador()
								ri.region=actual.region
								ri.idRegion=actual.idRegion
								ri.municipio= actual.municipio
								ri.idMunicipio= actual.idMunicipio
								ri.resultados.add(res)
								resultados.add(ri)

							}
						}






					}

					num=0
					rVariables= new ArrayList<RVariable>()

					listTemp = new ArrayList<ResultadoTemporal>()






					break;

			

			}



		}
		
		DetalleIndicador detalleIndicador = new DetalleIndicador()
		detalleIndicador.resultados = resultados
		detalleIndicador.rVariables = resutaldoVariables

		return detalleIndicador
	}
	
	DetalleIndicador visorIndicador(Long id, int idTipo){
		
				def indicadorInstance = Indicador.get(id);
				def opcion= idTipo;
		
				def formula =  indicadorInstance?.formula?.sentencia
				def sentencia= indicadorInstance?.formula?.variables
				def variables= sentencia.split("\\|")
				def List<ResultadoIndicador> resultados= new ArrayList<ResultadoIndicador>()
		
				def List<RVariable> resutaldoVariables = new ArrayList<RVariable>()
				def List<RVariable> rVariables = new ArrayList<RVariable>()
				def List<ResultadoTemporal> listTemp = new ArrayList<ResultadoTemporal>()
				RVariable temVar
		
				def num=0
				def letra
				def valorBase
		
				for(anio in 2005..2020){
					boolean  b = true
		
					switch (opcion) {
		
						case 1:
						/***
						 * PROCESO DE SALIDA POR ESTADO
						 *
						 * */
		
						/***
						 * Comienza la busqueda en el origen de datos en base a las variable
						 * */
		
							for(vari in indicadorInstance.variables){
								def sql = new Sql(sessionFactory.currentSession.connection())
		
								def query = "SELECT "+
										"clave, "+
										"descripcion,"+
										"sum(o.mujeres) as mujeres, "+
										"sum(o.hombres) as hombres , "+
										"sum(o.total) as total "+
										"FROM (SELECT cat_variable.cvv_clave AS clave, "+
										"	cat_variable.cvv_descripcion AS descripcion, "+
										"	cat_variable.cvv_region AS region_id, "+
										"	cat_variable.cvv_municipio AS municipio_id, "+
										"	cat_variable.cvv_localidad AS localidad_id, "+
										"    cat_variable.cvv_mujeres AS mujeres, "+
										"	cat_variable.cvv_hombres AS hombres, "+
										"	cat_variable.cvv_poblacion_total AS total "+
										" FROM cat_variable "+
										"where "+
										" cvv_clave='"+vari.claveVar+"'    and   cvv_anio="+anio+" "
		
								if(vari.categorias){
									query=query+" and "+
											"("
		
								}
								def queryTipo="select ctt_id from cat_categoria ca ,cat_tipo ct where ca.cct_ctt_id=ct.ctt_id "+
										" and ca.cct_id in ( select cdc_cct_id from cat_dvariable_categoria where cdc_cdv_id = "+ vari.id+") group by ctt_id"
		
								def resultTipo
								def result = sql.rows(queryTipo.toString())
		
								def tamTipo =result.size()
								def cc=1
								result?.each
								{
									def queryCat="select cct_id from cat_categoria ca ,cat_tipo ct where ca.cct_ctt_id=ct.ctt_id "+
											" and ca.cct_id in ( select cdc_cct_id from cat_dvariable_categoria where cdc_cdv_id = "+  vari.id+" ) and ctt_id ="+ it.ctt_id
									resultTipo= sql.rows(queryCat.toString())
									def tam =resultTipo.size()
									def c=1
									resultTipo?.each
									{
										query=query+" cvv_id in (select  cvc_cvv_id from cat_variable_categoria where cvc_cct_id = "+it.cct_id+")  "
										if(c!=tam)
											query=query+" or "
										c++
									}
		
									if(cc!=tam)
										query=query+" and "
		
									cc++
								}
		
								if(vari.categorias){
									query=query+"  ) "
								}
		
								query=query+") o LEFT JOIN cat_region cr ON cr.crg_id = o.region_id LEFT JOIN cat_municipio cm ON cm.mun_id = o.municipio_id LEFT JOIN cat_localidad cl ON cl.ctl_id = o.localidad_id  group by clave,descripcion"
		
		//						System.out.println("LA CONSULTA ES : "+query);
								def resultTotal = sql.rows(query.toString())
		
								if(resultTotal.size()>0){
//																										System.out.println("LA CONSULTA ES : "+query);
									temVar= new RVariable()
									temVar.letra=vari.clave
		
									resultTotal?.each
									{
										//System.out.println("LA CONSULTA ES : "+query);
//										System.out.println("Variable "+vari.clave+" Mujeres : "+it.mujeres+" Hombres : "+it.hombres +" -- "+anio)
										ResultadoTemporal valorTem = new ResultadoTemporal()
										switch (vari.poblacion.clave) {
											case "H":
		
												valorTem.indicador=it.hombres
												valorTem.anio=anio
												temVar.valores.add(valorTem)
												temVar.descripcion=it.descripcion
												break;
		
											case "M":
		
												valorTem.indicador=it.mujeres
												valorTem.anio=anio
												temVar.valores.add(valorTem)
												temVar.descripcion=it.descripcion
												break;
		
											case "T":
		
												valorTem.indicador=it.total
												valorTem.anio=anio
												temVar.valores.add(valorTem)
												temVar.descripcion=it.descripcion
												break;
											default:
												break;
										}
									}
		
									rVariables.add(temVar)
									resutaldoVariables.add(temVar)
								}
								sql.close()
							}
		
						/***
						 * Comienza el calculo del indicador en base a las variables
						 * */
						//											System.out.println("ANIO : "+anio+ "  -   variables "+rVariables.size());
							if(rVariables.size()>0){
		
		
								rVariables.each {
									var->
		
									formula=formula.replaceAll(var.letra, String.valueOf(var.valores.get(0).indicador))
		
								}
//								System.out.println(formula);
		
								ResultadoTemporal rTemp = new ResultadoTemporal()
		
		
								ScriptEngineManager script = new ScriptEngineManager();
								ScriptEngine js = script.getEngineByName("JavaScript");
								try {
		
									rTemp.resultadoIndicador =js.eval("eval('"+formula+"')")
									rTemp.anio=anio
									listTemp.add(rTemp)
		
								} catch (Exception e) {
									// TODO Auto-generated catch block
									//e.printStackTrace();
								}
		
		
								formula= indicadorInstance?.formula?.sentencia
		
		
		
							}
		
						/***
						 * Comienza el proceso de ordenamiento para salida
						 * */
						//												System.out.println("Valor final : "+listTemp.size()+ "anio"+anio );
							listTemp.each {
								actual->
								def ban=0
								if(resultados.size()>0){
		
									Resultado res= new Resultado()
									res.anio=actual.anio
									
									if(!Double.isNaN(actual.resultadoIndicador)){
										res.indicador=actual.resultadoIndicador
									}else{
										res.indicador=null;
									}
									resultados.get(0).resultados.add(res)
									//																System.out.println("Veces que entro al sistema 1 ");
		
								}else{
									Resultado res= new Resultado()
									res.anio=actual.anio
									if(!Double.isNaN(actual.resultadoIndicador)){
										res.indicador=actual.resultadoIndicador
									}else{
										res.indicador=null;
									}
									ResultadoIndicador ri =  new  ResultadoIndicador()
		
									ri.resultados.add(res)
									resultados.add(ri)
									//																System.out.println("Veces que entro al sistema 2 ");
								}
							}
		
		
		
		
		
		
		
							num=0
							rVariables= new ArrayList<RVariable>()
							listTemp = new ArrayList<ResultadoTemporal>()
		
		
		
		
							break;
		
						case 2:
		
						/***
						 * PROCESO DE SALIDA POR REGIONES
						 *
						 * */
		
		
						/***
						 * Comienza la busqueda en el origen de datos en base a las variable
						 * */
		
							for(vari in indicadorInstance.variables){
		
								def sql = new Sql(sessionFactory.currentSession.connection())
		
								def query = "SELECT o.region_id,"+
										"descripcion,"+
										"COALESCE(cr.crg_descripcion, ''::character varying) AS region,"+
										"sum(o.mujeres) as mujeres, "+
										"sum(o.hombres) as hombres , "+
										"sum(o.total)  as total "+
										"FROM (SELECT cat_variable.cvv_clave AS clave, "+
										"	cat_variable.cvv_descripcion AS descripcion, "+
										"	cat_variable.cvv_region AS region_id, "+
										"	cat_variable.cvv_municipio AS municipio_id, "+
										"	cat_variable.cvv_localidad AS localidad_id, "+
										"    cat_variable.cvv_mujeres AS mujeres, "+
										"	cat_variable.cvv_hombres AS hombres, "+
										"	cat_variable.cvv_poblacion_total AS total "+
										" FROM cat_variable "+
										"where "+
										" cvv_clave='"+vari.claveVar+"' and   cvv_region is not null     and   cvv_anio="+anio+"  "
		
								if(vari.categorias){
		
									query=query+" and "+
											"("
		
								}
								def queryTipo="select ctt_id from cat_categoria ca ,cat_tipo ct where ca.cct_ctt_id=ct.ctt_id "+
										" and ca.cct_id in ( select cdc_cct_id from cat_dvariable_categoria where cdc_cdv_id = "+ vari.id+") group by ctt_id"
		
								def resultTipo
								def result = sql.rows(queryTipo.toString())
		
		
								def tamTipo =result.size()
								def cc=1
								result?.each
								{
		
		
									def queryCat="select cct_id from cat_categoria ca ,cat_tipo ct where ca.cct_ctt_id=ct.ctt_id "+
											" and ca.cct_id in ( select cdc_cct_id from cat_dvariable_categoria where cdc_cdv_id = "+  vari.id+" ) and ctt_id ="+ it.ctt_id
									resultTipo= sql.rows(queryCat.toString())
									def tam =resultTipo.size()
									def c=1
									resultTipo?.each
									{
										query=query+" cvv_id in (select  cvc_cvv_id from cat_variable_categoria where cvc_cct_id = "+it.cct_id+")  "
										if(c!=tam)
											query=query+" or "
										c++
									}
		
									if(cc!=tam)
										query=query+" and "
		
									cc++
		
								}
		
								if(vari.categorias){
		
									query=query+"  ) "
		
								}
		
								query=query+") o LEFT JOIN cat_region cr ON cr.crg_id = o.region_id LEFT JOIN cat_municipio cm ON cm.mun_id = o.municipio_id LEFT JOIN cat_localidad cl ON cl.ctl_id = o.localidad_id "+
										"GROUP BY "+
										"o.region_id,  "+
										"region,descripcion"
		
		
								//System.out.println("LA CONSULTA ES : "+query);
								def resultTotal = sql.rows(query.toString())
		
								if(resultTotal.size()>0){
									//System.out.println("LA CONSULTA ES : "+query);
									temVar= new RVariable()
									temVar.letra=vari.clave
		
									resultTotal?.each
									{
		
										//	System.out.println("Variable "+vari.clave+" Region-ID : "+it.region_id + " Region : "+it.region + " Mujeres : "+it.mujeres+" Hombres : "+it.hombres +" -- "+anio)
										ResultadoTemporal valorTem = new ResultadoTemporal()
										switch (vari.poblacion.clave) {
											case "H":
												valorTem.region=it.region
												valorTem.idRegion = it.region_id
												valorTem.indicador=it.hombres
												valorTem.anio=anio
												temVar.valores.add(valorTem)
												temVar.descripcion=it.descripcion
												break;
		
											case "M":
												valorTem.region=it.region
												valorTem.idRegion =it.region_id
												valorTem.indicador=it.mujeres
												valorTem.anio=anio
												temVar.valores.add(valorTem)
												temVar.descripcion=it.descripcion
												break;
		
											case "T":
												valorTem.region=it.region
												valorTem.idRegion =it.region_id
												valorTem.indicador=it.total
												valorTem.anio=anio
												temVar.valores.add(valorTem)
												temVar.descripcion=it.descripcion
												break;
											default:
												break;
										}
									}
		
									rVariables.add(temVar)
									resutaldoVariables.add(temVar)
								}
								sql.close()
							}
		
						/***
						 * Comienza el calculo del indicador en base a las variables
						 * */
		
							if(rVariables.size()>0){
		
								num=rVariables.get(0).letra
								letra=rVariables.get(0).valores.size()
		
		
		
								rVariables.each {
									if( it.valores.size()<num){
										num=it.valores.size()
										letra=it.letra
										valorBase=it.valores
									}
								}
		
								valorBase.each {
									base->
		
		
									formula=formula.replaceAll(letra, String.valueOf(base.indicador))
									rVariables.each {
										var->
										if(var.letra!=letra){
											var.valores.each {
		
												if(base.idRegion==it.idRegion){
		
													formula=formula.replaceAll(var.letra, String.valueOf(it.indicador))
												}
											}
										}
		
									}
//									System.out.println(formula);
		
									ResultadoTemporal rTemp = new ResultadoTemporal()
		
		
									ScriptEngineManager script = new ScriptEngineManager();
									ScriptEngine js = script.getEngineByName("JavaScript");
									try {
		
										rTemp.resultadoIndicador =js.eval("eval('"+formula+"')")
										rTemp.region= base.region
										rTemp.idRegion= base.idRegion
										rTemp.anio=base.anio
										listTemp.add(rTemp)
		
									} catch (Exception e) {
										// TODO Auto-generated catch block
										//e.printStackTrace();
									}
		
		
									formula= indicadorInstance?.formula?.sentencia
		
		
		
								}
		
								/***
							 * Comienza el proceso de ordenamiento para salida
							 * */
		
								listTemp.each {
									actual->
									def ban=0
									if(resultados.size()>0){
		
										resultados.each {
		
											if(it.idRegion==actual.idRegion){
												Resultado res= new Resultado()
												res.anio=actual.anio
												if(!Double.isNaN(actual.resultadoIndicador)){
													res.indicador=actual.resultadoIndicador
												}else{
													res.indicador=null;
												}
												it.resultados.add(res)
												ban=1
											}
										}
		
		
										if(ban!=1){
											Resultado res= new Resultado()
											res.anio=actual.anio
											if(!Double.isNaN(actual.resultadoIndicador)){
												res.indicador=actual.resultadoIndicador
											}else{
												res.indicador=null;
											}
											ResultadoIndicador ri =  new  ResultadoIndicador()
											ri.region=actual.region
											ri.idRegion=actual.idRegion
											ri.resultados.add(res)
											resultados.add(ri)
		
										}
									}else{
										Resultado res= new Resultado()
										res.anio=actual.anio
										if(!Double.isNaN(actual.resultadoIndicador)){
											res.indicador=actual.resultadoIndicador
										}else{
											res.indicador=null;
										}
										ResultadoIndicador ri =  new  ResultadoIndicador()
										ri.region=actual.region
										ri.idRegion=actual.idRegion
										ri.resultados.add(res)
										resultados.add(ri)
		
									}
								}
		
		
		
		
		
		
							}
		
							num=0
							rVariables= new ArrayList<RVariable>()
							listTemp = new ArrayList<ResultadoTemporal>()
		
		
		
							break;
		
						case 3:
		
		
		
		
						/***
						 * PROCESO DE SALIDA POR MUNICIPIOS
						 *
						 * */
		
		
						/***
						 * Comienza la busqueda en el origen de datos en base a las variable
						 * */
		
							for(vari in indicadorInstance.variables){
		
								def sql = new Sql(sessionFactory.currentSession.connection())
		
								def query = "SELECT o.region_id,"+
										"descripcion,"+
										"COALESCE(cr.crg_descripcion, ''::character varying) AS region,"+
										"o.municipio_id,"+
										"COALESCE(cm.mun_descripcion, ''::character varying) AS municipio,"+
										"sum(o.mujeres) as mujeres, "+
										"sum(o.hombres) as hombres , "+
										"sum(o.total)  as total "+
										"FROM (SELECT cat_variable.cvv_clave AS clave, "+
										"	cat_variable.cvv_descripcion AS descripcion, "+
										"	cat_variable.cvv_region AS region_id, "+
										"	cat_variable.cvv_municipio AS municipio_id, "+
										"	cat_variable.cvv_localidad AS localidad_id, "+
										"    cat_variable.cvv_mujeres AS mujeres, "+
										"	cat_variable.cvv_hombres AS hombres, "+
										"	cat_variable.cvv_poblacion_total AS total "+
										" FROM cat_variable "+
										"where "+
										" cvv_clave='"+vari.claveVar+"' and   cvv_region is not null     and   cvv_anio="+anio+" and   cvv_municipio is not null  "
		
								if(vari.categorias){
		
									query=query+" and "+
											"("
		
								}
								def queryTipo="select ctt_id from cat_categoria ca ,cat_tipo ct where ca.cct_ctt_id=ct.ctt_id "+
										" and ca.cct_id in ( select cdc_cct_id from cat_dvariable_categoria where cdc_cdv_id = "+ vari.id+") group by ctt_id"
		
								def resultTipo
								def result = sql.rows(queryTipo.toString())
		
		
								def tamTipo =result.size()
								def cc=1
								result?.each
								{
		
		
									def queryCat="select cct_id from cat_categoria ca ,cat_tipo ct where ca.cct_ctt_id=ct.ctt_id "+
											" and ca.cct_id in ( select cdc_cct_id from cat_dvariable_categoria where cdc_cdv_id = "+  vari.id+" ) and ctt_id ="+ it.ctt_id
									resultTipo= sql.rows(queryCat.toString())
									def tam =resultTipo.size()
									def c=1
									resultTipo?.each
									{
										query=query+" cvv_id in (select  cvc_cvv_id from cat_variable_categoria where cvc_cct_id = "+it.cct_id+")  "
										if(c!=tam)
											query=query+" or "
										c++
									}
		
									if(cc!=tam)
										query=query+" and "
		
									cc++
		
								}
		
								if(vari.categorias){
		
									query=query+"  ) "
		
								}
		
								query=query+") o LEFT JOIN cat_region cr ON cr.crg_id = o.region_id LEFT JOIN cat_municipio cm ON cm.mun_id = o.municipio_id LEFT JOIN cat_localidad cl ON cl.ctl_id = o.localidad_id "+
										"GROUP BY "+
										"o.region_id,  "+
										"region,"+
										"o.municipio_id, " +
										"municipio,descripcion"
		
		
								//System.out.println("LA CONSULTA ES : "+query);
								def resultTotal = sql.rows(query.toString())
		
								if(resultTotal.size()>0){
//									System.out.println("LA CONSULTA ES : "+query);
									temVar= new RVariable()
									temVar.letra=vari.clave
		
									resultTotal?.each
									{
		
										//	System.out.println("Variable "+vari.clave+" Region-ID : "+it.region_id + " Region : "+it.region + " Mujeres : "+it.mujeres+" Hombres : "+it.hombres +" -- "+anio)
										ResultadoTemporal valorTem = new ResultadoTemporal()
										switch (vari.poblacion.clave) {
											case "H":
												valorTem.region=it.region
												valorTem.idRegion = it.region_id
												valorTem.municipio=it.municipio
												valorTem.idMunicipio = it.municipio_id
												valorTem.indicador=it.hombres
												valorTem.anio=anio
												temVar.valores.add(valorTem)
												temVar.descripcion=it.descripcion
												break;
		
											case "M":
												valorTem.region=it.region
												valorTem.idRegion =it.region_id
												valorTem.municipio=it.municipio
												valorTem.idMunicipio = it.municipio_id
												valorTem.indicador=it.mujeres
												valorTem.anio=anio
												temVar.valores.add(valorTem)
												temVar.descripcion=it.descripcion
												break;
		
											case "T":
												valorTem.region=it.region
												valorTem.idRegion =it.region_id
												valorTem.municipio=it.municipio
												valorTem.idMunicipio = it.municipio_id
												valorTem.indicador=it.total
												valorTem.anio=anio
												temVar.valores.add(valorTem)
												temVar.descripcion=it.descripcion
												break;
											default:
												break;
										}
									}
		
									rVariables.add(temVar)
									resutaldoVariables.add(temVar)
								}
								sql.close()
							}
		
						/***
						 * Comienza el calculo del indicador en base a las variables
						 * */
		
							if(rVariables.size()>0){
		
								letra=rVariables.get(0).letra
								num=rVariables.get(0).valores.size()
		
		
		
								rVariables.each {
									if( it.valores.size()!=0){
										num=it.valores.size()
										letra=it.letra
										valorBase=it.valores
		
		
									}
								}
		
								valorBase.each {
									base->
		
		
									formula=formula.replaceAll(letra, String.valueOf(base.indicador))
									rVariables.each {
										var->
										if(var.letra!=letra){
											var.valores.each {
		
												if(base.idMunicipio==it.idMunicipio){
		
													formula=formula.replaceAll(var.letra, String.valueOf(it.indicador))
												}
											}
										}
		
									}
//									System.out.println(formula);
		
									ResultadoTemporal rTemp = new ResultadoTemporal()
		
		
									ScriptEngineManager script = new ScriptEngineManager();
									ScriptEngine js = script.getEngineByName("JavaScript");
									try {
		
										rTemp.resultadoIndicador =js.eval("eval('"+formula+"')")
										rTemp.region= base.region
										rTemp.idRegion= base.idRegion
										rTemp.municipio= base.municipio
										rTemp.idMunicipio= base.idMunicipio
										rTemp.anio=base.anio
										listTemp.add(rTemp)
		
									} catch (Exception e) {
										// TODO Auto-generated catch block
										//e.printStackTrace();
									}
		
		
									formula= indicadorInstance?.formula?.sentencia
		
		
		
								}
		
								/***
							 * Comienza el proceso de ordenamiento para salida
							 * */
		
								listTemp.each {
									actual->
									def ban=0
									if(resultados.size()>0){
		
										resultados.each {
		
											if(it.idMunicipio==actual.idMunicipio){
												Resultado res= new Resultado()
												res.anio=actual.anio
												if(!Double.isNaN(actual.resultadoIndicador)){
													res.indicador=actual.resultadoIndicador
												}else{
													res.indicador=null;
												}
												it.resultados.add(res)
												ban=1
											}
										}
		
		
										if(ban!=1){
											Resultado res= new Resultado()
											res.anio=actual.anio
											if(!Double.isNaN(actual.resultadoIndicador)){
												res.indicador=actual.resultadoIndicador
											}else{
												res.indicador=null;
											}
											ResultadoIndicador ri =  new  ResultadoIndicador()
											ri.region=actual.region
											ri.idRegion=actual.idRegion
											ri.municipio= actual.municipio
											ri.idMunicipio= actual.idMunicipio
											ri.resultados.add(res)
											resultados.add(ri)
		
										}
									}else{
										Resultado res= new Resultado()
										res.anio=actual.anio
										if(!Double.isNaN(actual.resultadoIndicador)){
											res.indicador=actual.resultadoIndicador
										}else{
											res.indicador=null;
										}
										ResultadoIndicador ri =  new  ResultadoIndicador()
										ri.region=actual.region
										ri.idRegion=actual.idRegion
										ri.municipio= actual.municipio
										ri.idMunicipio= actual.idMunicipio
										ri.resultados.add(res)
										resultados.add(ri)
		
									}
								}
		
		
		
		
		
		
							}
		
							num=0
							rVariables= new ArrayList<RVariable>()
		
							listTemp = new ArrayList<ResultadoTemporal>()
		
		
		
		
		
		
							break;
		
						case 4:
		
		
		
						/***
						 * PROCESO DE SALIDA POR LOCALIDADES
						 *
						 * */
		
		
						/***
						 * Comienza la busqueda en el origen de datos en base a las variable
						 * */
		
							for(vari in indicadorInstance.variables){
		
								def sql = new Sql(sessionFactory.currentSession.connection())
		
								def query = "SELECT o.region_id,"+
										"descripcion,"+
										"COALESCE(cr.crg_descripcion, ''::character varying) AS region,"+
										"o.municipio_id,"+
										"COALESCE(cm.mun_descripcion, ''::character varying) AS municipio,"+
										"o.localidad_id,"+
										"COALESCE(cl.ctl_descripcion, ''::character varying) AS localidad,"+
										"sum(o.mujeres) as mujeres, "+
										"sum(o.hombres) as hombres , "+
										"sum(o.total)  as total "+
										"FROM (SELECT cat_variable.cvv_clave AS clave, "+
										"	cat_variable.cvv_descripcion AS descripcion, "+
										"	cat_variable.cvv_region AS region_id, "+
										"	cat_variable.cvv_municipio AS municipio_id, "+
										"	cat_variable.cvv_localidad AS localidad_id, "+
										"    cat_variable.cvv_mujeres AS mujeres, "+
										"	cat_variable.cvv_hombres AS hombres, "+
										"	cat_variable.cvv_poblacion_total AS total "+
										" FROM cat_variable "+
										"where "+
										" cvv_clave='"+vari.claveVar+"' and   cvv_region is not null     and   cvv_anio="+anio+" and   cvv_municipio is not null  and   cvv_localidad is not null  "
		
								if(vari.categorias){
		
									query=query+" and "+
											"("
		
								}
								def queryTipo="select ctt_id from cat_categoria ca ,cat_tipo ct where ca.cct_ctt_id=ct.ctt_id "+
										" and ca.cct_id in ( select cdc_cct_id from cat_dvariable_categoria where cdc_cdv_id = "+ vari.id+") group by ctt_id"
		
								def resultTipo
								def result = sql.rows(queryTipo.toString())
		
		
								def tamTipo =result.size()
								def cc=1
								result?.each
								{
		
		
									def queryCat="select cct_id from cat_categoria ca ,cat_tipo ct where ca.cct_ctt_id=ct.ctt_id "+
											" and ca.cct_id in ( select cdc_cct_id from cat_dvariable_categoria where cdc_cdv_id = "+  vari.id+" ) and ctt_id ="+ it.ctt_id
									resultTipo= sql.rows(queryCat.toString())
									def tam =resultTipo.size()
									def c=1
									resultTipo?.each
									{
										query=query+" cvv_id in (select  cvc_cvv_id from cat_variable_categoria where cvc_cct_id = "+it.cct_id+")  "
										if(c!=tam)
											query=query+" or "
										c++
									}
		
									if(cc!=tam)
										query=query+" and "
		
									cc++
		
								}
		
								if(vari.categorias){
		
									query=query+"  ) "
		
								}
		
								query=query+") o LEFT JOIN cat_region cr ON cr.crg_id = o.region_id LEFT JOIN cat_municipio cm ON cm.mun_id = o.municipio_id LEFT JOIN cat_localidad cl ON cl.ctl_id = o.localidad_id "+
										"GROUP BY "+
										"o.region_id,  "+
										"region,"+
										"o.municipio_id, " +
										"municipio,"+
										"o.localidad_id,"+
										"localidad,descripcion"
		
		
								//System.out.println("LA CONSULTA ES : "+query);
								def resultTotal = sql.rows(query.toString())
		
								if(resultTotal.size()>0){
									System.out.println("LA CONSULTA ES : "+query);
									temVar= new RVariable()
									temVar.letra=vari.clave
		
									resultTotal?.each
									{
		
										//	System.out.println("Variable "+vari.clave+" Region-ID : "+it.region_id + " Region : "+it.region + " Mujeres : "+it.mujeres+" Hombres : "+it.hombres +" -- "+anio)
										ResultadoTemporal valorTem = new ResultadoTemporal()
										switch (vari.poblacion.clave) {
											case "H":
												valorTem.region=it.region
												valorTem.idRegion = it.region_id
												valorTem.municipio=it.municipio
												valorTem.idMunicipio = it.municipio_id
												valorTem.localidad=it.localidad
												valorTem.idLocalidad = it.localidad_id
												valorTem.indicador=it.hombres
												valorTem.anio=anio
												temVar.valores.add(valorTem)
												temVar.descripcion=it.descripcion
												break;
		
											case "M":
												valorTem.region=it.region
												valorTem.idRegion =it.region_id
												valorTem.municipio=it.municipio
												valorTem.idMunicipio = it.municipio_id
												valorTem.localidad=it.localidad
												valorTem.idLocalidad = it.localidad_id
												valorTem.indicador=it.mujeres
												valorTem.anio=anio
												temVar.valores.add(valorTem)
												temVar.descripcion=it.descripcion
												break;
		
											case "T":
												valorTem.region=it.region
												valorTem.idRegion =it.region_id
												valorTem.municipio=it.municipio
												valorTem.idMunicipio = it.municipio_id
												valorTem.localidad=it.localidad
												valorTem.idLocalidad = it.localidad_id
												valorTem.indicador=it.total
												valorTem.anio=anio
												temVar.valores.add(valorTem)
												temVar.descripcion=it.descripcion
												break;
											default:
												break;
										}
									}
		
									rVariables.add(temVar)
									resutaldoVariables.add(temVar)
								}
								sql.close()
							}
		
						/***
						 * Comienza el calculo del indicador en base a las variables
						 * */
		
							if(rVariables.size()>0){
		
								num=rVariables.get(0).letra
								letra=rVariables.get(0).valores.size()
		
		
		
								rVariables.each {
									if( it.valores.size()<num){
										num=it.valores.size()
										letra=it.letra
										valorBase=it.valores
									}
								}
		
								valorBase.each {
									base->
		
		
									formula=formula.replaceAll(letra, String.valueOf(base.indicador))
									rVariables.each {
										var->
										if(var.letra!=letra){
											var.valores.each {
		
												if(base.idLocalidad==it.idLocalidad){
		
													formula=formula.replaceAll(var.letra, String.valueOf(it.indicador))
												}
											}
										}
		
									}
									System.out.println(formula);
		
									ResultadoTemporal rTemp = new ResultadoTemporal()
		
		
									ScriptEngineManager script = new ScriptEngineManager();
									ScriptEngine js = script.getEngineByName("JavaScript");
									try {
		
										rTemp.resultadoIndicador =js.eval("eval('"+formula+"')")
										rTemp.region= base.region
										rTemp.idRegion= base.idRegion
										rTemp.municipio= base.municipio
										rTemp.idMunicipio= base.idMunicipio
										rTemp.localidad= base.localidad
										rTemp.idLocalidad= base.idLocalidad
										rTemp.anio=base.anio
										listTemp.add(rTemp)
		
									} catch (ScriptException e) {
										// TODO Auto-generated catch block
										//e.printStackTrace();
									}
		
		
									formula= indicadorInstance?.formula?.sentencia
		
		
		
								}
		
								/***
							 * Comienza el proceso de ordenamiento para salida
							 * */
		
								listTemp.each {
									actual->
									def ban=0
									if(resultados.size()>0){
		
										resultados.each {
		
											if(it.idLocalidad==actual.idLocalidad){
												Resultado res= new Resultado()
												res.anio=actual.anio
												if(!Double.isNaN(actual.resultadoIndicador)){
													res.indicador=actual.resultadoIndicador
												}else{
													res.indicador=null;
												}
												it.resultados.add(res)
												ban=1
											}
										}
		
		
										if(ban==1){
											Resultado res= new Resultado()
											res.anio=actual.anio
											if(!Double.isNaN(actual.resultadoIndicador)){
												res.indicador=actual.resultadoIndicador
											}else{
												res.indicador=null;
											}
											ResultadoIndicador ri =  new  ResultadoIndicador()
											ri.region=actual.region
											ri.idRegion=actual.idRegion
											ri.municipio= actual.municipio
											ri.idMunicipio= actual.idMunicipio
											ri.localidad= actual.localidad
											ri.idLocalidad= actual.idLocalidad
		
											ri.resultados.add(res)
											resultados.add(ri)
		
										}
									}else{
										Resultado res= new Resultado()
										res.anio=actual.anio
										if(!Double.isNaN(actual.resultadoIndicador)){
											res.indicador=actual.resultadoIndicador
										}else{
											res.indicador=null;
										}
										ResultadoIndicador ri =  new  ResultadoIndicador()
										ri.municipio= actual.municipio
										ri.idMunicipio= actual.idMunicipio
										ri.localidad= actual.localidad
										ri.idLocalidad= actual.idLocalidad
										ri.resultados.add(res)
										resultados.add(ri)
		
									}
								}
		
		
		
		
		
		
							}
		
							num=0
							rVariables= new ArrayList<RVariable>()
							listTemp = new ArrayList<ResultadoTemporal>()
		
		
		
							break;
		
					}
		
		
		
				}
		
		
		
				//		resultados.each {
				//
				//			System.out.println(it.idRegion + " : "+it.region+"    "+ it.idMunicipio + " : "+it.municipio);
				//			System.out.println("Tama�o  "+it.resultados.size());
				//			it.resultados.each {
				//				an->
				//
				//					an.each {
				//
				//						System.out.println("A�o : "+it.anio + " :Indicador  :"+it.indicador);
				//
				//					}
				//			}
				//
				//		}
				//
				//		System.out.println("rVariable tam: "+resutaldoVariables.size())
				//		resutaldoVariables.each{
				//			System.out.println("valores tam: "+it.valores.size())
				//			it.valores.each {
				//				System.out.println("Region: "+it.region)
				//			}
				//
				//		}
				
				DetalleIndicador detalleIndicador = new DetalleIndicador()
				detalleIndicador.resultados = resultados
				detalleIndicador.rVariables = resutaldoVariables
		
		
				return detalleIndicador
		
				//[indicadorInstance:indicadorInstance,resultados:resultados]
		
		
			}
	
	def descargarDocumento(){
		try {
			def path = grailsApplication.config.mx.indesti.cednna.valores.directoriouploads + params.tipo + "/" + params.nivel + "/" + params.documento
			def archivo = new File (path)
			response.setContentType("application/octet-stream")
			response.setHeader("Content-disposition", "attachment;filename=${archivo.getName()}")
			response.outputStream << archivo.newInputStream()
		} catch(Exception ex){
			response.sendError(500)
		}
	}
	
	def actualizarTablaDocumento(Integer id){
		render template:'tablaDocumento', model:[nivel: params.nivel, tipo:id]
	}
	
	def enviarMensaje(){
		def correo = Valor.findByKey('correoContacto')
		String mensaje = params.mensaje
		String nombre = params.nombre!=''? ("Atentamente: " + params.nombre + "."):''
		sendMail {
			to correo?.valor
		   subject params.asunto
		   html  """
		   			${mensaje} <br><br>
		   			${nombre}
		   		"""
		}
		
		String nombreRespuesta = params.nombre!=''?(', ' +params.nombre) + '.':''
		
		sendMail {
			to params?.correo
		   subject 'Respuesta CEDNNA'
		   html  """
		   			<b>Gracias ${nombreRespuesta}<b> <br><br>
		   			Hemos recibido tu mensaje, en breve te responderemos.<br>
		   			No responda a este correo.
		   		"""
		}
		redirect action:'contacto' 
	}

}	
