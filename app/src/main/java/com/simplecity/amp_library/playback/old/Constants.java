package com.simplecity.amp_library.playback.old;

public class Constants {
    public static final String INTERNAL_INTENT_PREFIX = "com.simplecity.shuttle";
    public static final String SERVICE_COMMAND_PREFIX = "com.simplecity.shuttle.music_service_command";

    public interface InternalIntents {
        String PLAY_STATE_CHANGED = INTERNAL_INTENT_PREFIX + ".playstatechanged";
        String POSITION_CHANGED = INTERNAL_INTENT_PREFIX + ".positionchanged";
        String TRACK_ENDING = INTERNAL_INTENT_PREFIX + ".trackending";
        String META_CHANGED = INTERNAL_INTENT_PREFIX + ".metachanged";
        String QUEUE_CHANGED = INTERNAL_INTENT_PREFIX + ".queuechanged";
        String SHUFFLE_CHANGED = INTERNAL_INTENT_PREFIX + ".shufflechanged";
        String REPEAT_CHANGED = INTERNAL_INTENT_PREFIX + ".repeatchanged";
        String FAVORITE_CHANGED = INTERNAL_INTENT_PREFIX + ".favoritechanged";
        String SERVICE_CONNECTED = INTERNAL_INTENT_PREFIX + ".serviceconnected";
    }

    public interface MediaButtonCommand {
        String CMD_NAME = "command";
        String TOGGLE_PAUSE = "togglepause";
        String STOP = "stop";
        String PAUSE = "pause";
        String PLAY = "play";
        String PREVIOUS = "previous";
        String NEXT = "skip";
        String TOGGLE_FAVORITE = "togglefavorite";
        String FROM_MEDIA_BUTTON = "frommediabutton";
    }

    public interface ServiceCommand {
        String SERVICE_COMMAND = SERVICE_COMMAND_PREFIX;
        String TOGGLE_PAUSE_ACTION = SERVICE_COMMAND_PREFIX + ".togglepause";
        String PAUSE_ACTION = SERVICE_COMMAND_PREFIX + ".pause";
        String PREV_ACTION = SERVICE_COMMAND_PREFIX + ".prev";
        String NEXT_ACTION = SERVICE_COMMAND_PREFIX + ".skip";
        String STOP_ACTION = SERVICE_COMMAND_PREFIX + ".stop";
        String SHUFFLE_ACTION = SERVICE_COMMAND_PREFIX + ".shuffle";
        String REPEAT_ACTION = SERVICE_COMMAND_PREFIX + ".repeat";
        String SHUTDOWN = SERVICE_COMMAND_PREFIX + ".shutdown";
        String TOGGLE_FAVORITE = SERVICE_COMMAND_PREFIX + ".togglefavorite";
    }

    public interface ExternalIntents {
        String PLAY_STATUS_REQUEST = "com.android.music.playstatusrequest";
        String PLAY_STATUS_RESPONSE = "com.android.music.playstatusresponse";

        String AVRCP_PLAY_STATE_CHANGED = "com.android.music.playstatechanged";
        String AVRCP_META_CHANGED = "com.android.music.metachanged";

        String TASKER = "net.dinglisch.android.tasker.extras.VARIABLE_REPLACE_KEYS";

        String SCROBBLER = "com.adam.aslfms.notify.playstatechanged";

        String PEBBLE = "com.getpebble.action.NOW_PLAYING";
    }

    public interface ShortcutCommands {
        String PLAY = "com.simplecity.amp_library.shortcuts.PLAY";
        String SHUFFLE_ALL = "com.simplecity.amp_library.shortcuts.SHUFFLE";
        String FOLDERS = "com.simplecity.amp_library.shortcuts.FOLDERS";
        String PLAYLIST = "com.simplecity.amp_library.shortcuts.PLAYLIST";
    }

    interface Status {
        int START = 0;
        int RESUME = 1;
        int PAUSE = 2;
        int COMPLETE = 3;
    }

    interface PlayerHandler {
        int TRACK_ENDED = 1;
        int RELEASE_WAKELOCK = 2;
        int SERVER_DIED = 3;
        int FOCUS_CHANGE = 4;
        int FADE_DOWN = 5;
        int FADE_UP = 6;
        int TRACK_WENT_TO_NEXT = 7;
        int FADE_DOWN_STOP = 9;
        int GO_TO_NEXT = 10;
        int GO_TO_PREV = 11;
        int SHUFFLE_ALL = 12;
    }
}
