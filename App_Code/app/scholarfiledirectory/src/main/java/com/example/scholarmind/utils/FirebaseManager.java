package com.example.scholarmind.utils;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class FirebaseManager {
    private static FirebaseManager instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private FirebaseManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public String getCurrentUserId() {
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }

    // --- USER PROFILE CRUD ---
    public void getUserProfile(String userId, OnSuccessListener<DocumentSnapshot> successListener, OnFailureListener failureListener) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void updateUserProfile(String userId, String fullName, OnCompleteListener<Void> completeListener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        db.collection("users").document(userId).update(updates)
                .addOnCompleteListener(completeListener);
    }

    // --- PAPERS CRUD ---
    public void addPaper(String title, String author, String year, String size, String category, OnSuccessListener<DocumentReference> successListener, OnFailureListener failureListener) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        Map<String, Object> paper = new HashMap<>();
        paper.put("userId", userId);
        paper.put("title", title);
        paper.put("author", author);
        paper.put("year", year);
        paper.put("size", size);
        paper.put("category", category);
        paper.put("uploadDate", FieldValue.serverTimestamp());

        db.collection("papers").add(paper)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void getPapers(OnSuccessListener<QuerySnapshot> successListener, OnFailureListener failureListener) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        db.collection("papers")
                .whereEqualTo("userId", userId)
                .orderBy("uploadDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void deletePaper(String paperId, OnCompleteListener<Void> completeListener) {
        db.collection("papers").document(paperId).delete()
                .addOnCompleteListener(completeListener);
    }

    // --- READING HISTORY CRUD ---
    public void addReadingHistory(String paperId, String title, String author, String year, OnSuccessListener<DocumentReference> successListener, OnFailureListener failureListener) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        Map<String, Object> history = new HashMap<>();
        history.put("userId", userId);
        history.put("paperId", paperId);
        history.put("title", title);
        history.put("author", author);
        history.put("year", year);
        history.put("lastRead", FieldValue.serverTimestamp());

        db.collection("reading_history").add(history)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void getReadingHistory(OnSuccessListener<QuerySnapshot> successListener, OnFailureListener failureListener) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        db.collection("reading_history")
                .whereEqualTo("userId", userId)
                .orderBy("lastRead", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void clearReadingHistory() {
        String userId = getCurrentUserId();
        if (userId == null) return;

        db.collection("reading_history")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        doc.getReference().delete();
                    }
                });
    }

    // --- FLASHCARDS CRUD ---
    public void addFlashcard(String paperId, String question, String answer, OnSuccessListener<DocumentReference> successListener, OnFailureListener failureListener) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        Map<String, Object> card = new HashMap<>();
        card.put("userId", userId);
        card.put("paperId", paperId);
        card.put("question", question);
        card.put("answer", answer);
        card.put("isLearned", false);
        card.put("createdAt", FieldValue.serverTimestamp());

        db.collection("flashcards").add(card)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void getFlashcards(OnSuccessListener<QuerySnapshot> successListener, OnFailureListener failureListener) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        db.collection("flashcards")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void updateFlashcardLearned(String cardId, boolean isLearned, OnCompleteListener<Void> completeListener) {
        db.collection("flashcards").document(cardId)
                .update("isLearned", isLearned)
                .addOnCompleteListener(completeListener);
    }

    public void deleteFlashcard(String cardId, OnCompleteListener<Void> completeListener) {
        db.collection("flashcards").document(cardId).delete()
                .addOnCompleteListener(completeListener);
    }
}
