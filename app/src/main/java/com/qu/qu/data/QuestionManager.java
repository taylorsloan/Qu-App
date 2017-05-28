package com.qu.qu.data;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.qu.qu.data.models.Question;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import timber.log.Timber;

/**
 * Created by taylo on 5/18/2017.
 */

public class QuestionManager {

    public interface OnReceivedQuestionListener{
        void onReceivedQuestion(DatabaseReference reference);
    }

    private DatabaseReference mDatabase;

    public QuestionManager(){
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public DatabaseReference getUserQuestionDatabase(String userId){
        return mDatabase.child("user-questions").child(userId);
    }

    public DatabaseReference getQuestion(String key){
        return mDatabase.child("questions").child(key);
    }

    public void createQuestion(Question question){
        String key = mDatabase.child("questions").push().getKey();
        Map<String, Object> questionValues = question.toMap();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/questions/" + key, questionValues);
        childUpdates.put("/user-questions/" + question.getUser() + "/" +key, questionValues);
        mDatabase.updateChildren(childUpdates);
    }

    public void removeQuestion(DatabaseReference ref){
        ref.removeValue();
    }

    public void retrieveRandomQuestionKey(final OnReceivedQuestionListener listener){
        DatabaseReference data = mDatabase.child("questions");
        final ThreadLocalRandom random = ThreadLocalRandom.current(); //Random class does not support long type
        data.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Not my finest hour, Firebase cannot get random objects
                long size = dataSnapshot.getChildrenCount();
                Timber.d("Returned %s questions!", size);
                long rand = random.nextLong(size);
                Timber.d("Picking Question #%s", rand);
                long count = 0;
                for(DataSnapshot snapshot: dataSnapshot.getChildren()){
                    if(count == rand){
                        listener.onReceivedQuestion(snapshot.getRef());
                        break;
                    }else {
                        count++;
                    }

                }

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.w(databaseError.toException(), "Retrieve question failed");
            }
        });
    }

    public void incPositiveCount(DatabaseReference ref){
        incResponseCount(ref, 1); // Updates questions child
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Question question = dataSnapshot.getValue(Question.class);
                DatabaseReference userQuestion = getUserQuestionDatabase(question.getUser())
                        .child(dataSnapshot.getKey());
                incResponseCount(userQuestion, 1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.w(databaseError.toException(), "Error Incrementing Positive");
            }
        });
    }

    public void incNegativeCount(DatabaseReference ref){
        incResponseCount(ref, 2);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Question question = dataSnapshot.getValue(Question.class);
                DatabaseReference userQuestion = getUserQuestionDatabase(question.getUser())
                        .child(dataSnapshot.getKey());
                incResponseCount(userQuestion, 2);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.w(databaseError.toException(), "Error Incrementing Negative");
            }
        });
    }

    private void incResponseCount(DatabaseReference ref, final int code){
        // Code 1 = Increment Positive,
        // Code 2 = Increment Negative
        ref.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Question q = mutableData.getValue(Question.class);
                if(q == null){
                    return Transaction.success(mutableData);
                }

                if(code == 1){
                    q.incPositive();
                }else if(code == 2){
                    q.incNegative();
                }
                mutableData.setValue(q);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                Timber.d("Transaction Completed: %s", databaseError);
            }
        });
    }
}
