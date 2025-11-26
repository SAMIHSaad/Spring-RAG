package ma.emsi.samih.springrag.telegram;

import jakarta.annotation.PostConstruct;
import ma.emsi.samih.springrag.agents.AIAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);
    
    @Value("${telegram.api.key}")
    private String telegramBotToken;
    private AIAgent aiAgent;

    public TelegramBot(AIAgent aiAgent) {
        this.aiAgent = aiAgent;
    }
    @PostConstruct
    public void registerTelegramBot(){
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpdateReceived(Update telegraRequest) {
        try {
            if(!telegraRequest.hasMessage()) {
                logger.debug("Update has no message");
                return;
            }
            String messageText = telegraRequest.getMessage().getText();
            Long chatId = telegraRequest.getMessage().getChatId();
            logger.info("Received message from chat {}: {}", chatId, messageText);
            
            sendTypingQuestion(chatId);
            String answer = aiAgent.askAgent(messageText);
            logger.info("Generated answer: {}", answer);
            
            if (answer == null || answer.isEmpty()) {
                logger.warn("Answer is null or empty");
                sendTextMessage(chatId, "Je n'ai pas pu générer une réponse");
            } else {
                sendTextMessage(chatId, answer);
            }
        } catch (TelegramApiException e) {
            logger.error("Telegram API error", e);
        } catch (Exception e) {
            logger.error("Error processing update", e);
        }
    }

    @Override
    public String getBotUsername() {
        return "SaadSAMIH23_bot";
    }

    @Override
    public String getBotToken() {
        return telegramBotToken;
    }

    private void sendTextMessage(long chatId, String text) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), text);
        execute(sendMessage);
    }
    private void sendTypingQuestion(long chatId) throws TelegramApiException {
        SendChatAction sendChatAction = new SendChatAction();
        sendChatAction.setChatId(String.valueOf(chatId));
        sendChatAction.setAction(ActionType.TYPING);
        execute(sendChatAction);
    }
}
