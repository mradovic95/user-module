package com.comex.usermodule.core.port;

import com.comex.usermodule.core.event.UserEvent;

public interface EventPublisher {

	void publish(UserEvent userEvent);
}
