package com.comex.usermodule.infrastructure.messaging;

import com.comex.usermodule.core.event.UserEvent;
import com.comex.usermodule.core.port.EventPublisher;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventPublisherMock implements EventPublisher {

	@Override
	public void publish(UserEvent userEvent) {
		log.info("Publishing event {}.", userEvent);
	}
}
