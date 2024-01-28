package com.dhcs;

import java.util.HashMap;
import java.util.Map.Entry;

public class AppMessagesDashboard extends Thread {
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
