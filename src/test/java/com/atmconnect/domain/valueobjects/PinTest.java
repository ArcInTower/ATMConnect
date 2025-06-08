package com.atmconnect.domain.valueobjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

class PinTest {
    
    @Test
    @DisplayName("Should create PIN with valid 6-digit input")
    void shouldCreatePinWithValidSixDigitInput() {
        assertThatCode(() -> new Pin("123456")).doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("Should throw exception for null PIN")
    void shouldThrowExceptionForNullPin() {
        assertThatThrownBy(() -> new Pin(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("PIN must be exactly 6 digits");
    }
    
    @Test
    @DisplayName("Should throw exception for PIN with wrong length")
    void shouldThrowExceptionForPinWithWrongLength() {
        assertThatThrownBy(() -> new Pin("12345"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("PIN must be exactly 6 digits");
        
        assertThatThrownBy(() -> new Pin("1234567"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("PIN must be exactly 6 digits");
    }
    
    @Test
    @DisplayName("Should throw exception for PIN with non-numeric characters")
    void shouldThrowExceptionForPinWithNonNumericCharacters() {
        assertThatThrownBy(() -> new Pin("12345a"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("PIN must be exactly 6 digits");
    }
    
    @Test
    @DisplayName("Should throw exception for simple patterns")
    void shouldThrowExceptionForSimplePatterns() {
        assertThatThrownBy(() -> new Pin("111111"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("PIN is too simple");
        
        assertThatThrownBy(() -> new Pin("123456"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("PIN is too simple");
        
        assertThatThrownBy(() -> new Pin("000000"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("PIN is too simple");
    }
    
    @Test
    @DisplayName("Should verify correct PIN")
    void shouldVerifyCorrectPin() {
        Pin pin = new Pin("987654");
        
        assertThat(pin.verify("987654")).isTrue();
    }
    
    @Test
    @DisplayName("Should reject incorrect PIN")
    void shouldRejectIncorrectPin() {
        Pin pin = new Pin("987654");
        
        assertThat(pin.verify("123456")).isFalse();
        assertThat(pin.verify("98765")).isFalse();
        assertThat(pin.verify(null)).isFalse();
    }
    
    @Test
    @DisplayName("Should create PIN from hash and salt")
    void shouldCreatePinFromHashAndSalt() {
        Pin originalPin = new Pin("987654");
        String hash = originalPin.getHashedPin();
        String salt = originalPin.getSalt();
        
        Pin recreatedPin = Pin.fromHash(hash, salt);
        
        assertThat(recreatedPin.verify("987654")).isTrue();
        assertThat(recreatedPin.verify("123456")).isFalse();
    }
    
    @Test
    @DisplayName("Should generate different salts for same PIN")
    void shouldGenerateDifferentSaltsForSamePin() {
        Pin pin1 = new Pin("987654");
        Pin pin2 = new Pin("987654");
        
        assertThat(pin1.getSalt()).isNotEqualTo(pin2.getSalt());
        assertThat(pin1.getHashedPin()).isNotEqualTo(pin2.getHashedPin());
        
        // But both should verify the same PIN
        assertThat(pin1.verify("987654")).isTrue();
        assertThat(pin2.verify("987654")).isTrue();
    }
}