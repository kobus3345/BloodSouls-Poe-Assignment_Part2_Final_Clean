package com.darkforge.bloodsouls

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

class DatabaseHelper(private val context: Context):
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){

    companion object{
        private const val DATABASE_NAME = "UserDatabase.db"
        private const val DATABASE_VERSION = 3
        private const val TABLE_NAME = "data"
        private const val COLUMN_ID = "id"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_FONT_SIZE = "font_size"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = ("CREATE TABLE $TABLE_NAME("+
            "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "$COLUMN_EMAIL TEXT UNIQUE," +
            "$COLUMN_PASSWORD TEXT),"+
            "$COLUMN_FONT_SIZE REAL DEFAULT 14.0)")
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            try {
                db?.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_FONT_SIZE REAL DEFAULT 14.0")
            } catch (e: Exception) {
                db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
                onCreate(db)
            }
        } else {
            val dropTableQuery = "DROP TABLE IF EXISTS $TABLE_NAME"
            db?.execSQL(dropTableQuery)
            onCreate(db)
        }
    }

    fun insertUser(email:String,password: String):Long{
        val values = ContentValues().apply {
            put(COLUMN_EMAIL,email)
            put(COLUMN_PASSWORD,password)
        }
        val db = writableDatabase
        return db.insert(TABLE_NAME,null,values)
    }

    fun readUser(email: String,password: String): Boolean{
        val db = readableDatabase

        val cursor = db.query(TABLE_NAME, arrayOf(COLUMN_ID),
            "$COLUMN_EMAIL = ? AND $COLUMN_PASSWORD = ?",
            arrayOf(email, password), null, null, null)

        val userExists = cursor.moveToFirst()
        cursor.close()
        db.close()
        return userExists
    }

    fun userExists(email: String): Boolean {
        val db = readableDatabase

        val cursor = db.query(TABLE_NAME, arrayOf(COLUMN_ID),
            "$COLUMN_EMAIL = ?",
            arrayOf(email), null, null, null)

        val exists = cursor.moveToFirst()
        cursor.close()
        db.close()
        return exists
    }

    fun updateUserFontSize(email: String, size: Float): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FONT_SIZE, size)
        }
        val rowsAffected = db.update(TABLE_NAME, values, "$COLUMN_EMAIL = ?", arrayOf(email))
        db.close()
        return rowsAffected > 0
    }

    fun getUserFontSize(email: String): Float {
        val db = readableDatabase
        var fontSize = 14.0f
        val cursor = db.query(TABLE_NAME, arrayOf(COLUMN_FONT_SIZE),
            "$COLUMN_EMAIL = ?", arrayOf(email), null, null, null)

        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(COLUMN_FONT_SIZE)
            if (columnIndex != -1) {
                fontSize = cursor.getFloat(columnIndex)
            }
        }
        cursor.close()
        db.close()
        return fontSize
    }
}
