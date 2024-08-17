import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MySpotifyBot extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        return "spotify_merkuzio88_bot";
    }

    @Override
    public String getBotToken() {
        return "7510490643:AAHqA4OpW5L-B1e5ndW6qjnZxIkM5JPfK-s";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();

            if (messageText.startsWith("https://open.spotify.com/track/")) {
                String trackId = messageText.split("/track/")[1].split("\\?")[0];

                String responseJson = sendRequest(trackId);

                if (responseJson != null) {
                    JSONObject response = new JSONObject(responseJson);

                    if (response.getBoolean("success")) {
                        String downloadLink = response.getString("link");

                        String chatId = update.getMessage().getChatId().toString();
                        sendMP3File(downloadLink, chatId, trackId);
                    }
                }
            }
        }
    }

    private String sendRequest(String id) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.spotifydown.com/download/" + id))
                .header("accept", "*/*")
                .header("accept-language", "ru,en;q=0.9")
                .header("origin", "https://spotifydown.com")
                .header("priority", "u=1, i")
                .header("referer", "https://spotifydown.com/")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "same-site")
                .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 YaBrowser/24.7.0.0 Safari/537.36")
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void sendMP3File(String downloadLink, String chatId, String trackId) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadLink))
                .build();

        String tempFileName = "track_" + trackId + ".mp3";

        try {
            HttpResponse<java.nio.file.Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(Paths.get(tempFileName)));

            File filePath = response.body().toFile();
            SendAudio sendAudio = new SendAudio();
            sendAudio.setChatId(chatId);
            sendAudio.setAudio(new InputFile(filePath));

            execute(sendAudio);

            Files.deleteIfExists(filePath.toPath());
        } catch (IOException | InterruptedException | TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
