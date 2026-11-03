package dao;

import entity.model.Prescription;
import entity.model.PrescriptionMedicine;
import exception.PrescriptionAlreadyVerifiedException;
import exception.PrescriptionRequiredException;
import java.util.List;
import java.util.Objects;

public class PrescriptionService implements IPrescriptionService {
    private final InMemoryRepository<Prescription> prescriptions = new InMemoryRepository<>();

    @Override
    public Prescription getPrescriptionById(int prescriptionId) {
        return prescriptions.get(prescriptionId);
    }

    @Override
    public List<Prescription> getAllPrescriptions() {
        return prescriptions.getAll();
    }

    @Override
    public void addPrescription(Prescription prescription) {
        validatePrescription(prescription);
        prescriptions.add(prescription,
                Prescription::getPrescriptionId, Prescription::setPrescriptionId);
    }

    @Override
    public void updatePrescription(Prescription prescription) {
        validatePrescription(prescription);
        Prescription existing = prescriptions.get(prescription.getPrescriptionId());
        if (existing == null) {
            throw new IllegalArgumentException(
                    "Prescription does not exist: " + prescription.getPrescriptionId());
        }
        if (!"Pending".equals(existing.getStatus())
                && !existing.getStatus().equals(prescription.getStatus())) {
            throw new IllegalArgumentException(
                    "A verified prescription status cannot be changed");
        }
        prescriptions.update(prescription, Prescription::getPrescriptionId);
    }

    @Override
    public void deletePrescription(int prescriptionId) {
        if (!prescriptions.delete(prescriptionId)) {
            throw new IllegalArgumentException(
                    "Prescription does not exist: " + prescriptionId);
        }
    }

    @Override
    public void approve(Prescription prescription)
            throws PrescriptionAlreadyVerifiedException {
        Prescription stored = requirePendingPrescription(prescription);
        try {
            stored.approve();
        } catch (IllegalStateException exception) {
            throw new PrescriptionAlreadyVerifiedException(exception.getMessage(), exception);
        }
    }

    @Override
    public void reject(Prescription prescription)
            throws PrescriptionAlreadyVerifiedException {
        Prescription stored = requirePendingPrescription(prescription);
        try {
            stored.reject();
        } catch (IllegalStateException exception) {
            throw new PrescriptionAlreadyVerifiedException(exception.getMessage(), exception);
        }
    }

    @Override
    public void verifyPrescription(PrescriptionMedicine medicine, Prescription prescription)
            throws PrescriptionRequiredException, PrescriptionAlreadyVerifiedException {
        Objects.requireNonNull(medicine, "medicine cannot be null");
        if (prescription == null) {
            throw new PrescriptionRequiredException("A prescription is required");
        }
        Prescription stored = requirePendingPrescription(prescription);
        approve(stored);
        medicine.setPrescription(stored);
    }

    private Prescription requirePendingPrescription(Prescription prescription)
            throws PrescriptionAlreadyVerifiedException {
        Objects.requireNonNull(prescription, "prescription cannot be null");
        Prescription stored = prescriptions.get(prescription.getPrescriptionId());
        if (stored == null) {
            throw new IllegalArgumentException(
                    "Prescription does not exist: " + prescription.getPrescriptionId());
        }
        if (!"Pending".equals(stored.getStatus())) {
            throw new PrescriptionAlreadyVerifiedException(
                    "Prescription has already been verified");
        }
        return stored;
    }

    private static void validatePrescription(Prescription prescription) {
        Objects.requireNonNull(prescription, "prescription cannot be null");
        Objects.requireNonNull(prescription.getCustomer(), "customer cannot be null");
        Objects.requireNonNull(prescription.getDocumentPath(), "documentPath cannot be null");
        Objects.requireNonNull(prescription.getStatus(), "status cannot be null");
    }
}
