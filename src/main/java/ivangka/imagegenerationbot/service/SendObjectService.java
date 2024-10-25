package ivangka.imagegenerationbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class SendObjectService {

    private static final Logger logger = LoggerFactory.getLogger(SendObjectService.class);

    public SendMessage deleteKeyBoard(long chatId, String text) {
        logger.debug("Creating message to delete keyboard for chatId: {}", chatId);
        ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove(true);
        return SendMessage
                .builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .replyMarkup(keyboardRemove)
                .build();
    }

    public SendMessage errorGenerating(long chatId) {
        logger.debug("Creating SendMessage object for chatId: {}", chatId);
        List<KeyboardRow> keyboard = createGenerateAgainKeyboard();
        return SendMessage.builder()
                .chatId(chatId)
                .text("Error generating. Try again.")
                .parseMode("HTML")
                .replyMarkup(ReplyKeyboardMarkup.builder()
                        .keyboard(keyboard)
                        .resizeKeyboard(true)
                        .build())
                .build();
    }

    public SendPhoto sendPhotoObject(long chatId, File image) {
        logger.debug("Creating SendPhoto object for chatId: {}", chatId);
        List<KeyboardRow> keyboard = createGenerateAgainKeyboard();
        return SendPhoto.builder()
                .chatId(chatId)
                .photo(new InputFile(image))
                .replyMarkup(ReplyKeyboardMarkup.builder()
                        .keyboard(keyboard)
                        .resizeKeyboard(true)
                        .build())
                .build();
    }

    private List<KeyboardRow> createGenerateAgainKeyboard() {
        logger.debug("Creating keyboard with button \"Generate again\"");
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow currentRow = new KeyboardRow();
        currentRow.add("Generate again");
        keyboard.add(currentRow);
        return keyboard;
    }

    public SendMessage defaultSendMessageObject(long chatId, String text) {
        logger.debug("Creating default SendMessage object for chatId: {}", chatId);
        return SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .build();
    }

}
