package fr.anisekai.discord.interactions.broadcast;

import fr.anisekai.discord.interfaces.InteractionResponse;
import fr.anisekai.server.planifier.CalibrationResult;
import fr.anisekai.server.services.BroadcastService;
import fr.anisekai.server.services.BroadcastWorkflowService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BroadcastManagementSlashInteractionTest {

    @Test
    void executeCalibrateReturnsSuccess() {

        BroadcastService service = mock(BroadcastService.class);
        BroadcastWorkflowService workflowService = mock(BroadcastWorkflowService.class);
        CalibrationResult result = new CalibrationResult(3, 1);
        when(service.calibrate()).thenReturn(result);

        BroadcastManagementSlashInteraction interaction = new BroadcastManagementSlashInteraction(service, workflowService);
        InteractionResponse response = interaction.executeCalibrate();

        assertNotNull(response);
        verify(service).calibrate();
    }
}
