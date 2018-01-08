package com.songplaylist.shanakagamage.mobilesonglist;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.Array;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements MediaPlayer.OnCompletionListener,View.OnClickListener{
    private Cursor audioCursor;
    private MediaPlayer player = null;
    private List<Map<String,String>> foundSongList = new ArrayList<>();
    Map<String, String> foundSongRaw = new HashMap<>();
    private ArrayList<String> songList = new ArrayList<String>();
    private List<Map<String,String>> downloadList = new ArrayList<>();
    Map<String, String> downloadRow;
    private List<Map<Integer, String>> timeRawList = new ArrayList<>();
    private List<Map<Integer, String>> timeRawList2 = new ArrayList<>();
    Map<Integer, String> timeRaw;
    private String[] songList1 = new String[6];
    int currentTrack = 0;
    final ArrayList<String> list = new ArrayList<String>();
    private ArrayList<Integer> missingSongs = new ArrayList<>();
    private ListView listview;
    private JSONArray jsonArray;
    private JSONArray tempJson;
    private String key;
    private SharedPreferences pref;
    private SharedPreferences locationPref;
    private SharedPreferences devicePref;
    private SharedPreferences.Editor editor;
    private ArrayList<String> values = new ArrayList<>();
    private ConnectivityManager connectivityManager;
    private boolean musicPlay = false;
    final public static String ONE_TIME = "onetime";
    private boolean isSongsPlaying = false;
    private int songIndexToPlay = 0;
    private String runTime;
    private BroadcastReceiver mReceiver;
    private Button btnSubmit;
    private EditText edtLocation;
    private EditText edtDeviceId;
    private int timRawCount = 0;
    private String lastSongTime;
    private static final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private int requestCount = 0;
    private AlarmManagerBroadcastReceiver alarm;
    private String requestUrl = "http://soundsharingwebapplication20171220103351.azurewebsites.net/api/playlists/GetBydate?date=";
    //private String requestUrl ="http://dev1.vocanic.net/shanaka/downloadmp3/jsontest.php";
    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listviewexampleactivity);
        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        edtLocation = (EditText) findViewById(R.id.edtLocation);
        edtDeviceId = (EditText) findViewById(R.id.edtId);
        btnSubmit.setOnClickListener(this);
        Date date = new Date();
        requestUrl = requestUrl+sdf.format(date);
        pref = getApplicationContext().getSharedPreferences("AppKeyVal", 0);
        deleteSharedPreferences();
        locationPref = getApplicationContext().getSharedPreferences("locationPref", 0);
        devicePref = getApplicationContext().getSharedPreferences("devicePref", 0);
        String locationName = locationPref.getString("locationPref",null);
        String deviceId     = devicePref.getString("devicePref",null);
        if(locationName !=null){
           edtLocation.setText(locationName);
        }
        if(deviceId != null){
           edtDeviceId.setText(deviceId);
        }
        connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            DownloadPlayListJson jsonTask = new DownloadPlayListJson();
            jsonTask.execute();
        }
        else{

        }
    }

    public void checkCorrectTimeToRunSong(boolean isSongsPlaying , String runTime , final int songIndexToPlay){
        SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss");
        Date currentDate = new Date();
        System.out.println("currentDate : "+currentDate+" End time "+runTime);
        try{
            String startTime = currentDate.getHours()+":"+currentDate.getMinutes()+":"+currentDate.getSeconds();
            String endTime   = runTime;
            Date date1 = df.parse(startTime);
            Date date2 = df.parse(endTime);
            System.out.println("date1 : "+date1+" date2"+date2);
            String[] startTimeSepareted = startTime.split(":");
            String[] endTimeSeperated   = endTime.split(":");
            int startTimeH =    Integer.parseInt(startTimeSepareted[0]);
            int startTimeM =    Integer.parseInt(startTimeSepareted[1]);
            int startTimeS =    Integer.parseInt(startTimeSepareted[2]);
            int endTimeH   =    Integer.parseInt(endTimeSeperated[0]);
            int endTimeM   =    Integer.parseInt(endTimeSeperated[1]);
            int endTimeS   =    Integer.parseInt(endTimeSeperated[2]);
            listview = (ListView) findViewById(R.id.listview);
            final StableArrayAdapter adapter = new StableArrayAdapter(this,R.layout.arryalisttemplate, list);
            listview.setAdapter(adapter);
                if (date2.after(date1)) {
                    long diff = date2.getTime() - date1.getTime();
                    System.out.println("wait for this time please: " + diff);
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            currentTrack = songIndexToPlay;
                                    getSongList(currentTrack);
                            //Do something after 100ms
                        }
                    }, diff);
                    System.out.println("Run time is grater that start time");
                } else {

                    System.out.println("Start time is grater than run time timeRawList2.size() "+timeRawList2.size()+songList.size());
                    for (int i = 0; i < timeRawList2.size(); i++) {
                        Date date3 = df.parse(startTime);
                        Date date4 = df.parse(timeRawList2.get(i).get(i).toString());
                        long diff = date4.getTime()-date3.getTime();
                        final int nextSongIndex = i;
                        currentTrack = nextSongIndex;
                       if (date4.after(date3)) {
                           i = timeRawList2.size();
                           System.out.println("def time: "+diff+" execute time "+date4);
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    getSongList(currentTrack);
                                    //Do something after 100ms
                                }
                            }, (date4.getTime() - date3.getTime()));
                            System.out.println("diffrence of time: " + diff+" song index is: "+nextSongIndex);
                        }

                    }
                }
                System.out.println("song list size: "+songList.size()+" list size "+list.size()+" timeRawList.size() "+timeRawList.size());
        }catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void startRepeatingTimer() {
        Context context = this.getApplicationContext();
        SetAlarm(context);
    }

    public boolean storeSharedPreferences(String keyValue){
        editor = pref.edit();
        editor.putString("lastTime", keyValue);
        editor.commit();
        return true;
    }

    public String getSharedPreferences(){
        return (pref.getString("lastTime", null));
    }

    public boolean deleteSharedPreferences(){
        editor = pref.edit();
        editor.remove("lastTime");
        editor.commit();
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(this.mReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startTimer();

        /*IntentFilter intentFilter = new IntentFilter(
                "android.intent.action.MAIN");
        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                StringBuilder msgStr = new StringBuilder();

                if(extras != null && extras.getBoolean(ONE_TIME, Boolean.FALSE)){
                    //Make sure this intent has been sent by the one-time timer button.
                    msgStr.append("One time Timer : ");
                }
                Format formatter = new SimpleDateFormat("hh:mm:ss a");
                msgStr.append(formatter.format(new Date()));
                GetPlaylistKey jsonTask = new GetPlaylistKey();
                jsonTask.execute();
                Toast.makeText(context, "shana", Toast.LENGTH_LONG).show();

            }
        };
        //registering our receiver
        this.registerReceiver(mReceiver, intentFilter);*/
    }

    public void SetAlarm(Context context) {
        //Toast.makeText(context, "shana", Toast.LENGTH_LONG).show();
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.putExtra(ONE_TIME, Boolean.FALSE);
        //Intent intent = new Intent(context,MainActivity.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        //After after 5 seconds
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 30 , pi);
    }

    public void startTimer() {
        timer = new Timer();
        initializeTimerTask();
        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 0, 60000); //
    }

    public void stoptimertask(View v) {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
                            int currentMin = new Date().getMinutes();
                            System.out.println("System time is: "+currentMin);
                            if(currentMin == 45){
                                if(timeRawList2.isEmpty()){
                                    lastSongTime = null;
                                }else{
                                    lastSongTime = timeRawList2.get((timeRawList2.size()-1)).get((timeRawList2.size()-1));
                                }
                                downloadJson();
                            }
                        }
                        else{

                        }
                    }
                });
            }
        };
    }

    public void downloadJson(){
        DownloadPlayListJson jsonTask = new DownloadPlayListJson();
        jsonTask.execute();
        /*GetPlaylistKey jsonTask = new GetPlaylistKey();
        jsonTask.execute();*/
    }

    public void processControll(){
        if(checkSongExist()){
            System.out.println("isSongsPlaying : "+isSongsPlaying);
            if(!isSongsPlaying){
                isSongsPlaying = true;
                checkCorrectTimeToRunSong(isSongsPlaying , runTime,0);
            }else{
                System.out.println("isSongsPlaying is : "+isSongsPlaying);
                System.out.println("Song view List: "+list.toString());
                System.out.println("Song Play List: "+songList.toString());
                listview = (ListView) findViewById(R.id.listview);
                final StableArrayAdapter adapter = new StableArrayAdapter(this,R.layout.arryalisttemplate, list);
                listview.setAdapter(adapter);
            }

        }else{
            DownloadMusic task = new DownloadMusic();
            task.execute(downloadList);
            System.out.println("Some songs not found..!");
        }
    }

    private boolean checkSongExist(){
        requestCount++;
        downloadList.clear();
        ArrayList<String> valuesOfFounded = new ArrayList<String>();
        audioCursor = this.managedQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        startManagingCursor(audioCursor);
        //songList = new String[audioCursor.getCount()];
        Log.d("Array", "Array "+MediaStore.Audio.Media.TITLE);
        int i = 0;
        System.out.println("Value Size is: "+values.size());
        for(int z = 0 ; z < values.size() ; z++){
            System.out.println("Values: "+values.get(z).toString());
        }
        for(audioCursor.moveToFirst(); !audioCursor.isAfterLast(); audioCursor.moveToNext()) {
            //11-16 14:41:53.756 698-698/? I/System.out: [_id, _data, _display_name, _size, mime_type, date_added, is_drm, date_modified, title, title_key, duration, artist_id, composer, album_id, track, year, is_ringtone, is_music, is_alarm, is_notification, is_podcast, bookmark, album_artist, title_pinyin_key, title_bopomo_key, title_general_key, artist_id:1, artist_key, artist, artist_pinyin_key, artist_bopomo_key, artist_general_key, album_id:1, album_key, album, album_pinyin_key, album_bopomo_key, album_general_key]
            // System.out.println(audioCursor.getString(audioCursor.getColumnIndex("title"))+" Display name "+audioCursor.getString(audioCursor.getColumnIndex("_display_name"))+" path if audiot file "+audioCursor.getString(audioCursor.getColumnIndex("_data")));
            //list.add(audioCursor.getString(audioCursor.getColumnIndex("title"))+" Display name "+audioCursor.getString(audioCursor.getColumnIndex("_display_name"))+" path if audiot file "+audioCursor.getString(audioCursor.getColumnIndex("_data")));
            //songList.add(audioCursor.getString(audioCursor.getColumnIndex("_data")));
            //i++;
            if((audioCursor.getString(audioCursor.getColumnIndex("mime_type"))).compareTo("audio/mpeg") == 0){
                if(!foundSongRaw.containsKey((audioCursor.getString(audioCursor.getColumnIndex("title"))))){
                    foundSongRaw.put(audioCursor.getString(audioCursor.getColumnIndex("title")), audioCursor.getString(audioCursor.getColumnIndex("_data")));
                }
                System.out.println("All song here: "+audioCursor.getString(audioCursor.getColumnIndex("title")));
                if(values.contains(audioCursor.getString(audioCursor.getColumnIndex("title")))){
                   /* if(list.contains(audioCursor.getString(audioCursor.getColumnIndex("title")))){
                        System.out.println("Values here list: "+audioCursor.getString(audioCursor.getColumnIndex("title")));
                    }else{*/
                        for(int j =0; j <songList.size();j++){
                            System.out.println("Song list array data : "+songList.get(j));
                        }
                        for(int j =0; j <list.size();j++){
                            System.out.println("list array data : "+list.get(j));
                        }
                        System.out.println("Value is exist in list array: "+audioCursor.getString(audioCursor.getColumnIndex("title")));
                        //list.add(audioCursor.getString(audioCursor.getColumnIndex("title")));
                        //songList.add(audioCursor.getString(audioCursor.getColumnIndex("_data")));
                        valuesOfFounded.add(audioCursor.getString(audioCursor.getColumnIndex("title")).toString());
                        i++;
                    //}
                }else{
                }
            }
        }
        if(valuesOfFounded.size() >= values.size() || requestCount == 4){
            for(int j=0;j<values.size();j++){
                if(valuesOfFounded.contains(values.get(j).toString())){
                    list.add(values.get(j).toString());
                    songList.add(foundSongRaw.get(values.get(j).toString()));
                    System.out.println("Could find this index of song  timeRawList2.get(i).get(i) "+values.get(j).toString()+" song list "+foundSongRaw.get(values.get(j).toString()));

                }
                else{
                    missingSongs.add(j);
                    System.out.println("Could not find this index of song "+values.get(j).toString()+" song list "+foundSongRaw.get(values.get(j).toString()));
                }
            }

            System.out.println("list size: "+list.size()+" value size "+values.size()+" timeRawList size "+timeRawList.size()+" missingSongs "+missingSongs.size());
            int p = 0;
            for(int l =0;l<values.size();l++){
 /*               if(list.get(l).equals(values.get(l))){

                }*/
                if(missingSongs.contains(l)){}
                else{
                    System.out.println("adding time: "+timeRawList.get(l).get(l));
                    String tempTime = timeRawList.get(l).get(l);
                    timeRaw = new HashMap<>();
                    timeRaw.put(p,tempTime);
                    timeRawList2.add(timeRaw);
                    p++;
                }
            }
            timeRawList = timeRawList2;
            for(int k = 0 ;k<timeRawList2.size();k++){
                System.out.println("timeRawList2 : "+timeRawList2.get(k).get(k));
            }
            return true;
        }else{
            for(int j=0; j < values.size(); j++){
                if(valuesOfFounded.indexOf(values.get(j)) == -1){
                    //System.out.println("Index of array : "+values.get(j)+"  "+valuesOfFounded.indexOf(values.get(j).toString())+" Size of valuesOfFounded "+valuesOfFounded.size());
                    try {
                        for (int k = 0; k < jsonArray.length(); k++) {
                            // key = jsonArray.getJSONObject(i).getString("Id");
                            tempJson = jsonArray.getJSONObject(k).getJSONArray("PlaylistSong");
                            for (int l = 0; l < tempJson.length(); l++) {
                                String tempTitle = tempJson.getJSONObject(l).getString("Title").toString();
                                //System.out.println("This song is not in downloads: "+tempTitle);
                                if((values.get(j).toString()).equals(tempTitle)){
                                    System.out.println("song: "+tempJson.getJSONObject(l).getString("DownloadUrl"));
                                    downloadRow = new HashMap<>();
                                    downloadRow.put("Title", tempJson.getJSONObject(l).getString("Title").toString());
                                    downloadRow.put("Url", (tempJson.getJSONObject(l).getString("DownloadUrl")).replace(" ", "%20"));
                                    downloadList.add(downloadRow);
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }
    }

    private void getSongList(int songIndex){
       // System.out.println("This is song list...!");
        if(musicPlay == false){
            String filePath = songList.get(songIndex);
            filePath.replace("file:///", "/");
           //System.out.print("next file path: "+filePath.toString());
            player = MediaPlayer.create(this, Uri.parse(filePath));
            player.setOnCompletionListener(this);
            player.start();
            musicPlay = true;
        }

       /* player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });*/
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        System.out.println("Song completed");
        currentTrack++;
        if(currentTrack < songList.size()) {
            System.out.print("Song end shana! " + currentTrack + " song list size " + songList.size() + " song time is: " + timeRawList2.get(currentTrack).get(currentTrack));

            SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss");
            Date currentDate = new Date();
            System.out.println("currentDate : " + currentDate + " End time " + runTime);
            String startTime = currentDate.getHours() + ":" + currentDate.getMinutes() + ":" + currentDate.getSeconds();
            Date date3 = null;
            Date date4 = null;
            try {
                date3 = df.parse(startTime);
                date4 = df.parse(timeRawList2.get(currentTrack).get(currentTrack));
                System.out.println("Song should strt time: " + date4 + " Song system time " + date3);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            long diff = date4.getTime() - date3.getTime();
            if (date4.after(date3)) {
                System.out.println("def time: " + diff + " execute time " + date4);
                final Handler handler = new Handler();
                final Date finalDate = date4;
                final MediaPlayer finalMediaPlayer = mediaPlayer;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Song should start now " + finalDate);
                        delayPlayTime(finalMediaPlayer, currentTrack);
                    }
                }, (date4.getTime() - date3.getTime()));
                System.out.println("diffrence of time: " + diff + " song index is: " + currentTrack);
            } else {
                String filePath = songList.get(currentTrack);
                filePath.replace("file:///", "/");
                System.out.print("next file path: " + filePath.toString());
                mediaPlayer = MediaPlayer.create(this, Uri.parse(filePath));
                mediaPlayer.setOnCompletionListener(this);
                mediaPlayer.start();
                musicPlay = true;
            }
        }else{
                //timeRawList.clear();
                currentTrack =0;
                System.out.println("Media player stop");
                list.clear();
                songList.clear();
                mediaPlayer.reset();
                mediaPlayer.release();
                musicPlay = false;
                isSongsPlaying = false;
            }
    }

    public void delayPlayTime(MediaPlayer mediaPlayer ,int currentTrack){
        if (currentTrack < songList.size()) {
            String filePath = songList.get(currentTrack);
            filePath.replace("file:///", "/");
            System.out.print("next file path: "+filePath.toString());
            mediaPlayer = MediaPlayer.create(this, Uri.parse(filePath));
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.start();
            musicPlay = true;
        }else{
            //timeRawList.clear();
            currentTrack =0;
            System.out.println("Media player stop");
            list.clear();
            songList.clear();
            mediaPlayer.reset();
            mediaPlayer.release();
            musicPlay = false;
            isSongsPlaying = false;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btnSubmit:
                String locationName = locationPref.getString("locationPref",null);
                String DeviceId     = devicePref.getString("devicePref",null);
                Toast.makeText(MainActivity.this,"Location "+edtLocation.getText().toString()+" and device Id"+edtDeviceId.getText().toString()+" saved successfully!",Toast.LENGTH_SHORT).show();
                if(locationName == null){
                        editor = locationPref.edit();
                        editor.putString("locationPref", edtLocation.getText().toString());
                        editor.commit();
                }
                else {
                    editor = locationPref.edit();
                    editor.remove("locationPref");
                    editor.commit();
                    editor = locationPref.edit();
                    editor.putString("locationPref", edtLocation.getText().toString());
                    editor.commit();
                }
                if(DeviceId == null){
                editor = devicePref.edit();
                editor.putString("devicePref", edtDeviceId.getText().toString());
                editor.commit();
                }
                else{
                    editor = devicePref.edit();
                    editor.remove("devicePref");
                    editor.commit();
                    editor = devicePref.edit();
                    editor.putString("devicePref", edtDeviceId.getText().toString());
                    editor.commit();

                }
        }
        InputMethodManager imm = (InputMethodManager)
        getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(
                edtDeviceId.getWindowToken(), 0);

        getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(
                edtLocation.getWindowToken(), 0);
    }

    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }

    private class GetPlaylistKey extends  AsyncTask<Void,Void,Boolean>{

        @Override
        protected Boolean doInBackground(Void... voids) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(requestUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                StringBuilder sb = new StringBuilder();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                    sb.append(line.trim());
                    //Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)

                }
                String response = sb.toString();
                JSONObject json;
                try {
                    json = new JSONObject(response);

                    // key = json.getString("Id");
                    jsonArray = json.getJSONArray("Playlists");
                    for (int i = 0; i < jsonArray.length(); i++) {
                       /* Boolean inActive = jsonArray.getJSONObject(i).getBoolean("IsActive");
                        if(inActive){
                            key = jsonArray.getJSONObject(i).getString("Id");
                        }*/
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
                DownloadPlayListJson jsonTask = new DownloadPlayListJson();
                jsonTask.execute();
        }
    }

    private class DownloadPlayListJson extends AsyncTask<Void,Void,Boolean>{

        @Override
        protected Boolean doInBackground(Void... voids) {
            requestCount = 0;
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(requestUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                StringBuilder sb = new StringBuilder();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                    sb.append(line.trim());
                    Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)

                }
                //sb.deleteCharAt(0);
                //sb.deleteCharAt(sb.length()-1);
                String response = sb.toString();
                JSONObject json;
                try {
                    json = new JSONObject(response);
                    // System.out.println("Response: "+json.toString());



                    // key = json.getString("Id");
                    jsonArray = json.getJSONArray("Playlists");
                    boolean foundDate = false;
                    values.clear();
                    //pref        =  getApplicationContext().getSharedPreferences("lastTime", 0);
                    //String lastSongTimePref = pref.getString("lastTime",null);
                    System.out.println("Last from app song time: "+lastSongTime);
                    for (int i = 0; i < jsonArray.length(); i++) {
                            tempJson = jsonArray.getJSONObject(i).getJSONArray("PlaylistSong");
                            //System.out.println("tempJson.getJSONObject(j) :"+tempJson.toString());
                            for (int j = 0; j < tempJson.length(); j++) {
                                /*System.out.println(" Title of song is : "+
                                        tempJson.getJSONObject(j).getString("Title")+" Time of song: "+
                                        tempJson.getJSONObject(j).getString("CumilativeDuration"));*/
                                if(i == 0 && j == 0){
                                    runTime = (tempJson.getJSONObject(j).get("CumilativeDuration").toString());
                                }
                                //System.out.println("check lastSongTime exist: "+lastSongTime);
                                if(lastSongTime == null){
                                    //System.out.println("lasSong time is null: "+lastSongTime);
                                    timeRaw = new HashMap<>();
                                    timeRaw.put(timRawCount,(tempJson.getJSONObject(j).getString("CumilativeDuration")).toString());
                                    timeRawList.add(timeRaw);
                                    //System.out.println("lastSongTime "+lastSongTime+" CumilativeDuration "+tempJson.getJSONObject(j).getString("CumilativeDuration"));
                                    //System.out.println(" Title of song is : "+tempJson.getJSONObject(j).getString("Title"));
                                    values.add(tempJson.getJSONObject(j).getString("Title"));
                                }else{
                                    //System.out.println("check lastSongTime is not null: "+lastSongTime+" "+(tempJson.getJSONObject(j).getString("CumilativeDuration")));
                                    if(lastSongTime.equals(tempJson.getJSONObject(j).getString("CumilativeDuration"))){
                                        //System.out.println("More songs found!");
                                        foundDate = true;
                                    }
                                    if(foundDate && ((j+1) < tempJson.length())){
                                        timeRaw = new HashMap<>();
                                        timeRaw.put(timRawCount,(tempJson.getJSONObject(j+1).getString("CumilativeDuration")).toString());
                                        System.out.println("lastSongTime in next json"+lastSongTime+" CumilativeDuration "+tempJson.getJSONObject(j+1).getString("CumilativeDuration"));
                                        timeRawList.add(timeRaw);
                                        //System.out.println(" Title of more song is  in next json : "+tempJson.getJSONObject(j).getString("Title"));
                                        values.add(tempJson.getJSONObject(j+1).getString("Title"));
                                    }
                                }
                                timRawCount++;
                            }
                    }
                   lastSongTime = timeRawList.get(timeRawList.size()-1).get(timeRawList.size()-1);
                   /* if(lastSongTimePref == null){
                        storeSharedPreferences(lastSongTime);
                    }else{
                        deleteSharedPreferences();
                        storeSharedPreferences(lastSongTime);
                    }
*/
                    timRawCount=0;
                    System.out.println("last song time: "+lastSongTime+" "+values.size()+" "+timeRawList.size());


                } catch (JSONException e) {
                    e.printStackTrace();
                }


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
           /* String keyValue = getSharedPreferences();
            System.out.println("keyValue :"+keyValue+" key: "+key);
            if(keyValue == null){
                System.out.println("no share preferences!");
                storeSharedPreferences(key);
            }else if(keyValue.equals(key)){
                System.out.println("same shared preferences");
            }else{
                deleteSharedPreferences();
                storeSharedPreferences(key);
                System.out.println("new share preferences!");
            }*/
            processControll();
        }
    }

    private class DownloadMusic extends AsyncTask<List<Map<String,String>>,Void,Boolean>{

        @Override
        protected Boolean doInBackground(List<Map<String, String>>[] lists) {
            // System.out.println("lists[0].size() "+lists[0].toString());
            // System.out.println("lists[0].size() "+lists[0].get(0).get("Title")+" lists[0].get(i).get(\"Title\") "+lists[0].get(1).get("Title"));
            try{
                Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
                Boolean isSDSupportedDevice = Environment.isExternalStorageRemovable();
//this for new devices
               // File cacheDir = new File(android.os.Environment.getExternalStorageDirectory(),"Download");

//this is for old devices

                File cacheDir;
                if(isSDSupportedDevice && isSDPresent){
                    // yes SD-card is present
                     cacheDir=new File(android.os.Environment.getExternalStorageDirectory(),"Download");
                }
                else{
                    // Sorry
                     cacheDir=new File(getApplicationContext().getFilesDir(),"");
                }
                if(!cacheDir.exists())
                    cacheDir.mkdirs();
                for (int i=0; i<lists[0].size();i++){
                    String songName = lists[0].get(i).get("Title");
                    //System.out.println("lists[0].size() "+lists[0].get(i).get("Title"));
                    File file = new File(cacheDir,songName+".mp3");
                    URL url = new URL(lists[0].get(i).get("Url"));
                    InputStream input = new BufferedInputStream(url.openStream());
                    OutputStream output = new FileOutputStream(file);

                    byte data[] = new byte[1024];
                    long total = 0;
                    int count=0;
                    while ((count = input.read(data)) != -1) {
                        total++;
                        //Log.e("while","A"+total);

                        output.write(data, 0, count);
                    }
                    Log.e("downloaded","file was downloaded: "+songName);
                    output.flush();
                    output.close();
                    input.close();
                    MediaScannerConnection.scanFile(
                            getApplicationContext(),
                            new String[]{file.getAbsolutePath()},
                            null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                @Override
                                public void onScanCompleted(String path, Uri uri) {
                                    // only at this point are files in MediaStore
                                }
                            });
                    System.out.println("This is AsyncTask class for download! and song title is: "+lists[0].get(i).get("Title"));
                }
            }catch(Exception e){
                System.out.println("File can not download!");
                e.printStackTrace();
            }
            return true;
        }

        //AsyncTask<ArrayList<String>,Void,Boolean>
       /* @Override
        protected Boolean doInBackground(ArrayList<String>[] arrayLists) {
            for (int i=0; i<arrayLists[0].size();i++){
                System.out.println("This is AsyncTask class for download! and song title is: "+arrayLists[0].get(i));
            }
            return true;
        }
*/
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            processControll();
            //System.out.println("Post was executed done shana...!");
        }
    }

}
