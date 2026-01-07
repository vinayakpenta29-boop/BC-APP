package com.example.bcapp;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface BcDao {
    @Query("SELECT * FROM bc_table")
    List<BcEntity> getAll();

    @Insert
    void insertAll(List<BcEntity> bcs);

    @Insert
    void insert(BcEntity bc);

    @Delete
    void delete(BcEntity bc);

    @Query("DELETE FROM bc_table")
    void deleteAll();
}
