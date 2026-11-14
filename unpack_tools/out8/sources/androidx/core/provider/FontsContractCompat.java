package androidx.core.provider;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import androidx.core.util.Preconditions;

/* loaded from: classes.dex */
public class FontsContractCompat {

    /* loaded from: classes.dex */
    public static class FontRequestCallback {
        public void onTypefaceRequestFailed(int i) {
            throw null;
        }

        public void onTypefaceRetrieved(Typeface typeface) {
            throw null;
        }
    }

    public static Typeface requestFont(Context context, FontRequest request, int style, boolean isBlockingFetch, int timeout, Handler handler, FontRequestCallback callback) {
        CallbackWithHandler callbackWrapper = new CallbackWithHandler(callback, handler);
        if (isBlockingFetch) {
            return FontRequestWorker.requestFontSync(context, request, callbackWrapper, style, timeout);
        }
        return FontRequestWorker.requestFontAsync(context, request, style, null, callbackWrapper);
    }

    /* loaded from: classes.dex */
    public static class FontInfo {
        private final boolean mItalic;
        private final int mResultCode;
        private final int mTtcIndex;
        private final Uri mUri;
        private final int mWeight;

        @Deprecated
        public FontInfo(Uri uri, int ttcIndex, int weight, boolean italic, int resultCode) {
            this.mUri = (Uri) Preconditions.checkNotNull(uri);
            this.mTtcIndex = ttcIndex;
            this.mWeight = weight;
            this.mItalic = italic;
            this.mResultCode = resultCode;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static FontInfo create(Uri uri, int ttcIndex, int weight, boolean italic, int resultCode) {
            return new FontInfo(uri, ttcIndex, weight, italic, resultCode);
        }

        public Uri getUri() {
            return this.mUri;
        }

        public int getTtcIndex() {
            return this.mTtcIndex;
        }

        public int getWeight() {
            return this.mWeight;
        }

        public boolean isItalic() {
            return this.mItalic;
        }

        public int getResultCode() {
            return this.mResultCode;
        }
    }

    /* loaded from: classes.dex */
    public static class FontFamilyResult {
        private final FontInfo[] mFonts;
        private final int mStatusCode;

        @Deprecated
        public FontFamilyResult(int statusCode, FontInfo[] fonts) {
            this.mStatusCode = statusCode;
            this.mFonts = fonts;
        }

        public int getStatusCode() {
            return this.mStatusCode;
        }

        public FontInfo[] getFonts() {
            return this.mFonts;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static FontFamilyResult create(int statusCode, FontInfo[] fonts) {
            return new FontFamilyResult(statusCode, fonts);
        }
    }
}
