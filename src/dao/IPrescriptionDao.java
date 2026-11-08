package dao;

import entity.model.Prescription;
import java.time.LocalDate;

public interface IPrescriptionDao extends ICrudDao<Prescription> {
    boolean transitionStatus(int prescriptionId, String expectedStatus,
            String newStatus, LocalDate verifiedOn);
}
