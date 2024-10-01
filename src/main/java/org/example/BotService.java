package org.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.SneakyThrows;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BotService {
    public static TelegramBot telegramBot = new TelegramBot("7433655943:AAEWaiR2hvV0NZsYTecwR2TzzeA9GI9RxHU");

    public static TgUser getOrCreateUser(Long chatId) {
        for (TgUser user : DB.USERS) {
            if (user.getChatId().equals(chatId)) {
                return user;
            }
        }
        TgUser tgUser = new TgUser();
        tgUser.setChatId(chatId);
        DB.USERS.add(tgUser);
        return tgUser;
    }

    public static void acceptStartSendUsers(TgUser tgUser) {
        List<User> users = giveUsers();
        SendMessage sendMessage = new SendMessage(tgUser.getChatId(), "Choose:");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        for (User user : users) {
            InlineKeyboardButton button1 = new InlineKeyboardButton(user.getName() + " " + user.getId()).callbackData("posts " + user.getId() + 1000);
            InlineKeyboardButton button2 = new InlineKeyboardButton("posts").callbackData("" + user.getId());
            markup.addRow(button1, button2);
        }
        sendMessage.replyMarkup(markup);
        telegramBot.execute(sendMessage);
        tgUser.setTgState(TgState.USERS);
    }

    @SneakyThrows
    private static List<User> giveUsers() {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://jsonplaceholder.typicode.com/users"))
                .GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String str = response.body();
        Gson gson = new Gson();
        return gson.fromJson(str, new TypeToken<List<User>>() {
        }.getType());
    }

    public static void acceptUserSendPosts(TgUser tgUser, String data) {
        if (data.equals("back")) {
            sendBackToUsers(tgUser);
        }
        List<User> users = giveUsers();
        User user = findSelectedUser(data, users);
        tgUser.setTempUser(user);
        List<Post> posts = givePostsOfSelectedUser(user);
        SendMessage sendMessage = new SendMessage(tgUser.getChatId(), "Choose:");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        for (Post post : posts) {
            InlineKeyboardButton button1 = new InlineKeyboardButton(post.getTitle() + " " + post.userId).callbackData(post.id.toString() + 1000);
            InlineKeyboardButton button2 = new InlineKeyboardButton("Comments").callbackData(post.id.toString());
            markup.addRow(button1, button2);
        }
        markup.addRow(new InlineKeyboardButton("back").callbackData("back"));
        sendMessage.replyMarkup(markup);
        telegramBot.execute(sendMessage);
        tgUser.setTgState(TgState.POSTS);
    }

    private static void sendBackToUsers(TgUser tgUser) {
        SendMessage sendMessage = new SendMessage(tgUser.getChatId(), "back!");
        telegramBot.execute(sendMessage);
        tgUser.setTgState(TgState.USERS);
        acceptStartSendUsers(tgUser);
    }

    @SneakyThrows
    private static List<Post> givePostsOfSelectedUser(User user) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://jsonplaceholder.typicode.com/posts"))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        String str = response.body();
        Gson gson = new Gson();
        List<Post> posts = gson.fromJson(str, new TypeToken<List<Post>>() {
        }.getType());
        return posts.stream()
                .filter(post -> post.userId.equals(user.getId()))
                .toList();
    }

    private static User findSelectedUser(String data, List<User> users) {
        Integer tempId = Integer.valueOf(data);
        Optional<User> first = users.stream()
                .filter(user -> Objects.equals(user.getId(), tempId))
                .findFirst();
        return first.orElse(null);
    }

    public static void acceptPostSendComments(TgUser tgUser, String data) {
        if (data.equals("back")){
            sendBackToPosts(tgUser);
        }
        List<Post> posts = givePostsOfSelectedUser(tgUser.getTempUser());
        Post post = findSelectedPost(data, posts);
        List<Comment> comments = giveCommentsOfCurrentPost(post);
        SendMessage sendMessage = new SendMessage(tgUser.getChatId(), "Choose:");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        for (Comment comment : comments) {
            InlineKeyboardButton button1 = new InlineKeyboardButton(post.getTitle() + " " + comment.id).callbackData(comment.id.toString() + 1000);
            InlineKeyboardButton button2 = new InlineKeyboardButton("done").callbackData(comment.id.toString());
            markup.addRow(button1, button2);
        }
        markup.addRow(new InlineKeyboardButton("back").callbackData("back"));
        sendMessage.replyMarkup(markup);
        telegramBot.execute(sendMessage);
        tgUser.setTgState(TgState.COMMENTS);
    }

    private static void sendBackToPosts(TgUser tgUser) {
        SendMessage sendMessage = new SendMessage(tgUser.getChatId(), "back!");
        telegramBot.execute(sendMessage);
        tgUser.setTgState(TgState.POSTS);
        acceptStartSendUsers(tgUser);
    }

    @SneakyThrows
    private static List<Comment> giveCommentsOfCurrentPost(Post post) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://jsonplaceholder.typicode.com/comments"))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        String str = response.body();
        Gson gson = new Gson();
        List<Comment> comments = gson.fromJson(str, new TypeToken<List<Comment>>() {
        }.getType());
        return comments.stream()
                .filter(comment -> comment.postId.equals(post.getId()))
                .toList();
    }

    private static Post findSelectedPost(String data, List<Post> posts) {
        Integer tempId = Integer.valueOf(data);
        Optional<Post> first = posts.stream()
                .filter(post -> Objects.equals(post.getId(), tempId))
                .findFirst();
        return first.orElse(null);
    }
}
