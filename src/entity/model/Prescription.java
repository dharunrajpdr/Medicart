package entity.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class Prescription {
    private static final String PENDING = "Pending";
    private static final String APPROVED = "Approved";
    private static final String REJECTED = "Rejected";

    private int prescriptionId;
    private Customer customer;
    private String documentPath;
    private LocalDateTime uploadedOn = LocalDateTime.now();
    private Integer verifiedBy;
    private LocalDate verifiedOn;
    private String status = PENDING;

    public Prescription() {
    }

    public Prescription(int prescriptionId, Customer customer, String documentPath,
            Integer verifiedBy, LocalDate verifiedOn, String status) {
        this(prescriptionId, customer, documentPath, verifiedBy, verifiedOn, status,
                LocalDateTime.now());
    }

    public Prescription(int prescriptionId, Customer customer, String documentPath,
            Integer verifiedBy, LocalDate verifiedOn, String status, LocalDateTime uploadedOn) {
        setPrescriptionId(prescriptionId);
        setCustomer(customer);
        setDocumentPath(documentPath);
        setUploadedOn(uploadedOn);
        setVerifiedBy(verifiedBy);
        setVerifiedOn(verifiedOn);
        setStatus(status);
    }

    public int getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(int prescriptionId) {
        this.prescriptionId = ModelValidation.nonNegativeId(prescriptionId, "prescriptionId");
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = Objects.requireNonNull(customer, "customer cannot be null");
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public void setDocumentPath(String documentPath) {
        this.documentPath = ModelValidation.requiredText(documentPath, "documentPath", 200);
    }

    public LocalDateTime getUploadedOn() {
        return uploadedOn;
    }

    public void setUploadedOn(LocalDateTime uploadedOn) {
        this.uploadedOn = Objects.requireNonNull(uploadedOn, "uploadedOn cannot be null");
    }

    public Integer getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(Integer verifiedBy) {
        if (verifiedBy != null && verifiedBy <= 0) {
            throw new IllegalArgumentException("verifiedBy must be positive");
        }
        this.verifiedBy = verifiedBy;
    }

    public LocalDate getVerifiedOn() {
        return verifiedOn;
    }

    public void setVerifiedOn(LocalDate verifiedOn) {
        this.verifiedOn = verifiedOn;
    }

    public synchronized String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        String value = ModelValidation.requiredText(status, "status", 20);
        if (!PENDING.equals(value) && !APPROVED.equals(value) && !REJECTED.equals(value)) {
            throw new IllegalArgumentException("status must be Pending, Approved, or Rejected");
        }
        this.status = value;
    }

    public synchronized boolean isApproved() {
        return APPROVED.equals(status);
    }

    public synchronized void approve() {
        ensurePending();
        status = APPROVED;
        verifiedOn = LocalDate.now();
    }

    public synchronized void reject() {
        ensurePending();
        status = REJECTED;
    }

    private void ensurePending() {
        if (!PENDING.equals(status)) {
            throw new IllegalStateException("Only a pending prescription can be verified");
        }
    }
}
