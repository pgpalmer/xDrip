package com.eveningoutpost.dexdrip.Services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.CalibrationSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.MongoSendTask;
import com.eveningoutpost.dexdrip.UtilityModels.NightscoutUploader;
import com.eveningoutpost.dexdrip.UtilityModels.RestCalls;
import com.eveningoutpost.dexdrip.UtilityModels.SensorSendQueue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SyncService extends IntentService {
    int mStartMode;
    private Context mContext;
    private Boolean enableRESTUpload;
    private Boolean enableMongoUpload;
    private SharedPreferences prefs;

    public SyncService() {
        super("SyncService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.w("SYNC SERVICE:", "STARTING INTENT SERVICE");
        setRetryTimer();
        attemptSend();
    }

    public void attemptSend() {
        mContext = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        enableRESTUpload = prefs.getBoolean("cloud_storage_api_enable", false);
        enableMongoUpload = prefs.getBoolean("cloud_storage_mongodb_enable", false);

        if (enableRESTUpload || enableMongoUpload) { syncToMogoDb(); }

        if (false) {
            for (SensorSendQueue job : SensorSendQueue.queue()) {
                RestCalls.sendSensor(job);
            }
            for (CalibrationSendQueue job : CalibrationSendQueue.queue()) {
                RestCalls.sendCalibration(job);
            }
            for (BgSendQueue job : BgSendQueue.queue()) {
                RestCalls.sendBgReading(job);
            }
        }
        setRetryTimer();
    }

    public void setRetryTimer() {
        Calendar calendar = Calendar.getInstance();
        AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarm.set(alarm.RTC_WAKEUP, calendar.getTimeInMillis() + (1000 * 30 * 5), PendingIntent.getService(this, 0, new Intent(this, SyncService.class), 0));
    }

    public void syncToMogoDb() {
        MongoSendTask task = new MongoSendTask(getApplicationContext());
        task.execute();
    }
}
