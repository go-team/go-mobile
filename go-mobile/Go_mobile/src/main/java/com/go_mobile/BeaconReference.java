package com.go_mobile;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.EditText;
import android.location.LocationManager;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.RegionBootstrap;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.BeaconConsumer;
import android.location.Location;
import com.loopj.android.http.*;
import android.location.Criteria;
import org.apache.http.Header;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.net.URLEncoder;

import 	android.telephony.TelephonyManager;

/**
 * Created by dyoung on 12/13/13.
 * Modified Apr. 12 2015
 * Adapted for Go-Team demo
 */
public class BeaconReference extends Application implements BootstrapNotifier {
    private static final String TAG = "iBeacon";
    private RegionBootstrap regionBootstrap;
    private BackgroundPowerSaver backgroundPowerSaver;
    private boolean haveDetectedBeaconsSinceBoot = false;
    private MonitoringActivity monitoringActivity = null;
    BeaconManager beaconManager;
    private String _url = "http://gobeaconify.herokuapp.com/";
    private int _port = 80;

    private String mPhoneNumber;

    private List<Beacon> _activeBeacons = new ArrayList<Beacon>();
    private Region bgRegion = new Region("myRangingUniqueId", null, null, null);

    public void onCreate() {
        super.onCreate();
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        TelephonyManager tMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mPhoneNumber = tMgr.getLine1Number();

        Log.d(TAG, "setting up background monitoring");
       Region region = new Region("backgroundRegion",
                null, null, null);
        regionBootstrap = new RegionBootstrap(this, region);

        backgroundPowerSaver = new BackgroundPowerSaver(this);
    }

    @Override
    public void didEnterRegion(Region arg0) {
        range_and_send();
    }


    @Override
    public void didExitRegion(Region region) {
        if (monitoringActivity != null) {
            monitoringActivity.clearDisplay();
            monitoringActivity.logToDisplay("I no longer see any beacons.");
        }

    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {

    }

    private void range_and_send() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            //Range once in order to find UUID
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    Beacon current = beacons.iterator().next();
                    if (monitoringActivity != null) {
                        monitoringActivity.clearDisplay();
                        monitoringActivity.logToDisplay(current.getId1().toString());
                    }
                    send_interaction_report(current);
                    try {
                        beaconManager.stopRangingBeaconsInRegion(bgRegion);
                    } catch (RemoteException e) {
                    }
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(bgRegion);
        } catch (RemoteException e) {
        }

    }

    private void send_interaction_report(Beacon current) {
          AsyncHttpClient client = new AsyncHttpClient(_port);
            try {
                String query = URLEncoder.encode(current.getId1().toString(), "utf-8");
                client.get( _url + "/device/report_interaction/" + query + "/?msg=" +mPhoneNumber, new AsyncHttpResponseHandler() {
                    @Override
                    public void onStart() {
                        // called before request is started
                    }
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                        // called when response HTTP status is "200 OK"
                    }
                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                        // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                    }
                    @Override
                    public void onRetry(int retryNo) {
                        // called when request is retried
                    }
                });

            } catch (UnsupportedEncodingException e) {

            }

    }
    private void sendNotification() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Beacon Reference Application")
                        .setContentText("An beacon is nearby.")
                        .setSmallIcon(R.drawable.ic_launcher);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(new Intent(this, MonitoringActivity.class));
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    public void setMonitoringActivity(MonitoringActivity activity) {
        this.monitoringActivity = activity;
    }

}