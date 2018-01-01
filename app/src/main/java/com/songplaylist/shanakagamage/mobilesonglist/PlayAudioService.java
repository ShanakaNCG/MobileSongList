package com.songplaylist.shanakagamage.mobilesonglist;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by shanaka.gamage on 12/5/2017.
 */

public class PlayAudioService extends Service{

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        System.out.println("run activity service");
        Intent playAudioActivity = new Intent(this, BootUpReceiver.class);
        playAudioActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(playAudioActivity);
    }
}
