package com.hadoga.hadoga.utils;

import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseService {
    private static FirebaseFirestore db;

    private FirebaseService() {}

    public static FirebaseFirestore getInstance() {
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }
        return db;
    }
}
