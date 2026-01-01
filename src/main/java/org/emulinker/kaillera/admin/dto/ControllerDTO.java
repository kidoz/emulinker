package org.emulinker.kaillera.admin.dto;

import java.util.List;

/**
 * DTO for controller information.
 */
public record ControllerDTO(String version, int bufferSize, int numClients,
        List<String> clientTypes) {
}
