package vrsync.samsung.com.vrsync.Utils;

public interface ConstantSFValues {

    interface Numbers {
        int CONTROLLER_PORT = 5000;
        int MAX_DEVICE_CONNECTIONS = 100;
        int SEND_PORT_NUMBER = 5001;
        Long ONE_MINUTE = 1000L;
    }

    interface Preferences {
        String CONTROLLER_PORT = "PrefKeyControllerPort";
        String CONTROLLER_WHITE_LIST = "PrefKeyControllerWhiteList";
        String PLAYLIST_ITEMS = "PrefKeyPlaylistItems";
    }

    interface PlayListControls {
        String SERVICE = "Action";
        String IDLE = "Idle";
        String PLAY = "Play";
        String PAUSE = "Pause";
        String PREVIOUS = "Previous";
        String NEXT = "Next";
        String RESET = "Reset";
        String SEEK = "Seek";
        String SEEK_VALUE = "SeekValue";
    }

    interface Fragments {
        String PLAYLIST_FRAGMENT = "PLAYLIST_FRAGMENT";
        String DEVICE_LIST_FRAGMENT = "DEVICE_LIST_FRAGMENT";
        String NO_DEVICE_CONNECTED_FRAGMENT = "NO_DEVICE_CONNECTED_FRAGMENT";
    }

    interface Playlist {
        String STORAGE_PATH = "/storage/emulated/0/MilkVR";
        String SIZE = "size";
        String FILE_PATH = "filepath";
        String PLAYLIST_KEY = "PLAYLIST";
    }
}