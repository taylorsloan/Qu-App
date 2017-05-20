package com.qu.qu.data;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.qu.qu.data.models.Question;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by taylo on 5/18/2017.
 */

public class QuestionManager {

    private DatabaseReference mDatabase;

    public QuestionManager(){
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public DatabaseReference getUserQuestionDatabase(String userId){
        return mDatabase.child("user-questions").child(userId);
    }

    public void createQuestion(Question question, String userId){
        String key = mDatabase.child("questions").push().getKey();
        Map<String, Object> questionValues = question.toMap();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/questions/" + key, questionValues);
        childUpdates.put("/user-questions/" + userId + "/" +key, questionValues);
        mDatabase.updateChildren(childUpdates);
    }

    public Question getRandomQuestion(){
        return null;
    }

    public void incPositiveCount(String questionId){
        incResponseCount(questionId, 1);
    }

    public void incNegativeCount(String questionId){
        incResponseCount(questionId, 2);
    }

    private void incResponseCount(String questionId, final int code){
        // Code 1 = Increment Positive,
        // Code 2 = Increment Negative
        mDatabase.child("questions").child(questionId).runTransaction(new Transaction.Handler() {
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
