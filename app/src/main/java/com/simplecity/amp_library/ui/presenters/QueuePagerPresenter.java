package com.simplecity.amp_library.ui.presenters;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.bumptech.glide.RequestManager;
import com.cantrowitz.rxbroadcast.RxBroadcast;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.playback.old.Constants;
import com.simplecity.amp_library.playback.events.MetadataChangedEvent;
import com.simplecity.amp_library.playback.events.MusicEventRelay;
import com.simplecity.amp_library.playback.events.QueueChangedEvent;
import com.simplecity.amp_library.playback.events.QueuePositionChangedEvent;
import com.simplecity.amp_library.ui.modelviews.QueuePagerItemView;
import com.simplecity.amp_library.ui.views.QueuePagerView;
import com.simplecity.amp_library.playback.MusicUtils;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.BackpressureStrategy;
import io.reactivex.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class QueuePagerPresenter extends Presenter<QueuePagerView> {

    @Inject
    RequestManager requestManager;

    public boolean ignoreQueueChangeEvent = false;

    @Inject
    public QueuePagerPresenter() {

    }

    @Override
    public void bindView(@NonNull QueuePagerView view) {
        super.bindView(view);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.InternalIntents.META_CHANGED);
        filter.addAction(Constants.InternalIntents.REPEAT_CHANGED);
        filter.addAction(Constants.InternalIntents.SHUFFLE_CHANGED);
        filter.addAction(Constants.InternalIntents.QUEUE_CHANGED);
        filter.addAction(Constants.InternalIntents.SERVICE_CONNECTED);

        addDisposable(RxBroadcast.fromBroadcast(ShuttleApplication.getInstance(), filter)
                .startWith(new Intent(Constants.InternalIntents.QUEUE_CHANGED))
                .toFlowable(BackpressureStrategy.LATEST)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(intent -> {
                    final String action = intent.getAction();

                    QueuePagerView queuePagerView = getView();
                    if (queuePagerView == null) {
                        return;
                    }

                    if (action != null) {
                        switch (action) {
                            case Constants.InternalIntents.META_CHANGED:
                                queuePagerView.updateQueuePosition(MusicUtils.getQueuePosition());
                                break;
                            case Constants.InternalIntents.REPEAT_CHANGED:
                            case Constants.InternalIntents.SHUFFLE_CHANGED:
                            case Constants.InternalIntents.QUEUE_CHANGED:
                            case Constants.InternalIntents.SERVICE_CONNECTED:

                                List<ViewModel> items = Stream.of(MusicUtils.getQueue())
                                        .map(song -> new QueuePagerItemView(song, requestManager))
                                        .collect(Collectors.toList());

                                queuePagerView.loadData(items, MusicUtils.getQueuePosition());
                                break;
                        }
                    }
                }));

        addDisposable(MusicEventRelay.getInstance().getEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(queueEvent -> {
                    QueuePagerView queuePagerView = getView();
                    switch (queueEvent.getType()) {
                        case MetadataChangedEvent.TYPE:
                        case QueueChangedEvent.TYPE:
                            if (ignoreQueueChangeEvent) {
                                ignoreQueueChangeEvent = false;
                                Timber.d("Ignoring queue change event");
                                break;
                            }

                            Timber.d("Responding to queue change event");

                            List<ViewModel> items = Stream.of(MusicUtils.getQueue())
                                    .map(song -> new QueuePagerItemView(song, requestManager))
                                    .collect(Collectors.toList());

                            if (queuePagerView != null) {
                                queuePagerView.loadData(items, MusicUtils.getQueuePosition());
                            }
                            break;
                        case QueuePositionChangedEvent.TYPE:
                            if (ignoreQueueChangeEvent) {
                                ignoreQueueChangeEvent = false;
                                Timber.d("Ignoring queue position change event");
                                break;
                            }
                            Timber.d("Responding to queue position change event");
                            if (queuePagerView != null) {
                                queuePagerView.updateQueuePosition(MusicUtils.getQueuePosition());
                            }
                            break;

                    }
                }));
    }
}