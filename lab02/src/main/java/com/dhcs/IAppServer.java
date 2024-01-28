package com.dhcs;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAppServer extends Remote {
    public void putMessage(String author, String message) throws RemoteException;
    public void removeAuthor(String author) throws RemoteException;
}
