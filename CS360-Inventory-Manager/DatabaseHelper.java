package com.example.cs360projecttwo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "inventory_manager.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL)");

        db.execSQL("CREATE TABLE inventory (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "item_name TEXT NOT NULL, " +
                "quantity INTEGER NOT NULL, " +
                "FOREIGN KEY(user_id) REFERENCES users(id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS inventory");
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    public boolean createUser(String username, String password) {
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", hashPassword(password));

        return getWritableDatabase().insert(
                "users",
                null,
                values
        ) != -1;
    }

    public boolean isValidUser(String username, String password) {
        String hashedPassword = hashPassword(password);

        Cursor cursor = getReadableDatabase().query(
                "users",
                null,
                "username = ? AND password = ?",
                new String[]{username, hashedPassword},
                null,
                null,
                null
        );

        boolean userFound = cursor.moveToFirst();
        cursor.close();

        return userFound;
    }

    public int getUserId(String username) {
        Cursor cursor = getReadableDatabase().query(
                "users",
                new String[]{"id"},
                "username = ?",
                new String[]{username},
                null,
                null,
                null
        );

        int userId = -1;

        if (cursor.moveToFirst()) {
            userId = cursor.getInt(
                    cursor.getColumnIndexOrThrow("id")
            );
        }

        cursor.close();
        return userId;
    }

    public boolean addInventoryItem(
            int userId,
            String itemName,
            int quantity
    ) {

        ContentValues values = new ContentValues();

        values.put("user_id", userId);
        values.put("item_name", itemName);
        values.put("quantity", quantity);

        return getWritableDatabase().insert(
                "inventory",
                null,
                values
        ) != -1;
    }

    public boolean updateInventoryItem(
            int itemId,
            String itemName,
            int quantity
    ) {

        ContentValues values = new ContentValues();

        values.put("item_name", itemName);
        values.put("quantity", quantity);

        int rowsUpdated = getWritableDatabase().update(
                "inventory",
                values,
                "id = ?",
                new String[]{String.valueOf(itemId)}
        );

        return rowsUpdated > 0;
    }

    public boolean deleteInventoryItem(int itemId) {

        int rowsDeleted = getWritableDatabase().delete(
                "inventory",
                "id = ?",
                new String[]{String.valueOf(itemId)}
        );

        return rowsDeleted > 0;
    }

    public List<InventoryItem> getInventoryItems(int userId) {

        List<InventoryItem> items = new ArrayList<>();

        Cursor cursor = getReadableDatabase().query(
                "inventory",
                null,
                "user_id = ?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                "item_name ASC"
        );

        while (cursor.moveToNext()) {

            items.add(new InventoryItem(
                    cursor.getInt(
                            cursor.getColumnIndexOrThrow("id")
                    ),
                    cursor.getString(
                            cursor.getColumnIndexOrThrow("item_name")
                    ),
                    cursor.getInt(
                            cursor.getColumnIndexOrThrow("quantity")
                    )
            ));
        }

        cursor.close();

        return items;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                hexString.append(
                        String.format("%02x", b)
                );
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                    "Password hashing failed.",
                    e
            );
        }
    }

    public static class InventoryItem {

        public final int id;
        public final String name;
        public final int quantity;

        public InventoryItem(
                int id,
                String name,
                int quantity
        ) {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
        }
    }
}