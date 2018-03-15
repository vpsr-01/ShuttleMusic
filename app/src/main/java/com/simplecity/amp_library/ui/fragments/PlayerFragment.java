package com.simplecity.amp_library.ui.fragments;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.Util;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.jakewharton.rxbinding2.widget.RxSeekBar;
import com.jakewharton.rxbinding2.widget.SeekBarChangeEvent;
import com.jakewharton.rxbinding2.widget.SeekBarProgressChangeEvent;
import com.jakewharton.rxbinding2.widget.SeekBarStartChangeEvent;
import com.jakewharton.rxbinding2.widget.SeekBarStopChangeEvent;
import com.jp.wasabeef.glide.transformations.BlurTransformation;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.dagger.module.FragmentModule;
import com.simplecity.amp_library.glide.palette.PaletteBitmap;
import com.simplecity.amp_library.glide.palette.PaletteBitmapTranscoder;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.playback.MusicUtils;
import com.simplecity.amp_library.playback.QueueManager;
import com.simplecity.amp_library.rx.UnsafeConsumer;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.drawer.NavigationEventRelay;
import com.simplecity.amp_library.ui.presenters.PlayerPresenter;
import com.simplecity.amp_library.ui.views.FavoriteActionBarView;
import com.simplecity.amp_library.ui.views.PlayPauseView;
import com.simplecity.amp_library.ui.views.PlayerView;
import com.simplecity.amp_library.ui.views.RepeatButton;
import com.simplecity.amp_library.ui.views.RepeatingImageButton;
import com.simplecity.amp_library.ui.views.ShuffleButton;
import com.simplecity.amp_library.ui.views.SizableSeekBar;
import com.simplecity.amp_library.ui.views.SnowfallView;
import com.simplecity.amp_library.ui.views.multisheet.MultiSheetSlideEventRelay;
import com.simplecity.amp_library.utils.DataManager;
import com.simplecity.amp_library.utils.LogUtils;
import com.simplecity.amp_library.utils.PlaceholderProvider;
import com.simplecity.amp_library.utils.ShuttleUtils;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecity.amp_library.utils.UISettings;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class PlayerFragment extends BaseFragment implements
        PlayerView,
        Toolbar.OnMenuItemClickListener {

    private final String TAG = ((Object) this).getClass().getSimpleName();

    private boolean isSeeking;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Nullable
    @BindView(R.id.play)
    PlayPauseView playPauseView;

    @Nullable
    @BindView(R.id.shuffle)
    ShuffleButton shuffleButton;

    @Nullable
    @BindView(R.id.repeat)
    RepeatButton repeatButton;

    @Nullable
    @BindView(R.id.next)
    RepeatingImageButton nextButton;

    @Nullable
    @BindView(R.id.prev)
    RepeatingImageButton prevButton;

    @Nullable
    @BindView(R.id.current_time)
    TextView currentTime;

    @Nullable
    @BindView(R.id.total_time)
    TextView totalTime;

    @Nullable
    @BindView(R.id.text1)
    TextView track;

    @Nullable
    @BindView(R.id.text2)
    TextView album;

    @Nullable
    @BindView(R.id.text3)
    TextView artist;

    @BindView(R.id.backgroundView)
    ImageView backgroundView;

    @Nullable
    @BindView(R.id.seekbar)
    SizableSeekBar seekBar;

    @BindView(R.id.let_it_snow)
    SnowfallView snowfallView;

    CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    PlayerPresenter presenter;

    @Inject
    NavigationEventRelay navigationEventRelay;

    @Inject
    MultiSheetSlideEventRelay sheetEventRelay;

    private Unbinder unbinder;

    int currentColor = Color.TRANSPARENT;

    private boolean isLandscape;

    private boolean isExpanded;

    @Nullable
    private Target<GlideDrawable> target;

    @Nullable
    private SimpleTarget<PaletteBitmap> paletteTarget;

    public PlayerFragment() {
    }

    public static PlayerFragment newInstance() {
        PlayerFragment playerFragment = new PlayerFragment();
        Bundle args = new Bundle();
        playerFragment.setArguments(args);
        return playerFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShuttleApplication.getInstance().getAppComponent()
                .plus(new FragmentModule(this))
                .inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        isLandscape = ShuttleUtils.isLandscape();

        unbinder = ButterKnife.bind(this, rootView);

        toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
        toolbar.inflateMenu(R.menu.menu_now_playing);

        CastButtonFactory.setUpMediaRouteButton(getContext().getApplicationContext(), toolbar.getMenu(), R.id.media_route_menu_item);

        MenuItem favoriteMenuItem = toolbar.getMenu().findItem(R.id.favorite);
        FavoriteActionBarView menuActionView = (FavoriteActionBarView) favoriteMenuItem.getActionView();
        menuActionView.setOnClickListener(v -> onMenuItemClick(favoriteMenuItem));
        toolbar.setOnMenuItemClickListener(this);

        if (playPauseView != null) {
            playPauseView.setOnClickListener(v -> {
                playPauseView.toggle();
                playPauseView.postDelayed(() -> presenter.togglePlayback(), 200);
            });
        }

        if (repeatButton != null) {
            repeatButton.setOnClickListener(v -> presenter.toggleRepeat());
        }

        if (shuffleButton != null) {
            shuffleButton.setOnClickListener(v -> presenter.toggleShuffle());
        }

        if (nextButton != null) {
            nextButton.setOnClickListener(v -> presenter.skip());
            nextButton.setRepeatListener((v, duration, repeatCount) -> presenter.scanForward(repeatCount, duration));
        }

        if (prevButton != null) {
            prevButton.setOnClickListener(v -> presenter.prev(true));
            prevButton.setRepeatListener((v, duration, repeatCount) -> presenter.scanBackward(repeatCount, duration));
        }

        if (seekBar != null) {
            seekBar.setMax(1000);
        }

        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction()
                    .add(R.id.main_container, QueuePagerFragment.newInstance(), "QueuePagerFragment")
                    .commit();
        }

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenter.bindView(this);
    }

    @Override
    public void onDestroyView() {
        if (target != null) {
            Glide.clear(target);
        }

        if (paletteTarget != null) {
            Glide.clear(paletteTarget);
        }

        snowfallView.clear();

        presenter.unbindView(this);
        unbinder.unbind();

        super.onDestroyView();
    }

    public void update() {
        if (presenter != null) {
            presenter.updateTrackInfo(MusicUtils.getCurrentSong());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        disposables.add(Aesthetic.get(getContext())
                .colorPrimary()
                .subscribe(this::invalidateColors));

        if (seekBar != null) {
            Flowable<SeekBarChangeEvent> sharedSeekBarEvents = RxSeekBar.changeEvents(seekBar)
                    .toFlowable(BackpressureStrategy.LATEST)
                    .ofType(SeekBarChangeEvent.class)
                    .observeOn(AndroidSchedulers.mainThread())
                    .share();

            disposables.add(sharedSeekBarEvents.subscribe(seekBarChangeEvent -> {
                if (seekBarChangeEvent instanceof SeekBarStartChangeEvent) {
                    isSeeking = true;
                } else if (seekBarChangeEvent instanceof SeekBarStopChangeEvent) {
                    isSeeking = false;
                }
            }, error -> LogUtils.logException(TAG, "Error in seek change event", error)));

            disposables.add(sharedSeekBarEvents
                    .ofType(SeekBarProgressChangeEvent.class)
                    .filter(SeekBarProgressChangeEvent::fromUser)
                    .debounce(15, TimeUnit.MILLISECONDS)
                    .subscribe(seekBarChangeEvent -> presenter.seekTo(seekBarChangeEvent.progress()),
                            error -> LogUtils.logException(TAG, "Error receiving seekbar progress", error)));
        }

        disposables.add(RxSharedPreferences.create(PreferenceManager.getDefaultSharedPreferences(getContext()))
                .getBoolean(UISettings.KEY_DISPLAY_REMAINING_TIME)
                .asObservable()
                .subscribe(aBoolean -> presenter.updateRemainingTime()));

        disposables.add(sheetEventRelay.getEvents()
                .subscribe(event -> {
                    if (event.nowPlayingExpanded()) {
                        isExpanded = true;
                        snowfallView.letItSnow();
                    } else if (event.nowPlayingCollapsed()) {
                        isExpanded = false;
                        snowfallView.clear();
                    }
                }, throwable -> Log.e(TAG, "error listening for sheet slide events", throwable)));

        update();
    }

    @Override
    public void onPause() {
        disposables.clear();
        super.onPause();
    }

    @Override
    protected String screenName() {
        return TAG;
    }

    ////////////////////////////////////////////////////////////////////
    // View implementation
    ////////////////////////////////////////////////////////////////////

    @Override
    public void setSeekProgress(int progress) {
        if (!isSeeking && seekBar != null) {
            seekBar.setProgress(progress);
        }
    }

    @Override
    public void currentTimeVisibilityChanged(boolean visible) {
        if (currentTime != null) {
            currentTime.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Override
    public void currentTimeChanged(long seconds) {
        if (currentTime != null) {
            currentTime.setText(StringUtils.makeTimeString(this.getActivity(), seconds));
        }
    }

    @Override
    public void totalTimeChanged(long seconds) {
        if (totalTime != null) {
            totalTime.setText(StringUtils.makeTimeString(this.getActivity(), seconds));
        }
    }

    @Override
    public void queueChanged(int queuePosition, int queueLength) {

    }

    @Override
    public void playbackChanged(boolean isPlaying) {
        if (playPauseView != null) {
            if (isPlaying) {
                if (playPauseView.isPlay()) {
                    playPauseView.toggle();
                    playPauseView.setContentDescription(getString(R.string.btn_pause));
                }
            } else {
                if (!playPauseView.isPlay()) {
                    playPauseView.toggle();
                    playPauseView.setContentDescription(getString(R.string.btn_play));
                }
            }
        }

        if (!isPlaying) {
            snowfallView.removeSnow();
        }
    }

    @Override
    public void shuffleChanged(@QueueManager.ShuffleMode String shuffleMode) {
        if (shuffleButton != null) {
            shuffleButton.setShuffleMode(shuffleMode);
        }
    }

    @Override
    public void repeatChanged(@QueueManager.RepeatMode String repeatMode) {
        if (repeatButton != null) {
            repeatButton.setRepeatMode(repeatMode);
        }
    }

    @Override
    public void favoriteChanged(boolean isFavorite) {
        FavoriteActionBarView favoriteActionBarView = (FavoriteActionBarView) toolbar.getMenu().findItem(R.id.favorite).getActionView();
        favoriteActionBarView.setIsFavorite(isFavorite);
    }

    Song song = null;

    @Override
    public void trackInfoChanged(@Nullable Song song) {

        if (song == null) return;

        if (isExpanded && !snowfallView.isSnowing()) {
            snowfallView.letItSnow();
        } else {
            snowfallView.removeSnow();
        }

        String totalTimeString = StringUtils.makeTimeString(this.getActivity(), song.duration / 1000);
        if (!TextUtils.isEmpty(totalTimeString)) {
            if (totalTime != null) {
                totalTime.setText(totalTimeString);
            }
        }

        if (track != null) {
            track.setText(song.name);
            track.setSelected(true);
        }
        if (album != null) {
            album.setText(String.format("%s | %s", song.artistName, song.albumName));
        }

        if (isLandscape) {
            toolbar.setTitle(song.name);
            toolbar.setSubtitle(String.format("%s | %s", song.artistName, song.albumName));

            target = Glide.with(this)
                    .load(song)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .bitmapTransform(new BlurTransformation(getContext(), 15, 4))
                    .error(PlaceholderProvider.getInstance().getPlaceHolderDrawable(song.name, true))
                    .thumbnail(Glide
                            .with(this)
                            .load(this.song)
                            .bitmapTransform(new BlurTransformation(getContext(), 15, 4)))
                    .crossFade(600)
                    .into(backgroundView);

            this.song = song;
        } else {
            backgroundView.setImageDrawable(null);
            toolbar.setTitle(null);
            toolbar.setSubtitle(null);
        }

        if (UISettings.getInstance().getUsePalette()) {
            //noinspection unchecked

            if (paletteTarget != null) {
                Glide.clear(paletteTarget);
            }
            disposables.add(Completable.timer(250, TimeUnit.MILLISECONDS)
                    .andThen(Completable.fromAction(() -> {
                                paletteTarget = new SimpleTarget<PaletteBitmap>() {
                                    @Override
                                    public void onResourceReady(PaletteBitmap resource, GlideAnimation<? super PaletteBitmap> glideAnimation) {

                                        if (!isAdded() || getContext() == null) {
                                            return;
                                        }

                                        Palette.Swatch swatch = resource.palette.getDarkMutedSwatch();
                                        if (swatch != null) {
                                            if (UISettings.getInstance().getUsePalette()) {
                                                if (UISettings.getInstance().getUsePaletteNowPlayingOnly()) {
                                                    animateColors(currentColor, swatch.getRgb(), color -> invalidateColors(color));
                                                } else {
                                                    // Set Aesthetic colors globally, based on the current Palette swatch
                                                    disposables.add(
                                                            Aesthetic.get(getContext())
                                                                    .colorPrimary()
                                                                    .take(1)
                                                                    .subscribe(integer -> animateColors(integer, swatch.getRgb(), color -> {
                                                                        if (getContext() != null && isAdded()) {
                                                                            Aesthetic aesthetic = Aesthetic.get(getContext())
                                                                                    .colorPrimary(color)
                                                                                    .colorStatusBarAuto();

                                                                            if (UISettings.getInstance().getTintNavBar()) {
                                                                                aesthetic = aesthetic.colorNavigationBar(color);
                                                                            }

                                                                            aesthetic.apply();
                                                                        }
                                                                    })));
                                                }
                                            }
                                        } else {
                                            // Failed to generate the dark muted swatch, fall back to the primary theme colour.
                                            Aesthetic.get(getContext())
                                                    .colorPrimary()
                                                    .take(1)
                                                    .subscribe(primaryColor -> animateColors(currentColor, primaryColor, PlayerFragment.this::invalidateColors));
                                        }
                                    }

                                    @Override
                                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                        super.onLoadFailed(e, errorDrawable);
                                        Aesthetic.get(getContext())
                                                .colorPrimary()
                                                .take(1)
                                                .subscribe(primaryColor -> animateColors(currentColor, primaryColor, color -> invalidateColors(color)));
                                    }
                                };
                                Glide.with(PlayerFragment.this)
                                        .load(song)
                                        .asBitmap()
                                        .transcode(new PaletteBitmapTranscoder(getContext()), PaletteBitmap.class)
                                        .override(250, 250)
                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                                        .into(paletteTarget);
                            }).subscribeOn(AndroidSchedulers.mainThread())
                    )
                    .onErrorComplete()
                    .subscribe());
        }
    }

    void invalidateColors(int color) {

        currentColor = color;

        boolean isColorLight = Util.isColorLight(color);
        int textColor = isColorLight ? Color.BLACK : Color.WHITE;

        if (!isLandscape && backgroundView != null) {
            backgroundView.setBackgroundColor(color);
        }

        if (currentTime != null) {
            currentTime.setTextColor(textColor);
        }

        if (totalTime != null) {
            totalTime.setTextColor(textColor);
        }
        if (track != null) {
            track.setTextColor(textColor);
        }
        if (album != null) {
            album.setTextColor(textColor);
        }
        if (artist != null) {
            artist.setTextColor(textColor);
        }
    }

    @Override
    public void showToast(String message, int duration) {
        Toast.makeText(getContext(), message, duration).show();
    }

    @Override
    public void showLyricsDialog(MaterialDialog dialog) {
        dialog.show();
    }

    @Override
    public void showTaggerDialog(TaggerDialog taggerDialog) {
        taggerDialog.show(getFragmentManager());
    }

    @Override
    public void showSongInfoDialog(MaterialDialog dialog) {
        dialog.show();
    }

    @Override
    public void showUpgradeDialog(MaterialDialog dialog) {
        dialog.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.favorite:
                ((FavoriteActionBarView) item.getActionView()).toggle();
                presenter.toggleFavorite();
                return true;
            case R.id.lyrics:
                presenter.showLyrics(getContext());
                return true;
            case R.id.goToArtist:
                goToArtist();
                return true;
            case R.id.goToAlbum:
                goToAlbum();
                return true;
            case R.id.goToGenre:
                goToGenre();
                return true;
            case R.id.editTags:
                presenter.editTagsClicked(getActivity());
                return true;
            case R.id.songInfo:
                presenter.songInfoClicked(getContext());
                return true;
            case R.id.share:
                presenter.shareClicked(getContext());
                return true;
        }
        return false;
    }

    @SuppressLint("CheckResult")
    private void goToArtist() {
        Song currentSong = MusicUtils.getCurrentSong();
        if (currentSong != null) {
            // song.getAlbumArtist() is only populated with the album the current Song belongs to.
            // Let's find the matching AlbumArtist in the DataManager.albumArtistRelay
            DataManager.getInstance().getAlbumArtistsRelay()
                    .first(Collections.emptyList())
                    .flatMapObservable(Observable::fromIterable)
                    .filter(albumArtist -> currentSong.getAlbumArtist() != null && albumArtist.name.equals(currentSong.getAlbumArtist().name) && albumArtist.albums.containsAll(song.getAlbumArtist().albums))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(albumArtist -> navigationEventRelay.sendEvent(new NavigationEventRelay.NavigationEvent(NavigationEventRelay.NavigationEvent.Type.GO_TO_ARTIST, albumArtist, true)));
        }
    }

    private void goToAlbum() {
        Song song = MusicUtils.getCurrentSong();
        if (song != null) {
            navigationEventRelay.sendEvent(new NavigationEventRelay.NavigationEvent(NavigationEventRelay.NavigationEvent.Type.GO_TO_ALBUM, song.getAlbum(), true));
        }
    }

    @SuppressLint("CheckResult")
    private void goToGenre() {
        Song currentSong = MusicUtils.getCurrentSong();
        if (currentSong != null) {
            currentSong.getGenre()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            (UnsafeConsumer<Genre>) genre -> navigationEventRelay.sendEvent(new NavigationEventRelay.NavigationEvent(NavigationEventRelay.NavigationEvent.Type.GO_TO_GENRE, genre, true)),
                            error -> LogUtils.logException(TAG, "Error retrieving genre", error));
        }
    }

    void animateColors(int from, int to, UnsafeConsumer<Integer> consumer) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(from, to);
        valueAnimator.setEvaluator(new ArgbEvaluator());
        valueAnimator.setDuration(450);
        valueAnimator.addUpdateListener(animator -> consumer.accept((Integer) animator.getAnimatedValue()));
        valueAnimator.start();
    }

}
