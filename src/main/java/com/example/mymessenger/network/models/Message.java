package com.example.mymessenger.network.models;

import java.io.Serializable;
import java.util.Date;

public class Message implements Serializable {

    private final MessageType type;
    private final String userName;
    private final Date date;
    private final String text;
    private static final long serialVersionUID = 6529685098267757690L;


    //Constructors
    public Message(MessageType type, String userName, Date date, String text) {
        this.type = type;
        this.userName = userName;
        this.text = text;
        this.date = date;
    }

    public Message(MessageType type, String userName, Date date) {
        this.type = type;
        this.userName = userName;
        this.text = null;
        this.date = date;
    }


    //getters
    public MessageType getType() {
        return type;
    }
    public String getUserName() { return userName; }
    public String getText() {
        return text;
    }
    public Date getDate() { return date; }
}