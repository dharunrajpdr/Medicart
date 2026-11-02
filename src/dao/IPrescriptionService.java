package dao;

import entity.model.Prescription;
import entity.model.PrescriptionMedicine;
import exception.PrescriptionAlreadyVerifiedException;
import exception.PrescriptionRequiredException;
import java.util.List;

public interface IPrescriptionService {
    Prescription getPrescriptionById(int prescriptionId);

    List<Prescription> getAllPrescriptions();

    void addPrescription(Prescription prescription);

    void updatePrescription(Prescription prescription);

    void deletePrescription(int prescriptionId);

    void approve(Prescription prescription) throws PrescriptionAlreadyVerifiedException;

    void reject(Prescription prescription) throws PrescriptionAlreadyVerifiedException;

    void verifyPrescription(PrescriptionMedicine medicine, Prescription prescription)
            throws PrescriptionRequiredException, PrescriptionAlreadyVerifiedException;
}
