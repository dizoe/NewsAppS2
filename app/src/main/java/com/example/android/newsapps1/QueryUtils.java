package com.example.android.newsapps1;


import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class QueryUtils {

    private static final String LOG_TAG = QueryUtils.class.getSimpleName();

    private QueryUtils() {
    }

    public static List<News> fetchNewsData(String requestUrl) {

        URL url = createUrl(requestUrl);

        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem with HTTP ", e);
        }

        List<News> news = extractFeatureFromJson(jsonResponse);

        return extractFeatureFromJson(jsonResponse);
    }

    private static URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Problem with URL ", e);
        }
        return url;
    }


    private static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        if (url == null) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem with JSON ", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {

                inputStream.close();
            }
        }
        return jsonResponse;
    }

    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    private static List<News> extractFeatureFromJson(String newsJSON) {

        if (TextUtils.isEmpty(newsJSON)) {
            return null;
        }

        List<News> newsList = new ArrayList<>();

        try {

            JSONObject baseJsonResponse = new JSONObject(newsJSON);

            JSONArray newsArray = baseJsonResponse.getJSONObject("response").getJSONArray("results");

            for (int i = 0; i < newsArray.length(); i++) {

                JSONObject currentNews = newsArray.getJSONObject(i);

                String title = currentNews.getString("webTitle");

                String sectionName = currentNews.getString("sectionName");

                JSONArray tags = currentNews.getJSONArray("tags");

                String authorFullName = "";
                if (!tags.isNull(0)) {
                    JSONObject currentTag = tags.getJSONObject(0);

                    String authorFirstName = !currentTag.isNull("firstName") ? currentTag.getString("firstName") : "";

                    String authorLastName = !currentTag.isNull("lastName") ? currentTag.getString("lastName") : "";

                    authorFullName = StringUtils.capitalize(authorFirstName.toLowerCase().trim()) + " " + StringUtils.capitalize(authorLastName.toLowerCase().trim());
                    if (authorFirstName.trim() != "" || authorLastName.trim() != "") {
                        authorFullName = ("Author: ").concat(authorFullName);
                    } else {
                        authorFullName = "";
                    }
                }

                String originalPublicationDate = currentNews.getString("webPublicationDate");

                Date publicationDate = null;
                try {
                    publicationDate = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")).parse(originalPublicationDate);
                } catch (Exception e) {

                    Log.e("QueryUtils", "Problem parsing the news date", e);
                }

                String url = currentNews.getString("webUrl");

                String image = "";

                if (!currentNews.isNull("fields") && !currentNews.getJSONObject("fields").isNull("thumbnail")) {
                    image = currentNews.getJSONObject("fields").getString("thumbnail");
                }

                if (image == "") {
                    image = "http://via.placeholder.com/500x500";
                }

                News news = new News(title, sectionName, authorFullName, publicationDate, url, image);

                newsList.add(news);
            }
        } catch (JSONException e) {

            Log.e("QueryUtils", "Problem parsing the news JSON results", e);
        }

        return newsList;
    }
}
