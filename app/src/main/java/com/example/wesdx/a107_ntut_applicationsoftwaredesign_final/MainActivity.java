package com.example.wesdx.a107_ntut_applicationsoftwaredesign_final;

//https://github.com/ptxmotc/Sample-code

import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

public class MainActivity extends AppCompatActivity {

    private final static String XMLUrl = "https://ptx.transportdata.tw/MOTC/v2/Rail/TRA/Station?$top=30&$format=xml";
    private final static String APIUrl = "https://ptx.transportdata.tw/MOTC/v2/Rail/TRA/Station?$top=30&$format=JSON";

    private TextView location, country, temperature, humidity, pressure;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        location = (TextView) findViewById(R.id.textView);
        country = (TextView) findViewById(R.id.textView2);
        temperature = (TextView) findViewById(R.id.textView3);
        humidity = (TextView) findViewById(R.id.textView4);
        pressure = (TextView) findViewById(R.id.textView5);

        runAsyncTask();
    }

    //取得當下UTC時間
    public static String getServerTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.TAIWAN);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }

    private void runAsyncTask() {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... voids) {

                HttpURLConnection connection = null;
                //申請的APPID
                //（FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF 為 Guest 帳號，以IP作為API呼叫限制，請替換為註冊的APPID & APPKey）
                String APPID = "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF";
                //申請的APPKey
                String APPKey = "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF";

                //取得當下的UTC時間，Java8有提供時間格式DateTimeFormatter.RFC_1123_DATE_TIME
                //但是格式與C#有一點不同，所以只能自行定義
                String xdate = getServerTime();
                String SignDate = "x-date: " + xdate;


                String Signature = "";
                try {
                    //取得加密簽章
                    Signature = HMAC_SHA1.Signature(SignDate, APPKey);
                } catch (SignatureException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

                System.out.println("Signature :" + Signature);
                String sAuth = "hmac username=\"" + APPID + "\", algorithm=\"hmac-sha1\", headers=\"x-date\", signature=\"" + Signature + "\"";
                System.out.println(sAuth);

                try {
                    //URL url = new URL(XMLUrl);
                    URL url = new URL(APIUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setRequestProperty("Authorization", sAuth);
                    connection.setRequestProperty("x-date", xdate);
                    connection.setRequestProperty("Accept-Encoding", "gzip");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);

                    //將InputStream轉換為Byte
                    InputStream inputStream = connection.getInputStream();
                    ByteArrayOutputStream bao = new ByteArrayOutputStream();
                    byte[] buff = new byte[1024];
                    int bytesRead = 0;
                    while ((bytesRead = inputStream.read(buff)) != -1) {
                        bao.write(buff, 0, bytesRead);
                    }

                    //解開GZIP
                    ByteArrayInputStream bais = new ByteArrayInputStream(bao.toByteArray());
                    GZIPInputStream gzis = new GZIPInputStream(bais);
                    InputStreamReader reader = new InputStreamReader(gzis);
                    BufferedReader in = new BufferedReader(reader);

                    //讀取回傳資料
                    String line, response = "";
                    while ((line = in.readLine()) != null) {
                        response += (line + "\n");
                    }

                    Type RailStationListType = new TypeToken<ArrayList<RailStation>>() {
                    }.getType();
                    Gson gsonReceiver = new Gson();
                    List<RailStation> obj = gsonReceiver.fromJson(response, RailStationListType);
                    System.out.println(response);

                } catch (ProtocolException e) {
                    e.printStackTrace();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                location.setText("AAAAAA");
            }
        }.execute();
    }
}
/*

                HttpURLConnection connection = null;
                //申請的APPID
                //（FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF 為 Guest 帳號，以IP作為API呼叫限制，請替換為註冊的APPID & APPKey）
                String APPID = "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF";
                //申請的APPKey
                String APPKey = "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF";

                //取得當下的UTC時間，Java8有提供時間格式DateTimeFormatter.RFC_1123_DATE_TIME
                //但是格式與C#有一點不同，所以只能自行定義
                String xdate = getServerTime();
                String SignDate = "x-date: " + xdate;


                String Signature = "";
                try {
                    //取得加密簽章
                    Signature = HMAC_SHA1.Signature(SignDate, APPKey);
                } catch (SignatureException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

                System.out.println("Signature :" + Signature);
                String sAuth = "hmac username=\"" + APPID + "\", algorithm=\"hmac-sha1\", headers=\"x-date\", signature=\"" + Signature + "\"";
                System.out.println(sAuth);

                try {
                    URL url = new URL(this.url);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Authorization", sAuth);
                    connection.setRequestProperty("x-date", xdate);
                    connection.setRequestProperty("Accept-Encoding", "gzip");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);

                    //將InputStream轉換為Byte
                    InputStream inputStream = connection.getInputStream();
                    ByteArrayOutputStream bao = new ByteArrayOutputStream();
                    byte[] buff = new byte[1024];
                    int bytesRead = 0;
                    while ((bytesRead = inputStream.read(buff)) != -1) {
                        bao.write(buff, 0, bytesRead);
                    }

                    //解開GZIP
                    ByteArrayInputStream bais = new ByteArrayInputStream(bao.toByteArray());
                    GZIPInputStream gzis = new GZIPInputStream(bais);
                    InputStreamReader reader = new InputStreamReader(gzis);
                    BufferedReader in = new BufferedReader(reader);

                    //讀取回傳資料
                    String line, response = "";
                    while ((line = in.readLine()) != null) {
                        response += (line + "\n");
                    }

                    Type RailStationListType = new TypeToken<ArrayList<RailStation>>() {
                    }.getType();
                    Gson gsonReceiver = new Gson();
                    List<RailStation> obj = gsonReceiver.fromJson(response, RailStationListType);
                    System.out.println(response);

                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
 */