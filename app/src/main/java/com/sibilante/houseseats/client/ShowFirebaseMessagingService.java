package com.sibilante.houseseats.client;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.sibilante.houseseats.client.model.Show;

public class ShowFirebaseMessagingService extends FirebaseMessagingService {

    public static final String TAG = "ShowFirebaseMessagingService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Show show = new Show (remoteMessage.getData().get("id"), remoteMessage.getData().get("name"));
        sendNotification(show);
        Log.d(TAG, "Dados: " + remoteMessage.getData().toString());

        Intent intent = new Intent();
        intent.setAction("houseseats.NEW_SHOW");
        intent.putExtra("showID", remoteMessage.getData().get("id"));
        intent.putExtra("showName", remoteMessage.getData().get("name"));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }

    @Override
    public void onNewToken(String token) {
        FirebaseMessaging.getInstance().subscribeToTopic("shows").addOnCompleteListener(new OnCompleteListener<Void>() {

            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (!task.isSuccessful()) {
                    Log.d(TAG, "Fail to subscribe to the topic");
                } else {
                    Log.d(TAG, "Success in subscribe to the topic");
                }
            }
        });
        Log.d(TAG, "Refreshed token: " + token);

    }

    // Post a notification indicating whether a new show was found.
    private void sendNotification(Show show) {
        NotificationManager notificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        // Pending implicit intent to view url
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://lv.houseseats.com/member/tickets/view/?showid=" + show.getId()));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // https://developer.android.com/training/notify-user/channels.html#java
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("my_channel_01", "Mais merda", importance);

            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, "my_channel_01")
                .setSmallIcon(android.R.drawable.star_big_on)
                .setContentTitle(show.getName());

        notification.setContentIntent(pendingIntent);
        notification.setAutoCancel(true);
        notification.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
        notificationManager.notify(String.valueOf(System.currentTimeMillis()), 0, notification.build());
    }
}
