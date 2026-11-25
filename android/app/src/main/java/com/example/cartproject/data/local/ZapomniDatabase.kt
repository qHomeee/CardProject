package com.example.cartproject.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Постоянная база локальных материалов и прогресса. */
@Database(
    entities = [
        DeckEntity::class,
        CardEntity::class,
        ReviewScheduleEntity::class,
        ReviewEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
internal abstract class ZapomniDatabase : RoomDatabase() {
    /** Возвращает доступ к материалам. */
    abstract fun decks(): DeckDao

    /** Возвращает доступ к расписаниям и оценкам. */
    abstract fun reviews(): ReviewDao
}

/** Открывает файловую базу приложения. Имя можно подменить для изолированной проверки. */
internal fun openZapomniDatabase(context: Context, name: String = "zapomni.db"): ZapomniDatabase =
    Room.databaseBuilder(context, ZapomniDatabase::class.java, name).build()
