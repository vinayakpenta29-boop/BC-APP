package com.example.bcapp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper {

    public static DatabaseReference getReference() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid())
                    .child("bc_data");
        }
        // Fallback or handle unauthenticated state gracefully
        return FirebaseDatabase.getInstance().getReference("bc_data_anonymous");
    }

    public static void saveBc(String id, Object bc) {
        getReference().child(id).setValue(bc);
    }

    public static void deleteBc(String id) {
        getReference().child(id).removeValue();
    }
}
