package com.legalaid.backend.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.legalaid.backend.dto.ChatSummaryDto;
import com.legalaid.backend.model.Case;
import com.legalaid.backend.model.ChatMessage;
import com.legalaid.backend.model.LawyerProfile;
import com.legalaid.backend.model.Match;
import com.legalaid.backend.model.MatchStatus;
import com.legalaid.backend.model.NGOProfile;
import com.legalaid.backend.model.ProviderType;
import com.legalaid.backend.model.Role;
import com.legalaid.backend.model.User;
import com.legalaid.backend.repository.ChatRepository;
import com.legalaid.backend.repository.LawyerProfileRepository;
import com.legalaid.backend.repository.MatchRepository;
import com.legalaid.backend.repository.NGOProfileRepository;
import com.legalaid.backend.repository.UserRepository;
import com.legalaid.backend.util.TestDataBuilder;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LawyerProfileRepository lawyerProfileRepository;

    @Mock
    private NGOProfileRepository ngoProfileRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ChatService chatService;

    private User citizenUser;
    private User lawyerUser;
    private Case testCase;
    private LawyerProfile lawyerProfile;
    private Match match;
    private ChatMessage chatMessage;

    @BeforeEach
    void setUp() {
        citizenUser = TestDataBuilder.createUser(1, "citizen@example.com", "Test Citizen", Role.CITIZEN);
        lawyerUser = TestDataBuilder.createUser(2, "lawyer@example.com", "Test Lawyer", Role.LAWYER);
        testCase = TestDataBuilder.createCase(1, "Test Case", "Criminal Law", citizenUser);
        lawyerProfile = TestDataBuilder.createLawyerProfile(1, lawyerUser, "Criminal Law");
        match = TestDataBuilder.createMatch(1, testCase, ProviderType.LAWYER, lawyerProfile.getId());
        match.setStatus(MatchStatus.PROVIDER_CONFIRMED);
        chatMessage = TestDataBuilder.createChatMessage(1L, match.getId(), citizenUser, "Test message");
    }

    @Test
    void testGetCitizenChats_Success() {
        // Arrange
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizenUser));
        when(matchRepository.findByCaseObjCreatedById(citizenUser.getId())).thenReturn(Arrays.asList(match));
        when(chatRepository.findLastMessagesByMatchIds(anyList())).thenReturn(Arrays.asList(chatMessage));
        when(lawyerProfileRepository.findById(lawyerProfile.getId())).thenReturn(Optional.of(lawyerProfile));

        // Act
        List<ChatSummaryDto> chats = chatService.getCitizenChats("citizen@example.com");

        // Assert
        assertNotNull(chats);
        assertFalse(chats.isEmpty());
        assertEquals(1, chats.size());
        assertEquals(match.getId(), chats.get(0).getMatchId());
    }

    @Test
    void testGetCitizenChats_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> chatService.getCitizenChats("nonexistent@example.com"));
    }

    @Test
    void testGetCitizenChats_NoMatches() {
        // Arrange
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizenUser));
        when(matchRepository.findByCaseObjCreatedById(citizenUser.getId())).thenReturn(Arrays.asList());

        // Act
        List<ChatSummaryDto> chats = chatService.getCitizenChats("citizen@example.com");

        // Assert
        assertTrue(chats.isEmpty());
    }

    @Test
    void testGetProviderChats_LawyerSuccess() {
        // Arrange
        when(userRepository.findByEmail("lawyer@example.com")).thenReturn(Optional.of(lawyerUser));
        when(lawyerProfileRepository.findByUser(lawyerUser)).thenReturn(Optional.of(lawyerProfile));
        when(matchRepository.findByProviderTypeAndProviderIdAndStatus(
            ProviderType.LAWYER, lawyerProfile.getId(), MatchStatus.PROVIDER_CONFIRMED))
            .thenReturn(Arrays.asList(match));
        when(chatRepository.findLastMessagesByMatchIds(anyList())).thenReturn(Arrays.asList(chatMessage));

        // Act
        List<ChatSummaryDto> chats = chatService.getProviderChats("lawyer@example.com");

        // Assert
        assertNotNull(chats);
        assertFalse(chats.isEmpty());
        assertEquals(1, chats.size());
    }

    @Test
    void testGetProviderChats_NGOSuccess() {
        // Arrange
        User ngoUser = TestDataBuilder.createUser(3, "ngo@example.com", "Test NGO", Role.NGO);
        NGOProfile ngoProfile = TestDataBuilder.createNGOProfile(1, ngoUser, "Test NGO");
        Match ngoMatch = TestDataBuilder.createMatch(2, testCase, ProviderType.NGO, ngoProfile.getId());
        ngoMatch.setStatus(MatchStatus.PROVIDER_CONFIRMED);
        ChatMessage ngoMessage = TestDataBuilder.createChatMessage(2L, ngoMatch.getId(), ngoUser, "NGO message");

        when(userRepository.findByEmail("ngo@example.com")).thenReturn(Optional.of(ngoUser));
        when(ngoProfileRepository.findByUser(ngoUser)).thenReturn(Optional.of(ngoProfile));
        when(matchRepository.findByProviderTypeAndProviderIdAndStatus(
            ProviderType.NGO, ngoProfile.getId(), MatchStatus.PROVIDER_CONFIRMED))
            .thenReturn(Arrays.asList(ngoMatch));
        when(chatRepository.findLastMessagesByMatchIds(anyList())).thenReturn(Arrays.asList(ngoMessage));

        // Act
        List<ChatSummaryDto> chats = chatService.getProviderChats("ngo@example.com");

        // Assert
        assertNotNull(chats);
        assertFalse(chats.isEmpty());
    }

    @Test
    void testGetProviderChats_UserNotProvider() {
        // Arrange
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizenUser));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> chatService.getProviderChats("citizen@example.com"));
    }

    @Test
    void testGetProviderChats_NoMatches() {
        // Arrange
        when(userRepository.findByEmail("lawyer@example.com")).thenReturn(Optional.of(lawyerUser));
        when(lawyerProfileRepository.findByUser(lawyerUser)).thenReturn(Optional.of(lawyerProfile));
        when(matchRepository.findByProviderTypeAndProviderIdAndStatus(
            ProviderType.LAWYER, lawyerProfile.getId(), MatchStatus.PROVIDER_CONFIRMED))
            .thenReturn(Arrays.asList());

        // Act
        List<ChatSummaryDto> chats = chatService.getProviderChats("lawyer@example.com");

        // Assert
        assertTrue(chats.isEmpty());
    }

    @Test
    void testLoadChats_Success() {
        // Arrange
        List<ChatMessage> messages = Arrays.asList(chatMessage);
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizenUser));
        when(userRepository.findById(citizenUser.getId())).thenReturn(Optional.of(citizenUser)); // Added this mock
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(chatRepository.findByMatchIdOrderByCreatedAtAsc(match.getId())).thenReturn(messages);

        // Act
        List<ChatMessage> result = chatService.loadChats(match.getId(), "citizen@example.com");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(chatMessage.getMessage(), result.get(0).getMessage());
    }

    @Test
    void testLoadChats_MatchNotConfirmed() {
        // Arrange
        match.setStatus(MatchStatus.PENDING);
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizenUser));
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> chatService.loadChats(match.getId(), "citizen@example.com"));
    }

    @Test
    void testLoadChats_UnauthorizedUser() {
        // Arrange
        User unauthorizedUser = TestDataBuilder.createUser(99, "unauthorized@example.com", "Unauthorized", Role.CITIZEN);
        when(userRepository.findByEmail("unauthorized@example.com")).thenReturn(Optional.of(unauthorizedUser));
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> chatService.loadChats(match.getId(), "unauthorized@example.com"));
    }

    @Test
    void testSaveMessage_CitizenSuccess() {
        // Arrange
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizenUser));
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(userRepository.findById(citizenUser.getId())).thenReturn(Optional.of(citizenUser));
        when(chatRepository.save(any(ChatMessage.class))).thenReturn(chatMessage);
        when(lawyerProfileRepository.findById(lawyerProfile.getId())).thenReturn(Optional.of(lawyerProfile));

        // Act
        ChatMessage result = chatService.saveMessage(match.getId(), "citizen@example.com", "Test message");

        // Assert
        assertNotNull(result);
        verify(chatRepository, times(1)).save(any(ChatMessage.class));
        verify(notificationService, times(1)).notifyUser(anyLong(), eq("MESSAGE"), anyString(), anyString());
    }

    @Test
    void testSaveMessage_LawyerSuccess() {
        // Arrange
        when(userRepository.findByEmail("lawyer@example.com")).thenReturn(Optional.of(lawyerUser));
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(userRepository.findById(lawyerUser.getId())).thenReturn(Optional.of(lawyerUser));
        when(lawyerProfileRepository.findByUser(lawyerUser)).thenReturn(Optional.of(lawyerProfile));
        when(chatRepository.save(any(ChatMessage.class))).thenReturn(chatMessage);

        // Act
        ChatMessage result = chatService.saveMessage(match.getId(), "lawyer@example.com", "Reply message");

        // Assert
        assertNotNull(result);
        verify(chatRepository, times(1)).save(any(ChatMessage.class));
        verify(notificationService, times(1)).notifyUser(anyLong(), eq("MESSAGE"), anyString(), anyString());
    }

    @Test
    void testSaveMessage_MatchNotFound() {
        // Arrange
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizenUser));
        when(matchRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> chatService.saveMessage(999, "citizen@example.com", "Message"));
    }

    @Test
    void testSaveMessage_MatchNotConfirmed() {
        // Arrange
        match.setStatus(MatchStatus.PENDING);
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizenUser));
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        // Act & Assert
        assertThrows(RuntimeException.class,
            () -> chatService.saveMessage(match.getId(), "citizen@example.com", "Message"));
    }

    @Test
    void testSaveMessage_UnauthorizedUser() {
        // Arrange
        User unauthorizedUser = TestDataBuilder.createUser(99, "unauthorized@example.com", "Unauthorized", Role.CITIZEN);
        when(userRepository.findByEmail("unauthorized@example.com")).thenReturn(Optional.of(unauthorizedUser));
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(userRepository.findById(unauthorizedUser.getId())).thenReturn(Optional.of(unauthorizedUser));

        // Act & Assert
        assertThrows(RuntimeException.class,
            () -> chatService.saveMessage(match.getId(), "unauthorized@example.com", "Message"));
    }
}

