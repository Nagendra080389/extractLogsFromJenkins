package com.salesForceFileName;

import com.google.gson.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by nagesingh on 6/14/2017.
 * Written just for particular USE CASE
 * There are many things to be corrected here, but this works well. No code
 * refactoring has been done. Hence the code is a messay one.
 *
 * The Code will run when the build gets triggered and it will check for the affected files in the particular commit
 * and will save it to a txt file.
 */

public class ExtractLogs {

    public static final String FILE_NAME_TO_READ = "C:\\Jenkins\\ConfigurationFile.txt";

    public static void main(String[] args) throws IOException {
        Map<String, String> propertiesMap = new HashMap<String, String>();
        FileReader fileReader = new FileReader(FILE_NAME_TO_READ);
        createMapOfProperties(fileReader, propertiesMap);
        boolean classPresent = false;
        // Cleaning the file to push the new Changed Files
        clearTheFile(propertiesMap);
        List<String> salesForceClasses = new ArrayList<String>();
        HttpURLConnection conn = null;
        BufferedWriter bufferedWriter  = new BufferedWriter(new FileWriter(propertiesMap.get("ClassesTextFilepath")));;
        try {

            URL url = new URL(propertiesMap.get("JenkinsBuildURL"));
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String output;
            JsonParser parser = new JsonParser();
            JsonObject jsonObject = null;

            while ((output = br.readLine()) != null) {
                jsonObject = parser.parse(output).getAsJsonObject();
            }

            if (jsonObject != null) {
                System.out.println("jsonObject ---> "+jsonObject);
                jsonObject = (JsonObject) jsonObject.get("lastBuild");

                String lastBuildUrl = jsonObject.get("url").getAsString();

                URL newUrl = new URL(lastBuildUrl + "api/json");
                //URL newUrl = new URL("http://localhost:8080/job/FirstBuild/119/api/json");
                conn = (HttpURLConnection) newUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                if (conn.getResponseCode() != 200) {
                    throw new RuntimeException("Failed : HTTP error code : "
                            + conn.getResponseCode());
                }

                BufferedReader newBr = new BufferedReader(new InputStreamReader(
                        (conn.getInputStream())));

                String newOutput;
                JsonParser newParser = new JsonParser();
                JsonObject newJsonObject = null;

                while ((newOutput = newBr.readLine()) != null) {
                    newJsonObject = newParser.parse(newOutput).getAsJsonObject();
                }

                if (newJsonObject != null) {
                    JsonObject newObject = (JsonObject) newJsonObject.get("changeSet");
                    JsonArray jsonArray = (JsonArray) newObject.get("items");
                    for (JsonElement jsonElement : jsonArray) {
                        JsonObject jsonObject1 = (JsonObject) jsonElement;
                        JsonArray affectedPaths = (JsonArray) jsonObject1.get("affectedPaths");
                        for (JsonElement affectedPath : affectedPaths) {
                            String asString = affectedPath.getAsString();
                            //salesForceClasses.add(asString);
                            if (asString != null && asString.startsWith("src/classes")) {
                                String[] split = asString.split("/");
                                for (String className : split) {
                                    if (className.endsWith(".cls")) {
                                        classPresent = true;
                                        System.out.println("NEW CLASSES COMMITTED, PMD CODE REVIEW WILL RUN");
                                        salesForceClasses.add(className.substring(0, className.length() - 4));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            for (String salesForceClass : salesForceClasses) {
                bufferedWriter.write(salesForceClass + '\n');
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if(!classPresent){
                System.out.println("******** SORRY NO CLASSES COMMITTED ********");
            }
        }
    }

    private static void createMapOfProperties(FileReader fileReader, Map<String, String> propertiesMap) throws IOException {
        BufferedReader bufferedReader = null;
        String sCurrentLine;
        bufferedReader = new BufferedReader(fileReader);

        while ((sCurrentLine = bufferedReader.readLine()) != null) {
            sCurrentLine= sCurrentLine.replaceAll("\\s+","");
            String[] split = sCurrentLine.split("=");
            propertiesMap.put(split[0], split[1]);

        }
    }

    private static void clearTheFile(Map<String, String> propertiesMap) throws IOException {
        FileWriter fwOb = new FileWriter(propertiesMap.get("ClassesTextFilepath"), false);
        PrintWriter pwOb = new PrintWriter(fwOb, false);
        pwOb.flush();
        pwOb.close();
        fwOb.close();
    }

}
