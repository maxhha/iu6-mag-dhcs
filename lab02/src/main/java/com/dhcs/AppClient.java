package com.dhcs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class AppClient {
    private BufferedReader in;
    private String name;
    private String address;
    private int port;

    private IAppServer server;


    private static class Builder {
        private BufferedReader in;
        private String name;
        private String address;
        private int port;

        public Builder(BufferedReader in) {
            this.in = in;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setAddress(String address) {
            this.address = address;
            return this;
        }

        public Builder askUser() throws IOException {
            if (name == null) {
                this.askName();
            }

            return this;
        }

        private void askName() throws IOException {
            System.out.println("Введите имя:");
            while (true) {
                System.out.print("> ");
                name = in.readLine();
                if (name != null || !name.equals("")) {
                    return;
                }
            }
        }

        public AppClient build() {
            return new AppClient(in, name, address, port);
        }
    }

    private class ShutdownHook extends Thread {
        private AppClient client;

        ShutdownHook(AppClient c) {
            client = c;
        }

        @Override
        public void run() {
            client.sendQuit();
        }
    }


    public AppClient(BufferedReader in, String name, String address, int port) {
        this.in = in;
        this.name = name;
        this.address = address;
        this.port = port;
    }

    public void start() {
        try {
            Registry re = LocateRegistry.getRegistry(address, port);
            server = (IAppServer) re.lookup("server");
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
        process();
    }



    private void process() {
        System.out.println("Соединение установлено");
        System.out.println("Введите сообщение или CTRL+C, чтобы завершить сеанс");
        while (true) {
            System.out.print("> ");
            String userMessage = null;
            try {
                userMessage = in.readLine();
            } catch (InterruptedIOException e) {
                System.out.println();
                return;
            } catch (IOException e) {
                System.out.println();
                e.printStackTrace(System.err);
                return;
            }

            if (userMessage == null) {
                System.out.println();
                return;
            }

            if (userMessage.equals("")) {
                continue;
            }

            try {
                server.putMessage(name, userMessage);
            } catch (RemoteException e) {
                e.printStackTrace(System.err);
                continue;
            }
        }
    }

    private void sendQuit() {
        System.out.println();
        try {
            server.removeAuthor(name);
        } catch (RemoteException e) {
            e.printStackTrace(System.err);
        }
    }


    static void printHeader() {
        System.out.println(" +==================================+");
        System.out.println("||              _РВВС_              ||");
        System.out.println("||      Лабораторная работа №2      ||");
        System.out.println("||               RMI                ||");
        System.out.println("||      --|==1line==chat====>       ||");
        System.out.println(" +==================================+");
        System.out.println("");
        System.out.println("");
        System.out.println("");
    }

    public static void main(String[] args) {
        BufferedReader stdinReader = new BufferedReader(new InputStreamReader(System.in));

        printHeader();
        try {
            new Builder(stdinReader).setAddress("127.0.0.1").setPort(3001).askUser().build().start();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
