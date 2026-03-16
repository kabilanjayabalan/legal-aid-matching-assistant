package com.legalaid.backend.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.legalaid.backend.dto.ChatSummaryDto;
import com.legalaid.backend.model.ChatFileAttachment;
import com.legalaid.backend.model.ChatMessage;
import com.legalaid.backend.model.LawyerProfile;
import com.legalaid.backend.model.Match;
import com.legalaid.backend.model.MatchStatus;
import com.legalaid.backend.model.NGOProfile;
import com.legalaid.backend.model.ProviderType;
import com.legalaid.backend.model.Role;
import com.legalaid.backend.model.User;
import com.legalaid.backend.repository.ChatFileAttachmentRepository;
import com.legalaid.backend.repository.ChatRepository;
import com.legalaid.backend.repository.LawyerProfileRepository;
import com.legalaid.backend.repository.MatchRepository;
import com.legalaid.backend.repository.NGOProfileRepository;
import com.legalaid.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatFileAttachmentRepository chatFileAttachmentRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final LawyerProfileRepository lawyerProfileRepository;
    private final NGOProfileRepository ngoProfileRepository;
    private final NotificationService notificationService;

    public List<ChatSummaryDto> getCitizenChats(String citizenEmail) {

    User citizen = userRepository.findByEmail(citizenEmail)
            .orElseThrow(() -> new RuntimeException("Citizen not found"));

    // 1️⃣ All matches created by this citizen
    List<Match> matches =
            matchRepository.findByCaseObjCreatedById(citizen.getId());

    if (matches.isEmpty()) return List.of();

    List<Integer> matchIds = matches.stream()
            .map(Match::getId)
            .toList();

    // 2️⃣ Last message per match
    List<ChatMessage> lastMessages =
            chatRepository.findLastMessagesByMatchIds(matchIds);

    // 3️⃣ Build UI DTO
    return lastMessages.stream()
        .map(chat -> {

            Integer matchId = chat.getMatchId();

            Match match = matches.stream()
                    .filter(m -> m.getId().equals(matchId))
                    .findFirst()
                    .orElseThrow(() ->
                            new RuntimeException("Match not found for id " + matchId)
                    );

            User otherUser;
            String displayName;

            if (match.getProviderType() == ProviderType.LAWYER) {

                LawyerProfile lawyerProfile = lawyerProfileRepository
                        .findById(match.getProviderId())
                        .orElseThrow(() -> new RuntimeException("Lawyer profile not found"));

                otherUser = lawyerProfile.getUser();
                displayName = lawyerProfile.getName();

            } else if (match.getProviderType() == ProviderType.NGO) {

                NGOProfile ngoProfile = ngoProfileRepository
                        .findById(match.getProviderId())
                        .orElseThrow(() -> new RuntimeException("NGO profile not found"));

                otherUser = ngoProfile.getUser();
                displayName = ngoProfile.getNgoName();

            } else {
                throw new RuntimeException("Unsupported provider type");
            }

            return new ChatSummaryDto(
                    matchId,
                    displayName,
                    otherUser.getRole().name(),
                    otherUser.getEmail(),
                    match.getCaseObj().getTitle(),
                    chat.getMessage(),
                    chat.getCreatedAt() // ✅ maps to lastMessageAt
            );
        })
        .sorted((a, b) -> b.getLastMessageAt().compareTo(a.getLastMessageAt()))
        .collect(Collectors.toList());
    }

    public List<ChatSummaryDto> getProviderChats(String providerEmail) {

        User provider = userRepository.findByEmail(providerEmail)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        // 1️⃣ Resolve provider profile ID based on role
        Integer providerProfileId = null;
        ProviderType providerType = null;

        if (provider.getRole() == Role.LAWYER) {
            LawyerProfile lawyerProfile = lawyerProfileRepository
                    .findByUser(provider)
                    .orElseThrow(() -> new RuntimeException("Lawyer profile not found"));
            providerProfileId = lawyerProfile.getId();
            providerType = ProviderType.LAWYER;
        } else if (provider.getRole() == Role.NGO) {
            NGOProfile ngoProfile = ngoProfileRepository
                    .findByUser(provider)
                    .orElseThrow(() -> new RuntimeException("NGO profile not found"));
            providerProfileId = ngoProfile.getId();
            providerType = ProviderType.NGO;
        } else {
            throw new RuntimeException("User is not a provider (lawyer or NGO)");
        }

        // 2️⃣ Find all matches where this provider is assigned and confirmed
        List<Match> matches = matchRepository.findByProviderTypeAndProviderIdAndStatus(
                providerType,
                providerProfileId,
                MatchStatus.PROVIDER_CONFIRMED
        );

        if (matches.isEmpty()) return List.of();

        List<Integer> matchIds = matches.stream()
                .map(Match::getId)
                .toList();

        // 3️⃣ Last message per match
        List<ChatMessage> lastMessages =
                chatRepository.findLastMessagesByMatchIds(matchIds);

        // 4️⃣ Build UI DTO - for provider, "other user" is the citizen
        return lastMessages.stream()
            .map(chat -> {

                Integer matchId = chat.getMatchId();

                Match match = matches.stream()
                        .filter(m -> m.getId().equals(matchId))
                        .findFirst()
                        .orElseThrow(() ->
                                new RuntimeException("Match not found for id " + matchId)
                        );

                // For provider, the "other user" is the citizen who created the case
                User citizen = match.getCaseObj().getCreatedBy();
                String displayName = citizen.getFullName() != null && !citizen.getFullName().isEmpty()
                        ? citizen.getFullName()
                        : citizen.getEmail();

                return new ChatSummaryDto(
                        matchId,
                        displayName,
                        citizen.getRole().name(),
                        citizen.getEmail(),
                        match.getCaseObj().getTitle(),
                        chat.getMessage(),
                        chat.getCreatedAt() // ✅ maps to lastMessageAt
                );
            })
            .sorted((a, b) -> b.getLastMessageAt().compareTo(a.getLastMessageAt()))
            .collect(Collectors.toList());
    }


    /* ===================== LOAD CHAT HISTORY ===================== */

    public List<ChatMessage> loadChats(Integer matchId, String username) {

        Integer senderId = resolveSenderId(username);
        validateSender(matchId, senderId);

        return chatRepository.findByMatchIdOrderByCreatedAtAsc(matchId);
    }

    /* ===================== SAVE MESSAGE ===================== */

    @Transactional
    public ChatMessage saveMessage(Integer matchId, String username, String message) {

        Integer senderId = resolveSenderId(username);
        validateSender(matchId, senderId);

        ChatMessage chat = ChatMessage.builder()
                .matchId(matchId)
                .senderId(senderId)
                .message(message)
                .build();

        ChatMessage saved = chatRepository.save(chat);

        // 🔔 CHAT NOTIFICATION (Similar to Appointment Notification)
        Integer receiverUserId = findReceiverUserId(matchId, senderId);
        if (receiverUserId != null) {
            notificationService.notifyUser(
                    receiverUserId.longValue(),
                    "MESSAGE",
                    "New message received",
                    String.valueOf(matchId)
            );
            log.info("Chat notification sent to user {} for match {}", receiverUserId, matchId);
        }

        return saved;
    }

    /* ===================== USER RESOLUTION ===================== */

    private Integer resolveSenderId(String username) {

        User user = userRepository.findByEmail(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found for username: " + username)
                );

        return user.getId();
    }

    /* ===================== AUTHORIZATION ===================== */

    private void validateSender(Integer matchId, Integer senderId) {

        Match match = matchRepository.findById(matchId)
            .orElseThrow(() -> new RuntimeException("Match not found"));

        // Chat only after provider confirmation
        if (match.getStatus() != MatchStatus.PROVIDER_CONFIRMED) {
            throw new RuntimeException("Chat not allowed. Match not confirmed");
        }

        // Citizen USER ID
        Integer citizenUserId =
            match.getCaseObj().getCreatedBy().getId();

        // Resolve sender USER
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Integer senderProfileId = null;

        //  Resolve PROFILE ID based on role
        if (sender.getRole() == Role.LAWYER) {

            senderProfileId = lawyerProfileRepository
                .findByUser(sender)
                .orElseThrow(() -> new RuntimeException("Lawyer profile not found"))
                .getId();

        } else if (sender.getRole() == Role.NGO) {

            senderProfileId = ngoProfileRepository
                .findByUser(sender)
                .orElseThrow(() -> new RuntimeException("NGO profile not found"))
                .getId();
        }

        //  Provider PROFILE ID stored in match
        Integer matchProviderProfileId = match.getProviderId();

        //  DEBUG (optional – good for now)
        System.out.println(
            "CHAT VALIDATION → matchId=" + matchId
            + ", senderUserId=" + senderId
            + ", senderProfileId=" + senderProfileId
            + ", senderRole=" + sender.getRole()
            + ", citizenUserId=" + citizenUserId
            + ", providerProfileId=" + matchProviderProfileId
            + ", status=" + match.getStatus()
        );

        // FINAL AUTHORIZATION CHECK
        boolean isCitizen = senderId.equals(citizenUserId);
        boolean isProvider =
            senderProfileId != null
            && senderProfileId.equals(matchProviderProfileId);

        if (!isCitizen && !isProvider) {
        throw new RuntimeException("Unauthorized chat access");
        }
    }
    private String resolveProviderUserEmail(Match match) {

        if (match.getProviderType() == ProviderType.LAWYER) {

            return lawyerProfileRepository
                    .findById(match.getProviderId())
                    .orElseThrow(() -> new RuntimeException("Lawyer profile not found"))
                    .getUser()
                    .getEmail();

        } else if (match.getProviderType() == ProviderType.NGO) {

            return ngoProfileRepository
                    .findById(match.getProviderId())
                    .orElseThrow(() -> new RuntimeException("NGO profile not found"))
                    .getUser()
                    .getEmail();
        }

        throw new RuntimeException("Invalid provider type");
    }

    /* ===================== FIND RECEIVER USER ID ===================== */

    private Integer findReceiverUserId(Integer matchId, Integer senderId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        Integer citizenUserId = match.getCaseObj().getCreatedBy().getId();

        // If sender is citizen, receiver is provider
        if (senderId.equals(citizenUserId)) {
            if (match.getProviderType() == ProviderType.LAWYER) {
                LawyerProfile lawyerProfile = lawyerProfileRepository
                        .findById(match.getProviderId())
                        .orElseThrow(() -> new RuntimeException("Lawyer profile not found"));
                return lawyerProfile.getUser().getId();
            } else if (match.getProviderType() == ProviderType.NGO) {
                NGOProfile ngoProfile = ngoProfileRepository
                        .findById(match.getProviderId())
                        .orElseThrow(() -> new RuntimeException("NGO profile not found"));
                return ngoProfile.getUser().getId();
            }
        } else {
            // If sender is provider, receiver is citizen
            return citizenUserId;
        }

        return null;
    }

    /* ===================== SAVE MESSAGE WITH FILE ===================== */

    @Transactional
    public ChatMessage saveMessageWithFile(Integer matchId, String username, String message, MultipartFile file) {
        Integer senderId = resolveSenderId(username);
        validateSender(matchId, senderId);

        // Use default message if empty
        String messageText = (message == null || message.trim().isEmpty()) 
            ? "📎 " + file.getOriginalFilename() 
            : message;

        ChatMessage chat = ChatMessage.builder()
                .matchId(matchId)
                .senderId(senderId)
                .message(messageText)
                .fileAttachments(new ArrayList<>())
                .build();

        ChatMessage saved = chatRepository.save(chat);
        Long savedMessageId = saved.getId();

        // Save file attachment
        try {
            ChatFileAttachment fileAttachment = ChatFileAttachment.builder()
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .data(file.getBytes())
                    .chatMessage(saved)
                    .build();

            chatFileAttachmentRepository.save(fileAttachment);
        } catch (IOException e) {
            log.error("Failed to save file attachment", e);
            throw new RuntimeException("Failed to save file attachment: " + e.getMessage());
        }

        // Reload the message with file attachments to ensure they're included in the response
        // Use the query that eagerly loads file attachments
        List<ChatMessage> reloaded = chatRepository.findByMatchIdOrderByCreatedAtAsc(matchId);
        ChatMessage messageWithFiles = reloaded.stream()
                .filter(m -> m.getId().equals(savedMessageId))
                .findFirst()
                .orElse(saved);

        // 🔔 CHAT NOTIFICATION
        Integer receiverUserId = findReceiverUserId(matchId, senderId);
        if (receiverUserId != null) {
            notificationService.notifyUser(
                    receiverUserId.longValue(),
                    "MESSAGE",
                    "New file received",
                    String.valueOf(matchId)
            );
            log.info("Chat notification sent to user {} for match {}", receiverUserId, matchId);
        }

        return messageWithFiles;
    }

    /* ===================== GET FILE ATTACHMENT ===================== */

    public ChatFileAttachment getFileAttachment(Long fileId, String username) {
        ChatFileAttachment fileAttachment = chatFileAttachmentRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File attachment not found"));

        // Validate user has access to this file's chat
        ChatMessage chatMessage = fileAttachment.getChatMessage();
        Integer userId = resolveSenderId(username);
        validateSender(chatMessage.getMatchId(), userId);

        return fileAttachment;
    }

}

