/**
 *  Smart Heat Tape Controller 
 *
 *  Copyright 2015 Edward Robertshaw
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(

    name: "Smart Heat Tape Controller ",
    namespace: "www.aperations.com",
    author: "Edward Robertshaw",
    description: "Smart heat tape controller. Reduced energy usage by checking weather forecast.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
    appSetting "zipcode"
}


preferences { 
  section("Smart heat tape setup") {
    input "heattape", "capability.switch", title: "Heat tape switch", required: true, multiple: true
    input "zipcode", "text", title: "Zipcode", required: true
  }
}




def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	runEvery1Hour(updateHeatTape)	
}


def updateHeatTape() {
    log.debug "updateHeatTape"  
    
    if (state.snowOnRoof == null){
    	state.snowOnRoof = false 
    } 
    
    if(state.melt_point_score_since_snow == null){
    	state.melt_point_score_since_snow = 0
    }
       
    Map currentConditions =  getWeatherFeature("conditions" , zipcode)
    
    log.debug "zipcode:" + zipcode
    
   
   	//currentConditions.current_observation.each { k ,v -> log.debug(k + ": " + v) }

	recordWeatherStats(currentConditions.current_observation)
    
   
    log.debug "aboveMeltTemperature:" + state.aboveMeltTemperature
    log.debug "----melt_point_score_since_snow:" + state.melt_point_score_since_snow
    
 
    state.snowOnRoof = (state.melt_point_score_since_snow < 504 )
    
    log.debug "snowOnRoof:" + state.snowOnRoof
    
    setHeatTape()
   
}

def calculateSnowMeltScore(observation){
        if( observation.temp_c > 20 ){
        	state.melt_point_score_since_snow = 100 + state.melt_point_score_since_snow  
        } else if(observation.temp_c > 15 ){
        	state.melt_point_score_since_snow = 70 + state.melt_point_score_since_snow 
        } else if( observation.temp_c > 10 ){
        	state.melt_point_score_since_snow = 50 + state.melt_point_score_since_snow 
        } else if( observation.temp_c > 5 ){
        	state.melt_point_score_since_snow = 4 + state.melt_point_score_since_snow 
        } else if( observation.temp_c > 0 ){
        	state.melt_point_score_since_snow = 2 + state.melt_point_score_since_snow  
        } else if( observation.temp_c > -3 ){
        	state.melt_point_score_since_snow = 1 + state.melt_point_score_since_snow  
        }
}

def recordWeatherStats(observation){
	log.debug "temp_c:" + observation.temp_c
	if( observation.temp_c < 0){
		state.lastFreeze = new Date()
    }
    log.debug "lastFreeze:" + state.lastFreeze

    log.debug "precip_today_metric:" + observation.precip_today_metric
    if( observation.precip_today_metric > 0){
		state.lastPrecipitation = new Date()
    }
    log.debug "lastPrecipitation:" + state.lastPrecipitation
    
    if( observation.precip_today_metric > 0 && observation.temp_c < 0 ){
		state.lastFreezingPrecipitation = new Date()
        state.melt_point_score_since_snow = 0;
    }
    log.debug "lastFreezingPrecipitation:" + state.lastFreezingPrecipitation
    
    if( observation.temp_c > -3 ){
    	state.aboveMeltTemperature = true
        calculateSnowMeltScore(observation)
    } else {
    	state.aboveMeltTemperature = false		
    }
    
}


def setHeatTape(){
	if(state.snowOnRoof && state.aboveMeltTemperature){
    	heattape.on()
    }else{
    	heattape.off()
    }
}




