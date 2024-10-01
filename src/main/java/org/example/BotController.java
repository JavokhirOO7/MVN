package org.example;

import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.example.BotService.*;

public class BotController {

    private static ExecutorService executorService = Executors.newCachedThreadPool();

    public void start() {
        telegramBot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                executorService.execute(() -> {
                    try {
                        handleUpdate(update);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    private void handleUpdate(Update update) {
        if (update.message() != null) {
            Message message = update.message();
            TgUser tgUser = getOrCreateUser(message.chat().id());
            if (message.text() != null) {
                String text = message.text();
                if (text.equals("/start")) {
                    acceptStartSendUsers(tgUser);
                }
            }
        } else if (update.callbackQuery() != null) {
            CallbackQuery callbackQuery = update.callbackQuery();
            TgUser tgUser = getOrCreateUser(callbackQuery.from().id());
            String data = callbackQuery.data();
            if (tgUser.getTgState().equals(TgState.USERS)) {
                acceptUserSendPosts(tgUser, data);
            }
            if (tgUser.getTgState().equals(TgState.POSTS)) {
                acceptPostSendComments(tgUser, data);
            }
        }
    }
}
