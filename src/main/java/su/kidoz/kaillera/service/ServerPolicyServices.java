package su.kidoz.kaillera.service;

import su.kidoz.kaillera.model.validation.LoginValidator;

/**
 * Groups server policy enforcement services.
 *
 * <p>
 * This record bundles services that enforce server policies and rules,
 * including login validation, chat moderation, and announcements.
 *
 * @param loginValidator
 *            validates user login requests
 * @param chatModerationService
 *            handles chat validation and flood control
 * @param announcementService
 *            sends server announcements to users
 */
public record ServerPolicyServices(LoginValidator loginValidator,
        ChatModerationService chatModerationService, AnnouncementService announcementService) {
}
