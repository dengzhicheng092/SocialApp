package innovativedeveloper.com.socialapp.config;

import android.content.Context;
import android.content.SharedPreferences;

public class DataStorage {
    static SharedPreferences prefs;
    SharedPreferences.Editor editor;
    Context mContext;

    public DataStorage(Context context) {
        this.mContext = context;
        prefs = mContext.getSharedPreferences("innovativedeveloper.com.socialapp", Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public String getString(String key, String defValue){
        return prefs.getString(key,defValue);
    }

    public Boolean getBoolean(String key, boolean defValue){ return prefs.getBoolean(key, defValue); }

    public Integer getInt(String key, Integer defValue){
        return prefs.getInt(key, defValue);
    }

    public void setString(String key, String value){
        prefs.edit().putString(key, value).apply();
    }

    public void setInt(String key, Integer value){
        prefs.edit().putInt(key, value).apply();
    }

    public void clear() {
        editor.clear();
        editor.commit();
    }
}
