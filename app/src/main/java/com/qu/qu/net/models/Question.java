package com.qu.qu.net.models;

/**
 * Created by Taylor on 3/15/2015.
 */

public class Question {

    int id;
    String question;
    int positive_count;
    int negative_count;

    public Question() {
    }

    public Question(String question) {
        this.question = question;
    }

    public int getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public int getPositiveCount() {
        return positive_count;
    }

    public int getNegativeCount() {
        return negative_count;
    }
}
