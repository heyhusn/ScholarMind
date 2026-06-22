package com.example.scholarapp.network;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class ContentUriRequestBody extends RequestBody {

    private final Context context;
    private final Uri uri;
    private final String mimeType;
    private final long contentLength;

    public ContentUriRequestBody(Context context, Uri uri, String mimeType, long contentLength) {
        this.context = context.getApplicationContext();
        this.uri = uri;
        this.mimeType = mimeType;
        this.contentLength = contentLength;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return MediaType.parse(mimeType);
    }

    @Override
    public long contentLength() {
        return contentLength > 0 ? contentLength : -1;
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        try (InputStream inputStream = resolver.openInputStream(uri)) {
            if (inputStream == null) {
                throw new IOException("Unable to open file stream.");
            }
            try (Source source = Okio.source(inputStream)) {
                sink.writeAll(source);
            }
        }
    }
}
