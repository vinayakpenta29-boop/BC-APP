package com.example.bcapp;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper {

    private static final DatabaseReference dbRef =
            FirebaseDatabase.getInstance()
                    .getReference("bc_data");

    // SAVE DATA
    public static void saveBc(String id, Object bc) {
        dbRef.child(id).setValue(bc);
    }

    // DELETE DATA
    public static void deleteBc(String id) {
        dbRef.child(id).removeValue();
    }

    public static DatabaseReference getReference() {
        return dbRef;
    }
}
