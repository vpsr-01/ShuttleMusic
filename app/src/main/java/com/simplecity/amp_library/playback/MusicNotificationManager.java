package com.simplecity.amp_library.playback;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.afollestad.aesthetic.Aesthetic;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.glide.utils.GlideUtils;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.activities.MainActivity;
import com.simplecity.amp_library.utils.PlaceholderProvider;

import timber.log.Timber;

public class MusicNotificationManager extends BroadcastReceiver {

    private NotificationCompat.Action prevAction;
    private NotificationCompat.Action skipAction;
    private NotificationCompat.Action playAction;
    private NotificationCompat.Action pauseAction;

    interface Action {
        String PAUSE = "com.simplecity.shuttle.notification_action.pause";
        String PLAY = "com.simplecity.shuttle.notification_action.play";
        String PREV = "com.simplecity.shuttle.notification_action.prev";
        String NEXT = "com.simplecity.shuttle.notification_action.next";
        String STOP = "com.simplecity.shuttle.notification_action.stop";
        String STOP_CASTING = "com.simplecity.shuttle.notification_action.stop_cast";
    }

    private static final int NOTIFICATION_ID = 212;
    private static final int REQUEST_CODE = 100;
    private static final String CHANNEL_ID = "com.simplecity.shuttle.MUSIC_CHANNEL_ID";

    private final NotificationManager notificationManager;

    private MusicService service;

    private MediaSessionCompat.Token sessionToken;
    private MediaControllerCompat controller;
    private MediaControllerCompat.TransportControls transportControls;

    private PlaybackStateCompat playbackState;
    private MediaMetadataCompat metadata;

    private boolean started = false;

    private final PendingIntent contentIntent;
    private final PendingIntent playIntent;
    private final PendingIntent pauseIntent;
    private final PendingIntent previousIntent;
    private final PendingIntent skipIntent;
    private final PendingIntent stopIntent;
    private final PendingIntent stopCastIntent;

    @Nullable
    private SimpleTarget<Bitmap> glideTarget;

    public MusicNotificationManager(MusicService service) throws RemoteException {

        this.service = service;
        updateSessionToken();

        notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

        String pkg = service.getPackageName();

        contentIntent = PendingIntent.getActivity(service, REQUEST_CODE, new Intent(service, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT);
        pauseIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(Action.PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        playIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(Action.PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        previousIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(Action.PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        skipIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(Action.NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        stopIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(Action.STOP).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        stopCastIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(Action.STOP_CASTING).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);

        prevAction = new NotificationCompat.Action(R.drawable.ic_skip_previous_24dp, service.getString(R.string.btn_prev), previousIntent);
        skipAction = new NotificationCompat.Action(R.drawable.ic_skip_next_24dp, service.getString(R.string.btn_skip), skipIntent);
        playAction = new NotificationCompat.Action(R.drawable.ic_play_24dp, service.getString(R.string.btn_play), playIntent);
        pauseAction = new NotificationCompat.Action(R.drawable.ic_pause_24dp, service.getString(R.string.btn_pause), pauseIntent);

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        notificationManager.cancelAll();
    }

    public void startNotification() {
        Timber.d("startNotification() called. started: %s", started);
        if (!started) {
            metadata = controller.getMetadata();
            playbackState = controller.getPlaybackState();

            // The notification must be updated after setting started to true
            Notification notification = createNotification();
            if (notification != null) {
                started = true;
                controller.registerCallback(mediaControllerCallback);
                IntentFilter filter = new IntentFilter();
                filter.addAction(Action.NEXT);
                filter.addAction(Action.PAUSE);
                filter.addAction(Action.PLAY);
                filter.addAction(Action.PREV);
                filter.addAction(Action.STOP_CASTING);
                service.registerReceiver(this, filter);
                service.startForeground(NOTIFICATION_ID, notification);
            }
        }
    }

    public void stopNotification() {
        Timber.d("stopNotification() called. started: %s", started);
        if (started) {
            started = false;
            controller.unregisterCallback(mediaControllerCallback);
            try {
                notificationManager.cancel(NOTIFICATION_ID);
                service.unregisterReceiver(this);
            } catch (IllegalArgumentException ex) {
                // ignore if the receiver is not registered.
            }
            service.stopForeground(true);
        }
    }

    @Nullable
    public Notification createNotification() {
        Timber.d("updateNotificationMetadata() called. metadata: %s", metadata);
        if (metadata == null || playbackState == null) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(service, CHANNEL_ID);

        MediaDescriptionCompat description = metadata.getDescription();

        addActions(notificationBuilder);
        notificationBuilder.setStyle(
                new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(stopIntent)
                        .setMediaSession(sessionToken))
                .setDeleteIntent(stopIntent)
                .setColor(Aesthetic.get(service).colorPrimary().blockingFirst())
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentIntent)
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setLargeIcon(GlideUtils.drawableToBitmap(PlaceholderProvider.getInstance().getPlaceHolderDrawable(
                        description.getTitle() == null ? "" : description.getTitle().toString(), false)));

        setNotificationPlaybackState(notificationBuilder);

        updateArtwork(notificationBuilder);

        return notificationBuilder.build();
    }

    private void updateArtwork(NotificationCompat.Builder builder) {
        Song currentSong = MusicUtils.getCurrentSong();
        if (currentSong != null) {
            if (glideTarget != null) {
                Glide.clear(glideTarget);
            }
            glideTarget = new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                    if (metadata != null && metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID).equals(String.valueOf(currentSong.id))) {
                        builder.setLargeIcon(resource);
                        addActions(builder);
                        notificationManager.notify(NOTIFICATION_ID, builder.build());
                    }
                }
            };
            Glide.with(service)
                    .load(currentSong)
                    .asBitmap()
                    .into(glideTarget);
        }
    }

    private void addActions(NotificationCompat.Builder notificationBuilder) {

        boolean isPlaying = playbackState.getState() == PlaybackStateCompat.STATE_PLAYING;

        if (!notificationBuilder.mActions.contains(prevAction)) {
            notificationBuilder.addAction(prevAction);
        }

        if (isPlaying) {
            if (!notificationBuilder.mActions.contains(pauseAction)) {
                notificationBuilder.mActions.remove(playAction);
                notificationBuilder.addAction(pauseAction);
            }
        } else {
            if (!notificationBuilder.mActions.contains(playAction)) {
                notificationBuilder.mActions.remove(pauseAction);
                notificationBuilder.addAction(playAction);
            }
        }

        if (!notificationBuilder.mActions.contains(skipAction)) {
            notificationBuilder.addAction(skipAction);
        }

//                .addAction(isFavorite ? R.drawable.ic_favorite_24dp_scaled : R.drawable.ic_favorite_border_24dp_scaled,
//                        service.getString(R.string.fav_add),
//                        MusicService.retrievePlaybackAction(service, Constants.ServiceCommand.TOGGLE_FAVORITE))
    }

    void setNotificationPlaybackState(NotificationCompat.Builder notificationBuilder) {
        Timber.d("setNotificationPlaybackState() called. mPlaybackState: %s", playbackState);
        if (playbackState == null || !started) {
            Timber.i("Playbackstate: " + playbackState + " started: " + started);
            Timber.d("setNotificationPlaybackState() called. cancelling notification!");
            Timber.i("stopForeground() removeNotification: true");
            service.stopForeground(true);
            return;
        }

        // Make sure that the notification can be dismissed by the user when we are not playing:
        notificationBuilder.setOngoing(playbackState.getState() == PlaybackStateCompat.STATE_PLAYING);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action != null) {
            Timber.d("Received intent with action: %s", action);
            switch (action) {
                case Action.PAUSE:
                    transportControls.pause();
                    break;
                case Action.PLAY:
                    transportControls.play();
                    break;
                case Action.NEXT:
                    transportControls.skipToNext();
                    break;
                case Action.PREV:
                    transportControls.skipToPrevious();
                    break;
                case Action.STOP_CASTING:
                    // Todo:
                    break;
                default:
                    Timber.w("Unknown intent ignored. Action: %s", action);
            }
        }
    }

    /**
     * Update the state based on a change on the session token. Called either when
     * we are running for the first time or when the media session owner has destroyed the session
     * (see {@link android.media.session.MediaController.Callback#onSessionDestroyed()})
     */
    private void updateSessionToken() throws RemoteException {
        MediaSessionCompat.Token freshToken = service.getSessionToken();
        if (sessionToken == null && freshToken != null ||
                sessionToken != null && !sessionToken.equals(freshToken)) {
            if (controller != null) {
                controller.unregisterCallback(mediaControllerCallback);
            }
            sessionToken = freshToken;
            if (sessionToken != null) {
                controller = new MediaControllerCompat(service, sessionToken);
                transportControls = controller.getTransportControls();
                if (started) {
                    controller.registerCallback(mediaControllerCallback);
                }
            }
        }
    }

    private final MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            playbackState = state;
            Timber.d("Received new playback state: %s", state);
            if (state.getState() == PlaybackStateCompat.STATE_STOPPED || state.getState() == PlaybackStateCompat.STATE_NONE) {
                stopNotification();
            } else {
                Notification notification = createNotification();
                if (notification != null) {
                    notificationManager.notify(NOTIFICATION_ID, notification);
                }
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            MusicNotificationManager.this.metadata = metadata;
            Timber.d("Received new metadata: %s", metadata);
            Notification notification = createNotification();
            if (notification != null) {
                notificationManager.notify(NOTIFICATION_ID, notification);
            }
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            Timber.d("Session was destroyed, resetting to the new session token");
            try {
                updateSessionToken();
            } catch (RemoteException e) {
                Timber.e(e, "could not connect media controller");
            }
        }
    };

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel existingNotificationChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
        if (existingNotificationChannel == null) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID,
                    service.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }
}