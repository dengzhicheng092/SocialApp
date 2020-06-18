package innovativedeveloper.com.socialapp.config;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

import innovativedeveloper.com.socialapp.dataset.Inbox;
import innovativedeveloper.com.socialapp.dataset.Message;
import innovativedeveloper.com.socialapp.dataset.Notification;
import innovativedeveloper.com.socialapp.dataset.User;

public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DBVersion = 2;
    private static final String DBName = "socialDatabase";

    public DatabaseHandler(Context context)
    {
        super(context, DBName, null, DBVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String usersTable = "CREATE TABLE IF NOT EXISTS users (username VARCHAR(255), name VARCHAR(50), email VARCHAR(255), profilePhoto TEXT, creation DATETIME);";
        String notificationTable = "CREATE TABLE IF NOT EXISTS notifications (id INTEGER, userId INTEGER, postId INTEGER, username VARCHAR(255), action INTEGER, messageData TEXT, creation DATETIME, isRead INTEGER, icon TEXT, commentId INTEGER)";
        String messagesTable = "CREATE TABLE IF NOT EXISTS messages (id INTEGER PRIMARY KEY, username VARCHAR(255), type INTEGER, message TEXT, creation DATETIME, isSent INTEGER, isChecked INTEGER, isOwn INTEGER);";

        db.execSQL(usersTable);
        db.execSQL(notificationTable);
        db.execSQL(messagesTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS notifications");
        db.execSQL("DROP TABLE IF EXISTS messages");
        onCreate(db);
    }

    public boolean isUserExists(String username)
    {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username = '" + username + "'", null);
        int userCount = cursor.getCount();
        cursor.close();
        return userCount == 1;
    }

    public ArrayList<Message> getAllMessages(String username) {
        ArrayList<Message> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM messages WHERE username = '$sender'".replace("$sender", username), null);
        if (cursor.moveToFirst()) {
            do {
                Message m = new Message();
                m.setId(cursor.getString(0));
                m.setUsername(cursor.getString(1));
                m.setType(cursor.getInt(2));
                m.setMessage(cursor.getString(3));
                m.setCreation(cursor.getString(4));
                m.setSent(cursor.getInt(5));
                m.setChecked(cursor.getInt(6));
                m.setIsOwn(cursor.getInt(7));
                messages.add(m);
            } while (cursor.moveToNext());
        }
        cursor.close();
        markMessagesAsRead(username);
        return messages;
    }

    public User getUser(String username)
    {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username = '" + username + "'", null);
        User u;
        if(cursor.moveToFirst()) {
            u = new User();
            u.setUsername(cursor.getString(0));
            u.setName(cursor.getString(1));
            u.setEmail(cursor.getString(2));
            u.setProfilePhoto(cursor.getString(3));
            u.setCreation(cursor.getString(4));
            cursor.close();
            return u;
        }
        u = new User();
        u.setUsername(username);
        u.setName(username);
        cursor.close();
        return u;
    }

    public ArrayList<Inbox> getInbox() {
        ArrayList<Inbox> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("select *, (SELECT COUNT(*) FROM messages m WHERE m.username = mm.username AND m.isChecked = 0) as messagesCount from messages mm where id in (select max(id) from messages group by username) ORDER BY creation DESC", null);
        if (cursor.moveToFirst()) {
            do {
                Inbox n = new Inbox();
                n.setId(cursor.getString(0));
                n.setType(cursor.getInt(2));
                n.setMessage(cursor.getString(3));
                n.setCreation(cursor.getString(4));
                n.setSent(cursor.getInt(5));
                n.setChecked(cursor.getInt(6));
                n.setCounts(cursor.getInt(8));
                n.setUser(getUser(cursor.getString(1)));
                messages.add(n);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return messages;
    }

    public void deleteConversation(String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("messages", "username = ?", new String[] { username });
    }

    void markMessagesAsRead(String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("isChecked", 1);
        db.update("messages", values, "username = ':id'".replace(":id", username), null);
    }

    public void markAsReadNotification(Notification n) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("isRead", 1);
        db.update("notifications", values, "id = :id".replace(":id", n.getId()), null);
    }

    public void addUser(User u) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", u.getUsername());
        values.put("name", u.getName());
        values.put("email", u.getEmail());
        values.put("profilePhoto", u.getProfilePhoto());
        values.put("creation", u.getCreation());
        db.insertWithOnConflict("users", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void addMessage(Message m) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", m.getUsername());
        values.put("type", m.getType());
        values.put("message", m.getMessage());
        values.put("creation", m.getCreation());
        values.put("isSent", m.getSent());
        values.put("isChecked", m.getChecked());
        values.put("isOwn", m.getIsOwn());
        db.insertWithOnConflict("messages", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void addNotification(Notification n) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", n.getId());
        values.put("userId", n.getUser_id());
        values.put("commentId", n.getCommentId());
        values.put("postId", n.getPostId());
        values.put("username", n.getUsername());
        values.put("action", n.getAction());
        values.put("messageData", n.getData());
        values.put("creation", n.getCreation());
        values.put("icon", n.getIcon());
        values.put("isRead", 0);
        db.insertWithOnConflict("notifications", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public ArrayList<Notification> getNotifications() {
        ArrayList<Notification> notificationArrayList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM notifications WHERE isRead = 0 GROUP BY id ORDER BY creation desc", null);
        if (cursor.moveToFirst()) {
            do {
                Notification n = new Notification();
                n.setId(cursor.getString(0));
                n.setUser_id(cursor.getString(1));
                n.setPostId(cursor.getString(2));
                n.setUsername(cursor.getString(3));
                n.setAction(cursor.getString(4));
                n.setData(cursor.getString(5));
                n.setCreation(cursor.getString(6));
                n.setIcon(cursor.getString(8));
                n.setCommentId(cursor.getString(9));
                notificationArrayList.add(n);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return notificationArrayList;
    }

    public int getMessagesCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) from messages where isChecked = 0;", null);
        if (cursor.moveToFirst()) {
            return cursor.getInt(0);
        }
        cursor.close();
        return 0;
    }

    public int getNotificationsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) from notifications where isRead = 0;", null);
        if (cursor.moveToFirst()) {
            return cursor.getInt(0);
        }
        cursor.close();
        return 0;
    }

    public void resetNotifications() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM notifications");
        db.close();
    }

    public void resetDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM notifications");
        db.execSQL("DELETE FROM messages");
        db.execSQL("DELETE FROM users");
        db.close();
    }
}
