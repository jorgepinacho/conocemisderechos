package mx.gob.redoaxaca.cednna

import org.codehaus.groovy.grails.web.json.JSONArray
import grails.converters.JSON
import groovy.json.JsonSlurper
import grails.plugins.springsecurity.Secured
import java.text.NumberFormat

@Secured( ['IS_AUTHENTICATED_ANONYMOUSLY'])
class PublicoService {

    def serviceMethod() {

    }



    def getTablaIndicador(resultadosIndicador, tipo){
    	def list = []

    	if(resultadosIndicador!=null){
	    	resultadosIndicador.each{ lista ->
	    		def listRow = [:]
	    		int index = 0
	    		
	    		if(tipo==1){
	    			listRow.put(index,'Oaxaca')  
	    			index++  
	    		}

	    		lista.each{ resultadoIndicador ->	    			
	    			if(tipo==2 || tipo==3 || tipo==4){
	    				listRow.put(index,resultadoIndicador?.region) 
	    				index++
	    			}
	    			if(tipo==3 || tipo==4){    				
	    				listRow.put(index,resultadoIndicador?.municipio)
	    				index++
	    			}
	    			
	    			resultadoIndicador?.resultados.each{ result ->
	    				listRow.put(index,result?.indicador==null ? '-':result?.indicador.round(2)) 	
	    				index++    				
	    			}
	    		}

	    		list.addAll(0,listRow)	    	
	    	}
    	}
    	return list
    }

    def getTablaDatosCalculo(datosCalculo, tamVariables, tipo){
    	def list = []

    	if(datosCalculo!=null){
	    	datosCalculo.eachWithIndex(){ datos, i ->
	    		datos?.valores.each{ valor ->
		    		def listRow = [:]
		    		int index = 0

		    		
		    		if(i<tamVariables){
		    			listRow.put(index, datos.descripcion)
		    			index++
		    			if(tipo==1){
		    				listRow.put(index, 'Oaxaca')
		    				index++
		    			}else if(tipo==2){
		    				listRow.put(index, valor.region)
		    				index++
		    			}else{
		    				listRow.put(index, valor.municipio)
		    				index++
		    			}
		    			
		    			datosCalculo.eachWithIndex(){ datosAux, cont ->
			    			if(((cont+i) % tamVariables) == 0){
			    				datosAux?.valores.each{ valores ->	

			    					if(tipo==1){
					    				listRow.put(index, addCommas(valores.indicador))
					    				index++
			    						if(valores.mujeres==0)
			    							listRow.put(index, '-')
			    						else
			    							listRow.put(index, addCommas(valores.mujeres))
			    						index++
			    						if(valores.hombres==0)
			    							listRow.put(index, '-')
			    						else
			    							listRow.put(index, addCommas(valores.hombres))
			    						index++
					    			}else if(tipo==2 && valor.region==valores.region){
					    				listRow.put(index, addCommas(valores.indicador))
					    				index++		
					    				if(valores.mujeres==0)
			    							listRow.put(index, '-')
			    						else
			    							listRow.put(index, addCommas(valores.mujeres))
			    						index++
			    						if(valores.hombres==0)
			    							listRow.put(index, '-')
			    						else
			    							listRow.put(index, addCommas(valores.hombres))
			    						index++
					    			}else if(tipo==3 && valor.municipio.equals(valores.municipio)){
					    				listRow.put(index, addCommas(valores.indicador))
					    				index++
					    				if(valores.mujeres==0)
			    							listRow.put(index, '-')
			    						else
			    							listRow.put(index, addCommas(valores.mujeres))
			    						index++
			    						if(valores.hombres==0)
			    							listRow.put(index, '-')
			    						else
			    							listRow.put(index, addCommas(valores.hombres))
			    						index++
					    			}
			    				}
			    			}
			    		}
		    		}

		    		if(listRow.size()!=0){
		    			list.addAll(list.size(),listRow)	
		    		}
	    		}
	    	}	
		}
		
    	return list
    }

    def addCommas(def nStr) {
    	if(!nStr instanceof Integer){
    		nStr.toDouble()
    	}
    	nStr = nStr.toString()
    	def x = nStr.split('\\.')
    	def num = x[0].toInteger()
    	def decimales = x.length > 1 ? '.' + x[1] : ''
    	def numeroConFormato = NumberFormat.getNumberInstance(Locale.US).format(num)
    	return numeroConFormato + decimales
    }
}
