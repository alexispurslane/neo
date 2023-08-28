package io.github.alexispurslane.bloc.data

import io.github.alexispurslane.bloc.data.network.RevoltWebSocketService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RevoltServersRepository @Inject constructor(
    private val revoltWebSocketService: RevoltWebSocketService
) {

}