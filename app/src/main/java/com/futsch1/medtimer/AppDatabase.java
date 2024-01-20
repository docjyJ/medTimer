package com.futsch1.medtimer;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.futsch1.medtimer.logic.Medicine;
import com.futsch1.medtimer.logic.MedicineDao;
import com.futsch1.medtimer.logic.Reminder;

@Database(entities = {Medicine.class, Reminder.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract MedicineDao medicineDao();
}
