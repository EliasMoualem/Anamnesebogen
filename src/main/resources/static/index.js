let signaturePad;

document.addEventListener("DOMContentLoaded", function() {
    // Initialize signature pad
    const canvas = document.getElementById("signature-pad");
    signaturePad = new SignaturePad(canvas);

    // Reset button
    document.getElementById("reset-button").addEventListener("click", function() {
        signaturePad.clear();
    });

    // Form submission
    document.getElementById("patient-form").addEventListener("submit", function(event) {
        if (signaturePad.isEmpty()) {
            if (!confirm("Sie haben nicht unterschrieben. Möchten Sie fortfahren?")) {
                event.preventDefault();
                return;
            }
        } else {
            // Get signature data and add it to the form
            const signatureData = signaturePad.toDataURL();

            // Create hidden input for signature data
            let signatureInput = document.getElementById("signature-data");
            if (!signatureInput) {
                signatureInput = document.createElement("input");
                signatureInput.type = "hidden";
                signatureInput.name = "signatureData";
                signatureInput.id = "signature-data";
                this.appendChild(signatureInput);
            }

            // Set value
            signatureInput.value = signatureData;
        }
    });
});