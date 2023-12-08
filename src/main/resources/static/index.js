    let signaturePad
    let signatureData

    document.getElementById("reset-button").addEventListener("click", function(){
        signaturePad.clear()

    });
    document.getElementById("submit-button").addEventListener("click", function(){
        signatureData = signaturePad.toDataURL();
        console.log(signatureData)
    });

    document.addEventListener("DOMContentLoaded", function(){
        const canvas = document.getElementById("signature-pad")
        signaturePad = new SignaturePad(canvas)
    });