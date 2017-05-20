package com.qu.qu.data.models;

import java.util.HashMap;

/**
 * Created by Taylor on 3/15/2015.
 */

public class Question {

    String question;
    int positiveCount = 0;
    int negativeCount = 0;

    public Question() {
    }

    public Question(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }

    public int getPositiveCount() {
        return positiveCount;
    }

    public int getNegativeCount() {
        return negativeCount;
    }


    public void setQuestion(String question) {
        this.question = question;
    }

    public void setPositiveCount(int positiveCount) {
        this.positiveCount = positiveCount;
    }

    public void setNegativeCount(int negativeCount) {
        this.negativeCount = negativeCount;
    }

    public int incPositive(){
        positiveCount++;
        return positiveCount;
    }

    public int incNegative(){
        negativeCount++;
        return negativeCount;
    }

    public HashMap<String, Object> toMap(){
        HashMap<String, Object> map = new HashMap<>(3);
        map.put("question", this.question);
        map.put("positiveCount", this.positiveCount);
        map.put("negativeCount", this.negativeCount);
        return map;
    }
}
