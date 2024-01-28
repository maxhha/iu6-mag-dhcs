package com.dhcs;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class AppServer extends UnicastRemoteObject implements IAppServer {
    private static final long serialVersionUID = 1L;
    private AppState state = new AppState();

    protected AppServer() throws RemoteException {}

    public static void main(String[] args) throws RemoteException {
        AppServer s = new AppServer();
        Registry re = LocateRegistry.createRegistry(3001);
        re.rebind("server", s);

        new AppMessagesDashboard(s.state).start();
    }

    @Override
    public void putMessage(String author, String message) throws RemoteException {
        state.putMessage(author, message);
    }

    @Override
    public void removeAuthor(String author) throws RemoteException {
        state.removeAuthor(author);
    }
}
