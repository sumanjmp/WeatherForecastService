package com.comp.services;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Scanner;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
/**
 * @author suman jampany
 * This class is used to get the weather forecast information
 *
 */
public class WeatherForecastService {
	
	private static final WeatherForecastService INSTANCE = new WeatherForecastService();
	public static WeatherForecastService getInstance(){ return INSTANCE; }
	private static final Logger logger = Logger.getLogger(WeatherForecastService.class.getName());
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
				if(args.length == 0 || (args.length != 2))
			    {
					logger.error("Missing the latitude and longitude arguments");
			        System.exit(0);
			    }
				Double latitude = Double.parseDouble(args[0]);
				Double longitude = Double.parseDouble(args[1]);
				WeatherForecastService.getInstance().validateInputArgs(latitude,longitude);
				String weatherPointUrl =WeatherForecastService.getInstance().buildweatherPointUrl(latitude,longitude);
				String weatherPointDetails =WeatherForecastService.getInstance().getWeatherDetails(weatherPointUrl);
				JSONObject forecastProperty =WeatherForecastService.getInstance().getForecastLocation(weatherPointDetails);
				String forecast  =WeatherForecastService.getInstance().getForecastValue(forecastProperty,"forecast");
				String forecastResponse =WeatherForecastService.getInstance().getWeatherDetails(forecast);
				JSONObject forecastSubProperty =WeatherForecastService.getInstance().getForecastLocation(forecastResponse);
				JSONArray forecastPropertyValues  =WeatherForecastService.getInstance().getForecastForWeek(forecastSubProperty,"periods");
				HashMap<String, String> detailedForecasts =WeatherForecastService.getInstance().prepareForecastDetailsForFiveDays(forecastPropertyValues);
				WeatherForecastService.getInstance().displayForecastDetails(detailedForecasts);
			} 
			catch (Exception ex) 
	        { 
				logger.error("Exception encountered " + ex); 
	        } 
	}
	
	
	/**
	 * @return
	 * @throws IOException
	 */
	public Properties getPropValues() throws IOException {
		Properties  prop = new Properties();
		String propFileName ="config.properties";
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
		if(inputStream !=null) {
			prop.load(inputStream);
		}
		return prop;
	}
	
	
	/**
	 * @param latitude
	 * @param longitude
	 * @return
	 * @throws IOException
	 */
	public boolean validateInputArgs(Double latitude,Double longitude) throws IOException {
		Boolean isLatitudeValid = true;
		Boolean isLongitudeValid = true;
		 if (latitude < -90 || latitude > 90)
            {
			 logger.error("Latitude must be between -90 and 90 degrees");
			 isLatitudeValid =false;
			 }
		 if (longitude < -180 || longitude > 180)
            {
			 logger.error("Longitude must be between -180 and 180 degrees");
			 isLongitudeValid =false;
		     }
		 if(isLatitudeValid==false || isLongitudeValid==false ) {
			 System.exit(0);
		 }
		return true;
	}
	
	
	/**
	 * @param weatherPointUrl
	 * @return
	 * @throws MalformedURLException
	 * @throws ProtocolException
	 * @throws IOException
	 */
	public String getWeatherDetails(String weatherPointUrl) throws MalformedURLException,ProtocolException,IOException {
		
			URL url = new URL(weatherPointUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			Properties prop =WeatherForecastService.getInstance().getPropValues();
			conn.setConnectTimeout(Integer.valueOf(prop.getProperty("connectionTimeOut")));
			conn.setReadTimeout(Integer.valueOf(prop.getProperty("readTimeOut")));
			conn.connect();
			int forecastUrlResponsecode = conn.getResponseCode();
			Logger logger = Logger.getLogger(WeatherForecastService.class.getName());
			logger.debug("Response code for "+weatherPointUrl + " is :"+forecastUrlResponsecode);
			Scanner scanner =new Scanner(url.openStream());
			String forecastResponse = "";
			while(scanner.hasNext()) {
				forecastResponse+=scanner.nextLine();
			}
			scanner.close();
			return forecastResponse;
		
	}
	
	
	/**
	 * @param weatherResponse
	 * @return
	 */
	public JSONObject getForecastLocation(String weatherResponse) {
		JSONObject obj = new JSONObject(weatherResponse);
		JSONObject properties  = obj.getJSONObject("properties");
		return properties;
	}
	
	
	/**
	 * @param forecastDetails
	 * @param key
	 * @return
	 */
	public String getForecastValue(JSONObject forecastDetails,String key) {
		String forecastValue  = (String) forecastDetails.get(key);
		return forecastValue;
	}
	
	
	/**
	 * @param futureForecast
	 * @param key
	 * @return
	 */
	public JSONArray getForecastForWeek(JSONObject futureForecast,String key) {
		JSONArray properties  = futureForecast.getJSONArray(key);
		return properties;
	}
	
	
	/**
	 * @param latitude
	 * @param longitude
	 * @return
	 * @throws IOException
	 */
	public String buildweatherPointUrl(Double latitude,Double longitude) throws IOException {
		Properties prop =WeatherForecastService.getInstance().getPropValues();
		String weatherPointUrl =prop.getProperty("weatherPointUrl");
		weatherPointUrl =weatherPointUrl.concat(Double.toString(latitude)).concat(",").concat(Double.toString(longitude));
		return weatherPointUrl;
	}
	
	
	/**
	 * @param forecastPropertyValues
	 * @return
	 * @throws IOException
	 */
	public HashMap<String, String> prepareForecastDetailsForFiveDays(JSONArray forecastPropertyValues) throws IOException {
		HashMap<String, String> detailedForecasts = new LinkedHashMap<>();
        Properties prop =WeatherForecastService.getInstance().getPropValues();
        int forecastNoOfDaysCount =Integer.parseInt(prop.getProperty("forecastNoOfDaysCount"));
		for (int forecastProperties = 0; forecastProperties < forecastPropertyValues.length(); forecastProperties++) {
			 if(forecastProperties==forecastNoOfDaysCount){  
                    //breaking the loop  
                    break;  
                } 
			JSONObject periodObject = forecastPropertyValues.getJSONObject(forecastProperties);
			detailedForecasts.put((String) periodObject.get("name"),(String) periodObject.get("detailedForecast"));
		}
		return detailedForecasts;
	}
	
	/**
	 * @param detailedForecasts
	 */
	public void displayForecastDetails(HashMap<String, String> detailedForecasts) {
		for (String dayOfWeek : detailedForecasts.keySet()) {
			logger.info(dayOfWeek + " : " + detailedForecasts.get(dayOfWeek));
			}
	}
}
