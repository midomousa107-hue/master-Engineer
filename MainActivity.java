package com.masterengineer.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView webView;
    private RecordsDb recordsDb;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(8,35,63));
        getWindow().setNavigationBarColor(Color.rgb(6,27,50));
        recordsDb = new RecordsDb();
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(242,247,251));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(false);
        webView.getSettings().setDatabaseEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new AndroidDB(), "AndroidDB");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    private class AndroidDB {
        @JavascriptInterface public void saveRecord(String type, String result) {
            recordsDb.insert(type, result);
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "تم حفظ العملية", Toast.LENGTH_SHORT).show());
        }
        @JavascriptInterface public String getRecordsHtml() {
            JSONArray arr = recordsDb.all();
            StringBuilder out = new StringBuilder();
            if (arr.length() == 0) return "لا توجد عمليات محفوظة حتى الآن.";
            for (int i=0; i<arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                String type = escape(o.optString("type"));
                String result = escape(o.optString("result")).replace("\\n", "<br>");
                String date = escape(o.optString("created_at"));
                out.append("<div class='lesson'><b>").append(type).append("</b><br>")
                   .append(result).append("<br><small>").append(date).append("</small></div>");
            }
            return out.toString();
        }
        private String escape(String s) {
            return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("'","&#39;").replace("\"","&quot;");
        }
    }

    private class RecordsDb extends SQLiteOpenHelper {
        RecordsDb() { super(MainActivity.this, "master_engineer.db", null, 1); }
        @Override public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE records (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, result TEXT NOT NULL, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        }
        @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
        void insert(String type, String result) {
            ContentValues v = new ContentValues(); v.put("type", type); v.put("result", result);
            getWritableDatabase().insert("records", null, v);
        }
        JSONArray all() {
            JSONArray a = new JSONArray();
            try (Cursor c = getReadableDatabase().rawQuery("SELECT type,result,created_at FROM records ORDER BY id DESC", null)) {
                while (c.moveToNext()) {
                    JSONObject o = new JSONObject();
                    o.put("type", c.getString(0)); o.put("result", c.getString(1)); o.put("created_at", c.getString(2));
                    a.put(o);
                }
            } catch (Exception ignored) {}
            return a;
        }
    }
}