package com.github.webmorph.security.account.event;

import com.github.webmorph.eventbus.event.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
@RequiredArgsConstructor
public class AccountAuthenticateEvent extends Event {
    private final String username;
    private final boolean rememberMe;
    private Duration lifetime = Duration.ofHours(6);
}
