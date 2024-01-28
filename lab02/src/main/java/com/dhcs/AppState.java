package com.dhcs;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AppState {

    public class Message {
        private int lifeTime = 0;
        private String text;

        public Message(String text) {
            this.text = text;
        }

        public Object clone() {
            Message copy = new Message(this.text);
            copy.lifeTime = this.lifeTime;
            return (Object) copy;
        }

        void timeStep() {
            lifeTime += 1;
        }

        public String toString() {
            return text;
        }

        public int getLifeTime() {
            return lifeTime;
        }
    }

    private HashMap<String, Message> messages = new HashMap<String, Message>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public void putMessage(String author, String message) {
        lock.writeLock().lock();
        messages.put(author, new Message(message));
        lock.writeLock().unlock();
    }

    public void removeAuthor(String author) {
        lock.writeLock().lock();
        messages.remove(author);
        lock.writeLock().unlock();
    }

    public void timeStep() {
        lock.writeLock().lock();
        for (Message message : messages.values()) {
            message.timeStep();
        }
        lock.writeLock().unlock();
    }

    public HashMap<String, Message> getMessages() {
        HashMap<String, Message> copy = new HashMap<String, Message>(messages.size());

        lock.readLock().lock();
        for (Entry<String, Message> entry : messages.entrySet()) {
            copy.put(entry.getKey(), (Message) entry.getValue().clone());
        }

        lock.readLock().unlock();
        return copy;
    }
}
