package com.simplecity.amp_library.playback;

import android.content.Context;

import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.simplecity.amp_library.constants.Config;

import java.util.List;

public class CastOptionsProvider implements OptionsProvider {

    @Override
    public CastOptions getCastOptions(Context context) {
        return new CastOptions.Builder()
                .setReceiverApplicationId(Config.CHROMECAST_APP_ID)
                .build();
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}
