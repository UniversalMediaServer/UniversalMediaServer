package net.pms.util;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import static java.net.URLEncoder.encode;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
Copyright (c) 2015 yakka34

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

/*
 * Unofficial cpubenchmark.net API
 *
 * @version 1.0.2.1
 * @author yakka34
 */
public class CbmAPI {

	private static final Logger LOGGER = LoggerFactory.getLogger(CbmAPI.class);

    //Folowing 3 methods are the only ones that can be called outside of the class.
    //getCpuByUrl takes a complete cpubenchmark.net url and passes it forward as it is.
    public static String getCpuByUrl(String url) {
        String jsonString = getCpuInfo(url);
        return jsonString;
    }

    //getCpuById uses cpubenchmark.net individually assigned cpu id.
    public static String getCpuById(int id) {
        String jsonString = getCpuInfo("https://www.cpubenchmark.net/cpu.php?id=" + id);
        return jsonString;
    }

    //Names must be complete and intel cpus has to have "@ clockspeed" in them. Examples: Intel Core i7-5960X @ 3.00GHz or AMD FX-8350 Eight-Core
    public static String getCpuByName(String cpuName) {
        String encodedUrl = encodeToUrl(cpuName);
        String jsonString = getCpuInfo("https://www.cpubenchmark.net/cpu.php?cpu=" + encodedUrl);
        return jsonString;
    }

    //Uses cpubenchmark.net's zoom zearch and returns cpu's benhmark/information url.
    public static String searchCpuByName(String cpuName) {
        String encodedName = encodeToUrl(cpuName);
        Document html = null;
        String url = null;
        try {
            //Connects to zoom's search engine and looks for given cpu from benhmarks section.
            html = Jsoup.connect("https://www.passmark.com/search/zoomsearch.php?zoom_sort=0&zoom_query=" + encodedName + "&zoom_cat%5B%5D=5").get();
        } catch (IOException e) {
            System.out.println("Connection throws an exception: " + e);
        }
        
        //Regex check is used to validate correct search result.
        if (html != null) {
            Elements links = html.select("div.results");
            links = links.select("a[href~=^(https?:\\/\\/www.cpubenchmark.net/cpu.php\\?)]");
            url = links.attr("href");
            if(url.isEmpty()){
                return "No results found for: " + cpuName;
            }
        } //message for connection issues.
        else {
            return "Connection to the search engine failed.";
        }
        return url;
    }

    //getCpuInfo calls various methods to construct a complete Json formated string from given url.
    private static String getCpuInfo(String url) {
        Document html = null;
        String infoString;
        String jsonString;
        String infoArray[];
        try {
            html = Jsoup.connect(url).get();
        } catch (IOException e) {
            System.out.println("Connection to: " + url + " ,throws an exception: " + e);
        }

        if (html != null) {
            //Attributes in the infoString are seperated by commas and data by semicolons.
            infoString = parseHtmlForInfo(html);
            //infoString needs to be split into array for further processing.
            infoArray = parseStringToArray(infoString);
            //Array is used to create JSONString.
            jsonString = convertArrayToJsonString(infoArray);
        } else {
            System.out.println("No html value assigned returning null!");
            return null;
        }
        return jsonString;
    }

    //Parses given html file. Data parsing is hardcoded because lack of id tagging on cpubenchmark.net behalf.
    private static String parseHtmlForInfo(Document html) {
        //Instead of parsing the the whole html page everytime, only useful table section is used.
        Element table = html.select("table.desc").first();
        //<span> containing the name is clearly labeled as cpuname.
        String cpuName = table.select("span.cpuname").text();
        //Score is the last but one to use <span> tag and will be parsed to int.
        int cpuScoreTagPosition = table.select("span").size() - 2;
        int cpuScore = 0;
        String cpuScoreString = null;
        try {
        	cpuScoreString = table.select("span").get(cpuScoreTagPosition).text();
        	cpuScore = Integer.parseInt(cpuScoreString);
        } catch (NumberFormatException e) {
			LOGGER.debug("Retrieved CPU score format '{}' is wrong.", cpuScoreString);
		}

        //There are 2 <em> tags containing information. First one has description and second one has "Other names" eg.alternative name.
        String description = table.select("em").first().text();
        String altName = table.select("em").last().text();
        //Name -> Score -> possible description -> AltName.
        String infoString = cpuName + ",Score:" + cpuScore + "," + description + ",AltName:" + altName;
        return infoString;
    }

    //Splits the infoString into array by using regex split.
    private static String[] parseStringToArray(String infoString) {
        //Splits the String everytime it founds comma or semicolon.
        String[] infoArray = infoString.split("[,:]");
        return infoArray;
    }

    //Data from array is read and placed into json object which will be parsed into String.
    private static String convertArrayToJsonString(String[] infoArray) {
        //Depending on prefered formating, use of temp is not necessary.
        JSONObject temp = new JSONObject();
        JSONObject jObj = new JSONObject();
        int length = infoArray.length;
        for (int i = 1; i < length - 1; i += 2) {
            int y = i + 1;
            temp.put(infoArray[i].trim(), infoArray[y].trim());
        }
        //Name of the cpu is always located first in the array.
        jObj.put(infoArray[0], temp);
        return jObj.toString();
    }

    private static String encodeToUrl(String string) {
        String encodedUrl = null;
        try {
            encodedUrl = encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.out.println("Encoding not supported: " + e);
        }
        return encodedUrl;
    }
}
