package com.salardev.autodash;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.LibraryResult;
import androidx.media3.common.util.UnstableApi;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

@UnstableApi
public class AutoDashMediaService extends MediaLibraryService {
    private MediaLibrarySession mediaLibrarySession;
    private ExoPlayer player;
    private final List<MediaItem> library = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(this).build();
        player.setRepeatMode(Player.REPEAT_MODE_OFF);
        mediaLibrarySession = new MediaLibrarySession.Builder(this, player, new LibraryCallback()).build();
        loadLocalMusic();
    }

    private void loadLocalMusic() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<MediaItem> result = new ArrayList<>();
            ContentResolver resolver = getContentResolver();
            Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION
            };
            String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
            try (Cursor cursor = resolver.query(collection, projection, selection, null,
                    MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC")) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                    int albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                    int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idCol);
                        String title = cursor.getString(titleCol);
                        String artist = cursor.getString(artistCol);
                        String album = cursor.getString(albumCol);
                        long duration = cursor.getLong(durationCol);
                        Uri uri = ContentUris.withAppendedId(collection, id);
                        MediaMetadata metadata = new MediaMetadata.Builder()
                                .setTitle(title == null || title.isEmpty() ? "Unknown track" : title)
                                .setArtist(artist == null || artist.isEmpty() ? "Unknown artist" : artist)
                                .setAlbumTitle(album == null ? "" : album)
                                .setIsBrowsable(false)
                                .setIsPlayable(true)
                                .setDurationMs(duration)
                                .build();
                        result.add(new MediaItem.Builder().setMediaId(uri.toString()).setUri(uri).setMediaMetadata(metadata).build());
                    }
                }
            } catch (SecurityException ignored) {
                // Permission can be granted after service startup; the next client request will retry.
            }
            synchronized (library) {
                library.clear();
                library.addAll(result);
            }
            if (mediaLibrarySession != null) {
                mediaLibrarySession.notifyChildrenChanged("ROOT", Integer.MAX_VALUE, null);
            }
        });
    }

    @Override
    public MediaLibrarySession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaLibrarySession;
    }

    @Override
    public void onTaskRemoved(@Nullable android.content.Intent rootIntent) {
        if (player != null && !player.getPlayWhenReady()) {
            stopSelf();
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        if (mediaLibrarySession != null) {
            mediaLibrarySession.release();
            mediaLibrarySession = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    private final class LibraryCallback implements MediaLibrarySession.Callback {
        @Override
        public LibraryResult<MediaItem> onGetLibraryRoot(MediaLibrarySession session,
                                                          MediaSession.ControllerInfo browser,
                                                          MediaLibraryService.LibraryParams params) {
            MediaMetadata rootMetadata = new MediaMetadata.Builder()
                    .setTitle("AutoDash Music")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build();
            MediaItem root = new MediaItem.Builder().setMediaId("ROOT").setMediaMetadata(rootMetadata).build();
            return LibraryResult.ofItem(root, params);
        }

        @Override
        public LibraryResult<ImmutableList<MediaItem>> onGetChildren(MediaLibrarySession session,
                                                                      MediaSession.ControllerInfo browser,
                                                                      String parentId,
                                                                      int page,
                                                                      int pageSize,
                                                                      MediaLibraryService.LibraryParams params) {
            if (!"ROOT".equals(parentId)) {
                return LibraryResult.ofItemList(ImmutableList.of(), params);
            }
            List<MediaItem> snapshot;
            synchronized (library) {
                snapshot = new ArrayList<>(library);
            }
            int from = page * pageSize;
            if (from >= snapshot.size()) {
                return LibraryResult.ofItemList(ImmutableList.of(), params);
            }
            int to = Math.min(snapshot.size(), from + pageSize);
            return LibraryResult.ofItemList(ImmutableList.copyOf(snapshot.subList(from, to)), params);
        }

        @Override
        public LibraryResult<MediaItem> onGetItem(MediaLibrarySession session,
                                                   MediaSession.ControllerInfo browser,
                                                   String mediaId) {
            synchronized (library) {
                for (MediaItem item : library) {
                    if (item.mediaId.equals(mediaId)) {
                        return LibraryResult.ofItem(item);
                    }
                }
            }
            return LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE);
        }

        @Override
        public void onSearch(MediaLibrarySession session,
                             MediaSession.ControllerInfo browser,
                             String query,
                             MediaLibraryService.LibraryParams params) {
            String q = query == null ? "" : query.toLowerCase();
            List<MediaItem> matches = new ArrayList<>();
            synchronized (library) {
                for (MediaItem item : library) {
                    CharSequence title = item.mediaMetadata.title;
                    CharSequence artist = item.mediaMetadata.artist;
                    if ((title != null && title.toString().toLowerCase().contains(q)) ||
                            (artist != null && artist.toString().toLowerCase().contains(q))) {
                        matches.add(item);
                    }
                }
            }
            session.notifySearchResultChanged(browser, query, matches.size(), params);
        }

        @Override
        public LibraryResult<ImmutableList<MediaItem>> onGetSearchResult(MediaLibrarySession session,
                                                                          MediaSession.ControllerInfo browser,
                                                                          String query,
                                                                          int page,
                                                                          int pageSize,
                                                                          MediaLibraryService.LibraryParams params) {
            String q = query == null ? "" : query.toLowerCase();
            List<MediaItem> matches = new ArrayList<>();
            synchronized (library) {
                for (MediaItem item : library) {
                    CharSequence title = item.mediaMetadata.title;
                    CharSequence artist = item.mediaMetadata.artist;
                    if ((title != null && title.toString().toLowerCase().contains(q)) ||
                            (artist != null && artist.toString().toLowerCase().contains(q))) {
                        matches.add(item);
                    }
                }
            }
            int from = page * pageSize;
            if (from >= matches.size()) return LibraryResult.ofItemList(ImmutableList.of(), params);
            int to = Math.min(matches.size(), from + pageSize);
            return LibraryResult.ofItemList(ImmutableList.copyOf(matches.subList(from, to)), params);
        }
    }
}
