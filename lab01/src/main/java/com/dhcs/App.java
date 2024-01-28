package com.dhcs;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class App {
    private enum StartType {
        SERVER, CLIENT
    };

    static private class AppState {

        private class Message {
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

        void putMessage(String author, String message) {
            lock.writeLock().lock();
            messages.put(author, new Message(message));
            lock.writeLock().unlock();
        }

        void timeStep() {
            lock.writeLock().lock();
            for (Message message : messages.values()) {
                message.timeStep();
            }
            lock.writeLock().unlock();
        }

        HashMap<String, Message> getMessages() {
            HashMap<String, Message> copy = new HashMap<String, Message>(messages.size());

            lock.readLock().lock();
            for (Entry<String, Message> entry : messages.entrySet()) {
                copy.put(entry.getKey(), (Message) entry.getValue().clone());
            }

            lock.readLock().unlock();
            return copy;
        }
    }

    static private class AppMessagesDashboard extends Thread {
        private AppState appState;

        public AppMessagesDashboard(AppState state) {
            appState = state;
        }

        public void run() {
            while (true) {
                try {
                    sleep(125);
                } catch (InterruptedException e) {
                    return;
                }

                HashMap<String, AppState.Message> messages = appState.getMessages();
                appState.timeStep();
                printDashboard(messages);
            }
        }

        private void printDashboard(HashMap<String, AppState.Message> messages) {
            // clear screen
            System.out.print("\033[H\033[2J");
            System.out.print("+  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  --|==1line==chat====>-  +\n");
            if (messages.size() == 0) {
                System.out.print("+                                 NO CLIENTS                                  +\n");
            } else {
                for (Entry<String, AppState.Message> entry : messages.entrySet()) {
                    printEntry(entry.getKey(), entry.getValue());
                }
            }

            System.out.print("+  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  +\n");
            System.out.flush();
        }

        private void printEntry(String author, AppState.Message message) {
            System.out.printf("+ [%-10s]: ", author.substring(0, Math.min(author.length(), 10)));
            printMessage(message);
            System.out.printf(" |\n");
        }

        private void printMessage(AppState.Message message) {
            int rest = 61;
            String color = "";

            if (message.getLifeTime() < 1) {
                System.out.printf("        ");
                rest -= 8;
            } else if (message.getLifeTime() < 2) {
                System.out.printf("  ");
                rest -= 2;
            } else

            if (message.getLifeTime() < 12) {
                color = "\033[1;32m";
            }
            if (message.getLifeTime() < 24) {
                color = "\033[32m";
            }
            String body = message.toString();
            body = body.substring(0, Math.min(body.length(), rest));
            String fmt = "%s%-" + String.format("%d", rest) + "s\033[0m";
            System.out.print(String.format(fmt, color, body));
        }
    }

    static private class AppServer {
        private ServerSocket serverSocket;
        private AppState state = new AppState();

        void start(int port) {
            try {
                serverSocket = new ServerSocket(port);
            } catch (IOException e) {
                System.err.printf("ServerSocket: %s\n", e.toString());
                return;
            }

            new AppMessagesDashboard(state).start();

            while (true) {
                try {
                    new ClientHandler(serverSocket.accept(), state).start();
                } catch (IOException e) {
                    System.err.printf("ClientHandler.clientSocket.getInputStream: %s\n", e.toString());
                }
            }
        }

        private class ClientHandler extends Thread {
            private Socket clientSocket;
            private AppState appState;
            private BufferedReader in;

            public ClientHandler(Socket socket, AppState state) {
                clientSocket = socket;
                appState = state;
            }

            public void run() {
                try {
                    in = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream()));
                } catch (IOException e) {
                    System.err.printf("ClientHandler.clientSocket.getInputStream: %s\n", e.toString());
                }

                if (in != null) {
                    process();
                    try {
                        in.close();
                    } catch (IOException e) {
                        System.err.printf("ClientHandler.in.close: %s\n", e.toString());
                    }
                }

                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.printf("ClientHandler.clientSocket.close: %s\n", e.toString());
                }
            }

            private void process() {
                while (true) {
                    ArrayList<String> data;
                    try {
                        data = readRequest();
                    } catch (IOException e) {
                        System.err.printf("ClientHandler.readNext: %s\n", e.toString());
                        return;
                    }

                    if (data.size() == 0) {
                        return;
                    }

                    String method = data.remove(0);
                    if (method.equals("")) {
                        continue;
                    }
                    if (method.equals("putMessage")) {
                        if (data.size() == 2) {
                            appState.putMessage(data.get(0), data.get(1));
                        } else {
                            System.err.printf(
                                    "ClientHandler.process: wrong number of arguments: expected 2 but receive %d\n",
                                    data.size());
                        }
                    } else {
                        System.err.printf("ClientHandler.process: Unknown method: %s\n", method);
                    }
                }
            }

            private ArrayList<String> readRequest() throws IOException {
                ArrayList<String> data = new ArrayList<String>();

                while (true) {
                    String line = in.readLine();
                    if (line == null) {
                        break;
                    }

                    if (line.equals("")) {
                        break;
                    }

                    data.add(line);
                }

                return data;
            }
        }
    }

    private static class AppClientBuilder {
        private BufferedReader in;
        private String name;
        private String address;
        private int port;

        public AppClientBuilder(BufferedReader in) {
            this.in = in;
        }

        public AppClientBuilder setPort(int port) {
            this.port = port;
            return this;
        }

        public AppClientBuilder setAddress(String address) {
            this.address = address;
            return this;
        }

        public AppClientBuilder askUser() throws IOException {
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

    private static class AppClient {
        private BufferedReader in;
        private String name;
        private String address;
        private int port;

        private Socket clientSocket;
        private PrintWriter out;

        public AppClient(BufferedReader in, String name, String address, int port) {
            this.in = in;
            this.name = name;
            this.address = address;
            this.port = port;
        }

        public void start() {
            try {
                clientSocket = new Socket(address, port);
            } catch (UnknownHostException e) {
                e.printStackTrace(System.err);
                return;
            } catch (IOException e) {
                e.printStackTrace(System.err);
                return;
            }

            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }

            if (out != null) {
                process();
                sendQuit();
                out.close();
            }

            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }

        private void process() {
            System.out.println("Соединение установлено");
            System.out.println("Введите сообщение или CTRL+C, чтобы завершить сеанс");
            while (true) {
                System.out.print("> ");
                String userMessage;
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

                out.println("putMessage");
                out.println(name);
                out.println(userMessage);
                out.println();
            }
        }

        private void sendQuit() {
            out.println("removeAuthor");
            out.println(name);
            out.println();
        }
    }

    static void printHeader() {
        System.out.println(" +==================================+");
        System.out.println("||              _РВВС_              ||");
        System.out.println("||      Лабораторная работа №1      ||");
        System.out.println("||      --|==1line==chat====>       ||");
        System.out.println(" +==================================+");
        System.out.println("");
        System.out.println("");
        System.out.println("");
    }

    static Optional<StartType> selectStartType(BufferedReader stdinReader) {
        System.out.println("Что хотите запустить?");
        System.out.println("1. сервер");
        System.out.println("2. клиент");
        System.out.println("q. выйти");

        while (true) {
            System.out.print("> ");
            String choice;
            try {
                choice = stdinReader.readLine();
            } catch (IOException e) {
                return Optional.empty();
            }

            if (choice == null || choice.equals("q")) {
                return Optional.empty();
            }
            if (choice.equals("1")) {
                return Optional.of(StartType.SERVER);
            }
            if (choice.equals("2")) {
                return Optional.of(StartType.CLIENT);
            }

            System.out.println(
                    String.format("Вы ввели \"%s\", что не представлено в списке. Введите число или q", choice));
        }
    }

    public static void main(String[] args) {
        BufferedReader stdinReader = new BufferedReader(new InputStreamReader(System.in));

        printHeader();
        Optional<StartType> userChoice = selectStartType(stdinReader);

        if (userChoice.isEmpty()) {
            return;
        }
        switch (userChoice.get()) {
            case SERVER:
                new AppServer().start(3001);
                break;
            case CLIENT:
                try {
                    new AppClientBuilder(stdinReader).setAddress("127.0.0.1").setPort(3001).askUser().build().start();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                break;
        }
    }
}
