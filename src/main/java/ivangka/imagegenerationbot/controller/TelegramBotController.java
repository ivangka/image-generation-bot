package ivangka.imagegenerationbot.controller;

import ivangka.imagegenerationbot.config.SecurityConfig;
import ivangka.imagegenerationbot.model.UserState;
import ivangka.imagegenerationbot.service.ImageGenerationService;
import ivangka.imagegenerationbot.service.SendObjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class TelegramBotController implements LongPollingSingleThreadUpdateConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotController.class);

    private final TelegramClient telegramClient;
    private final SendObjectService sendObjectService;
    private final SecurityConfig securityConfig;
    private final ImageGenerationService imageGenerationService;
    private final Map<Long, UserState> userStates;

    @Autowired
    public TelegramBotController(TelegramClient telegramClient,
                                 SendObjectService sendObjectService,
                                 SecurityConfig securityConfig,
                                 ConcurrentHashMap<Long, UserState> userStates,
                                 ImageGenerationService imageGenerationService) {

        this.telegramClient = telegramClient;
        this.sendObjectService = sendObjectService;
        this.securityConfig = securityConfig;
        this.userStates = userStates;
        this.imageGenerationService = imageGenerationService;

    }

    @Override
    public void consume(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            logger.debug("[User ID: {}] Processing message from user: {}", update.getMessage().getFrom().getId(),
                    update.getMessage().getFrom().getId());

            // inits
            User user = update.getMessage().getFrom();
            long userId = user.getId();
            String messageFromUserText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            SendMessage sendMessage = null;
            SendPhoto sendPhoto;
            UserState userState = userStates.getOrDefault(userId, new UserState(userId));

            // access check
            if (!securityConfig.isUserAllowed(userId)) {
                logger.warn("[User ID: {}] User {} is not allowed to access the resource", userId, userId);
                sendMessage = sendObjectService.defaultSendMessageObject(chatId,
                        "You don't have access to this bot.");
                execute(sendMessage);
                return;
            }

            userStates.put(userId, userState);
            logger.debug("[User ID: {}] User state for user {}: {}", userState.getId(), userId, userState);

            switch (messageFromUserText) {

                case "/start":
                    logger.debug("[User ID: {}] Handling /start command", userId);
                    userState.resetFields();
                    sendMessage = sendObjectService.deleteKeyBoard(chatId, "Write a prompt to generate an image.");
                    execute(sendMessage);
                    logger.info("[User ID: {}] Sent hello-message to chat ID: {}",
                            userState.getId(), chatId);
                    break;

                case "Generate again":
                    logger.debug("[User ID: {}] Handling 'Generate again' command", userId);
                    if (userState.getPrompt() == null) {
                        logger.debug("[User ID: {}] No prompt found for image generation", userId);
                        sendMessage = sendObjectService.defaultSendMessageObject(chatId,
                                "You didn't write a prompt.");
                        execute(sendMessage);
                    } else {
                        try {
                            logger.info("[User ID: {}] Generating image for prompt: {}", userId, userState.getPrompt());
                            sendMessage = sendObjectService.defaultSendMessageObject(chatId,
                                    "Wait, the image is being generated...");
                            execute(sendMessage);
                            File image = imageGenerationService.generateImage(userId, userState.getPrompt());
                            if (image == null) {
                                logger.error("[User ID: {}] Failed to generate image", userId);
                                sendMessage = sendObjectService.defaultSendMessageObject(chatId,
                                        "Error generating. Try again.");
                                execute(sendMessage);
                            } else {
                                logger.info("[User ID: {}] Image generated successfully", userId);
                                sendPhoto = sendObjectService.sendPhotoObject(chatId, image);
                                execute(sendPhoto);
                            }
                        } catch (IOException | InterruptedException e) {
                            logger.error("[User ID: {}] Error during image generation: {}", userId, e.getMessage(), e);
                            sendMessage = sendObjectService.defaultSendMessageObject(chatId,
                                    "Error generating. Try again.");
                            execute(sendMessage);
                        }
                    }
                    break;

                default: // user wrote a prompt
                    logger.debug("[User ID: {}] User {} wrote a prompt", userState.getId(), userId);
                    try {
                        logger.info("[User ID: {}] Setting prompt for user: {}", userId, messageFromUserText);
                        userState.setPrompt(messageFromUserText);
                        sendMessage = sendObjectService.defaultSendMessageObject(chatId,
                                "Wait, the image is being generated...");
                        execute(sendMessage);
                        File image = imageGenerationService.generateImage(userId, messageFromUserText);
                        if (image == null) {
                            logger.error("[User ID: {}] Image generation returned null", userId);
                            sendMessage = sendObjectService.errorGenerating(chatId);
                            execute(sendMessage);
                        } else {
                            logger.info("[User ID: {}] Image generated and will be sent to user", userId);
                            sendPhoto = sendObjectService.sendPhotoObject(chatId, image);
                            execute(sendPhoto);
                        }
                    } catch (IOException | InterruptedException e) {
                        logger.error("[User ID: {}] Error while processing prompt: {}", userId, e.getMessage(), e);
                        sendMessage = sendObjectService.errorGenerating(chatId);
                        execute(sendMessage);
                    }
                    break;

            }

        }

    }

    // execute text message
    private void execute(SendMessage sendMessage) {
        try {
            telegramClient.execute(sendMessage);
            logger.debug("Message sent successfully: {}", sendMessage.getText());
        } catch (TelegramApiException e) {
            logger.error("Telegram API error: {}", e.getMessage(), e);
        }
    }

    // execute message with photo
    private void execute(SendPhoto sendPhoto) {
        try {
            telegramClient.execute(sendPhoto);
            logger.debug("Message with image ({}) sent successfully: {}", sendPhoto.getPhoto().getMediaName(),
                    sendPhoto.getCaption());
        } catch (TelegramApiException e) {
            logger.error("Telegram API error: {}", e.getMessage(), e);
        }
    }

}
