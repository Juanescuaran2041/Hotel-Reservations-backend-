package com.hotel.reservations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.reservations.auth.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class HotelFlowIntegrationTest {

    private static final String PASSWORD = "Str0ngPass1";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;

    @Test
    void hotelEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/hotel/disponibilidad")
                        .param("checkInDate", "2030-03-10").param("checkOutDate", "2030-03-12"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordsAreStoredAsBcryptHashes() throws Exception {
        String email = register();

        assertThat(userRepository.findByEmail(email)).get()
                .satisfies(user -> assertThat(user.getPasswordHash()).startsWith("$2").isNotEqualTo(PASSWORD));
    }

    @Test
    void loginRejectsWrongPasswordAndUnknownUserWithSameMessage() throws Exception {
        String email = register();

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", email, "password", "WrongPass99")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "nobody@example.com", "password", "WrongPass99")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void weakPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("fullName", "Weak", "email", uniqueEmail(), "password", "short")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() throws Exception {
        JsonNode tokens = login(register());

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + tokens.get("refreshToken").asText()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json("refreshToken", tokens.get("refreshToken").asText())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void fullReservationLifecycleProducesTheFinalInvoice() throws Exception {
        String token = login(register()).get("accessToken").asText();
        LocalDate checkIn = LocalDate.now().plusYears(1).withMonth(3).withDayOfMonth(10);
        LocalDate checkOut = checkIn.plusDays(3);

        mockMvc.perform(get("/api/v1/hotel/disponibilidad").header("Authorization", "Bearer " + token)
                        .param("checkInDate", checkIn.toString()).param("checkOutDate", checkOut.toString())
                        .param("roomType", "DOUBLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms.length()").value(8))
                .andExpect(jsonPath("$.rooms[0].totalPrice").value(360.00));

        String reservationBody = objectMapper.writeValueAsString(java.util.Map.of(
                "guestName", "Ana Gomez", "guestEmail", "ana@example.com", "roomType", "DOUBLE",
                "checkInDate", checkIn.toString(), "checkOutDate", checkOut.toString()));
        String created = mockMvc.perform(post("/api/v1/hotel/reservar").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(reservationBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.roomCost").value(360.00))
                .andReturn().getResponse().getContentAsString();
        String reservationId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(get("/api/v1/hotel/disponibilidad").header("Authorization", "Bearer " + token)
                        .param("checkInDate", checkIn.toString()).param("checkOutDate", checkOut.toString())
                        .param("roomType", "DOUBLE"))
                .andExpect(jsonPath("$.rooms.length()").value(7));

        addService(token, reservationId, "SPA");
        mockMvc.perform(post("/api/v1/hotel/servicios/" + reservationId).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json("serviceType", "BREAKFAST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicesTotal").value(95.00))
                .andExpect(jsonPath("$.total").value(455.00));

        mockMvc.perform(put("/api/v1/hotel/checkout/" + reservationId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/v1/hotel/checkin/" + reservationId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"))
                .andExpect(jsonPath("$.digitalKey.code").isNotEmpty());

        mockMvc.perform(put("/api/v1/hotel/checkout/" + reservationId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"))
                .andExpect(jsonPath("$.digitalKey").doesNotExist())
                .andExpect(jsonPath("$.invoice.invoiceNumber").isNotEmpty())
                .andExpect(jsonPath("$.invoice.roomSubtotal").value(360.00))
                .andExpect(jsonPath("$.invoice.servicesSubtotal").value(95.00))
                .andExpect(jsonPath("$.invoice.total").value(455.00));

        mockMvc.perform(get("/api/v1/hotel/reserva/" + reservationId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoice.total").value(455.00));

        mockMvc.perform(get("/api/v1/hotel/disponibilidad").header("Authorization", "Bearer " + token)
                        .param("checkInDate", checkIn.toString()).param("checkOutDate", checkOut.toString())
                        .param("roomType", "DOUBLE"))
                .andExpect(jsonPath("$.rooms.length()").value(8));
    }

    @Test
    void anotherGuestCannotReadSomeoneElsesReservation() throws Exception {
        String ownerToken = login(register()).get("accessToken").asText();
        String strangerToken = login(register()).get("accessToken").asText();
        LocalDate checkIn = LocalDate.now().plusYears(1).withMonth(4).withDayOfMonth(5);
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "guestName", "Owner", "guestEmail", "owner@example.com", "roomType", "SINGLE",
                "checkInDate", checkIn.toString(), "checkOutDate", checkIn.plusDays(1).toString()));
        String created = mockMvc.perform(post("/api/v1/hotel/reservar").header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(get("/api/v1/hotel/reserva/" + id).header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/hotel/reserva/" + UUID.randomUUID()).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void noRoomIsLeftOnceTheTypeIsFullyBooked() throws Exception {
        String token = login(register()).get("accessToken").asText();
        LocalDate checkIn = LocalDate.now().plusYears(2).withMonth(5).withDayOfMonth(2);
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "guestName", "Guest", "guestEmail", "guest@example.com", "roomType", "SUITE",
                "checkInDate", checkIn.toString(), "checkOutDate", checkIn.plusDays(2).toString()));

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/v1/hotel/reservar").header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(post("/api/v1/hotel/reservar").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    private void addService(String token, String reservationId, String serviceType) throws Exception {
        mockMvc.perform(post("/api/v1/hotel/servicios/" + reservationId).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json("serviceType", serviceType)))
                .andExpect(status().isOk());
    }

    private String register() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("fullName", "Test User", "email", email, "password", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        return email;
    }

    private JsonNode login(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", email, "password", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private String json(String... keyValues) throws Exception {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return objectMapper.writeValueAsString(map);
    }
}
