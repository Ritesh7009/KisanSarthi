package com.kisansarthi.weather;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WeatherService {

    private final Map<String, WeatherDto> weatherCache = new ConcurrentHashMap<>();

    public WeatherService() {
        initPresets();
    }

    private void initPresets() {
        WeatherDto sehore = new WeatherDto();
        sehore.setDistrict("Sehore");
        sehore.setCondition("Clear Sky");
        sehore.setDescription("Clear sunny weather, ideal for harvest transport.");
        sehore.setHindiDescription("साफ धूप का मौसम, फसल परिवहन के लिए सर्वथा उपयुक्त।");
        sehore.setTempCelsius(31.5);
        sehore.setRainProbability(5);
        sehore.setHumidity(42);
        sehore.setWindSpeedKmH(9.5);
        sehore.setAlertSeverity("NONE");
        sehore.setAdvisory("No rain expected for the next 72 hours. Ideal window for trolley movement.");
        sehore.setHindiAdvisory("अगले 72 घंटों में वर्षा की संभावना नहीं है। ट्रॉली परिवहन के लिए आदर्श समय।");
        weatherCache.put("sehore", sehore);

        WeatherDto harda = new WeatherDto();
        harda.setDistrict("Harda");
        harda.setCondition("Partly Cloudy");
        harda.setDescription("Scattered clouds, low precipitation chance.");
        harda.setHindiDescription("आंशिक बादल, वर्षा की नगण्य संभावना।");
        harda.setTempCelsius(33.0);
        harda.setRainProbability(15);
        harda.setHumidity(48);
        harda.setWindSpeedKmH(12.0);
        harda.setAlertSeverity("LOW");
        harda.setAdvisory("Ensure tarpaulin cover on trolleys during transit as cautionary practice.");
        harda.setHindiAdvisory("सतर्कता के तौर पर रास्ते में ट्राली पर तिरपाल ढककर लाएं।");
        weatherCache.put("harda", harda);

        WeatherDto ujjain = new WeatherDto();
        ujjain.setDistrict("Ujjain");
        ujjain.setCondition("Sunny");
        ujjain.setDescription("Dry, warm conditions favorable for mandi operations.");
        ujjain.setHindiDescription("शुष्क व गर्म मौसम, मंडी तुलाई के लिए अनुकूल।");
        ujjain.setTempCelsius(32.8);
        ujjain.setRainProbability(8);
        ujjain.setHumidity(38);
        ujjain.setWindSpeedKmH(10.2);
        ujjain.setAlertSeverity("NONE");
        ujjain.setAdvisory("Moisture levels will remain within acceptable procurement limits.");
        ujjain.setHindiAdvisory("नमी का स्तर उपार्जन मानकों के अनुरूप बना रहेगा।");
        weatherCache.put("ujjain", ujjain);
    }

    public WeatherDto getWeatherForDistrict(String district) {
        String key = district != null ? district.toLowerCase().trim() : "sehore";
        WeatherDto dto = weatherCache.get(key);
        if (dto != null) return dto;

        WeatherDto generic = new WeatherDto();
        generic.setDistrict(district != null ? district : "Madhya Pradesh");
        generic.setCondition("Clear Sky");
        generic.setDescription("Normal weather conditions across Malwa-Nimar region.");
        generic.setHindiDescription("मालवा-निमाड़ क्षेत्र में सामान्य अनुकूल मौसम।");
        generic.setTempCelsius(32.0);
        generic.setRainProbability(10);
        generic.setHumidity(44);
        generic.setWindSpeedKmH(11.0);
        generic.setAlertSeverity("NONE");
        generic.setAdvisory("Optimum window for grain drying and dispatch.");
        generic.setHindiAdvisory("उपार्जन केंद्र तक अनाज लाने हेतु उपयुक्त मौसम।");
        return generic;
    }
}
