package dao;

import entity.model.Prescription;
import entity.model.PrescriptionMedicine;
import exception.PrescriptionAlreadyVerifiedException;
import exception.PrescriptionRequiredException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class PrescriptionService implements IPrescriptionService {
    private final InMemoryRepository<Prescription> prescriptions = new InMemoryRepository<>();
    private final IPrescriptionDao prescriptionDao;

    public PrescriptionService() {
        this(null);
    }

    public PrescriptionService(IPrescriptionDao prescriptionDao) {
        this.prescriptionDao = prescriptionDao;
    }

    @Override
    public Prescription getPrescriptionById(int prescriptionId) {
        return prescriptionDao == null
                ? prescriptions.get(prescriptionId) : prescriptionDao.findById(prescriptionId);
    }

    @Override
    public List<Prescription> getAllPrescriptions() {
        return prescriptionDao == null ? prescriptions.getAll() : prescriptionDao.findAll();
    }

    @Override
    public synchronized void addPrescription(Prescription prescription) {
        validatePrescription(prescription);
        if (prescriptionDao != null) {
            prescriptionDao.insert(prescription);
            return;
        }
        prescriptions.add(prescription,
                Prescription::getPrescriptionId, Prescription::setPrescriptionId);
    }

    @Override
    public synchronized void updatePrescription(Prescription prescription) {
        validatePrescription(prescription);
        if (prescriptionDao != null) {
            prescriptionDao.update(prescription);
            return;
        }
        Prescription existing = prescriptions.get(prescription.getPrescriptionId());
        if (existing == null) {
            throw new IllegalArgumentException(
                    "Prescription does not exist: " + prescription.getPrescriptionId());
        }
        if (!existing.getStatus().equals(prescription.getStatus())) {
            throw new IllegalArgumentException(
                    "Prescription status must be changed through approve or reject");
        }
        prescriptions.update(prescription, Prescription::getPrescriptionId);
    }

    @Override
    public synchronized void deletePrescription(int prescriptionId) {
        if (prescriptionDao != null) {
            if (prescriptionDao.findById(prescriptionId) == null) {
                throw new IllegalArgumentException(
                        "Prescription does not exist: " + prescriptionId);
            }
            prescriptionDao.delete(prescriptionId);
            return;
        }
        if (!prescriptions.delete(prescriptionId)) {
            throw new IllegalArgumentException(
                    "Prescription does not exist: " + prescriptionId);
        }
    }

    @Override
    public synchronized void approve(Prescription prescription)
            throws PrescriptionAlreadyVerifiedException {
        Prescription stored = requirePendingPrescription(prescription);
        if (prescriptionDao != null) {
            LocalDate verifiedOn = LocalDate.now();
            if (!prescriptionDao.transitionStatus(stored.getPrescriptionId(),
                    "Pending", "Approved", verifiedOn)) {
                throw new PrescriptionAlreadyVerifiedException(
                        "Prescription has already been verified");
            }
            stored.setStatus("Approved");
            stored.setVerifiedOn(verifiedOn);
            prescription.setStatus("Approved");
            prescription.setVerifiedOn(verifiedOn);
            return;
        }
        try {
            stored.approve();
        } catch (IllegalStateException exception) {
            throw new PrescriptionAlreadyVerifiedException(exception.getMessage(), exception);
        }
    }

    @Override
    public synchronized void reject(Prescription prescription)
            throws PrescriptionAlreadyVerifiedException {
        Prescription stored = requirePendingPrescription(prescription);
        if (prescriptionDao != null) {
            if (!prescriptionDao.transitionStatus(stored.getPrescriptionId(),
                    "Pending", "Rejected", null)) {
                throw new PrescriptionAlreadyVerifiedException(
                        "Prescription has already been verified");
            }
            stored.setStatus("Rejected");
            stored.setVerifiedOn(null);
            prescription.setStatus("Rejected");
            prescription.setVerifiedOn(null);
            return;
        }
        try {
            stored.reject();
        } catch (IllegalStateException exception) {
            throw new PrescriptionAlreadyVerifiedException(exception.getMessage(), exception);
        }
    }

    @Override
    public synchronized void verifyPrescription(PrescriptionMedicine medicine,
            Prescription prescription)
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
        Prescription stored = prescriptionDao == null
                ? prescriptions.get(prescription.getPrescriptionId())
                : prescriptionDao.findById(prescription.getPrescriptionId());
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
