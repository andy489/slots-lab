package com.slotslab.dto.scatters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.slotslab.dto.spin.PayoutEntry;

import java.util.List;

@JsonPropertyOrder("_className")
public record ContactsDto(
        List<ContactDto> contacts,
        double globalMultiplier,
        double winAmount
) implements PayoutEntry {

    public static ContactsDto of(List<ContactDto> contacts, double globalMultiplier) {
        double total = contacts.stream().mapToDouble(ContactDto::winAmount).sum();
        return new ContactsDto(contacts, globalMultiplier, total);
    }

    @Override
    @JsonProperty("_className")
    public String className() {
        return ContactsDto.class.getName();
    }
}
