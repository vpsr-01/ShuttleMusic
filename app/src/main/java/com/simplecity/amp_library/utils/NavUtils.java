package com.simplecity.amp_library.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.simplecity.amp_library.model.AlbumArtist;
import com.simplecity.amp_library.ui.fragments.ArtistDetailFragment;
import com.simplecity.amp_library.ui.fragments.DetailFragment;

import java.io.Serializable;

public class NavUtils {

    public static Fragment getDetailFragment(@NonNull Serializable object) {
        return getDetailFragment(object, null);
    }

    public static Fragment getDetailFragment(@NonNull Serializable object, @Nullable String transitionName) {

        if (object instanceof AlbumArtist) {
            return ArtistDetailFragment.newInstance((AlbumArtist) object, transitionName);
        } else {
            return DetailFragment.newInstance(object, transitionName);
        }
    }

}
