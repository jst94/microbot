/*
 * Copyright (c) 2024, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.config;

import com.google.gson.Gson;
import com.google.inject.Inject;
import java.awt.Color;
import java.awt.TrayIcon;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;
import net.runelite.client.Notifier;

@ConfigSerializer(NotificationSerializer.class)
@AllArgsConstructor
@NoArgsConstructor(force = true)
@Getter
@Setter
@With
public class Notification
{
	public static final Notification OFF = new Notification();
	public static final Notification ON = new Notification().withEnabled(true);

	private boolean enabled;
	private boolean sendWhenFocused;
	private boolean requestFocus;
	private boolean tray;
	private TrayIconType trayIconType;
	private String sound;
	private int volume;
	private boolean flash;
	private Color flashColor;

	public Notification() {
		// Default constructor
	}

	// Add any additional constructors or methods as needed
}

class NotificationSerializer implements Serializer<Notification>
{
	private final Gson gson;

	@Inject
	private NotificationSerializer(Gson gson)
	{
		this.gson = gson;
	}

	@Override
	public String serialize(Notification value)
	{
		return gson.toJson(value);
	}

	@Override
	public Notification deserialize(String s)
	{
		if ("true".equals(s) || "false".equals(s))
		{
			return new Notification()
				.withEnabled(Boolean.parseBoolean(s));
		}

		return gson.fromJson(s, Notification.class);
	}
}