package com.example.scholarapp.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DocumentTextExtractor {

    public static String extractText(Context context, Uri uri, String filename) {
        String lower = filename.toLowerCase();
        try {
            if (lower.endsWith(".docx")) {
                return extractTextFromDocx(context, uri);
            } else if (lower.endsWith(".doc")) {
                return extractTextFromDoc(context, uri);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String extractTextFromDocx(Context context, Uri uri) throws Exception {
        try (InputStream is = context.getContentResolver().openInputStream(uri);
             ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    StringBuilder textBuilder = new StringBuilder();
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(zis, "UTF-8");
                    int eventType = parser.getEventType();
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG && "t".equals(parser.getName())) {
                            textBuilder.append(parser.nextText()).append(" ");
                        }
                        eventType = parser.next();
                    }
                    return textBuilder.toString().replaceAll("\\s+", " ").trim();
                }
            }
        }
        return null;
    }

    private static String extractTextFromDoc(Context context, Uri uri) throws Exception {
        byte[] fileBytes;
        try (InputStream is = context.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            fileBytes = baos.toByteArray();
        }

        // 1. Try to open as zip first (in case it is a renamed .docx)
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(fileBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    StringBuilder textBuilder = new StringBuilder();
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(zis, "UTF-8");
                    int eventType = parser.getEventType();
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG && "t".equals(parser.getName())) {
                            textBuilder.append(parser.nextText()).append(" ");
                        }
                        eventType = parser.next();
                    }
                    return textBuilder.toString().replaceAll("\\s+", " ").trim();
                }
            }
        } catch (Exception e) {
            // ignore and fallback to binary scan
        }

        // 2. Legacy .doc binary text scan (heuristic search for ASCII/UTF-16 printable blocks)
        StringBuilder sb = new StringBuilder();
        int minLength = 8;
        int currentLength = 0;
        StringBuilder temp = new StringBuilder();

        for (byte b : fileBytes) {
            char c = (char) (b & 0xFF);
            if ((c >= 32 && c <= 126) || c == '\n' || c == '\r' || c == '\t') {
                temp.append(c);
                currentLength++;
            } else {
                if (currentLength >= minLength) {
                    sb.append(temp.toString().replaceAll("\\s+", " ").trim()).append(" ");
                }
                temp.setLength(0);
                currentLength = 0;
            }
        }
        if (currentLength >= minLength) {
            sb.append(temp.toString().replaceAll("\\s+", " ").trim()).append(" ");
        }

        // Return extracted text if it has enough contents
        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }
}
