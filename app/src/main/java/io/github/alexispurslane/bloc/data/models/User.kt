package io.github.alexispurslane.bloc.data.models

import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.Presence

data class User(val userId: UserId, val avatarUrl: String? = null, val displayName: String? = null, val presence: Presence = Presence.OFFLINE)