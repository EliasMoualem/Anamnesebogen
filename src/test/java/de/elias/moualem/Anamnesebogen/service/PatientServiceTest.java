package de.elias.moualem.Anamnesebogen.service;

import de.elias.moualem.Anamnesebogen.dto.GuardianDTO;
import de.elias.moualem.Anamnesebogen.dto.PatientFormDTO;
import de.elias.moualem.Anamnesebogen.dto.PolicyholderDTO;
import de.elias.moualem.Anamnesebogen.entity.*;
import de.elias.moualem.Anamnesebogen.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for PatientService business logic.
 * Tests patient validation rules for minors and adults with different insurance types.
 *
 * Business Rules Tested:
 * 1. Minor patients (< 18 years) MUST have a legal guardian
 * 2. Minor patients MUST be FAMILY_INSURED (never SELF_INSURED)
 * 3. Minor patients MUST have a policyholder
 * 4. Adult patients (>= 18 years) can be SELF_INSURED or FAMILY_INSURED
 * 5. FAMILY_INSURED patients MUST have a policyholder
 * 6. SELF_INSURED patients do NOT require guardian or policyholder
 */
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PolicyholderRepository policyholderRepository;

    @Mock
    private LegalGuardianRepository legalGuardianRepository;

    @Mock
    private SignatureRepository signatureRepository;

    @Mock
    private FormSubmissionRepository formSubmissionRepository;

    @Mock
    private ConsentRepository consentRepository;

    private PatientService patientService;

    @BeforeEach
    void setUp() {
        patientService = new PatientService(
                patientRepository,
                policyholderRepository,
                legalGuardianRepository,
                signatureRepository,
                formSubmissionRepository,
                consentRepository
        );
    }

    // ===================================
    // Test 1: Happy path for minor patient
    // ===================================

    @Test
    void testSaveMinorPatient_WithGuardianAndPolicyholder_Success() {
        // Given: 10-year-old minor with guardian and policyholder
        PatientFormDTO patientForm = createMinorPatientForm();
        patientForm.setGuardian(createGuardianDTO());
        patientForm.setPolicyholder(createPolicyholderDTO());

        // Mock repository responses
        Patient savedPatient = createMockPatient(patientForm, true);
        savedPatient.setInsuranceType(InsuranceType.FAMILY_INSURED); // Minor must be FAMILY_INSURED
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);

        // Mock guardian save - set guardian on patient when saved
        when(legalGuardianRepository.save(any(LegalGuardian.class))).thenAnswer(invocation -> {
            LegalGuardian guardian = invocation.getArgument(0);
            savedPatient.setGuardian(guardian);
            return guardian;
        });

        // Mock policyholder save - set policyholder on patient when saved
        when(policyholderRepository.save(any(Policyholder.class))).thenAnswer(invocation -> {
            Policyholder policyholder = invocation.getArgument(0);
            savedPatient.setPolicyholder(policyholder);
            return policyholder;
        });

        when(formSubmissionRepository.save(any(FormSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(consentRepository.save(any(Consent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Save the patient
        UUID patientId = patientService.savePatient(patientForm, "192.168.1.1", "TestAgent");

        // Then: Patient should be saved successfully
        assertThat(patientId).isNotNull();
        verify(patientRepository).save(any(Patient.class));
        verify(legalGuardianRepository).save(any(LegalGuardian.class));
        verify(policyholderRepository).save(any(Policyholder.class));
        verify(formSubmissionRepository).save(any(FormSubmission.class));
        verify(consentRepository).save(any(Consent.class));
    }

    // ===================================
    // Test 2: Minor without guardian - should fail
    // ===================================

    @Test
    void testSaveMinorPatient_WithoutGuardian_ThrowsException() {
        // Given: 10-year-old minor WITHOUT guardian (but with policyholder)
        PatientFormDTO patientForm = createMinorPatientForm();
        patientForm.setGuardian(null); // No guardian
        patientForm.setPolicyholder(createPolicyholderDTO());

        // Mock repository responses
        Patient savedPatient = createMockPatient(patientForm, true);
        savedPatient.setGuardian(null); // No guardian set
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);

        // When/Then: Should throw IllegalStateException
        assertThatThrownBy(() -> patientService.savePatient(patientForm, "192.168.1.1", "TestAgent"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Minor patients (under 18 years) must have a legal guardian");

        verify(patientRepository).save(any(Patient.class));
    }

    // ===================================
    // Test 3: Minor without policyholder - should fail
    // ===================================

    @Test
    void testSaveMinorPatient_WithoutPolicyholder_ThrowsException() {
        // Given: 10-year-old minor WITHOUT policyholder (but with guardian)
        PatientFormDTO patientForm = createMinorPatientForm();
        patientForm.setGuardian(createGuardianDTO());
        patientForm.setPolicyholder(null); // No policyholder

        // Mock repository responses
        Patient savedPatient = createMockPatient(patientForm, true);
        savedPatient.setInsuranceType(InsuranceType.FAMILY_INSURED); // Minor must be FAMILY_INSURED
        savedPatient.setPolicyholder(null); // No policyholder set
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);
        when(legalGuardianRepository.save(any(LegalGuardian.class))).thenAnswer(invocation -> {
            LegalGuardian guardian = invocation.getArgument(0);
            savedPatient.setGuardian(guardian);
            return guardian;
        });

        // When/Then: Should throw IllegalStateException
        assertThatThrownBy(() -> patientService.savePatient(patientForm, "192.168.1.1", "TestAgent"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FAMILY_INSURED patients must have a policyholder");

        verify(patientRepository).save(any(Patient.class));
        verify(legalGuardianRepository).save(any(LegalGuardian.class));
    }

    // ===================================
    // Test 4: Minor as SELF_INSURED - should fail
    // ===================================

    @Test
    void testSaveMinorPatient_AsSelfInsured_ThrowsException() {
        // Given: 10-year-old minor with SELF_INSURED insurance type
        PatientFormDTO patientForm = createMinorPatientForm();
        patientForm.setGuardian(createGuardianDTO());
        patientForm.setPolicyholder(null); // No policyholder = SELF_INSURED

        // Mock repository responses
        Patient savedPatient = createMockPatient(patientForm, true);
        savedPatient.setInsuranceType(InsuranceType.SELF_INSURED); // Force SELF_INSURED
        savedPatient.setPolicyholder(null);
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);
        when(legalGuardianRepository.save(any(LegalGuardian.class))).thenAnswer(invocation -> {
            LegalGuardian guardian = invocation.getArgument(0);
            savedPatient.setGuardian(guardian);
            return guardian;
        });

        // When/Then: Should throw IllegalStateException
        assertThatThrownBy(() -> patientService.savePatient(patientForm, "192.168.1.1", "TestAgent"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Minor patients must be FAMILY_INSURED");

        verify(patientRepository).save(any(Patient.class));
        verify(legalGuardianRepository).save(any(LegalGuardian.class));
    }

    // ===================================
    // Test 5: Adult SELF_INSURED - should succeed
    // ===================================

    @Test
    void testSaveAdultPatient_SelfInsured_Success() {
        // Given: 25-year-old adult with SELF_INSURED (no guardian, no policyholder)
        PatientFormDTO patientForm = createAdultPatientForm();
        patientForm.setGuardian(null); // No guardian needed for adults
        patientForm.setPolicyholder(null); // No policyholder = SELF_INSURED

        // Mock repository responses
        Patient savedPatient = createMockPatient(patientForm, false);
        savedPatient.setInsuranceType(InsuranceType.SELF_INSURED);
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);
        when(formSubmissionRepository.save(any(FormSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(consentRepository.save(any(Consent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Save the patient
        UUID patientId = patientService.savePatient(patientForm, "192.168.1.1", "TestAgent");

        // Then: Patient should be saved successfully
        assertThat(patientId).isNotNull();
        verify(patientRepository).save(any(Patient.class));
        verify(legalGuardianRepository, never()).save(any(LegalGuardian.class));
        verify(policyholderRepository, never()).save(any(Policyholder.class));
        verify(formSubmissionRepository).save(any(FormSubmission.class));
        verify(consentRepository).save(any(Consent.class));
    }

    // ===================================
    // Test 6: Adult FAMILY_INSURED with policyholder - should succeed
    // ===================================

    @Test
    void testSaveAdultPatient_FamilyInsuredWithPolicyholder_Success() {
        // Given: 20-year-old adult with FAMILY_INSURED and policyholder (e.g., spouse)
        PatientFormDTO patientForm = createAdultPatientForm();
        patientForm.setGuardian(null); // No guardian needed for adults
        patientForm.setPolicyholder(createPolicyholderDTO());

        // Mock repository responses
        Patient savedPatient = createMockPatient(patientForm, false);
        savedPatient.setInsuranceType(InsuranceType.FAMILY_INSURED);
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);
        when(policyholderRepository.save(any(Policyholder.class))).thenAnswer(invocation -> {
            Policyholder policyholder = invocation.getArgument(0);
            savedPatient.setPolicyholder(policyholder);
            return policyholder;
        });
        when(formSubmissionRepository.save(any(FormSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(consentRepository.save(any(Consent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Save the patient
        UUID patientId = patientService.savePatient(patientForm, "192.168.1.1", "TestAgent");

        // Then: Patient should be saved successfully
        assertThat(patientId).isNotNull();
        verify(patientRepository).save(any(Patient.class));
        verify(legalGuardianRepository, never()).save(any(LegalGuardian.class)); // No guardian for adults
        verify(policyholderRepository).save(any(Policyholder.class));
        verify(formSubmissionRepository).save(any(FormSubmission.class));
        verify(consentRepository).save(any(Consent.class));
    }

    // ===================================
    // Test 7: Adult FAMILY_INSURED without policyholder - should fail
    // ===================================

    @Test
    void testSaveAdultPatient_FamilyInsuredWithoutPolicyholder_ThrowsException() {
        // Given: 20-year-old adult marked as FAMILY_INSURED but no policyholder
        PatientFormDTO patientForm = createAdultPatientForm();
        patientForm.setGuardian(null);
        patientForm.setPolicyholder(null); // Missing policyholder

        // Mock repository responses - force FAMILY_INSURED
        Patient savedPatient = createMockPatient(patientForm, false);
        savedPatient.setInsuranceType(InsuranceType.FAMILY_INSURED);
        savedPatient.setPolicyholder(null);
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);

        // When/Then: Should throw IllegalStateException
        assertThatThrownBy(() -> patientService.savePatient(patientForm, "192.168.1.1", "TestAgent"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FAMILY_INSURED patients must have a policyholder");

        verify(patientRepository).save(any(Patient.class));
    }

    // ===================================
    // Test 8: Load guardian for minor patient in PDF retrieval
    // ===================================

    @Test
    void testGetPatientForPdf_LoadsGuardianForMinor() {
        // Given: Saved minor patient with guardian
        UUID patientId = UUID.randomUUID();
        Patient patient = createSavedMinorPatient(patientId);
        LegalGuardian guardian = createSavedGuardian(patient);
        patient.setGuardian(guardian);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(legalGuardianRepository.findByPatientId(patientId)).thenReturn(Optional.of(guardian));
        when(signatureRepository.findLatestByPatientId(patientId)).thenReturn(Optional.empty());
        when(policyholderRepository.findByPatientId(patientId)).thenReturn(Optional.empty());

        // When: Retrieve patient for PDF
        Optional<PatientFormDTO> result = patientService.getPatientForPdf(patientId);

        // Then: Guardian should be loaded
        assertThat(result).isPresent();
        PatientFormDTO patientForm = result.get();
        assertThat(patientForm.getGuardian()).isNotNull();
        assertThat(patientForm.getGuardian().getFirstName()).isEqualTo("Maria");
        assertThat(patientForm.getGuardian().getLastName()).isEqualTo("Mustermann");
        assertThat(patientForm.getGuardian().getRelationshipType()).isEqualTo("MOTHER");

        verify(legalGuardianRepository).findByPatientId(patientId);
    }

    // ===================================
    // Test 9: Load policyholder for FAMILY_INSURED patient in PDF retrieval
    // ===================================

    @Test
    void testGetPatientForPdf_LoadsPolicyholderForFamilyInsured() {
        // Given: Saved FAMILY_INSURED patient with policyholder
        UUID patientId = UUID.randomUUID();
        Patient patient = createSavedAdultPatient(patientId);
        patient.setInsuranceType(InsuranceType.FAMILY_INSURED);
        Policyholder policyholder = createSavedPolicyholder(patient);
        patient.setPolicyholder(policyholder);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(policyholderRepository.findByPatientId(patientId)).thenReturn(Optional.of(policyholder));
        when(signatureRepository.findLatestByPatientId(patientId)).thenReturn(Optional.empty());

        // When: Retrieve patient for PDF
        Optional<PatientFormDTO> result = patientService.getPatientForPdf(patientId);

        // Then: Policyholder should be loaded
        assertThat(result).isPresent();
        PatientFormDTO patientForm = result.get();
        assertThat(patientForm.getPolicyholder()).isNotNull();
        assertThat(patientForm.getPolicyholder().getFirstName()).isEqualTo("Hans");
        assertThat(patientForm.getPolicyholder().getLastName()).isEqualTo("Schmidt");

        verify(policyholderRepository).findByPatientId(patientId);
    }

    // ===================================
    // Test 10: Age calculation boundary case
    // ===================================

    @Test
    void testAgeCalculation_BoundaryCase_17Years11Months() {
        // Given: Patient born 17 years and 11 months ago (still a minor)
        LocalDate birthDate = LocalDate.now().minusYears(17).minusMonths(11);
        PatientFormDTO patientForm = createMinorPatientForm();
        patientForm.setBirthDate(birthDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        patientForm.setGuardian(createGuardianDTO());
        patientForm.setPolicyholder(createPolicyholderDTO());

        // Create mock patient and verify age
        Patient savedPatient = Patient.builder()
                .id(UUID.randomUUID())
                .firstName(patientForm.getFirstName())
                .lastName(patientForm.getLastName())
                .birthDate(birthDate)
                .insuranceType(InsuranceType.FAMILY_INSURED)
                .build();

        // Then: Patient should be considered a minor
        assertThat(savedPatient.isMinor()).isTrue();
        assertThat(savedPatient.getAge()).isEqualTo(17);

        // Test boundary: exactly 18 years old (adult)
        LocalDate adultBirthDate = LocalDate.now().minusYears(18);
        Patient adultPatient = Patient.builder()
                .birthDate(adultBirthDate)
                .build();
        assertThat(adultPatient.isMinor()).isFalse();
        assertThat(adultPatient.getAge()).isEqualTo(18);
    }

    // ===================================
    // Test 11: Verify insurance type determination logic
    // ===================================

    @Test
    void testSavePatient_DeterminesInsuranceType_BasedOnPolicyholderPresence() {
        // Given: Patient form with policyholder
        PatientFormDTO patientFormWithPolicyholder = createAdultPatientForm();
        patientFormWithPolicyholder.setPolicyholder(createPolicyholderDTO());

        // Mock repository
        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        when(patientRepository.save(patientCaptor.capture())).thenAnswer(invocation -> {
            Patient patient = invocation.getArgument(0);
            patient.setId(UUID.randomUUID());
            return patient;
        });
        when(policyholderRepository.save(any(Policyholder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(formSubmissionRepository.save(any(FormSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(consentRepository.save(any(Consent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Save patient with policyholder
        patientService.savePatient(patientFormWithPolicyholder, "192.168.1.1", "TestAgent");

        // Then: Insurance type should be FAMILY_INSURED
        Patient savedPatient = patientCaptor.getValue();
        assertThat(savedPatient.getInsuranceType()).isEqualTo(InsuranceType.FAMILY_INSURED);
    }

    @Test
    void testSavePatient_DeterminesInsuranceType_SelfInsuredWithoutPolicyholder() {
        // Given: Patient form without policyholder
        PatientFormDTO patientFormWithoutPolicyholder = createAdultPatientForm();
        patientFormWithoutPolicyholder.setPolicyholder(null);

        // Mock repository
        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        when(patientRepository.save(patientCaptor.capture())).thenAnswer(invocation -> {
            Patient patient = invocation.getArgument(0);
            patient.setId(UUID.randomUUID());
            return patient;
        });
        when(formSubmissionRepository.save(any(FormSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(consentRepository.save(any(Consent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Save patient without policyholder
        patientService.savePatient(patientFormWithoutPolicyholder, "192.168.1.1", "TestAgent");

        // Then: Insurance type should be SELF_INSURED
        Patient savedPatient = patientCaptor.getValue();
        assertThat(savedPatient.getInsuranceType()).isEqualTo(InsuranceType.SELF_INSURED);
    }

    // ===================================
    // Test 12: Signature handling
    // ===================================

    @Test
    void testSavePatient_SavesSignatureWhenPresent() {
        // Given: Patient with signature
        PatientFormDTO patientForm = createAdultPatientForm();
        byte[] signatureData = "test-signature-data".getBytes();
        patientForm.setSignature(signatureData);

        // Mock repositories
        Patient savedPatient = createMockPatient(patientForm, false);
        savedPatient.setInsuranceType(InsuranceType.SELF_INSURED);
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);
        when(signatureRepository.save(any(Signature.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(formSubmissionRepository.save(any(FormSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(consentRepository.save(any(Consent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Save patient
        UUID patientId = patientService.savePatient(patientForm, "192.168.1.1", "TestAgent");

        // Then: Signature should be saved
        assertThat(patientId).isNotNull();
        verify(signatureRepository).save(any(Signature.class));
    }

    @Test
    void testSavePatient_DoesNotSaveSignatureWhenAbsent() {
        // Given: Patient without signature
        PatientFormDTO patientForm = createAdultPatientForm();
        patientForm.setSignature(null);

        // Mock repositories
        Patient savedPatient = createMockPatient(patientForm, false);
        savedPatient.setInsuranceType(InsuranceType.SELF_INSURED);
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);
        when(formSubmissionRepository.save(any(FormSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(consentRepository.save(any(Consent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Save patient
        UUID patientId = patientService.savePatient(patientForm, "192.168.1.1", "TestAgent");

        // Then: Signature should NOT be saved
        assertThat(patientId).isNotNull();
        verify(signatureRepository, never()).save(any(Signature.class));
    }

    // ===================================
    // Helper Methods
    // ===================================

    /**
     * Create a minor patient form (10 years old)
     */
    private PatientFormDTO createMinorPatientForm() {
        PatientFormDTO form = new PatientFormDTO();
        form.setFirstName("Max");
        form.setLastName("Mustermann");
        form.setBirthDate(formatBirthDate(LocalDate.now().minusYears(10)));
        form.setGender("male");
        form.setStreet("Musterstraße 123");
        form.setZipCode("12345");
        form.setCity("Berlin");
        form.setMobileNumber("0171-1234567");
        form.setEmailAddress("max.mustermann@example.com");
        form.setLanguage("de");
        form.setInsuranceProvider("AOK");
        return form;
    }

    /**
     * Create an adult patient form (25 years old)
     */
    private PatientFormDTO createAdultPatientForm() {
        PatientFormDTO form = new PatientFormDTO();
        form.setFirstName("Anna");
        form.setLastName("Schmidt");
        form.setBirthDate(formatBirthDate(LocalDate.now().minusYears(25)));
        form.setGender("female");
        form.setStreet("Hauptstraße 45");
        form.setZipCode("10115");
        form.setCity("Berlin");
        form.setMobileNumber("0172-9876543");
        form.setEmailAddress("anna.schmidt@example.com");
        form.setLanguage("de");
        form.setInsuranceProvider("TK");
        return form;
    }

    /**
     * Create a guardian DTO
     */
    private GuardianDTO createGuardianDTO() {
        return GuardianDTO.builder()
                .firstName("Maria")
                .lastName("Mustermann")
                .birthDate(formatBirthDate(LocalDate.now().minusYears(40)))
                .gender("female")
                .street("Musterstraße 123")
                .zipCode("12345")
                .city("Berlin")
                .mobileNumber("0170-1111111")
                .emailAddress("maria.mustermann@example.com")
                .relationshipType("MOTHER")
                .build();
    }

    /**
     * Create a policyholder DTO
     */
    private PolicyholderDTO createPolicyholderDTO() {
        return PolicyholderDTO.builder()
                .firstName("Hans")
                .lastName("Schmidt")
                .birthDate(formatBirthDate(LocalDate.now().minusYears(45)))
                .gender("male")
                .street("Hauptstraße 45")
                .zipCode("10115")
                .city("Berlin")
                .mobileNumber("0173-2222222")
                .emailAddress("hans.schmidt@example.com")
                .job("Engineer")
                .build();
    }

    /**
     * Create a mock patient entity from form
     */
    private Patient createMockPatient(PatientFormDTO form, boolean isMinor) {
        LocalDate birthDate = isMinor ? LocalDate.now().minusYears(10) : LocalDate.now().minusYears(25);
        Patient patient = Patient.builder()
                .id(UUID.randomUUID())
                .firstName(form.getFirstName())
                .lastName(form.getLastName())
                .birthDate(birthDate)
                .gender(form.getGender())
                .street(form.getStreet())
                .zipCode(form.getZipCode())
                .city(form.getCity())
                .mobileNumber(form.getMobileNumber())
                .emailAddress(form.getEmailAddress())
                .language(form.getLanguage())
                .insuranceProvider(form.getInsuranceProvider())
                .build();

        return patient;
    }

    /**
     * Create a saved minor patient entity
     */
    private Patient createSavedMinorPatient(UUID id) {
        return Patient.builder()
                .id(id)
                .firstName("Max")
                .lastName("Mustermann")
                .birthDate(LocalDate.now().minusYears(10))
                .gender("male")
                .street("Musterstraße 123")
                .zipCode("12345")
                .city("Berlin")
                .insuranceType(InsuranceType.FAMILY_INSURED)
                .build();
    }

    /**
     * Create a saved adult patient entity
     */
    private Patient createSavedAdultPatient(UUID id) {
        return Patient.builder()
                .id(id)
                .firstName("Anna")
                .lastName("Schmidt")
                .birthDate(LocalDate.now().minusYears(25))
                .gender("female")
                .street("Hauptstraße 45")
                .zipCode("10115")
                .city("Berlin")
                .insuranceType(InsuranceType.SELF_INSURED)
                .build();
    }

    /**
     * Create a saved guardian entity
     */
    private LegalGuardian createSavedGuardian(Patient patient) {
        return LegalGuardian.builder()
                .patient(patient)
                .firstName("Maria")
                .lastName("Mustermann")
                .birthDate(LocalDate.now().minusYears(40))
                .gender("female")
                .street("Musterstraße 123")
                .zipCode("12345")
                .city("Berlin")
                .mobileNumber("0170-1111111")
                .relationshipType(LegalGuardian.RelationshipType.MOTHER)
                .build();
    }

    /**
     * Create a saved policyholder entity
     */
    private Policyholder createSavedPolicyholder(Patient patient) {
        return Policyholder.builder()
                .patient(patient)
                .firstName("Hans")
                .lastName("Schmidt")
                .birthDate(LocalDate.now().minusYears(45))
                .gender("male")
                .street("Hauptstraße 45")
                .zipCode("10115")
                .city("Berlin")
                .mobileNumber("0173-2222222")
                .build();
    }

    /**
     * Format birth date to DD.MM.YYYY string
     */
    private String formatBirthDate(LocalDate date) {
        return date.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
}
