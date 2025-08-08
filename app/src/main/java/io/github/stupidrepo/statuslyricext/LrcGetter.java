package io.github.stupidrepo.statuslyricext;

import android.content.Context;
import android.media.MediaMetadata;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import cn.zhaiyifan.lyric.LyricUtils;
import cn.zhaiyifan.lyric.model.Lyric;
import io.github.stupidrepo.statuslyricext.provider.ILrcProvider;
import io.github.stupidrepo.statuslyricext.provider.KugouProvider;
import io.github.stupidrepo.statuslyricext.provider.LRCLibProvider;
import io.github.stupidrepo.statuslyricext.provider.LyricsifyProvider;
import io.github.stupidrepo.statuslyricext.provider.NeteaseProvider;
import io.github.stupidrepo.statuslyricext.provider.QQMusicProvider;
import io.github.stupidrepo.statuslyricext.provider.utils.LyricSearchUtil;

public class LrcGetter {
    private static final String TAG = "LrcGetter";

    private static final ILrcProvider[] providers = {
            new KugouProvider(),
            new QQMusicProvider(),
            new NeteaseProvider(),
            new LyricsifyProvider(),
            new LRCLibProvider()
    };

    private static MessageDigest messageDigest;
    private static final char[] hexCode = "0123456789ABCDEF".toCharArray();

    public static Lyric getLyric(Context context, MediaMetadata mediaMetadata) {
        if (messageDigest == null) {
            try {
                messageDigest =  MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException e) {
                Log.e("ERROR", String.valueOf(e));
                return null;
            }
        }
        File cachePath = context.getCacheDir();
        String meta = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE) + "," + mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST) + "," +
                mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM) + ", " + mediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        File requireLrcPath = new File(cachePath, printHexBinary(messageDigest.digest(meta.getBytes())) + ".lrc");
        if (requireLrcPath.exists()) {
            return LyricUtils.parseLyric(requireLrcPath, "UTF-8");
        }
        ILrcProvider.LyricResult currentResult = null;
        for (ILrcProvider provider : providers) {
            try {
                ILrcProvider.LyricResult lyricResult = provider.getLyric(mediaMetadata);
                if (lyricResult != null && LyricSearchUtil.isLyricContent(lyricResult.mLyric) && (currentResult == null || currentResult.mDistance > lyricResult.mDistance)) {
                    currentResult = lyricResult;
                }
            } catch (IOException e) {
                Log.w(TAG, "Failed to fetch from provider: " + e.getMessage());
            }
        }
        if (currentResult != null && LyricSearchUtil.isLyricContent(currentResult.mLyric)) {
            try {
                FileOutputStream lrcOut = new FileOutputStream(requireLrcPath);
                lrcOut.write(currentResult.mLyric.getBytes());
                lrcOut.close();
                return LyricUtils.parseLyric(requireLrcPath, "UTF-8");
            } catch (IOException e) {
                Log.e("ERROR", String.valueOf(e));
            }
        }
        return null;
    }

    public static String printHexBinary(byte[] data) {
        StringBuilder r = new StringBuilder(data.length * 2);
        for (byte b : data) {
            r.append(hexCode[(b >> 4) & 0xF]);
            r.append(hexCode[b & 0xF]);
        }
        return r.toString();
    }
}
