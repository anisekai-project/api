package fr.anisekai.core.internal.plannifier.data;

/**
 * Represent a schedule calibration result.
 *
 * @param updateCount
 *         Number of events that has been updated in the schedule.
 * @param deleteCount
 *         Number of events that has been removed in the schedule.
 */
public record CalibrationResult(int updateCount, int deleteCount) {

}
