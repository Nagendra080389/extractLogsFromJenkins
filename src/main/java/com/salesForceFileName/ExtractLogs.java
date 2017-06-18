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
import java.util.List;

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

    private static String FILE_NAME = "C:\\Jenkins\\SalesForceClasses.txt";

    public static void main(String[] args) throws IOException {
        boolean classPresent = false;
        clearTheFile();
        List<String> salesForceClasses = new ArrayList<String>();
        HttpURLConnection conn = null;
        BufferedWriter bufferedWriter  = new BufferedWriter(new FileWriter(FILE_NAME));;
        try {

            URL url = new URL("http://localhost:8080/job/FirstBuild/api/json");
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

    private static void clearTheFile() throws IOException {
        FileWriter fwOb = new FileWriter(FILE_NAME, false);
        PrintWriter pwOb = new PrintWriter(fwOb, false);
        pwOb.flush();
        pwOb.close();
        fwOb.close();
    }

}
